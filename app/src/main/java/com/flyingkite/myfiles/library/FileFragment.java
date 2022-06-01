package com.flyingkite.myfiles.library;

import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.flyingkite.myfiles.R;
import com.flyingkite.myfiles.media.ImagePlayer;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import flyingkite.library.android.util.BackPage;
import flyingkite.library.androidx.TicTac2;
import flyingkite.library.androidx.recyclerview.Library;
import flyingkite.library.java.util.FileUtil;

public class FileFragment extends BaseFragment {
    public static final String TAG = "FileFragment";

    private Library<FileAdapter> diskLib;
    private TicTac2 clock = new TicTac2();
    private File parent;
    private FrameLayout frameImage;


    private TextView parentFolder;
    private View dfsFile;

    private String state;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        initDisk();
        frameImage = findViewById(R.id.frameImage);

        File root = Environment.getExternalStorageDirectory();
        fileList(root);
    }

    private void initDisk() {
        dfsFile = findViewById(R.id.dfsSize);
        dfsFile.setOnClickListener((v) -> {
            Map<File, Long> spaces = getFileSizes(parent);
            diskLib.adapter.setSpaces(spaces);
            diskLib.adapter.notifyDataSetChanged();
        });
        parentFolder = findViewById(R.id.parentFolder);

        diskLib = new Library<>(findViewById(R.id.recyclerDisk), true);
        List<File> ans = new ArrayList<>();
        FileAdapter ta = new FileAdapter();
        ta.setItemListener(new FileAdapter.ItemListener() {
            @Override
            public void onClick(File item, FileAdapter.FileVH holder, int position) {
                logE("Disk #%s, %s", position, item);
                fileList(item);
                if (item.isFile()) {
                    openImage(item);
                }
            }
        });
        ta.setDataList(ans);
        diskLib.setViewAdapter(ta);

    }

    private void openImage(File item) {
        ImagePlayer ip = new ImagePlayer();
        Bundle b = new Bundle();
        b.putString(ImagePlayer.BUNDLE_PATH, item.getAbsolutePath());
        ip.setArguments(b);

        childFragmentManager_Replace(R.id.frameImage, ip, ImagePlayer.TAG);
//        FragmentManager fm = getChildFragmentManager();
//        FragmentTransaction tx = fm.beginTransaction();
//        tx.replace(R.id.frameImage, ip, ImagePlayer.TAG);
//        tx.commitAllowingStateLoss();
        //fm.executePendingTransactions();
    }


    @Override
    public void onResume() {
        super.onResume();
    }

    // Android 11 cannot list file in emulated/storage/0 ?
    // Mis-list the file of emulated/storage/0/a.txt
    // Access : Allowed to manage all files,
    // allowed to access media only, not allowed
    private void fileList(File f) {
        logE("fileList = %s", f);
        parent = f;
        updateFile();

        List<File> all = new ArrayList<>();
        long ms = -1;
        int dn = -1;
        int fn = -1;
        int n = -1;
        if (f != null) {
            clock.tic();
            clock.tic();
            String[] a = f.list();
            clock.tac("File listed %s", f);
            clock.tic();
            sort(a);
            clock.tac("File listed sorting %s", f);
            ms = clock.tac("File listed %s", f);
            if (a != null) {
                fn = dn = 0;
                n = a.length;
                logE("%s items", a.length);
                for (int i = 0; i < a.length; i++) {
                    File fi = new File(f, a[i]);
                    String k = fi.getAbsolutePath();
                    logE("#%s : %s", i, fi);
                    all.add(fi);
                    if (fi.isFile()) {
                        fn++;
                    } else {
                        dn++;
                    }
                }
            }
        }
        state = String.format("%sms %s items = %s D + %s F for %s", ms, n, dn, fn, f);
        diskLib.adapter.setDataList(all);
        diskLib.adapter.notifyDataSetChanged();
        updateFile();
    }

    private void sort(String[] a) {
        if (a != null) {
            Arrays.sort(a);
        }
    }

    @Override
    public boolean onBackPressed() {
        Fragment f = findFragmentById_FromChildFM(R.id.frameImage);
        logE("f = %s", f);
        if (f instanceof BackPage) {
            BackPage b = (BackPage) f;
            if (b.onBackPressed()) {
                return true;
            }
        }
        logE("parent = %s", parent);

        if (parent != null) {
            File p = parent.getParentFile();
            if (p != null) {
                fileList(p);
                return true;
            }
        }
        return false;
    }

    // TODO into MyAndroid
    private Map<File, Long> getFileSizes(File root) {
        Map<File, Long> map = new HashMap<>();
        TicTac2 clk = new TicTac2();
        clk.tic();
        dfsFileSize(root, new OnDFSFile() {
            @Override
            public void onStart(File f) {
                clk.tic();
            }

            @Override
            public void onComplete(File f, long size) {
                clk.tac("File sized %s on %s", FileUtil.toGbMbKbB(size), f);
                if ("/storage/emulated/0/Android/data/jp.naver.line.android/storage".equals(f.getAbsolutePath())) {
                    logE("X_X %s, %s, %s", size, FileUtil.toGbMbKbB(size), diskLib.adapter.toGbMbKbB(size));
                }
                map.put(f, size);
            }
        });
        clk.tac("dfsFileSize %s", root);
        List<File> keys = new ArrayList<>(map.keySet());
        Collections.sort(keys);
        logE("%s keys", keys.size());
        for (int i = 0; i < keys.size(); i++) {
            File key = keys.get(i);
            long val = map.get(key);
            String s = FileUtil.toGbMbKbB(val);
            String par = "";
            if (key.equals(root) == false) {
                File pa = key.getParentFile();
                long pz = map.get(pa);
                double pr = 10000F * val / pz;
                par = _fmt("(%.1f%% %%) ", pr);
            }
            String x = _fmt("%11s %13s", s, par);
            logE("#%5s : %s = %s", i, x, key);
        }
        return map;
    }

    // TODO into MyAndroid
    private interface OnDFSFile {
        void onStart(File f);
        default File[] onFileListed(File root, File[] sub) { return sub; }
        default void onFileSize(File f, long size) { }
        void onComplete(File f, long size);
    }

    // TODO into MyAndroid
    private long dfsFileSize(File f, OnDFSFile lis) {
        long ans = 0;
        if (f == null) {
            return ans;
        }
        if (lis != null) {
            lis.onStart(f);
        }
        if (f.isDirectory()) {
            File[] sub = f.listFiles();
            // lis
            if (lis != null) {
                sub = lis.onFileListed(f, sub);
            }
            // core
            if (sub != null) {
                for (int i = 0; i < sub.length; i++) {
                    File g = sub[i];
                    long it = dfsFileSize(g, lis);
                    ans += it;
                }
            }
            // lis
            if (lis != null) {
                lis.onFileSize(f, ans);
            }
        } else {
            // lis
            if (lis != null) {
                lis.onFileListed(f, null);
            }
            // core
            ans = f.length();
            // lis
            if (lis != null) {
                lis.onFileSize(f, ans);
            }
        }

        // notify its state
        if (lis != null) {
            lis.onComplete(f, ans);
        }
        return ans;
    }

    private void updateFile() {
        parentFolder.setText(state);
    }


    @Override
    protected int getPageLayoutId() {
        return R.layout.fragment_file;
    }
}
