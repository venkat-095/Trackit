package com.example.trackit;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private FusedLocationProviderClient fusedLocationClient;
    private TextView latitudeTextView, longitudeTextView, altitudeTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        latitudeTextView = findViewById(R.id.latitudeTextView);
        longitudeTextView = findViewById(R.id.longitudeTextView);
        altitudeTextView = findViewById(R.id.altitudeTextView);

        Button getLocationButton = findViewById(R.id.getLocationButton);
        Button openInMapsButton = findViewById(R.id.openInMapsButton);
        Button shareLocationButton = findViewById(R.id.shareLocationButton);
        Button panicButton = findViewById(R.id.panicButton);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Set up button click listeners with animations
        setupButtonAnimations(getLocationButton);
        getLocationButton.setOnClickListener(v -> getLocation());

        setupButtonAnimations(openInMapsButton);
        openInMapsButton.setOnClickListener(v -> openInMaps());

        setupButtonAnimations(shareLocationButton);
        shareLocationButton.setOnClickListener(v -> shareLocation());

        setupButtonAnimations(panicButton);
        panicButton.setOnClickListener(v -> sendEmergencyAlert());
    }

    private void setupButtonAnimations(Button button) {
        button.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.button_click_scale));
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.clearAnimation();
                    break;
            }
            return false;
        });
    }

    private void getLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            updateLocationUI(location);
                        } else {
                            Toast.makeText(MainActivity.this, "Unable to get location.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void updateLocationUI(Location location) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        double altitude = location.getAltitude();

        latitudeTextView.setText(String.format("Latitude: %s", latitude));
        longitudeTextView.setText(String.format("Longitude: %s", longitude));
        altitudeTextView.setText(String.format("Altitude: %s", altitude));

        // Save location to database (assuming database implementation is in place)
        LocationEntity locationEntity = new LocationEntity(latitude, longitude, altitude, System.currentTimeMillis());
        new Thread(() -> LocationDatabase.getInstance(getApplicationContext()).locationDao().insert(locationEntity)).start();
    }

    private void openInMaps() {
        String latitude = latitudeTextView.getText().toString().split(": ")[1];
        String longitude = longitudeTextView.getText().toString().split(": ")[1];

        if (!latitude.isEmpty() && !longitude.isEmpty()) {
            String uri = "geo:" + latitude + "," + longitude;
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            startActivity(intent);
        } else {
            Toast.makeText(this, "Location not available. Retrieve location first.", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareLocation() {
        String latitude = latitudeTextView.getText().toString().split(": ")[1];
        String longitude = longitudeTextView.getText().toString().split(": ")[1];

        if (!latitude.isEmpty() && !longitude.isEmpty()) {
            // Create a Google Maps link
            String mapsLink = "https://www.google.com/maps?q=" + latitude + "," + longitude;
            String shareText = "My current location: " + mapsLink;

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
            startActivity(Intent.createChooser(shareIntent, "Share Location via"));
        } else {
            Toast.makeText(this, "Location not available. Retrieve location first.", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendEmergencyAlert() {
        String latitudeText = latitudeTextView.getText().toString();
        String longitudeText = longitudeTextView.getText().toString();

        // Check if both latitude and longitude values are present
        if (latitudeText.contains(":") && longitudeText.contains(":")) {
            // Extract latitude and longitude values after the colon
            String latitude = latitudeText.split(": ")[1].trim();
            String longitude = longitudeText.split(": ")[1].trim();

            // Construct the Google Maps link
            String mapsLink = "https://www.google.com/maps?q=" + latitude + "," + longitude;
            String emergencyText = "Emergency! I need help! Here is my current location:\n" + mapsLink;

            Intent sendIntent = new Intent(Intent.ACTION_SENDTO);
            sendIntent.setData(Uri.parse("smsto:")); // Only SMS apps should handle this
            sendIntent.putExtra("sms_body", emergencyText);
            startActivity(sendIntent);
        } else {
            Toast.makeText(this, "Location not available. Retrieve location first.", Toast.LENGTH_SHORT).show();
        }
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocation();
            } else {
                Toast.makeText(this, "Permission denied.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
