package com.example.waywake;

public class PlaceItem {
    String title;
    String subtitle;
    boolean isHistory;
    String originalText;

    public PlaceItem(String title, String subtitle) {
        this(title, subtitle, false, null);
    }

    public PlaceItem(String title, String subtitle, boolean isHistory, String originalText) {
        this.title = title;
        this.subtitle = subtitle;
        this.isHistory = isHistory;
        this.originalText = originalText;
    }
}

