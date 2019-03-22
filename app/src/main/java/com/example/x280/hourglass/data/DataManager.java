package com.example.x280.hourglass.data;

import android.Manifest;
import android.app.AppOpsManager;
import android.app.usage.NetworkStats;
import android.app.usage.NetworkStatsManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.RemoteException;
//import android.preference.SettingManager;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.example.x280.hourglass.Service.AppUtil;
import com.example.x280.hourglass.Service.SortEnum;
import com.example.x280.hourglass.data.db.DbIgnoreExecutor;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

public class DataManager {

    private static DataManager mInstance;

    public static void init() {
        mInstance = new DataManager();
    }

    public static DataManager getInstance() {
        return mInstance;
    }

    public void requestPermission(Context context) {
        Intent intent = new Intent(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
        intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    // check the permission
    public boolean hasPermission(Context context) {
        AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        if (appOps != null) {
            int mode = appOps.checkOpNoThrow("android:get_usage_stats", android.os.Process.myUid(), context.getPackageName());
            return mode == AppOpsManager.MODE_ALLOWED;
        }
        return false;
    }

    // Unnecessary
    // Get the event timeline detail of the app..
    public List<AppItem> getTargetAppTimeline(Context context, String target, int offset) {
        List<AppItem> items = new ArrayList<>();
        UsageStatsManager manager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        if (manager != null) {
            //Set time range
            long[] range = AppUtil.getTimeRange(SortEnum.getSortEnum(offset));
            UsageEvents events = manager.queryEvents(range[0], range[1]);
            UsageEvents.Event event = new UsageEvents.Event();

            //
            AppItem item = new AppItem();
            item.mPackageName = target;
            item.mName = AppUtil.parsePackageName(context.getPackageManager(), target);

            // restore
            ClonedEvent prevEndEvent = null;
            long start = 0;

            while (events.hasNextEvent()) {
                events.getNextEvent(event);
                String currentPackage = event.getPackageName();
                int eventType = event.getEventType();
                long eventTime = event.getTimeStamp();
                Log.d("Event", currentPackage + " " + target + " " + new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()).format(new Date(eventTime)) + " " + eventType);

                // start the target record
                if (currentPackage.equals(target)) {
                    Log.d("AppTarget", currentPackage + " " + target + " " + new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()).format(new Date(eventTime)) + " " + eventType);

                    // beginning event
                    if (eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                        Log.d("StartEvent", "start " + start);
                        //init the data
                        if (start == 0) {
                            start = eventTime;
                            item.mEventTime = eventTime;
                            item.mEventType = eventType;
                            item.mUsageTime = 0;
                            items.add(item.copy());
                        }
                    } else if (eventType == UsageEvents.Event.MOVE_TO_BACKGROUND) { // END THIS EVENT
                         if (start > 0) {
                            prevEndEvent = new ClonedEvent(event);
                        }
                        Log.d("AddEventDebug", "add end " + start);
                    }
                } else {
                    // Last event
                    if (prevEndEvent != null && start > 0) {
                        item.mEventTime = prevEndEvent.timeStamp;
                        item.mEventType = prevEndEvent.eventType;
                        item.mUsageTime = prevEndEvent.timeStamp - start;

                        if (item.mUsageTime <= 0) item.mUsageTime = 0;
                        if (item.mUsageTime > AppConst.USAGE_TIME_MIX) item.mCount++;
                        items.add(item.copy());
                        start = 0;
                        prevEndEvent = null;
                    }
                }
            }
        }
        return items;
    }

