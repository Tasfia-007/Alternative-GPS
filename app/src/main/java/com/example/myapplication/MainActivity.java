package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.PopupMenu;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.myapplication.AlertActivity;

import com.google.android.material.navigation.NavigationView;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private SharedPreferences sharedPreferences;
    private static final String TAG = "MainActivity";
    private static final int LOCATION_PERMISSION_REQUEST = 1;
    private DrawerLayout drawerLayout;
    private ImageView profileIcon;
    private TextView trafficSummary;
    private MapView mapView;
    private MyLocationNewOverlay locationOverlay;
    private SearchView searchView;
    private static final int SPEECH_REQUEST_CODE = 100;
    private static final int AUDIO_PERMISSION_REQUEST = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Configuration.getInstance().setUserAgentValue("com.example.myapplication/1.0");
        sharedPreferences = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
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
        ImageButton voiceSearchButton = findViewById(R.id.voice_search_button);
        voiceSearchButton.setOnClickListener(v -> {
            if (checkAndRequestAudioPermissions()) {
                startVoiceSearch();
            }
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
        // Profile Icon Click Listener
        profileIcon.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(MainActivity.this, profileIcon);
            popup.getMenuInflater().inflate(R.menu.profile_menu, popup.getMenu());

            updateMenuItems(popup.getMenu());
            popup.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.menu_login) {
                    if (isLoggedIn()) {
                        logoutUser(); // If logged in, log out
                    } else {
                        Intent loginIntent = new Intent(MainActivity.this, LoginActivity.class);
                        startActivity(loginIntent);
                    }
                    return true;
                } else if (itemId == R.id.menu_signup) {
                    Intent signupIntent = new Intent(MainActivity.this, SignUpActivity.class);
                    startActivity(signupIntent);
                    return true;
                } else if (itemId == R.id.menu_profile) {
                    if (isLoggedIn()) {
                        Intent profileIntent = new Intent(MainActivity.this, ProfileActivity.class);
                        startActivity(profileIntent);

                    } else {
                        Toast.makeText(MainActivity.this, "Please log in to access your profile.", Toast.LENGTH_SHORT).show();
                    }
                    return true;
                }
                return false;
            });

            popup.show();
        });


        // Bottom Panel Click Listener
        trafficSummary.setOnClickListener(v -> {
            Toast.makeText(this, "Traffic Summary Clicked", Toast.LENGTH_SHORT).show();
        });


        fetchPOIs(); // Ensure POIs are loaded before filtering

        // Setup category buttons (THIS FIXES YOUR ISSUE)
        setupCategoryButtons();


        sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        drawerLayout = findViewById(R.id.drawer_layout);

//for alert part
        menuIcon.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(MainActivity.this, menuIcon);
            popup.getMenuInflater().inflate(R.menu.drawer_menu, popup.getMenu());

            popup.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.alert) {
                    // Check if user is logged in before navigating
                    if (isLoggedIn()) {
                        Intent intent = new Intent(MainActivity.this, AlertActivity.class);
                        startActivity(intent);
                    } else {
                        Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
                    }
                    return true;
                } else if (itemId == R.id.home) {
                    Toast.makeText(this, "Home clicked", Toast.LENGTH_SHORT).show();
                    return true;
                } else  if (itemId == R.id.weather) {
                    // Navigate to WeatherActivity
                    Intent intent = new Intent(MainActivity.this, WeatherActivity.class);
                    startActivity(intent);
                    return true;
                } else if (itemId == R.id.report_issue) {
                    Intent intent = new Intent(MainActivity.this, ReportIssueActivity.class);
                    startActivity(intent);
                    return true;


            } else if (itemId == R.id.settings) {
                    Toast.makeText(this, "Settings clicked", Toast.LENGTH_SHORT).show();
                    return true;
                } else if (itemId == R.id.help) {
                    Toast.makeText(this, "Help clicked", Toast.LENGTH_SHORT).show();
                    return true;
                }
                return false;
            });

            popup.show();
        });

