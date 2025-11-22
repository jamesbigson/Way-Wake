package com.example.waywake;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

//public class SuggestionAdapter extends RecyclerView.Adapter<SuggestionAdapter.ViewHolder> {
//
//    private List<String> items;
//    private OnItemClickListener listener;
//
//    public interface OnItemClickListener {
//        void onItemClick(String item);
//    }
//
//    public SuggestionAdapter(List<String> items, OnItemClickListener listener) {
//        this.items = items;
//        this.listener = listener;
//    }
//
//    @NonNull
//    @Override
//    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//        View view = LayoutInflater.from(parent.getContext())
//                .inflate(android.R.layout.simple_list_item_1, parent, false);
//        return new ViewHolder(view);
//    }
//
//    @Override
//    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
//        String text = items.get(position);
//        holder.textView.setText(text);
//        holder.itemView.setOnClickListener(v -> listener.onItemClick(text));
//    }
//
//    @Override
//    public int getItemCount() {
//        return items.size();
//    }
//
//    public void updateList(List<String> newList) {
//        items = newList;
//        notifyDataSetChanged();
//    }
//
//    public static class ViewHolder extends RecyclerView.ViewHolder {
//        TextView textView;
//
//        public ViewHolder(@NonNull View itemView) {
//            super(itemView);
//            textView = itemView.findViewById(android.R.id.text1);
//        }
//    }
//}


public class SuggestionAdapter extends RecyclerView.Adapter<SuggestionAdapter.PlaceViewHolder> {

    private List<PlaceItem> placeList;
    private OnPlaceClick listener;

    public interface OnPlaceClick {
        void onPlaceSelected(PlaceItem item);
    }

    public SuggestionAdapter(List<PlaceItem> placeList, OnPlaceClick listener) {
        this.placeList = placeList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PlaceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_place, parent, false);
        return new PlaceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaceViewHolder holder, int position) {
        PlaceItem item = placeList.get(position);
        holder.title.setText(item.title);
        holder.subtitle.setText(item.subtitle);

        holder.itemView.setOnClickListener(v -> listener.onPlaceSelected(item));
    }

    @Override
    public int getItemCount() {
        return placeList.size();
    }

    public static class PlaceViewHolder extends RecyclerView.ViewHolder {
        TextView title, subtitle;

        public PlaceViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.placeTitle);
            subtitle = itemView.findViewById(R.id.placeSubtitle);
        }
    }
}

