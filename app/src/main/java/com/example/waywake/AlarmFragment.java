package com.example.waywake;

import static android.app.Activity.RESULT_OK;
import static android.content.Context.MODE_PRIVATE;
import static android.content.Context.NOTIFICATION_SERVICE;
import static android.content.Context.VIBRATOR_SERVICE;

import static com.example.waywake.AlarmSettings.KEY_ALARM_SOUND;
import static com.example.waywake.AlarmSettings.KEY_DISTANCE_UNIT;
import static com.example.waywake.AlarmSettings.KEY_VIBRATION;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;

import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.media.MediaPlayer;
import android.net.Uri;

import android.os.Build;
import android.os.Bundle;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;

import org.osmdroid.config.Configuration;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import org.osmdroid.views.overlay.Marker;

import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import android.os.Vibrator;
import android.view.View;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.provider.Settings;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

public class AlarmFragment extends Fragment {

    private MapView mapView;
    private GeoPoint destination;
    private EditText locationInput;
    private Marker marker;
    private Polygon circle;
    private MyLocationNewOverlay locationOverlay;
    private SharedPreferences sharedPreferences, userSettingsSP;
    private SharedPreferences.Editor editor;
    private TextView statusText;
    private String distanceUnit;
    private TextView radiusLabel;
    Button setAlarmButton;
    private Vibrator vibrator;
    private MediaPlayer mediaPlayer;
    private int destRadius = 5000;
    private boolean isFirstAlarm = true;
//    private AutoCompleteTextView autoCompleteTextView;
    private boolean isMonitorRunning = false;
    public static final int PERMISSION_REQUEST_CODE = 1;
    private static final String CHANNEL_ID = "location_alarm_channel";
    private boolean isShowingLocation = false;
    private boolean isAlarmRinging = false;
    private AlertDialog alarmDialog;
    private boolean showMyLocation = true;
    private CompassOverlay compassOverlay;
    ActivityResultLauncher<Intent> searchLauncher;
    private static final String HISTORY_PREF_NAME = "history_pref";
    public static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    public static final String USER_SETTINGS_PREFS_NAME = "user_settings";


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        registerStopAlarmReceiver();
    }

    private void registerStopAlarmReceiver() {
        IntentFilter filter = new IntentFilter("STOP_ALARM_EVENT");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireContext().registerReceiver(stopAlarmReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            requireContext().registerReceiver(stopAlarmReceiver, filter);
        }
    }

    private final android.content.BroadcastReceiver stopAlarmReceiver = new android.content.BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("AlarmFragment", "Broadcast received: STOP_ALARM_EVENT in instance " + AlarmFragment.this.hashCode());
            stopAlarm();
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.cancel(2);
            }
        }
    };

    @SuppressLint({"ClickableViewAccessibility", "SetTextI18n"})
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_alarm, container, false);

        Configuration.getInstance().setUserAgentValue("MyLocationApp/1.0");

        mapView = view.findViewById(R.id.map);
        locationInput = view.findViewById(R.id.location_input);
        statusText = view.findViewById(R.id.status_text);
        setAlarmButton = view.findViewById(R.id.set_alarm_button);
        radiusLabel = view.findViewById(R.id.radius_label);
        vibrator = (Vibrator) requireContext().getSystemService(VIBRATOR_SERVICE);

        SeekBar radiusSeekBar = view.findViewById(R.id.radius_seekbar);
        ImageButton myLocationButton = view.findViewById(R.id.my_location_button);
        ImageButton mapLocationButton = view.findViewById(R.id.map_location_button);
        ImageButton threeDotMenu = view.findViewById(R.id.more_menu);
        ImageView clearTextButton = view.findViewById(R.id.clear_text);
        ConstraintLayout backgroundLayout = view.findViewById(R.id.background_layout);

        // Initialize SharedPreferences
        sharedPreferences = requireActivity().getSharedPreferences(HISTORY_PREF_NAME, MODE_PRIVATE);
        editor = sharedPreferences.edit();
        userSettingsSP = requireActivity().getSharedPreferences(USER_SETTINGS_PREFS_NAME, MODE_PRIVATE);
        distanceUnit = userSettingsSP.getString(KEY_DISTANCE_UNIT, "Meter" );

        mapView.setMultiTouchControls(true);
        myLocationButton.setSelected(true);

        try {
            checkPermissions();
        } catch (Exception e) {
            //Exception
            Log.d("Permission","Permission Error");
        }

        if(distanceUnit.equals("Kilometer")){
            radiusLabel.setText("20 Kilometer");
            destRadius = 20000;
        }

        // Start the foreground service for background tracking
        startLocationService();
        createNotificationChannel();

