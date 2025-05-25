package com.example.mastermate.utils;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import com.example.mastermate.R;

public class LoadingDialog {
    private Activity activity;
    private Dialog dialog;
    private TextView messageTextView;

    public LoadingDialog(Activity activity, String message) {
        this.activity = activity;
        if (activity == null || activity.isFinishing()) {
            return;
        }
        dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_loading, null);
        dialog.setContentView(view);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        messageTextView = view.findViewById(R.id.loadingDialogMessage);
        if (messageTextView != null && message != null) {
            messageTextView.setText(message);
        }
    }

    public void startDialog() {
        if (dialog != null && !dialog.isShowing() && activity != null && !activity.isFinishing()) {
            try {
                dialog.show();
            } catch (Exception e) {
                Log.e("LoadingDialog", "Error showing dialog", e);
            }
        }
    }

    public void dismissDialog() {
        if (dialog != null && dialog.isShowing()) {
            try {
                dialog.dismiss();
            } catch (Exception e) {
                Log.e("LoadingDialog", "Error dismissing dialog", e);
            }
        }
    }

    public boolean isDialogShowing(){
        return dialog != null && dialog.isShowing();
    }

    public void setMessage(String message) {
        if (messageTextView != null && message != null) {
            messageTextView.setText(message);
        }
    }
}