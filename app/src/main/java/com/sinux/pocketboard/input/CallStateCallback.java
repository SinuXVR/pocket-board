package com.sinux.pocketboard.input;

import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

public class CallStateCallback extends PhoneStateListener {

    private boolean calling;

    public void onCallStateChanged(int state, String incomingNumber) {
        calling = state == TelephonyManager.CALL_STATE_RINGING || state == TelephonyManager.CALL_STATE_OFFHOOK;
    }

    public boolean isCalling() {
        return calling;
    }
}