//        testBtn.setOnClickListener(v -> {
//            Intent i = new Intent(requireContext(),Search_page.class);
//            startActivity(i);
//        });

        myLocationButton.setOnClickListener(v -> {
            boolean isSelected = !myLocationButton.isSelected();
            showMyLocation = isSelected;
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

        mapView.setOnTouchListener((v, event) -> {
            v.getParent().requestDisallowInterceptTouchEvent(true);

            if (myLocationButton.isSelected()) {
                myLocationButton.callOnClick();
            }
            return false;
        });

        setAlarmButton.setOnClickListener(v -> {

            locationInput.clearFocus();
            InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(locationInput.getWindowToken(), 0);
            }


            if (myLocationButton.isSelected()) {
                myLocationButton.callOnClick();
            }

            locationOverlay.disableFollowLocation();

//            mapView.callOnClick();


            String locationName = locationInput.getText().toString();

            addHistory(locationName);

            if(locationName.equals("Selected destination")){
                userSelectedLocation(destination);
            }else{

                if (!locationName.isEmpty()) {
                    getCoordinatesFromLocationName(locationName);
                } else {
                    Toast.makeText(requireContext(), "Please enter a location name", Toast.LENGTH_SHORT).show();
                }
            }
        });

        radiusSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int increment = 500;
                if(distanceUnit.equals("Kilometer")){
                    increment = 2;
                }
                radiusLabel.setText(progress * increment + " " + distanceUnit );
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Optional: Handle when the user starts dragging the slider
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Update the circle radius once the user stops adjusting
                int radius = seekBar.getProgress();
                if(distanceUnit.equals("Kilometer")){
                    radius *= 2000;
                }else{
                    radius *= 500;
                }

                destRadius = radius;
                if (destination != null) {
                    mapView.invalidate();
                    addMarkerWithCircle(destination, "Destination", radius);
                }

                if (!isFirstAlarm && !isMonitorRunning) {
                    startAlarmMonitor();
                }
            }
        });

        mapLocationButton.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), SelectMapLocation.class);
            startActivityForResult(intent, 200);
        });


        // Register launcher
        searchLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            String place = data.getStringExtra("selected_place");
                            locationInput.setText(place);   // Set the returned text
                            setAlarmButton.callOnClick();
                        }
                    }
                }
        );

        // Open SearchActivity when clicked
        locationInput.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), Search_page.class);
            intent.putExtra("LOCATION_INPUT", locationInput.getText().toString());
            searchLauncher.launch(intent);
        });

        locationInput.setOnEditorActionListener((v, actionId, event) -> {
            if(actionId== EditorInfo.IME_ACTION_DONE){
                setAlarmButton.callOnClick();
                return true;
            }
            return false;
        });

        locationInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(locationInput.getText().toString().isEmpty()){
                    clearTextButton.setVisibility(View.GONE);
                }else{
                    clearTextButton.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        threeDotMenu.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(requireActivity(), v);
            MenuInflater menuInflater = popupMenu.getMenuInflater();
            menuInflater.inflate(R.menu.more_menu, popupMenu.getMenu());

            // Handle menu item clicks
            popupMenu.setOnMenuItemClickListener(item -> {
                if(item.getItemId()==R.id.add_favorite){
                    addFavorite();
                    return true;
                }
                else if (item.getItemId()==R.id.reload) {
                    if (!isShowingLocation) showCurrentLocation();
                    return true;
                }
                else if (item.getItemId()==R.id.stop_alarm) {
                    destination = null;
                    locationInput.setText("");
                    return true;
                } else{
                    return false;
                }
            });

            popupMenu.show();
        });

        clearTextButton.setOnClickListener(v -> {
            locationInput.setText("");
        });

        backgroundLayout.setOnTouchListener((v, event) -> {
            locationInput.clearFocus();
            InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(locationInput.getWindowToken(), 0);
            }
            return false;
        });


