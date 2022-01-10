package com.sinux.pocketboard.input;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.telecom.TelecomManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.KeyEvent;

import com.sinux.pocketboard.PocketBoardIME;
import com.sinux.pocketboard.preferences.PreferencesHolder;

public class MetaKeyManager {

    private final PocketBoardIME pocketBoardIME;
    private final PreferencesHolder preferencesHolder;
    private final long keyLongPressDuration;
    private final CallStateCallback callStateCallback;

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

        callStateCallback = new CallStateCallback();
        TelephonyManager tm = getTelephonyManager();
        if (tm != null) {
            tm.listen(callStateCallback, PhoneStateListener.LISTEN_CALL_STATE);
        }

        reset();
    }

    public void destroy() {
        TelephonyManager tm = getTelephonyManager();
        if (tm != null) {
            tm.listen(callStateCallback, PhoneStateListener.LISTEN_NONE);
        }
    }

    public boolean handleKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
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
        if (callStateCallback.isCalling() && preferencesHolder.isPhoneControlEnabled()) {
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
        if (callStateCallback.isCalling() && preferencesHolder.isPhoneControlEnabled()) {
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
