package com.flyingkite.myfiles;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import flyingkite.library.android.log.Loggable;
import flyingkite.library.android.util.ToastUtil;

public class BaseActivity extends AppCompatActivity implements Loggable, ToastUtil {

    protected static final String[] PERMISSION_RESULT_STATE = {"Denied", "Granted", "OK", "Cancel"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        log("LConCreate(%s) %s", savedInstanceState, this);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        log("LConRestart() %s", this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        log("LConStart() %s", this);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        log("LConRestoreInstanceState(%s) %s", savedInstanceState, this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        log("LConResume() %s", this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        log("LConPause() %s", this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        log("LConStop() %s", this);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        log("LConSaveInstanceState(%s) %s", outState, this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        log("LConDestroy() %s", this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        log("LConActivityResult(%s, %s, %s)", requestCode, resultCode, data);
        //Activity.RESULT_OK = -1
    }

    @Override
    public void log(String message) {
        //Loggable.super.log(message);
        logE(message);
        //Log.e(LTag(), message);
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

    @Override
    public Context getContext() {
        return this;
    }
}
