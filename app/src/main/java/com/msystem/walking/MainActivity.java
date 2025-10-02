package com.msystem.walking;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
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
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import com.msystem.walking.auth.LoginActivity;
import com.msystem.walking.databinding.ActivityMainBinding;
import com.msystem.walking.history.HistoryActivity;
import com.msystem.walking.leaderboard.LeaderboardActivity;
import com.msystem.walking.model.LocationPoint;
import com.msystem.walking.model.Territory;
import com.msystem.walking.model.User;
import com.msystem.walking.model.WalkSession;
import com.msystem.walking.service.LocationTrackingService;
import com.msystem.walking.tracking.TrackingActivity;
import com.msystem.walking.utils.LocationPermissionHelper;
import com.msystem.walking.utils.LocationAccuracyHelper;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
    private static final int GPS_ENABLE_REQUEST_CODE = 1001;

    private ActivityMainBinding binding;
    private GoogleMap googleMap;
    private MainViewModel viewModel;
    private BottomSheetBehavior<View> bottomSheetBehavior;

    private boolean isWalking = false;
    private WalkSession currentWalkSession;

    // Mapas para gerenciar territórios exibidos
    private Map<String, Polygon> territoryPolygons = new HashMap<>();
    private Map<String, Marker> territoryMarkers = new HashMap<>();
    private List<Territory> currentTerritories = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Verificar autenticação
        checkUserAuthentication();

        // Inicializar ViewModel
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        // Configurar toolbar
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(true);
        }

        // Inicializar mapa
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        setupBottomSheet();
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

    private void setupBottomSheet() {
        View bottomSheet = findViewById(R.id.bottomSheetMainInfo);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);

        // Configurar comportamento do bottom sheet
        bottomSheetBehavior.setHideable(false);
        bottomSheetBehavior.setPeekHeight(getResources().getDimensionPixelSize(R.dimen.bottom_sheet_peek_height));

        // Listener para expandir/contrair conteúdo
        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                View expandedContent = findViewById(R.id.bottomSheetExpandedContent);
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    expandedContent.setVisibility(View.VISIBLE);
                } else if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    expandedContent.setVisibility(View.GONE);
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                // Animação suave do conteúdo expandido
                View expandedContent = findViewById(R.id.bottomSheetExpandedContent);
                expandedContent.setAlpha(slideOffset);
            }
        });
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
                // Atualizar pontos no toolbar
                binding.tvPointsCompact.setText(String.format(Locale.getDefault(), "%d pts", user.getTotalPoints()));

                // Atualizar dados no bottom sheet
                TextView tvTotalPoints = findViewById(R.id.tvTotalPoints);
                TextView tvTerritories = findViewById(R.id.tvTerritories);
                TextView tvTerritoriesDetailed = findViewById(R.id.tvTerritoriesDetailed);
                TextView tvTotalDistance = findViewById(R.id.tvTotalDistance);

                if (tvTotalPoints != null) tvTotalPoints.setText(String.valueOf(user.getTotalPoints()));
                if (tvTerritories != null) tvTerritories.setText(String.valueOf(user.getTerritoriesCount()));
                if (tvTerritoriesDetailed != null) tvTerritoriesDetailed.setText(String.valueOf(user.getTerritoriesCount()));
                if (tvTotalDistance != null) tvTotalDistance.setText(String.format(Locale.getDefault(), "%.1f", user.getTotalDistance()));
            }
        });

        // Observer para distância diária
        viewModel.getTodayDistance().observe(this, distance -> {
            TextView tvDistanceToday = findViewById(R.id.tvDistanceToday);
            if (tvDistanceToday != null) {
                tvDistanceToday.setText(String.format(Locale.getDefault(), "%.1f", distance));
            }
        });

        // Observer para status de caminhada
        viewModel.getWalkingStatus().observe(this, walking -> {
            isWalking = walking;
            updateWalkingButton(walking);

            if (walking) {
                binding.territoryCapturingCard.setVisibility(View.VISIBLE);
                // Colapsar bottom sheet durante caminhada para dar mais espaço ao mapa
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
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

                // Carregar territórios na área visível
                loadTerritoriesAroundLocation(location.getLatitude(), location.getLongitude());
            }
        });

        // ✅ Observer para todos os territórios em tempo real
        viewModel.getAllTerritories().observe(this, territories -> {
            if (territories != null) {
                currentTerritories = territories;
                updateTerritoriesOnMap(territories);

                // Atualizar contador de territórios no bottom sheet
                TextView tvTotalTerritoriesGlobal = findViewById(R.id.tvTotalTerritoriesGlobal);
                if (tvTotalTerritoriesGlobal != null) {
                    tvTotalTerritoriesGlobal.setText(
                        String.format(Locale.getDefault(), "Territórios globais: %d", territories.size())
                    );
                }
            }
        });

        // ✅ Observer para leaderboard em tempo real
        viewModel.getLeaderboard().observe(this, users -> {
            if (users != null && !users.isEmpty()) {
                // Mostrar top 3 no bottom sheet
                updateLeaderboardPreview(users);
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

        // Botão histórico no bottom sheet
        findViewById(R.id.fabHistory).setOnClickListener(v ->
            startActivity(new Intent(this, HistoryActivity.class)));

        // Botão leaderboard no toolbar (agora é TextView)
        binding.btnLeaderboard.setOnClickListener(v ->
            startActivity(new Intent(this, LeaderboardActivity.class)));

        // ✅ Clique no preview do leaderboard para abrir tela completa
        findViewById(R.id.leaderboardPreviewCard).setOnClickListener(v -> {
            startActivity(new Intent(this, LeaderboardActivity.class));
        });

        // Clique no header do bottom sheet para expandir/colapsar
        findViewById(R.id.bottomSheetHeader).setOnClickListener(v -> {
            if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            } else {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
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

        // ✅ NOVO: Configurar listener para cliques no mapa
        googleMap.setOnMapClickListener(latLng -> {
            if (isWalking) {
                // Adicionar waypoint ou marcador especial durante caminhada
                viewModel.addWaypoint(latLng);
            }
        });

        // ✅ NOVO: Configurar listener para cliques em territórios
        googleMap.setOnPolygonClickListener(polygon -> {
            // Encontrar território clicado
            Territory clickedTerritory = findTerritoryByPolygon(polygon);
            if (clickedTerritory != null) {
                showTerritoryDetails(clickedTerritory);
            }
        });

        // ✅ NOVO: Listener para mudanças na câmera (recarregar territórios)
        googleMap.setOnCameraIdleListener(() -> {
            LatLng center = googleMap.getCameraPosition().target;
            loadTerritoriesAroundLocation(center.latitude, center.longitude);
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

            // O botão já está definido no layout activity_main.xml como btnEnableGps
            // Não há necessidade de definir listener aqui pois já está no setupClickListeners()
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

    /**
     * ✅ NOVO: Atualiza os territórios exibidos no mapa
     */
    private void updateTerritoriesOnMap(List<Territory> territories) {
        if (googleMap == null) return;

        // Limpar territórios antigos
        clearTerritories();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String currentUserId = currentUser != null ? currentUser.getUid() : "";

        // Adicionar novos territórios
        for (Territory territory : territories) {
            if (territory.getPolygon() != null && !territory.getPolygon().isEmpty()) {
                addTerritoryToMap(territory, currentUserId.equals(territory.getOwnerId()));
            }
        }
    }

    /**
     * ✅ NOVO: Adiciona um território ao mapa
     */
    private void addTerritoryToMap(Territory territory, boolean isOwner) {
        if (googleMap == null || territory.getPolygon() == null) return;

        // Converter pontos para LatLng
        List<LatLng> polygonPoints = new ArrayList<>();
        for (LocationPoint point : territory.getPolygon()) {
            polygonPoints.add(new LatLng(point.getLatitude(), point.getLongitude()));
        }

        // Definir cores baseado no proprietário
        int strokeColor = Color.parseColor(territory.getColor());
        int fillColor = isOwner ?
            Color.argb(60, Color.red(strokeColor), Color.green(strokeColor), Color.blue(strokeColor)) :
            Color.argb(30, Color.red(strokeColor), Color.green(strokeColor), Color.blue(strokeColor));

        // Criar polígono
        PolygonOptions polygonOptions = new PolygonOptions()
                .addAll(polygonPoints)
                .strokeColor(strokeColor)
                .strokeWidth(isOwner ? 4f : 2f)
                .fillColor(fillColor)
                .clickable(true);

        Polygon polygon = googleMap.addPolygon(polygonOptions);
        territoryPolygons.put(territory.getTerritoryId(), polygon);

        // Adicionar marcador no centro do território
        LatLng center = calculateCenter(polygonPoints);
        MarkerOptions markerOptions = new MarkerOptions()
                .position(center)
                .title(territory.getOwnerName())
                .snippet(String.format(Locale.getDefault(),
                    "%d pontos • %.0f m²", territory.getPointsValue(), territory.getArea()));

        Marker marker = googleMap.addMarker(markerOptions);
        if (marker != null) {
            territoryMarkers.put(territory.getTerritoryId(), marker);
        }
    }

    /**
     * ✅ NOVO: Limpa todos os territórios do mapa
     */
    private void clearTerritories() {
        for (Polygon polygon : territoryPolygons.values()) {
            polygon.remove();
        }
        for (Marker marker : territoryMarkers.values()) {
            marker.remove();
        }
        territoryPolygons.clear();
        territoryMarkers.clear();
    }

    /**
     * ✅ NOVO: Carrega territórios ao redor de uma localização - CORRIGIDO
     */
    private void loadTerritoriesAroundLocation(double lat, double lng) {
        viewModel.loadTerritoriesInArea(lat, lng, 5.0, new com.msystem.walking.repository.TerritoryRepository.Callback<List<Territory>>() {
            @Override
            public void onResult(List<Territory> territories) {
                // ✅ Os territórios já são atualizados via LiveData observer - não mostrar toast
                android.util.Log.d("MainActivity", "Territórios carregados com sucesso: " + territories.size());
            }

            @Override
            public void onError(String error) {
                // ✅ Apenas log do erro, sem mostrar toast ao usuário (pode ser normal não ter territórios)
                android.util.Log.w("MainActivity", "Aviso ao carregar territórios: " + error);
                // Não mostrar Toast para não incomodar o usuário desnecessariamente
            }
        });
    }

    /**
     * ✅ NOVO: Encontra território pelo polígono clicado
     */
    private Territory findTerritoryByPolygon(Polygon polygon) {
        for (Map.Entry<String, Polygon> entry : territoryPolygons.entrySet()) {
            if (entry.getValue().equals(polygon)) {
                String territoryId = entry.getKey();
                return currentTerritories.stream()
                        .filter(t -> territoryId.equals(t.getTerritoryId()))
                        .findFirst()
                        .orElse(null);
            }
        }
        return null;
    }

    /**
     * ✅ NOVO: Mostra detalhes do território
     */
    private void showTerritoryDetails(Territory territory) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        boolean isOwner = currentUser != null && currentUser.getUid().equals(territory.getOwnerId());

        String message = String.format(Locale.getDefault(),
                "Proprietário: %s\n" +
                "Área: %.0f m²\n" +
                "Pontos: %d\n" +
                "Conquistado: %s",
                territory.getOwnerName(),
                territory.getArea(),
                territory.getPointsValue(),
                android.text.format.DateFormat.format("dd/MM/yyyy HH:mm", territory.getConqueredAt())
        );

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(isOwner ? "Seu Território" : "Território de " + territory.getOwnerName())
                .setMessage(message)
                .setPositiveButton("OK", null);

        // Se não é o proprietário, adicionar opção de conquistar
        if (!isOwner && currentUser != null) {
            builder.setNegativeButton("Conquistar", (dialog, which) -> {
                attemptConquerTerritory(territory, currentUser);
            });
        }

        builder.show();
    }

    /**
     * ✅ NOVO: Tenta conquistar um território
     */
    private void attemptConquerTerritory(Territory territory, FirebaseUser currentUser) {
        new AlertDialog.Builder(this)
                .setTitle("Conquistar Território")
                .setMessage("Deseja conquistar este território? Isso custará pontos se você não estiver próximo.")
                .setPositiveButton("Sim", (dialog, which) -> {
                    viewModel.conquerTerritory(
                            territory.getTerritoryId(),
                            currentUser.getUid(),
                            currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "Usuário",
                            new com.msystem.walking.repository.TerritoryRepository.Callback<Boolean>() {
                                @Override
                                public void onResult(Boolean success) {
                                    if (success) {
                                        Toast.makeText(MainActivity.this, "Território conquistado!", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(MainActivity.this, "Falha ao conquistar território", Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onError(String error) {
                                    Toast.makeText(MainActivity.this, "Erro: " + error, Toast.LENGTH_SHORT).show();
                                }
                            }
                    );
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    /**
     * ✅ NOVO: Atualiza preview do leaderboard no bottom sheet
     */
    private void updateLeaderboardPreview(List<User> users) {
        TextView tvLeader1 = findViewById(R.id.tvLeader1);
        TextView tvLeader2 = findViewById(R.id.tvLeader2);
        TextView tvLeader3 = findViewById(R.id.tvLeader3);

        if (tvLeader1 != null && users.size() >= 1) {
            tvLeader1.setText(String.format(Locale.getDefault(),
                "1º %s - %d pts", users.get(0).getDisplayName(), users.get(0).getTotalPoints()));
        }
        if (tvLeader2 != null && users.size() >= 2) {
            tvLeader2.setText(String.format(Locale.getDefault(),
                "2º %s - %d pts", users.get(1).getDisplayName(), users.get(1).getTotalPoints()));
        }
        if (tvLeader3 != null && users.size() >= 3) {
            tvLeader3.setText(String.format(Locale.getDefault(),
                "3º %s - %d pts", users.get(2).getDisplayName(), users.get(2).getTotalPoints()));
        }
    }

    private void updateWalkingButton(boolean walking) {
        if (walking) {
            binding.fabStartWalk.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_stop));
            binding.fabStartWalk.setText("Parar Caminhada");
            binding.fabStartWalk.setBackgroundTintList(
                    ContextCompat.getColorStateList(this, R.color.black));
        } else {
            binding.fabStartWalk.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_walking));
            binding.fabStartWalk.setText("Iniciar Caminhada");
            binding.fabStartWalk.setBackgroundTintList(
                    ContextCompat.getColorStateList(this, R.color.black));
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

    /**
     * Calcula o centro de um polígono
     */
    private LatLng calculateCenter(List<LatLng> points) {
        double lat = 0, lng = 0;
        for (LatLng point : points) {
            lat += point.latitude;
            lng += point.longitude;
        }
        return new LatLng(lat / points.size(), lng / points.size());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.menu_profile) {
            // Abrir tela de perfil
            startActivity(new Intent(this, com.msystem.walking.profile.ProfileActivity.class));
            return true;
        } else if (id == R.id.menu_logout) {
            // Mostrar diálogo de logout
            showLogoutDialog();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Sair")
                .setMessage("Tem certeza que deseja sair da sua conta?")
                .setPositiveButton("Sair", (dialog, which) -> performLogout())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void performLogout() {
        // Fazer logout do Firebase
        FirebaseAuth.getInstance().signOut();

        // Redirecionar para tela de login
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
