package com.example.ecetcountdown;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class SettingsActivity extends AppCompatActivity {

    private static final String REMINDER_WORK_NAME = "ecet_daily_reminder_work";
    private static final String PREFS_NAME = "ecet_prefs";
    private static final String KEY_REMINDER_ENABLED = "reminder_enabled";
    private static final String KEY_REMINDER_HOUR = "reminder_hour";
    private static final String KEY_REMINDER_MINUTE = "reminder_minute";

    private TextView textSelectedDate, textSelectedTime, textReminderTime;
    private Button btnPickDate, btnPickTime, btnPickReminderTime, btnSave, btnReset;
    private Switch switchReminder;

    private Calendar workingCalendar;       // exam date/time being edited
    private int reminderHour = 20;          // default reminder time: 8:00 PM
    private int reminderMinute = 0;

    // Modern way to request a runtime permission and handle the result
    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    scheduleReminderWork();
                    Toast.makeText(this, "Reminders enabled", Toast.LENGTH_SHORT).show();
                } else {
                    switchReminder.setChecked(false);
                    Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        textSelectedDate = findViewById(R.id.textSelectedDate);
        textSelectedTime = findViewById(R.id.textSelectedTime);
        textReminderTime = findViewById(R.id.textReminderTime);
        btnPickDate = findViewById(R.id.btnPickDate);
        btnPickTime = findViewById(R.id.btnPickTime);
        btnPickReminderTime = findViewById(R.id.btnPickReminderTime);
        btnSave = findViewById(R.id.btnSave);
        btnReset = findViewById(R.id.btnReset);
        switchReminder = findViewById(R.id.switchReminder);

        workingCalendar = Calendar.getInstance();
        workingCalendar.setTimeInMillis(ExamPrefs.getExamTimeMillis(this));

        loadReminderPrefs();
        refreshDisplayedDateTime();
        refreshDisplayedReminderTime();

        btnPickDate.setOnClickListener(v -> showDatePicker());
        btnPickTime.setOnClickListener(v -> showTimePicker());
        btnPickReminderTime.setOnClickListener(v -> showReminderTimePicker());
        btnSave.setOnClickListener(v -> saveAndClose());
        btnReset.setOnClickListener(v -> resetAndClose());

        switchReminder.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                requestNotificationPermissionIfNeeded();
            } else {
                cancelReminderWork();
                Toast.makeText(this, "Reminders disabled", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ---------- Exam date/time pickers ----------

    private void showDatePicker() {
        int year = workingCalendar.get(Calendar.YEAR);
        int month = workingCalendar.get(Calendar.MONTH);
        int day = workingCalendar.get(Calendar.DAY_OF_MONTH);

        new DatePickerDialog(this, (view, selectedYear, selectedMonth, selectedDay) -> {
            workingCalendar.set(Calendar.YEAR, selectedYear);
            workingCalendar.set(Calendar.MONTH, selectedMonth);
            workingCalendar.set(Calendar.DAY_OF_MONTH, selectedDay);
            refreshDisplayedDateTime();
        }, year, month, day).show();
    }

    private void showTimePicker() {
        int hour = workingCalendar.get(Calendar.HOUR_OF_DAY);
        int minute = workingCalendar.get(Calendar.MINUTE);

        new TimePickerDialog(this, (view, selectedHour, selectedMinute) -> {
            workingCalendar.set(Calendar.HOUR_OF_DAY, selectedHour);
            workingCalendar.set(Calendar.MINUTE, selectedMinute);
            refreshDisplayedDateTime();
        }, hour, minute, false).show();
    }

    private void refreshDisplayedDateTime() {
        java.text.SimpleDateFormat dateFormat =
                new java.text.SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        java.text.SimpleDateFormat timeFormat =
                new java.text.SimpleDateFormat("hh:mm a", Locale.getDefault());

        textSelectedDate.setText(dateFormat.format(workingCalendar.getTime()));
        textSelectedTime.setText(timeFormat.format(workingCalendar.getTime()));
    }

    // ---------- Reminder time picker ----------

    private void showReminderTimePicker() {
        new TimePickerDialog(this, (view, selectedHour, selectedMinute) -> {
            reminderHour = selectedHour;
            reminderMinute = selectedMinute;
            refreshDisplayedReminderTime();
            saveReminderPrefs();
            if (switchReminder.isChecked()) {
                scheduleReminderWork(); // reschedule with new time
            }
        }, reminderHour, reminderMinute, false).show();
    }

    private void refreshDisplayedReminderTime() {
        Calendar temp = Calendar.getInstance();
        temp.set(Calendar.HOUR_OF_DAY, reminderHour);
        temp.set(Calendar.MINUTE, reminderMinute);

        java.text.SimpleDateFormat timeFormat =
                new java.text.SimpleDateFormat("hh:mm a", Locale.getDefault());
        textReminderTime.setText(timeFormat.format(temp.getTime()));
    }

    // ---------- Reminder prefs (separate small helper, kept simple inline) ----------

    private void loadReminderPrefs() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean enabled = prefs.getBoolean(KEY_REMINDER_ENABLED, false);
        reminderHour = prefs.getInt(KEY_REMINDER_HOUR, 20);
        reminderMinute = prefs.getInt(KEY_REMINDER_MINUTE, 0);
        switchReminder.setChecked(enabled);
    }

    private void saveReminderPrefs() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit()
                .putBoolean(KEY_REMINDER_ENABLED, switchReminder.isChecked())
                .putInt(KEY_REMINDER_HOUR, reminderHour)
                .putInt(KEY_REMINDER_MINUTE, reminderMinute)
                .apply();
    }

    // ---------- Permission handling ----------

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED) {
                scheduleReminderWork();
                saveReminderPrefs();
            } else {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        } else {
            // Below Android 13, no runtime permission needed
            scheduleReminderWork();
            saveReminderPrefs();
        }
    }

    // ---------- WorkManager scheduling ----------

    private void scheduleReminderWork() {
        saveReminderPrefs();

        long initialDelay = calculateInitialDelayMillis(reminderHour, reminderMinute);

        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                ReminderWorker.class, 1, TimeUnit.DAYS)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                REMINDER_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
        );
    }

    private void cancelReminderWork() {
        WorkManager.getInstance(this).cancelUniqueWork(REMINDER_WORK_NAME);
        saveReminderPrefs();
    }

    /**
     * Calculates milliseconds from now until the next occurrence
     * of the given hour:minute (today if still upcoming, otherwise tomorrow).
     */
    private long calculateInitialDelayMillis(int hour, int minute) {
        Calendar now = Calendar.getInstance();
        Calendar target = Calendar.getInstance();
        target.set(Calendar.HOUR_OF_DAY, hour);
        target.set(Calendar.MINUTE, minute);
        target.set(Calendar.SECOND, 0);
        target.set(Calendar.MILLISECOND, 0);

        if (target.before(now)) {
            target.add(Calendar.DAY_OF_YEAR, 1);
        }

        return target.getTimeInMillis() - now.getTimeInMillis();
    }

    // ---------- Save / Reset ----------

    private void saveAndClose() {
        workingCalendar.set(Calendar.SECOND, 0);
        workingCalendar.set(Calendar.MILLISECOND, 0);
        ExamPrefs.setExamTimeMillis(this, workingCalendar.getTimeInMillis());
        saveReminderPrefs();
        Toast.makeText(this, "Exam date updated", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void resetAndClose() {
        ExamPrefs.resetToDefault(this);
        Toast.makeText(this, "Reset to default exam date", Toast.LENGTH_SHORT).show();
        finish();
    }
}