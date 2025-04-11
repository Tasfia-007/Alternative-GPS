//package com.example.myapplication;
//
//import android.location.Address;
//import android.location.Geocoder;
//import android.os.Bundle;
//import android.util.Log;
//import android.view.View;
//import android.widget.Button;
//import android.widget.SearchView;
//import android.widget.Toast;
//
//import androidx.appcompat.app.AppCompatActivity;
//
//import org.json.JSONArray;
//import org.json.JSONObject;
//import org.osmdroid.api.IMapController;
//import org.osmdroid.config.Configuration;
//import org.osmdroid.events.MapEventsReceiver;
//import org.osmdroid.util.GeoPoint;
//import org.osmdroid.views.MapView;
//import org.osmdroid.views.overlay.MapEventsOverlay;
//import org.osmdroid.views.overlay.Marker;
//import org.osmdroid.views.overlay.Polygon;
//
//import java.io.File;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.util.List;
//import java.util.Locale;
//
//import okhttp3.OkHttpClient;
//import okhttp3.Request;
//import okhttp3.Response;
//
//public class ReportIssueActivity extends AppCompatActivity {
//
//    private static final String TAG = "ReportIssueActivity";
//    private static final String FILE_NAME = "route_data.csv";
//
//    private MapView mapView;
//    private SearchView searchView;
//    private GeoPoint[] points = new GeoPoint[4]; // Store the 4 points selected
//    private Button btnSaveRoute, btnClearPoints;
//
//    private Polygon areaPolygon; // Store the polygon object for visibility management
//    private Marker[] markers = new Marker[4]; // Store markers for the selected points
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_report_issue);
//
//        // Initialize map
//        Configuration.getInstance().setUserAgentValue(getApplicationContext().getPackageName());
//        mapView = findViewById(R.id.mapView);
//        searchView = findViewById(R.id.search_view);
//        mapView.setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK);
//        mapView.setMultiTouchControls(true);
//
//        IMapController mapController = mapView.getController();
//        mapController.setZoom(15);
//        mapController.setCenter(new GeoPoint(23.8103, 90.4125)); // Default center Dhaka
//
//        // Initialize buttons
//        btnSaveRoute = findViewById(R.id.btn_save_route);
//        btnClearPoints = findViewById(R.id.btn_clear_points);
//
//        btnSaveRoute.setVisibility(View.GONE);
//        btnClearPoints.setVisibility(View.GONE);
//
//        btnSaveRoute.setOnClickListener(v -> saveAreaToCsv());
//        btnClearPoints.setOnClickListener(v -> clearLastSelectedPoint());
//
//        // Setup Listeners
//        setupMapTouchListener();
//        setupSearchView();
//    }
//
//    private void clearLastSelectedPoint() {
//        // Clear the last selected point
//        for (int i = points.length - 1; i >= 0; i--) {
//            if (points[i] != null) {
//                points[i] = null;
//                if (markers[i] != null) {
//                    mapView.getOverlays().remove(markers[i]);
//                    markers[i] = null;
//                }
//                Log.d(TAG, "Point " + (i + 1) + " Deselected");
//                break;
//            }
//        }
//
//        // If no points remain, hide the save and clear buttons
//        if (points[0] == null && points[1] == null && points[2] == null && points[3] == null) {
//            btnSaveRoute.setVisibility(View.GONE);
//            btnClearPoints.setVisibility(View.GONE);
//        }
//        mapView.invalidate();
//    }
//
//    private void setupSearchView() {
//        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
//            @Override
//            public boolean onQueryTextSubmit(String query) {
//                Log.d(TAG, "Search Query Submitted: " + query);
//                fetchCoordinates(query);
//                searchView.clearFocus();
//                return true;
//            }
//
//            @Override
//            public boolean onQueryTextChange(String newText) {
//                return false;
//            }
//        });
//    }
//
//    private void fetchCoordinates(String query) {
//        String apiUrl = String.format(Locale.getDefault(),
//                "https://nominatim.openstreetmap.org/search?q=%s&format=json&addressdetails=1" +
//                        "&viewbox=90.2792,23.7104,90.5120,23.9135&bounded=1", query);
//
//        OkHttpClient client = new OkHttpClient();
//        Request request = new Request.Builder().url(apiUrl).build();
//
//        new Thread(() -> {
//            try {
//                Response response = client.newCall(request).execute();
//                if (response.isSuccessful() && response.body() != null) {
//                    String jsonResponse = response.body().string();
//                    Log.d(TAG, "Search API Response: " + jsonResponse);
//                    runOnUiThread(() -> parseCoordinates(jsonResponse));
//                }
//            } catch (Exception e) {
//                Log.e(TAG, "Error fetching coordinates", e);
//            }
//        }).start();
//    }
//
//    private void parseCoordinates(String jsonResponse) {
//        try {
//            JSONArray jsonArray = new JSONArray(jsonResponse);
//            if (jsonArray.length() > 0) {
//                JSONObject locationObject = jsonArray.getJSONObject(0);
//                double lat = locationObject.getDouble("lat");
//                double lon = locationObject.getDouble("lon");
//
//                GeoPoint geoPoint = new GeoPoint(lat, lon);
//                mapView.getController().setCenter(geoPoint);
//
//                Marker marker = new Marker(mapView);
//                marker.setPosition(geoPoint);
//                marker.setTitle("Searched Location");
//                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
//                mapView.getOverlays().add(marker);
//                mapView.invalidate();
//
//                Log.d(TAG, "Search Location Plotted: " + lat + ", " + lon);
//            } else {
//                Toast.makeText(this, "No results found", Toast.LENGTH_SHORT).show();
//            }
//        } catch (Exception e) {
//            Log.e(TAG, "Error parsing coordinates", e);
//        }
//    }
//
//    private void setupMapTouchListener() {
//        MapEventsOverlay mapEventsOverlay = new MapEventsOverlay(new MapEventsReceiver() {
//            @Override
//            public boolean singleTapConfirmedHelper(GeoPoint p) {
//                for (int i = 0; i < points.length; i++) {
//                    if (points[i] == null) {
//                        points[i] = p;
//                        addMarker(p, "Point " + (i + 1));
//                        if (i == 3) {
//                            drawPolygon();
//                            btnSaveRoute.setVisibility(View.VISIBLE);
//                            btnClearPoints.setVisibility(View.VISIBLE);
//                        }
//                        Log.d(TAG, "Point " + (i + 1) + " Selected: " + p.getLatitude() + ", " + p.getLongitude());
//                        break;
//                    }
//                }
//                return true;
//            }
//
//            @Override
//            public boolean longPressHelper(GeoPoint p) {
//                return false;
//            }
//        });
//
//        mapView.getOverlays().add(mapEventsOverlay);
//    }
//
//    private void addMarker(GeoPoint point, String title) {
//        Marker marker = new Marker(mapView);
//        marker.setPosition(point);
//        marker.setTitle(title);
//        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
//        mapView.getOverlays().add(marker);
//        mapView.invalidate();
//    }
//
//    private void drawPolygon() {
//        if (points[0] != null && points[1] != null && points[2] != null && points[3] != null) {
//            // Remove any existing polygon
//            if (areaPolygon != null) {
//                mapView.getOverlays().remove(areaPolygon);
//            }
//
//            // Create a new polygon and set points
//            areaPolygon = new Polygon();
//            areaPolygon.setPoints(List.of(points[0], points[1], points[2], points[3], points[0])); // Create a polygon
//            mapView.getOverlays().add(areaPolygon);
//            mapView.invalidate();
//        }
//    }
//
//    private void saveAreaToCsv() {
//        if (points[0] == null || points[1] == null || points[2] == null || points[3] == null) {
//            Toast.makeText(this, "Select all 4 points", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        String roadName = getRoadName(points[0]);  // Using the first point's road name
//        File file = new File(getExternalFilesDir(null), FILE_NAME);
//        boolean isNewFile = !file.exists();
//
//        try (FileWriter writer = new FileWriter(file, true)) {
//            if (isNewFile) {
//                writer.append("Road Name,Point 1 Latitude,Point 1 Longitude,Point 2 Latitude,Point 2 Longitude,Point 3 Latitude,Point 3 Longitude,Point 4 Latitude,Point 4 Longitude\n");
//            }
//            String data = roadName + "," + points[0].getLatitude() + "," + points[0].getLongitude() + "," +
//                    points[1].getLatitude() + "," + points[1].getLongitude() + "," +
//                    points[2].getLatitude() + "," + points[2].getLongitude() + "," +
//                    points[3].getLatitude() + "," + points[3].getLongitude() + "\n";
//            writer.append(data);
//            writer.flush();
//
//            Log.d(TAG, "CSV Saved: " + data);
//            Toast.makeText(this, "Area saved!", Toast.LENGTH_SHORT).show();
//
//            // Deselect all points after saving
//            clearLastSelectedPoint();
//        } catch (IOException e) {
//            Log.e(TAG, "Error writing CSV", e);
//        }
//    }
//
//    private String getRoadName(GeoPoint geoPoint) {
//        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
//        try {
//            List<Address> addresses = geocoder.getFromLocation(geoPoint.getLatitude(), geoPoint.getLongitude(), 1);
//            if (addresses != null && !addresses.isEmpty()) {
//                return addresses.get(0).getThoroughfare();
//            }
//        } catch (IOException e) {
//            Log.e(TAG, "Geocoder failed", e);
//        }
//        return "Unknown Road";
//    }
//}







