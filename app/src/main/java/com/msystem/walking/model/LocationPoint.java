package com.msystem.walking.model;

import java.util.Date;

public class LocationPoint {
    private double latitude;
    private double longitude;
    private Date timestamp;
    private double altitude;
    private float accuracy;

    public LocationPoint() {
        // Construtor vazio necessário para Firebase
    }

    public LocationPoint(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = new Date();
    }

    public LocationPoint(double latitude, double longitude, double altitude, float accuracy) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
        this.accuracy = accuracy;
        this.timestamp = new Date();
    }

    // Getters e Setters
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }

    public double getAltitude() { return altitude; }
    public void setAltitude(double altitude) { this.altitude = altitude; }

    public float getAccuracy() { return accuracy; }
    public void setAccuracy(float accuracy) { this.accuracy = accuracy; }
}
