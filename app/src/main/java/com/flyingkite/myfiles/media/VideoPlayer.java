package com.flyingkite.myfiles.media;

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.MediaController;
import android.widget.VideoView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.flyingkite.myfiles.R;
import com.flyingkite.myfiles.library.BaseFragment;

public class VideoPlayer extends BaseFragment {

    public static final String BUNDLE_PATH = "path";
    private String srcPath = "";
    private VideoView mainVideo;

    // https://developer.android.com/guide/topics/media/mediaplayer
    private MediaPlayer player;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        parseArgs();
        initUI();

        playPath(srcPath);
    }

    private void initUI() {
        mainVideo = findViewById(R.id.mainVideo);
    }

    private void parseArgs() {
        Bundle arg = getArguments();
        if (arg != null) {
            srcPath = arg.getString(BUNDLE_PATH, srcPath);
        }
    }

    private void playRaw(int rawId) {
        MediaPlayer mediaPlayer = MediaPlayer.create(getContext(), rawId);
        mediaPlayer.start(); // no need to call prepare(); create() does that for you
    }

    private void playPath(String path) {
        MediaController control = new MediaController(getContext());
        control.setAnchorView(mainVideo);
        Uri uri = Uri.parse(path);
        //mainVideo.setVideoPath(path);
        mainVideo.setMediaController(control);
        mainVideo.setVideoURI(uri);
        mainVideo.start();
    }

    private void releasePlayer() {
        if (player != null) {
            player.stop();
            player.release();
            player = null;
        }
    }

    @Override
    public boolean onBackPressed() {
        removeFromParent(this);
        return false;
    }

    @Override
    protected int getPageLayoutId() {
        return R.layout.fragment_video;
    }
}
