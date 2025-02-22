package com.example.myapplication;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
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

    private View routeInfoContainer;
    private TextView bestRouteTime;
    private TextView alternativeRoutes;



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

        // Default mode is "driving"
        final String[] selectedMode = {"driving"};

        // Listen for Spinner selection
        travelModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                selectedMode[0] = parentView.getItemAtPosition(position).toString().toLowerCase(); // Update mode
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                selectedMode[0] = "driving"; // Default to driving
            }
        });


        getRouteButton.setOnClickListener(v -> {
            String from = fromLocation.getText().toString().trim();
            String to = toLocation.getText().toString().trim();

            if (from.isEmpty() || to.isEmpty()) {
                Toast.makeText(this, "Please enter both locations", Toast.LENGTH_SHORT).show();
                return;
            }

            // Pass selectedMode to fetchCoordinatesAndDisplayRoute()
            fetchCoordinatesAndDisplayRoute(from, to, selectedMode[0]);
        });





        // New elements for route information
        routeInfoContainer = findViewById(R.id.route_info_container);
        bestRouteTime = findViewById(R.id.best_route_time);
        alternativeRoutes = findViewById(R.id.alternative_routes);

        // Hide the info box initially
        routeInfoContainer.setVisibility(View.GONE);
    }



    private void fetchCoordinatesAndDisplayRoute(String from, String to, String mode) {
        new Thread(() -> {
            try {
                GeoPoint fromPoint = geocodeLocation(from);
                GeoPoint toPoint = geocodeLocation(to);

                if (fromPoint != null && toPoint != null) {
                    fetchRoute(fromPoint, toPoint, mode); // Pass selected mode
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
//                "?overview=full&geometries=geojson&alternatives=true";
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


    private void fetchRoute(GeoPoint fromPoint, GeoPoint toPoint, String mode) {
        runOnUiThread(() -> {
            mapView.getOverlays().clear();
            mapView.invalidate();
        });

        // Use OSRM API with the selected transport mode
        String osrmUrl = "http://router.project-osrm.org/route/v1/" + mode + "/" +
                fromPoint.getLongitude() + "," + fromPoint.getLatitude() + ";" +
                toPoint.getLongitude() + "," + toPoint.getLatitude() +
                "?overview=full&geometries=geojson&alternatives=true&steps=true"; // Add 'steps=true' to get detailed route information

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
                        runOnUiThread(() -> displayRoute(jsonResponse, fromPoint, toPoint));
                    }
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

            if (routes.length() == 0) {
                Toast.makeText(this, "No routes found.", Toast.LENGTH_SHORT).show();
                return;
            }

            int totalAlternatives = 0;

            // Loop through each route
            for (int i = 0; i < routes.length(); i++) {
                JSONObject route = routes.getJSONObject(i);
                JSONArray coordinates = route.getJSONObject("geometry").getJSONArray("coordinates");

                List<GeoPoint> geoPoints = new ArrayList<>();
                for (int j = 0; j < coordinates.length(); j++) {
                    JSONArray coord = coordinates.getJSONArray(j);
                    double lon = coord.getDouble(0);
                    double lat = coord.getDouble(1);
                    geoPoints.add(new GeoPoint(lat, lon));
                }

                // Create the route polyline
                Polyline polyline = new Polyline();
                polyline.setPoints(geoPoints);

                if (i == 0) {
                    // Main route - Red Color
                    polyline.setColor(getResources().getColor(android.R.color.holo_red_dark));
                    polyline.setWidth(8.0f);
                } else {
                    // Alternative routes - Blue or Violet Color
                    polyline.setColor(getResources().getColor(android.R.color.holo_blue_dark)); // Blue
                    polyline.setWidth(6.0f);
                    polyline.getPaint().setAlpha(150); // Semi-transparent for alternatives
                    totalAlternatives++;
                }

                // Add the polyline to the map overlays
                mapView.getOverlays().add(polyline);

                // 🟢 **Attach Time Label Marker Above the Start Point**
                Marker timeMarker = new Marker(mapView);
                GeoPoint timeMarkerPosition = new GeoPoint(fromPoint.getLatitude() + 0.0005, fromPoint.getLongitude()); // Slightly above start
                timeMarker.setPosition(timeMarkerPosition);
                timeMarker.setTitle(route.getJSONArray("legs").getJSONObject(0).getDouble("duration") / 60 + " min"); // Show estimated time
                timeMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                timeMarker.setTextLabelFontSize(24); // Increase text size
                timeMarker.setTextLabelBackgroundColor(getResources().getColor(android.R.color.white)); // White background
                timeMarker.setTextLabelForegroundColor(getResources().getColor(android.R.color.black)); // Black text

                mapView.getOverlays().add(timeMarker);
            }

            // Add Start Marker
            Marker startMarker = new Marker(mapView);
            startMarker.setPosition(fromPoint);
            startMarker.setTitle("Start: " + fromLocation.getText().toString());
            startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            mapView.getOverlays().add(startMarker);

            // Add End Marker
            Marker endMarker = new Marker(mapView);
            endMarker.setPosition(toPoint);
            endMarker.setTitle("End: " + toLocation.getText().toString());
            endMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            mapView.getOverlays().add(endMarker);

            mapView.invalidate(); // Refresh the map to show new routes

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error displaying routes: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }



}
