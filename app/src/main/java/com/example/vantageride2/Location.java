package com.example.vantageride2;

public class Location {
    private String id;
    private String cityName;

    public Location(String id, String cityName) {
        this.id = id;
        this.cityName = cityName;
    }

    public String getId() { return id; }
    public String getCityName() { return cityName; }

    // This is required so the AutoCompleteTextView shows the city name!
    @Override
    public String toString() {
        return cityName;
    }
}