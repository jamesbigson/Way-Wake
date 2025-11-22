package com.example.waywake;

import static android.content.Context.LOCATION_SERVICE;

import static com.example.waywake.AlarmFragment.LOCATION_PERMISSION_REQUEST_CODE;
import static com.example.waywake.AlarmFragment.PERMISSION_REQUEST_CODE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.SettingsClient;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;


public class HomeFragment extends Fragment {
    private MapView mapView;
    private MyLocationNewOverlay locationOverlay;
    private RecyclerView recentAlarms;
    private HistoryAdapter adapter;
    private List<AlarmItem> historyList;
    private SharedPreferences sharedPreferences;
    private SharedPreferences favoriteSP;
    private SharedPreferences.Editor editor;
    private TextView currentLocationName;
    private TextView locationLabel;
    private EditText searchLocationInput;
    private List<FavoriteItem> favoriteList;
    private ChipGroup chipGroup;
    private static final String FAVORITE_PREF_NAME = "favorites_pref";
    private static final String HISTORY_PREF_NAME = "history_pref";
    private static final String KEY_LOCATION_NAME = "current_location";
    private static final String FAVORITES_KEY = "favorites";

    @SuppressLint("ClickableViewAccessibility")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_home, container, false);
        Configuration.getInstance().setUserAgentValue("MyLocationApp/1.0");

        mapView = view.findViewById(R.id.map);
        Button setAlarmButton = view.findViewById(R.id.btnSetAlarm);
        BottomNavigationView bottomNavigationView = requireActivity().findViewById(R.id.bottomNavigationView);
        ConstraintLayout backgroundLayout = view.findViewById(R.id.background_layout);
        chipGroup = view.findViewById(R.id.chip_group);
        recentAlarms = view.findViewById(R.id.rvRecentAlarms);
        currentLocationName = view.findViewById(R.id.tvLocationName);
        locationLabel = view.findViewById(R.id.tvCurrentLocation);
        searchLocationInput = view.findViewById(R.id.search_location);
        sharedPreferences = requireActivity().getSharedPreferences(HISTORY_PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
        favoriteSP = requireActivity().getSharedPreferences(FAVORITE_PREF_NAME, Context.MODE_PRIVATE);

        checkPermissions();


        mapView.setMultiTouchControls(true);
        showCurrentLocation();

        recentAlarms.setLayoutManager(new LinearLayoutManager(getContext()));

        loadHistory();
        createChips();

        adapter = new HistoryAdapter(historyList);
        recentAlarms.setAdapter(adapter);


        setAlarmButton.setOnClickListener(v -> {
            String searchLocation = searchLocationInput.getText().toString();
            if(!searchLocation.isEmpty()){
                FragmentManager fragmentManager = getParentFragmentManager();

                AlarmFragment alarmFragment = (AlarmFragment) fragmentManager.findFragmentByTag("f"+2);

                if(alarmFragment!=null){
                    alarmFragment.setAlarm(searchLocation);
                    bottomNavigationView.setSelectedItemId(R.id.nav_set_alarm);
                }
            }else{
                bottomNavigationView.setSelectedItemId(R.id.nav_set_alarm);
            }
        });


        searchLocationInput.setOnEditorActionListener((v, actionId, event) -> {
            if(actionId== EditorInfo.IME_ACTION_SEARCH){

                searchLocationInput.clearFocus();
                InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(searchLocationInput.getWindowToken(), 0);
                }


                String searchLocation = searchLocationInput.getText().toString();

                if(!searchLocation.isEmpty()){
                    getCoordinatesFromLocationName(searchLocation);
                }
                return true;
            }
            return false;
        });

        backgroundLayout.setOnTouchListener((v, event) -> {
            searchLocationInput.clearFocus();
            InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(searchLocationInput.getWindowToken(), 0);
            }

            return false;
        });

        return view;
    }


    @SuppressLint("SetTextI18n")
    private void getCurrentDistrictName() {
        LocationManager locationManager = (LocationManager) requireActivity().getSystemService(LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        if (location != null) {
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();

            Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    String currentPlace = addresses.get(0).getLocality(); // Get the place name
                    currentLocationName.setText(currentPlace);
                    saveCurrentLocationName(currentPlace);
                } else {
                    Toast.makeText(requireContext(), "Unable to fetch district name", Toast.LENGTH_SHORT).show();
                }
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            String savedPlace = sharedPreferences.getString(KEY_LOCATION_NAME,"");
            locationLabel.setText("Previous Location");
            currentLocationName.setText(savedPlace);
        }
    }

    private void getCoordinatesFromLocationName(String locationName) {
        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
        try {
            // Fetch address list using location name
            List<Address> addresses = geocoder.getFromLocationName(locationName, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                double latitude = address.getLatitude();
                double longitude = address.getLongitude();

                // Display latitude and longitude
                mapView.getController().animateTo(new GeoPoint(latitude,longitude));
//                locationOverlay.disableFollowLocation();

            } else {
                Toast.makeText(requireContext(), "Location not found", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }



    private void showCurrentLocation() {
        mapView.getController().setZoom(17.0);

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
            requireActivity().runOnUiThread(() -> {
//                mapView.getController().setCenter(currentLocation);
                mapView.getController().animateTo(currentLocation);
//                mapView.getController().setZoom(17.0); // Adjust zoom level as needed
            });
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void loadHistory() {
        Set<String> historySet = sharedPreferences.getStringSet("historyList", new HashSet<>());
        historyList = new ArrayList<>();

        adapter = new HistoryAdapter(historyList);
        recentAlarms.setAdapter(adapter);

        if (!historySet.isEmpty()) {
            for (String item : historySet) {
                String[] parts = item.split(";", 2); // Assuming format "location;timestamp"
                String location = parts[0];
                long timestamp = Long.parseLong(parts[1]);
                historyList.add(new AlarmItem(location, timestamp));
                adapter.notifyDataSetChanged();
            }
        } else {
            Log.d("HistoryFragment", "No history to display");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadHistory();
        createChips();
    }

    public void saveCurrentLocationName(String locationName){
        editor.putString(KEY_LOCATION_NAME,locationName);
        editor.apply();
    }

    private void checkPermissions() {

        if (ActivityCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            turnOnGPS();
            getCurrentDistrictName();
        }

        if (ActivityCompat.checkSelfPermission(requireContext(), android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.POST_NOTIFICATIONS}, PERMISSION_REQUEST_CODE);
        }
    }

    private void turnOnGPS() {
        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10000)
                .setFastestInterval(5000);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
        SettingsClient settingsClient = LocationServices.getSettingsClient(requireContext());
        settingsClient.checkLocationSettings(builder.build())
                .addOnSuccessListener(requireActivity(), locationSettingsResponse -> {
                    // GPS is already enabled
                    getCurrentDistrictName();
                })
                .addOnFailureListener(requireActivity(), e -> {
                    // GPS is off, show settings
                    showLocationOn();
                });
    }

    private void showLocationOn() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Enable Location")
                .setMessage("Your location is turned off.Please enable location services to continue.")
                .setPositiveButton("Enable", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadFavorites() {
        Set<String> favoritesSet = favoriteSP.getStringSet(FAVORITES_KEY, new HashSet<>());
        favoriteList = new ArrayList<>();

        if (!favoritesSet.isEmpty()) {
            for (String item : favoritesSet) {
                if(favoriteList.size()<=5){
                    String[] parts = item.split(";", 2); // Assuming format "locationName;locationAddress"
                    String locationName = parts[0];
                    String locationAddress = parts[1];
                    if (!Character.isDigit(locationAddress.charAt(0))) {
                        favoriteList.add(new FavoriteItem(locationName, locationAddress));
                    }
                }else{
                    break;
                }
            }
        } else {
            Log.d("FavoriteFragment", "No Favorites to display");
        }
    }

    private void createChips() {
        loadFavorites();

        chipGroup.removeAllViews();
        if(favoriteList!=null){
            for (FavoriteItem item : favoriteList) {
                Chip chip = new Chip(requireContext());
                chip.setText(item.getLocationName()); // Display only location name
                chip.setClickable(true);
                chip.setChipBackgroundColor(ColorStateList.valueOf(getResources().getColor(R.color.light_purple))); // Customize color
                chip.setTextColor(ColorStateList.valueOf(getResources().getColor(R.color.purple_1000)));

                // Set Click Listener
                chip.setOnClickListener(v -> {
                    String locationAddress = item.getLocationAddress();
                    if(!locationAddress.isEmpty()){
                        BottomNavigationView bottomNavigationView = requireActivity().findViewById(R.id.bottomNavigationView);
                        FragmentManager fragmentManager = getParentFragmentManager();

                        AlarmFragment alarmFragment = (AlarmFragment) fragmentManager.findFragmentByTag("f" + 2);

                        if (alarmFragment != null) {
                            alarmFragment.setAlarmText(locationAddress);
                            bottomNavigationView.setSelectedItemId(R.id.nav_set_alarm);
                        }
                    }
                });

                chipGroup.addView(chip);
            }
        }

    }


}
