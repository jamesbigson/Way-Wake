package com.example.waywake;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import org.osmdroid.config.Configuration;
import android.app.Activity;

public class AlarmSettings extends Activity {

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private Switch vibrationSwitch;
    private TextView selectedAlarmSound;

    // SharedPreferences
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    // Vibrator Service
    private Vibrator vibrator;
    private Spinner unitSpinner;

    private String selectedUnit = "Meter"; // Default unit


    // Key names for SharedPreferences
    public static final String PREFS_NAME = "user_settings";
    public static final String KEY_VIBRATION = "vibration";
    public static final String KEY_DISTANCE_UNIT = "distance_unit";
    public static final String KEY_ALARM_SOUND = "alarm_sound";


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().setUserAgentValue(getPackageName());
        setContentView(R.layout.activity_alarm_settings);

        // Initialize UI components
        vibrationSwitch = findViewById(R.id.vibration_toggle);
        LinearLayout selectAlarmSoundButton = findViewById(R.id.select_alarm_sound);
        Button stopBackgroundButton = findViewById(R.id.stop_background_check);
//        ImageView backButton = findViewById(R.id.back_button);
        selectedAlarmSound = findViewById(R.id.selected_alarm_sound);
        unitSpinner = findViewById(R.id.unitSpinner);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        editor = sharedPreferences.edit();

        // Load saved preferences
        loadPreferences();

        // Handle Vibration Toggle
        vibrationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            editor.putBoolean(KEY_VIBRATION, isChecked);
            editor.apply();
            if (isChecked) {
                if (vibrator.hasVibrator()) {
                    vibrator.vibrate(200); // Vibrate for 200ms
                }
            }
        });

        String[] units = {"Meter", "Kilometer"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, units);
        unitSpinner.setAdapter(adapter);

        // Set item selection listener
        unitSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedUnit = units[position];

                if (!selectedUnit.isEmpty()) {
                    editor.putString(KEY_DISTANCE_UNIT, selectedUnit);
                    editor.apply();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });


        // Handle Select Alarm Sound
        selectAlarmSoundButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, SelectAlarmSound.class);
            startActivity(intent);
        });

        // Handle Stop Background Service
        stopBackgroundButton.setOnClickListener(v -> {
            stopBackgroundService();
        });

//        backButton.setOnClickListener(v -> {
//            onBackPressed();
//        });

    }

    // Function to stop background service
    private void stopBackgroundService() {
        Intent serviceIntent = new Intent(this, ForegroundService.class);
        stopService(serviceIntent);
        Toast.makeText(this, "Background Service Stopped", Toast.LENGTH_SHORT).show();
    }

    // Load saved preferences
    private void loadPreferences() {
        boolean vibrationEnabled = sharedPreferences.getBoolean(KEY_VIBRATION, true);
        vibrationSwitch.setChecked(vibrationEnabled);

        String distanceUnit = sharedPreferences.getString(KEY_DISTANCE_UNIT, "Meter" );

        if(distanceUnit.equals("Meter")){
            unitSpinner.setSelection(0);
        }else{
            unitSpinner.setSelection(1);
        }

        String alarmSound = sharedPreferences.getString(KEY_ALARM_SOUND, "chiptune");

        if (alarmSound.startsWith("content://")) {
            selectedAlarmSound.setText(getFileNameFromUri(Uri.parse(alarmSound)));
        }else{
            selectedAlarmSound.setText(alarmSound);
        }

    }

    @Override
    protected void onResume() {
        loadPreferences();
        super.onResume();
    }

    public String getFileNameFromUri(Uri uri) {
        String fileName = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null) {
                try {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        cursor.moveToFirst();
                        fileName = cursor.getString(nameIndex);
                    }
                } finally {
                    cursor.close();
                }
            }
        }
        if (fileName == null) {
            fileName = uri.getLastPathSegment();
        }
        return fileName;
    }
}