//
//
//
//
//package com.example.myapplication;
//
//import android.location.Address;
//import android.location.Geocoder;
//import android.os.Bundle;
//import android.util.Log;
//import android.view.View;
//import android.widget.Button;
//import android.widget.SearchView;
//import android.widget.Toast;
//
//import androidx.appcompat.app.AppCompatActivity;
//
//import org.json.JSONArray;
//import org.json.JSONObject;
//import org.osmdroid.api.IMapController;
//import org.osmdroid.config.Configuration;
//import org.osmdroid.events.MapEventsReceiver;
//import org.osmdroid.util.GeoPoint;
//import org.osmdroid.views.MapView;
//import org.osmdroid.views.overlay.MapEventsOverlay;
//import org.osmdroid.views.overlay.Marker;
//import org.osmdroid.views.overlay.Polygon;
//
//import java.io.File;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.util.List;
//import java.util.Locale;
//
//import okhttp3.OkHttpClient;
//import okhttp3.Request;
//import okhttp3.Response;
//
//public class ReportIssueActivity extends AppCompatActivity {
//
//    private static final String TAG = "ReportIssueActivity";
//    private static final String FILE_NAME = "route_data.csv";
//
//    private MapView mapView;
//    private SearchView searchView;
//    private GeoPoint[] points = new GeoPoint[4]; // Store the 4 points selected
//    private Button btnSaveRoute, btnClearPoints;
//
//    private Polygon areaPolygon; // Store the polygon object for visibility management
//    private Marker[] markers = new Marker[4]; // Store markers for the selected points
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_report_issue);
//
//        // Initialize map
//        Configuration.getInstance().setUserAgentValue(getApplicationContext().getPackageName());
//        mapView = findViewById(R.id.mapView);
//        searchView = findViewById(R.id.search_view);
//        mapView.setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK);
//        mapView.setMultiTouchControls(true);
//
//        IMapController mapController = mapView.getController();
//        mapController.setZoom(15);
//        mapController.setCenter(new GeoPoint(23.8103, 90.4125)); // Default center Dhaka
//
//        // Initialize buttons
//        btnSaveRoute = findViewById(R.id.btn_save_route);
//        btnClearPoints = findViewById(R.id.btn_clear_points);
//
//        btnSaveRoute.setVisibility(View.GONE);
//        btnClearPoints.setVisibility(View.GONE);
//
//        btnSaveRoute.setOnClickListener(v -> saveAreaToCsv());
//        btnClearPoints.setOnClickListener(v -> clearLastSelectedPoint());
//
//        // Setup Listeners
//        setupMapTouchListener();
//        setupSearchView();
//    }
//
//    private void clearLastSelectedPoint() {
//        // Find the last selected point and deselect it
//        for (int i = points.length - 1; i >= 0; i--) {
//            if (points[i] != null) {
//                // Remove the point and its associated marker
//                points[i] = null;
//                if (markers[i] != null) {
//                    mapView.getOverlays().remove(markers[i]);
//                    markers[i] = null;
//                }
//                Log.d(TAG, "Point " + (i + 1) + " Deselected");
//                break;
//            }
//        }
//
//        // If no points remain, hide the save button
//        if (points[0] == null && points[1] == null && points[2] == null && points[3] == null) {
//            btnSaveRoute.setVisibility(View.GONE);
//        }
//        mapView.invalidate();
//    }
//
//    private void setupSearchView() {
//        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
//            @Override
//            public boolean onQueryTextSubmit(String query) {
//                Log.d(TAG, "Search Query Submitted: " + query);
//                fetchCoordinates(query);
//                searchView.clearFocus();
//                return true;
//            }
//
//            @Override
//            public boolean onQueryTextChange(String newText) {
//                return false;
//            }
//        });
//    }
//
//    private void fetchCoordinates(String query) {
//        String apiUrl = String.format(Locale.getDefault(),
//                "https://nominatim.openstreetmap.org/search?q=%s&format=json&addressdetails=1" +
//                        "&viewbox=90.2792,23.7104,90.5120,23.9135&bounded=1", query);
//
//        OkHttpClient client = new OkHttpClient();
//        Request request = new Request.Builder().url(apiUrl).build();
//
//        new Thread(() -> {
//            try {
//                Response response = client.newCall(request).execute();
//                if (response.isSuccessful() && response.body() != null) {
//                    String jsonResponse = response.body().string();
//                    Log.d(TAG, "Search API Response: " + jsonResponse);
//                    runOnUiThread(() -> parseCoordinates(jsonResponse));
//                }
//            } catch (Exception e) {
//                Log.e(TAG, "Error fetching coordinates", e);
//            }
//        }).start();
//    }
//
//    private void parseCoordinates(String jsonResponse) {
//        try {
//            JSONArray jsonArray = new JSONArray(jsonResponse);
//            if (jsonArray.length() > 0) {
//                JSONObject locationObject = jsonArray.getJSONObject(0);
//                double lat = locationObject.getDouble("lat");
//                double lon = locationObject.getDouble("lon");
//
//                GeoPoint geoPoint = new GeoPoint(lat, lon);
//                mapView.getController().setCenter(geoPoint);
//
//                Marker marker = new Marker(mapView);
//                marker.setPosition(geoPoint);
//                marker.setTitle("Searched Location");
//                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
//                mapView.getOverlays().add(marker);
//                mapView.invalidate();
//
//                Log.d(TAG, "Search Location Plotted: " + lat + ", " + lon);
//            } else {
//                Toast.makeText(this, "No results found", Toast.LENGTH_SHORT).show();
//            }
//        } catch (Exception e) {
//            Log.e(TAG, "Error parsing coordinates", e);
//        }
//    }
//
//    private void setupMapTouchListener() {
//        MapEventsOverlay mapEventsOverlay = new MapEventsOverlay(new MapEventsReceiver() {
//            @Override
//            public boolean singleTapConfirmedHelper(GeoPoint p) {
//                // Add the point and marker, and then continue the process
//                for (int i = 0; i < points.length; i++) {
//                    if (points[i] == null) {
//                        points[i] = p;
//                        addMarker(p, "Point " + (i + 1));
//
//                        // Show the clear button after selecting the first point
//                        btnClearPoints.setVisibility(View.VISIBLE);
//
//                        if (i == 3) { // Once all 4 points are selected, show the save button
//                            drawPolygon();
//                            btnSaveRoute.setVisibility(View.VISIBLE);
//                        }
//                        Log.d(TAG, "Point " + (i + 1) + " Selected: " + p.getLatitude() + ", " + p.getLongitude());
//                        break;
//                    }
//                }
//                return true;
//            }
//
//            @Override
//            public boolean longPressHelper(GeoPoint p) {
//                return false;
//            }
//        });
//
//        mapView.getOverlays().add(mapEventsOverlay);
//    }
//
//    private void addMarker(GeoPoint point, String title) {
//        Marker marker = new Marker(mapView);
//        marker.setPosition(point);
//        marker.setTitle(title);
//        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
//        mapView.getOverlays().add(marker);
//        markers[getNextAvailablePointIndex()] = marker; // Store the marker for this point
//        mapView.invalidate();
//    }
//
//    private int getNextAvailablePointIndex() {
//        for (int i = 0; i < points.length; i++) {
//            if (points[i] == null) {
//                return i;
//            }
//        }
//        return -1; // All points are already selected
//    }
//
//    private void drawPolygon() {
//        if (points[0] != null && points[1] != null && points[2] != null && points[3] != null) {
//            // Remove any existing polygon
//            if (areaPolygon != null) {
//                mapView.getOverlays().remove(areaPolygon);
//            }
//
//            // Create a new polygon and set points
//            areaPolygon = new Polygon();
//            areaPolygon.setPoints(List.of(points[0], points[1], points[2], points[3], points[0])); // Create a polygon
//            mapView.getOverlays().add(areaPolygon);
//            mapView.invalidate();
//        }
//    }
//
//    private void saveAreaToCsv() {
//        if (points[0] == null || points[1] == null || points[2] == null || points[3] == null) {
//            Toast.makeText(this, "Select all 4 points", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        String roadName = getRoadName(points[0]);  // Using the first point's road name
//        File file = new File(getExternalFilesDir(null), FILE_NAME);
//        boolean isNewFile = !file.exists();
//
//        try (FileWriter writer = new FileWriter(file, true)) {
//            if (isNewFile) {
//                writer.append("Road Name,Point 1 Latitude,Point 1 Longitude,Point 2 Latitude,Point 2 Longitude,Point 3 Latitude,Point 3 Longitude,Point 4 Latitude,Point 4 Longitude\n");
//            }
//            String data = roadName + "," + points[0].getLatitude() + "," + points[0].getLongitude() + "," +
//                    points[1].getLatitude() + "," + points[1].getLongitude() + "," +
//                    points[2].getLatitude() + "," + points[2].getLongitude() + "," +
//                    points[3].getLatitude() + "," + points[3].getLongitude() + "\n";
//            writer.append(data);
//            writer.flush();
//
//            Log.d(TAG, "CSV Saved: " + data);
//            Toast.makeText(this, "Area saved!", Toast.LENGTH_SHORT).show();
//
//            // After saving, remove points and markers
//            clearAllPoints();
//            mapView.invalidate();
//        } catch (IOException e) {
//            Log.e(TAG, "Error writing CSV", e);
//        }
//    }
//
//    private void clearAllPoints() {
//        // Remove all points and markers
//        for (int i = 0; i < points.length; i++) {
//            points[i] = null;
//            if (markers[i] != null) {
//                mapView.getOverlays().remove(markers[i]);
//                markers[i] = null;
//            }
//        }
//        btnSaveRoute.setVisibility(View.GONE);
//        btnClearPoints.setVisibility(View.GONE);
//    }
//
//    private String getRoadName(GeoPoint geoPoint) {
//        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
//        try {
//            List<Address> addresses = geocoder.getFromLocation(geoPoint.getLatitude(), geoPoint.getLongitude(), 1);
//            if (addresses != null && !addresses.isEmpty()) {
//                return addresses.get(0).getThoroughfare();
//            }
//        } catch (IOException e) {
//            Log.e(TAG, "Geocoder failed", e);
//        }
//        return "Unknown Road";
//    }
//
//
//}
//





















//------2nd version, good but need modification------

