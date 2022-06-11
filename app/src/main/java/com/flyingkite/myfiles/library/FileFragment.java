package com.flyingkite.myfiles.library;

import android.graphics.Point;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.flyingkite.myfiles.FilePreference;
import com.flyingkite.myfiles.R;
import com.flyingkite.myfiles.media.ImagePlayer;
import com.flyingkite.myfiles.media.VideoPlayer;

import java.io.File;
import java.net.URLConnection;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import flyingkite.library.android.media.MediaMetadataRetrieverUtil;
import flyingkite.library.android.media.MimeTypeMapUtil;
import flyingkite.library.android.util.BackPage;
import flyingkite.library.androidx.TicTac2;
import flyingkite.library.androidx.recyclerview.CenterScroller;
import flyingkite.library.androidx.recyclerview.Library;
import flyingkite.library.java.util.FileUtil;

public class FileFragment extends BaseFragment {
    public static final String TAG = "FileFragment";

    private Library<FileAdapter> diskLib;
    private CenterScroller scroller = new CenterScroller() {
        @Override
        public RecyclerView getRecyclerView() {
            if (diskLib != null) {
                return diskLib.recyclerView;
            }
            return null;
        }
    };
    private TicTac2 clock = new TicTac2();
    private File parent;
    private FrameLayout frameImage;


    private TextView parentFolder;
    private View reload;
    private View dfsFile;
    private TextView sortBtn;

    private String state;
    private FilePreference filePref = new FilePreference();

    private boolean unstableScroll = false;
    private Deque<Point> savedPos = new ArrayDeque<>();

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        initDisk();
        frameImage = findViewById(R.id.frameImage);

        parent = Environment.getExternalStorageDirectory();
        reload.callOnClick();
//        File root = Environment.getExternalStorageDirectory();
//        fileList(root);
    }

    private Point makePoint() {
        Point ans = new Point();
        RecyclerView.LayoutManager lm = diskLib.recyclerView.getLayoutManager();
        View v = null;
        if (lm != null) {
            v = lm.getChildAt(0);
        }
        RecyclerView.ViewHolder vh = null;
        if (v != null) {
            vh = diskLib.recyclerView.getChildViewHolder(v);
        }
        if (vh != null) {
            ans.x = vh.getAdapterPosition();
            ans.y = v.getTop();
        }
        logE("makePoint x = %s, v = %s\n, vh = %s\n", ans.x, v, vh);
        return ans;
    }

    private void savedPosPush(Point p) {
        savedPos.push(p);
    }

    private Point savedPosPop() {
        if (savedPos.isEmpty()) {
            return new Point();
        }
        return savedPos.pop();
    }

    private void initDisk() {
        reload = findViewById(R.id.reload);
        reload.setOnClickListener((v) -> {
            updateSortState();
            fileList(parent);
        });
        dfsFile = findViewById(R.id.dfsSize);
        dfsFile.setOnClickListener((v) -> {
            Map<File, Long> spaces = getFileSizes(parent);
            diskLib.adapter.setSpaces(spaces);
            diskLib.adapter.notifyDataSetChanged();
        });
        sortBtn = findViewById(R.id.sortBtn);

        sortBtn.setOnClickListener((v) -> {
            filePref.fileListSort.next();
            reload.callOnClick();
        });

        parentFolder = findViewById(R.id.parentFolder);

        diskLib = new Library<>(findViewById(R.id.recyclerDisk), true);

        List<File> ans = new ArrayList<>();
        FileAdapter ta = new FileAdapter();
        ta.setItemListener(new FileAdapter.ItemListener() {
            @Override
            public void onClick(File item, FileAdapter.FileVH holder, int position) {
                String path = item.getAbsolutePath();
                logE("Disk #%s, %s", position, item);
                savedPosPush(makePoint());
                fileList(item);
                if (item.isFile()) {
                    // not support for image
                    long duration = MediaMetadataRetrieverUtil.extractMetadataFromFilepath(item.getAbsolutePath(), MediaMetadataRetriever.METADATA_KEY_DURATION, -1L);
                    logE("Duration = %s", duration);

                    boolean isV = MimeTypeMapUtil.getMimeTypeFromExtension("video/", path);
                    boolean isI = MimeTypeMapUtil.getMimeTypeFromExtension("image/", path);
                    boolean isA = MimeTypeMapUtil.getMimeTypeFromExtension("audio/", path);
                    logE("v, i, a = %s, %s, %s", isV, isI, isA);
                    if (isI) {
                        openImage(item);
                    } else if (isV) {
                        openVideo(item);
                    } else if (isA) {
                        openVideo(item);
                    } else {
                        openVideo(item);
                    }
//                    openImage(item);
//                    openVideo(item);
                }
            }
        });
        ta.setDataList(ans);
        diskLib.setViewAdapter(ta);
    }

    private void updateSortState() {
        int v = filePref.fileListSort.get();
        String text = getString(FilePreference.fileListSortBy[v]);
        sortBtn.setText("Sort = " + text);
    }

    private void openImage(File item) {
        ImagePlayer ip = new ImagePlayer();
        Bundle b = new Bundle();
        b.putString(ImagePlayer.BUNDLE_PATH, item.getAbsolutePath());
        ip.setArguments(b);

        childFragmentManager_Replace(R.id.frameImage, ip, ImagePlayer.TAG);
    }

    private void openVideo(File item) {
        VideoPlayer vp = new VideoPlayer();
        Bundle b = new Bundle();
        b.putString(ImagePlayer.BUNDLE_PATH, item.getAbsolutePath());
        vp.setArguments(b);

        childFragmentManager_Replace(R.id.frameImage, vp, ImagePlayer.TAG);
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

        List<File> all = new ArrayList<>();
        long ms = -1;
        int dn = -1;
        int fn = -1;
        int n = -1;
        if (f != null) {
            clock.tic();
            clock.tic();
            File[] a = f.listFiles();
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
                    //File fi = new File(f, a[i]);
                    File fi = a[i];
                    String k = fi.getAbsolutePath();
                    String mime = URLConnection.getFileNameMap().getContentTypeFor(k);
                    logE("#%s : %s %s", i, mime, fi);
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

    //
    // Android\data\jp.naver.line.android\storage\toyboximg\com.linecorp.advertise
    private void sort(File[] a) {
        if (a != null) {
            // Also take the spaces map into consideration for folder size
            Comparator<File> cmp = FilePreference.getComparatorFileList(diskLib.adapter.getSpaces());
            Arrays.sort(a, cmp);
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
                Point pp = savedPosPop();
                if (unstableScroll) {
                    logE("makePoint pop = %s", pp);
                    diskLib.recyclerView.postDelayed(() -> {
                        scroller.scrollToLeft(pp.x);
                        //scroller.scrollToPercent(pp.x, 0, 30, false);
                    }, 3000);
                }
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
