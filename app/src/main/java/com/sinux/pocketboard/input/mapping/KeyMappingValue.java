package com.sinux.pocketboard.input.mapping;

public final class KeyMappingValue {

    private final int value;
    private final int shiftValue;

    public KeyMappingValue(int value, int shiftValue) {
        if (value == 0) {
            throw new IllegalArgumentException("Key value must be set");
        }
        this.value = value;
        this.shiftValue = shiftValue;
    }

    public int getValue() {
        return value;
    }

    public int getShiftValue() {
        return shiftValue;
    }
}