//package com.example.myapplication;
//
//
//import android.location.Address;
//import android.location.Geocoder;
//import android.os.Bundle;
//import android.util.Log;
//import android.view.View;
//import android.widget.Button;
//import android.widget.SearchView;
//import android.widget.Toast;
//
//import androidx.appcompat.app.AppCompatActivity;
//
//import org.json.JSONArray;
//import org.json.JSONObject;
//import org.osmdroid.api.IMapController;
//import org.osmdroid.config.Configuration;
//import org.osmdroid.events.MapEventsReceiver;
//import org.osmdroid.util.GeoPoint;
//import org.osmdroid.views.MapView;
//import org.osmdroid.views.overlay.MapEventsOverlay;
//import org.osmdroid.views.overlay.Marker;
//import org.osmdroid.views.overlay.Polygon;
//
//import java.io.File;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.util.List;
//import java.util.Locale;
//
//import okhttp3.OkHttpClient;
//import okhttp3.Request;
//import okhttp3.Response;
//
//public class ReportIssueActivity extends AppCompatActivity {
//
//    private static final String TAG = "ReportIssueActivity";
//    private static final String FILE_NAME = "route_data.csv";
//
//    private MapView mapView;
//    private SearchView searchView;
//    private GeoPoint[] points = new GeoPoint[4]; // Store the 4 points selected
//    private Button btnSaveRoute, btnClearPoints;
//
//    // Store the polygon object for visibility management
//    private Polygon areaPolygon;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_report_issue);
//
//        // Initialize map
//        Configuration.getInstance().setUserAgentValue(getApplicationContext().getPackageName());
//        mapView = findViewById(R.id.mapView);
//        searchView = findViewById(R.id.search_view);
//        mapView.setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK);
//        mapView.setMultiTouchControls(true);
//
//        IMapController mapController = mapView.getController();
//        mapController.setZoom(15);
//        mapController.setCenter(new GeoPoint(23.8103, 90.4125)); // Default center Dhaka
//
//        // Initialize buttons
//        btnSaveRoute = findViewById(R.id.btn_save_route);
//        btnClearPoints = findViewById(R.id.btn_clear_points);
//
//        btnSaveRoute.setVisibility(View.GONE);
//        btnClearPoints.setVisibility(View.GONE);
//
//        btnSaveRoute.setOnClickListener(v -> saveAreaToCsv());
//        btnClearPoints.setOnClickListener(v -> clearPoints());
//
//        // Setup Listeners
//        setupMapTouchListener();
//        setupSearchView();
//    }
//
//    private void clearPoints() {
//        // Clear the points and remove the polygon
//        for (int i = 0; i < points.length; i++) {
//            points[i] = null;
//        }
//        btnSaveRoute.setVisibility(View.GONE);
//        btnClearPoints.setVisibility(View.GONE);
//
//        // Remove the existing polygon from map
//        if (areaPolygon != null) {
//            mapView.getOverlays().remove(areaPolygon);
//            areaPolygon = null;
//        }
//
//        setupMapTouchListener(); // Reset map listener
//        Toast.makeText(this, "Points Cleared", Toast.LENGTH_SHORT).show();
//        Log.d(TAG, "Points Cleared");
//    }
//
//    private void setupSearchView() {
//        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
//            @Override
//            public boolean onQueryTextSubmit(String query) {
//                Log.d(TAG, "Search Query Submitted: " + query);
//                fetchCoordinates(query);
//                searchView.clearFocus();
//                return true;
//            }
//
//            @Override
//            public boolean onQueryTextChange(String newText) {
//                return false;
//            }
//        });
//    }
//
//    private void fetchCoordinates(String query) {
//        // Nominatim API with Dhaka bounding box
//        String apiUrl = String.format(Locale.getDefault(),
//                "https://nominatim.openstreetmap.org/search?q=%s&format=json&addressdetails=1" +
//                        "&viewbox=90.2792,23.7104,90.5120,23.9135&bounded=1", query);
//
//        OkHttpClient client = new OkHttpClient();
//        Request request = new Request.Builder().url(apiUrl).build();
//
//        new Thread(() -> {
//            try {
//                Response response = client.newCall(request).execute();
//                if (response.isSuccessful() && response.body() != null) {
//                    String jsonResponse = response.body().string();
//                    Log.d(TAG, "Search API Response: " + jsonResponse);
//                    runOnUiThread(() -> parseCoordinates(jsonResponse));
//                }
//            } catch (Exception e) {
//                Log.e(TAG, "Error fetching coordinates", e);
//            }
//        }).start();
//    }
//
//    private void parseCoordinates(String jsonResponse) {
//        try {
//            JSONArray jsonArray = new JSONArray(jsonResponse);
//            if (jsonArray.length() > 0) {
//                JSONObject locationObject = jsonArray.getJSONObject(0);
//                double lat = locationObject.getDouble("lat");
//                double lon = locationObject.getDouble("lon");
//
//                GeoPoint geoPoint = new GeoPoint(lat, lon);
//                mapView.getController().setCenter(geoPoint);
//
//                Marker marker = new Marker(mapView);
//                marker.setPosition(geoPoint);
//                marker.setTitle("Searched Location");
//                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
//                mapView.getOverlays().add(marker);
//                mapView.invalidate();
//
//                Log.d(TAG, "Search Location Plotted: " + lat + ", " + lon);
//            } else {
//                Toast.makeText(this, "No results found", Toast.LENGTH_SHORT).show();
//            }
//        } catch (Exception e) {
//            Log.e(TAG, "Error parsing coordinates", e);
//        }
//    }
//
//    private void setupMapTouchListener() {
//        MapEventsOverlay mapEventsOverlay = new MapEventsOverlay(new MapEventsReceiver() {
//            @Override
//            public boolean singleTapConfirmedHelper(GeoPoint p) {
//                for (int i = 0; i < points.length; i++) {
//                    if (points[i] == null) {
//                        points[i] = p;
//                        addMarker(p, "Point " + (i + 1));
//                        if (i == 3) {  // Once all 4 points are selected
//                            drawPolygon();
//                            btnSaveRoute.setVisibility(View.VISIBLE);
//                            btnClearPoints.setVisibility(View.VISIBLE);
//                        }
//                        Log.d(TAG, "Point " + (i + 1) + " Selected: " + p.getLatitude() + ", " + p.getLongitude());
//                        break;
//                    }
//                }
//                return true;
//            }
//
//            @Override
//            public boolean longPressHelper(GeoPoint p) {
//                return false;
//            }
//        });
//
//        mapView.getOverlays().add(mapEventsOverlay);
//    }
//
//    private void addMarker(GeoPoint point, String title) {
//        Marker marker = new Marker(mapView);
//        marker.setPosition(point);
//        marker.setTitle(title);
//        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
//        mapView.getOverlays().add(marker);
//        mapView.invalidate();
//    }
//
//    private void drawPolygon() {
//        // Only draw the polygon if all 4 points are selected
//        if (points[0] != null && points[1] != null && points[2] != null && points[3] != null) {
//            // If there is already a polygon, remove it
//            if (areaPolygon != null) {
//                mapView.getOverlays().remove(areaPolygon);
//            }
//
//            // Create a new polygon and set points
//            areaPolygon = new Polygon();
//            areaPolygon.setPoints(List.of(points[0], points[1], points[2], points[3], points[0])); // Create a polygon
//            mapView.getOverlays().add(areaPolygon);
//            mapView.invalidate();  // Redraw the map
//        }
//    }
//
//    private void saveAreaToCsv() {
//        if (points[0] == null || points[1] == null || points[2] == null || points[3] == null) {
//            Toast.makeText(this, "Select all 4 points", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        String roadName = getRoadName(points[0]);  // Using the first point's road name
//        File file = new File(getExternalFilesDir(null), FILE_NAME);
//        boolean isNewFile = !file.exists();
//
//        try (FileWriter writer = new FileWriter(file, true)) {
//            if (isNewFile) {
//                writer.append("Road Name,Point 1 Latitude,Point 1 Longitude,Point 2 Latitude,Point 2 Longitude,Point 3 Latitude,Point 3 Longitude,Point 4 Latitude,Point 4 Longitude\n");
//            }
//            String data = roadName + "," + points[0].getLatitude() + "," + points[0].getLongitude() + "," +
//                    points[1].getLatitude() + "," + points[1].getLongitude() + "," +
//                    points[2].getLatitude() + "," + points[2].getLongitude() + "," +
//                    points[3].getLatitude() + "," + points[3].getLongitude() + "\n";
//            writer.append(data);
//            writer.flush();
//
//            Log.d(TAG, "CSV Saved: " + data);
//            Toast.makeText(this, "Area saved!", Toast.LENGTH_SHORT).show();
//        } catch (IOException e) {
//            Log.e(TAG, "Error writing CSV", e);
//        }
//    }
//
//    private String getRoadName(GeoPoint geoPoint) {
//        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
//        try {
//            List<Address> addresses = geocoder.getFromLocation(geoPoint.getLatitude(), geoPoint.getLongitude(), 1);
//            if (addresses != null && !addresses.isEmpty()) {
//                return addresses.get(0).getThoroughfare();
//            }
//        } catch (IOException e) {
//            Log.e(TAG, "Geocoder failed", e);
//        }
//        return "Unknown Road";
//    }
//}











