package com.example.maps;

import static com.example.maps.R.id.txtLongitude;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity implements LocationListener {
    private static final int PERMISSION_REQUEST_CODE = 1;
    MapView map = null;
    ScaleBarOverlay mScaleBarOverlay;
    TextView speedTextView, txtLongitude, txtLatitude;
    LocationManager locationManager;
    boolean isLocationUpdatesEnabled = false;
    private Timer timer;
    private TimerTask timerTask;

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

        txtLongitude = findViewById(R.id.txtLongitude);
        txtLatitude = findViewById(R.id.txtLatitude);


        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);

        // Start the LocationService
        startService(new Intent(this, LocationService.class));
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
                String fileName = "data.txt";
                String filePath = "/storage/emulated/0/Android/data/com.example.maps/files/";
                Location location = myLocationOverlay.getLastFix();
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                // Update the speed text view
                float speed = location.getSpeed();

                // Create a SimpleDateFormat instance with the desired format
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());

                if (location != null) {
                    final GeoPoint userLocation = new GeoPoint(latitude, longitude);

                    runOnUiThread(() -> {
                        mapController.setCenter(userLocation);
                        txtLongitude.setText("Longitude : " + longitude);
                        txtLatitude.setText("Latitude : " + latitude);
                        speedTextView.setText(String.format("Speed: %.2f m/s", speed));

                        // Create a File object
                        File file = new File(filePath + fileName);

                        try {
                            // Create the file if it doesn't exist
                            if (!file.exists()) {
                                file.createNewFile();
                            }

                            // Create a FileOutputStream to write to the file
                            FileOutputStream fos = new FileOutputStream(file, true); // Pass 'true' to append data to the existing file

                            // Create an OutputStreamWriter to write characters to the file
                            OutputStreamWriter osw = new OutputStreamWriter(fos);

                            // Write data to the file
                            osw.write("Time : " + dateFormat.format(new Date()) + ", " + "Longitude : " + longitude + ", " + "Latitude : " + latitude + ", " + "Speed : " + String.format("Speed: %.2f m/s", speed) + "\n");

                            // Close the OutputStreamWriter
                            osw.close();

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
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

            Intent serviceIntent = new Intent(this, LocationService.class);
            startService(serviceIntent);
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

            // Cancel the timer and timer task
            if (timer != null) {
                timer.cancel();
            }
            if (timerTask != null) {
                timerTask.cancel();
            }
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        String fileName = "data.txt";
        String filePath = "/storage/emulated/0/Android/data/com.example.maps/files/";

        // Create a File object
        File file = new File(filePath + fileName);

        // Update the user's location on the map
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        final GeoPoint userLocation = new GeoPoint(latitude, longitude);
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());

        runOnUiThread(() -> {
            IMapController mapController = map.getController();
            mapController.setCenter(userLocation);

            // Update the speed text view
            float speed = location.getSpeed();
            speedTextView.setText(String.format("Speed: %.2f m/s", speed));
            txtLongitude.setText("Longitude : " + longitude);
            txtLatitude.setText("Latitude : " + latitude);
            timer = new Timer();
            timerTask = new TimerTask() {
                @Override
                public void run() {
                    try {
                        // Create the file if it doesn't exist
                        if (!file.exists()) {
                            file.createNewFile();
                        }

                        // Create a FileOutputStream to write to the file
                        FileOutputStream fos = new FileOutputStream(file, true); // Pass 'true' to append data to the existing file

                        // Create an OutputStreamWriter to write characters to the file
                        OutputStreamWriter osw = new OutputStreamWriter(fos);

                        // Write data to the file
                        osw.write("Time : " + dateFormat.format(new Date()) + ", " + "Longitude : " + longitude + ", " + "Latitude : " + latitude + ", " + "Speed : " + String.format("Speed: %.2f m/s", speed) + "\n");

                        // Close the OutputStreamWriter
                        osw.close();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            };
            // Schedule the timer task to run every minute (60000 milliseconds)
            timer.schedule(timerTask, 0, 60000);
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
//        Intent serviceIntent = new Intent(this, LocationService.class);
//        stopService(serviceIntent);
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

    public void writeToLogFile(String content) {
        try {
            String filename = "mi_exception_log";

            // Get the file object
            File file = new File(getApplicationContext().getFilesDir(), filename);

            // Create a FileWriter object with append mode (true)
            FileWriter writer = new FileWriter(file, true);

            // Write the content to the file
            writer.write(content);
            writer.close();

            // Writing to the file was successful
            // Add any additional logic or error handling as needed
        } catch (IOException e) {
            e.printStackTrace();
            // Handle the exception, log the error, or display an error message
        }
    }

}