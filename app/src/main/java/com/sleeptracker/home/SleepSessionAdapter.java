package com.sleeptracker.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.sleeptracker.R;
import com.sleeptracker.model.SleepSession;

import java.util.List;

public class SleepSessionAdapter extends RecyclerView.Adapter<SleepSessionAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(SleepSession session);
    }

    private final List<SleepSession> sessions;
    private final OnItemClickListener listener;

    public SleepSessionAdapter(List<SleepSession> sessions, OnItemClickListener listener) {
        this.sessions = sessions;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SleepSessionAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_session, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull SleepSessionAdapter.ViewHolder holder, int position) {
        SleepSession session = sessions.get(position);
        holder.tvStart.setText("Start: " + session.getStart());
        holder.tvStop.setText("Stop: " + session.getStop());

        holder.itemView.setOnClickListener(v -> listener.onItemClick(session));
    }

    @Override
    public int getItemCount() {
        return sessions.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvStart, tvStop;

        ViewHolder(View view) {
            super(view);
            tvStart = view.findViewById(R.id.tvStart);
            tvStop = view.findViewById(R.id.tvStop);
        }
    }
}
