package com.flyingkite.myfiles.library;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.View;
import android.widget.TextView;

import com.flyingkite.myfiles.R;

import java.io.File;

import flyingkite.library.android.log.Loggable;
import flyingkite.library.android.util.DialogUtil;
import flyingkite.library.java.util.FileUtil;

public class DeleteDialog extends DialogUtil.Alert implements Loggable {
    private File item;
    private ActionListener onAction;

    public DeleteDialog(Activity a, File locate, ActionListener listener) {
        super(a, R.layout.dialog_message);
        item = locate;
        onAction = listener;
    }

    @Override
    public void onFinishInflate(View view, AlertDialog dialog) {
        TextView t;
        t = view.findViewById(R.id.itemTitle);
        t.setText(getString(R.string.delete_title, item.getName()));
        t = view.findViewById(R.id.itemMessage);
        t.setText(getString(R.string.delete_title_confirm, item.getAbsolutePath()));

        view.findViewById(R.id.itemOK).setOnClickListener((v) -> {
            boolean ok = FileUtil.ensureDelete(item);
            logE("ok = %s, for %s", ok, item);
            if (onAction != null) {
                onAction.onAction(Activity.RESULT_OK);
            }
            dialog.dismiss();
        });
        view.findViewById(R.id.itemCancel).setOnClickListener((v) -> {
            if (onAction != null) {
                onAction.onAction(Activity.RESULT_CANCELED);
            }
            dialog.dismiss();
        });
    }
}
