package com.example.myapplication;

import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.Toast;
import java.io.FileReader;

import androidx.appcompat.app.AlertDialog;
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
    private int selectedPolygonIndex = -1;  // Track the selected polygon index for editing


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



    private void setupMapTouchListener() {
    }

    private void saveAreaToCsv() {
    }

    private void clearLastSelectedPoint() {
    }

    private void setupSearchView() {
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

                        // Add a click listener for the marker
                        final int finalPolygonCount = polygonCount;
                        marker.setOnMarkerClickListener((marker1, mapView) -> {
                            selectedPolygonIndex = finalPolygonCount - 1;  // Save index of clicked polygon
                            showEditDialog(selectedPolygonIndex);
                            return true;
                        });

                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Error parsing coordinates from CSV line: " + line, e);
                    }
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Error reading CSV from assets", e);
        }
    }

    private void showEditDialog(int polygonIndex) {
        // Create an AlertDialog to get user input
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Set the dialog title to indicate the polygon number (based on the row number in the CSV)
        builder.setTitle("Edit Polygon Data to " + (polygonIndex + 1));

        // Set up the input fields
        final EditText rainThresholdInput = new EditText(this);
        rainThresholdInput.setHint("Rain Threshold (mm)");
        final EditText waterloggedDurationInput = new EditText(this);
        waterloggedDurationInput.setHint("Waterlogged Duration (hrs)");

        // Layout for dialog
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(rainThresholdInput);
        layout.addView(waterloggedDurationInput);
        builder.setView(layout);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String rainThreshold = rainThresholdInput.getText().toString();
            String waterloggedDuration = waterloggedDurationInput.getText().toString();

            // Validate input
            if (!rainThreshold.isEmpty() && !waterloggedDuration.isEmpty()) {
                updateCsvData(polygonIndex, rainThreshold, waterloggedDuration);
            } else {
                Toast.makeText(this, "Please fill both fields", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }


    private void updateCsvData(int polygonIndex, String rainThreshold, String waterloggedDuration) {
        File file = new File(getExternalFilesDir(null), FILE_NAME);
        try (
                // Using try-with-resources to automatically close resources
                BufferedReader reader = new BufferedReader(new FileReader(file));

        ) {
            // Read the CSV file
            List<String> lines = new ArrayList<>();
            String line;
            boolean headerUpdated = false;

            // Read lines and check if headers exist
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }

            // Check if the file contains the new headers
            String[] headers = lines.get(0).split(",");
            boolean hasRainThreshold = false;
            boolean hasWaterloggedDuration = false;

            // Check if the columns exist
            for (String header : headers) {
                if (header.equals("Rain Threshold (mm)")) {
                    hasRainThreshold = true;
                }
                if (header.equals("Waterlogged Duration (hrs)")) {
                    hasWaterloggedDuration = true;
                }
            }

            // Add new columns if they don't exist
            if (!hasRainThreshold || !hasWaterloggedDuration) {
                StringBuilder newHeader = new StringBuilder(lines.get(0));
                if (!hasRainThreshold) {
                    newHeader.append(",Rain Threshold (mm)");
                }
                if (!hasWaterloggedDuration) {
                    newHeader.append(",Waterlogged Duration (hrs)");
                }
                lines.set(0, newHeader.toString());  // Update the header
            }

            // Update the relevant row for the selected polygon
            String[] selectedRow = lines.get(polygonIndex + 1).split(",");
            selectedRow[headers.length] = rainThreshold;  // Add Rain Threshold
            selectedRow[headers.length + 1] = waterloggedDuration;  // Add Waterlogged Duration
            lines.set(polygonIndex + 1, String.join(",", selectedRow));

            // Write back to the CSV file
            try (FileWriter writer = new FileWriter(file)) {
                for (String lineToWrite : lines) {
                    writer.append(lineToWrite).append("\n");
                }
            }

            Toast.makeText(this, "Data saved!", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Log.e(TAG, "Error updating CSV", e);
            Toast.makeText(this, "Error updating data", Toast.LENGTH_SHORT).show();
        }
    }

    // The rest of your existing methods (clearLastSelectedPoint, setupMapTouchListener, etc.) remain unchanged.
}
