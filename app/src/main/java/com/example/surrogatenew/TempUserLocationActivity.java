package com.example.surrogatenew;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask; // Re-introducing AsyncTask for reverse geocoding
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.model.LatLng;

import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;

import org.json.JSONArray;   // For JSON parsing
import org.json.JSONObject;  // For JSON parsing

import java.io.BufferedReader; // For network requests
import java.io.IOException;    // For network requests
import java.io.InputStream;    // For network requests
import java.io.InputStreamReader; // For network requests
import java.net.HttpURLConnection; // For network requests
import java.net.URL;           // For network requests
import java.util.Arrays;


public class TempUserLocationActivity extends AppCompatActivity {

    private TextView selectedAddressTextView;
    private LatLng selectedLatLng; // Still useful to store coordinates
    private String selectedAddressString; // NEW: This variable will store the human-readable address

    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 101;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.temp_user_location_interface);

        selectedAddressTextView = findViewById(R.id.tv_selected_address);
        Button useLocationButton = findViewById(R.id.btn_use_current_location);
        Button enterButton = findViewById(R.id.btn_confirm_location); // Your "Enter" button

        // Initialize Places SDK (use your actual API key)
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), "AIzaSyCpopm2bPwjo6sLGAUroXUXBMx5UUY44WY");
        }
        // PlacesClient placesClient = Places.createClient(this); // This line is not directly used, can remove

        // Initialize FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);


        AutocompleteSupportFragment autocompleteFragment =
                (AutocompleteSupportFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.autocomplete_fragment);

        if (autocompleteFragment != null) {
            autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG));
            autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
                @Override
                public void onPlaceSelected(Place place) {
                    selectedLatLng = place.getLatLng(); // Store LatLng
                    selectedAddressString = place.getAddress(); // Store the address string directly from Places SDK
                    selectedAddressTextView.setText("Selected: " + selectedAddressString);
                    Toast.makeText(TempUserLocationActivity.this, "Place selected: " + place.getName(), Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(@NonNull Status status) {
                    Log.e("TempUserLocation", "An error occurred: " + status);
                    selectedAddressTextView.setText("Error: " + status.getStatusMessage());
                    Toast.makeText(TempUserLocationActivity.this, "Error in Autocomplete: " + status.getStatusMessage(), Toast.LENGTH_LONG).show();
                }
            });
        }

        useLocationButton.setOnClickListener(v -> {
            getLocationAndUpdateUI();
        });

        // The "Enter" button's action:
        enterButton.setOnClickListener(v -> {
            if (selectedLatLng != null && selectedAddressString != null && !selectedAddressString.isEmpty()) {
                // Now you have both the address string and coordinates available for database entry
                Log.d("LocationEntry", "Location stored in variable: Address='" + selectedAddressString + "', Lat=" + selectedLatLng.latitude + ", Lng=" + selectedLatLng.longitude);
                Toast.makeText(this, "Location stored: Address='" + selectedAddressString + "'", Toast.LENGTH_LONG).show();

            } else {
                Toast.makeText(this, "Please select or get a location first before pressing Enter.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Attempts to get the current location and update the UI.
     * Handles permission checks.
     */
    private void getLocationAndUpdateUI() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    null
            ).addOnSuccessListener(this, location -> {
                if (location != null) {
                    selectedLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                    // Instead of directly updating TextView with coordinates,
                    // initiate reverse geocoding to get the address string.
                    startReverseGeocodingTask(selectedLatLng);
                    Toast.makeText(this, "Fetching address for current location...", Toast.LENGTH_SHORT).show();
                } else {
                    selectedAddressString = "Unable to get address"; // Fallback
                    selectedAddressTextView.setText("Selected: Current Location (Unable to get address)");
                    Toast.makeText(this, "Unable to retrieve current location. Please ensure GPS is enabled.", Toast.LENGTH_LONG).show();
                    Log.w("TempUserLocation", "Current location is null.");
                }
            }).addOnFailureListener(this, e -> {
                selectedAddressString = "Error getting address"; // Fallback on error
                selectedAddressTextView.setText("Selected: Current Location (Error getting address)");
                Toast.makeText(this, "Error getting location: " + e.getMessage(), Toast.LENGTH_LONG).show();
                Log.e("TempUserLocation", "Error getting location: " + e.getMessage(), e);
            });
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            Toast.makeText(this, "Requesting location permission...", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Location permission granted! Getting current location...", Toast.LENGTH_SHORT).show();
                getLocationAndUpdateUI();
            } else {
                Toast.makeText(this, "Location permission denied. Cannot use current location.", Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Initiates an AsyncTask to perform reverse geocoding (coordinates to address).
     */
    private void startReverseGeocodingTask(LatLng latLng) {
        // Construct the Geocoding API URL
        String geocodingUrl = "https://maps.googleapis.com/maps/api/geocode/json?latlng="
                + latLng.latitude + "," + latLng.longitude
                + "&key=AIzaSyCpopm2bPwjo6sLGAUroXUXBMx5UUY44WY"; // Use your API key here

        new ReverseGeocodingTask().execute(geocodingUrl);
    }

    /**
     * AsyncTask to handle the network request for reverse geocoding and JSON parsing.
     */
    private class ReverseGeocodingTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... urls) {
            String data = "";
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            try {
                URL url = new URL(urls[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.connect();

                InputStream inputStream = urlConnection.getInputStream();
                reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder buffer = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line).append("\n");
                }
                data = buffer.toString();
            } catch (Exception e) {
                Log.e("ReverseGeocoding", "Error fetching geocoding data: " + e.getMessage(), e);
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        Log.e("ReverseGeocoding", "Error closing reader: " + e.getMessage(), e);
                    }
                }
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            try {
                JSONObject jsonObject = new JSONObject(result);
                // Check if status is "OK" and results are available
                if ("OK".equals(jsonObject.optString("status")) && jsonObject.has("results")) {
                    JSONArray resultsArray = jsonObject.getJSONArray("results");

                    if (resultsArray.length() > 0) {
                        JSONObject firstResult = resultsArray.getJSONObject(0);
                        String formattedAddress = firstResult.getString("formatted_address");
                        selectedAddressString = formattedAddress; // Store the address string
                        selectedAddressTextView.setText("Selected: Current Location (" + formattedAddress + ")");
                        Toast.makeText(TempUserLocationActivity.this, "Address found: " + formattedAddress, Toast.LENGTH_SHORT).show();
                    } else {
                        selectedAddressString = "Address not found"; // Fallback
                        selectedAddressTextView.setText("Selected: Current Location (Address not found)");
                        Toast.makeText(TempUserLocationActivity.this, "No address found for current location.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // Handle API error status (e.g., "ZERO_RESULTS", "OVER_QUERY_LIMIT", "REQUEST_DENIED")
                    String status = jsonObject.optString("status", "UNKNOWN_ERROR");
                    String errorMessage = jsonObject.optString("error_message", "No specific error message.");
                    selectedAddressString = "Error: " + status;
                    selectedAddressTextView.setText("Selected: Current Location (Error: " + status + ")");
                    Toast.makeText(TempUserLocationActivity.this, "Geocoding API error: " + errorMessage, Toast.LENGTH_LONG).show();
                    Log.e("ReverseGeocoding", "Geocoding API status: " + status + ", Message: " + errorMessage);
                }
            } catch (Exception e) {
                Log.e("ReverseGeocoding", "Error parsing geocoding JSON: " + e.getMessage(), e);
                selectedAddressString = "Error getting address"; // Fallback on parsing error
                selectedAddressTextView.setText("Selected: Current Location (Error getting address)");
                Toast.makeText(TempUserLocationActivity.this, "Failed to parse address data.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}