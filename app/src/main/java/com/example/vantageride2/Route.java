package com.example.vantageride2;

public class Route {
    private String id;
    private String sourceCity;
    private String destCity;
    private String km;
    private double price4;
    private double price6;
    private String driverId;

    // 🚀 FIXED: Added driverId as the 7th parameter to match your fragment
    public Route(String id, String sourceCity, String destCity, String km, double price4, double price6, String driverId) {
        this.id = id;
        this.sourceCity = sourceCity;
        this.destCity = destCity;
        this.km = km;
        this.price4 = price4;
        this.price6 = price6;
        this.driverId = driverId; // 🚀 FIXED: Changed from "role" to "driverId"
    }

    public String getId() { return id; }
    public String getSource() { return sourceCity; }
    public String getDest() { return destCity; }
    public String getKm() { return km; }
    public double getPrice4() { return price4; }
    public double getPrice6() { return price6; }
    public String getDriverId() { return driverId; }
}