//for map location





        setupMapTouchListener();




    }






    // Set up touch listener for the map

//    private void setupMapTouchListener() {
//        MapEventsOverlay mapEventsOverlay = new MapEventsOverlay(new MapEventsReceiver() {
//            @Override
//            public boolean singleTapConfirmedHelper(GeoPoint p) {
//                displayLocationInfo(p); // Now it works correctly
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


    private boolean checkAndRequestAudioPermissions() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.RECORD_AUDIO}, AUDIO_PERMISSION_REQUEST);
            return false;
        }
        return true;
    }

    private void displayLocationInfo(GeoPoint geoPoint) {
        // Reverse geocode to get location name
        String locationName = getLocationName(geoPoint);

        // Create a new marker
        Marker marker = new Marker(mapView);
        marker.setPosition(geoPoint);
        marker.setTitle(locationName + "\nLat: " + geoPoint.getLatitude() + "\nLon: " + geoPoint.getLongitude());
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

        // Dynamically resize the icon based on zoom level
        float zoomLevel = (float) mapView.getZoomLevelDouble();
        int iconSize = (int) (zoomLevel * 5); // Adjust multiplier as needed
        Drawable resizedIcon = resizeDrawable(R.drawable.loc, iconSize, iconSize);
        marker.setIcon(resizedIcon);

        // Add marker to map
        mapView.getOverlays().add(marker);
        mapView.invalidate();

        // Show info window
        marker.showInfoWindow();

        // Remove marker after 4 seconds
        new Handler().postDelayed(() -> {
            marker.closeInfoWindow(); // Hide info box
            mapView.getOverlays().remove(marker); // Remove marker
            mapView.invalidate();
        }, 4000); // 4 seconds
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



    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }

    // Reverse geocode to get location name
    private String getLocationName(GeoPoint geoPoint) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        // Check if the device has internet connectivity
        if (!isNetworkAvailable()) {
            Log.e(TAG, "No internet connection");
            return "Internet required for location lookup";
        }

        try {
            List<Address> addresses = geocoder.getFromLocation(
                    geoPoint.getLatitude(),
                    geoPoint.getLongitude(),
                    1
            );

            if (addresses != null && !addresses.isEmpty()) {
                return addresses.get(0).getAddressLine(0);
            }
        } catch (IOException e) {
            Log.e(TAG, "Geocoder failed: " + e.getMessage(), e);
        }

        return "Unknown Location";
    }

    // Setup location overlay
