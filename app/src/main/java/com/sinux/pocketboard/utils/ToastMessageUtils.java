package com.sinux.pocketboard.utils;

import android.content.Context;
import android.widget.Toast;

public class ToastMessageUtils {

    private static Toast toastMessage;

    public static void showMessage(Context context, int resId) {
        showMessage(context, context.getString(resId));
    }

    public static void showMessage(Context context, String message) {
        if (toastMessage != null) {
            toastMessage.cancel();
        }
        toastMessage = Toast.makeText(context, message, Toast.LENGTH_SHORT);
        toastMessage.show();
    }
}
