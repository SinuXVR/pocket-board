package com.sinux.pocketboard.input;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.telecom.TelecomManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.KeyEvent;

import com.sinux.pocketboard.PocketBoardIME;
import com.sinux.pocketboard.R;
import com.sinux.pocketboard.preferences.PreferencesHolder;

import java.util.function.Consumer;

public class MetaKeyManager {

    private final PocketBoardIME pocketBoardIME;
    private final PreferencesHolder preferencesHolder;
    private final long keyLongPressDuration;
    private final int sympadFixEventRepeatCount;
    private final CallStateListener callStateListener;

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
    private boolean multipleKeyJustPressed;

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

        reset();
    }

    public void destroy() {
        TelephonyManager tm = getTelephonyManager();
        if (tm != null) {
            callStateListener.unregister(tm);
        }
    }

    public boolean handleKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_SYM:
            case KeyEvent.KEYCODE_PICTSYMBOLS:
                setSymPressed(true);
                if (event.getRepeatCount() == 0 && sym == MetaKeyState.FIXED) {
                    pocketBoardIME.hideStatusIcon();
                    sym = MetaKeyState.ENABLED;
                } else if (event.getRepeatCount() == sympadFixEventRepeatCount && preferencesHolder.isLockSymPadEnabled()) {
                    sym = MetaKeyState.FIXED;
                    pocketBoardIME.showStatusIcon(R.drawable.ic_sym_pad_icon);
                }
                break;
            case KeyEvent.KEYCODE_SHIFT_LEFT:
            case KeyEvent.KEYCODE_SHIFT_RIGHT:
                if (event.getRepeatCount() == 0) {
                    if (handleShiftOnCalling()) {
                        return true;
                    }
                    setShiftPressed(true);
                    shiftPressTime = event.getEventTime();
                    if (altPressed) {
                        multipleKeyJustPressed = true;
                        // Switch to next input subtype on ALT + SHIFT
                        pocketBoardIME.switchToNextInputMethod(true);
                    }
                }
                break;
            case KeyEvent.KEYCODE_ALT_LEFT:
            case KeyEvent.KEYCODE_ALT_RIGHT:
                if (handleAltOnCalling()) {
                    return true;
                }
                setAltPressed(true);
                if (event.getRepeatCount() == 0) {
                    setAltPressed(true);
                    altPressTime = event.getEventTime();
                    if (shiftPressed) {
                        multipleKeyJustPressed = true;
                        // Switch to next input subtype on ALT + SHIFT
                        pocketBoardIME.switchToNextInputMethod(true);
                    }
                }
                break;
            default:
                return false;
        }

        return true;
    }

    public boolean handleKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_SYM:
            case KeyEvent.KEYCODE_PICTSYMBOLS:
                setSymPressed(false);
                if (sym != MetaKeyState.FIXED) {
                    sym = MetaKeyState.DISABLED;
                }
                break;
            case KeyEvent.KEYCODE_SHIFT_LEFT:
            case KeyEvent.KEYCODE_SHIFT_RIGHT:
                setShiftPressed(false);
                if (!multipleKeyJustPressed && (event.getEventTime() - shiftPressTime < keyLongPressDuration)) {
                    if (event.getEventTime() - lastShiftUpTime < keyLongPressDuration) {
                        fixShift();
                    } else {
                        toggleShiftNextState();
                    }
                }
                multipleKeyJustPressed = altPressed;
                lastShiftUpTime = event.getEventTime();
                break;
            case KeyEvent.KEYCODE_ALT_LEFT:
            case KeyEvent.KEYCODE_ALT_RIGHT:
                setAltPressed(false);
                if (!multipleKeyJustPressed && (event.getEventTime() - altPressTime < keyLongPressDuration)) {
                    if (event.getEventTime() - lastAltUpTime < keyLongPressDuration) {
                        fixAlt();
                    } else {
                        toggleAltNextState();
                    }
                }
                multipleKeyJustPressed = shiftPressed;
                lastAltUpTime = event.getEventTime();
                break;
            default:
                return false;
        }

        return true;
    }

    public boolean isSymFixed() {
        return symPressed || sym == MetaKeyState.FIXED;
    }

    public void setSymPressed(boolean symPressed) {
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

    public void fixShift() {
        setShiftState(MetaKeyState.FIXED);
    }

    public void setShiftPressed(boolean shiftPressed) {
        this.shiftPressed = shiftPressed;
        notifyMetaKeyStateChangeListener();
    }

    public boolean isShiftEnabled() {
        return shiftPressed || shift != MetaKeyState.DISABLED;
    }

    public boolean isShiftPressed() {
        return shiftPressed;
    }

    public boolean isShiftFixed() {
        return shiftPressed || shift == MetaKeyState.FIXED;
    }

    public void toggleShiftNextState() {
        setShiftState(shift.next());
    }

    public void updateShift() {
        if (shift == MetaKeyState.ENABLED) {
            setShiftState(MetaKeyState.DISABLED);
        }
    }

    public void fixAlt() {
        setAltState(MetaKeyState.FIXED);
    }

    public void setAltPressed(boolean altPressed) {
        this.altPressed = altPressed;
        notifyMetaKeyStateChangeListener();
    }

    public boolean isAltEnabled() {
        return altPressed || alt != MetaKeyState.DISABLED;
    }

    public boolean isAltFixed() {
        return altPressed || alt == MetaKeyState.FIXED;
    }

    public void toggleAltNextState() {
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
