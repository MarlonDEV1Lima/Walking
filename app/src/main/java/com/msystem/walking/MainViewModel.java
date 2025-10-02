package com.msystem.walking;

import android.app.Application;
import android.location.Location;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.maps.model.LatLng;
import com.msystem.walking.model.Territory;
import com.msystem.walking.model.User;
import com.msystem.walking.model.WalkSession;
import com.msystem.walking.repository.LocationRepository;
import com.msystem.walking.repository.TerritoryRepository;
import com.msystem.walking.repository.UserRepository;
import com.msystem.walking.repository.WalkRepository;

import java.util.List;

public class MainViewModel extends AndroidViewModel {

    private final UserRepository userRepository;
    private final WalkRepository walkRepository;
    private final LocationRepository locationRepository;
    private final TerritoryRepository territoryRepository;

    private final MutableLiveData<User> userData = new MutableLiveData<>();
    private final MutableLiveData<Double> todayDistance = new MutableLiveData<>();
    private final MutableLiveData<Boolean> walkingStatus = new MutableLiveData<>();
    private final MutableLiveData<Integer> territoryProgress = new MutableLiveData<>();

    public MainViewModel(@NonNull Application application) {
        super(application);
        userRepository = UserRepository.getInstance();
        walkRepository = WalkRepository.getInstance();
        locationRepository = new LocationRepository(application);
        territoryRepository = TerritoryRepository.getInstance();
        walkingStatus.setValue(false);
    }

    public LiveData<User> getUserData() {
        return userData;
    }

    public LiveData<Double> getTodayDistance() {
        return todayDistance;
    }

    public LiveData<Boolean> getWalkingStatus() {
        return walkingStatus;
    }

    public LiveData<Integer> getTerritoryProgress() {
        return territoryProgress;
    }

    public LiveData<Location> getCurrentLocation() {
        return locationRepository.getCurrentLocation();
    }

    public void loadUserData(String userId) {userRepository.getUserData(userId, user -> userData.setValue(user));
    }

    public void loadTodayDistance(String userId) {
        walkRepository.getTodayDistance(userId, distance -> todayDistance.setValue(distance));
    }

    public void startWalkSession(WalkSession walkSession) {
        walkRepository.startWalkSession(walkSession);
        walkingStatus.setValue(true);
    }

    public void stopWalkSession(WalkSession walkSession) {
        walkRepository.stopWalkSession(walkSession);
        walkingStatus.setValue(false);
    }

    public void startLocationUpdates() {
        locationRepository.startLocationUpdates();
    }

    public void stopLocationUpdates() {
        locationRepository.stopLocationUpdates();
    }

    public void requestCurrentLocation() {
        locationRepository.requestCurrentLocation();
    }

    public void addWaypoint(LatLng latLng) {
        // Implementar a lógica para adicionar um waypoint
    }

    public void loadUserTerritories() {
        // Implementar a lógica para carregar os territórios do usuário
    }

    public void loadRecentWalks() {
        // Implementar a lógica para carregar as caminhadas recentes
    }

    /**
     * Obtém todos os territórios em tempo real
     */
    public LiveData<List<Territory>> getAllTerritories() {
        return territoryRepository.getAllTerritoriesLiveData();
    }

    /**
     * Obtém leaderboard em tempo real
     */
    public LiveData<List<User>> getLeaderboard() {
        return territoryRepository.getLeaderboardLiveData();
    }

    /**
     * Carrega territórios na área visível do mapa
     */
    public void loadTerritoriesInArea(double centerLat, double centerLng, double radiusKm,
                                      TerritoryRepository.Callback<List<Territory>> callback) {
        territoryRepository.getTerritoriesInArea(centerLat, centerLng, radiusKm, callback);
    }

    /**
     * Salva um novo território
     */
    public void saveTerritory(Territory territory, TerritoryRepository.Callback<String> callback) {
        territoryRepository.saveTerritory(territory, callback);
    }

    /**
     * Verifica conflitos de território
     */
    public void checkTerritoryConflicts(Territory territory, TerritoryRepository.Callback<List<Territory>> callback) {
        territoryRepository.checkTerritoryConflicts(territory, callback);

    }

    /**
     * Conquista um território de outro usuário
     */
    public void conquerTerritory(String territoryId, String newOwnerId, String newOwnerName,
                                 TerritoryRepository.Callback<Boolean> callback) {
        territoryRepository.conquerTerritory(territoryId, newOwnerId, newOwnerName, callback);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        territoryRepository.cleanup();
    }
}
