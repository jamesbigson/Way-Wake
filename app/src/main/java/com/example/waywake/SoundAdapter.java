package com.example.waywake;

import static android.content.Context.MODE_PRIVATE;

import static com.example.waywake.SelectAlarmSound.DEFAULT_SOUNDS;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SoundAdapter extends RecyclerView.Adapter<SoundAdapter.ViewHolder> {

    private Context context;
    private List<String> soundList;
    private int selectedPosition = 1; // To keep track of selected radio button
    private MediaPlayer mediaPlayer;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    String selectedAlarmSound;
    private static final String PREFS_NAME = "user_settings";
    private static final String KEY_ALARM_SOUND = "alarm_sound";
    private static final String KEY_SOUND_LIST = "sound_LIST";
    private static final String KEY_ALARM_SOUND_POSITION = "alarm_sound_position";

    public SoundAdapter(Context context, List<String> soundList) {
        this.context = context;
        this.soundList = soundList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_sound, parent, false);

        loadPreferences();
        return new ViewHolder(view);
    }


    @SuppressLint({"NotifyDataSetChanged", "SetTextI18n"})
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        String soundName = soundList.get(position);

        if(soundName.equals("Silent")){
            holder.soundName.setText("Silent");
            holder.radioButton.setChecked(position == selectedPosition );

            holder.itemView.setOnClickListener(v -> {
                selectedPosition = position;
                notifyDataSetChanged();

                // Play the selected sound
                if (mediaPlayer != null) {
                    mediaPlayer.stop();
                }
            });

            holder.radioButton.setOnClickListener(v -> {
                selectedPosition = position;
                notifyDataSetChanged();
            });

        } else if (soundName.startsWith("content://")) {
            holder.soundName.setText(getFileNameFromUri(Uri.parse(soundName)));

            holder.radioButton.setChecked(position == selectedPosition);

            // Handle item selection
            holder.itemView.setOnClickListener(v -> {
                selectedPosition = position;
                notifyDataSetChanged();

                // Play the selected sound
                if (mediaPlayer != null) {
                    mediaPlayer.stop();
                    mediaPlayer.release();
                }

                mediaPlayer = MediaPlayer.create(context, Uri.parse(soundName));
                mediaPlayer.start();
            });

            holder.itemView.setOnLongClickListener(v -> {
                if(position >= DEFAULT_SOUNDS) {
                    showRemoveDialog(position,soundName);
                }
                return true;
            });

            holder.radioButton.setOnClickListener(v -> {
                selectedPosition = position;
                notifyDataSetChanged();
            });


        } else {

            holder.soundName.setText(soundName);
            holder.radioButton.setChecked(position == selectedPosition);

            // Handle item selection
            holder.itemView.setOnClickListener(v -> {
                selectedPosition = position;
                notifyDataSetChanged();

                // Play the selected sound
                if (mediaPlayer != null) {
                    mediaPlayer.stop();
                    mediaPlayer.release();
                }
                @SuppressLint("DiscouragedApi")
                int soundResId = context.getResources().getIdentifier(soundName, "raw", context.getPackageName());
                mediaPlayer = MediaPlayer.create(context, soundResId);

                mediaPlayer.start();
            });

            holder.itemView.setOnLongClickListener(v -> {
                if(position >= DEFAULT_SOUNDS) {
                    showRemoveDialog(position,soundName);
                }
                return true;
            });

            holder.radioButton.setOnClickListener(v -> {
                selectedPosition = position;
                notifyDataSetChanged();
            });


        }

        editor.putString(KEY_ALARM_SOUND, this.getSelectedSound() );
        editor.putString(KEY_ALARM_SOUND_POSITION, String.valueOf(selectedPosition));
        editor.apply();
    }

    @Override
    public int getItemCount() {
        return soundList.size();
    }

    private void showRemoveDialog(int position,String itemName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Remove Sound")
                .setMessage("Do you want to remove this sound?")
                .setPositiveButton("Remove", (dialog, which) -> {
                    soundList.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, soundList.size());
                    removeItem(itemName);
                    Toast.makeText(context, "Sound removed", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }

    @SuppressLint("MutatingSharedPrefs")
    public void removeItem(String item){
        Set<String> soundSet = sharedPreferences.getStringSet(KEY_SOUND_LIST , new HashSet<>());

        soundSet.remove(item);
        editor.putStringSet(KEY_SOUND_LIST, soundSet);
        editor.apply();
    }


    public String getSelectedSound() {
        return selectedPosition != -1 ? soundList.get(selectedPosition) : null;
    }

    public MediaPlayer getMediaPlayer(){
        return mediaPlayer;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView soundName;
        RadioButton radioButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            soundName = itemView.findViewById(R.id.sound_name);
            radioButton = itemView.findViewById(R.id.radio_button);
        }
    }

    private void loadPreferences(){
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        editor = sharedPreferences.edit();
        selectedAlarmSound = sharedPreferences.getString(KEY_ALARM_SOUND, "");

        selectedPosition = Integer.parseInt(sharedPreferences.getString(KEY_ALARM_SOUND_POSITION, "1"));
        if(selectedPosition >= getItemCount()){
            selectedPosition = 1;
        }
    }

    private String getFileNameFromUri(Uri uri) {
        String fileName = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
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
