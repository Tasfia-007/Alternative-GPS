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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
    private GeoPoint startPoint = null;
    private GeoPoint endPoint = null;
    private Button btnSaveRoute, btnClearStart, btnClearEnd;

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
        btnClearStart = findViewById(R.id.btn_clear_start);
        btnClearEnd = findViewById(R.id.btn_clear_end);

        btnSaveRoute.setVisibility(View.GONE);
        btnClearStart.setVisibility(View.GONE);
        btnClearEnd.setVisibility(View.GONE);

        btnSaveRoute.setOnClickListener(v -> saveRouteToCsv());
        btnClearStart.setOnClickListener(v -> clearStartPoint());
        btnClearEnd.setOnClickListener(v -> clearEndPoint());

        // Setup Listeners
        setupMapTouchListener();
        setupSearchView();
    }



























    private void clearStartPoint() {
        if (startPoint != null) {
            startPoint = null;
            btnClearStart.setVisibility(View.GONE);
            mapView.getOverlays().clear();
            setupMapTouchListener(); // Reset map listener
            Toast.makeText(this, "Start Point Cleared", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Start Point Cleared");
        }
    }

    private void clearEndPoint() {
        if (endPoint != null) {
            endPoint = null;
            btnClearEnd.setVisibility(View.GONE);
            mapView.getOverlays().clear();
            setupMapTouchListener(); // Reset map listener
            Toast.makeText(this, "End Point Cleared", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "End Point Cleared");
        }
    }


    private void setupSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Log.d(TAG, "Search Query Submitted: " + query);
                fetchCoordinates(query);
                searchView.clearFocus();
                return true;






            }



            @Override
            public boolean onQueryTextChange(String newText) {
                return false;














            }
        });



    }

    private void fetchCoordinates(String query) {
        // Nominatim API with Dhaka bounding box
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
                    runOnUiThread(() -> parseCoordinates(jsonResponse));




                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching coordinates", e);

            }
        }).start();
    }

    private void parseCoordinates(String jsonResponse) {
        try {
            JSONArray jsonArray = new JSONArray(jsonResponse);
            if (jsonArray.length() > 0) {
                JSONObject locationObject = jsonArray.getJSONObject(0);
                double lat = locationObject.getDouble("lat");
                double lon = locationObject.getDouble("lon");

                GeoPoint geoPoint = new GeoPoint(lat, lon);
                mapView.getController().setCenter(geoPoint);

                Marker marker = new Marker(mapView);
                marker.setPosition(geoPoint);
                marker.setTitle("Searched Location");
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                mapView.getOverlays().add(marker);
                mapView.invalidate();

                Log.d(TAG, "Search Location Plotted: " + lat + ", " + lon);
            } else {
                Toast.makeText(this, "No results found", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing coordinates", e);
        }
    }

    private void setupMapTouchListener() {
        MapEventsOverlay mapEventsOverlay = new MapEventsOverlay(new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                if (startPoint == null) {
                    startPoint = p;
                    addMarker(p, "Start Point");
                    btnClearStart.setVisibility(View.VISIBLE);
                    Log.d(TAG, "Start Point Selected: " + p.getLatitude() + ", " + p.getLongitude());
                } else if (endPoint == null) {
                    endPoint = p;
                    addMarker(p, "End Point");
                    btnSaveRoute.setVisibility(View.VISIBLE);
                    btnClearEnd.setVisibility(View.VISIBLE);
                    Log.d(TAG, "End Point Selected: " + p.getLatitude() + ", " + p.getLongitude());
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

    private void addMarker(GeoPoint point, String title) {
        Marker marker = new Marker(mapView);
        marker.setPosition(point);
        marker.setTitle(title);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        mapView.getOverlays().add(marker);
        mapView.invalidate();
    }

    private void saveRouteToCsv() {
        if (startPoint == null || endPoint == null) {
            Toast.makeText(this, "Select Start and End points", Toast.LENGTH_SHORT).show();
            return;
        }



        String roadName = getRoadName(startPoint);
        File file = new File(getExternalFilesDir(null), FILE_NAME);
        boolean isNewFile = !file.exists();



        try (FileWriter writer = new FileWriter(file, true)) {
            if (isNewFile) {
                writer.append("Road Name,Start Latitude,Start Longitude,End Latitude,End Longitude\n");
            }
            String data = roadName + "," + startPoint.getLatitude() + "," + startPoint.getLongitude() + ","
                    + endPoint.getLatitude() + "," + endPoint.getLongitude() + "\n";
            writer.append(data);
            writer.flush();

            Log.d(TAG, "CSV Saved: " + data);
            Toast.makeText(this, "Route saved!", Toast.LENGTH_SHORT).show();
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
}