package com.example.waywake;

import static android.content.Context.LOCATION_SERVICE;
import static com.example.waywake.AlarmFragment.LOCATION_PERMISSION_REQUEST_CODE;
import static com.example.waywake.AlarmFragment.PERMISSION_REQUEST_CODE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import java.util.Calendar;
import java.util.Collections;
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
    private SharedPreferences activeAlarmSP;
    private SharedPreferences.Editor editor;
    private TextView currentLocationName;
    private TextView locationLabel;
    private EditText searchLocationInput;
    private List<FavoriteItem> favoriteList;
    private ChipGroup chipGroup;

    private View favRow1;
    private View favRow2;
    private LinearLayout favCard1;
    private LinearLayout favCard2;
    private LinearLayout favCard3;
    private LinearLayout favCard4;
    private ImageView favIcon1;
    private ImageView favIcon2;
    private ImageView favIcon3;
    private ImageView favIcon4;
    private TextView favText1;
    private TextView favText2;
    private TextView favText3;
    private TextView favText4;

    // Active & Inactive Alarm Card Views
    private View cardNoActiveAlarm;
    private View cardActiveAlarm;
    private TextView tvActiveDestName;
    private TextView tvActiveDestType;
    private TextView tvActiveRemaining;
    private TextView tvActiveEta;
    private TextView tvActiveRadius;
    private Button btnStopActiveAlarm;
    private TextView tvGreeting;

    // Recent Activity Card Views
    private TextView tvRecentDestName;
    private TextView tvRecentTime;
    private View btnViewHistory;

    private static final String FAVORITE_PREF_NAME = "favorites_pref";
    private static final String HISTORY_PREF_NAME = "history_pref";
    private static final String ACTIVE_ALARM_PREF_NAME = "active_alarm_pref";
    private static final String KEY_LOCATION_NAME = "current_location";
    private static final String FAVORITES_KEY = "favorites";

    private final BroadcastReceiver updateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null) return;
            if (ForegroundService.BROADCAST_LOCATION_UPDATE.equals(intent.getAction()) ||
                ForegroundService.BROADCAST_ALARM_TRIGGERED.equals(intent.getAction()) ||
                "STOP_ALARM_EVENT".equals(intent.getAction())) {
                updateActiveAlarmCard();
            }
        }
    };

    @SuppressLint({"ClickableViewAccessibility", "SetTextI18n"})
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_home, container, false);
        Configuration.getInstance().setUserAgentValue("WayWake/1.0 (jamesbigson/Way-Wake)");

        mapView = view.findViewById(R.id.map);
        Button setAlarmButton = view.findViewById(R.id.btnSetAlarm);
        BottomNavigationView bottomNavigationView = requireActivity().findViewById(R.id.bottomNavigationView);
        View backgroundLayout = view.findViewById(R.id.background_layout);
        chipGroup = view.findViewById(R.id.chip_group);
        recentAlarms = view.findViewById(R.id.rvRecentAlarms);
        currentLocationName = view.findViewById(R.id.tvLocationName);
        locationLabel = view.findViewById(R.id.tvCurrentLocation);
        searchLocationInput = view.findViewById(R.id.search_location);

        // Header Greeting
        tvGreeting = view.findViewById(R.id.tv_greeting);
        updateGreeting();

        // Cards Binding
        cardNoActiveAlarm = view.findViewById(R.id.card_no_active_alarm);
        cardActiveAlarm = view.findViewById(R.id.card_active_alarm);
        tvActiveDestName = view.findViewById(R.id.tv_active_dest_name);
        tvActiveDestType = view.findViewById(R.id.tv_active_dest_type);
        tvActiveRemaining = view.findViewById(R.id.tv_active_remaining);
        tvActiveEta = view.findViewById(R.id.tv_active_eta);
        tvActiveRadius = view.findViewById(R.id.tv_active_radius);
        btnStopActiveAlarm = view.findViewById(R.id.btn_stop_active_alarm);

        // Recent Activity Binding
        tvRecentDestName = view.findViewById(R.id.tv_recent_dest_name);
        tvRecentTime = view.findViewById(R.id.tv_recent_time);
        btnViewHistory = view.findViewById(R.id.btn_view_history);

        // SharedPreferences
        sharedPreferences = requireActivity().getSharedPreferences(HISTORY_PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
        favoriteSP = requireActivity().getSharedPreferences(FAVORITE_PREF_NAME, Context.MODE_PRIVATE);
        activeAlarmSP = requireActivity().getSharedPreferences(ACTIVE_ALARM_PREF_NAME, Context.MODE_PRIVATE);

        checkPermissions();

        if (mapView != null) {
            mapView.setMultiTouchControls(true);
            showCurrentLocation();
        }

        if (recentAlarms != null) {
            recentAlarms.setLayoutManager(new LinearLayoutManager(getContext()));
        }

        loadHistory();
        createChips();

        if (recentAlarms != null && historyList != null) {
            adapter = new HistoryAdapter(historyList);
            recentAlarms.setAdapter(adapter);
        }

        // Set New Alarm Button Action
        if (setAlarmButton != null) {
            setAlarmButton.setOnClickListener(v -> {
                bottomNavigationView.setSelectedItemId(R.id.nav_set_alarm);
            });
        }

        // Stop Active Alarm Button Action
        if (btnStopActiveAlarm != null) {
            btnStopActiveAlarm.setOnClickListener(v -> {
                stopActiveAlarm();
            });
        }

        // Setup Quick Favorites Click Handlers
        setupQuickFavorites(view, bottomNavigationView);

        // View History Action
        if (btnViewHistory != null) {
            btnViewHistory.setOnClickListener(v -> {
                bottomNavigationView.setSelectedItemId(R.id.nav_history);
            });
        }

        if (searchLocationInput != null) {
            searchLocationInput.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    searchLocationInput.clearFocus();
                    InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(searchLocationInput.getWindowToken(), 0);
                    }
                    String searchLocation = searchLocationInput.getText().toString();
                    if (!searchLocation.isEmpty()) {
                        getCoordinatesFromLocationName(searchLocation);
                    }
                    return true;
                }
                return false;
            });
        }

        updateActiveAlarmCard();
        updateRecentActivityCard();

        return view;
    }

    private void updateGreeting() {
        if (tvGreeting == null) return;
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour >= 5 && hour < 12) {
            tvGreeting.setText("Good Morning 👋");
        } else if (hour >= 12 && hour < 17) {
            tvGreeting.setText("Good Afternoon 👋");
        } else {
            tvGreeting.setText("Good Evening 👋");
        }
    }

    private void setupQuickFavorites(View view, BottomNavigationView bottomNavigationView) {
        favRow1 = view.findViewById(R.id.fav_row1);
        favRow2 = view.findViewById(R.id.fav_row2);

        favCard1 = view.findViewById(R.id.fav_card_home);
        favCard2 = view.findViewById(R.id.fav_card_college);
        favCard3 = view.findViewById(R.id.fav_card_railway);
        favCard4 = view.findViewById(R.id.fav_card_add_new);

        favIcon1 = view.findViewById(R.id.fav_card_1_icon);
        favIcon2 = view.findViewById(R.id.fav_card_2_icon);
        favIcon3 = view.findViewById(R.id.fav_card_3_icon);
        favIcon4 = view.findViewById(R.id.fav_card_4_icon);

        favText1 = view.findViewById(R.id.fav_card_1_text);
        favText2 = view.findViewById(R.id.fav_card_2_text);
        favText3 = view.findViewById(R.id.fav_card_3_text);
        favText4 = view.findViewById(R.id.fav_card_4_text);

        View seeAll = view.findViewById(R.id.btn_see_all_favorites);
        if (seeAll != null) {
            seeAll.setOnClickListener(v -> bottomNavigationView.setSelectedItemId(R.id.nav_favorites));
        }

        updateFavoritesGrid();
    }

    private void triggerFavoriteAlarm(FavoriteItem fav, BottomNavigationView bottomNavigationView) {
        String address = fav.getLocationAddress();
        FragmentManager fragmentManager = getParentFragmentManager();
        AlarmFragment alarmFragment = (AlarmFragment) fragmentManager.findFragmentByTag("f" + 2);
        if (alarmFragment != null) {
            if (address != null && !address.isEmpty() && Character.isDigit(address.charAt(0))) {
                try {
                    String[] parts = address.split(";", 2);
                    double lat = Double.parseDouble(parts[0]);
                    double lon = Double.parseDouble(parts[1]);
                    alarmFragment.setDestination(lat, lon);
                } catch (Exception e) {
                    alarmFragment.setAlarmText(address);
                }
            } else {
                alarmFragment.setAlarm(address);
            }
            bottomNavigationView.setSelectedItemId(R.id.nav_set_alarm);
        } else {
            bottomNavigationView.setSelectedItemId(R.id.nav_set_alarm);
        }
    }

    public static String[] parseLocationHierarchy(String rawLocation) {
        if (rawLocation == null || rawLocation.trim().isEmpty()) {
            return new String[]{"Selected Destination", "Final Destination"};
        }
        rawLocation = rawLocation.trim();

        String major = rawLocation;
        String minor = "";

        if (rawLocation.contains(",")) {
            String[] parts = rawLocation.split(",", 2);
            major = parts[0].trim();
            minor = parts[1].trim();
        }

        // Check if major contains landmark keyword followed by extra sub-locality text
        String[] landmarks = {"Railway Station", "Train Station", "Bus Stand", "Bus Stop", "Bus Terminal", "Airport", "Junction", "College", "University", "Hospital", "Mall", "Beach", "Park", "Temple"};
        for (String landmark : landmarks) {
            int idx = major.toLowerCase().indexOf(landmark.toLowerCase());
            if (idx != -1) {
                int endIdx = idx + landmark.length();
                if (endIdx < major.length()) {
                    String extra = major.substring(endIdx).trim();
                    major = major.substring(0, endIdx).trim();
                    if (!extra.isEmpty()) {
                        minor = minor.isEmpty() ? extra : extra + ", " + minor;
                    }
                }
                break;
            }
        }

        if (minor.isEmpty()) {
            minor = "Final Destination";
        }

        return new String[]{major, minor};
    }

    public void updateActiveAlarmCard() {
        if (cardActiveAlarm == null || cardNoActiveAlarm == null) return;

        boolean isActive = activeAlarmSP.getBoolean("is_active", false);
        if (isActive) {
            cardActiveAlarm.setVisibility(View.VISIBLE);
            cardNoActiveAlarm.setVisibility(View.GONE);

            String destName = activeAlarmSP.getString("dest_name", "Selected Destination");
            int radiusMeters = activeAlarmSP.getInt("dest_radius", 5000);
            String remaining = activeAlarmSP.getString("remaining_distance", "Calculating...");

            String[] hierarchy = parseLocationHierarchy(destName);
            if (tvActiveDestName != null) tvActiveDestName.setText(hierarchy[0]);
            if (tvActiveDestType != null) tvActiveDestType.setText(hierarchy[1]);
            if (tvActiveRemaining != null) tvActiveRemaining.setText(remaining);

            // Radius formatting
            String radiusStr;
            if (radiusMeters >= 1000) {
                radiusStr = String.format(Locale.US, "%d km", radiusMeters / 1000);
            } else {
                radiusStr = String.format(Locale.US, "%d m", radiusMeters);
            }
            if (tvActiveRadius != null) tvActiveRadius.setText(radiusStr);

            // Estimated Time of Arrival (ETA) calculation
            String etaStr = "15 mins";
            if (!remaining.equals("Calculating...")) {
                try {
                    String cleanRem = remaining.replaceAll("[^0-9.]", "");
                    if (!cleanRem.isEmpty()) {
                        double km = Double.parseDouble(cleanRem);
                        if (remaining.contains("m") && !remaining.contains("km")) {
                            km = km / 1000.0;
                        }
                        int mins = Math.max(1, (int) Math.round((km / 35.0) * 60));
                        etaStr = mins + " mins";
                    }
                } catch (Exception ignored) {}
            }
            if (tvActiveEta != null) tvActiveEta.setText(etaStr);

        } else {
            cardActiveAlarm.setVisibility(View.GONE);
            cardNoActiveAlarm.setVisibility(View.VISIBLE);
        }
    }

    private void stopActiveAlarm() {
        if (getActivity() instanceof MainPage) {
            ((MainPage) getActivity()).stopAlarm();
        } else {
            Intent intent = new Intent(requireContext(), ForegroundService.class);
            intent.setAction(ForegroundService.ACTION_STOP_ALARM);
            requireContext().startService(intent);
        }

        activeAlarmSP.edit().putBoolean("is_active", false).apply();
        updateActiveAlarmCard();
        Toast.makeText(requireContext(), "Alarm stopped", Toast.LENGTH_SHORT).show();
    }

    private void updateRecentActivityCard() {
        if (tvRecentDestName == null || tvRecentTime == null) return;

        if (historyList != null && !historyList.isEmpty()) {
            AlarmItem item = historyList.get(0);
            String[] hierarchy = parseLocationHierarchy(item.getLocation());
            tvRecentDestName.setText("Last Alarm – " + hierarchy[0]);

            long timeMillis = item.getTimestamp();
            if (timeMillis > 0) {
                Calendar current = Calendar.getInstance();
                Calendar target = Calendar.getInstance();
                target.setTimeInMillis(timeMillis);

                String formattedTime = DateFormat.format("h:mm a", target).toString().toUpperCase();

                if (current.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
                    current.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)) {
                    tvRecentTime.setText("Today • " + formattedTime);
                } else if (current.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
                           current.get(Calendar.DAY_OF_YEAR) - target.get(Calendar.DAY_OF_YEAR) == 1) {
                    tvRecentTime.setText("Yesterday • " + formattedTime);
                } else {
                    String formattedDate = DateFormat.format("MMM d", target).toString();
                    tvRecentTime.setText(formattedDate + " • " + formattedTime);
                }
            } else {
                tvRecentTime.setText("Recently completed");
            }
        } else {
            tvRecentDestName.setText("No Recent Alarms");
            tvRecentTime.setText("Your trip history will appear here.");
        }
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

            editor.putFloat("current_latitude", (float) latitude);
            editor.putFloat("current_longitude", (float) longitude);
            editor.apply();

            Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    String currentPlace = addresses.get(0).getLocality();
                    if (currentPlace == null || currentPlace.isEmpty()) {
                        currentPlace = addresses.get(0).getAdminArea();
                    }
                    if (currentLocationName != null) {
                        currentLocationName.setText(currentPlace != null ? currentPlace : "Current Location");
                    }
                    saveCurrentLocationName(currentPlace != null ? currentPlace : "");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            String savedPlace = sharedPreferences.getString(KEY_LOCATION_NAME, "");
            if (locationLabel != null) locationLabel.setText("Previous Location");
            if (currentLocationName != null && !savedPlace.isEmpty()) currentLocationName.setText(savedPlace);
        }
    }

    private void getCoordinatesFromLocationName(String locationName) {
        if (mapView == null) return;
        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocationName(locationName, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                double latitude = address.getLatitude();
                double longitude = address.getLongitude();
                mapView.getController().animateTo(new GeoPoint(latitude, longitude));
            } else {
                Toast.makeText(requireContext(), "Location not found", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showCurrentLocation() {
        if (mapView == null) return;
        mapView.getController().setZoom(17.0);
        locationOverlay = new MyLocationNewOverlay(mapView);
        locationOverlay.enableMyLocation();
        locationOverlay.enableFollowLocation();
        locationOverlay.runOnFirstFix(this::run);
        mapView.getOverlays().add(locationOverlay);
    }

    private void run() {
        if (locationOverlay == null || mapView == null) return;
        GeoPoint currentLocation = locationOverlay.getMyLocation();
        if (currentLocation != null) {
            double latitude = currentLocation.getLatitude();
            double longitude = currentLocation.getLongitude();

            editor.putFloat("current_latitude", (float) latitude);
            editor.putFloat("current_longitude", (float) longitude);
            editor.apply();

            requireActivity().runOnUiThread(() -> {
                mapView.getController().animateTo(currentLocation);
            });
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void loadHistory() {
        Set<String> historySet = sharedPreferences.getStringSet("historyList", new HashSet<>());
        historyList = new ArrayList<>();

        if (recentAlarms != null) {
            adapter = new HistoryAdapter(historyList);
            recentAlarms.setAdapter(adapter);
        }

        if (historySet != null && !historySet.isEmpty()) {
            List<String> sortedHistory = new ArrayList<>(historySet);
            Collections.sort(sortedHistory, (a, b) -> {
                try {
                    String[] partsA = a.split(";", 2);
                    String[] partsB = b.split(";", 2);
                    if (partsA.length < 2 || partsB.length < 2) return 0;
                    long t1 = Long.parseLong(partsA[1]);
                    long t2 = Long.parseLong(partsB[1]);
                    return Long.compare(t2, t1);
                } catch (Exception e) {
                    return 0;
                }
            });

            Set<String> seenLocations = new HashSet<>();
            for (String item : sortedHistory) {
                String[] parts = item.split(";", 2);
                if (parts.length >= 2) {
                    String location = parts[0].trim();
                    if (!location.isEmpty() && !seenLocations.contains(location.toLowerCase())) {
                        seenLocations.add(location.toLowerCase());
                        try {
                            long timestamp = Long.parseLong(parts[1]);
                            historyList.add(new AlarmItem(location, timestamp));
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
            if (adapter != null) adapter.notifyDataSetChanged();
        }
        updateRecentActivityCard();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadHistory();
        createChips();
        updateFavoritesGrid();
        updateGreeting();
        updateActiveAlarmCard();
        updateRecentActivityCard();

        IntentFilter filter = new IntentFilter();
        filter.addAction(ForegroundService.BROADCAST_LOCATION_UPDATE);
        filter.addAction(ForegroundService.BROADCAST_ALARM_TRIGGERED);
        filter.addAction("STOP_ALARM_EVENT");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireContext().registerReceiver(updateReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            requireContext().registerReceiver(updateReceiver, filter);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            requireContext().unregisterReceiver(updateReceiver);
        } catch (Exception ignored) {}
    }

    public void saveCurrentLocationName(String locationName) {
        editor.putString(KEY_LOCATION_NAME, locationName);
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
                    getCurrentDistrictName();
                })
                .addOnFailureListener(requireActivity(), e -> {
                    showLocationOn();
                });
    }

    private void showLocationOn() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Enable Location")
                .setMessage("Your location is turned off. Please enable location services to continue.")
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

        if (favoritesSet != null && !favoritesSet.isEmpty()) {
            for (String item : favoritesSet) {
                String[] parts = item.split(";", 2);
                if (parts.length >= 2) {
                    String locationName = parts[0];
                    String locationAddress = parts[1];
                    favoriteList.add(new FavoriteItem(locationName, locationAddress));
                }
            }
        }
    }

    private void updateFavoritesGrid() {
        loadFavorites();
        int numFavs = (favoriteList != null) ? favoriteList.size() : 0;

        if (numFavs == 0) {
            bindCardAsAddNew(favCard1, favIcon1, favText1);
            if (favCard1 != null) favCard1.setVisibility(View.VISIBLE);
            if (favCard2 != null) favCard2.setVisibility(View.INVISIBLE);
            if (favRow1 != null) favRow1.setVisibility(View.VISIBLE);
            if (favRow2 != null) favRow2.setVisibility(View.GONE);
        } else if (numFavs == 1) {
            bindCardAsFavorite(favCard1, favIcon1, favText1, favoriteList.get(0));
            if (favCard1 != null) favCard1.setVisibility(View.VISIBLE);

            bindCardAsAddNew(favCard2, favIcon2, favText2);
            if (favCard2 != null) favCard2.setVisibility(View.VISIBLE);

            if (favRow1 != null) favRow1.setVisibility(View.VISIBLE);
            if (favRow2 != null) favRow2.setVisibility(View.GONE);
        } else if (numFavs == 2) {
            bindCardAsFavorite(favCard1, favIcon1, favText1, favoriteList.get(0));
            if (favCard1 != null) favCard1.setVisibility(View.VISIBLE);

            bindCardAsFavorite(favCard2, favIcon2, favText2, favoriteList.get(1));
            if (favCard2 != null) favCard2.setVisibility(View.VISIBLE);

            bindCardAsAddNew(favCard3, favIcon3, favText3);
            if (favCard3 != null) favCard3.setVisibility(View.VISIBLE);

            if (favCard4 != null) favCard4.setVisibility(View.INVISIBLE);

            if (favRow1 != null) favRow1.setVisibility(View.VISIBLE);
            if (favRow2 != null) favRow2.setVisibility(View.VISIBLE);
        } else if (numFavs == 3) {
            bindCardAsFavorite(favCard1, favIcon1, favText1, favoriteList.get(0));
            if (favCard1 != null) favCard1.setVisibility(View.VISIBLE);

            bindCardAsFavorite(favCard2, favIcon2, favText2, favoriteList.get(1));
            if (favCard2 != null) favCard2.setVisibility(View.VISIBLE);

            bindCardAsFavorite(favCard3, favIcon3, favText3, favoriteList.get(2));
            if (favCard3 != null) favCard3.setVisibility(View.VISIBLE);

            bindCardAsAddNew(favCard4, favIcon4, favText4);
            if (favCard4 != null) favCard4.setVisibility(View.VISIBLE);

            if (favRow1 != null) favRow1.setVisibility(View.VISIBLE);
            if (favRow2 != null) favRow2.setVisibility(View.VISIBLE);
        } else {
            bindCardAsFavorite(favCard1, favIcon1, favText1, favoriteList.get(0));
            if (favCard1 != null) favCard1.setVisibility(View.VISIBLE);

            bindCardAsFavorite(favCard2, favIcon2, favText2, favoriteList.get(1));
            if (favCard2 != null) favCard2.setVisibility(View.VISIBLE);

            bindCardAsFavorite(favCard3, favIcon3, favText3, favoriteList.get(2));
            if (favCard3 != null) favCard3.setVisibility(View.VISIBLE);

            if (favCard4 != null) favCard4.setVisibility(View.INVISIBLE);

            if (favRow1 != null) favRow1.setVisibility(View.VISIBLE);
            if (favRow2 != null) favRow2.setVisibility(View.VISIBLE);
        }
    }

    private void bindCardAsFavorite(LinearLayout cardView, ImageView iconView, TextView textView, FavoriteItem favorite) {
        if (cardView == null) return;

        cardView.setBackgroundResource(R.drawable.bg_card_surface);
        cardView.setGravity(Gravity.CENTER_VERTICAL);

        String name = favorite.getLocationName();
        String nameLower = name.toLowerCase();
        int iconRes;
        if (nameLower.contains("home")) {
            iconRes = R.drawable.ic_home_purple;
        } else if (nameLower.contains("college") || nameLower.contains("school") || nameLower.contains("uni") || nameLower.contains("class")) {
            iconRes = R.drawable.ic_college;
        } else if (nameLower.contains("station") || nameLower.contains("train") || nameLower.contains("railway") || nameLower.contains("metro") || nameLower.contains("bus")) {
            iconRes = R.drawable.ic_train;
        } else {
            iconRes = R.drawable.ic_location;
        }

        if (iconView != null) {
            iconView.setImageResource(iconRes);
        }

        if (textView != null) {
            textView.setText(name);
            textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_dark));
        }

        cardView.setOnClickListener(v -> {
            BottomNavigationView bottomNavigationView = requireActivity().findViewById(R.id.bottomNavigationView);
            triggerFavoriteAlarm(favorite, bottomNavigationView);
        });
    }

    private void bindCardAsAddNew(LinearLayout cardView, ImageView iconView, TextView textView) {
        if (cardView == null) return;

        cardView.setBackgroundResource(R.drawable.bg_card_dashed);
        cardView.setGravity(Gravity.CENTER);

        if (iconView != null) {
            iconView.setImageResource(R.drawable.ic_add_purple);
        }

        if (textView != null) {
            textView.setText("Add New");
            textView.setTextColor(android.graphics.Color.parseColor("#6A00FF"));
        }

        cardView.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), AddFavoriteActivity.class);
            startActivity(intent);
        });
    }

    private void createChips() {
        loadFavorites();
        if (chipGroup == null) return;
        chipGroup.removeAllViews();
        if (favoriteList != null) {
            for (FavoriteItem item : favoriteList) {
                Chip chip = new Chip(requireContext());
                chip.setText(item.getLocationName());
                chip.setClickable(true);
                chip.setChipBackgroundColor(ColorStateList.valueOf(getResources().getColor(R.color.light_purple)));
                chip.setTextColor(ColorStateList.valueOf(getResources().getColor(R.color.purple_1000)));

                chip.setOnClickListener(v -> {
                    String locationAddress = item.getLocationAddress();
                    if (!locationAddress.isEmpty()) {
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
