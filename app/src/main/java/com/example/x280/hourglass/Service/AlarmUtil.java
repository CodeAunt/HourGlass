package com.example.x280.hourglass.Service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * created 20190319 zhouzili
 */
public class AlarmUtil {

    public static void setAlarm(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            long start = System.currentTimeMillis();
            Intent in = new Intent("ALARM_RECEIVER");
            PendingIntent pi = PendingIntent.getBroadcast(context, 0, in, PendingIntent.FLAG_CANCEL_CURRENT);
            alarmManager.set(AlarmManager.RTC_WAKEUP, start +  3 * 1000, pi);//86400
            Log.d("AlarmDebug", "alarm set");
        }
    }
}