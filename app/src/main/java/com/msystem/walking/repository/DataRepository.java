package com.msystem.walking.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.msystem.walking.model.Activity;
import com.msystem.walking.model.Territory;
import com.msystem.walking.model.User;

import java.util.ArrayList;
import java.util.List;

public class DataRepository {

    // Interface compatível com API 21+ (substitui Consumer)
    public interface Callback<T> {
        void onResult(T result);
    }

    private static DataRepository instance;
    private FirebaseFirestore firestore;
    private MutableLiveData<List<Activity>> activitiesLiveData;
    private MutableLiveData<List<Territory>> territoriesLiveData;
    private MutableLiveData<List<User>> leaderboardLiveData;

    private DataRepository() {
        firestore = FirebaseFirestore.getInstance();
        activitiesLiveData = new MutableLiveData<>();
        territoriesLiveData = new MutableLiveData<>();
        leaderboardLiveData = new MutableLiveData<>();
    }

    public static DataRepository getInstance() {
        if (instance == null) {
            instance = new DataRepository();
        }
        return instance;
    }

    // ==================== TERRITÓRIOS ====================

    /**
     * Salva um território no Firebase
     */
    public void saveTerritory(Territory territory) {
        firestore.collection("territories")
                .add(territory)
                .addOnSuccessListener(documentReference -> {
                    territory.setTerritoryId(documentReference.getId());
                    updateUserTerritoryStats(territory.getOwnerId(), territory.getPointsValue());
                })
                .addOnFailureListener(e -> {
                    // Log erro se necessário
                });
    }

    /**
     * Carrega territórios de um usuário específico
     */
    public void loadUserTerritories(String userId, Callback<List<Territory>> callback) {
        firestore.collection("territories")
                .whereEqualTo("ownerId", userId)
                .orderBy("conqueredAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Territory> territories = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Territory territory = document.toObject(Territory.class);
                        territory.setTerritoryId(document.getId());
                        territories.add(territory);
                    }
                    callback.onResult(territories);
                })
                .addOnFailureListener(e -> callback.onResult(new ArrayList<>()));
    }

    /**
     * Carrega todos os territórios visíveis na área do mapa
     */
    public void loadTerritoriesInArea(double centerLat, double centerLng, double radiusKm, Callback<List<Territory>> callback) {
        firestore.collection("territories")
                .limit(50) // Limitar para performance
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Territory> territories = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Territory territory = document.toObject(Territory.class);
                        territory.setTerritoryId(document.getId());
                        territories.add(territory);
                    }
                    callback.onResult(territories);
                })
                .addOnFailureListener(e -> callback.onResult(new ArrayList<>()));
    }

    /**
     * Atualiza estatísticas de território do usuário
     */
    private void updateUserTerritoryStats(String userId, int pointsEarned) {
        firestore.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            user.setTotalPoints(user.getTotalPoints() + pointsEarned);
                            user.setTerritoriesCount(user.getTerritoriesCount() + 1);

                            firestore.collection("users").document(userId)
                                    .set(user);
                        }
                    }
                });
    }

    // ==================== ATIVIDADES ====================

    /**
     * Salva uma atividade no Firebase
     */
    public void saveActivity(Activity activity) {
        firestore.collection("activities")
                .add(activity)
                .addOnSuccessListener(documentReference -> {
                    activity.setActivityId(documentReference.getId());
                    updateUserActivityStats(activity);
                });
    }

    /**
     * Carrega atividades do usuário
     */
    public void loadUserActivities(String userId, Callback<List<Activity>> callback) {
        firestore.collection("activities")
                .whereEqualTo("userId", userId)
                .orderBy("startTime", Query.Direction.DESCENDING)
                .limit(20)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Activity> activities = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Activity activity = document.toObject(Activity.class);
                        activity.setActivityId(document.getId());
                        activities.add(activity);
                    }
                    callback.onResult(activities);
                })
                .addOnFailureListener(e -> callback.onResult(new ArrayList<>()));
    }

    /**
     * Atualiza estatísticas de atividade do usuário
     */
    private void updateUserActivityStats(Activity activity) {
        firestore.collection("users").document(activity.getUserId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            user.setTotalDistance(user.getTotalDistance() + activity.getDistance());
                            user.setTotalDuration(user.getTotalDuration() + activity.getDuration());

                            firestore.collection("users").document(activity.getUserId())
                                    .set(user);
                        }
                    }
                });
    }

    // ==================== USUÁRIOS E RANKING ====================

    /**
     * Carrega o ranking de usuários por pontos
     */
    public void loadLeaderboard(Callback<List<User>> callback) {
        firestore.collection("users")
                .orderBy("totalPoints", Query.Direction.DESCENDING)
                .limit(20)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<User> users = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        User user = document.toObject(User.class);
                        users.add(user);
                    }
                    callback.onResult(users);
                })
                .addOnFailureListener(e -> callback.onResult(new ArrayList<>()));
    }

    /**
     * Salva ou atualiza dados do usuário
     */
    public void saveUser(User user) {
        firestore.collection("users").document(user.getUserId())
                .set(user);
    }

    /**
     * Carrega dados do usuário
     */
    public void loadUser(String userId, Callback<User> callback) {
        firestore.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        callback.onResult(user);
                    } else {
                        callback.onResult(null);
                    }
                })
                .addOnFailureListener(e -> callback.onResult(null));
    }

    /**
     * Carrega todos os territórios
     */
    public void getAllTerritories(Callback<List<Territory>> callback) {
        firestore.collection("territories")
                .limit(100) // Limitar para performance
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Territory> territories = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Territory territory = document.toObject(Territory.class);
                        territory.setTerritoryId(document.getId());
                        territories.add(territory);
                    }
                    callback.onResult(territories);
                })
                .addOnFailureListener(e -> callback.onResult(new ArrayList<>()));
    }

    /**
     * Carrega todos os usuários para o leaderboard
     */
    public void getAllUsers(Callback<List<User>> callback) {
        firestore.collection("users")
                .orderBy("totalPoints", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<User> users = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        User user = document.toObject(User.class);
                        user.setUserId(document.getId());
                        users.add(user);
                    }
                    callback.onResult(users);
                })
                .addOnFailureListener(e -> callback.onResult(new ArrayList<>()));
    }

    /**
     * Carrega um usuário específico pelo ID
     */
    public void getUserById(String userId, Callback<User> callback) {
        firestore.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            user.setUserId(documentSnapshot.getId());
                        }
                        callback.onResult(user);
                    } else {
                        callback.onResult(null);
                    }
                })
                .addOnFailureListener(e -> callback.onResult(null));
    }

    /**
     * Salva uma conquista/achievement
     */
    public void saveAchievement(com.msystem.walking.model.Achievement achievement) {
        firestore.collection("achievements")
                .add(achievement)
                .addOnSuccessListener(documentReference -> {
                    achievement.setAchievementId(documentReference.getId());
                })
                .addOnFailureListener(e -> {
                    // Log erro se necessário
                });
    }

    // ==================== LIVEDATA GETTERS ====================

    public LiveData<List<Activity>> getActivitiesLiveData() {
        return activitiesLiveData;
    }

    public LiveData<List<Territory>> getTerritoriesLiveData() {
        return territoriesLiveData;
    }

    public LiveData<List<User>> getLeaderboardLiveData() {
        return leaderboardLiveData;
    }
}
