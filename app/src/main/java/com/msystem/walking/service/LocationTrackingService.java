package com.msystem.walking.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import com.msystem.walking.MainActivity;
import com.msystem.walking.R;
import com.msystem.walking.model.LocationPoint;

import java.util.ArrayList;
import java.util.List;

public class LocationTrackingService extends Service {
    private static final String CHANNEL_ID = "LocationTrackingChannel";
    private static final int NOTIFICATION_ID = 1;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;

    private List<LocationPoint> routePoints = new ArrayList<>();
    private double totalDistance = 0.0;
    private Location lastLocation;
    private boolean isTracking = false;

    private final IBinder binder = new LocationBinder();

    public interface LocationUpdateListener {
        void onLocationUpdate(LocationPoint point, double totalDistance);
    }

    private LocationUpdateListener locationUpdateListener;

    public class LocationBinder extends Binder {
        public LocationTrackingService getService() {
            return LocationTrackingService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        createLocationRequest();
        createLocationCallback();
        createNotificationChannel();
    }

    private void createLocationRequest() {
        locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(2000)
                .setMaxUpdateDelayMillis(10000)
                .build();
    }

    private void createLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                if (locationResult == null) {
                    return;
                }

                for (Location location : locationResult.getLocations()) {
                    if (isTracking) {
                        LocationPoint point = new LocationPoint(
                                location.getLatitude(),
                                location.getLongitude(),
                                location.getAltitude(),
                                location.getAccuracy()
                        );

                        routePoints.add(point);

                        // Calcular distância
                        if (lastLocation != null) {
                            double distance = calculateDistance(lastLocation, location);
                            totalDistance += distance;
                        }

                        lastLocation = location;

                        // Notificar listener
                        if (locationUpdateListener != null) {
                            locationUpdateListener.onLocationUpdate(point, totalDistance);
                        }

                        // Atualizar notificação
                        updateNotification();
                    }
                }
            }
        };
    }

    private double calculateDistance(Location start, Location end) {
        float[] results = new float[1];
        Location.distanceBetween(
                start.getLatitude(), start.getLongitude(),
                end.getLatitude(), end.getLongitude(),
                results
        );
        return results[0] / 1000.0; // Converter para km
    }

    public void startTracking() {
        isTracking = true;
        routePoints.clear();
        totalDistance = 0.0;
        lastLocation = null;

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
            startForeground(NOTIFICATION_ID, createNotification());
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    public void stopTracking() {
        isTracking = false;
        fusedLocationClient.removeLocationUpdates(locationCallback);
        stopForeground(true);
    }

    public List<LocationPoint> getRoutePoints() {
        return new ArrayList<>(routePoints);
    }

    public double getTotalDistance() {
        return totalDistance;
    }

    public void setLocationUpdateListener(LocationUpdateListener listener) {
        this.locationUpdateListener = listener;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Rastreamento de Localização",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Rastreando atividade")
                .setContentText(String.format("Distância: %.2f km", totalDistance))
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .build();
    }

    private void updateNotification() {
        // Verificar se temos permissão para postar notificações (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // Sem permissão, não mostrar notificação
                return;
            }
        }

        Notification notification = createNotification();
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (isTracking) {
            stopTracking();
        }
    }
}
