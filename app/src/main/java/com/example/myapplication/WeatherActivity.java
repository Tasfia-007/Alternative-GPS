package com.example.myapplication;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity {
    private static final String API_KEY = "5b49a51672cf4390b35181020252202";  // Your WeatherAPI Key
    private static final String TAG = "WeatherActivity";
    private FusedLocationProviderClient fusedLocationClient;
    private TextView currentWeather, dailyWeather, fullHourlyWeather, logView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);

        currentWeather = findViewById(R.id.current_weather);
//        dailyWeather = findViewById(R.id.daily_weather);
//        fullHourlyWeather = findViewById(R.id.full_hourly_weather);


        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        getCurrentLocation();
        startUpdatingTime();
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();
                    fetchWeatherData(latitude, longitude);
                } else {
                    logError("Location Error", "Failed to fetch location.");
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                logError("Location Failure", e.getMessage());
            }
        });
    }

    private void fetchWeatherData(double lat, double lon) {
        String url = String.format(Locale.US,
                "https://api.weatherapi.com/v1/forecast.json?key=%s&q=%f,%f&days=5&aqi=no&alerts=no",
                API_KEY, lat, lon);

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                logError("API Failure", e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    logError("API Response Error", "Response code: " + response.code());
                    return;
                }

                try {
                    JSONObject jsonResponse = new JSONObject(response.body().string());
                    parseWeatherData(jsonResponse);
                } catch (JSONException e) {
                    logError("JSON Parsing Error", e.getMessage());
                }
            }
        });
    }









    private void parseWeatherData(JSONObject json) throws JSONException {
        // Extract current weather data
        JSONObject current = json.getJSONObject("current");

        double temperature = current.getDouble("temp_c");
        String condition = current.getJSONObject("condition").getString("text");

        double windSpeed = current.getDouble("wind_kph");
        int humidity = current.getInt("humidity");
        double precipitation = current.has("precip_mm") ? current.getDouble("precip_mm") : 0.0;

        // Update individual UI elements
        updateUI(findViewById(R.id.current_weather), String.format("🌡️ %.1f°C", temperature));
        updateUI(findViewById(R.id.weather_condition), String.format("Condition: %s", condition));
        updateUI(findViewById(R.id.wind_info), String.format("💨 Wind: %.1f km/h", windSpeed));
        updateUI(findViewById(R.id.humidity_info), String.format("💧 Humidity: %d%%", humidity));
        updateUI(findViewById(R.id.precipitation_info), String.format("🌧️ Precipitation: %.1f mm", precipitation));

        // 🌧️ Full Hourly Forecast (24 Hours)

        // Get the hourly forecast array
        JSONArray hourly = json.getJSONObject("forecast").getJSONArray("forecastday").getJSONObject(0).getJSONArray("hour");

        // Find the container for hourly forecasts
        LinearLayout hourlyContainer = findViewById(R.id.hourly_forecast_container);

        // Clear any old data (if refreshing)
        runOnUiThread(() -> hourlyContainer.removeAllViews());

        // Loop through each hour
        for (int i = 0; i < 24; i++) {
            JSONObject hour = hourly.getJSONObject(i);

            // Extract data
            String time = hour.getString("time").substring(11, 16); // Get HH:mm format
            double temp = hour.getDouble("temp_c");
            String hourlyCondition = hour.getJSONObject("condition").getString("text");
            double rainChance = hour.getDouble("chance_of_rain");

            // Extract the hourly precipitation (mm)
            double hourlyPrecipitation;
            boolean isPrecipitationAvailable = false;  // Flag to check if precipitation data is available

            if (hour.has("precip_mm")) {
                hourlyPrecipitation = hour.getDouble("precip_mm");  // Precipitation in mm (ml)
                isPrecipitationAvailable = true;  // Precipitation data is available
            } else {
                hourlyPrecipitation = 0.0;  // No precipitation data available
                isPrecipitationAvailable = false;  // Precipitation data is not available
            }

            // Log the availability of precipitation data (for debugging purposes)
            Log.d(TAG, "Precipitation data available for " + time + ": " + isPrecipitationAvailable);

            // Create new layout for each hour (vertical)
            LinearLayout hourBlock = new LinearLayout(this);
            hourBlock.setOrientation(LinearLayout.VERTICAL);
            hourBlock.setPadding(20, 10, 20, 10);

            // Time TextView (Top)
            TextView timeView = new TextView(this);
            timeView.setText(time);
            timeView.setTextSize(16);
            timeView.setGravity(Gravity.CENTER);
            timeView.setTypeface(null, Typeface.BOLD);

            // Weather Info TextView (Below)
            TextView infoView = new TextView(this);
            // Show both chance of rain and precipitation
            if (isPrecipitationAvailable) {
                infoView.setText(String.format("%.1f°C\n%s\n🌧️ %.1f%% chance of rain\n💧 %.1f mm/ml precipitation",
                        temp, hourlyCondition, rainChance, hourlyPrecipitation));
            } else {
                infoView.setText(String.format("%.1f°C\n%s\n🌧️ %.1f%% chance of rain\n💧 No precipitation data available",
                        temp, hourlyCondition, rainChance));
            }
            infoView.setTextSize(14);
            infoView.setGravity(Gravity.CENTER);

            // Add views to layout
            hourBlock.addView(timeView);
            hourBlock.addView(infoView);

            // Add to horizontal container
            runOnUiThread(() -> hourlyContainer.addView(hourBlock));
        }

        // 📅 5-Day Forecast (Upcoming Forecast)
        JSONArray daily = json.getJSONObject("forecast").getJSONArray("forecastday");

        // Find the container for upcoming forecasts
        LinearLayout upcomingContainer = findViewById(R.id.upcoming_forecast_container);

        // Clear any old data (if refreshing)
        runOnUiThread(() -> upcomingContainer.removeAllViews());

        for (int i = 1; i < daily.length(); i++) { // Start from 1 (Skip today)
            JSONObject day = daily.getJSONObject(i);

            // Extract Date in "02 Feb, Sun" Format
            long timeEpoch = day.getLong("date_epoch") * 1000L;
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM, EEE", Locale.getDefault());
            String formattedDate = dateFormat.format(new Date(timeEpoch));

            // Extract Data
            double maxTemp = day.getJSONObject("day").getDouble("maxtemp_c");
            double minTemp = day.getJSONObject("day").getDouble("mintemp_c");
            String dayCondition = day.getJSONObject("day").getJSONObject("condition").getString("text");
            double rainChance = day.getJSONObject("day").getDouble("daily_chance_of_rain");

            // Create a layout for each day (vertical)
            LinearLayout dayBlock = new LinearLayout(this);
            dayBlock.setOrientation(LinearLayout.VERTICAL);
            dayBlock.setPadding(20, 10, 20, 10);

            // Date TextView (Top)
            TextView dateView = new TextView(this);
            dateView.setText(formattedDate);
            dateView.setTextSize(16);
            dateView.setGravity(Gravity.CENTER);
            dateView.setTypeface(null, Typeface.BOLD);

            // Weather Info TextView (Below)
            TextView dayInfoView = new TextView(this);
            dayInfoView.setText(String.format("🌡️ %.1f°C / %.1f°C\n%s\n🌧️ %.1f%% chance of rain",
                    maxTemp, minTemp, dayCondition, rainChance));
            dayInfoView.setTextSize(14);
            dayInfoView.setGravity(Gravity.CENTER);

            // Add views to layout
            dayBlock.addView(dateView);
            dayBlock.addView(dayInfoView);

            // Add to horizontal container
            runOnUiThread(() -> upcomingContainer.addView(dayBlock));
        }
    }












