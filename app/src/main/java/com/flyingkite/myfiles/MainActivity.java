package com.flyingkite.myfiles;

import android.Manifest;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.flyingkite.myfiles.library.FileFragment;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import flyingkite.library.android.data.StatFsData;
import flyingkite.library.android.util.BackPage;
import flyingkite.library.androidx.TicTac2;

public class MainActivity extends BaseActivity implements ViewHelper2 {

    private TicTac2 clock = new TicTac2();
    private FrameLayout frame;
    private View back;
    private View usages;
    private Lab lab = new Lab(this);

    private ViewGroup topStorage;
    private View topExternal;
    private View topSDCard01;
    //-- Data
    private List<File> storages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();

        reqStorage();
        topExternal.callOnClick();
    }

    private void init() {
        storages = App.me.listStorage();
        initTop();
        frame = findViewById(R.id.fileFragment);
        back = findViewById(R.id.backBtn);
        back.setOnClickListener((v) -> {
            onBackPressed();
        });
        //initLab();
    }

    private void initTop() {
        topStorage = findViewById(R.id.storage);
        topExternal = findViewById(R.id.storageExternal);
        topSDCard01 = findViewById(R.id.storageSDCard1);
        topExternal.setVisibility(View.VISIBLE);
        topSDCard01.setVisibility(storages.size() > 1 ? View.VISIBLE : View.GONE);
        topExternal.setOnClickListener((v) -> {
            int at = 0;
            selectStorage(v);
            replaceFileFragment(storages.get(at).getAbsolutePath());
        });
        topSDCard01.setOnClickListener((v) -> {
            int at = 1;
            selectStorage(v);
            replaceFileFragment(storages.get(at).getAbsolutePath());
        });
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
        Fragment ff = findFragmentById(R.id.fileFragment);
        if (ff instanceof BackPage) {
            BackPage b = (BackPage) ff;
            if (b.onBackPressed()) {
                return;
            }
        }
        super.onBackPressed();
    }

    private void replaceFileFragment(String path) {
        FileFragment f = new FileFragment();
        if (!TextUtils.isEmpty(path)) {
            Bundle b = new Bundle();
            b.putString(FileFragment.EXTRA_PATH, path);
            f.setArguments(b);
        }
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