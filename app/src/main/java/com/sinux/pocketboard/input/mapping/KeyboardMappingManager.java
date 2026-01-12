package com.sinux.pocketboard.input.mapping;

import android.content.Context;
import android.text.TextUtils;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;

import androidx.annotation.NonNull;
import androidx.collection.LruCache;

import com.sinux.pocketboard.R;
import com.sinux.pocketboard.utils.ToastMessageUtils;

public class KeyboardMappingManager {

    private static final int KEYBOARD_MAPPING_CACHE_DEPTH = 2;
    private static final String INPUT_METHOD_KEYBOARD_MAPPING = "KeyboardMapping";
    private static final String TITAN_MODEL_NAME = "Titan";

    private final Context context;
    private final LruCache<String, KeyboardMapping> keyboardMappings;

    private KeyboardMapping currentMapping;

    public KeyboardMappingManager(Context context, InputMethodManager inputMethodManager) {
        this.context = context;
        keyboardMappings = new LruCache<>(KEYBOARD_MAPPING_CACHE_DEPTH);
        switchToKeyboardMapping(inputMethodManager.getCurrentInputMethodSubtype());
    }

    public KeyboardMapping getCurrentMapping() {
        return currentMapping;
    }

    public void switchToNumericKeyboardMapping() {
        setCurrentKeyboardMapping("numeric");
    }

    public void switchToKeyboardMapping(InputMethodSubtype inputMethodSubtype) {
        setCurrentKeyboardMapping(inputMethodSubtype.getExtraValueOf(INPUT_METHOD_KEYBOARD_MAPPING));
    }

    private void setCurrentKeyboardMapping(String keyMappingFile) {
        if (TextUtils.isEmpty(keyMappingFile)) {
            return;
        }

        if (keyboardMappings.get(keyMappingFile) == null) {
            try {
                KeyboardMapping mapping = loadKeyboardMapping(keyMappingFile);
                keyboardMappings.put(keyMappingFile, mapping);
                currentMapping = mapping;
            } catch (Exception e) {
                toastError();
            }
        } else {
            currentMapping = keyboardMappings.get(keyMappingFile);
        }
    }

    private void toastError() {
        ToastMessageUtils.showMessage(context, R.string.keyboard_mapping_load_failed);
    }

    private String getDeviceModelString() {
        if (TITAN_MODEL_NAME.equalsIgnoreCase(android.os.Build.MODEL)) {
            return "_titan";
        }

        return "";
    }

   @NonNull
   private KeyboardMapping loadKeyboardMapping(String keyMappingFile) throws Exception {
       KeyboardMappingParser parser = new KeyboardMappingParser(context, keyMappingFile, getDeviceModelString());
       return parser.parseMapping();
   }
}
