package com.flyingkite.myfiles.activity;

import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.flyingkite.myfiles.R;
import com.flyingkite.myfiles.library.FileFragment;

import java.util.ArrayList;

import flyingkite.library.android.util.BackPage;

public class FolderActivity extends BaseActivity implements FileFragment.OnFileActions {
    public static final String EXTRA_PATH = "path";
    public static final String EXTRA_SOURCES = "sources";
    private String parentPath = "";
    private ArrayList<String> sourcePaths;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_folder);
        parseArg();
        replaceFileFragment();
    }

    private void parseArg() {
        Intent it = getIntent();
        if (it == null) return;
        String sn = it.getExtras().getString(EXTRA_PATH, parentPath);
        String sp = it.getStringExtra(EXTRA_PATH);
        sourcePaths = it.getStringArrayListExtra(EXTRA_SOURCES);
        logE("sn = %s", sn);
        logE("sp = %s", sp);
        logE("sourcePath = %s", sourcePaths);
    }

    @Override
    public void onBackPressed() {
        Fragment ff = findFragmentById(R.id.folderFragment);
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
        f.setArguments(getIntent().getExtras());
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction fx = fm.beginTransaction();
        fx.replace(R.id.folderFragment, f, FileFragment.TAG);
        fx.commitAllowingStateLoss();
        //fm.executePendingTransactions();
    }

    @Override
    public boolean onActionPerformed(int action) {
        switch (action) {
            case FileFragment.ACTION_MOVE:
                finish();
                return true;
            case FileFragment.ACTION_COPY:
                finish();
                return true;
        }
        return false;
    }
}
