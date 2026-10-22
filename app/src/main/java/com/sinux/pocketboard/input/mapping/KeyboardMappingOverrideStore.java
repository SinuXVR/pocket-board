package com.sinux.pocketboard.input.mapping;

import android.content.Context;
import android.util.AtomicFile;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;

/**
 * Stores user edits separately from built-in XML keyboard mappings.
 */
public final class KeyboardMappingOverrideStore {

    public static final String FILE_NAME = "keyboard_mapping_overrides.json";

    private static final String TAG = "PocketBoard";
    private static final String PROFILES = "profiles";
    private static final String VALUES = "values";
    private static final String ALT_VALUES = "altValues";
    private static final String VALUE = "value";
    private static final String SHIFT_VALUE = "shiftValue";

    private KeyboardMappingOverrideStore() {
    }

    public static synchronized KeyboardMapping apply(
            Context context,
            String profileId,
            KeyboardMapping baseMapping
    ) {
        try {
            JSONObject profiles = readRoot(context).optJSONObject(PROFILES);
            if (profiles == null) {
                return baseMapping;
            }

            JSONObject profile = profiles.optJSONObject(profileId);
            if (profile == null) {
                return baseMapping;
            }

            Map<Integer, KeyMapping> merged =
                    new HashMap<>(baseMapping.getKeyMappings());

            Iterator<String> keys = profile.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                int keyCode = Integer.parseInt(key);

                // Only override keys already present in the built-in mapping.
                if (merged.containsKey(keyCode)) {
                    merged.put(keyCode, readKeyMapping(profile.getJSONObject(key)));
                }
            }

            return new KeyboardMapping(merged);
        } catch (Exception e) {
            Log.e(TAG, "Could not apply keyboard mapping overrides", e);
            return baseMapping;
        }
    }

    public static synchronized void saveKey(
            Context context,
            String profileId,
            int keyCode,
            KeyMapping mapping
    ) throws Exception {
        JSONObject root = readRoot(context);
        JSONObject profiles = root.optJSONObject(PROFILES);
        if (profiles == null) {
            profiles = new JSONObject();
            root.put(PROFILES, profiles);
        }

        JSONObject profile = profiles.optJSONObject(profileId);
        if (profile == null) {
            profile = new JSONObject();
            profiles.put(profileId, profile);
        }

        JSONObject key = new JSONObject();
        key.put(VALUES, writeValues(mapping.getValues()));
        key.put(ALT_VALUES, writeValues(mapping.getAltValues()));
        profile.put(Integer.toString(keyCode), key);

        writeRoot(context, root);
    }

    public static synchronized void resetProfile(
            Context context,
            String profileId
    ) throws Exception {
        JSONObject root = readRoot(context);
        JSONObject profiles = root.optJSONObject(PROFILES);
        if (profiles == null) {
            return;
        }

        profiles.remove(profileId);
        if (profiles.length() == 0) {
            root.remove(PROFILES);
        }
        writeRoot(context, root);
    }

    public static synchronized boolean hasProfile(
            Context context,
            String profileId
    ) {
        JSONObject profiles = readRoot(context).optJSONObject(PROFILES);
        return profiles != null && profiles.optJSONObject(profileId) != null;
    }

    private static KeyMapping readKeyMapping(JSONObject json) throws JSONException {
        KeyMappingValue[] values = readValues(json.optJSONArray(VALUES), true);
        KeyMappingValue[] altValues = readValues(json.optJSONArray(ALT_VALUES), false);
        return new KeyMapping(values, altValues);
    }

    private static KeyMappingValue[] readValues(
            JSONArray json,
            boolean required
    ) throws JSONException {
        if (json == null || json.length() == 0) {
            if (required) {
                throw new JSONException("A key mapping must have at least one normal value");
            }
            return new KeyMappingValue[0];
        }

        KeyMappingValue[] result = new KeyMappingValue[json.length()];
        for (int i = 0; i < json.length(); i++) {
            JSONObject item = json.getJSONObject(i);
            String value = item.getString(VALUE);
            String shiftValue = item.optString(SHIFT_VALUE, "");

            if (value.isEmpty()) {
                throw new JSONException("Key value cannot be empty");
            }

            int valueCodePoint = value.codePointAt(0);
            int shiftCodePoint = shiftValue.isEmpty() ? 0 : shiftValue.codePointAt(0);
            result[i] = new KeyMappingValue(valueCodePoint, shiftCodePoint);
        }
        return result;
    }

    private static JSONArray writeValues(KeyMappingValue[] values) throws JSONException {
        JSONArray result = new JSONArray();

        for (KeyMappingValue item : values) {
            JSONObject value = new JSONObject();
            value.put(VALUE, new String(Character.toChars(item.getValue())));
            value.put(
                    SHIFT_VALUE,
                    item.getShiftValue() == 0
                            ? ""
                            : new String(Character.toChars(item.getShiftValue()))
            );
            result.put(value);
        }

        return result;
    }

    private static JSONObject readRoot(Context context) {
        File file = new File(context.getFilesDir(), FILE_NAME);
        if (!file.exists()) {
            return new JSONObject();
        }

        AtomicFile atomicFile = new AtomicFile(file);
        try (
                FileInputStream input = atomicFile.openRead();
                Scanner scanner = new Scanner(input, StandardCharsets.UTF_8.name())
                        .useDelimiter("\\A")
        ) {
            String json = scanner.hasNext() ? scanner.next() : "";
            return json.isEmpty() ? new JSONObject() : new JSONObject(json);
        } catch (Exception e) {
            Log.e(TAG, "Could not read keyboard mapping overrides", e);
            return new JSONObject();
        }
    }

    private static void writeRoot(Context context, JSONObject root) throws Exception {
        File file = new File(context.getFilesDir(), FILE_NAME);
        AtomicFile atomicFile = new AtomicFile(file);
        FileOutputStream output = null;

        try {
            output = atomicFile.startWrite();
            output.write(root.toString(2).getBytes(StandardCharsets.UTF_8));
            atomicFile.finishWrite(output);
            output = null;
        } finally {
            if (output != null) {
                atomicFile.failWrite(output);
            }
        }
    }
}
