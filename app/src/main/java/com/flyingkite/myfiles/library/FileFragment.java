package com.flyingkite.myfiles.library;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.text.TextUtils;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupMenu;
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
import com.flyingkite.myfiles.FolderActivity;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import flyingkite.library.android.media.MediaMetadataRetrieverUtil;
import flyingkite.library.android.media.MimeTypeMapUtil;
import flyingkite.library.android.util.BackPage;
import flyingkite.library.androidx.TicTac2;
import flyingkite.library.androidx.recyclerview.CenterScroller;
import flyingkite.library.androidx.recyclerview.Library;
import flyingkite.library.java.data.FileInfo;
import flyingkite.library.java.util.FileUtil;

public class FileFragment extends BaseFragment {
    public static final String TAG = "FileFragment";
    public static final String EXTRA_PATH = "path";
    public static final String EXTRA_ACTION = "action";
    public static final String EXTRA_SOURCES = "sources";
    public static final String EXTRA_DESTINATION = "destination";
    public static final int ACTION_LIST = 0;
    public static final int ACTION_MOVE = 1;
    public static final int ACTION_COPY = 2;
    private static final String[] actionString = {"List", "Move", "Copy"};
    private int fileAction = ACTION_LIST;

    private OnFileActions onFileActions;
    public interface OnFileActions {
        boolean onActionPerformed(int action);
    }

    private Library<FileAdapter> diskLib;
    private FileAdapter fileAdapter = new FileAdapter();
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
    private FrameLayout frameImage;

    private View filesAction;
    private TextView parentFolder;
    private View reload;
    private View dfsFile;
    private TextView sortBtn;
    private View createFolderBtn;
    private ImageView pasteBtn;
    private View deleteBtn;

    //--
    private TextView confirm;
    private PopupMenu filesMenu;



    // states
    private File parentNowAt = Environment.getExternalStorageDirectory();
    private int moveTo;
    private File moveSrcFile;

    private String state;
    private FilePreference filePref = new FilePreference();
    private ArrayList<String> sourcePaths;
    private String destination;

    private Pair<View, PopupWindow> sortMenu;
    private boolean unstableScroll = false;
    private Deque<Point> savedPos = new ArrayDeque<>();
    private static final int REQ_INSTALL_APP = 456;

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
        if (context instanceof OnFileActions) {
            onFileActions = (OnFileActions) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        onFileActions = null;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        parentNowAt = Environment.getExternalStorageDirectory();
        parseArg();
        pathItems.add(parentNowAt);
        init();
        initDisk();
        frameImage = findViewById(R.id.frameImage);
        updateArg();
    }

    private void parseArg() {
        Bundle b = getArguments();
        if (b == null) return;

        String path = b.getString(EXTRA_PATH);
        if (TextUtils.isEmpty(path)) {
            parentNowAt = Environment.getExternalStorageDirectory();
        } else {
            parentNowAt = new File(path);
        }
        sourcePaths = b.getStringArrayList(EXTRA_SOURCES);
        destination = b.getString(EXTRA_DESTINATION, destination);
        fileAction = b.getInt(EXTRA_ACTION, fileAction);
        logE("action = %s", fileAction);
        logE("sourcePath = %s", sourcePaths.size());
        for (int i = 0; i < sourcePaths.size(); i++) {
            String f = sourcePaths.get(i);
            logE("#%s : %s", i, f);
        }
        logE("destination = %s", destination);
    }

    private void init() {
        confirm = findViewById(R.id.confirm);
        confirm.setOnClickListener((v) -> {
            if (fileAction == ACTION_MOVE){
                // perform move
                moveFiles(parentNowAt, sourcePaths);
                notifyActionPerformed();
            } else if (fileAction == ACTION_COPY){
                // perform copy
                copyFiles(parentNowAt, sourcePaths);
                notifyActionPerformed();
            } else {
                logE("X_X move to : %s", action(fileAction));
            }
        });
        inflateFilesMenu(findViewById(R.id.filesAction));
    }

    private void updateArg() {
        if (fileAction == ACTION_MOVE) {
            confirm.setText(R.string.move_to_here);
        } else if (fileAction == ACTION_COPY) {
            confirm.setText(R.string.copy_to_here);
        } else {
            confirm.setVisibility(View.GONE);
        }
    }

