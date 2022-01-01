package com.sinux.pocketboard.input.handler;

import android.view.KeyEvent;
import android.view.inputmethod.InputConnection;

public interface InputHandler {

    boolean handleKeyDown(int keyCode, KeyEvent event, InputConnection inputConnection,
                          boolean shiftEnabled, boolean altEnabled);

    boolean handleKeyUp(int keyCode, KeyEvent event, InputConnection inputConnection,
                        boolean shiftEnabled, boolean altEnabled);

}
