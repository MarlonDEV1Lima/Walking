package com.msystem.walking.repository;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Looper;

import androidx.core.app.ActivityCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.location.CurrentLocationRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

public class LocationRepository {

    private static final long LOCATION_UPDATE_INTERVAL = 3000; // 3 segundos para mais frequência
    private static final long FASTEST_LOCATION_UPDATE_INTERVAL = 1000; // 1 segundo
    private static final float MINIMUM_DISPLACEMENT = 5f; // 5 metros mínimo de movimento

    private final FusedLocationProviderClient fusedLocationClient;
    private final MutableLiveData<Location> currentLocation = new MutableLiveData<>();
    private final Context context;
    private LocationCallback locationCallback;
    private boolean isRequestingLocationUpdates = false;

    public LocationRepository(Context context) {
        this.context = context;
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        setupLocationCallback();
    }

    private void setupLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }

                // Pegar a localização mais recente e precisa
                Location bestLocation = null;
                for (Location location : locationResult.getLocations()) {
                    if (isBetterLocation(location, bestLocation)) {
                        bestLocation = location;
                    }
                }

                if (bestLocation != null) {
                    currentLocation.setValue(bestLocation);
                }
            }
        };
    }

    public LiveData<Location> getCurrentLocation() {
        return currentLocation;
    }

    public void startLocationUpdates() {
        if (!hasLocationPermission()) {
            return;
        }

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, LOCATION_UPDATE_INTERVAL)
                .setMinUpdateIntervalMillis(FASTEST_LOCATION_UPDATE_INTERVAL)
                .setMinUpdateDistanceMeters(MINIMUM_DISPLACEMENT)
                .setWaitForAccurateLocation(true)
                .setMaxUpdateDelayMillis(LOCATION_UPDATE_INTERVAL * 2)
                .build();

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
            isRequestingLocationUpdates = true;
        } catch (SecurityException e) {
            // Handle permission error
        }
    }

    public void stopLocationUpdates() {
        if (isRequestingLocationUpdates) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            isRequestingLocationUpdates = false;
        }
    }

    public void requestCurrentLocation() {
        if (!hasLocationPermission()) {
            return;
        }

        // Usar getCurrentLocation para obter localização mais precisa
        CurrentLocationRequest currentLocationRequest = new CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setDurationMillis(10000) // 10 segundos para encontrar localização precisa
                .setMaxUpdateAgeMillis(5000) // Aceitar localização de até 5 segundos atrás
                .build();

        try {
            fusedLocationClient.getCurrentLocation(currentLocationRequest, null)
                    .addOnSuccessListener(location -> {
                        if (location != null && isLocationAccurate(location)) {
                            currentLocation.setValue(location);
                        } else {
                            // Fallback para última localização conhecida
                            getLastKnownLocation();
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Fallback para última localização conhecida
                        getLastKnownLocation();
                    });
        } catch (SecurityException e) {
            // Handle permission error
        }
    }

    private void getLastKnownLocation() {
        if (!hasLocationPermission()) {
            return;
        }

        try {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null && isLocationAccurate(location)) {
                            currentLocation.setValue(location);
                        }
                    });
        } catch (SecurityException e) {
            // Handle permission error
        }
    }

    private boolean hasLocationPermission() {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    public boolean isRequestingLocationUpdates() {
        return isRequestingLocationUpdates;
    }

    // Método para determinar se uma localização é melhor que a atual
    private boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // Uma nova localização é sempre melhor que nenhuma localização
            return true;
        }

        // Verificar se a nova localização é muito antiga
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > 2 * 60 * 1000; // 2 minutos
        boolean isSignificantlyOlder = timeDelta < -2 * 60 * 1000;
        boolean isNewer = timeDelta > 0;

        if (isSignificantlyNewer) {
            return true;
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Verificar se a nova localização é mais precisa
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Determinar qualidade da localização usando uma combinação de fatores
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate) {
            return true;
        }
        return false;
    }

    // Verificar se a localização é precisa o suficiente
    private boolean isLocationAccurate(Location location) {
        if (location == null) {
            return false;
        }

        // Verificar precisão (accuracy) - deve ser menor que 50 metros
        if (location.getAccuracy() > 50) {
            return false;
        }

        // Verificar se a localização não é muito antiga (máximo 5 minutos)
        long locationAge = System.currentTimeMillis() - location.getTime();
        if (locationAge > 5 * 60 * 1000) { // 5 minutos
            return false;
        }

        return true;
    }
}