//
//
//    private void parseWeatherData(JSONObject json) throws JSONException {
//        // Extract current weather data
//        JSONObject current = json.getJSONObject("current");
//
//        double temperature = current.getDouble("temp_c");
//        String condition = current.getJSONObject("condition").getString("text");
//
//        double windSpeed = current.getDouble("wind_kph");
//        int humidity = current.getInt("humidity");
//        double precipitation = current.has("precip_mm") ? current.getDouble("precip_mm") : 0.0;
//
//        // Update individual UI elements
//        updateUI(findViewById(R.id.current_weather), String.format("🌡️ %.1f°C", temperature));
//        updateUI(findViewById(R.id.weather_condition), String.format("Condition: %s", condition));
//        updateUI(findViewById(R.id.wind_info), String.format("💨 Wind: %.1f km/h", windSpeed));
//        updateUI(findViewById(R.id.humidity_info), String.format("💧 Humidity: %d%%", humidity));
//        updateUI(findViewById(R.id.precipitation_info), String.format("🌧️ Precipitation: %.1f mm", precipitation));
//
//        // 🌧️ Full Hourly Forecast (24 Hours)
//
//        // Get the hourly forecast array
//        JSONArray hourly = json.getJSONObject("forecast").getJSONArray("forecastday").getJSONObject(0).getJSONArray("hour");
//
//        // Find the container for hourly forecasts
//        LinearLayout hourlyContainer = findViewById(R.id.hourly_forecast_container);
//
//        // Clear any old data (if refreshing)
//        runOnUiThread(() -> hourlyContainer.removeAllViews());
//
//        // Loop through each hour
//        for (int i = 0; i < 24; i++) {
//            JSONObject hour = hourly.getJSONObject(i);
//
//            // Extract data
//            String time = hour.getString("time").substring(11, 16); // Get HH:mm format
//            double temp = hour.getDouble("temp_c");
//            String hourlyCondition = hour.getJSONObject("condition").getString("text");
//            double rainChance = hour.getDouble("chance_of_rain");
//
//            // Create new layout for each hour (vertical)
//            LinearLayout hourBlock = new LinearLayout(this);
//            hourBlock.setOrientation(LinearLayout.VERTICAL);
//            hourBlock.setPadding(20, 10, 20, 10);
//
//            // Time TextView (Top)
//            TextView timeView = new TextView(this);
//            timeView.setText(time);
//            timeView.setTextSize(16);
//            timeView.setGravity(Gravity.CENTER);
//            timeView.setTypeface(null, Typeface.BOLD);
//
//            // Weather Info TextView (Below)
//            TextView infoView = new TextView(this);
//            infoView.setText(String.format("%.1f°C\n%s\n🌧️ %.1f%%", temp, hourlyCondition, rainChance));
//            infoView.setTextSize(14);
//            infoView.setGravity(Gravity.CENTER);
//
//            // Add views to layout
//            hourBlock.addView(timeView);
//            hourBlock.addView(infoView);
//
//            // Add to horizontal container
//            runOnUiThread(() -> hourlyContainer.addView(hourBlock));
//        }
//
//        // 📅 5-Day Forecast (Upcoming Forecast)
//        JSONArray daily = json.getJSONObject("forecast").getJSONArray("forecastday");
//
//        // Find the container for upcoming forecasts
//        LinearLayout upcomingContainer = findViewById(R.id.upcoming_forecast_container);
//
//        // Clear any old data (if refreshing)
//        runOnUiThread(() -> upcomingContainer.removeAllViews());
//
//        for (int i = 1; i < daily.length(); i++) { // Start from 1 (Skip today)
//            JSONObject day = daily.getJSONObject(i);
//
//            // Extract Date in "02 Feb, Sun" Format
//            long timeEpoch = day.getLong("date_epoch") * 1000L;
//            SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM, EEE", Locale.getDefault());
//            String formattedDate = dateFormat.format(new Date(timeEpoch));
//
//            // Extract Data
//            double maxTemp = day.getJSONObject("day").getDouble("maxtemp_c");
//            double minTemp = day.getJSONObject("day").getDouble("mintemp_c");
//            String dayCondition = day.getJSONObject("day").getJSONObject("condition").getString("text");
//            double rainChance = day.getJSONObject("day").getDouble("daily_chance_of_rain");
//
//            // Create a layout for each day (vertical)
//            LinearLayout dayBlock = new LinearLayout(this);
//            dayBlock.setOrientation(LinearLayout.VERTICAL);
//            dayBlock.setPadding(20, 10, 20, 10);
//
//            // Date TextView (Top)
//            TextView dateView = new TextView(this);
//            dateView.setText(formattedDate);
//            dateView.setTextSize(16);
//            dateView.setGravity(Gravity.CENTER);
//            dateView.setTypeface(null, Typeface.BOLD);
//
//            // Weather Info TextView (Below)
//            TextView dayInfoView = new TextView(this);
//            dayInfoView.setText(String.format("🌡️ %.1f°C / %.1f°C\n%s\n🌧️ %.1f%%", maxTemp, minTemp, dayCondition, rainChance));
//            dayInfoView.setTextSize(14);
//            dayInfoView.setGravity(Gravity.CENTER);
//
//            // Add views to layout
//            dayBlock.addView(dateView);
//            dayBlock.addView(dayInfoView);
//
//            // Add to horizontal container
//            runOnUiThread(() -> upcomingContainer.addView(dayBlock));
//        }
//    }


    // ✅ Helper Method to Log Errors & Show Toast Messages
    private void logError(String title, String message) {
        String logMsg = String.format("%s: %s\n", title, message);
        Log.e(TAG, logMsg);
        runOnUiThread(() -> {
            logView.append(logMsg);
            Toast.makeText(this, logMsg, Toast.LENGTH_LONG).show();
        });
    }

    private void updateUI(TextView textView, String data) {
        runOnUiThread(() -> textView.setText(data));
    }



    private final Handler handler = new Handler();
    private Runnable updateTimeRunnable;

    @Override
    protected void onResume() {
        super.onResume();
        startUpdatingTime();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopUpdatingTime();
    }

    private void startUpdatingTime() {
        updateTimeRunnable = new Runnable() {
            @Override
            public void run() {
                // Format date as "02 Feb, Sunday, 2025"
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM, EEEE, yyyy", Locale.getDefault());
                String formattedDate = dateFormat.format(new Date());

                // Format time as "HH:mm:ss" (24-hour format with seconds)
                SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
                String formattedTime = timeFormat.format(new Date());

                // Update UI
                runOnUiThread(() -> {
                    ((TextView) findViewById(R.id.current_date)).setText(formattedDate);
                    ((TextView) findViewById(R.id.current_time)).setText(formattedTime);
                });

                // Schedule the next update in 1 second
                handler.postDelayed(this, 1000);
            }
        };

        // Start the first execution
        handler.post(updateTimeRunnable);
    }

    private void stopUpdatingTime() {
        // Stop the updates when activity is paused
        handler.removeCallbacks(updateTimeRunnable);
    }



}
