package com.sleeptracker.sensor;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.sleeptracker.R;
import com.sleeptracker.adapter.InsightsAdapter;
import com.sleeptracker.analysis.SleepAnalysisManager;
import com.sleeptracker.api.ApiClient;
import com.sleeptracker.api.ApiService;
import com.sleeptracker.model.SleepInsight;
import com.sleeptracker.model.SleepSession;
import com.sleeptracker.utils.SessionManager;

import org.json.JSONArray;
import org.json.JSONException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
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
    private LineChart eventsChart;
    private RecyclerView recyclerViewEvents;
    private MaterialButton btnBack;
    private ApiService apiService;

    // AI Sleep Analysis components
    private PieChart sleepStagesChart;
    private CircularProgressIndicator sleepQualityIndicator;
    private TextView tvSleepScore, tvSleepQualityDesc, tvNoAnalysis;
    private RecyclerView recyclerViewInsights;
    private MaterialButton btnGenerateAnalysis;
    private ProgressBar progressAnalysis;
    private SleepAnalysisManager analysisManager;
    private List<SleepInsight> insights = new ArrayList<>();

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

        // Initialize AI analysis views
        sleepStagesChart = findViewById(R.id.sleepStagesChart);
        sleepQualityIndicator = findViewById(R.id.sleepQualityIndicator);
        tvSleepScore = findViewById(R.id.tvSleepScore);
        tvSleepQualityDesc = findViewById(R.id.tvSleepQualityDesc);
        tvNoAnalysis = findViewById(R.id.tvNoAnalysis);
        recyclerViewInsights = findViewById(R.id.recyclerViewInsights);
        btnGenerateAnalysis = findViewById(R.id.btnGenerateAnalysis);
        progressAnalysis = findViewById(R.id.progressAnalysis);

        // Initialize analysis manager
        analysisManager = new SleepAnalysisManager(this);

        ImageButton backArrowButton = findViewById(R.id.btnBackArrow);
        backArrowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // or your navigation method
            }
        });

        // Set up RecyclerView
        recyclerViewEvents.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewInsights.setLayoutManager(new LinearLayoutManager(this));

        // Back button listener
        btnBack.setOnClickListener(v -> finish());

        // Generate analysis button listener
        btnGenerateAnalysis.setOnClickListener(v -> generateSleepAnalysis());

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

                // Reset AI analysis views to default state
                resetAnalysisViews();
            }
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing dates", e);
            Toast.makeText(this, "Error parsing session dates", Toast.LENGTH_SHORT).show();
        }
    }

    private void resetAnalysisViews() {
        // Reset sleep quality indicator
        sleepQualityIndicator.setProgress(0);
        tvSleepScore.setText("--");
        tvSleepQualityDesc.setText("Not analyzed");

        // Reset sleep stages chart
        sleepStagesChart.clear();
        sleepStagesChart.setNoDataText("No analysis data");
        sleepStagesChart.invalidate();

        // Reset insights
        insights.clear();
        recyclerViewInsights.setAdapter(new InsightsAdapter(insights));

        // Show message and enable button
        tvNoAnalysis.setVisibility(View.VISIBLE);
        tvNoAnalysis.setText("Click 'Generate Analysis' to analyze your sleep pattern");
        btnGenerateAnalysis.setEnabled(true);
        progressAnalysis.setVisibility(View.GONE);
    }

    private void generateSleepAnalysis() {
        if (currentSession == null) {
            Toast.makeText(this, "No sleep session data available", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading state
        progressAnalysis.setVisibility(View.VISIBLE);
        tvNoAnalysis.setText("Analyzing your sleep pattern...");
        tvNoAnalysis.setVisibility(View.VISIBLE);
        btnGenerateAnalysis.setEnabled(false);

        // Use a background thread for analysis to avoid blocking UI
        new Thread(() -> {
            // Calculate sleep quality score
            int sleepScore = analysisManager.calculateSleepQualityScore(currentSession);
            String qualityDesc = analysisManager.getSleepQualityDescription(sleepScore);

            // Generate sleep stage data
            PieData sleepStagesData = analysisManager.analyzeSleepStages(currentSession);

            // Generate insights
            insights = analysisManager.generateInsights(currentSession);

            // Update UI on main thread
            runOnUiThread(() -> {
                // Update sleep quality indicator
                sleepQualityIndicator.setProgress(sleepScore);
                tvSleepScore.setText(String.valueOf(sleepScore));
                tvSleepQualityDesc.setText(qualityDesc);

                // Set up sleep stages chart
                sleepStagesChart.setData(sleepStagesData);
                sleepStagesChart.getDescription().setEnabled(false);
                sleepStagesChart.setUsePercentValues(true);
                sleepStagesChart.setDrawHoleEnabled(true);
                sleepStagesChart.setHoleColor(android.graphics.Color.WHITE);
                sleepStagesChart.setHoleRadius(40f);
                sleepStagesChart.setTransparentCircleRadius(45f);
                sleepStagesChart.setDrawEntryLabels(true);
                sleepStagesChart.setEntryLabelTextSize(12f);
                sleepStagesChart.setEntryLabelColor(android.graphics.Color.WHITE);
                sleepStagesChart.getLegend().setEnabled(true);
                sleepStagesChart.getLegend().setTextSize(12f);
                sleepStagesChart.getLegend().setWordWrapEnabled(true);
                sleepStagesChart.animateY(1000);

                // Set up insights recycler view
                InsightsAdapter insightsAdapter = new InsightsAdapter(insights);
                recyclerViewInsights.setAdapter(insightsAdapter);

                // Hide loading indicators
                progressAnalysis.setVisibility(View.GONE);
                tvNoAnalysis.setVisibility(insights.isEmpty() ? View.VISIBLE : View.GONE);
                if (insights.isEmpty()) {
                    tvNoAnalysis.setText("No insights available");
                }

                btnGenerateAnalysis.setEnabled(true);
            });
        }).start();
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
            // Parse and extract event times from events list
            List<Date> soundEventTimes = new ArrayList<>();
            List<Date> movementEventTimes = new ArrayList<>();
            List<Float> soundIntensities = new ArrayList<>();
            List<Float> movementIntensities = new ArrayList<>();

            // Get start and end time of session for X-axis bounds
            Date sessionStartDate = sdfInput.parse(currentSession.getStart());
            Date sessionEndDate = sdfInput.parse(currentSession.getStop());

            // Calculate total duration in milliseconds for scaling
            long sessionDurationMillis = sessionEndDate.getTime() - sessionStartDate.getTime();

            // Parse events and extract timestamps
            for (String event : eventsList) {
                try {
                    String timeStr = null;
                    float intensity = 0f;

                    // Extract time and intensity from event string
                    if (event.contains("Sound detected at ")) {
                        timeStr = event.substring("Sound detected at ".length(),
                                event.indexOf(" (Amplitude"));
                        String amplitudeStr = extractValue(event, "Amplitude: ", ")");
                        intensity = Float.parseFloat(amplitudeStr);

                        Date eventTime = timeFormat.parse(timeStr);
                        if (eventTime != null) {
                            soundEventTimes.add(eventTime);
                            soundIntensities.add(intensity);
                        }
                    } else if (event.contains("Movement detected at ")) {
                        timeStr = event.substring("Movement detected at ".length(),
                                event.indexOf(" (Intensity"));
                        String intensityStr = extractValue(event, "Intensity: ", ")");
                        intensity = Float.parseFloat(intensityStr);

                        Date eventTime = timeFormat.parse(timeStr);
                        if (eventTime != null) {
                            movementEventTimes.add(eventTime);
                            movementIntensities.add(intensity);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing event: " + event, e);
                }
            }

            // Create chart entries based on timeline positions
            List<Entry> soundEntries = new ArrayList<>();
            List<Entry> movementEntries = new ArrayList<>();

            // Calculate x-value positions for the chart based on time elapsed since start
            // For sound events
            for (int i = 0; i < soundEventTimes.size(); i++) {
                Date eventTime = soundEventTimes.get(i);

                // Convert time string to Calendar to get hours and minutes
                Calendar eventCal = Calendar.getInstance();
                eventCal.setTime(eventTime);

                // Extract hour and minute from session start
                Calendar startCal = Calendar.getInstance();
                startCal.setTime(sessionStartDate);

                // Calculate minutes since session start
                long startTimeMinutes = startCal.get(Calendar.HOUR_OF_DAY) * 60 + startCal.get(Calendar.MINUTE);
                long eventTimeMinutes = eventCal.get(Calendar.HOUR_OF_DAY) * 60 + eventCal.get(Calendar.MINUTE);

                // Handle case where event might be next day
                if (eventTimeMinutes < startTimeMinutes) {
                    eventTimeMinutes += 24 * 60; // Add a day in minutes
                }

                // Calculate x position (0 to 1 scale)
                float minutesSinceStart = eventTimeMinutes - startTimeMinutes;
                float xPosition = minutesSinceStart / (float)(sessionDurationMillis / (1000 * 60));

                // Add entry with position and intensity
                soundEntries.add(new Entry(xPosition, soundIntensities.get(i)));
            }

            // For movement events (same logic)
            for (int i = 0; i < movementEventTimes.size(); i++) {
                Date eventTime = movementEventTimes.get(i);

                Calendar eventCal = Calendar.getInstance();
                eventCal.setTime(eventTime);

                Calendar startCal = Calendar.getInstance();
                startCal.setTime(sessionStartDate);

                long startTimeMinutes = startCal.get(Calendar.HOUR_OF_DAY) * 60 + startCal.get(Calendar.MINUTE);
                long eventTimeMinutes = eventCal.get(Calendar.HOUR_OF_DAY) * 60 + eventCal.get(Calendar.MINUTE);

                if (eventTimeMinutes < startTimeMinutes) {
                    eventTimeMinutes += 24 * 60;
                }

                float minutesSinceStart = eventTimeMinutes - startTimeMinutes;
                float xPosition = minutesSinceStart / (float)(sessionDurationMillis / (1000 * 60));

                movementEntries.add(new Entry(xPosition, movementIntensities.get(i)));
            }

            // Sort entries by X value to ensure proper line connection
            Collections.sort(soundEntries, new Comparator<Entry>() {
                @Override
                public int compare(Entry e1, Entry e2) {
                    return Float.compare(e1.getX(), e2.getX());
                }
            });

            Collections.sort(movementEntries, new Comparator<Entry>() {
                @Override
                public int compare(Entry e1, Entry e2) {
                    return Float.compare(e1.getX(), e2.getX());
                }
            });

            // Create datasets
            LineDataSet soundDataSet = new LineDataSet(soundEntries, "Sound");
            soundDataSet.setColor(ColorTemplate.rgb("#FF6D00"));
            soundDataSet.setLineWidth(2f);
            soundDataSet.setDrawFilled(false);  // No fill for clearer view of timeline
            soundDataSet.setDrawCircles(true);
            soundDataSet.setCircleColor(ColorTemplate.rgb("#FF6D00"));
            soundDataSet.setCircleHoleColor(ColorTemplate.rgb("#FFFFFF"));
            soundDataSet.setCircleRadius(4f);
            soundDataSet.setDrawValues(false);  // Hide values for cleaner timeline look
            soundDataSet.setMode(LineDataSet.Mode.LINEAR);  // Connect with lines

            LineDataSet movementDataSet = new LineDataSet(movementEntries, "Movement");
            movementDataSet.setColor(ColorTemplate.rgb("#0069C0"));
            movementDataSet.setLineWidth(2f);
            movementDataSet.setDrawFilled(false);
            movementDataSet.setDrawCircles(true);
            movementDataSet.setCircleColor(ColorTemplate.rgb("#0069C0"));
            movementDataSet.setCircleHoleColor(ColorTemplate.rgb("#FFFFFF"));
            movementDataSet.setCircleRadius(4f);
            movementDataSet.setDrawValues(false);
            movementDataSet.setMode(LineDataSet.Mode.LINEAR);

            // Create line data
            LineData lineData = new LineData();
            if (!soundEntries.isEmpty()) {
                lineData.addDataSet(soundDataSet);
            }
            if (!movementEntries.isEmpty()) {
                lineData.addDataSet(movementDataSet);
            }

            // Set up chart appearance
            eventsChart.setData(lineData);
            eventsChart.getDescription().setEnabled(false);
            eventsChart.setDrawGridBackground(false);
            eventsChart.setBackgroundColor(ColorTemplate.rgb("#FFFFFF"));
            eventsChart.setDrawBorders(false);

            // Improve legend appearance
            eventsChart.getLegend().setEnabled(true);
            eventsChart.getLegend().setForm(com.github.mikephil.charting.components.Legend.LegendForm.CIRCLE);
            eventsChart.getLegend().setTextSize(12f);
            eventsChart.getLegend().setTextColor(ColorTemplate.rgb("#333333"));

            // Interaction settings
            eventsChart.setTouchEnabled(true);
            eventsChart.setDragEnabled(true);
            eventsChart.setScaleEnabled(true);
            eventsChart.setPinchZoom(true);

            // Format X-axis as a time axis from start to end
            XAxis xAxis = eventsChart.getXAxis();
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setDrawGridLines(false);
            xAxis.setAxisMinimum(0f);
            xAxis.setAxisMaximum(1f);  // Full session duration normalized to 1.0
            xAxis.setTextSize(10f);
            xAxis.setTextColor(ColorTemplate.rgb("#333333"));

            // Custom formatter to show times based on position in sleep session
            xAxis.setValueFormatter(new com.github.mikephil.charting.formatter.ValueFormatter() {
                @Override
                public String getFormattedValue(float value) {
                    // Convert normalized position (0-1) back to time
                    long timeOffsetMillis = (long) (value * sessionDurationMillis);
                    Date pointDate = new Date(sessionStartDate.getTime() + timeOffsetMillis);
                    return timeFormat.format(pointDate);
                }
            });

            // Show time labels at start, 25%, 50%, 75% and end of sleep session
            xAxis.setLabelCount(5, true);

            // Improve Y-axis appearance
            com.github.mikephil.charting.components.YAxis leftAxis = eventsChart.getAxisLeft();
            leftAxis.setDrawLabels(true);
            leftAxis.setTextColor(ColorTemplate.rgb("#333333"));
            leftAxis.setTextSize(10f);
            leftAxis.setAxisMinimum(0f);
            leftAxis.setDrawGridLines(true);
            leftAxis.setGridColor(ColorTemplate.rgb("#EEEEEE"));

            // Remove right Y-axis
            eventsChart.getAxisRight().setEnabled(false);

            // Add some padding
            eventsChart.setExtraBottomOffset(10f);
            eventsChart.setExtraLeftOffset(10f);
            eventsChart.setExtraRightOffset(10f);

            // Add chart animation
            eventsChart.animateX(1000);

            // Refresh chart
            eventsChart.invalidate();

        } catch (Exception e) {
            Log.e(TAG, "Error setting up chart", e);
            Toast.makeText(this, "Error creating events chart", Toast.LENGTH_SHORT).show();
        }
    }

    // Helper method to extract values from event strings
    private String extractValue(String event, String prefix, String suffix) {
        try {
            int startIndex = event.indexOf(prefix) + prefix.length();
            int endIndex = event.indexOf(suffix, startIndex);
            return event.substring(startIndex, endIndex);
        } catch (Exception e) {
            Log.e(TAG, "Error extracting value from event: " + event, e);
            return "0";
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