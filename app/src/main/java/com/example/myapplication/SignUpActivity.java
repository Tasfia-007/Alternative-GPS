package com.example.myapplication;

import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import android.Manifest;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.File;
import java.util.UUID;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

public class SignUpActivity extends AppCompatActivity {
    private static final int CAMERA_REQUEST_CODE = 101;
    private static final int GALLERY_REQUEST_CODE = 102;
    private static final int PERMISSION_REQUEST_CODE = 103;
    private ImageView appLogo;
    private Uri imageUri;
    private String imagePath; // Path
    private EditText nameInput, emailInput, passwordInput;
    private Button signUpButton;

    // Supabase configurations
    private static final String SUPABASE_URL = "https://kquvuygavkhsxvdpqyfn.supabase.co"; // Replace with your Supabase URL
    private static final String SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImtxdXZ1eWdhdmtoc3h2ZHBxeWZuIiwicm9sZSI6ImFub24iLCJpYXQiOjE3MzcxMDQ4NjcsImV4cCI6MjA1MjY4MDg2N30.YVPKExfM-ZxzO9JvM9RQZQrBiyG1iT50fiwGUcvw8EI"; // Replace with your Supabase API Key
    private static final String SIGNUP_TABLE = "signup";

    private static final String TAG = "SignUpActivity"; // For logging

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        // Initialize UI elements
        nameInput = findViewById(R.id.name_input);
        emailInput = findViewById(R.id.signup_email_input);
        passwordInput = findViewById(R.id.signup_password_input);
        signUpButton = findViewById(R.id.signup_button);
        appLogo = findViewById(R.id.app_logo);

        // Set up button listener
        signUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String name = nameInput.getText().toString().trim();
                String email = emailInput.getText().toString().trim();
                String password = passwordInput.getText().toString().trim();

