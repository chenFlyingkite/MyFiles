package com.flyingkite.myfiles.library;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.flyingkite.myfiles.FragmentUtil;

import flyingkite.library.android.log.Loggable;
import flyingkite.library.android.util.ActivityUtil;
import flyingkite.library.android.util.BackPage;
import flyingkite.library.android.util.ToastUtil;

public class BaseFragment extends Fragment implements Loggable, BackPage, ToastUtil, FragmentUtil, ActivityUtil {

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        log("onAttach(%s)", context);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        log("onCreate(%s)", savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        int pageId = getPageLayoutId();
        if (pageId > 0) {
            return inflater.inflate(pageId, container, false);
        } else {
            return null;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        log("onStart()");
    }

    @Override
    public void onResume() {
        super.onResume();
        log("onResume()");
    }

    @Override
    public void onPause() {
        super.onPause();
        log("onPause()");
    }

    @Override
    public void onStop() {
        super.onStop();
        log("onStop()");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        log("onDestroy()");
    }

    @LayoutRes
    protected int getPageLayoutId() {
        return -1;
    }

    //-- common method for fragment

    public void childFragmentManager_Replace(int id, Fragment f, String tag) {
        FragmentManager fm = getChildFragmentManager();
        FragmentTransaction tx = fm.beginTransaction();
        tx.replace(id, f, tag);
        tx.commitAllowingStateLoss();
    }

    public Fragment findFragmentById_FromChildFM(int id) {
        return getChildFragmentManager().findFragmentById(id);
    }

    public void removeFromParent(Fragment f) {
        getParentFragmentManager()
                .beginTransaction()
                .remove(f)
                .commitAllowingStateLoss();
    }

    public final void runOnUiThread(Runnable r) {
        Activity a = getActivity();
        if (a != null) {
            a.runOnUiThread(r);
        }
    }
}
