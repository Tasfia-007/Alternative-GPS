package com.example.myapplication;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.location.Location;
import android.os.AsyncTask;
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
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity {
    private static final String API_KEY = "PV5RTCQMTHUNRL9MCH9DKDQ32";  // Visual Crossing API Key
    private static final String TAG = "WeatherActivity";
    private FusedLocationProviderClient fusedLocationClient;
    private TextView currentWeather, logView;
    private final Handler handler = new Handler();
    private Runnable updateTimeRunnable;

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
                    parseWeatherData(jsonResponse);
                } catch (JSONException e) {
                    logError("JSON Parsing Error", e.getMessage());
                }
            }
        });
    }

    private void parseWeatherData(JSONObject json) throws JSONException {
        Log.d("WeatherActivity", "parseWeatherData called with JSON: " + json.toString());
        JSONObject current = json.getJSONObject("currentConditions");
        double temperature = current.getDouble("temp");
        String condition = current.getString("conditions");
        double windSpeed = current.getDouble("windspeed");
        int humidity = current.getInt("humidity");
        double precipitation = current.getDouble("precip");
        double feelsLike = current.getDouble("feelslike");

        double tempMax = json.getJSONArray("days").getJSONObject(0).getDouble("tempmax");
        double tempMin = json.getJSONArray("days").getJSONObject(0).getDouble("tempmin");

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
        runOnUiThread(() -> hourlyContainer.removeAllViews());

        StringBuilder hourlySummaryData = new StringBuilder();
        for (int i = 0; i < hourly.length(); i++) {
            JSONObject hour = hourly.getJSONObject(i);
            String time = hour.getString("datetime").substring(0, 5);
            double temp = hour.getDouble("temp");
            String hourlyCondition = hour.getString("conditions");
            double rainChance = hour.getDouble("precipprob");
            double precip = hour.getDouble("precip");

            hourlySummaryData.append(String.format(
                    "At %s: %.1f°C, %s, %.1f%% chance of rain, %.1f mm precipitation\n",
                    time, temp, hourlyCondition, rainChance, precip
            ));

            LinearLayout hourBlock = new LinearLayout(this);
            hourBlock.setOrientation(LinearLayout.VERTICAL);
            hourBlock.setPadding(20, 10, 20, 10);

            TextView timeView = new TextView(this);
            timeView.setText(time);
            timeView.setTextSize(16);
            timeView.setGravity(Gravity.CENTER);
            timeView.setTypeface(null, Typeface.BOLD);

            TextView infoView = new TextView(this);
            infoView.setText(String.format(
                    "%.1f°C\n%s\n🌧️ %.1f%%\n💧 %.1f mm",
                    temp, hourlyCondition, rainChance, precip
            ));
            infoView.setTextSize(14);
            infoView.setGravity(Gravity.CENTER);

            hourBlock.addView(timeView);
            hourBlock.addView(infoView);

            runOnUiThread(() -> hourlyContainer.addView(hourBlock));
        }

        Log.d("HourlySummary", hourlySummaryData.toString());

        // Call OpenAI API to summarize the hourly data
        new SummarizeHourlyDataTask().execute(hourlySummaryData.toString());
        JSONArray daily = json.getJSONArray("days");
        LinearLayout upcomingContainer = findViewById(R.id.upcoming_forecast_container);
        runOnUiThread(() -> upcomingContainer.removeAllViews());

        StringBuilder upcomingSummaryData = new StringBuilder();
        for (int i = 1; i < daily.length(); i++) {
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

            runOnUiThread(() -> upcomingContainer.addView(dayBlock));
        }

        // Call the SummarizeUpcomingDataTask to summarize the upcoming forecast
        new SummarizeUpcomingDataTask().execute(upcomingSummaryData.toString());
    }
    private class SummarizeHourlyDataTask extends AsyncTask<String, Void, String> {
        private final String GEMINI_API_KEY = "AIzaSyDlMBR9V4KtgIkQRrC7VZkAEgbguhm2jmI";
        private final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + GEMINI_API_KEY;

        @Override
        protected String doInBackground(String... params) {
            String hourlyData = params[0];
            OkHttpClient client = new OkHttpClient();

            String prompt = "Summarize the following upcoming weather forecast in 3-4 lines but as a paragraph and dont write anything like(3-4 lines precise bla bla)write in a humanize and easily so that the user may get a good idea about the weather in his area.One more thing..your description will show the user...so i just need the description in paragraph...not astric sign no intro and conclusion just description\n" + hourlyData;

            JSONObject requestBody = new JSONObject();
            try {
                // Gemini API এর জন্য সঠিক ফরম্যাট
                JSONArray contents = new JSONArray();
                JSONObject content = new JSONObject();
                content.put("parts", new JSONArray().put(new JSONObject().put("text", prompt)));
                contents.put(content);

                requestBody.put("contents", contents);
                requestBody.put("generationConfig", new JSONObject().put("maxOutputTokens", 200));
            } catch (JSONException e) {
                e.printStackTrace();
                return "Error: JSON creation failed.";
            }

            RequestBody body = RequestBody.create(
                    MediaType.parse("application/json"),
                    requestBody.toString()
            );

            Request request = new Request.Builder()
                    .url(GEMINI_API_URL)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    Log.d("API_RESPONSE", responseBody);

                    JSONObject jsonResponse = new JSONObject(responseBody);
                    // Gemini API-র সঠিক পার্সিং
                    return jsonResponse.getJSONArray("candidates")
                            .getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .getString("text");
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "No error body";
                    Log.e("API_ERROR", "Error response: " + errorBody);
                    return "Error: Response Code " + response.code();
                }
            } catch (Exception e) {
                e.printStackTrace();
                return "Error: " + e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(String summary) {
            super.onPostExecute(summary);
            Log.d("WeatherActivity", "Summary: " + summary);
            updateUI(findViewById(R.id.hourly_description), summary);
        }
    }
    private class SummarizeUpcomingDataTask extends AsyncTask<String, Void, String> {
        private final String GEMINI_API_KEY = "AIzaSyDlMBR9V4KtgIkQRrC7VZkAEgbguhm2jmI";
        private final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + GEMINI_API_KEY;

        @Override
        protected String doInBackground(String... params) {
            String upcomingData = params[0];
            OkHttpClient client = new OkHttpClient();

            String prompt = "Summarize the following upcoming weather forecast in 3-4 lines:\n" + upcomingData;

            JSONObject requestBody = new JSONObject();
            try {
                // Gemini API এর জন্য সঠিক রিকোয়েস্ট ফরম্যাট
                JSONArray contents = new JSONArray();
                JSONObject content = new JSONObject();
                content.put("parts", new JSONArray().put(new JSONObject().put("text", prompt)));
                contents.put(content);

                requestBody.put("contents", contents);
                requestBody.put("generationConfig", new JSONObject().put("maxOutputTokens", 200));
            } catch (JSONException e) {
                e.printStackTrace();
                return "Error: JSON creation failed.";
            }

            RequestBody body = RequestBody.create(
                    MediaType.parse("application/json"),
                    requestBody.toString()
            );

            Request request = new Request.Builder()
                    .url(GEMINI_API_URL)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    Log.d("API_RESPONSE", responseBody); // সঠিক রেসপন্স লগ করা

                    JSONObject jsonResponse = new JSONObject(responseBody);
                    // Gemini এর জন্য সঠিক JSON পার্সিং
                    return jsonResponse.getJSONArray("candidates")
                            .getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .getString("text");
                } else {
                    String errorBody = response.body().string();
                    Log.e("API_ERROR", "Error response: " + errorBody);
                    return "Error: Response Code " + response.code();
                }
            } catch (Exception e) {
                e.printStackTrace();
                return "Error: " + e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(String summary) {
            super.onPostExecute(summary);
            Log.d("WeatherActivity", "Summary: " + summary);
            generateWeatherSummary("Upcoming Forecast", summary, findViewById(R.id.upcoming_description));
        }
    }


    private void generateWeatherSummary(String forecastType, String forecastData, TextView targetView) {
        OkHttpClient client = new OkHttpClient();
        String GEMINI_API_KEY = "AIzaSyDlMBR9V4KtgIkQRrC7VZkAEgbguhm2jmI";
        String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + GEMINI_API_KEY;

        JSONObject requestBody = new JSONObject();
        Log.d(TAG, "Calling Gemini API to generate summary...");

        try {
            // Gemini API ফরম্যাট
            JSONArray contents = new JSONArray();
            JSONObject content = new JSONObject();
            content.put("parts", new JSONArray().put(new JSONObject().put("text",
                    String.format("Provide a short weather summary for this %s in 3-4 lines:\n%s", forecastType, forecastData))));
            contents.put(content);

            requestBody.put("contents", contents);
            requestBody.put("generationConfig", new JSONObject().put("maxOutputTokens", 150));
        } catch (JSONException e) {
            logError("JSON Error", e.getMessage());
            return;
        }

        RequestBody body = RequestBody.create(requestBody.toString(), MediaType.parse("application/json"));

        Request request = new Request.Builder()
                .url(GEMINI_API_URL)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                logError("Gemini API Failure", e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "No error body";
                    logError("Gemini API Error", "Response code: " + response.code() + ", Body: " + errorBody);
                    return;
                }

                try {
                    String responseString = response.body().string();
                    Log.d("API_RESPONSE", responseString);

                    JSONObject jsonResponse = new JSONObject(responseString);
                    // Gemini API-র জন্য সঠিক পার্সিং
                    String summary = jsonResponse.getJSONArray("candidates")
                            .getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .getString("text");

                    updateUI(targetView, summary);
                } catch (JSONException e) {
                    logError("Parsing Error", e.getMessage());
                }
            }
        });
    }


    private void logError(String title, String message) {
        String logMsg = title + ": " + message + "\n";
        Log.e(TAG, logMsg);
        runOnUiThread(() -> {
            if (logView != null) {
                logView.append(logMsg);
            } else {
                Toast.makeText(this, logMsg, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void updateUI(TextView textView, String data) {
        runOnUiThread(() -> textView.setText(data));
    }

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
            String date = new SimpleDateFormat("dd MMM, EEEE, yyyy", Locale.getDefault()).format(new Date());
            String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());

            runOnUiThread(() -> {
                ((TextView) findViewById(R.id.current_date)).setText(date);
                ((TextView) findViewById(R.id.current_time)).setText(time);
            });

            handler.postDelayed(updateTimeRunnable, 1000);
        };
        handler.post(updateTimeRunnable);
    }

    private void stopUpdatingTime() {
        handler.removeCallbacks(updateTimeRunnable);
    }
}
