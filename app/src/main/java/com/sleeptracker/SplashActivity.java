package com.sleeptracker;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ProgressBar;
import androidx.appcompat.app.AppCompatActivity;

import com.sleeptracker.auth.LoginActivity;

public class SplashActivity extends AppCompatActivity {

    ProgressBar progressBar;
    int progressStatus = 0;
    Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        progressBar = findViewById(R.id.splashProgressBar);

        new Thread(() -> {
            while (progressStatus < 50) {
                progressStatus++;
                handler.post(() -> progressBar.setProgress(progressStatus));
                try {
                    Thread.sleep(50); // 100 x 100ms = 10 seconds
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            // After loading, redirect to LoginActivity
            Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        }).start();
    }
}
