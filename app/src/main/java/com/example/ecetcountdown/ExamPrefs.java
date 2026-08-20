package com.example.ecetcountdown;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Calendar;

/**
 * Handles saving and loading the exam date/time so it can be
 * edited by the user and survives app restarts.
 *
 * If the user has never set a custom date, getExamTimeMillis()
 * falls back to the default hardcoded date (15 May 2027, 9:00 AM).
 */
public class ExamPrefs {

    private static final String PREFS_NAME = "ecet_prefs";
    private static final String KEY_EXAM_TIME_MILLIS = "exam_time_millis";

    // Default exam date/time — same as before, used as fallback
    private static final int DEFAULT_YEAR = 2027;
    private static final int DEFAULT_MONTH = Calendar.MAY;
    private static final int DEFAULT_DAY = 15;
    private static final int DEFAULT_HOUR = 9;
    private static final int DEFAULT_MINUTE = 0;

    public static long getDefaultExamTimeMillis() {
        Calendar cal = Calendar.getInstance();
        cal.set(DEFAULT_YEAR, DEFAULT_MONTH, DEFAULT_DAY, DEFAULT_HOUR, DEFAULT_MINUTE, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    public static long getExamTimeMillis(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        // If no custom value was ever saved, return the default
        return prefs.getLong(KEY_EXAM_TIME_MILLIS, getDefaultExamTimeMillis());
    }

    public static void setExamTimeMillis(Context context, long millis) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putLong(KEY_EXAM_TIME_MILLIS, millis).apply();
    }

    public static void resetToDefault(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_EXAM_TIME_MILLIS).apply();
    }
}