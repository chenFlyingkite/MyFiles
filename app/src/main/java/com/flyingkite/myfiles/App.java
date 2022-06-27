package com.flyingkite.myfiles;

import android.content.Context;
import android.net.Uri;
import android.os.StrictMode;
import androidx.core.content.FileProvider;
import androidx.multidex.MultiDex;
import androidx.multidex.MultiDexApplication;

import java.io.File;

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

    public static String getFileProviderAuthority(Context c) {
        return c.getPackageName() + ".fileprovider";
    }

    public static Uri getUriForFile(File file) {
        Context c = App.me;
        Uri uri = FileProvider.getUriForFile(c, getFileProviderAuthority(c), file);
        return uri;
    }
}
