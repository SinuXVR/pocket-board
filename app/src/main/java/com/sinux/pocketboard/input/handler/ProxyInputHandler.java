package com.sinux.pocketboard.input.handler;

import android.view.KeyEvent;
import android.view.inputmethod.InputConnection;

import com.sinux.pocketboard.PocketBoardIME;

import java.util.LinkedHashSet;

public abstract class ProxyInputHandler implements InputHandler {

    private final long keyLongPressDuration;

    private long lastKeyDownTime;
    private final LinkedHashSet<Integer> pressedOriginalKeyCodes;

    public ProxyInputHandler(PocketBoardIME pocketBoardIME) {
        keyLongPressDuration = pocketBoardIME.getPreferencesHolder().getLongKeyPressDuration();
        pressedOriginalKeyCodes = new LinkedHashSet<>(4);
    }

    public boolean hasPressedKey(int originalKeyCode) {
        return pressedOriginalKeyCodes.contains(originalKeyCode);
    }

    @Override
    public boolean handleKeyDown(int keyCode, KeyEvent event, InputConnection inputConnection, boolean shiftEnabled, boolean altEnabled) {
        pressedOriginalKeyCodes.add(event.getKeyCode());
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
        if (!pressedOriginalKeyCodes.remove(event.getKeyCode()))
            return false;

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
}