//    private void setupLocationOverlay() {
//        locationOverlay = new MyLocationNewOverlay(mapView);
//        mapView.getOverlays().add(locationOverlay);
//        locationOverlay.enableMyLocation();
//        locationOverlay.enableFollowLocation();
//    }



    private void updateMenuItems(android.view.Menu menu) {
        if (isLoggedIn()) {
            menu.findItem(R.id.menu_login).setTitle("Logout");
            menu.findItem(R.id.menu_signup).setVisible(false); // Hide Sign Up if logged in
        } else {
            menu.findItem(R.id.menu_login).setTitle("Login");
            menu.findItem(R.id.menu_signup).setVisible(true); // Show Sign Up if not logged in
        }
    }



    // Check login state
    private boolean isLoggedIn() {
        return sharedPreferences.getBoolean("isLoggedIn", false);
    }

    // Log out user
    private void logoutUser() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear(); // Clear all stored session data
        editor.apply();

        Toast.makeText(this, "Logged out successfully!", Toast.LENGTH_SHORT).show();
        // Optionally, navigate to login screen or refresh activity
        recreate(); // Refresh the activity to update the menu
    }
    private void startVoiceSearch() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your destination...");
        try {
            startActivityForResult(intent, SPEECH_REQUEST_CODE);
        } catch (Exception e) {
            Toast.makeText(this, "Speech recognition not supported", Toast.LENGTH_SHORT).show();
        }
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                String spokenText = results.get(0);
                searchView.setQuery(spokenText, true); // Trigger search with spoken text
            }
        }
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

    private static final int MAX_MARKERS_PER_CATEGORY = 1000; // Adjust the number as needed


    private void setupCategoryButtons() {
        findViewById(R.id.btn_hospitals).setOnClickListener(v -> showPOI(hospitalMarkers, isHospitalVisible, "hospital"));
        findViewById(R.id.btn_banks).setOnClickListener(v -> showPOI(bankMarkers, isBankVisible, "bank"));
        findViewById(R.id.btn_schools).setOnClickListener(v -> showPOI(schoolMarkers, isSchoolVisible, "school"));
        findViewById(R.id.btn_police).setOnClickListener(v -> showPOI(policeMarkers, isPoliceVisible, "police"));
        findViewById(R.id.btn_gas_stations).setOnClickListener(v -> showPOI(gasStationMarkers, isGasStationVisible, "gas"));
        findViewById(R.id.btn_atms).setOnClickListener(v -> showPOI(atmMarkers, isAtmVisible, "atm"));
        findViewById(R.id.btn_libraries).setOnClickListener(v -> showPOI(libraryMarkers, isLibraryVisible, "library"));
    }

    // Track if each category is currently visible
    private boolean isHospitalVisible = false;
    private boolean isBankVisible = false;
    private boolean isSchoolVisible = false;
    private boolean isPoliceVisible = false;
    private boolean isGasStationVisible = false;
    private boolean isAtmVisible = false;
    private boolean isLibraryVisible = false;


    private void showPOI(List<Marker> markers, boolean isVisible, String category) {
        if (isVisible) {
            // If already visible, remove these POIs
            mapView.getOverlays().removeIf(overlay -> markers.contains(overlay));
            isVisible = false;
        } else {
            // Add POIs only if they were not visible before
            mapView.getOverlays().addAll(markers);
            isVisible = true;
        }

        // Ensure location overlay remains on the map
        if (locationOverlay != null) {
            mapView.getOverlays().add(locationOverlay);
        }

        // Ensure touch events remain
        mapView.getOverlays().add(new MapEventsOverlay(mapTouchReceiver));

        // Refresh map
        mapView.invalidate();

        // Update visibility flags
        switch (category) {
            case "hospital": isHospitalVisible = isVisible; break;
            case "bank": isBankVisible = isVisible; break;
            case "school": isSchoolVisible = isVisible; break;
            case "police": isPoliceVisible = isVisible; break;
            case "gas": isGasStationVisible = isVisible; break;
            case "atm": isAtmVisible = isVisible; break;
            case "library": isLibraryVisible = isVisible; break;
        }
    }






    private MapEventsOverlay mapTouchListener;
    private MapEventsReceiver mapTouchReceiver;

