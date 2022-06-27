package com.flyingkite.myfiles;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import androidx.annotation.NonNull;

import java.io.File;

import flyingkite.library.android.log.Loggable;

public class ShareUtil {
    private static final Loggable z = new Loggable() {};

    /**
     *  i18n text string for share title
     */
    public static String shareTo(Context context) {
        return context.getString(R.string.share_to);
    }

    /**
     *  i18n text string for open file with
     */
    public static String openWith(Context context) {
        return context.getString(R.string.open_with);
    }

    public static void sendUriIntent(@NonNull Context context, Uri uri, String type) {
        sendUriIntent(context, shareTo(context), uri, type);
    }

    public static void sendUriIntent(@NonNull Context context, String title, Uri uri, String type) {
        Intent it = new Intent(Intent.ACTION_SEND);
        it.putExtra(Intent.EXTRA_STREAM, uri);
        int flg = Intent.FLAG_GRANT_READ_URI_PERMISSION;
        //flg |= Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
        it.addFlags(flg);
        //it.setClipData(ClipData.newRawUri("", uri));
        //it.setData(uri);
        it.setDataAndType(uri, type);
        //z.logE("sendUriIntent %s %s", type, uri);
        try {
            context.startActivity(Intent.createChooser(it, title));
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void openUriIntent(@NonNull Context context, Uri uri, String type) {
        openUriIntent(context, openWith(context), uri, type);
    }

    public static void openUriIntent(@NonNull Context context, String title, Uri uri, String type) {
        Intent it = new Intent(Intent.ACTION_VIEW);
        it.putExtra(Intent.EXTRA_STREAM, uri);
        int flg = Intent.FLAG_GRANT_READ_URI_PERMISSION;
        //flg |= Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
        it.addFlags(flg);
        //it.setClipData(ClipData.newRawUri("", uri));
        //it.setData(uri);
        it.setDataAndType(uri, type);
        //z.logE("sendUriIntent %s %s", type, uri);
        try {
            context.startActivity(Intent.createChooser(it, title));
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * 1. Scan png path as uri by file provider and send it as "image/png" intent
     * 2.
     */
    private static void sharePng(Context context, String pngPath) {
        File file = new File(pngPath);
        Uri uri = App.getUriForFile(file);
//        file = /storage/emulated/0/Android/data/com.flyingkite.mytoswiki.debug/cache/20220331_100723829.png
//        uri = content://com.flyingkite.mytoswiki.debug.fileprovider/cache_data/20220331_100723829.png
        z.logE("file = %s\nuri = %s", file, uri);
        sendUriIntent(context, uri, "image/png");
    }

    private static void scanFile(Context context, String filename) {
        MediaScannerConnection.scanFile(context, new String[]{filename}, null, (path, uri) -> {
            z.logE("Scanned %s\n  as -> %s", path, uri);
        });
    }

    // Fields for #extFilesFile()
    // Maybe public for other class
    private static final String data = "data";
}
