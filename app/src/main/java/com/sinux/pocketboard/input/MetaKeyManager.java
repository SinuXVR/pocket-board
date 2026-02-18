package com.sinux.pocketboard.input;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.telecom.TelecomManager;
import android.telephony.TelephonyManager;
import android.view.KeyEvent;
import android.view.inputmethod.InputConnection;

import androidx.annotation.Nullable;

import com.sinux.pocketboard.PocketBoardIME;
import com.sinux.pocketboard.R;
import com.sinux.pocketboard.preferences.PreferencesHolder;
import com.sinux.pocketboard.utils.InputUtils;
import com.sinux.pocketboard.utils.VoiceInputUtils;

import java.util.LinkedHashSet;

public class MetaKeyManager {

    private final PocketBoardIME pocketBoardIME;
    private final PreferencesHolder preferencesHolder;
    private final long keyLongPressDuration;
    private final int sympadFixEventRepeatCount;
    private final CallStateListener callStateListener;
    private final LinkedHashSet<Integer> shortcutKeysUsed;

    private MetaKeyState sym;
    private boolean symPressed;
    private MetaKeyState shift;
    private boolean shiftPressed;
    private long shiftPressTime;
    private long lastShiftUpTime;
    private MetaKeyState alt;
    private long altPressTime;
    private long lastAltUpTime;
    private boolean altPressed;

    private MetaKeyStateChangeListener metaKeyStateChangeListener;

    public MetaKeyManager(PocketBoardIME pocketBoardIME) {
        this.pocketBoardIME = pocketBoardIME;
        this.preferencesHolder = pocketBoardIME.getPreferencesHolder();
        keyLongPressDuration = pocketBoardIME.getPreferencesHolder().getLongKeyPressDuration();
        sympadFixEventRepeatCount = pocketBoardIME.getResources().getInteger(R.integer.sympad_fix_event_repeat_count);

        callStateListener = new CallStateListener();
        TelephonyManager tm = getTelephonyManager();
        if (tm != null) {
            callStateListener.register(pocketBoardIME, tm);
        }

        preferencesHolder.registerPreferenceChangeListener(
                pocketBoardIME.getString(R.string.ime_extra_phone_control_prefs_key),
                value -> {
                    if (tm != null) {
                        if (Boolean.TRUE.equals(value)) {
                            callStateListener.register(pocketBoardIME, tm);
                        } else {
                            callStateListener.unregister(tm);
                        }
                    }
                }
        );

        shortcutKeysUsed = new LinkedHashSet<>(3);

        reset();
    }

    public void destroy() {
        TelephonyManager tm = getTelephonyManager();
        if (tm != null) {
            callStateListener.unregister(tm);
        }
    }

    public boolean handleKeyDown(int keyCode, KeyEvent event, @Nullable InputConnection inputConnection) {
        return switch (keyCode) {
            case KeyEvent.KEYCODE_SYM, KeyEvent.KEYCODE_PICTSYMBOLS -> {
                handleSymDown(event);
                yield true;
            }
            case KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT -> {
                handleShiftDown(event);
                yield true;
            }
            case KeyEvent.KEYCODE_ALT_LEFT, KeyEvent.KEYCODE_ALT_RIGHT -> {
                handleAltDown(event);
                yield true;
            }
            case KeyEvent.KEYCODE_CTRL_LEFT, KeyEvent.KEYCODE_CTRL_RIGHT -> {
                handleCtrlDown(event);
                yield false;
            }
            // Translate Shift+Backspace to Ctrl+Backspace
            case KeyEvent.KEYCODE_DEL -> handleShiftShortcutDown(event, KeyEvent.KEYCODE_DEL, KeyEvent.KEYCODE_CTRL_LEFT, KeyEvent.META_CTRL_ON, inputConnection);
            // Pass Shift+Enter
            case KeyEvent.KEYCODE_ENTER -> handleShiftShortcutDown(event, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.META_SHIFT_ON, inputConnection);
            default -> false;
        };
    }

    private void handleSymDown(KeyEvent event) {
        setSymPressed(true);

        if (event.getRepeatCount() == 0 && sym == MetaKeyState.FIXED) {
            sym = MetaKeyState.ENABLED;
        } else if (event.getRepeatCount() == sympadFixEventRepeatCount && preferencesHolder.isLockSymPadEnabled()) {
            sym = MetaKeyState.FIXED;
            pocketBoardIME.showStatusIcon(R.drawable.ic_sym_pad_icon);
        }
    }

