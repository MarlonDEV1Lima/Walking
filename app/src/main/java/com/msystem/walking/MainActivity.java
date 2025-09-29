package com.msystem.walking;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.msystem.walking.auth.LoginActivity;
import com.msystem.walking.repository.AuthRepository;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private AuthRepository authRepository;
    private MapView mapView;
    private IMapController mapController;
    private MyLocationNewOverlay locationOverlay;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private Marker userMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Configurar OSMDroid para melhor performance
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        Configuration.getInstance().setUserAgentValue(getPackageName());

        setContentView(R.layout.activity_main);

        authRepository = AuthRepository.getInstance();

        // Verificar se o usuário está autenticado
        if (authRepository.getCurrentUser() == null) {
            redirectToLogin();
            return;
        }

        initializeMap();
        initializeLocationServices();
        setupObservers();
        setupButtonListeners();

        // Verificar e solicitar permissões de localização
        if (checkLocationPermissions()) {
            startLocationTracking();
        } else {
            requestLocationPermissions();
        }
    }

    private void initializeMap() {
        mapView = findViewById(R.id.osmMapView);

        // Configurar o mapa com melhor qualidade
        mapView.setTileSource(TileSourceFactory.MAPNIK); // OpenStreetMap padrão
        mapView.setMultiTouchControls(true);
        mapView.setBuiltInZoomControls(false);

        mapController = mapView.getController();
        mapController.setZoom(18.0); // Zoom GPS - bem próximo para navegação

        // Configurar overlay de localização
        locationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), mapView);
        locationOverlay.enableMyLocation();
        locationOverlay.enableFollowLocation(); // Seguir automaticamente o usuário
        locationOverlay.setDrawAccuracyEnabled(true); // Mostrar círculo de precisão
        mapView.getOverlays().add(locationOverlay);

        // Localização inicial padrão (será substituída pelo GPS)
        GeoPoint startPoint = new GeoPoint(38.7223, -9.1393); // Lisboa
        mapController.setCenter(startPoint);
    }

    private void initializeLocationServices() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Callback para atualizações de localização em tempo real com filtros de precisão
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (location != null && isLocationAccurate(location)) {
                    updateUserLocationOnMap(location);
                }
                // Se a localização não for precisa o suficiente, simplesmente ignora
            }
        };
    }

    private void updateUserLocationOnMap(Location location) {
        GeoPoint userLocation = new GeoPoint(location.getLatitude(), location.getLongitude());

        // Atualizar marcador personalizado do usuário
        updateUserMarker(userLocation, location);

        // Centralizar mapa automaticamente (modo GPS)
        mapController.animateTo(userLocation);

        // Feedback de precisão para o usuário
        providePrecisionFeedback(location);

        mapView.invalidate();
    }

    private void updateUserMarker(GeoPoint location, Location gpsLocation) {
        // Remover marcador anterior
        if (userMarker != null) {
            mapView.getOverlays().remove(userMarker);
        }

        // Criar marcador atualizado
        userMarker = new Marker(mapView);
        userMarker.setPosition(location);
        userMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        userMarker.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_location_user));
        userMarker.setTitle("📍 Minha Localização");

        // Informações detalhadas do GPS - corrigir String.format para usar Locale
        String snippet = String.format(Locale.getDefault(),
                "📐 Lat: %.6f\n📐 Lon: %.6f\n🎯 Precisão: %.1fm\n⚡ Velocidade: %.1f km/h",
                gpsLocation.getLatitude(),
                gpsLocation.getLongitude(),
                gpsLocation.getAccuracy(),
                gpsLocation.getSpeed() * 3.6 // m/s para km/h
        );
        userMarker.setSnippet(snippet);

        mapView.getOverlays().add(userMarker);
    }

    private void providePrecisionFeedback(Location location) {
        float accuracy = location.getAccuracy();

        // Feedback visual baseado na precisão GPS
        if (accuracy <= 5) {
            // GPS excelente (≤5m) - adicionar comentário para evitar warning
            // Sem avisos necessários para GPS excelente
        } else if (accuracy <= 15) {
            // GPS bom (5-15m) - aviso ocasional
            if (Math.random() < 0.1) { // 10% chance de mostrar
                Toast.makeText(this, "GPS: Boa precisão (" + (int) accuracy + "m)",
                        Toast.LENGTH_SHORT).show();
            }
        } else if (accuracy <= 30) {
            // GPS regular (15-30m) - aviso mais frequente
            Toast.makeText(this, "GPS: Precisão moderada (" + (int) accuracy + "m)",
                    Toast.LENGTH_SHORT).show();
        } else {
            // GPS ruim (>30m) - aviso sempre
            Toast.makeText(this, "⚠️ GPS: Baixa precisão (" + (int) accuracy + "m)",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void setupButtonListeners() {
        // Botão "Minha Localização" - centralizar no usuário
        findViewById(R.id.fabMyLocation).setOnClickListener(v -> centerOnUserLocation());

        // Botão "Ativar Localização" no banner de aviso
        findViewById(R.id.btnEnableLocation).setOnClickListener(v -> {
            if (!checkLocationPermissions()) {
                requestLocationPermissions();
            } else {
                startLocationTracking();
                findViewById(R.id.limitedModeWarning).setVisibility(android.view.View.GONE);
                Toast.makeText(this, "✅ GPS ativado!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void centerOnUserLocation() {
        if (!checkLocationPermissions()) {
            Toast.makeText(this, "📍 Permissão de localização necessária", Toast.LENGTH_SHORT).show();
            requestLocationPermissions();
            return;
        }

        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestLocationPermissions();
                return;
            }
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    GeoPoint userLocation = new GeoPoint(location.getLatitude(), location.getLongitude());
                    mapController.animateTo(userLocation);
                    mapController.setZoom(19.0); // Zoom muito próximo
                    Toast.makeText(this, "📍 Centralizado na sua localização", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "🔍 Aguardando sinal GPS...", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (SecurityException e) {
            Toast.makeText(this, "❌ Erro ao acessar GPS", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupObservers() {
        // Observar logout
        authRepository.getLoggedOutLiveData().observe(this, isLoggedOut -> {
            if (Boolean.TRUE.equals(isLoggedOut)) {
                redirectToLogin();
            }
        });
    }

    private boolean checkLocationPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED &&
               ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                },
                LOCATION_PERMISSION_REQUEST_CODE);
    }

    private void startLocationTracking() {
        if (!checkLocationPermissions()) {
            return;
        }

        // Configuração otimizada para GPS preciso
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(1000)
                .setMaxUpdateDelayMillis(5000)
                .build();

        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestLocationPermissions();
                return;
            }
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, getMainLooper());

            // Obter última localização conhecida para inicialização rápida
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    updateUserLocationOnMap(location);
                }
            });

            Toast.makeText(this, "🛰️ GPS ativado - Aguardando sinal...", Toast.LENGTH_SHORT).show();
        } catch (SecurityException e) {
            Toast.makeText(this, "❌ Erro ao ativar GPS: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void stopLocationTracking() {
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    private void redirectToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            authRepository.signOut();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Registrar o receiver para atualizações de localização ao retomar a atividade
        if (checkLocationPermissions()) {
            startLocationTracking();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Remover atualizações de localização ao pausar a atividade
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Limpar recursos e referências
        mapView.getOverlays().remove(locationOverlay);
        if (userMarker != null) {
            mapView.getOverlays().remove(userMarker);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permissão concedida - iniciar rastreamento de localização
                startLocationTracking();
            } else {
                // Permissão negada - mostrar mensagem apropriada
                Toast.makeText(this, "❌ Permissão de localização necessária para o funcionamento do app", Toast.LENGTH_LONG).show();
            }
        }
    }

    // Adicionar método que estava faltando
    private boolean isLocationAccurate(Location location) {
        // Considera uma localização precisa se tiver precisão melhor que 50 metros
        return location.getAccuracy() <= 50.0f;
    }
}
