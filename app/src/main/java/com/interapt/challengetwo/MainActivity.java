package com.interapt.challengetwo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
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
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.PhotoMetadata;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPhotoRequest;
import com.google.android.libraries.places.api.net.FetchPhotoResponse;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FetchPlaceResponse;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.interapt.challengetwo.databinding.ActivityMainBinding;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity implements LocationListener {
    private ActivityMainBinding binding;
    private final String apiKey = "AIzaSyBcwe1M5pUtNfkZqZ3SSerMNF9oD3Tvczc";
    // Remember not to save API key to repo!
    PlacesClient placesClient;
    int PROXIMITY_RADIUS = 800;
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
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
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

                if (permission.equals(Manifest.permission.ACCESS_COARSE_LOCATION)) {
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
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                99);
        // on deny, currently button will not work in emulator testing
    }

    public void onSearchTouch(View v) {
        if (!binding.radiusInput.getText().toString().isEmpty()) {
            if ((1609 * Integer.parseInt(binding.radiusInput.getText().toString())) < 50000) {
                PROXIMITY_RADIUS = 1609 * Integer.parseInt(binding.radiusInput.getText().toString());
            } else {
                PROXIMITY_RADIUS = 48000;
            }
            Log.d("debugy", "PROXIMITY_RADIUS in meters = " + PROXIMITY_RADIUS);
        }
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        try {
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ex) {
            Log.d("debugy", "location_services Exception throw : " + ex.toString());
        }
        // although we did this check already, the app seems to require it when getting the getLastKnownLocation
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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

    private void parseLocationResult(final JSONObject result) {
        String name = null;
        String address = null;
        String photo_ref = null;
        String place_id = null;
        try {
            JSONArray placesResultsArray = result.getJSONArray("results");
            try {
                name = placesResultsArray.getJSONObject(0).getString("name");
                Log.d("debugy", "JSONobject: name - " + name);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            try {
                place_id = placesResultsArray.getJSONObject(0).getString("place_id");
                Log.d("debugy", "JSONobject: place_id - " + place_id);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            try {
                address = placesResultsArray.getJSONObject(0).getString("formatted_address");
                Log.d("debugy", "JSONobject: address - " + address);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
//        try {
//            JSONArray arr = result.getJSONArray("results");
//            photo_ref = arr.getJSONObject(0).getJSONArray("photos").getJSONObject(0).getString("photo_reference");
//            Log.d("debugy", "JSONobject: photo_ref - " + photo_ref);
////            JSONArray photoArr = arr.getJSONObject(0).getJSONArray("photos");
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//
//        if (photo_ref != null) {
//            try {
//                StringBuilder photoUrl =
//                        new StringBuilder("https://maps.googleapis.com/maps/api/place/photo?maxwidth=800&photoreference=");
//                photoUrl.append(photo_ref);
//                photoUrl.append("&key=" + apiKey);
//                Log.d("debugy", "Request = " + photoUrl);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }

        if (place_id != null) {
            final FetchPlaceRequest placeRequest = FetchPlaceRequest.newInstance(place_id, Collections.singletonList(Place.Field.PHOTO_METADATAS));
            placesClient.fetchPlace(placeRequest).addOnSuccessListener(new OnSuccessListener<FetchPlaceResponse>() {
                @Override
                public void onSuccess(FetchPlaceResponse response) {
                    final Place place = response.getPlace();
                    final List<PhotoMetadata> metadata = place.getPhotoMetadatas();
                    if (metadata == null || metadata.isEmpty()) {
                        Log.w("debugy", "No photo metadata.");
                        return;
                    }
                    final PhotoMetadata photoMetadata = metadata.get(0);
//                    final String attributions = photoMetadata.getAttributions();
                    final FetchPhotoRequest photoRequest = FetchPhotoRequest.builder(photoMetadata).build();
                    placesClient.fetchPhoto(photoRequest).addOnSuccessListener(new OnSuccessListener<FetchPhotoResponse>() {
                        @Override
                        public void onSuccess(FetchPhotoResponse fetchPhotoResponse) {
                            Bitmap bitmap = fetchPhotoResponse.getBitmap();
                            binding.placePhoto.setImageBitmap(bitmap);
                            binding.placePhoto.setVisibility(View.VISIBLE);
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            if (exception instanceof ApiException) {
                                final ApiException apiException = (ApiException) exception;
                                Log.e("debugy", "Place not found: " + exception.getMessage());
                            }
                        }
                    });
                }
            });

            if (name != null || address != null) {
                binding.searchButton.setVisibility(View.INVISIBLE);
                binding.radiusInput.setVisibility(View.INVISIBLE);
                binding.rInstruct.setVisibility(View.INVISIBLE);
                binding.nameDisplay.setText(name);
                binding.nameDisplay.setVisibility(View.VISIBLE);
                binding.addressDisplay.setText(address);
                binding.addressDisplay.setVisibility(View.VISIBLE);
            }
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