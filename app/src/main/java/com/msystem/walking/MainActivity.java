package com.msystem.walking;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import com.msystem.walking.auth.LoginActivity;
import com.msystem.walking.databinding.ActivityMainBinding;
import com.msystem.walking.history.HistoryActivity;
import com.msystem.walking.leaderboard.LeaderboardActivity;
import com.msystem.walking.model.WalkSession;
import com.msystem.walking.service.LocationTrackingService;
import com.msystem.walking.tracking.TrackingActivity;
import com.msystem.walking.utils.LocationPermissionHelper;
import com.msystem.walking.utils.LocationAccuracyHelper;

import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
    private static final int GPS_ENABLE_REQUEST_CODE = 1001;

    private ActivityMainBinding binding;
    private GoogleMap googleMap;
    private MainViewModel viewModel;

    private boolean isWalking = false;
    private WalkSession currentWalkSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Verificar autenticação
        checkUserAuthentication();

        // Inicializar ViewModel
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        // Inicializar mapa
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        setupUI();
        setupObservers();
        setupClickListeners();

        checkLocationPermission();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkGPSStatus();
        loadUserData();
    }

    private void checkUserAuthentication() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }

    private void setupUI() {
        // Configurar estado inicial dos botões
        updateWalkingButton(false);
        binding.territoryCapturingCard.setVisibility(View.GONE);
        binding.gpsWarningCard.setVisibility(View.GONE);
    }

    private void setupObservers() {
        // Observer para dados do usuário
        viewModel.getUserData().observe(this, user -> {
            if (user != null) {
                binding.tvTotalPoints.setText(String.valueOf(user.getTotalPoints()));
                binding.tvTerritories.setText(String.valueOf(user.getTerritoriesConquered()));
            }
        });

        // Observer para distância diária
        viewModel.getTodayDistance().observe(this, distance ->
                binding.tvDistanceToday.setText(String.format(Locale.getDefault(), "%.1f", distance))
        );

        // Observer para status de caminhada
        viewModel.getWalkingStatus().observe(this, walking -> {
            isWalking = walking;
            updateWalkingButton(walking);

            if (walking) {
                binding.territoryCapturingCard.setVisibility(View.VISIBLE);
            } else {
                binding.territoryCapturingCard.setVisibility(View.GONE);
            }
        });

        // Observer para progresso de captura de território
        viewModel.getTerritoryProgress().observe(this, progress -> {
            binding.progressTerritoryCapture.setProgress(progress);
            binding.tvTerritoryProgress.setText(
                    String.format(Locale.getDefault(), "Capturando território... %d%%", progress)
            );
        });

        // Observer para localização atual
        viewModel.getCurrentLocation().observe(this, location -> {
            if (location != null && googleMap != null) {
                // Verificar se está no emulador com localização do Google
                if (LocationAccuracyHelper.isRunningOnEmulator() &&
                        LocationAccuracyHelper.isGoogleHQLocation(location)) {
                    showEmulatorLocationWarning();
                }

                // Log detalhado da localização para debug
                LocationAccuracyHelper.logLocationDetails(location, "MainActivity");

                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18f));
            }
        });
    }

    private void setupClickListeners() {
        // Botão iniciar/parar caminhada
        binding.fabStartWalk.setOnClickListener(v -> {
            if (!isWalking) {
                startWalking();
            } else {
                stopWalking();
            }
        });

        // Botão minha localização
        binding.fabMyLocation.setOnClickListener(v -> viewModel.requestCurrentLocation());

        // Botão histórico
        binding.fabHistory.setOnClickListener(v -> startActivity(new Intent(this, HistoryActivity.class)));

        // Clique longo no card de status para abrir ranking
        binding.tvTotalPoints.setOnLongClickListener(v -> {
            startActivity(new Intent(this, LeaderboardActivity.class));
            return true;
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;

        // Configurar estilo do mapa
        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(false);

        // Tentar habilitar localização se houver permissão
        if (hasLocationPermission()) {
            enableMyLocation();
        }

        // Configurar listener para cliques no mapa
        googleMap.setOnMapClickListener(latLng -> {
            if (isWalking) {
                // Adicionar waypoint ou marcador especial durante caminhada
                viewModel.addWaypoint(latLng);
            }
        });

        // Carregar dados do mapa
        loadMapData();
    }

    private void enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        googleMap.setMyLocationEnabled(true);
        viewModel.startLocationUpdates();
    }

    private void loadMapData() {
        // Carregar territórios do usuário
        viewModel.loadUserTerritories();

        // Carregar caminhadas recentes
        viewModel.loadRecentWalks();
    }

    private void checkLocationPermission() {
        if (!LocationPermissionHelper.hasLocationPermission(this)) {
            if (LocationPermissionHelper.shouldShowRequestPermissionRationale(this)) {
                showPermissionRationale();
            } else {
                LocationPermissionHelper.requestLocationPermission(this);
            }
        } else {
            // Verificar se também temos permissão de localização em segundo plano (para melhor precisão)
            checkBackgroundLocationPermission();
        }
    }

    private void checkBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                // Explicar ao usuário por que precisamos da permissão de localização em segundo plano
                new AlertDialog.Builder(this)
                        .setTitle("Permissão de Localização Contínua")
                        .setMessage("Para maior precisão durante as caminhadas, o app precisa acessar sua localização mesmo quando estiver em segundo plano. Isso garante que o rastreamento seja preciso.")
                        .setPositiveButton("Permitir", (dialog, which) -> {
                            ActivityCompat.requestPermissions(this,
                                    new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, 1001);
                        })
                        .setNegativeButton("Agora não", null)
                        .show();
            }
        }
    }

    private void showPermissionRationale() {
        new AlertDialog.Builder(this)
                .setTitle("Permissão de Localização")
                .setMessage("Este app precisa da sua localização para funcionar corretamente.")
                .setPositiveButton("Permitir", (dialog, which) ->
                        LocationPermissionHelper.requestLocationPermission(this))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void checkGPSStatus() {
        if (!LocationPermissionHelper.isOptimalLocationSetup(this)) {
            binding.gpsWarningCard.setVisibility(View.VISIBLE);

            binding.btnEnableGPS.setOnClickListener(v -> {
                if (!LocationPermissionHelper.isGPSEnabled(this) ||
                        !LocationPermissionHelper.isHighAccuracyModeEnabled(this)) {
                    // Abrir configurações de localização
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivityForResult(intent, GPS_ENABLE_REQUEST_CODE);
                } else {
                    // Se o GPS está ligado mas não temos permissão, solicitar novamente
                    checkLocationPermission();
                }
            });
        } else {
            binding.gpsWarningCard.setVisibility(View.GONE);
        }
    }

    private void loadUserData() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            viewModel.loadUserData(user.getUid());
            viewModel.loadTodayDistance(user.getUid());
        }
    }

    private boolean hasLocationPermission() {
        return LocationPermissionHelper.hasLocationPermission(this);
    }

    private void startWalking() {
        if (!hasLocationPermission()) {
            checkLocationPermission();
            return;
        }

        if (!LocationPermissionHelper.isGPSEnabled(this)) {
            checkGPSStatus();
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            currentWalkSession = new WalkSession();
            currentWalkSession.setUserId(user.getUid());
            currentWalkSession.setStartTime(new Date());
            currentWalkSession.setWaypoints(new ArrayList<>());

            viewModel.startWalkSession(currentWalkSession);

            // Iniciar serviço de rastreamento
            Intent serviceIntent = new Intent(this, LocationTrackingService.class);
            startService(serviceIntent);

            // Ir para tela de rastreamento
            Intent trackingIntent = new Intent(this, TrackingActivity.class);
            startActivity(trackingIntent);
        }
    }

    private void stopWalking() {
        if (currentWalkSession != null) {
            viewModel.stopWalkSession(currentWalkSession);

            // Parar serviço de rastreamento
            Intent serviceIntent = new Intent(this, LocationTrackingService.class);
            stopService(serviceIntent);

            currentWalkSession = null;
        }
    }

    private void updateWalkingButton(boolean walking) {
        if (walking) {
            binding.fabStartWalk.setIconResource(R.drawable.ic_stop);
            binding.fabStartWalk.setBackgroundTintList(
                    ContextCompat.getColorStateList(this, R.color.red_500));
        } else {
            binding.fabStartWalk.setIconResource(R.drawable.ic_play_arrow);
            binding.fabStartWalk.setBackgroundTintList(
                    ContextCompat.getColorStateList(this, R.color.green_500));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (googleMap != null) {
                    enableMyLocation();
                }
            } else {
                Toast.makeText(this, "Permissão de localização negada", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GPS_ENABLE_REQUEST_CODE) {
            checkGPSStatus();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (viewModel != null) {
            viewModel.stopLocationUpdates();
        }
    }

    private void showEmulatorLocationWarning() {
        new AlertDialog.Builder(this)
                .setTitle("Atenção")
                .setMessage("Você está usando um emulador com localização do Google HQ. " +
                        "Isso pode afetar a precisão do rastreamento. " +
                        "Recomendamos usar um dispositivo físico para melhores resultados.")
                .setPositiveButton("OK", null)
                .show();
    }
}
