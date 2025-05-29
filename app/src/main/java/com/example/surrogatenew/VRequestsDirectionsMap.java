package com.example.surrogatenew;

import android.Manifest;
import android.content.pm.PackageManager;
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
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;


import android.graphics.Color; // For Polyline color
import android.os.AsyncTask;   // For AsyncTask base class

import com.google.android.gms.maps.model.PolylineOptions; // For drawing polylines
import com.google.maps.android.PolyUtil; // For decoding polylines (requires android-maps-utils dependency)

import org.json.JSONArray;   // For JSON parsing
import org.json.JSONObject;  // For JSON parsing

import java.io.BufferedReader; // For network requests
import java.io.IOException;    // For network requests
import java.io.InputStream;    // For network requests
import java.io.InputStreamReader; // For network requests
import java.net.HttpURLConnection; // For network requests
import java.net.URL;           // For network requests
import java.util.ArrayList;    // For List/HashMap implementations
import java.util.HashMap;      // For List/HashMap implementations
import java.util.List;         // For List/HashMap implementations



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
        Button showDirectionsButton = findViewById(R.id.btnShowDirections);

        // Initialize map fragment
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        showDirectionsButton.setOnClickListener(view -> {
            // Show the map fragment
            findViewById(R.id.map).setVisibility(View.VISIBLE);

            // Load map
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

                    // 🟦 Call your directions function
                    String url = getDirectionsUrl(currentLatLng, requestorLatLng);
                    new DownloadTask().execute(url);



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

    // ✅ helper method HERE
    private String getDirectionsUrl(LatLng origin, LatLng destination) {
        String strOrigin = "origin=" + origin.latitude + "," + origin.longitude;
        String strDest = "destination=" + destination.latitude + "," + destination.longitude;
        String mode = "mode=driving";
        String parameters = strOrigin + "&" + strDest + "&" + mode + "&key=AIzaSyCpopm2bPwjo6sLGAUroXUXBMx5UUY44WY";// <-- Put your key here
        return "https://maps.googleapis.com/maps/api/directions/json?" + parameters;
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

    private class DownloadTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... url) {
            String data = "";
            try {
                URL requestUrl = new URL(url[0]);
                HttpURLConnection connection = (HttpURLConnection) requestUrl.openConnection();
                connection.connect();

                InputStream inputStream = connection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

                StringBuffer buffer = new StringBuffer();
                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line);
                }

                data = buffer.toString();
                reader.close();
                inputStream.close();
                connection.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            new ParserTask().execute(result);
        }
    } // end of DownloadTask



    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {
            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;
            try {
                jObject = new JSONObject(jsonData[0]);
                DirectionsJSONParser parser = new DirectionsJSONParser();
                routes = parser.parse(jObject);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return routes;
        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            ArrayList<LatLng> points = null;
            PolylineOptions lineOptions = null; // Initialize here

            // Check if result is not null and has routes
            if (result != null && !result.isEmpty()) {
                for (int i = 0; i < result.size(); i++) {
                    points = new ArrayList<>();
                    lineOptions = new PolylineOptions(); // Initialize for each route if drawing multiple

                    List<HashMap<String, String>> path = result.get(i);
                    for (HashMap<String, String> point : path) {
                        double lat = Double.parseDouble(point.get("lat"));
                        double lng = Double.parseDouble(point.get("lng"));
                        points.add(new LatLng(lat, lng));
                    }

                    lineOptions.addAll(points);
                    lineOptions.width(10);
                    lineOptions.color(Color.BLUE);
                    // Add polyline to map for each route (if drawing multiple)
                    if (myMap != null) { // Ensure map is not null
                        myMap.addPolyline(lineOptions);
                    }
                }
            } else {
                // Handle case where no routes are found or result is null
                // Toast.makeText(getApplicationContext(), "No routes found", Toast.LENGTH_SHORT).show(); // Example
            }
        }
    } // end of ParserTask


    // START: DirectionsJSONParser class
    private class DirectionsJSONParser {
        public List<List<HashMap<String, String>>> parse(JSONObject jObject) {
            List<List<HashMap<String, String>>> routes = new ArrayList<>();
            JSONArray jRoutes, jLegs, jSteps;

            try {
                jRoutes = jObject.getJSONArray("routes");

                for (int i = 0; i < jRoutes.length(); i++) {
                    jLegs = ((JSONObject) jRoutes.get(i)).getJSONArray("legs");
                    List<HashMap<String, String>> path = new ArrayList<>();

                    for (int j = 0; j < jLegs.length(); j++) {
                        jSteps = ((JSONObject) jLegs.get(j)).getJSONArray("steps");

                        for (int k = 0; k < jSteps.length(); k++) {
                            String polyline = ((JSONObject) ((JSONObject) jSteps.get(k)).get("polyline")).getString("points");
                            List<LatLng> list = decodePoly(polyline);

                            for (LatLng l : list) {
                                HashMap<String, String> hm = new HashMap<>();
                                hm.put("lat", Double.toString(l.latitude));
                                hm.put("lng", Double.toString(l.longitude));
                                path.add(hm);
                            }
                        }
                        routes.add(path);
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            return routes;
        }

        private List<LatLng> decodePoly(String encoded) {
            List<LatLng> poly = new ArrayList<>();
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

                poly.add(new LatLng(lat / 1E5, lng / 1E5));
            }

            return poly;
        }
    } // END: DirectionsJSONParser class




}
