package com.example.myapplication;

import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;

public class TicketWebViewActivity extends AppCompatActivity {

    private WebView webView;

    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);

        String from = getIntent().getStringExtra("from");
        String to = getIntent().getStringExtra("to");
        String date = getIntent().getStringExtra("date");

        String url = "https://bdtickets.com/bus/search/" + from + "-to-" + to + "?journeyDate=" + date;

        webView = findViewById(R.id.webView);

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);              // ✅ Enable JS
        webSettings.setDomStorageEnabled(true);              // ✅ Enable DOM Storage

        webSettings.setLoadWithOverviewMode(true);           // Optional performance tweaks
        webSettings.setUseWideViewPort(true);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);  // Cache handling

        webView.setWebViewClient(new WebViewClient());        // Load inside app
        webView.loadUrl(url);                                 // Open the ticket search page
    }

}
