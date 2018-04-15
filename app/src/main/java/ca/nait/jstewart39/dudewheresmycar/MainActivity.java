package ca.nait.jstewart39.dudewheresmycar;

import android.content.Context;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback
{
    private static final String TAG = "MainActivity";
    TextView textViewCurrentLatitude;
    TextView textViewCurrentLongitude;
    TextView textViewSetLatitude;
    TextView textViewSetLongitude;
    TextView textViewMeters;
    Button btnSetLocation;

    double currentLat;
    double currentLong;

    // REMINDER - in order to make this app more versatile, make buttons to set these latitudes and longitudes on demand
    double setLat = 53.440832;
    double setLong = -113.427787;

    GoogleMap mGoogleMap;
    Marker marker;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        // retrieve the content view for the main screen
        setContentView(R.layout.activity_main);

        // Get the SupportMapFragment and request notification when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Instantiate TextViews
        textViewCurrentLatitude = (TextView) findViewById(R.id.text_view_current_lat);
        textViewCurrentLongitude = (TextView) findViewById(R.id.text_view_current_long);
        textViewSetLatitude = (TextView) findViewById(R.id.text_view_set_lat);
        textViewSetLongitude = (TextView) findViewById(R.id.text_view_set_long);
        textViewMeters = (TextView) findViewById(R.id.text_view_meters);
        btnSetLocation = (Button) findViewById(R.id.btn_set_location);

        // this is to get the location service
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        // this wants us to catch a security exception, so we wrap it in a try/catch
        try
        {
            // if this is set to 0,0 it updates location very fast. Make sure to ask permissions in the manifest
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, new MyLocationListener());
        } catch (SecurityException e)
        {
            Toast.makeText(this, "Security Exception - Go to app info and enable location services to use this app", Toast.LENGTH_LONG).show();
        }

        btnSetLocation.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                setLat = currentLat;
                setLong = currentLong;
                // check that there is an existing marker and remove it
                if (marker != null)
                {
                    marker.remove();
                }

                // add a new marker at the current position
                LatLng latLng = new LatLng(currentLat, currentLong);
                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(latLng);
                markerOptions.title("Set Location Marker");
                marker = mGoogleMap.addMarker(markerOptions);

                //move map camera to new location
                mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14));
            }
        });
    }

    // this configures the map to the Lat and Long that are set
    @Override
    public void onMapReady(GoogleMap googleMap)
    {
        mGoogleMap = googleMap;
        // move the maps camera to the set location.
        LatLng setLocation = new LatLng(currentLat, currentLong);
        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(setLocation));
        // this is used to zoom on the current location
        mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(setLocation, 14));
    }

    // Called when the location has changed
    public class MyLocationListener implements LocationListener
    {

        @Override
        public void onLocationChanged(Location location)
        {
            double meters = 33;
            if (location != null) // before getting Latitude and Longitude, check if location is not null
            {
                // get the current Latitude and Longitude
                currentLat = location.getLatitude();
                currentLong = location.getLongitude();

                meters = calcMeterDifference(currentLat, currentLong);

                /* and alternative instead of the calcMeterDifference method that could be used is:
                   meters = setLocation.distanceTo(location);
                   although, this way may not be as accurate so it is not being used */

                // format the meters into a decimal format
                NumberFormat formatter = new DecimalFormat("0.00");
                String strMeters = formatter.format(meters);

                // set the set and current TextViews to the proper Longitude and Latitude
                textViewSetLatitude.setText(String.valueOf(setLat));
                textViewSetLongitude.setText(String.valueOf(setLong));
                textViewCurrentLatitude.setText(String.valueOf(currentLat));
                textViewCurrentLongitude.setText(String.valueOf(currentLong));
                // set the metersTextView to distance from the set location in meters
                textViewMeters.setText(strMeters);
            }
        }

        // this is an equation to calculate the distance from one point to another that includes the curvature of the earth (not straight through)
        private double calcMeterDifference(double currentLat, double currentLong)
        {
            double distanceInMeters = 45;
            // earths radius in miles
            double earthRadius = 3958.75;
            double dLatitude = Math.toRadians(currentLat - setLat);
            double dLongitude = Math.toRadians(currentLong - setLong);
            double sindLat = Math.sin(dLatitude / 2);
            double sindLon = Math.sin(dLongitude / 2);
            double a = Math.pow(sindLat, 2) + Math.pow(sindLon, 2) * Math.cos(currentLat) * Math.cos(setLat);
            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
            double distInMiles = earthRadius * c;

            distanceInMeters = (distInMiles * 1.609344 * 1000);
            return distanceInMeters;
        }

        // Called when the provider status changes
        @Override
        public void onStatusChanged(String s, int i, Bundle bundle)
        {
            Toast.makeText(MainActivity.this, "Status Changed", Toast.LENGTH_LONG).show();
        }

        // Called when the provider is enabled by the user
        @Override
        public void onProviderEnabled(String s)
        {
            Toast.makeText(MainActivity.this, "Provider Enabled", Toast.LENGTH_LONG).show();
        }

        // Called when the provider is disabled by the user
        @Override
        public void onProviderDisabled(String s)
        {
            Toast.makeText(MainActivity.this, "Provider Disabled", Toast.LENGTH_LONG).show();
        }
    }
}