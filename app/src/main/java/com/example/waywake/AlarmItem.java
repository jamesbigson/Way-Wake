package com.example.waywake;

public class AlarmItem {
    private String location;
    private long timestamp;

    public AlarmItem(String location, long timestamp) {
        this.location = location;
        this.timestamp = timestamp;
    }

    public String getLocation() {
        return location;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
