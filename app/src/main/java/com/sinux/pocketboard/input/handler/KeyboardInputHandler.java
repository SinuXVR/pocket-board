package com.sinux.pocketboard.input.handler;

import android.os.SystemClock;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;

import com.sinux.pocketboard.PocketBoardIME;
import com.sinux.pocketboard.R;
import com.sinux.pocketboard.input.mapping.KeyMapping;
import com.sinux.pocketboard.input.mapping.KeyboardMappingManager;
import com.sinux.pocketboard.preferences.PreferencesHolder;
import com.sinux.pocketboard.utils.InputUtils;
import com.sinux.pocketboard.utils.CharacterUtils;

import java.util.Arrays;
import java.util.List;

public class KeyboardInputHandler implements InputHandler {

    private final PocketBoardIME pocketBoardIME;
    private final InputMethodManager inputMethodManager;
    private final PreferencesHolder preferencesHolder;
    private final KeyboardMappingManager keyboardMappingManager;

    private final StringBuilder textComposer;
    private final String nonLetterOrDigitExclusions;
    private final int wordLookupLength;
    private final long keyLongPressDuration;
    private final int layoutChangeShortcutEventRepeatCount;

    private boolean composingEnabled;
    private boolean numericInputMode;
    private boolean layoutChangeShortcut;
    private boolean doubleSpacePeriod;
    private boolean autocorrection;

    private CharSequence currentSelectedText;
    private byte keyIterationCounter;
    private long lastKeyDownTime;
    private int lastKeyCode;
    private boolean lastShiftEnabled;
    private boolean lastAltEnabled;
    private int lastCursorPosition;

    private final List<String> rawInputEditors;
    private boolean rawInputMode;

    public KeyboardInputHandler(PocketBoardIME pocketBoardIME) {
        this.pocketBoardIME = pocketBoardIME;
        this.inputMethodManager = pocketBoardIME.getInputMethodManager();
        this.preferencesHolder = pocketBoardIME.getPreferencesHolder();
        keyboardMappingManager = new KeyboardMappingManager(pocketBoardIME, inputMethodManager);

        textComposer = new StringBuilder();
        nonLetterOrDigitExclusions = pocketBoardIME.getResources().getString(R.string.non_letter_or_digit_exclusions);
        wordLookupLength = pocketBoardIME.getResources().getInteger(R.integer.word_lookup_length);
        keyLongPressDuration = preferencesHolder.getLongKeyPressDuration();
        layoutChangeShortcutEventRepeatCount = pocketBoardIME.getResources().getInteger(R.integer.layout_change_shortcut_event_repeat_count);

        rawInputEditors = Arrays.asList(pocketBoardIME.getResources().getStringArray(R.array.raw_input_editors));
    }

    public void onStartInput(EditorInfo attribute, boolean suggestionsAllowed, int cursorPosition) {
        rawInputMode = rawInputEditors.contains(attribute.packageName);

        composingEnabled = suggestionsAllowed && !rawInputMode;

        // Switch to numeric keyboard
        if (InputUtils.isNumericEditor(attribute)) {
            numericInputMode = true;
            keyboardMappingManager.switchToNumericKeyboardMapping();
        } else {
            numericInputMode = false;
            keyboardMappingManager.switchToKeyboardMapping(inputMethodManager.getCurrentInputMethodSubtype());
        }

        layoutChangeShortcut = preferencesHolder.isLayoutChangeShortcutEnabled();
        doubleSpacePeriod = preferencesHolder.isDoubleSpacePeriodEnabled();
        autocorrection = composingEnabled && preferencesHolder.isAutoCorrectionEnabled();

        if (composingEnabled) {
            textComposer.setLength(0);
        }

        lastCursorPosition = cursorPosition;
    }

    public void onFinishInput() {
        if (composingEnabled) {
            textComposer.setLength(0);
        }
    }

    public void onUpdateSelection(InputConnection inputConnection, int newSelStart, int newSelEnd, int candidatesEnd) {
        if (composingEnabled) {
            currentSelectedText = "";
            if (textComposer.length() > 0 && (newSelStart != candidatesEnd || newSelEnd != candidatesEnd)) {
                textComposer.setLength(0);
                if (inputConnection != null) {
                    inputConnection.finishComposingText();
                }
            } else if (newSelStart != newSelEnd) {
                currentSelectedText = inputConnection.getSelectedText(0);
            }
        }
        lastCursorPosition = Math.min(newSelStart, newSelEnd);
    }