    private void handleShiftDown(KeyEvent event) {
        if (event.getRepeatCount() != 0)
            return;

        if (handleShiftOnCalling())
            return;

        if (event.isCtrlPressed()) {
            shortcutKeysUsed.add(KeyEvent.KEYCODE_SHIFT_LEFT);
            return;
        }

        setShiftPressed(true);
        shiftPressTime = event.getEventTime();

        // Switch to next input subtype on ALT + SHIFT
        if (event.isAltPressed()) {
            shortcutKeysUsed.add(KeyEvent.KEYCODE_SHIFT_LEFT);
            shortcutKeysUsed.add(KeyEvent.KEYCODE_ALT_LEFT);
            pocketBoardIME.switchToNextInputMethod(true);
        }
    }

    private void handleAltDown(KeyEvent event) {
        if (event.getRepeatCount() != 0)
            return;

        if (handleAltOnCalling())
            return;

        if (event.isCtrlPressed()) {
            shortcutKeysUsed.add(KeyEvent.KEYCODE_ALT_LEFT);
            launchVoiceIME();
            return;
        }

        setAltPressed(true);
        altPressTime = event.getEventTime();

        // Switch to next input subtype on ALT + SHIFT
        if (event.isShiftPressed()) {
            shortcutKeysUsed.add(KeyEvent.KEYCODE_SHIFT_LEFT);
            shortcutKeysUsed.add(KeyEvent.KEYCODE_ALT_LEFT);
            pocketBoardIME.switchToNextInputMethod(true);
        }
    }

    private void handleCtrlDown(KeyEvent event) {
        if (event.getRepeatCount() != 0)
            return;

        if (event.isAltPressed()) {
            shortcutKeysUsed.add(KeyEvent.KEYCODE_ALT_LEFT);
            launchVoiceIME();
        }
    }

    private boolean handleShiftShortcutDown(KeyEvent event, int targetKeyCode, int metaKeyCode, int metaState, InputConnection inputConnection) {
        if (!event.isShiftPressed() || isSymFixed() || inputConnection == null)
            return false;

        if (event.getRepeatCount() == 0) {
            shortcutKeysUsed.add(targetKeyCode);
            inputConnection.sendKeyEvent(InputUtils.translateKeyEvent(event, metaKeyCode, KeyEvent.ACTION_DOWN, metaState));
        }

        inputConnection.sendKeyEvent(InputUtils.translateKeyEvent(event, targetKeyCode, KeyEvent.ACTION_DOWN, metaState));

        return true;
    }

    private void launchVoiceIME() {
        if (preferencesHolder.isVoiceInputShortcutEnabled() && pocketBoardIME.isInputViewShown()) {
            VoiceInputUtils.launchVoiceIME(pocketBoardIME);
        }
    }

    public boolean handleKeyUp(int keyCode, KeyEvent event, @Nullable InputConnection inputConnection) {
        return switch (keyCode) {
            case KeyEvent.KEYCODE_SYM, KeyEvent.KEYCODE_PICTSYMBOLS -> {
                handleSymUp();
                yield true;
            }
            case KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT -> {
                handleShiftUp(event);
                yield true;
            }
            case KeyEvent.KEYCODE_ALT_LEFT, KeyEvent.KEYCODE_ALT_RIGHT -> {
                handleAltUp(event);
                yield true;
            }
            case KeyEvent.KEYCODE_DEL -> handleShiftShortcutKeyUp(event, KeyEvent.KEYCODE_DEL, KeyEvent.KEYCODE_CTRL_LEFT, KeyEvent.META_CTRL_ON, inputConnection);
            case KeyEvent.KEYCODE_ENTER -> handleShiftShortcutKeyUp(event, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.META_SHIFT_ON, inputConnection);
            default -> false;
        };
    }

    private void handleSymUp() {
        setSymPressed(false);

        if (sym != MetaKeyState.FIXED) {
            pocketBoardIME.hideStatusIcon();
            sym = MetaKeyState.DISABLED;
        }
    }

    private void handleShiftUp(KeyEvent event) {
        boolean shortcutUsed = shortcutKeysUsed.remove(KeyEvent.KEYCODE_SHIFT_LEFT);

        setShiftPressed(false);

        if (!shortcutUsed && (event.getEventTime() - shiftPressTime < keyLongPressDuration)) {
            if (event.getEventTime() - lastShiftUpTime < keyLongPressDuration) {
                fixShift();
            } else {
                toggleShiftNextState();
            }
        }

        lastShiftUpTime = event.getEventTime();
    }

    private void handleAltUp(KeyEvent event) {
        boolean shortcutUsed = shortcutKeysUsed.remove(KeyEvent.KEYCODE_ALT_LEFT);

        setAltPressed(false);

        if (!shortcutUsed && (event.getEventTime() - altPressTime < keyLongPressDuration)) {
            if (event.getEventTime() - lastAltUpTime < keyLongPressDuration) {
                fixAlt();
            } else {
                toggleAltNextState();
            }
        }

        lastAltUpTime = event.getEventTime();
    }

