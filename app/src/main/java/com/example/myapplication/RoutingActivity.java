

package com.example.myapplication;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.myapplication.R;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import android.location.Location;

import org.json.JSONException;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.Polygon;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import org.json.JSONArray;
import org.json.JSONObject;

public class RoutingActivity extends AppCompatActivity {

    private EditText fromLocation;
    private EditText toLocation;
    private Button getRouteButton;
    private View routeInfoContainer; // Class-level field
    private TextView bestRouteTime; // Class-level field
    private TextView alternativeRoutes; // Class-level field
    private MapView mapView;
    int skippedCount = 0;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private GeoPoint currentLocation;
    private Map<Integer, String[]> visibilityTimes = new HashMap<>();

    private String[] selectedMode = {"driving"}; // Default to driving

    private List<Polygon> savedPolygons = new ArrayList<>(); // List to store saved polygons

    private static final String TAG = "RoutingActivity"; // For logging
    private static final String SUPABASE_URL = "https://kquvuygavkhsxvdpqyfn.supabase.co"; // Replace with your Supabase URL
    private static final String SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImtxdXZ1eWdhdmtoc3h2ZHBxeWZuIiwicm9sZSI6ImFub24iLCJpYXQiOjE3MzcxMDQ4NjcsImV4cCI6MjA1MjY4MDg2N30.YVPKExfM-ZxzO9JvM9RQZQrBiyG1iT50fiwGUcvw8EI";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_routing);
        // Initialize FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
// Check location permissions and get current location
        checkLocationPermissions();


        // Initialize Views
        fromLocation = findViewById(R.id.from_location);
        toLocation = findViewById(R.id.to_location);
        getRouteButton = findViewById(R.id.get_route_button);
        mapView = findViewById(R.id.mapview);
        Spinner travelModeSpinner = findViewById(R.id.travel_mode_spinner);  // Initialize Spinner

        // Configure MapView
        Configuration.getInstance().setUserAgentValue(getApplicationContext().getPackageName());
        mapView.setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        IMapController mapController = mapView.getController();
        mapController.setZoom(10.0);
        mapController.setCenter(new GeoPoint(23.8103, 90.4125)); // Default to Dhaka

        Log.d(TAG, "Map initialized successfully.");

        // Load polygons from CSV in assets folder
        loadPolygonsFromDatabase();

        // Default mode is "driving"
        final String[] selectedMode = {"driving"};

        travelModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                String newMode = parentView.getItemAtPosition(position).toString().toLowerCase();
                Log.d(TAG, "Selected travel mode: " + newMode);

