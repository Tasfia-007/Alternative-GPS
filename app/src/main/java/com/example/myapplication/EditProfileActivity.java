package com.example.myapplication;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.UUID;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class EditProfileActivity extends AppCompatActivity {
    private static final String TAG = "EditProfileActivity";
    private static final String SUPABASE_URL = "https://kquvuygavkhsxvdpqyfn.supabase.co";
    private static final String SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImtxdXZ1eWdhdmtoc3h2ZHBxeWZuIiwicm9sZSI6ImFub24iLCJpYXQiOjE3MzcxMDQ4NjcsImV4cCI6MjA1MjY4MDg2N30.YVPKExfM-ZxzO9JvM9RQZQrBiyG1iT50fiwGUcvw8EI";
    private static final int CAMERA_REQUEST_CODE = 101;
    private static final int GALLERY_REQUEST_CODE = 102;
    private ImageView profilePicture;
    private EditText usernameInput;
    private EditText emailInput;
    private EditText oldPasswordInput;
    private EditText newPasswordInput;
    private EditText emergencyContactNameInput;
    private EditText emergencyPhoneNumberInput;
    private Button saveButton;

    private String userId = "1"; // Replace with dynamic logged-in user ID
    private String originalUsername, originalEmail, originalPassword, originalContactName, originalContactNumber, profileImageUrl;




    private Uri imageUri;
    private String newImagePath;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);
//        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
//        userId = sharedPreferences.getString("userId", null);
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        userId = sharedPreferences.getString("userId", null);

        if (userId == null) {
            Toast.makeText(this, "User is not logged in", Toast.LENGTH_SHORT).show();
            finish(); // Close the activity as there's no user logged in
            return;
        }

        // Initialize views
        profilePicture = findViewById(R.id.profile_picture_edit);
        usernameInput = findViewById(R.id.username_input);
        emailInput = findViewById(R.id.email_input);

        newPasswordInput = findViewById(R.id.new_password_input);
        emergencyContactNameInput = findViewById(R.id.emergency_contact_name_input);
        emergencyPhoneNumberInput = findViewById(R.id.emergency_phone_number_input);
        saveButton = findViewById(R.id.save_button);

        // Fetch user data from Supabase and populate fields
        fetchUserData();

        // Save button click listener
        saveButton.setOnClickListener(v -> validateAndSaveChanges());

        // Set click listener for profile picture
        profilePicture.setOnClickListener(v -> showImagePickerDialog());

        // Set click listener for "Edit Profile Picture" text

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
                        originalUsername = userData.getString("username");
                        originalEmail = userData.getString("emailid");
                        originalPassword = userData.getString("password");
                        profileImageUrl = userData.optString("image", null);

                        runOnUiThread(() -> {
                            usernameInput.setText(originalUsername);
                            emailInput.setText(originalEmail);

                            // Fetch and display profile picture
                            if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                                fetchProfileImage(profileImageUrl);
                            }
                        });
                    }
                } else {
                    Log.e(TAG, "Failed to fetch user data: " + userResponse.message());
                    runOnUiThread(() -> Toast.makeText(this, "Failed to fetch user data", Toast.LENGTH_SHORT).show());
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
                        originalContactName = contactData.optString("contact_name", "");
                        originalContactNumber = contactData.optString("contact_number", "");

                        runOnUiThread(() -> {
                            emergencyContactNameInput.setText(originalContactName.isEmpty() ? "Not Updated" : originalContactName);
                            emergencyPhoneNumberInput.setText(originalContactNumber.isEmpty() ? "Not Updated" : originalContactNumber);
                        });
                    } else {
                        runOnUiThread(() -> {
                            emergencyContactNameInput.setHint("Not Updated");
                            emergencyPhoneNumberInput.setHint("Not Updated");
                        });
                    }
                } else {
                    Log.e(TAG, "Failed to fetch emergency contact data: " + contactResponse.message());
                    runOnUiThread(() -> Toast.makeText(this, "Failed to fetch emergency contact data", Toast.LENGTH_SHORT).show());
                }

            } catch (Exception e) {
                Log.e(TAG, "Error fetching user data: " + e.getMessage(), e);
                runOnUiThread(() -> Toast.makeText(this, "Error fetching user data", Toast.LENGTH_SHORT).show());
            }
        }).start();

        // Initialize password visibility toggle
        ImageView togglePasswordVisibility = findViewById(R.id.toggle_password_visibility);

        togglePasswordVisibility.setOnClickListener(v -> {
            if (newPasswordInput.getInputType() == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                // Show password
                newPasswordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                togglePasswordVisibility.setImageResource(R.drawable.eyeopen); // Use the "eye open" icon
            } else {
                // Hide password
                newPasswordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                togglePasswordVisibility.setImageResource(R.drawable.eye); // Use the "eye closed" icon
            }
            newPasswordInput.setSelection(newPasswordInput.getText().length()); // Set cursor at the end
        });
    }










