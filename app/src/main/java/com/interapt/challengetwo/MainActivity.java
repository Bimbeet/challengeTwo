package com.interapt.challengetwo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.interapt.challengetwo.databinding.ActivityMainBinding;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;


public class MainActivity extends AppCompatActivity implements LocationListener {
    private ActivityMainBinding binding;
    private final String apiKey = "";
    // Remember not to save API key to repo!
    PlacesClient placesClient;
    int PROXIMITY_RADIUS = 1609;
    double latitude;
    double longitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        View tView = binding.getRoot();
        setContentView(tView);
        Places.initialize(getApplicationContext(), apiKey);
        placesClient = Places.createClient(this);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            binding.searchButton.setVisibility(View.VISIBLE);
            binding.radiusInput.setVisibility(View.VISIBLE);
            binding.rInstruct.setVisibility(View.VISIBLE);
        } else {
            singleLocationRequest(null);
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

                        binding.needLoc.setVisibility(View.INVISIBLE);
                        binding.locPermButton.setVisibility(View.INVISIBLE);
                    } else {
                        binding.needLoc.setVisibility(View.VISIBLE);
                        binding.locPermButton.setVisibility(View.VISIBLE);
                    }
                }
            }
        }
    }

    public void singleLocationRequest(@Nullable View view) {
        Log.d("debugy", "singleLocationRequest - requestPermissions hit");
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                99);
        // on deny, currently button will not work in emulator testing
    }

    public void onSearchTouch(View v) {
        if (!binding.radiusInput.getText().toString().isEmpty()) {
            PROXIMITY_RADIUS = 1609 * Integer.parseInt(binding.radiusInput.getText().toString());
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
        lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 50000, 0, this);

        Location currlocation = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        Log.d("debugy", "Lat / long = " + currlocation.getLatitude() + " / " + currlocation.getLongitude());
        latitude = currlocation.getLatitude();
        longitude = currlocation.getLongitude();
//        longitude = -85.773934499646;
//        latitude = 38.200575703571;
        preformRequest();
    }

    private void preformRequest() {
        StringBuilder googlePlacesUrl =
                new StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json?");
        googlePlacesUrl.append("location=").append(latitude).append(",").append(longitude);
        googlePlacesUrl.append("&radius=").append(PROXIMITY_RADIUS);
        googlePlacesUrl.append("&types=restaurant&keyword=restaurant&sensor=true");
        googlePlacesUrl.append("&key=" + apiKey);
        Log.d("debugy", "Request = " + googlePlacesUrl.toString());

        JsonObjectRequest request = new JsonObjectRequest(googlePlacesUrl.toString(), null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject result) {
                        Log.i("debugy", "onResponse: Result = " + result.toString());
                        parseLocationResult(result);
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
        latitude = location.getLatitude();
        longitude = location.getLongitude();
//        preformRequest();
    }

    public static Bitmap getBitmapFromURL(String src) {
        try {
            URL url = new URL(src);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            Bitmap myBitmap = BitmapFactory.decodeStream(input);
            Log.e("Bitmap", "returned");
            return myBitmap;
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("Exception", e.getMessage());
            return null;
        }
    }

    private void parseLocationResult(final JSONObject result) {
        String name = null;
        String address = null;
        String photo_ref = null;
        try {
            JSONArray arr = result.getJSONArray("results");
            name = arr.getJSONObject(0).getString("name");
            Log.d("debugy", "JSONobject: name - " + name);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {
            JSONArray arr = result.getJSONArray("results");
            address = arr.getJSONObject(0).getString("formatted_address");
            Log.d("debugy", "JSONobject: address - " + address);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {
            JSONArray arr = result.getJSONArray("results");
            photo_ref = arr.getJSONObject(0).getJSONArray("photos").getJSONObject(0).getString("photo_reference");
            Log.d("debugy", "JSONobject: photo_ref - " + photo_ref);
//            JSONArray photoArr = arr.getJSONObject(0).getJSONArray("photos");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (photo_ref != null) {
            try {
                StringBuilder photoUrl =
                        new StringBuilder("https://maps.googleapis.com/maps/api/place/photo?maxwidth=800&photoreference=");
                photoUrl.append(photo_ref);
                photoUrl.append("&key=" + apiKey);
                Log.d("debugy", "Request = " + photoUrl);

                StringRequest request = new StringRequest(Request.Method.GET, photoUrl.toString(),
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                Log.i("debugy", "onResponse: response = " + response);
//                                photoReqResults[0] = result;
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

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (name != null || address != null) {
            binding.searchButton.setVisibility(View.INVISIBLE);
            binding.radiusInput.setVisibility(View.INVISIBLE);
            binding.rInstruct.setVisibility(View.INVISIBLE);
            binding.nameDisplay.setText(name);
            binding.nameDisplay.setVisibility(View.VISIBLE);
            binding.addressDisplay.setText(address);
            binding.addressDisplay.setVisibility(View.VISIBLE);
//            Log.d("debugy", "urlBitmap - " + photoReqResults[0]);
//            binding.placePhoto.setImageBitmap(getBitmapFromURL(photoUrl.toString()));
//            binding.placePhoto.setVisibility(View.VISIBLE);
        }
    }
}

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