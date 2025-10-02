package com.msystem.walking.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.msystem.walking.model.Territory;
import com.msystem.walking.model.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TerritoryRepository {

    public interface Callback<T> {
        void onResult(T result);
        void onError(String error);
    }

    private static TerritoryRepository instance;
    private FirebaseFirestore firestore;
    private MutableLiveData<List<Territory>> allTerritoriesLiveData;
    private MutableLiveData<List<User>> leaderboardLiveData;
    private ListenerRegistration territoriesListener;
    private ListenerRegistration leaderboardListener;

    private TerritoryRepository() {
        firestore = FirebaseFirestore.getInstance();
        allTerritoriesLiveData = new MutableLiveData<>();
        leaderboardLiveData = new MutableLiveData<>();
    }

    public static TerritoryRepository getInstance() {
        if (instance == null) {
            instance = new TerritoryRepository();
        }
        return instance;
    }

    /**
     * Obtém LiveData de todos os territórios em tempo real
     */
    public LiveData<List<Territory>> getAllTerritoriesLiveData() {
        if (territoriesListener == null) {
            startTerritoriesListener();
        }
        return allTerritoriesLiveData;
    }

    /**
     * Obtém LiveData do leaderboard em tempo real
     */
    public LiveData<List<User>> getLeaderboardLiveData() {
        if (leaderboardListener == null) {
            startLeaderboardListener();
        }
        return leaderboardLiveData;
    }

    /**
     * Inicia listener em tempo real para todos os territórios
     */
    private void startTerritoriesListener() {
        // ✅ CORRIGIDO: Removendo orderBy que precisa de índice e adicionando tratamento de erro melhor
        territoriesListener = firestore.collection("territories")
                .limit(200) // Limitar para performance
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        // Log do erro para debug
                        android.util.Log.e("TerritoryRepository", "Erro ao carregar territórios: " + error.getMessage());
                        allTerritoriesLiveData.setValue(new ArrayList<>());
                        return;
                    }

                    if (value != null) {
                        List<Territory> territories = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : value) {
                            try {
                                Territory territory = doc.toObject(Territory.class);
                                territory.setTerritoryId(doc.getId());

                                // ✅ Validação adicional antes de adicionar
                                if (territory.getOwnerId() != null && territory.getOwnerName() != null) {
                                    territories.add(territory);
                                }
                            } catch (Exception e) {
                                // Log erro de conversão mas continua processando outros
                                android.util.Log.w("TerritoryRepository", "Erro ao converter território: " + e.getMessage());
                            }
                        }
                        allTerritoriesLiveData.setValue(territories);
                    } else {
                        // Se value é null, retornar lista vazia
                        allTerritoriesLiveData.setValue(new ArrayList<>());
                    }
                });
    }

    /**
     * Inicia listener em tempo real para o leaderboard
     */
    private void startLeaderboardListener() {
        leaderboardListener = firestore.collection("users")
                .orderBy("totalPoints", Query.Direction.DESCENDING)
                .limit(50)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        leaderboardLiveData.setValue(new ArrayList<>());
                        return;
                    }

                    if (value != null) {
                        List<User> users = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : value) {
                            User user = doc.toObject(User.class);
                            user.setUserId(doc.getId());
                            users.add(user);
                        }
                        leaderboardLiveData.setValue(users);
                    }
                });
    }

    /**
     * Salva um novo território
     */
    public void saveTerritory(Territory territory, Callback<String> callback) {
        firestore.collection("territories")
                .add(territory)
                .addOnSuccessListener(documentReference -> {
                    territory.setTerritoryId(documentReference.getId());

                    // Atualizar estatísticas do usuário
                    updateUserTerritoryStats(territory.getOwnerId(), territory.getPointsValue());

                    if (callback != null) {
                        callback.onResult(documentReference.getId());
                    }
                })
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onError("Erro ao salvar território: " + e.getMessage());
                    }
                });
    }

    /**
     * Carrega territórios em uma área específica (para otimização de mapa)
     */
    public void getTerritoriesInArea(double centerLat, double centerLng, double radiusKm, Callback<List<Territory>> callback) {
        // ✅ CORRIGIDO: Melhor tratamento de erro e validação
        firestore.collection("territories")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Territory> territories = new ArrayList<>();

                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            try {
                                Territory territory = document.toObject(Territory.class);
                                territory.setTerritoryId(document.getId());

                                // ✅ Validação antes de verificar área
                                if (territory.getOwnerId() != null && territory.getPolygon() != null) {
                                    // Verificar se está na área (aproximação simples)
                                    if (isInArea(territory, centerLat, centerLng, radiusKm)) {
                                        territories.add(territory);
                                    }
                                }
                            } catch (Exception e) {
                                android.util.Log.w("TerritoryRepository", "Erro ao processar território: " + e.getMessage());
                            }
                        }
                    }

                    callback.onResult(territories);
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("TerritoryRepository", "Erro ao carregar territórios da área: " + e.getMessage());
                    callback.onError("Erro ao carregar territórios: " + e.getMessage());
                });
    }

    /**
     * Verifica se um território está conquistando outro (conflito)
     */
    public void checkTerritoryConflicts(Territory newTerritory, Callback<List<Territory>> callback) {
        firestore.collection("territories")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Territory> conflicts = new ArrayList<>();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Territory existingTerritory = document.toObject(Territory.class);
                        existingTerritory.setTerritoryId(document.getId());

                        // Verificar se há sobreposição
                        if (hasOverlap(newTerritory, existingTerritory)) {
                            conflicts.add(existingTerritory);
                        }
                    }

                    callback.onResult(conflicts);
                })
                .addOnFailureListener(e -> callback.onError("Erro ao verificar conflitos: " + e.getMessage()));
    }

    /**
     * Conquista um território de outro usuário
     */
    public void conquerTerritory(String territoryId, String newOwnerId, String newOwnerName, Callback<Boolean> callback) {
        firestore.collection("territories")
                .document(territoryId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Territory territory = documentSnapshot.toObject(Territory.class);
                        if (territory != null) {
                            String oldOwnerId = territory.getOwnerId();
                            int pointsValue = territory.getPointsValue();

                            // Atualizar território
                            Map<String, Object> updates = new HashMap<>();
                            updates.put("ownerId", newOwnerId);
                            updates.put("ownerName", newOwnerName);
                            updates.put("conqueredAt", new java.util.Date());

                            firestore.collection("territories")
                                    .document(territoryId)
                                    .update(updates)
                                    .addOnSuccessListener(aVoid -> {
                                        // Atualizar pontos: remover do antigo, adicionar ao novo
                                        updateUserTerritoryStats(oldOwnerId, -pointsValue);
                                        updateUserTerritoryStats(newOwnerId, pointsValue);

                                        if (callback != null) {
                                            callback.onResult(true);
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        if (callback != null) {
                                            callback.onError("Erro ao conquistar território: " + e.getMessage());
                                        }
                                    });
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onError("Erro ao acessar território: " + e.getMessage());
                    }
                });
    }

    /**
     * Atualiza estatísticas de território do usuário
     */
    private void updateUserTerritoryStats(String userId, int pointsChange) {
        firestore.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            int newPoints = Math.max(0, user.getTotalPoints() + pointsChange);
                            int newTerritoryCount = pointsChange > 0 ?
                                user.getTerritoriesCount() + 1 :
                                Math.max(0, user.getTerritoriesCount() - 1);

                            Map<String, Object> updates = new HashMap<>();
                            updates.put("totalPoints", newPoints);
                            updates.put("territoriesCount", newTerritoryCount);

                            firestore.collection("users")
                                    .document(userId)
                                    .update(updates);
                        }
                    }
                });
    }

    /**
     * Verifica se um território está dentro de uma área
     */
    private boolean isInArea(Territory territory, double centerLat, double centerLng, double radiusKm) {
        if (territory.getPolygon() == null || territory.getPolygon().isEmpty()) {
            return false;
        }

        // Calcular centro do território
        double territoryLat = territory.getPolygon().stream()
                .mapToDouble(point -> point.getLatitude())
                .average().orElse(0);
        double territoryLng = territory.getPolygon().stream()
                .mapToDouble(point -> point.getLongitude())
                .average().orElse(0);

        // Calcular distância simples (aproximação)
        double distance = Math.sqrt(
                Math.pow(centerLat - territoryLat, 2) +
                Math.pow(centerLng - territoryLng, 2)
        ) * 111; // Conversão aproximada para km

        return distance <= radiusKm;
    }

    /**
     * Verifica se dois territórios têm sobreposição
     */
    private boolean hasOverlap(Territory territory1, Territory territory2) {
        if (territory1.getPolygon() == null || territory2.getPolygon() == null) {
            return false;
        }

        // Implementação simples: verificar se alguns pontos estão dentro
        for (int i = 0; i < Math.min(territory1.getPolygon().size(), 5); i++) {
            if (territory2.containsPoint(territory1.getPolygon().get(i))) {
                return true;
            }
        }

        return false;
    }

    /**
     * Para limpeza de recursos
     */
    public void cleanup() {
        if (territoriesListener != null) {
            territoriesListener.remove();
            territoriesListener = null;
        }
        if (leaderboardListener != null) {
            leaderboardListener.remove();
            leaderboardListener = null;
        }
    }
}
