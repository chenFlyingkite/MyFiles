package com.flyingkite.myfiles.library;

import android.content.Context;
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

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import flyingkite.library.android.log.Loggable;
import flyingkite.library.androidx.TicTac2;
import flyingkite.library.androidx.recyclerview.RVAdapter;
import flyingkite.library.java.data.FileInfo;
import flyingkite.library.java.util.FileUtil;

public class FileAdapter extends RVAdapter<File, FileAdapter.FileVH, FileAdapter.ItemListener> implements Loggable {

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
    private TicTac2 clock = new TicTac2();
    private final SimpleDateFormat timeYYYYMMDD = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US);

    private final Set<Integer> selectedIndex = new HashSet<>();

    private Map<File, FileInfo> spaces = new HashMap<>();

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
        if (selectedIndex.isEmpty()) {
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

    @NonNull
    @Override
    public FileVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (context == null) {
            context = parent.getContext();
        }
        return new FileVH(inflateView(parent, R.layout.view_file_row));
    }

    @Override
    public void onBindViewHolder(FileVH vh, int position) {
        super.onBindViewHolder(vh, position);
        clock.enable(false);
        clock.tic();
        clock.tic();

        File it = itemOf(position);
        clock.tac("itemOf");
        clock.tic();
        vh.name.setText(it.getName());
        clock.tac("name");
        clock.tic();
        vh.info.setText(info(it, position));
        clock.tac("info");
        clock.tic();
        vh.itemView.setBackgroundColor(colors[position % colors.length]);
        clock.tac("back");
        clock.tic();
        if (it.isFile()) {
            Glide.with(vh.thumb).load(it).placeholder(R.mipmap.ic_launcher_round).into(vh.thumb);
        } else {
            vh.thumb.setImageResource(R.drawable.baseline_folder_24);
        }
        clock.tac("Glide");
        long rate = 0;
        if (spaces.containsKey(it)) {
            FileInfo info = spaces.get(it);
            logE("info = %s for it = %s", info, it);
            long me = info.fileSize;
            File par = it.getParentFile();
            long max = vh.sizeRate.getMax();
            if (me > 0 && par != null && spaces.containsKey(par)) {
                long parMe = info.fileSize;
                if (parMe > 0) {
                    rate = max * me / parMe;
                } else {
                    rate = max;
                }
            }
            vh.sizeB.setText(rate + "%% " + FileUtil.toGbMbKbB(me));
        } else {
            vh.sizeB.setText("");
        }
        vh.sizeRate.setProgress((int) rate);
        clock.tac("#onBind %s : %s", position, it);
        vh.itemView.setOnLongClickListener((v) -> {
            if (onItem != null) {
                return onItem.onLongClick(it, vh, position);
            }
            return false;
        });
        vh.action.setVisibility(isInSelectionMode() ? View.GONE : View.VISIBLE);
        vh.action.setOnClickListener((v) -> {
            if (onItem != null) {
                onItem.onAction(it, vh, position);
            }
        });
        vh.selected.setVisibility(isInSelectionMode() ? View.VISIBLE : View.GONE);
        vh.selected.setChecked(selectedIndex.contains(position));
    }

    private String info(File f, int at) {
        if (f == null) return "";

        clock.tic();
        String time = timeYYYYMMDD.format(f.lastModified());
        String si = "";
        boolean isDir = f.isDirectory();
        if (isDir) {
            String[] fs = null;
            fs = f.list();
            if (fs != null) {
                Arrays.sort(fs);
            }
            String n = fs == null ? "no" : (fs.length + "");
            si += context.getString(R.string.file_item, n);
            clock.tac("list %s", si);
        } else {
            long size = f.length();
            si += FileUtil.toGbMbKbB(size);
            clock.tac("size %s", si);
        }

        return _fmt("#%d : %s, %s", at, time, si);
    }

    public static class FileVH extends RecyclerView.ViewHolder {

        public TextView info;
        public TextView name;
        public TextView sizeB;
        public ImageView thumb;
        public View action;
        public CheckBox selected;
        public ProgressBar sizeRate;

        public FileVH(@NonNull View v) {
            super(v);
            info = v.findViewById(R.id.itemInfo);
            name = v.findViewById(R.id.itemTitle);
            sizeB = v.findViewById(R.id.itemSizeB);
            thumb = v.findViewById(R.id.itemThumb);
            action = v.findViewById(R.id.itemAction);
            sizeRate = v.findViewById(R.id.itemSizeRate);
            selected = v.findViewById(R.id.itemSelected);
        }
    }
}
