package com.flyingkite.myfiles;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        log("onActivityResult(%s, %s, %s)", requestCode, resultCode, data);
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

    protected Fragment findFragmentById(@IdRes int fragmentId) {
        return getSupportFragmentManager().findFragmentById(fragmentId);
    }

    protected Fragment findFragmentByTag(String tag) {
        return getSupportFragmentManager().findFragmentByTag(tag);
    }
}
