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
    private static final String API_KEY = "PV5RTCQMTHUNRL9MCH9DKDQ32";  // Your Visual Crossing API Key
    private static final String TAG = "WeatherActivity";
    private FusedLocationProviderClient fusedLocationClient;
    private TextView currentWeather, dailyWeather, fullHourlyWeather, logView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);

        // Initialize TextViews
        currentWeather = findViewById(R.id.current_weather);
        logView = findViewById(R.id.logView);  // Ensure that this TextView exists in your layout

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
                    // Log the entire JSON response for inspection
                    String responseString = response.body().string();
                    Log.d(TAG, "API Response: " + responseString);  // Log the full JSON response

                    JSONObject jsonResponse = new JSONObject(responseString);
                    parseWeatherData(jsonResponse);
                } catch (JSONException e) {
                    logError("JSON Parsing Error", e.getMessage());
                }
            }
        });
    }

    private void parseWeatherData(JSONObject json) throws JSONException {
        // Log the JSON structure to inspect the response
        Log.d(TAG, "Full JSON Response: " + json.toString());

        // Extract current weather data
        JSONObject current = json.getJSONObject("currentConditions");

        double temperature = current.getDouble("temp");
        String condition = current.getString("conditions");

        double windSpeed = current.getDouble("windspeed");
        int humidity = current.getInt("humidity");
        double precipitation = current.getDouble("precip");

        // Max and Min temperatures
        double tempMax = json.getJSONArray("days").getJSONObject(0).getDouble("tempmax");
        double tempMin = json.getJSONArray("days").getJSONObject(0).getDouble("tempmin");
        double feelsLike = current.getDouble("feelslike");

        // Update individual UI elements
        updateUI(findViewById(R.id.current_weather), String.format("🌡️ %.1f°C", temperature));
        updateUI(findViewById(R.id.weather_condition), String.format("Condition: %s", condition));
        updateUI(findViewById(R.id.wind_info), String.format("💨 Wind: %.1f km/h", windSpeed));
        updateUI(findViewById(R.id.humidity_info), String.format("💧 Humidity: %d%%", humidity));
        updateUI(findViewById(R.id.precipitation_info), String.format("🌧️ Precipitation: %.1f mm", precipitation));

        // Fill in the new TextViews for tempmax, tempmin, feelslike
        updateUI(findViewById(R.id.tempmax_info), String.format("Max Temp: %.1f°C", tempMax));
        updateUI(findViewById(R.id.tempmin_info), String.format("Min Temp: %.1f°C", tempMin));
        updateUI(findViewById(R.id.feelslike_info), String.format("Feels Like: %.1f°C", feelsLike));

        // 🌧️ Full Hourly Forecast (24 Hours)
        JSONArray hourly = json.getJSONArray("days").getJSONObject(0).getJSONArray("hours");

        // Find the container for hourly forecasts
        LinearLayout hourlyContainer = findViewById(R.id.hourly_forecast_container);
        runOnUiThread(() -> hourlyContainer.removeAllViews());

        for (int i = 0; i < hourly.length(); i++) {
            JSONObject hour = hourly.getJSONObject(i);

            // Extract data
            String time = hour.getString("datetime");
            if (time.length() >= 5) {
                time = time.substring(0, 5); // Extract "HH:mm" format
            } else {
                time = "Unknown Time"; // Default value
            }

            double temp = hour.getDouble("temp");
            String hourlyCondition = hour.getString("conditions");
            double rainChance = hour.getDouble("precipprob");

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
            timeView.setTextColor(getResources().getColor(android.R.color.black));  // Set time text color to black

            // Weather Info TextView (Below)
            TextView infoView = new TextView(this);
            infoView.setText(String.format("%.1f°C\n%s\n🌧️ %.1f%% chance of rain\n💧 %.1f mm precipitation",
                    temp, hourlyCondition, rainChance, hour.getDouble("precip")));
            infoView.setTextSize(14);
            infoView.setGravity(Gravity.CENTER);
            infoView.setTextColor(getResources().getColor(android.R.color.black));  // Set weather info text color to black

            // Add views to layout
            hourBlock.addView(timeView);
            hourBlock.addView(infoView);

            // Add to container
            runOnUiThread(() -> hourlyContainer.addView(hourBlock));
        }

        // 📅 5-Day Forecast (Upcoming Forecast)
        JSONArray daily = json.getJSONArray("days");

        LinearLayout upcomingContainer = findViewById(R.id.upcoming_forecast_container);
        runOnUiThread(() -> upcomingContainer.removeAllViews());

        for (int i = 1; i < daily.length(); i++) { // Skip today's weather (i = 1)
            JSONObject day = daily.getJSONObject(i);

            long timeEpoch = day.getLong("datetimeEpoch") * 1000L;
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM, EEE", Locale.getDefault());
            String formattedDate = dateFormat.format(new Date(timeEpoch));

            // ✅ NEW: Fetch current temperature for the day
            double currentTemp = day.getDouble("temp");

            double maxTemp = day.getDouble("tempmax");
            double minTemp = day.getDouble("tempmin");
            String dayCondition = day.getString("conditions");
            double rainChance = day.getDouble("precipprob");
            double dailyPrecipitation = day.getDouble("precip");

            LinearLayout dayBlock = new LinearLayout(this);
            dayBlock.setOrientation(LinearLayout.VERTICAL);
            dayBlock.setPadding(20, 10, 20, 10);

            TextView dateView = new TextView(this);
            dateView.setText(formattedDate);
            dateView.setTextSize(16);
            dateView.setGravity(Gravity.CENTER);
            dateView.setTypeface(null, Typeface.BOLD);
            dateView.setTextColor(getResources().getColor(android.R.color.black));  // Set date text color to black

            TextView dayInfoView = new TextView(this);
            dayInfoView.setText(String.format(
                    "🌡️ %.1f°C\n%.1f°C/%.1f°C\n%s\n🌧️ %.1f%% chance of rain\n💧 %.1f mm precipitation",
                    currentTemp, maxTemp, minTemp, dayCondition, rainChance, dailyPrecipitation));

            dayInfoView.setTextSize(14);
            dayInfoView.setGravity(Gravity.CENTER);
            dayInfoView.setTextColor(getResources().getColor(android.R.color.black));  // Set day info text color to black

            dayBlock.addView(dateView);
            dayBlock.addView(dayInfoView);

            runOnUiThread(() -> upcomingContainer.addView(dayBlock));
        }
    }

    private void logError(String title, String message) {
        String logMsg = String.format("%s: %s\n", title, message);
        Log.e(TAG, logMsg);
        runOnUiThread(() -> {
            if (logView != null) {
                logView.append(logMsg);
                logView.setTextColor(getResources().getColor(android.R.color.black));  // Set text color to black
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
        updateTimeRunnable = new Runnable() {
            @Override
            public void run() {
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM, EEEE, yyyy", Locale.getDefault());
                String formattedDate = dateFormat.format(new Date());

                SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
                String formattedTime = timeFormat.format(new Date());

                runOnUiThread(() -> {
                    ((TextView) findViewById(R.id.current_date)).setText(formattedDate);
                    ((TextView) findViewById(R.id.current_time)).setText(formattedTime);
                });

                handler.postDelayed(this, 1000);
            }
        };

        handler.post(updateTimeRunnable);
    }

    private void stopUpdatingTime() {
        handler.removeCallbacks(updateTimeRunnable);
    }
}
