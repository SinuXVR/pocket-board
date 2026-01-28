package com.sinux.pocketboard.preferences;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;

import androidx.preference.PreferenceManager;

import com.sinux.pocketboard.R;
import com.sinux.pocketboard.utils.InputUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class PreferencesHolder implements SharedPreferences.OnSharedPreferenceChangeListener {

    private final Context deviceProtectedStorageContext;
    private final SharedPreferences sharedPreferences;

    private final String autoCorrectionKey;
    private final String dictShortcutsKey;
    private final String autoCapitalizationKey;
    private final String doubleSpacePeriodKey;
    private final String layoutChangeIndicationKey;
    private final String voiceInputShortcutKey;
    private final String virtualTouchpadKey;
    private final String virtualTouchpadSpeedKey;
    private final String layoutChangeShortcutKey;
    private final String lockSymPadKey;
    private final String inlineSuggestionsKey;
    private final String manualRecentEmojiKey;
    private final String showPanelKey;
    private final String showEmojiKey;
    private final String showSuggestionsKey;
    private final String showMetaLayoutKey;
    private final String showVoiceKey;
    private final String phoneControlKey;
    private final String recentEmojiKey;

    private final Map<String, Object> prefValues;
    private final Map<String, Set<Consumer<Object>>> prefsChangeListeners = new HashMap<>();

    public PreferencesHolder(Context context) {
        deviceProtectedStorageContext = context.createDeviceProtectedStorageContext();
        PreferenceManager.setDefaultValues(deviceProtectedStorageContext, R.xml.preferences, false);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(deviceProtectedStorageContext);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);

        autoCorrectionKey = context.getString(R.string.ime_auto_correction_prefs_key);
        dictShortcutsKey = context.getString(R.string.ime_dict_shortcuts_prefs_key);
        autoCapitalizationKey = context.getString(R.string.ime_auto_capitalization_prefs_key);
        doubleSpacePeriodKey = context.getString(R.string.ime_double_space_period_prefs_key);
        layoutChangeIndicationKey = context.getString(R.string.ime_show_layout_toast_prefs_key);
        voiceInputShortcutKey = context.getString(R.string.ime_voice_input_shortcut_prefs_key);
        virtualTouchpadKey = context.getString(R.string.ime_virtual_touchpad_prefs_key);
        virtualTouchpadSpeedKey = context.getString(R.string.ime_virtual_touchpad_speed_prefs_key);
        layoutChangeShortcutKey = context.getString(R.string.ime_layout_change_shortcut_prefs_key);
        lockSymPadKey = context.getString(R.string.ime_lock_sympad_prefs_key);
        inlineSuggestionsKey = context.getString(R.string.ime_show_inline_suggestions_prefs_key);
        manualRecentEmojiKey = context.getString(R.string.ime_manual_recent_emoji_management_prefs_key);
        showPanelKey = context.getString(R.string.ime_show_panel_prefs_key);
        showEmojiKey = context.getString(R.string.ime_show_emoji_prefs_key);
        showSuggestionsKey = context.getString(R.string.ime_show_suggestions_prefs_key);
        showMetaLayoutKey = context.getString(R.string.ime_show_meta_layout_prefs_key);
        showVoiceKey = context.getString(R.string.ime_show_voice_prefs_key);
        phoneControlKey = context.getString(R.string.ime_extra_phone_control_prefs_key);
        recentEmojiKey = context.getString(R.string.ime_recent_emoji_prefs_key);

        prefValues = new HashMap<>();
    }

    public void registerPreferenceChangeListener(String key, Consumer<Object> listener) {
        prefsChangeListeners.computeIfAbsent(key, k -> new HashSet<>()).add(listener);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Object value = sharedPreferences.getAll().get(key);
        if (value != null) {
            prefValues.put(key, value);
            if (prefsChangeListeners.containsKey(key) && prefsChangeListeners.get(key) != null) {
                prefsChangeListeners.get(key).forEach(consumer -> consumer.accept(value));
            }
        }
    }

    public boolean isAutoCorrectionEnabled() {
        return (boolean) prefValues.computeIfAbsent(autoCorrectionKey, key -> getValue(key, Boolean.class, true));
    }

    public boolean isDictShortcutsEnabled() {
        return (boolean) prefValues.computeIfAbsent(dictShortcutsKey, key -> getValue(key, Boolean.class, true));
    }

    public boolean isAutoCapitalizationEnabled() {
        return (boolean) prefValues.computeIfAbsent(autoCapitalizationKey, key -> getValue(key, Boolean.class, true));
    }

    public boolean isDoubleSpacePeriodEnabled() {
        return (boolean) prefValues.computeIfAbsent(doubleSpacePeriodKey, key -> getValue(key, Boolean.class, true));
    }

    public boolean isLayoutChangeIndicationEnabled() {
        return (boolean) prefValues.computeIfAbsent(layoutChangeIndicationKey, key -> getValue(key, Boolean.class, true));
    }

    public boolean isVoiceInputShortcutEnabled() {
        return (boolean) prefValues.computeIfAbsent(voiceInputShortcutKey, key -> getValue(key, Boolean.class, true));
    }

    public boolean isVirtualTouchpadEnabled() {
        return (boolean) prefValues.computeIfAbsent(virtualTouchpadKey, key -> getValue(key, Boolean.class, true));
    }

    public int getVirtualTouchpadSpeed() {
        return (int) prefValues.computeIfAbsent(virtualTouchpadSpeedKey, key -> getValue(key, Integer.class, 5));
    }

    public boolean isLayoutChangeShortcutEnabled() {
        return (boolean) prefValues.computeIfAbsent(layoutChangeShortcutKey, key -> getValue(key, Boolean.class, true));
    }

    public boolean isLockSymPadEnabled() {
        return (boolean) prefValues.computeIfAbsent(lockSymPadKey, key -> getValue(key, Boolean.class, true));
    }

    public boolean isInlineSuggestionsEnabled() {
        return (boolean) prefValues.computeIfAbsent(inlineSuggestionsKey, key -> getValue(key, Boolean.class, true));
    }

    public boolean isManualRecentEmojiEnabled() {
        return (boolean) prefValues.computeIfAbsent(manualRecentEmojiKey, key -> getValue(key, Boolean.class, false));
    }

    public boolean isShowPanelEnabled() {
        return (boolean) prefValues.computeIfAbsent(showPanelKey, key -> getValue(key, Boolean.class, true));
    }

    public boolean isShowEmojiEnabled() {
        return (boolean) prefValues.computeIfAbsent(showEmojiKey, key -> getValue(key, Boolean.class, true));
    }

    public boolean isShowSuggestionsEnabled() {
        return (boolean) prefValues.computeIfAbsent(showSuggestionsKey, key -> getValue(key, Boolean.class, true));
    }

    public boolean isShowVoiceEnabled() {
        return (boolean) prefValues.computeIfAbsent(showVoiceKey, key -> getValue(key, Boolean.class, true));
    }

    public boolean isShowMetaLayoutEnabled() {
        return (boolean) prefValues.computeIfAbsent(showMetaLayoutKey, key -> getValue(key, Boolean.class, true));
    }

    public boolean isPhoneControlEnabled() {
        return (boolean) prefValues.computeIfAbsent(phoneControlKey, key -> getValue(key, Boolean.class, false));
    }

    public long getLongKeyPressDuration() {
        return deviceProtectedStorageContext.getResources().getInteger(R.integer.key_long_press_duration);
    }

    public String getRecentEmojiString() {
        return getValue(recentEmojiKey, String.class, "");
    }

    public void saveRecentEmojiString(String recentEmojiString) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(recentEmojiKey, recentEmojiString);
        editor.apply();
    }

    @SuppressWarnings({"unchecked", "SameParameterValue"})
    private <T> T getValue(String key, Class<T> type, T defaultValue) {
        Object value;
        if (Boolean.class.isAssignableFrom(type)) {
            value = sharedPreferences.getBoolean(key, (Boolean) defaultValue);
        } else if (String.class.isAssignableFrom(type)) {
            value = sharedPreferences.getString(key, (String) defaultValue);
        } else if (Integer.class.isAssignableFrom(type)) {
            value = sharedPreferences.getInt(key, (Integer) defaultValue);
        } else if (Long.class.isAssignableFrom(type)) {
            value = sharedPreferences.getLong(key, (Long) defaultValue);
        } else if (Float.class.isAssignableFrom(type)) {
            value = sharedPreferences.getFloat(key, (Float) defaultValue);
        } else if (Set.class.isAssignableFrom(type)) {
            value = sharedPreferences.getStringSet(key, (Set<String>) defaultValue);
        } else {
            throw new IllegalArgumentException("Unsupported preference value type " + type.getName());
        }

        return type.cast(value);
    }

    public static void launchInputMethodSubtypeSettings(Context context, InputMethodManager inputMethodManager) {
        launchInputMethodSubtypeSettings(context, InputUtils.getInputMethodInfo(context, inputMethodManager));
    }

    public static void launchInputMethodSubtypeSettings(Context context, InputMethodInfo inputMethodInfo) {
        Intent intent = new Intent(Settings.ACTION_INPUT_METHOD_SUBTYPE_SETTINGS);
        intent.putExtra(Settings.EXTRA_INPUT_METHOD_ID, inputMethodInfo.getId());
        intent.putExtra(Intent.EXTRA_TITLE, context.getString(R.string.ime_subtypes_select));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
}
