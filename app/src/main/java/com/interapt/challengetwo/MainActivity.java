package com.interapt.challengetwo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.interapt.challengetwo.databinding.ActivityMainBinding;

import org.json.JSONObject;


public class MainActivity extends AppCompatActivity implements LocationListener {
    private ActivityMainBinding binding;
    private final String apiKey = "AIzaSyBcwe1M5pUtNfkZqZ3SSerMNF9oD3Tvczc";
    // Remember not to save API key to repo!
    private int PROXIMITY_RADIUS = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        View tView = binding.getRoot();
        setContentView(tView);
        Places.initialize(getApplicationContext(), apiKey);
        PlacesClient placesClient = Places.createClient(this);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            binding.searchButton.setVisibility(View.VISIBLE);
            binding.radiusInput.setVisibility(View.VISIBLE);
            binding.rInstruct.setVisibility(View.VISIBLE);
//        } else if (shouldShowRequestPermissionRationale()) {
//            binding.needLoc.setVisibility(View.VISIBLE);
            // this would need work if we needed code for something weird, like the app not displaying the perm req on startup
        } else {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    99);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 99) {
            for (int i = 0; i < permissions.length; i++) {
                String permission = permissions[i];
                int grantResult = grantResults[i];

                if (permission.equals(Manifest.permission.ACCESS_FINE_LOCATION) || permission.equals(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                        binding.searchButton.setVisibility(View.VISIBLE);
                        binding.radiusInput.setVisibility(View.VISIBLE);
                        binding.rInstruct.setVisibility(View.VISIBLE);
                    } else {
                        binding.needLoc.setVisibility(View.VISIBLE);
//                        ActivityCompat.requestPermissions(MainActivity.this,
//                                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
//                                99);
                    }
                }
            }
        }
    }

    public void onSearchTouch(View v) {
        if (!binding.radiusInput.getText().toString().isEmpty()) {
            PROXIMITY_RADIUS = Integer.parseInt(binding.radiusInput.getText().toString());
            Log.d("debugy", "PROXIMITY_RADIUS = " + PROXIMITY_RADIUS);
        }
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        try {
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ex) {
            Log.d("debugy", "location_services Exception throw : " + ex.toString());
        }
        // although we did this check already, the app seems to require it when getting the getLastKnownLocation
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
//        try {
//            lm.requestSingleUpdate( LocationManager.NETWORK_PROVIDER, new LocationListener(), null );
//        } catch ( SecurityException e ) { e.printStackTrace(); }
        lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10000, 0, this);

        Location currlocation = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        Log.d("debugy", "Lat / long = " + currlocation.getLatitude() + " / " + currlocation.getLongitude());
        double latitude = currlocation.getLatitude();
        double longitude = currlocation.getLongitude();
//        double longitude = -85.773934499646;
//        double latitude = 38.200575703571;
        StringBuilder googlePlacesUrl =
                new StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json?");
        googlePlacesUrl.append("location=").append(latitude).append(",").append(longitude);
        googlePlacesUrl.append("&radius=").append(PROXIMITY_RADIUS);
        googlePlacesUrl.append("&types=food&sensor=true");
        googlePlacesUrl.append("&key=" + apiKey);
        Log.d("debugy", "Request = " + googlePlacesUrl.toString());

        JsonObjectRequest request = new JsonObjectRequest(googlePlacesUrl.toString(), null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject result) {
                        Log.i("debugy", "onResponse: Result = " + result.toString());
//                        parseLocationResult(result);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("debugy", "onErrorResponse: Error = " + error);
                        Log.e("debugy", "onErrorResponse: Error = " + error.getMessage());
                    }
                });

        SearchHelper.getInstance(this).addToRequestQueue(request);
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        Log.d("dj", "on location changed: " + location.getLatitude() + " & " + location.getLongitude());
    }
//    private void parseLocationResult(final JSONObject result) {}
}

//    class LocationListener myLocationListener = new LocationListener() {
//        @Override
//        public void onLocationChanged(Location location) {
//            Log.d("dj","on location changed: "+location.getLatitude()+" & "+location.getLongitude());
////            toastLocation(location);
//        }
//
//        @Override
//        public void onStatusChanged(String provider, int status, Bundle extras) {
//
//        }
//
//        @Override
//        public void onProviderEnabled(String provider) {
//
//        }
//
//        @Override
//        public void onProviderDisabled(String provider) {
//
//        }
//    };

class SearchHelper {
    @SuppressLint("StaticFieldLeak")
    // Unknown how to fix slight memory leak here caused by saving the instance as static
    private static SearchHelper INSTANCE;
    private RequestQueue requestQueue;
    private final Context context;

    private SearchHelper(Context ctx) {
        context = ctx;
        requestQueue = getRequestQueue();
    }

    public static synchronized SearchHelper getInstance(Context ctx) {
        if (INSTANCE == null) {
            INSTANCE = new SearchHelper(ctx);
        }
        return (INSTANCE);
    }

    public RequestQueue getRequestQueue() {
        if (requestQueue == null) {
            requestQueue = Volley.newRequestQueue(context.getApplicationContext());
        }
        return requestQueue;
    }

    public <T> void addToRequestQueue(Request<T> req) {
        getRequestQueue().add(req);
    }
}