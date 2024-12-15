package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class LoginActivity extends AppCompatActivity {

    private EditText emailInput, passwordInput;
    private Button loginButton, googleLoginButton;
    private TextView signupRedirect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Views
        emailInput = findViewById(R.id.email_input);
        passwordInput = findViewById(R.id.password_input);
        loginButton = findViewById(R.id.login_button);
        googleLoginButton = findViewById(R.id.google_login_button);
        signupRedirect = findViewById(R.id.signup_redirect);

        // Handle Login Button Click
        loginButton.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
            } else {
                // Perform login action (e.g., authenticate with API)
                Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show();
                finish(); // Close the login activity
            }
        });

        // Handle Google Login Button Click
        googleLoginButton.setOnClickListener(v -> {
            // Perform Google Login Action (integrate Google Sign-In API)
            Toast.makeText(this, "Google Login Clicked", Toast.LENGTH_SHORT).show();
        });

        // Redirect to Signup Page
        signupRedirect.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
            startActivity(intent);
        });
    }
}