//----------------- first primary version------
//package com.example.myapplication;
//
//import android.location.Address;
//import android.location.Geocoder;
//import android.os.Bundle;
//import android.util.Log;
//import android.view.View;
//import android.widget.Button;
//import android.widget.SearchView;
//import android.widget.Toast;
//
//import androidx.appcompat.app.AppCompatActivity;
//
//import org.json.JSONArray;
//import org.json.JSONObject;
//import org.osmdroid.api.IMapController;
//import org.osmdroid.config.Configuration;
//import org.osmdroid.events.MapEventsReceiver;
//import org.osmdroid.util.GeoPoint;
//import org.osmdroid.views.MapView;
//import org.osmdroid.views.overlay.MapEventsOverlay;
//import org.osmdroid.views.overlay.Marker;
//import org.osmdroid.views.overlay.Polygon;
//
//import java.io.File;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.util.List;
//import java.util.Locale;
//
//import okhttp3.OkHttpClient;
//import okhttp3.Request;
//import okhttp3.Response;
//
//public class ReportIssueActivity extends AppCompatActivity {
//
//    private static final String TAG = "ReportIssueActivity";
//    private static final String FILE_NAME = "route_data.csv";
//
//    private MapView mapView;
//    private SearchView searchView;
//    private GeoPoint[] points = new GeoPoint[4]; // Store the 4 points selected
//    private Button btnSaveRoute, btnClearPoints;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_report_issue);
//
//        // Initialize map
//        Configuration.getInstance().setUserAgentValue(getApplicationContext().getPackageName());
//        mapView = findViewById(R.id.mapView);
//        searchView = findViewById(R.id.search_view);
//        mapView.setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK);
//        mapView.setMultiTouchControls(true);
//
//        IMapController mapController = mapView.getController();
//        mapController.setZoom(15);
//        mapController.setCenter(new GeoPoint(23.8103, 90.4125)); // Default center Dhaka
//
//        // Initialize buttons
//        btnSaveRoute = findViewById(R.id.btn_save_route);
//        btnClearPoints = findViewById(R.id.btn_clear_points);
//
//        btnSaveRoute.setVisibility(View.GONE);
//        btnClearPoints.setVisibility(View.GONE);
//
//        btnSaveRoute.setOnClickListener(v -> saveAreaToCsv());
//        btnClearPoints.setOnClickListener(v -> clearPoints());
//
//        // Setup Listeners
//        setupMapTouchListener();
//        setupSearchView();
//    }
//
//    private void clearPoints() {
//        // Clear the points and remove the polygon
//        for (int i = 0; i < points.length; i++) {
//            points[i] = null;
//        }
//        btnSaveRoute.setVisibility(View.GONE);
//        btnClearPoints.setVisibility(View.GONE);
//        mapView.getOverlays().clear();
//        setupMapTouchListener(); // Reset map listener
//        Toast.makeText(this, "Points Cleared", Toast.LENGTH_SHORT).show();
//        Log.d(TAG, "Points Cleared");
//    }
//
//    private void setupSearchView() {
//        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
//            @Override
//            public boolean onQueryTextSubmit(String query) {
//                Log.d(TAG, "Search Query Submitted: " + query);
//                fetchCoordinates(query);
//                searchView.clearFocus();
//                return true;
//            }
//
//            @Override
//            public boolean onQueryTextChange(String newText) {
//                return false;
//            }
//        });
//    }
//
//    private void fetchCoordinates(String query) {
//        // Nominatim API with Dhaka bounding box
//        String apiUrl = String.format(Locale.getDefault(),
//                "https://nominatim.openstreetmap.org/search?q=%s&format=json&addressdetails=1" +
//                        "&viewbox=90.2792,23.7104,90.5120,23.9135&bounded=1", query);
//
//        OkHttpClient client = new OkHttpClient();
//        Request request = new Request.Builder().url(apiUrl).build();
//
//        new Thread(() -> {
//            try {
//                Response response = client.newCall(request).execute();
//                if (response.isSuccessful() && response.body() != null) {
//                    String jsonResponse = response.body().string();
//                    Log.d(TAG, "Search API Response: " + jsonResponse);
//                    runOnUiThread(() -> parseCoordinates(jsonResponse));
//                }
//            } catch (Exception e) {
//                Log.e(TAG, "Error fetching coordinates", e);
//            }
//        }).start();
//    }
//
//    private void parseCoordinates(String jsonResponse) {
//        try {
//            JSONArray jsonArray = new JSONArray(jsonResponse);
//            if (jsonArray.length() > 0) {
//                JSONObject locationObject = jsonArray.getJSONObject(0);
//                double lat = locationObject.getDouble("lat");
//                double lon = locationObject.getDouble("lon");
//
//                GeoPoint geoPoint = new GeoPoint(lat, lon);
//                mapView.getController().setCenter(geoPoint);
//
//                Marker marker = new Marker(mapView);
//                marker.setPosition(geoPoint);
//                marker.setTitle("Searched Location");
//                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
//                mapView.getOverlays().add(marker);
//                mapView.invalidate();
//
//                Log.d(TAG, "Search Location Plotted: " + lat + ", " + lon);
//            } else {
//                Toast.makeText(this, "No results found", Toast.LENGTH_SHORT).show();
//            }
//        } catch (Exception e) {
//            Log.e(TAG, "Error parsing coordinates", e);
//        }
//    }
//
//    private void setupMapTouchListener() {
//        MapEventsOverlay mapEventsOverlay = new MapEventsOverlay(new MapEventsReceiver() {
//            @Override
//            public boolean singleTapConfirmedHelper(GeoPoint p) {
//                for (int i = 0; i < points.length; i++) {
//                    if (points[i] == null) {
//                        points[i] = p;
//                        addMarker(p, "Point " + (i + 1));
//                        if (i == 3) {  // Once all 4 points are selected
//                            drawPolygon();
//                            btnSaveRoute.setVisibility(View.VISIBLE);
//                            btnClearPoints.setVisibility(View.VISIBLE);
//                        }
//                        Log.d(TAG, "Point " + (i + 1) + " Selected: " + p.getLatitude() + ", " + p.getLongitude());
//                        break;
//                    }
//                }
//                return true;
//            }
//
//            @Override
//            public boolean longPressHelper(GeoPoint p) {
//                return false;
//            }
//        });
//
//        mapView.getOverlays().add(mapEventsOverlay);
//    }
//
//    private void addMarker(GeoPoint point, String title) {
//        Marker marker = new Marker(mapView);
//        marker.setPosition(point);
//        marker.setTitle(title);
//        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
//        mapView.getOverlays().add(marker);
//        mapView.invalidate();
//    }
//
//    private void drawPolygon() {
//        if (points[0] != null && points[1] != null && points[2] != null && points[3] != null) {
//            Polygon polygon = new Polygon();
//            polygon.setPoints(List.of(points[0], points[1], points[2], points[3], points[0])); // Create a polygon
//            mapView.getOverlays().add(polygon);
//            mapView.invalidate();
//        }
//    }
//
//    private void saveAreaToCsv() {
//        if (points[0] == null || points[1] == null || points[2] == null || points[3] == null) {
//            Toast.makeText(this, "Select all 4 points", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        String roadName = getRoadName(points[0]);  // Using the first point's road name
//        File file = new File(getExternalFilesDir(null), FILE_NAME);
//        boolean isNewFile = !file.exists();
//
//        try (FileWriter writer = new FileWriter(file, true)) {
//            if (isNewFile) {
//                writer.append("Road Name,Point 1 Latitude,Point 1 Longitude,Point 2 Latitude,Point 2 Longitude,Point 3 Latitude,Point 3 Longitude,Point 4 Latitude,Point 4 Longitude\n");
//            }
//            String data = roadName + "," + points[0].getLatitude() + "," + points[0].getLongitude() + "," +
//                    points[1].getLatitude() + "," + points[1].getLongitude() + "," +
//                    points[2].getLatitude() + "," + points[2].getLongitude() + "," +
//                    points[3].getLatitude() + "," + points[3].getLongitude() + "\n";
//            writer.append(data);
//            writer.flush();
//
//            Log.d(TAG, "CSV Saved: " + data);
//            Toast.makeText(this, "Area saved!", Toast.LENGTH_SHORT).show();
//        } catch (IOException e) {
//            Log.e(TAG, "Error writing CSV", e);
//        }
//    }
//
//    private String getRoadName(GeoPoint geoPoint) {
//        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
//        try {
//            List<Address> addresses = geocoder.getFromLocation(geoPoint.getLatitude(), geoPoint.getLongitude(), 1);
//            if (addresses != null && !addresses.isEmpty()) {
//                return addresses.get(0).getThoroughfare();
//            }
//        } catch (IOException e) {
//            Log.e(TAG, "Geocoder failed", e);
//        }
//        return "Unknown Road";
//    }
//}



