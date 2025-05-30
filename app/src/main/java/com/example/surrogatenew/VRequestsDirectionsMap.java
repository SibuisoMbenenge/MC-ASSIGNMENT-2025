package com.example.surrogatenew;

import android.Manifest;
import android.content.Intent; // Import for Intent
import android.content.pm.PackageManager;
import android.net.Uri; // Import for Uri
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import android.location.Location;
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
// import androidx.fragment.app.FragmentActivity; // This import is not needed for AppCompatActivity

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

// --- REMOVED IMPORTS FOR POLYLINE DRAWING AND JSON PARSING ---
// import android.graphics.Color;
// import android.os.AsyncTask;
// import com.google.android.gms.maps.model.PolylineOptions;
// import com.google.maps.android.PolyUtil;
// import org.json.JSONArray;
// import org.json.JSONObject;
// import java.io.BufferedReader;
// import java.io.IOException;
// import java.io.InputStream;
// import java.io.InputStreamReader;
// import java.net.HttpURLConnection;
// import java.net.URL;
// import java.util.ArrayList;
// import java.util.HashMap;
// import java.util.List;
// --- END REMOVED IMPORTS ---


public class VRequestsDirectionsMap extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap myMap;
    private SupportMapFragment mapFragment;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private FusedLocationProviderClient fusedLocationClient;


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

        // Button to trigger map loading
        // We can rename this button to "Launch Directions in Maps App" or similar in XML if desired.
        Button showDirectionsButton = findViewById(R.id.btnShowDirections);

        // Initialize map fragment
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        showDirectionsButton.setOnClickListener(view -> {
            // Show the map fragment
            findViewById(R.id.map).setVisibility(View.VISIBLE);

            // Load map - this will now trigger the native map launch
            if (mapFragment != null) {
                mapFragment.getMapAsync(this);
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        myMap = googleMap;

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            myMap.setMyLocationEnabled(true);
            myMap.getUiSettings().setMyLocationButtonEnabled(true);


            // 1. Hardcoded requestor coordinates (Tyrwhitt, Rosebank, JHB)
            LatLng requestorLatLng = new LatLng(-26.1458, 28.0416);
            myMap.addMarker(new MarkerOptions()
                    .position(requestorLatLng)
                    .title("Requestor Location")
                    .snippet("Tyrwhitt, Rosebank"));


            // ✅ Get FRESH location (not cached)
            fusedLocationClient.getCurrentLocation(
                    com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                    null
            ).addOnSuccessListener(location -> {
                if (location != null) {
                    LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());

                    myMap.addMarker(new MarkerOptions().position(currentLatLng).title("You are here"));
                    myMap.addMarker(new MarkerOptions().position(requestorLatLng).title("Requestor is here"));

                    // Move camera to show both points
                    myMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 13f));

                    // --- NEW LOGIC: Launch native Google Maps for directions ---
                    String uri = "http://maps.google.com/maps?saddr=" + currentLatLng.latitude + "," + currentLatLng.longitude + "&daddr=" + requestorLatLng.latitude + "," + requestorLatLng.longitude + "&mode=driving";
                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                    mapIntent.setPackage("com.google.android.apps.maps"); // Optional: Tries to open specifically in Google Maps app

                    if (mapIntent.resolveActivity(getPackageManager()) != null) {
                        startActivity(mapIntent);
                    } else {
                        // Fallback if Google Maps app is not installed
                        Toast.makeText(this, "Google Maps app not found. Opening in browser.", Toast.LENGTH_LONG).show();
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                        startActivity(browserIntent);
                    }
                    // --- END NEW LOGIC ---

                } else {
                    Toast.makeText(this, "Unable to get current location", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED && myMap != null) {
                    myMap.setMyLocationEnabled(true);
                    myMap.getUiSettings().setMyLocationButtonEnabled(true);
                }
            } else {
                Toast.makeText(this, "Location permission is required to show your location on the map.", Toast.LENGTH_LONG).show();
            }
        }
    }//end of onRequestPermissionsResult

    // --- REMOVED ALL POLYLINE-RELATED CODE ---
    // The getDirectionsUrl method is no longer needed.
    // The DownloadTask inner class is no longer needed.
    // The ParserTask inner class is no longer needed.
    // The DirectionsJSONParser inner class is no longer needed.
    // --- END REMOVED CODE ---
}
