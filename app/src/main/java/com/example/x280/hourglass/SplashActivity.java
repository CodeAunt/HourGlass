package com.example.x280.hourglass;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

import com.example.x280.hourglass.data.FileLogManager;

import java.util.ArrayList;
import java.util.List;



//check normal permissions
public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        //check for permission. delay for every time you use.
        if (checkPermissions().size() == 0) {
            delayEnter();
        } else {
            requestPermissions();
        }

    }

    private void delayEnter() {
        new CountDownTimer(1200, 1200) {
            @Override
            public void onTick(long l) {
            }

            //Open Drawer
            @Override
            public void onFinish() {
                startActivity(new Intent(SplashActivity.this, Drawer.class));
                finish();
            }
        }.start();
    }

    private List<String> checkPermissions() {
        List<String> permissions = new ArrayList<>();

        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.READ_PHONE_STATE);
        }
        return permissions;
    }

    private void requestPermissions() {
        List<String> permissions = checkPermissions();
        if (permissions.size() > 0) {
            ActivityCompat.requestPermissions(this, permissions.toArray(new String[permissions.size()]), 0);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for (int i = 0; i < permissions.length; i++) {
            String permission = permissions[i];
            if (permission.equals(Manifest.permission.WRITE_EXTERNAL_STORAGE) && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                FileLogManager.init();
                break;
            }
        }
        delayEnter();
    }
}
