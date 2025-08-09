package com.example.myapplication;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polygon;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class InMap extends AppCompatActivity {

    private static final String TAG = "InMap";
    private static final String SUPABASE_URL = "https://kquvuygavkhsxvdpqyfn.supabase.co"; // Replace with your Supabase URL
    private static final String SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImtxdXZ1eWdhdmtoc3h2ZHBxeWZuIiwicm9sZSI6ImFub24iLCJpYXQiOjE3MzcxMDQ4NjcsImV4cCI6MjA1MjY4MDg2N30.YVPKExfM-ZxzO9JvM9RQZQrBiyG1iT50fiwGUcvw8EI";

    private MapView mapView;
    private List<Polygon> polygonList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().setUserAgentValue(getPackageName());
        setContentView(R.layout.activity_in_map); // Create a layout with MapView having id `mapview`

        mapView = findViewById(R.id.mapview);
        mapView.setMultiTouchControls(true);

        IMapController mapController = mapView.getController();
        mapController.setZoom(12.0);
        mapController.setCenter(new GeoPoint(23.8103, 90.4125)); // Dhaka

        fetchPolygonData();
    }

    private void fetchPolygonData() {
        OkHttpClient client = new OkHttpClient();
        String url = SUPABASE_URL + "/rest/v1/water_logging_data";

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                .addHeader("Accept", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Error fetching data: " + e.getMessage());
            }


            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) return;
                String jsonData = response.body().string();

                Log.d(TAG, "Supabase response:\n" + jsonData);  // ← ADD THIS

                runOnUiThread(() -> {
                    try {
                        parseAndShowPolygons(jsonData);
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON parse error: " + e.getMessage());
                    }
                });
            }

        });
    }

    private void parseAndShowPolygons(String jsonData) throws JSONException {
        JSONArray jsonArray = new JSONArray(jsonData);

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject polygonObj = jsonArray.getJSONObject(i);

            double lat1 = polygonObj.getDouble("point1_lat");
            double lon1 = polygonObj.getDouble("point1_lon");
            double lat2 = polygonObj.getDouble("point2_lat");
            double lon2 = polygonObj.getDouble("point2_lon");
            double lat3 = polygonObj.getDouble("point3_lat");
            double lon3 = polygonObj.getDouble("point3_lon");
            double lat4 = polygonObj.getDouble("point4_lat");
            double lon4 = polygonObj.getDouble("point4_lon");


            float waterLevel = (float) polygonObj.getDouble("water_level");
            String comment = polygonObj.optString("comment", "No comment");

            List<GeoPoint> points = new ArrayList<>();
            points.add(new GeoPoint(lat1, lon1));
            points.add(new GeoPoint(lat2, lon2));
            points.add(new GeoPoint(lat3, lon3));
            points.add(new GeoPoint(lat4, lon4));
            points.add(new GeoPoint(lat1, lon1)); // Close the loop

            Polygon polygon = new Polygon();
            polygon.setPoints(points);
            polygon.setFillColor(getColorForLevel(waterLevel));
            polygon.setStrokeColor(Color.BLACK);
            polygon.setStrokeWidth(2.0f);

            int finalI = i;
            polygon.setOnClickListener((poly, mapView, eventPos) -> {
                showPolygonDialog(waterLevel, comment, finalI + 1);
                return true;
            });

            polygonList.add(polygon);
            mapView.getOverlays().add(polygon);
        }

        mapView.invalidate();
    }

    private void showPolygonDialog(float waterLevel, String comment, int polygonNum) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Polygon #" + polygonNum);
        builder.setMessage("💧 Water Level: " + getWaterLevelDescription((int) waterLevel)
                + "\n💬 Comment: " + comment);
        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private int getColorForLevel(float level) {
        switch ((int) level) {
            case 1:
                return Color.argb(100, 255, 255, 0);   // Yellow
            case 2:
                return Color.argb(100, 0, 0, 255);     // Blue
            case 3:
                return Color.argb(100, 255, 0, 0);     // Red
            case 4:
                return Color.argb(100, 139, 69, 19);   // Brown
            default:
                return Color.argb(100, 150, 150, 150); // Gray
        }
    }

    private String getWaterLevelDescription(int level) {
        switch (level) {
            case 1:
                return "Not much water:Level 1";
            case 2:
                return "Significant water accumulation:Level 2";
            case 3:
                return "High water level - caution advised:Level 3";
            case 4:
                return "Very high water level - try to avoid this road:Level 4";
            default:
                return "Unknown water level";
        }
    }
}
