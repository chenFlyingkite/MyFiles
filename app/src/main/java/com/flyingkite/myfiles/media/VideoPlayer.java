package com.flyingkite.myfiles.media;

import android.content.Context;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.flyingkite.myfiles.R;
import com.flyingkite.myfiles.library.BaseFragment;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import flyingkite.library.androidx.TicTac2;
import flyingkite.library.androidx.mediastore.MediaStoreKit;
import flyingkite.library.androidx.mediastore.listener.DataListener;
import flyingkite.library.androidx.mediastore.request.MediaRequest;
import flyingkite.library.androidx.mediastore.store.StoreVideo;
import flyingkite.library.java.util.FileUtil;
import flyingkite.library.java.util.StringUtil;

public class VideoPlayer extends BaseFragment {

    public static final String BUNDLE_PATH = "path";
    private String videoPath = "";
    private VideoView mainVideo;
    private TextView videoInfo;
    private View videoShare;
    private Map<String, String> cursorMap;

    // https://developer.android.com/guide/topics/media/mediaplayer
    private MediaPlayer player;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        parseArgs();
        initUI();

        playPath(videoPath);
        queryVideo(videoPath);
    }

    private void initUI() {
        mainVideo = findViewById(R.id.mainVideo);
        videoInfo = findViewById(R.id.videoInfo);
        videoShare = findViewById(R.id.videoShare);

        videoShare.setOnClickListener((v) -> {
            Context c = getContext();
            if (c == null) return;
            String mime = null;
            if (cursorMap != null) {
                mime = cursorMap.get(MediaStore.Images.ImageColumns.MIME_TYPE);
            }
            Uri uri = Uri.parse(videoPath);
            ImagePlayer.sendUriIntent(c, uri, mime);
        });
    }

    private void parseArgs() {
        Bundle arg = getArguments();
        if (arg != null) {
            videoPath = arg.getString(BUNDLE_PATH, videoPath);
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

    // TODO similar with ImagePlayer
    private void queryVideo(String path) {
        Context c = getContext();
        if (c == null) return;

        MediaStoreKit kit = new MediaStoreKit(c);
        StoreVideo si = new StoreVideo(c);
        MediaRequest q = si.newRequest();
        si.applyFile(path, q);
        q.listener = new DataListener<>() {
            private TicTac2 clock = new TicTac2();
            @Override
            public void onPreExecute() {
                logE("onPreExec");
                clock.tic();
            }

            @Override
            public void onQueried(int count, Cursor cursor) {
                logE(" > %s items, %s columns in %s", count, cursor.getColumnCount(), cursor);
            }

            @Override
            public void onInfo(int position, int count, String info) {
                logE(" -> I : #%4d/%4d : %s", position, count, info);
            }

            @Override
            public void onProgress(int position, int count, Map<String, String> data) {
                logE(" -> P : #%4d/%4d : %s", position, count, data);
            }

            @Override
            public void onComplete(List<Map<String, String>> all) {
                clock.tac("complete for %s", path);
                if (all.size() > 0) {
                    Map<String, String> it = all.get(0);
                    cursorMap = it;
                    // date taken
                    String dateMod = it.get(MediaStore.Images.ImageColumns.DATE_MODIFIED);
                    String dateAdd = it.get(MediaStore.Images.ImageColumns.DATE_ADDED);
                    long dateModif = StringUtil.parseLong(dateMod);
                    long dateAdded = StringUtil.parseLong(dateAdd);
                    String dateM = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.US).format(dateModif * 1000);
                    String dateA = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.US).format(dateAdded * 1000);
                    long size = StringUtil.parseLong(it.get(MediaStore.Images.ImageColumns.SIZE));
                    logE("dateModif = %s", dateModif);
                    logE("dateAdded = %s", dateAdded);
                    String sizeZ = FileUtil.toGbMbKbB(size);
                    StringBuilder sb = new StringBuilder();
                    sb.append(it.get(MediaStore.Images.ImageColumns.DATA)).append("\n")
                            .append(getWidth(it)).append("x")
                            .append(getHeight(it)).append("  ")
                            .append(it.get(MediaStore.Images.ImageColumns.ORIENTATION)).append("∘  ")
                            .append(it.get(MediaStore.Images.ImageColumns.MIME_TYPE)).append("  ")
                            .append(sizeZ).append("\n")
                            .append(getString(R.string.fileInfo_Date_Modified, dateM))
                            .append("   ").append(dateMod)
                            .append("\n")
                            // Added seems to be no need
                            .append(getString(R.string.fileInfo_Date_Added, dateA))
                            .append("   ").append(dateAdd)
                            .append("\n")
                    ;

                    videoInfo.setText(sb.toString());
                }
            }

            @Override
            public void onError(Exception error) {
                clock.tac("error for %s", path);
            }
        };
        kit.queryRequest(q);
    }

    private String getWidth(Map<String, String> it) {
        if (it == null) {
            return null;
        }

        String ori = it.get(MediaStore.Images.ImageColumns.ORIENTATION);
        if ("90".equals(ori) || "270".equals(ori)) {
            String h = it.get(MediaStore.Images.ImageColumns.HEIGHT);
            return h;
        } else {
            String w = it.get(MediaStore.Images.ImageColumns.WIDTH);
            return w;
        }
    }

    private String getHeight(Map<String, String> it) {
        if (it == null) {
            return null;
        }

        String ori = it.get(MediaStore.Images.ImageColumns.ORIENTATION);
        if ("90".equals(ori) || "270".equals(ori)) {
            String w = it.get(MediaStore.Images.ImageColumns.WIDTH);
            return w;
        } else {
            String h = it.get(MediaStore.Images.ImageColumns.HEIGHT);
            return h;
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
