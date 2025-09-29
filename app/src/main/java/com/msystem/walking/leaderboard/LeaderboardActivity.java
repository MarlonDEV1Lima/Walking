package com.msystem.walking.leaderboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.msystem.walking.R;
import com.msystem.walking.databinding.ActivityLeaderboardBinding;
import com.msystem.walking.model.User;
import com.msystem.walking.repository.DataRepository;

import java.util.List;

public class LeaderboardActivity extends AppCompatActivity {

    private ActivityLeaderboardBinding binding;
    private DataRepository dataRepository;
    private LeaderboardAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLeaderboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dataRepository = DataRepository.getInstance();

        setupRecyclerView();
        setupObservers();
        loadLeaderboard();
    }

    private void setupRecyclerView() {
        adapter = new LeaderboardAdapter();
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);
    }

    private void setupObservers() {
        dataRepository.getLeaderboardLiveData().observe(this, new Observer<List<User>>() {
            @Override
            public void onChanged(List<User> users) {
                adapter.setUsers(users);
            }
        });
    }

    private void loadLeaderboard() {
        dataRepository.getLeaderboard(50); // Top 50 usuários
    }

    private static class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.ViewHolder> {
        private List<User> users;

        public void setUsers(List<User> users) {
            this.users = users;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_leaderboard, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            if (users != null && position < users.size()) {
                User user = users.get(position);
                holder.bind(user, position + 1);
            }
        }

        @Override
        public int getItemCount() {
            return users != null ? users.size() : 0;
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            private TextView tvPosition;
            private TextView tvName;
            private TextView tvPoints;
            private TextView tvDistance;
            private TextView tvTerritories;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvPosition = itemView.findViewById(R.id.tvPosition);
                tvName = itemView.findViewById(R.id.tvName);
                tvPoints = itemView.findViewById(R.id.tvPoints);
                tvDistance = itemView.findViewById(R.id.tvDistance);
                tvTerritories = itemView.findViewById(R.id.tvTerritories);
            }

            public void bind(User user, int position) {
                tvPosition.setText(String.valueOf(position));
                tvName.setText(user.getDisplayName() != null ? user.getDisplayName() : "Usuário");
                tvPoints.setText(String.format("%d pts", user.getTotalPoints()));
                tvDistance.setText(String.format("%.1f km", user.getTotalDistance()));
                tvTerritories.setText(String.format("%d territórios", user.getConqueredTerritories()));

                // Destacar top 3
                if (position <= 3) {
                    itemView.setBackgroundResource(R.color.design_default_color_primary_variant);
                } else {
                    itemView.setBackgroundResource(android.R.color.transparent);
                }
            }
        }
    }
}
