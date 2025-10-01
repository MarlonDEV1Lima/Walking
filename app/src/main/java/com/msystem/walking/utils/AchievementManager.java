package com.msystem.walking.utils;

import com.msystem.walking.model.Achievement;
import com.msystem.walking.model.Activity;
import com.msystem.walking.model.User;
import com.msystem.walking.repository.DataRepository;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class AchievementManager {
    private static final String TAG = "AchievementManager";

    private DataRepository dataRepository;
    private OnAchievementUnlockedListener listener;

    public interface OnAchievementUnlockedListener {
        void onAchievementUnlocked(Achievement achievement);
    }

    public AchievementManager(DataRepository dataRepository) {
        this.dataRepository = dataRepository;
    }

    public void setOnAchievementUnlockedListener(OnAchievementUnlockedListener listener) {
        this.listener = listener;
    }

    /**
     * Verifica se novas conquistas foram desbloqueadas após uma atividade
     */
    public void checkAchievements(User user, Activity activity) {
        List<Achievement> newAchievements = new ArrayList<>();

        // Verificar conquistas de distância
        checkDistanceAchievements(user, newAchievements);

        // Verificar conquistas de território
        checkTerritoryAchievements(user, newAchievements);

        // Verificar conquistas de sequência
        checkStreakAchievements(user, newAchievements);

        // Verificar conquistas especiais baseadas na atividade atual
        checkSpecialAchievements(user, activity, newAchievements);

        // Notificar e salvar novas conquistas
        for (Achievement achievement : newAchievements) {
            achievement.setUnlocked(true);
            dataRepository.saveAchievement(achievement);

            // Adicionar pontos de bônus ao usuário
            user.setTotalPoints(user.getTotalPoints() + achievement.getPointsReward());

            if (listener != null) {
                listener.onAchievementUnlocked(achievement);
            }
        }

        if (!newAchievements.isEmpty()) {
            dataRepository.saveUser(user);
        }
    }

    private void checkDistanceAchievements(User user, List<Achievement> newAchievements) {
        double totalDistance = user.getTotalDistance();

        // Primeira caminhada
        if (totalDistance > 0 && !hasAchievement(user, "Primeiro Passo")) {
            newAchievements.add(createAchievement(user.getUserId(), "Primeiro Passo"));
        }

        // 5 km total
        if (totalDistance >= 5.0 && !hasAchievement(user, "Caminhante")) {
            newAchievements.add(createAchievement(user.getUserId(), "Caminhante"));
        }

        // 25 km total
        if (totalDistance >= 25.0 && !hasAchievement(user, "Explorador")) {
            newAchievements.add(createAchievement(user.getUserId(), "Explorador"));
        }

        // 100 km total
        if (totalDistance >= 100.0 && !hasAchievement(user, "Aventureiro")) {
            newAchievements.add(createAchievement(user.getUserId(), "Aventureiro"));
        }

        // 500 km total
        if (totalDistance >= 500.0 && !hasAchievement(user, "Maratonista")) {
            newAchievements.add(createAchievement(user.getUserId(), "Maratonista"));
        }
    }

    private void checkTerritoryAchievements(User user, List<Achievement> newAchievements) {
        int territoriesCount = user.getTerritoriesCount();

        // Primeiro território
        if (territoriesCount >= 1 && !hasAchievement(user, "Conquistador")) {
            newAchievements.add(createAchievement(user.getUserId(), "Conquistador"));
        }

        // 5 territórios
        if (territoriesCount >= 5 && !hasAchievement(user, "Senhor da Terra")) {
            newAchievements.add(createAchievement(user.getUserId(), "Senhor da Terra"));
        }

        // 25 territórios
        if (territoriesCount >= 25 && !hasAchievement(user, "Imperador")) {
            newAchievements.add(createAchievement(user.getUserId(), "Imperador"));
        }

        // 100 territórios
        if (territoriesCount >= 100 && !hasAchievement(user, "Dominador")) {
            newAchievements.add(createAchievement(user.getUserId(), "Dominador"));
        }
    }

    private void checkStreakAchievements(User user, List<Achievement> newAchievements) {
        int currentStreak = user.getCurrentStreak();

        // 3 dias consecutivos
        if (currentStreak >= 3 && !hasAchievement(user, "Consistente")) {
            newAchievements.add(createAchievement(user.getUserId(), "Consistente"));
        }

        // 7 dias consecutivos
        if (currentStreak >= 7 && !hasAchievement(user, "Dedicado")) {
            newAchievements.add(createAchievement(user.getUserId(), "Dedicado"));
        }

        // 30 dias consecutivos
        if (currentStreak >= 30 && !hasAchievement(user, "Disciplinado")) {
            newAchievements.add(createAchievement(user.getUserId(), "Disciplinado"));
        }

        // 100 dias consecutivos
        if (currentStreak >= 100 && !hasAchievement(user, "Lendário")) {
            newAchievements.add(createAchievement(user.getUserId(), "Lendário"));
        }
    }

    private void checkSpecialAchievements(User user, Activity activity, List<Achievement> newAchievements) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(activity.getStartTime());
        int hour = calendar.get(Calendar.HOUR_OF_DAY);

        // Madrugador (antes das 6h)
        if (hour < 6 && !hasAchievement(user, "Madrugador")) {
            newAchievements.add(createAchievement(user.getUserId(), "Madrugador"));
        }

        // Noturno (depois das 22h)
        if (hour >= 22 && !hasAchievement(user, "Noturno")) {
            newAchievements.add(createAchievement(user.getUserId(), "Noturno"));
        }

        // Velocista (velocidade média > 6 km/h)
        if (activity.getAverageSpeed() > 6.0 && !hasAchievement(user, "Velocista")) {
            newAchievements.add(createAchievement(user.getUserId(), "Velocista"));
        }

        // Resistente (mais de 2 horas)
        long durationHours = activity.getDuration() / 3600000; // converter ms para horas
        if (durationHours >= 2 && !hasAchievement(user, "Resistente")) {
            newAchievements.add(createAchievement(user.getUserId(), "Resistente"));
        }
    }

    private boolean hasAchievement(User user, String achievementTitle) {
        // Verificar no Firebase se o usuário já tem essa conquista
        // Por simplicidade, assumindo que temos uma lista de conquistas no User
        return user.getAchievements() != null && user.getAchievements().containsKey(achievementTitle);
    }

    private Achievement createAchievement(String userId, String title) {
        Achievement[] defaultAchievements = Achievement.getDefaultAchievements(userId);

        for (Achievement achievement : defaultAchievements) {
            if (achievement.getTitle().equals(title)) {
                return achievement;
            }
        }

        return null;
    }

    /**
     * Calcula o nível do usuário baseado nos pontos totais
     */
    public static int calculateUserLevel(int totalPoints) {
        if (totalPoints < 100) return 1;
        if (totalPoints < 300) return 2;
        if (totalPoints < 600) return 3;
        if (totalPoints < 1000) return 4;
        if (totalPoints < 1500) return 5;
        if (totalPoints < 2500) return 6;
        if (totalPoints < 4000) return 7;
        if (totalPoints < 6000) return 8;
        if (totalPoints < 9000) return 9;
        if (totalPoints < 12000) return 10;

        // Níveis avançados (acima do nível 10)
        return 10 + (totalPoints - 12000) / 2000;
    }

    /**
     * Calcula os pontos necessários para o próximo nível
     */
    public static int getPointsForNextLevel(int totalPoints) {
        int currentLevel = calculateUserLevel(totalPoints);
        int nextLevel = currentLevel + 1;

        if (nextLevel <= 1) return 100;
        if (nextLevel <= 2) return 300;
        if (nextLevel <= 3) return 600;
        if (nextLevel <= 4) return 1000;
        if (nextLevel <= 5) return 1500;
        if (nextLevel <= 6) return 2500;
        if (nextLevel <= 7) return 4000;
        if (nextLevel <= 8) return 6000;
        if (nextLevel <= 9) return 9000;
        if (nextLevel <= 10) return 12000;

        // Níveis avançados
        return 12000 + (nextLevel - 10) * 2000;
    }
}