                if (validateInput(name, email, password)) {
                    String hashedPassword = hashPassword(password);
                    if (hashedPassword != null) {
                        if (imagePath != null) {
                            uploadImageToSupabase(name, email, hashedPassword);
                        } else {
                            saveToSupabase(name, email, hashedPassword, null);
                        }
                    } else {
                        Log.e(TAG, "Password hashing failed");
                        Toast.makeText(SignUpActivity.this, "Error hashing password", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        // Image picker click listener
        appLogo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkPermissions()) {
                    showImagePickerDialog();
                } else {
                    requestPermissions();
                }
            }
        });
    }


    private boolean checkPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }



    private void showImagePickerDialog() {
        // Open a dialog to choose between Camera and Gallery
        String[] options = {"Take Photo", "Choose from Gallery"};
        new androidx.appcompat.app.AlertDialog.Builder(this)
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

    private void requestPermissions() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                PERMISSION_REQUEST_CODE
        );
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showImagePickerDialog(); // Retry image selection
            } else {
                Toast.makeText(this, "Permissions denied. Cannot open camera or gallery.", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void openCamera() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "New Picture");
        values.put(MediaStore.Images.Media.DESCRIPTION, "From the Camera");
        imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);

        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE);
        } else {
            Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show();
        }
    }


    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent, GALLERY_REQUEST_CODE);
    }

    private boolean validateInput(String name, String email, String password) {
        if (TextUtils.isEmpty(name)) {
            Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Validation failed: Name is empty");
            return false;
        }
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Email is required", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Validation failed: Email is empty");
            return false;
        }
        if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Password is required", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Validation failed: Password is empty");
            return false;
        }
        if (password.length() < 6 || !password.matches(".*\\d.*")) {
            Toast.makeText(this, "Password must be at least 6 characters long and contain a digit", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Validation failed: Password too short or missing digit");
            return false;
        }
        return true;
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = digest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashedBytes) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "Error hashing password: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private void saveToSupabase(String name, String email, String password, String imageUrl) {
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();

                // JSON payload
                JSONObject json = new JSONObject();
                json.put("username", name);
                json.put("emailid", email);
                json.put("password", password);

                // Add image URL to payload if available
                if (imageUrl != null) {
                    json.put("image", imageUrl);
                } else {
                    Log.d(TAG, "No image URL provided. Image will be null in the database.");
                }

                // Create request body
                RequestBody requestBody = RequestBody.create(json.toString(), MediaType.parse("application/json"));

                // Build request
                Request request = new Request.Builder()
                        .url(SUPABASE_URL + "/rest/v1/" + SIGNUP_TABLE)
                        .addHeader("apikey", SUPABASE_KEY)
                        .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                        .post(requestBody)
                        .build();

                Response response = client.newCall(request).execute();

                if (response.isSuccessful()) {
                    String responseBody = response.body() != null ? response.body().string() : "No Response Body";
                    Log.d(TAG, "Signup Success: " + responseBody);
                    runOnUiThread(() -> Toast.makeText(SignUpActivity.this, "Sign up successful!", Toast.LENGTH_SHORT).show());
                } else {
                    String errorMessage = response.body() != null ? response.body().string() : "Unknown Error";
                    Log.e(TAG, "Signup Failed: " + errorMessage);
                    runOnUiThread(() -> Toast.makeText(SignUpActivity.this, "Signup failed: " + errorMessage, Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                Log.e(TAG, "Signup request failed: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(SignUpActivity.this, "Signup failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                e.printStackTrace();
            }
        }).start();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == CAMERA_REQUEST_CODE && imageUri != null) {
                appLogo.setImageURI(imageUri); // Display the selected image
                imagePath = saveImageLocally(imageUri); // Save the image locally
            } else if (requestCode == GALLERY_REQUEST_CODE && data != null && data.getData() != null) {
                imageUri = data.getData(); // Get image URI from gallery
                appLogo.setImageURI(imageUri); // Display the selected image
                imagePath = saveImageLocally(imageUri); // Save the image locally
            } else {
                Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private String saveImageLocally(Uri uri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
            File file = new File(getFilesDir(), UUID.randomUUID().toString() + ".jpg");
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
            return file.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save image locally", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    private void uploadImageToSupabase(String name, String email, String hashedPassword) {
        if (imagePath != null) {
            Log.d(TAG, "Image Path: " + imagePath); // Log the image path before starting the upload thread

            new Thread(() -> {
                try {


                    OkHttpClient client = new OkHttpClient.Builder()
                            .connectTimeout(20, java.util.concurrent.TimeUnit.SECONDS) // Increase connection timeout
                            .writeTimeout(20, java.util.concurrent.TimeUnit.SECONDS)   // Increase write timeout
                            .readTimeout(20, java.util.concurrent.TimeUnit.SECONDS)    // Increase read timeout
                            .build();
                    File imageFile = new File(imagePath);
                    imageFile = compressImage(imageFile); // Compress the image

                    RequestBody fileBody = RequestBody.create(imageFile, MediaType.parse("image/jpeg"));
                    String uniqueFileName = UUID.randomUUID().toString() + ".jpg";
                    RequestBody requestBody = new MultipartBody.Builder()
                            .setType(MultipartBody.FORM)
                            .addFormDataPart("file", uniqueFileName, fileBody)
                            .build();

                    String bucketName = "user-images"; // Replace with your bucket name
                    String uploadUrl = SUPABASE_URL + "/storage/v1/object/" + bucketName + "/" + uniqueFileName;
                    Log.d(TAG, "Upload URL: " + uploadUrl); // Log the upload URL

                    Request request = new Request.Builder()
                            .url(uploadUrl)
                            .addHeader("Authorization", "Bearer " + SUPABASE_KEY) // Use correct API key
                            .post(requestBody)
                            .build();

                    Response response = client.newCall(request).execute();

                    if (response.isSuccessful()) {
                        String imageUrl = SUPABASE_URL + "/storage/v1/object/public/" + bucketName + "/" + uniqueFileName;
                        Log.d(TAG, "Image Upload Success: " + imageUrl);

                        // Save user details with the uploaded image URL
                        saveToSupabase(name, email, hashedPassword, imageUrl);

                        runOnUiThread(() -> Toast.makeText(SignUpActivity.this, "Image uploaded successfully!", Toast.LENGTH_SHORT).show());
                    } else {
                        String errorMessage = response.body() != null ? response.body().string() : "Unknown Error";
                        Log.e(TAG, "Image Upload Response: " + errorMessage); // Log response if upload fails
                        runOnUiThread(() -> Toast.makeText(SignUpActivity.this, "Image upload failed: " + errorMessage, Toast.LENGTH_SHORT).show());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error uploading image: " + e.getMessage());
                    runOnUiThread(() -> Toast.makeText(SignUpActivity.this, "Error uploading image: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    e.printStackTrace();
                }
            }).start();
        } else {
            Log.e(TAG, "Image path is null. Saving user data without image URL.");
            saveToSupabase(name, email, hashedPassword, null); // Save user without image
        }
    }



    private File compressImage(File originalFile) {
        try {
            Bitmap bitmap = BitmapFactory.decodeFile(originalFile.getAbsolutePath());
            File compressedFile = new File(getFilesDir(), UUID.randomUUID().toString() + ".jpg");

            FileOutputStream out = new FileOutputStream(compressedFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out); // Adjust quality (80%) to reduce size
            out.close();

            return compressedFile;
        } catch (IOException e) {
            e.printStackTrace();
            return originalFile; // Return original if compression fails
        }
    }




//    private void retryUpload(File imageFile, int retries) {
//        if (retries <= 0) {
//            Log.e(TAG, "Upload failed after retries.");
//            runOnUiThread(() -> Toast.makeText(this, "Image upload failed after retries.", Toast.LENGTH_SHORT).show());
//            return;
//        }
//
//        new Thread(() -> {
//            try {
//                OkHttpClient client = new OkHttpClient.Builder()
//                        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
//                        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
//                        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
//                        .build();
//
//                RequestBody fileBody = RequestBody.create(imageFile, MediaType.parse("image/jpeg"));
//                String uniqueFileName = UUID.randomUUID().toString() + ".jpg";
//                RequestBody requestBody = new MultipartBody.Builder()
//                        .setType(MultipartBody.FORM)
//                        .addFormDataPart("file", uniqueFileName, fileBody)
//                        .build();
//
//                String bucketName = "user-images";
//                String uploadUrl = SUPABASE_URL + "/storage/v1/object/" + bucketName + "/" + uniqueFileName;
//
//                Request request = new Request.Builder()
//                        .url(uploadUrl)
//                        .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
//                        .post(requestBody)
//                        .build();
//
//                Response response = client.newCall(request).execute();
//
//                if (response.isSuccessful()) {
//                    String imageUrl = SUPABASE_URL + "/storage/v1/object/public/" + bucketName + "/" + uniqueFileName;
//                    Log.d(TAG, "Image Upload Success: " + imageUrl);
//                    saveToSupabase(nameInput.getText().toString().trim(),
//                            emailInput.getText().toString().trim(),
//                            hashPassword(passwordInput.getText().toString().trim()),
//                            imageUrl);
//                    runOnUiThread(() -> Toast.makeText(SignUpActivity.this, "Image uploaded successfully!", Toast.LENGTH_SHORT).show());
//                } else {
//                    Log.e(TAG, "Upload failed: " + response.message());
//                    retryUpload(imageFile, retries - 1); // Retry on failure
//                }
//            } catch (Exception e) {
//                Log.e(TAG, "Error during upload: " + e.getMessage());
//                retryUpload(imageFile, retries - 1); // Retry on exception
//            }
//        }).start();
//    }




}




