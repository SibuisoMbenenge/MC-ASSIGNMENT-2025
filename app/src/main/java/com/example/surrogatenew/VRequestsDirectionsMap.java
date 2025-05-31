package com.example.surrogatenew;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView; // Make sure this is imported
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class VRequestsDirectionsMap extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap myMap;
    private SupportMapFragment mapFragment;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private FusedLocationProviderClient fusedLocationClient;

    private String requestorLocationString;
    private LatLng requestorLatLng;
    private String requestorName;
    private String requestorContact;

    private TextView tvOrderInfo; // CHANGED ID to match your XML
    private Button showDirectionsButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_vrequests_directions_map);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize UI elements matching your XML
        tvOrderInfo = findViewById(R.id.tvOrderInfo); // Corrected ID
        showDirectionsButton = findViewById(R.id.btnShowDirections);

        // Retrieve data from Intent
        Intent intent = getIntent();
        if (intent != null) {
            requestorLocationString = intent.getStringExtra("request_location");
            requestorName = intent.getStringExtra("request_name");
            requestorContact = intent.getStringExtra("request_contact");

            // Display the received information in tvOrderInfo
            StringBuilder infoBuilder = new StringBuilder();
            if (requestorName != null) infoBuilder.append("Name: ").append(requestorName).append("\n");
          //  if (requestorContact != null) infoBuilder.append("Contact: ").append(requestorContact).append("\n");
            if (requestorLocationString != null) infoBuilder.append("Location: ").append(requestorLocationString);
            tvOrderInfo.setText(infoBuilder.toString());

            Log.d("VRequestsDirectionsMap", "Received location: " + requestorLocationString +
                    ", Name: " + requestorName + ", Contact: " + requestorContact);
        } else {
            requestorLocationString = "Johannesburg, South Africa"; // Fallback/Default
            tvOrderInfo.setText("No request info received, using default location.");
            Toast.makeText(this, "No request information received, using default location.", Toast.LENGTH_LONG).show();
            Log.w("VRequestsDirectionsMap", "No intent data found.");
        }

        // Initialize map fragment
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        // Ensure map is initially hidden - your XML already handles this with android:visibility="gone"
        if (mapFragment != null && mapFragment.getView() != null) {
            mapFragment.getView().setVisibility(View.GONE); // Redundant if XML is correct, but safe.
        }


        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Set up the button click listener to load the map
        showDirectionsButton.setOnClickListener(view -> {
            // Make the map fragment visible when button is clicked
            if (mapFragment != null && mapFragment.getView() != null) {
                mapFragment.getView().setVisibility(View.VISIBLE);
            }

            // Load map - this will now trigger onMapReady
            if (mapFragment != null) {
                mapFragment.getMapAsync(this);
            } else {
                Toast.makeText(this, "Map fragment not found!", Toast.LENGTH_SHORT).show();
            }
        });

        // Handle Back, Take Order, Finish Order buttons if needed.
        // For example:
        findViewById(R.id.btnBack).setOnClickListener(v -> finish()); // Go back to previous activity
        // findViewById(R.id.btnTakeOrder).setOnClickListener(v -> { /* Implement take order logic */ });
        // findViewById(R.id.btnFinishOrder).setOnClickListener(v -> { /* Implement finish order logic */ });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        myMap = googleMap;

        // Clear existing markers to prevent duplicates if onMapReady is called multiple times
        myMap.clear();

        // Check and request location permission if not granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return; // Exit here, onMapReady will be called again if permission is granted
        }

        // Permission is granted, enable my location layer
        myMap.setMyLocationEnabled(true);
        myMap.getUiSettings().setMyLocationButtonEnabled(true);

        // 2. Convert the requestorLocationString to LatLng using Geocoder
        if (requestorLocationString != null && !requestorLocationString.isEmpty()) {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocationName(requestorLocationString, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    requestorLatLng = new LatLng(address.getLatitude(), address.getLongitude());
                    Log.d("VRequestsDirectionsMap", "Geocoded Requestor LatLng: " + requestorLatLng.latitude + ", " + requestorLatLng.longitude);

                    // Add marker for requestor's location
                    myMap.addMarker(new MarkerOptions()
                            .position(requestorLatLng)
                            .title("Requestor Location")
                            .snippet(requestorLocationString)); // Use the original string for snippet

                    // Move camera to show requestor's location initially
                    myMap.animateCamera(CameraUpdateFactory.newLatLngZoom(requestorLatLng, 15f));


                    // 3. Get current location and then launch Google Maps for directions
                    fusedLocationClient.getCurrentLocation(
                            com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                            null
                    ).addOnSuccessListener(location -> {
                        if (location != null) {
                            LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());

                            // Launch native Google Maps for directions
                            String directionsUri = "https://www.google.com/maps/dir/?api=1" +
                                    "&origin=" + currentLatLng.latitude + "," + currentLatLng.longitude +
                                    "&destination=" + requestorLatLng.latitude + "," + requestorLatLng.longitude +
                                    "&travelmode=driving";

                            Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(directionsUri));
                            mapIntent.setPackage("com.google.android.apps.maps");

                            if (mapIntent.resolveActivity(getPackageManager()) != null) {
                                startActivity(mapIntent);
                            } else {
                                Toast.makeText(this, "Google Maps app not found. Opening directions in browser.", Toast.LENGTH_LONG).show();
                                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(directionsUri));
                                startActivity(browserIntent);
                            }

                        } else {
                            Toast.makeText(this, "Unable to get your current location. Only requestor's location shown.", Toast.LENGTH_LONG).show();
                            myMap.animateCamera(CameraUpdateFactory.newLatLngZoom(requestorLatLng, 15f));
                        }
                    }).addOnFailureListener(e -> {
                        Log.e("VRequestsDirectionsMap", "Error getting current location: " + e.getMessage());
                        Toast.makeText(this, "Error getting current location. Only requestor's location shown.", Toast.LENGTH_LONG).show();
                        if (requestorLatLng != null) {
                            myMap.animateCamera(CameraUpdateFactory.newLatLngZoom(requestorLatLng, 15f));
                        }
                    });

                } else {
                    Toast.makeText(this, "Could not find coordinates for: " + requestorLocationString + ". Cannot show pin.", Toast.LENGTH_LONG).show();
                    Log.e("VRequestsDirectionsMap", "Geocoder returned no addresses for: " + requestorLocationString);
                    myMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(-26.2041, 28.0473), 10f)); // Default to JHB
                }
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Geocoder service not available. Cannot show pin.", Toast.LENGTH_LONG).show();
                Log.e("VRequestsDirectionsMap", "Geocoder service error: " + e.getMessage());
                myMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(-26.2041, 28.0473), 10f)); // Default to JHB
            }
        } else {
            Toast.makeText(this, "Requestor location is empty. Cannot show pin.", Toast.LENGTH_LONG).show();
            myMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(-26.2041, 28.0473), 10f)); // Default to JHB
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, re-try loading map (will re-trigger onMapReady)
                if (mapFragment != null) {
                    mapFragment.getMapAsync(this);
                }
            } else {
                Toast.makeText(this, "Location permission is required to show your location and get directions.", Toast.LENGTH_LONG).show();
                if (showDirectionsButton != null) {
                    showDirectionsButton.setEnabled(false); // Disable button if permission denied
                }
            }
        }
    }
}