package com.flyingkite.myfiles;

import android.app.Activity;
import android.view.View;

public interface FragmentUtil extends ViewUtil {

    Activity getActivity();

    @Override
    default <T extends View> T findViewById(int id) {
        T v = ViewUtil.super.findViewById(id);

        Activity a = getActivity();
        if (v == null) {
            if (a != null) {
                v = a.findViewById(id);
            }
        }

        return v;
    }
}
