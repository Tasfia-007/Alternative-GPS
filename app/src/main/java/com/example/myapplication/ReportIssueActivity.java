package com.example.myapplication;

import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ReportIssueActivity extends AppCompatActivity {

    private MapView mapView;
    private GeoPoint firstLocation = null;
    private GeoPoint secondLocation = null;
    private GestureDetector gestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_routing);

        // Initialize MapView
        mapView = findViewById(R.id.mapview);
        Configuration.getInstance().setUserAgentValue(getApplicationContext().getPackageName());
        mapView.setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        IMapController mapController = mapView.getController();
        mapController.setZoom(10.0);
        mapController.setCenter(new GeoPoint(23.8103, 90.4125)); // Default to Dhaka

        // Initialize GestureDetector
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                GeoPoint point = (GeoPoint) mapView.getProjection().fromPixels((int) e.getX(), (int) e.getY());

                // Handle first location
                if (firstLocation == null) {
                    firstLocation = point;
                    placeMarker(point, "First Location");
                    Toast.makeText(ReportIssueActivity.this, "First location selected", Toast.LENGTH_SHORT).show();
                }
                // Handle second location
                else if (secondLocation == null) {
                    secondLocation = point;
                    placeMarker(point, "Second Location");
                    Toast.makeText(ReportIssueActivity.this, "Second location selected", Toast.LENGTH_SHORT).show();

                    // Fetch nearest points on the road from OSRM
                    fetchNearestCoordinatesAndRoute(firstLocation, secondLocation);
                }
                return true;
            }
        });

        // Set onTouchListener to capture the touch events on the map
        mapView.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));
    }

    private void placeMarker(GeoPoint point, String title) {
        Marker marker = new Marker(mapView);
        marker.setPosition(point);
        marker.setTitle(title);
        mapView.getOverlays().add(marker);
        mapView.invalidate(); // Refresh the map
    }

    private void fetchNearestCoordinatesAndRoute(GeoPoint from, GeoPoint to) {
        new Thread(() -> {
            try {
                // Fetch nearest point on the road for both locations using OSRM
                GeoPoint nearestFrom = getNearestPointOnRoad(from);
                GeoPoint nearestTo = getNearestPointOnRoad(to);

                if (nearestFrom != null && nearestTo != null) {
                    fetchRoute(nearestFrom, nearestTo); // Fetch route with snapped points
                } else {
                    runOnUiThread(() -> Toast.makeText(this, "Failed to fetch nearest points on road.", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private GeoPoint getNearestPointOnRoad(GeoPoint point) {
        try {
            String osrmUrl = "http://router.project-osrm.org/nearest/v1/driving/" + point.getLongitude() + "," + point.getLatitude();
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder().url(osrmUrl).build();
            Response response = client.newCall(request).execute();

            if (response.isSuccessful() && response.body() != null) {
                JSONObject jsonObject = new JSONObject(response.body().string());
                JSONArray coordinates = jsonObject.getJSONArray("waypoints");

                if (coordinates.length() > 0) {
                    JSONArray coord = coordinates.getJSONArray(0);
                    double lon = coord.getDouble(0);
                    double lat = coord.getDouble(1);
                    return new GeoPoint(lat, lon);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null; // Return null if no nearest point found
    }

    private void fetchRoute(GeoPoint fromPoint, GeoPoint toPoint) {
        runOnUiThread(() -> mapView.getOverlays().clear());

        // OSRM route API to get the path between two snapped points
        String osrmUrl = "http://router.project-osrm.org/route/v1/driving/" +
                fromPoint.getLongitude() + "," + fromPoint.getLatitude() + ";" +
                toPoint.getLongitude() + "," + toPoint.getLatitude() +
                "?overview=full&geometries=geojson";

        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder().url(osrmUrl).build();
                Response response = client.newCall(request).execute();

                if (response.isSuccessful() && response.body() != null) {
                    String jsonResponse = response.body().string();
                    JSONObject jsonObject = new JSONObject(jsonResponse);
                    JSONArray routes = jsonObject.getJSONArray("routes");

                    if (routes.length() > 0) {
                        runOnUiThread(() -> displayRoute(jsonResponse));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Error fetching route: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void displayRoute(String jsonResponse) {
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            JSONArray routes = jsonObject.getJSONArray("routes");

            if (routes.length() == 0) {
                Toast.makeText(this, "No routes found.", Toast.LENGTH_SHORT).show();
                return;
            }

            JSONObject route = routes.getJSONObject(0);
            JSONArray coordinates = route.getJSONObject("geometry").getJSONArray("coordinates");

            List<GeoPoint> geoPoints = new ArrayList<>();
            for (int i = 0; i < coordinates.length(); i++) {
                JSONArray coord = coordinates.getJSONArray(i);
                double lon = coord.getDouble(0);
                double lat = coord.getDouble(1);
                geoPoints.add(new GeoPoint(lat, lon));
            }

            // Create polyline with violet color for the selected route
            Polyline polyline = new Polyline();
            polyline.setPoints(geoPoints);
            polyline.setColor(ContextCompat.getColor(this, R.color.violet_color));
            polyline.setWidth(8.0f);

            // Add polyline to the map
            mapView.getOverlays().add(polyline);
            mapView.invalidate(); // Refresh the map to show the route

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error displaying route: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
