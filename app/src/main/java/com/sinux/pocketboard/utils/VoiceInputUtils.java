package com.sinux.pocketboard.utils;

import android.content.Context;
import android.inputmethodservice.InputMethodService;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;

import com.sinux.pocketboard.R;

import java.util.Collection;
import java.util.Set;

public class VoiceInputUtils {

    private static final Collection<String> VOICE_IME_PACKAGES = Set.of(
            "com.google.android.tts",
            "com.google.android.googlequicksearchbox"
    );

    public static void launchVoiceIME(InputMethodService inputMethodService) {
        InputMethodManager imm = (InputMethodManager) inputMethodService.getSystemService(Context.INPUT_METHOD_SERVICE);

        for (InputMethodInfo imi : imm.getInputMethodList()) {
            if (VOICE_IME_PACKAGES.contains(imi.getPackageName())) {
                inputMethodService.switchInputMethod(imi.getId());
                return;
            }
        }

        ToastMessageUtils.showMessage(inputMethodService, R.string.voice_ime_not_configured);
    }
}
