package com.msystem.walking.leaderboard;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.msystem.walking.R;
import com.msystem.walking.model.User;
import com.msystem.walking.utils.AchievementManager;

import java.util.ArrayList;
import java.util.List;

public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.ViewHolder> {

    private List<User> users = new ArrayList<>();
    private Context context;
    private String currentUserId;
    private int sortType = 0; // 0=pontos, 1=territórios, 2=distância, 3=sequência

    public LeaderboardAdapter(Context context, String currentUserId) {
        this.context = context;
        this.currentUserId = currentUserId;
    }

    public void updateUsers(List<User> newUsers, int sortType) {
        this.users = newUsers;
        this.sortType = sortType;
        notifyDataSetChanged();
    }

    public User getUserAt(int position) {
        if (position >= 0 && position < users.size()) {
            return users.get(position);
        }
        return null;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_leaderboard, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = users.get(position);
        int rank = position + 1;

        // Configurar posição/ranking
        holder.tvRank.setText(String.valueOf(rank));

        // Configurar ícone de posição
        if (rank == 1) {
            holder.ivRankIcon.setImageResource(R.drawable.ic_trophy_gold);
            holder.ivRankIcon.setVisibility(View.VISIBLE);
        } else if (rank == 2) {
            holder.ivRankIcon.setImageResource(R.drawable.ic_trophy_silver);
            holder.ivRankIcon.setVisibility(View.VISIBLE);
        } else if (rank == 3) {
            holder.ivRankIcon.setImageResource(R.drawable.ic_trophy_bronze);
            holder.ivRankIcon.setVisibility(View.VISIBLE);
        } else {
            holder.ivRankIcon.setVisibility(View.GONE);
        }

        // Configurar nome do usuário
        holder.tvUsername.setText(user.getDisplayName() != null ? user.getDisplayName() : "Caminhante");

        // Calcular e mostrar nível
        int userLevel = AchievementManager.calculateUserLevel(user.getTotalPoints());
        holder.tvLevel.setText("Nível " + userLevel);

        // Configurar estatística principal baseada no tipo de ordenação
        switch (sortType) {
            case 0: // Pontos
                holder.tvMainStat.setText(String.format("%,d pontos", user.getTotalPoints()));
                holder.tvSecondaryStat.setText(String.format("%.1f km • %d territórios",
                    user.getTotalDistance(), user.getTerritoriesCount()));
                break;
            case 1: // Territórios
                holder.tvMainStat.setText(String.format("%d territórios", user.getTerritoriesCount()));
                holder.tvSecondaryStat.setText(String.format("%,d pontos • %.1f km",
                    user.getTotalPoints(), user.getTotalDistance()));
                break;
            case 2: // Distância
                holder.tvMainStat.setText(String.format("%.1f km", user.getTotalDistance()));
                holder.tvSecondaryStat.setText(String.format("%,d pontos • %d territórios",
                    user.getTotalPoints(), user.getTerritoriesCount()));
                break;
            case 3: // Sequência
                holder.tvMainStat.setText(String.format("%d dias seguidos", user.getCurrentStreak()));
                holder.tvSecondaryStat.setText(String.format("%,d pontos • %.1f km",
                    user.getTotalPoints(), user.getTotalDistance()));
                break;
        }

        // Destacar usuário atual
        if (user.getUserId().equals(currentUserId)) {
            holder.itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.current_user_highlight));
            holder.tvUsername.setText(holder.tvUsername.getText() + " (Você)");
        } else {
            holder.itemView.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
        }

        // Configurar ícone de atividade (online/offline)
        if (isUserActiveToday(user)) {
            holder.ivActivityStatus.setImageResource(R.drawable.ic_circle_green);
            holder.ivActivityStatus.setVisibility(View.VISIBLE);
        } else {
            holder.ivActivityStatus.setImageResource(R.drawable.ic_circle_gray);
            holder.ivActivityStatus.setVisibility(View.VISIBLE);
        }
    }

    private boolean isUserActiveToday(User user) {
        // Verificar se o usuário foi ativo hoje (simplificado)
        return user.getTodayDistance() > 0;
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRank, tvUsername, tvLevel, tvMainStat, tvSecondaryStat;
        ImageView ivRankIcon, ivActivityStatus;

        ViewHolder(View itemView) {
            super(itemView);
            tvRank = itemView.findViewById(R.id.tvRank);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            tvLevel = itemView.findViewById(R.id.tvLevel);
            tvMainStat = itemView.findViewById(R.id.tvMainStat);
            tvSecondaryStat = itemView.findViewById(R.id.tvSecondaryStat);
            ivRankIcon = itemView.findViewById(R.id.ivRankIcon);
            ivActivityStatus = itemView.findViewById(R.id.ivActivityStatus);
        }
    }
}
