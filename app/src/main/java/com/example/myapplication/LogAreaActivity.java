package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class LogAreaActivity extends AppCompatActivity {
    private static final String TAG = "LogAreaActivity";

    private static final String SUPABASE_URL = "https://kquvuygavkhsxvdpqyfn.supabase.co"; // Replace with your Supabase URL
    private static final String SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImtxdXZ1eWdhdmtoc3h2ZHBxeWZuIiwicm9sZSI6ImFub24iLCJpYXQiOjE3MzcxMDQ4NjcsImV4cCI6MjA1MjY4MDg2N30.YVPKExfM-ZxzO9JvM9RQZQrBiyG1iT50fiwGUcvw8EI";


    private LinearLayout containerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundResource(R.drawable.alrt2);
        containerLayout = new LinearLayout(this);
        containerLayout.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(containerLayout);

        setContentView(scrollView);


        fetchRecentLogs();
    }

    private void fetchRecentLogs() {
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();

                // Today's and Yesterday's date
                LocalDate today = LocalDate.now();
                LocalDate yesterday = today.minusDays(1);
                DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;

                // Query URL to fetch only today and yesterday
                String url = SUPABASE_URL + "/rest/v1/water_logging_data" +
                        "?entry_time=gte." + yesterday.toString() + "T00:00:00" +
                        "&entry_time=lte." + today.plusDays(1).toString() + "T00:00:00" +
                        "&order=entry_time.desc";

                Request request = new Request.Builder()
                        .url(url)
                        .addHeader("apikey", SUPABASE_KEY)
                        .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                        .addHeader("Content-Type", "application/json")
                        .get()
                        .build();

                Response response = client.newCall(request).execute();
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    JSONArray jsonArray = new JSONArray(responseBody);

                    runOnUiThread(() -> displayLogs(jsonArray));
                } else {
                    runOnUiThread(() -> Toast.makeText(this, "Failed to fetch logs", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Error fetching logs", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void displayLogs(JSONArray logs) {
        for (int i = 0; i < logs.length(); i++) {
            try {
                JSONObject log = logs.getJSONObject(i);

                String entryTime = log.getString("entry_time");
                String placeName = log.getString("place_name");
                int waterLevel = log.getInt("water_level");
                String comment = log.optString("comment", ""); // optional
                String pictureUrl = log.optString("picture_url", null);

                // Create Card
                LinearLayout card = createCard(entryTime, placeName, waterLevel, comment, pictureUrl);
                containerLayout.addView(card);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private LinearLayout createCard(String entryTime, String placeName, int waterLevel, String comment, String pictureUrl) {
        Context context = this;

        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(20, 20, 20, 20);
        card.setLayoutParams(cardParams);
        card.setPadding(20, 20, 20, 20);
        card.setBackgroundResource(R.drawable.card_background); // rounded corner background

        // Left (Info)
        LinearLayout infoLayout = new LinearLayout(context);
        infoLayout.setOrientation(LinearLayout.VERTICAL);
        infoLayout.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2f));

        // ✅ Format entryTime nicely
        String formattedTime = formatEntryTime(entryTime);

        TextView timeText = new TextView(context);
        timeText.setText("Entry Time: " + formattedTime);
        infoLayout.addView(timeText);

        TextView placeText = new TextView(context);
        placeText.setText("Place: " + placeName);
        infoLayout.addView(placeText);

        TextView levelText = new TextView(context);
        levelText.setText("Water Level: " + waterLevel);
        infoLayout.addView(levelText);

        if (!TextUtils.isEmpty(comment)) {
            TextView commentText = new TextView(context);
            commentText.setText("Comment: " + comment);
            infoLayout.addView(commentText);
        }

        // Right (Image)
        // Right (Image)
        LinearLayout imageLayout = new LinearLayout(context);
        imageLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams imageLayoutParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        imageLayoutParams.setMargins(10, 10, 10, 10); // Small margins around image box
        imageLayout.setLayoutParams(imageLayoutParams);
        imageLayout.setBackgroundResource(R.drawable.image_background); // Rounded background for image box
        imageLayout.setPadding(10, 10, 10, 10); // Same top-bottom padding

        ImageView imageView = new ImageView(context);
        LinearLayout.LayoutParams imageViewParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 300);
        imageView.setLayoutParams(imageViewParams);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP); // Make image fit nicely

        if (pictureUrl != null && !pictureUrl.equals("null")) {
            new Thread(() -> {
                try {
                    URL url = new URL(pictureUrl);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setDoInput(true);
                    connection.connect();
                    InputStream input = connection.getInputStream();
                    Bitmap bitmap = BitmapFactory.decodeStream(input);
                    runOnUiThread(() -> imageView.setImageBitmap(bitmap));
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> imageView.setImageResource(R.drawable.no_image));
                }
            }).start();
        } else {
            imageView.setImageResource(R.drawable.no_image);
        }

        imageLayout.addView(imageView);

// Add both to card
        // Add the "Show in Map" button
        Button mapButton = new Button(context);
        mapButton.setText("Show in Map");
        mapButton.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        mapButton.setOnClickListener(v -> {
            Intent intent = new Intent(context, InMap.class);
            intent.putExtra("place_name", placeName); // You can also send coordinates if needed
            context.startActivity(intent);
        });

        infoLayout.addView(mapButton); // Add button to the info layout

        card.addView(infoLayout);
        card.addView(imageLayout);


        return card;
    }




    private String formatEntryTime(String isoTime) {
        try {
            java.time.LocalDateTime dateTime = java.time.LocalDateTime.parse(isoTime);
            java.time.format.DateTimeFormatter outputFormatter = java.time.format.DateTimeFormatter.ofPattern("hh:mm a, MM/dd/yyyy");
            return dateTime.format(outputFormatter);
        } catch (Exception e) {
            e.printStackTrace();
            return isoTime; // Fallback if error happens
        }
    }


}