    public void onInputMethodSubtypeChanged(InputMethodSubtype inputMethodSubtype, boolean suggestionsAllowed) {
        if (composingEnabled) {
            InputConnection inputConnection = pocketBoardIME.getCurrentInputConnection();
            if (inputConnection != null) {
                commitComposingText(inputConnection);
            }
        }
        composingEnabled = suggestionsAllowed;
        keyboardMappingManager.switchToKeyboardMapping(inputMethodSubtype);
    }

    public CharSequence getCurrentComposingText() {
        return TextUtils.isEmpty(textComposer) ? currentSelectedText : textComposer;
    }

    public void applySuggestion(CharSequence text, InputConnection inputConnection, boolean appendSpace) {
        if (inputConnection != null) {
            if (composingEnabled) {
                textComposer.setLength(0);
                textComposer.append(text);
                if (appendSpace) {
                    textComposer.append(' ');
                }
                commitComposingText(inputConnection);
            } else {
                inputConnection.commitText(text, 0);
            }
        }
    }

    @Override
    public boolean handleKeyDown(int keyCode, KeyEvent event, InputConnection inputConnection,
                                 boolean shiftEnabled, boolean altEnabled) {
        long eventTime = event.getEventTime();

        if (keyCode == KeyEvent.KEYCODE_DEL) {
            if (!composingEnabled || event.getRepeatCount() == 0) {
                // Remove last char on DEL short press
                handleBackspace(inputConnection);
                lastKeyDownTime = eventTime;
                lastKeyCode = keyCode;
            } else {
                // Remove last word on DEL long press
                if (composingEnabled && eventTime - lastKeyDownTime > keyLongPressDuration) {
                    handleBackspace(inputConnection);
                    boolean hadComposingText = textComposer.length() > 0;
                    inputConnection.beginBatchEdit();
                    textComposer.setLength(0);
                    inputConnection.commitText("", 1);
                    inputConnection.endBatchEdit();
                    // Delay before removing next word
                    if (hadComposingText) {
                        lastKeyDownTime = eventTime;
                    }
                }
            }
            return true;
        }

        if (keyCode == KeyEvent.KEYCODE_SPACE) {
            handleSpace(inputConnection, eventTime, event.getRepeatCount());
            lastKeyDownTime = eventTime;
            lastKeyCode = keyCode;
            return true;
        }

        if (event.getUnicodeChar() == 0) {
            return false;
        }

        if (handleCharacter(keyCode, event, inputConnection, shiftEnabled, altEnabled, eventTime)) {
            lastKeyDownTime = eventTime;
            lastKeyCode = keyCode;
            return true;
        }

        return false;
    }

    private void handleBackspace(InputConnection inputConnection) {
        if (composingEnabled) {
            int composingLength = textComposer.length();
            if (composingLength > 1) {
                // Remove next composing character
                textComposer.setLength(textComposer.length() - CharacterUtils.getLastCharacterLength(textComposer));
                inputConnection.setComposingText(textComposer, 1);
            } else if (composingLength > 0) {
                // Remove last composing character
                textComposer.setLength(0);
                inputConnection.commitText("", 1);
            } else {
                // Try to remove last character then find and convert last word to new composing
                inputConnection.beginBatchEdit();
                deleteLastCharacter(inputConnection);
                findAndComposeLastWord(inputConnection);
                inputConnection.endBatchEdit();
            }
        } else {
            deleteLastCharacter(inputConnection);
        }
    }

    private void deleteLastCharacter(InputConnection inputConnection) {
        if (rawInputMode) {
            pocketBoardIME.sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL);
            return;
        }

