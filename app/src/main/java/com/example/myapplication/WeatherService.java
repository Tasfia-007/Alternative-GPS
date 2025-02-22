package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class WeatherService {

    private static final String TAG = "WeatherService";
    private static final OkHttpClient client = new OkHttpClient();

    public static void fetchWeatherData(Context context, double latitude, double longitude, WeatherDataListener listener) {
        @SuppressLint("DefaultLocale")
        String apiUrl = String.format(
                "https://api.openweathermap.org/data/2.5/weather?lat=%f&lon=%f&appid=902d845de7f5de16ecc661bd23826128&units=metric",
                latitude, longitude
        );

        Log.d(TAG, "Requesting weather data: " + apiUrl);

        Request request = new Request.Builder().url(apiUrl).build();

        new Thread(() -> {
            try {
                Response response = client.newCall(request).execute();

                if (response.isSuccessful() && response.body() != null) {
                    String jsonResponse = response.body().string();
                    Log.d(TAG, "Weather API Response: " + jsonResponse);
                    listener.onWeatherDataFetched(jsonResponse);
                } else {
                    String errorMessage = "Failed to fetch weather data. Response code: " + response.code();
                    Log.e(TAG, errorMessage);
                    showToast(context, errorMessage);
                    listener.onError(errorMessage);
                }
            } catch (Exception e) {
                String errorMessage = "Error fetching weather data: " + e.getMessage();
                Log.e(TAG, errorMessage);
                showToast(context, errorMessage);
                listener.onError(errorMessage);
            }
        }).start();
    }

    // Callback interface for handling weather data responses
    public interface WeatherDataListener {
        void onWeatherDataFetched(String jsonResponse);
        void onError(String errorMessage);
    }

    // Method to safely show Toast messages from a background thread
    private static void showToast(Context context, String message) {
        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(context, message, Toast.LENGTH_LONG).show());
    }
}
