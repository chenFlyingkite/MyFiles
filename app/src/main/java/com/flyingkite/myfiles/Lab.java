package com.flyingkite.myfiles;

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
import android.os.Environment;
import android.os.storage.StorageManager;
import android.provider.Settings;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import flyingkite.library.android.log.Loggable;
import flyingkite.library.android.util.ToastUtil;
import flyingkite.library.androidx.TicTac2;
import flyingkite.library.java.util.FileUtil;

public class Lab implements ToastUtil, Loggable {

    private Activity activity;

    public Lab(Activity ctx) {
        activity = ctx;
    }

    @Override
    public Context getContext() {
        return activity;
    }

    public Activity getActivity() {
        return activity;
    }

    // Not delete success on others?
    public void caches() {
        TicTac2 clock = new TicTac2();
        logE("caches");
        // really is visible apps in devices
        PackageManager pm = getContext().getPackageManager();
        Intent mi = new Intent(Intent.ACTION_MAIN);
        mi.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> li = pm.queryIntentActivities(mi, 0);
        logE("---");
        clock.tic();
        List<File> caches;
        // Delete device cache
        caches = deviceCacheDirs();
        for (int i = 0; i < caches.size(); i++) {
            File f = caches.get(i);
            f.delete();
        }
        // Delete package cache
        caches = appCacheDirs(getContext());
        for (int i = 0; i < li.size(); i++) {
            ResolveInfo ri = li.get(i);
            String pkg = ri.activityInfo.packageName;
            String me = getContext().getPackageName();
            logE("for package name #%s: %s", i, pkg);
            for (int j = 0; j < caches.size(); j++) {
                File fi = caches.get(j);
                String si = fi.getAbsolutePath().replaceFirst(me, pkg);
                File dst = new File(si);
                //logE("dst %s for %s", dst.exists(), dst);
                if (dst.exists()) {
                    clock.tic();
                    //boolean ok = FileUtil.ensureDelete(dst);
                    boolean ok = dst.delete();
                    clock.tac("Delete OK = %s, %s", ok, dst);
                }
            }
        }
        clock.tac("All cache deleted");
        showToast("All cache deleted");
    }

    private boolean next;
    private final int ASD = 123456;
    public void clearAppCache() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Activity a = getActivity();
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

    public void test(File file) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            StorageManager mgr = (StorageManager) getActivity().getSystemService(Context.STORAGE_SERVICE);
            StorageStatsManager smgr = (StorageStatsManager) getActivity().getSystemService(Context.STORAGE_STATS_SERVICE);
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
        ActivityManager am = (ActivityManager) getActivity().getSystemService(Context.ACTIVITY_SERVICE);
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

    public <T> void ln(List<T> li) {
        int n = li == null ? -1 : li.size();
        logE("%s items in list", n);
        for (int i = 0; i < n; i++) {
            T it = li.get(i);
            logE("#%s : %s", i, it);
        }
    }

    public void seeMyAppAllFilesAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            boolean isMgr = Environment.isExternalStorageManager();
            //if (!isMgr) {
            Uri uri = App.getPackageUri();
            Intent it = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri);
            getActivity().startActivityForResult(it, 7878);
            logE("it = %s", it);
            //}
        }
    }

    public void seeListOfAppAllFilesAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            boolean isMgr = Environment.isExternalStorageManager();
            Intent it = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
            getActivity().startActivity(it);
            logE("it = %s", it);
        }
    }

    public void testUsage() {
        listUsage();
        listApps();
        listApps2();
    }

    private static final SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
    private void listUsage() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return;
        Activity a = getActivity();

        UsageStatsManager umgr = (UsageStatsManager) a.getSystemService(Context.USAGE_STATS_SERVICE);
        long time = System.currentTimeMillis();
        List<UsageStats> stats = umgr.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000 * 10, time);
        logE("stats = %s", stats);
        if (stats == null || stats.isEmpty()) {
            // Usage access is not enabled
            Uri uri = App.getPackageUri();
            Intent it = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS); // get list android 9 ok
            //Intent it = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, uri); // see my page, android crash by activity not found
            a.startActivity(it);
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
        Activity a = getActivity();
        PackageManager pm = a.getPackageManager();
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
        Activity a = getActivity();
        // really is visible apps in devices
        PackageManager pm = a.getPackageManager();
        Intent mi = new Intent(Intent.ACTION_MAIN);
        mi.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> li = pm.queryIntentActivities(mi, 0);
        logE("ResolveInfo");
        ln(li);
    }

    private List<File> deviceCacheDirs() {
        List<File> all = new ArrayList<>();
        all.add(Environment.getDownloadCacheDirectory());
        return all;
    }

    private List<File> appCacheDirs(Context c) {
        List<File> all = new ArrayList<>();
        if (c != null) {
            all.add(c.getCacheDir());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                all.add(c.getCodeCacheDir());
            }
            all.add(c.getExternalCacheDir());

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                File[] fs = c.getExternalCacheDirs();
                all.addAll(Arrays.asList(fs));
            }
        }
//        2022-08-17 22:43:49.620 E/MainActivity: f = /data/cache
//        2022-08-17 22:43:49.620 E/MainActivity: f = /data/user/0/com.flyingkite.myfiles.debug/cache
//        2022-08-17 22:43:49.620 E/MainActivity: f = /data/user/0/com.flyingkite.myfiles.debug/code_cache
//        2022-08-17 22:43:49.630 E/MainActivity: f = /storage/emulated/0/Android/data/com.flyingkite.myfiles.debug/cache
//        2022-08-17 22:43:49.631 E/MainActivity: fs = [/storage/emulated/0/Android/data/com.flyingkite.myfiles.debug/cache]
        return all;
    }
}
