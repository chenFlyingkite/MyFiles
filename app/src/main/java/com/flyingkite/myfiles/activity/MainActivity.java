package com.flyingkite.myfiles.activity;

import android.Manifest;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.flyingkite.myfiles.App;
import com.flyingkite.myfiles.Lab;
import com.flyingkite.myfiles.R;
import com.flyingkite.myfiles.library.FileFragment;
import com.flyingkite.myfiles.util.ViewHelper2;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import flyingkite.library.android.data.StatFsData;
import flyingkite.library.android.util.BackPage;
import flyingkite.library.androidx.TicTac2;
import flyingkite.library.java.util.FileUtil;

public class MainActivity extends BaseActivity implements ViewHelper2, FileFragment.OnFileActions {

    private TicTac2 clock = new TicTac2();
    private ViewGroup mainFrag;
    private View externalFrag;
    private View sdcard1Frag;
    private View storageInfo;
    private TextView externalInfo;
    private TextView sdcard1Info;
    private final int[] fragIDs = {R.id.externalFrag, R.id.sdcard1Frag};
    private final int[] infoIDs = {R.id.storageInfoExternal, R.id.storageInfoSDCard1};
    private int storageNowID;
    private View back;
    private View usages;
    private Lab lab = new Lab(this);

    private ViewGroup topStorage;
    private View topExternal;
    private View topSDCard01;
    //-- Data
    private List<StatFsData> storages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();

        reqStorage();
        topExternal.callOnClick();
    }

    private void init() {
        storages = App.me.listStorageStatFs();
        initTop();
        mainFrag = findViewById(R.id.mainFragments);
        sdcard1Frag = mainFrag.findViewById(R.id.sdcard1Frag);
        externalFrag = mainFrag.findViewById(R.id.externalFrag);
        back = findViewById(R.id.backBtn);
        back.setOnClickListener((v) -> {
            onBackPressed();
        });
        //initLab();
        replaceFileFragments();
    }

    private void initTop() {
        topStorage = findViewById(R.id.storage);
        topSDCard01 = findViewById(R.id.storageSDCard1);
        topExternal = findViewById(R.id.storageExternal);
        storageInfo = findViewById(R.id.storageInfo);
        sdcard1Info = findViewById(R.id.storageInfoSDCard1);
        externalInfo = findViewById(R.id.storageInfoExternal);
        topExternal.setVisibility(View.VISIBLE);
        topSDCard01.setVisibility(storages.size() > 1 ? View.VISIBLE : View.GONE);
        externalInfo.setVisibility(topExternal.getVisibility());
        sdcard1Info.setVisibility(topSDCard01.getVisibility());
        topExternal.setOnClickListener((v) -> {
            selectStorage(v);
            storageNowID = R.id.externalFrag;
            externalFrag.bringToFront();
        });
        topSDCard01.setOnClickListener((v) -> {
            selectStorage(v);
            storageNowID = R.id.sdcard1Frag;
            sdcard1Frag.bringToFront();
        });
    }

    @Override
    public boolean onUIAction(int action) {
        if (action == FileFragment.ACTION_UI_SHOW) {
            storageInfo.setVisibility(View.VISIBLE);
            return true;
        } else if (action == FileFragment.ACTION_UI_HIDE) {
            storageInfo.setVisibility(View.GONE);
            return true;
        }
        return false;
    }

    private void replaceFileFragments() {
        for (int i = 0; i < storages.size(); i++) {
            StatFsData data = storages.get(i);
            String path = data.path;
            replaceFileFragment(path, fragIDs[i], path);

            //--
            String ok = FileUtil.toGbMbKbB(data.available);
            String all = FileUtil.toGbMbKbB(data.totalSize);
            TextView t = findViewById(infoIDs[i]);
            t.setText(getString(R.string.storageState, all, ok));
            logE("#%s : path = %s", i, path);
        }
    }

    private void selectStorage(View v) {
        setSingleSelectChildren(topStorage, v);
    }

    private void initLab() {
        final File mainDir = Environment.getExternalStorageDirectory();
        final StatFsData stat = new StatFsData(mainDir.getAbsolutePath());
        usages = findViewById(R.id.usageStats);
        usages.setOnClickListener((v) -> {
            lab.testUsage();
        });

        findViewById(R.id.spaceIntent).setOnClickListener((v) -> {
            lab.test(mainDir); // total = 119.21 GB, free = 91.76 GB
        });

        findViewById(R.id.myStorage).setOnClickListener((v) -> {
            lab.seeMyAppAllFilesAccess();
        });
        findViewById(R.id.allOwners).setOnClickListener((v) -> {
            lab.seeListOfAppAllFilesAccess();
        });

        findViewById(R.id.clearCache).setOnClickListener((v) -> {
            String msg;
            logE("before delete %s", stat);
            showToast("before delete " + stat);

            lab.caches();
            stat.restat();

            logE("after delete %s", stat);
            showToast("after delete " + stat);
        });

        findViewById(R.id.clearCacheIntent).setOnClickListener((v) -> {
            File root = Environment.getExternalStorageDirectory();
            logE("before delete %s", stat);

            lab.clearAppCache();
            stat.restat();

            logE("after delete %s", stat);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPermissionState();
    }

    @Override
    public void onBackPressed() {
        if (onBackPressedFragment()) {
            return;
        }
        super.onBackPressed();
    }

    private boolean onBackPressedFragment() {
        int id = storageNowID;

        Fragment ff = findFragmentById(id);
        if (ff instanceof BackPage) {
            BackPage b = (BackPage) ff;
            if (b.onBackPressed()) {
                return true;
            }
        }
        return false;
    }

    private void replaceFileFragment(String path, int viewID, String tag) {
        FileFragment f = new FileFragment();
        if (!TextUtils.isEmpty(path)) {
            Bundle b = new Bundle();
            b.putString(FileFragment.EXTRA_PATH, path);
            b.putString(FileFragment.EXTRA_ROOT, path);
            f.setArguments(b);
        }
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction fx = fm.beginTransaction();
        fx.replace(viewID, f, tag);
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