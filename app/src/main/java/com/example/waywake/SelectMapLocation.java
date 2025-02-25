package com.example.waywake;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.config.Configuration;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;


import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class SelectMapLocation extends AppCompatActivity {

    private MapView mapView;
    GeoPoint selectedPoint;
    Marker marker;

    private MyLocationNewOverlay locationOverlay;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().setUserAgentValue(getPackageName());
        setContentView(R.layout.activity_select_map_location);

        mapView = findViewById(R.id.osm_map);
        Button saveButton = findViewById(R.id.save_button);
        ImageView backButton = findViewById(R.id.back_button);
        EditText searchPlaceInput = findViewById(R.id.search_place);
        ImageButton myLocationButton = findViewById(R.id.my_location_button);
        LinearLayout backgroundLayout = findViewById(R.id.background_layout);

        mapView.setMultiTouchControls(true);


        // Set default zoom and center
        GeoPoint startPoint = new GeoPoint(20.5937, 78.9629); // India
        mapView.getController().setZoom(10.0);
        mapView.getController().setCenter(startPoint);

        // Add gesture overlay for map rotation
        RotationGestureOverlay rotationOverlay = new RotationGestureOverlay(mapView);
        rotationOverlay.setEnabled(true);
        mapView.getOverlays().add(rotationOverlay);

        showCurrentLocation();

        GestureDetector gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(@NonNull MotionEvent e) {
                IGeoPoint geoPoint = mapView.getProjection().fromPixels((int) e.getX(), (int) e.getY());
                selectedPoint= (GeoPoint) geoPoint;
                selectMarker(new GeoPoint(geoPoint.getLatitude(), geoPoint.getLongitude()));
                return true;
            }

        });

        mapView.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            if (myLocationButton.isSelected()) {
                myLocationButton.callOnClick();
            }
            return false;
        });

        saveButton.setOnClickListener(v -> {
            if(selectedPoint!=null){
                Intent resultIntent = new Intent();
                resultIntent.putExtra("latitude", selectedPoint.getLatitude());
                resultIntent.putExtra("longitude", selectedPoint.getLongitude());
                setResult(RESULT_OK, resultIntent);
                finish();
            }else{
                Toast.makeText(this, "Select your destination", Toast.LENGTH_SHORT).show();
            }
        });

        backButton.setOnClickListener(v -> {
            onBackPressed();
        });

        searchPlaceInput.setOnEditorActionListener((v, actionId, event) -> {
            if(actionId== EditorInfo.IME_ACTION_SEARCH){

                String placeName = searchPlaceInput.getText().toString();
                if(!placeName.isEmpty()){
                    getCoordinatesFromLocationName(placeName);
                }

                searchPlaceInput.clearFocus();
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(searchPlaceInput.getWindowToken(), 0);
                }

                return true;
            }
            return false;
        });

        myLocationButton.setOnClickListener(v -> {
            boolean isSelected = !myLocationButton.isSelected();
            myLocationButton.setSelected(isSelected);

            if (isSelected) {
                myLocationButton.setImageResource(R.drawable.ic_my_location_on);

                // Move the map to the current location on the first fix
                GeoPoint currentLocation = locationOverlay.getMyLocation();

                if (currentLocation != null) {
                    mapView.getController().animateTo(currentLocation);
                    mapView.getController().setZoom(17.0);
                }
            } else {
                myLocationButton.setImageResource(R.drawable.ic_my_location_off);
            }
        });

        backgroundLayout.setOnTouchListener((v, event) -> {
            searchPlaceInput.clearFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(searchPlaceInput.getWindowToken(), 0);
            }

            return false;
        });

    }
    @SuppressLint("UseCompatLoadingForDrawables")
    private void selectMarker(GeoPoint geoPoint) {
        if(marker!=null){
            mapView.getOverlays().remove(marker);
        }

        marker = new Marker(mapView);
        marker.setPosition(geoPoint);
        marker.setTitle("Destination");
        marker.setIcon(getResources().getDrawable(R.drawable.ic_location));
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        mapView.getOverlays().add(marker);

        mapView.invalidate();

    }

    private void showCurrentLocation() {

        locationOverlay = new MyLocationNewOverlay(mapView);
        locationOverlay.enableMyLocation();
        locationOverlay.enableFollowLocation();
        locationOverlay.runOnFirstFix(this::run);
        mapView.getOverlays().add(locationOverlay);
    }

    private void run() {
        // Move the map to the current location on the first fix
        GeoPoint currentLocation = locationOverlay.getMyLocation();

        if (currentLocation != null) {
            this.runOnUiThread(() -> {
                mapView.getController().setCenter(currentLocation);
                mapView.getController().animateTo(currentLocation);
                mapView.getController().setZoom(17.0); // Adjust zoom level as needed
            });
        }
    }

    private void getCoordinatesFromLocationName(String locationName) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            // Fetch address list using location name
            List<Address> addresses = geocoder.getFromLocationName(locationName, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                double latitude = address.getLatitude();
                double longitude = address.getLongitude();

                GeoPoint result = new GeoPoint(latitude, longitude);

                mapView.getController().animateTo(result);
                mapView.getController().setZoom(15.0);

            } else {
                Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

}
