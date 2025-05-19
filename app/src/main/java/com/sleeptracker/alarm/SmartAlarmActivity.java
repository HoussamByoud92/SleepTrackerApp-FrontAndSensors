package com.sleeptracker.alarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.sleeptracker.R;
import com.sleeptracker.api.ApiClient;
import com.sleeptracker.api.ApiService;
import com.sleeptracker.model.SleepSession;
import com.sleeptracker.utils.SessionManager;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SmartAlarmActivity extends AppCompatActivity {

    private TextView tvRecommendedWakeTime;
    private TextView tvSleepDebt;
    private TextView tvAvgSleepTime;
    private TimePicker timePicker;
    private MaterialButton btnSetAlarm;
    private MaterialCardView cardRecommended;

    private ApiService apiService;
    private SessionManager session;
    private int userId;

    // Ideal sleep time in hours
    private static final int IDEAL_SLEEP_HOURS = 8;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_smart_alarm);

        // Initialize views
        tvRecommendedWakeTime = findViewById(R.id.tvRecommendedWakeTime);
        tvSleepDebt = findViewById(R.id.tvSleepDebt);
        tvAvgSleepTime = findViewById(R.id.tvAvgSleepTime);
        timePicker = findViewById(R.id.timePicker);
        btnSetAlarm = findViewById(R.id.btnSetAlarm);
        cardRecommended = findViewById(R.id.cardRecommended);

        // Initialize API service and session
        session = new SessionManager(this);
        userId = session.getUserId();
        apiService = ApiClient.getClient().create(ApiService.class);
        ImageButton btnBackArrow = findViewById(R.id.btnBackArrow);
        btnBackArrow.setOnClickListener(v -> {
            // This will close the current activity and go back to the previous one
            finish();
        });
        // Set 24-hour view for time picker
        timePicker.setIs24HourView(true);

        // Load sleep data
        loadSleepData();

        // Set up button click listener
        btnSetAlarm.setOnClickListener(v -> setSmartAlarm());

        // Set up recommended time card click listener
        cardRecommended.setOnClickListener(v -> useRecommendedTime());
    }

    private void loadSleepData() {
        apiService.getSessions(userId).enqueue(new Callback<List<SleepSession>>() {
            @Override
            public void onResponse(Call<List<SleepSession>> call, Response<List<SleepSession>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    calculateSleepMetrics(response.body());
                }
            }

            @Override
            public void onFailure(Call<List<SleepSession>> call, Throwable t) {
                Toast.makeText(SmartAlarmActivity.this, "Failed to load sleep data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void calculateSleepMetrics(List<SleepSession> sessions) {
        if (sessions.isEmpty()) {
            tvAvgSleepTime.setText("No sleep data available");
            tvSleepDebt.setText("Sleep debt: Unknown");
            tvRecommendedWakeTime.setText("Set a wake time");
            return;
        }

        // Calculate average sleep time
        long totalSleepMillis = 0;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

        for (SleepSession session : sessions) {
            try {
                Date start = sdf.parse(session.getStart());
                Date stop = sdf.parse(session.getStop());
                if (start != null && stop != null) {
                    totalSleepMillis += (stop.getTime() - start.getTime());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Calculate average sleep time in hours and minutes
        float avgSleepHours = (float) totalSleepMillis / sessions.size() / (1000 * 60 * 60);
        int avgSleepHoursInt = (int) avgSleepHours;
        int avgSleepMinutes = (int) ((avgSleepHours - avgSleepHoursInt) * 60);

        tvAvgSleepTime.setText(String.format(Locale.getDefault(),
                "Average sleep: %dh %dm", avgSleepHoursInt, avgSleepMinutes));

        // Calculate sleep debt
        float sleepDebt = IDEAL_SLEEP_HOURS - avgSleepHours;
        if (sleepDebt > 0) {
            int sleepDebtHours = (int) sleepDebt;
            int sleepDebtMinutes = (int) ((sleepDebt - sleepDebtHours) * 60);
            tvSleepDebt.setText(String.format(Locale.getDefault(),
                    "Sleep debt: %dh %dm", sleepDebtHours, sleepDebtMinutes));
        } else {
            tvSleepDebt.setText("No sleep debt");
        }

        // Calculate recommended wake time based on ideal sleep duration
        Calendar calendar = Calendar.getInstance();
        // Assume bedtime is now
        calendar.add(Calendar.HOUR_OF_DAY, IDEAL_SLEEP_HOURS);
        // Add extra time to recover sleep debt (max 2 hours)
        if (sleepDebt > 0) {
            calendar.add(Calendar.MINUTE, (int) Math.min(sleepDebt * 60, 120));
        }

        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        tvRecommendedWakeTime.setText(String.format(Locale.getDefault(),
                "Recommended: %02d:%02d", hour, minute));

        // Pre-set the time picker to the recommended time
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            timePicker.setHour(hour);
            timePicker.setMinute(minute);
        } else {
            timePicker.setCurrentHour(hour);
            timePicker.setCurrentMinute(minute);
        }
    }

    private void setSmartAlarm() {
        // Get time from time picker
        int hour, minute;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            hour = timePicker.getHour();
            minute = timePicker.getMinute();
        } else {
            hour = timePicker.getCurrentHour();
            minute = timePicker.getCurrentMinute();
        }

        // Set alarm
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);

        // If time is earlier than current time, set for next day
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        // Create alarm intent
        Intent intent = new Intent(this, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        // Get alarm manager
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            try {
                // Check if we can schedule exact alarms on Android 12+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        scheduleExactAlarm(alarmManager, calendar, pendingIntent);
                    } else {
                        // Request permission to schedule exact alarms
                        showExactAlarmPermissionDialog();
                        return;
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendar.getTimeInMillis(),
                            pendingIntent);
                } else {
                    alarmManager.setExact(
                            AlarmManager.RTC_WAKEUP,
                            calendar.getTimeInMillis(),
                            pendingIntent);
                }

                // Format time for display
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                String formattedTime = sdf.format(calendar.getTime());

                Toast.makeText(this, "Alarm set for " + formattedTime, Toast.LENGTH_SHORT).show();
                finish();
            } catch (SecurityException e) {
                // Handle the case where the app doesn't have permission
                Toast.makeText(this, "Cannot schedule exact alarms. Please grant permission in Settings.",
                        Toast.LENGTH_LONG).show();

                // Open alarm permission settings if on Android 12+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    showExactAlarmPermissionDialog();
                }
            }
        }
    }

    // Helper method to schedule exact alarm with proper error handling
    @RequiresApi(api = Build.VERSION_CODES.S)
    private void scheduleExactAlarm(AlarmManager alarmManager, Calendar calendar, PendingIntent pendingIntent) {
        try {
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    pendingIntent);
        } catch (SecurityException e) {
            // Fallback to inexact alarm if permission is denied
            alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    pendingIntent);
            Toast.makeText(this, "Using inexact alarm timing due to permission restrictions",
                    Toast.LENGTH_SHORT).show();
        }
    }

    // Show dialog to guide user to grant exact alarm permission
    private void showExactAlarmPermissionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Permission Required")
                .setMessage("To set exact alarm times, this app needs permission to schedule exact alarms. " +
                        "Please grant this permission in Settings.")
                .setPositiveButton("Open Settings", (dialog, which) -> {
                    // Open the exact alarm permission settings
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                        intent.setData(Uri.parse("package:" + getPackageName()));
                        startActivity(intent);
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void useRecommendedTime() {
        // Extract time from recommended wake time text
        String recommendedTime = tvRecommendedWakeTime.getText().toString();
        if (recommendedTime.contains(":")) {
            String[] parts = recommendedTime.substring(recommendedTime.indexOf(":") - 2).split(":");
            try {
                int hour = Integer.parseInt(parts[0]);
                int minute = Integer.parseInt(parts[1]);

                // Set time picker to recommended time
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    timePicker.setHour(hour);
                    timePicker.setMinute(minute);
                } else {
                    timePicker.setCurrentHour(hour);
                    timePicker.setCurrentMinute(minute);
                }

                Toast.makeText(this, "Using recommended wake time", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}