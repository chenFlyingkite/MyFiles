package com.flyingkite.myfiles.library;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.View;
import android.widget.EditText;

import com.flyingkite.myfiles.R;

import java.io.File;

import flyingkite.library.android.log.Loggable;
import flyingkite.library.android.util.DialogUtil;

public class RenameFileDialog extends DialogUtil.Alert implements Loggable {

    private File target;
    private ActionListener onAction;
    public RenameFileDialog(Activity a, File root, ActionListener listener) {
        super(a, R.layout.dialog_rename);
        target = root;
        onAction = listener;
    }

    @Override
    public void onFinishInflate(View view, AlertDialog dialog) {
        EditText edit = view.findViewById(R.id.itemInput);
        edit.setText(target.getName());

        view.findViewById(R.id.itemCancel).setOnClickListener((vv) -> {
            if (onAction != null) {
                onAction.onAction(Activity.RESULT_CANCELED);
            }
            dialog.dismiss();
        });
        view.findViewById(R.id.itemOK).setOnClickListener((vv) -> {
            File next = new File(target.getParent(), edit.getText().toString());
            boolean ok = target.renameTo(next);
            logE("rename ok = %s", ok);
            logE("old = %s\nnew = %s", target, next);
            if (onAction != null) {
                onAction.onAction(Activity.RESULT_OK);
            }
            dialog.dismiss();
        });
    }
}
