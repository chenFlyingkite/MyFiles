package com.flyingkite.myfiles;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.os.StrictMode;
import androidx.core.content.FileProvider;
import androidx.multidex.MultiDex;
import androidx.multidex.MultiDexApplication;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import flyingkite.library.android.data.StatFsData;
import flyingkite.library.android.log.Loggable;

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
        listStorage();
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

    public List<File> listStorage() {
        List<File> ans = new ArrayList<>();
        File emulated = Environment.getExternalStorageDirectory();
        // emulated/storage/0 as first one
        ans.add(emulated);

        // Other SD cards
        File parent = new File("/storage/");
        File[] fs = parent.listFiles();
        if (fs != null) {
            for (int i = 0; i < fs.length; i++) {
                File f = fs[i];//new File(parent, s);
                String path = f.getAbsolutePath();
                logE("#%s : %s", i, f);
                if (emulated.getAbsolutePath().startsWith(path)) {
                    // omit
                } else if (f.isDirectory() && f.canRead()) {
                    ans.add(f);
                }
                StatFsData data = new StatFsData(path);
                logE("#%s : %s", i, data);
            }
        }
        return ans;
    }
}
