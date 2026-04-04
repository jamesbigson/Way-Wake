package com.example.waywake;


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.IBinder;
import android.os.Vibrator;
import android.util.Log;

import androidx.core.app.NotificationCompat;

public class ForegroundService extends Service {

    public static final String ACTION_UPDATE_NOTIFICATION = "com.example.waywake.UPDATE_NOTIFICATION";
    public static final String ACTION_START_ALARM = "com.example.waywake.START_ALARM";
    public static final String ACTION_STOP_ALARM = "com.example.waywake.STOP_ALARM";
    public static final String EXTRA_DISTANCE = "extra_distance";

    private MediaPlayer mediaPlayer;
    private Vibrator vibrator;
    private boolean isAlarmRinging = false;

    @Override
    public void onCreate() {
        super.onCreate();

        createNotificationChannel();
        startForeground(1, createNotification("Foreground Service is active."));
        Log.d("ForegroundService", "Service created...");
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
                PendingIntent.FLAG_IMMUTABLE // Changed from FLAG_MUTABLE for better practice if not needed
        );

        return new NotificationCompat.Builder(this, "foreground_service_channel")
                .setContentTitle("Way Wake Active")
                .setContentText(contentText)
                .setSmallIcon(R.drawable.ic_location)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
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
                        manager.notify(1, createNotification(distance + " remaining"));
                    }
                }
            } else if (ACTION_START_ALARM.equals(action)) {
                startAlarm(intent);
            } else if (ACTION_STOP_ALARM.equals(action)) {
                stopAlarm();
            }
        }
        // Service logic here
        Log.d("ForegroundService", "Service running with action: " + (intent != null ? intent.getAction() : "null"));
        return START_STICKY; // Keeps service alive until explicitly stopped
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
            // Check for sound name/URI in intent if needed
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
    }

    private void stopAlarm() {
        Log.d("ForegroundService", "Stopping alarm sound and vibration");
        isAlarmRinging = false;
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
    }

    @Override
    public void onDestroy() {
        stopAlarm();
        super.onDestroy();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.d("ForegroundService", "App swiped out from recents. Stopping service...");
        stopSelf();
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

