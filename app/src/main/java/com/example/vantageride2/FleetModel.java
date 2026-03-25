package com.example.vantageride2;

public class FleetModel {
    private String id;
    private String driverId; // 🚀 Added
    private String name;
    private String type;     // 🚀 Added
    private String description;
    private String imageUrl;
    private double ratePerKm;

    // 🚀 Updated constructor to include driverId and type
    public FleetModel(String id, String driverId, String name, String type, String description, String imageUrl, double ratePerKm) {
        this.id = id;
        this.driverId = driverId;
        this.name = name;
        this.type = type;
        this.description = description;
        this.imageUrl = imageUrl;
        this.ratePerKm = ratePerKm;
    }

    // Getters
    public String getId() { return id; }
    public String getDriverId() { return driverId; } // 🚀 Solves 'getDriverId' error
    public String getName() { return name; }
    public String getType() { return type; }         // 🚀 Solves 'getType' error
    public String getDescription() { return description; }
    public String getImageUrl() { return imageUrl; }
    public double getRatePerKm() { return ratePerKm; }
}