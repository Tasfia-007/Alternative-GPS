package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ProfileActivity extends AppCompatActivity {
    private static final String TAG = "ProfileActivity";
    private static final String SUPABASE_URL = "https://kquvuygavkhsxvdpqyfn.supabase.co";
    private static final String SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImtxdXZ1eWdhdmtoc3h2ZHBxeWZuIiwicm9sZSI6ImFub24iLCJpYXQiOjE3MzcxMDQ4NjcsImV4cCI6MjA1MjY4MDg2N30.YVPKExfM-ZxzO9JvM9RQZQrBiyG1iT50fiwGUcvw8EI";

    private ImageView profilePicture;
    private TextView usernameDisplay;
    private TextView emailDisplay;
    private TextView emergencyContactNameDisplay;
    private TextView emergencyContactNumberDisplay;
    private Button editProfileButton;

    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Initialize views
        profilePicture = findViewById(R.id.profile_picture);
        usernameDisplay = findViewById(R.id.username_display);
        emailDisplay = findViewById(R.id.email_display);
        emergencyContactNameDisplay = findViewById(R.id.emergency_contact_name_display);
        emergencyContactNumberDisplay = findViewById(R.id.emergency_contact_number_display);
        editProfileButton = findViewById(R.id.edit_profile_button);

        // Get user ID from shared preferences
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        userId = sharedPreferences.getString("userId", null);

        if (userId == null) {
            Toast.makeText(this, "User is not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Fetch user data
        fetchUserData();

        // Edit Profile button click listener
        editProfileButton.setOnClickListener(v -> {
            startActivity(new Intent(ProfileActivity.this, EditProfileActivity.class));
        });
    }

    private void fetchUserData() {
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();

                // Fetch user data from the signup table
                String userUrl = SUPABASE_URL + "/rest/v1/signup?select=*&id=eq." + userId;
                Request userRequest = new Request.Builder()
                        .url(userUrl)
                        .addHeader("apikey", SUPABASE_KEY)
                        .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                        .get()
                        .build();

                Response userResponse = client.newCall(userRequest).execute();
                if (userResponse.isSuccessful() && userResponse.body() != null) {
                    String responseBody = userResponse.body().string();
                    Log.d(TAG, "User data fetched successfully: " + responseBody);

                    JSONArray userArray = new JSONArray(responseBody);
                    if (userArray.length() > 0) {
                        JSONObject userData = userArray.getJSONObject(0);
                        String username = userData.getString("username");
                        String email = userData.getString("emailid");
                        String imageUrl = userData.optString("image", null);

                        runOnUiThread(() -> {
                            usernameDisplay.setText(username);
                            emailDisplay.setText(email);

                            // Fetch and display profile picture
                            if (imageUrl != null && !imageUrl.isEmpty()) {
                                fetchProfileImage(imageUrl);
                            }
                        });
                    }
                }

                // Fetch emergency contact data from the emergency_contacts table
                String contactUrl = SUPABASE_URL + "/rest/v1/emergency_contacts?select=*&user_id=eq." + userId;
                Request contactRequest = new Request.Builder()
                        .url(contactUrl)
                        .addHeader("apikey", SUPABASE_KEY)
                        .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                        .get()
                        .build();

                Response contactResponse = client.newCall(contactRequest).execute();
                if (contactResponse.isSuccessful() && contactResponse.body() != null) {
                    String responseBody = contactResponse.body().string();
                    Log.d(TAG, "Emergency contact data fetched successfully: " + responseBody);

                    JSONArray contactArray = new JSONArray(responseBody);
                    if (contactArray.length() > 0) {
                        JSONObject contactData = contactArray.getJSONObject(0);
                        String contactName = contactData.optString("contact_name", "Not Updated");
                        String contactNumber = contactData.optString("contact_number", "Not Updated");

                        runOnUiThread(() -> {
                            emergencyContactNameDisplay.setText(contactName);
                            emergencyContactNumberDisplay.setText(contactNumber);
                        });
                    } else {
                        runOnUiThread(() -> {
                            emergencyContactNameDisplay.setText("Not Updated");
                            emergencyContactNumberDisplay.setText("Not Updated");
                        });
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching user data: " + e.getMessage(), e);
                runOnUiThread(() -> Toast.makeText(this, "Error fetching user data", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void fetchProfileImage(String imageUrl) {
        new Thread(() -> {
            try {
                URL url = new URL(imageUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();

                InputStream input = connection.getInputStream();
                Bitmap bitmap = BitmapFactory.decodeStream(input);

                runOnUiThread(() -> profilePicture.setImageBitmap(bitmap));
            } catch (Exception e) {
                Log.e(TAG, "Error fetching profile image: " + e.getMessage(), e);
                runOnUiThread(() -> Toast.makeText(this, "Error fetching profile image", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}
