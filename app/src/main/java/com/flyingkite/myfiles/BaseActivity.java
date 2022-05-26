package com.flyingkite.myfiles;

import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import flyingkite.library.android.log.Loggable;

public class BaseActivity extends AppCompatActivity implements Loggable {

    protected static final String[] PERMISSION_RESULT_STATE = {"Denied", "Granted", "OK", "Cancel"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        log("onCreate(%s)", savedInstanceState);
    }

    @Override
    protected void onStart() {
        super.onStart();
        log("onStart()");
    }

    @Override
    protected void onResume() {
        super.onResume();
        log("onResume()");
    }

    @Override
    protected void onPause() {
        super.onPause();
        log("onPause()");
    }

    @Override
    protected void onStop() {
        super.onStop();
        log("onStop()");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        log("onDestroy()");
    }

    protected String[] neededPermissions() {
        return new String[0];
    }

    protected void checkPermissionState() {
        String[] all = neededPermissions();
        int n = all == null ? 0 : all.length;
        for (int i = 0; i < n; i++) {
            String s = all[i];
            int v = PackageManager.PERMISSION_GRANTED;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                v = checkSelfPermission(s);
            }
            logE("%s for %s", PERMISSION_RESULT_STATE[v + 1], s);
        }
    }
}
