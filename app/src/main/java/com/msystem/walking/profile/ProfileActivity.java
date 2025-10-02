package com.msystem.walking.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.msystem.walking.auth.LoginActivity;
import com.msystem.walking.databinding.ActivityProfileBinding;
import com.msystem.walking.history.HistoryActivity;
import com.msystem.walking.leaderboard.LeaderboardActivity;
import com.msystem.walking.model.User;
import com.msystem.walking.repository.AuthRepository;
import com.msystem.walking.repository.DataRepository;

import java.util.Locale;

public class ProfileActivity extends AppCompatActivity {
    private ActivityProfileBinding binding;
    private AuthRepository authRepository;
    private DataRepository dataRepository;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Configurar toolbar
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        authRepository = AuthRepository.getInstance();
        dataRepository = DataRepository.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        setupClickListeners();
        loadUserData();
    }

    private void setupClickListeners() {
        // Navegação - voltar
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        // Histórico
        binding.btnHistory.setOnClickListener(v -> {
            startActivity(new Intent(this, HistoryActivity.class));
        });

        // Ranking
        binding.btnLeaderboard.setOnClickListener(v -> {
            startActivity(new Intent(this, LeaderboardActivity.class));
        });

        // Logout
        binding.btnLogout.setOnClickListener(v -> showLogoutDialog());
    }

    private void loadUserData() {
        if (currentUser != null) {
            // Carregar dados básicos do usuário
            binding.tvUserName.setText(currentUser.getDisplayName() != null ?
                currentUser.getDisplayName() : "Usuário");
            binding.tvUserEmail.setText(currentUser.getEmail());

            // Carregar estatísticas do usuário usando método que existe
            showLoading(true);
            dataRepository.getUserById(currentUser.getUid(), new DataRepository.Callback<User>() {
                @Override
                public void onResult(User user) {
                    showLoading(false);
                    if (user != null) {
                        updateUserStats(user);
                    } else {
                        // Carregar dados padrão se usuário não encontrado
                        binding.tvTotalPoints.setText("0");
                        binding.tvTotalTerritories.setText("0");
                        binding.tvTotalDistance.setText("0.0");
                    }
                }
            });
        }
    }

    private void updateUserStats(User user) {
        binding.tvTotalPoints.setText(String.valueOf(user.getTotalPoints()));
        binding.tvTotalTerritories.setText(String.valueOf(user.getTerritoriesCount()));
        binding.tvTotalDistance.setText(String.format(Locale.getDefault(), "%.1f", user.getTotalDistance()));
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
        showLoading(true);

        // Fazer logout do Firebase
        FirebaseAuth.getInstance().signOut();

        // Fazer logout do Google (se logado)
        if (authRepository.getGoogleSignInClient() != null) {
            authRepository.getGoogleSignInClient().signOut().addOnCompleteListener(task -> {
                showLoading(false);
                // Redirecionar para tela de login
                Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        } else {
            showLoading(false);
            // Redirecionar para tela de login
            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }

    private void showLoading(boolean show) {
        binding.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.btnHistory.setEnabled(!show);
        binding.btnLeaderboard.setEnabled(!show);
        binding.btnLogout.setEnabled(!show);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Recarregar dados quando voltar à tela
        loadUserData();
    }
}
