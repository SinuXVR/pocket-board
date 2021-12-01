package com.sinux.pocketboard.preferences;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;

import com.sinux.pocketboard.R;
import com.sinux.pocketboard.utils.InputUtils;
import com.sinux.pocketboard.utils.ToastMessageUtils;

public class PreferencesFragment extends PreferenceFragmentCompat {

    private static final int ANSWER_PHONE_CALLS_CODE = 8124;

    private Preference subtypesPreference;
    private SwitchPreference phoneControlPreference;
    private InputMethodManager inputMethodManager;
    private InputMethodInfo inputMethodInfo;
    private Context context;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        getPreferenceManager().setStorageDeviceProtected();
        setPreferencesFromResource(R.xml.preferences, rootKey);

        context = getContext();
        subtypesPreference = findPreference(getString(R.string.ime_subtypes_prefs_key));
        phoneControlPreference = findPreference(getString(R.string.ime_extra_phone_control_prefs_key));
        inputMethodManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodInfo = InputUtils.getInputMethodInfo(context, inputMethodManager);

        // Set pref click listener to start subtype selection activity
        if (subtypesPreference != null) {
            subtypesPreference.setOnPreferenceClickListener(preference -> {
                PreferencesHolder.launchInputMethodSubtypeSettings(context, inputMethodInfo);
                return true;
            });
        }

        // Set pref click listener to request answer phone calls permissions
        if (phoneControlPreference != null) {
            phoneControlPreference.setOnPreferenceClickListener(preference -> {
                if (((SwitchPreference) preference).isChecked()) {
                    requestPermissions(new String[]{Manifest.permission.ANSWER_PHONE_CALLS}, ANSWER_PHONE_CALLS_CODE);
                }
                return true;
            });
        }

        updateInputSubtypesPrefSummary();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateInputSubtypesPrefSummary();
    }

    private void updateInputSubtypesPrefSummary() {
        if (context != null && subtypesPreference != null &&
                inputMethodManager != null && inputMethodInfo != null) {
            subtypesPreference.setSummary(getEnabledSubtypesString(context, inputMethodManager, inputMethodInfo));
        }
    }

    private static String getEnabledSubtypesString(Context context, InputMethodManager imm, InputMethodInfo imi) {
        if (context == null || imm == null || imi == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        for (InputMethodSubtype ims : imm.getEnabledInputMethodSubtypeList(imi, true)) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(ims.getDisplayName(context, imi.getPackageName(), imi.getServiceInfo().applicationInfo));
        }
        return sb.toString();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == ANSWER_PHONE_CALLS_CODE) {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                if (phoneControlPreference != null) {
                    phoneControlPreference.setChecked(false);
                    ToastMessageUtils.showMessage(context, R.string.ime_extra_phone_control_permission_required);
                }
            }
        }
    }
}
