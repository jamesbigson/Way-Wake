package com.example.waywake;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.Vibrator;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

public class ForegroundService extends Service {

    public static final String ACTION_UPDATE_NOTIFICATION = "com.example.waywake.UPDATE_NOTIFICATION";
    public static final String ACTION_START_ALARM = "com.example.waywake.START_ALARM";
    public static final String ACTION_STOP_ALARM = "com.example.waywake.STOP_ALARM";
    public static final String ACTION_START_MONITORING = "com.example.waywake.START_MONITORING";
    public static final String ACTION_STOP_MONITORING = "com.example.waywake.STOP_MONITORING";

    public static final String EXTRA_DISTANCE = "extra_distance";
    public static final String EXTRA_DEST_LAT = "extra_dest_lat";
    public static final String EXTRA_DEST_LNG = "extra_dest_lng";
    public static final String EXTRA_DEST_RADIUS = "extra_dest_radius";
    public static final String EXTRA_DEST_NAME = "extra_dest_name";
    public static final String EXTRA_ALARM_SOUND = "alarm_sound";
    public static final String EXTRA_VIBRATION_ENABLED = "vibration_enabled";

    public static final String BROADCAST_LOCATION_UPDATE = "LOCATION_UPDATE_EVENT";
    public static final String BROADCAST_ALARM_TRIGGERED = "ALARM_TRIGGERED_EVENT";

    private MediaPlayer mediaPlayer;
    private Vibrator vibrator;
    private boolean isAlarmRinging = false;

    private final Handler alarmHandler = new Handler(Looper.getMainLooper());
    private final Runnable alarmStopRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d("ForegroundService", "Alarm threshold limit reached (30 seconds). Auto-stopping alarm.");
            stopAlarm();
            
