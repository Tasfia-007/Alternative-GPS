package com.example.myapplication;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.telephony.SmsManager;
import android.util.Log;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AlertActivity extends AppCompatActivity {
    private static final String TAG = "AlertActivity";
    private static final int SMS_PERMISSION_REQUEST = 100;

    private TextView contactName, contactNumber;
    private Button alertButton;

    private String emergencyNumber = "Include Number";
    private String emergencyName = "Include Contact";
    private String currentLocation = "Unknown Location";

    // Supabase API details
    private static final String SUPABASE_URL = "https://kquvuygavkhsxvdpqyfn.supabase.co"; // Replace with your Supabase URL
    private static final String SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImtxdXZ1eWdhdmtoc3h2ZHBxeWZuIiwicm9sZSI6ImFub24iLCJpYXQiOjE3MzcxMDQ4NjcsImV4cCI6MjA1MjY4MDg2N30.YVPKExfM-ZxzO9JvM9RQZQrBiyG1iT50fiwGUcvw8EI";
    private static final String CONTACT_TABLE = "emergency_contacts";
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alert);

        contactName = findViewById(R.id.contact_name);
        contactNumber = findViewById(R.id.contact_number);
        alertButton = findViewById(R.id.alert_button);

        // Initialize SharedPreferences to get the logged-in user information
        sharedPreferences = getSharedPreferences("UserSession", Context.MODE_PRIVATE);

        // Fetch emergency contact based on the logged-in user's userId
        String loggedInUserId = sharedPreferences.getString("userId", "");
        fetchEmergencyContact(loggedInUserId);

        // Check SMS permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, SMS_PERMISSION_REQUEST);
        }

        // Fetch user's location
        fetchCurrentLocation();

        // Alert button click listener
        alertButton.setOnClickListener(v -> {
            if (emergencyNumber != null && !emergencyNumber.equals("Include Number")) {
                sendAlertMessage();
            } else {
                Toast.makeText(AlertActivity.this, "Emergency contact not available", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Fetch emergency contact based on logged-in user's userId
    private void fetchEmergencyContact(String userId) {
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient(); // Create the OkHttpClient instance here
                // Modify the URL to fetch data based on logged-in user's userId
                String url = SUPABASE_URL + "/rest/v1/" + CONTACT_TABLE + "?user_id=eq." + userId;

                Log.d(TAG, "Supabase URL: " + url); // Log the URL to check if it's correctly formed

                Request request = new Request.Builder()
                        .url(url)
                        .addHeader("apikey", SUPABASE_KEY)
                        .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                        .build();

                Response response = client.newCall(request).execute();

                // Log the response to understand what is being returned
                if (response.isSuccessful()) {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    Log.d(TAG, "Response Body: " + responseBody); // Log the full response body

                    // Parse the JSON response
                    JSONArray jsonArray = new JSONArray(responseBody);

                    if (jsonArray.length() > 0) {
                        JSONObject contact = jsonArray.getJSONObject(0);
                        emergencyName = contact.optString("contact_name", "Include Contact");
                        emergencyNumber = contact.optString("contact_number", "Include Number");
                    }

                    runOnUiThread(() -> {
                        contactName.setText(emergencyName);
                        contactNumber.setText(emergencyNumber);

                        if (!emergencyNumber.equals("Include Number")) {
                            alertButton.setEnabled(true);
                            alertButton.setBackgroundColor(getResources().getColor(android.R.color.holo_red_dark));
                        } else {
                            alertButton.setEnabled(false);
                            alertButton.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
                        }
                    });
                } else {
                    Log.e(TAG, "Failed to fetch emergency contact: " + response.code());
                    String errorBody = response.body() != null ? response.body().string() : "";
                    Log.e(TAG, "Error Response Body: " + errorBody); // Log error body to understand the response
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching emergency contact: ", e);
            }
        }).start();
    }

    // Fetch current location
    private void fetchCurrentLocation() {
        try {
            LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            if (locationManager != null) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (location != null) {
                        currentLocation = "https://www.google.com/maps?q=" + location.getLatitude() + "," + location.getLongitude();
                    }
                } else {
                    Toast.makeText(this, "Location permission not granted", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error fetching location: ", e);
        }
    }

    // Send SMS alert with location
    private void sendAlertMessage() {
        String message = "I NEED HELP!\nMy location: " + currentLocation;
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(emergencyNumber, null, message, null, null);
            Toast.makeText(this, "Alert sent successfully!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to send alert", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "SMS error: ", e);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SMS_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "SMS permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}