package com.example.waywake;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HistoryFragment extends Fragment {

    private RecyclerView rvHistory;
    private HistoryAdapter adapter;
    private List<AlarmItem> historyList;
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "history_pref";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_history, container, false);

        rvHistory = view.findViewById(R.id.rvHistory);
        rvHistory.setLayoutManager(new LinearLayoutManager(getContext()));

        ImageView btnClearHistory = view.findViewById(R.id.btnClearHistory);
        sharedPreferences = getActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        loadHistory();

        adapter = new HistoryAdapter(historyList);
        rvHistory.setAdapter(adapter);


        btnClearHistory.setOnClickListener(v -> {
            showClearHistoryConfirmation();
        });

        return view;
    }

    public void loadHistory() {
        Set<String> historySet = sharedPreferences.getStringSet("historyList", new HashSet<>());
        historyList = new ArrayList<>();

        adapter = new HistoryAdapter(historyList);
        rvHistory.setAdapter(adapter);

        if (historySet != null && !historySet.isEmpty()) {
            for (String item : historySet) {
                String[] parts = item.split(";", 2); // Assuming format "location;timestamp"
                String location = parts[0];
                long timestamp = Long.parseLong(parts[1]);
                historyList.add(new AlarmItem(location, timestamp));
                adapter.notifyDataSetChanged();
            }
        } else {
            Log.d("HistoryFragment", "No history to display");
        }
    }

    public void clearHistory() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
        historyList.clear();
        adapter.notifyDataSetChanged();

        if (rvHistory.getAdapter() != null) {
            historyList = new ArrayList<>(); // Convert set to list

            // Set up RecyclerView or ListView adapter
            HistoryAdapter adapter = new HistoryAdapter(historyList);
            rvHistory.setAdapter(adapter);
        }
        Log.d("HistoryFragment", "History cleared");
        Toast.makeText(getContext(), "History cleared", Toast.LENGTH_SHORT).show();
    }

    public void addHistory(String location) {
        long timestamp = System.currentTimeMillis();
        Set<String> historySet = sharedPreferences.getStringSet("historyList", new HashSet<>());
        historySet.add(location + ";" + timestamp);

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putStringSet("historyList", historySet);
        editor.apply();

        Log.d("HistoryFragment", "History added: " + location);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadHistory();
    }

    private void showClearHistoryConfirmation() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Clear History")
                .setMessage("Are you sure you want to clear all history?")
                .setPositiveButton("Yes", (dialog, which) -> clearHistory())
                .setNegativeButton("No", null)
                .show();
    }
}


