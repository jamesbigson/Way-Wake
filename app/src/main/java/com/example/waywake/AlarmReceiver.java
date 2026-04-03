package com.example.waywake;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class AlarmReceiver extends BroadcastReceiver {
    public static final String ACTION_STOP_ALARM = "com.example.waywake.STOP_ALARM";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && ACTION_STOP_ALARM.equals(intent.getAction())) {
            Log.d("AlarmReceiver", "Stop Alarm action received");

            // 1. Immediately tell the ForegroundService to stop the noise
            Intent serviceIntent = new Intent(context, ForegroundService.class);
            serviceIntent.setAction(ForegroundService.ACTION_STOP_ALARM);
            context.startService(serviceIntent);

            // 2. Send broadcast to be caught by AlarmFragment (to dismiss the popup if app is open)
            Intent stopIntent = new Intent("STOP_ALARM_EVENT");
            context.sendBroadcast(stopIntent);

            // 3. Launch MainPage to handle any other cleanup and show the app
            Intent mainIntent = new Intent(context, MainPage.class);
            mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            mainIntent.putExtra("stop_alarm", true);
            context.startActivity(mainIntent);
        }
    }
}
