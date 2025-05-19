package com.sleeptracker.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sleeptracker.R;
import com.sleeptracker.model.SleepInsight;

import java.util.List;

public class InsightsAdapter extends RecyclerView.Adapter<InsightsAdapter.InsightViewHolder> {

    private final List<SleepInsight> insights;

    public InsightsAdapter(List<SleepInsight> insights) {
        this.insights = insights;
    }

    @NonNull
    @Override
    public InsightViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_insight, parent, false);
        return new InsightViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull InsightViewHolder holder, int position) {
        SleepInsight insight = insights.get(position);
        holder.tvInsightTitle.setText(insight.getTitle());
        holder.tvInsightDescription.setText(insight.getDescription());
    }

    @Override
    public int getItemCount() {
        return insights.size();
    }

    static class InsightViewHolder extends RecyclerView.ViewHolder {
        TextView tvInsightTitle;
        TextView tvInsightDescription;

        public InsightViewHolder(@NonNull View itemView) {
            super(itemView);
            tvInsightTitle = itemView.findViewById(R.id.tvInsightTitle);
            tvInsightDescription = itemView.findViewById(R.id.tvInsightDescription);
        }
    }
}