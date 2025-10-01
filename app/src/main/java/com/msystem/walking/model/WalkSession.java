package com.msystem.walking.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class WalkSession implements Parcelable {
    private String id;
    private String userId;
    private Date startTime;
    private Date endTime;
    private Double distance; // em quilômetros
    private Long duration; // em segundos
    private Integer steps;
    private Integer calories;
    private Integer points;
    private List<LatLng> route;
    private List<LatLng> waypoints;
    private String status; // "active", "paused", "completed"
    private Double averageSpeed; // km/h
    private Double maxSpeed; // km/h
    private Integer territoriesConquered;

    public WalkSession() {
        // Construtor vazio necessário para Firebase
        this.route = new ArrayList<>();
        this.waypoints = new ArrayList<>();
        this.status = "active";
        this.distance = 0.0;
        this.duration = 0L;
        this.steps = 0;
        this.calories = 0;
        this.points = 0;
        this.territoriesConquered = 0;
    }

    public WalkSession(String userId) {
        this();
        this.userId = userId;
        this.startTime = new Date();
    }

    // Construtor para Parcelable
    protected WalkSession(Parcel in) {
        id = in.readString();
        userId = in.readString();
        startTime = new Date(in.readLong());
        long endTimeLong = in.readLong();
        endTime = endTimeLong != -1 ? new Date(endTimeLong) : null;
        distance = in.readDouble();
        duration = in.readLong();
        steps = in.readInt();
        calories = in.readInt();
        points = in.readInt();
        route = in.createTypedArrayList(LatLng.CREATOR);
        waypoints = in.createTypedArrayList(LatLng.CREATOR);
        status = in.readString();
        averageSpeed = in.readDouble();
        maxSpeed = in.readDouble();
        territoriesConquered = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(userId);
        dest.writeLong(startTime != null ? startTime.getTime() : -1);
        dest.writeLong(endTime != null ? endTime.getTime() : -1);
        dest.writeDouble(distance != null ? distance : 0.0);
        dest.writeLong(duration != null ? duration : 0L);
        dest.writeInt(steps != null ? steps : 0);
        dest.writeInt(calories != null ? calories : 0);
        dest.writeInt(points != null ? points : 0);
        dest.writeTypedList(route);
        dest.writeTypedList(waypoints);
        dest.writeString(status);
        dest.writeDouble(averageSpeed != null ? averageSpeed : 0.0);
        dest.writeDouble(maxSpeed != null ? maxSpeed : 0.0);
        dest.writeInt(territoriesConquered != null ? territoriesConquered : 0);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<WalkSession> CREATOR = new Creator<WalkSession>() {
        @Override
        public WalkSession createFromParcel(Parcel in) {
            return new WalkSession(in);
        }

        @Override
        public WalkSession[] newArray(int size) {
            return new WalkSession[size];
        }
    };

    // Getters e Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public Date getStartTime() { return startTime; }
    public void setStartTime(Date startTime) { this.startTime = startTime; }

    public Date getEndTime() { return endTime; }
    public void setEndTime(Date endTime) { this.endTime = endTime; }

    public Double getDistance() { return distance; }
    public void setDistance(Double distance) { this.distance = distance; }

    public Long getDuration() { return duration; }
    public void setDuration(Long duration) { this.duration = duration; }

    public Integer getSteps() { return steps; }
    public void setSteps(Integer steps) { this.steps = steps; }

    public Integer getCalories() { return calories; }
    public void setCalories(Integer calories) { this.calories = calories; }

    public Integer getPoints() { return points; }
    public void setPoints(Integer points) { this.points = points; }

    public List<LatLng> getRoute() { return route; }
    public void setRoute(List<LatLng> route) { this.route = route; }

    public List<LatLng> getWaypoints() { return waypoints; }
    public void setWaypoints(List<LatLng> waypoints) { this.waypoints = waypoints; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Double getAverageSpeed() { return averageSpeed; }
    public void setAverageSpeed(Double averageSpeed) { this.averageSpeed = averageSpeed; }

    public Double getMaxSpeed() { return maxSpeed; }
    public void setMaxSpeed(Double maxSpeed) { this.maxSpeed = maxSpeed; }

    public Integer getTerritoriesConquered() { return territoriesConquered; }
    public void setTerritoriesConquered(Integer territoriesConquered) { this.territoriesConquered = territoriesConquered; }

    // Métodos utilitários
    public void addWaypoint(LatLng waypoint) {
        if (waypoints == null) {
            waypoints = new ArrayList<>();
        }
        waypoints.add(waypoint);
    }

    public void addRoutePoint(LatLng point) {
        if (route == null) {
            route = new ArrayList<>();
        }
        route.add(point);
    }

    public void calculateDuration() {
        if (startTime != null && endTime != null) {
            duration = (endTime.getTime() - startTime.getTime()) / 1000; // em segundos
        }
    }

    public void calculateAverageSpeed() {
        if (duration != null && duration > 0 && distance != null) {
            averageSpeed = (distance * 3600) / duration; // km/h
        }
    }

    public boolean isActive() {
        return "active".equals(status);
    }

    public boolean isCompleted() {
        return "completed".equals(status);
    }

    public void complete() {
        this.status = "completed";
        this.endTime = new Date();
        calculateDuration();
        calculateAverageSpeed();
    }
}
