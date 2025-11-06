package com.sinux.pocketboard.input;

import android.os.Build;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;

import androidx.annotation.RequiresApi;

/**
 * Callback for handling phone call state changes.
 * Supports both legacy PhoneStateListener (API < 31) and new TelephonyCallback (API 31+).
 */
public class CallStateCallback extends PhoneStateListener {

    private boolean calling;

    /**
     * Legacy callback for API < 31
     */
    @Override
    public void onCallStateChanged(int state, String incomingNumber) {
        calling = state == TelephonyManager.CALL_STATE_RINGING || state == TelephonyManager.CALL_STATE_OFFHOOK;
    }

    /**
     * Check if there's an active or incoming call
     */
    public boolean isCalling() {
        return calling;
    }

    /**
     * Modern TelephonyCallback implementation for API 31+
     */
    @RequiresApi(api = Build.VERSION_CODES.S)
    public static class Modern extends TelephonyCallback implements TelephonyCallback.CallStateListener {

        private boolean calling;

        @Override
        public void onCallStateChanged(int state) {
            calling = state == TelephonyManager.CALL_STATE_RINGING || state == TelephonyManager.CALL_STATE_OFFHOOK;
        }

        public boolean isCalling() {
            return calling;
        }
    }
}
