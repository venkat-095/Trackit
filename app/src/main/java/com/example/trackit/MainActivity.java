package com.example.trackit;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private TextView latitudeTextView, longitudeTextView, altitudeTextView;
    private Button getLocationButton, openInMapsButton;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private boolean isLocationUpdatesActive = false; // Flag to track location updates state

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        latitudeTextView = findViewById(R.id.latitudeTextView);
        longitudeTextView = findViewById(R.id.longitudeTextView);
        altitudeTextView = findViewById(R.id.altitudeTextView);
        getLocationButton = findViewById(R.id.getLocationButton);
        openInMapsButton = findViewById(R.id.openInMapsButton);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Define location callback
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null || !isLocationUpdatesActive) {
                    return; // Stop processing if location updates are not active
                }
                for (Location location : locationResult.getLocations()) {
                    updateLocationUI(location);
                }
                // Optionally stop updates after receiving location
                // isLocationUpdatesActive = false;
                // fusedLocationClient.removeLocationUpdates(locationCallback);
            }
        };

        // Check location permissions and request if not granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }

        // Button to get location
        getLocationButton.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                toggleLocationUpdates(); // Call the toggle method
            } else {
                Toast.makeText(this, "Location permission not granted", Toast.LENGTH_SHORT).show();
            }
        });

        // Button to open location in Google Maps
        openInMapsButton.setOnClickListener(v -> openLocationInGoogleMaps());
    }

    private void toggleLocationUpdates() {
        if (isLocationUpdatesActive) {
            // If location updates are active, stop them
            fusedLocationClient.removeLocationUpdates(locationCallback);
            isLocationUpdatesActive = false;
            Toast.makeText(this, "Stopped getting GPS location.", Toast.LENGTH_SHORT).show();
        } else {
            // If location updates are not active, start them
            requestLocationUpdates();
            isLocationUpdatesActive = true;
        }
    }

    private void requestLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(2000);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, getMainLooper());
            Toast.makeText(this, "Getting GPS location...", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateLocationUI(Location location) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        double altitude = location.getAltitude();

        latitudeTextView.setText(String.format("Latitude: %s", latitude));
        longitudeTextView.setText(String.format("Longitude: %s", longitude));
        altitudeTextView.setText(String.format("Altitude: %s", altitude));
    }

    private void openLocationInGoogleMaps() {
        String latitude = latitudeTextView.getText().toString().split(": ")[1];
        String longitude = longitudeTextView.getText().toString().split(": ")[1];

        if (!latitude.isEmpty() && !longitude.isEmpty()) {
            // Create the geo URI with latitude and longitude
            String uri = String.format("geo:%s,%s?q=%s,%s", latitude, longitude, latitude, longitude);
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));

            // Create an Intent chooser to allow the user to select an app
            Intent chooser = Intent.createChooser(intent, "Open with");

            if (chooser.resolveActivity(getPackageManager()) != null) {
                startActivity(chooser);
            } else {
                Toast.makeText(this, "No compatible app found to open map location.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Location not available. Retrieve location first.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestLocationUpdates();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