    public List<AppItem> getApps(Context context, int sort, int offset) {
        // restore the item
        List<AppItem> items = new ArrayList<>();
        List<AppItem> newList = new ArrayList<>();

        UsageStatsManager manager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        if (manager != null) {

            String prevPackage = "";
            Map<String, Long> startPoints = new HashMap<>();
            Map<String, ClonedEvent> endPoints = new HashMap<>();
            // Fetch the events
            long[] range = AppUtil.getTimeRange(SortEnum.getSortEnum(offset));
            //long beginTime, long endTime
            UsageEvents events = manager.queryEvents(range[0], range[1]);
            UsageEvents.Event event = new UsageEvents.Event();

            while (events.hasNextEvent()) {
                // analysis the time
                events.getNextEvent(event);
                int eventType = event.getEventType();
                long eventTime = event.getTimeStamp();
                String eventPackage = event.getPackageName();
                // record the start point
                if (eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    AppItem item = containItem(items, eventPackage);
                    if (item == null) {
                        item = new AppItem();
                        item.mPackageName = eventPackage;
                        items.add(item);
                    }
                    if (!startPoints.containsKey(eventPackage)) {
                        startPoints.put(eventPackage, eventTime);
                    }
                }
                // record the end point
                if (eventType == UsageEvents.Event.MOVE_TO_BACKGROUND) {
                    if (startPoints.size() > 0 && startPoints.containsKey(eventPackage)) {
                        endPoints.put(eventPackage, new ClonedEvent(event));
                    }
                }

                // Time and frequency
                if (TextUtils.isEmpty(prevPackage)) prevPackage = eventPackage;

                if (!prevPackage.equals(eventPackage)) { // 包名有变化
                    if (startPoints.containsKey(prevPackage) && endPoints.containsKey(prevPackage)) {
                        ClonedEvent lastEndEvent = endPoints.get(prevPackage);
                        AppItem listItem = containItem(items, prevPackage);
                        if (listItem != null) { // update list item info
                            listItem.mEventTime = lastEndEvent.timeStamp;
                            long duration = lastEndEvent.timeStamp - startPoints.get(prevPackage);
                            //filter yesterday event
                            if (duration <= 0) {
                                duration = 0;
                            }
                            listItem.mUsageTime += duration;
                            if (duration > AppConst.USAGE_TIME_MIX) {
                                listItem.mCount++;
                            }
                        }
                        startPoints.remove(prevPackage);
                        endPoints.remove(prevPackage);
                    }
                    prevPackage = eventPackage;
                }
            }
        }

        // Sort by time
        if (items.size() > 0) {
            boolean valid = false;
            Map<String, Long> mobileData = new HashMap<>();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                valid = true;
                NetworkStatsManager networkStatsManager = (NetworkStatsManager) context.getSystemService(Context.NETWORK_STATS_SERVICE);
                TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                mobileData = getMobileData(context, telephonyManager, networkStatsManager, offset);
            }

            boolean hideSystem = SettingManager.getInstance().getBoolean(SettingManager.PREF_SETTINGS_HIDE_SYSTEM_APPS);
            boolean hideUninstall = SettingManager.getInstance().getBoolean(SettingManager.PREF_SETTINGS_HIDE_UNINSTALL_APPS);
            List<IgnoreItem> ignoreItems = DbIgnoreExecutor.getInstance().getAllItems();
            PackageManager packageManager = context.getPackageManager();
            for (AppItem item : items) {
                if (!AppUtil.openable(packageManager, item.mPackageName)) {
                    continue;
                }
                if (hideSystem && AppUtil.isSystemApp(packageManager, item.mPackageName)) {
                    continue;
                }
                if (hideUninstall && !AppUtil.isInstalled(packageManager, item.mPackageName)) {
                    continue;
                }
                if (inIgnoreList(ignoreItems, item.mPackageName)) {
                    continue;
                }
                if (valid) {
                    String key = "u" + AppUtil.getAppUid(packageManager, item.mPackageName);
                    if (mobileData.size() > 0 && mobileData.containsKey(key)) {
                        item.mMobile = mobileData.get(key);
                    }
                }
                item.mName = AppUtil.parsePackageName(packageManager, item.mPackageName);
                newList.add(item);
            }

            if (sort == 0) {
                Collections.sort(newList, new Comparator<AppItem>() {
                    @Override
                    public int compare(AppItem left, AppItem right) {
                        return (int) (right.mUsageTime - left.mUsageTime);
                    }
                });
            } else if (sort == 1) {
                Collections.sort(newList, new Comparator<AppItem>() {
                    @Override
                    public int compare(AppItem left, AppItem right) {
                        return (int) (right.mEventTime - left.mEventTime);
                    }
                });
            } else if (sort == 2) {
                Collections.sort(newList, new Comparator<AppItem>() {
                    @Override
                    public int compare(AppItem left, AppItem right) {
                        return right.mCount - left.mCount;
                    }
                });
            } else {
                Collections.sort(newList, new Comparator<AppItem>() {
                    @Override
                    public int compare(AppItem left, AppItem right) {
                        return (int) (right.mMobile - left.mMobile);
                    }
                });
            }
        }
        return newList;
    }

    // mobile data
    private Map<String, Long> getMobileData(Context context, TelephonyManager tm, NetworkStatsManager nsm, int offset) {
        Map<String, Long> result = new HashMap<>();
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            long[] range = AppUtil.getTimeRange(SortEnum.getSortEnum(offset));
            NetworkStats networkStatsM;
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    networkStatsM = nsm.querySummary(ConnectivityManager.TYPE_MOBILE, tm.getSubscriberId(), range[0], range[1]);
                    if (networkStatsM != null) {
                        while (networkStatsM.hasNextBucket()) {
                            NetworkStats.Bucket bucket = new NetworkStats.Bucket();
                            networkStatsM.getNextBucket(bucket);
                            String key = "u" + bucket.getUid();
                            Log.d("mobileData", key + " " + bucket.getTxBytes() + "");
                            if (result.containsKey(key)) {
                                result.put(key, result.get(key) + bucket.getTxBytes() + bucket.getRxBytes());
                            } else {
                                result.put(key, bucket.getTxBytes() + bucket.getRxBytes());
                            }
                        }
                    }
                }
            } catch (RemoteException e) {
                e.printStackTrace();
                Log.e("GetMessageError", e.getMessage());
            }
        }
        return result;
    }

    private AppItem containItem(List<AppItem> items, String packageName) {
        for (AppItem item : items) {
            if (item.mPackageName.equals(packageName)) return item;
        }
        return null;
    }

    private boolean inIgnoreList(List<IgnoreItem> items, String packageName) {
        for (IgnoreItem item : items) {
            if (item.mPackageName.equals(packageName)) return true;
        }
        return false;
    }

    class ClonedEvent {

        String packageName;
        String eventClass;
        long timeStamp;
        int eventType;

        ClonedEvent(UsageEvents.Event event) {
            packageName = event.getPackageName();
            eventClass = event.getClassName();
            timeStamp = event.getTimeStamp();
            eventType = event.getEventType();
        }
    }
}
