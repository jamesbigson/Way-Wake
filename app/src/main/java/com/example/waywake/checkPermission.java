package com.example.waywake;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;

public class checkPermission extends AppCompatActivity {
    Button allowAccessButton;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 101;
    private boolean locationPermission = false;
    private boolean notificationPermission = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_permission);

        allowAccessButton = findViewById(R.id.allow_access_button);
        Button notNowButton = findViewById(R.id.not_now_button);
        ImageView closeButton = findViewById(R.id.close_button);

        allowAccessButton.setOnClickListener(v -> {
            if(allowPermissions()){
                Intent intent = new Intent(this, MainPage.class);
                startActivity(intent);
                finish();
            }
        });

        notNowButton.setOnClickListener(v -> {
            finish();
        });

        closeButton.setOnClickListener(v -> {
            finish();
        });
    }

    private boolean allowPermissions() {

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQUEST_CODE);
        }

        if(ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT <= Build.VERSION_CODES.TIRAMISU)){
            return true;
        }
        return false;

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode==LOCATION_PERMISSION_REQUEST_CODE){
            if((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)){
                allowAccessButton.callOnClick();
                locationPermission = true;
            }
        }else if(requestCode==NOTIFICATION_PERMISSION_REQUEST_CODE){
            if((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)){
                notificationPermission = true;
            }
        }

        if(locationPermission && notificationPermission){
            Intent intent = new Intent(this, MainPage.class);
            startActivity(intent);
            finish();
        }
    }
}