        if (TextUtils.isEmpty(inputConnection.getSelectedText(0))) {
            CharSequence str = inputConnection.getTextBeforeCursor(wordLookupLength, 0);
            if (!TextUtils.isEmpty(str)) {
                int beforeLength = CharacterUtils.getLastCharacterLength(str);
                inputConnection.deleteSurroundingText(beforeLength, 0);
                lastCursorPosition -= beforeLength;
            }
        } else {
            inputConnection.commitText("", 1);
        }
    }

    private void findAndComposeLastWord(InputConnection inputConnection) {
        // Find and convert last word to new composing
        CharSequence str = inputConnection.getTextBeforeCursor(wordLookupLength, 0);
        if (!TextUtils.isEmpty(str)) {
            int regionStart = CharacterUtils.getLastWordStartIndex(str, nonLetterOrDigitExclusions);
            int regionEnd = str.length();
            if (regionStart != regionEnd) {
                textComposer.append(str.subSequence(regionStart, regionEnd));
                int composingLength = textComposer.length();
                inputConnection.finishComposingText();
                inputConnection.setComposingRegion(lastCursorPosition - composingLength, lastCursorPosition);
            }
        }
    }

    private void handleSpace(InputConnection inputConnection, long eventTime, int eventRepeatCount) {
        // Layout change on long press
        if (layoutChangeShortcut) {
            if (eventRepeatCount == layoutChangeShortcutEventRepeatCount) {
                handleBackspace(inputConnection);
                pocketBoardIME.switchToNextInputMethod(true);
                return;
            } else if (eventRepeatCount > 0) {
                return;
            }
        }

        // Double-space period
        if (doubleSpacePeriod && eventTime - lastKeyDownTime <= keyLongPressDuration) {
            if (composingEnabled) {
                if (!handleAutocorrection()) {
                    commitComposingText(inputConnection);
                }
            }
            CharSequence lastChars = inputConnection.getTextBeforeCursor(3, 0);
            if (CharacterUtils.isLetterOrDigitAndSpace(lastChars)) {
                inputConnection.beginBatchEdit();
                inputConnection.deleteSurroundingText(1, 0);
                inputConnection.commitText(". ", 1);
                inputConnection.endBatchEdit();
            } else {
                inputConnection.commitText(" ", 1);
            }
        } else {
            if (composingEnabled) {
                if (!handleAutocorrection()) {
                    commitComposingText(inputConnection);
                }
            }
            inputConnection.commitText(" ", 1);
        }
    }

    private boolean handleCharacter(int keyCode, KeyEvent event, InputConnection inputConnection,
                                    boolean shiftEnabled, boolean altEnabled, long eventTime) {
        // Handle first key press
        if (event.getRepeatCount() == 0) {
            // Get current key mapping
            KeyMapping keyMapping = keyboardMappingManager.getCurrentMapping().getKeyMapping(keyCode);

            // Skip unknown keys
            if (keyMapping == null) {
                return false;
            }

            // Check if user has entered into additional key values iteration mode by fast clicking on same key
            boolean isNewKey = lastKeyCode != keyCode;
            boolean isShortPress = eventTime - lastKeyDownTime <= keyLongPressDuration;
            boolean keyIterationModeEnabled;
            if (keyMapping.hasAdditionalValues(lastAltEnabled) && !isNewKey && isShortPress) {
                keyIterationModeEnabled = true;
                keyIterationCounter++;
            } else {
                keyIterationModeEnabled = false;
                keyIterationCounter = 0;
                lastShiftEnabled = shiftEnabled;
                lastAltEnabled = altEnabled;
            }

            // Print next character
            if (!keyIterationModeEnabled || numericInputMode) {
                printNextCharacter(inputConnection, keyMapping.getValue(lastShiftEnabled, lastAltEnabled, keyIterationCounter));
            } else {
                // Or replace last (in additional key values iteration mode)
                replaceLastCharacter(inputConnection, keyMapping.getValue(lastShiftEnabled, lastAltEnabled, keyIterationCounter));
            }

            return true;
        } else {
            // Handle long key press - replace current char with first alt value
            if (!numericInputMode && !lastAltEnabled && (eventTime - lastKeyDownTime > keyLongPressDuration)) {
                lastAltEnabled = true;
                keyIterationCounter = 0;

                KeyMapping keyMapping = keyboardMappingManager.getCurrentMapping().getKeyMapping(keyCode);
                if (keyMapping == null) {
                    return false;
                }
                replaceLastCharacter(inputConnection, keyMapping.getValue(lastShiftEnabled, lastAltEnabled, keyIterationCounter));
                lastKeyDownTime = eventTime;

                return true;
            }
        }

        return false;
    }

    private void printNextCharacter(InputConnection inputConnection, int keyCharacterCodePoint) {
        if (rawInputMode) {
            pocketBoardIME.sendKeyChar((char) keyCharacterCodePoint);
            return;
        }

        if (CharacterUtils.isPunctuationCharacter(keyCharacterCodePoint)) {
            handlePunctuationCharacter(inputConnection, keyCharacterCodePoint, false);
        } else if (composingEnabled) {
            composeNewCharacter(inputConnection, keyCharacterCodePoint);
        } else {
            inputConnection.commitText(new String(Character.toChars(keyCharacterCodePoint)), 1);
        }
    }

    private void replaceLastCharacter(InputConnection inputConnection, int keyCharacterCodePoint) {
        if (rawInputMode) {
            handleBackspace(inputConnection);
            SystemClock.sleep(10); // Stupid but working trick to prevent events race condition
            printNextCharacter(inputConnection, keyCharacterCodePoint);
            return;
        }

        if (CharacterUtils.isPunctuationCharacter(keyCharacterCodePoint)) {
            handlePunctuationCharacter(inputConnection, keyCharacterCodePoint, true);
        } else if (composingEnabled) {
            if (textComposer.length() > 0) {
                textComposer.setLength(textComposer.length() - CharacterUtils.getLastCharacterLength(textComposer));
                composeNewCharacter(inputConnection, keyCharacterCodePoint);
            } else {
                inputConnection.beginBatchEdit();
                inputConnection.deleteSurroundingTextInCodePoints(1, 0);
                inputConnection.commitText(new String(Character.toChars(keyCharacterCodePoint)), 1);
                findAndComposeLastWord(inputConnection);
                inputConnection.endBatchEdit();
            }
        } else {
            inputConnection.beginBatchEdit();
            inputConnection.deleteSurroundingTextInCodePoints(1, 0);
            printNextCharacter(inputConnection, keyCharacterCodePoint);
            inputConnection.endBatchEdit();
        }
    }

    private void composeNewCharacter(InputConnection inputConnection, int keyCharacterCodePoint) {
        textComposer.appendCodePoint(keyCharacterCodePoint);
        inputConnection.setComposingText(textComposer, 1);
        if (!Character.isLetterOrDigit(keyCharacterCodePoint) &&
                !nonLetterOrDigitExclusions.contains(new String(Character.toChars(keyCharacterCodePoint)))) {
            commitComposingText(inputConnection);
        }
    }

    private void handlePunctuationCharacter(InputConnection inputConnection, int keyCharacterCodePoint, boolean removeLastCharacter) {
        inputConnection.beginBatchEdit();

        if (composingEnabled) {
            commitComposingText(inputConnection);
        }

        if (removeLastCharacter) {
            inputConnection.deleteSurroundingTextInCodePoints(1, 0);
        }

        CharSequence lastChars = inputConnection.getTextBeforeCursor(3, 0);
        if (CharacterUtils.isLetterOrDigitAndSpace(lastChars)) {
            inputConnection.deleteSurroundingText(1, 0);
            inputConnection.commitText(new String(Character.toChars(keyCharacterCodePoint)) + " ", 1);
        } else {
            inputConnection.commitText(new String(Character.toChars(keyCharacterCodePoint)), 1);
        }

        inputConnection.endBatchEdit();
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean handleAutocorrection() {
        if (autocorrection) {
            CharSequence recommendedSuggestion = pocketBoardIME.getSuggestionsManager().getCurrentRecommendedSuggestion();
            if (recommendedSuggestion != null) {
                applySuggestion(recommendedSuggestion, pocketBoardIME.getCurrentInputConnection(), false);
                return true;
            }
        }

        return false;
    }

    private void commitComposingText(InputConnection inputConnection) {
        if (inputConnection != null && textComposer.length() > 0) {
            inputConnection.commitText(textComposer, 1);
            textComposer.setLength(0);
        }
    }

    public void commitEmoji(CharSequence itemValue) {
        InputConnection inputConnection = pocketBoardIME.getCurrentInputConnection();
        commitComposingText(inputConnection);
        applySuggestion(itemValue, pocketBoardIME.getCurrentInputConnection(), false);
    }

    public boolean isInRawInputMode() {
        return rawInputMode;
    }
}
