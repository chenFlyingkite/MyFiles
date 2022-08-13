package com.flyingkite.myfiles;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.os.StrictMode;
import android.os.storage.StorageManager;
import androidx.core.content.FileProvider;
import androidx.multidex.MultiDex;
import androidx.multidex.MultiDexApplication;

import java.io.File;

import flyingkite.library.android.log.Loggable;
import flyingkite.library.java.util.FileUtil;

public class App extends MultiDexApplication implements Loggable {
    public static App me;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
        me = this;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        strictMode();
//        FirebaseApp.initializeApp(this);
//        CrashReport.init(this, DEBUG);
//        RemoteConfig.init(R.xml.remote_config_default);
//        FabricAnswers.logAppOnCreate();
//        TosWiki.init(this);
        //initCrashHandler();
        File[] fs = {Environment.getExternalStorageDirectory(),
                Environment.getDataDirectory(),
                new File("/"),
        };
        for (int i = 0; i < fs.length; i++) {
            File f = fs[i];
            logE("#%s : %s", i, f);
            statfs(f);
        }

    }

    private void strictMode() {
        if (BuildConfig.DEBUG) {
            // http://developer.android.com/intl/zh-tw/training/articles/perf-anr.html
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                            .detectAll()
//                    .detectNetwork()
//                    .detectCustomSlowCalls()
                    .penaltyLog()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                            .detectAll()
//                    .detectActivityLeaks()
//                    .detectFileUriExposure()
//                    .detectLeakedRegistrationObjects()
                    .penaltyLog()
                    .build());
        }
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        logE("onTrimMemory(%s)", level);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        logE("onLowMemory");
    }

    // emulator
    @Override
    public void onTerminate() {
        super.onTerminate();
        logE("onTerminate");
    }

    public static String getFileProviderAuthority(Context c) {
        return c.getPackageName() + ".fileprovider";
    }

    public static Uri getPackageUri() {
        return Uri.parse("package:" + BuildConfig.APPLICATION_ID);
    }

    public static Uri getUriForFile(File file) {
        Context c = App.me;
        Uri uri = FileProvider.getUriForFile(c, getFileProviderAuthority(c), file);
        return uri;
    }

    // Test cases
    // Navigate folders, also uses back icon and system back
    // DFS files, perform sort file
    // Install APK
    // Copy paste at same folder
    // ------------- new created folder

    public void statfs(File f) {
        StatFs stat = new StatFs(f.getPath());
        long bytesAvailable;
        long total;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            bytesAvailable = stat.getAvailableBytes();
            total = stat.getTotalBytes();
            String all = FileUtil.toGbMbKbB(total);
            String ok = FileUtil.toGbMbKbB(bytesAvailable);
            logE("all = %s, ok = %s", all, ok);
            bytesAvailable = stat.getAvailableBlocksLong() * stat.getBlockSizeLong();
            total = stat.getBlockCountLong() * stat.getBlockSizeLong();
        } else {
            bytesAvailable = (long)stat.getBlockSize() * stat.getAvailableBlocks();
            total = (long)stat.getBlockCount() * stat.getBlockSize();
        }

        String all = FileUtil.toGbMbKbB(total);
        String ok = FileUtil.toGbMbKbB(bytesAvailable);
        logE("all = %s, ok = %s", all, ok);
        StorageManager mgr = (StorageManager) getSystemService(Context.STORAGE_SERVICE);
    }
}
