package com.example.myapplication;

import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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

public class WeatherByMinutesActivity extends AppCompatActivity {

    private static final String API_KEY = "PV5RTCQMTHUNRL9MCH9DKDQ32";  // Your Visual Crossing API Key
    private static final String TAG = "WeatherByMinutesActivity";
    private static final String BASE_URL = "https://weather.visualcrossing.com/VisualCrossingWebServices/rest/services/timeline/";

    private LinearLayout minutesForecastContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather_by_minutes);

        // Initialize UI
        minutesForecastContainer = findViewById(R.id.minutes_forecast_container);

        // Fetch the current location
        double latitude = getIntent().getDoubleExtra("latitude", 0.0);
        double longitude = getIntent().getDoubleExtra("longitude", 0.0);

        fetchMinuteByMinuteWeatherData(latitude, longitude);
    }

    private void fetchMinuteByMinuteWeatherData(double lat, double lon) {
        String url = String.format(Locale.US,
                "%s%f,%f?key=%s&contentType=json&unitGroup=metric&hours=24",
                BASE_URL, lat, lon, API_KEY);

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(WeatherByMinutesActivity.this, "Failed to load weather data", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String responseString = response.body().string();
                        JSONObject jsonResponse = new JSONObject(responseString);
                        parseMinuteByMinuteWeatherData(jsonResponse);
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON Parsing Error", e);
                    }
                } else {
                    Log.e(TAG, "Error fetching weather data");
                }
            }
        });
    }

    private void parseMinuteByMinuteWeatherData(JSONObject jsonResponse) throws JSONException {
        JSONArray hourly = jsonResponse.getJSONArray("days").getJSONObject(0).getJSONArray("hours");

        runOnUiThread(() -> minutesForecastContainer.removeAllViews());

        for (int i = 0; i < hourly.length(); i++) {
            JSONObject hourData = hourly.getJSONObject(i);

            String baseTime = hourData.getString("datetime"); // "00:00" or "01:00"
            String hourPart = baseTime.length() >= 2 ? baseTime.substring(0, 2) : "00";

            double temp = hourData.getDouble("temp");
            double feelsLike = hourData.getDouble("feelslike");
            double windSpeed = hourData.getDouble("windspeed");
            String condition = hourData.getString("conditions");
            double precipitation = hourData.getDouble("precip");
            double rainChance = hourData.getDouble("precipprob");

            // Simulate 3 entries: at 00 min, 20 min, 40 min
            String[] minutesOffsets = {"00", "20", "40"};

            for (String minuteOffset : minutesOffsets) {
                String simulatedTime = hourPart + ":" + minuteOffset;

                LinearLayout minuteBlock = new LinearLayout(this);
                minuteBlock.setOrientation(LinearLayout.HORIZONTAL);
                minuteBlock.setPadding(20, 10, 20, 10);

                // Add Time TextView
                TextView timeTextView = new TextView(this);
                timeTextView.setText(simulatedTime);
                timeTextView.setTextSize(16);
                timeTextView.setGravity(Gravity.START);
                minuteBlock.addView(timeTextView);

                // Temperature
                TextView tempTextView = new TextView(this);
                tempTextView.setText(String.format("%.1f°C", temp));
                tempTextView.setTextSize(16);
                tempTextView.setGravity(Gravity.START);
                minuteBlock.addView(tempTextView);

                // Feels Like
                TextView feelsLikeTextView = new TextView(this);
                feelsLikeTextView.setText(String.format("Feels: %.1f°C", feelsLike));
                feelsLikeTextView.setTextSize(16);
                feelsLikeTextView.setGravity(Gravity.START);
                minuteBlock.addView(feelsLikeTextView);

                // Condition
                TextView conditionTextView = new TextView(this);
                conditionTextView.setText(condition);
                conditionTextView.setTextSize(16);
                conditionTextView.setGravity(Gravity.START);
                minuteBlock.addView(conditionTextView);

                // Wind Speed
                TextView windTextView = new TextView(this);
                windTextView.setText(String.format("Wind: %.1f km/h", windSpeed));
                windTextView.setTextSize(16);
                windTextView.setGravity(Gravity.START);
                minuteBlock.addView(windTextView);

                // Precipitation
                TextView precipTextView = new TextView(this);
                precipTextView.setText(String.format("Precip: %.1f mm", precipitation));
                precipTextView.setTextSize(16);
                precipTextView.setGravity(Gravity.START);
                minuteBlock.addView(precipTextView);

                // Rain Chance
                TextView rainChanceTextView = new TextView(this);
                rainChanceTextView.setText(String.format("Rain: %.1f%%", rainChance));
                rainChanceTextView.setTextSize(16);
                rainChanceTextView.setGravity(Gravity.START);
                minuteBlock.addView(rainChanceTextView);

                // Add to container
                runOnUiThread(() -> minutesForecastContainer.addView(minuteBlock));
            }
        }
    }

}
