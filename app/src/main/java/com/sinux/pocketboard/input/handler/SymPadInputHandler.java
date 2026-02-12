package com.sinux.pocketboard.input.handler;

import android.content.Context;
import android.media.AudioManager;
import android.os.Build;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.inputmethod.InputConnection;

import com.sinux.pocketboard.PocketBoardIME;

import java.util.LinkedHashSet;

public class SymPadInputHandler extends ProxyInputHandler {

    private final PocketBoardIME pocketBoardIME;
    private final LinkedHashSet<Integer> pressedOriginalKeyCodes;

    private AudioManager audioManager;
    private boolean isLegacyShiftPressed;

    public SymPadInputHandler(PocketBoardIME pocketBoardIME) {
        super(pocketBoardIME);
        this.pocketBoardIME = pocketBoardIME;
        pressedOriginalKeyCodes = new LinkedHashSet<>(4);
    }

    public boolean hasPressedKey(int originalKeyCode) {
        return pressedOriginalKeyCodes.contains(originalKeyCode);
    }

    @Override
    protected int translateShortPressKeyCode(int keyCode) {
        return switch (keyCode) {
            // R, U - Home, F, J - End (text navigation)
            case KeyEvent.KEYCODE_R, KeyEvent.KEYCODE_U -> KeyEvent.KEYCODE_MOVE_HOME;
            case KeyEvent.KEYCODE_F, KeyEvent.KEYCODE_J -> KeyEvent.KEYCODE_MOVE_END;
            // Q, W, E, A, S, D, Z, X, C or I, O, P, K, L, DEL, N, M, ENTER - 9-positional D-pad
            case KeyEvent.KEYCODE_Q, KeyEvent.KEYCODE_I -> KeyEvent.KEYCODE_DPAD_UP_LEFT;
            case KeyEvent.KEYCODE_W, KeyEvent.KEYCODE_O -> KeyEvent.KEYCODE_DPAD_UP;
            case KeyEvent.KEYCODE_E, KeyEvent.KEYCODE_P -> KeyEvent.KEYCODE_DPAD_UP_RIGHT;
            case KeyEvent.KEYCODE_A, KeyEvent.KEYCODE_K -> KeyEvent.KEYCODE_DPAD_LEFT;
            case KeyEvent.KEYCODE_S, KeyEvent.KEYCODE_L -> KeyEvent.KEYCODE_DPAD_CENTER;
            case KeyEvent.KEYCODE_D, KeyEvent.KEYCODE_DEL -> KeyEvent.KEYCODE_DPAD_RIGHT;
            case KeyEvent.KEYCODE_Z, KeyEvent.KEYCODE_N -> KeyEvent.KEYCODE_DPAD_DOWN_LEFT;
            case KeyEvent.KEYCODE_X, KeyEvent.KEYCODE_M -> KeyEvent.KEYCODE_DPAD_DOWN;
            case KeyEvent.KEYCODE_C, KeyEvent.KEYCODE_ENTER -> KeyEvent.KEYCODE_DPAD_DOWN_RIGHT;
            // V - Prev, Space - Play/Pause, B - Next (media navigation)
            case KeyEvent.KEYCODE_V -> KeyEvent.KEYCODE_MEDIA_PREVIOUS;
            case KeyEvent.KEYCODE_SPACE -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE;
            case KeyEvent.KEYCODE_B -> KeyEvent.KEYCODE_MEDIA_NEXT;
            default -> 0;
        };
    }

    @Override
    protected int translateLongPressKeyCode(int keyCode) {
        return switch (keyCode) {
            // V - Rewind, B - Fast forward (media navigation)
            case KeyEvent.KEYCODE_V -> KeyEvent.KEYCODE_MEDIA_REWIND;
            case KeyEvent.KEYCODE_B -> KeyEvent.KEYCODE_MEDIA_FAST_FORWARD;
            default -> 0;
        };
    }

    @Override
    protected void handleKeyDownInternal(int translatedKeyCode, KeyEvent originalEvent, InputConnection inputConnection) {
        pressedOriginalKeyCodes.add(originalEvent.getKeyCode());
        if (isPlaybackKey(translatedKeyCode)) {
            dispatchMediaKeyEvent(translatedKeyCode, originalEvent, KeyEvent.ACTION_DOWN);
        } else {
            // TouchMode issue workaround for android < 12L
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S && originalEvent.isShiftPressed() && !isLegacyShiftPressed) {
                isLegacyShiftPressed = true;
                inputConnection.sendKeyEvent(createNewKeyEvent(KeyEvent.KEYCODE_SHIFT_LEFT, originalEvent, KeyEvent.ACTION_DOWN, InputDevice.SOURCE_KEYBOARD));
            }
            inputConnection.sendKeyEvent(createNewKeyEvent(translatedKeyCode, originalEvent, KeyEvent.ACTION_DOWN, InputDevice.SOURCE_DPAD));
        }
    }

    @Override
    protected void handleKeyUpInternal(int translatedKeyCode, KeyEvent originalEvent, InputConnection inputConnection) {
        pressedOriginalKeyCodes.remove(originalEvent.getKeyCode());
        if (isPlaybackKey(translatedKeyCode)) {
            dispatchMediaKeyEvent(translatedKeyCode, originalEvent, KeyEvent.ACTION_UP);
        } else {
            inputConnection.sendKeyEvent(createNewKeyEvent(translatedKeyCode, originalEvent, KeyEvent.ACTION_UP, InputDevice.SOURCE_DPAD));
            // TouchMode issue workaround for android < 12L
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S && (originalEvent.isShiftPressed() || isLegacyShiftPressed)) {
                isLegacyShiftPressed = false;
                inputConnection.sendKeyEvent(createNewKeyEvent(KeyEvent.KEYCODE_SHIFT_LEFT, originalEvent, KeyEvent.ACTION_UP, InputDevice.SOURCE_KEYBOARD));
            }
        }
    }

    @Override
    protected void handleKeyLongPressInternal(int translatedKeyCode, KeyEvent originalEvent, InputConnection inputConnection) {
        pressedOriginalKeyCodes.add(originalEvent.getKeyCode());
        if (isPlaybackKey(translatedKeyCode)) {
            dispatchMediaKeyEvent(translatedKeyCode, originalEvent, KeyEvent.ACTION_DOWN);
        }
    }

    private void dispatchMediaKeyEvent(int translatedKeyCode, KeyEvent originalEvent, int action) {
        AudioManager audioManager = getAudioManager();
        if (audioManager != null) {
            audioManager.dispatchMediaKeyEvent(createNewKeyEvent(translatedKeyCode, originalEvent, action, InputDevice.SOURCE_KEYBOARD));
        }
    }

    private AudioManager getAudioManager() {
        if (audioManager == null) {
            audioManager = (AudioManager) pocketBoardIME.getSystemService(Context.AUDIO_SERVICE);
        }
        return audioManager;
    }

    private static boolean isPlaybackKey(int keyCode) {
        return switch (keyCode) {
            // There are many media keys, but we only need these
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_MEDIA_PREVIOUS,
                 KeyEvent.KEYCODE_MEDIA_NEXT, KeyEvent.KEYCODE_MEDIA_REWIND,
                 KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> true;
            default -> false;
        };
    }
}