//
//
//package com.example.myapplication;
//
//import android.location.Address;
//import android.location.Geocoder;
//import android.os.Bundle;
//import android.util.Log;
//import android.view.View;
//import android.widget.Button;
//import android.widget.SearchView;
//import android.widget.Toast;
//
//import androidx.appcompat.app.AppCompatActivity;
//
//import org.json.JSONArray;
//import org.json.JSONObject;
//import org.osmdroid.api.IMapController;
//import org.osmdroid.config.Configuration;
//import org.osmdroid.events.MapEventsReceiver;
//import org.osmdroid.util.GeoPoint;
//import org.osmdroid.views.MapView;
//import org.osmdroid.views.overlay.MapEventsOverlay;
//import org.osmdroid.views.overlay.Marker;
//import org.osmdroid.views.overlay.Polygon;
//
//import java.io.File;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.util.List;
//import java.util.Locale;
//
//import okhttp3.OkHttpClient;
//import okhttp3.Request;
//import okhttp3.Response;
//
//public class ReportIssueActivity extends AppCompatActivity {
//
//    private static final String TAG = "ReportIssueActivity";
//    private static final String FILE_NAME = "route_data.csv";
//
//    private MapView mapView;
//    private SearchView searchView;
//    private GeoPoint[] points = new GeoPoint[4]; // Store the 4 points selected
//    private Button btnSaveRoute, btnClearPoints;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_report_issue);
//
//        // Initialize map
//        Configuration.getInstance().setUserAgentValue(getApplicationContext().getPackageName());
//        mapView = findViewById(R.id.mapView);
//        searchView = findViewById(R.id.search_view);
//        mapView.setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK);
//        mapView.setMultiTouchControls(true);
//
//        IMapController mapController = mapView.getController();
//        mapController.setZoom(15);
//        mapController.setCenter(new GeoPoint(23.8103, 90.4125)); // Default center Dhaka
//
//        // Initialize buttons
//        btnSaveRoute = findViewById(R.id.btn_save_route);
//        btnClearPoints = findViewById(R.id.btn_clear_points);
//
//        btnSaveRoute.setVisibility(View.GONE);
//        btnClearPoints.setVisibility(View.GONE);
//
//        btnSaveRoute.setOnClickListener(v -> saveAreaToCsv());
//        btnClearPoints.setOnClickListener(v -> clearPoints());
//
//        // Setup Listeners
//        setupMapTouchListener();
//        setupSearchView();
//    }
//
//    private void clearPoints() {
//        // Remove the last selected point (i.e., point 4, 3, 2, 1)
//        for (int i = points.length - 1; i >= 0; i--) {
//            if (points[i] != null) {
//                points[i] = null;
//                mapView.getOverlays().clear(); // Clear the existing markers and polygon
//                for (int j = 0; j < i; j++) { // Re-add remaining markers
//                    addMarker(points[j], "Point " + (j + 1));
//                }
//                setupMapTouchListener(); // Reset map listener
//                Toast.makeText(this, "Last Point Cleared", Toast.LENGTH_SHORT).show();
//                Log.d(TAG, "Last Point Cleared");
//                return;
//            }
//        }
//        Toast.makeText(this, "No Points to Clear", Toast.LENGTH_SHORT).show();
//    }
//
//    private void setupSearchView() {
//        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
//            @Override
//            public boolean onQueryTextSubmit(String query) {
//                Log.d(TAG, "Search Query Submitted: " + query);
//                fetchCoordinates(query);
//                searchView.clearFocus();
//                return true;
//            }
//
//            @Override
//            public boolean onQueryTextChange(String newText) {
//                return false;
//            }
//        });
//    }
//
//    private void fetchCoordinates(String query) {
//        // Nominatim API with Dhaka bounding box
//        String apiUrl = String.format(Locale.getDefault(),
//                "https://nominatim.openstreetmap.org/search?q=%s&format=json&addressdetails=1" +
//                        "&viewbox=90.2792,23.7104,90.5120,23.9135&bounded=1", query);
//
//        OkHttpClient client = new OkHttpClient();
//        Request request = new Request.Builder().url(apiUrl).build();
//
//        new Thread(() -> {
//            try {
//                Response response = client.newCall(request).execute();
//                if (response.isSuccessful() && response.body() != null) {
//                    String jsonResponse = response.body().string();
//                    Log.d(TAG, "Search API Response: " + jsonResponse);
//                    runOnUiThread(() -> parseCoordinates(jsonResponse));
//                }
//            } catch (Exception e) {
//                Log.e(TAG, "Error fetching coordinates", e);
//            }
//        }).start();
//    }
//
//    private void parseCoordinates(String jsonResponse) {
//        try {
//            JSONArray jsonArray = new JSONArray(jsonResponse);
//            if (jsonArray.length() > 0) {
//                JSONObject locationObject = jsonArray.getJSONObject(0);
//                double lat = locationObject.getDouble("lat");
//                double lon = locationObject.getDouble("lon");
//
//                GeoPoint geoPoint = new GeoPoint(lat, lon);
//                mapView.getController().setCenter(geoPoint);
//
//                Marker marker = new Marker(mapView);
//                marker.setPosition(geoPoint);
//                marker.setTitle("Searched Location");
//                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
//                mapView.getOverlays().add(marker);
//                mapView.invalidate();
//
//                Log.d(TAG, "Search Location Plotted: " + lat + ", " + lon);
//            } else {
//                Toast.makeText(this, "No results found", Toast.LENGTH_SHORT).show();
//            }
//        } catch (Exception e) {
//            Log.e(TAG, "Error parsing coordinates", e);
//        }
//    }
//
//    private void setupMapTouchListener() {
//        MapEventsOverlay mapEventsOverlay = new MapEventsOverlay(new MapEventsReceiver() {
//            @Override
//            public boolean singleTapConfirmedHelper(GeoPoint p) {
//                for (int i = 0; i < points.length; i++) {
//                    if (points[i] == null) {
//                        points[i] = p;
//                        addMarker(p, "Point " + (i + 1));
//                        if (i == 3) {  // After selecting 4 points
//                            drawPolygon(); // Draw polygon after selecting 4 points
//                            btnSaveRoute.setVisibility(View.VISIBLE); // Show Save button
//                            btnClearPoints.setVisibility(View.VISIBLE); // Show Clear button
//                        }
//                        Log.d(TAG, "Point " + (i + 1) + " Selected: " + p.getLatitude() + ", " + p.getLongitude());
//                        break;
//                    }
//                }
//                return true;
//            }
//
//            @Override
//            public boolean longPressHelper(GeoPoint p) {
//                return false;
//            }
//        });
//
//        mapView.getOverlays().add(mapEventsOverlay);
//    }
//
//    private void addMarker(GeoPoint point, String title) {
//        Marker marker = new Marker(mapView);
//        marker.setPosition(point);
//        marker.setTitle(title);
//        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
//        mapView.getOverlays().add(marker);
//        mapView.invalidate();
//    }
//
//    private void drawPolygon() {
//        if (points[0] != null && points[1] != null && points[2] != null && points[3] != null) {
//            Polygon polygon = new Polygon();
//            polygon.setPoints(List.of(points[0], points[1], points[2], points[3], points[0])); // Create a polygon
//            polygon.setFillColor(0x220000FF);  // Transparent blue
//            polygon.setStrokeColor(0xFF0000FF); // Blue outline
//            polygon.setStrokeWidth(2.5f);
//            mapView.getOverlays().add(polygon);
//            mapView.invalidate();
//        }
//    }
//
//    private void saveAreaToCsv() {
//        if (points[0] == null || points[1] == null || points[2] == null || points[3] == null) {
//            Toast.makeText(this, "Select all 4 points", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        String roadName = getRoadName(points[0]);  // Using the first point's road name
//        File file = new File(getExternalFilesDir(null), FILE_NAME);
//        boolean isNewFile = !file.exists();
//
//        try (FileWriter writer = new FileWriter(file, true)) {
//            if (isNewFile) {
//                writer.append("Road Name,Point 1 Latitude,Point 1 Longitude,Point 2 Latitude,Point 2 Longitude,Point 3 Latitude,Point 3 Longitude,Point 4 Latitude,Point 4 Longitude\n");
//            }
//            String data = roadName + "," + points[0].getLatitude() + "," + points[0].getLongitude() + "," +
//                    points[1].getLatitude() + "," + points[1].getLongitude() + "," +
//                    points[2].getLatitude() + "," + points[2].getLongitude() + "," +
//                    points[3].getLatitude() + "," + points[3].getLongitude() + "\n";
//            writer.append(data);
//            writer.flush();
//
//            Log.d(TAG, "CSV Saved: " + data);
//            Toast.makeText(this, "Area saved!", Toast.LENGTH_SHORT).show();
//        } catch (IOException e) {
//            Log.e(TAG, "Error writing CSV", e);
//        }
//    }
//
//    private String getRoadName(GeoPoint geoPoint) {
//        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
//        try {
//            List<Address> addresses = geocoder.getFromLocation(geoPoint.getLatitude(), geoPoint.getLongitude(), 1);
//            if (addresses != null && !addresses.isEmpty()) {
//                return addresses.get(0).getThoroughfare();
//            }
//        } catch (IOException e) {
//            Log.e(TAG, "Geocoder failed", e);
//        }
//        return "Unknown Road";
//    }
//}



