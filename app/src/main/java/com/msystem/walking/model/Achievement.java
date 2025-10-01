package com.msystem.walking.model;

import java.util.Date;

public class Achievement {
    private String achievementId;
    private String userId;
    private String title;
    private String description;
    private String iconName;
    private int pointsReward;
    private Date unlockedAt;
    private AchievementType type;
    private boolean isUnlocked;

    public enum AchievementType {
        DISTANCE("Distância"),
        TERRITORY("Território"),
        STREAK("Sequência"),
        SPECIAL("Especial");

        private final String displayName;

        AchievementType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public Achievement() {
        // Construtor vazio para Firebase
    }

    public Achievement(String userId, String title, String description, AchievementType type, int pointsReward) {
        this.userId = userId;
        this.title = title;
        this.description = description;
        this.type = type;
        this.pointsReward = pointsReward;
        this.isUnlocked = false;
    }

    // Getters e Setters
    public String getAchievementId() { return achievementId; }
    public void setAchievementId(String achievementId) { this.achievementId = achievementId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIconName() { return iconName; }
    public void setIconName(String iconName) { this.iconName = iconName; }

    public int getPointsReward() { return pointsReward; }
    public void setPointsReward(int pointsReward) { this.pointsReward = pointsReward; }

    public Date getUnlockedAt() { return unlockedAt; }
    public void setUnlockedAt(Date unlockedAt) { this.unlockedAt = unlockedAt; }

    public AchievementType getType() { return type; }
    public void setType(AchievementType type) { this.type = type; }

    public boolean isUnlocked() { return isUnlocked; }
    public void setUnlocked(boolean unlocked) {
        this.isUnlocked = unlocked;
        if (unlocked && unlockedAt == null) {
            this.unlockedAt = new Date();
        }
    }

    /**
     * Lista de conquistas padrão do sistema
     */
    public static Achievement[] getDefaultAchievements(String userId) {
        return new Achievement[]{
            // Conquistas de Distância
            new Achievement(userId, "Primeiro Passo", "Complete sua primeira caminhada", AchievementType.DISTANCE, 50),
            new Achievement(userId, "Caminhante", "Percorra 5 km em total", AchievementType.DISTANCE, 100),
            new Achievement(userId, "Explorador", "Percorra 25 km em total", AchievementType.DISTANCE, 250),
            new Achievement(userId, "Aventureiro", "Percorra 100 km em total", AchievementType.DISTANCE, 500),
            new Achievement(userId, "Maratonista", "Percorra 500 km em total", AchievementType.DISTANCE, 1000),

            // Conquistas de Território
            new Achievement(userId, "Conquistador", "Conquiste seu primeiro território", AchievementType.TERRITORY, 100),
            new Achievement(userId, "Senhor da Terra", "Conquiste 5 territórios", AchievementType.TERRITORY, 300),
            new Achievement(userId, "Imperador", "Conquiste 25 territórios", AchievementType.TERRITORY, 750),
            new Achievement(userId, "Dominador", "Conquiste 100 territórios", AchievementType.TERRITORY, 1500),

            // Conquistas de Sequência
            new Achievement(userId, "Consistente", "Caminhe por 3 dias consecutivos", AchievementType.STREAK, 150),
            new Achievement(userId, "Dedicado", "Caminhe por 7 dias consecutivos", AchievementType.STREAK, 350),
            new Achievement(userId, "Disciplinado", "Caminhe por 30 dias consecutivos", AchievementType.STREAK, 1000),
            new Achievement(userId, "Lendário", "Caminhe por 100 dias consecutivos", AchievementType.STREAK, 2500),

            // Conquistas Especiais
            new Achievement(userId, "Madrugador", "Complete uma caminhada antes das 6h", AchievementType.SPECIAL, 200),
            new Achievement(userId, "Noturno", "Complete uma caminhada depois das 22h", AchievementType.SPECIAL, 200),
            new Achievement(userId, "Velocista", "Mantenha velocidade média acima de 6 km/h", AchievementType.SPECIAL, 300),
            new Achievement(userId, "Resistente", "Complete uma caminhada de mais de 2 horas", AchievementType.SPECIAL, 400)
        };
    }
}
