package com.example.ecetcountdown;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextView textDays, textHours, textMinutes, textSeconds;
    private long targetTimeMillis;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable countdownRunnable = new Runnable() {
        @Override
        public void run() {
            updateCountdown();
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textDays = findViewById(R.id.textDays);
        textHours = findViewById(R.id.textHours);
        textMinutes = findViewById(R.id.textMinutes);
        textSeconds = findViewById(R.id.textSeconds);

        TextView btnSettings = findViewById(R.id.btnSettings);
        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-read the exam time every time this screen becomes visible,
        // in case the user just changed it in Settings.
        targetTimeMillis = ExamPrefs.getExamTimeMillis(this);

        // (Re)start the countdown loop
        handler.removeCallbacks(countdownRunnable);
        handler.post(countdownRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stop updating while this screen isn't visible, to save battery
        handler.removeCallbacks(countdownRunnable);
    }

    private void updateCountdown() {
        long currentTimeMillis = System.currentTimeMillis();
        long remainingMillis = targetTimeMillis - currentTimeMillis;

        if (remainingMillis < 0) {
            remainingMillis = 0;
        }

        long days = remainingMillis / (1000 * 60 * 60 * 24);
        long hours = (remainingMillis / (1000 * 60 * 60)) % 24;
        long minutes = (remainingMillis / (1000 * 60)) % 60;
        long seconds = (remainingMillis / 1000) % 60;

        textDays.setText(String.format(Locale.getDefault(), "%02d", days));
        textHours.setText(String.format(Locale.getDefault(), "%02d", hours));
        textMinutes.setText(String.format(Locale.getDefault(), "%02d", minutes));
        textSeconds.setText(String.format(Locale.getDefault(), "%02d", seconds));
    }
}