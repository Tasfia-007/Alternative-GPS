





package com.example.myapplication;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList; // Import ArrayList
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ReportIssueActivity extends AppCompatActivity {
    private SharedPreferences sharedPreferences;


    private static final String TAG = "ReportIssueActivity";
    private static final String FILE_NAME = "route_data.csv";

    private MapView mapView;
    private SearchView searchView;
    private GeoPoint[] points = new GeoPoint[4]; // Store the 4 points selected
    private Button btnSaveRoute, btnClearPoints;
    private Polygon polygon; // Polygon to hold the drawn polygon
    private List<Polygon> savedPolygons = new ArrayList<>(); // Initialize the list to store saved polygons
    private boolean isPolygonDrawn = false; // Flag to check if a polygon has been drawn
    private static final int CAMERA_REQUEST_CODE = 201; // New request code
    private Uri imageUri;
    private String imagePath = null;
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
        sharedPreferences = getSharedPreferences("UserSession",Context.MODE_PRIVATE);


    }




    // Check permissions
    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    // Request permissions
    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, new String[]{
                android.Manifest.permission.CAMERA,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        }, CAMERA_REQUEST_CODE);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera(); // If permission granted, now open camera
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
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
        boolean isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false);
        if (!isLoggedIn) {
            Toast.makeText(this, "You must log in to save area!", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Save attempt without login.");
            return;
        }

        if (points[0] == null || points[1] == null || points[2] == null || points[3] == null) {
            Toast.makeText(this, "Select all 4 points", Toast.LENGTH_SHORT).show();
            return;
        }

        // --- Show Dialog ---
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Enter Details");

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_save_area, null);
        builder.setView(dialogView);

        EditText placeNameInput = dialogView.findViewById(R.id.place_name_input);
        EditText waterLevelInput = dialogView.findViewById(R.id.water_level_input);
        EditText commentInput = dialogView.findViewById(R.id.comment_input);
        Button takePictureButton = dialogView.findViewById(R.id.take_picture_button);

        takePictureButton.setOnClickListener(v -> {
            if (hasCameraPermission()) {
                openCamera();
            } else {
                requestCameraPermission();
            }
        });



        builder.setPositiveButton("Save", (dialog, which) -> {
            String placeName = placeNameInput.getText().toString().trim();
            String waterLevel = waterLevelInput.getText().toString().trim();
            String comment = commentInput.getText().toString().trim();

            if (TextUtils.isEmpty(placeName) || TextUtils.isEmpty(waterLevel)) {
                Toast.makeText(this, "Place name and Water level are required", Toast.LENGTH_SHORT).show();
                return;
            }

            // Now save to CSV
            saveAreaWithDetails(placeName, waterLevel, comment, imagePath);

            // Clear selection
            clearPoints();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        builder.create().show();
    }



//    private void saveAreaWithDetails(String placeName, String waterLevel, String comment, String imagePath) {
//        File file = new File(getExternalFilesDir(null), FILE_NAME);
//        boolean isNewFile = !file.exists();
//
//        try (FileWriter writer = new FileWriter(file, true)) {
//            if (isNewFile) {
//                writer.append("Place Name,Water Level,Comment,Image Path,Point 1 Lat,Point 1 Lon,Point 2 Lat,Point 2 Lon,Point 3 Lat,Point 3 Lon,Point 4 Lat,Point 4 Lon\n");
//            }
//            String data = placeName + "," + waterLevel + "," + comment.replace(",", " ") + "," + (imagePath != null ? imagePath : "No Image") + "," +
//                    points[0].getLatitude() + "," + points[0].getLongitude() + "," +
//                    points[1].getLatitude() + "," + points[1].getLongitude() + "," +
//                    points[2].getLatitude() + "," + points[2].getLongitude() + "," +
//                    points[3].getLatitude() + "," + points[3].getLongitude() + "\n";
//            writer.append(data);
//            writer.flush();
//
//            Log.d(TAG, "Area saved with details: " + data);
//            Toast.makeText(this, "Area saved!", Toast.LENGTH_SHORT).show();
//        } catch (IOException e) {
//            Log.e(TAG, "Error writing CSV", e);
//        }
//    }


    private void openCamera() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "New Picture");
        values.put(MediaStore.Images.Media.DESCRIPTION, "From the Camera");
        imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);

        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE);
        } else {
            Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == CAMERA_REQUEST_CODE && imageUri != null) {
                imagePath = saveImageLocally(imageUri); // Save captured image
                Toast.makeText(this, "Picture Taken!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Save image locally
    private String saveImageLocally(Uri uri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
            File file = new File(getFilesDir(), UUID.randomUUID().toString() + ".jpg");
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
            return file.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save image locally", Toast.LENGTH_SHORT).show();
            return null;
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








    private static final String SUPABASE_URL = "https://kquvuygavkhsxvdpqyfn.supabase.co"; // Replace with your Supabase URL
    private static final String SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImtxdXZ1eWdhdmtoc3h2ZHBxeWZuIiwicm9sZSI6ImFub24iLCJpYXQiOjE3MzcxMDQ4NjcsImV4cCI6MjA1MjY4MDg2N30.YVPKExfM-ZxzO9JvM9RQZQrBiyG1iT50fiwGUcvw8EI";






    private void saveAreaWithDetails(String placeName, String waterLevel, String comment, String imagePath) {
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();

                String finalImageUrl = null;

                if (imagePath != null) {
                    // First compress the image before uploading
                    File imageFile = new File(imagePath);
                    imageFile = compressImage(imageFile); // 🔥 Compress before uploading

                    RequestBody fileBody = RequestBody.create(imageFile, okhttp3.MediaType.parse("image/jpeg"));
                    String uniqueFileName = UUID.randomUUID().toString() + ".jpg";

                    RequestBody requestBodyUpload = new MultipartBody.Builder()
                            .setType(MultipartBody.FORM)
                            .addFormDataPart("file", uniqueFileName, fileBody)
                            .build();

                    String bucketName = "waterlogging-pictures"; // YOUR SUPABASE BUCKET NAME
                    String uploadUrl = SUPABASE_URL + "/storage/v1/object/" + bucketName + "/" + uniqueFileName;

                    Request uploadRequest = new Request.Builder()
                            .url(uploadUrl)
                            .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                            .post(requestBodyUpload)
                            .build();

                    Response uploadResponse = client.newCall(uploadRequest).execute();

                    if (uploadResponse.isSuccessful()) {
                        finalImageUrl = SUPABASE_URL + "/storage/v1/object/public/" + bucketName + "/" + uniqueFileName;
                        Log.d(TAG, "Image uploaded successfully: " + finalImageUrl);
                    } else {
                        Log.e(TAG, "Image upload failed: " + uploadResponse.message());
                    }
                }

                // Now prepare entry for table
                String entryTime = java.time.LocalDateTime.now().toString(); // Current time in ISO format

                JSONObject json = new JSONObject();
                json.put("entry_time", entryTime);
                json.put("place_name", placeName);
                json.put("water_level", Integer.parseInt(waterLevel)); // Make sure integer
                json.put("comment", TextUtils.isEmpty(comment) ? JSONObject.NULL : comment); // Use JSONObject.NULL if empty
                json.put("picture_url", finalImageUrl != null ? finalImageUrl : JSONObject.NULL);

                RequestBody requestBodyInsert = RequestBody.create(
                        json.toString(),
                        okhttp3.MediaType.parse("application/json")
                );

                Request insertRequest = new Request.Builder()
                        .url(SUPABASE_URL + "/rest/v1/water_logging_data")
                        .addHeader("apikey", SUPABASE_KEY)
                        .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                        .addHeader("Content-Type", "application/json")
                        .post(requestBodyInsert)
                        .build();

                Response insertResponse = client.newCall(insertRequest).execute();

                if (insertResponse.isSuccessful()) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Area saved successfully to Supabase!", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Data inserted successfully.");
                    });
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Failed to insert data.", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Insert failed: " + insertResponse.message());
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Save error: " + e.getMessage());
                });
            }
        }).start();
    }




    private File compressImage(File originalFile) {
        try {
            Bitmap bitmap = BitmapFactory.decodeFile(originalFile.getAbsolutePath());
            File compressedFile = new File(getFilesDir(), UUID.randomUUID().toString() + ".jpg");

            FileOutputStream out = new FileOutputStream(compressedFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out); // Compression 80%
            out.close();

            return compressedFile;
        } catch (IOException e) {
            e.printStackTrace();
            return originalFile;
        }
    }








}