                if (!newMode.equals(selectedMode[0])) {
                    selectedMode[0] = newMode;
                    String from = fromLocation.getText().toString().trim();
                    String to = toLocation.getText().toString().trim();
                    if (!from.isEmpty() && !to.isEmpty()) {
                        if (from.equals("Your Location") && fromLocation.getTag() instanceof GeoPoint) {
                            GeoPoint fromPoint = (GeoPoint) fromLocation.getTag();
                            boolean isToCoordinates = to.matches("-?\\d+\\.\\d+,-?\\d+\\.\\d+");
                            if (isToCoordinates) {
                                try {
                                    String[] toLatLon = to.split(",");
                                    double toLat = Double.parseDouble(toLatLon[0].trim());
                                    double toLon = Double.parseDouble(toLatLon[1].trim());
                                    GeoPoint toPoint = new GeoPoint(toLat, toLon);
                                    fetchRoute(fromPoint, toPoint, selectedMode[0]);
                                } catch (Exception e) {
                                    Toast.makeText(RoutingActivity.this, "Invalid 'to' coordinates", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                fetchCoordinatesAndDisplayRoute(from, to, selectedMode[0]);
                            }
                        } else {
                            fetchCoordinatesAndDisplayRoute(from, to, selectedMode[0]);
                        }
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                selectedMode[0] = "driving";
                Log.d(TAG, "No travel mode selected, defaulting to 'driving'.");
            }
        });

        getRouteButton.setOnClickListener(v -> {
            String from = fromLocation.getText().toString().trim();
            String to = toLocation.getText().toString().trim();

            if (from.isEmpty() || to.isEmpty()) {
                Toast.makeText(this, "Please enter both locations", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Missing location input.");
                return;
            }

            if (from.equals("Your Location") && fromLocation.getTag() instanceof GeoPoint) {
                GeoPoint fromPoint = (GeoPoint) fromLocation.getTag();
                Log.d(TAG, "Using 'Your Location' with coordinates: " + fromPoint);

                boolean isToCoordinates = to.matches("-?\\d+\\.\\d+,-?\\d+\\.\\d+");
                if (isToCoordinates) {
                    try {
                        String[] toLatLon = to.split(",");
                        double toLat = Double.parseDouble(toLatLon[0].trim());
                        double toLon = Double.parseDouble(toLatLon[1].trim());
                        GeoPoint toPoint = new GeoPoint(toLat, toLon);
                        Log.d(TAG, "Parsed 'to' coordinates: " + toPoint);
                        fetchRoute(fromPoint, toPoint, selectedMode[0]);
                    } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                        Log.e(TAG, "Error parsing 'to' coordinates: " + e.getMessage());
                        Toast.makeText(this, "Invalid coordinates format. Use: lat,lon", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    new Thread(() -> {
                        try {
                            GeoPoint toPoint = geocodeLocation(to);
                            if (toPoint != null) {
                                runOnUiThread(() -> {
                                    Log.d(TAG, "Geocoded 'to' to: " + toPoint);
                                    fetchRoute(fromPoint, toPoint, selectedMode[0]);
                                });
                            } else {
                                runOnUiThread(() -> Toast.makeText(this, "Failed to fetch 'to' coordinates.", Toast.LENGTH_SHORT).show());
                            }
                        } catch (Exception e) {
                            runOnUiThread(() -> Toast.makeText(this, "Error geocoding 'to': " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        }
                    }).start();
                }
            } else {
                boolean isFromCoordinates = from.matches("-?\\d+\\.\\d+,-?\\d+\\.\\d+");
                boolean isToCoordinates = to.matches("-?\\d+\\.\\d+,-?\\d+\\.\\d+");

                if (isFromCoordinates && isToCoordinates) {
                    try {
                        String[] fromLatLon = from.split(",");
                        double fromLat = Double.parseDouble(fromLatLon[0].trim());
                        double fromLon = Double.parseDouble(fromLatLon[1].trim());
                        GeoPoint fromPoint = new GeoPoint(fromLat, fromLon);

                        String[] toLatLon = to.split(",");
                        double toLat = Double.parseDouble(toLatLon[0].trim());
                        double toLon = Double.parseDouble(toLatLon[1].trim());
                        GeoPoint toPoint = new GeoPoint(toLat, toLon);

                        Log.d(TAG, "Parsed coordinates: From - " + fromPoint + ", To - " + toPoint);
                        fetchRoute(fromPoint, toPoint, selectedMode[0]);
                    } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                        Log.e(TAG, "Error parsing coordinates: " + e.getMessage());
                        Toast.makeText(this, "Invalid coordinates format. Use: lat,lon", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.d(TAG, "Fetching route from: " + from + " to: " + to);
                    fetchCoordinatesAndDisplayRoute(from, to, selectedMode[0]);
                }
            }
        });

    }
    private void checkLocationPermissions() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getCurrentLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Location permission denied");
                fromLocation.setText("Your Location"); // Set default without GeoPoint
            }
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            currentLocation = new GeoPoint(location.getLatitude(), location.getLongitude());
                            Log.d(TAG, "Current location: " + currentLocation);

                            // Add a marker for the current location
                            Marker currentMarker = new Marker(mapView);
                            currentMarker.setPosition(currentLocation);
                            currentMarker.setTitle("Your Location");
                            currentMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                            mapView.getOverlays().add(currentMarker);

                            // Center the map on the current location
                            IMapController mapController = mapView.getController();
                            mapController.setCenter(currentLocation);

                            // Set "Your Location" as the default in the "From" field
                            fromLocation.setText("Your Location");
                            fromLocation.setTag(currentLocation); // Store GeoPoint for routing

                            mapView.invalidate();
                        } else {
                            Log.d(TAG, "Location is null");
                            Toast.makeText(RoutingActivity.this, "Unable to get current location", Toast.LENGTH_SHORT).show();
                            fromLocation.setText("Your Location"); // Set default without GeoPoint
                        }
                    }
                });
    }

    private List<Integer> polygonIds = new ArrayList<>();
    private List<Float> polygonWaterLevels = new ArrayList<>();

    private void loadPolygonsFromDatabase() {
        OkHttpClient client = new OkHttpClient();

        String url = SUPABASE_URL + "/rest/v1/polygon_data";

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                .addHeader("Accept", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Error fetching polygon data: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String jsonResponse = response.body().string();
                    runOnUiThread(() -> {
                        try {
                            parsePolygonData(jsonResponse);
                            fetchVisibilityData();  // 🆕 After loading polygons, check which are visible
                        } catch (JSONException e) {
                            Log.e(TAG, "Error parsing polygon data: " + e.getMessage());
                        }
                    });
                }
            }
        });
    }


    private List<String> polygonStartTimes = new ArrayList<>();
    private List<String> polygonEndTimes = new ArrayList<>();
    private List<Marker> polygonMarkers = new ArrayList<>();


    private void parsePolygonData(String jsonResponse) throws JSONException {
        savedPolygons.clear();
        polygonIds.clear();
        polygonWaterLevels.clear();

        JSONArray polygonsArray = new JSONArray(jsonResponse);

        for (int i = 0; i < polygonsArray.length(); i++) {
            JSONObject polygonObject = polygonsArray.getJSONObject(i);

            int polygonId = polygonObject.getInt("id");
            double lat1 = polygonObject.getDouble("point_1_latitude");
            double lon1 = polygonObject.getDouble("point_1_longitude");
            double lat2 = polygonObject.getDouble("point_2_latitude");
            double lon2 = polygonObject.getDouble("point_2_longitude");
            double lat3 = polygonObject.getDouble("point_3_latitude");
            double lon3 = polygonObject.getDouble("point_3_longitude");
            double lat4 = polygonObject.getDouble("point_4_latitude");
            double lon4 = polygonObject.getDouble("point_4_longitude");
            float waterLevel = (float) polygonObject.getDouble("water_level");

            GeoPoint p1 = new GeoPoint(lat1, lon1);
            GeoPoint p2 = new GeoPoint(lat2, lon2);
            GeoPoint p3 = new GeoPoint(lat3, lon3);
            GeoPoint p4 = new GeoPoint(lat4, lon4);

            List<GeoPoint> points = new ArrayList<>();
            points.add(p1);
            points.add(p2);
            points.add(p3);
            points.add(p4);

            Polygon polygon = new Polygon();
            polygon.setPoints(points);
            polygon.setFillColor(Color.TRANSPARENT);
            polygon.setStrokeColor(Color.BLACK);
            polygon.setStrokeWidth(2.5f);

            savedPolygons.add(polygon);
            polygonIds.add(polygonId);
            polygonWaterLevels.add(waterLevel);

            mapView.getOverlays().add(polygon);

            // 🆕 🆕 🆕  ADD MARKER HERE INSIDE LOOP!
            Marker marker = new Marker(mapView);
            marker.setPosition(p1);
            marker.setTitle("Polygon ID: " + polygonId);

// Temporarily simple click
            marker.setOnMarkerClickListener((clickedMarker, clickedMapView) -> {
                Toast.makeText(this, "Polygon ID: " + polygonId, Toast.LENGTH_SHORT).show();
                return true;
            });

            mapView.getOverlays().add(marker);
            polygonMarkers.add(marker); // 🆕 Save marker to list

        }

        mapView.invalidate();
    }

    private String getLiveMarkerInfo(int polygonId, float waterLevel) {
        StringBuilder infoBuilder = new StringBuilder();
        String nowTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());

        infoBuilder.append("⏰ Current Time: ").append(nowTime).append("\n");

        // Check if polygon is currently active
        for (int i = 0; i < polygonIds.size(); i++) {
            if (polygonIds.get(i) == polygonId) {
                // Assume you already have visibility data loaded
                // You can store visibilityStartTimes and visibilityEndTimes in two separate lists if needed
                // For now just example:
                // Example only! You need real start/end times based on your visibility dataset
                String startTime = "10:00"; // Placeholder
                String endTime = "15:00";   // Placeholder

                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                    Date now = sdf.parse(nowTime.substring(0, 5));
                    Date start = sdf.parse(startTime);
                    Date end = sdf.parse(endTime);

                    if (now != null && start != null && end != null) {
                        if (now.compareTo(start) >= 0 && now.compareTo(end) <= 0) {
                            infoBuilder.append("✅ This area is water blocked\n");
                            infoBuilder.append("Water Level: ").append(getWaterLevelDescription(waterLevel)).append("\n");
                        } else {
                            infoBuilder.append("⚠️ Warning: Water Blocked from ")
                                    .append(startTime)
                                    .append(" to ")
                                    .append(endTime)
                                    .append("\nWater Level: ")
                                    .append(getWaterLevelDescription(waterLevel))
                                    .append("\n");
                        }
                    }
                } catch (ParseException e) {
                    infoBuilder.append("Error parsing time");
                }
                break;
            }
        }

        return infoBuilder.toString();
    }


    private String getWaterLevelDescription(float waterLevel) {
        switch ((int) waterLevel) {
            case 1:
                return "Not much water ";
            case 2:
                return "Significant water accumulation";
            case 3:
                return "High water level - caution advised";
            case 4:
                return "Very high water level -try to avoid this road";
            default:
                return "Unknown water level";
        }

    }


    private void showLiveInfoDialog(String startTime, String endTime, float waterLevel, int polygonId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Polygon " + polygonId + " Info");

        final TextView clockView = new TextView(this);
        final TextView statusView = new TextView(this);

        clockView.setPadding(20, 20, 20, 20);
        statusView.setPadding(20, 20, 20, 20);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(clockView);
        layout.addView(statusView);

        builder.setView(layout);
        builder.setPositiveButton("Close", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                String currentTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
                clockView.setText("⏰ Current Time: " + currentTime);

                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                    Date now = sdf.parse(currentTime.substring(0, 5));
                    Date start = sdf.parse(startTime);
                    Date end = sdf.parse(endTime);

                    if (now != null && start != null && end != null) {
                        if (now.compareTo(start) >= 0 && now.compareTo(end) <= 0) {
                            statusView.setText("🛑 Water: Water Blockage\n" +
                                    "🌊 Water Level: " + getWaterLevelDescription(waterLevel));
                        } else {
                            statusView.setText("⚠️ Warning: Water blockage from " + startTime + " to " + endTime +
                                    "\n🌊 Water Level: " + getWaterLevelDescription(waterLevel));
                        }
                    }
                } catch (ParseException e) {
                    statusView.setText("Error parsing time!");
                }

                handler.postDelayed(this, 1000); // Update every second
            }
        };
        handler.post(runnable);

        dialog.setOnDismissListener(d -> handler.removeCallbacks(runnable));
    }


    private void fetchVisibilityData() {
        OkHttpClient client = new OkHttpClient();

        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        String url = SUPABASE_URL + "/rest/v1/polygon_visibility"
                + "?select=polygon_id,visibility_start_time,visibility_end_time"
                + "&is_visible=eq.true"; // Only fetch visible polygons

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                .addHeader("Accept", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Error fetching visibility data: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    runOnUiThread(() -> {
                        try {
                            applyVisibility(new JSONArray(responseBody));
                        } catch (JSONException e) {
                            Log.e(TAG, "Error parsing visibility data: " + e.getMessage());
                        }
                    });
                }
            }
        });
    }


    private void applyVisibility(JSONArray visibilityArray) throws JSONException {
        boolean anyPolygonVisible = false;

        String nowTimeStr = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());

        Date nowTime;
        try {
            nowTime = sdf.parse(nowTimeStr);
        } catch (ParseException e) {
            Log.e("VisibilityUpdate", "Error parsing current time: " + e.getMessage());
            return;
        }

        // Map to quickly lookup start/end times by polygon ID
        visibilityTimes = new HashMap<>();

        for (int i = 0; i < visibilityArray.length(); i++) {
            JSONObject visibilityObject = visibilityArray.getJSONObject(i);
            int visiblePolygonId = visibilityObject.getInt("polygon_id");
            String startTimeStr = visibilityObject.getString("visibility_start_time").substring(0, 5);
            String endTimeStr = visibilityObject.getString("visibility_end_time").substring(0, 5);

            visibilityTimes.put(visiblePolygonId, new String[]{startTimeStr, endTimeStr});
        }

        for (int j = 0; j < polygonIds.size(); j++) {
            int polygonId = polygonIds.get(j);
            Polygon polygon = savedPolygons.get(j);
            float waterLevel = polygonWaterLevels.get(j);
            Marker marker = polygonMarkers.get(j);

            if (visibilityTimes.containsKey(polygonId)) {
                String[] times = visibilityTimes.get(polygonId);
                String startTimeStr = times[0];
                String endTimeStr = times[1];

                Date startTime, endTime;
                try {
                    startTime = sdf.parse(startTimeStr);
                    endTime = sdf.parse(endTimeStr);
                } catch (ParseException e) {
                    Log.e("VisibilityUpdate", "Error parsing visibility times: " + e.getMessage());
                    continue;
                }

                if (nowTime.compareTo(startTime) >= 0 && nowTime.compareTo(endTime) <= 0) {
                    int fillColor = getFillColorForWaterLevel(waterLevel);
                    polygon.setFillColor(fillColor);
                    anyPolygonVisible = true;

                    Log.d("VisibilityUpdate", "✅ Polygon ID: " + polygonId + " visible between " + startTimeStr + " - " + endTimeStr);
                }

                marker.setOnMarkerClickListener((clickedMarker, clickedMapView) -> {
                    showLiveInfoDialog(startTimeStr, endTimeStr, waterLevel, polygonId);
                    return true;
                });

            } else {
                // 🆕 No blockage today
                marker.setOnMarkerClickListener((clickedMarker, clickedMapView) -> {
                    showNoBlockageTodayDialog(polygonId);
                    return true;
                });
            }
        }

        if (anyPolygonVisible) {
            Log.d("VisibilityUpdate", "🎯 Some polygons were filled based on current time period.");
        } else {
            Log.d("VisibilityUpdate", "⚡ No polygons matched visibility at this time.");
        }

        mapView.invalidate();
    }

    private void showNoBlockageTodayDialog(int polygonId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Polygon " + polygonId + " Info");

        final TextView clockView = new TextView(this);
        final TextView statusView = new TextView(this);

        clockView.setPadding(20, 20, 20, 20);
        statusView.setPadding(20, 20, 20, 20);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(clockView);
        layout.addView(statusView);

        builder.setView(layout);
        builder.setPositiveButton("Close", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                String currentTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
                clockView.setText("⏰ Current Time: " + currentTime);

                statusView.setText("😊 No water blockage today!");

                handler.postDelayed(this, 1000);
            }
        };
        handler.post(runnable);

        dialog.setOnDismissListener(d -> handler.removeCallbacks(runnable));
    }

    private boolean isPolygonBlockedNow(int polygonId) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String nowStr = sdf.format(new Date());
        try {
            Date now = sdf.parse(nowStr);
            if (visibilityTimes.containsKey(polygonId)) {
                String[] times = visibilityTimes.get(polygonId);
                Date start = sdf.parse(times[0]);
                Date end = sdf.parse(times[1]);
                if (now.compareTo(start) >= 0 && now.compareTo(end) <= 0) {
                    return true;  // currently blocked
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        Log.d("DEBUG", "Checking polygon " + polygonId + ", visibilityTimes size: " + visibilityTimes.size());
        return false;
    }


    private void fetchCoordinatesAndDisplayRoute(String from, String to, String mode) {
        new Thread(() -> {
            try {
                GeoPoint fromPoint = geocodeLocation(from);
                GeoPoint toPoint = geocodeLocation(to);

                if (fromPoint != null && toPoint != null) {
                    Log.d(TAG, "Coordinates found: From - " + fromPoint + ", To - " + toPoint);
                    fetchRoute(fromPoint, toPoint, mode); // Pass selected mode
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Failed to fetch coordinates.", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Failed to fetch coordinates.");
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error fetching coordinates: " + e.getMessage());
                });
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
                    GeoPoint geoPoint = new GeoPoint(lat, lon);
                    Log.d(TAG, "Geocoding result: " + geoPoint);
                    return geoPoint;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during geocoding: " + e.getMessage());
            e.printStackTrace();
        }
        return null; // Return null if geocoding fails
    }

    private void fetchRoute(GeoPoint fromPoint, GeoPoint toPoint, String mode) {
        String vehicle;
        switch (mode.toLowerCase()) {
            case "driving":
                vehicle = "car";
                break;
            case "walking":
                vehicle = "foot";
                break;
            case "cycling":
                vehicle = "bike";
                break;
            default:
                vehicle = "car";
                Log.w(TAG, "Unknown mode: " + mode + ", defaulting to car");
        }

        Log.d(TAG, "Fetching route from: " + fromPoint.getLatitude() + "," + fromPoint.getLongitude() +
                " to: " + toPoint.getLatitude() + "," + toPoint.getLongitude() + " with mode: " + mode);

        String graphHopperApiKey = "d20cbf9f-8c4d-4f87-94b9-09203bcba7cb";

        // ✅ Working GraphHopper URL for alternative routes
        String graphHopperUrl = "https://graphhopper.com/api/1/route?" +
                "point=" + fromPoint.getLatitude() + "," + fromPoint.getLongitude() +
                "&point=" + toPoint.getLatitude() + "," + toPoint.getLongitude() +
                "&vehicle=" + vehicle +
                "&algorithm=alternative_route" +
                "&max_paths=5" +
                "&alternative_route.max_weight_factor=2.0" +
                "&alternative_route.max_share_factor=0.4" +
                "&locale=en" +
                "&instructions=true" +
                "&calc_points=true" +
                "&points_encoded=true" +
                "&key=" + graphHopperApiKey;

        Log.d(TAG, "GraphHopper URL: " + graphHopperUrl);

        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(60, TimeUnit.SECONDS)
                        .readTimeout(60, TimeUnit.SECONDS)
                        .writeTimeout(60, TimeUnit.SECONDS)
                        .build();

                Request request = new Request.Builder()
                        .url(graphHopperUrl)
                        .build();

                Log.d(TAG, "Sending GraphHopper route request for mode: " + vehicle);
                long startTime = System.currentTimeMillis();
                Response response = client.newCall(request).execute();
                long endTime = System.currentTimeMillis();
                Log.d(TAG, "GraphHopper request completed in " + (endTime - startTime) + "ms");

                if (response.isSuccessful() && response.body() != null) {
                    String jsonResponse = response.body().string();
                    Log.d(TAG, "GraphHopper full response: " + jsonResponse);

                    JSONObject jsonObject = new JSONObject(jsonResponse);

                    // ✅ Check if paths exist and log count
                    if (!jsonObject.has("paths")) {
                        Log.e(TAG, "No 'paths' key in response");
                        runOnUiThread(() -> Toast.makeText(this, "Invalid response format", Toast.LENGTH_SHORT).show());
                        return;
                    }

                    JSONArray paths = jsonObject.getJSONArray("paths");
                    Log.d(TAG, "Number of paths received: " + paths.length());

                    if (paths.length() > 0) {
                        // ✅ Create filtered JSON response with only valid routes
                        List<String> validRouteResponses = new ArrayList<>();
                        List<RouteInfo> routeInfoList = new ArrayList<>();
                        int[] colors = new int[]{
                                Color.argb(150, 255, 0, 0),
                                Color.argb(150, 0, 255, 0),
                                Color.argb(150, 0, 0, 255),
                                Color.argb(150, 255, 255, 0),
                                Color.argb(150, 255, 0, 255)
                        };

                        int skippedCount = 0;
                        int validRouteCount = 0;

                        // ✅ Build new JSON with valid routes only
                        JSONObject filteredResponse = new JSONObject();
                        JSONArray validPaths = new JSONArray();

                        for (int i = 0; i < paths.length() && i < 5; i++) {
                            JSONObject path = paths.getJSONObject(i);
                            String encodedPolyline = path.getString("points");
                            List<GeoPoint> geoPoints = decodePolyline(encodedPolyline);
                            boolean isBlocked = false;

                            // ✅ Check for water blockage
                            if (geoPoints != null && !geoPoints.isEmpty()) {
                                for (GeoPoint point : geoPoints) {
                                    for (int j = 0; j < savedPolygons.size(); j++) {
                                        Polygon polygon = savedPolygons.get(j);
                                        List<GeoPoint> polygonPoints = polygon.getActualPoints();

                                        int intersectCount = 0;
                                        for (int k = 0; k < polygonPoints.size() - 1; k++) {
                                            double lat1 = polygonPoints.get(k).getLatitude();
                                            double lon1 = polygonPoints.get(k).getLongitude();
                                            double lat2 = polygonPoints.get(k + 1).getLatitude();
                                            double lon2 = polygonPoints.get(k + 1).getLongitude();
                                            double lat = point.getLatitude();
                                            double lon = point.getLongitude();

                                            if (((lat1 > lat) != (lat2 > lat)) &&
                                                    (lon < (lon2 - lon1) * (lat - lat1) / (lat2 - lat1) + lon1)) {
                                                intersectCount++;
                                            }
                                        }

                                        boolean pointInside = (intersectCount % 2 == 1);
                                        if (pointInside) {
                                            int polygonId = polygonIds.get(j);
                                            if (isPolygonBlockedNow(polygonId)) {
                                                isBlocked = true;
                                                break;
                                            }
                                        }
                                    }
                                    if (isBlocked) break;
                                }
                            }

                            if (isBlocked) {
                                Log.d(TAG, "🚫 Skipping route " + (i + 1) + " due to polygon water blockage");
                                skippedCount++;
                                continue;
                            }

                            // ✅ Route is valid, add to filtered response
                            validPaths.put(path);
                            double distance = path.getDouble("distance") / 1000;
                            double duration = path.getDouble("time") / 1000 / 60;
                            String colorName = getColorName(colors[validRouteCount]);
                            routeInfoList.add(new RouteInfo(colorName, distance, duration));

                            Log.d(TAG, "✅ Valid Route " + (validRouteCount + 1) + ": Mode=" + vehicle +
                                    ", Distance=" + distance + "km, Duration=" + duration + "min");
                            validRouteCount++;
                        }

                        // ✅ Create filtered JSON response
                        try {
                            filteredResponse.put("paths", validPaths);
                            if (jsonObject.has("info")) {
                                filteredResponse.put("info", jsonObject.getJSONObject("info"));
                            }
                            validRouteResponses.add(filteredResponse.toString());
                        } catch (Exception e) {
                            Log.e(TAG, "Error creating filtered response: " + e.getMessage());
                            validRouteResponses.add(jsonResponse); // Fallback to original
                        }

                        final int finalSkippedCount = skippedCount;
                        final int finalValidRouteCount = validRouteCount;

                        runOnUiThread(() -> {
                            if (finalValidRouteCount > 0) {
                                displayRoute(validRouteResponses, fromPoint, toPoint, routeInfoList);
                                Log.d(TAG, "Displaying " + finalValidRouteCount + " valid route(s)");

                                if (finalSkippedCount > 0) {
                                    Toast.makeText(this, "✅ " + finalValidRouteCount + " route(s) found, 🚫 " +
                                            finalSkippedCount + " skipped due to water blockage", Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(this, "✅ " + finalValidRouteCount + " route(s) found", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(this, "🚫 All routes blocked by water. No alternative routes available.", Toast.LENGTH_LONG).show();
                            }
                        });
                    } else {
                        runOnUiThread(() -> Toast.makeText(this, "No routes found.", Toast.LENGTH_SHORT).show());
                        Log.e(TAG, "No paths found in GraphHopper response.");
                    }
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "No response body";
                    Log.e(TAG, "Failed to fetch route: HTTP " + response.code() + " - " + errorBody);
                    runOnUiThread(() -> Toast.makeText(this, "Failed to fetch route: " + response.message(), Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching route: " + e.getMessage(), e);
                runOnUiThread(() -> Toast.makeText(this, "Error fetching route: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            } finally {
                runOnUiThread(() -> {
                    for (Polygon polygon : savedPolygons) {
                        mapView.getOverlays().add(polygon);
                    }
                    mapView.invalidate();
                });
            }
        }).start();
    }



    private static class RouteInfo {
        String colorName;
        double distance;
        double duration;

        RouteInfo(String colorName, double distance, double duration) {
            this.colorName = colorName;
            this.distance = distance;
            this.duration = duration;
        }
    }
    private String getColorName(int color) {
        if (color == Color.argb(150, 255, 0, 0)) return "Red";
        if (color == Color.argb(150, 0, 255, 0)) return "Green";
        if (color == Color.argb(150, 0, 0, 255)) return "Blue";
        if (color == Color.argb(150, 255, 255, 0)) return "Yellow";
        if (color == Color.argb(150, 255, 0, 255)) return "Magenta";
        return "Unknown";
    }
    private void displayRoute(List<String> routeResponses, GeoPoint fromPoint, GeoPoint toPoint, List<RouteInfo> routeInfoList) {
        try {
            // Remove previous route overlays (only polylines)
            mapView.getOverlays().removeIf(overlay -> overlay instanceof Polyline);

            int[] colors = new int[]{
                    Color.argb(150, 255, 0, 0),
                    Color.argb(150, 0, 255, 0),
                    Color.argb(150, 0, 0, 255),
                    Color.argb(150, 255, 255, 0),
                    Color.argb(150, 255, 0, 255)
            };

            int routesDisplayed = 0;

            // Loop through each route response
            for (String jsonResponse : routeResponses) {
                JSONObject jsonObject = new JSONObject(jsonResponse);
                JSONArray paths = jsonObject.getJSONArray("paths");

                for (int i = 0; i < paths.length() && routesDisplayed < 5; i++) {
                    JSONObject path = paths.getJSONObject(i);
                    String encodedPolyline = path.getString("points");

                    List<GeoPoint> geoPoints = decodePolyline(encodedPolyline);

                    Polyline polyline = new Polyline();
                    polyline.setPoints(geoPoints);
                    polyline.setColor(colors[routesDisplayed % colors.length]);
                    polyline.setWidth(8.0f);
                    mapView.getOverlays().add(polyline);

                    double distance = path.getDouble("distance") / 1000;
                    double duration = path.getDouble("time") / 1000 / 60;
                    Log.d(TAG, "Displayed Route " + (routesDisplayed + 1) + ": Distance=" + distance + "km, Duration=" + duration + "min");

                    routesDisplayed++;
                }
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

            // Route info update
            routeInfoContainer = findViewById(R.id.route_info_container);
            bestRouteTime = findViewById(R.id.best_route_time);
            alternativeRoutes = findViewById(R.id.alternative_routes);

            if (!routeInfoList.isEmpty()) {
                routeInfoList.sort((r1, r2) -> Double.compare(r1.duration, r2.duration));

                RouteInfo bestRoute = routeInfoList.get(0);
                String modeDisplay = selectedMode[0].substring(0, 1).toUpperCase() + selectedMode[0].substring(1).toLowerCase();
                String durationText = formatDuration(bestRoute.duration);
                bestRouteTime.setText(String.format("%s Best Route (%s): %.2f km, %s",
                        modeDisplay, bestRoute.colorName, bestRoute.distance, durationText));

                StringBuilder alternativesText = new StringBuilder("Alternative Routes:\n");
                for (int i = 1; i < routeInfoList.size(); i++) {
                    RouteInfo route = routeInfoList.get(i);
                    String durationTextAlt = formatDuration(route.duration);
                    alternativesText.append(String.format("%s Route: %.2f km, %s\n",
                            route.colorName, route.distance, durationTextAlt));
                }
                alternativeRoutes.setText(alternativesText.toString());

                routeInfoContainer.setVisibility(View.VISIBLE);
            } else {
                routeInfoContainer.setVisibility(View.GONE);
            }

            mapView.invalidate();
            Log.d(TAG, "Displayed " + routesDisplayed + " routes.");
            Toast.makeText(this, routesDisplayed + " routes displayed.", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Error displaying routes: " + e.getMessage());
            Toast.makeText(this, "Error displaying routes: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String formatDuration(double minutes) {
        int totalMinutes = (int) Math.round(minutes); // Round to nearest minute
        int hours = totalMinutes / 60;
        int remainingMinutes = totalMinutes % 60;

        if (hours > 0) {
            return hours + " hr " + remainingMinutes + " mins";
        } else {
            return totalMinutes + " mins";
        }
    }
    // Method to decode GraphHopper's encoded polyline
    private List<GeoPoint> decodePolyline(String encoded) {
        List<GeoPoint> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            GeoPoint p = new GeoPoint((double) lat / 1E5, (double) lng / 1E5);
            poly.add(p);
        }
        return poly;
    }

    // Load saved polygons from CSV in assets folder



    private int getFillColorForWaterLevel(float waterLevel) {
        switch ((int) waterLevel) {
            case 1:
                return Color.argb(150, 255, 255, 0); // Transparent Yellow
            case 2:
                return Color.argb(150, 0, 0, 255);   // Transparent Blue
            case 3:
                return Color.argb(150, 255, 0, 0);   // Transparent Red
            case 4:
                return Color.argb(150, 139, 69, 19); // Transparent Brown
            // You can add more cases if needed, for other water levels
        }

        // If the water level is not 1, 2, 3, or 4, the polygon won't be drawn
        // You can add logging or error handling if needed.
        return -1;  // Invalid color
    }













//    private void showPolygonDetailsDialog(int index, String rainThreshold, String waterloggedDuration, String waterLevel) {
//        AlertDialog.Builder builder = new AlertDialog.Builder(this);
//        builder.setTitle("Polygon Details");
//
//        // Set the dialog message with the polygon's details
//        builder.setMessage("Index: " + index + "\n" +
//                "Rain Threshold (mm): " + rainThreshold + "\n" +
//                "Waterlogged Duration (mins): " + waterloggedDuration + "\n" +
//                "Water Level: " + waterLevel);
//
//        // Add a button to close the dialog
//        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
//
//        // Show the dialog
//        builder.show();
//
//    }






    private void loadPolygonsFromAssets() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(getAssets().open("route_data.csv")))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("Road Name")) { // Skip the header line
                    String[] values = line.split(",");

                    // Ensure there are at least 9 elements in the row (Road Name + 8 coordinates)
                    if (values.length >= 9) {
                        try {
                            // Parse the latitudes and longitudes (we are ignoring the road name)
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

                            // Create a polygon from the points
                            List<GeoPoint> points = new ArrayList<>();
                            points.add(p1);
                            points.add(p2);
                            points.add(p3);
                            points.add(p4);

                            Polygon polygon = new Polygon();
                            polygon.setPoints(points);
                            polygon.setFillColor(0x220000FF);  // Transparent blue
                            polygon.setStrokeColor(0xFF0000FF); // Blue outline
                            polygon.setStrokeWidth(2.5f);

                            // Add the polygon to the saved polygons list
                            savedPolygons.add(polygon);

                            // Add the polygon to the map
                            mapView.getOverlays().add(polygon);

                            Log.d(TAG, "Loaded polygon from CSV: " + points);

                        } catch (NumberFormatException e) {
                            Log.e(TAG, "Error parsing coordinates from CSV line: " + line, e);
                        }
                    }
                }
            }

            // Refresh the map to show the polygons
            mapView.invalidate();
            Log.d(TAG, "All polygons loaded from assets.");

        } catch (IOException e) {
            Log.e("RoutingActivity", "Error reading CSV from assets", e);
        }
    }


}