package com.example.waywake;

import android.annotation.SuppressLint;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.ImageView;
import android.Manifest;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SelectAlarmSound extends AppCompatActivity {

    List<String> soundList;
    SoundAdapter adapter;
    private MediaPlayer mediaPlayer;
    private static final int STORAGE_PERMISSION_CODE = 101;
    private static final int AUDIO_PERMISSION_CODE = 102;
    public static final int DEFAULT_SOUNDS = 4;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "user_settings";
    private static final String KEY_SOUND_LIST = "sound_LIST";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_sound);

        Button addSoundButton = findViewById(R.id.add_sound_button);
        ImageView backButton = findViewById(R.id.back_button);
        RecyclerView recyclerView = findViewById(R.id.sound_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);


        // Sample sound list (use your actual sound names in the raw folder)
        requestStoragePermission();

        soundList = new ArrayList<>();

        soundList.add("Silent");        // 0
        soundList.add("chiptune");      // 1
        soundList.add("jingle_bells");  // 2
        soundList.add("tropical");      // 3


        adapter = new SoundAdapter(this, soundList);
        recyclerView.setAdapter(adapter);

        loadSound();

        addSoundButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("audio/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(intent, 100); // Use a request code
        });

        backButton.setOnClickListener(v -> onBackPressed());
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            Uri audioUri = data.getData();
            assert audioUri != null;
            addSound(audioUri.toString());
            getContentResolver().takePersistableUriPermission(audioUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        mediaPlayer = adapter.getMediaPlayer();
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
        }
        return super.dispatchTouchEvent(event);
    }

    @Override
    public void onBackPressed() {
        mediaPlayer = adapter.getMediaPlayer();
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
        super.onBackPressed();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mediaPlayer = adapter.getMediaPlayer();
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
    }


    private void requestStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    STORAGE_PERMISSION_CODE
            );
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.READ_MEDIA_AUDIO},
                    AUDIO_PERMISSION_CODE
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
                Log.d("permission","Storage grant");
            } else {
                // Permission denied
                Log.d("permission","Permission denied");
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void loadSound() {
        Set<String> soundSet = sharedPreferences.getStringSet(KEY_SOUND_LIST , new HashSet<>());


        if (!soundSet.isEmpty()) {
            for (String item : soundSet) {
                soundList.add(item);
                adapter.notifyDataSetChanged();
            }
        } else {
            Log.d("SelectSound", "No sound to display");
        }
    }

    @SuppressLint({"NotifyDataSetChanged", "MutatingSharedPrefs"})
    public void addSound(String sound){
        Set<String> soundSet = sharedPreferences.getStringSet(KEY_SOUND_LIST, new HashSet<>());
        if(!soundSet.contains(sound)){
            soundSet.add(sound);
            soundList.add(sound);
            adapter.notifyDataSetChanged();

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putStringSet(KEY_SOUND_LIST, soundSet);
            editor.apply();
        }
    }

}
