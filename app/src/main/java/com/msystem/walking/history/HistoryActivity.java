package com.msystem.walking.history;

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

import com.google.firebase.auth.FirebaseAuth;
import com.msystem.walking.R;
import com.msystem.walking.databinding.ActivityHistoryBinding;
import com.msystem.walking.model.Activity;
import com.msystem.walking.repository.DataRepository;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class HistoryActivity extends AppCompatActivity {

    private ActivityHistoryBinding binding;
    private DataRepository dataRepository;
    private HistoryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dataRepository = DataRepository.getInstance();

        setupToolbar();
        setupRecyclerView();
        setupObservers();
        loadUserActivities();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupRecyclerView() {
        adapter = new HistoryAdapter();
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);
    }

    private void setupObservers() {
        dataRepository.getActivitiesLiveData().observe(this, new Observer<List<Activity>>() {
            @Override
            public void onChanged(List<Activity> activities) {
                adapter.setActivities(activities);
                if (activities == null || activities.isEmpty()) {
                    binding.tvEmptyMessage.setVisibility(View.VISIBLE);
                    binding.recyclerView.setVisibility(View.GONE);
                } else {
                    binding.tvEmptyMessage.setVisibility(View.GONE);
                    binding.recyclerView.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void loadUserActivities() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        dataRepository.getUserActivities(userId);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private static class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
        private List<Activity> activities;
        private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

        public void setActivities(List<Activity> activities) {
            this.activities = activities;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_history, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            if (activities != null && position < activities.size()) {
                Activity activity = activities.get(position);
                holder.bind(activity, dateFormat);
            }
        }

        @Override
        public int getItemCount() {
            return activities != null ? activities.size() : 0;
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            private TextView tvDate;
            private TextView tvType;
            private TextView tvDistance;
            private TextView tvDuration;
            private TextView tvPoints;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvDate = itemView.findViewById(R.id.tvDate);
                tvType = itemView.findViewById(R.id.tvType);
                tvDistance = itemView.findViewById(R.id.tvDistance);
                tvDuration = itemView.findViewById(R.id.tvDuration);
                tvPoints = itemView.findViewById(R.id.tvPoints);
            }

            public void bind(Activity activity, SimpleDateFormat dateFormat) {
                if (activity.getStartTime() != null) {
                    tvDate.setText(dateFormat.format(activity.getStartTime()));
                }

                String type = activity.getType() != null ? activity.getType() : "walking";
                tvType.setText(type.equals("running") ? "Corrida" : "Caminhada");

                tvDistance.setText(String.format("%.2f km", activity.getDistance()));

                // Converter duração de milissegundos para formato legível
                long minutes = activity.getDuration() / (1000 * 60);
                long seconds = (activity.getDuration() / 1000) % 60;
                tvDuration.setText(String.format("%02d:%02d", minutes, seconds));

                tvPoints.setText(String.format("%d pontos", activity.getPointsEarned()));
            }
        }
    }
}
