package com.sinux.pocketboard.input;

public enum MetaKeyState {

    DISABLED,
    ENABLED,
    FIXED;

    private static final MetaKeyState[] values = values();

    public MetaKeyState next() {
        return values[(this.ordinal() + 1) % values.length];
    }
}