//-----------------------best one so far---------------------
//
//package com.example.myapplication;
//
//import android.location.Address;
//import android.location.Geocoder;
//import android.os.Bundle;
//import android.util.Log;
//import android.view.View;
//import android.widget.Button;
//import android.widget.SearchView;
//import android.widget.Toast;
//
//import androidx.appcompat.app.AppCompatActivity;
//
//import org.json.JSONArray;
//import org.json.JSONObject;
//import org.osmdroid.api.IMapController;
//import org.osmdroid.config.Configuration;
//import org.osmdroid.events.MapEventsReceiver;
//import org.osmdroid.util.GeoPoint;
//import org.osmdroid.views.MapView;
//import org.osmdroid.views.overlay.MapEventsOverlay;
//import org.osmdroid.views.overlay.Marker;
//import org.osmdroid.views.overlay.Polygon;
//
//import java.io.File;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.util.List;
//import java.util.Locale;
//
//import okhttp3.OkHttpClient;
//import okhttp3.Request;
//import okhttp3.Response;
//
//public class ReportIssueActivity extends AppCompatActivity {
//
//    private static final String TAG = "ReportIssueActivity";
//    private static final String FILE_NAME = "route_data.csv";
//
//    private MapView mapView;
//    private SearchView searchView;
//    private GeoPoint[] points = new GeoPoint[4]; // Store the 4 points selected
//    private Button btnSaveRoute, btnClearPoints;
//    private Polygon polygon; // Polygon to hold the drawn polygon
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_report_issue);
//
//        // Initialize map
//        Configuration.getInstance().setUserAgentValue(getApplicationContext().getPackageName());
//        mapView = findViewById(R.id.mapView);
//        searchView = findViewById(R.id.search_view);
//        mapView.setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK);
//        mapView.setMultiTouchControls(true);
//
//        IMapController mapController = mapView.getController();
//        mapController.setZoom(15);
//        mapController.setCenter(new GeoPoint(23.8103, 90.4125)); // Default center Dhaka
//
//        // Initialize buttons
//        btnSaveRoute = findViewById(R.id.btn_save_route);
//        btnClearPoints = findViewById(R.id.btn_clear_points);
//
//        btnSaveRoute.setVisibility(View.GONE);
//        btnClearPoints.setVisibility(View.GONE);
//
//        btnSaveRoute.setOnClickListener(v -> saveAreaToCsv());
//        btnClearPoints.setOnClickListener(v -> clearPoints());
//
//        // Setup Listeners
//        setupMapTouchListener();
//        setupSearchView();
//    }
//
//    private void clearPoints() {
//        // Remove the last selected point (i.e., point 4, 3, 2, 1)
//        for (int i = points.length - 1; i >= 0; i--) {
//            if (points[i] != null) {
//                points[i] = null;
//                mapView.getOverlays().clear(); // Clear the existing markers and polygon
//                for (int j = 0; j < i; j++) { // Re-add remaining markers
//                    addMarker(points[j], "Point " + (j + 1));
//                }
//                setupMapTouchListener(); // Reset map listener
//                Toast.makeText(this, "Last Point Cleared", Toast.LENGTH_SHORT).show();
//                Log.d(TAG, "Last Point Cleared");
//                return;
//            }
//        }
//        Toast.makeText(this, "No Points to Clear", Toast.LENGTH_SHORT).show();
//    }
//
//    private void setupSearchView() {
//        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
//            @Override
//            public boolean onQueryTextSubmit(String query) {
//                Log.d(TAG, "Search Query Submitted: " + query);
//                fetchCoordinates(query);
//                searchView.clearFocus();
//                return true;
//            }
//
//            @Override
//            public boolean onQueryTextChange(String newText) {
//                return false;
//            }
//        });
//    }
//
//    private void fetchCoordinates(String query) {
//        // Nominatim API with Dhaka bounding box
//        String apiUrl = String.format(Locale.getDefault(),
//                "https://nominatim.openstreetmap.org/search?q=%s&format=json&addressdetails=1" +
//                        "&viewbox=90.2792,23.7104,90.5120,23.9135&bounded=1", query);
//
//        OkHttpClient client = new OkHttpClient();
//        Request request = new Request.Builder().url(apiUrl).build();
//
//        new Thread(() -> {
//            try {
//                Response response = client.newCall(request).execute();
//                if (response.isSuccessful() && response.body() != null) {
//                    String jsonResponse = response.body().string();
//                    Log.d(TAG, "Search API Response: " + jsonResponse);
//                    runOnUiThread(() -> parseCoordinates(jsonResponse));
//                }
//            } catch (Exception e) {
//                Log.e(TAG, "Error fetching coordinates", e);
//            }
//        }).start();
//    }
//
//    private void parseCoordinates(String jsonResponse) {
//        try {
//            JSONArray jsonArray = new JSONArray(jsonResponse);
//            if (jsonArray.length() > 0) {
//                JSONObject locationObject = jsonArray.getJSONObject(0);
//                double lat = locationObject.getDouble("lat");
//                double lon = locationObject.getDouble("lon");
//
//                GeoPoint geoPoint = new GeoPoint(lat, lon);
//                mapView.getController().setCenter(geoPoint);
//
//                Marker marker = new Marker(mapView);
//                marker.setPosition(geoPoint);
//                marker.setTitle("Searched Location");
//                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
//                mapView.getOverlays().add(marker);
//                mapView.invalidate();
//
//                Log.d(TAG, "Search Location Plotted: " + lat + ", " + lon);
//            } else {
//                Toast.makeText(this, "No results found", Toast.LENGTH_SHORT).show();
//            }
//        } catch (Exception e) {
//            Log.e(TAG, "Error parsing coordinates", e);
//        }
//    }
//
//    private void setupMapTouchListener() {
//        MapEventsOverlay mapEventsOverlay = new MapEventsOverlay(new MapEventsReceiver() {
//            @Override
//            public boolean singleTapConfirmedHelper(GeoPoint p) {
//                for (int i = 0; i < points.length; i++) {
//                    if (points[i] == null) {
//                        points[i] = p;
//                        addMarker(p, "Point " + (i + 1));
//                        if (i == 3) {  // After selecting 4 points
//                            drawPolygon(); // Draw polygon after selecting 4 points
//                            btnSaveRoute.setVisibility(View.VISIBLE); // Show Save button
//                            btnClearPoints.setVisibility(View.VISIBLE); // Show Clear button
//                        }
//                        Log.d(TAG, "Point " + (i + 1) + " Selected: " + p.getLatitude() + ", " + p.getLongitude());
//                        break;
//                    }
//                }
//                return true;
//            }
//
//            @Override
//            public boolean longPressHelper(GeoPoint p) {
//                return false;
//            }
//        });
//
//        mapView.getOverlays().add(mapEventsOverlay);
//    }
//
//    private void addMarker(GeoPoint point, String title) {
//        Marker marker = new Marker(mapView);
//        marker.setPosition(point);
//        marker.setTitle(title);
//        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
//        mapView.getOverlays().add(marker);
//        mapView.invalidate();
//    }
//
//    private void drawPolygon() {
//        if (points[0] != null && points[1] != null && points[2] != null && points[3] != null) {
//            polygon = new Polygon();
//            polygon.setPoints(List.of(points[0], points[1], points[2], points[3], points[0])); // Create a polygon
//            polygon.setFillColor(0x220000FF);  // Transparent blue
//            polygon.setStrokeColor(0xFF0000FF); // Blue outline
//            polygon.setStrokeWidth(2.5f);
//            mapView.getOverlays().add(polygon);
//            mapView.invalidate();
//        }
//    }
//
//    private void saveAreaToCsv() {
//        if (points[0] == null || points[1] == null || points[2] == null || points[3] == null) {
//            Toast.makeText(this, "Select all 4 points", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        String roadName = getRoadName(points[0]);  // Using the first point's road name
//        File file = new File(getExternalFilesDir(null), FILE_NAME);
//        boolean isNewFile = !file.exists();
//
//        try (FileWriter writer = new FileWriter(file, true)) {
//            if (isNewFile) {
//                writer.append("Road Name,Point 1 Latitude,Point 1 Longitude,Point 2 Latitude,Point 2 Longitude,Point 3 Latitude,Point 3 Longitude,Point 4 Latitude,Point 4 Longitude\n");
//            }
//            String data = roadName + "," + points[0].getLatitude() + "," + points[0].getLongitude() + "," +
//                    points[1].getLatitude() + "," + points[1].getLongitude() + "," +
//                    points[2].getLatitude() + "," + points[2].getLongitude() + "," +
//                    points[3].getLatitude() + "," + points[3].getLongitude() + "\n";
//            writer.append(data);
//            writer.flush();
//
//            Log.d(TAG, "CSV Saved: " + data);
//            Toast.makeText(this, "Area saved!", Toast.LENGTH_SHORT).show();
//
//            // Clear points after saving
//            clearPoints();
//        } catch (IOException e) {
//            Log.e(TAG, "Error writing CSV", e);
//        }
//    }
//
//    private String getRoadName(GeoPoint geoPoint) {
//        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
//        try {
//            List<Address> addresses = geocoder.getFromLocation(geoPoint.getLatitude(), geoPoint.getLongitude(), 1);
//            if (addresses != null && !addresses.isEmpty()) {
//                return addresses.get(0).getThoroughfare();
//            }
//        } catch (IOException e) {
//            Log.e(TAG, "Geocoder failed", e);
//        }
//        return "Unknown Road";
//    }
//}



