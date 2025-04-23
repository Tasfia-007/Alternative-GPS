//package com.example.myapplication;
//
//import android.os.Bundle;
//import android.util.Log;
//import android.view.View;
//import android.widget.AdapterView;
//import android.widget.Button;
//import android.widget.EditText;
//import android.widget.RelativeLayout;
//import android.widget.Spinner;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.appcompat.app.AppCompatActivity;
//
//import org.osmdroid.api.IMapController;
//import org.osmdroid.config.Configuration;
//import org.osmdroid.util.GeoPoint;
//import org.osmdroid.views.MapView;
//import org.osmdroid.views.overlay.Marker;
//import org.osmdroid.views.overlay.Polyline;
//
//import java.util.ArrayList;
//import java.util.List;
//
//import okhttp3.OkHttpClient;
//import okhttp3.Request;
//import okhttp3.Response;
//
//import org.json.JSONArray;
//import org.json.JSONObject;
//
//public class RoutingActivity extends AppCompatActivity {
//
//    private EditText fromLocation;
//    private EditText toLocation;
//    private Button getRouteButton;
//    private MapView mapView;
//
//    private View routeInfoContainer;
//    private TextView bestRouteTime;
//    private TextView alternativeRoutes;
//
//
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_routing);
//
//        // Initialize Views
//        fromLocation = findViewById(R.id.from_location);
//        toLocation = findViewById(R.id.to_location);
//        getRouteButton = findViewById(R.id.get_route_button);
//        mapView = findViewById(R.id.mapview);
//        Spinner travelModeSpinner = findViewById(R.id.travel_mode_spinner);  // Initialize Spinner
//
//        // Configure MapView
//        Configuration.getInstance().setUserAgentValue(getApplicationContext().getPackageName());
//        mapView.setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK);
//        mapView.setMultiTouchControls(true);
//        IMapController mapController = mapView.getController();
//        mapController.setZoom(10.0);
//        mapController.setCenter(new GeoPoint(23.8103, 90.4125)); // Default to Dhaka
//
//        // Default mode is "driving"
//        final String[] selectedMode = {"driving"};
//
//        // Listen for Spinner selection
//        travelModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//            @Override
//            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
//                selectedMode[0] = parentView.getItemAtPosition(position).toString().toLowerCase(); // Update mode
//            }
//
//            @Override
//            public void onNothingSelected(AdapterView<?> parentView) {
//                selectedMode[0] = "driving"; // Default to driving
//            }
//        });
//
//
//        getRouteButton.setOnClickListener(v -> {
//            String from = fromLocation.getText().toString().trim();
//            String to = toLocation.getText().toString().trim();
//
//            if (from.isEmpty() || to.isEmpty()) {
//                Toast.makeText(this, "Please enter both locations", Toast.LENGTH_SHORT).show();
//                return;
//            }
//
//            // Pass selectedMode to fetchCoordinatesAndDisplayRoute()
//            fetchCoordinatesAndDisplayRoute(from, to, selectedMode[0]);
//        });
//
//
//
//
//
//        // New elements for route information
//        routeInfoContainer = findViewById(R.id.route_info_container);
//        bestRouteTime = findViewById(R.id.best_route_time);
//        alternativeRoutes = findViewById(R.id.alternative_routes);
//
//        // Hide the info box initially
//        routeInfoContainer.setVisibility(View.GONE);
//    }
//
//
//
//    private void fetchCoordinatesAndDisplayRoute(String from, String to, String mode) {
//        new Thread(() -> {
//            try {
//                GeoPoint fromPoint = geocodeLocation(from);
//                GeoPoint toPoint = geocodeLocation(to);
//
//                if (fromPoint != null && toPoint != null) {
//                    fetchRoute(fromPoint, toPoint, mode); // Pass selected mode
//                } else {
//                    runOnUiThread(() -> Toast.makeText(this, "Failed to fetch coordinates.", Toast.LENGTH_SHORT).show());
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//                runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
//            }
//        }).start();
//    }
//
//
//    private GeoPoint geocodeLocation(String location) {
//        try {
//            String url = "https://nominatim.openstreetmap.org/search?q=" + location
//                    + "&format=json&addressdetails=1&countrycodes=bd"; // Restrict to Bangladesh
//            OkHttpClient client = new OkHttpClient();
//            Request request = new Request.Builder().url(url).build();
//            Response response = client.newCall(request).execute();
//
//            if (response.isSuccessful() && response.body() != null) {
//                JSONArray jsonArray = new JSONArray(response.body().string());
//                if (jsonArray.length() > 0) {
//                    JSONObject jsonObject = jsonArray.getJSONObject(0);
//                    double lat = jsonObject.getDouble("lat");
//                    double lon = jsonObject.getDouble("lon");
//                    return new GeoPoint(lat, lon);
//                }
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return null; // Return null if geocoding fails
//    }
//
//
//
//
////    private void fetchRoute(GeoPoint fromPoint, GeoPoint toPoint, String mode) {
////        runOnUiThread(() -> {
////            mapView.getOverlays().clear();
////            mapView.invalidate();
////        });
////
////        // Use OSRM API with the selected transport mode
////        String osrmUrl = "http://router.project-osrm.org/route/v1/" + mode + "/" +
////                fromPoint.getLongitude() + "," + fromPoint.getLatitude() + ";" +
////                toPoint.getLongitude() + "," + toPoint.getLatitude() +
////                "?overview=full&geometries=geojson&alternatives=true";
////
////        new Thread(() -> {
////            try {
////                OkHttpClient client = new OkHttpClient();
////                Request request = new Request.Builder().url(osrmUrl).build();
////                Response response = client.newCall(request).execute();
////
////                if (response.isSuccessful() && response.body() != null) {
////                    String jsonResponse = response.body().string();
////                    JSONObject jsonObject = new JSONObject(jsonResponse);
////                    JSONArray routes = jsonObject.getJSONArray("routes");
////
////                    if (routes.length() > 0) {
////                        runOnUiThread(() -> displayRoute(jsonResponse, fromPoint, toPoint));
////                    }
////                }
////            } catch (Exception e) {
////                e.printStackTrace();
////                runOnUiThread(() -> Toast.makeText(this, "Error fetching route: " + e.getMessage(), Toast.LENGTH_LONG).show());
////            }
////        }).start();
////    }
////
//
//
//    private void fetchRoute(GeoPoint fromPoint, GeoPoint toPoint, String mode) {
//        runOnUiThread(() -> {
//            mapView.getOverlays().clear();
//            mapView.invalidate();
//        });
//
//        // Use OSRM API with the selected transport mode
//        String osrmUrl = "http://router.project-osrm.org/route/v1/" + mode + "/" +
//                fromPoint.getLongitude() + "," + fromPoint.getLatitude() + ";" +
//                toPoint.getLongitude() + "," + toPoint.getLatitude() +
//                "?overview=full&geometries=geojson&alternatives=true&steps=true"; // Add 'steps=true' to get detailed route information
//
//        new Thread(() -> {
//            try {
//                OkHttpClient client = new OkHttpClient();
//                Request request = new Request.Builder().url(osrmUrl).build();
//                Response response = client.newCall(request).execute();
//
//                if (response.isSuccessful() && response.body() != null) {
//                    String jsonResponse = response.body().string();
//                    JSONObject jsonObject = new JSONObject(jsonResponse);
//                    JSONArray routes = jsonObject.getJSONArray("routes");
//
//                    if (routes.length() > 0) {
//                        runOnUiThread(() -> displayRoute(jsonResponse, fromPoint, toPoint));
//                    }
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//                runOnUiThread(() -> Toast.makeText(this, "Error fetching route: " + e.getMessage(), Toast.LENGTH_LONG).show());
//            }
//        }).start();
//    }
//
//
//    private void displayRoute(String jsonResponse, GeoPoint fromPoint, GeoPoint toPoint) {
//        try {
//            JSONObject jsonObject = new JSONObject(jsonResponse);
//            JSONArray routes = jsonObject.getJSONArray("routes");
//
//            if (routes.length() == 0) {
//                Toast.makeText(this, "No routes found.", Toast.LENGTH_SHORT).show();
//                return;
//            }
//
//            int totalAlternatives = 0;
//
//            // Loop through each route
//            for (int i = 0; i < routes.length(); i++) {
//                JSONObject route = routes.getJSONObject(i);
//                JSONArray coordinates = route.getJSONObject("geometry").getJSONArray("coordinates");
//
//                List<GeoPoint> geoPoints = new ArrayList<>();
//                for (int j = 0; j < coordinates.length(); j++) {
//                    JSONArray coord = coordinates.getJSONArray(j);
//                    double lon = coord.getDouble(0);
//                    double lat = coord.getDouble(1);
//                    geoPoints.add(new GeoPoint(lat, lon));
//                }
//
//                // Create the route polyline
//                Polyline polyline = new Polyline();
//                polyline.setPoints(geoPoints);
//
//                if (i == 0) {
//                    // Main route - Red Color
//                    polyline.setColor(getResources().getColor(android.R.color.holo_red_dark));
//                    polyline.setWidth(8.0f);
//                } else {
//                    // Alternative routes - Blue or Violet Color
//                    polyline.setColor(getResources().getColor(android.R.color.holo_blue_dark)); // Blue
//                    polyline.setWidth(6.0f);
//                    polyline.getPaint().setAlpha(150); // Semi-transparent for alternatives
//                    totalAlternatives++;
//                }
//
//                // Add the polyline to the map overlays
//                mapView.getOverlays().add(polyline);
//
//                // 🟢 **Attach Time Label Marker Above the Start Point**
//                Marker timeMarker = new Marker(mapView);
//                GeoPoint timeMarkerPosition = new GeoPoint(fromPoint.getLatitude() + 0.0005, fromPoint.getLongitude()); // Slightly above start
//                timeMarker.setPosition(timeMarkerPosition);
//                timeMarker.setTitle(route.getJSONArray("legs").getJSONObject(0).getDouble("duration") / 60 + " min"); // Show estimated time
//                timeMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
//                timeMarker.setTextLabelFontSize(24); // Increase text size
//                timeMarker.setTextLabelBackgroundColor(getResources().getColor(android.R.color.white)); // White background
//                timeMarker.setTextLabelForegroundColor(getResources().getColor(android.R.color.black)); // Black text
//
//                mapView.getOverlays().add(timeMarker);
//            }
//
//            // Add Start Marker
//            Marker startMarker = new Marker(mapView);
//            startMarker.setPosition(fromPoint);
//            startMarker.setTitle("Start: " + fromLocation.getText().toString());
//            startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
//            mapView.getOverlays().add(startMarker);
//
//            // Add End Marker
//            Marker endMarker = new Marker(mapView);
//            endMarker.setPosition(toPoint);
//            endMarker.setTitle("End: " + toLocation.getText().toString());
//            endMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
//            mapView.getOverlays().add(endMarker);
//
//            mapView.invalidate(); // Refresh the map to show new routes
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            Toast.makeText(this, "Error displaying routes: " + e.getMessage(), Toast.LENGTH_LONG).show();
//        }
//    }
//
//
//
//}



