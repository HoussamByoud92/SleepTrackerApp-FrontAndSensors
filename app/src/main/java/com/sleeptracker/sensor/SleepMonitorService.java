package com.sleeptracker.sensor;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.sleeptracker.R;
import com.sleeptracker.home.HomeActivity;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class SleepMonitorService extends Service implements SensorEventListener {

    private static final String TAG = "SleepMonitorService";
    private static final String CHANNEL_ID = "sleep_channel";
    private static final String EVENT_CHANNEL_ID = "sleep_event_channel";
    private static final int NOTIFICATION_ID = 1001;
    private static final int EVENT_NOTIFICATION_BASE_ID = 2000;
    private int eventCounter = 0;

    // Sensitivity threshold for sound detection
    // Lower values will make it more sensitive
    private static final int SOUND_THRESHOLD = 300;

    // Flag to control if movement detection is enabled - Changed to true
    private static final boolean ENABLE_MOVEMENT_DETECTION = true;

    // Adjust movement threshold to appropriate sensitivity (reduced for more sensitivity)
    private static final float MOVEMENT_THRESHOLD = 1.0f;

    // Cooldown period for movement detection to avoid excessive notifications (milliseconds)
    private static final long MOVEMENT_COOLDOWN = 5000; // 5 seconds
    private long lastMovementTime = 0;

    // Key for passing events back to the activity
    public static final String EXTRA_SLEEP_EVENTS = "sleep_events";
    public static final String ACTION_TEST_SOUND = "com.sleeptracker.TEST_SOUND";

    private MediaRecorder recorder;
    private Handler handler;
    private Runnable soundChecker;

    private SensorManager sensorManager;
    private Sensor accelerometer;

    private float lastX, lastY, lastZ;

    // Changed from private final to static to make accessible
    private static final ArrayList<String> events = new ArrayList<>();
    private final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

    private boolean isRecorderInitialized = false;
    private NotificationManager notificationManager;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: Service creating");

        // Clear events when service starts
        events.clear();

        createNotificationChannels();
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Create a notification with an intent to open the app when clicked
        Intent notificationIntent = new Intent(this, HomeActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Sleep Tracker")
                .setContentText("Monitoring sleep...")
                .setSmallIcon(R.drawable.ic_sleep)
                .setContentIntent(pendingIntent)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build();

        startForeground(NOTIFICATION_ID, notification);

        try {
            if (ENABLE_MOVEMENT_DETECTION) {
                setupAccelerometer();
            }

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED) {
                setupMicrophone();
                startMicrophoneListener();
            } else {
                Log.e(TAG, "Microphone permission not granted");
                addEvent("Error: Microphone permission not granted");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage());
            e.printStackTrace();
            addEvent("Error initializing sensors: " + e.getMessage());
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: Service starting");

        // Check if this is a test sound request
        if (intent != null && ACTION_TEST_SOUND.equals(intent.getAction())) {
            Log.d(TAG, "Test sound requested");
            addEvent("Test sound detected");
            return START_STICKY;
        }

        return START_STICKY;
    }

    // ------------------ MICROPHONE SETUP ------------------
    private void setupMicrophone() {
        try {
            if (recorder != null) {
                try {
                    recorder.release();
                } catch (Exception e) {
                    Log.e(TAG, "Error releasing existing recorder: " + e.getMessage());
                }
            }

            recorder = new MediaRecorder();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                recorder.setAudioSource(MediaRecorder.AudioSource.UNPROCESSED);
            } else {
                recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            }

            recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);

            // Create a temporary file for output (required on some devices)
            File outputFile = new File(getCacheDir(), "temp_audio.3gp");
            recorder.setOutputFile(outputFile.getAbsolutePath());

            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

            try {
                recorder.prepare();
                recorder.start();
                isRecorderInitialized = true;
                Log.d(TAG, "MediaRecorder started successfully");

            } catch (IOException e) {
                Log.e(TAG, "Microphone prepare error: " + e.getMessage());
                e.printStackTrace();
            }
        } catch (Exception e) {
            Log.e(TAG, "Microphone setup error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void startMicrophoneListener() {
        if (!isRecorderInitialized) {
            Log.e(TAG, "Cannot start microphone listener - recorder not initialized");
            return;
        }

        handler = new Handler(Looper.getMainLooper());
        soundChecker = new Runnable() {
            @Override
            public void run() {
                if (recorder != null) {
                    try {
                        int amplitude = recorder.getMaxAmplitude();
                        Log.d(TAG, "Current amplitude: " + amplitude);

                        if (amplitude > SOUND_THRESHOLD) {
                            String msg = "Sound detected at " + sdf.format(new Date()) +
                                    " (Amplitude: " + amplitude + ")";
                            addEvent(msg);
                            sendEventNotification(msg);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error getting amplitude: " + e.getMessage());
                    }
                }
                handler.postDelayed(this, 1500); // Every 1.5 seconds for more responsive detection
            }
        };
        handler.post(soundChecker);
    }

    // ------------------ ACCELEROMETER SETUP ------------------
    private void setupAccelerometer() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if (accelerometer != null) {
                sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
                Log.d(TAG, "Accelerometer registered successfully");
            } else {
                Log.e(TAG, "No accelerometer found!");
                addEvent("Error: No accelerometer found on this device");
            }
        } else {
            Log.e(TAG, "Could not get sensor service!");
            addEvent("Error: Could not access device sensors");
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (!ENABLE_MOVEMENT_DETECTION) return;

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            float deltaX = Math.abs(x - lastX);
            float deltaY = Math.abs(y - lastY);
            float deltaZ = Math.abs(z - lastZ);

            // Apply cooldown to prevent flooding with movement events
            long currentTime = System.currentTimeMillis();
            boolean cooldownPassed = (currentTime - lastMovementTime) > MOVEMENT_COOLDOWN;

            // Check if movement exceeds threshold and cooldown has passed
            if ((deltaX > MOVEMENT_THRESHOLD || deltaY > MOVEMENT_THRESHOLD || deltaZ > MOVEMENT_THRESHOLD)
                    && cooldownPassed) {
                // Calculate movement intensity for reporting
                float intensity = (deltaX + deltaY + deltaZ) / 3;

                String msg = "Movement detected at " + sdf.format(new Date()) +
                        " (Intensity: " + String.format("%.2f", intensity) + ")";

                addEvent(msg);
                sendEventNotification(msg);

                // Update cooldown timestamp
                lastMovementTime = currentTime;
            }

            lastX = x;
            lastY = y;
            lastZ = z;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    // ------------------ NOTIFICATION ------------------
    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Main service channel
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Sleep Service",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            serviceChannel.setDescription("Channel for sleep monitoring service");

            // Event notification channel
            NotificationChannel eventChannel = new NotificationChannel(
                    EVENT_CHANNEL_ID,
                    "Sleep Events",
                    NotificationManager.IMPORTANCE_HIGH
            );
            eventChannel.setDescription("Channel for sleep event notifications");
            eventChannel.enableVibration(true);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
                manager.createNotificationChannel(eventChannel);
                Log.d(TAG, "Notification channels created");
            } else {
                Log.e(TAG, "Could not get NotificationManager");
            }
        }
    }

    private void sendEventNotification(String eventMessage) {
        // Create an intent to open the app when notification is clicked
        Intent intent = new Intent(this, HomeActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        // Create notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, EVENT_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_sleep)
                .setContentTitle("Sleep Event Detected")
                .setContentText(eventMessage)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        // Show notification
        if (notificationManager != null) {
            notificationManager.notify(EVENT_NOTIFICATION_BASE_ID + eventCounter++, builder.build());
        }
    }

    // ------------------ CLEANUP ------------------
    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy: Service being destroyed");

        if (handler != null && soundChecker != null) {
            handler.removeCallbacks(soundChecker);
        }

        if (recorder != null) {
            try {
                if (isRecorderInitialized) {
                    recorder.stop();
                }
                recorder.release();
                isRecorderInitialized = false;
            } catch (Exception e) {
                Log.e(TAG, "Error stopping recorder: " + e.getMessage());
            }
            recorder = null;
        }

        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }

        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // Helper method to add an event and log it
    private void addEvent(String eventMessage) {
        Log.d(TAG, eventMessage);
        events.add(eventMessage);
    }

    // Method to get the current events list
    public static ArrayList<String> getEvents() {
        return events;
    }

    // Method to convert events ArrayList to JSON array string
    public static String getEventsAsJsonString() {
        if (events.isEmpty()) {
            return "[]";
        }

        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < events.size(); i++) {
            json.append("\"").append(events.get(i)).append("\"");
            if (i < events.size() - 1) {
                json.append(",");
            }
        }
        json.append("]");

        return json.toString();
    }

    // Static method to trigger a test sound event for development/testing
    public static void simulateTestSound() {
        // Method kept for development purposes but no longer used in main flow
        String eventMsg = "Test sound detected at " + new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        events.add(eventMsg);
    }
}