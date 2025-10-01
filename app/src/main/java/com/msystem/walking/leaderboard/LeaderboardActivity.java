package com.msystem.walking.leaderboard;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.msystem.walking.R;
import com.msystem.walking.databinding.ActivityLeaderboardBinding;
import com.msystem.walking.model.User;
import com.msystem.walking.repository.DataRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LeaderboardActivity extends AppCompatActivity {
    private static final String TAG = "LeaderboardActivity";

    private ActivityLeaderboardBinding binding;
    private DataRepository dataRepository;
    private LeaderboardAdapter adapter;
    private List<User> allUsers = new ArrayList<>();
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLeaderboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Configurar ActionBar
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("🏆 Ranking");
        }

        dataRepository = DataRepository.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        setupRecyclerView();
        setupTabs();
        loadLeaderboard();
    }

    private void setupRecyclerView() {
        adapter = new LeaderboardAdapter(this, currentUserId);
        binding.recyclerViewLeaderboard.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerViewLeaderboard.setAdapter(adapter);
    }

    private void setupTabs() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("🏆 Pontos"));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("🗺️ Territórios"));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("🚶 Distância"));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("🔥 Sequência"));

        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                sortLeaderboard(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void loadLeaderboard() {
        binding.progressBar.setVisibility(View.VISIBLE);

        dataRepository.getAllUsers(users -> {
            if (users != null && !users.isEmpty()) {
                allUsers.clear();
                allUsers.addAll(users);
                sortLeaderboard(0); // Ordenar por pontos por padrão

                binding.progressBar.setVisibility(View.GONE);
                binding.recyclerViewLeaderboard.setVisibility(View.VISIBLE);

                // Mostrar posição do usuário atual
                showCurrentUserPosition();
            } else {
                binding.progressBar.setVisibility(View.GONE);
                binding.tvEmptyMessage.setVisibility(View.VISIBLE);
                binding.tvEmptyMessage.setText("Nenhum usuário encontrado.\nSeja o primeiro a caminhar!");
            }
        });
    }

    private void sortLeaderboard(int tabPosition) {
        List<User> sortedUsers = new ArrayList<>(allUsers);

        switch (tabPosition) {
            case 0: // Pontos
                Collections.sort(sortedUsers, (u1, u2) ->
                    Integer.compare(u2.getTotalPoints(), u1.getTotalPoints()));
                break;
            case 1: // Territórios
                Collections.sort(sortedUsers, (u1, u2) ->
                    Integer.compare(u2.getTerritoriesCount(), u1.getTotalPoints()));
                break;
            case 2: // Distância
                Collections.sort(sortedUsers, (u1, u2) ->
                    Double.compare(u2.getTotalDistance(), u1.getTotalDistance()));
                break;
            case 3: // Sequência
                Collections.sort(sortedUsers, (u1, u2) ->
                    Integer.compare(u2.getCurrentStreak(), u1.getCurrentStreak()));
                break;
        }

        adapter.updateUsers(sortedUsers, tabPosition);
        showCurrentUserPosition();
    }

    private void showCurrentUserPosition() {
        for (int i = 0; i < adapter.getItemCount(); i++) {
            User user = adapter.getUserAt(i);
            if (user != null && user.getUserId().equals(currentUserId)) {
                binding.tvCurrentPosition.setText(
                    String.format("Sua posição: #%d de %d", i + 1, adapter.getItemCount())
                );
                binding.tvCurrentPosition.setVisibility(View.VISIBLE);
                break;
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Recarregar dados quando voltar à tela
        loadLeaderboard();
    }
}
