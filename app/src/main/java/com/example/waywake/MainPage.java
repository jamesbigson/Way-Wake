package com.example.waywake;

import static com.example.waywake.AlarmFragment.LOCATION_PERMISSION_REQUEST_CODE;
import static com.example.waywake.AlarmFragment.PERMISSION_REQUEST_CODE;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.SettingsClient;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;

import org.osmdroid.config.Configuration;

public class MainPage extends AppCompatActivity implements NetworkChangeReceiver.NetworkChangeListener {

    ViewPager2 viewPager;
    BottomNavigationView bottomNavigationView;
    private boolean isVPCreated = false;
    TextView retryButton,retryText;

    private NetworkChangeReceiver networkChangeReceiver;
    private Snackbar snackbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().setUserAgentValue("MyLocationApp/1.0");
        setContentView(R.layout.activity_main_page);

        viewPager = findViewById(R.id.viewPager);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        retryButton = findViewById(R.id.retry_button);
        retryText = findViewById(R.id.retry_text);

        networkChangeReceiver = new NetworkChangeReceiver(this);

        // Register the broadcast receiver
        registerReceiver(networkChangeReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        checkPermissions();
        createViewPager();


        // Sync BottomNavigationView with ViewPager2
        bottomNavigationView.setOnItemSelectedListener(item -> {
            if(item.getItemId()==R.id.nav_home){ //nav_home
                viewPager.setCurrentItem(0,false);
                return true;
            }
            else if(item.getItemId()==R.id.nav_favorites){ //nav_favorites
                viewPager.setCurrentItem(1,false);
                return true;
            }
            else if (item.getItemId()==R.id.nav_set_alarm) { //nav_settings
                viewPager.setCurrentItem(2,false);
                return true;
            }
            else if (item.getItemId()==R.id.nav_history) { //nav_settings
                viewPager.setCurrentItem(3,false);
                return true;
            }
            else if (item.getItemId()==R.id.nav_settings) { //nav_settings
                viewPager.setCurrentItem(4,false);
                return true;
            }
            return false;
        });

        retryButton.setOnClickListener(v -> {
            checkPermissions();
            if(isLocationEnabled()){
                createViewPager();
            }
        });
    }

    private void createViewPager() {
        // Set up ViewPager2 adapter
        if (!isVPCreated && isLocationEnabled()) {

            retryButton.setVisibility(View.GONE);
            retryText.setVisibility(View.GONE);

            ViewPagerAdapter adapter = new ViewPagerAdapter(this);
            viewPager.setAdapter(adapter);
            viewPager.setOffscreenPageLimit(5);
            viewPager.setUserInputEnabled(false);

            isVPCreated = true;
        }
    }

    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    @Override
    protected void onResume() {
        if(isLocationEnabled()){
            createViewPager();
        }
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        if(viewPager.getCurrentItem()==0){
            super.onBackPressed();
        }else{
            viewPager.setCurrentItem(0,false);
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
        }
    }

    private void checkPermissions() {

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            turnOnGPS();
        }

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, PERMISSION_REQUEST_CODE);
        }
    }

    private void turnOnGPS() {
        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10000)
                .setFastestInterval(5000);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
        SettingsClient settingsClient = LocationServices.getSettingsClient(this);
        settingsClient.checkLocationSettings(builder.build())
                .addOnSuccessListener(this, locationSettingsResponse -> {
                    // GPS is already enabled
                })
                .addOnFailureListener(this, e -> {
                    // GPS is off, show settings
                    showEnableLocation();
                });
    }

    private void showEnableLocation() {
        new AlertDialog.Builder(this)
                .setTitle("Enable Location")
                .setMessage("Your location is turned off.Please enable location services to continue.")
                .setPositiveButton("Enable", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    dialog.dismiss();
                })
                .show();
    }

    @Override
    protected void onDestroy() {
        stopAlarm();
        super.onDestroy();
        unregisterReceiver(networkChangeReceiver);
    }

    public void stopAlarm() {
        AlarmFragment alarmFragment = (AlarmFragment) getSupportFragmentManager().findFragmentByTag("f"+2);
        if(alarmFragment!=null){
            alarmFragment.stopAlarm();
        }

    }

    @Override
    public void onNetworkChange(boolean isConnected) {
        if (!isConnected) {
            showNoInternetSnackbar();
        } else {
            dismissNoInternetSnackbar();
        }
    }

    private void showNoInternetSnackbar() {
        if (snackbar == null || !snackbar.isShown()) {
            snackbar = Snackbar.make(findViewById(android.R.id.content),
                            "⚠ No Internet Connection", Snackbar.LENGTH_INDEFINITE)
                    .setBackgroundTint(getResources().getColor(android.R.color.holo_red_dark))
                    .setTextColor(getResources().getColor(android.R.color.white));
            snackbar.show();
        }
    }

    private void dismissNoInternetSnackbar() {
        if (snackbar != null && snackbar.isShown()) {
            snackbar.dismiss();
        }
    }

}

