package com.flyingkite.myfiles.media;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.flyingkite.myfiles.R;
import com.flyingkite.myfiles.library.BaseFragment;

public class ImagePlayer extends BaseFragment {
    public static final String TAG = "ImagePlayer";

    private ImageView mainView;
    public static final String BUNDLE_PATH = "path";
    private String imagePath = "";


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        initUI();
        parseArg();
        Glide.with(mainView).load(imagePath).placeholder(R.mipmap.ic_launcher_round).into(mainView);
    }

    private void parseArg() {
        Bundle b = getArguments();
        if (b != null) {
            imagePath = b.getString(BUNDLE_PATH, imagePath);
        }
    }

    private void initUI() {
        mainView = findViewById(R.id.mainImage);
    }

    @Override
    public boolean onBackPressed() {
        removeFromParent(this);
        return false;
    }

    @Override
    protected int getPageLayoutId() {
        return R.layout.fragment_image;
    }
}
