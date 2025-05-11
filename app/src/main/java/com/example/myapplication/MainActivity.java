package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.PopupMenu;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.myapplication.AlertActivity;

import com.google.android.material.navigation.NavigationView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private SharedPreferences sharedPreferences;
    private static final String TAG = "MainActivity";
    private static final int LOCATION_PERMISSION_REQUEST = 1;
    private DrawerLayout drawerLayout;
    private ImageView profileIcon;
    private TextView trafficSummary;
    private MapView mapView;
    private MyLocationNewOverlay locationOverlay;
    private SearchView searchView;
    private static final int SPEECH_REQUEST_CODE = 100;
    private static final int AUDIO_PERMISSION_REQUEST = 2;
    private static final OkHttpClient client = new OkHttpClient();

    JSONArray weatherData = fetchHourlyWeatherData();


    private static final String API_KEY = "PV5RTCQMTHUNRL9MCH9DKDQ32";

    private static final String SUPABASE_URL = "https://kquvuygavkhsxvdpqyfn.supabase.co"; // Replace with your Supabase URL
    private static final String SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImtxdXZ1eWdhdmtoc3h2ZHBxeWZuIiwicm9sZSI6ImFub24iLCJpYXQiOjE3MzcxMDQ4NjcsImV4cCI6MjA1MjY4MDg2N30.YVPKExfM-ZxzO9JvM9RQZQrBiyG1iT50fiwGUcvw8EI";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Configuration.getInstance().setUserAgentValue("com.example.myapplication/1.0");
        sharedPreferences = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        // Initialize Views
        drawerLayout = findViewById(R.id.drawer_layout);
        ImageView menuIcon = findViewById(R.id.menu_icon);
        profileIcon = findViewById(R.id.profile_icon);

        searchView = findViewById(R.id.search_view);
        ImageButton directionButton = findViewById(R.id.direction_button);
        directionButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RoutingActivity.class);
            startActivity(intent);
        });
        ImageButton voiceSearchButton = findViewById(R.id.voice_search_button);
        voiceSearchButton.setOnClickListener(v -> {
            if (checkAndRequestAudioPermissions()) {
                startVoiceSearch();
            }
        });
//        EditText searchPlate = searchView.findViewById(androidx.appcompat.R.id.search_src_text);
//        searchPlate.setHintTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
//        searchPlate.setTextColor(ContextCompat.getColor(this, android.R.color.black));

        // Initialize MapView
        mapView = findViewById(R.id.mapview);
        mapView.setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);

        // Set initial zoom and position on the map (Dhaka city coordinates)
        IMapController mapController = mapView.getController();
        mapController.setZoom(11);
        mapController.setCenter(new GeoPoint(23.8103, 90.4125)); // Center at Dhaka City

        // Check location permissions and services before enabling MyLocationOverlay
        if (checkAndRequestLocationPermissions()) {
            if (isLocationEnabled()) {
                setupLocationOverlay(); // Enable location overlay
            } else {
                Toast.makeText(this, "Please enable location services in your device settings.", Toast.LENGTH_LONG).show();
                Log.e(TAG, "Location services are disabled.");
            }
        }

        setupSearchView();
        // Fetch Points of Interest (POIs) in Dhaka
        fetchPOIs();

        // Route between two points in Dhaka
        calculateRoute(new GeoPoint(23.8103, 90.4125), new GeoPoint(23.7949, 90.4043)); // Example: Dhaka route

        // Hamburger Menu Click Listener
        menuIcon.setOnClickListener(v -> drawerLayout.openDrawer(findViewById(R.id.navigation_view)));
        // Profile Icon Click Listener
        profileIcon.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(MainActivity.this, profileIcon);
            popup.getMenuInflater().inflate(R.menu.profile_menu, popup.getMenu());

            updateMenuItems(popup.getMenu());
            popup.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.menu_login) {
                    if (isLoggedIn()) {
                        logoutUser(); // If logged in, log out
                    } else {
                        Intent loginIntent = new Intent(MainActivity.this, LoginActivity.class);
                        startActivity(loginIntent);
                    }
                    return true;
                } else if (itemId == R.id.menu_signup) {
                    Intent signupIntent = new Intent(MainActivity.this, SignUpActivity.class);
                    startActivity(signupIntent);
                    return true;
                } else if (itemId == R.id.menu_profile) {
                    if (isLoggedIn()) {
                        Intent profileIntent = new Intent(MainActivity.this, ProfileActivity.class);
                        startActivity(profileIntent);

                    } else {
                        Toast.makeText(MainActivity.this, "Please log in to access your profile.", Toast.LENGTH_SHORT).show();
                    }
                    return true;
                }
                return false;
            });

            popup.show();
        });





        fetchPOIs(); // Ensure POIs are loaded before filtering

        // Setup category buttons (THIS FIXES YOUR ISSUE)
        setupCategoryButtons();


        sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        drawerLayout = findViewById(R.id.drawer_layout);

