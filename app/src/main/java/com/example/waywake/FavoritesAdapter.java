package com.example.waywake;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Objects;

public class FavoritesAdapter extends RecyclerView.Adapter<FavoritesAdapter.FavoriteViewHolder> {

    private List<FavoriteItem> favoriteList;
    private OnFavoriteRemoveListener removeListener;
    private OnSetAlarmListener alarmListener;

    private OnDestinationAlarmListener destinationAlarmListener;

    public FavoritesAdapter(List<FavoriteItem> favoriteList, OnFavoriteRemoveListener removeListener, OnSetAlarmListener alarmListener,OnDestinationAlarmListener destinationAlarmListener ) {
        this.favoriteList = favoriteList;
        this.removeListener = removeListener;
        this.alarmListener = alarmListener;
        this.destinationAlarmListener = destinationAlarmListener;
    }

    @NonNull
    @Override
    public FavoriteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_favorite, parent, false);
        return new FavoriteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FavoriteViewHolder holder, int position) {
        FavoriteItem location = favoriteList.get(position);

        holder.tvLocationName.setText(location.getLocationName());
        holder.tvLocationAddress.setText(location.getLocationAddress());

        holder.btnDelete.setOnClickListener(v -> removeListener.onRemove(location.getLocationName()));

        holder.itemView.setOnClickListener(v -> {
            String locationAddress = holder.tvLocationAddress.getText().toString();

            if(Character.isDigit(locationAddress.charAt(0))){
                try{
                    String[] parts = locationAddress.split(";", 2);
                    destinationAlarmListener.onSetDestination(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]));
                }catch (Exception e){
                    Log.d("error", Objects.requireNonNull(e.getMessage()));
                }
            }else {
                alarmListener.onsetAlarm(locationAddress);
            }
        });
    }

    @Override
    public int getItemCount() {
        return favoriteList.size();
    }

    static class FavoriteViewHolder extends RecyclerView.ViewHolder {
        TextView tvLocationName;
        TextView tvLocationAddress;
        ImageButton btnDelete;

        public FavoriteViewHolder(@NonNull View itemView) {
            super(itemView);
            tvLocationName = itemView.findViewById(R.id.tvLocationName);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            tvLocationAddress = itemView.findViewById(R.id.tvLocationAddress);
        }
    }

    public interface OnFavoriteRemoveListener {
        void onRemove(String location);
    }

    public interface OnSetAlarmListener {
        void onsetAlarm(String location);
    }

    public interface OnDestinationAlarmListener {
        void onSetDestination(double latitude, double longitude);
    }
}