            // Broadcast STOP_ALARM_EVENT so UI fragments know it stopped and can dismiss the dialog/popup
            Intent stopIntent = new Intent("STOP_ALARM_EVENT");
            sendBroadcast(stopIntent);
        }
    };

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private PowerManager.WakeLock wakeLock;

    private boolean isMonitoring = false;
    private double destLat = 0.0;
    private double destLng = 0.0;
    private int destRadius = 5000;
    private String destName = "Selected Destination";
    private String alarmSound = "chiptune";
    private boolean vibrationEnabled = true;

    private String lastLocationName = "";
    private double lastGeocodedLat = 0.0;
    private double lastGeocodedLng = 0.0;
    private boolean isGeocodingInProgress = false;

    @Override
    public void onCreate() {
        super.onCreate();

        createNotificationChannel();
        lastLocationName = getSharedPreferences("history_pref", MODE_PRIVATE).getString("current_location", "");
        startForeground(1, createNotification("Foreground Service is active."));
        Log.d("ForegroundService", "Service created...");

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        if (powerManager != null) {
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WayWake:ForegroundServiceWakeLock");
        }
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                "foreground_service_channel",
                "Foreground Service",
                NotificationManager.IMPORTANCE_LOW
        );

        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification(String contentText) {
        Intent intent = new Intent(this, MainPage.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, "foreground_service_channel")
                .setContentTitle("Way Wake Active")
                .setContentText(contentText)
                .setSmallIcon(R.drawable.ic_location)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    private Notification createNotification(String distanceStr, String locationName) {
        Intent intent = new Intent(this, MainPage.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE
        );

        String collapsedText = distanceStr + " to destination";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "foreground_service_channel")
                .setContentTitle("Way Wake Active")
                .setContentText(collapsedText)
                .setSmallIcon(R.drawable.ic_location)
                .setContentIntent(pendingIntent)
                .setOngoing(true);

        StringBuilder expandedText = new StringBuilder(collapsedText);
        if (locationName != null && !locationName.isEmpty()) {
            expandedText.append("\nCurrent location: ").append(locationName);
        }
        builder.setStyle(new NotificationCompat.BigTextStyle().bigText(expandedText.toString()));

        return builder.build();
    }

    private void updateNotification(String distanceStr, String locationName) {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(1, createNotification(distanceStr, locationName));
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_UPDATE_NOTIFICATION.equals(action)) {
                String distance = intent.getStringExtra(EXTRA_DISTANCE);
                if (distance != null) {
                    NotificationManager manager = getSystemService(NotificationManager.class);
                    if (manager != null) {
                        manager.notify(1, createNotification(distance, lastLocationName));
                    }
                }
            } else if (ACTION_START_MONITORING.equals(action)) {
                destLat = intent.getDoubleExtra(EXTRA_DEST_LAT, 0.0);
                destLng = intent.getDoubleExtra(EXTRA_DEST_LNG, 0.0);
                destRadius = intent.getIntExtra(EXTRA_DEST_RADIUS, 5000);
                if (intent.hasExtra(EXTRA_DEST_NAME)) {
                    destName = intent.getStringExtra(EXTRA_DEST_NAME);
                }
                if (intent.hasExtra(EXTRA_ALARM_SOUND)) {
                    alarmSound = intent.getStringExtra(EXTRA_ALARM_SOUND);
                }
                vibrationEnabled = intent.getBooleanExtra(EXTRA_VIBRATION_ENABLED, true);
                startMonitoring();
            } else if (ACTION_STOP_MONITORING.equals(action)) {
                stopMonitoring();
                stopAlarm();
            } else if (ACTION_START_ALARM.equals(action)) {
                startAlarm(intent);
            } else if (ACTION_STOP_ALARM.equals(action)) {
                stopMonitoring();
                stopAlarm();
            }
        }
        Log.d("ForegroundService", "Service running with action: " + (intent != null ? intent.getAction() : "null"));
        return START_STICKY;
    }

    @SuppressLint("MissingPermission")
    private void startMonitoring() {
        if (isMonitoring) {
            stopMonitoring();
        }
        isMonitoring = true;

        // Persist active alarm state for HomeFragment
        getSharedPreferences("active_alarm_pref", MODE_PRIVATE).edit()
                .putBoolean("is_active", true)
                .putString("dest_name", destName != null ? destName : "Selected Destination")
                .putInt("dest_radius", destRadius)
                .putFloat("dest_lat", (float) destLat)
                .putFloat("dest_lng", (float) destLng)
                .apply();

        if (wakeLock != null && !wakeLock.isHeld()) {
            wakeLock.acquire();
            Log.d("ForegroundService", "WakeLock acquired for location monitoring");
        }

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateIntervalMillis(3000)
                .setMinUpdateDistanceMeters(0)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null) return;
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        processLocationUpdate(location);
                    }
                }
            }
        };

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
            Log.d("ForegroundService", "Started location updates in background service");
        } catch (SecurityException e) {
            Log.e("ForegroundService", "Permission error requesting location updates", e);
        }
    }

    private void processLocationUpdate(Location location) {
        float[] results = new float[1];
        Location.distanceBetween(location.getLatitude(), location.getLongitude(), destLat, destLng, results);
        float distanceMeters = results[0];

        String distanceStr;
        if (distanceMeters >= 1000) {
            distanceStr = String.format(Locale.US, "%.1f km", distanceMeters / 1000.0);
        } else {
            distanceStr = String.format(Locale.US, "%d m", (int) distanceMeters);
        }

        // Save latest remaining distance
        getSharedPreferences("active_alarm_pref", MODE_PRIVATE).edit()
                .putString("remaining_distance", distanceStr)
                .apply();

        // Update Foreground Notification immediately with the minimal layout and last known location name
        updateNotification(distanceStr, lastLocationName);

        // Fetch location name asynchronously if distance since last query is at least 100 meters
        final String finalDistanceStr = distanceStr;
        final double currentLat = location.getLatitude();
        final double currentLng = location.getLongitude();

        float[] distanceChange = new float[1];
        if (lastGeocodedLat != 0.0 || lastGeocodedLng != 0.0) {
            Location.distanceBetween(lastGeocodedLat, lastGeocodedLng, currentLat, currentLng, distanceChange);
        } else {
            distanceChange[0] = 999.0f; // Force geocoding on first run
        }

        if (distanceChange[0] >= 100.0f && !isGeocodingInProgress) {
            isGeocodingInProgress = true;
            new Thread(() -> {
                try {
                    Geocoder geocoder = new Geocoder(ForegroundService.this, Locale.getDefault());
                    List<Address> addresses = geocoder.getFromLocation(currentLat, currentLng, 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        String currentPlace = addresses.get(0).getLocality();
                        if (currentPlace == null || currentPlace.isEmpty()) {
                            currentPlace = addresses.get(0).getAdminArea();
                        }
                        if (currentPlace != null && !currentPlace.isEmpty()) {
                            lastLocationName = currentPlace;
                            lastGeocodedLat = currentLat;
                            lastGeocodedLng = currentLng;

                            getSharedPreferences("history_pref", MODE_PRIVATE).edit()
                                    .putString("current_location", currentPlace)
                                    .apply();

                            updateNotification(finalDistanceStr, lastLocationName);
                        }
                    }
                } catch (IOException e) {
                    Log.e("ForegroundService", "Geocoder query failed", e);
                } finally {
                    isGeocodingInProgress = false;
                }
            }).start();
        }

        // Broadcast update to UI if active
        Intent broadcastIntent = new Intent(BROADCAST_LOCATION_UPDATE);
        broadcastIntent.putExtra("distance_meters", distanceMeters);
        broadcastIntent.putExtra("distance_str", distanceStr);
        broadcastIntent.putExtra("latitude", location.getLatitude());
        broadcastIntent.putExtra("longitude", location.getLongitude());
        broadcastIntent.putExtra("dest_name", destName);
        sendBroadcast(broadcastIntent);

        if (distanceMeters <= destRadius && isMonitoring) {
            Log.d("ForegroundService", "Destination reached! Distance: " + distanceMeters + " meters, Radius: " + destRadius);
            stopMonitoring();
            wakeUpScreen();

            Intent alarmIntent = new Intent();
            alarmIntent.putExtra("alarm_sound", alarmSound);
            alarmIntent.putExtra("vibration_enabled", vibrationEnabled);
            startAlarm(alarmIntent);

            Intent triggerIntent = new Intent(BROADCAST_ALARM_TRIGGERED);
            sendBroadcast(triggerIntent);
        }
    }

    private void wakeUpScreen() {
        try {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                @SuppressWarnings("deprecation")
                PowerManager.WakeLock screenLock = pm.newWakeLock(
                        PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE,
                        "WayWake:AlarmScreenWake"
                );
                screenLock.acquire(10000);
            }
        } catch (Exception e) {
            Log.e("ForegroundService", "Error waking up screen", e);
        }
    }

    private void stopMonitoring() {
        isMonitoring = false;

        // Clear active alarm state
        getSharedPreferences("active_alarm_pref", MODE_PRIVATE).edit()
                .putBoolean("is_active", false)
                .remove("remaining_distance")
                .apply();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            locationCallback = null;
            Log.d("ForegroundService", "Stopped location updates in background service");
        }
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            Log.d("ForegroundService", "WakeLock released");
        }
    }

    private void startAlarm(Intent intent) {
        if (isAlarmRinging) return;
        isAlarmRinging = true;

        Log.d("ForegroundService", "Starting alarm sound and vibration");

        // Vibration
        if (intent.getBooleanExtra("vibration_enabled", true)) {
            vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (vibrator != null) {
                long[] pattern = {0, 500, 500, 500};
                vibrator.vibrate(pattern, 0);
            }
        }

        // Sound 
        try {
            String sound = intent.getStringExtra("alarm_sound");
            if (sound == null || sound.equals("chiptune")) {
                 int soundResId = getResources().getIdentifier("chiptune", "raw", getPackageName());
                 mediaPlayer = MediaPlayer.create(this, soundResId);
            } else if (sound.startsWith("content://")) {
                mediaPlayer = MediaPlayer.create(this, Uri.parse(sound));
            } else if (!sound.equals("Silent")) {
                int soundResId = getResources().getIdentifier(sound, "raw", getPackageName());
                mediaPlayer = MediaPlayer.create(this, soundResId);
            }

            if (mediaPlayer != null) {
                mediaPlayer.setLooping(true);
                mediaPlayer.start();
            }
        } catch (Exception e) {
            Log.e("ForegroundService", "Error starting sound", e);
        }

        // Schedule automatic stop after 30 seconds
        alarmHandler.postDelayed(alarmStopRunnable, 30000);
    }

    private void stopAlarm() {
        Log.d("ForegroundService", "Stopping alarm sound and vibration");
        isAlarmRinging = false;

        // Cancel any pending auto-stop callback
        alarmHandler.removeCallbacks(alarmStopRunnable);

        if (vibrator != null) {
            vibrator.cancel();
        }
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
            } catch (Exception e) {
                Log.e("ForegroundService", "Error stopping sound", e);
            } finally {
                mediaPlayer = null;
            }
        }

        // Cancel the alarm ringing notification (ID 2)
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.cancel(2);
        }
    }

    @Override
    public void onDestroy() {
        stopMonitoring();
        stopAlarm();
        super.onDestroy();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.d("ForegroundService", "App swiped out from recents. Stopping service...");
        stopMonitoring();
        stopAlarm();
        stopSelf();
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
