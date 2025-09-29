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

    // Salvar atividade
    public void saveActivity(Activity activity) {
        firestore.collection("activities")
                .add(activity)
                .addOnSuccessListener(documentReference -> {
                    activity.setActivityId(documentReference.getId());
                    // Atualizar estatísticas do usuário
                    updateUserStats(activity);
                });
    }

    // Atualizar estatísticas do usuário
    private void updateUserStats(Activity activity) {
        firestore.collection("users").document(activity.getUserId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            user.setTotalDistance(user.getTotalDistance() + activity.getDistance());
                            user.setTotalPoints(user.getTotalPoints() + activity.getPointsEarned());
                            if (activity.getConqueredTerritoryIds() != null) {
                                user.setConqueredTerritories(user.getConqueredTerritories() +
                                    activity.getConqueredTerritoryIds().size());
                            }

                            firestore.collection("users").document(user.getUid()).set(user);
                        }
                    }
                });
    }

    // Salvar território
    public void saveTerritory(Territory territory) {
        firestore.collection("territories")
                .add(territory)
                .addOnSuccessListener(documentReference -> {
                    territory.setTerritoryId(documentReference.getId());
                });
    }

    // Buscar atividades do usuário
    public void getUserActivities(String userId) {
        firestore.collection("activities")
                .whereEqualTo("userId", userId)
                .orderBy("startTime", Query.Direction.DESCENDING)
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        return;
                    }

                    List<Activity> activities = new ArrayList<>();
                    if (queryDocumentSnapshots != null) {
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            Activity activity = doc.toObject(Activity.class);
                            activity.setActivityId(doc.getId());
                            activities.add(activity);
                        }
                    }
                    activitiesLiveData.postValue(activities);
                });
    }

    // Buscar territórios na região
    public void getTerritoriesInRegion(String region) {
        Query query = firestore.collection("territories");
        if (region != null && !region.isEmpty()) {
            query = query.whereEqualTo("region", region);
        }

        query.addSnapshotListener((queryDocumentSnapshots, e) -> {
            if (e != null) {
                return;
            }

            List<Territory> territories = new ArrayList<>();
            if (queryDocumentSnapshots != null) {
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    Territory territory = doc.toObject(Territory.class);
                    territory.setTerritoryId(doc.getId());
                    territories.add(territory);
                }
            }
            territoriesLiveData.postValue(territories);
        });
    }

    // Buscar leaderboard
    public void getLeaderboard(int limit) {
        firestore.collection("users")
                .orderBy("totalPoints", Query.Direction.DESCENDING)
                .limit(limit)
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        return;
                    }

                    List<User> users = new ArrayList<>();
                    if (queryDocumentSnapshots != null) {
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            User user = doc.toObject(User.class);
                            users.add(user);
                        }
                    }
                    leaderboardLiveData.postValue(users);
                });
    }

    // LiveData getters
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
