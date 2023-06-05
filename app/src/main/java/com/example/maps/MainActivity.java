package com.example.maps;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.TextView;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends Activity implements LocationListener {
    private static final int PERMISSION_REQUEST_CODE = 1;
    MapView map = null;
    ScaleBarOverlay mScaleBarOverlay;
    TextView speedTextView;
    LocationManager locationManager;
    boolean isLocationUpdatesEnabled = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Request necessary permissions
        String[] permissions = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        };
        requestPermissions(permissions, PERMISSION_REQUEST_CODE);

        // Load/initialize the osmdroid configuration
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        // Set the layout
        setContentView(R.layout.activity_main);

        // Get references to the views
        map = findViewById(R.id.maps);
        speedTextView = findViewById(R.id.speedTextView);

        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            // Check if the required permissions were granted
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permissions granted, do any required initialization
                map.onResume();

                // Get the user's location
                getUserLocation();
            } else {
                // Permissions denied, handle the situation gracefully
            }
        }
    }

    private void getUserLocation() {
        // Check if location permissions are granted
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            // Get the user's location
            MyLocationNewOverlay myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), map);
            myLocationOverlay.enableMyLocation();
            map.getOverlays().add(myLocationOverlay);

            IMapController mapController = map.getController();
            myLocationOverlay.runOnFirstFix(() -> {
                Location location = myLocationOverlay.getLastFix();

                if (location != null) {
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();
                    final GeoPoint userLocation = new GeoPoint(latitude, longitude);

                    runOnUiThread(() -> {
                        mapController.setCenter(userLocation);
                    });
                } else {
                    // Handle the case where the location is not available
                    // You can set a default location or display an error message
                }
            });

            // Set map zoom level
            mapController.setZoom(15);

            // Start listening for location updates
            startLocationUpdates();
        } else {
            // Permissions not granted, handle the situation gracefully
        }
    }

    private void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            // Initialize the location manager
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

            // Request location updates
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1000, // Minimum time interval between updates (in milliseconds)
                    0, // Minimum distance between updates (in meters)
                    this);

            isLocationUpdatesEnabled = true;
        } else {
            // Permissions not granted, handle the situation gracefully
        }
    }

    private void stopLocationUpdates() {
        if (locationManager != null) {
            locationManager.removeUpdates(this);
            isLocationUpdatesEnabled = false;
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        // Update the user's location on the map
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        final GeoPoint userLocation = new GeoPoint(latitude, longitude);

        runOnUiThread(() -> {
            IMapController mapController = map.getController();
            mapController.setCenter(userLocation);

            // Update the speed text view
            float speed = location.getSpeed();
            speedTextView.setText(String.format("Speed: %.2f m/s", speed));
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        map.onResume();

        if (!isLocationUpdatesEnabled) {
            startLocationUpdates();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        map.onPause();

        if (isLocationUpdatesEnabled) {
            stopLocationUpdates();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopLocationUpdates();
    }

    // Other LocationListener methods
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }
}