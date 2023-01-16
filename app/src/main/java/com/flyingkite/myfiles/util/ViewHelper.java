package com.flyingkite.myfiles.util;

import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import androidx.annotation.IdRes;
import androidx.annotation.Nullable;

public interface ViewHelper {
    @Nullable
    View getView();

    default void setOnClickListeners(View.OnClickListener lis, @IdRes int... ids) {
        for (int i : ids) {
            findViewById(i).setOnClickListener(lis);
        }
    }

    // keyboards
    // https://stackoverflow.com/questions/1109022/close-hide-the-android-soft-keyboard
    default void showKeyBoard(boolean show, View view) {
        if (view == null) return;
        Context c = view.getContext();

        InputMethodManager imm = (InputMethodManager) c.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            if (show) {
                imm.showSoftInput(view, 0);
            } else {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }
//    default void setMovementMethod(TextView t) {
//        t.setMovementMethod(LinkMovementMethod.getInstance());
//    }


    default <T extends View> T findViewById(@IdRes int id) {
        View w = getView();
        if (w == null) return null;
        return w.findViewById(id);
//        if (getActivity() != null) {
//            return getActivity().findViewById(id);
//        }
    }
}
