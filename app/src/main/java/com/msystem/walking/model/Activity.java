package com.msystem.walking.model;

import java.util.Date;
import java.util.List;

public class Activity {
    private String activityId;
    private String userId;
    private String userName;
    private List<LocationPoint> route;
    private double distance; // em quilômetros
    private long duration; // em milissegundos
    private int pointsEarned;
    private Date startTime;
    private Date endTime;
    private String type; // "walking" ou "running"
    private List<String> conqueredTerritoryIds;

    public Activity() {
        // Construtor vazio necessário para Firebase
    }

    public Activity(String userId, String userName, String type) {
        this.userId = userId;
        this.userName = userName;
        this.type = type;
        this.startTime = new Date();
        this.distance = 0;
        this.pointsEarned = 0;
    }

    // Getters e Setters
    public String getActivityId() { return activityId; }
    public void setActivityId(String activityId) { this.activityId = activityId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public List<LocationPoint> getRoute() { return route; }
    public void setRoute(List<LocationPoint> route) { this.route = route; }

    public double getDistance() { return distance; }
    public void setDistance(double distance) { this.distance = distance; }

    public long getDuration() { return duration; }
    public void setDuration(long duration) { this.duration = duration; }

    public int getPointsEarned() { return pointsEarned; }
    public void setPointsEarned(int pointsEarned) { this.pointsEarned = pointsEarned; }

    public Date getStartTime() { return startTime; }
    public void setStartTime(Date startTime) { this.startTime = startTime; }

    public Date getEndTime() { return endTime; }
    public void setEndTime(Date endTime) { this.endTime = endTime; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public List<String> getConqueredTerritoryIds() { return conqueredTerritoryIds; }
    public void setConqueredTerritoryIds(List<String> conqueredTerritoryIds) { this.conqueredTerritoryIds = conqueredTerritoryIds; }

    public double getAverageSpeed() {
        if (duration == 0) return 0.0;
        // Calcular velocidade média em km/h
        double durationInHours = duration / 3600000.0; // converter ms para horas
        return distance / durationInHours;
    }
}
