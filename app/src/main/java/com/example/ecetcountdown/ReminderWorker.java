package com.example.ecetcountdown;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

/**
 * Background job that runs once a day (scheduled via WorkManager)
 * and triggers the daily reminder notification showing days left.
 */
public class ReminderWorker extends Worker {

    public ReminderWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();

        // Make sure the channel exists (safe to call every time)
        NotificationHelper.createNotificationChannel(context);

        long examTimeMillis = ExamPrefs.getExamTimeMillis(context);
        long remainingMillis = examTimeMillis - System.currentTimeMillis();

        long daysRemaining = remainingMillis / (1000 * 60 * 60 * 24);
        if (daysRemaining < 0) {
            daysRemaining = 0;
        }

        NotificationHelper.showDailyReminder(context, daysRemaining);

        return Result.success();
    }
}