    private String action(int action) {
        return actionString[action];
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
        filesAction = findViewById(R.id.filesAction);
        filesAction.setOnClickListener((v) -> {
            prepareFilesMenu();
            filesMenu.show();
        });
        reload = findViewById(R.id.reload);
        reload.setOnClickListener((v) -> {
            updateSortState();
            fileList(parentNowAt);
        });
        dfsFile = findViewById(R.id.dfsSize);
        dfsFile.setOnClickListener((v) -> {
            Map<File, FileInfo> spaces = getFileSizes(parentNowAt);
            diskLib.adapter.setSpaces(spaces);
            reloadMe();
        });
        sortBtn = findViewById(R.id.sortBtn);
        sortBtn.setOnClickListener((v) -> {
            showSortMenu(v);
        });
        createFolderBtn = findViewById(R.id.createFolderBtn);
        createFolderBtn.setOnClickListener((v) -> {
            createFolderAt(parentNowAt);
        });
        pasteBtn = findViewById(R.id.pasteBtn);
        pasteBtn.setOnClickListener((v) -> {
            if (moveSrcFile == null) return;
            String name = moveSrcFile.getName();
            String path = moveSrcFile.getAbsolutePath();
            File dst = FileUtil.getUnconflictFile(parentNowAt, name);
            logE("name = %s, dst = %s", name, dst);
            if (moveTo == 1) {
                // move
                boolean ok = moveSrcFile.renameTo(dst);
                Toast.makeText(getContext(), "ok = " + ok, Toast.LENGTH_SHORT).show();
            } else if (moveTo == 2) {
                // copy
                List<File> dfs = FileUtil.listAllFiles(moveSrcFile);
                logE("dfs get %s items", dfs.size());
                for (int i = 0; i < dfs.size(); i++) {
                    logE("#%s : %s", i, dfs.get(i));
                }
                // this is test
//                List<File> bfs = FileUtil.listAllFilesBFS(moveSrcFile);
//                logE("bfs get %s items", bfs.size());
//                for (int i = 0; i < bfs.size(); i++) {
//                    logE("#%s : %s", i, bfs.get(i));
//                }
                for (int i = 0; i < dfs.size(); i++) {
                    File next = dfs.get(i);
                    String newPath = next.getAbsolutePath().replaceFirst(path, dst.getAbsolutePath());
                    File newItem = new File(newPath);
                    FileUtil.copy(newItem, next);
                    logE("new as exist = %s, %s", newItem.exists(), newItem);
                }

                Toast.makeText(getContext(), "copy ok", Toast.LENGTH_SHORT).show();
            }
            moveSrcFile = null;
            moveTo = 0;
            updateMove();
            reloadMe();
        });
        deleteBtn = findViewById(R.id.deleteBtn);
        deleteBtn.setOnClickListener((v) -> {
            List<String> chosen = fileAdapter.getSelectedPaths();
            deleteFiles(chosen);
        });

        updateDelete();
        updateMove();

        parentFolder = findViewById(R.id.parentFolder);
        initDiskLib();
        initPathLib();
    }

    private void createFolderAt(File root) {
        Activity a = getActivity();
        if (a == null) {
            return;
        }

        new NewFolderDialog(a, root, new ActionListener() {
            @Override
            public void onAction(int result) {
                reloadMe();
            }
        }).buildAndShow();
    }

    private void deleteFiles(List<String> list) {
        if (list == null) return;
        for (int i = 0; i < list.size(); i++) {
            File f = new File(list.get(i));
            FileUtil.ensureDelete(f);
        }
        clearSelection();
        reloadMe();
    }

    private void clearSelection() {
        fileAdapter.getSelectedIndex().clear();
        fileAdapter.notifyDataSetChanged();
        updateDelete();
    }

