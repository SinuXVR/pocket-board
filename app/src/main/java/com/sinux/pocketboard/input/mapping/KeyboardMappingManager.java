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
    private static final String TITAN_2_MODEL_NAME = "Titan 2";

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

        String modelSuffix = getEffectiveModelSuffix(keyMappingFile);
        String cacheKey = keyMappingFile + modelSuffix;

        KeyboardMapping cachedMapping = keyboardMappings.get(cacheKey);
        if (cachedMapping == null) {
            try {
                KeyboardMapping mapping = loadKeyboardMapping(keyMappingFile, modelSuffix);
                keyboardMappings.put(cacheKey, mapping);
                currentMapping = KeyboardMappingOverrideStore.apply(context, cacheKey, mapping);
            } catch (Exception e) {
                toastError();
            }
        } else {
            currentMapping = KeyboardMappingOverrideStore.apply(context, cacheKey, cachedMapping);
        }
    }

    private void toastError() {
        ToastMessageUtils.showMessage(context, R.string.keyboard_mapping_load_failed);
    }

    private String getDeviceModelString() {
        String model = android.os.Build.MODEL;
        if (TITAN_2_MODEL_NAME.equalsIgnoreCase(model)) {
            return "_titan2";
        }
        if (TITAN_MODEL_NAME.equalsIgnoreCase(model)) {
            return "_titan";
        }
        return "";
    }

    private String getEffectiveModelSuffix(String keyMappingFile) {
        String modelSuffix = getDeviceModelString();

        if (!modelSuffix.isEmpty()) {
            String resourceName = "keyboard_mapping_" + keyMappingFile + modelSuffix;
            int resourceId = context.getResources().getIdentifier(
                    resourceName, "xml", context.getPackageName());

            if (resourceId == 0) {
                modelSuffix = "";
            }
        }

        return modelSuffix;
    }

    @NonNull
    private KeyboardMapping loadKeyboardMapping(
            String keyMappingFile,
            String modelSuffix
    ) throws Exception {
        KeyboardMapping baseMapping = new KeyboardMappingParser(
                context,
                keyMappingFile,
                modelSuffix
        ).parseMapping();

        return baseMapping;
    }
}
