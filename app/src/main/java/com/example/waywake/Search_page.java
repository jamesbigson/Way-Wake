package com.example.waywake;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;

import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;


import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import android.content.SharedPreferences;
import android.content.Context;


public class Search_page extends AppCompatActivity {

    // IMPORTANT: Replace this with your actual LocationIQ API Key
    private static final String LOCATIONIQ_API_KEY = "pk.5a152e97cf3d57ae0d71c8891b9b9676";

    // LocationIQ Autocomplete Endpoint


    EditText searchBox;
    RecyclerView suggestionList;
    ImageView clearTextButton;
    List<PlaceItem> list = new ArrayList<>();
    SuggestionAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_page);

        searchBox = findViewById(R.id.searchBar);
        suggestionList = findViewById(R.id.suggestionList);
        clearTextButton = findViewById(R.id.clear_text);

        Intent intent = getIntent();
        String location = intent.getStringExtra("LOCATION_INPUT");
        if (location != null) {
            searchBox.setText(location);
        }

        adapter = new SuggestionAdapter(list, item -> {
            String place;
            if ("History".equals(item.subtitle)) {
                place = item.title;
            } else {
                place = (item.subtitle == null || item.subtitle.isEmpty()) 
                        ? item.title 
                        : String.format("%s %s", item.title, item.subtitle);
            }
            searchBox.setText(place);

            Intent resultIntent = new Intent();
            resultIntent.putExtra("selected_place", place);
            setResult(RESULT_OK, resultIntent);
            finish();   // go back to AlarmFragment
        });

        suggestionList.setLayoutManager(new LinearLayoutManager(this));
        suggestionList.setAdapter(adapter);

        clearTextButton.setOnClickListener(v -> {
            searchBox.setText("");
        });

        searchBox.setOnEditorActionListener((v, actionId, event) -> {
            if(actionId== EditorInfo.IME_ACTION_DONE){

                String place = searchBox.getText().toString();

                if (place.isEmpty()) {
                    Toast.makeText(this, "Please enter a location", Toast.LENGTH_SHORT).show();
                    return true;
                }

                Intent resultIntent = new Intent();
                resultIntent.putExtra("selected_place", place);
                setResult(RESULT_OK, resultIntent);
                finish();   // go back to AlarmFragment
                return true;
            }
            return false;
        });

        searchBox.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(searchBox.getText().toString().isEmpty()){
                    clearTextButton.setVisibility(View.GONE);
                    loadHistorySuggestions();
                }else{
                    clearTextButton.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 2) fetchLocationIQ(s.toString());
                else if (s.length() == 0) loadHistorySuggestions();
            }
        });

        loadHistorySuggestions();
    }

    private void loadHistorySuggestions() {
        SharedPreferences sharedPreferences = getSharedPreferences("history_pref", Context.MODE_PRIVATE);
        Set<String> historySet = sharedPreferences.getStringSet("historyList", new HashSet<>());
        
        list.clear();
        if (historySet != null) {
            List<String> sortedHistory = new ArrayList<>(historySet);
            // Sort by timestamp descending
            Collections.sort(sortedHistory, (a, b) -> {
                try {
                    String[] partsA = a.split(";", 2);
                    String[] partsB = b.split(";", 2);
                    if (partsA.length < 2 || partsB.length < 2) return 0;
                    long t1 = Long.parseLong(partsA[1]);
                    long t2 = Long.parseLong(partsB[1]);
                    return Long.compare(t2, t1); // Descending order
                } catch (Exception e) {
                    return 0;
                }
            });

            for (String item : sortedHistory) {
                String[] parts = item.split(";", 2);
                if (parts.length >= 1) {
                    list.add(new PlaceItem(parts[0], "History"));
                }
            }
        }
        runOnUiThread(adapter::notifyDataSetChanged);
    }

    private void fetchLocationIQ(String query) {
        new Thread(() -> {
            try {
                String urlStr = "https://us1.locationiq.com/v1/autocomplete.php?key=" + LOCATIONIQ_API_KEY + "&q="
                        + URLEncoder.encode(query, "UTF-8")
                        + "&limit=8";

                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("User-Agent", "MyApp");

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                JSONArray arr = new JSONArray(sb.toString());

                list.clear();

                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);

                    String display = obj.getString("display_name");

                    String[] parts = display.split(",", 2);
                    String title = parts[0];
                    String subtitle = parts.length > 1 ? parts[1] : "";

                    list.add(new PlaceItem(title, subtitle));
                }

                runOnUiThread(adapter::notifyDataSetChanged);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }



}