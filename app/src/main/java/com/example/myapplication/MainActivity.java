package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int LOCATION_PERMISSION_REQUEST = 1;
    private DrawerLayout drawerLayout;
    private ImageView profileIcon;
    private TextView trafficSummary;
    private MapView mapView;
    private MyLocationNewOverlay locationOverlay;
    private SearchView searchView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Configuration.getInstance().setUserAgentValue("com.example.myapplication/1.0");

        // Initialize Views
        drawerLayout = findViewById(R.id.drawer_layout);
        ImageView menuIcon = findViewById(R.id.menu_icon);
        profileIcon = findViewById(R.id.profile_icon);
        trafficSummary = findViewById(R.id.traffic_summary);
        searchView = findViewById(R.id.search_view);
        ImageButton directionButton = findViewById(R.id.direction_button);
        directionButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RoutingActivity.class);
            startActivity(intent);
        });
//        EditText searchPlate = searchView.findViewById(androidx.appcompat.R.id.search_src_text);
//        searchPlate.setHintTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
//        searchPlate.setTextColor(ContextCompat.getColor(this, android.R.color.black));

        // Initialize MapView
        mapView = findViewById(R.id.mapview);
        mapView.setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);

        // Set initial zoom and position on the map (Dhaka city coordinates)
        IMapController mapController = mapView.getController();
        mapController.setZoom(11);
        mapController.setCenter(new GeoPoint(23.8103, 90.4125)); // Center at Dhaka City

        // Check location permissions and services before enabling MyLocationOverlay
        if (checkAndRequestLocationPermissions()) {
            if (isLocationEnabled()) {
                setupLocationOverlay(); // Enable location overlay
            } else {
                Toast.makeText(this, "Please enable location services in your device settings.", Toast.LENGTH_LONG).show();
                Log.e(TAG, "Location services are disabled.");
            }
        }

        setupSearchView();
        // Fetch Points of Interest (POIs) in Dhaka
        fetchPOIs();

        // Route between two points in Dhaka
        calculateRoute(new GeoPoint(23.8103, 90.4125), new GeoPoint(23.7949, 90.4043)); // Example: Dhaka route

        // Hamburger Menu Click Listener
        menuIcon.setOnClickListener(v -> drawerLayout.openDrawer(findViewById(R.id.navigation_view)));

        // Profile/Login Click Listener
        profileIcon.setOnClickListener(v -> {
            Toast.makeText(this, "Profile/Login Clicked", Toast.LENGTH_SHORT).show();
        });

        // Bottom Panel Click Listener
        trafficSummary.setOnClickListener(v -> {
            Toast.makeText(this, "Traffic Summary Clicked", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (locationOverlay != null) {
            locationOverlay.enableMyLocation();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (locationOverlay != null) {
            locationOverlay.disableMyLocation();
        }
    }

    // Method to check and request location permissions
    private boolean checkAndRequestLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Location permission granted!", Toast.LENGTH_SHORT).show();
                if (isLocationEnabled()) {
                    setupLocationOverlay(); // Enable location overlay
                } else {
                    Toast.makeText(this, "Please enable location services in your device settings.", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(this, "Location permission denied!", Toast.LENGTH_LONG).show();
            }
        }
    }

    // Method to check if location services are enabled
    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    // Method to set up MyLocationOverlay
    private void setupLocationOverlay() {
        locationOverlay = new MyLocationNewOverlay(mapView);
        mapView.getOverlays().add(locationOverlay);
        locationOverlay.enableMyLocation();
        locationOverlay.enableFollowLocation();

        if (!locationOverlay.isMyLocationEnabled()) {
            Toast.makeText(this, "My location overlay could not be enabled!", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Location overlay not enabled.");
        } else {
            Toast.makeText(this, "My location overlay enabled successfully.", Toast.LENGTH_SHORT).show();
            locationOverlay.runOnFirstFix(() -> {
                GeoPoint currentLocation = locationOverlay.getMyLocation();
                if (currentLocation != null) {
                    Log.d(TAG, "Current location: " + currentLocation.getLatitude() + ", " + currentLocation.getLongitude());
                    runOnUiThread(() -> mapView.getController().setCenter(currentLocation));
                } else {
                    Log.e(TAG, "Unable to fetch current location.");
                }
            });
        }
    }

    private void setupSearchView() {
        searchView.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                fetchCoordinates(query);
                searchView.clearFocus(); // Clear focus after search
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
    }

    private void fetchCoordinates(String query) {
        String apiUrl = String.format(Locale.getDefault(), "https://nominatim.openstreetmap.org/search?q=%s&format=json&addressdetails=1", query);

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(apiUrl).build();

        new Thread(() -> {
            try {
                Response response = client.newCall(request).execute();
                if (response.isSuccessful() && response.body() != null) {
                    String jsonResponse = response.body().string();
                    Log.d(TAG, "Overpass API Response: " + jsonResponse);
                    // Use the built-in runOnUiThread method
                    runOnUiThread(() -> parseCoordinates(jsonResponse));
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
                marker.setTitle("Location: " + locationObject.getString("display_name"));
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                mapView.getOverlays().add(marker);
                mapView.invalidate(); // Refresh the map

                Toast.makeText(this, "Moved to location: " + locationObject.getString("display_name"), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "No results found for the location.", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing coordinates", e);
            Toast.makeText(this, "Error parsing location data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // Method to fetch POIs like hospitals, banks, and schools from Overpass API
    private void fetchPOIs() {
        String overpassUrl = "http://overpass-api.de/api/interpreter?data=[out:json];(node[amenity=hospital](23.7,90.3,23.9,90.5);node[amenity=bank](23.7,90.3,23.9,90.5);node[amenity=school](23.7,90.3,23.9,90.5););out;";

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(overpassUrl).build();

        new Thread(() -> {
            try {
                Response response = client.newCall(request).execute();
                if (response.isSuccessful() && response.body() != null) {
                    String jsonResponse = response.body().string();
                    Log.d(TAG, "Overpass API Response: " + jsonResponse);
                    // Use the built-in runOnUiThread method
                    runOnUiThread(() -> displayPOIs(jsonResponse));
                } else {
                    String errorMsg = "Request failed. Code: " + response.code();
                    Log.e(TAG, errorMsg);
                    runOnUiThread(() -> Toast.makeText(this, "Failed to fetch POIs: " + errorMsg, Toast.LENGTH_LONG).show());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching POIs", e);
                runOnUiThread(() -> Toast.makeText(this, "Error fetching POIs: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    // Method to parse POIs JSON response and display markers on the map
    private void displayPOIs(String jsonResponse) {
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            JSONArray elements = jsonObject.getJSONArray("elements");

            for (int i = 0; i < elements.length(); i++) {
                JSONObject element = elements.getJSONObject(i);

                double lat = element.getDouble("lat");
                double lon = element.getDouble("lon");
                String type = element.optJSONObject("tags").optString("amenity", "Unknown");

                Log.d(TAG, "Adding marker: " + type + " at [" + lat + ", " + lon + "]");

                Marker marker = new Marker(mapView);
                marker.setPosition(new GeoPoint(lat, lon));
                marker.setTitle(type);
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                mapView.getOverlays().add(marker);
            }

            mapView.invalidate(); // Refresh map
            Toast.makeText(this, "POIs added to map", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error parsing POIs", e);
            Toast.makeText(this, "Error parsing POIs: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // Method to calculate a route between two points
    private void calculateRoute(GeoPoint startPoint, GeoPoint endPoint) {
        // Add markers for start and end points
        Marker startMarker = new Marker(mapView);
        startMarker.setPosition(startPoint);
        startMarker.setTitle("Start Point");
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        mapView.getOverlays().add(startMarker);

        Marker endMarker = new Marker(mapView);
        endMarker.setPosition(endPoint);
        endMarker.setTitle("End Point");
        endMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        mapView.getOverlays().add(endMarker);

        String osrmUrl = "http://router.project-osrm.org/route/v1/driving/" + startPoint.getLongitude() + "," + startPoint.getLatitude() + ";" + endPoint.getLongitude() + "," + endPoint.getLatitude() + "?overview=full&geometries=geojson";

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(osrmUrl).build();

        new Thread(() -> {
            try {
                Response response = client.newCall(request).execute();
                if (response.isSuccessful() && response.body() != null) {
                    String jsonResponse = response.body().string();
                    Log.d(TAG, "OSRM API Response: " + jsonResponse);
                    runOnUiThread(() -> displayRoute(jsonResponse, startPoint, endPoint));
                } else {
                    String errorMsg = "Route request failed. Code: " + response.code();
                    Log.e(TAG, errorMsg);
                    runOnUiThread(() -> Toast.makeText(this, "Failed to calculate route: " + errorMsg, Toast.LENGTH_LONG).show());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error calculating route", e);
                runOnUiThread(() -> Toast.makeText(this, "Error calculating route: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    // Method to display route on the map
    private void displayRoute(String jsonResponse, GeoPoint startPoint, GeoPoint endPoint) {
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            JSONArray coordinates = jsonObject.getJSONArray("routes").getJSONObject(0).getJSONObject("geometry").getJSONArray("coordinates");

            List<GeoPoint> geoPoints = new ArrayList<>();
            for (int i = 0; i < coordinates.length(); i++) {
                JSONArray coord = coordinates.getJSONArray(i);
                double lon = coord.getDouble(0);
                double lat = coord.getDouble(1);
                geoPoints.add(new GeoPoint(lat, lon));
            }

            // Create and style the polyline
            Polyline polyline = new Polyline();
            polyline.setPoints(geoPoints);
            polyline.setColor(getResources().getColor(android.R.color.holo_red_dark)); // Dark red color
            polyline.setWidth(7.0f); // Adjust width
            mapView.getOverlays().add(polyline);

            // Adjust map to fit the route
            BoundingBox boundingBox = BoundingBox.fromGeoPoints(geoPoints);
            mapView.zoomToBoundingBox(boundingBox, true);

            mapView.invalidate(); // Refresh the map
            Toast.makeText(this, "Route added to map", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error displaying route", e);
            Toast.makeText(this, "Error displaying route: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
