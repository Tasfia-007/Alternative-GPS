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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity {

    private static final String API_KEY = "PV5RTCQMTHUNRL9MCH9DKDQ32";
    private static final String TAG = "WeatherActivity";
    private FusedLocationProviderClient fusedLocationClient;
    private TextView currentWeather, logView;
    private boolean cleanupDoneToday = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);

        currentWeather = findViewById(R.id.current_weather);
        logView = findViewById(R.id.logView);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        getCurrentLocation();
        startUpdatingTime();
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                fetchWeatherData(location.getLatitude(), location.getLongitude());
            } else {
                logError("Location Error", "Failed to fetch location.");
            }
        }).addOnFailureListener(e -> logError("Location Failure", e.getMessage()));
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
                logError("API Failure", e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    logError("API Response Error", "Response code: " + response.code());
                    return;
                }
                try {
                    String responseString = response.body().string();
                    JSONObject jsonResponse = new JSONObject(responseString);

                    // 🧹 Clean up yesterday's data first (only once)
                    checkAndCleanupPreviousDay();

                    // ✅ Now parse and save today's weather data
                    runOnUiThread(() -> {
                        try {
                            parseWeatherData(jsonResponse);

                            // ➡️ After successful parsing and UI update
                            Log.d("WeatherData", "✅ Weather data updated!");
                            Toast.makeText(WeatherActivity.this, "✅ Weather data updated!", Toast.LENGTH_SHORT).show();

                        } catch (JSONException e) {
                            logError("JSON Parse", e.getMessage());
                        }
                    });

                } catch (JSONException e) {
                    logError("JSON Parsing Error", e.getMessage());
                }
            }
        });
    }

    private void parseWeatherData(JSONObject json) throws JSONException {
        JSONObject current = json.getJSONObject("currentConditions");

        double temperature = current.getDouble("temp");
        String condition = current.getString("conditions");
        double windSpeed = current.getDouble("windspeed");
        int humidity = current.getInt("humidity");
        double precipitation = current.getDouble("precip");

        double tempMax = json.getJSONArray("days").getJSONObject(0).getDouble("tempmax");
        double tempMin = json.getJSONArray("days").getJSONObject(0).getDouble("tempmin");
        double feelsLike = current.getDouble("feelslike");

        updateUI(findViewById(R.id.current_weather), String.format("🌡️ %.1f°C", temperature));
        updateUI(findViewById(R.id.weather_condition), String.format("Condition: %s", condition));
        updateUI(findViewById(R.id.wind_info), String.format("💨 Wind: %.1f km/h", windSpeed));
        updateUI(findViewById(R.id.humidity_info), String.format("💧 Humidity: %d%%", humidity));
        updateUI(findViewById(R.id.precipitation_info), String.format("🌧️ Precipitation: %.1f mm", precipitation));
        updateUI(findViewById(R.id.tempmax_info), String.format("Max Temp: %.1f°C", tempMax));
        updateUI(findViewById(R.id.tempmin_info), String.format("Min Temp: %.1f°C", tempMin));
        updateUI(findViewById(R.id.feelslike_info), String.format("Feels Like: %.1f°C", feelsLike));

        JSONArray hourly = json.getJSONArray("days").getJSONObject(0).getJSONArray("hours");

        LinearLayout hourlyContainer = findViewById(R.id.hourly_forecast_container);
        runOnUiThread(hourlyContainer::removeAllViews);

        for (int i = 0; i < hourly.length(); i++) {
            JSONObject hour = hourly.getJSONObject(i);
            String time = hour.getString("datetime").substring(0, 5);
            double temp = hour.getDouble("temp");
            String conditionHour = hour.getString("conditions");
            double rainChance = hour.getDouble("precipprob");
            double precip = hour.getDouble("precip");

            LinearLayout hourBlock = new LinearLayout(this);
            hourBlock.setOrientation(LinearLayout.VERTICAL);
            hourBlock.setPadding(20, 10, 20, 10);

            TextView timeView = new TextView(this);
            timeView.setText(time);
            timeView.setTextSize(16);
            timeView.setGravity(Gravity.CENTER);
            timeView.setTypeface(null, Typeface.BOLD);

            TextView infoView = new TextView(this);
            infoView.setText(String.format("%.1f°C\n%s\n🌧️ %.1f%%\n💧 %.1f mm", temp, conditionHour, rainChance, precip));
            infoView.setTextSize(14);
            infoView.setGravity(Gravity.CENTER);

            hourBlock.addView(timeView);
            hourBlock.addView(infoView);

            runOnUiThread(() -> hourlyContainer.addView(hourBlock));
        }

        showUpcoming5DayForecast(json);
    }

    private void checkAndCleanupPreviousDay() {
        if (cleanupDoneToday) return;

        // Cleanup logic here...
        // Set cleanupDoneToday to true once cleanup is done
    }

    private void logError(String title, String message) {
        String logMsg = title + ": " + message;
        Log.e(TAG, logMsg);
        runOnUiThread(() -> {
            if (logView != null) {
                logView.append(logMsg + "\n");
            } else {
                Toast.makeText(this, logMsg, Toast.LENGTH_LONG).show();
            }
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
        updateTimeRunnable = () -> {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM, EEEE, yyyy", Locale.getDefault());
            String date = dateFormat.format(new Date());

            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
            String time = timeFormat.format(new Date());

            runOnUiThread(() -> {
                ((TextView) findViewById(R.id.current_date)).setText(date);
                ((TextView) findViewById(R.id.current_time)).setText(time);
            });

            handler.postDelayed(this.updateTimeRunnable, 1000);
        };

        handler.post(updateTimeRunnable);
    }

    private void stopUpdatingTime() {
        handler.removeCallbacks(updateTimeRunnable);
    }


    private void showUpcoming5DayForecast(JSONObject json) throws JSONException {
        JSONArray daily = json.getJSONArray("days");
        LinearLayout upcomingContainer = findViewById(R.id.upcoming_forecast_container);
        runOnUiThread(() -> upcomingContainer.removeAllViews());

        StringBuilder upcomingSummaryData = new StringBuilder();
        for (int i = 1; i < daily.length(); i++) {  // Start from 1 to avoid the current day
            JSONObject day = daily.getJSONObject(i);
            long timeEpoch = day.getLong("datetimeEpoch") * 1000L;
            String formattedDate = new SimpleDateFormat("dd MMM, EEE", Locale.getDefault()).format(new Date(timeEpoch));
            double currentTemp = day.getDouble("temp");
            double maxTemp = day.getDouble("tempmax");
            double minTemp = day.getDouble("tempmin");
            String dayCondition = day.getString("conditions");
            double rainChance = day.getDouble("precipprob");
            double dailyPrecipitation = day.getDouble("precip");

            upcomingSummaryData.append(String.format("On %s: %.1f°C, %.1f°C/%.1f°C, %s, %.1f%% rain, %.1f mm precipitation\n",
                    formattedDate, currentTemp, maxTemp, minTemp, dayCondition, rainChance, dailyPrecipitation));

            // Creating UI for each day's forecast
            LinearLayout dayBlock = new LinearLayout(this);
            dayBlock.setOrientation(LinearLayout.VERTICAL);
            dayBlock.setPadding(20, 10, 20, 10);

            TextView dateView = new TextView(this);
            dateView.setText(formattedDate);
            dateView.setTextSize(16);
            dateView.setGravity(Gravity.CENTER);
            dateView.setTypeface(null, Typeface.BOLD);

            TextView dayInfoView = new TextView(this);
            dayInfoView.setText(String.format("🌡️ %.1f°C\n%.1f°C/%.1f°C\n%s\n🌧️ %.1f%%\n💧 %.1f mm",
                    currentTemp, maxTemp, minTemp, dayCondition, rainChance, dailyPrecipitation));
            dayInfoView.setTextSize(14);
            dayInfoView.setGravity(Gravity.CENTER);

            dayBlock.addView(dateView);
            dayBlock.addView(dayInfoView);

            // Update the UI with upcoming forecast
            runOnUiThread(() -> upcomingContainer.addView(dayBlock));
        }
    }

}
