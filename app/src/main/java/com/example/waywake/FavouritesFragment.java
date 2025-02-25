package com.example.waywake;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class FavouritesFragment extends Fragment {

    private RecyclerView rvFavorites;
    private FavoritesAdapter adapter;
    private List<FavoriteItem> favoriteList;
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "favorites_pref";
    private static final String FAVORITES_KEY = "favorites";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_favourites, container, false);

        rvFavorites = view.findViewById(R.id.rvFavorites);
        rvFavorites.setLayoutManager(new LinearLayoutManager(getContext()));

        FloatingActionButton btnAddFavorite = view.findViewById(R.id.btnAddFavorite);
        sharedPreferences = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        favoriteList = new ArrayList<>();
        adapter = new FavoritesAdapter(favoriteList, this::removeFavorite,this::setAlarm, this::setDestination);
        rvFavorites.setAdapter(adapter);

        // Load favorites from SharedPreferences
        loadFavorites();

//        btnAddFavorite.setOnClickListener(v -> addFavorite("New Location " + (favoriteList.size() + 1)));

        btnAddFavorite.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), AddFavoriteActivity.class);
            startActivity(intent);
        });

        return view;
    }

    @SuppressLint("NotifyDataSetChanged")
    private void loadFavorites() {
        Set<String> favoritesSet = sharedPreferences.getStringSet(FAVORITES_KEY, new HashSet<>());
        favoriteList = new ArrayList<>();

        if (!favoritesSet.isEmpty()) {
            for (String item : favoritesSet) {
                String[] parts = item.split(";", 2); // Assuming format "locationName;locationAddress"
                String locationName = parts[0];
                String locationAddress = parts[1];
                favoriteList.add(new FavoriteItem(locationName, locationAddress));
                adapter.notifyDataSetChanged();
            }
        } else {
            Log.d("FavoriteFragment", "No Favorites to display");
        }

        adapter = new FavoritesAdapter(favoriteList, this::removeFavorite,this::setAlarm,this::setDestination);
        rvFavorites.setAdapter(adapter);
    }

    private void addFavorite(String locationName, String locationAddress) {
        if (!favoriteList.contains(locationName)) {
            favoriteList.add(new FavoriteItem(locationName,locationAddress));

            saveFavorites();
            Toast.makeText(requireContext(), "Favorite added", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(requireContext(), "Already in favorites", Toast.LENGTH_SHORT).show();
        }
    }

    private void removeFavorite(String location) {

        Set<String> favoritesSet = new HashSet<>();

        for (FavoriteItem item : favoriteList) {
            favoritesSet.add(item.getLocationName() + ";" + item.getLocationAddress());
        }

        int position = getPosition(location);
        if (position != -1) {
            favoriteList.remove(position);
            adapter.notifyItemRemoved(position);
            saveFavorites();
            Toast.makeText(getContext(), "Favorite removed", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveFavorites() {
        Set<String> favoritesSet = new HashSet<>();

        for (FavoriteItem item : favoriteList) {
            favoritesSet.add(item.getLocationName() + ";" + item.getLocationAddress());
        }

        sharedPreferences.edit().putStringSet(FAVORITES_KEY, favoritesSet).apply();
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onResume() {
        super.onResume();
        loadFavorites();
    }

    public int getPosition(String locationName){
        for (FavoriteItem item : favoriteList) {
            if(Objects.equals(item.getLocationName(), locationName)){
                return favoriteList.indexOf(item);
            }
        }
        return -1;
    }

    public void setAlarm(String placeInput){
        BottomNavigationView bottomNavigationView = requireActivity().findViewById(R.id.bottomNavigationView);
        FragmentManager fragmentManager = getParentFragmentManager();

        AlarmFragment alarmFragment = (AlarmFragment) fragmentManager.findFragmentByTag("f"+2);

        if(alarmFragment!=null){
            alarmFragment.setAlarm(placeInput);
            bottomNavigationView.setSelectedItemId(R.id.nav_set_alarm);
        }
    }

    public void setDestination(double latitude, double longitude){
        BottomNavigationView bottomNavigationView = requireActivity().findViewById(R.id.bottomNavigationView);
        FragmentManager fragmentManager = getParentFragmentManager();

        AlarmFragment alarmFragment = (AlarmFragment) fragmentManager.findFragmentByTag("f"+2);

        if(alarmFragment!=null){
            alarmFragment.setDestination(latitude, longitude);
            bottomNavigationView.setSelectedItemId(R.id.nav_set_alarm);
        }
    }
}
