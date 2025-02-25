package com.example.waywake;

public class FavoriteItem {
    private String locationName;
    private String locationAddress;

    public FavoriteItem(String locationName, String locationAddress) {
        this.locationName = locationName;
        this.locationAddress = locationAddress;
    }

    public String getLocationName() {
        return locationName;
    }

    public String getLocationAddress() {
        return locationAddress;
    }
}
