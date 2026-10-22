package com.sinux.pocketboard.preferences;

import android.os.Bundle;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.KeyEvent;
import android.text.InputType;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.sinux.pocketboard.R;
import com.sinux.pocketboard.input.mapping.KeyMapping;
import com.sinux.pocketboard.input.mapping.KeyMappingValue;
import com.sinux.pocketboard.input.mapping.KeyboardMapping;
import com.sinux.pocketboard.input.mapping.KeyboardMappingOverrideStore;
import com.sinux.pocketboard.input.mapping.KeyboardMappingParser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class KeyboardMappingEditorActivity extends AppCompatActivity {

    private final Profile[] profiles = new Profile[2];

    private Spinner profileSpinner;
    private Spinner keySpinner;
    private EditText valuesField;
    private EditText shiftValuesField;
    private EditText altValuesField;
    private EditText altShiftValuesField;
    private TextView statusView;

    private Profile currentProfile;
    private KeyboardMapping baseMapping;
    private KeyboardMapping editedMapping;
    private List<Integer> keyCodes = Collections.emptyList();
    private boolean updating;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        profiles[0] = new Profile(
                "en_us_titan2", "en_us",
                getString(R.string.keyboard_editor_profile_en));
        profiles[1] = new Profile(
                "ru_bbp_titan2", "ru_bbp",
                getString(R.string.keyboard_editor_profile_ru));

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.keyboard_editor_title);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        buildScreen();
        loadProfile(profiles[0]);
    }

    private void buildScreen() {
        ScrollView scroll = new ScrollView(this);
        ViewCompat.setOnApplyWindowInsetsListener(scroll, (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(view.getPaddingLeft(), bars.top, view.getPaddingRight(), bars.bottom);
            return windowInsets;
        });
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(16);
        content.setPadding(padding, padding, padding, padding);
        scroll.addView(content);

        addLabel(content, R.string.keyboard_editor_profile);
        profileSpinner = new Spinner(this);
        ArrayAdapter<Profile> profileAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, profiles);
        profileAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        profileSpinner.setAdapter(profileAdapter);
        content.addView(profileSpinner);

        addLabel(content, R.string.keyboard_editor_key);
        keySpinner = new Spinner(this);
        content.addView(keySpinner);

        valuesField = addMultilineField(content, R.string.keyboard_editor_values);
        shiftValuesField = addMultilineField(content, R.string.keyboard_editor_shift_values);
        altValuesField = addMultilineField(content, R.string.keyboard_editor_alt_values);
        altShiftValuesField = addMultilineField(content, R.string.keyboard_editor_alt_shift_values);

        Button saveButton = new Button(this);
        saveButton.setText(R.string.keyboard_editor_save);
        saveButton.setOnClickListener(v -> saveCurrentKey());
        content.addView(saveButton);

        Button resetButton = new Button(this);
        resetButton.setText(R.string.keyboard_editor_reset_profile);
        resetButton.setOnClickListener(v -> confirmResetProfile());
        content.addView(resetButton);

        statusView = new TextView(this);
        content.addView(statusView);

        profileSpinner.setOnItemSelectedListener(new SimpleSelectionListener(position -> {
            if (!updating && position >= 0 && position < profiles.length) {
                loadProfile(profiles[position]);
            }
        }));

        keySpinner.setOnItemSelectedListener(new SimpleSelectionListener(position -> {
            if (!updating) {
                loadSelectedKey(position);
            }
        }));

        setContentView(scroll);
    }

    private EditText addMultilineField(LinearLayout parent, int titleId) {
        addLabel(parent, titleId);
        EditText field = new EditText(this);
        field.setHint(R.string.keyboard_editor_lines_hint);
        field.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        field.setMinLines(2);
        field.setGravity(Gravity.TOP | Gravity.START);
        parent.addView(field, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return field;
    }

    private void addLabel(LinearLayout parent, int textId) {
        TextView label = new TextView(this);
        label.setText(textId);
        label.setPadding(0, dp(12), 0, dp(4));
        parent.addView(label);
    }

    private void loadProfile(Profile profile) {
        try {
            updating = true;
            currentProfile = profile;
            baseMapping = new KeyboardMappingParser(
                    this, profile.mappingName, "_titan2").parseMapping();
            editedMapping = KeyboardMappingOverrideStore.apply(
                    this, profile.id, baseMapping);

            ArrayList<Integer> codes = new ArrayList<>(baseMapping.getKeyMappings().keySet());
            codes.sort((left, right) ->
                    KeyEvent.keyCodeToString(left).compareTo(KeyEvent.keyCodeToString(right)));
            keyCodes = codes;

            ArrayList<String> labels = new ArrayList<>();
            for (int code : keyCodes) {
                labels.add(KeyEvent.keyCodeToString(code) + " [" + code + "]");
            }

            ArrayAdapter<String> keyAdapter = new ArrayAdapter<>(
                    this, android.R.layout.simple_spinner_item, labels);
            keyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            keySpinner.setAdapter(keyAdapter);
            updating = false;

            if (!keyCodes.isEmpty()) {
                keySpinner.setSelection(0);
                loadSelectedKey(0);
            }
            statusView.setText("");
        } catch (Exception e) {
            updating = false;
            statusView.setText(R.string.keyboard_editor_load_error);
        }
    }

    private void loadSelectedKey(int position) {
        if (editedMapping == null || position < 0 || position >= keyCodes.size()) {
            return;
        }

        KeyMapping mapping = editedMapping.getKeyMapping(keyCodes.get(position));
        if (mapping == null) {
            return;
        }

        valuesField.setText(formatValues(mapping.getValues(), false));
        shiftValuesField.setText(formatValues(mapping.getValues(), true));
        altValuesField.setText(formatValues(mapping.getAltValues(), false));
        altShiftValuesField.setText(formatValues(mapping.getAltValues(), true));
        statusView.setText("");
    }

    private void saveCurrentKey() {
        try {
            int position = keySpinner.getSelectedItemPosition();
            if (currentProfile == null || position < 0 || position >= keyCodes.size()) {
                return;
            }

            KeyMappingValue[] values = parseValues(
                    valuesField.getText().toString(),
                    shiftValuesField.getText().toString(),
                    true);
            KeyMappingValue[] altValues = parseValues(
                    altValuesField.getText().toString(),
                    altShiftValuesField.getText().toString(),
                    false);

            int keyCode = keyCodes.get(position);
            KeyboardMappingOverrideStore.saveKey(
                    this, currentProfile.id, keyCode, new KeyMapping(values, altValues));

            editedMapping = KeyboardMappingOverrideStore.apply(
                    this, currentProfile.id, baseMapping);
            loadSelectedKey(position);
            statusView.setText(R.string.keyboard_editor_saved);
        } catch (Exception e) {
            Toast.makeText(this, R.string.keyboard_editor_invalid_values, Toast.LENGTH_LONG).show();
        }
    }

    private void confirmResetProfile() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setMessage(R.string.keyboard_editor_reset_confirm)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    try {
                        KeyboardMappingOverrideStore.resetProfile(this, currentProfile.id);
                        editedMapping = baseMapping;
                        loadSelectedKey(keySpinner.getSelectedItemPosition());
                        statusView.setText(R.string.keyboard_editor_reset_done);
                    } catch (Exception e) {
                        statusView.setText(R.string.keyboard_editor_load_error);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private static KeyMappingValue[] parseValues(
            String valuesText,
            String shiftsText,
            boolean required
    ) {
        List<String> values = lines(valuesText);
        List<String> shifts = lines(shiftsText);

        if (required && values.isEmpty()) {
            throw new IllegalArgumentException("At least one value is required");
        }
        if (shifts.size() > values.size()) {
            throw new IllegalArgumentException("Too many Shift values");
        }

        KeyMappingValue[] result = new KeyMappingValue[values.size()];
        for (int i = 0; i < values.size(); i++) {
            String value = values.get(i);
            if (value.codePointCount(0, value.length()) != 1) {
                throw new IllegalArgumentException("Enter one character per line");
            }

            String shift = i < shifts.size() ? shifts.get(i) : "";
            if (!shift.isEmpty() && shift.codePointCount(0, shift.length()) != 1) {
                throw new IllegalArgumentException("Enter one Shift character per line");
            }

            result[i] = new KeyMappingValue(
                    value.codePointAt(0),
                    shift.isEmpty() ? 0 : shift.codePointAt(0));
        }
        return result;
    }

    private static List<String> lines(String text) {
        ArrayList<String> result = new ArrayList<>();
        if (text.isEmpty()) {
            return result;
        }

        String[] split = text.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        int end = split.length;
        while (end > 0 && split[end - 1].isEmpty()) {
            end--;
        }
        for (int i = 0; i < end; i++) {
            result.add(split[i]);
        }
        return result;
    }

    private static String formatValues(KeyMappingValue[] values, boolean shift) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                result.append('\n');
            }
            int codePoint = shift
                    ? values[i].getShiftValue()
                    : values[i].getValue();
            if (codePoint != 0) {
                result.appendCodePoint(codePoint);
            }
        }
        return result.toString();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private static final class Profile {
        final String id;
        final String mappingName;
        final String title;

        Profile(String id, String mappingName, String title) {
            this.id = id;
            this.mappingName = mappingName;
            this.title = title;
        }

        @Override
        public String toString() {
            return title;
        }
    }

    private interface SelectionCallback {
        void onSelected(int position);
    }

    private static final class SimpleSelectionListener
            implements android.widget.AdapterView.OnItemSelectedListener {
        private final SelectionCallback callback;

        SimpleSelectionListener(SelectionCallback callback) {
            this.callback = callback;
        }

        @Override
        public void onItemSelected(android.widget.AdapterView<?> parent,
                                   android.view.View view,
                                   int position,
                                   long id) {
            callback.onSelected(position);
        }

        @Override
        public void onNothingSelected(android.widget.AdapterView<?> parent) {
        }
    }
}
