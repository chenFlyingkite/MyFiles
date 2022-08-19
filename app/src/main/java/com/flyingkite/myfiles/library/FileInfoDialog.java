package com.flyingkite.myfiles.library;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.View;
import android.widget.TextView;

import com.flyingkite.myfiles.R;

import java.io.File;
import java.util.List;
import java.util.Map;

import flyingkite.library.android.log.Loggable;
import flyingkite.library.android.util.DialogUtil;
import flyingkite.library.android.util.ThreadUtil;
import flyingkite.library.androidx.mediastore.listener.DataListener;
import flyingkite.library.androidx.mediastore.request.MediaRequest;
import flyingkite.library.androidx.mediastore.store.StoreFiles;

public class FileInfoDialog extends DialogUtil.Alert implements Loggable {
    private File item;
    private ActionListener onAction;

    public FileInfoDialog(Activity a, File locate, ActionListener listener) {
        super(a, R.layout.dialog_message);
        item = locate;
        onAction = listener;
    }

    @Override
    public void onFinishInflate(View view, AlertDialog dialog) {
        TextView t = view.findViewById(R.id.itemTitle);
        t.setText(item.getAbsolutePath());
        StoreFiles sf = new StoreFiles(getActivity());
        MediaRequest r = sf.newRequest();
        r.listener = new DataListener<>() {

            @Override
            public void onError(Exception error) {
                logE("error = %s", error);
            }

            @Override
            public void onComplete(List<Map<String, String>> all) {
                logE("all %s, %s, %s", all.size(), all.get(0).size(), all.get(0).keySet());
                logE("all %s", all);
                ThreadUtil.runOnUiThread(() -> {
                    String m = all.toString();
                    TextView t = view.findViewById(R.id.itemMessage);
                    t.setText(m);
                });
            }
        };
        sf.queryItem(item.getAbsolutePath(), r);
    }
}
