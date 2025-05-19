package com.sleeptracker.home;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.DatePicker;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Color;
import android.view.WindowManager;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.animation.Easing;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.animation.Easing;
import com.google.android.material.button.MaterialButton;
import com.sleeptracker.R;
import com.sleeptracker.analysis.SleepAnalysisManager;
import com.sleeptracker.api.ApiClient;
import com.sleeptracker.api.ApiService;
import com.sleeptracker.model.SleepInsight;
import com.sleeptracker.model.SleepSession;
import com.sleeptracker.utils.SessionManager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SleepHistoryActivity extends AppCompatActivity {

    private static final String TAG = "SleepHistoryActivity";
    private RecyclerView recyclerView;
    private TextView tvNoSessions;
    private TextView tvSelectedDate;
    private ImageButton btnClearDate;
    private ApiService apiService;
    private SessionManager session;
    private int userId;
    private List<SleepSession> allSessions = new ArrayList<>();
    private List<SleepSession> filteredSessions = new ArrayList<>();
    private SleepSessionAdapter adapter;
    private Calendar selectedDate = null;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat fullDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sleep_history);

        // Initialize UI components
        recyclerView = findViewById(R.id.recyclerSleepHistory);
        tvNoSessions = findViewById(R.id.tvNoSessions);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        btnClearDate = findViewById(R.id.btnClearDate);
        MaterialButton btnSelectDate = findViewById(R.id.btnSelectDate);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SleepSessionAdapter(filteredSessions, this::showEventsPopup);
        recyclerView.setAdapter(adapter);

        // Initialize API service and session manager
        session = new SessionManager(this);
        userId = session.getUserId();
        apiService = ApiClient.getClient().create(ApiService.class);

        // Set up date picker button
        btnSelectDate.setOnClickListener(v -> showDatePicker());

        // Set up clear date button
        btnClearDate.setOnClickListener(v -> clearDateFilter());

        // Back button functionality
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Fetch all sessions
        fetchAllSessions();
    }

    private void fetchAllSessions() {
        apiService.getSessions(userId).enqueue(new Callback<List<SleepSession>>() {
            @Override
            public void onResponse(Call<List<SleepSession>> call, Response<List<SleepSession>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allSessions.clear();
                    allSessions.addAll(response.body());

                    // Limit to last 30 sessions if more exist
                    if (allSessions.size() > 30) {
                        allSessions = allSessions.subList(0, 30);
                    }

                    updateDisplayedSessions();
                } else {
                    showError("Failed to fetch sessions");
                }
            }

            @Override
            public void onFailure(Call<List<SleepSession>> call, Throwable t) {
                showError("Network error: " + t.getMessage());
                Log.e(TAG, "Error fetching sessions", t);
            }
        });
    }

    private void showDatePicker() {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    selectedDate = Calendar.getInstance();
                    selectedDate.set(selectedYear, selectedMonth, selectedDay);

                    // Format the date for display
                    String formattedDate = dateFormat.format(selectedDate.getTime());
                    tvSelectedDate.setText(formattedDate);
                    tvSelectedDate.setVisibility(View.VISIBLE);
                    btnClearDate.setVisibility(View.VISIBLE);

                    // Filter sessions by selected date
                    filterSessionsByDate();
                },
                year, month, day
        );
        datePickerDialog.show();
    }

    private void clearDateFilter() {
        selectedDate = null;
        tvSelectedDate.setVisibility(View.GONE);
        btnClearDate.setVisibility(View.GONE);
        updateDisplayedSessions();
    }

    private void filterSessionsByDate() {
        if (selectedDate == null) {
            updateDisplayedSessions();
            return;
        }

        try {
            String selectedDateStr = dateFormat.format(selectedDate.getTime());

            filteredSessions.clear();

            for (SleepSession session : allSessions) {
                try {
                    Date startDate = fullDateFormat.parse(session.getStart());
                    if (startDate != null) {
                        String sessionDateStr = dateFormat.format(startDate);
                        if (sessionDateStr.equals(selectedDateStr)) {
                            filteredSessions.add(session);
                        }
                    }
                } catch (ParseException e) {
                    Log.e(TAG, "Error parsing date: " + session.getStart(), e);
                }
            }

            updateRecyclerView();

        } catch (Exception e) {
            Log.e(TAG, "Error filtering by date", e);
            showError("Error applying date filter");
        }
    }

    private void updateDisplayedSessions() {
        filteredSessions.clear();
        filteredSessions.addAll(allSessions);
        updateRecyclerView();
    }

    private void updateRecyclerView() {
        adapter.notifyDataSetChanged();

        if (filteredSessions.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            tvNoSessions.setVisibility(View.VISIBLE);
            if (selectedDate != null) {
                tvNoSessions.setText("No sleep sessions found for this date");
            } else {
                tvNoSessions.setText("No sleep sessions found");
            }
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            tvNoSessions.setVisibility(View.GONE);
        }
    }

    private void showEventsPopup(SleepSession session) {
        View view = getLayoutInflater().inflate(R.layout.popup_sleep_events, null);
        TextView tvSleepQualityScore = view.findViewById(R.id.tvSleepQualityScore);
        TextView tvAiAnalysisContent = view.findViewById(R.id.tvAiAnalysisContent);
        TextView tvSleepDuration = view.findViewById(R.id.tvSleepDuration);

        // Initialize pie chart
        com.github.mikephil.charting.charts.PieChart sleepStagesChart = view.findViewById(R.id.sleepStagesChart);

        // Generate and display AI analysis
        SleepAnalysisManager analysisManager = new SleepAnalysisManager(this);

        try {
            // Calculate sleep duration
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date startDate = sdf.parse(session.getStart());
            Date endDate = sdf.parse(session.getStop());

            if (startDate != null && endDate != null) {
                long durationMinutes = TimeUnit.MILLISECONDS.toMinutes(endDate.getTime() - startDate.getTime());
                long hours = durationMinutes / 60;
                long mins = durationMinutes % 60;
                tvSleepDuration.setText("Sleep Duration: " + hours + "h " + mins + "m");
            } else {
                tvSleepDuration.setText("Sleep Duration: N/A");
            }
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing dates", e);
            tvSleepDuration.setText("Sleep Duration: N/A");
        }

        // Calculate sleep quality score
        int qualityScore = analysisManager.calculateSleepQualityScore(session);
        String qualityDescription = analysisManager.getSleepQualityDescription(qualityScore);
        tvSleepQualityScore.setText("Sleep Quality: " + qualityScore + " (" + qualityDescription + ")");

        // Generate insights
        List<SleepInsight> insights = analysisManager.generateInsights(session);

        // Format insights for display
        StringBuilder insightsText = new StringBuilder();
        for (SleepInsight insight : insights) {
            insightsText.append("• ").append(insight.getTitle()).append(": ")
                    .append(insight.getDescription()).append("\n\n");
        }

        tvAiAnalysisContent.setText(insightsText.toString().trim());

        // Configure and display sleep stages chart
        PieData sleepStagesData = analysisManager.analyzeSleepStages(session);
        configureSleepStagesChart(sleepStagesChart, sleepStagesData);

        // Show the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(view);
        builder.setPositiveButton("Close", (dialog, which) -> dialog.dismiss());

        // Create a wider dialog for better chart display
        AlertDialog dialog = builder.create();
        dialog.show();

        // Set dialog width to 90% of screen width
        if (dialog.getWindow() != null) {
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.copyFrom(dialog.getWindow().getAttributes());
            layoutParams.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.9);
            dialog.getWindow().setAttributes(layoutParams);
        }
    }

    private void configureSleepStagesChart(PieChart chart, PieData data) {
        // Basic chart configuration
        chart.setUsePercentValues(true);
        chart.getDescription().setEnabled(false);
        chart.setExtraOffsets(5, 10, 5, 5);
        chart.setDragDecelerationFrictionCoef(0.95f);

        // Center hole configuration
        chart.setDrawHoleEnabled(true);
        chart.setHoleColor(Color.WHITE);
        chart.setTransparentCircleColor(Color.WHITE);
        chart.setTransparentCircleAlpha(110);
        chart.setHoleRadius(58f);
        chart.setTransparentCircleRadius(61f);

        // Center text
        chart.setDrawCenterText(true);
        chart.setCenterText("Sleep\nStages");
        chart.setCenterTextSize(16f);

        // Legend configuration
        chart.getLegend().setEnabled(true);
        chart.getLegend().setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        chart.getLegend().setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        chart.getLegend().setOrientation(Legend.LegendOrientation.VERTICAL);
        chart.getLegend().setDrawInside(false);
        chart.getLegend().setTextSize(12f);

        // Set data and animate
        chart.setData(data);
        chart.highlightValues(null);
        chart.invalidate();
        chart.animateY(1400, Easing.EaseInOutQuad);
    }


    // Format events JSON for better display (reused from HomeActivity)
    private String formatEventsForDisplay(String eventsJson) {
        if (eventsJson.equals("[]")) {
            return "No events recorded.";
        }

        // Simple parsing of JSON array format: ["event1","event2",...]
        String processed = eventsJson
                .replace("[", "")
                .replace("]", "")
                .replace("\"", "")
                .replace(",", "\n");

        return processed;
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}