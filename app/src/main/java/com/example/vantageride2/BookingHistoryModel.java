package com.example.vantageride2;

public class BookingHistoryModel {
    private String id;
    private String status;
    private String contactName;
    private String contactPhone;
    private String sourceLocation;
    private String destinationLocation;
    private String pickupTime;

    // 1. ADDED: Empty Constructor
    // This allows you to write: new BookingHistoryModel();
    public BookingHistoryModel() {
    }

    // Existing: Parameterized Constructor
    public BookingHistoryModel(String id, String status, String contactName, String contactPhone,
                               String sourceLocation, String destinationLocation, String pickupTime) {
        this.id = id;
        this.status = status;
        this.contactName = contactName;
        this.contactPhone = contactPhone;
        this.sourceLocation = sourceLocation;
        this.destinationLocation = destinationLocation;
        this.pickupTime = pickupTime;
    }

    // Existing: Getters
    public String getId() { return id; }
    public String getStatus() { return status; }
    public String getContactName() { return contactName; }
    public String getContactPhone() { return contactPhone; }
    public String getSourceLocation() { return sourceLocation; }
    public String getDestinationLocation() { return destinationLocation; }
    public String getPickupTime() { return pickupTime; }

    // 2. ADDED: Setters
    // These allow you to set the data one piece at a time after creating a blank model
    public void setId(String id) {
        this.id = id;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public void setSourceLocation(String sourceLocation) {
        this.sourceLocation = sourceLocation;
    }

    public void setDestinationLocation(String destinationLocation) {
        this.destinationLocation = destinationLocation;
    }

    public void setPickupTime(String pickupTime) {
        this.pickupTime = pickupTime;
    }
}