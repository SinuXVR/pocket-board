package com.sinux.pocketboard.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;
import android.widget.Toast;

import com.sinux.pocketboard.R;

public class ToastMessageUtils {

    private static Toast toastMessage;

    public static void showMessage(Context context, int resId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        try {
            showMessage(context, context.getString(resId));
        } catch (Resources.NotFoundException e) {
            showMessage(context, context.getString(R.string.pocketboard_disabled));
        }
    }

    private static void showMessage(Context context, String message) {
        if (toastMessage != null) {
            toastMessage.cancel();
        }
        toastMessage = Toast.makeText(context, message, Toast.LENGTH_SHORT);
        toastMessage.show();
    }
}
