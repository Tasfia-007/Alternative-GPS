package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import androidx.appcompat.app.AppCompatActivity;

public class TravelActivity extends AppCompatActivity {

    private WebView webView;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_travel);

        // Initialize views
        Spinner websiteSpinner = findViewById(R.id.websiteSpinner);
        Button searchButton = findViewById(R.id.searchButton);
        webView = findViewById(R.id.webView);
        progressBar = findViewById(R.id.progressBar);

        // Set up Spinner with website options
        String[] websites = {"Select Website", "Shohoz", "Bangladesh Railway", "Bangladesh Biman"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, websites);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        websiteSpinner.setAdapter(adapter);

        // Configure WebView
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
                webView.setVisibility(View.VISIBLE);
            }
        });

        // Handle Search Button click
        searchButton.setOnClickListener(v -> {
            String selectedWebsite = websiteSpinner.getSelectedItem().toString();
            webView.setVisibility(View.GONE); // Hide WebView until page loads
            switch (selectedWebsite) {
                case "Shohoz":
                    webView.loadUrl("https://www.shohoz.com");
                    break;
                case "Bangladesh Railway":
                    webView.loadUrl("https://bangladesh-railway.com/"); // Placeholder URL
                    break;
                case "Bangladesh Biman":
                    webView.loadUrl("https://www.biman-airlines.com/"); // Placeholder URL
                    break;
                default:
                    webView.setVisibility(View.GONE);
                    break;
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}