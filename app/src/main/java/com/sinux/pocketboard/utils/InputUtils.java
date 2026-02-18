package com.sinux.pocketboard.utils;

import android.content.Context;
import android.text.InputType;
import android.view.InputDevice;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;

public class InputUtils {

    public static InputMethodInfo getInputMethodInfo(Context context, InputMethodManager inputMethodManager) {
        for (InputMethodInfo imi : inputMethodManager.getInputMethodList()) {
            if (imi.getPackageName().equals(context.getPackageName())) {
                return imi;
            }
        }
        return null;
    }

    public static boolean isNumericEditor(EditorInfo editorInfo) {
        return switch (editorInfo.inputType & InputType.TYPE_MASK_CLASS) {
            case InputType.TYPE_CLASS_NUMBER, InputType.TYPE_CLASS_DATETIME,
                 InputType.TYPE_CLASS_PHONE -> true;
            default -> false;
        };
    }

    public static boolean isSuggestionAllowedEditor(EditorInfo editorInfo) {
        if (editorInfo.inputType == InputType.TYPE_NULL) {
            return false;
        }

        return switch (editorInfo.inputType & InputType.TYPE_MASK_VARIATION) {
            case InputType.TYPE_TEXT_VARIATION_PASSWORD,
                 InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
                 InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,
                 InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
                 InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS,
                 InputType.TYPE_TEXT_VARIATION_URI -> false;
            default ->
                // Should be 'return (editorInfo.inputType & InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE) == 0;'
                // but IRL almost all auto complete fields don't work as expected, so we can fell free
                // to display our own suggestions
                    true;
        };
    }

    public static KeyEvent translateKeyEvent(KeyEvent originalEvent, int translatedKeyCode, int action, int metaState) {
        return new KeyEvent(
                originalEvent.getDownTime(),
                originalEvent.getEventTime(),
                action,
                translatedKeyCode,
                originalEvent.getRepeatCount(),
                metaState,
                KeyCharacterMap.VIRTUAL_KEYBOARD,
                translatedKeyCode,
                0,
                InputDevice.SOURCE_KEYBOARD
        );
    }

    public static KeyEvent createKeyEvent(long eventTime, int keyCode, int action, int repeatCount, int metaState) {
        return new KeyEvent(
                eventTime,
                eventTime,
                action,
                keyCode,
                repeatCount,
                metaState,
                KeyCharacterMap.VIRTUAL_KEYBOARD,
                keyCode,
                0,
                InputDevice.SOURCE_KEYBOARD
        );
    }
}