//    private MapEventsOverlay mapTouchListener;

    private void setupMapTouchListener() {
        mapTouchReceiver = new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                displayLocationInfo(p);
                return true;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                return false;
            }
        };

        mapView.getOverlays().add(new MapEventsOverlay(mapTouchReceiver));
    }




    private void displayPOIs(String jsonResponse) {
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            JSONArray elements = jsonObject.getJSONArray("elements");

            int hospitalCount = 0, bankCount = 0, schoolCount = 0, policeCount = 0;
            int gasStationCount = 0, shoppingMallCount = 0, atmCount = 0, libraryCount = 0;

            for (int i = 0; i < elements.length(); i++) {
                JSONObject element = elements.getJSONObject(i);
                double lat = element.getDouble("lat");
                double lon = element.getDouble("lon");
                String type = element.optJSONObject("tags").optString("amenity", "Unknown");

                Marker marker = new Marker(mapView);
                marker.setPosition(new GeoPoint(lat, lon));
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

                // Set and scale icons
                // Set and scale icons based on zoom level
                float zoomLevel = (float) mapView.getZoomLevelDouble();
                int iconSize = (int) (zoomLevel * 5); // Dynamically scale size based on zoom

                Drawable icon = null;
                switch (type) {
                    case "hospital":
                        if (hospitalCount++ < MAX_MARKERS_PER_CATEGORY)
                            icon = resizeIcon(R.drawable.hos, iconSize, iconSize);
                        hospitalMarkers.add(marker);
                        break;
                    case "bank":
                        if (bankCount++ < MAX_MARKERS_PER_CATEGORY)
                            icon = resizeIcon(R.drawable.bank, iconSize, iconSize);
                        bankMarkers.add(marker);
                        break;
                    case "school":
                        if (schoolCount++ < MAX_MARKERS_PER_CATEGORY)
                            icon = resizeIcon(R.drawable.schl, iconSize, iconSize);
                        schoolMarkers.add(marker);
                        break;
                    case "police":
                        if (policeCount++ < MAX_MARKERS_PER_CATEGORY)
                            icon = resizeIcon(R.drawable.police, iconSize, iconSize);
                        policeMarkers.add(marker);
                        break;
                    case "fuel":  // Gas Station
                        if (gasStationCount++ < MAX_MARKERS_PER_CATEGORY)
                            icon = resizeIcon(R.drawable.gas, iconSize, iconSize);
                        gasStationMarkers.add(marker);
                        break;
                    case "atm":
                        if (atmCount++ < MAX_MARKERS_PER_CATEGORY)
                            icon = resizeIcon(R.drawable.atm, iconSize, iconSize);
                        atmMarkers.add(marker);
                        break;
                    case "library":
                        if (libraryCount++ < MAX_MARKERS_PER_CATEGORY)
                            icon = resizeIcon(R.drawable.lib, iconSize, iconSize);
                        libraryMarkers.add(marker);
                        break;
                }

                if (icon != null) marker.setIcon(icon);

            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing POIs", e);
        }
    }


    private Drawable resizeIcon(int drawableId, int width, int height) {
        Drawable drawable = ContextCompat.getDrawable(this, drawableId);
        if (drawable == null) return null;

        // Handle PNGs (BitmapDrawable)
        if (drawable instanceof BitmapDrawable) {
            Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, width, height, false);
            return new BitmapDrawable(getResources(), resizedBitmap);
        }
        // Handle SVGs (VectorDrawable)
        else if (drawable instanceof VectorDrawable) {
            return getBitmapFromVector(drawable, width, height);
        }

        return drawable; // Return original if not handled
    }




    // Store POIs by category
    private List<Marker> hospitalMarkers = new ArrayList<>();
    private List<Marker> bankMarkers = new ArrayList<>();
    private List<Marker> schoolMarkers = new ArrayList<>();
    private List<Marker> policeMarkers = new ArrayList<>();
    private List<Marker> gasStationMarkers = new ArrayList<>();
    private List<Marker> shoppingMallMarkers = new ArrayList<>();
    private List<Marker> atmMarkers = new ArrayList<>();
    private List<Marker> libraryMarkers = new ArrayList<>();

    // Fetch POIs including police stations, gas stations, shopping malls, ATMs, and libraries
    private void fetchPOIs() {
        String overpassUrl = "http://overpass-api.de/api/interpreter?data=[out:json];" +
                "(node[amenity=hospital](23.7,90.3,23.9,90.5);" +
                "node[amenity=bank](23.7,90.3,23.9,90.5);" +
                "node[amenity=school](23.7,90.3,23.9,90.5);" +
                "node[amenity=police](23.7,90.3,23.9,90.5);" +
                "node[amenity=fuel](23.7,90.3,23.9,90.5);" +
                "node[shop=mall](23.7,90.3,23.9,90.5);" +
                "node[amenity=atm](23.7,90.3,23.9,90.5);" +
                "node[amenity=library](23.7,90.3,23.9,90.5);" +
                ");out;";

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(overpassUrl).build();

        new Thread(() -> {
            try {
                Response response = client.newCall(request).execute();
                if (response.isSuccessful() && response.body() != null) {
                    String jsonResponse = response.body().string();
                    runOnUiThread(() -> displayPOIs(jsonResponse));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching POIs", e);
            }
        }).start();
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



    // Fetch and show location name on touch




}