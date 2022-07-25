package com.flyingkite.myfiles.library;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.contract.ActivityResultContract;
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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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
import flyingkite.library.java.data.FileInfo;
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

    private Library<PathItemAdapter> folderPathLib;
    private PathItemAdapter pathItemAdapter = new PathItemAdapter();
    private List<File> pathItems = new ArrayList<>();
    private TicTac2 clock = new TicTac2();
    private File parentNowAt;
    private FrameLayout frameImage;

    private TextView parentFolder;
    private View reload;
    private View dfsFile;
    private TextView sortBtn;
    private View createFolderBtn;
    private ImageView pasteBtn;

    private int moveTo;
    private File moveSrcFile;

    private String state;
    private FilePreference filePref = new FilePreference();

    private Pair<View, PopupWindow> sortMenu;
    private boolean unstableScroll = false;
    private Deque<Point> savedPos = new ArrayDeque<>();
    private final int ASD = 123456;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        // https://developer.android.com/reference/android/os/storage/StorageManager#ACTION_CLEAR_APP_CACHE
        registerForActivityResult(new ActivityResultContract<Void, Void>() {
            @NonNull
            @Override
            public Intent createIntent(@NonNull Context context, Void input) {
                Intent it = new Intent(StorageManager.ACTION_CLEAR_APP_CACHE);
                logE("create intent = %s", it);
                return it;
            }

            @Override
            public Void parseResult(int resultCode, @Nullable Intent intent) {
                logE("resultCode = %s, %s", resultCode, intent);
                return null;
            }
        }, new ActivityResultCallback<Void>() {
            @Override
            public void onActivityResult(Void result) {
                logE("result");
            }
        });
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        parentNowAt = Environment.getExternalStorageDirectory();
        pathItems.add(parentNowAt);
        initDisk();
        frameImage = findViewById(R.id.frameImage);
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
            ans.x = vh.getBindingAdapterPosition();
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
            fileList(parentNowAt);
        });
        dfsFile = findViewById(R.id.dfsSize);
        dfsFile.setOnClickListener((v) -> {
            Map<File, FileInfo> spaces = getFileSizes(parentNowAt);
            diskLib.adapter.setSpaces(spaces);
            diskLib.adapter.notifyDataSetChanged();
        });
        sortBtn = findViewById(R.id.sortBtn);
        sortBtn.setOnClickListener((v) -> {
            showSortMenu(v);
        });
        createFolderBtn = findViewById(R.id.createFolderBtn);
        createFolderBtn.setOnClickListener((v) -> {
            Activity a = getActivity();
            if (a == null) {
                return;
            }

            new DialogUtil.Alert(a, R.layout.dialog_rename, new DialogUtil.InflateListener() {
                @Override
                public void onFinishInflate(View view, AlertDialog dialog) {
                    File root = parentNowAt;
                    TextView title = view.findViewById(R.id.itemTitle);
                    EditText input = view.findViewById(R.id.itemInput);
                    View okBtn = view.findViewById(R.id.itemOK);
                    View cancel = view.findViewById(R.id.itemCancel);
                    TextWatcher onText = new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                        }

                        @Override
                        public void onTextChanged(CharSequence s, int start, int before, int count) {

                        }

                        @Override
                        public void afterTextChanged(Editable s) {
                            okBtn.setEnabled(s.length() > 0);
                        }
                    };
                    title.setText(getString(R.string.action_create_folder));
                    input.setHint(R.string.folder_name);
                    input.addTextChangedListener(onText);
                    input.setText("");
                    okBtn.setEnabled(false);
                    okBtn.setOnClickListener((v) -> {
                        String name = input.getText().toString();
                        File dst = new File(root, name);
                        boolean ok = dst.mkdir();
                        String msg = title.getText() + " " + getString(ok ? R.string.success : R.string.fail);
                        showToast(msg);
                        dialog.dismiss();
                    });
                    cancel.setOnClickListener((v) -> {
                        dialog.dismiss();
                    });
                    dialog.setOnDismissListener((self) -> {
                        input.removeTextChangedListener(onText);
                        reloadMe();
                    });
                }
            }).buildAndShow();
        });
        pasteBtn = findViewById(R.id.pasteBtn);
        pasteBtn.setOnClickListener((v) -> {
            if (moveSrcFile == null) return;
            String name = moveSrcFile.getName();
            String path = moveSrcFile.getAbsolutePath();
            File dst = new File(parentNowAt, name);
            logE("name = %s, dst = %s", name, dst);
            if (moveTo == 1) {
                // move
                boolean ok = moveSrcFile.renameTo(dst);
                Toast.makeText(getContext(), "ok = " + ok, Toast.LENGTH_SHORT).show();
            } else if (moveTo == 2) {
                // copy
                List<File> dfs = listSub(moveSrcFile);
                logE("dfs get %s items", dfs.size());
                for (int i = 0; i < dfs.size(); i++) {
                    logE("#%s : %s", i, dfs.get(i));
                }
                for (int i = 0; i < dfs.size(); i++) {
                    File next = dfs.get(i);
                    String newPath = next.getAbsolutePath().replaceFirst(path, dst.getAbsolutePath());
                    File newItem = new File(newPath);
                    copy(newItem, next);
                    logE("new as exist = %s, %s", newItem.exists(), newItem);
                }

                Toast.makeText(getContext(), "ok = ", Toast.LENGTH_SHORT).show();
            }
            moveSrcFile = null;
            moveTo = 0;
            updateMove();
            reloadMe();
        });
        updateMove();

        parentFolder = findViewById(R.id.parentFolder);
        initDiskLib();
        initPathLib();
    }

    private void copy(File dst, File src) {
        if (FileUtil.isGone(src)) return;

        if (src.isDirectory() && dst.isDirectory()) {
            dst.mkdirs();
        } else {
            FileUtil.createNewFile(dst);
            try {
                FileUtil.copy(new FileInputStream(src), new FileOutputStream(dst));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private List<File> listSub(File root) {
        List<File> ans = new ArrayList<>();
        if (root == null) return ans;
        ans.add(root);
        if (root.isDirectory()) {
            File[] fs = root.listFiles();
            if (fs != null) {
                List<File> sub;
                for (int i = 0; i < fs.length; i++) {
                    sub = listSub(fs[i]);
                    ans.addAll(sub);
                }
            }
        }
        return ans;
    }

    private void updateMove() {
        final int[] src = {R.drawable.baseline_folder_24, R.drawable.baseline_file_open_24, R.drawable.baseline_folder_copy_24};
        pasteBtn.setEnabled(moveSrcFile != null);
        pasteBtn.setImageResource(src[moveTo]);
    }

    private void initPathLib() {
        folderPathLib = new Library<>(findViewById(R.id.folderPath), false);
        folderPathLib.setViewAdapter(pathItemAdapter);
        pathItemAdapter.setDataList(pathItems);
        pathItemAdapter.setItemListener(new PathItemAdapter.ItemListener() {
            @Override
            public void onClick(File item, PathItemAdapter.PathVH holder, int position) {
                //pathItemAdapter.setNowAt(position);
                pathItemAdapter.moveTo(item);
                fileList(item);
            }
        });
        updatePathLib();
    }

    private void updatePathLib() {
        String main = parentNowAt.getAbsolutePath();
//        List<String> split = itemTo(parentDeepAt, parentNowAt);
//        logE("main = %s", main);
//        logE("deep = %s", deep);
//        logE("split = %s", split);
//        pathItemAdapter.setDataList(split);
//        pathItemAdapter.notifyDataSetChanged();
    }

    private List<String> buildItems(File src, File root) {
        List<String> ans = new ArrayList<>();
        if (src == null || root == null) return ans;
        if (src.getAbsolutePath().startsWith(root.getAbsolutePath())) {
            File now = src;
            while (now != null && !now.equals(root)) {
                String item = now.getAbsolutePath();
                ans.add(0, item);
                now = now.getParentFile();
            }
            ans.add(root.getAbsolutePath());
        }
        return ans;
    }

    private void initDiskLib() {
        diskLib = new Library<>(findViewById(R.id.recyclerDisk), true);

        List<File> ans = new ArrayList<>();
        FileAdapter ta = new FileAdapter();
        ta.setItemListener(new FileAdapter.ItemListener() {
            @Override
            public void onClick(File item, FileAdapter.FileVH holder, int position) {
//                if (parentNowAt.equals(item.getParentFile())) {
                    // next folder
                    //pathItems.set(pathItems.size() - 1, item.getAbsolutePath());
//                    pathItemAdapter.notifyItemChanged(pathItems.size() - 1);
//                    pathItems.remove(pathItems.size() - 1); // pop
//                    if (pathItemAdapter.getNowAt() == pathItems.size() - 1) {
//                        pathItems.add(item); // push
//                        int end = pathItems.size() - 1;
//                        pathItemAdapter.notifyItemInserted(end);
//                        pathItemAdapter.setNowAt(end);
//                    } else {
//                        pathItems = buildItems(item, Environment.getExternalStorageDirectory());
//                        pathItemAdapter.setDataList(pathItems);
//                        pathItemAdapter.notifyDataSetChanged();
//                    }
//                } else {
//                }
                pathItemAdapter.moveTo(item);

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
                updatePathLib();
            }

            @Override
            public boolean onLongClick(File item, FileAdapter.FileVH holder, int position) {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Activity a = getActivity();
                    Intent it = new Intent(StorageManager.ACTION_CLEAR_APP_CACHE);
                    a.startActivityForResult(it, ASD);
                }
                return true;
            }

            @Override
            public void onAction(File item, FileAdapter.FileVH vh, int position) {
                // TODO
                logE("show drop down %s", item);
                Pair<View, PopupWindow> pair = createPopupWindow(R.layout.popup_file_menu, getView());
                initActions(pair, item);
                pair.second.showAsDropDown(vh.action);
                //pair.second.showAsDropDown(vh.itemView); // still out of bound
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

    private void initActions(Pair<View, PopupWindow> pair, File item) {
        View w = pair.first;
        PopupWindow pw = pair.second;
        // file name
        TextView txt = w.findViewById(R.id.itemTitle);
        txt.setText(item.toString());
        w.findViewById(R.id.itemShare).setOnClickListener((v) -> {
            share(item);
            pw.dismiss();
        });
        w.findViewById(R.id.itemOpenWith).setOnClickListener((v) -> {
            openWith(item);
            pw.dismiss();
        });
        w.findViewById(R.id.itemMoveTo).setOnClickListener((v) -> {
            moveSrcFile = item;
            moveTo = 1;
            updateMove();
            pw.dismiss();
        });
        w.findViewById(R.id.itemCopyTo).setOnClickListener((v) -> {
            moveSrcFile = item;
            moveTo = 2;
            updateMove();
            pw.dismiss();
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
                        pw.dismiss();
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
                        pw.dismiss();
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
        parentNowAt = f;

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
                    File fi = a[i];
                    String k = fi.getAbsolutePath();
                    String mime = URLConnection.getFileNameMap().getContentTypeFor(k);
                    //logE("#%s : %s %s", i, mime, fi);
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
        logE("parent = %s", parentNowAt);

        if (parentNowAt != null) {
            File p = parentNowAt.getParentFile();
            //pathItemAdapter.moveNowAt(-1);
            //pathItemAdapter.setNowAt(pathItemAdapter.getNowAt() - 1);
            pathItemAdapter.moveTo(p);
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
                updatePathLib();
                return true;
            }
        }
        return false;
    }

    private Map<File, FileInfo> getFileSizes(File root) {
        Map<File, FileInfo> map = FileUtil.getFileInfoMap(root, new FileUtil.OnDFSFile<>() {
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
