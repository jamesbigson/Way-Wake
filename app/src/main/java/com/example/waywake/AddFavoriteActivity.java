package com.example.waywake;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class AddFavoriteActivity extends AppCompatActivity {
    private List<FavoriteItem> favoriteList;
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "favorites_pref";
    private static final String FAVORITES_KEY = "favorites";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_favorite);

        EditText inputLocationName = findViewById(R.id.etName);
        EditText inputLocationAddress = findViewById(R.id.etAddress);
        Button saveButton = findViewById(R.id.btnSave);

        sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        loadFavorites();

        Intent intent = getIntent();
        if(intent!=null){
            String locationAddress = intent.getStringExtra("LocationAddress");
            inputLocationAddress.setText(locationAddress);
        }

        saveButton.setOnClickListener(v -> {
            String locationName = inputLocationName.getText().toString();
            String locationAddress = inputLocationAddress.getText().toString();

            if(!locationName.isEmpty() && !locationAddress.isEmpty()){
                addFavorite(locationName, locationAddress);
                finish();
            }
        });

    }

    private void loadFavorites() {
        Set<String> favoritesSet = sharedPreferences.getStringSet(FAVORITES_KEY, new HashSet<>());
        favoriteList = new ArrayList<>();

        if (favoritesSet != null && !favoritesSet.isEmpty()) {
            for (String item : favoritesSet) {
                String[] parts = item.split(";", 2); // Assuming format "locationName;locationAddress"
                String locationName = parts[0];
                String locationAddress = parts[1];
                favoriteList.add(new FavoriteItem(locationName, locationAddress));
            }
        } else {
            Log.d("FavoriteFragment", "No Favorites to display");
        }
    }

    private void addFavorite(String locationName, String locationAddress) {
        if (getPosition(locationName)==-1) {
            favoriteList.add(new FavoriteItem(locationName,locationAddress));

            saveFavorites();
            Toast.makeText(this, "Favorite added", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Already in favorites", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveFavorites() {
        Set<String> favoritesSet = new HashSet<>();

        for (FavoriteItem item : favoriteList) {
            favoritesSet.add(item.getLocationName() + ";" + item.getLocationAddress());
        }

        sharedPreferences.edit().putStringSet(FAVORITES_KEY, favoritesSet).apply();
    }

    public int getPosition(String locationName){

        for (FavoriteItem item : favoriteList) {
            if(Objects.equals(item.getLocationName(), locationName)){
                return favoriteList.indexOf(item);
            }
        }
        return -1;
    }
}