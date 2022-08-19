package com.flyingkite.myfiles.library;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.flyingkite.myfiles.R;

import java.io.File;

import flyingkite.library.android.util.DialogUtil;
import flyingkite.library.android.util.ToastUtil;
import flyingkite.library.java.util.FileUtil;

public class NewFolderDialog extends DialogUtil.Alert implements ToastUtil {
    private File parentNowAt;
    private ActionListener onAction;

    public NewFolderDialog(Activity a, File locate, ActionListener listener) {
        super(a, R.layout.dialog_rename);
        parentNowAt = locate;
        onAction = listener;
    }

    @Override
    public Context getContext() {
        return getActivity();
    }

    @Override
    public void onFinishInflate(View view, AlertDialog dialog) {
        File root = parentNowAt;
        File next = FileUtil.getUnconflictFile(root, getString(R.string.newFolder));
        String nameIfEmpty = next.getName();
        TextView title = view.findViewById(R.id.itemTitle);
        EditText input = view.findViewById(R.id.itemInput);
        View okBtn = view.findViewById(R.id.itemOK);
        View cancel = view.findViewById(R.id.itemCancel);
        title.setText(R.string.actionCreateFolder);
        input.setHint(nameIfEmpty);
        //input.addTextChangedListener(onText);
        input.setText("");
        okBtn.setOnClickListener((v) -> {
            String name = input.getText().toString();
            if (TextUtils.isEmpty(name)) {
                name = nameIfEmpty;
            }
            File dst = new File(root, name);
            boolean ok = dst.mkdir();
            String msg = title.getText() + " " + getActivity().getString(ok ? R.string.success : R.string.fail);
            showToast(msg);
            if (onAction != null) {
                onAction.onAction(Activity.RESULT_OK);
            }
            dialog.dismiss();
        });
        cancel.setOnClickListener((v) -> {
            if (onAction != null) {
                onAction.onAction(Activity.RESULT_CANCELED);
            }
            dialog.dismiss();
        });
//        dialog.setOnDismissListener((self) -> {
//            input.removeTextChangedListener(onText);
//        });
    }
}
