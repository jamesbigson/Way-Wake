package com.example.waywake;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.RelativeLayout;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        RelativeLayout splashScreen = findViewById(R.id.splash_screen);
        Animation selectAnimation = AnimationUtils.loadAnimation(this, R.anim.select_animation);

        // Delay for 3 seconds before moving to the main activity
        new Handler().postDelayed(() -> {
            Intent intent;
            splashScreen.startAnimation(selectAnimation);
            if(checkPermissions()) {
                intent = new Intent(SplashActivity.this, MainPage.class);
            }
            else{
                intent = new Intent(SplashActivity.this, checkPermission.class);
            }
            startActivity(intent);
            finish();
        }, 1000);
    }

    private boolean checkPermissions() {

        if(ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&( Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU || ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED )){
            return true;
        }
        return false;
    }
}