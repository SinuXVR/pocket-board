package com.sinux.pocketboard.preferences;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;

import com.sinux.pocketboard.R;
import com.sinux.pocketboard.utils.InputUtils;
import com.sinux.pocketboard.utils.ToastMessageUtils;

public class PreferencesFragment extends PreferenceFragmentCompat {

    private Preference subtypesPreference;
    private SwitchPreference phoneControlPreference;
    private InputMethodManager inputMethodManager;
    private InputMethodInfo inputMethodInfo;
    private Context context;

    private ActivityResultLauncher<String[]> phonePermissionLauncher;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        getPreferenceManager().setStorageDeviceProtected();
        setPreferencesFromResource(R.xml.preferences, rootKey);

        context = getContext();
        subtypesPreference = findPreference(getString(R.string.ime_subtypes_prefs_key));
        phoneControlPreference = findPreference(getString(R.string.ime_extra_phone_control_prefs_key));
        inputMethodManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodInfo = InputUtils.getInputMethodInfo(context, inputMethodManager);

        phonePermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
            if (phoneControlPreference != null) {
                boolean allGranted = result.values().stream().allMatch(granted -> granted);
                phoneControlPreference.setChecked(allGranted);
            }
        });

        // Set pref click listener to start subtype selection activity
        if (subtypesPreference != null) {
            subtypesPreference.setOnPreferenceClickListener(preference -> {
                PreferencesHolder.launchInputMethodSubtypeSettings(context, inputMethodInfo);
                return true;
            });
        }

        // Set pref click listener to request answer phone calls permissions
        if (phoneControlPreference != null) {
            phoneControlPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean isCheckingOn = (boolean) newValue;
                if (isCheckingOn) {
                    // Check if we already have permissions
                    boolean hasPermissions = context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED &&
                            context.checkSelfPermission(Manifest.permission.ANSWER_PHONE_CALLS) == PackageManager.PERMISSION_GRANTED;
                    if (!hasPermissions) {
                        phonePermissionLauncher.launch(new String[]{
                                Manifest.permission.READ_PHONE_STATE,
                                Manifest.permission.ANSWER_PHONE_CALLS
                        });
                        return false;
                    }
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
}
