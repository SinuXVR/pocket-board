package com.sinux.pocketboard;

import android.annotation.SuppressLint;
import android.inputmethodservice.InputMethodService;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Size;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InlineSuggestion;
import android.view.inputmethod.InlineSuggestionsRequest;
import android.view.inputmethod.InlineSuggestionsResponse;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;
import android.widget.inline.InlinePresentationSpec;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.autofill.inline.UiVersions;
import androidx.autofill.inline.v1.InlineSuggestionUi;

import com.sinux.pocketboard.input.SuggestionsManager;
import com.sinux.pocketboard.input.handler.SymPadInputHandler;
import com.sinux.pocketboard.input.handler.KeyboardInputHandler;
import com.sinux.pocketboard.input.MetaKeyManager;
import com.sinux.pocketboard.preferences.PreferencesHolder;
import com.sinux.pocketboard.ui.InputView;
import com.sinux.pocketboard.utils.InputUtils;
import com.sinux.pocketboard.utils.ToastMessageUtils;
import com.sinux.pocketboard.utils.VoiceInputUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PocketBoardIME extends InputMethodService {

    private InputMethodManager inputMethodManager;
    private PreferencesHolder preferencesHolder;
    private MetaKeyManager metaKeyManager;
    private KeyboardInputHandler keyboardInputHandler;
    private SymPadInputHandler symPadInputHandler;

    private InputView inputView;
    private SuggestionsManager suggestionsManager;

    private boolean autoCapitalization;
    private boolean symPadJustUsed;
    private int currentCursorPos = 0;
    private int selectionAnchor = -1;

    private List<String> directInputEditors;

    @Override
    public void onCreate() {
        super.onCreate();

        inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        preferencesHolder = new PreferencesHolder(this);

        metaKeyManager = new MetaKeyManager(this);
        keyboardInputHandler = new KeyboardInputHandler(this);
        symPadInputHandler = new SymPadInputHandler(this);
        suggestionsManager = new SuggestionsManager(this, keyboardInputHandler);

        directInputEditors = Arrays.asList(getResources().getStringArray(R.array.direct_input_editors));
    }

    @Override
    public void onDestroy() {
        metaKeyManager.destroy();
        super.onDestroy();
    }

    @Override
    public void onWindowHidden() {
        super.onWindowHidden();
        if (inputView != null) {
            inputView.hideEmojiPanel();
        }
    }

    @SuppressLint("InflateParams")
    @Override
    public View onCreateInputView() {
        inputView = (InputView) getLayoutInflater().inflate(R.layout.input, null);
        suggestionsManager.setInputView(inputView);
        metaKeyManager.setMetaKeyStateChangeListener(inputView);
        return inputView;
    }

    @Override
    public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);
        autoCapitalization = preferencesHolder.isAutoCapitalizationEnabled();

        if (!restarting) {
            metaKeyManager.reset();
        }

        InputMethodSubtype currentInputMethodSubtype = inputMethodManager.getCurrentInputMethodSubtype();
        suggestionsManager.onStartInput(attribute, currentInputMethodSubtype);

        selectionAnchor = -1;
        currentCursorPos = 0;
        InputConnection inputConnection = getCurrentInputConnection();
        if (inputConnection != null) {
            ExtractedText extractedText = inputConnection.getExtractedText(new ExtractedTextRequest(), 0);
            if (extractedText != null) {
                currentCursorPos = Math.min(extractedText.selectionStart, extractedText.selectionEnd);
            }
        }
        keyboardInputHandler.onStartInput(attribute, suggestionsManager.isSuggestionsAllowed(), currentCursorPos);

        updateMetaState();
    }

    @Override
    public void onStartInputView(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);
        InputMethodSubtype currentInputMethodSubtype = inputMethodManager.getCurrentInputMethodSubtype();
        suggestionsManager.onStartInputView(currentInputMethodSubtype);
        suggestionsManager.update();
        inputView.onStartInputView(attribute, currentInputMethodSubtype, suggestionsManager.isSuggestionsAllowed());
    }

    @Override
    public void onFinishInputView(boolean finishingInput) {
        super.onFinishInputView(finishingInput);
        suggestionsManager.onFinishInput();
        keyboardInputHandler.onFinishInput();
    }

    @Override
    public boolean onEvaluateInputViewShown() {
        super.onEvaluateInputViewShown();
        // Force keyboard show to allow user toggle emoji panel even in hidden mode
        return true;
    }

    @Override
    protected void onCurrentInputMethodSubtypeChanged(InputMethodSubtype newSubtype) {
        super.onCurrentInputMethodSubtypeChanged(newSubtype);
        if (preferencesHolder.isLayoutChangeIndicationEnabled()) {
            ToastMessageUtils.showMessage(this, newSubtype.getNameResId());
        }
        suggestionsManager.onInputMethodSubtypeChanged(newSubtype);
        keyboardInputHandler.onInputMethodSubtypeChanged(newSubtype, suggestionsManager.isSuggestionsAllowed());
        if (inputView != null) {
            inputView.onInputMethodSubtypeChanged(newSubtype, suggestionsManager.isSuggestionsAllowed());
        }
        updateMetaState();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                if (isInputViewShown()) {
                    if (inputView.isEmojiPanelVisible()) {
                        inputView.hideEmojiPanel();
                    } else if (suggestionsManager.isInlineSuggestionsShown()) {
                        suggestionsManager.cancelInlineSuggestions();
                    } else {
                        requestHideSelf(0);
                    }
                    return true;
                }
                return false;
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                return false;
            default:
                // Voice input shortcut
                if (preferencesHolder.isVoiceInputShortcutEnabled() &&
                        event.isCtrlPressed() && event.isAltPressed() &&
                        event.getRepeatCount() == 0 && isInputViewShown()) {
                    VoiceInputUtils.launchVoiceIME(this);
                    return true;
                }

                // Meta keys
                if (metaKeyManager.handleKeyDown(keyCode, event)) {
                    return true;
                }

                InputConnection inputConnection = getCurrentInputConnection();
                if (inputConnection == null) {
                    return false;
                }

                // Skip CTRL+X shortcuts
                if (event.isCtrlPressed()) {
                    return false;
                }

                // Emulate D-pad and some media keys
                if (metaKeyManager.isSymFixed()) {
                    symPadInputHandler.handleKeyDown(keyCode, event, inputConnection,
                            metaKeyManager.isShiftEnabled(), metaKeyManager.isAltEnabled());
                    symPadJustUsed = true;
                    return true;
                }

                if (keyCode == KeyEvent.KEYCODE_ENTER) {
                    return false;
                }

                EditorInfo editorInfo = getCurrentInputEditorInfo();
                if (editorInfo != null && directInputEditors.contains(editorInfo.packageName)) {
                    return false;
                }

                // Handle text input in Keyboard mode
                if (editorInfo != null && (editorInfo.inputType != InputType.TYPE_NULL || keyboardInputHandler.isInRawInputMode())) {
                    // Handle emoji shortcuts
                    if (inputView != null && inputView.isEmojiPanelVisible() && inputView.handleEmojiShortcut(keyCode)) {
                        return true;
                    }
                    // Handle input
                    if (keyboardInputHandler.handleKeyDown(keyCode, event, inputConnection,
                            metaKeyManager.isShiftEnabled(), metaKeyManager.isAltEnabled())) {
                        // Show input view if it's hidden
                        if (!isInputViewShown()) {
                            requestShowSelf(InputMethodManager.SHOW_FORCED);
                        }
                    }
                } else {
                    /*
                      This code runs when user presses a key, but there is no focused editor available to handle this input.
                      We consume input but inform the system, because in some cases it can automatically focus on
                      some editor and call the IME
                     */
                    if (event.isPrintingKey()) {
                        inputConnection.performEditorAction(EditorInfo.IME_ACTION_NONE);
                    } else {
                        return false;
                    }
                }
        }

        return true;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        InputConnection inputConnection = getCurrentInputConnection();
        EditorInfo editorInfo = getCurrentInputEditorInfo();

        if (inputConnection == null || editorInfo == null) {
            return false;
        }

        if (directInputEditors.contains(editorInfo.packageName)) {
            return false;
        }

        if (metaKeyManager.isSymFixed()) {
            if (symPadInputHandler.handleKeyUp(keyCode, event, inputConnection,
                    metaKeyManager.isShiftEnabled(), metaKeyManager.isAltEnabled())) {
                return true;
            }
        }

        if (metaKeyManager.handleKeyUp(keyCode, event)) {
            if (keyCode == KeyEvent.KEYCODE_SYM || keyCode == KeyEvent.KEYCODE_PICTSYMBOLS) {
                if (!symPadJustUsed && !metaKeyManager.isSymFixed()) {
                    // Toggle emoji panel on SYM release
                    if (isInputViewShown()) {
                        inputView.toggleEmojiPanel();
                    }
                } else {
                    symPadJustUsed = false;
                }
            }

            return true;
        }

        return (editorInfo.inputType != InputType.TYPE_NULL || keyboardInputHandler.isInRawInputMode()) &&
                keyboardInputHandler.handleKeyUp(keyCode, event, inputConnection,
                        metaKeyManager.isShiftEnabled(), metaKeyManager.isAltEnabled());
    }

    @Override
    public void onUpdateSelection(int oldSelStart, int oldSelEnd, int newSelStart, int newSelEnd, int candidatesStart, int candidatesEnd) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd);
        keyboardInputHandler.onUpdateSelection(getCurrentInputConnection(), newSelStart, newSelEnd, candidatesEnd);
        updateMetaState();
        suggestionsManager.update();

        if (selectionAnchor != -1) {
            if (newSelStart == selectionAnchor) {
                currentCursorPos = newSelEnd;
            } else if (newSelEnd == selectionAnchor) {
                currentCursorPos = newSelStart;
            } else {
                currentCursorPos = newSelEnd;
                selectionAnchor = -1;
            }
        } else {
            currentCursorPos = newSelEnd;
            if (newSelStart != newSelEnd) {
                selectionAnchor = newSelStart;
            }
        }
    }

    public void moveCursor(int offset, boolean isShiftPressed) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;

        int newPos = currentCursorPos + offset;
        if (newPos < 0) newPos = 0;

        if (isShiftPressed) {
            if (selectionAnchor == -1) {
                selectionAnchor = currentCursorPos;
            }
            // Set selection if shift is pressed
            currentCursorPos = newPos;
            ic.setSelection(
                    Math.min(selectionAnchor, newPos),
                    Math.max(selectionAnchor, newPos)
            );
        } else {
            // Otherwise just move cursor
            selectionAnchor = -1;
            currentCursorPos = newPos;
            ic.setSelection(newPos, newPos);
        }
    }

    @Override
    public void onDisplayCompletions(CompletionInfo[] completions) {
        suggestionsManager.update(completions);
    }

    @RequiresApi(Build.VERSION_CODES.R)
    @Nullable
    @Override
    public InlineSuggestionsRequest onCreateInlineSuggestionsRequest(@NonNull Bundle uiExtras) {
        if (!preferencesHolder.isInlineSuggestionsEnabled()) {
            return null;
        }

        UiVersions.StylesBuilder stylesBuilder = UiVersions.newStylesBuilder();

        InlineSuggestionUi.Style style = InlineSuggestionUi
                .newStyleBuilder()
                .build();
        stylesBuilder.addStyle(style);
        Bundle stylesBundle = stylesBuilder.build();

        InlinePresentationSpec spec = new InlinePresentationSpec.Builder(
                new Size(0, 0),
                new Size(Integer.MAX_VALUE, Integer.MAX_VALUE)
        )
                .setStyle(stylesBundle)
                .build();

        return new InlineSuggestionsRequest.Builder(Collections.singletonList(spec))
                .setMaxSuggestionCount(InlineSuggestionsRequest.SUGGESTION_COUNT_UNLIMITED)
                .setExtras(uiExtras)
                .build();
    }

    @RequiresApi(Build.VERSION_CODES.R)
    @Override
    public boolean onInlineSuggestionsResponse(@NonNull InlineSuggestionsResponse response) {
        if (isInputViewShown()) {
            List<InlineSuggestion> inlineSuggestions = response.getInlineSuggestions();
            if (inlineSuggestions != null && !inlineSuggestions.isEmpty()) {
                return suggestionsManager.showInlineSuggestions(inlineSuggestions);
            } else {
                suggestionsManager.cancelInlineSuggestions();
            }
        }

        return false;
    }

    @Override
    public void hideStatusIcon() {
        super.hideStatusIcon();
        symPadJustUsed = true;
    }

    private void updateMetaState() {
        // Auto-capitalize
        if (autoCapitalization) {
            EditorInfo editorInfo = getCurrentInputEditorInfo();
            if (editorInfo != null && (editorInfo.inputType != InputType.TYPE_NULL || keyboardInputHandler.isInRawInputMode())) {
                InputConnection inputConnection = getCurrentInputConnection();
                if (inputConnection != null) {
                    if (inputConnection.getCursorCapsMode(TextUtils.CAP_MODE_SENTENCES) > 0 &&
                            InputUtils.isSuggestionAllowedEditor(editorInfo)) {
                        metaKeyManager.enableShift();
                    } else {
                        metaKeyManager.disableShift();
                    }
                    metaKeyManager.updateAlt();
                }
            }
        } else {
            metaKeyManager.updateShift();
            metaKeyManager.updateAlt();
        }
    }

    public InputMethodManager getInputMethodManager() {
        return inputMethodManager;
    }

    public PreferencesHolder getPreferencesHolder() {
        return preferencesHolder;
    }

    public MetaKeyManager getMetaKeyManager() {
        return metaKeyManager;
    }

    public SuggestionsManager getSuggestionsManager() {
        return suggestionsManager;
    }

    public KeyboardInputHandler getKeyboardInputHandler() {
        return keyboardInputHandler;
    }

    public boolean isShouldShowIme() {
        if (preferencesHolder.isShowPanelEnabled()) {
            return Settings.Secure.getInt(
                    getContentResolver(),
                    "show_ime_with_hard_keyboard",
                    0
            ) == 1;
        }

        return false;
    }
}