



package com.example.myapplication;

import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polygon;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList; // Import ArrayList
import java.util.List;
import java.util.Locale;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ReportIssueActivity extends AppCompatActivity {

    private static final String TAG = "ReportIssueActivity";
    private static final String FILE_NAME = "route_data.csv";

    private MapView mapView;
    private SearchView searchView;
    private GeoPoint[] points = new GeoPoint[4]; // Store the 4 points selected
    private Button btnSaveRoute, btnClearPoints;
    private Polygon polygon; // Polygon to hold the drawn polygon
    private List<Polygon> savedPolygons = new ArrayList<>(); // Initialize the list to store saved polygons
    private boolean isPolygonDrawn = false; // Flag to check if a polygon has been drawn


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_issue);

        // Initialize map
        Configuration.getInstance().setUserAgentValue(getApplicationContext().getPackageName());
        mapView = findViewById(R.id.mapView);
        searchView = findViewById(R.id.search_view);
        mapView.setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);

        IMapController mapController = mapView.getController();
        mapController.setZoom(15);
        mapController.setCenter(new GeoPoint(23.8103, 90.4125)); // Default center Dhaka

        // Initialize buttons
        btnSaveRoute = findViewById(R.id.btn_save_route);
        btnClearPoints = findViewById(R.id.btn_clear_points);

        btnSaveRoute.setVisibility(View.GONE);
        btnClearPoints.setVisibility(View.GONE);

        btnSaveRoute.setOnClickListener(v -> saveAreaToCsv());
        btnClearPoints.setOnClickListener(v -> clearLastSelectedPoint());

        // Setup Listeners
        setupMapTouchListener();
        setupSearchView();

        // Load saved polygons from assets (instead of CSV on external storage)
        loadPolygonsFromAssets();  // Ensure this is called after map view is initialized
    }




    private void loadPolygonsFromAssets() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(getAssets().open(FILE_NAME)))) {
            String line;
            reader.readLine(); // Skip the header line
            int polygonCount = 1;  // To track the polygon's number
            while ((line = reader.readLine()) != null) {
                String[] values = line.split(",");
                if (values.length >= 9) {
                    try {
                        // Parse the coordinates for the 4 points
                        double lat1 = Double.parseDouble(values[1].trim());
                        double lon1 = Double.parseDouble(values[2].trim());
                        double lat2 = Double.parseDouble(values[3].trim());
                        double lon2 = Double.parseDouble(values[4].trim());
                        double lat3 = Double.parseDouble(values[5].trim());
                        double lon3 = Double.parseDouble(values[6].trim());
                        double lat4 = Double.parseDouble(values[7].trim());
                        double lon4 = Double.parseDouble(values[8].trim());

                        GeoPoint p1 = new GeoPoint(lat1, lon1);
                        GeoPoint p2 = new GeoPoint(lat2, lon2);
                        GeoPoint p3 = new GeoPoint(lat3, lon3);
                        GeoPoint p4 = new GeoPoint(lat4, lon4);

                        // Create the polygon
                        Polygon polygon = new Polygon();
                        polygon.setPoints(List.of(p1, p2, p3, p4, p1)); // Close the polygon by adding the first point again
                        polygon.setFillColor(0x220000FF);  // Transparent blue
                        polygon.setStrokeColor(0xFF0000FF); // Blue outline
                        polygon.setStrokeWidth(2.5f);

                        // Add the polygon to the map
                        mapView.getOverlays().add(polygon);

                        // Add the polygon to the savedPolygons list
                        savedPolygons.add(polygon);

                        // Add a marker with the polygon number
                        Marker marker = new Marker(mapView);
                        marker.setPosition(p1); // Use the first point of the polygon for marker position
                        marker.setTitle("Polygon " + polygonCount); // Set the title to indicate the polygon number
                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                        mapView.getOverlays().add(marker);

                        polygonCount++; // Increment polygon number for the next polygon

                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Error parsing coordinates from CSV line: " + line, e);
                    }
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Error reading CSV from assets", e);
        }
    }


    private void setupSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Log.d(TAG, "Search Query Submitted: " + query);
                fetchCoordinates(query);  // Fetch coordinates for the search query
                searchView.clearFocus();  // Remove focus after submission
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false; // No action needed for text change
            }
        });
    }


    private void fetchCoordinates(String query) {
        // Nominatim API URL with Dhaka bounding box
        String apiUrl = String.format(Locale.getDefault(),
                "https://nominatim.openstreetmap.org/search?q=%s&format=json&addressdetails=1" +
                        "&viewbox=90.2792,23.7104,90.5120,23.9135&bounded=1", query);

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(apiUrl).build();

        new Thread(() -> {
            try {
                Response response = client.newCall(request).execute();
                if (response.isSuccessful() && response.body() != null) {
                    String jsonResponse = response.body().string();
                    Log.d(TAG, "Search API Response: " + jsonResponse);
                    runOnUiThread(() -> parseCoordinates(jsonResponse)); // Parse and update UI on the main thread
                } else {
                    String errorMsg = "Request failed. Code: " + response.code();
                    Log.e(TAG, errorMsg);
                    runOnUiThread(() -> Toast.makeText(this, "Failed to fetch location: " + errorMsg, Toast.LENGTH_LONG).show());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching coordinates", e);
                runOnUiThread(() -> Toast.makeText(this, "Error fetching location: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }



    // This will clear the last selected point
    private void clearLastSelectedPoint() {
        for (int i = points.length - 1; i >= 0; i--) {
            if (points[i] != null) {
                points[i] = null;
                mapView.getOverlays().clear(); // Clear all markers and polygons

                // Re-add the remaining saved polygons
                for (Polygon savedPolygon : savedPolygons) {
                    mapView.getOverlays().add(savedPolygon);
                }

                // Re-add the remaining points as markers
                for (int j = 0; j < points.length; j++) {
                    if (points[j] != null) {
                        addMarker(points[j], "Point " + (j + 1));
                    }
                }

                setupMapTouchListener(); // Reset the map listener
                Toast.makeText(this, "Last Point Cleared", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Last Point Cleared");
                return;
            }
        }
        Toast.makeText(this, "No Points to Clear", Toast.LENGTH_SHORT).show();
    }


    private void setupMapTouchListener() {
        MapEventsOverlay mapEventsOverlay = new MapEventsOverlay(new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                for (int i = 0; i < points.length; i++) {
                    if (points[i] == null) {
                        points[i] = p;
                        addMarker(p, "Point " + (i + 1));

                        // Remove any existing polygon when a point is selected
                        if (polygon != null) {
                            mapView.getOverlays().remove(polygon);
                        }

                        // After selecting the 4th point, show the Save button
                        if (i == 3) {
                            btnSaveRoute.setVisibility(View.VISIBLE); // Show Save button
                            btnClearPoints.setVisibility(View.VISIBLE); // Show Clear button
                        }
                        Log.d(TAG, "Point " + (i + 1) + " Selected: " + p.getLatitude() + ", " + p.getLongitude());
                        break;
                    }
                }
                return true;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                return false;
            }
        });

        mapView.getOverlays().add(mapEventsOverlay);
    }


    private void drawPolygon() {
        // Draw polygon only when all 4 points are selected
        if (points[0] != null && points[1] != null && points[2] != null && points[3] != null) {
            polygon = new Polygon();
            polygon.setPoints(List.of(points[0], points[1], points[2], points[3], points[0])); // Create a polygon
            polygon.setFillColor(0x220000FF);  // Transparent blue
            polygon.setStrokeColor(0xFF0000FF); // Blue outline
            polygon.setStrokeWidth(2.5f);
            mapView.getOverlays().add(polygon);

            // Add to savedPolygons list to keep track of saved polygons
            savedPolygons.add(polygon);

            mapView.invalidate();
        }
    }


    private void clearPoints() {
        // Remove all the selected points
        for (int i = 0; i < points.length; i++) {
            points[i] = null;
        }

        // Clear the markers (points) but keep the saved polygons
        mapView.getOverlays().clear();

        // Re-add saved polygons to the map
        for (Polygon savedPolygon : savedPolygons) {
            mapView.getOverlays().add(savedPolygon);
        }

        setupMapTouchListener(); // Reset the map listener
        Toast.makeText(this, "Points Cleared", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "All Points Cleared");
    }

    private void addMarker(GeoPoint point, String title) {
        Marker marker = new Marker(mapView);
        marker.setPosition(point);
        marker.setTitle(title);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        mapView.getOverlays().add(marker);
        mapView.invalidate();
    }


    private void saveAreaToCsv() {
        if (points[0] == null || points[1] == null || points[2] == null || points[3] == null) {
            Toast.makeText(this, "Select all 4 points", Toast.LENGTH_SHORT).show();
            return;
        }

        // Draw the polygon and save it only after clicking Save
        drawPolygon();

        String roadName = getRoadName(points[0]);  // Using the first point's road name
        File file = new File(getExternalFilesDir(null), FILE_NAME);
        boolean isNewFile = !file.exists();

        try (FileWriter writer = new FileWriter(file, true)) {
            if (isNewFile) {
                writer.append("Road Name,Point 1 Latitude,Point 1 Longitude,Point 2 Latitude,Point 2 Longitude,Point 3 Latitude,Point 3 Longitude,Point 4 Latitude,Point 4 Longitude\n");
            }
            String data = roadName + "," + points[0].getLatitude() + "," + points[0].getLongitude() + "," +
                    points[1].getLatitude() + "," + points[1].getLongitude() + "," +
                    points[2].getLatitude() + "," + points[2].getLongitude() + "," +
                    points[3].getLatitude() + "," + points[3].getLongitude() + "\n";
            writer.append(data);
            writer.flush();

            Log.d(TAG, "CSV Saved: " + data);
            Toast.makeText(this, "Area saved!", Toast.LENGTH_SHORT).show();

            // Clear points after saving
            clearPoints();
        } catch (IOException e) {
            Log.e(TAG, "Error writing CSV", e);
        }
    }



    private String getRoadName(GeoPoint geoPoint) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(geoPoint.getLatitude(), geoPoint.getLongitude(), 1);
            if (addresses != null && !addresses.isEmpty()) {
                return addresses.get(0).getThoroughfare();
            }
        } catch (IOException e) {
            Log.e(TAG, "Geocoder failed", e);
        }
        return "Unknown Road";
    }

private void parseCoordinates(String jsonResponse) {
    try {
        JSONArray jsonArray = new JSONArray(jsonResponse);
        if (jsonArray.length() > 0) {
            JSONObject locationObject = jsonArray.getJSONObject(0);
            double lat = locationObject.getDouble("lat");
            double lon = locationObject.getDouble("lon");

            org.osmdroid.util.GeoPoint geoPoint = new org.osmdroid.util.GeoPoint(lat, lon);
            mapView.getController().setCenter(geoPoint);

            Marker marker = new Marker(mapView);
            marker.setPosition(geoPoint);
            marker.setTitle("Searched Location");
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            mapView.getOverlays().add(marker);
            mapView.invalidate(); // Refresh the map

            Log.d(TAG, "Search Location Plotted: " + lat + ", " + lon);
        } else {
            Toast.makeText(this, "No results found", Toast.LENGTH_SHORT).show();
        }
    } catch (Exception e) {
        Log.e(TAG, "Error parsing coordinates", e);
    }
}

}
