package com.sinux.pocketboard.input.handler;

import android.content.Context;
import android.media.AudioManager;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.inputmethod.InputConnection;

import com.sinux.pocketboard.PocketBoardIME;

public class SymPadInputHandler extends ProxyInputHandler {

    private final PocketBoardIME pocketBoardIME;

    public SymPadInputHandler(PocketBoardIME pocketBoardIME) {
        super(pocketBoardIME);
        this.pocketBoardIME = pocketBoardIME;
    }

    @Override
    protected int translateShortPressKeyCode(int keyCode) {
        switch (keyCode) {
            // R, U - Home, F, J - End (text navigation)
            case KeyEvent.KEYCODE_R:
            case KeyEvent.KEYCODE_U:
                return KeyEvent.KEYCODE_MOVE_HOME;
            case KeyEvent.KEYCODE_F:
            case KeyEvent.KEYCODE_J:
                return KeyEvent.KEYCODE_MOVE_END;
            // Q, W, E, A, S, D, Z, X, C or I, O, P, K, L, DEL, N, M, ENTER - 9-positional D-pad
            case KeyEvent.KEYCODE_Q:
            case KeyEvent.KEYCODE_I:
                return KeyEvent.KEYCODE_DPAD_UP_LEFT;
            case KeyEvent.KEYCODE_W:
            case KeyEvent.KEYCODE_O:
                return KeyEvent.KEYCODE_DPAD_UP;
            case KeyEvent.KEYCODE_E:
            case KeyEvent.KEYCODE_P:
                return KeyEvent.KEYCODE_DPAD_UP_RIGHT;
            case KeyEvent.KEYCODE_A:
            case KeyEvent.KEYCODE_K:
                return KeyEvent.KEYCODE_DPAD_LEFT;
            case KeyEvent.KEYCODE_S:
            case KeyEvent.KEYCODE_L:
                return KeyEvent.KEYCODE_DPAD_CENTER;
            case KeyEvent.KEYCODE_D:
            case KeyEvent.KEYCODE_DEL:
                return KeyEvent.KEYCODE_DPAD_RIGHT;
            case KeyEvent.KEYCODE_Z:
            case KeyEvent.KEYCODE_N:
                return KeyEvent.KEYCODE_DPAD_DOWN_LEFT;
            case KeyEvent.KEYCODE_X:
            case KeyEvent.KEYCODE_M:
                return KeyEvent.KEYCODE_DPAD_DOWN;
            case KeyEvent.KEYCODE_C:
            case KeyEvent.KEYCODE_ENTER:
                return KeyEvent.KEYCODE_DPAD_DOWN_RIGHT;
            // V - Prev, Space - Play/Pause, B - Next (media navigation)
            case KeyEvent.KEYCODE_V:
                return KeyEvent.KEYCODE_MEDIA_PREVIOUS;
            case KeyEvent.KEYCODE_SPACE:
                return KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE;
            case KeyEvent.KEYCODE_B:
                return KeyEvent.KEYCODE_MEDIA_NEXT;
        }

        return 0;
    }

    @Override
    protected int translateLongPressKeyCode(int keyCode) {
        switch (keyCode) {
            // V - Rewind, B - Fast forward (media navigation)
            case KeyEvent.KEYCODE_V:
                return KeyEvent.KEYCODE_MEDIA_REWIND;
            case KeyEvent.KEYCODE_B:
                return KeyEvent.KEYCODE_MEDIA_FAST_FORWARD;
        }

        return 0;
    }

    @Override
    protected void handleKeyPress(int translatedKeyCode, KeyEvent originalEvent,
                                  InputConnection inputConnection, boolean shiftEnabled, boolean altEnabled) {
        if (isPlaybackKey(translatedKeyCode)) {
            AudioManager audioManager = (AudioManager)pocketBoardIME.getSystemService(Context.AUDIO_SERVICE);
            if (audioManager != null) {
                audioManager.dispatchMediaKeyEvent(createNewKeyEvent(translatedKeyCode, originalEvent, KeyEvent.ACTION_DOWN, InputDevice.SOURCE_KEYBOARD));
                audioManager.dispatchMediaKeyEvent(createNewKeyEvent(translatedKeyCode, originalEvent, KeyEvent.ACTION_UP, InputDevice.SOURCE_KEYBOARD));
            }
        } else {
            if (originalEvent.isShiftPressed()) {
                inputConnection.sendKeyEvent(createNewKeyEvent(KeyEvent.KEYCODE_SHIFT_LEFT, originalEvent, KeyEvent.ACTION_DOWN, InputDevice.SOURCE_KEYBOARD));
            }
            inputConnection.sendKeyEvent(createNewKeyEvent(translatedKeyCode, originalEvent, KeyEvent.ACTION_DOWN, InputDevice.SOURCE_DPAD));
            inputConnection.sendKeyEvent(createNewKeyEvent(translatedKeyCode, originalEvent, KeyEvent.ACTION_UP, InputDevice.SOURCE_DPAD));
            if (originalEvent.isShiftPressed()) {
                inputConnection.sendKeyEvent(createNewKeyEvent(KeyEvent.KEYCODE_SHIFT_LEFT, originalEvent, KeyEvent.ACTION_UP, InputDevice.SOURCE_KEYBOARD));
            }
        }
    }

    public static boolean isPlaybackKey(int keyCode) {
        switch (keyCode) {
            // There are many media keys, but we only need these
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
            case KeyEvent.KEYCODE_MEDIA_NEXT:
            case KeyEvent.KEYCODE_MEDIA_REWIND:
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                return true;
        }

        return false;
    }
}
