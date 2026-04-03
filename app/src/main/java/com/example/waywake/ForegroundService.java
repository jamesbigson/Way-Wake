package com.example.waywake;


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;

import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

public class ForegroundService extends Service {

    public static final String ACTION_UPDATE_NOTIFICATION = "com.example.waywake.UPDATE_NOTIFICATION";
    public static final String EXTRA_DISTANCE = "extra_distance";

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
        if (intent != null && ACTION_UPDATE_NOTIFICATION.equals(intent.getAction())) {
            String distance = intent.getStringExtra(EXTRA_DISTANCE);
            if (distance != null) {
                NotificationManager manager = getSystemService(NotificationManager.class);
                if (manager != null) {
                    manager.notify(1, createNotification( distance + " remaining"));
                }
            }
        }
        // Service logic here
        Log.d("ForegroundService", "Service running...");
        return START_STICKY; // Keeps service alive until explicitly stopped
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        // Called when app is swiped from recent apps
        Log.d("ForegroundService", "App removed from recent apps. Stopping service...");
        stopSelf(); // Stop the service
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