//for alert part
        menuIcon.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(MainActivity.this, menuIcon);
            popup.getMenuInflater().inflate(R.menu.drawer_menu, popup.getMenu());

            popup.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.alert) {
                    // Check if user is logged in before navigating
                    if (isLoggedIn()) {
                        Intent intent = new Intent(MainActivity.this, AlertActivity.class);
                        startActivity(intent);
                    } else {
                        Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
                    }
                    return true;
                } else if (itemId == R.id.home) {
                    Toast.makeText(this, "Home clicked", Toast.LENGTH_SHORT).show();
                    return true;
                } else  if (itemId == R.id.weather) {
                    // Navigate to WeatherActivity
                    Intent intent = new Intent(MainActivity.this, WeatherActivity.class);
                    startActivity(intent);
                    return true;
                } else if (itemId == R.id.report_issue) {
                    Intent intent = new Intent(MainActivity.this, ReportIssueActivity.class);
                    startActivity(intent);
                    return true;


            } else if (itemId == R.id.water) {
                    Intent intent = new Intent(MainActivity.this, LogAreaActivity.class);
                    startActivity(intent);
                    return true;
                }

             else if (itemId == R.id.help) {
                    Toast.makeText(this, "Help clicked", Toast.LENGTH_SHORT).show();
                    return true;
                }
                return false;
            });

            popup.show();
        });

//for map location

        checkAndCleanupPreviousDay();



        getCurrentLocation();

        new Thread(() -> { // ⚡ run in background thread (important for network)
            JSONArray weatherData = fetchHourlyWeatherData();

            if (weatherData.length() > 0) {
                for (int polygonId = 193; polygonId <= 382; polygonId++) {
                    processPolygonVisibility(polygonId, weatherData);
                }
            } else {
                Log.e("WeatherActivity", "No hourly weather data fetched.");
            }
        }).start();


    }










    private void getCurrentLocation() {
        // Fixed coordinates for Dhaka
        double lat = 23.8103;  // Latitude for Dhaka
        double lon = 90.4125;  // Longitude for Dhaka

        // Fetch weather data based on the fixed location (Dhaka)
        fetchWeatherData(lat, lon);  // No need for location permissions now
    }


    private void fetchWeatherData(double lat, double lon) {
        String url = String.format(Locale.US,
                "https://weather.visualcrossing.com/VisualCrossingWebServices/rest/services/timeline/%f,%f?key=%s&contentType=json&unitGroup=metric",
                lat, lon, API_KEY);

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "API Failure: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "API Response Error: " + response.code());
                    return;
                }

                try {
                    String responseString = response.body().string();
                    JSONObject jsonResponse = new JSONObject(responseString);

                    // Parse weather data and save it to the database
                    runOnUiThread(() -> {
                        try {
                            saveWeatherDataToDatabase(jsonResponse);
                        } catch (JSONException e) {
                            Log.e(TAG, "JSON Parsing Error: " + e.getMessage());
                        }
                    });

                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing JSON response: " + e.getMessage());
                }
            }
        });
    }

    private void saveWeatherDataToDatabase(JSONObject jsonResponse) throws JSONException {
        // Get the current date in yyyy-MM-dd format
        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        JSONArray hourlyArray = jsonResponse.getJSONArray("days").getJSONObject(0).getJSONArray("hours");

        // Save hourly forecast data
        for (int i = 0; i < hourlyArray.length(); i++) {
            JSONObject hour = hourlyArray.getJSONObject(i);

            // Prepare data to insert into Supabase
            JSONObject dataToInsert = new JSONObject();
            dataToInsert.put("date", todayDate);

            // ✅ Fix: Format time correctly for TIME column
            String rawTime = hour.getString("datetime");  // Example: "09:00" or "21:00"
            String[] parts = rawTime.split(":");
            String formattedTime = String.format("%02d:00:00", Integer.parseInt(parts[0]));
            dataToInsert.put("time", formattedTime); // Insert formatted time into the "time" column

            // ✅ Populate remaining fields as per schema
            dataToInsert.put("temperature", hour.getDouble("temp"));
            dataToInsert.put("feelslike", hour.getDouble("feelslike"));
            dataToInsert.put("windspeed", hour.getDouble("windspeed"));
            dataToInsert.put("precipitation", hour.getDouble("precip"));
            dataToInsert.put("condition", hour.getString("conditions"));
            dataToInsert.put("rainchance", hour.getDouble("precipprob"));

            Log.d(TAG, "Uploading hourly forecast data: " + dataToInsert.toString());

            // ✅ Insert into daily_forecast table
            insertIntoSupabase("daily_forecast", dataToInsert);
        }

        // Save summary data (e.g., total precipitation)
        saveTodaySummaryIntoSupabase(jsonResponse, hourlyArray);
    }


    private void saveTodaySummaryIntoSupabase(JSONObject json, JSONArray hourly) {
        try {
            String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            double cumulativePrecip = 0.0; // Initialize cumulative precipitation

            // Fetch yesterday's 23rd hour precipitation (if it exists)
            double yesterday23Precip = getYesterdayHour23Precipitation();
            cumulativePrecip += yesterday23Precip; // Add yesterday's precipitation to cumulative

            for (int i = 0; i < hourly.length(); i++) {
                JSONObject hour = hourly.getJSONObject(i);

                // Extract the hour (e.g., "14:00:00" -> 14)
                int timeHour = Integer.parseInt(hour.getString("datetime").substring(0, 2));

                // Get precipitation for this hour
                double precip = hour.getDouble("precip");

                // Special case: If time_hour = 0 and cumulativePrecip is adjusted
                if (timeHour == 0 && precip > 0 && yesterday23Precip > 0) {
                    // Add yesterday's 23-hour precip to today's 0-hour precip
                    cumulativePrecip += yesterday23Precip;
                } else {
                    // Update cumulative precipitation as usual
                    if (precip > 0) {
                        cumulativePrecip += precip;
                    } else {
                        cumulativePrecip = 0.0;  // Reset cumulative precipitation if no precipitation
                    }
                }

                // Prepare summary data to insert
                JSONObject summaryData = new JSONObject();
                summaryData.put("date", todayDate);
                summaryData.put("time_hour", timeHour);  // Use time_hour for the hour value
                summaryData.put("prec", precip);
                summaryData.put("total_precipitation", cumulativePrecip); // Adjusted total precipitation

                Log.d(TAG, "Uploading summary data for hour " + timeHour + ": " + summaryData.toString());

                // Insert the data into Supabase
                insertIntoSupabase("current_day_summary", summaryData);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error saving summary data: " + e.getMessage());
        }
    }



    private void insertIntoSupabase(String tableName, JSONObject dataObject) {
        OkHttpClient client = new OkHttpClient();

        // ✅ Use correct on_conflict clause for each table
        String conflictClause = "";
        if (tableName.equals("daily_forecast")) {
            conflictClause = "?on_conflict=date,time";
        } else if (tableName.equals("current_day_summary")) {
            conflictClause = "?on_conflict=date,time_hour";
        } else if (tableName.equals("polygon_visibility")) {
            conflictClause = "?on_conflict=date,polygon_id,visibility_start_time,visibility_end_time";
        }

        String url = SUPABASE_URL + "/rest/v1/" + tableName + conflictClause;

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "resolution=merge-duplicates")
                .post(okhttp3.RequestBody.create(dataObject.toString(), okhttp3.MediaType.parse("application/json")))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Error inserting data into Supabase: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Successfully inserted into " + tableName);
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    Log.e(TAG, "Insert failed for " + tableName + ": " + response.code() + " | " + errorBody);
                }
            }
        });
    }


    private double getYesterdayHour23Precipitation() {
        try {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DATE, -1); // Move one day back
            String yesterdayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.getTime());

            // Construct the URL to fetch yesterday's time_hour = 23 precipitation
            String url = SUPABASE_URL + "/rest/v1/current_day_summary"
                    + "?select=total_precipitation"
                    + "&date=eq." + yesterdayDate
                    + "&time_hour=eq.23"; // Get only time_hour = 23 data for the previous day

            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("apikey", SUPABASE_KEY)
                    .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                    .addHeader("Accept", "application/json")
                    .build();

            Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                String body = response.body().string();
                JSONArray array = new JSONArray(body);

                if (array.length() > 0) {
                    JSONObject obj = array.getJSONObject(0);
                    double precip = obj.getDouble("total_precipitation");
                    Log.d("FetchYesterday23", "✅ Yesterday 23h total_precip: " + precip);
                    return precip;  // Return yesterday's precipitation for time_hour = 23
                } else {
                    Log.d("FetchYesterday23", "⚡ No data found for yesterday's time_hour 23.");
                }
            } else {
                Log.e("FetchYesterday23", "❌ Failed to fetch yesterday's data: " + response.code());
            }
        } catch (Exception e) {
            Log.e("FetchYesterday23", "❌ Exception: " + e.getMessage());
        }
        return 0.0;  // Return 0 if no data found or an error occurs
    }

    private boolean cleanupDoneToday = false; // Add this as a class-level variable

    private void checkAndCleanupPreviousDay() {
        if (cleanupDoneToday) return;

        // Get yesterday's date
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, -1); // Move one day back
        String yesterdayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.getTime());

        // Construct the SQL query to delete data except for yesterday's time_hour = 23
        String deleteUrl = SUPABASE_URL + "/rest/v1/current_day_summary"
                + "?date=eq." + yesterdayDate
                + "&time_hour=lt.23"; // Delete everything except time_hour = 23

        OkHttpClient client = new OkHttpClient();
        Request deleteRequest = new Request.Builder()
                .url(deleteUrl)
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                .delete() // Delete the data
                .build();

        // Execute the delete request
        client.newCall(deleteRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to delete old data: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    Log.d(TAG, "✅ Successfully deleted old data for previous day except for time_hour 23.");
                    cleanupDoneToday = true;
                } else {
                    Log.e(TAG, "❌ Error deleting old data: " + response.code());
                }
            }
        });
    }
















    // Set up touch listener for the map

