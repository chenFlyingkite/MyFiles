package com.flyingkite.myfiles.util;

import android.app.Activity;
import android.view.View;

public interface FragmentUtil extends ViewHelper {

    Activity getActivity();

    @Override
    default <T extends View> T findViewById(int id) {
        T v = ViewHelper.super.findViewById(id);

        if (v == null) {
            Activity a = getActivity();
            if (a != null) {
                v = a.findViewById(id);
            }
        }

        return v;
    }
}
