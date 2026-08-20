package com.example.ecetcountdown;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

/**
 * Handles creating the notification channel (required on Android 8+)
 * and building/showing the actual daily reminder notification.
 */
public class NotificationHelper {

    private static final String CHANNEL_ID = "ecet_daily_reminder";
    private static final int NOTIFICATION_ID = 1001;

    /**
     * Must be called once before showing any notification.
     * Safe to call multiple times — creating an existing channel again does nothing.
     */
    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Daily Exam Reminder",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Daily reminder showing days left for ECET 2027");

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * Builds and shows the daily reminder notification with the
     * current number of days remaining.
     */
    public static void showDailyReminder(Context context, long daysRemaining) {
        Intent openAppIntent = new Intent(context, MainActivity.class);
        openAppIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String title = "ECET 2027 Countdown";
        String message;
        if (daysRemaining > 0) {
            message = daysRemaining + " day(s) left. Stay focused and keep studying!";
        } else {
            message = "Exam day is here. All the best for ECET 2027!";
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        // Guard: on Android 13+, showing a notification without permission
        // would crash. This check keeps things safe even if permission
        // was somehow revoked after being granted.
        if (androidx.core.content.ContextCompat.checkSelfPermission(context,
                android.Manifest.permission.POST_NOTIFICATIONS)
                == android.content.pm.PackageManager.PERMISSION_GRANTED
                || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        }
    }
}