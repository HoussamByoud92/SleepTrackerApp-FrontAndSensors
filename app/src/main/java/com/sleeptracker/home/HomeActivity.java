package com.sleeptracker.home;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.sleeptracker.R;
import com.sleeptracker.api.ApiClient;
import com.sleeptracker.api.ApiService;
import com.sleeptracker.model.ApiResponse;
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
        TextView tvTitle = view.findViewById(R.id.tvPopupTitle);
        TextView tvContent = view.findViewById(R.id.tvPopupContent);

        String events = session.getEvents();
        tvContent.setText(events.equals("[]") ? "No events recorded." : formatEventsForDisplay(events));

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(view);
        builder.setPositiveButton("Close", (dialog, which) -> dialog.dismiss());
        builder.show();
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