package com.msystem.walking.model;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class User {
    private String uid;
    private String email;
    private String displayName;
    private String photoUrl;
    private int totalPoints;
    private double totalDistance;
    private int conqueredTerritories;
    private Date createdAt;
    private Map<String, Integer> achievements;

    public User() {
        // Construtor vazio necessário para Firebase
    }

    public User(String uid, String email, String displayName) {
        this.uid = uid;
        this.email = email;
        this.displayName = displayName;
        this.totalPoints = 0;
        this.totalDistance = 0;
        this.conqueredTerritories = 0;
        this.createdAt = new Date();
    }

    // Getters e Setters
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public int getTotalPoints() { return totalPoints; }
    public void setTotalPoints(int totalPoints) { this.totalPoints = totalPoints; }

    public double getTotalDistance() { return totalDistance; }
    public void setTotalDistance(double totalDistance) { this.totalDistance = totalDistance; }

    public int getConqueredTerritories() { return conqueredTerritories; }
    public void setConqueredTerritories(int conqueredTerritories) { this.conqueredTerritories = conqueredTerritories; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Map<String, Integer> getAchievements() { return achievements; }
    public void setAchievements(Map<String, Integer> achievements) { this.achievements = achievements; }
}
