package com.flyingkite.myfiles;

import android.view.View;
import android.view.ViewGroup;

import flyingkite.library.android.log.Loggable;

public interface ViewHelper2 extends Loggable {

    default void toggleSelected(View v) {
        if (v == null) return;
        v.setSelected(!v.isSelected());
    }

    default void setAllChildrenSelected(ViewGroup vg, boolean sel) {
        if (vg == null) return;
        int n = vg.getChildCount();
        for (int i = 0; i < n; i++) {
            vg.getChildAt(i).setSelected(sel);
        }
    }

    default void setSingleSelectChildren(ViewGroup vg, View sel) {
        if (vg == null) return;
        int n = vg.getChildCount();
        for (int i = 0; i < n; i++) {
            View v = vg.getChildAt(i);
            logE("#%s : v == sel = %s", i, sel == v);
            v.setSelected(sel == v);
        }
    }
}
