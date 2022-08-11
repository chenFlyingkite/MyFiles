package com.flyingkite.myfiles;

import android.Manifest;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.flyingkite.myfiles.library.FileFragment;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import flyingkite.library.android.util.BackPage;
import flyingkite.library.androidx.TicTac2;

public class MainActivity extends BaseActivity {

    private static final SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
    private TicTac2 clock = new TicTac2();
    private FrameLayout frame;
    private View back;
    private View usages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();

        reqStorage();
        replaceFileFragment();
    }

    private void init() {
        frame = findViewById(R.id.fileFragment);
        back = findViewById(R.id.backBtn);
        back.setOnClickListener((v) -> {
            onBackPressed();
        });
        usages = findViewById(R.id.usageStats);
        usages.setOnClickListener((v) -> {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return;

            UsageStatsManager umgr = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
            long time = System.currentTimeMillis();
            List<UsageStats> stats = umgr.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000 * 10, time);
            logE("stats = %s", stats);
            if (stats == null || stats.isEmpty()) {
                // Usage access is not enabled
                startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
            } else {
                logE("%s stats", stats.size());

                for (int i = 0; i < stats.size(); i++) {
                    UsageStats it = stats.get(i);
                    String s = _fmt("%s\n1stTime = %s, lastTime = %s, used = %s", it.getPackageName()
                            , dateFmt.format(new Date(it.getFirstTimeStamp()))
                            , dateFmt.format(new Date(it.getLastTimeStamp()))
                            , dateFmt.format(new Date(it.getLastTimeUsed()))
                    );
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        s += _fmt(", lasVis = %s, lastFsu = %s, allFsu = %s, allTF = %s, allTV = %s"
                                , dateFmt.format(new Date(it.getLastTimeVisible()))
                                , dateFmt.format(new Date(it.getLastTimeForegroundServiceUsed()))
                                , dateFmt.format(new Date(it.getTotalTimeForegroundServiceUsed()))
                                , dateFmt.format(new Date(it.getTotalTimeInForeground()))
                                , dateFmt.format(new Date(it.getTotalTimeVisible()))
                        );
                    }

                    logE("#%s : %s", i, s);
                }
            }
            listApps();
            listApps2();
        });
        //usages.callOnClick();
    }


    private void listApps() {
        PackageManager pm = getPackageManager();
        if (pm != null) {
            //pm.getApplicationIcon("name");
            List<ApplicationInfo> li = pm.getInstalledApplications(PackageManager.GET_META_DATA);
            logE("%s ApplicationInfo", li.size());
            for (int i = 0; i < li.size(); i++) {
                ApplicationInfo ai = li.get(i);
                logE("#%s : %s, %s, targetSdk = %s", i, ai.className, ai.enabled, ai.targetSdkVersion);
            }
            List<PackageInfo> pi = pm.getInstalledPackages(0);
            logE("%s PackageInfo", pi.size());
            for (int i = 0; i < pi.size(); i++) {
                PackageInfo ai = pi.get(i);
                logE("#%s : %s, update = %s, install= %s, reqPer = %s", i, ai.packageName
                        , dateFmt.format(new Date(ai.lastUpdateTime))
                        , dateFmt.format(new Date(ai.firstInstallTime))
                        , Arrays.toString(ai.requestedPermissions)
                );
            }
        }
    }

    private void listApps2() {
        PackageManager pm = getPackageManager();
        Intent mi = new Intent(Intent.ACTION_MAIN, null);
        mi.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> li = pm.queryIntentActivities(mi, 0);
        logE("%s ResolveInfo", li.size());
        for (int i = 0; i < li.size(); i++) {
            ResolveInfo ri = li.get(i);
            logE("#%s : %s", i, ri);
        }
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
        //fm.executePendingTransactions();
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
        log("Request permissions = " + Arrays.toString(permissions));
        log("and returns results = " + Arrays.toString(grantResults));
        switch (requestCode) {
            case myFileReq:
                break;
        }
    }

    private void reqStorage() {
        // M = 23
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return;

        requestPermissions(neededPermissions(), myFileReq);
    }
}