//------polygone is shown----------------------------
//package com.example.myapplication;
//
//import android.location.Address;
//import android.location.Geocoder;
//import android.os.Bundle;
//import android.util.Log;
//import android.view.View;
//import android.widget.Button;
//import android.widget.SearchView;
//import android.widget.Toast;
//
//import androidx.appcompat.app.AppCompatActivity;
//
//import org.json.JSONArray;
//import org.json.JSONObject;
//import org.osmdroid.api.IMapController;
//import org.osmdroid.config.Configuration;
//import org.osmdroid.events.MapEventsReceiver;
//import org.osmdroid.util.GeoPoint;
//import org.osmdroid.views.MapView;
//import org.osmdroid.views.overlay.MapEventsOverlay;
//import org.osmdroid.views.overlay.Marker;
//import org.osmdroid.views.overlay.Polygon;
//
//import java.io.File;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.util.ArrayList; // Import ArrayList
//import java.util.List;
//import java.util.Locale;
//
//import okhttp3.OkHttpClient;
//import okhttp3.Request;
//import okhttp3.Response;
//
//public class ReportIssueActivity extends AppCompatActivity {
//
//    private static final String TAG = "ReportIssueActivity";
//    private static final String FILE_NAME = "route_data.csv";
//
//    private MapView mapView;
//    private SearchView searchView;
//    private GeoPoint[] points = new GeoPoint[4]; // Store the 4 points selected
//    private Button btnSaveRoute, btnClearPoints;
//    private Polygon polygon; // Polygon to hold the drawn polygon
//    private List<Polygon> savedPolygons = new ArrayList<>(); // Initialize the list to store saved polygons
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_report_issue);
//
//        // Initialize map
//        Configuration.getInstance().setUserAgentValue(getApplicationContext().getPackageName());
//        mapView = findViewById(R.id.mapView);
//        searchView = findViewById(R.id.search_view);
//        mapView.setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK);
//        mapView.setMultiTouchControls(true);
//
//        IMapController mapController = mapView.getController();
//        mapController.setZoom(15);
//        mapController.setCenter(new GeoPoint(23.8103, 90.4125)); // Default center Dhaka
//
//        // Initialize buttons
//        btnSaveRoute = findViewById(R.id.btn_save_route);
//        btnClearPoints = findViewById(R.id.btn_clear_points);
//
//        btnSaveRoute.setVisibility(View.GONE);
//        btnClearPoints.setVisibility(View.GONE);
//
//        btnSaveRoute.setOnClickListener(v -> saveAreaToCsv());
//        btnClearPoints.setOnClickListener(v -> clearPoints());
//
//        // Setup Listeners
//        setupMapTouchListener();
//        setupSearchView();
//    }
//
//    private void clearPoints() {
//        // Remove all the selected points
//        for (int i = 0; i < points.length; i++) {
//            points[i] = null;
//        }
//
//        mapView.getOverlays().clear(); // Clear the existing markers and polygons
//
//        // Re-add saved polygons to the map
//        for (Polygon savedPolygon : savedPolygons) {
//            mapView.getOverlays().add(savedPolygon);
//        }
//
//        setupMapTouchListener(); // Reset map listener
//        Toast.makeText(this, "Points Cleared", Toast.LENGTH_SHORT).show();
//    }
//
//    private void setupSearchView() {
//        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
//            @Override
//            public boolean onQueryTextSubmit(String query) {
//                Log.d(TAG, "Search Query Submitted: " + query);
//                fetchCoordinates(query);
//                searchView.clearFocus();
//                return true;
//            }
//
//            @Override
//            public boolean onQueryTextChange(String newText) {
//                return false;
//            }
//        });
//    }
//
//    private void fetchCoordinates(String query) {
//        // Nominatim API with Dhaka bounding box
//        String apiUrl = String.format(Locale.getDefault(),
//                "https://nominatim.openstreetmap.org/search?q=%s&format=json&addressdetails=1" +
//                        "&viewbox=90.2792,23.7104,90.5120,23.9135&bounded=1", query);
//
//        OkHttpClient client = new OkHttpClient();
//        Request request = new Request.Builder().url(apiUrl).build();
//
//        new Thread(() -> {
//            try {
//                Response response = client.newCall(request).execute();
//                if (response.isSuccessful() && response.body() != null) {
//                    String jsonResponse = response.body().string();
//                    Log.d(TAG, "Search API Response: " + jsonResponse);
//                    runOnUiThread(() -> parseCoordinates(jsonResponse));
//                }
//            } catch (Exception e) {
//                Log.e(TAG, "Error fetching coordinates", e);
//            }
//        }).start();
//    }
//
//    private void parseCoordinates(String jsonResponse) {
//        try {
//            JSONArray jsonArray = new JSONArray(jsonResponse);
//            if (jsonArray.length() > 0) {
//                JSONObject locationObject = jsonArray.getJSONObject(0);
//                double lat = locationObject.getDouble("lat");
//                double lon = locationObject.getDouble("lon");
//
//                GeoPoint geoPoint = new GeoPoint(lat, lon);
//                mapView.getController().setCenter(geoPoint);
//
//                Marker marker = new Marker(mapView);
//                marker.setPosition(geoPoint);
//                marker.setTitle("Searched Location");
//                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
//                mapView.getOverlays().add(marker);
//                mapView.invalidate();
//
//                Log.d(TAG, "Search Location Plotted: " + lat + ", " + lon);
//            } else {
//                Toast.makeText(this, "No results found", Toast.LENGTH_SHORT).show();
//            }
//        } catch (Exception e) {
//            Log.e(TAG, "Error parsing coordinates", e);
//        }
//    }
//
//    private void setupMapTouchListener() {
//        MapEventsOverlay mapEventsOverlay = new MapEventsOverlay(new MapEventsReceiver() {
//            @Override
//            public boolean singleTapConfirmedHelper(GeoPoint p) {
//                for (int i = 0; i < points.length; i++) {
//                    if (points[i] == null) {
//                        points[i] = p;
//                        addMarker(p, "Point " + (i + 1));
//                        if (i == 3) {  // After selecting 4 points
//                            drawPolygon(); // Draw polygon after selecting 4 points
//                            btnSaveRoute.setVisibility(View.VISIBLE); // Show Save button
//                            btnClearPoints.setVisibility(View.VISIBLE); // Show Clear button
//                        }
//                        Log.d(TAG, "Point " + (i + 1) + " Selected: " + p.getLatitude() + ", " + p.getLongitude());
//                        break;
//                    }
//                }
//                return true;
//            }
//
//            @Override
//            public boolean longPressHelper(GeoPoint p) {
//                return false;
//            }
//        });
//
//        mapView.getOverlays().add(mapEventsOverlay);
//    }
//
//    private void addMarker(GeoPoint point, String title) {
//        Marker marker = new Marker(mapView);
//        marker.setPosition(point);
//        marker.setTitle(title);
//        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
//        mapView.getOverlays().add(marker);
//        mapView.invalidate();
//    }
//
//    private void drawPolygon() {
//        if (points[0] != null && points[1] != null && points[2] != null && points[3] != null) {
//            polygon = new Polygon();
//            polygon.setPoints(List.of(points[0], points[1], points[2], points[3], points[0])); // Create a polygon
//            polygon.setFillColor(0x220000FF);  // Transparent blue
//            polygon.setStrokeColor(0xFF0000FF); // Blue outline
//            polygon.setStrokeWidth(2.5f);
//            mapView.getOverlays().add(polygon);
//
//            // Add to savedPolygons list to keep track of polygons
//            savedPolygons.add(polygon);
//
//            mapView.invalidate();
//        }
//    }
//
//    private void saveAreaToCsv() {
//        if (points[0] == null || points[1] == null || points[2] == null || points[3] == null) {
//            Toast.makeText(this, "Select all 4 points", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        String roadName = getRoadName(points[0]);  // Using the first point's road name
//        File file = new File(getExternalFilesDir(null), FILE_NAME);
//        boolean isNewFile = !file.exists();
//
//        try (FileWriter writer = new FileWriter(file, true)) {
//            if (isNewFile) {
//                writer.append("Road Name,Point 1 Latitude,Point 1 Longitude,Point 2 Latitude,Point 2 Longitude,Point 3 Latitude,Point 3 Longitude,Point 4 Latitude,Point 4 Longitude\n");
//            }
//            String data = roadName + "," + points[0].getLatitude() + "," + points[0].getLongitude() + "," +
//                    points[1].getLatitude() + "," + points[1].getLongitude() + "," +
//                    points[2].getLatitude() + "," + points[2].getLongitude() + "," +
//                    points[3].getLatitude() + "," + points[3].getLongitude() + "\n";
//            writer.append(data);
//            writer.flush();
//
//            Log.d(TAG, "CSV Saved: " + data);
//            Toast.makeText(this, "Area saved!", Toast.LENGTH_SHORT).show();
//
//            // Clear points after saving
//            clearPoints();
//        } catch (IOException e) {
//            Log.e(TAG, "Error writing CSV", e);
//        }
//    }
//
//    private String getRoadName(GeoPoint geoPoint) {
//        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
//        try {
//            List<Address> addresses = geocoder.getFromLocation(geoPoint.getLatitude(), geoPoint.getLongitude(), 1);
//            if (addresses != null && !addresses.isEmpty()) {
//                return addresses.get(0).getThoroughfare();
//            }
//        } catch (IOException e) {
//            Log.e(TAG, "Geocoder failed", e);
//        }
//        return "Unknown Road";
//    }
//}































//
//
//
//
//package com.example.myapplication;
//
//import android.location.Address;
//import android.location.Geocoder;
//import android.os.Bundle;
//import android.util.Log;
//import android.view.View;
//import android.widget.Button;
//import android.widget.SearchView;
//import android.widget.Toast;
//
//import androidx.appcompat.app.AppCompatActivity;
//
//import org.json.JSONArray;
//import org.json.JSONObject;
//import org.osmdroid.api.IMapController;
//import org.osmdroid.config.Configuration;
//import org.osmdroid.events.MapEventsReceiver;
//import org.osmdroid.util.GeoPoint;
//import org.osmdroid.views.MapView;
//import org.osmdroid.views.overlay.MapEventsOverlay;
//import org.osmdroid.views.overlay.Marker;
//import org.osmdroid.views.overlay.Polygon;
//
//import java.io.File;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.util.ArrayList; // Import ArrayList
//import java.util.List;
//import java.util.Locale;
//
//import okhttp3.OkHttpClient;
//import okhttp3.Request;
//import okhttp3.Response;
//
//public class ReportIssueActivity extends AppCompatActivity {
//
//    private static final String TAG = "ReportIssueActivity";
//    private static final String FILE_NAME = "route_data.csv";
//
//    private MapView mapView;
//    private SearchView searchView;
//    private GeoPoint[] points = new GeoPoint[4]; // Store the 4 points selected
//    private Button btnSaveRoute, btnClearPoints;
//    private Polygon polygon; // Polygon to hold the drawn polygon
//    private List<Polygon> savedPolygons = new ArrayList<>(); // Initialize the list to store saved polygons
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_report_issue);
//
//        // Initialize map
//        Configuration.getInstance().setUserAgentValue(getApplicationContext().getPackageName());
//        mapView = findViewById(R.id.mapView);
//        searchView = findViewById(R.id.search_view);
//        mapView.setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK);
//        mapView.setMultiTouchControls(true);
//
//        IMapController mapController = mapView.getController();
//        mapController.setZoom(15);
//        mapController.setCenter(new GeoPoint(23.8103, 90.4125)); // Default center Dhaka
//
//        // Initialize buttons
//        btnSaveRoute = findViewById(R.id.btn_save_route);
//        btnClearPoints = findViewById(R.id.btn_clear_points);
//
//        btnSaveRoute.setVisibility(View.GONE);
//        btnClearPoints.setVisibility(View.GONE);
//
//        btnSaveRoute.setOnClickListener(v -> saveAreaToCsv());
//        btnClearPoints.setOnClickListener(v -> clearLastSelectedPoint());
//
//        // Setup Listeners
//        setupMapTouchListener();
//        setupSearchView();
//    }
//
//    // This will clear the last selected point
//    private void clearLastSelectedPoint() {
//        for (int i = points.length - 1; i >= 0; i--) {
//            if (points[i] != null) {
//                points[i] = null;
//                mapView.getOverlays().clear(); // Clear all markers and polygons
//
//                // Re-add the remaining saved polygons
//                for (Polygon savedPolygon : savedPolygons) {
//                    mapView.getOverlays().add(savedPolygon);
//                }
//
//                // Re-add the remaining points as markers
//                for (int j = 0; j < points.length; j++) {
//                    if (points[j] != null) {
//                        addMarker(points[j], "Point " + (j + 1));
//                    }
//                }
//
//                setupMapTouchListener(); // Reset the map listener
//                Toast.makeText(this, "Last Point Cleared", Toast.LENGTH_SHORT).show();
//                Log.d(TAG, "Last Point Cleared");
//                return;
//            }
//        }
//        Toast.makeText(this, "No Points to Clear", Toast.LENGTH_SHORT).show();
//    }
//
//    private void setupSearchView() {
//        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
//            @Override
//            public boolean onQueryTextSubmit(String query) {
//                Log.d(TAG, "Search Query Submitted: " + query);
//                fetchCoordinates(query);
//                searchView.clearFocus();
//                return true;
//            }
//
//            @Override
//            public boolean onQueryTextChange(String newText) {
//                return false;
//            }
//        });
//    }
//
//    private void fetchCoordinates(String query) {
//        // Nominatim API with Dhaka bounding box
//        String apiUrl = String.format(Locale.getDefault(),
//                "https://nominatim.openstreetmap.org/search?q=%s&format=json&addressdetails=1" +
//                        "&viewbox=90.2792,23.7104,90.5120,23.9135&bounded=1", query);
//
//        OkHttpClient client = new OkHttpClient();
//        Request request = new Request.Builder().url(apiUrl).build();
//
//        new Thread(() -> {
//            try {
//                Response response = client.newCall(request).execute();
//                if (response.isSuccessful() && response.body() != null) {
//                    String jsonResponse = response.body().string();
//                    Log.d(TAG, "Search API Response: " + jsonResponse);
//                    runOnUiThread(() -> parseCoordinates(jsonResponse));
//                }
//            } catch (Exception e) {
//                Log.e(TAG, "Error fetching coordinates", e);
//            }
//        }).start();
//    }
//
//    private void parseCoordinates(String jsonResponse) {
//        try {
//            JSONArray jsonArray = new JSONArray(jsonResponse);
//            if (jsonArray.length() > 0) {
//                JSONObject locationObject = jsonArray.getJSONObject(0);
//                double lat = locationObject.getDouble("lat");
//                double lon = locationObject.getDouble("lon");
//
//                GeoPoint geoPoint = new GeoPoint(lat, lon);
//                mapView.getController().setCenter(geoPoint);
//
//                Marker marker = new Marker(mapView);
//                marker.setPosition(geoPoint);
//                marker.setTitle("Searched Location");
//                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
//                mapView.getOverlays().add(marker);
//                mapView.invalidate();
//
//                Log.d(TAG, "Search Location Plotted: " + lat + ", " + lon);
//            } else {
//                Toast.makeText(this, "No results found", Toast.LENGTH_SHORT).show();
//            }
//        } catch (Exception e) {
//            Log.e(TAG, "Error parsing coordinates", e);
//        }
//    }
//
//    private void setupMapTouchListener() {
//        MapEventsOverlay mapEventsOverlay = new MapEventsOverlay(new MapEventsReceiver() {
//            @Override
//            public boolean singleTapConfirmedHelper(GeoPoint p) {
//                for (int i = 0; i < points.length; i++) {
//                    if (points[i] == null) {
//                        points[i] = p;
//                        addMarker(p, "Point " + (i + 1));
//                        if (i == 3) {  // After selecting 4 points
//                            drawPolygon(); // Draw polygon after selecting 4 points
//                            btnSaveRoute.setVisibility(View.VISIBLE); // Show Save button
//                            btnClearPoints.setVisibility(View.VISIBLE); // Show Clear button
//                        }
//                        Log.d(TAG, "Point " + (i + 1) + " Selected: " + p.getLatitude() + ", " + p.getLongitude());
//                        break;
//                    }
//                }
//                return true;
//            }
//
//            @Override
//            public boolean longPressHelper(GeoPoint p) {
//                return false;
//            }
//        });
//
//        mapView.getOverlays().add(mapEventsOverlay);
//    }
//
//    private void addMarker(GeoPoint point, String title) {
//        Marker marker = new Marker(mapView);
//        marker.setPosition(point);
//        marker.setTitle(title);
//        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
//        mapView.getOverlays().add(marker);
//        mapView.invalidate();
//    }
//
//    private void drawPolygon() {
//        if (points[0] != null && points[1] != null && points[2] != null && points[3] != null) {
//            polygon = new Polygon();
//            polygon.setPoints(List.of(points[0], points[1], points[2], points[3], points[0])); // Create a polygon
//            polygon.setFillColor(0x220000FF);  // Transparent blue
//            polygon.setStrokeColor(0xFF0000FF); // Blue outline
//            polygon.setStrokeWidth(2.5f);
//            mapView.getOverlays().add(polygon);
//
//            // Add to savedPolygons list to keep track of polygons
//            savedPolygons.add(polygon);
//
//            mapView.invalidate();
//        }
//    }
//
//    private void saveAreaToCsv() {
//        if (points[0] == null || points[1] == null || points[2] == null || points[3] == null) {
//            Toast.makeText(this, "Select all 4 points", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        String roadName = getRoadName(points[0]);  // Using the first point's road name
//        File file = new File(getExternalFilesDir(null), FILE_NAME);
//        boolean isNewFile = !file.exists();
//
//        try (FileWriter writer = new FileWriter(file, true)) {
//            if (isNewFile) {
//                writer.append("Road Name,Point 1 Latitude,Point 1 Longitude,Point 2 Latitude,Point 2 Longitude,Point 3 Latitude,Point 3 Longitude,Point 4 Latitude,Point 4 Longitude\n");
//            }
//            String data = roadName + "," + points[0].getLatitude() + "," + points[0].getLongitude() + "," +
//                    points[1].getLatitude() + "," + points[1].getLongitude() + "," +
//                    points[2].getLatitude() + "," + points[2].getLongitude() + "," +
//                    points[3].getLatitude() + "," + points[3].getLongitude() + "\n";
//            writer.append(data);
//            writer.flush();
//
//            Log.d(TAG, "CSV Saved: " + data);
//            Toast.makeText(this, "Area saved!", Toast.LENGTH_SHORT).show();
//
//            // Clear points after saving
//            clearPoints();
//        } catch (IOException e) {
//            Log.e(TAG, "Error writing CSV", e);
//        }
//    }
//
//    private String getRoadName(GeoPoint geoPoint) {
//        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
//        try {
//            List<Address> addresses = geocoder.getFromLocation(geoPoint.getLatitude(), geoPoint.getLongitude(), 1);
//            if (addresses != null && !addresses.isEmpty()) {
//                return addresses.get(0).getThoroughfare();
//            }
//        } catch (IOException e) {
//            Log.e(TAG, "Geocoder failed", e);
//        }
//        return "Unknown Road";
//    }
//
//
//    private void clearPoints() {
////        // Remove all the selected points
//        for (int i = 0; i < points.length; i++) {
//            points[i] = null;
//        }
//    }
//
//}





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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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



