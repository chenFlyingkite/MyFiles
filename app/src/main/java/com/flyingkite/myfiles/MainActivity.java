package com.flyingkite.myfiles;

import android.Manifest;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import flyingkite.library.android.log.Loggable;
import flyingkite.library.androidx.TicTac2;
import flyingkite.library.androidx.recyclerview.Library;
import flyingkite.library.androidx.recyclerview.RVAdapter;
import flyingkite.library.androidx.recyclerview.RVSelectAdapter;
import flyingkite.library.java.util.FileUtil;

public class MainActivity extends BaseActivity {

    private Library<TRA> diskLib;
    private TicTac2 clock = new TicTac2();
    private File parent;


    private TextView parentFolder;
    private String state;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initDisk();
        reqStorage();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPermissionState();
        File root = Environment.getExternalStorageDirectory();
        fileList(root);
    }

    @Override
    public void onBackPressed() {
        File root = Environment.getExternalStorageDirectory();
        boolean isRoot = root.getAbsolutePath().equals(parent.getAbsolutePath());
        isRoot = false;
        if (!isRoot) {
            fileList(parent.getParentFile());
            return;
        }
        super.onBackPressed();
    }

    private void initDisk() {
        diskLib = new Library<>(findViewById(R.id.recyclerDisk), true);
        List<File> ans = new ArrayList<>();
        TRA ta = new TRA();
        ta.setItemListener(new TRA.ItemListener() {
            @Override
            public void onClick(File item, TRA.VH holder, int position) {
                logE("Disk #%s, %s", position, item);
                fileList(item);
            }
        });
        ta.setDataList(ans);
        diskLib.setViewAdapter(ta);
        parentFolder = findViewById(R.id.parentFolder);
    }


    // Android 11 cannot list file in emulated/storage/0 ?
    // Mis-list the file of emulated/storage/0/a.txt
    // Access : Allowed to manage all files,
    // allowed to access media only, not allowed
    private void fileList(File f) {
        checkPermissionState();
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
            String[] a = f.list();
            //sort(a);
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

    private void updateFile() {
        parentFolder.setText(state);
    }

    private static final int myFileReq = 123;

    @Override
    protected String[] neededPermissions() {
        //String[] permissions = {Manifest.permission_group.STORAGE};
        String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
        //String[] permissions = {Manifest.permission.MANAGE_EXTERNAL_STORAGE};
        //String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.MANAGE_EXTERNAL_STORAGE};
        if (Build.VERSION.SDK_INT >= 30) {
            // R = 30
            permissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.MANAGE_EXTERNAL_STORAGE};
        }
        return permissions;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case myFileReq:
                log("Request permissions = " + Arrays.toString(permissions));
                log("and returns results = " + Arrays.toString(grantResults));
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        logE("onActivityResult(%s, %s, %s)", requestCode, resultCode, data);
    }

    private void reqStorage() {
        if (Build.VERSION.SDK_INT >= 23) {
            //M = 23
            requestPermissions(neededPermissions(), myFileReq);
        }
    }

    private static class TRA extends RVAdapter<File, TRA.VH, TRA.ItemListener> implements Loggable {

        private interface ItemListener extends RVAdapter.ItemListener<File, TRA.VH> {

        }

        private TicTac2 tt = new TicTac2();

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(inflateView(parent, R.layout.view_text));
        }

        private Set<Long> free = new HashSet<>();
        private Set<Long> total = new HashSet<>();
        private Set<Long> used = new HashSet<>();

        @Override
        public void onBindViewHolder(VH vh, int position) {
            super.onBindViewHolder(vh, position);
            tt.tic();
            File f = itemOf(position);
            long fr = f.getFreeSpace();
            free.add(fr);
            long to = f.getTotalSpace();
            total.add(to);
            long us = f.getUsableSpace();
            used.add(us);
            // hint, not guarantee
            logE("free = %s, total = %s, used = %s", free, total, used);
            String sp = String.format("F %s\nT %s\nU %s", fr, to, us);
            String s = sp + "\n";
            s = "";
            long leng = f.length();
            String len = FileUtil.toGbMbKbB(leng);
            if (f.isDirectory()) {
                int sub = -1;
                String[] fl = f.list();
                if (fl != null) {
                    sub = fl.length;
                }
                s += String.format("%s : %s (%s items) %s", position, f.getName(), sub, len);
            } else {
                s += String.format("%s : %s %s", position, f.getName(), len);
            }
            int tc = Color.BLACK;
            if (f.isFile()) {
                tc = Color.BLUE;
            }
            tt.tac("#onBind %s : %s", position, f);
            vh.msg.setText(s);
            vh.msg.setTextColor(tc);
        }

        private static class VH extends RecyclerView.ViewHolder {
            private TextView msg;
            public VH(@NonNull View v) {
                super(v);
                msg = v.findViewById(R.id.itemText);
            }
        }
    }

    private static class RVA extends RVSelectAdapter<String, RVA.RVAH, RVA.ItemListener> implements Loggable {

        public boolean sel = false;

        @NonNull
        @Override
        public RVAH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new RVAH(inflateView(parent, R.layout.view_text));
        }

        @Override
        public boolean hasSelection() {
            return sel;
        }

        @Override
        public void onBindViewHolder(RVAH holder, int position) {
            super.onBindViewHolder(holder, position);
            holder.text.setText(itemOf(position));
        }

        public interface ItemListener extends RVSelectAdapter.ItemListener<String, RVAH> {

        }



        public class RVAH extends RecyclerView.ViewHolder {

            private TextView text;

            public RVAH(View v) {
                super(v);
                text = v.findViewById(R.id.itemText);
            }
        }
    }
}