//    private void fetchUserData() {
//        new Thread(() -> {
//            try {
//                OkHttpClient client = new OkHttpClient();
//
//                // Fetch user data from the signup table
//                String userUrl = SUPABASE_URL + "/rest/v1/signup?select=*&id=eq." + userId;
//                Request userRequest = new Request.Builder()
//                        .url(userUrl)
//                        .addHeader("apikey", SUPABASE_KEY)
//                        .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
//                        .get()
//                        .build();
//
//                Response userResponse = client.newCall(userRequest).execute();
//                if (userResponse.isSuccessful() && userResponse.body() != null) {
//                    String responseBody = userResponse.body().string();
//                    Log.d(TAG, "User data fetched successfully: " + responseBody);
//
//                    JSONArray userArray = new JSONArray(responseBody);
//                    if (userArray.length() > 0) {
//                        JSONObject userData = userArray.getJSONObject(0);
//                        originalUsername = userData.getString("username");
//                        originalEmail = userData.getString("emailid");
//                        originalPassword = userData.getString("password");
//                        profileImageUrl = userData.optString("image", null);
//
//                        runOnUiThread(() -> {
//                            usernameInput.setText(originalUsername);
//                            emailInput.setText(originalEmail);
//
//                            // Fetch and display profile picture
//                            if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
//                                fetchProfileImage(profileImageUrl);
//                            }
//                        });
//                    }
//                } else {
//                    Log.e(TAG, "Failed to fetch user data: " + userResponse.message());
//                    runOnUiThread(() -> Toast.makeText(this, "Failed to fetch user data", Toast.LENGTH_SHORT).show());
//                }
//
//                // Fetch emergency contact data from the emergency_contacts table
//                String contactUrl = SUPABASE_URL + "/rest/v1/emergency_contacts?select=*&user_id=eq." + userId;
//                Request contactRequest = new Request.Builder()
//                        .url(contactUrl)
//                        .addHeader("apikey", SUPABASE_KEY)
//                        .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
//                        .get()
//                        .build();
//
//                Response contactResponse = client.newCall(contactRequest).execute();
//                if (contactResponse.isSuccessful() && contactResponse.body() != null) {
//                    String responseBody = contactResponse.body().string();
//                    Log.d(TAG, "Emergency contact data fetched successfully: " + responseBody);
//
//                    JSONArray contactArray = new JSONArray(responseBody);
//                    if (contactArray.length() > 0) {
//                        JSONObject contactData = contactArray.getJSONObject(0);
//                        originalContactName = contactData.optString("contact_name", "");
//                        originalContactNumber = contactData.optString("contact_number", "");
//
//                        runOnUiThread(() -> {
//                            emergencyContactNameInput.setText(originalContactName.isEmpty() ? "Not Updated" : originalContactName);
//                            emergencyPhoneNumberInput.setText(originalContactNumber.isEmpty() ? "Not Updated" : originalContactNumber);
//                        });
//                    } else {
//                        runOnUiThread(() -> {
//                            emergencyContactNameInput.setHint("Not Updated");
//                            emergencyPhoneNumberInput.setHint("Not Updated");
//                        });
//                    }
//                } else {
//                    Log.e(TAG, "Failed to fetch emergency contact data: " + contactResponse.message());
//                    runOnUiThread(() -> Toast.makeText(this, "Failed to fetch emergency contact data", Toast.LENGTH_SHORT).show());
//                }
//
//            } catch (Exception e) {
//                Log.e(TAG, "Error fetching user data: " + e.getMessage(), e);
//                runOnUiThread(() -> Toast.makeText(this, "Error fetching user data", Toast.LENGTH_SHORT).show());
//            }
//        }).start();
//
//
//        // Initialize password visibility toggle
//        ImageView togglePasswordVisibility = findViewById(R.id.toggle_password_visibility);
//
//        togglePasswordVisibility.setOnClickListener(v -> {
//            if (newPasswordInput.getInputType() == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
//                // Show password
//                newPasswordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
//                togglePasswordVisibility.setImageResource(R.drawable.eyeopen); // Use the "eye open" icon
//            } else {
//                // Hide password
//                newPasswordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
//                togglePasswordVisibility.setImageResource(R.drawable.eye); // Use the "eye closed" icon
//            }
//            newPasswordInput.setSelection(newPasswordInput.getText().length()); // Set cursor at the end
//        });
//    }


    private void fetchProfileImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            Log.e(TAG, "Profile image URL is null or empty.");
            runOnUiThread(() -> Toast.makeText(this, "No profile image available.", Toast.LENGTH_SHORT).show());
            return;
        }

        new Thread(() -> {
            try {
                URL url = new URL(imageUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();

                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    throw new FileNotFoundException("Invalid response from server: " + connection.getResponseCode());
                }

                InputStream input = connection.getInputStream();
                Bitmap bitmap = BitmapFactory.decodeStream(input);

                runOnUiThread(() -> profilePicture.setImageBitmap(bitmap));
            } catch (FileNotFoundException e) {
                Log.e(TAG, "Profile image not found at URL: " + imageUrl, e);
                runOnUiThread(() -> Toast.makeText(this, "Profile image not found.", Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                Log.e(TAG, "Error fetching profile image: " + e.getMessage(), e);
                runOnUiThread(() -> Toast.makeText(this, "Error fetching profile image.", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }









    //edit start here ig=============== =====================




    private void validateAndSaveChanges() {
        new AlertDialog.Builder(this)
                .setTitle("Are you sure?")
                .setMessage("Do you want to update your profile?")
                .setPositiveButton("Yes", (dialog, which) -> showPasswordVerificationDialog())
                .setNegativeButton("No", null)
                .show();
    }



    private void showPasswordVerificationDialog() {
        EditText passwordInput = new EditText(this);
        passwordInput.setHint("Enter current password");
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        new AlertDialog.Builder(this)
                .setTitle("Verify Password")
                .setView(passwordInput)
                .setPositiveButton("Verify", (dialog, which) -> {
                    String enteredPassword = passwordInput.getText().toString().trim();
                    String hashedEnteredPassword = hashPassword(enteredPassword); // Hash the entered password
                    if (hashedEnteredPassword.equals(originalPassword)) { // Compare hashed values
                        saveChangesToDatabase(); // Proceed with saving changes
                    } else {
                        Toast.makeText(this, "Incorrect password", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Password verification failed. Entered password does not match.");
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }


//    private void saveChangesToDatabase() {
//        new Thread(() -> {
//            try {
//                OkHttpClient client = new OkHttpClient();
//
//                // Update profile picture if a new one is selected
//                if (!TextUtils.isEmpty(newImagePath)) {
//                    Log.d(TAG, "Uploading new profile picture...");
//                    uploadImageToSupabase(newImagePath);
//                } else {
//                    Log.d(TAG, "No new profile picture to upload.");
//                }
//
//                // Update password if it has been changed
//                String newPassword = newPasswordInput.getText().toString().trim();
//                if (!TextUtils.isEmpty(newPassword) && !newPassword.equals(originalPassword)) {
//                    if (newPassword.length() >= 6 && newPassword.matches(".*\\d.*")) {
//                        Log.d(TAG, "Updating password...");
//                        updatePasswordInDatabase(newPassword);
//                    } else {
//                        runOnUiThread(() -> {
//                            Toast.makeText(this, "Password must be 6+ characters and contain at least 1 digit", Toast.LENGTH_SHORT).show();
//                            Log.e(TAG, "Password validation failed.");
//                        });
//                        return;
//                    }
//                } else {
//                    Log.d(TAG, "No new password provided or password unchanged.");
//                }
//
//                // Update or insert emergency contacts
//                String contactName = emergencyContactNameInput.getText().toString().trim();
//                String contactNumber = emergencyPhoneNumberInput.getText().toString().trim();
//                if (!TextUtils.isEmpty(contactName) || !TextUtils.isEmpty(contactNumber)) {
//                    Log.d(TAG, "Updating emergency contact...");
//                    updateOrInsertEmergencyContact(contactName, contactNumber);
//                } else {
//                    Log.d(TAG, "No emergency contact details to update.");
//                }
//
//                runOnUiThread(() -> Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show());
//            } catch (Exception e) {
//                Log.e(TAG, "Error saving changes to database", e);
//                runOnUiThread(() -> Toast.makeText(this, "Failed to update profile. Please try again.", Toast.LENGTH_SHORT).show());
//            }
//        }).start();
//    }
//





//    private void saveChangesToDatabase() {
//        new Thread(() -> {
//            try {
//                OkHttpClient client = new OkHttpClient();
//
//                // Update profile picture if selected
//                if (!TextUtils.isEmpty(newImagePath)) {
//                    Log.d(TAG, "New profile picture detected. Uploading...");
//                    uploadImageToSupabase(newImagePath);
//                } else {
//                    Log.d(TAG, "No new profile picture selected.");
//                }
//
//                // Update password
//                String newPassword = newPasswordInput.getText().toString().trim();
//                if (!TextUtils.isEmpty(newPassword) && !newPassword.equals(originalPassword)) {
//                    if (newPassword.length() >= 6 && newPassword.matches(".*\\d.*")) {
//                        Log.d(TAG, "Updating password...");
//                        updatePasswordInDatabase(newPassword);
//                    } else {
//                        runOnUiThread(() -> {
//                            Toast.makeText(this, "Password must be 6+ characters and contain at least 1 digit.", Toast.LENGTH_SHORT).show();
//                        });
//                        return;
//                    }
//                } else {
//                    Log.d(TAG, "No password change detected.");
//                }
//
//                // Update emergency contacts
//                String contactName = emergencyContactNameInput.getText().toString().trim();
//                String contactNumber = emergencyPhoneNumberInput.getText().toString().trim();
//                if (!TextUtils.isEmpty(contactName) || !TextUtils.isEmpty(contactNumber)) {
//                    Log.d(TAG, "Updating or inserting emergency contact...");
//                    updateOrInsertEmergencyContact(contactName, contactNumber);
//                } else {
//                    Log.d(TAG, "No emergency contact updates detected.");
//                }
//
//                runOnUiThread(() -> Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show());
//            } catch (Exception e) {
//                Log.e(TAG, "Error saving changes to database: ", e);
//                runOnUiThread(() -> Toast.makeText(this, "Failed to update profile. Please try again.", Toast.LENGTH_SHORT).show());
//            }
//        }).start();
//    }
//

//    private void saveChangesToDatabase() {
//        new Thread(() -> {
//            try {
//                OkHttpClient client = new OkHttpClient();
//
//                // Update profile picture if a new one is selected
//                if (!TextUtils.isEmpty(newImagePath)) {
//                    Log.d(TAG, "Attempting to upload new profile picture...");
//                    uploadImageToSupabase(newImagePath);
//                } else {
//                    Log.d(TAG, "No new profile picture selected.");
//                }
//
//                // Update password if it has been changed
//                // Update password if it has been changed
//                String newPassword = newPasswordInput.getText().toString().trim();
//                if (!TextUtils.isEmpty(newPassword) && !newPassword.equals(originalPassword)) {
//                    if (newPassword.length() >= 6 && newPassword.matches(".*\\d.*")) {
//                        Log.d(TAG, "Updating password...");
//                        updatePasswordInDatabase(newPassword); // Call the method here
//                    } else {
//                        runOnUiThread(() -> {
//                            Toast.makeText(this, "Password must be 6+ characters and contain at least 1 digit", Toast.LENGTH_SHORT).show();
//                            Log.e(TAG, "Password validation failed.");
//                        });
//                        return;
//                    }
//                } else {
//                    Log.d(TAG, "No new password provided or password unchanged.");
//                }
//
//                // Update or insert emergency contacts
//                String contactName = emergencyContactNameInput.getText().toString().trim();
//                String contactNumber = emergencyPhoneNumberInput.getText().toString().trim();
//                if (!TextUtils.isEmpty(contactName) || !TextUtils.isEmpty(contactNumber)) {
//                    Log.d(TAG, "Attempting to update or insert emergency contact...");
//                    updateOrInsertEmergencyContact(contactName, contactNumber);
//                } else {
//                    Log.d(TAG, "No emergency contact details to update.");
//                }
//
//                runOnUiThread(() -> Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show());
//            } catch (Exception e) {
//                Log.e(TAG, "Error saving changes to database: " + e.getMessage(), e);
//                runOnUiThread(() -> Toast.makeText(this, "Failed to update profile. Please try again.", Toast.LENGTH_SHORT).show());
//            }
//        }).start();
//    }









    private void updatePasswordInDatabase(String newPassword) {
        new Thread(() -> {
            try {
                String hashedPassword = hashPassword(newPassword); // Hash the new password
                JSONObject json = new JSONObject();
                json.put("password", hashedPassword);

                Log.d(TAG, "Hashed new password: " + hashedPassword);

                RequestBody body = RequestBody.create(json.toString(), MediaType.parse("application/json"));
                String url = SUPABASE_URL + "/rest/v1/signup?id=eq." + userId;
                Log.d(TAG, "Password update URL: " + url);

                Request request = new Request.Builder()
                        .url(url)
                        .patch(body)
                        .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                        .addHeader("apikey", SUPABASE_KEY)
                        .build();

                Response response = new OkHttpClient().newCall(request).execute();

                Log.d(TAG, "Password update response: " + response.message());
                if (response.isSuccessful()) {
                    Log.d(TAG, "Password updated successfully.");
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Password updated successfully. Logging out...", Toast.LENGTH_SHORT).show();
                        logoutAndRedirectToLogin();
                    });
                } else {
                    Log.e(TAG, "Failed to update password: " + response.message());
                    runOnUiThread(() -> Toast.makeText(this, "Failed to update password.", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error updating password", e);
                runOnUiThread(() -> Toast.makeText(this, "Error updating password.", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }











//    private void updatePasswordInDatabase(String newPassword) {
//        new Thread(() -> {
//            try {
//                String hashedPassword = hashPassword(newPassword); // Hash the new password
//                JSONObject json = new JSONObject();
//                json.put("password", hashedPassword);
//
//                Log.d(TAG, "Hashed new password: " + hashedPassword);
//
//                RequestBody body = RequestBody.create(json.toString(), MediaType.parse("application/json"));
//                String url = SUPABASE_URL + "/rest/v1/signup?id=eq." + userId;
//                Log.d(TAG, "Password update URL: " + url);
//
//                Request request = new Request.Builder()
//                        .url(url)
//                        .patch(body)
//                        .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
//                        .addHeader("apikey", SUPABASE_KEY)
//                        .build();
//
//                Response response = new OkHttpClient().newCall(request).execute();
//
//                Log.d(TAG, "Password update response: " + response.message());
//                if (response.isSuccessful()) {
//                    Log.d(TAG, "Password updated successfully.");
//                    runOnUiThread(() -> Toast.makeText(this, "Password updated successfully!", Toast.LENGTH_SHORT).show());
//                } else {
//                    Log.e(TAG, "Failed to update password: " + response.message());
//                    runOnUiThread(() -> Toast.makeText(this, "Failed to update password.", Toast.LENGTH_SHORT).show());
//                }
//            } catch (Exception e) {
//                Log.e(TAG, "Error updating password", e);
//                runOnUiThread(() -> Toast.makeText(this, "Error updating password.", Toast.LENGTH_SHORT).show());
//            }
//        }).start();
//    }

    private void saveChangesToDatabase() {
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();
                boolean isPasswordUpdated = false;

                // Update profile picture if a new one is selected
                if (!TextUtils.isEmpty(newImagePath)) {
                    Log.d(TAG, "Attempting to upload new profile picture...");
                    uploadImageToSupabase(newImagePath);
                } else {
                    Log.d(TAG, "No new profile picture selected.");
                }

                // Update password if it has been changed
                String newPassword = newPasswordInput.getText().toString().trim();
                if (!TextUtils.isEmpty(newPassword) && !newPassword.equals(originalPassword)) {
                    if (newPassword.length() >= 6 && newPassword.matches(".*\\d.*")) {
                        Log.d(TAG, "Updating password...");
                        updatePasswordInDatabase(newPassword); // Call the method here
                        isPasswordUpdated = true; // Mark password as updated
                    } else {
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Password must be 6+ characters and contain at least 1 digit", Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Password validation failed.");
                        });
                        return;
                    }
                } else {
                    Log.d(TAG, "No new password provided or password unchanged.");
                }

                // Update or insert emergency contacts
                // Update or insert emergency contacts
                String contactName = emergencyContactNameInput.getText().toString().trim();
                String contactNumber = emergencyPhoneNumberInput.getText().toString().trim();

                if (!TextUtils.isEmpty(contactName) || !TextUtils.isEmpty(contactNumber)) {
                    Log.d(TAG, "Attempting to update or insert emergency contact...");
                    updateOrInsertEmergencyContact(contactName, contactNumber);
                } else {
                    Log.d(TAG, "No emergency contact details provided.");
                }


                // Determine redirection behavior based on whether the password was updated
                if (isPasswordUpdated) {
                    // Log out the user
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Password updated. Logging out...", Toast.LENGTH_SHORT).show();
                        logoutAndRedirectToLogin();
                    });
                } else {
                    // Redirect to ProfileActivity
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                        navigateToProfile();
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error saving changes to database: " + e.getMessage(), e);
                runOnUiThread(() -> Toast.makeText(this, "Failed to update profile. Please try again.", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }




    private void updateOrInsertEmergencyContact(String contactName, String contactNumber) {
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();

                // Check if emergency contact already exists
                String contactUrl = SUPABASE_URL + "/rest/v1/emergency_contacts?select=*&user_id=eq." + userId;
                Request checkRequest = new Request.Builder()
                        .url(contactUrl)
                        .addHeader("apikey", SUPABASE_KEY)
                        .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                        .get()
                        .build();

                Response checkResponse = client.newCall(checkRequest).execute();
                if (checkResponse.isSuccessful() && checkResponse.body() != null) {
                    String responseBody = checkResponse.body().string();
                    JSONArray array = new JSONArray(responseBody);

                    JSONObject json = new JSONObject();
                    json.put("user_id", userId);
                    json.put("contact_name", contactName);
                    json.put("contact_number", contactNumber);

                    RequestBody body = RequestBody.create(json.toString(), MediaType.parse("application/json"));
                    String url;
                    Request.Builder builder = new Request.Builder()
                            .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                            .addHeader("apikey", SUPABASE_KEY);

                    if (array.length() > 0) {
                        // If contact exists, update it
                        url = SUPABASE_URL + "/rest/v1/emergency_contacts?user_id=eq." + userId;
                        builder.url(url).patch(body);
                    } else {
                        // If no contact exists, insert new one
                        url = SUPABASE_URL + "/rest/v1/emergency_contacts";
                        builder.url(url).post(body);
                    }

                    Response response = client.newCall(builder.build()).execute();
                    if (response.isSuccessful()) {
                        Log.d(TAG, "Emergency contact successfully updated/inserted.");
                        runOnUiThread(() -> Toast.makeText(this, "Emergency contact saved!", Toast.LENGTH_SHORT).show());
                    } else {
                        Log.e(TAG, "Failed to update/insert emergency contact: " + response.message());
                        runOnUiThread(() -> Toast.makeText(this, "Failed to save emergency contact.", Toast.LENGTH_SHORT).show());
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "Error updating or inserting emergency contact", e);
                runOnUiThread(() -> Toast.makeText(this, "Error updating emergency contact.", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }








    private void navigateToProfile() {
        Intent intent = new Intent(EditProfileActivity.this, ProfileActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish(); // Close EditProfileActivity
    }

    private void logoutAndRedirectToLogin() {
        // Clear user session
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear(); // Clear all saved data
        editor.apply();

        // Redirect to LoginActivity
        Intent intent = new Intent(EditProfileActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish(); // Close EditProfileActivity
    }



//    private void saveChangesToDatabase() {
//        new Thread(() -> {
//            try {
//                OkHttpClient client = new OkHttpClient();
//
//                // Update profile picture if a new one is selected
//                if (!TextUtils.isEmpty(newImagePath)) {
//                    uploadImageToSupabase(newImagePath);
//                }
//
//                // Update password if it has been changed
//                String newPassword = newPasswordInput.getText().toString().trim();
//                if (!TextUtils.isEmpty(newPassword) && !newPassword.equals(originalPassword)) {
//                    if (newPassword.length() >= 6 && newPassword.matches(".*\\d.*")) {
//                        updatePasswordInDatabase(newPassword);
//                    } else {
//                        runOnUiThread(() -> Toast.makeText(this, "Password must be 6+ characters and contain at least 1 digit", Toast.LENGTH_SHORT).show());
//                        return;
//                    }
//                }
//
//                // Update or insert emergency contacts
//                String contactName = emergencyContactNameInput.getText().toString().trim();
//                String contactNumber = emergencyPhoneNumberInput.getText().toString().trim();
//                if (!TextUtils.isEmpty(contactName) || !TextUtils.isEmpty(contactNumber)) {
//                    updateOrInsertEmergencyContact(contactName, contactNumber);
//                }
//
//                runOnUiThread(() -> Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show());
//            } catch (Exception e) {
//                Log.e(TAG, "Error saving changes to database", e);
//            }
//        }).start();
//    }



    //forimage=============================== ================
    private void showImagePickerDialog() {
        String[] options = {"Take Photo", "Choose from Gallery"};
        new AlertDialog.Builder(this)
                .setTitle("Select Image")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        openCamera();
                    } else {
                        openGallery();
                    }
                })
                .show();
    }


    private void openCamera() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "New Picture");
        values.put(MediaStore.Images.Media.DESCRIPTION, "From the Camera");
        imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE);
    }

    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent, GALLERY_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == CAMERA_REQUEST_CODE) {
                profilePicture.setImageURI(imageUri);
                newImagePath = saveImageLocally(imageUri);
            } else if (requestCode == GALLERY_REQUEST_CODE && data != null) {
                imageUri = data.getData();
                profilePicture.setImageURI(imageUri);
                newImagePath = saveImageLocally(imageUri);
            }
        }
    }





    private String saveImageLocally(Uri uri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
            File file = new File(getFilesDir(), UUID.randomUUID().toString() + ".jpg");
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out);
            out.close();
            return file.getAbsolutePath();
        } catch (Exception e) {
            Log.e(TAG, "Error saving image locally", e);
            return null;
        }
    }



//    private void uploadImageToSupabase(String imagePath) {
//        new Thread(() -> {
//            try {
//                File file = new File(imagePath);
//                RequestBody fileBody = RequestBody.create(file, MediaType.parse("image/jpeg"));
//                String fileName = UUID.randomUUID().toString() + ".jpg";
//
//                RequestBody requestBody = new MultipartBody.Builder()
//                        .setType(MultipartBody.FORM)
//                        .addFormDataPart("file", fileName, fileBody)
//                        .build();
//
//                String bucketUrl = SUPABASE_URL + "/storage/v1/object/user-images/" + fileName;
//                Request request = new Request.Builder()
//                        .url(bucketUrl)
//                        .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
//                        .post(requestBody)
//                        .build();
//
//                Response response = new OkHttpClient().newCall(request).execute();
//                if (response.isSuccessful()) {
//                    String newImageUrl = SUPABASE_URL + "/storage/v1/object/public/user-images/" + fileName;
//                    Log.d(TAG, "Image uploaded successfully: " + newImageUrl);
//                    updateImageUrlInDatabase(newImageUrl);
//                    runOnUiThread(() -> Toast.makeText(this, "Profile picture updated successfully!", Toast.LENGTH_SHORT).show());
//                } else {
//                    Log.e(TAG, "Image upload failed: " + response.message());
//                    runOnUiThread(() -> Toast.makeText(this, "Failed to upload image. Please try again.", Toast.LENGTH_SHORT).show());
//                }
//            } catch (Exception e) {
//                Log.e(TAG, "Error uploading image: " + e.getMessage(), e);
//                runOnUiThread(() -> Toast.makeText(this, "An error occurred while uploading the image.", Toast.LENGTH_SHORT).show());
//            }
//        }).start();
//    }
//


//    private void updatePasswordInDatabase(String newPassword) {
//        new Thread(() -> {
//            try {
//                // Prepare the JSON payload
//                JSONObject json = new JSONObject();
//                json.put("password", hashPassword(newPassword)); // Ensure password is hashed
//
//                // Log the payload
//                Log.d(TAG, "Password update payload: " + json.toString());
//
//                // Prepare the request URL
//                String url = SUPABASE_URL + "/rest/v1/signup?id=eq." + userId;
//                Log.d(TAG, "Password update URL: " + url);
//
//                // Create the request body
//                RequestBody body = RequestBody.create(json.toString(), MediaType.parse("application/json"));
//
//                // Build the request
//                Request request = new Request.Builder()
//                        .url(url)
//                        .patch(body)
//                        .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
//                        .build();
//
//                // Execute the request and log the response
//                Response response = new OkHttpClient().newCall(request).execute();
//                Log.d(TAG, "Password update response: " + response.message());
//            } catch (Exception e) {
//                Log.e(TAG, "Error updating password", e);
//            }
//        }).start();
//    }



    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = digest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashedBytes) {
                hexString.append(String.format("%02x", b));
            }
            Log.d(TAG, "Password hashed successfully.");
            return hexString.toString();
        } catch (Exception e) {
            Log.e(TAG, "Error hashing password: " + e.getMessage(), e);
            return null;
        }
    }




//    private String hashPassword(String password) {
//        try {
//            MessageDigest digest = MessageDigest.getInstance("SHA-256");
//            byte[] hashedBytes = digest.digest(password.getBytes());
//            StringBuilder hexString = new StringBuilder();
//            for (byte b : hashedBytes) {
//                hexString.append(String.format("%02x", b));
//            }
//            return hexString.toString();
//        } catch (Exception e) {
//            Log.e(TAG, "Error hashing password", e);
//            return null;
//        }
//    }


//    private void updateOrInsertEmergencyContact(String contactName, String contactNumber) {
//        new Thread(() -> {
//            try {
//                OkHttpClient client = new OkHttpClient();
//
//                // Check if the contact exists
//                String contactUrl = SUPABASE_URL + "/rest/v1/emergency_contacts?select=*&user_id=eq." + userId;
//                Log.d(TAG, "Emergency contact API URL: " + contactUrl);
//
//                Request checkRequest = new Request.Builder()
//                        .url(contactUrl)
//                        .addHeader("apikey", SUPABASE_KEY)
//                        .get()
//                        .build();
//
//                Response checkResponse = client.newCall(checkRequest).execute();
//                if (checkResponse.isSuccessful() && checkResponse.body() != null) {
//                    String responseBody = checkResponse.body().string();
//                    JSONArray array = new JSONArray(responseBody);
//
//                    JSONObject json = new JSONObject();
//                    json.put("contact_name", contactName);
//                    json.put("contact_number", contactNumber);
//
//                    // Log the payload
//                    Log.d(TAG, "Emergency contact payload: " + json.toString());
//
//                    RequestBody body = RequestBody.create(json.toString(), MediaType.parse("application/json"));
//                    String method = array.length() > 0 ? "PATCH" : "POST";
//                    String url = method.equals("PATCH") ?
//                            SUPABASE_URL + "/rest/v1/emergency_contacts?user_id=eq." + userId :
//                            SUPABASE_URL + "/rest/v1/emergency_contacts";
//
//                    Request.Builder builder = new Request.Builder()
//                            .url(url)
//                            .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
//                            .addHeader("apikey", SUPABASE_KEY);
//
//                    if (method.equals("PATCH")) {
//                        builder.patch(body);
//                    } else {
//                        builder.post(body);
//                    }
//
//                    Response response = client.newCall(builder.build()).execute();
//
//                    // Log detailed response information
//                    Log.d(TAG, "Emergency contact response code: " + response.code());
//                    Log.d(TAG, "Emergency contact response body: " + (response.body() != null ? response.body().string() : "null"));
//
//                    if (response.isSuccessful()) {
//                        Log.d(TAG, "Emergency contact successfully updated/inserted.");
//                        runOnUiThread(() -> Toast.makeText(this, "Emergency contact updated successfully!", Toast.LENGTH_SHORT).show());
//                    } else {
//                        Log.e(TAG, "Failed to update/insert emergency contact: " + response.message());
//                        runOnUiThread(() -> Toast.makeText(this, "Failed to update emergency contact.", Toast.LENGTH_SHORT).show());
//                    }
//                } else {
//                    Log.e(TAG, "Failed to fetch existing emergency contact: " + checkResponse.message());
//                }
//            } catch (Exception e) {
//                Log.e(TAG, "Error updating or inserting emergency contact", e);
//                runOnUiThread(() -> Toast.makeText(this, "Error updating emergency contact.", Toast.LENGTH_SHORT).show());
//            }
//        }).start();
//    }


    private void updateImageUrlInDatabase(String imageUrl) {
        new Thread(() -> {
            try {
                // Prepare the JSON payload
                JSONObject json = new JSONObject();
                json.put("image", imageUrl);

                // Log the payload
                Log.d(TAG, "Image URL update payload: " + json.toString());

                // Prepare the request URL
                String url = SUPABASE_URL + "/rest/v1/signup?id=eq." + userId;

                // Create the request body
                RequestBody body = RequestBody.create(json.toString(), MediaType.parse("application/json"));

                // Build the request
                Request request = new Request.Builder()
                        .url(url)
                        .patch(body)
                        .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                        .addHeader("apikey", SUPABASE_KEY)
                        .build();

                // Execute the request and log the response
                Response response = new OkHttpClient().newCall(request).execute();
                Log.d(TAG, "Image URL update response: " + response.message());

                if (response.isSuccessful()) {
                    Log.d(TAG, "Image URL updated in database successfully.");
                    runOnUiThread(() -> Toast.makeText(this, "Profile picture updated in database!", Toast.LENGTH_SHORT).show());
                } else {
                    Log.e(TAG, "Failed to update image URL in database: " + response.message());
                    runOnUiThread(() -> Toast.makeText(this, "Failed to update profile picture.", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error updating image URL in database: ", e);
                runOnUiThread(() -> Toast.makeText(this, "Error updating profile picture in database.", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }




    private void uploadImageToSupabase(String imagePath) {
        new Thread(() -> {
            try {
                File file = new File(imagePath);
                RequestBody fileBody = RequestBody.create(file, MediaType.parse("image/jpeg"));
                String fileName = UUID.randomUUID().toString() + ".jpg";

                RequestBody requestBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("file", fileName, fileBody)
                        .build();

                String bucketUrl = SUPABASE_URL + "/storage/v1/object/user-images/" + fileName;
                Request request = new Request.Builder()
                        .url(bucketUrl)
                        .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                        .post(requestBody)
                        .build();

                Response response = new OkHttpClient().newCall(request).execute();
                if (response.isSuccessful()) {
                    String newImageUrl = SUPABASE_URL + "/storage/v1/object/public/user-images/" + fileName;
                    Log.d(TAG, "Image uploaded successfully: " + newImageUrl);
                    runOnUiThread(() -> Toast.makeText(this, "Image uploaded successfully!", Toast.LENGTH_SHORT).show());

                    // Update the image URL in the database
                    updateImageUrlInDatabase(newImageUrl);
                } else {
                    Log.e(TAG, "Image upload failed: " + response.message());
                    runOnUiThread(() -> Toast.makeText(this, "Failed to upload image.", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error uploading image: ", e);
                runOnUiThread(() -> Toast.makeText(this, "An error occurred during image upload.", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }





//    private void updateImageUrlInDatabase(String imageUrl) {
//        new Thread(() -> {
//            try {
//                JSONObject json = new JSONObject();
//                json.put("image", imageUrl);
//
//                RequestBody body = RequestBody.create(json.toString(), MediaType.parse("application/json"));
//                String url = SUPABASE_URL + "/rest/v1/signup?id=eq." + userId;
//                Request request = new Request.Builder()
//                        .url(url)
//                        .patch(body)
//                        .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
//                        .build();
//
//                new OkHttpClient().newCall(request).execute();
//            } catch (Exception e) {
//                Log.e(TAG, "Error updating image URL", e);
//            }
//        }).start();
//    }
//
//
//
//
//
//
//





//private void saveChanges() {
//        // Implement save logic here
//    }
}
