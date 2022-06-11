package com.flyingkite.myfiles;

import android.content.Context;
import androidx.multidex.MultiDex;
import androidx.multidex.MultiDexApplication;

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
//        strictMode();
//        FirebaseApp.initializeApp(this);
//        CrashReport.init(this, DEBUG);
//        RemoteConfig.init(R.xml.remote_config_default);
//        FabricAnswers.logAppOnCreate();
//        TosWiki.init(this);
        //initCrashHandler();
    }
}
