package com.flyingkite.myfiles.media;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Size;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.GestureDetectorCompat;

import com.bumptech.glide.Glide;
import com.flyingkite.myfiles.R;
import com.flyingkite.myfiles.library.BaseFragment;
import com.flyingkite.myfiles.util.ShareUtil;

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
import flyingkite.library.java.util.MathUtil;
import flyingkite.library.java.util.StringUtil;

public class ImagePlayer extends BaseFragment {
    public static final String TAG = "ImagePlayer";

    public static final String BUNDLE_PATH = "path";
    private String imagePath = "";

    private FrameLayout mainFrame;
    private ImageView mainView;
    private TextView mainInfo;
    private View imageShare;
    private Map<String, String> cursorMap;

    private ScaleGestureDetector scaleR;
    private GestureDetectorCompat detector;
    private MyGesture gesture = new MyGesture();

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
        Context c = getContext();
        mainFrame = findViewById(R.id.mainFrame);
        mainView = findViewById(R.id.mainImage);
        mainInfo = findViewById(R.id.imageInfo);
        imageShare = findViewById(R.id.imageShare);
        imageShare.setOnClickListener((v) -> {
            if (c == null) return;
            String mime = null;
            if (cursorMap != null) {
                mime = cursorMap.get(MediaStore.Images.ImageColumns.MIME_TYPE);
            }
            Uri uri = Uri.parse(imagePath);
            ShareUtil.sendUriIntent(c, uri, mime);
        });
        if (c != null) {
            detector = new GestureDetectorCompat(c, gesture);
            scaleR = new ScaleGestureDetector(c, gesture);
        }
        mainFrame.setOnTouchListener((v, event) -> {
            boolean val = false;
            if (scaleR != null) {
                val |= scaleR.onTouchEvent(event);
            }
            if (detector != null) {
                val |= detector.onTouchEvent(event);
            }
            return val;
        });
    }

    // https://developer.android.com/develop/ui/views/touch-and-input/gestures/scale
    private class MyGesture extends GestureDetector.SimpleOnGestureListener implements ScaleGestureDetector.OnScaleGestureListener {
        // for scale
        private View scaledView;
        private Point frame0;
        private Size size0;
        private float span0;
        private float span;

        // for move
        private View movedView;
        private Point margin0;

        @Override
        public boolean onDown(MotionEvent e) {
            logE("onDown");
            downForScale(mainView);
            downForMove(mainView);
            return true;
        }

        private void downForScale(View v) {
            scaledView = v;
            if (frame0 == null) {
                frame0 = new Point(mainFrame.getWidth(), mainFrame.getHeight());
                size0 = new Size(v.getWidth(), v.getHeight());
            }
        }

        private void downForMove(View v) {
            ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            margin0 = new Point(p.leftMargin, p.topMargin);
            movedView = v;
        }

        // e1 = down Event, e2 = move event
        private void movingView(MotionEvent e1, MotionEvent e2) {
            float dx = e2.getX() - e1.getX();
            float dy = e2.getY() - e1.getY();
            ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) movedView.getLayoutParams();
            int nextL = Math.round(margin0.x + dx);
            int nextT = Math.round(margin0.y + dy);
            int minL = frame0.x - p.width;
            int minT = frame0.y - p.height;
            int marginL = MathUtil.makeInRange(nextL, minL, 0);
            int marginT = MathUtil.makeInRange(nextT, minT, 0);
//            logE("nL = %s, %s ~ %s", marginL, minL, nextL);
//            logE("nT = %s, %s ~ %s", marginT, minT, nextT);
            p.leftMargin = marginL;
            p.topMargin = marginT;
            movedView.setLayoutParams(p);
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            logE("onScroll(%s, %s)", distanceX, distanceY);
//            logE("e1 = %s", e1);
//            logE("e2 = %s", e2);
            movingView(e1, e2);
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            logE("onFling(%s, %s)", velocityX, velocityY);
            return super.onFling(e1, e2, velocityX, velocityY);
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            logE("onSingleTapUp");
            return super.onSingleTapUp(e);
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            logE("onSingleTapConfirmed");
            return super.onSingleTapConfirmed(e);
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            logE("onDoubleTap");
            return super.onDoubleTap(e);
        }

        @Override
        public boolean onDoubleTapEvent(MotionEvent e) {
            logE("onDoubleTapEvent");
            return super.onDoubleTapEvent(e);
        }

        @Override
        public void onLongPress(MotionEvent e) {
            logE("onLongPress");
            super.onLongPress(e);
        }


        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            span = detector.getCurrentSpan();
            logE("onScale %s, %s", detector.getScaleFactor(), span / span0);
            scalingView(span / span0);
            return false;
        }

        private void scalingView(float scale) {
            View v = scaledView;
            ViewGroup.LayoutParams p = v.getLayoutParams();

            int nextW = Math.round(size0.getWidth() * scale);
            int nextH = Math.round(size0.getHeight() * scale);
            int minW = frame0.x;
            int minH = frame0.y;
            int maxW = minW * 5;
            int maxH = minH * 5;
            p.width = MathUtil.makeInRange(nextW, minW, maxW);
            p.height = MathUtil.makeInRange(nextH, minH, maxH);
            v.setLayoutParams(p);
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            logE("frame0 = %s", frame0);
            span0 = detector.getCurrentSpan();
            logE("onScaleBegin");
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            logE("onScaleEnd");
        }
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
        return true;
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
