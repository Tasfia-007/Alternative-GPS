package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private static final String SUPABASE_URL = "https://kquvuygavkhsxvdpqyfn.supabase.co"; // Replace with your Supabase URL
    private static final String SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImtxdXZ1eWdhdmtoc3h2ZHBxeWZuIiwicm9sZSI6ImFub24iLCJpYXQiOjE3MzcxMDQ4NjcsImV4cCI6MjA1MjY4MDg2N30.YVPKExfM-ZxzO9JvM9RQZQrBiyG1iT50fiwGUcvw8EI"; // Replace with your Supabase API Key
    private static final String SIGNUP_TABLE = "signup";

    private EditText emailInput, passwordInput;
    private Button loginButton;
    private TextView signupRedirect;

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize UI components
        emailInput = findViewById(R.id.email_input);
        passwordInput = findViewById(R.id.password_input);
        loginButton = findViewById(R.id.login_button);
        signupRedirect = findViewById(R.id.signup_redirect);

        // Initialize shared preferences
        sharedPreferences = getSharedPreferences("UserSession", Context.MODE_PRIVATE);

        // Check if user is already logged in
        if (isLoggedIn()) {
            navigateToHome(); // Skip login if already logged in
        }

        // Login button action
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailInput.getText().toString().trim();
                String password = passwordInput.getText().toString().trim();

                if (validateInput(email, password)) {
                    loginUser(email, password);
                }
            }
        });

        // Redirect to Signup
        signupRedirect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
                startActivity(intent);
            }
        });
    }

    private boolean validateInput(String email, String password) {
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Email is required", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Password is required", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

//    private void loginUser(String email, String password) {
//        new Thread(() -> {
//            try {
//                OkHttpClient client = new OkHttpClient();
//
//                String url = SUPABASE_URL + "/rest/v1/" + SIGNUP_TABLE + "?emailid=eq." + email;
//                Log.d(TAG, "Login URL: " + url);
//
//                Request request = new Request.Builder()
//                        .url(url)
//                        .addHeader("apikey", SUPABASE_KEY)
//                        .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
//                        .addHeader("Content-Type", "application/json")
//                        .get()
//                        .build();
//
//                Response response = client.newCall(request).execute();
//
//                if (response.isSuccessful()) {
//                    String responseBody = response.body() != null ? response.body().string() : "";
//                    JSONArray jsonArray = new JSONArray(responseBody);
//
//                    if (jsonArray.length() > 0) {
//                        JSONObject user = jsonArray.getJSONObject(0);
//                        String storedPassword = user.getString("password");
//
//                        // Check password
//                        if (hashPassword(password).equals(storedPassword)) {
//                            // Save user session
//                            saveUserSession(user.getString("username"), email, user.getString("id"));
//
//                            runOnUiThread(() -> {
//                                Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
//                                navigateToHome();
//                            });
//                        } else {
//                            runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Invalid password", Toast.LENGTH_SHORT).show());
//                        }
//                    } else {
//                        runOnUiThread(() -> Toast.makeText(LoginActivity.this, "User not found", Toast.LENGTH_SHORT).show());
//                    }
//                } else {
//                    Log.e(TAG, "Login Failed: " + response.message());
//                    runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Login failed: " + response.message(), Toast.LENGTH_SHORT).show());
//                }
//            } catch (Exception e) {
//                Log.e(TAG, "Error during login: " + e.getMessage());
//                runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Error during login: " + e.getMessage(), Toast.LENGTH_SHORT).show());
//            }
//        }).start();
//    }
//

    private void loginUser(String email, String password) {
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();

                String url = SUPABASE_URL + "/rest/v1/" + SIGNUP_TABLE + "?emailid=eq." + email;
                Log.d(TAG, "Login URL: " + url);

                Request request = new Request.Builder()
                        .url(url)
                        .addHeader("apikey", SUPABASE_KEY)
                        .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                        .addHeader("Content-Type", "application/json")
                        .get()
                        .build();

                Response response = client.newCall(request).execute();

                if (response.isSuccessful()) {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    JSONArray jsonArray = new JSONArray(responseBody);

                    if (jsonArray.length() > 0) {
                        JSONObject user = jsonArray.getJSONObject(0);
                        String storedPassword = user.getString("password");
                        String userId = user.getString("id"); // Ensure you get the user's ID from the response

                        // Check password
                        if (hashPassword(password).equals(storedPassword)) {
                            // Save user session, including userId
                            saveUserSession(userId, user.getString("username"), email);
                            runOnUiThread(() -> {
                                Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                                navigateToHome();
                            });
                        } else {
                            runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Invalid password", Toast.LENGTH_SHORT).show());
                        }
                    } else {
                        runOnUiThread(() -> Toast.makeText(LoginActivity.this, "User not found", Toast.LENGTH_SHORT).show());
                    }
                } else {
                    Log.e(TAG, "Login Failed: " + response.message());
                    runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Login failed: " + response.message(), Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error during login: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Error during login: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
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
            e.printStackTrace();
            return null;
        }
    }



//    private void saveUserSession(String username, String email, String userId) {
//        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
//        SharedPreferences.Editor editor = sharedPreferences.edit();
//        editor.putBoolean("isLoggedIn", true);
//        editor.putString("username", username);
//        editor.putString("email", email);
//        editor.putString("userId", userId); // Save the user ID
//        editor.apply();
//    }


    private void saveUserSession(String userId, String username, String email) {
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("isLoggedIn", true);
        editor.putString("userId", userId); // Save the userId
        editor.putString("username", username);
        editor.putString("email", email);
        editor.apply();
    }




    private boolean isLoggedIn() {
        return sharedPreferences.getBoolean("isLoggedIn", false);
    }

    private void navigateToHome() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class); // Replace with your Home Activity
        startActivity(intent);
        finish();
    }
}
