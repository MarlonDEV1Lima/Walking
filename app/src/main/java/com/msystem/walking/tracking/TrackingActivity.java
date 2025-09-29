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
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;

import com.msystem.walking.R;
import com.msystem.walking.databinding.ActivityTrackingBinding;
import com.msystem.walking.model.Activity;
import com.msystem.walking.model.LocationPoint;
import com.msystem.walking.model.Territory;
import com.msystem.walking.repository.DataRepository;
import com.msystem.walking.service.LocationTrackingService;
import com.msystem.walking.utils.TerritoryUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TrackingActivity extends AppCompatActivity implements OnMapReadyCallback {

    private ActivityTrackingBinding binding;
    private GoogleMap googleMap;
    private LocationTrackingService locationService;
    private boolean isServiceBound = false;
    private boolean isTracking = false;

    private DataRepository dataRepository;
    private Activity currentActivity;
    private long startTime;
    private List<LatLng> routePoints = new ArrayList<>();

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LocationTrackingService.LocationBinder binder = (LocationTrackingService.LocationBinder) service;
            locationService = binder.getService();
            isServiceBound = true;

            // Configurar listener de atualizações de localização
            locationService.setLocationUpdateListener(new LocationTrackingService.LocationUpdateListener() {
                @Override
                public void onLocationUpdate(LocationPoint point, double totalDistance) {
                    runOnUiThread(() -> {
                        updateUI(point, totalDistance);
                    });
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isServiceBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTrackingBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        dataRepository = DataRepository.getInstance();

        // Configurar callback para o botão voltar
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

        // Inicializar mapa
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        setupClickListeners();

        // Bind ao serviço de localização
        Intent intent = new Intent(this, LocationTrackingService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void setupClickListeners() {
        binding.btnStartStop.setOnClickListener(v -> {
            if (!isTracking) {
                startTracking();
            } else {
                stopTracking();
            }
        });

        binding.btnFinish.setOnClickListener(v -> {
            finishActivity();
        });
    }

    private void startTracking() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permissão de localização necessária", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isServiceBound) {
            isTracking = true;
            startTime = SystemClock.elapsedRealtime();

            // Criar nova atividade
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            String userName = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
            currentActivity = new Activity(userId, userName, "walking");

            locationService.startTracking();
            binding.btnStartStop.setText("Pausar");
            binding.btnStartStop.setIcon(getDrawable(android.R.drawable.ic_media_pause));
            binding.chronometer.setBase(SystemClock.elapsedRealtime());
            binding.chronometer.start();
        }
    }

    private void stopTracking() {
        if (isServiceBound) {
            isTracking = false;
            locationService.stopTracking();
            binding.btnStartStop.setText("Continuar");
            binding.btnStartStop.setIcon(getDrawable(android.R.drawable.ic_media_play));
            binding.chronometer.stop();
        }
    }

    private void finishActivity() {
        if (isServiceBound) {
            locationService.stopTracking();

            if (currentActivity != null) {
                // Finalizar atividade
                currentActivity.setEndTime(new Date());
                currentActivity.setDuration(SystemClock.elapsedRealtime() - startTime);
                currentActivity.setDistance(locationService.getTotalDistance());
                currentActivity.setRoute(locationService.getRoutePoints());

                // Calcular pontos (10 pontos por km)
                int points = (int) (currentActivity.getDistance() * 10);
                currentActivity.setPointsEarned(points);

                // Criar territórios baseados na rota
                List<Territory> newTerritories = TerritoryUtils.createTerritoriesFromRoute(
                    currentActivity.getRoute(),
                    currentActivity.getUserId(),
                    currentActivity.getUserName()
                );

                // Salvar territórios
                List<String> territoryIds = new ArrayList<>();
                for (Territory territory : newTerritories) {
                    dataRepository.saveTerritory(territory);
                    territoryIds.add(territory.getTerritoryId());
                }
                currentActivity.setConqueredTerritoryIds(territoryIds);

                // Salvar no Firebase
                dataRepository.saveActivity(currentActivity);

                Toast.makeText(this, String.format("Atividade salva! %d pontos ganhos e %d territórios conquistados!",
                              points, newTerritories.size()), Toast.LENGTH_LONG).show();
            }

            finish();
        }
    }

    private void updateUI(LocationPoint point, double totalDistance) {
        // Atualizar estatísticas na UI
        binding.tvDistance.setText(String.format("%.2f km", totalDistance));
        binding.tvPoints.setText(String.format("%d pontos", (int)(totalDistance * 10)));

        // Adicionar ponto ao mapa
        LatLng latLng = new LatLng(point.getLatitude(), point.getLongitude());
        routePoints.add(latLng);

        if (googleMap != null) {
            // Desenhar linha da rota
            if (routePoints.size() > 1) {
                PolylineOptions polylineOptions = new PolylineOptions()
                        .addAll(routePoints)
                        .width(8)
                        .color(0xFF00FF00); // Verde

                googleMap.addPolyline(polylineOptions);
            }

            // Mover câmera para localização atual
            googleMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;

        // Configurar mapa
        googleMap.getUiSettings().setZoomControlsEnabled(true);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
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
