package com.sleeptracker.sensor;

import android.os.Build;
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
import java.util.List;
import java.util.Locale;
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
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_display_events);

            // Initialize views
            tvSessionStart = findViewById(R.id.tvSessionStart);
            tvSessionEnd = findViewById(R.id.tvSessionEnd);
            tvSessionDuration = findViewById(R.id.tvSessionDuration);
            tvEventCount = findViewById(R.id.tvEventCount);
            tvNoEvents = findViewById(R.id.tvNoEvents);
            eventsChart = findViewById(R.id.eventsChart);

            // Add this to the onCreate method, right after initializing the eventsChart
            // This ensures the chart is properly configured from the start
            if (eventsChart != null) {
                eventsChart.setNoDataText("No sleep data available yet");
                eventsChart.setNoDataTextColor(ColorTemplate.rgb("#666666"));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    eventsChart.setDefaultFocusHighlightEnabled(true);
                }
                eventsChart.setHighlightPerTapEnabled(true);
                eventsChart.setTouchEnabled(true);
                Log.d(TAG, "Chart initialized in onCreate");
            }

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
            if (backArrowButton != null) {
                backArrowButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        finish(); // or your navigation method
                    }
                });
            }

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
        } catch (Exception e) {
            Log.e(TAG, "Fatal error in onCreate", e);
            Toast.makeText(this, "Error initializing activity: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void fetchLatestSession() {
        try {
            SessionManager sessionManager = new SessionManager(this);
            int userId = sessionManager.getUserId();

            // Show a loading state
            tvNoEvents.setText("Loading session data...");
            tvNoEvents.setVisibility(View.VISIBLE);

            apiService.getSessions(userId).enqueue(new Callback<List<SleepSession>>() {
                @Override
                public void onResponse(Call<List<SleepSession>> call, Response<List<SleepSession>> response) {
                    try {
                        if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                            // Get the latest session (assuming the API returns sorted data with newest first)
                            currentSession = response.body().get(0);
                            displaySessionData(currentSession);
                        } else {
                            tvNoEvents.setText("No sleep sessions found");
                            tvNoEvents.setVisibility(View.VISIBLE);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing response", e);
                        tvNoEvents.setText("Error processing session data");
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
        } catch (Exception e) {
            Log.e(TAG, "Error in fetchLatestSession", e);
            tvNoEvents.setText("Error fetching session data");
            tvNoEvents.setVisibility(View.VISIBLE);
        }
    }

    // Replace the displaySessionData method with this version that uses background threads
    private void displaySessionData(SleepSession session) {
        try {
            // Parse session data - this is lightweight and can stay on main thread
            Date startDate = sdfInput.parse(session.getStart());
            Date endDate = sdfInput.parse(session.getStop());

            if (startDate != null && endDate != null) {
                // Display session times - UI updates on main thread
                tvSessionStart.setText(sdfDisplay.format(startDate));
                tvSessionEnd.setText(sdfDisplay.format(endDate));

                // Calculate and display duration - lightweight calculation
                long durationMillis = endDate.getTime() - startDate.getTime();
                long hours = TimeUnit.MILLISECONDS.toHours(durationMillis);
                long minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis) % 60;
                tvSessionDuration.setText(String.format(Locale.getDefault(),
                        "%d hours %d minutes", hours, minutes));

                // Show loading indicator
                tvNoEvents.setText("Processing events data...");
                tvNoEvents.setVisibility(View.VISIBLE);

                // Move heavy processing to background thread
                new Thread(() -> {
                    try {
                        // Parse events from JSON - potentially heavy operation
                        parseEvents(session.getEvents());

                        // Update UI on main thread after parsing is complete
                        runOnUiThread(() -> {
                            try {
                                // Display event count
                                tvEventCount.setText(String.valueOf(eventsList.size()));

                                // Display events in chart and recycler view
                                if (eventsList.isEmpty()) {
                                    tvNoEvents.setVisibility(View.VISIBLE);
                                    eventsChart.setVisibility(View.GONE);
                                } else {
                                    tvNoEvents.setVisibility(View.GONE);
                                    checkChartVisibility();
                                    eventsChart.setVisibility(View.VISIBLE);

                                    // Setup chart and recycler view
                                    setupEventsChart(startDate);
                                    setupEventsRecyclerView();
                                }

                                // Reset AI analysis views to default state
                                resetAnalysisViews();
                            } catch (Exception e) {
                                Log.e(TAG, "Error updating UI after event processing", e);
                                tvNoEvents.setText("Error processing events data");
                                tvNoEvents.setVisibility(View.VISIBLE);
                            }
                        });
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing events in background", e);
                        runOnUiThread(() -> {
                            tvNoEvents.setText("Error processing events data");
                            tvNoEvents.setVisibility(View.VISIBLE);
                        });
                    }
                }).start();
            } else {
                throw new ParseException("Invalid date format", 0);
            }
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing dates", e);
            Toast.makeText(this, "Error parsing session dates", Toast.LENGTH_SHORT).show();
            tvNoEvents.setText("Error parsing session dates");
            tvNoEvents.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            Log.e(TAG, "Error displaying session data", e);
            Toast.makeText(this, "Error displaying session data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            tvNoEvents.setText("Error displaying session data");
            tvNoEvents.setVisibility(View.VISIBLE);
        }
    }

    private void resetAnalysisViews() {
        try {
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
        } catch (Exception e) {
            Log.e(TAG, "Error resetting analysis views", e);
        }
    }

    private void generateSleepAnalysis() {
        try {
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
                try {
                    // Calculate sleep quality score
                    int sleepScore = analysisManager.calculateSleepQualityScore(currentSession);
                    String qualityDesc = analysisManager.getSleepQualityDescription(sleepScore);

                    // Generate sleep stage data
                    PieData sleepStagesData = analysisManager.analyzeSleepStages(currentSession);

                    // Generate insights
                    insights = analysisManager.generateInsights(currentSession);

                    // Update UI on main thread
                    runOnUiThread(() -> {
                        try {
                            // Update sleep quality indicator
                            sleepQualityIndicator.setProgress(sleepScore);
                            tvSleepScore.setText(String.valueOf(sleepScore));
                            tvSleepQualityDesc.setText(qualityDesc);

                            // Set up sleep stages chart
                            if (sleepStagesData != null) {
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
                            } else {
                                sleepStagesChart.setNoDataText("No sleep stages data available");
                                sleepStagesChart.invalidate();
                            }

                            // Set up insights recycler view
                            if (insights != null && !insights.isEmpty()) {
                                InsightsAdapter insightsAdapter = new InsightsAdapter(insights);
                                recyclerViewInsights.setAdapter(insightsAdapter);
                                tvNoAnalysis.setVisibility(View.GONE);
                            } else {
                                tvNoAnalysis.setVisibility(View.VISIBLE);
                                tvNoAnalysis.setText("No insights available");
                            }

                            // Hide loading indicators
                            progressAnalysis.setVisibility(View.GONE);
                            btnGenerateAnalysis.setEnabled(true);
                        } catch (Exception e) {
                            Log.e(TAG, "Error updating UI with analysis results", e);
                            progressAnalysis.setVisibility(View.GONE);
                            tvNoAnalysis.setVisibility(View.VISIBLE);
                            tvNoAnalysis.setText("Error displaying analysis results");
                            btnGenerateAnalysis.setEnabled(true);
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error generating sleep analysis", e);
                    runOnUiThread(() -> {
                        progressAnalysis.setVisibility(View.GONE);
                        tvNoAnalysis.setVisibility(View.VISIBLE);
                        tvNoAnalysis.setText("Error generating analysis: " + e.getMessage());
                        btnGenerateAnalysis.setEnabled(true);
                    });
                }
            }).start();
        } catch (Exception e) {
            Log.e(TAG, "Error starting analysis", e);
            progressAnalysis.setVisibility(View.GONE);
            tvNoAnalysis.setVisibility(View.VISIBLE);
            tvNoAnalysis.setText("Error starting analysis");
            btnGenerateAnalysis.setEnabled(true);
        }
    }

    private void parseEvents(String eventsJson) {
        eventsList.clear();

        try {
            if (eventsJson != null && !eventsJson.isEmpty() && !eventsJson.equals("[]")) {
                JSONArray jsonArray = new JSONArray(eventsJson);
                for (int i = 0; i < jsonArray.length(); i++) {
                    eventsList.add(jsonArray.getString(i));
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing events JSON", e);
            Toast.makeText(this, "Error parsing events data", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error parsing events", e);
            Toast.makeText(this, "Unexpected error parsing events", Toast.LENGTH_SHORT).show();
        }
    }

    // Replace the setupEventsChart method with this optimized version
    private void setupEventsChart(Date startDate) {
        try {
            // Show a loading indicator while chart is being prepared
            eventsChart.setNoDataText("Preparing chart...");
            eventsChart.invalidate();

            // First make sure the chart is visible
            eventsChart.setVisibility(View.VISIBLE);

            // Move the heavy chart processing to a background thread
            new Thread(() -> {
                try {
                    // Create the chart data in background
                    final LineData lineData = prepareChartData(startDate);

                    // Update the UI on the main thread
                    runOnUiThread(() -> {
                        try {
                            if (lineData != null && (lineData.getDataSetCount() > 0)) {
                                Log.d(TAG, "Chart data prepared successfully with " + lineData.getDataSetCount() + " datasets");

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

                                // Format X-axis
                                configureChartAxis(eventsChart);

                                // Add chart animation
                                eventsChart.animateX(1000);

                                // Refresh chart
                                eventsChart.invalidate();

                                // Log success
                                Log.d(TAG, "Chart setup completed successfully");
                            } else {
                                Log.w(TAG, "No chart data available to display");
                                eventsChart.setNoDataText("No chart data available");
                                eventsChart.invalidate();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error setting up chart UI", e);
                            eventsChart.setNoDataText("Error creating chart");
                            eventsChart.invalidate();
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error preparing chart data", e);
                    runOnUiThread(() -> {
                        eventsChart.setNoDataText("Error preparing chart data");
                        eventsChart.invalidate();
                    });
                }
            }).start();
        } catch (Exception e) {
            Log.e(TAG, "Error in setupEventsChart", e);
            eventsChart.setNoDataText("Error setting up chart");
            eventsChart.invalidate();
        }
    }

    // Add this new method to prepare chart data in background
    private LineData prepareChartData(Date startDate) {
        try {
            Log.d(TAG, "Starting chart data preparation");

            // Parse and extract event times from events list
            List<Date> soundEventTimes = new ArrayList<>();
            List<Date> movementEventTimes = new ArrayList<>();
            List<Float> soundIntensities = new ArrayList<>();
            List<Float> movementIntensities = new ArrayList<>();

            // Get start and end time of session for X-axis bounds
            Date sessionStartDate = null;
            Date sessionEndDate = null;

            try {
                sessionStartDate = sdfInput.parse(currentSession.getStart());
                sessionEndDate = sdfInput.parse(currentSession.getStop());
                Log.d(TAG, "Session period: " + sessionStartDate + " to " + sessionEndDate);
            } catch (ParseException e) {
                Log.e(TAG, "Error parsing session dates", e);
                return null;
            }

            // Safety check for null dates
            if (sessionStartDate == null || sessionEndDate == null) {
                Log.e(TAG, "Session start or end date is null");
                return null;
            }

            // Calculate total duration in milliseconds for scaling
            long sessionDurationMillis = sessionEndDate.getTime() - sessionStartDate.getTime();
            Log.d(TAG, "Session duration: " + sessionDurationMillis + "ms");

            // Safety check for zero duration
            if (sessionDurationMillis <= 0) {
                Log.e(TAG, "Session duration is zero or negative: " + sessionDurationMillis);
                return null;
            }

            // Log the number of events to process
            Log.d(TAG, "Processing " + eventsList.size() + " events for chart");

            // Process events in batches to avoid long-running loops
            final int BATCH_SIZE = 50;
            for (int eventIndex = 0; eventIndex < eventsList.size(); eventIndex += BATCH_SIZE) {
                int endIndex = Math.min(eventIndex + BATCH_SIZE, eventsList.size());
                for (int i = eventIndex; i < endIndex; i++) {
                    String event = eventsList.get(i);
                    processEventForChart(event, soundEventTimes, movementEventTimes,
                            soundIntensities, movementIntensities);
                }

                // Small sleep to avoid CPU spikes
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            // Log the extracted events
            Log.d(TAG, "Extracted " + soundEventTimes.size() + " sound events and "
                    + movementEventTimes.size() + " movement events");

            // Create chart entries based on timeline positions
            List<Entry> soundEntries = new ArrayList<>();
            List<Entry> movementEntries = new ArrayList<>();

            // Process sound events
            for (int i = 0; i < soundEventTimes.size(); i++) {
                try {
                    Date eventTime = soundEventTimes.get(i);
                    if (eventTime == null) continue;

                    float xPosition = calculateEventPosition(eventTime, sessionStartDate, sessionDurationMillis);
                    if (xPosition >= 0 && i < soundIntensities.size()) {
                        soundEntries.add(new Entry(xPosition, soundIntensities.get(i)));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing sound event at index " + i, e);
                }
            }

            // Process movement events
            for (int i = 0; i < movementEventTimes.size(); i++) {
                try {
                    Date eventTime = movementEventTimes.get(i);
                    if (eventTime == null) continue;

                    float xPosition = calculateEventPosition(eventTime, sessionStartDate, sessionDurationMillis);
                    if (xPosition >= 0 && i < movementIntensities.size()) {
                        movementEntries.add(new Entry(xPosition, movementIntensities.get(i)));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing movement event at index " + i, e);
                }
            }

            // Log the created entries
            Log.d(TAG, "Created " + soundEntries.size() + " sound entries and "
                    + movementEntries.size() + " movement entries for chart");

            // Check if we have any valid entries before proceeding
            if (soundEntries.isEmpty() && movementEntries.isEmpty()) {
                Log.w(TAG, "No valid chart entries could be created");
                return null;
            }

            // Sort entries by X value to ensure proper line connection
            Collections.sort(soundEntries, (e1, e2) -> Float.compare(e1.getX(), e2.getX()));
            Collections.sort(movementEntries, (e1, e2) -> Float.compare(e1.getX(), e2.getX()));

            // Create datasets
            LineData lineData = new LineData();

            if (!soundEntries.isEmpty()) {
                LineDataSet soundDataSet = createLineDataSet(soundEntries, "Sound", "#FF6D00");
                lineData.addDataSet(soundDataSet);
                Log.d(TAG, "Added sound dataset to chart");
            }

            if (!movementEntries.isEmpty()) {
                LineDataSet movementDataSet = createLineDataSet(movementEntries, "Movement", "#0069C0");
                lineData.addDataSet(movementDataSet);
                Log.d(TAG, "Added movement dataset to chart");
            }

            Log.d(TAG, "Chart data preparation completed successfully");
            return lineData;
        } catch (Exception e) {
            Log.e(TAG, "Error preparing chart data", e);
            return null;
        }
    }

    // Add a fallback method to create a simple chart if the complex one fails
    private void createFallbackChart() {
        try {
            Log.d(TAG, "Creating fallback chart");

            // Create some simple data
            ArrayList<Entry> entries = new ArrayList<>();
            entries.add(new Entry(0f, 0f));
            entries.add(new Entry(0.5f, 1f));
            entries.add(new Entry(1f, 0.5f));

            LineDataSet dataSet = new LineDataSet(entries, "Sample Data");
            dataSet.setColor(ColorTemplate.rgb("#4CAF50"));
            dataSet.setLineWidth(2f);
            dataSet.setCircleColor(ColorTemplate.rgb("#4CAF50"));
            dataSet.setCircleRadius(4f);
            dataSet.setDrawValues(false);

            LineData lineData = new LineData(dataSet);

            // Set the data to the chart
            eventsChart.setData(lineData);
            eventsChart.getDescription().setText("Sample Chart (Fallback)");
            eventsChart.invalidate();

            Log.d(TAG, "Fallback chart created successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error creating fallback chart", e);
        }
    }

    private void processEventForChart(String event, List<Date> soundEventTimes, List<Date> movementEventTimes,
                                      List<Float> soundIntensities, List<Float> movementIntensities) {
        try {
            String timeStr = null;
            float intensity = 0f;

            // Extract time and intensity from event string
            if (event.contains("Sound detected at ")) {
                int startIndex = "Sound detected at ".length();
                int endIndex = event.indexOf(" (Amplitude");

                // Check for valid indices
                if (endIndex > startIndex && endIndex < event.length()) {
                    timeStr = event.substring(startIndex, endIndex);
                } else if (startIndex < event.length()) {
                    // Try to get at least the time part if the format is different
                    timeStr = event.substring(startIndex);
                    // Limit to first 8 chars or less if string is shorter
                    int maxLength = Math.min(timeStr.length(), 8);
                    timeStr = timeStr.substring(0, maxLength);
                }

                // Extract amplitude value
                String amplitudeStr = extractValue(event, "Amplitude: ", ")");
                try {
                    intensity = Float.parseFloat(amplitudeStr);
                } catch (NumberFormatException nfe) {
                    Log.e(TAG, "Error parsing amplitude: " + amplitudeStr, nfe);
                    intensity = 1.0f; // Default value
                }

                // Parse time if we have a valid time string
                if (timeStr != null && timeStr.length() >= 5) { // Minimum "HH:MM" format
                    try {
                        Date eventTime = timeFormat.parse(timeStr);
                        if (eventTime != null) {
                            soundEventTimes.add(eventTime);
                            soundIntensities.add(intensity);
                        }
                    } catch (ParseException pe) {
                        Log.e(TAG, "Error parsing time: " + timeStr, pe);
                    }
                }
            } else if (event.contains("Movement detected at ")) {
                int startIndex = "Movement detected at ".length();
                int endIndex = event.indexOf(" (Intensity");

                // Check for valid indices
                if (endIndex > startIndex && endIndex < event.length()) {
                    timeStr = event.substring(startIndex, endIndex);
                } else if (startIndex < event.length()) {
                    // Try to get at least the time part if the format is different
                    timeStr = event.substring(startIndex);
                    // Limit to first 8 chars or less if string is shorter
                    int maxLength = Math.min(timeStr.length(), 8);
                    timeStr = timeStr.substring(0, maxLength);
                }

                // Extract intensity value
                String intensityStr = extractValue(event, "Intensity: ", ")");
                try {
                    intensity = Float.parseFloat(intensityStr);
                } catch (NumberFormatException nfe) {
                    Log.e(TAG, "Error parsing intensity: " + intensityStr, nfe);
                    intensity = 1.0f; // Default value
                }

                // Parse time if we have a valid time string
                if (timeStr != null && timeStr.length() >= 5) { // Minimum "HH:MM" format
                    try {
                        Date eventTime = timeFormat.parse(timeStr);
                        if (eventTime != null) {
                            movementEventTimes.add(eventTime);
                            movementIntensities.add(intensity);
                        }
                    } catch (ParseException pe) {
                        Log.e(TAG, "Error parsing time: " + timeStr, pe);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing event: " + event, e);
        }
    }

    private float calculateEventPosition(Date eventTime, Date sessionStartDate, long sessionDurationMillis) {
        try {
            // Convert time to Calendar to get hours and minutes
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

            // Safety check to avoid division by zero
            if (sessionDurationMillis > 0) {
                return minutesSinceStart / (float) (sessionDurationMillis / (1000 * 60));
            }
            return -1; // Invalid position
        } catch (Exception e) {
            Log.e(TAG, "Error calculating event position", e);
            return -1;
        }
    }

    private LineDataSet createLineDataSet(List<Entry> entries, String label, String colorHex) {
        LineDataSet dataSet = new LineDataSet(entries, label);
        dataSet.setColor(ColorTemplate.rgb(colorHex));
        dataSet.setLineWidth(2f);
        dataSet.setDrawFilled(false);
        dataSet.setDrawCircles(true);
        dataSet.setCircleColor(ColorTemplate.rgb(colorHex));
        dataSet.setCircleHoleColor(ColorTemplate.rgb("#FFFFFF"));
        dataSet.setCircleRadius(4f);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.LINEAR);
        return dataSet;
    }

    private void configureChartAxis(LineChart chart) {
        try {
            // Get session dates for axis configuration
            Date sessionStartDate = sdfInput.parse(currentSession.getStart());
            Date sessionEndDate = sdfInput.parse(currentSession.getStop());

            if (sessionStartDate == null || sessionEndDate == null) {
                return;
            }

            final long sessionDurationMillis = sessionEndDate.getTime() - sessionStartDate.getTime();

            // Format X-axis as a time axis from start to end
            XAxis xAxis = chart.getXAxis();
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
                    try {
                        // Convert normalized position (0-1) back to time
                        long timeOffsetMillis = (long) (value * sessionDurationMillis);
                        Date pointDate = new Date(sessionStartDate.getTime() + timeOffsetMillis);
                        return timeFormat.format(pointDate);
                    } catch (Exception e) {
                        Log.e(TAG, "Error formatting axis value", e);
                        return "";
                    }
                }
            });

            // Show time labels at start, 25%, 50%, 75% and end of sleep session
            xAxis.setLabelCount(5, true);

            // Improve Y-axis appearance
            com.github.mikephil.charting.components.YAxis leftAxis = chart.getAxisLeft();
            leftAxis.setDrawLabels(true);
            leftAxis.setTextColor(ColorTemplate.rgb("#333333"));
            leftAxis.setTextSize(10f);
            leftAxis.setAxisMinimum(0f);
            leftAxis.setDrawGridLines(true);
            leftAxis.setGridColor(ColorTemplate.rgb("#EEEEEE"));

            // Remove right Y-axis
            chart.getAxisRight().setEnabled(false);

            // Add some padding
            chart.setExtraBottomOffset(10f);
            chart.setExtraLeftOffset(10f);
            chart.setExtraRightOffset(10f);
        } catch (Exception e) {
            Log.e(TAG, "Error configuring chart axis", e);
        }
    }

    private String extractValue(String event, String prefix, String suffix) {
        try {
            int startIndex = event.indexOf(prefix);
            if (startIndex == -1) return "0";

            startIndex += prefix.length();
            int endIndex = event.indexOf(suffix, startIndex);

            if (endIndex == -1 || startIndex >= endIndex) return "0";

            return event.substring(startIndex, endIndex);
        } catch (Exception e) {
            Log.e(TAG, "Error extracting value from event: " + event, e);
            return "0";
        }
    }

    private void setupEventsRecyclerView() {
        try {
            EventsAdapter adapter = new EventsAdapter(eventsList);
            recyclerViewEvents.setAdapter(adapter);
        } catch (Exception e) {
            Log.e(TAG, "Error setting up events recycler view", e);
        }
    }

    // Inner class for RecyclerView adapter
    private class EventsAdapter extends RecyclerView.Adapter<EventsAdapter.EventViewHolder> {

        private final List<String> events;

        public EventsAdapter(List<String> events) {
            this.events = events;
        }

        @Override
        public EventViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            try {
                View view = getLayoutInflater().inflate(R.layout.item_event, parent, false);
                return new EventViewHolder(view);
            } catch (Exception e) {
                Log.e(TAG, "Error creating view holder", e);
                // Create an empty view as fallback
                View fallbackView = new View(parent.getContext());
                return new EventViewHolder(fallbackView);
            }
        }

        @Override
        public void onBindViewHolder(EventViewHolder holder, int position) {
            try {
                String event = events.get(position);
                holder.bind(event, position);
            } catch (Exception e) {
                Log.e(TAG, "Error binding view holder at position " + position, e);
            }
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
                try {
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
                } catch (Exception e) {
                    Log.e(TAG, "Error binding event at position " + position, e);
                    // Set fallback text
                    if (tvEventText != null) tvEventText.setText("Error displaying event");
                    if (tvEventTime != null) tvEventTime.setText("");
                }
            }

            private String extractTime(String event, String prefix) {
                try {
                    int startIndex = event.indexOf(prefix);
                    if (startIndex == -1) return "";

                    startIndex += prefix.length();
                    // Make sure we don't go out of bounds
                    if (startIndex + 8 <= event.length()) {
                        return event.substring(startIndex, startIndex + 8); // "HH:mm:ss" is 8 chars
                    } else if (startIndex + 5 <= event.length()) {
                        return event.substring(startIndex, startIndex + 5); // "HH:mm" is 5 chars
                    } else if (startIndex < event.length()) {
                        return event.substring(startIndex); // Return whatever is left
                    }
                    return "";
                } catch (Exception e) {
                    Log.e(TAG, "Error extracting time from event: " + event, e);
                    return "";
                }
            }
        }
    }

    // Add this method to the class to check if the chart is properly initialized
    private void checkChartVisibility() {
        try {
            if (eventsChart == null) {
                Log.e(TAG, "Chart is null - not properly initialized");
                return;
            }

            int visibility = eventsChart.getVisibility();
            String visibilityStr = visibility == View.VISIBLE ? "VISIBLE" :
                    (visibility == View.INVISIBLE ? "INVISIBLE" : "GONE");

            Log.d(TAG, "Chart visibility: " + visibilityStr);
            Log.d(TAG, "Chart width: " + eventsChart.getWidth() + ", height: " + eventsChart.getHeight());

            if (visibility != View.VISIBLE) {
                Log.d(TAG, "Setting chart to VISIBLE");
                eventsChart.setVisibility(View.VISIBLE);
            }

            if (eventsChart.getData() == null) {
                Log.d(TAG, "Chart has no data");
            } else {
                Log.d(TAG, "Chart has data with " + eventsChart.getData().getDataSetCount() + " datasets");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking chart visibility", e);
        }
    }
}
