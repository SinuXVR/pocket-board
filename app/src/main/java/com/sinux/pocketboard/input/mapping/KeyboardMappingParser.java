package com.sinux.pocketboard.input.mapping;

import android.content.Context;

import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KeyboardMappingParser {

    private static final String KEYBOARD_MAPPING_TAG = "KeyboardMapping";
    private static final String KEY_TAG = "Key";
    private static final String ADD_TAG = "Add";
    private static final String ALT_TAG = "Alt";
    private static final String CODE_ATTR = "code";
    private static final String VALUE_ATTR = "value";
    private static final String SHIFT_VALUE_ATTR = "shiftValue";

    private final XmlPullParser xpp;

    public KeyboardMappingParser(Context context, String keyMappingFile) {
        this(context.getResources().getXml(
                context.getResources()
                        .getIdentifier("keyboard_mapping_" + keyMappingFile, "xml", context.getPackageName())
        ));
    }

    public KeyboardMappingParser(XmlPullParser xpp) {
        this.xpp = xpp;
    }

    public KeyboardMapping parseMapping() throws Exception {
        Map<Integer, KeyMapping> keyMappings = new HashMap<>();
        List<KeyMappingValue> currentKeyValues = new ArrayList<>();
        List<KeyMappingValue> currentKeyAltValues = new ArrayList<>();
        int currentKeyCode = 0;

        while (xpp.getEventType() != XmlPullParser.END_DOCUMENT) {
            switch (xpp.getEventType()) {
                case XmlPullParser.START_TAG:
                    if (KEY_TAG.equals(xpp.getName())) {
                        currentKeyCode = 0;
                        currentKeyValues.clear();
                        currentKeyAltValues.clear();
                        for (int i = 0; i < xpp.getAttributeCount(); i++) {
                            if (CODE_ATTR.equals(xpp.getAttributeName(i))) {
                                currentKeyCode = Integer.parseInt(xpp.getAttributeValue(i));
                            }
                        }
                        parseAndPutValue(xpp, currentKeyValues);
                    } else if (ADD_TAG.equals(xpp.getName())) {
                        parseAndPutValue(xpp, currentKeyValues);
                    } else if (ALT_TAG.equals(xpp.getName())) {
                        parseAndPutValue(xpp, currentKeyAltValues);
                    }
                    break;

                case XmlPullParser.END_TAG:
                    if (KEYBOARD_MAPPING_TAG.equals(xpp.getName())) {
                        return new KeyboardMapping(keyMappings);
                    } else if (KEY_TAG.equals(xpp.getName())) {
                        keyMappings.put(currentKeyCode,
                                new KeyMapping(
                                        currentKeyValues.toArray(currentKeyValues.toArray(new KeyMappingValue[0])),
                                        currentKeyAltValues.toArray(currentKeyAltValues.toArray(currentKeyAltValues.toArray(new KeyMappingValue[0])))
                                ));
                    }
                    break;

                default:
                    break;
            }
            xpp.next();
        }

        throw new IllegalStateException("An error occurred during KeyboardMapping parsing");
    }

    private static void parseAndPutValue(XmlPullParser xpp, List<KeyMappingValue> target) {
        int value = 0;
        int shiftValue = 0;
        for (int i = 0; i < xpp.getAttributeCount(); i++) {
            if (VALUE_ATTR.equals(xpp.getAttributeName(i))) {
                value = xpp.getAttributeValue(i).codePointAt(0);
            } else if (SHIFT_VALUE_ATTR.equals(xpp.getAttributeName(i))) {
                shiftValue = xpp.getAttributeValue(i).codePointAt(0);
            }
        }
        target.add(new KeyMappingValue(value, shiftValue));
    }
}