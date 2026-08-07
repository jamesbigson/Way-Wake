package com.example.waywake;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {
    private List<AlarmItem> historyList;
    private static final long ONE_DAY_MS = 24 * 60 * 60 * 1000; // 1 day in milliseconds

    public HistoryAdapter(List<AlarmItem> historyList) {
        this.historyList = historyList;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        AlarmItem alarmItem = historyList.get(position);
        String rawLocation = alarmItem.getLocation();
        String title = rawLocation;
        String subtitle = "";

        if (rawLocation != null) {
            String[] parts = rawLocation.split("  ", 2);
            if (parts.length == 2) {
                title = parts[0].trim();
                subtitle = parts[1].trim();
            } else {
                parts = rawLocation.split(",", 2);
                if (parts.length == 2) {
                    title = parts[0].trim();
                    subtitle = parts[1].trim();
                }
            }
        }

        holder.tvLocation.setText(title);
        if (subtitle == null || subtitle.isEmpty()) {
            holder.tvLocationSub.setVisibility(View.GONE);
        } else {
            holder.tvLocationSub.setText(subtitle);
            holder.tvLocationSub.setVisibility(View.VISIBLE);
        }

        holder.tvTime.setText(formatTime(alarmItem.getTimestamp()));

        // Check if the alarm is from the same day or older
        long currentTime = System.currentTimeMillis();
        if (currentTime - alarmItem.getTimestamp() < ONE_DAY_MS) {
            holder.tvDate.setText("Today");
        } else {
            holder.tvDate.setText(formatDate(alarmItem.getTimestamp()));
        }
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvLocation, tvLocationSub, tvTime, tvDate;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvLocation = itemView.findViewById(R.id.alarmLocation);
            tvLocationSub = itemView.findViewById(R.id.alarmLocationSub);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvDate = itemView.findViewById(R.id.tvDate);
        }
    }

    // Helper methods to format time and date
    private String formatTime(long timestamp) {
        SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
        return timeFormat.format(new Date(timestamp));
    }

    private String formatDate(long timestamp) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());
        return dateFormat.format(new Date(timestamp));
    }
}

