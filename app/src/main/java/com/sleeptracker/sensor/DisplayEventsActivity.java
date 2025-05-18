package com.sleeptracker.sensor;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.button.MaterialButton;
import com.sleeptracker.R;
import com.sleeptracker.api.ApiClient;
import com.sleeptracker.api.ApiService;
import com.sleeptracker.model.SleepSession;
import com.sleeptracker.utils.SessionManager;

import org.json.JSONArray;
import org.json.JSONException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DisplayEventsActivity extends AppCompatActivity {

    private static final String TAG = "DisplayEventsActivity";
    private TextView tvSessionStart, tvSessionEnd, tvSessionDuration, tvEventCount, tvNoEvents;
    private BarChart eventsChart;
    private RecyclerView recyclerViewEvents;
    private MaterialButton btnBack;
    private ApiService apiService;

    private final SimpleDateFormat sdfInput = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    private final SimpleDateFormat sdfDisplay = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    private SleepSession currentSession;
    private ArrayList<String> eventsList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_events);

        // Initialize views
        tvSessionStart = findViewById(R.id.tvSessionStart);
        tvSessionEnd = findViewById(R.id.tvSessionEnd);
        tvSessionDuration = findViewById(R.id.tvSessionDuration);
        tvEventCount = findViewById(R.id.tvEventCount);
        tvNoEvents = findViewById(R.id.tvNoEvents);
        eventsChart = findViewById(R.id.eventsChart);
        recyclerViewEvents = findViewById(R.id.recyclerViewEvents);
        btnBack = findViewById(R.id.btnBack);

        ImageButton backArrowButton = findViewById(R.id.btnBackArrow);
        backArrowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // or your navigation method
            }
        });

        // Set up RecyclerView
        recyclerViewEvents.setLayoutManager(new LinearLayoutManager(this));

        // Back button listener
        btnBack.setOnClickListener(v -> finish());

        // Initialize API service
        apiService = ApiClient.getClient().create(ApiService.class);

        // Fetch the latest sleep session for the current user
        fetchLatestSession();
    }

    private void fetchLatestSession() {
        SessionManager sessionManager = new SessionManager(this);
        int userId = sessionManager.getUserId();

        // Show a loading state
        tvNoEvents.setText("Loading session data...");
        tvNoEvents.setVisibility(View.VISIBLE);

        apiService.getSessions(userId).enqueue(new Callback<List<SleepSession>>() {
            @Override
            public void onResponse(Call<List<SleepSession>> call, Response<List<SleepSession>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    // Get the latest session (assuming the API returns sorted data with newest first)
                    currentSession = response.body().get(0);
                    displaySessionData(currentSession);
                } else {
                    tvNoEvents.setText("No sleep sessions found");
                    tvNoEvents.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<List<SleepSession>> call, Throwable t) {
                Log.e(TAG, "API call failed", t);
                Toast.makeText(DisplayEventsActivity.this,
                        "Failed to load session data: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
                tvNoEvents.setText("Failed to load session data");
                tvNoEvents.setVisibility(View.VISIBLE);
            }
        });
    }

    private void displaySessionData(SleepSession session) {
        try {
            // Parse session data
            Date startDate = sdfInput.parse(session.getStart());
            Date endDate = sdfInput.parse(session.getStop());

            if (startDate != null && endDate != null) {
                // Display session times
                tvSessionStart.setText(sdfDisplay.format(startDate));
                tvSessionEnd.setText(sdfDisplay.format(endDate));

                // Calculate and display duration
                long durationMillis = endDate.getTime() - startDate.getTime();
                long hours = TimeUnit.MILLISECONDS.toHours(durationMillis);
                long minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis) % 60;
                tvSessionDuration.setText(String.format(Locale.getDefault(),
                        "%d hours %d minutes", hours, minutes));

                // Parse events from JSON
                parseEvents(session.getEvents());

                // Display event count
                tvEventCount.setText(String.valueOf(eventsList.size()));

                // Display events in chart and recycler view
                if (eventsList.isEmpty()) {
                    tvNoEvents.setVisibility(View.VISIBLE);
                    eventsChart.setVisibility(View.GONE);
                } else {
                    tvNoEvents.setVisibility(View.GONE);
                    eventsChart.setVisibility(View.VISIBLE);

                    // Setup chart and recycler view
                    setupEventsChart(startDate);
                    setupEventsRecyclerView();
                }
            }
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing dates", e);
            Toast.makeText(this, "Error parsing session dates", Toast.LENGTH_SHORT).show();
        }
    }

    private void parseEvents(String eventsJson) {
        eventsList.clear();

        try {
            if (eventsJson != null && !eventsJson.equals("[]")) {
                JSONArray jsonArray = new JSONArray(eventsJson);
                for (int i = 0; i < jsonArray.length(); i++) {
                    eventsList.add(jsonArray.getString(i));
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing events JSON", e);
            Toast.makeText(this, "Error parsing events data", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupEventsChart(Date startDate) {
        try {
            // Group events by hour
            Map<String, Map<String, Integer>> eventCounts = new HashMap<>();
            eventCounts.put("Sound", new TreeMap<>());
            eventCounts.put("Movement", new TreeMap<>());

            // Set up time slots for the chart (hourly bins)
            long sessionStartMillis = startDate.getTime();
            for (String event : eventsList) {
                String timeStr = null;
                String category = null;

                // Extract time and category from event string
                if (event.contains("Sound detected at ")) {
                    timeStr = event.substring("Sound detected at ".length(),
                            event.indexOf(" (Amplitude"));
                    category = "Sound";
                } else if (event.contains("Movement detected at ")) {
                    timeStr = event.substring("Movement detected at ".length(),
                            event.indexOf(" (Intensity"));
                    category = "Movement";
                }

                if (timeStr != null) {
                    try {
                        // Get hour mark for binning
                        String hourMark = timeStr.substring(0, 2) + ":00";

                        // Increment event count for this hour and category
                        Map<String, Integer> categoryMap = eventCounts.get(category);
                        categoryMap.put(hourMark, categoryMap.getOrDefault(hourMark, 0) + 1);
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing time: " + timeStr, e);
                    }
                }
            }

            // Create chart entries
            List<BarEntry> soundEntries = new ArrayList<>();
            List<BarEntry> movementEntries = new ArrayList<>();
            List<String> timeLabels = new ArrayList<>();

            // Combine all hour marks from both categories
            TreeMap<String, Integer> allHours = new TreeMap<>();
            for (String hourMark : eventCounts.get("Sound").keySet()) {
                allHours.put(hourMark, 0);
            }
            for (String hourMark : eventCounts.get("Movement").keySet()) {
                allHours.put(hourMark, 0);
            }

            // If no events, add at least the start hour
            if (allHours.isEmpty()) {
                SimpleDateFormat hourFormat = new SimpleDateFormat("HH:00", Locale.getDefault());
                allHours.put(hourFormat.format(startDate), 0);
            }

            // Create chart entries
            int index = 0;
            for (String hourMark : allHours.keySet()) {
                timeLabels.add(hourMark);
                soundEntries.add(new BarEntry(index, eventCounts.get("Sound").getOrDefault(hourMark, 0)));
                movementEntries.add(new BarEntry(index, eventCounts.get("Movement").getOrDefault(hourMark, 0)));
                index++;
            }

            // Create datasets
            BarDataSet soundDataSet = new BarDataSet(soundEntries, "Sound Events");
            soundDataSet.setColor(ColorTemplate.rgb("#FF9800"));  // Orange color

            BarDataSet movementDataSet = new BarDataSet(movementEntries, "Movement Events");
            movementDataSet.setColor(ColorTemplate.rgb("#2196F3"));  // Blue color

            // Create bar data
            BarData barData = new BarData(soundDataSet, movementDataSet);
            barData.setBarWidth(0.35f);

            // Set up chart
            eventsChart.setData(barData);
            eventsChart.getDescription().setEnabled(false);
            eventsChart.setDrawGridBackground(false);
            eventsChart.getLegend().setEnabled(true);

            // X-axis setup
            XAxis xAxis = eventsChart.getXAxis();
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setGranularity(1f);
            xAxis.setValueFormatter(new IndexAxisValueFormatter(timeLabels));
            xAxis.setLabelRotationAngle(45f);

            // Group the bars
            float groupSpace = 0.1f;
            float barSpace = 0.05f;
            eventsChart.groupBars(0, groupSpace, barSpace);

            // Refresh chart
            eventsChart.invalidate();

        } catch (Exception e) {
            Log.e(TAG, "Error setting up chart", e);
            Toast.makeText(this, "Error creating events chart", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupEventsRecyclerView() {
        EventsAdapter adapter = new EventsAdapter(eventsList);
        recyclerViewEvents.setAdapter(adapter);
    }

    // Inner class for RecyclerView adapter
    private class EventsAdapter extends RecyclerView.Adapter<EventsAdapter.EventViewHolder> {

        private final List<String> events;

        public EventsAdapter(List<String> events) {
            this.events = events;
        }

        @Override
        public EventViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_event, parent, false);
            return new EventViewHolder(view);
        }

        @Override
        public void onBindViewHolder(EventViewHolder holder, int position) {
            String event = events.get(position);
            holder.bind(event, position);
        }

        @Override
        public int getItemCount() {
            return events.size();
        }

        class EventViewHolder extends RecyclerView.ViewHolder {
            private final TextView tvEventText;
            private final TextView tvEventTime;
            private final View eventIndicator;

            public EventViewHolder(View itemView) {
                super(itemView);
                tvEventText = itemView.findViewById(R.id.tvEventText);
                tvEventTime = itemView.findViewById(R.id.tvEventTime);
                eventIndicator = itemView.findViewById(R.id.eventIndicator);
            }



            public void bind(String event, int position) {
                // Extract event type and time
                String eventType = null;
                String eventTime = null;
                String eventValue = null;

                if (event.contains("Sound detected at ")) {
                    eventType = "Sound";
                    eventTime = extractTime(event, "Sound detected at ");
                    eventValue = extractValue(event, "Amplitude: ", ")");
                } else if (event.contains("Movement detected at ")) {
                    eventType = "Movement";
                    eventTime = extractTime(event, "Movement detected at ");
                    eventValue = extractValue(event, "Intensity: ", ")");
                } else {
                    // Handle other event types or errors
                    eventType = "Other";
                    eventTime = "";
                    eventValue = event;
                }

                // Set the event text and details
                if ("Sound".equals(eventType)) {
                    tvEventText.setText("Sound Event (Amplitude: " + eventValue + ")");
                    eventIndicator.setBackgroundResource(R.drawable.sound_indicator);
                } else if ("Movement".equals(eventType)) {
                    tvEventText.setText("Movement Event (Intensity: " + eventValue + ")");
                    eventIndicator.setBackgroundResource(R.drawable.movement_indicator);
                } else {
                    tvEventText.setText(event);
                    eventIndicator.setBackgroundResource(R.drawable.other_indicator);
                }

                tvEventTime.setText(eventTime);
            }

            private String extractTime(String event, String prefix) {
                try {
                    int startIndex = event.indexOf(prefix) + prefix.length();
                    return event.substring(startIndex, startIndex + 8); // "HH:mm:ss" is 8 chars
                } catch (Exception e) {
                    Log.e(TAG, "Error extracting time from event: " + event, e);
                    return "";
                }
            }

            private String extractValue(String event, String prefix, String suffix) {
                try {
                    int startIndex = event.indexOf(prefix) + prefix.length();
                    int endIndex = event.indexOf(suffix, startIndex);
                    return event.substring(startIndex, endIndex);
                } catch (Exception e) {
                    Log.e(TAG, "Error extracting value from event: " + event, e);
                    return "";
                }
            }

        }
    }
}