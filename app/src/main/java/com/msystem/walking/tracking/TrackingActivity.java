package com.msystem.walking.tracking;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;

import com.msystem.walking.R;
import com.msystem.walking.databinding.ActivityTrackingBinding;
import com.msystem.walking.model.Activity;
import com.msystem.walking.model.LocationPoint;
import com.msystem.walking.model.Territory;
import com.msystem.walking.model.User;
import com.msystem.walking.repository.DataRepository;
import com.msystem.walking.service.LocationTrackingService;
import com.msystem.walking.utils.TerritoryUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TrackingActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "TrackingActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final int MIN_POINTS_FOR_TERRITORY = 20; // Mínimo de pontos para formar território

    private ActivityTrackingBinding binding;
    private GoogleMap googleMap;
    private LocationTrackingService locationService;
    private boolean isServiceBound = false;
    private boolean isTracking = false;

    private DataRepository dataRepository;
    private Activity currentActivity;
    private User currentUser;
    private long startTime;
    private List<LatLng> routePoints = new ArrayList<>();
    private List<LocationPoint> territoryCandidate = new ArrayList<>();
    private Polygon currentTerritoryPolygon;

    // Sistema de gamificação
    private int pointsEarned = 0;
    private double distanceThisSession = 0.0;
    private boolean territoryConquestMode = false;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LocationTrackingService.LocalBinder binder = (LocationTrackingService.LocalBinder) service;
            locationService = binder.getService();
            isServiceBound = true;

            locationService.setLocationUpdateListener(new LocationTrackingService.LocationUpdateListener() {
                @Override
                public void onLocationUpdate(LocationPoint point, double totalDistance) {
                    updateUI(point, totalDistance);
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            locationService = null;
            isServiceBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: Iniciando TrackingActivity");

        binding = ActivityTrackingBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        dataRepository = DataRepository.getInstance();

        if (!checkLocationPermissions()) {
            requestLocationPermissions();
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isTracking) {
                    Toast.makeText(TrackingActivity.this, "Finalize ou pause a atividade antes de sair", Toast.LENGTH_SHORT).show();
                } else {
                    finish();
                }
            }
        });

        Log.d(TAG, "onCreate: Inicializando Google Maps");
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            Log.d(TAG, "onCreate: MapFragment encontrado, chamando getMapAsync");
            mapFragment.getMapAsync(this);
        } else {
            Log.e(TAG, "onCreate: MapFragment não encontrado!");
            Toast.makeText(this, "Erro ao carregar mapa", Toast.LENGTH_LONG).show();
        }

        setupClickListeners();

        Intent intent = new Intent(this, LocationTrackingService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private boolean checkLocationPermissions() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
               ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermissions() {
        Log.d(TAG, "Solicitando permissões de localização");
        ActivityCompat.requestPermissions(this,
                new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                },
                LOCATION_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Permissões de localização concedidas");
                if (googleMap != null) {
                    configureMap();
                }
            } else {
                Log.w(TAG, "Permissões de localização negadas");
                Toast.makeText(this, "Permissões de localização são necessárias para o funcionamento do app", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void setupClickListeners() {
        // Botão iniciar/pausar
        binding.btnStartStop.setOnClickListener(v -> {
            if (!isTracking) {
                startTracking();
            } else {
                pauseTracking();
            }
        });

        // Botão finalizar - novo botão separado
        binding.btnFinish.setOnClickListener(v -> {
            showFinishConfirmationDialog();
        });
    }

    private void startTracking() {
        if (!checkLocationPermissions()) {
            requestLocationPermissions();
            return;
        }

        if (isServiceBound) {
            isTracking = true;
            startTime = SystemClock.elapsedRealtime();

            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            String userName = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
            currentActivity = new Activity(userId, userName, "walking");

            // Atualizar UI dos botões
            binding.btnStartStop.setText("Pausar");
            binding.btnStartStop.setIconResource(android.R.drawable.ic_media_pause);
            binding.btnFinish.setEnabled(true); // Habilitar botão finalizar

            binding.chronometer.setBase(SystemClock.elapsedRealtime());
            binding.chronometer.start();

            locationService.startTracking();
        }
    }

    private void pauseTracking() {
        if (isServiceBound && isTracking) {
            isTracking = false;
            binding.chronometer.stop();
            locationService.stopTracking();

            // Atualizar UI dos botões
            binding.btnStartStop.setText("Retomar");
            binding.btnStartStop.setIconResource(android.R.drawable.ic_media_play);
            // Botão finalizar continua habilitado mesmo pausado
        }
    }

    private void showFinishConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Finalizar Atividade")
                .setMessage("Deseja finalizar a atividade atual? O percurso será salvo e você poderá ver o território conquistado.")
                .setPositiveButton("Finalizar", (dialog, which) -> finishActivity())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void finishActivity() {
        if (isServiceBound && currentActivity != null) {
            // Parar o rastreamento se ainda estiver ativo
            if (isTracking) {
                isTracking = false;
                binding.chronometer.stop();
                locationService.stopTracking();
            }

            currentActivity.setEndTime(new Date());
            currentActivity.setDistance(locationService.getTotalDistance());
            currentActivity.setRoute(locationService.getRoutePoints());

            int pointsEarned = (int) (currentActivity.getDistance() * 10);
            currentActivity.setPointsEarned(pointsEarned);

            dataRepository.saveActivity(currentActivity);

            // Mostrar o território conquistado no mapa ANTES de finalizar
            showConqueredTerritoryOnMap(currentActivity.getRoute());

            processTerritoriesConquered(currentActivity.getRoute());

            // Mostrar diálogo sem finalizar automaticamente
            showActivitySummaryDialog(currentActivity);
        }
        // NÃO chamar finish() aqui - deixar o usuário decidir no diálogo
    }

    private void showConqueredTerritoryOnMap(List<LocationPoint> route) {
        if (googleMap == null || route == null || route.size() < 3) {
            Log.w(TAG, "Não é possível mostrar território: mapa nulo ou rota insuficiente");
            return;
        }

        try {
            // Limpar marcadores existentes (exceto a linha do percurso)
            googleMap.clear();

            // Redesenhar a linha do percurso em cor diferente (finalizada)
            PolylineOptions routePolyline = new PolylineOptions()
                    .width(12)
                    .color(0xFF0099FF) // Azul para percurso finalizado
                    .geodesic(true);

            List<LatLng> routeLatLngs = new ArrayList<>();
            for (LocationPoint point : route) {
                LatLng latLng = new LatLng(point.getLatitude(), point.getLongitude());
                routeLatLngs.add(latLng);
                routePolyline.add(latLng);
            }

            googleMap.addPolyline(routePolyline);

            // Criar e mostrar território conquistado (polígono)
            if (route.size() >= MIN_POINTS_FOR_TERRITORY) {
                showTerritoryPolygon(route);
            }

            // Ajustar zoom para mostrar todo o percurso
            adjustCameraToShowFullRoute(routeLatLngs);

            Log.d(TAG, "Território e percurso exibidos no mapa com " + route.size() + " pontos");

        } catch (Exception e) {
            Log.e(TAG, "Erro ao exibir território no mapa", e);
        }
    }

    private void showTerritoryPolygon(List<LocationPoint> route) {
        try {
            // Converter pontos da rota para polígono (território conquistado)
            PolygonOptions territoryPolygon = new PolygonOptions()
                    .strokeWidth(8)
                    .strokeColor(0xFF00AA00) // Verde para território conquistado
                    .fillColor(0x4000AA00)   // Verde translúcido
                    .geodesic(true);

            for (LocationPoint point : route) {
                territoryPolygon.add(new LatLng(point.getLatitude(), point.getLongitude()));
            }

            // Fechar o polígono conectando o último ponto ao primeiro
            if (route.size() > 0) {
                LocationPoint firstPoint = route.get(0);
                territoryPolygon.add(new LatLng(firstPoint.getLatitude(), firstPoint.getLongitude()));
            }

            googleMap.addPolygon(territoryPolygon);

            Log.d(TAG, "Polígono do território adicionado ao mapa");

        } catch (Exception e) {
            Log.e(TAG, "Erro ao criar polígono do território", e);
        }
    }

    private void adjustCameraToShowFullRoute(List<LatLng> routePoints) {
        if (routePoints.isEmpty()) return;

        try {
            // Calcular bounds para mostrar toda a rota
            com.google.android.gms.maps.model.LatLngBounds.Builder boundsBuilder =
                new com.google.android.gms.maps.model.LatLngBounds.Builder();

            for (LatLng point : routePoints) {
                boundsBuilder.include(point);
            }

            com.google.android.gms.maps.model.LatLngBounds bounds = boundsBuilder.build();

            // Adicionar padding para melhor visualização
            int padding = 100; // padding em pixels

            googleMap.animateCamera(
                CameraUpdateFactory.newLatLngBounds(bounds, padding),
                2000, // Animação de 2 segundos
                null
            );

        } catch (Exception e) {
            Log.e(TAG, "Erro ao ajustar câmera para mostrar rota completa", e);
            // Fallback: focar no último ponto conhecido
            if (!routePoints.isEmpty()) {
                LatLng lastPoint = routePoints.get(routePoints.size() - 1);
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(lastPoint, 16f));
            }
        }
    }

    private void showActivitySummaryDialog(Activity activity) {
        String message = String.format(
            "Atividade Concluída!\n\n" +
            "Distância: %.2f km\n" +
            "Tempo: %s\n" +
            "Pontos ganhos: %d\n" +
            "Território conquistado: %s",
            activity.getDistance(),
            formatDuration(SystemClock.elapsedRealtime() - startTime),
            activity.getPointsEarned(),
            (activity.getRoute() != null && activity.getRoute().size() >= MIN_POINTS_FOR_TERRITORY)
                ? "Sim" : "Não (percurso muito pequeno)"
        );

        new AlertDialog.Builder(this)
                .setTitle("Resumo da Atividade")
                .setMessage(message)
                .setPositiveButton("Ver no Mapa", (dialog, which) -> {
                    // Manter na tela para o usuário ver o território
                    Toast.makeText(this, "O território conquistado está destacado em verde no mapa", Toast.LENGTH_LONG).show();
                })
                .setNegativeButton("Voltar ao Menu", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    private String formatDuration(long durationMillis) {
        long seconds = durationMillis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        if (hours > 0) {
            return String.format("%dh %02dm %02ds", hours, minutes % 60, seconds % 60);
        } else if (minutes > 0) {
            return String.format("%dm %02ds", minutes, seconds % 60);
        } else {
            return String.format("%ds", seconds);
        }
    }

    private void processTerritoriesConquered(List<LocationPoint> route) {
        if (route != null && route.size() > 10) {
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            String userName = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
            Territory newTerritory = new Territory(userId, userName, route);

            if (newTerritory.isValid()) {
                dataRepository.saveTerritory(newTerritory);
                Toast.makeText(this, "Novo território conquistado!", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void updateUI(LocationPoint point, double totalDistance) {
        binding.tvDistance.setText(String.format("%.2f km", totalDistance));
        binding.tvPoints.setText(String.format("%d pontos", (int)(totalDistance * 10)));

        LatLng latLng = new LatLng(point.getLatitude(), point.getLongitude());
        routePoints.add(latLng);

        if (googleMap != null) {
            if (routePoints.size() > 1) {
                // Desenhar linha do percurso em tempo real
                PolylineOptions polylineOptions = new PolylineOptions()
                        .addAll(routePoints)
                        .width(8)
                        .color(0xFF00FF00) // Verde durante o rastreamento
                        .geodesic(true);
                googleMap.clear(); // Limpar polylines anteriores
                googleMap.addPolyline(polylineOptions);
            }

            // Manter câmera seguindo o usuário com vista inclinada estilo Waze
            if (isTracking) {
                // Calcular direção do movimento para orientar a câmera
                float bearing = 0f;
                if (routePoints.size() >= 2) {
                    LatLng previous = routePoints.get(routePoints.size() - 2);
                    bearing = calculateBearing(previous, latLng);
                }

                com.google.android.gms.maps.model.CameraPosition trackingCamera =
                    new com.google.android.gms.maps.model.CameraPosition.Builder()
                        .target(latLng)              // Seguir posição atual
                        .zoom(20f)                   // Zoom próximo para tracking
                        .tilt(67.5f)                 // Vista inclinada estilo Waze
                        .bearing(bearing)            // Orientar na direção do movimento
                        .build();

                googleMap.animateCamera(
                    com.google.android.gms.maps.CameraUpdateFactory.newCameraPosition(trackingCamera),
                    1000,  // Animação suave de 1 segundo
                    null
                );
            }

            // Atualizar marcador do usuário
            addTrackingUserMarker(latLng);
        }

        // Atualiza o modo de conquista de território
        updateTerritoryConquestMode(point);
    }

    private float calculateBearing(LatLng from, LatLng to) {
        double lat1 = Math.toRadians(from.latitude);
        double lat2 = Math.toRadians(to.latitude);
        double deltaLng = Math.toRadians(to.longitude - from.longitude);

        double y = Math.sin(deltaLng) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(deltaLng);

        double bearing = Math.toDegrees(Math.atan2(y, x));
        return (float) ((bearing + 360) % 360);
    }

    private void addTrackingUserMarker(LatLng position) {
        // Implementar marcador personalizado para o usuário em movimento
        // Pode incluir um ícone de seta indicando direção
    }

    private void updateTerritoryConquestMode(LocationPoint point) {
        if (!territoryConquestMode) return;

        territoryCandidate.add(point);

        if (territoryCandidate.size() >= MIN_POINTS_FOR_TERRITORY) {
            // Cria um polígono para o território
            if (currentTerritoryPolygon != null) {
                currentTerritoryPolygon.remove();
            }

            PolygonOptions polygonOptions = new PolygonOptions()
                    .strokeWidth(5)
                    .strokeColor(0xFFFF0000)
                    .fillColor(0x7FFF0000);

            // Converter LocationPoint para LatLng
            for (LocationPoint locationPoint : territoryCandidate) {
                LatLng latLng = new LatLng(locationPoint.getLatitude(), locationPoint.getLongitude());
                polygonOptions.add(latLng);
            }

            currentTerritoryPolygon = googleMap.addPolygon(polygonOptions);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d(TAG, "onMapReady: Google Maps carregado com sucesso!");
        this.googleMap = googleMap;

        try {
            googleMap.getUiSettings().setZoomControlsEnabled(true);
            googleMap.getUiSettings().setMyLocationButtonEnabled(true);
            googleMap.getUiSettings().setCompassEnabled(true);

            googleMap.moveCamera(CameraUpdateFactory.zoomTo(15f));

            configureMap();

            Toast.makeText(this, "Mapa carregado com sucesso!", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "onMapReady: Configuração do mapa concluída");

        } catch (Exception e) {
            Log.e(TAG, "onMapReady: Erro ao configurar mapa", e);
            Toast.makeText(this, "Erro ao configurar mapa: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void configureMap() {
        if (googleMap == null) {
            Log.w(TAG, "configureMap: googleMap é null");
            return;
        }

        if (checkLocationPermissions()) {
            try {
                googleMap.setMyLocationEnabled(true);
                Log.d(TAG, "configureMap: MyLocation habilitado");
            } catch (SecurityException e) {
                Log.e(TAG, "configureMap: Erro de segurança ao habilitar MyLocation", e);
            }
        } else {
            Log.w(TAG, "configureMap: Permissões de localização não concedidas");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isServiceBound) {
            unbindService(serviceConnection);
        }
    }
}