    private void updateDelete() {
        deleteBtn.setEnabled(fileAdapter.isInSelectionMode());
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
                pathItemAdapter.moveTo(item);
                fileList(item);
            }
        });
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
        fileAdapter.setItemListener(new FileAdapter.ItemListener() {
            @Override
            public void onClick(File item, FileAdapter.FileVH holder, int position) {
                if (fileAdapter.isInSelectionMode()) {
                    fileAdapter.toggleSelect(position);
                    updateDelete();
                    return;
                }

                String path = item.getAbsolutePath();
                logE("Disk #%s, %s", position, item);
                savedPosPush(makePoint());
                if (item.isDirectory()) {
                    pathItemAdapter.moveTo(item);
                    fileList(item);
                } else {
                    // not support for image
                    long duration = MediaMetadataRetrieverUtil.extractMetadataFromFilepath(item.getAbsolutePath(), MediaMetadataRetriever.METADATA_KEY_DURATION, -1L);
                    logE("Duration = %s", duration);

                    boolean isV = MimeTypeMapUtil.isPrefixOfWithMimeTypeFromExtension("video/", path);
                    boolean isI = MimeTypeMapUtil.isPrefixOfWithMimeTypeFromExtension("image/", path);
                    boolean isA = MimeTypeMapUtil.isPrefixOfWithMimeTypeFromExtension("audio/", path);
                    boolean isAPK = FileUtil.isAPK(item);
                    logE("v, i, a = %s, %s, %s", isV, isI, isA);
                    if (isI) {
                        openImage(item);
                    } else if (isV) {
                        openVideo(item);
                    } else if (isA) {
                        openVideo(item);
                    } else if (isAPK) {
                        installApp(App.getUriForFile(item), REQ_INSTALL_APP);
                    } else {
                        openImage(item);
                    }
                }
            }

            @Override
            public boolean onLongClick(File item, FileAdapter.FileVH holder, int position) {
                if (!fileAdapter.isInSelectionMode()) {
                    fileAdapter.addSelect(position);
                    updateDelete();
                }
                return true;
            }

            @Override
            public void onAction(File item, FileAdapter.FileVH vh, int position) {
                showActionMenu(vh.action, item, position);

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
        fileAdapter.setDataList(ans);
        diskLib.setViewAdapter(fileAdapter);
    }

    private void showPopupWindow(View anchor, File item, int position) {
        Pair<View, PopupWindow> pair = createPopupWindow(R.layout.popup_file_menu, getView());
        //initActions(pair, item, position);
        pair.second.showAsDropDown(anchor);
    }

    private void prepareFilesMenu() {
        Set<Integer> all = new HashSet<>();
        all.add(R.id.itemSelectAll);
        all.add(R.id.itemSelectToggle);
        all.add(R.id.itemCreateFolder);
        all.add(R.id.itemMoveTo);
        all.add(R.id.itemCopyTo);
        all.add(R.id.itemDelete);
        Set<Integer> show = new HashSet<>(all);
        if (fileAction == ACTION_LIST) {
            if (fileAdapter.isInSelectionMode()) {
                show.remove(R.id.itemCreateFolder);
            }
        } else if (fileAction == ACTION_MOVE) {
            show.remove(R.id.itemSelectAll);
            show.remove(R.id.itemSelectToggle);
            show.remove(R.id.itemMoveTo);
            show.remove(R.id.itemCopyTo);
            show.remove(R.id.itemDelete);
        } else if (fileAction == ACTION_COPY) {
            show.remove(R.id.itemSelectAll);
            show.remove(R.id.itemSelectToggle);
            show.remove(R.id.itemMoveTo);
            show.remove(R.id.itemCopyTo);
            show.remove(R.id.itemDelete);
        }
        for (int id : all) {
            MenuItem it = filesMenu.getMenu().findItem(id);
            if (show.contains(id)) {
                it.setVisible(true);
            } else {
                it.setVisible(false);
            }
        }
    }

    private void inflateFilesMenu(View anchor) {
        PopupMenu m = new PopupMenu(getContext(), anchor);
        m.inflate(R.menu.view_files_menu);
        m.setOnMenuItemClickListener((menu) -> {
            Activity a = getActivity();
            if (a == null) {
                return false;
            }

            int id = menu.getItemId();
            if (id == R.id.itemSelectAll) {
                fileAdapter.selectAll();
            } else if (id == R.id.itemSelectToggle) {
                fileAdapter.toggleSelect();
            } else if (id == R.id.itemCreateFolder) {
                createFolderAt(parentNowAt);
            } else if (id == R.id.itemMoveTo) {
                if (fileAction == ACTION_LIST) {
                    moveIntent();
                    clearSelection();
                } else {
                    logE("X_X move to : %s", action(fileAction));
                }
            } else if (id == R.id.itemCopyTo) {
                if (fileAction == ACTION_LIST) {
                    copyIntent();
                    clearSelection();
                } else {
                    logE("X_X copy to : %s", action(fileAction));
                }
            } else if (id == R.id.itemDelete) {
                List<String> chosen = fileAdapter.getSelectedPaths();
                deleteFiles(chosen);
            } else {
                return super.onOptionsItemSelected(menu);
            }
            return true;
        });

        filesMenu = m;
    }

    private void notifyActionPerformed() {
        if (onFileActions != null) {
            onFileActions.onActionPerformed(fileAction);
        }
    }

    private boolean moveFiles(File goal, List<String> source) {
        if (source == null) return false;

        for (int i = 0; i < source.size(); i++) {
            File src = new File(source.get(i));
            File dst = new File(goal, src.getName());
            logE("move from : %s\nto : %s", src, dst);
            src.renameTo(dst);
        }
        return true;
    }

    private void moveIntent() {
        ArrayList<String> paths = new ArrayList<>(fileAdapter.getSelectedPaths());
        String stay = parentNowAt.getAbsolutePath();

        Intent it = new Intent(getActivity(), FolderActivity.class);
        it.putExtra(EXTRA_PATH, stay);
        it.putExtra(EXTRA_SOURCES, paths);
        it.putExtra(EXTRA_ACTION, ACTION_MOVE);
        startActivity(it);
    }

    private boolean copyFiles(File goal, List<String> source) {
        if (source == null) return false;

        for (int i = 0; i < source.size(); i++) {
            File src = new File(source.get(i));
            File dst = new File(goal, src.getName());
            // TODO : delete exist or rename
            FileUtil.copy(dst, src);
            logE("copy from : %s\nto : %s", src, dst);
        }
        return true;
    }

    private void copyIntent() {
        ArrayList<String> paths = new ArrayList<>(fileAdapter.getSelectedPaths());
        String stay = parentNowAt.getAbsolutePath();

        Intent it = new Intent(getActivity(), FolderActivity.class);
        it.putExtra(EXTRA_PATH, stay);
        it.putExtra(EXTRA_SOURCES, paths);
        it.putExtra(EXTRA_ACTION, ACTION_COPY);
        startActivity(it);
    }

    private PopupMenu pmenu;

    private void showActionMenu(View anchor, File item, int position) {
        PopupMenu m = new PopupMenu(getContext(), anchor);
        pmenu = m;
        m.inflate(R.menu.view_file_menu);
        MenuItem it = m.getMenu().findItem(R.id.itemTitle);
        it.setTitle(item.getAbsolutePath());
        m.setOnMenuItemClickListener((menu) -> {
            Activity a = getActivity();
            if (a == null) {
                return false;
            }
            int id = menu.getItemId();

            if (id == R.id.itemShare) {
                share(item);
            } else if (id == R.id.itemOpenWith) {
                openWith(item);
            } else if (id == R.id.itemMoveTo) {
                moveSrcFile = item;
                moveTo = 1;
                updateMove();
            } else if (id == R.id.itemCopyTo) {
                moveSrcFile = item;
                moveTo = 2;
                updateMove();
            } else if (id == R.id.itemRename) {
                new RenameFileDialog(a, item, new ActionListener() {
                    @Override
                    public void onAction(int result) {
                        reloadMe();
                    }
                }).buildAndShow();
            } else if (id == R.id.itemDelete) {
                new DeleteDialog(a, item, new ActionListener() {
                    @Override
                    public void onAction(int result) {
                        reloadMe();
                    }
                }).buildAndShow();
            } else if (id == R.id.itemFileInfo) {

                new FileInfoDialog(a, item, null).buildAndShow();
            } else if (id == R.id.itemSelect) {
                fileAdapter.toggleSelect(position);
                updateDelete();
            } else if (id == R.id.itemTitle) {
                File f = parentNowAt;
                f = item;
                Intent enter = new Intent(getActivity(), FolderActivity.class);
                enter.putExtra(FolderActivity.EXTRA_PATH, f.getAbsolutePath());
                startActivity(enter);
            } else if (id == R.id.itemOpenWith) {
                openWith(item);
            } else {
                return super.onOptionsItemSelected(menu);
            }
            return true;
        });
        pmenu.show();
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

        // leave select mode
        if (fileAdapter.isInSelectionMode()) {
            clearSelection();
            return true;
        }

        // Back to parent folder
        if (parentNowAt != null) {
            File p = parentNowAt.getParentFile();
            pathItemAdapter.moveTo(p);
            if (p != null) {
                parentNowAt = p;
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
