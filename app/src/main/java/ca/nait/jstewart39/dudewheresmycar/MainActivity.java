package ca.nait.jstewart39.dudewheresmycar;

import android.Manifest;
import android.content.Context;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
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
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback
{
    private static final String TAG = "MainActivity";
    TextView textViewCurrentLatLong;
    TextView textViewSetLatLong;
    TextView textViewMeters;
    Button btnSetLocation;
    double currentLat;
    double currentLong;
    double setLat;
    double setLong;

    GoogleMap mGoogleMap;
    Marker setMarker;
    Marker currentMarker;
    Polyline line = null;
    protected LocationManager locationManager;
    Location location;
    public static final int PERMISSIONS_REQUEST_ACCESS_LOCATION = 1;

    public static String globalPreferences = "";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        // retrieve the content view for the main screen
        setContentView(R.layout.activity_main);

        // check for permissions at runtime
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_ACCESS_LOCATION);
        }

        // this is for getting the set location again after the app is closed
        SharedPreferences sharedPreferences = getSharedPreferences(globalPreferences, MODE_PRIVATE);
        // The 0.0 is there for if the the location hasn't been set before
        String stringSetLat = sharedPreferences.getString("setLat", "0.0");
        String stringSetLong = sharedPreferences.getString("setLong", "0.0");
        // set it back to double and assign it to setLat/setLong
        setLat = Double.parseDouble(stringSetLat);
        setLong = Double.parseDouble(stringSetLong);

        // Instantiate TextViews
        textViewCurrentLatLong = (TextView) findViewById(R.id.text_view_current_lat_long);
        textViewSetLatLong = (TextView) findViewById(R.id.text_view_set_lat_long);
        textViewMeters = (TextView) findViewById(R.id.text_view_meters);
        btnSetLocation = (Button) findViewById(R.id.btn_set_location);

        // Get the SupportMapFragment and request notification when the map is ready to be used.
        // Required for the map to work properly.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // this is to get the location service
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        btnSetLocation.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                setLat = currentLat;
                setLong = currentLong;
                // check that there is an existing setMarker and remove it
                if (setMarker != null)
                {
                    setMarker.remove();
                }

                // add a new setMarker at the current position
                LatLng latLng = new LatLng(currentLat, currentLong);
                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(latLng);
                markerOptions.title("Set Location Marker");
                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.car_icon_driving_car));
                setMarker = mGoogleMap.addMarker(markerOptions);

                //move map camera to new location
                mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14));

                // this is for saving the setLat and setLong so it can be retrieved again when the app closes
                SharedPreferences.Editor editor = getSharedPreferences(globalPreferences, MODE_PRIVATE).edit();
                editor.putString("setLat", Double.toString(setLat));
                editor.putString("setLong", Double.toString(setLong));
                editor.commit(); // Very important
            }
        });

    }

    // catch the request permissions result from the onCreate method
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
    {
        switch (requestCode)
        {
            case PERMISSIONS_REQUEST_ACCESS_LOCATION:
            {
                // if the request is cancelled, the result arrays are empty
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {

                    // permission was granted
                    Toast.makeText(getApplicationContext(), "Location permission allowed", Toast.LENGTH_SHORT).show();
                    // now reload the app to get the map working with the current or last known Lat and Long
                    finish();
                    startActivity(getIntent());
                } else
                {
                    // permission was denied
                    Toast.makeText(getApplicationContext(), "Permission denied. You must allow location permission to use this app!",
                            Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
    }

    // gets the last known location to be used on the onMapReady method, so it centers on current location when app is launched
    public Location getLocation(String provider)
    {
        if (locationManager.isProviderEnabled(provider))
        {
            // check that permissions are allowed before requesting for location updates
            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            {
                // if this is set to 0,0 it updates location very fast. Make sure to ask permissions in the manifest
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, new MyLocationListener());
                if (locationManager != null)
                {
                    location = locationManager.getLastKnownLocation(provider);
                    return location;
                }
            }
        }
        return location;
    }

    // this configures the map to the Lat and Long that are set
    @Override
    public void onMapReady(GoogleMap googleMap)
    {
        Location newlocation = getLocation(LocationManager.NETWORK_PROVIDER);
        if (newlocation != null) // before setting the map, check that the newlocation is not null
        {
            currentLat = newlocation.getLatitude();
            currentLong = newlocation.getLongitude();
            mGoogleMap = googleMap;
            // move the maps camera to the set location.
            LatLng setLocation = new LatLng(currentLat, currentLong);
            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(setLocation));
            // this is used to zoom on the current location
            mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(setLocation, 14));

            // this is for adding the set location icon again on startup if the setLat and setLong have been set before
            if (setLat != 0.0 && setLong != 0.0)
            {
                LatLng setLatLng = new LatLng(setLat, setLong);
                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(setLatLng);
                markerOptions.title("Set Location Marker");
                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.car_icon_driving_car));
                setMarker = mGoogleMap.addMarker(markerOptions);
            }
        }
    }

    // Called when the location has changed
    public class MyLocationListener implements LocationListener
    {
        @Override
        public void onLocationChanged(Location location)
        {
            double meters = 0;
            if (location != null) // before getting Latitude and Longitude, check if location is not null
            {
                // get the current Latitude and Longitude
                currentLat = location.getLatitude();
                currentLong = location.getLongitude();

                // this is for getting the meter difference between the set location and current location
                // and setting that to the meters variable
                meters = calcMeterDifference(currentLat, currentLong);

                // this is for drawing the polyline from the set location to the current location on the map
                if (setLat != 0 && setLong != 0) // check first that the location has been set to a Lat/Long
                {
                    if (line != null) // if line is not null, remove the old one
                    {
                        line.remove();
                    }
                    // now add a line from the current location to the set location
                    line = mGoogleMap.addPolyline(new PolylineOptions()
                            .add(new LatLng(currentLat, currentLong), new LatLng(setLat, setLong))
                            .width(5)
                            .color(Color.RED));
                }

                // format the meters into a decimal format
                NumberFormat formatter = new DecimalFormat("0.00");
                String strMeters = formatter.format(meters);

                // set the set and current TextViews to the proper Longitude and Latitude
                textViewSetLatLong.setText(String.valueOf(setLat) + "/" + String.valueOf(setLong));
                textViewCurrentLatLong.setText(String.valueOf(currentLat) + "/" + String.valueOf(currentLong));

                // set the metersTextView to distance from the set location in meters
                if (strMeters.equals("NaN")) // if the location hasn't been set, display proper message rather than "NaN"
                {
                    strMeters = "No location set";
                    textViewMeters.setText(strMeters);
                } else
                {
                    textViewMeters.setText(strMeters + " meters");
                }

                // check if current marker exists and remove the old one
                if (currentMarker != null)
                {
                    currentMarker.remove();
                }

                // show the current marker for the current location
                currentMarker = mGoogleMap.addMarker(new MarkerOptions()
                        .position(new LatLng(currentLat, currentLong))
                        .title("Current Location Marker")
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.maps_green_marker)));
            }
        }

        // this is an equation to calculate the distance from one point to another that includes
        // the curvature of the earth (not straight through)
        private double calcMeterDifference(double currentLat, double currentLong)
        {
            double distanceInMeters = 0;
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

        // These next three override methods are needed for the LocationListener
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