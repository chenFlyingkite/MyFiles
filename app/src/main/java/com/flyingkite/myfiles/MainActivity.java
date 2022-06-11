package com.flyingkite.myfiles;

import android.Manifest;
import android.os.Build;
import android.os.Bundle;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.flyingkite.myfiles.library.FileFragment;

import java.util.Arrays;

import flyingkite.library.android.util.BackPage;
import flyingkite.library.androidx.TicTac2;

public class MainActivity extends BaseActivity {

    private TicTac2 clock = new TicTac2();
    private FrameLayout frame;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        frame = findViewById(R.id.fileFragment);
        reqStorage();
        replaceFileFragment();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPermissionState();
    }

    @Override
    public void onBackPressed() {
        Fragment ff = findFragmentById(R.id.fileFragment);
        if (ff instanceof BackPage) {
            BackPage b = (BackPage) ff;
            if (b.onBackPressed()) {
                return;
            }
        }
        super.onBackPressed();
    }

    private void replaceFileFragment() {
        FileFragment f = new FileFragment();
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction fx = fm.beginTransaction();
        fx.replace(R.id.fileFragment, f, FileFragment.TAG);
        fx.commitAllowingStateLoss();
        fm.executePendingTransactions();
    }

    private static final int myFileReq = 123;

    @Override
    protected String[] neededPermissions() {
        // test these
        //String[] permissions = {Manifest.permission_group.STORAGE};
        //String[] permissions = {Manifest.permission.MANAGE_EXTERNAL_STORAGE};
        //String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.MANAGE_EXTERNAL_STORAGE};

        String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // R = 30
            permissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.MANAGE_EXTERNAL_STORAGE};
        }
        return permissions;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case myFileReq:
                log("Request permissions = " + Arrays.toString(permissions));
                log("and returns results = " + Arrays.toString(grantResults));
                break;
        }
    }

    private void reqStorage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // M = 23
            requestPermissions(neededPermissions(), myFileReq);
        }
    }
}