    private boolean handleShiftShortcutKeyUp(KeyEvent event, int targetKeyCode, int metaKeyCode, int metaState, InputConnection inputConnection) {
        if (inputConnection == null || isSymFixed()) {
            return false;
        }

        if (shortcutKeysUsed.remove(targetKeyCode)) {
            inputConnection.sendKeyEvent(InputUtils.translateKeyEvent(event, targetKeyCode, KeyEvent.ACTION_UP, metaState));
            inputConnection.sendKeyEvent(InputUtils.translateKeyEvent(event, metaKeyCode, KeyEvent.ACTION_UP, 0));
            return true;
        }

        return false;
    }

    public boolean isSymFixed() {
        return symPressed || sym == MetaKeyState.FIXED;
    }

    private void setSymPressed(boolean symPressed) {
        this.symPressed = symPressed;
        notifyMetaKeyStateChangeListener();
    }

    public void enableShift() {
        if (shift == MetaKeyState.DISABLED) {
            setShiftState(MetaKeyState.ENABLED);
        }
    }

    public void disableShift() {
        if (shift == MetaKeyState.ENABLED) {
            setShiftState(MetaKeyState.DISABLED);
        }
    }

    private void fixShift() {
        setShiftState(MetaKeyState.FIXED);
    }

    private void setShiftPressed(boolean shiftPressed) {
        this.shiftPressed = shiftPressed;
        notifyMetaKeyStateChangeListener();
    }

    public boolean isShiftEnabled() {
        return shiftPressed || shift != MetaKeyState.DISABLED;
    }

    public boolean isShiftFixed() {
        return shiftPressed || shift == MetaKeyState.FIXED;
    }

    private void toggleShiftNextState() {
        setShiftState(shift.next());
    }

    public void updateShift() {
        if (shift == MetaKeyState.ENABLED) {
            setShiftState(MetaKeyState.DISABLED);
        }
    }

    private void fixAlt() {
        setAltState(MetaKeyState.FIXED);
    }

    private void setAltPressed(boolean altPressed) {
        this.altPressed = altPressed;
        notifyMetaKeyStateChangeListener();
    }

    public boolean isAltEnabled() {
        return altPressed || alt != MetaKeyState.DISABLED;
    }

    public boolean isAltFixed() {
        return altPressed || alt == MetaKeyState.FIXED;
    }

    private void toggleAltNextState() {
        setAltState(alt.next());
    }

    public void updateAlt() {
        if (alt == MetaKeyState.ENABLED) {
            setAltState(MetaKeyState.DISABLED);
        }
    }

    public void reset() {
        setShiftState(MetaKeyState.DISABLED);
        setAltState(MetaKeyState.DISABLED);
    }

    private void setShiftState(MetaKeyState shiftState) {
        if (!shiftPressed) {
            shift = shiftState;
            notifyMetaKeyStateChangeListener();
        }
    }

    private void setAltState(MetaKeyState altState) {
        if (!altPressed) {
            alt = altState;
            notifyMetaKeyStateChangeListener();
        }
    }

    public void setMetaKeyStateChangeListener(MetaKeyStateChangeListener metaKeyStateChangeListener) {
        this.metaKeyStateChangeListener = metaKeyStateChangeListener;
        notifyMetaKeyStateChangeListener();
    }

    private void notifyMetaKeyStateChangeListener() {
        if (metaKeyStateChangeListener != null) {
            metaKeyStateChangeListener.onMetaKeyStateChanged(this);
        }
    }

    private boolean handleShiftOnCalling() {
        // Accept calls using SHIFT key
        if (callStateListener.isCalling() && preferencesHolder.isPhoneControlEnabled()) {
            TelecomManager tm = getTelecomManager();
            if (tm != null && pocketBoardIME.checkSelfPermission(Manifest.permission.ANSWER_PHONE_CALLS) == PackageManager.PERMISSION_GRANTED) {
                tm.acceptRingingCall();
                return true;
            }
        }
        return false;
    }

    private boolean handleAltOnCalling() {
        // End calls using ALT key
        if (callStateListener.isCalling() && preferencesHolder.isPhoneControlEnabled()) {
            TelecomManager tm = getTelecomManager();
            if (tm != null && pocketBoardIME.checkSelfPermission(Manifest.permission.ANSWER_PHONE_CALLS) == PackageManager.PERMISSION_GRANTED) {
                tm.endCall();
                return true;
            }
        }
        return false;
    }

    private TelephonyManager getTelephonyManager() {
        return (TelephonyManager) pocketBoardIME.getSystemService(Context.TELEPHONY_SERVICE);
    }

    private TelecomManager getTelecomManager() {
        return (TelecomManager) pocketBoardIME.getSystemService(Context.TELECOM_SERVICE);
    }
}
