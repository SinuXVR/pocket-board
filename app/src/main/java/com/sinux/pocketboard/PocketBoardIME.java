package com.sinux.pocketboard;

import android.annotation.SuppressLint;
import android.inputmethodservice.InputMethodService;
import android.os.Build;
import android.os.Bundle;
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
import com.sinux.pocketboard.ui.emoji.EmojiView;
import com.sinux.pocketboard.utils.InputUtils;
import com.sinux.pocketboard.utils.ToastMessageUtils;
import com.sinux.pocketboard.utils.VoiceInputUtils;

import java.util.Collections;
import java.util.List;

public class PocketBoardIME extends InputMethodService {

    private InputMethodManager inputMethodManager;
    private PreferencesHolder preferencesHolder;
    private MetaKeyManager metaKeyManager;
    private KeyboardInputHandler keyboardInputHandler;
    private SymPadInputHandler symPadInputHandler;
    private EmojiView emojiView;
    private InputView inputView;
    private SuggestionsManager suggestionsManager;

    private boolean emojiPanelVisible;
    private boolean autoCapitalization;
    private boolean symPadJustUsed;
    private boolean symPadModeLocked;

    private int sympadFixEventRepeatCount;

    @Override
    public void onCreate() {
        super.onCreate();

        inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        preferencesHolder = new PreferencesHolder(this);

        metaKeyManager = new MetaKeyManager(this);
        keyboardInputHandler = new KeyboardInputHandler(this);
        symPadInputHandler = new SymPadInputHandler(this);
        suggestionsManager = new SuggestionsManager(this, keyboardInputHandler);

        sympadFixEventRepeatCount = getResources().getInteger(R.integer.sympad_fix_event_repeat_count);
    }

    @Override
    public void onDestroy() {
        metaKeyManager.destroy();
        super.onDestroy();
    }

    /**
     * Use candidates view to display additional emoji panel on demand
     */
    @SuppressLint("InflateParams")
    @Override
    public View onCreateCandidatesView() {
        emojiPanelVisible = false;
        View candidatesView = getLayoutInflater().inflate(R.layout.emoji_panel, null);
        emojiView = candidatesView.findViewById(R.id.emojiView);
        return candidatesView;
    }

    @Override
    public void setCandidatesViewShown(boolean shown) {
        super.setCandidatesViewShown(shown);
        emojiPanelVisible = shown;
        if (!shown && emojiView != null) {
            emojiView.hidePopup();
        }
    }

    public boolean isEmojiPanelVisible() {
        return emojiPanelVisible;
    }

    @Override
    public void onWindowHidden() {
        super.onWindowHidden();
        if (emojiView != null) {
            emojiView.hidePopup();
        }
    }

    /**
     * Workaround to update UI properly on candidates view show/hide
     */
    @Override
    public void onComputeInsets(Insets outInsets) {
        super.onComputeInsets(outInsets);
        outInsets.contentTopInsets = outInsets.visibleTopInsets;
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
        setCandidatesViewShown(false);
        autoCapitalization = preferencesHolder.isAutoCapitalizationEnabled();

        if (!restarting) {
            metaKeyManager.reset();
        }

        InputMethodSubtype currentInputMethodSubtype = inputMethodManager.getCurrentInputMethodSubtype();
        suggestionsManager.onStartInput(attribute, currentInputMethodSubtype);

        int cursorPosition = -1;
        InputConnection inputConnection = getCurrentInputConnection();
        if (inputConnection != null) {
            ExtractedText extractedText = inputConnection.getExtractedText(new ExtractedTextRequest(), 0);
            if (extractedText != null) {
                cursorPosition = Math.min(extractedText.selectionStart, extractedText.selectionEnd);
            }
        }
        keyboardInputHandler.onStartInput(attribute, suggestionsManager.isSuggestionsAllowed(), cursorPosition);

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
    protected void onCurrentInputMethodSubtypeChanged(InputMethodSubtype newSubtype) {
        super.onCurrentInputMethodSubtypeChanged(newSubtype);
        if (isInputViewShown()) {
            if (preferencesHolder.isLayoutChangeIndicationEnabled()) {
                ToastMessageUtils.showMessage(this, newSubtype.getNameResId());
            }
            suggestionsManager.onInputMethodSubtypeChanged(newSubtype);
            keyboardInputHandler.onInputMethodSubtypeChanged(newSubtype, suggestionsManager.isSuggestionsAllowed());
            inputView.onInputMethodSubtypeChanged(newSubtype, suggestionsManager.isSuggestionsAllowed());
            updateMetaState();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                if (isInputViewShown()) {
                    if (emojiPanelVisible) {
                        setCandidatesViewShown(false);
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
            case KeyEvent.KEYCODE_SYM:
                if (event.getRepeatCount() == 0 && symPadModeLocked) {
                    hideStatusIcon();
                    symPadModeLocked = false;
                    symPadJustUsed = true;
                } else if (event.getRepeatCount() == sympadFixEventRepeatCount && preferencesHolder.isLockSymPadEnabled()) {
                    symPadModeLocked = true;
                    showStatusIcon(R.drawable.ic_sym_pad_icon);
                }
                return true;
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
                if (event.isSymPressed() || symPadModeLocked) {
                    symPadInputHandler.handleKeyDown(keyCode, event, inputConnection,
                            metaKeyManager.isShiftEnabled(), metaKeyManager.isAltEnabled());
                    symPadJustUsed = true;
                    return true;
                }

                if (keyCode == KeyEvent.KEYCODE_ENTER) {
                    return false;
                }

                // Handle text input in Keyboard mode
                EditorInfo editorInfo = getCurrentInputEditorInfo();
                if (editorInfo != null && editorInfo.inputType != InputType.TYPE_NULL) {
                    if (keyboardInputHandler.handleKeyDown(keyCode, event, inputConnection,
                            metaKeyManager.isShiftEnabled(), metaKeyManager.isAltEnabled())) {
                        // Show input view if it's hidden
                        if (!isInputViewShown()) {
                            requestShowSelf(0);
                        }
                        updateMetaState();
                        suggestionsManager.update();
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

        if (inputConnection == null) {
            return false;
        }

        if (keyCode == KeyEvent.KEYCODE_SYM) {
            if (!symPadJustUsed && !symPadModeLocked) {
                // Toggle emoji panel on SYM release
                if (isInputViewShown()) {
                    setCandidatesViewShown(!emojiPanelVisible);
                }
            } else {
                symPadJustUsed = false;
            }

            return true;
        }

        if (event.isSymPressed() || symPadModeLocked) {
            if (symPadInputHandler.handleKeyUp(keyCode, event, inputConnection,
                    metaKeyManager.isShiftEnabled(), metaKeyManager.isAltEnabled())) {
                return true;
            }
        }

        return metaKeyManager.handleKeyUp(keyCode, event);
    }

    @Override
    public void onUpdateSelection(int oldSelStart, int oldSelEnd, int newSelStart, int newSelEnd, int candidatesStart, int candidatesEnd) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd);
        if (isInputViewShown()) {
            keyboardInputHandler.onUpdateSelection(getCurrentInputConnection(), newSelStart, newSelEnd, candidatesEnd);
            updateMetaState();
            suggestionsManager.update();
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

    private void updateMetaState() {
        // Auto-capitalize
        if (autoCapitalization) {
            EditorInfo editorInfo = getCurrentInputEditorInfo();
            if (editorInfo != null && editorInfo.inputType != InputType.TYPE_NULL) {
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
}