//    private void setupMapTouchListener() {
//        MapEventsOverlay mapEventsOverlay = new MapEventsOverlay(new MapEventsReceiver() {
//            @Override
//            public boolean singleTapConfirmedHelper(GeoPoint p) {
//                displayLocationInfo(p); // Now it works correctly
//                return true;
//            }
//
//            @Override
//            public boolean longPressHelper(GeoPoint p) {
//                return false;
//            }
//        });
//
//        mapView.getOverlays().add(mapEventsOverlay);
//    }


    private boolean checkAndRequestAudioPermissions() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.RECORD_AUDIO}, AUDIO_PERMISSION_REQUEST);
            return false;
        }
        return true;
    }

    private void displayLocationInfo(GeoPoint geoPoint) {
        // Reverse geocode to get location name
        String locationName = getLocationName(geoPoint);

        // Create a new marker
        Marker marker = new Marker(mapView);
        marker.setPosition(geoPoint);
        marker.setTitle(locationName + "\nLat: " + geoPoint.getLatitude() + "\nLon: " + geoPoint.getLongitude());
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

        // Dynamically resize the icon based on zoom level
        float zoomLevel = (float) mapView.getZoomLevelDouble();
        int iconSize = (int) (zoomLevel * 5); // Adjust multiplier as needed
        Drawable resizedIcon = resizeDrawable(R.drawable.loc, iconSize, iconSize);
        marker.setIcon(resizedIcon);

        // Add marker to map
        mapView.getOverlays().add(marker);
        mapView.invalidate();

        // Show info window
        marker.showInfoWindow();

        // Remove marker after 4 seconds
        new Handler().postDelayed(() -> {
            marker.closeInfoWindow(); // Hide info box
            mapView.getOverlays().remove(marker); // Remove marker
            mapView.invalidate();
        }, 4000); // 4 seconds
    }



    private Drawable resizeDrawable(int drawableId, int width, int height) {
        Drawable drawable = ContextCompat.getDrawable(this, drawableId);
        if (drawable == null) return null;

        // If the drawable is already a BitmapDrawable, resize it
        if (drawable instanceof BitmapDrawable) {
            Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, width, height, false);
            return new BitmapDrawable(getResources(), resizedBitmap);
        }
        // If it's a VectorDrawable, convert it to a Bitmap and resize
        else if (drawable instanceof VectorDrawable) {
            return getBitmapFromVector(drawable, width, height);
        }
        return drawable; // Return original if not handled
    }


    private Drawable getBitmapFromVector(Drawable vectorDrawable, int width, int height) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        vectorDrawable.draw(canvas);
        return new BitmapDrawable(getResources(), bitmap);
    }



    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }

    // Reverse geocode to get location name
    private String getLocationName(GeoPoint geoPoint) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        // Check if the device has internet connectivity
        if (!isNetworkAvailable()) {
            Log.e(TAG, "No internet connection");
            return "Internet required for location lookup";
        }

        try {
            List<Address> addresses = geocoder.getFromLocation(
                    geoPoint.getLatitude(),
                    geoPoint.getLongitude(),
                    1
            );

            if (addresses != null && !addresses.isEmpty()) {
                return addresses.get(0).getAddressLine(0);
            }
        } catch (IOException e) {
            Log.e(TAG, "Geocoder failed: " + e.getMessage(), e);
        }

        return "Unknown Location";
    }

    // Setup location overlay
