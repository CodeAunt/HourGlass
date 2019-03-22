package com.example.x280.hourglass;


import android.app.Application;
import android.content.Intent;

import com.example.x280.hourglass.Service.AppService;
import com.example.x280.hourglass.Service.CrashHandler;
import com.example.x280.hourglass.data.AppConst;
import com.example.x280.hourglass.data.AppItem;
import com.example.x280.hourglass.data.DataManager;
import com.example.x280.hourglass.data.SettingManager;
import com.example.x280.hourglass.data.db.DbHistoryExecutor;
import com.example.x280.hourglass.data.db.DbIgnoreExecutor;

import java.util.ArrayList;
import java.util.List;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        SettingManager.init(this);
        getApplicationContext().startService(new Intent(getApplicationContext(), AppService.class));
        DbIgnoreExecutor.init(getApplicationContext());
        DbHistoryExecutor.init(getApplicationContext());
        DataManager.init();
        addDefaultIgnoreAppsToDB();
        if (AppConst.CRASH_TO_FILE) CrashHandler.getInstance().init();
    }

    private void addDefaultIgnoreAppsToDB() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<String> mDefaults = new ArrayList<>();
                mDefaults.add("com.android.settings");
                mDefaults.add(BuildConfig.APPLICATION_ID);
                for (String packageName : mDefaults) {
                    AppItem item = new AppItem();
                    item.mPackageName = packageName;
                    item.mEventTime = System.currentTimeMillis();
                    DbIgnoreExecutor.getInstance().insertItem(item);
                }
            }
        }).run();
    }
}