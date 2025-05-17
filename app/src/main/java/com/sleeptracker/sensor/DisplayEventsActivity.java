package com.sleeptracker.sensor;

import static android.content.Context.MODE_PRIVATE;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.sleeptracker.R;

public class DisplayEventsActivity extends AppCompatActivity {

    private SharedPreferences prefs;
    private TextView tvHistory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_events);

        tvHistory = findViewById(R.id.tvHistory);
        prefs = getSharedPreferences("sleepEvents", MODE_PRIVATE);

        long timeSaved = prefs.getLong("timestamp", 0);
        long now = System.currentTimeMillis();

        if (now - timeSaved < 86400000) { // less than 24 hours
            String logs = prefs.getString("events", "No events");
            tvHistory.setText(logs.replace(";", "\n"));
        } else {
            tvHistory.setText("No recent sleep events.");
        }
    }
}