//    private void setupLocationOverlay() {
//        locationOverlay = new MyLocationNewOverlay(mapView);
//        mapView.getOverlays().add(locationOverlay);
//        locationOverlay.enableMyLocation();
//        locationOverlay.enableFollowLocation();
//    }



    private void updateMenuItems(android.view.Menu menu) {
        if (isLoggedIn()) {
            menu.findItem(R.id.menu_login).setTitle("Logout");
            menu.findItem(R.id.menu_signup).setVisible(false); // Hide Sign Up if logged in
        } else {
            menu.findItem(R.id.menu_login).setTitle("Login");
            menu.findItem(R.id.menu_signup).setVisible(true); // Show Sign Up if not logged in
        }
    }



    // Check login state
    private boolean isLoggedIn() {
        return sharedPreferences.getBoolean("isLoggedIn", false);
    }

    // Log out user
    private void logoutUser() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear(); // Clear all stored session data
        editor.apply();

        Toast.makeText(this, "Logged out successfully!", Toast.LENGTH_SHORT).show();
        // Optionally, navigate to login screen or refresh activity
        recreate(); // Refresh the activity to update the menu
    }
    private void startVoiceSearch() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your destination...");
        try {
            startActivityForResult(intent, SPEECH_REQUEST_CODE);
        } catch (Exception e) {
            Toast.makeText(this, "Speech recognition not supported", Toast.LENGTH_SHORT).show();
        }
    }









    @Override
    protected void onResume() {
        super.onResume();
        if (locationOverlay != null) {
            locationOverlay.enableMyLocation();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (locationOverlay != null) {
            locationOverlay.disableMyLocation();
        }
    }

    // Method to check and request location permissions
    private boolean checkAndRequestLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST);
            return false;
        }
        return true;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                String spokenText = results.get(0);
                searchView.setQuery(spokenText, true); // Trigger search with spoken text
            }
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Location permission granted!", Toast.LENGTH_SHORT).show();
                if (isLocationEnabled()) {
                    setupLocationOverlay(); // Enable location overlay
                } else {
                    Toast.makeText(this, "Please enable location services in your device settings.", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(this, "Location permission denied!", Toast.LENGTH_LONG).show();
            }
        }
    }

    // Method to check if location services are enabled
    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    // Method to set up MyLocationOverlay
    private void setupLocationOverlay() {
        locationOverlay = new MyLocationNewOverlay(mapView);
        mapView.getOverlays().add(locationOverlay);
        locationOverlay.enableMyLocation();
        locationOverlay.enableFollowLocation();

        if (!locationOverlay.isMyLocationEnabled()) {
            Toast.makeText(this, "My location overlay could not be enabled!", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Location overlay not enabled.");
        } else {
            Toast.makeText(this, "My location overlay enabled successfully.", Toast.LENGTH_SHORT).show();
            locationOverlay.runOnFirstFix(() -> {
                GeoPoint currentLocation = locationOverlay.getMyLocation();
                if (currentLocation != null) {
                    Log.d(TAG, "Current location: " + currentLocation.getLatitude() + ", " + currentLocation.getLongitude());
                    runOnUiThread(() -> mapView.getController().setCenter(currentLocation));
                } else {
                    Log.e(TAG, "Unable to fetch current location.");
                }
            });
        }
    }

    private void setupSearchView() {
        searchView.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                fetchCoordinates(query);
                searchView.clearFocus(); // Clear focus after search
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
    }

    private void fetchCoordinates(String query) {
        String apiUrl = String.format(Locale.getDefault(), "https://nominatim.openstreetmap.org/search?q=%s&format=json&addressdetails=1", query);

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(apiUrl).build();

        new Thread(() -> {
            try {
                Response response = client.newCall(request).execute();
                if (response.isSuccessful() && response.body() != null) {
                    String jsonResponse = response.body().string();
                    Log.d(TAG, "Overpass API Response: " + jsonResponse);
                    // Use the built-in runOnUiThread method
                    runOnUiThread(() -> parseCoordinates(jsonResponse));
                } else {
                    String errorMsg = "Request failed. Code: " + response.code();
                    Log.e(TAG, errorMsg);
                    runOnUiThread(() -> Toast.makeText(this, "Failed to fetch location: " + errorMsg, Toast.LENGTH_LONG).show());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching coordinates", e);
                runOnUiThread(() -> Toast.makeText(this, "Error fetching location: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void parseCoordinates(String jsonResponse) {
        try {
            JSONArray jsonArray = new JSONArray(jsonResponse);
            if (jsonArray.length() > 0) {
                JSONObject locationObject = jsonArray.getJSONObject(0);
                double lat = locationObject.getDouble("lat");
                double lon = locationObject.getDouble("lon");

                GeoPoint geoPoint = new GeoPoint(lat, lon);
                mapView.getController().setCenter(geoPoint);

                Marker marker = new Marker(mapView);
                marker.setPosition(geoPoint);
                marker.setTitle("Location: " + locationObject.getString("display_name"));
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                mapView.getOverlays().add(marker);
                mapView.invalidate(); // Refresh the map

                Toast.makeText(this, "Moved to location: " + locationObject.getString("display_name"), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "No results found for the location.", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing coordinates", e);
            Toast.makeText(this, "Error parsing location data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private static final int MAX_MARKERS_PER_CATEGORY = 1000; // Adjust the number as needed


    private void setupCategoryButtons() {
        findViewById(R.id.btn_hospitals).setOnClickListener(v -> showPOI(hospitalMarkers, isHospitalVisible, "hospital"));
        findViewById(R.id.btn_banks).setOnClickListener(v -> showPOI(bankMarkers, isBankVisible, "bank"));
        findViewById(R.id.btn_schools).setOnClickListener(v -> showPOI(schoolMarkers, isSchoolVisible, "school"));
        findViewById(R.id.btn_police).setOnClickListener(v -> showPOI(policeMarkers, isPoliceVisible, "police"));
        findViewById(R.id.btn_gas_stations).setOnClickListener(v -> showPOI(gasStationMarkers, isGasStationVisible, "gas"));
        findViewById(R.id.btn_atms).setOnClickListener(v -> showPOI(atmMarkers, isAtmVisible, "atm"));
        findViewById(R.id.btn_libraries).setOnClickListener(v -> showPOI(libraryMarkers, isLibraryVisible, "library"));
    }

    // Track if each category is currently visible
    private boolean isHospitalVisible = false;
    private boolean isBankVisible = false;
    private boolean isSchoolVisible = false;
    private boolean isPoliceVisible = false;
    private boolean isGasStationVisible = false;
    private boolean isAtmVisible = false;
    private boolean isLibraryVisible = false;


    private void showPOI(List<Marker> markers, boolean isVisible, String category) {
        if (isVisible) {
            // If already visible, remove these POIs
            mapView.getOverlays().removeIf(overlay -> markers.contains(overlay));
            isVisible = false;
        } else {
            // Add POIs only if they were not visible before
            mapView.getOverlays().addAll(markers);
            isVisible = true;
        }

        // Ensure location overlay remains on the map
        if (locationOverlay != null) {
            mapView.getOverlays().add(locationOverlay);
        }

        // Ensure touch events remain
        mapView.getOverlays().add(new MapEventsOverlay(mapTouchReceiver));

        // Refresh map
        mapView.invalidate();

        // Update visibility flags
        switch (category) {
            case "hospital": isHospitalVisible = isVisible; break;
            case "bank": isBankVisible = isVisible; break;
            case "school": isSchoolVisible = isVisible; break;
            case "police": isPoliceVisible = isVisible; break;
            case "gas": isGasStationVisible = isVisible; break;
            case "atm": isAtmVisible = isVisible; break;
            case "library": isLibraryVisible = isVisible; break;
        }
    }






    private MapEventsOverlay mapTouchListener;
    private MapEventsReceiver mapTouchReceiver;

//    private MapEventsOverlay mapTouchListener;

    private void setupMapTouchListener() {
        mapTouchReceiver = new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                displayLocationInfo(p);
                return true;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                return false;
            }
        };

        mapView.getOverlays().add(new MapEventsOverlay(mapTouchReceiver));
    }




    private void displayPOIs(String jsonResponse) {
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            JSONArray elements = jsonObject.getJSONArray("elements");

            int hospitalCount = 0, bankCount = 0, schoolCount = 0, policeCount = 0;
            int gasStationCount = 0, shoppingMallCount = 0, atmCount = 0, libraryCount = 0;

            for (int i = 0; i < elements.length(); i++) {
                JSONObject element = elements.getJSONObject(i);
                double lat = element.getDouble("lat");
                double lon = element.getDouble("lon");
                String type = element.optJSONObject("tags").optString("amenity", "Unknown");

                Marker marker = new Marker(mapView);
                marker.setPosition(new GeoPoint(lat, lon));
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

                // Set and scale icons
                // Set and scale icons based on zoom level
                float zoomLevel = (float) mapView.getZoomLevelDouble();
                int iconSize = (int) (zoomLevel * 5); // Dynamically scale size based on zoom

                Drawable icon = null;
                switch (type) {
                    case "hospital":
                        if (hospitalCount++ < MAX_MARKERS_PER_CATEGORY)
                            icon = resizeIcon(R.drawable.hos, iconSize, iconSize);
                        hospitalMarkers.add(marker);
                        break;
                    case "bank":
                        if (bankCount++ < MAX_MARKERS_PER_CATEGORY)
                            icon = resizeIcon(R.drawable.bank, iconSize, iconSize);
                        bankMarkers.add(marker);
                        break;
                    case "school":
                        if (schoolCount++ < MAX_MARKERS_PER_CATEGORY)
                            icon = resizeIcon(R.drawable.schl, iconSize, iconSize);
                        schoolMarkers.add(marker);
                        break;
                    case "police":
                        if (policeCount++ < MAX_MARKERS_PER_CATEGORY)
                            icon = resizeIcon(R.drawable.police, iconSize, iconSize);
                        policeMarkers.add(marker);
                        break;
                    case "fuel":  // Gas Station
                        if (gasStationCount++ < MAX_MARKERS_PER_CATEGORY)
                            icon = resizeIcon(R.drawable.gas, iconSize, iconSize);
                        gasStationMarkers.add(marker);
                        break;
                    case "atm":
                        if (atmCount++ < MAX_MARKERS_PER_CATEGORY)
                            icon = resizeIcon(R.drawable.atm, iconSize, iconSize);
                        atmMarkers.add(marker);
                        break;
                    case "library":
                        if (libraryCount++ < MAX_MARKERS_PER_CATEGORY)
                            icon = resizeIcon(R.drawable.lib, iconSize, iconSize);
                        libraryMarkers.add(marker);
                        break;
                }

                if (icon != null) marker.setIcon(icon);

            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing POIs", e);
        }
    }


    private Drawable resizeIcon(int drawableId, int width, int height) {
        Drawable drawable = ContextCompat.getDrawable(this, drawableId);
        if (drawable == null) return null;

        // Handle PNGs (BitmapDrawable)
        if (drawable instanceof BitmapDrawable) {
            Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, width, height, false);
            return new BitmapDrawable(getResources(), resizedBitmap);
        }
        // Handle SVGs (VectorDrawable)
        else if (drawable instanceof VectorDrawable) {
            return getBitmapFromVector(drawable, width, height);
        }

        return drawable; // Return original if not handled
    }




    // Store POIs by category
    private List<Marker> hospitalMarkers = new ArrayList<>();
    private List<Marker> bankMarkers = new ArrayList<>();
    private List<Marker> schoolMarkers = new ArrayList<>();
    private List<Marker> policeMarkers = new ArrayList<>();
    private List<Marker> gasStationMarkers = new ArrayList<>();
    private List<Marker> shoppingMallMarkers = new ArrayList<>();
    private List<Marker> atmMarkers = new ArrayList<>();
    private List<Marker> libraryMarkers = new ArrayList<>();

    // Fetch POIs including police stations, gas stations, shopping malls, ATMs, and libraries
    private void fetchPOIs() {
        String overpassUrl = "http://overpass-api.de/api/interpreter?data=[out:json];" +
                "(node[amenity=hospital](23.7,90.3,23.9,90.5);" +
                "node[amenity=bank](23.7,90.3,23.9,90.5);" +
                "node[amenity=school](23.7,90.3,23.9,90.5);" +
                "node[amenity=police](23.7,90.3,23.9,90.5);" +
                "node[amenity=fuel](23.7,90.3,23.9,90.5);" +
                "node[shop=mall](23.7,90.3,23.9,90.5);" +
                "node[amenity=atm](23.7,90.3,23.9,90.5);" +
                "node[amenity=library](23.7,90.3,23.9,90.5);" +
                ");out;";

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(overpassUrl).build();

        new Thread(() -> {
            try {
                Response response = client.newCall(request).execute();
                if (response.isSuccessful() && response.body() != null) {
                    String jsonResponse = response.body().string();
                    runOnUiThread(() -> displayPOIs(jsonResponse));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching POIs", e);
            }
        }).start();
    }



    // Method to calculate a route between two points
    private void calculateRoute(GeoPoint startPoint, GeoPoint endPoint) {
        // Add markers for start and end points
        Marker startMarker = new Marker(mapView);
        startMarker.setPosition(startPoint);
        startMarker.setTitle("Start Point");
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        mapView.getOverlays().add(startMarker);

        Marker endMarker = new Marker(mapView);
        endMarker.setPosition(endPoint);
        endMarker.setTitle("End Point");
        endMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        mapView.getOverlays().add(endMarker);

        String osrmUrl = "http://router.project-osrm.org/route/v1/driving/" + startPoint.getLongitude() + "," + startPoint.getLatitude() + ";" + endPoint.getLongitude() + "," + endPoint.getLatitude() + "?overview=full&geometries=geojson";

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(osrmUrl).build();

        new Thread(() -> {
            try {
                Response response = client.newCall(request).execute();
                if (response.isSuccessful() && response.body() != null) {
                    String jsonResponse = response.body().string();
                    Log.d(TAG, "OSRM API Response: " + jsonResponse);
                    runOnUiThread(() -> displayRoute(jsonResponse, startPoint, endPoint));
                } else {
                    String errorMsg = "Route request failed. Code: " + response.code();
                    Log.e(TAG, errorMsg);
                    runOnUiThread(() -> Toast.makeText(this, "Failed to calculate route: " + errorMsg, Toast.LENGTH_LONG).show());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error calculating route", e);
                runOnUiThread(() -> Toast.makeText(this, "Error calculating route: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    // Method to display route on the map
    private void displayRoute(String jsonResponse, GeoPoint startPoint, GeoPoint endPoint) {
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            JSONArray coordinates = jsonObject.getJSONArray("routes").getJSONObject(0).getJSONObject("geometry").getJSONArray("coordinates");

            List<GeoPoint> geoPoints = new ArrayList<>();
            for (int i = 0; i < coordinates.length(); i++) {
                JSONArray coord = coordinates.getJSONArray(i);
                double lon = coord.getDouble(0);
                double lat = coord.getDouble(1);
                geoPoints.add(new GeoPoint(lat, lon));
            }

            // Create and style the polyline
            Polyline polyline = new Polyline();
            polyline.setPoints(geoPoints);
            polyline.setColor(getResources().getColor(android.R.color.holo_red_dark)); // Dark red color
            polyline.setWidth(7.0f); // Adjust width
            mapView.getOverlays().add(polyline);

            // Adjust map to fit the route
            BoundingBox boundingBox = BoundingBox.fromGeoPoints(geoPoints);
            mapView.zoomToBoundingBox(boundingBox, true);

            mapView.invalidate(); // Refresh the map
            Toast.makeText(this, "Route added to map", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error displaying route", e);
            Toast.makeText(this, "Error displaying route: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }



    // Fetch and show location name on touch
    private JSONArray fetchHourlyWeatherData() {
        try {
            String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            String url = SUPABASE_URL + "/rest/v1/current_day_summary?select=time_hour,total_precipitation&date=eq." + todayDate + "&order=time_hour.asc";

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("apikey", SUPABASE_KEY)
                    .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                    .addHeader("Accept", "application/json")
                    .build();

            Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                String responseBody = response.body().string();
                Log.d("FetchHourlyWeather", "✅ Fetched weather data successfully (sorted by time_hour)");
                return new JSONArray(responseBody);
            } else {
                Log.e("FetchHourlyWeather", "❌ Fetch failed! Status: " + response.code());
            }
        } catch (Exception e) {
            Log.e("FetchHourlyWeather", "❌ Exception: " + e.getMessage());
        }
        return new JSONArray();
    }


    private double getRainThresholdForPolygon(int polygonId) {
        if (polygonId >= 193 && polygonId <= 194) return 5;
        if (polygonId >= 195 && polygonId <= 199) return 6;
        if (polygonId == 200) return 7;
        if (polygonId >= 201 && polygonId <= 227) return 8;
        if (polygonId >= 228 && polygonId <= 269) return 10;
        if (polygonId >= 270 && polygonId <= 304) return 12;
        if (polygonId >= 305 && polygonId <= 339) return 15;
        if (polygonId >= 340 && polygonId <= 356) return 18;
        if (polygonId >= 357 && polygonId <= 378) return 20;
        if (polygonId >= 379 && polygonId <= 380) return 22;
        if (polygonId == 381) return 25;
        if (polygonId == 382) return 26;
        if (polygonId == 338) return 28;
        return Double.MAX_VALUE; // Default high value to never trigger
    }

    private double getReducePerMin(int polygonId) {
        return 5.0; // Example: fixed reduction rate
    }

    private void processPolygonVisibility(int polygonId, JSONArray weatherData) {
        double rainThreshold = getRainThresholdForPolygon(polygonId);
        double reducePerMin = getReducePerMin(polygonId);

        boolean visibilityStarted = false;
        int visibilityStartHour = -1;
        double waterAmount = 0;
        int lastNonZeroHour = -1;

        Log.d("PolygonProcessing", "▶️ Starting processing for polygonId = " + polygonId);

        for (int i = 0; i < weatherData.length(); i++) {
            try {
                JSONObject hourData = weatherData.getJSONObject(i);
                int timeHour = hourData.getInt("time_hour");
                double totalPrecip = hourData.getDouble("total_precipitation");

                if (!visibilityStarted && totalPrecip >= rainThreshold) {
                    visibilityStarted = true;
                    visibilityStartHour = timeHour;
                    waterAmount = totalPrecip;
                    lastNonZeroHour = timeHour;
                    Log.d("VisibilityStart", "🌧️ Visibility started for polygonId=" + polygonId + " at " + formatTime(visibilityStartHour));
                } else if (visibilityStarted) {
                    if (totalPrecip > 0) {
                        int minutesPassed = (timeHour - lastNonZeroHour) * 60;
                        double reducedWater = minutesPassed / reducePerMin;
                        waterAmount = Math.max(waterAmount - reducedWater, 0);
                        waterAmount += totalPrecip;
                        lastNonZeroHour = timeHour;
                        Log.d("VisibilityUpdate", "➕ New rain added at " + formatTime(timeHour) + ", updated waterAmount=" + waterAmount);
                    } else if (totalPrecip == 0) {
                        int minutesPassed = (timeHour - lastNonZeroHour) * 60;
                        double reducedWater = minutesPassed / reducePerMin;
                        waterAmount = Math.max(waterAmount - reducedWater, 0);

                        if (waterAmount <= 0) {
                            insertPolygonVisibilityIntoDatabase(polygonId, visibilityStartHour, timeHour);

                            // Reset
                            visibilityStarted = false;
                            visibilityStartHour = -1;
                            waterAmount = 0;
                            lastNonZeroHour = -1;

                            Log.d("VisibilityEnd", "✅ Water drained completely. Visibility recorded for polygonId=" + polygonId +
                                    " from " + formatTime(visibilityStartHour) + " to " + formatTime(timeHour));
                        }
                    }
                }
            } catch (JSONException e) {
                Log.e("PolygonProcessing", "❌ Error parsing weather data: " + e.getMessage());
            }
        }

        if (visibilityStarted && waterAmount > 0) {
            double finalHour = 23.0;
            double minutesLeft = waterAmount * reducePerMin;
            double visibilityEndHour = finalHour + (minutesLeft / 60.0);

            insertPolygonVisibilityIntoDatabase(polygonId, visibilityStartHour, visibilityEndHour);

            Log.d("VisibilityEndFinal", "✅ End of day processing: PolygonId=" + polygonId +
                    " Visibility from " + formatTime(visibilityStartHour) + " to " + convertHourToTimeFormat(visibilityEndHour));
        }

        Log.d("PolygonProcessing", "🏁 Done processing polygonId=" + polygonId);
    }

    private void insertPolygonVisibilityIntoDatabase(int polygonId, double startHour, double endHour) {
        OkHttpClient client = new OkHttpClient();

        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String startTimeStr = convertHourToTimeFormat(startHour);
        String endTimeStr = convertHourToTimeFormat(endHour);

        // Step 1: Check if already exists
        String checkUrl = SUPABASE_URL + "/rest/v1/polygon_visibility"
                + "?polygon_id=eq." + polygonId
                + "&visibility_start_time=eq." + startTimeStr
                + "&visibility_end_time=eq." + endTimeStr
                + "&date=eq." + todayDate;

        Request checkRequest = new Request.Builder()
                .url(checkUrl)
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                .addHeader("Accept", "application/json")
                .build();

        client.newCall(checkRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("InsertVisibilityCheck", "❌ Check failed: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    JSONArray existingRecords;
                    try {
                        existingRecords = new JSONArray(responseBody);
                        if (existingRecords.length() > 0) {
                            // Already exists
                            Log.d("InsertVisibilityCheck", "⚡ Already exists! Skipping polygonId=" + polygonId);
                        } else {
                            // Step 2: Not found, Insert it
                            JSONObject dataToInsert = new JSONObject();
                            try {
                                dataToInsert.put("polygon_id", polygonId);
                                dataToInsert.put("visibility_start_time", startTimeStr);
                                dataToInsert.put("visibility_end_time", endTimeStr);
                                dataToInsert.put("rain_threshold_mm", getRainThresholdForPolygon(polygonId));
                                dataToInsert.put("waterlogged_duration_minutes", (int)((endHour - startHour) * 60));
                                dataToInsert.put("is_visible", true);
                                dataToInsert.put("date", todayDate); // Make sure your DB has this field
                            } catch (JSONException e) {
                                Log.e("InsertVisibility", "❌ JSON creation failed: " + e.getMessage());
                                return;
                            }

                            String insertUrl = SUPABASE_URL + "/rest/v1/polygon_visibility";
                            Request insertRequest = new Request.Builder()
                                    .url(insertUrl)
                                    .addHeader("apikey", SUPABASE_KEY)
                                    .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                                    .addHeader("Content-Type", "application/json")
                                    .post(okhttp3.RequestBody.create(dataToInsert.toString(), okhttp3.MediaType.parse("application/json")))
                                    .build();

                            client.newCall(insertRequest).enqueue(new Callback() {
                                @Override
                                public void onFailure(Call call, IOException e) {
                                    Log.e("InsertVisibility", "❌ Failed inserting visibility: " + e.getMessage());
                                }

                                @Override
                                public void onResponse(Call call, Response response) throws IOException {
                                    if (response.isSuccessful()) {
                                        Log.d("InsertVisibility", "✅ Successfully inserted polygonId = " + polygonId);
                                    } else {
                                        String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                                        Log.e("InsertVisibility", "❌ Insert failed! Status=" + response.code() + " Body=" + errorBody);
                                    }
                                }
                            });
                        }
                    } catch (JSONException e) {
                        Log.e("InsertVisibilityCheck", "❌ JSON parse failed: " + e.getMessage());
                    }
                } else {
                    Log.e("InsertVisibilityCheck", "❌ Check failed! Status=" + response.code());
                }
            }
        });
    }


    private String formatTime(int hour) {
        return String.format(Locale.getDefault(), "%02d:00", hour);
    }

    private String addMinutesToTime(int startHour, double minutesToAdd) {
        int startMinutes = startHour * 60;
        int totalMinutes = (int) (startMinutes + minutesToAdd);
        int endHour = totalMinutes / 60;
        int endMinute = totalMinutes % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", endHour, endMinute);
    }

    private String convertHourToTimeFormat(double hour) {
        int hourPart = (int) hour;
        int minutePart = (int) Math.round((hour - hourPart) * 60);

        if (minutePart == 60) {
            hourPart += 1;
            minutePart = 0;
        }

        return String.format(Locale.getDefault(), "%02d:%02d:00", hourPart, minutePart);
    }

}