package com.example.myapplication;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

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

    private List<Polygon> savedPolygons = new ArrayList<>(); // List to store saved polygons

    private static final String TAG = "RoutingActivity"; // For logging

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_routing);

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
        loadPolygonsFromAssets();

        // Default mode is "driving"
        final String[] selectedMode = {"driving"};

        // Listen for Spinner selection
        travelModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                selectedMode[0] = parentView.getItemAtPosition(position).toString().toLowerCase(); // Update mode
                Log.d(TAG, "Selected travel mode: " + selectedMode[0]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                selectedMode[0] = "driving"; // Default to driving
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

            // Check if "from" and "to" are in lat,lon format (e.g., "23.8103,90.4125")
            boolean isFromCoordinates = from.matches("-?\\d+\\.\\d+,-?\\d+\\.\\d+");
            boolean isToCoordinates = to.matches("-?\\d+\\.\\d+,-?\\d+\\.\\d+");

            if (isFromCoordinates && isToCoordinates) {
                try {
                    // Parse "from" coordinates
                    String[] fromLatLon = from.split(",");
                    double fromLat = Double.parseDouble(fromLatLon[0].trim());
                    double fromLon = Double.parseDouble(fromLatLon[1].trim());
                    GeoPoint fromPoint = new GeoPoint(fromLat, fromLon);

                    // Parse "to" coordinates
                    String[] toLatLon = to.split(",");
                    double toLat = Double.parseDouble(toLatLon[0].trim());
                    double toLon = Double.parseDouble(toLatLon[1].trim());
                    GeoPoint toPoint = new GeoPoint(toLat, toLon);

                    Log.d(TAG, "Coordinates parsed: From - " + fromPoint + ", To - " + toPoint);
                    fetchRoute(fromPoint, toPoint, selectedMode[0]); // Directly fetch the route
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing coordinates", e);
                    Toast.makeText(this, "Invalid coordinates format", Toast.LENGTH_SHORT).show();
                }
            } else {
                // If not coordinates, use the existing geocoding method
                Log.d(TAG, "Fetching route from: " + from + " to: " + to);
                fetchCoordinatesAndDisplayRoute(from, to, selectedMode[0]);
            }
        });
    }

    private Drawable resizeDrawable(int drawableId, int width, int height) {
        Drawable drawable = ContextCompat.getDrawable(this, drawableId);
        if (drawable == null) return null;

        // If the drawable is already a BitmapDrawable, resize it
        if (drawable instanceof BitmapDrawable) {
            Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, width, height, false);
            return new BitmapDrawable(getResources(), resizedBitmap);
        }
        // If it's a VectorDrawable, convert it to a Bitmap and resize
        else if (drawable instanceof VectorDrawable) {
            return getBitmapFromVector(drawable, width, height);
        }
        return drawable; // Return original if not handled
    }

    private Drawable getBitmapFromVector(Drawable vectorDrawable, int width, int height) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        vectorDrawable.draw(canvas);
        return new BitmapDrawable(getResources(), bitmap);
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
        // Clear previous route polyline and markers, but keep polygons
        mapView.getOverlays().clear();
        mapView.invalidate();  // Ensure the map is refreshed before displaying new route
        Log.d(TAG, "Clearing previous overlays on map before new route.");

        // Create the base URL for fetching the route without obstacles
        String osrmUrl = "http://router.project-osrm.org/route/v1/" + mode + "/" +
                fromPoint.getLongitude() + "," + fromPoint.getLatitude() + ";" +
                toPoint.getLongitude() + "," + toPoint.getLatitude() +
                "?overview=full&geometries=geojson&alternatives=true&steps=true"; // Add 'steps=true' to get detailed route information

        // Perform the network request to fetch the route
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
                        runOnUiThread(() -> displayRoute(jsonResponse, fromPoint, toPoint));  // Ensure route is displayed on the UI thread
                        Log.d(TAG, "Route successfully fetched and displayed.");
                    } else {
                        Log.d(TAG, "No routes available.");
                    }
                } else {
                    Log.e(TAG, "Failed to fetch route.");
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Error fetching route: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }


    private void displayRoute(String jsonResponse, GeoPoint fromPoint, GeoPoint toPoint) {
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            JSONArray routes = jsonObject.getJSONArray("routes");

            // If no routes are found, show a toast
            if (routes.length() == 0) {
                Toast.makeText(this, "No routes found.", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "No routes found.");
                return;
            }

            // Define the colors with transparency for up to 5 routes
            int[] colors = new int[]{
                    Color.argb(150, 255, 0, 0), // Transparent Red
                    Color.argb(150, 0, 255, 0), // Transparent Green
                    Color.argb(150, 0, 0, 255), // Transparent Blue
                    Color.argb(150, 255, 255, 0), // Transparent Yellow
                    Color.argb(150, 0, 0, 0)  // Transparent Black
            };

            int routesToDisplay = Math.min(routes.length(), 5); // Display up to 5 routes

            // Loop through each route and display it
            for (int i = 0; i < routesToDisplay; i++) {
                JSONObject route = routes.getJSONObject(i);
                JSONArray coordinates = route.getJSONObject("geometry").getJSONArray("coordinates");

                List<GeoPoint> geoPoints = new ArrayList<>();
                for (int j = 0; j < coordinates.length(); j++) {
                    JSONArray coord = coordinates.getJSONArray(j);
                    double lon = coord.getDouble(0);
                    double lat = coord.getDouble(1);
                    geoPoints.add(new GeoPoint(lat, lon));
                }

                // Create the route polyline with a specific color
                Polyline polyline = new Polyline();
                polyline.setPoints(geoPoints);
                polyline.setColor(colors[i]);  // Use different colors for each route
                polyline.setWidth(8.0f);

                // Add the polyline to the map overlays
                mapView.getOverlays().add(polyline);
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

            // Re-add the polygons to the map after the routes are displayed
            for (Polygon polygon : savedPolygons) {
                mapView.getOverlays().add(polygon);
            }

            // Refresh the map to show the updated routes and markers
            mapView.invalidate();
            Log.d(TAG, "Routes and markers displayed on the map.");

            // Toast message to show the number of routes displayed
            if (routesToDisplay < 5) {
                Toast.makeText(this, routesToDisplay + " routes are shown.", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error displaying routes: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e(TAG, "Error displaying routes: " + e.getMessage());
        }
    }


    // Load saved polygons from CSV in assets folder
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
