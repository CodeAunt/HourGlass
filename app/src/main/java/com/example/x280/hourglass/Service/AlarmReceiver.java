package com.example.x280.hourglass.Service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import android.util.Log;
import android.widget.Toast;


public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        Toast.makeText(context, "You have use phone for 30 minutes.\nRemember to have a rest.",
                Toast.LENGTH_LONG).show();
        Log.d("AlarmDebug","Alarm!!!!");
    }
}