//        autoCompleteTextView.addTextChangedListener(new TextWatcher() {
//            @Override
//            public void onTextChanged(CharSequence s, int start, int before, int count) {
//                if (s.length() > 0) {
//                    fetchSuggestions(s.toString());
//                }
//            }
//
//            @Override
//            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
//            }
//
//            @Override
//            public void afterTextChanged(Editable s) {
//            }
//        });
//
//        autoCompleteTextView.setOnItemClickListener((parent, view1, position, id) -> {
//            String selectedPlace = parent.getItemAtPosition(position).toString();
//            Toast.makeText(requireContext(), "Selected: " + selectedPlace, Toast.LENGTH_SHORT).show();
//        });

        return view;
    }

    private void checkPermissions() {

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            turnOnGPS();
            setupMap();
            showCurrentLocation();
            setupCompass();
        }

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
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
                .addOnSuccessListener(requireActivity(), new OnSuccessListener<LocationSettingsResponse>() {
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                        // GPS is already enabled
                    }
                })
                .addOnFailureListener(requireActivity(), new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // GPS is off, show settings
                        showLocationOn();
                    }
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

    private void setupMap() {
        mapView.getController().setCenter(new GeoPoint(20.5937, 78.9629)); //india coordinates
        mapView.getController().animateTo(new GeoPoint(20.5937, 78.9629));
        mapView.getController().setZoom(5.0);
    }

    private void setupCompass() {
        // Create and configure the CompassOverlay
        compassOverlay = new CompassOverlay(requireContext(), mapView);
        compassOverlay.enableCompass();
        mapView.getOverlays().add(compassOverlay);
        mapView.invalidate();
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onResume() {
        super.onResume();

        if(!isMonitorRunning){
            distanceUnit = userSettingsSP.getString(KEY_DISTANCE_UNIT, "Meter" );

            if (distanceUnit.equals("Kilometer")) {
                if(destRadius ==5000){
                    radiusLabel.setText("20 Kilometer");
                }
            } else {
                if(destRadius ==20000){
                    radiusLabel.setText("5000 Meter");
                }
            }
        }

        if (mapView != null) {
            if (compassOverlay != null) compassOverlay.enableCompass();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) {
            if (compassOverlay != null) compassOverlay.enableCompass();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("AlarmFragment", "onDestroy() called for instance " + this.hashCode());
        stopAlarm();
        isMonitorRunning = false;
        try {
            requireContext().unregisterReceiver(stopAlarmReceiver);
        } catch (Exception e) {
            Log.e("AlarmFragment", "Error unregistering receiver", e);
        }
        if (mapView != null) {
            mapView.onDetach();
        }
    }

    private void triggerAlarm() {
        // Trigger the alarm actions in ForegroundService
        Intent intent = new Intent(requireContext(), ForegroundService.class);
        intent.setAction(ForegroundService.ACTION_START_ALARM);
        
        // Pass user settings to service
        String alarmSound = userSettingsSP.getString(KEY_ALARM_SOUND, "chiptune");
        intent.putExtra("alarm_sound", alarmSound);
        boolean vibrationEnabled = userSettingsSP.getBoolean(KEY_VIBRATION, true);
        intent.putExtra("vibration_enabled", vibrationEnabled);

        requireContext().startService(intent);

        if(!isAlarmRinging){
            isAlarmRinging = true;
            showPopup();
        }
    }

    void stopAlarm(){
        Log.d("AlarmFragment", "stopAlarm() called for instance " + this.hashCode() + ", isAlarmRinging: " + isAlarmRinging);
        
        // Stop the alarm actions in ForegroundService
        Intent intent = new Intent(requireContext(), ForegroundService.class);
        intent.setAction(ForegroundService.ACTION_STOP_ALARM);
        requireContext().startService(intent);

        isAlarmRinging = false;

        if (alarmDialog != null && alarmDialog.isShowing()) {
            alarmDialog.dismiss();
            alarmDialog = null;
        }
    }

    private void showPopup() {

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext(), R.style.TransparentDialogTheme);
        builder.setCancelable(false);
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_alarm_ringing, null);
        builder.setView(dialogView);

        if (alarmDialog != null && alarmDialog.isShowing()) {
            alarmDialog.dismiss();
        }
        alarmDialog = builder.create();
        alarmDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);


        dialogView.findViewById(R.id.stop_button).setOnClickListener(v -> {
            stopAlarm();
        });

        dialogView.findViewById(R.id.iv_close).setOnClickListener(v -> {
            stopAlarm();
        });

        alarmDialog.show();

    }

    private void showCurrentLocation() {

        locationOverlay = new MyLocationNewOverlay(mapView);
        locationOverlay.enableMyLocation();
        locationOverlay.enableFollowLocation();

        // Customize the location icons
        android.graphics.drawable.Drawable personDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_user_direction);
        if (personDrawable != null) {
            android.graphics.Bitmap personBitmap = android.graphics.Bitmap.createBitmap(personDrawable.getIntrinsicWidth(),
                    personDrawable.getIntrinsicHeight(), android.graphics.Bitmap.Config.ARGB_8888);
            android.graphics.Canvas canvas = new android.graphics.Canvas(personBitmap);
            personDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            personDrawable.draw(canvas);
            
            locationOverlay.setPersonIcon(personBitmap);
            locationOverlay.setDirectionArrow(personBitmap, personBitmap);
        }

        locationOverlay.runOnFirstFix(this::run);
        mapView.getOverlays().add(locationOverlay);

    }

    private void run() {

        // Move the map to the current location on the first fix
        GeoPoint currentLocation = locationOverlay.getMyLocation();

        if (currentLocation != null) {
            requireActivity().runOnUiThread(() -> {
                mapView.getController().setCenter(currentLocation);
                mapView.getController().animateTo(currentLocation);
                mapView.getController().setZoom(17.0); // Adjust zoom level as needed

                isShowingLocation = true;
            });
        }
    }

    @SuppressLint({"SetTextI18n", "DefaultLocale"})
    private void startAlarmMonitor() {
        if (!isAdded() || destination == null) {
            isMonitorRunning = false;
            return;
        }

        isMonitorRunning = true;
        GeoPoint location, destinationLatLng;

        location = locationOverlay.getMyLocation();
        destinationLatLng = destination;

        if (location != null && destinationLatLng != null) {
            float[] results = new float[1];
            Location.distanceBetween(location.getLatitude(), location.getLongitude(),
                    destinationLatLng.getLatitude(), destinationLatLng.getLongitude(), results);

            float distance = results[0];

            String distanceStr;
            if(distanceUnit.equals("Kilometer")){
                distanceStr = String.format("%.1f", distance/1000)  + " " + "km";
            }else{
                distanceStr = (int) distance + " " + "m";
            }
            statusText.setText("Distance to destination: " + distanceStr);

            // Update Foreground Service Notification
            updateForegroundServiceNotification(distanceStr);


            if(showMyLocation){
                // Move the map to the current location on the first fix
                GeoPoint currentLocation = locationOverlay.getMyLocation();

                if (currentLocation != null) {
                    mapView.getController().animateTo(currentLocation);
                    mapView.getController().setZoom(17.0);
                }
            }

            if (distance <= destRadius) {
                playAlarm();
                isFirstAlarm = false;
                isMonitorRunning = false;
            } else {
                // Repeat checking every 5 seconds
                statusText.postDelayed(this::startAlarmMonitor, 5000);
            }
        }
        else{
            statusText.setText("Alarm not set");
            isMonitorRunning = false;
        }

    }

    @SuppressLint("SetTextI18n")
    private void playAlarm() {

//        Toast.makeText(requireContext(), "Alarm Triggered!", Toast.LENGTH_LONG).show();
        statusText.setText("You are close to the destination!");
        // Add sound here if needed
        showNotification("Location Alarm", "You reached the Destination!");
        triggerAlarm();
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

                destination = new GeoPoint(latitude, longitude);
                GeoPoint currentLocation = locationOverlay.getMyLocation();
                // addMarker(destination, "Destination");
                addMarkerWithCircle(destination, "Destination", destRadius);

                mapView.getController().setCenter(destination);
                mapView.getController().animateTo(destination);

                BoundingBox box = BoundingBox.fromGeoPoints(
                        Arrays.asList(currentLocation, destination)
                );

                mapView.zoomToBoundingBox(box, true, 150);

                Toast.makeText(requireContext(), "Destination set on map!", Toast.LENGTH_SHORT).show();

                if (!isMonitorRunning) {
                    startAlarmMonitor();
                }

            } else {
                Toast.makeText(requireContext(), "Location not found", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private void addMarkerWithCircle(GeoPoint point, String title, double radiusMeters) {

        if(circle!=null){
            mapView.getOverlays().remove(circle);
        }

        // Add a transparent circle around the marker
        circle = new Polygon(mapView);
        circle.setPoints(Polygon.pointsAsCircle(point, radiusMeters)); // Convert meters to degrees
        circle.setFillColor(0x4D9E73FF);
        circle.setStrokeColor(0xFF9E73FF);
        circle.setStrokeWidth(2.0f);
        mapView.getOverlays().add(circle);


        if(marker!=null){
            mapView.getOverlays().remove(marker);
        }

        // Add the marker
        marker = new Marker(mapView);
        marker.setPosition(point);
        marker.setTitle(title);
        marker.setIcon(getResources().getDrawable(R.drawable.ic_location));
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        mapView.getOverlays().add(marker);

        mapView.invalidate();
    }

    @SuppressLint("MissingPermission")
    private void startLocationService(){
        Intent serviceIntent = new Intent(requireContext(), ForegroundService.class);
        requireContext().startForegroundService(serviceIntent); // For Android 8.0+ (Oreo)
    }

    private void updateForegroundServiceNotification(String distance) {
        if (isAdded()) {
            Intent serviceIntent = new Intent(requireContext(), ForegroundService.class);
            serviceIntent.setAction(ForegroundService.ACTION_UPDATE_NOTIFICATION);
            serviceIntent.putExtra(ForegroundService.EXTRA_DISTANCE, distance);
            requireContext().startService(serviceIntent);
        }
    }


    private void createNotificationChannel() {
        CharSequence name = "Location Alarm Channel";
        String description = "Channel for location alarm notifications";
        int importance = NotificationManager.IMPORTANCE_HIGH;

        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
        channel.setDescription(description);

        NotificationManager notificationManager = requireContext().getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void showNotification(String title, String message) {
        Intent intent = new Intent(requireContext(), MainPage.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(requireContext(), 0, intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent stopIntent = new Intent(requireContext(), AlarmReceiver.class);
        stopIntent.setAction(AlarmReceiver.ACTION_STOP_ALARM);
        PendingIntent stopPendingIntent = PendingIntent.getBroadcast(requireContext(), 1, stopIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        @SuppressLint("NotificationTrampoline") NotificationCompat.Builder builder = new NotificationCompat.Builder(requireContext(), CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_location)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setFullScreenIntent(pendingIntent, true)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setOngoing(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .addAction(android.R.drawable.ic_delete, "Stop Alarm", stopPendingIntent);

        NotificationManager notificationManager = (NotificationManager) requireContext().getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(2, builder.build());
        }
    }

    @SuppressLint({"NotifyDataSetChanged", "MutatingSharedPrefs"})
    public void addHistory(String location) {
        if (location == null || location.isEmpty()) {
            Log.e("HistoryFragment", "Invalid location");
            return;
        }

        long timestamp = System.currentTimeMillis();
        Set<String> historySet = sharedPreferences.getStringSet("historyList", new HashSet<>());
        historySet.add(location + ";" + timestamp);

        editor.putStringSet("historyList", historySet);
        editor.apply();

        Log.d("HistoryFragment", "History added: " + location);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 200 && resultCode == RESULT_OK) {
            double latitude = data.getDoubleExtra("latitude", 0.0);
            double longitude = data.getDoubleExtra("longitude", 0.0);

            // Use the selected location as needed
            userSelectedLocation(new GeoPoint(latitude,longitude));
        }
    }

    @SuppressLint("SetTextI18n")
    public void userSelectedLocation(GeoPoint location){

        locationInput.setText("Selected destination");

        destination = location;

        if(destination!=null){
            addMarkerWithCircle(destination, "Destination", destRadius);

            mapView.getController().setCenter(destination);
            Toast.makeText(requireContext(), "Destination set on map!", Toast.LENGTH_SHORT).show();

            if (!isMonitorRunning) {
                startAlarmMonitor();
            }
        }

    }

    private void addFavorite(){
        String location = locationInput.getText().toString();
        if(location.isEmpty()){
            Toast.makeText(requireContext(), "Please enter a location name", Toast.LENGTH_SHORT).show();
        }
        else if(location.equals("Selected destination")){
            if(destination!=null){
                String locationCoordinates = destination.getLatitude() + ";" + destination.getLongitude();
                Intent intent = new Intent(requireContext(), AddFavoriteActivity.class);
                intent.putExtra("LocationAddress",locationCoordinates);
                startActivity(intent);
            }
        }
        else{
            Intent intent = new Intent(requireContext(), AddFavoriteActivity.class);
            intent.putExtra("LocationAddress",location);
            startActivity(intent);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                turnOnGPS();
                showCurrentLocation();
            }
            else {
                Toast.makeText(requireContext(), "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    public void setAlarm(String placeInput){
        locationInput.setText(placeInput);
        setAlarmButton.callOnClick();
    }

    public void setDestination(double latitude, double longitude){
        destination = new GeoPoint(latitude, longitude);
        setAlarm("Selected destination");
    }

    public void setAlarmText(String placeInput){
        locationInput.setText(placeInput);
    }

}
