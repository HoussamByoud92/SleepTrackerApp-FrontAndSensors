package com.sleeptracker.sensor;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import com.sleeptracker.R;

import java.util.ArrayList;
import java.util.Date;
import java.text.SimpleDateFormat;

public class SleepMonitorActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private MediaRecorder recorder;
    private TextView tvEvents;
    private ArrayList<String> events;
    private SharedPreferences prefs;

    private final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sleep_monitor);

        tvEvents = findViewById(R.id.tvEvents);
        events = new ArrayList<>();
        prefs = getSharedPreferences("sleepEvents", MODE_PRIVATE);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);

        setupMicrophone();
        createNotificationChannel();
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        startMicrophoneListener();
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
        if (recorder != null) {
            recorder.stop();
            recorder.release();
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float x = event.values[0], y = event.values[1], z = event.values[2];
        double magnitude = Math.sqrt(x * x + y * y + z * z);

        if (magnitude > 15) {  // adjust this threshold
            String time = sdf.format(new Date());
            String msg = "Movement detected at " + time;
            logEvent(msg);
        }
    }

    private void setupMicrophone() {
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        recorder.setOutputFile("/dev/null");
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        try {
            recorder.prepare();
        } catch (Exception e) {
            Toast.makeText(this, "Microphone error", Toast.LENGTH_SHORT).show();
        }
    }

    private void startMicrophoneListener() {
        recorder.start();

        Handler handler = new Handler();
        Runnable soundChecker = new Runnable() {
            @Override
            public void run() {
                int amplitude = recorder.getMaxAmplitude();
                if (amplitude > 15000) {
                    String msg = "Sound detected at " + sdf.format(new Date());
                    logEvent(msg);
                    notifyUser("Possible wake-up detected");
                }
                handler.postDelayed(this, 3000); // every 3 seconds
            }
        };
        handler.post(soundChecker);
    }

    private void logEvent(String message) {
        events.add(message);
        tvEvents.append(message + "\n");
        saveToPrefs();
    }

    private void saveToPrefs() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("events", String.join(";", events));
        editor.putLong("timestamp", System.currentTimeMillis());
        editor.apply();
    }

    private void notifyUser(String msg) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "sleep_channel")
                .setSmallIcon(R.drawable.ic_sleep)
                .setContentTitle("Sleep Tracker")
                .setContentText(msg)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify((int) System.currentTimeMillis(), builder.build());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "sleep_channel",
                    "Sleep Events",
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}
