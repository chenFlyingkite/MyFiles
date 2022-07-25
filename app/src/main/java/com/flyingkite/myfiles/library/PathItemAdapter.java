package com.flyingkite.myfiles.library;

import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.flyingkite.myfiles.R;

import java.io.File;
import java.util.Arrays;

import flyingkite.library.android.log.Loggable;
import flyingkite.library.androidx.recyclerview.RVAdapter;

public class PathItemAdapter extends RVAdapter<File, PathItemAdapter.PathVH, PathItemAdapter.ItemListener> implements Loggable {
    public static final int MODE_END = 0;
    public static final int MODE_END2 = 0;
    private int mode = MODE_END;

    public interface ItemListener extends RVAdapter.ItemListener<File, PathVH> {

    }

    @NonNull
    @Override
    public PathVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new PathVH(inflateView(parent, R.layout.view_path_item));
    }

    private int nowAt;
    public void setNowAt(int index) {
        int prev = nowAt;
        int next = index;
        nowAt = next;
        logE("prev = %s, next = %s", prev, next);
        logE("data = %s", dataList);
        notifyItemRangeChanged(Math.min(prev, next) + 1, Math.abs(prev - next));
    }

    public void moveNowAt(int offset) {
        setNowAt(getNowAt() + offset);
    }

    public int getNowAt() {
        return nowAt;
    }

    public void moveTo(File it) {
        if (it == null) return;

        String dst = it.getAbsolutePath();
        logE("nowAt = %s, move to %s", nowAt, dst);
        int here = nowAt;
        int back = 0;
        File now = itemOf(here);
        // 1. Pop back to GCD common folder (deepest common folder)
        while (now != null && !dst.startsWith(now.getAbsolutePath())) {
            now = now.getParentFile();
            back++;
            logE("back = %s, now = %s", back, now);
        }
        logE("back = %s, now = %s", back, now);
        popTail(back);

        // 2. go down to it
        int go = 0;
        if (now != null) {
            String move = dst.replaceFirst(now.getAbsolutePath(), "");
            // if move == now, move = empty, or move = /a/b/c
            if (move.startsWith(File.separator)) {
                move = move.replaceFirst(File.separator, "");
            }
            logE("move = %s", move);
            if (move.isEmpty() == false) {
                String[] parts = move.split(File.separator);
                go = parts.length;
                logE("go = %s, parts = %s", go, Arrays.toString(parts));
                for (int i = 0; i < parts.length; i++) {
                    File next = new File(now, parts[i]);
                    dataList.add(next);
                    now = next;
                }
            }
        }

        // 3. Update nowAt
        nowAt = here - back + go;

        logE("end nowAt = %s", nowAt);
        ln();
        notifyDataSetChanged();
    }

    private void ln() {
        logE("dataList = ");
        for (int i = 0; i < dataList.size(); i++) {
            logE("#%s : %s", i, dataList.get(i));
        }
    }

    private void popTail(int count) {
        for (int i = 0; i < count; i++) {
            dataList.remove(dataList.size() - 1);
        }
        logE("pops as %s", dataList);
    }

    @Override
    public void onBindViewHolder(PathVH vh, int position) {
        super.onBindViewHolder(vh, position);
        File it = itemOf(position);
        String name = it.getName();
        if (position == 0) {
            name = it.getAbsolutePath();
        }
        vh.itemPath.setText(name);
        vh.itemPath.setTextColor(position <= nowAt ? Color.BLUE : Color.WHITE);
    }

    @Override
    protected void onWillClickItem(File item, PathVH holder) {
        //setNowAt(holder.getBindingAdapterPosition());
    }

    public class PathVH extends RecyclerView.ViewHolder {
        private TextView itemPath;

        public PathVH(@NonNull View v) {
            super(v);
            itemPath = v.findViewById(R.id.itemPath);
        }
    }
}
