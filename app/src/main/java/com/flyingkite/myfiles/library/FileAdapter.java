package com.flyingkite.myfiles.library;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.flyingkite.myfiles.R;
import com.flyingkite.myfiles.ViewHelper2;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import flyingkite.library.android.log.Loggable;
import flyingkite.library.android.util.PackageManagerUtil;
import flyingkite.library.androidx.TicTac2;
import flyingkite.library.androidx.recyclerview.RVAdapter;
import flyingkite.library.java.data.FileInfo;
import flyingkite.library.java.util.FileUtil;

public class FileAdapter extends RVAdapter<File, FileAdapter.FileVH, FileAdapter.ItemListener> implements ViewHelper2, Loggable {

    public interface ItemListener extends RVAdapter.ItemListener<File, FileVH> {

        default boolean onLongClick(File item, FileVH holder, int position) {
            return false;
        }

        default void onAction(File item, FileVH vh, int position) {
        }

    }

    // Color.WHITE, Color.RED, Color.GREEN, Color.BLUE, Color.CYAN, Color.YELLOW, Color.MAGENTA
    private int[] colors = {0x44888888, 0x44cc0000, 0x4400cc00, 0x440000cc,
            0x44cccc00, 0x4400cccc, 0x44cc00cc};
    private Context context;
    private PackageManagerUtil packageManager;
    private TicTac2 clock = new TicTac2();
    private final SimpleDateFormat timeYYYYMMDD = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US);

    private final Set<Integer> selectedIndex = new HashSet<>();

    private Map<File, FileInfo> spaces = new HashMap<>();

    private boolean isInGrid; // grid or row
    private final int[] viewIDs = {R.layout.view_file_row
            , R.layout.view_file_grid_folder, R.layout.view_file_grid_file};

    public void setSpaces(Map<File, FileInfo> space) {
        spaces = space;
    }

    public Map<File, FileInfo> getSpaces() {
        return spaces;
    }

    public boolean isInSelectionMode() {
        return selectedIndex.size() > 0;
    }

    public Set<Integer> getSelectedIndex() {
        return selectedIndex;
    }

    public List<String> getSelectedPaths() {
        List<String> ans = new ArrayList<>();
        for (int x : selectedIndex) {
            File f = itemOf(x);
            if (f != null) {
                ans.add(f.getAbsolutePath());
            }
        }
        return ans;
    }

    public void addSelect(int index) {
        boolean notifyAll = selectedIndex.isEmpty();
        boolean ok = selectedIndex.add(index);
        if (notifyAll) {
            notifyDataSetChanged();
        } else if (ok) {
            notifyItemChanged(index);
        }
    }

    public void removeSelect(int index) {
        boolean ok = selectedIndex.remove(index);
        boolean notifyAll = selectedIndex.isEmpty();
        if (notifyAll) {
            notifyDataSetChanged();
        } else if (ok) {
            notifyItemChanged(index);
        }
    }

    public void toggleSelect(int index) {
        if (selectedIndex.contains(index)) {
            removeSelect(index);
        } else {
            addSelect(index);
        }
    }

    public void toggleSelect() {
        Set<Integer> old = new HashSet<>(selectedIndex);
        selectedIndex.clear();
        for (int i = 0; i < getItemCount(); i++) {
            if (!old.contains(i)) {
                selectedIndex.add(i);
            }
        }
        notifyDataSetChanged();
    }

    public void selectAll() {
        selectedIndex.clear();
        for (int i = 0; i < getItemCount(); i++) {
            selectedIndex.add(i);
        }
        notifyDataSetChanged();
    }

    public void setInGrid(boolean grid) {
        isInGrid = grid;
    }

    public boolean getInGrid() {
        return isInGrid;
    }

    public void toggleGrid() {
        setInGrid(!isInGrid);
    }

    @NonNull
    @Override
    public FileVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (context == null) {
            context = parent.getContext();
            packageManager = new PackageManagerUtil(context);
        }
        int id;
        if (isInGrid) {
            if (viewType == 0) {
                id = viewIDs[1];
            } else {
                id = viewIDs[2];
            }
        } else {
            id = viewIDs[0];
        }
        return new FileVH(inflateView(parent, id));
    }

    @Override
    public int getItemViewType(int position) {
        if (isInGrid) {
            File it = itemOf(position);
            if (it.isDirectory()) {
                return 0;
            } else {
                return 1;
            }
        } else {
            return 0;
        }
    }

    @Override
    public void onBindViewHolder(FileVH vh, int position) {
        super.onBindViewHolder(vh, position);
        clock.enable(false);
        clock.tic();
        clock.tic();

        File it = itemOf(position);
        String path = it.getAbsolutePath();
        clock.tac("itemOf");
        clock.tic();
        vh.name.setText(it.getName());
        clock.tac("name");
        clock.tic();
        vh.info.setText(info(it));
        clock.tac("info");
        clock.tic();
        //vh.itemView.setBackgroundColor(colors[position % colors.length]);
        clock.tac("back");
        clock.tic();
        ImageView demo = setupPreview(vh, it);
        loadPreview(demo, it);
        clock.tac("Glide");
        setupSizePart(vh, it);
        clock.tac("#onBind %s : %s", position, it);
        vh.itemView.setOnLongClickListener((v) -> {
            if (onItem != null) {
                return onItem.onLongClick(it, vh, position);
            }
            return false;
        });
        if (vh.action != null) {
            vh.action.setVisibility(isInSelectionMode() ? View.GONE : View.VISIBLE);
            vh.action.setOnClickListener((v) -> {
                if (onItem != null) {
                    onItem.onAction(it, vh, position);
                }
            });
        }
        if (vh.selected != null) {
            vh.selected.setVisibility(isInSelectionMode() ? View.VISIBLE : View.GONE);
            vh.selected.setChecked(selectedIndex.contains(position));
        }
    }

    private void setupSizePart(FileVH vh, File it) {
        if (vh == null) return;
        if (vh.sizePart == null) return;
        if (vh.sizeRate == null) return;
        if (vh.sizeB == null) return;
        Context c = context;

        boolean spaceInfo = spaces != null && spaces.containsKey(it);
        long rate = 0;
        if (spaceInfo) {
            long max = vh.sizeRate.getMax();
            FileInfo info = spaces.get(it);
            long me = info.fileSize;
            File par = it.getParentFile();
            if (me > 0 && par != null && spaces.containsKey(par)) {
                FileInfo parFI = spaces.get(par);
                long parMe = parFI.fileSize;
                if (parMe > 0) {
                    rate = max * me / parMe;
                } else {
                    rate = max;
                }
            }
            vh.sizeB.setText(c.getString(R.string.percent_size, rate * 0.01, FileUtil.toGbMbKbB(me)));
            vh.sizePart.setVisibility(View.VISIBLE);
        } else {
            vh.sizePart.setVisibility(View.GONE);
        }
        vh.sizeRate.setProgress((int) rate);
    }

    private ImageView setupPreview(FileVH vh, File it) {
        String path = it.getAbsolutePath();
        ImageView demo = vh.thumb;
        if (isInGrid) {
            if (it.isFile()) {
                if (getIconId(path) != 0) {
                    demo = vh.thumb2;
                }
                demo = vh.thumb2;
            }
        }
        setViewVisibility(vh.thumb, false);
        setViewVisibility(vh.thumb2, false);
        setViewVisibility(demo, true);
        return demo;
    }

    private void loadPreview(ImageView thumb, File it) {
        String path = it.getAbsolutePath();
        if (it.isDirectory()) {
            thumb.setImageResource(R.drawable.baseline_folder_24);
        } else {
            if (FileUtil.isAPK(it)) {
                Drawable icon = packageManager.getPackageIcon(path);
                //CharSequence appName = packageManager.getPackageLabel(path);
                //Glide.with(vh.thumb).load(it).placeholder(icon).into(vh.thumb); // fail in android 12 resume
                thumb.setImageDrawable(icon);
            } else {
                int icon = getIconId(path);
                boolean useGlide = path.startsWith("Android/data/");
                if (icon != 0) {
                    thumb.setImageResource(icon);
                } else {
                    Glide.with(thumb).load(it)
                            .placeholder(R.drawable.icon_file)
//                        .listener(new RequestListener<>() {
//                        @Override
//                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
//                            logE("onLoadFailed 1st = %s, %s", isFirstResource, it);
//                            if (e != null) {
//                                e.printStackTrace();
//                            }
//                            int ic = R.mipmap.ic_launcher_round;
//                            vh.thumb.setImageResource(ic);
//                            return false;
//                        }
//
//                        @Override
//                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
//                            logE("onResourceReady, 1st = %s, %s, %s", isFirstResource, it, resource);
//                            return false;
//                        }
//                    })
                            .into(thumb); // todo : failed with resume on list has one non image file, like text file
                }
            }
        }
    }

    private int getIconId(String path) {
        int icon = 0;
        //icon = R.drawable.icon_file;
        if (FileUtil.isPDF(path)) {
            icon = R.drawable.pdf;
        } else if (FileUtil.isMicrosoftWord(path)) {
            icon = R.drawable.ms_word;
        } else if (FileUtil.isMicrosoftExcel(path)) {
            icon = R.drawable.ms_xls;
        } else if (FileUtil.isMicrosoftPowerPoint(path)) {
            icon = R.drawable.ms_ppt;
        } else if (FileUtil.isTXT(path)) {
            icon = R.drawable.txt;
        } else if (FileUtil.isJson(path)) {
            icon = R.drawable.json;
        }
        return icon;
    }

    private String info(File f) {
        if (f == null) return "";

        clock.tic();
        String time = timeYYYYMMDD.format(f.lastModified());
        String si = "";
        boolean isDir = f.isDirectory();
        if (isDir) {
            String[] fs = f.list();
            if (fs != null) {
                Arrays.sort(fs);
            }
            if (fs == null) {
                si = context.getString(R.string.items_no);
            } else {
                si = context.getString(R.string.items, fs.length);
            }
            clock.tac("list %s", si);
        } else {
            long size = f.length();
            si += FileUtil.toGbMbKbB(size);
            clock.tac("size %s", si);
        }

        return _fmt("%s, %s", time, si);
    }

    public static class FileVH extends RecyclerView.ViewHolder {
        public TextView info;
        public TextView name;
        public TextView sizeB;
        public ImageView thumb;
        public ImageView thumb2;
        public View action;
        public CheckBox selected;
        public View sizePart;
        public ProgressBar sizeRate;

        public FileVH(@NonNull View v) {
            super(v);
            info = v.findViewById(R.id.itemInfo);
            name = v.findViewById(R.id.itemTitle);
            sizeB = v.findViewById(R.id.itemSizeB);
            thumb = v.findViewById(R.id.itemThumb);
            action = v.findViewById(R.id.itemAction);
            thumb2 = v.findViewById(R.id.itemPreview);
            sizePart = v.findViewById(R.id.sizePart);
            sizeRate = v.findViewById(R.id.itemSizeRate);
            selected = v.findViewById(R.id.itemSelected);
        }
    }
}
