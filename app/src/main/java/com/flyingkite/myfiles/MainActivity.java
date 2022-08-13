package com.flyingkite.myfiles;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.usage.StorageStatsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.provider.Settings;
import android.view.View;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.flyingkite.myfiles.library.FileFragment;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import flyingkite.library.android.util.BackPage;
import flyingkite.library.androidx.TicTac2;
import flyingkite.library.java.util.FileUtil;

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
            listUsage();
            listApps();
            listApps2();
        });

        findViewById(R.id.spaceIntent).setOnClickListener((v) -> {
            StorageManager mgr = (StorageManager) getSystemService(Context.STORAGE_SERVICE);
            a(Environment.getExternalStorageDirectory()); // total = 119.21 GB, free = 91.76 GB
            //a(Environment.getRootDirectory());
        });

        findViewById(R.id.myStorage).setOnClickListener((v) -> {
            seeMyAppAllFilesAccess();
        });
        findViewById(R.id.allOwners).setOnClickListener((v) -> {
            seeListOfAppAllFilesAccess();
        });
        findViewById(R.id.clearCache).setOnClickListener((v) -> {
            File root = Environment.getExternalStorageDirectory();
            logE("before delete");
            App.me.statfs(root);

            caches();

            logE("after delete");
            App.me.statfs(root);
        });

        findViewById(R.id.clearCacheIntent).setOnClickListener((v) -> {
            File root = Environment.getExternalStorageDirectory();
            logE("before delete");
            App.me.statfs(root);
            logE("dir = %s", App.me.getCacheDir());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                logE("code dir = %s", App.me.getCodeCacheDir());
            }
            logE("ec dir = %s", App.me.getExternalCacheDir());
            logE("fd dir = %s", App.me.getFilesDir());

            clearAppCache();

            logE("after delete");
            App.me.statfs(root);
        });
    }

    private boolean next;
    private final int ASD = 123456;
    private void clearAppCache() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Activity a = this;
            Intent it;
            if (next) {
                it = new Intent(StorageManager.ACTION_CLEAR_APP_CACHE);
            } else {
                it = new Intent(StorageManager.ACTION_MANAGE_STORAGE);
            }
            next = !next;
            a.startActivityForResult(it, ASD);
            logE("clearAppCache %s", it);
        }
    }

    private void caches() {
        logE("caches");
        // really is visible apps in devices
        PackageManager pm = getPackageManager();
        Intent mi = new Intent(Intent.ACTION_MAIN);
        mi.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> li = pm.queryIntentActivities(mi, 0);
        File root = Environment.getExternalStorageDirectory();
        logE("---");
        clock.tic();
        // getCacheDir
//        2022-08-13 21:05:00.517 E/MainActivity: dir = /data/user/0/com.flyingkite.myfiles.debug/cache
        // getCodeCacheDir
//        2022-08-13 21:05:00.518 E/MainActivity: code dir = /data/user/0/com.flyingkite.myfiles.debug/code_cache
//        2022-08-13 21:05:00.903 E/MainActivity: ec dir = /storage/emulated/0/Android/data/com.flyingkite.myfiles.debug/cache
        for (int i = 0; i < li.size(); i++) {
            ResolveInfo ri = li.get(i);
            File[] cache = {
                    new File(root, "/Android/data/" + ri.activityInfo.packageName + "/cache"),
                    new File("/data/user/0/" + ri.activityInfo.packageName + "/cache"),
            };

            for (int j = 0; j < cache.length; j++) {
                File fi = cache[j];
                logE("exist = %s, for %s", fi.exists(), fi);
                clock.tic();
                if (fi.exists()) {
                    FileUtil.ensureDelete(fi);
                }
                clock.tac("Delete OK %s", fi);
            }
        }
        clock.tac("All cache deleted");
        showToast("All cache deleted");
    }

    private void seeMyAppAllFilesAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            boolean isMgr = Environment.isExternalStorageManager();
            //if (!isMgr) {
                Uri uri = App.getPackageUri();
                Intent it = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri);
                startActivityForResult(it, 7878);
                logE("it = %s", it);
            //}
        }
    }

    private void seeListOfAppAllFilesAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            boolean isMgr = Environment.isExternalStorageManager();
            Intent it = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
            startActivity(it);
            logE("it = %s", it);
        }
    }

    private void a(File file) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            StorageManager mgr = (StorageManager) getSystemService(Context.STORAGE_SERVICE);
            StorageStatsManager smgr = (StorageStatsManager) getSystemService(Context.STORAGE_STATS_SERVICE);
            try {
                UUID id = mgr.getUuidForPath(file);
                long total = smgr.getTotalBytes(id);
                long free = smgr.getFreeBytes(id);
                logE("uuid = %s", id);
                logE("total = %s, free = %s", FileUtil.toGbMbKbB(total), FileUtil.toGbMbKbB(free));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            //am.appNotResponding("Hi, ANR");
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            List<ActivityManager.AppTask> li = am.getAppTasks();
            logE("AppTask");
            ln(li);
        }
        List<ActivityManager.RunningAppProcessInfo> ai = am.getRunningAppProcesses();
        logE("RunningAppProcessInfo");
        ln(ai);
    }

    private void listUsage() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return;

        UsageStatsManager umgr = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        long time = System.currentTimeMillis();
        List<UsageStats> stats = umgr.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000 * 10, time);
        logE("stats = %s", stats);
        if (stats == null || stats.isEmpty()) {
            // Usage access is not enabled
            Uri uri = App.getPackageUri();
            Intent it = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS); // get list android 9 ok
            //Intent it = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, uri); // see my page, android crash by activity not found
            startActivity(it);
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
    }

    private void listApps() {
        PackageManager pm = getPackageManager();
        if (pm != null) {
            //pm.getApplicationIcon("name");
            List<ApplicationInfo> li = pm.getInstalledApplications(PackageManager.GET_META_DATA);
            logE("%s ApplicationInfo", li.size());
            for (int i = 0; i < li.size(); i++) {
                ApplicationInfo ai = li.get(i);
                logE("#%s : %s, %s, enable = %s, targetSdk = %s", i, ai.className, ai.dataDir, ai.enabled, ai.targetSdkVersion);
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
        // really is visible apps in devices
        PackageManager pm = getPackageManager();
        Intent mi = new Intent(Intent.ACTION_MAIN);
        mi.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> li = pm.queryIntentActivities(mi, 0);
        logE("ResolveInfo");
        ln(li);
    }
    private <T> void ln(List<T> li) {
        if (li != null) {
            logE("%s items", li.size());
            for (int i = 0; i < li.size(); i++) {
                T it = li.get(i);
                logE("#%s : %s", i, it);
            }
        } else {
            logE("null list");
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

    // https://developer.android.com/reference/android/os/storage/StorageManager#ACTION_MANAGE_STORAGE
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
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return;

        requestPermissions(neededPermissions(), myFileReq);
    }
}