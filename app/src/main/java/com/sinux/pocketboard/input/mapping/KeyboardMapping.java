package com.sinux.pocketboard.input.mapping;

import java.util.Collections;
import java.util.Map;

public final class KeyboardMapping {

    private final Map<Integer, KeyMapping> keyMappings;

    public KeyboardMapping(Map<Integer, KeyMapping> keyMappings) {
        if (keyMappings == null || keyMappings.isEmpty()) {
            throw new IllegalArgumentException("KeyboardMapping cannot be empty");
        }
        this.keyMappings = Collections.unmodifiableMap(keyMappings);
    }

    public KeyMapping getKeyMapping(int keyCode) {
        return keyMappings.get(keyCode);
    }
}