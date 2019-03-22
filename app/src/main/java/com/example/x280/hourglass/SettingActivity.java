package com.example.x280.hourglass;


import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.example.x280.hourglass.data.SettingManager;

public class SettingActivity extends AppCompatActivity {

    Switch mSwitchSystem;
    Switch mSwitchUninstall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.settings);
        }

        // hide system
        mSwitchSystem = findViewById(R.id.switch_system_apps);
        mSwitchSystem.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (SettingManager.getInstance().getSystemSettings(SettingManager.PREF_SETTINGS_HIDE_SYSTEM_APPS) != b) {
                    SettingManager.getInstance().putBoolean(SettingManager.PREF_SETTINGS_HIDE_SYSTEM_APPS, b);
                    setResult(1);
                }
            }
        });

        findViewById(R.id.group_system).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSwitchSystem.performClick();
            }
        });

        // hide uninstall
        mSwitchUninstall = findViewById(R.id.switch_uninstall_appps);
        mSwitchUninstall.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (SettingManager.getInstance().getUninstallSettings(SettingManager.PREF_SETTINGS_HIDE_UNINSTALL_APPS) != b) {
                    SettingManager.getInstance().putBoolean(SettingManager.PREF_SETTINGS_HIDE_UNINSTALL_APPS, b);
                    setResult(1);
                }
            }
        });

        findViewById(R.id.group_uninstall).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSwitchUninstall.performClick();
            }
        });

        // ignore list
        findViewById(R.id.group_ignore).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                startActivity(new Intent(SettingActivity.this, IgnoreActivity.class));
            }
        });

        restoreStatus();
    }

    private void restoreStatus() {
        mSwitchSystem.setChecked(SettingManager.getInstance().getSystemSettings(SettingManager.PREF_SETTINGS_HIDE_SYSTEM_APPS));
        mSwitchUninstall.setChecked(SettingManager.getInstance().getUninstallSettings(SettingManager.PREF_SETTINGS_HIDE_UNINSTALL_APPS));
    }
}
