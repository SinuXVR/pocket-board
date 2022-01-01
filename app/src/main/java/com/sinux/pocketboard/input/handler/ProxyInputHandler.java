package com.sinux.pocketboard.input.handler;

import android.view.InputDevice;
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
                handleKeyPress(shortKeyCode, event, inputConnection, shiftEnabled, altEnabled);
                return true;
            }
        } else if (longKeyCode != 0 && (event.getEventTime() - lastKeyDownTime > keyLongPressDuration)) {
            handleKeyPress(longKeyCode, event, inputConnection, shiftEnabled, altEnabled);
            return true;
        } else {
            handleKeyPress(shortKeyCode, event, inputConnection, shiftEnabled, altEnabled);
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
        if (longKeyCode != 0 && (event.getEventTime() - lastKeyDownTime <= keyLongPressDuration)) {
            handleKeyPress(shortKeyCode, event, inputConnection, shiftEnabled, altEnabled);
        }

        return true;
    }

    protected abstract int translateShortPressKeyCode(int keyCode);

    protected int translateLongPressKeyCode(int keyCode) {
        return 0;
    }

    protected void handleKeyPress(int translatedKeyCode, KeyEvent originalEvent,
                                  InputConnection inputConnection, boolean shiftEnabled, boolean altEnabled) {
        inputConnection.sendKeyEvent(createNewKeyEvent(translatedKeyCode, originalEvent, KeyEvent.ACTION_DOWN, InputDevice.SOURCE_KEYBOARD));
        inputConnection.sendKeyEvent(createNewKeyEvent(translatedKeyCode, originalEvent, KeyEvent.ACTION_UP, InputDevice.SOURCE_KEYBOARD));
    }

    protected KeyEvent createNewKeyEvent(int translatedKeyCode, KeyEvent originalEvent, int action, int source) {
        int shiftState = originalEvent.getMetaState() & KeyEvent.META_SHIFT_ON;
        return new KeyEvent(originalEvent.getDownTime(), originalEvent.getEventTime(),
                action, translatedKeyCode, originalEvent.getRepeatCount(), shiftState,
                KeyCharacterMap.VIRTUAL_KEYBOARD, translatedKeyCode, 0, source);
    }
}
