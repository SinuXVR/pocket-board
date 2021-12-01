package com.sinux.pocketboard.input.mapping;

public final class KeyMapping {

    private final KeyMappingValue[] keyMappingValues;
    private final boolean hasAdditionalValues;
    private final KeyMappingValue[] keyMappingAltValues;
    private final boolean hasAltValues;
    private final boolean hasAdditionalAltValues;

    public KeyMapping(KeyMappingValue[] keyMappingValues, KeyMappingValue[] keyMappingAltValues) {
        if (keyMappingValues == null || keyMappingValues.length == 0) {
            throw new IllegalArgumentException("KeyMapping must have at least one value");
        }
        this.keyMappingValues = keyMappingValues;
        hasAdditionalValues = keyMappingValues.length > 1;
        this.keyMappingAltValues = keyMappingAltValues;
        hasAltValues = keyMappingAltValues != null && keyMappingAltValues.length > 0;
        hasAdditionalAltValues = hasAltValues && keyMappingAltValues.length > 1;
    }

    public int getValue(boolean shiftEnabled, boolean altEnabled, byte keyIndex) {
        if (shiftEnabled) {
            if (altEnabled) {
                return getAltShiftValue(keyIndex);
            } else {
                return getShiftValue(keyIndex);
            }
        } else {
            if (altEnabled) {
                return getAltValue(keyIndex);
            } else {
                return getValue(keyIndex);
            }
        }
    }

    public boolean hasAdditionalValues(boolean altEnabled) {
        return altEnabled ? hasAdditionalAltValues : hasAdditionalValues;
    }

    private int getValue(byte keyIndex) {
        int index = (keyIndex & 0xFF) % keyMappingValues.length;
        return keyMappingValues[index].getValue();
    }

    private int getShiftValue(byte keyIndex) {
        int index = (keyIndex & 0xFF) % keyMappingValues.length;
        return keyMappingValues[index].getShiftValue() != 0 ? keyMappingValues[index].getShiftValue() : getValue(keyIndex);
    }

    private int getAltValue(byte keyIndex) {
        if (hasAltValues) {
            int index = (keyIndex & 0xFF) % keyMappingAltValues.length;
            return keyMappingAltValues[index].getValue();
        } else {
            return getValue(keyIndex);
        }
    }

    private int getAltShiftValue(byte keyIndex) {
        if (hasAltValues) {
            int index = (keyIndex & 0xFF) % keyMappingAltValues.length;
            int shiftValue = keyMappingAltValues[index].getShiftValue();
            return shiftValue != 0 ? shiftValue : getAltValue(keyIndex);
        } else {
            return getShiftValue(keyIndex);
        }
    }
}
