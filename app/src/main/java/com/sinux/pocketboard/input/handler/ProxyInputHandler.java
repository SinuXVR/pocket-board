package com.sinux.pocketboard.input.handler;

import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.inputmethod.InputConnection;

import com.sinux.pocketboard.PocketBoardIME;

public abstract class ProxyInputHandler implements InputHandler {

    private final long keyLongPressDuration;

    private long lastKeyDownTime;

    public ProxyInputHandler(PocketBoardIME pocketBoardIME) {
        keyLongPressDuration = pocketBoardIME.getPreferencesHolder().getLongKeyPressDuration();
    }

    @Override
    public boolean handleKeyDown(int keyCode, KeyEvent event, InputConnection inputConnection, boolean shiftEnabled, boolean altEnabled) {
        int shortKeyCode = translateShortPressKeyCode(keyCode);
        if (shortKeyCode == 0) {
            return false;
        }

        int longKeyCode = translateLongPressKeyCode(keyCode);
        if (event.getRepeatCount() == 0) {
            lastKeyDownTime = event.getEventTime();
            if (longKeyCode == 0) {
                handleKeyDownInternal(shortKeyCode, event, inputConnection);
                return true;
            }
        } else if (longKeyCode != 0 && (event.getEventTime() - lastKeyDownTime > keyLongPressDuration)) {
            handleKeyLongPressInternal(longKeyCode, event, inputConnection);
            return true;
        } else {
            handleKeyDownInternal(shortKeyCode, event, inputConnection);
            return true;
        }

        return false;
    }

    @Override
    public boolean handleKeyUp(int keyCode, KeyEvent event, InputConnection inputConnection,
                               boolean shiftEnabled, boolean altEnabled) {
        int shortKeyCode = translateShortPressKeyCode(keyCode);
        if (shortKeyCode == 0) {
            return false;
        }

        int longKeyCode = translateLongPressKeyCode(keyCode);
        if (longKeyCode != 0) {
            if ((event.getEventTime() - lastKeyDownTime <= keyLongPressDuration)) {
                handleKeyDownInternal(shortKeyCode, event, inputConnection);
                handleKeyUpInternal(shortKeyCode, event, inputConnection);
            } else {
                handleKeyUpInternal(longKeyCode, event, inputConnection);
            }
            return true;
        }

        handleKeyUpInternal(shortKeyCode, event, inputConnection);

        return true;
    }

    protected abstract int translateShortPressKeyCode(int keyCode);

    protected int translateLongPressKeyCode(int keyCode) {
        return 0;
    }

    protected abstract void handleKeyDownInternal(int translatedKeyCode, KeyEvent originalEvent, InputConnection inputConnection);

    protected abstract void handleKeyUpInternal(int translatedKeyCode, KeyEvent originalEvent, InputConnection inputConnection);

    protected abstract void handleKeyLongPressInternal(int translatedKeyCode, KeyEvent originalEvent, InputConnection inputConnection);

    protected KeyEvent createNewKeyEvent(int translatedKeyCode, KeyEvent originalEvent, int action, int source) {
        int shiftState = originalEvent.getMetaState() & KeyEvent.META_SHIFT_ON;
        return new KeyEvent(originalEvent.getDownTime(), originalEvent.getEventTime(),
                action, translatedKeyCode, originalEvent.getRepeatCount(), shiftState,
                KeyCharacterMap.VIRTUAL_KEYBOARD, translatedKeyCode, 0, source);
    }
}
