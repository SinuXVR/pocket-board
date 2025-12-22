package com.sinux.pocketboard.input;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;

import androidx.annotation.RequiresApi;

import com.sinux.pocketboard.PocketBoardIME;

public final class CallStateListener {

    private final CallListener listenerDelegate;

    public CallStateListener() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listenerDelegate = new Sdk35CallStateListener();
        } else {
            listenerDelegate = new LegacyCallStateListener();
        }
    }

    public void register(PocketBoardIME pocketBoardIME, TelephonyManager telephonyManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (pocketBoardIME.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                telephonyManager.registerTelephonyCallback(pocketBoardIME.getMainExecutor(), (Sdk35CallStateListener) listenerDelegate);
            }
        } else {
            telephonyManager.listen((LegacyCallStateListener) listenerDelegate, PhoneStateListener.LISTEN_CALL_STATE);
        }
    }

    public void unregister(TelephonyManager telephonyManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyManager.unregisterTelephonyCallback((Sdk35CallStateListener) listenerDelegate);
        } else {
            telephonyManager.listen((LegacyCallStateListener) listenerDelegate, PhoneStateListener.LISTEN_NONE);
        }
    }

    public boolean isCalling() {
        return listenerDelegate.isCalling();
    }

    /**
     * Common interface
     */
    private interface CallListener {

        boolean isCalling();

    }

    /**
     * Legacy API < 35 level implementation
     */
    private static final class LegacyCallStateListener extends PhoneStateListener implements CallListener {

        private boolean calling;

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            calling = state == TelephonyManager.CALL_STATE_RINGING || state == TelephonyManager.CALL_STATE_OFFHOOK;
        }

        @Override
        public boolean isCalling() {
            return calling;
        }
    }

    /**
     * Current API >= 35 level implementation
     */
    @RequiresApi(api = Build.VERSION_CODES.S)
    private static final class Sdk35CallStateListener extends TelephonyCallback implements TelephonyCallback.CallStateListener, CallListener {

        private boolean calling;

        @Override
        public void onCallStateChanged(int state) {
            calling = state == TelephonyManager.CALL_STATE_RINGING || state == TelephonyManager.CALL_STATE_OFFHOOK;
        }

        @Override
        public boolean isCalling() {
            return calling;
        }
    }
}
