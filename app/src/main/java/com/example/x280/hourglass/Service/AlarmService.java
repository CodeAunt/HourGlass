package com.example.x280.hourglass.Service;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;

import com.example.x280.hourglass.data.AppItem;
import com.example.x280.hourglass.data.DataManager;
import com.example.x280.hourglass.data.FileLogManager;
import com.example.x280.hourglass.data.HistoryItem;
import com.example.x280.hourglass.data.db.DbHistoryExecutor;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AlarmService extends IntentService {

    private static final String ALARM_SERVICE_NAME = "alarm.service";

    public AlarmService() {
        super(ALARM_SERVICE_NAME);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        DataManager manager = DataManager.getInstance();
        List<AppItem> items = manager.getApps(this.getApplicationContext(), 0, 1);
        for (AppItem item : items) {
            HistoryItem historyItem = new HistoryItem();
            historyItem.mName = item.mName;
            historyItem.mPackageName = item.mPackageName;
            historyItem.mMobileTraffic = item.mMobile;
            historyItem.mIsSystem = AppUtil.isSystemApp(getPackageManager(), item.mPackageName) ? 1 : 0;
            historyItem.mDuration = item.mUsageTime;
            historyItem.mTimeStamp = AppUtil.getYesterdayTimestamp();
            historyItem.mDate = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.getDefault()).format(new Date(historyItem.mTimeStamp));
            DbHistoryExecutor.getInstance().insert(historyItem);
        }

        FileLogManager fileLogManager = FileLogManager.getInstance();
        fileLogManager.log("alarm " + new SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.getDefault()).format(new Date(System.currentTimeMillis())) + "\n");

        AlarmUtil.setAlarm(this.getApplicationContext());
    }
}