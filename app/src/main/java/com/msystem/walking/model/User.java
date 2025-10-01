package com.msystem.walking.model;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class User {
    private String userId;
    private String email;
    private String displayName;
    private String photoUrl;
    private int totalPoints;
    private double totalDistance;
    private long totalDuration; // em segundos
    private int territoriesCount;
    private Date createdAt;
    private Date lastActiveAt;
    private Map<String, Integer> achievements;
    private int currentStreak; // dias consecutivos
    private int bestStreak;
    private double todayDistance;

    public User() {
        // Construtor vazio necessário para Firebase
        this.achievements = new HashMap<>();
    }

    public User(String userId, String email, String displayName) {
        this.userId = userId;
        this.email = email;
        this.displayName = displayName;
        this.totalPoints = 0;
        this.totalDistance = 0.0;
        this.totalDuration = 0;
        this.territoriesCount = 0;
        this.createdAt = new Date();
        this.lastActiveAt = new Date();
        this.achievements = new HashMap<>();
        this.currentStreak = 0;
        this.bestStreak = 0;
        this.todayDistance = 0.0;
    }

    // Getters e Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

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

    public long getTotalDuration() { return totalDuration; }
    public void setTotalDuration(long totalDuration) { this.totalDuration = totalDuration; }

    public int getTerritoriesCount() { return territoriesCount; }
    public void setTerritoriesCount(int territoriesCount) { this.territoriesCount = territoriesCount; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getLastActiveAt() { return lastActiveAt; }
    public void setLastActiveAt(Date lastActiveAt) { this.lastActiveAt = lastActiveAt; }

    public Map<String, Integer> getAchievements() { return achievements; }
    public void setAchievements(Map<String, Integer> achievements) { this.achievements = achievements; }

    public int getCurrentStreak() { return currentStreak; }
    public void setCurrentStreak(int currentStreak) { this.currentStreak = currentStreak; }

    public int getBestStreak() { return bestStreak; }
    public void setBestStreak(int bestStreak) { this.bestStreak = bestStreak; }

    public double getTodayDistance() { return todayDistance; }
    public void setTodayDistance(double todayDistance) { this.todayDistance = todayDistance; }

    // Métodos utilitários
    public void addPoints(int points) {
        this.totalPoints += points;
    }

    public void addDistance(double distance) {
        this.totalDistance += distance;
        this.todayDistance += distance;
    }

    public void addTerritory() {
        this.territoriesCount++;
    }

    public String getRank() {
        if (totalPoints < 100) return "Iniciante";
        if (totalPoints < 500) return "Caminhante";
        if (totalPoints < 1500) return "Explorador";
        if (totalPoints < 5000) return "Aventureiro";
        return "Lenda";
    }

    public int getLevel() {
        return (totalPoints / 200) + 1;
    }

    public String getId() { return userId; }
    public void setId(String id) { this.userId = id; }

    // Método compatível com MainActivity
    public int getTerritoriesConquered() { return territoriesCount; }
}
