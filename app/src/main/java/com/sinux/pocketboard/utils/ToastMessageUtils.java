package com.sinux.pocketboard.utils;

import android.content.Context;
import android.content.res.Resources;
import android.widget.Toast;

import com.sinux.pocketboard.R;

public class ToastMessageUtils {

    private static Toast toastMessage;

    public static void showMessage(Context context, int resId) {
        try {
            showMessage(context, context.getString(resId));
        } catch (Resources.NotFoundException e) {
            showMessage(context, context.getString(R.string.pocketboard_disabled));
        }
    }

    public static void showMessage(Context context, String message) {
        if (toastMessage != null) {
            toastMessage.cancel();
        }
        toastMessage = Toast.makeText(context, message, Toast.LENGTH_SHORT);
        toastMessage.show();
    }
}
