package com.example.myapplication;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import org.json.JSONArray;
import org.json.JSONObject;

public class RoutingActivity extends AppCompatActivity {

    private EditText fromLocation;
    private EditText toLocation;
    private Button getRouteButton;
    private MapView mapView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_routing);

        // Initialize Views
        fromLocation = findViewById(R.id.from_location);
        toLocation = findViewById(R.id.to_location);
        getRouteButton = findViewById(R.id.get_route_button);
        mapView = findViewById(R.id.mapview);

        // Configure MapView
        Configuration.getInstance().setUserAgentValue(getApplicationContext().getPackageName());
        mapView.setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        IMapController mapController = mapView.getController();
        mapController.setZoom(10.0);
        mapController.setCenter(new GeoPoint(23.8103, 90.4125)); // Default to Dhaka

        // Set Click Listener for Get Route Button
        getRouteButton.setOnClickListener(v -> {
            String from = fromLocation.getText().toString().trim();
            String to = toLocation.getText().toString().trim();

            if (from.isEmpty() || to.isEmpty()) {
                Toast.makeText(this, "Please enter both locations", Toast.LENGTH_SHORT).show();
                return;
            }

            // Fetch coordinates for 'From' and 'To' locations and display the route
            fetchCoordinatesAndDisplayRoute(from, to);
        });
    }

    private void fetchCoordinatesAndDisplayRoute(String from, String to) {
        new Thread(() -> {
            try {
                GeoPoint fromPoint = geocodeLocation(from);
                GeoPoint toPoint = geocodeLocation(to);

                if (fromPoint != null && toPoint != null) {
                    fetchRoute(fromPoint, toPoint);
                } else {
                    runOnUiThread(() -> Toast.makeText(this, "Failed to fetch coordinates.", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private GeoPoint geocodeLocation(String location) {
        try {
            String url = "https://nominatim.openstreetmap.org/search?q=" + location
                    + "&format=json&addressdetails=1&countrycodes=bd"; // Restrict to Bangladesh
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder().url(url).build();
            Response response = client.newCall(request).execute();

            if (response.isSuccessful() && response.body() != null) {
                JSONArray jsonArray = new JSONArray(response.body().string());
                if (jsonArray.length() > 0) {
                    JSONObject jsonObject = jsonArray.getJSONObject(0);
                    double lat = jsonObject.getDouble("lat");
                    double lon = jsonObject.getDouble("lon");
                    return new GeoPoint(lat, lon);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null; // Return null if geocoding fails
    }


    private void fetchRoute(GeoPoint fromPoint, GeoPoint toPoint) {
        String osrmUrl = "http://router.project-osrm.org/route/v1/driving/"
                + fromPoint.getLongitude() + "," + fromPoint.getLatitude() + ";"
                + toPoint.getLongitude() + "," + toPoint.getLatitude() + "?overview=full&geometries=geojson";

        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder().url(osrmUrl).build();
                Response response = client.newCall(request).execute();

                if (response.isSuccessful() && response.body() != null) {
                    String jsonResponse = response.body().string();
                    runOnUiThread(() -> displayRoute(jsonResponse, fromPoint, toPoint));
                } else {
                    runOnUiThread(() -> Toast.makeText(this, "Failed to fetch route", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Error fetching route: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void displayRoute(String jsonResponse, GeoPoint fromPoint, GeoPoint toPoint) {
        try {
            // Clear existing overlays
            mapView.getOverlays().clear();

            JSONObject jsonObject = new JSONObject(jsonResponse);
            JSONArray coordinates = jsonObject.getJSONArray("routes")
                    .getJSONObject(0)
                    .getJSONObject("geometry")
                    .getJSONArray("coordinates");

            List<GeoPoint> geoPoints = new ArrayList<>();
            for (int i = 0; i < coordinates.length(); i++) {
                JSONArray coord = coordinates.getJSONArray(i);
                double lon = coord.getDouble(0);
                double lat = coord.getDouble(1);
                geoPoints.add(new GeoPoint(lat, lon));
            }

            // Add markers for start and end points
            Marker startMarker = new Marker(mapView);
            startMarker.setPosition(fromPoint);
            startMarker.setTitle("Start: " + fromLocation.getText().toString());
            startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            mapView.getOverlays().add(startMarker);

            Marker endMarker = new Marker(mapView);
            endMarker.setPosition(toPoint);
            endMarker.setTitle("End: " + toLocation.getText().toString());
            endMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            mapView.getOverlays().add(endMarker);

            // Draw the route as a polyline
            Polyline polyline = new Polyline();
            polyline.setPoints(geoPoints);
            polyline.setColor(getResources().getColor(android.R.color.holo_blue_dark));
            polyline.setWidth(7.0f);
            mapView.getOverlays().add(polyline);

            mapView.invalidate();
            Toast.makeText(this, "Route displayed on the map", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error displaying route: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

}
