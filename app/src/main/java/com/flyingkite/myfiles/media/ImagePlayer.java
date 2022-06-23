package com.flyingkite.myfiles.media;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.flyingkite.myfiles.R;
import com.flyingkite.myfiles.ShareUtil;
import com.flyingkite.myfiles.library.BaseFragment;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import flyingkite.library.android.log.Loggable;
import flyingkite.library.androidx.TicTac2;
import flyingkite.library.androidx.mediastore.MediaStoreKit;
import flyingkite.library.androidx.mediastore.listener.DataListener;
import flyingkite.library.androidx.mediastore.request.MediaRequest;
import flyingkite.library.androidx.mediastore.store.StoreImages;
import flyingkite.library.java.util.FileUtil;
import flyingkite.library.java.util.StringUtil;

public class ImagePlayer extends BaseFragment {
    public static final String TAG = "ImagePlayer";

    public static final String BUNDLE_PATH = "path";
    private String imagePath = "";

    private ImageView mainView;
    private TextView mainInfo;
    private View imageShare;
    private Map<String, String> cursorMap;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        initUI();
        parseArg();

        Glide.with(mainView).load(imagePath).placeholder(R.mipmap.ic_launcher_round).into(mainView);
        queryImage(imagePath);
    }

    private void parseArg() {
        Bundle b = getArguments();
        if (b != null) {
            imagePath = b.getString(BUNDLE_PATH, imagePath);
        }
    }

    private void initUI() {
        mainView = findViewById(R.id.mainImage);
        mainInfo = findViewById(R.id.imageInfo);
        imageShare = findViewById(R.id.imageShare);
        imageShare.setOnClickListener((v) -> {
            Context c = getContext();
            if (c == null) return;
            String mime = null;
            if (cursorMap != null) {
                mime = cursorMap.get(MediaStore.Images.ImageColumns.MIME_TYPE);
            }
            Uri uri = Uri.parse(imagePath);
            ShareUtil.sendUriIntent(c, uri, mime);
        });
    }

    private void queryImage(String path) {
        Context c = getContext();
        if (c == null) return;

        MediaStoreKit kit = new MediaStoreKit(c);
        StoreImages si = new StoreImages(c);
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

                    mainInfo.setText(sb.toString());
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
        String ans = "";
        if ("90".equals(ori) || "270".equals(ori)) {
            ans = it.get(MediaStore.Images.ImageColumns.HEIGHT);
        } else {
            ans = it.get(MediaStore.Images.ImageColumns.WIDTH);
        }
        return ans;
    }

    private String getHeight(Map<String, String> it) {
        if (it == null) {
            return null;
        }

        String ori = it.get(MediaStore.Images.ImageColumns.ORIENTATION);
        String ans = "";
        if ("90".equals(ori) || "270".equals(ori)) {
            ans = it.get(MediaStore.Images.ImageColumns.WIDTH);
        } else {
            ans = it.get(MediaStore.Images.ImageColumns.HEIGHT);
        }
        return ans;
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

    private static class DataLis<T> implements DataListener<T>, Loggable {
        @Override
        public void onPreExecute() {
            logE("onPreExecute");
        }

        @Override
        public void onQueried(int count, Cursor cursor) {
            logE(" > %s items, %s columns in %s", count, cursor.getColumnCount(), cursor);
        }

        @Override
        public void onProgress(int position, int count, T data) {
            logE(" -> #%4d/%4d : %s", position, count, data);
        }

        @Override
        public void onComplete(List<T> all) {
            logE("%s items", all.size());
            for (int i = 0; i < all.size(); i++) {
                logE("#%4d : %s", i, all.get(i));
            }
        }

        @Override
        public void onError(Exception error) {
            logE("X_X failed %s", error);
        }
    }
}
