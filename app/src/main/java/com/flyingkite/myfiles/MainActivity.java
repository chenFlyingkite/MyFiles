package com.flyingkite.myfiles;

import android.Manifest;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;

import com.flyingkite.myfiles.library.FileFragment;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import flyingkite.library.android.log.Loggable;
import flyingkite.library.android.util.BackPage;
import flyingkite.library.androidx.TicTac2;
import flyingkite.library.androidx.recyclerview.RVAdapter;
import flyingkite.library.androidx.recyclerview.RVSelectAdapter;
import flyingkite.library.java.util.FileUtil;

public class MainActivity extends BaseActivity {

    private TicTac2 clock = new TicTac2();
    private FrameLayout frame;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        frame = findViewById(R.id.fileFragment);
        reqStorage();
        replaceFileFragment();
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

    private void replaceFileFragment() {
        FileFragment f = new FileFragment();
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction fx = fm.beginTransaction();
        fx.replace(R.id.fileFragment, f, FileFragment.TAG);
        fx.commitAllowingStateLoss();
        fm.executePendingTransactions();
    }

    private static final int myFileReq = 123;

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
        switch (requestCode) {
            case myFileReq:
                log("Request permissions = " + Arrays.toString(permissions));
                log("and returns results = " + Arrays.toString(grantResults));
                break;
        }
    }

    private void reqStorage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // M = 23
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