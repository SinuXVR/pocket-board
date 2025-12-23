package com.sinux.pocketboard.utils;

import android.content.Context;
import android.text.InputType;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;

public class InputUtils {

    public static InputMethodInfo getInputMethodInfo(Context context, InputMethodManager inputMethodManager) {
        for (InputMethodInfo imi : inputMethodManager.getEnabledInputMethodList()) {
            if (imi.getPackageName().equals(context.getPackageName())) {
                return imi;
            }
        }
        return null;
    }

    public static boolean isNumericEditor(EditorInfo editorInfo) {
        switch (editorInfo.inputType & InputType.TYPE_MASK_CLASS) {
            case InputType.TYPE_CLASS_NUMBER:
            case InputType.TYPE_CLASS_DATETIME:
            case InputType.TYPE_CLASS_PHONE:
                return true;
            default:
                return false;
        }
    }

    public static boolean isSuggestionAllowedEditor(EditorInfo editorInfo) {
        if (editorInfo.inputType == InputType.TYPE_NULL) {
            return false;
        }

        switch (editorInfo.inputType & InputType.TYPE_MASK_VARIATION) {
            case InputType.TYPE_TEXT_VARIATION_PASSWORD:
            case InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD:
            case InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD:
            case InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS:
            case InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS:
            case InputType.TYPE_TEXT_VARIATION_URI:
                return false;
            default:
                // Should be 'return (editorInfo.inputType & InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE) == 0;'
                // but IRL almost all auto complete fields don't work as expected, so we can fell free
                // to display our own suggestions
                return true;
        }
    }
}