//        Configuration.getInstance().setUserAgentValue(getApplicationContext().getPackageName());
//        mapView = findViewById(R.id.mapView);
//        searchView = findViewById(R.id.search_view);
//
//        mapView.setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK);
//        mapView.setMultiTouchControls(true);

//        IMapController mapController = mapView.getController();
//        mapController.setZoom(15);
//        mapController.setCenter(new org.osmdroid.util.GeoPoint(23.8103, 90.4125)); // Default center Dhaka

//        setupSearchView();

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



//    private void fetchCoordinates(String query) {
//        // Nominatim API with Dhaka bounding box
//        String apiUrl = String.format(Locale.getDefault(),
//                "https://nominatim.openstreetmap.org/search?q=%s&format=json&addressdetails=1" +
//                        "&viewbox=90.2792,23.7104,90.5120,23.9135&bounded=1", query);
//
//        OkHttpClient client = new OkHttpClient();
//        Request request = new Request.Builder().url(apiUrl).build();
//
//        new Thread(() -> {
//            try {
//                Response response = client.newCall(request).execute();
//                if (response.isSuccessful() && response.body() != null) {
//                    String jsonResponse = response.body().string();
//                    Log.d(TAG, "Search API Response: " + jsonResponse);
//                    runOnUiThread(() -> parseCoordinates(jsonResponse));
//                }
//            } catch (Exception e) {
//                Log.e(TAG, "Error fetching coordinates", e);
//            }
//        }).start();
//    }

//    private void parseCoordinates(String jsonResponse) {
//        try {
//            JSONArray jsonArray = new JSONArray(jsonResponse);
//            if (jsonArray.length() > 0) {
//                JSONObject locationObject = jsonArray.getJSONObject(0);
//                double lat = locationObject.getDouble("lat");
//                double lon = locationObject.getDouble("lon");
//
//                GeoPoint geoPoint = new GeoPoint(lat, lon);
//                mapView.getController().setCenter(geoPoint);
//
//                Marker marker = new Marker(mapView);
//                marker.setPosition(geoPoint);
//                marker.setTitle("Searched Location");
//                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
//                mapView.getOverlays().add(marker);
//                mapView.invalidate();
//
//                Log.d(TAG, "Search Location Plotted: " + lat + ", " + lon);
//            } else {
//                Toast.makeText(this, "No results found", Toast.LENGTH_SHORT).show();
//            }
//        } catch (Exception e) {
//            Log.e(TAG, "Error parsing coordinates", e);
//        }
//    }

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

//    private void drawPolygon() {
//        if (points[0] != null && points[1] != null && points[2] != null && points[3] != null && !isPolygonDrawn) {
//            polygon = new Polygon();
//            polygon.setPoints(List.of(points[0], points[1], points[2], points[3], points[0])); // Create a polygon
//            polygon.setFillColor(0x220000FF);  // Transparent blue
//            polygon.setStrokeColor(0xFF0000FF); // Blue outline
//            polygon.setStrokeWidth(2.5f);
//            mapView.getOverlays().add(polygon);
//
//            // Add to savedPolygons list to keep track of polygons
//            savedPolygons.add(polygon);
//
//            // Mark polygon as drawn
//            isPolygonDrawn = true;
//
//            mapView.invalidate();
//        }
//    }


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

//    private void setupSearchView() {
//        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
//            @Override
//            public boolean onQueryTextSubmit(String query) {
//                Log.d(TAG, "Search Query Submitted: " + query);
//                fetchCoordinates(query); // Fetch the coordinates for the location
//                searchView.clearFocus(); // Remove focus from the search view after submitting the query
//                return true;
//            }
//
//            @Override
//            public boolean onQueryTextChange(String newText) {
//                return false; // No action needed while typing
//            }
//        });
//    }


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
//    private void fetchCoordinates(String query) {
//        // Nominatim API with Dhaka bounding box
//        String apiUrl = String.format(Locale.getDefault(),
//                "https://nominatim.openstreetmap.org/search?q=%s&format=json&addressdetails=1" +
//                        "&viewbox=90.2792,23.7104,90.5120,23.9135&bounded=1", query);
//
//        OkHttpClient client = new OkHttpClient();
//        Request request = new Request.Builder().url(apiUrl).build();
//
//        new Thread(() -> {
//            try {
//                Response response = client.newCall(request).execute();
//                if (response.isSuccessful() && response.body() != null) {
//                    String jsonResponse = response.body().string();
//                    Log.d(TAG, "Search API Response: " + jsonResponse);
//                    runOnUiThread(() -> parseCoordinates(jsonResponse)); // Process the coordinates on the main thread
//                }
//            } catch (Exception e) {
//                Log.e(TAG, "Error fetching coordinates", e);
//            }
//        }).start();
//    }


//    private void clearPoints() {
//        // Remove all the selected points
//        for (int i = 0; i < points.length; i++) {
//            points[i] = null;
//        }
//
//        // Clear the markers (points) but keep the saved polygons
//        mapView.getOverlays().clear();
//
//        // Re-add saved polygons to the map
//        for (Polygon savedPolygon : savedPolygons) {
//            mapView.getOverlays().add(savedPolygon);
//        }
//
//        setupMapTouchListener(); // Reset the map listener
//        Toast.makeText(this, "Points Cleared", Toast.LENGTH_SHORT).show();
//        Log.d(TAG, "All Points Cleared");
//    }
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
