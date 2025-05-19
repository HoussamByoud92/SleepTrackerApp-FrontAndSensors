package com.sleeptracker.home;

import android.content.Intent;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
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
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.animation.Easing;
import com.google.android.material.button.MaterialButton;
import com.sleeptracker.R;
import com.sleeptracker.analysis.SleepAnalysisManager;
import com.sleeptracker.api.ApiClient;
import com.sleeptracker.api.ApiService;
import com.sleeptracker.model.ApiResponse;
import com.sleeptracker.model.SleepInsight;
import com.sleeptracker.model.SleepSession;
import com.sleeptracker.sensor.DisplayEventsActivity;
import com.sleeptracker.sensor.SleepMonitorService;
import com.sleeptracker.utils.SessionManager;

import java.text.SimpleDateFormat;
import java.util.*;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends AppCompatActivity {

    private static final String TAG = "HomeActivity";
    private static final int PERMISSION_REQUEST_CODE = 123;

    private MaterialButton btnSleepToggle;
    private TextView tvAvgSleep, tvWelcome;
    private RecyclerView recyclerView;
    private boolean isSleeping = false;
    private String sleepStartTime;
    private SessionManager session;
    private ApiService apiService;
    private List<SleepSession> sessionList = new ArrayList<>();
    private SleepSessionAdapter adapter;
    private int userId;

    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    private TextView tvTimer;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private long startTime = 0;
    private long elapsedTime = 0;
    private Runnable timerRunnable;

    // Permission request launcher
    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean allGranted = true;
                for (Boolean granted : result.values()) {
                    if (!granted) {
                        allGranted = false;
                        break;
                    }
                }

                if (allGranted) {
                    startSleepTracking();
                } else {
                    Toast.makeText(this, "Required permissions not granted", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        tvWelcome = findViewById(R.id.tvWelcome);
        tvAvgSleep = findViewById(R.id.tvAvgSleep);
        tvTimer = findViewById(R.id.tvTimer);
        btnSleepToggle = findViewById(R.id.btnSleepToggle);
        recyclerView = findViewById(R.id.recyclerSleepSessions);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SleepSessionAdapter(sessionList, this::showEventsPopup);
        recyclerView.setAdapter(adapter);

        MaterialButton btnViewEvents = findViewById(R.id.btnViewEvents);
        btnViewEvents.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, DisplayEventsActivity.class)));

        // Add button for viewing sleep history
        MaterialButton btnViewHistory = findViewById(R.id.btnViewHistory);
        btnViewHistory.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, SleepHistoryActivity.class)));

        btnSleepToggle.setOnClickListener(v -> toggleSleep());

        session = new SessionManager(this);
        userId = session.getUserId();
        apiService = ApiClient.getClient().create(ApiService.class);

        fetchSessions();
    }

    private void toggleSleep() {
        if (!isSleeping) {
            // Check permissions before starting sleep tracking
            if (checkAndRequestPermissions()) {
                startSleepTracking();
            }
        } else {
            stopSleepTracking();
        }
    }

    private boolean checkAndRequestPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();

        // Check for required permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.RECORD_AUDIO);
        }

        // Check for sensor permissions on newer Android versions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
                        != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACTIVITY_RECOGNITION);
        }

        // Post notifications permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        if (!permissionsNeeded.isEmpty()) {
            requestPermissionLauncher.launch(permissionsNeeded.toArray(new String[0]));
            return false;
        }

        return true;
    }

    private void startSleepTracking() {
        try {
            sleepStartTime = sdf.format(new Date());
            isSleeping = true;
            btnSleepToggle.setText("Stop Sleep");

            // Start timer
            startTime = System.currentTimeMillis() - elapsedTime;
            handler.postDelayed(timerRunnable = new Runnable() {
                @Override
                public void run() {
                    elapsedTime = System.currentTimeMillis() - startTime;
                    int minutes = (int) (elapsedTime / 1000 / 60);
                    int seconds = (int) (elapsedTime / 1000 % 60);
                    tvTimer.setText(String.format("%02d:%02d", minutes, seconds));
                    handler.postDelayed(this, 1000);
                }
            }, 0);

            // Start SleepMonitorService
            Intent serviceIntent = new Intent(this, SleepMonitorService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
                Log.d(TAG, "Starting foreground service on Android O+");
            } else {
                startService(serviceIntent);
                Log.d(TAG, "Starting service on pre-Android O");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error starting sleep tracking: " + e.getMessage());
            Toast.makeText(this, "Error starting sleep tracking: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void stopSleepTracking() {
        String sleepStopTime = sdf.format(new Date());
        isSleeping = false;
        btnSleepToggle.setText("Start Sleep");

        // Stop timer
        handler.removeCallbacks(timerRunnable);
        tvTimer.setText("00:00");
        elapsedTime = 0;
        startTime = 0;

        try {
            // Get events before stopping service
            String eventsJson = SleepMonitorService.getEventsAsJsonString();
            Log.d(TAG, "Events captured: " + eventsJson);

            // Stop SleepMonitorService
            Intent serviceIntent = new Intent(this, SleepMonitorService.class);
            stopService(serviceIntent);

            // Create session with events
            SleepSession session = new SleepSession(userId, sleepStartTime, sleepStopTime, eventsJson);

            apiService.addSession(session).enqueue(new Callback<ApiResponse>() {
                @Override
                public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        Toast.makeText(HomeActivity.this, "Sleep session saved", Toast.LENGTH_SHORT).show();
                        fetchSessions();
                    } else {
                        String msg = response.body() != null ? response.body().getMessage() : "Unknown error";
                        Toast.makeText(HomeActivity.this, "Failed to save session: " + msg, Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse> call, Throwable t) {
                    Toast.makeText(HomeActivity.this, "Failed to save session", Toast.LENGTH_SHORT).show();
                    Log.e("SleepSessionError", "onFailure: ", t);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error stopping sleep tracking: " + e.getMessage());
            Toast.makeText(this, "Error stopping sleep tracking: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void fetchSessions() {
        if (apiService == null) {
            Log.e("HomeActivity", "ApiService is not initialized");
            return;
        }

        apiService.getSessions(userId).enqueue(new Callback<List<SleepSession>>() {
            @Override
            public void onResponse(Call<List<SleepSession>> call, Response<List<SleepSession>> response) {
                sessionList.clear();
                if (response.body() != null) {
                    List<SleepSession> all = response.body();
                    sessionList.addAll(all.size() > 5 ? all.subList(0, 5) : all);
                    adapter.notifyDataSetChanged();
                    calculateAverageSleep(all);
                } else {
                    Log.e("FETCH_SESSIONS", "Empty response body");
                }
            }

            @Override
            public void onFailure(Call<List<SleepSession>> call, Throwable t) {
                Toast.makeText(HomeActivity.this, "Failed to fetch sessions", Toast.LENGTH_SHORT).show();
                Log.e("RETROFIT_FAIL", "Error fetching sessions", t);
            }
        });
    }

    private void calculateAverageSleep(List<SleepSession> sessionsToAverage) {
        long totalSleepMillis = 0;
        int validSessionCount = 0;

        for (SleepSession session : sessionsToAverage) {
            try {
                Date start = sdf.parse(session.getStart());
                Date stop = sdf.parse(session.getStop());
                if (start != null && stop != null && stop.after(start)) {
                    totalSleepMillis += (stop.getTime() - start.getTime());
                    validSessionCount++;
                }
            } catch (Exception e) {
                Log.e("AvgSleepCalc", "Date parsing error: " + e.getMessage());
            }
        }

        if (validSessionCount > 0) {
            long avgMillis = totalSleepMillis / validSessionCount;
            long hours = avgMillis / (1000 * 60 * 60);
            long minutes = (avgMillis / (1000 * 60)) % 60;

            tvAvgSleep.setText("Average: " + hours + "h " + minutes + "m");
        } else {
            tvAvgSleep.setText("Average: 0h");
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


    // Format events JSON for better display
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
}