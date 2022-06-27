package com.flyingkite.myfiles.library;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Point;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.PopupWindow;
import android.widget.RadioGroup;
import android.widget.TextView;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.flyingkite.myfiles.App;
import com.flyingkite.myfiles.FilePreference;
import com.flyingkite.myfiles.R;
import com.flyingkite.myfiles.ShareUtil;
import com.flyingkite.myfiles.media.ImagePlayer;
import com.flyingkite.myfiles.media.VideoPlayer;

import java.io.File;
import java.net.URLConnection;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Map;

import flyingkite.library.android.media.MediaMetadataRetrieverUtil;
import flyingkite.library.android.media.MimeTypeMapUtil;
import flyingkite.library.android.util.BackPage;
import flyingkite.library.android.util.DialogUtil;
import flyingkite.library.android.util.ThreadUtil;
import flyingkite.library.androidx.TicTac2;
import flyingkite.library.androidx.mediastore.listener.DataListener;
import flyingkite.library.androidx.mediastore.request.MediaRequest;
import flyingkite.library.androidx.mediastore.store.StoreFiles;
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

    private Pair<View, PopupWindow> sortMenu;
    private boolean unstableScroll = false;
    private Deque<Point> savedPos = new ArrayDeque<>();

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        initDisk();
        frameImage = findViewById(R.id.frameImage);

        parent = Environment.getExternalStorageDirectory();
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


    private void openWith(File file) {
        Activity a = getActivity();
        if (a == null) return;
        Uri uri = Uri.fromFile(file); // also ok?
        //Uri uri = App.getUriForFile(file);
        logE("openWith uri = %s", uri);
        String mime = MimeTypeMapUtil.getMimeTypeFromExtension(file.getAbsolutePath());
        ShareUtil.openUriIntent(a, uri, mime);
    }

    private void share(File file) {
        Activity a = getActivity();
        if (a == null) return;
        Uri uri = App.getUriForFile(file);
        logE("share uri = %s", uri);
        String mime = MimeTypeMapUtil.getMimeTypeFromExtension(file.getAbsolutePath());
        ShareUtil.sendUriIntent(a, uri, mime);
    }

    private void reloadMe() {
        reload.callOnClick();
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
            showSortMenu(v);
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

                    boolean isV = MimeTypeMapUtil.isPrefixOfWithMimeTypeFromExtension("video/", path);
                    boolean isI = MimeTypeMapUtil.isPrefixOfWithMimeTypeFromExtension("image/", path);
                    boolean isA = MimeTypeMapUtil.isPrefixOfWithMimeTypeFromExtension("audio/", path);
                    logE("v, i, a = %s, %s, %s", isV, isI, isA);
                    if (isI) {
                        openImage(item);
                    } else if (isV) {
                        openVideo(item);
                    } else if (isA) {
                        openVideo(item);
                    } else {
                        openImage(item);
                    }
//                    openImage(item);
//                    openVideo(item);
                }
            }

            @Override
            public boolean onLongClick(File item, FileAdapter.FileVH holder, int position) {
                share(item);
                return true;
            }

            @Override
            public void onAction(File item, FileAdapter.FileVH vh, int position) {
                // TODO
                logE("show drop down %s", item);
                Pair<View, PopupWindow> pair = createPopupWindow(R.layout.popup_file_menu, getView());
                initActions(pair.first, item);
                pair.second.showAsDropDown(vh.action);
                // For folders :
                // select
                // move to
                // copy to
                // rename
                // permanent delete
                // info

                // For files :
                // select
                // share
                // open with
                // move to
                // copy to
                // rename
                // add to favorite
                // trash
                // safe folder
                // permanent delete
                // info
            }
        });
        ta.setDataList(ans);
        diskLib.setViewAdapter(ta);
    }

    private void initActions(View w, File item) {
        // file name
        TextView txt = w.findViewById(R.id.itemTitle);
        txt.setText(item.toString());
        w.findViewById(R.id.itemShare).setOnClickListener((v) -> {
            share(item);
        });
        w.findViewById(R.id.itemOpenWith).setOnClickListener((v) -> {
            openWith(item);
        });
        w.findViewById(R.id.itemRename).setOnClickListener((v) -> {
            Activity a = getActivity();
            if (a == null) {
                return;
            }

            new DialogUtil.Alert(a, R.layout.dialog_rename, 0, new DialogUtil.InflateListener() {
                @Override
                public void onFinishInflate(View view, AlertDialog dialog) {
                    EditText edit = view.findViewById(R.id.itemInput);
                    edit.setText(item.getName());

                    view.findViewById(R.id.itemCancel).setOnClickListener((vv) -> {
                        dialog.dismiss();
                    });
                    view.findViewById(R.id.itemOK).setOnClickListener((vv) -> {
                        File next = new File(item.getParent(), edit.getText().toString());
                        boolean ok = item.renameTo(next);
                        logE("rename ok = %s", ok);
                        logE("old = %s\nnew = %s", item, next);
                        dialog.dismiss();
                        reloadMe();
                    });
                }
            }).buildAndShow();
        });
        w.findViewById(R.id.itemDelete).setOnClickListener((v) -> {
            Activity a = getActivity();
            if (a == null) {
                return;
            }

            new DialogUtil.Alert(a, R.layout.dialog_message, 0, new DialogUtil.InflateListener() {
                @Override
                public void onFinishInflate(View view, AlertDialog dialog) {
//                    init(view, dialog);
//                }
//
//                private void init(View view, AlertDialog dialog) {
                    TextView t;
                    t = view.findViewById(R.id.itemTitle);
                    t.setText(getString(R.string.delete_title, item.getName()));
                    t = view.findViewById(R.id.itemMessage);
                    t.setText(getString(R.string.delete_title_confirm, item.getAbsolutePath()));

                    view.findViewById(R.id.itemOK).setOnClickListener((v) -> {
                        boolean ok = FileUtil.ensureDelete(item);
                        logE("ok = %s, for %s", ok, item);
                        reloadMe();
                        dialog.dismiss();
                    });
                    view.findViewById(R.id.itemCancel).setOnClickListener((v) -> {
                        dialog.dismiss();
                    });
                }
            }).buildAndShow();
        });
        w.findViewById(R.id.itemFileInfo).setOnClickListener((v) -> {
            Activity a = getActivity();
            if (a == null) {
                return;
            }
            new DialogUtil.Alert(a, R.layout.dialog_message, 0, new DialogUtil.InflateListener() {
                @Override
                public void onFinishInflate(View view, AlertDialog dialog) {
                    TextView t = null;
                    t = view.findViewById(R.id.itemTitle);
                    t.setText(item.getAbsolutePath());
                    StoreFiles sf = new StoreFiles(a);
                    MediaRequest r = sf.newRequest();
                    r.listener = new DataListener<Map<String, String>>() {

                        @Override
                        public void onError(Exception error) {
                            logE("error = %s", error);
                        }

                        @Override
                        public void onComplete(List<Map<String, String>> all) {
                            logE("all %s, %s, %s", all.size(), all.get(0).size(), all.get(0).keySet());
                            logE("all %s", all);
                            ThreadUtil.runOnUiThread(() -> {
                                String m = all.toString();
                                TextView t = view.findViewById(R.id.itemMessage);
                                t.setText(m);
                            });
                        }
                    };
                    sf.queryItem(item.getAbsolutePath(), r);
                }
            }).buildAndShow();
        });
    }

    private void showSortMenu(View anchor) {
        if (sortMenu == null) {
            Pair<View, PopupWindow> pair = createPopupWindow(R.layout.popup_sort_menu, getView());
            View vs = pair.first;
            RadioGroup rg = vs.findViewById(R.id.sortGroup);
            int id = RadioGroup.NO_ID;
            for (int i = 0; i < rg.getChildCount(); i++) {
                View vi = rg.getChildAt(i);
                String tag = vi.getTag() + "";
                vi.setOnClickListener((v) -> {
                    filePref.fileSort.set(tag);
                    reloadMe();
                });
                if (filePref.fileSort.get().equals(tag)) {
                    id = vi.getId();
                }
            }
            rg.check(id);
            sortMenu = pair;
        }
        sortMenu.second.showAsDropDown(anchor);
    }

    /**
     * @return pair of inflated menu view & popup window
     */
    private Pair<View, PopupWindow> createPopupWindow(@LayoutRes int layoutId, View root) {
        ViewGroup vg = null;
        if (root instanceof ViewGroup) {
            vg = (ViewGroup) root;
        }
        // Create MenuWindow
        View menu = LayoutInflater.from(getActivity()).inflate(layoutId, vg, false);
        int wrap = ViewGroup.LayoutParams.WRAP_CONTENT;
        PopupWindow w = new PopupWindow(menu, wrap, wrap, true);
        w.setOutsideTouchable(true);
        //w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));// still transparent
        return new Pair<>(menu, w);
    }

    private void updateSortState() {
        String text = FilePreference.sortString();
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
        reloadMe();
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
        //diskLib.adapter.notifyItemRangeChanged(0, diskLib.adapter.getItemCount()); // x
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

    private Map<File, Long> getFileSizes(File root) {
        Map<File, Long> map = FileUtil.getFileSizeMap(root, new FileUtil.OnDFSFile() {
            @Override
            public void onStart(File f) {
                // empty
            }
        });
        return map;
    }

    private void updateFile() {
        parentFolder.setText(state);
    }


    @Override
    protected int getPageLayoutId() {
        return R.layout.fragment_file;
    }
}
