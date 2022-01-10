package com.sinux.pocketboard.input;

public enum MetaKeyState {

    DISABLED,
    ENABLED,
    FIXED;

    public MetaKeyState next() {
        // Toggle between DISABLED and ENABLED excluding FIXED
        return this == DISABLED ? ENABLED : DISABLED;
    }
}
