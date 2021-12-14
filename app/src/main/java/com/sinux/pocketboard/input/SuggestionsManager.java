package com.sinux.pocketboard.input;

import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InlineSuggestion;
import android.view.inputmethod.InputMethodSubtype;
import android.view.textservice.SentenceSuggestionsInfo;
import android.view.textservice.SpellCheckerSession;
import android.view.textservice.SuggestionsInfo;
import android.view.textservice.TextInfo;
import android.view.textservice.TextServicesManager;

import androidx.annotation.RequiresApi;

import com.sinux.pocketboard.PocketBoardIME;
import com.sinux.pocketboard.R;
import com.sinux.pocketboard.input.handler.KeyboardInputHandler;
import com.sinux.pocketboard.preferences.PreferencesHolder;
import com.sinux.pocketboard.ui.InputView;
import com.sinux.pocketboard.ui.SuggestionView;
import com.sinux.pocketboard.utils.InputUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SuggestionsManager implements SuggestionView.OnClickListener,
        SpellCheckerSession.SpellCheckerSessionListener {

    private static final String AOSP_SPELLCHECKER_PACKAGE = "com.android.inputmethod.latin";
    private static final String AOSP_SPELLCHECKER_PACKAGE_OPENBOARD = "org.dslul.openboard.inputmethod.latin";

    private final PocketBoardIME pocketBoardIME;
    private final PreferencesHolder preferencesHolder;
    private final KeyboardInputHandler keyboardInputHandler;

    private final int suggestionsCount;
    private final List<CharSequence> suggestions;

    private InputView inputView;
    private SpellCheckerSession spellCheckerSession;

    private boolean suggestionsAllowed;
    private boolean aospSpellchecker;
    private boolean hasRecommendedSuggestion;
    private CharSequence lastRecommendedSuggestion;

    public SuggestionsManager(PocketBoardIME pocketBoardIME, KeyboardInputHandler keyboardInputHandler) {
        this.pocketBoardIME = pocketBoardIME;
        this.preferencesHolder = pocketBoardIME.getPreferencesHolder();
        this.keyboardInputHandler = keyboardInputHandler;

        suggestionsCount = pocketBoardIME.getResources().getInteger(R.integer.suggestions_count);
        suggestions = new ArrayList<>(suggestionsCount);
    }

    public void setInputView(InputView inputView) {
        this.inputView = inputView;
    }

    public void onStartInput(EditorInfo attribute, InputMethodSubtype currentInputMethodSubtype) {
        disallowSuggestions();
        suggestionsAllowed = preferencesHolder.isShowSuggestionsEnabled() && preferencesHolder.isShowPanelEnabled() &&
                InputUtils.isSuggestionAllowedEditor(attribute) && !InputUtils.isNumericEditor(attribute) &&
                startSpellCheckerSession(currentInputMethodSubtype);
    }

    public void onStartInputView(InputMethodSubtype currentInputMethodSubtype) {
        if (suggestionsAllowed && spellCheckerSession == null) {
            startSpellCheckerSession(currentInputMethodSubtype);
        }
    }

    public void onFinishInput() {
        closeSpellCheckerSession();
    }

    public void disallowSuggestions() {
        clear();
        closeSpellCheckerSession();
        suggestionsAllowed = false;
    }

    public boolean isSuggestionsAllowed() {
        return suggestionsAllowed;
    }

    public void clear() {
        suggestions.clear();
        hasRecommendedSuggestion = false;
        showSuggestions();
    }

    public void update() {
        if (suggestionsAllowed) {
            if (spellCheckerSession != null && !spellCheckerSession.isSessionDisconnected()) {
                CharSequence composingText = keyboardInputHandler.getCurrentComposingText();
                if (!TextUtils.isEmpty(composingText)) {
                    // Magic trick to make the AOSP spellchecker give more satisfying results
                    if (aospSpellchecker) {
                        composingText += "#";
                    }
                    spellCheckerSession.getSentenceSuggestions(new TextInfo[]{
                            new TextInfo(composingText, 0, composingText.length(), 0, 0)
                    }, suggestionsCount);
                } else {
                    clear();
                }
            }
        }
    }

    public void update(CompletionInfo[] completions) {
        if (suggestionsAllowed) {
            suggestions.clear();
            hasRecommendedSuggestion = false;
            if (completions != null) {
                if (spellCheckerSession != null) {
                    spellCheckerSession.cancel();
                }
                for (int i = 0; i < completions.length && i < suggestionsCount; i++) {
                    suggestions.add(completions[i].getText().toString());
                }
            }
            showSuggestions();
        }
    }

    public CharSequence getCurrentRecommendedSuggestion() {
        if (hasRecommendedSuggestion && suggestions.size() > 0) {
            lastRecommendedSuggestion = suggestions.get(0);
            return lastRecommendedSuggestion;
        }
        return null;
    }

    public void onInputMethodSubtypeChanged(InputMethodSubtype inputMethodSubtype) {
        closeSpellCheckerSession();
        if (suggestionsAllowed) {
            suggestionsAllowed = startSpellCheckerSession(inputMethodSubtype);
        }
    }

    @Override
    public void onClick(View v) {
        applySuggestion(((SuggestionView) v).getText());
    }

    @Override
    public void onGetSuggestions(SuggestionsInfo[] results) {
    }

    @Override
    public void onGetSentenceSuggestions(SentenceSuggestionsInfo[] results) {
        if (suggestionsAllowed) {
            if (results != null) {
                List<CharSequence> tempSuggestions = new ArrayList<>(suggestionsCount);
                boolean hasRecommended = false;
                for (SentenceSuggestionsInfo ssi : results) {
                    if (ssi == null) {
                        continue;
                    }

                    for (int i = 0; i < ssi.getSuggestionsCount(); i++) {
                        SuggestionsInfo si = ssi.getSuggestionsInfoAt(i);
                        if (si == null) {
                            continue;
                        }

                        hasRecommended |= (si.getSuggestionsAttributes() & SuggestionsInfo.RESULT_ATTR_HAS_RECOMMENDED_SUGGESTIONS) != 0;

                        for (int j = 0; j < si.getSuggestionsCount(); j++) {
                            if (!TextUtils.isEmpty(si.getSuggestionAt(j))) {
                                tempSuggestions.add(si.getSuggestionAt(j));
                            }
                        }

                    }
                }

                if (!tempSuggestions.isEmpty()) {
                    suggestions.clear();
                    suggestions.addAll(tempSuggestions);
                    if (hasRecommended &&
                            lastRecommendedSuggestion != null &&
                            lastRecommendedSuggestion.equals(tempSuggestions.get(0))) {
                        hasRecommendedSuggestion = false;
                    } else {
                        hasRecommendedSuggestion = hasRecommended;
                    }
                    showSuggestions();
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    public boolean showInlineSuggestions(List<InlineSuggestion> inlineSuggestions) {
        if (inputView != null) {
            return inputView.setInlineSuggestions(inlineSuggestions);
        } else {
            return false;
        }
    }

    public boolean isInlineSuggestionsShown() {
        if (inputView != null) {
            return inputView.isInlineSuggestionsShown();
        } else {
            return false;
        }
    }

    public void cancelInlineSuggestions() {
        if (inputView != null) {
            inputView.cancelInlineSuggestions();
        }
    }

    private void showSuggestions() {
        if (inputView != null) {
            inputView.setSuggestions(suggestions, hasRecommendedSuggestion);
        }
    }

    private void applySuggestion(CharSequence text) {
        if (!TextUtils.isEmpty(text)) {
            keyboardInputHandler.applySuggestion(text, pocketBoardIME.getCurrentInputConnection(), true);
        }
    }

    private boolean startSpellCheckerSession(InputMethodSubtype inputMethodSubtype) {
        Locale locale = Locale.forLanguageTag(inputMethodSubtype.getLanguageTag());
        TextServicesManager tsm = (TextServicesManager) pocketBoardIME.getSystemService(Context.TEXT_SERVICES_MANAGER_SERVICE);
        spellCheckerSession = tsm.newSpellCheckerSession(null, locale, this, false);

        if (spellCheckerSession != null) {
            aospSpellchecker = spellCheckerSession.getSpellChecker() != null &&
                    (AOSP_SPELLCHECKER_PACKAGE.equals(spellCheckerSession.getSpellChecker().getPackageName()) ||
                            AOSP_SPELLCHECKER_PACKAGE_OPENBOARD.equals(spellCheckerSession.getSpellChecker().getPackageName()));
            return true;
        }

        return false;
    }

    private void closeSpellCheckerSession() {
        if (spellCheckerSession != null) {
            spellCheckerSession.cancel();
            spellCheckerSession.close();
            spellCheckerSession = null;
        }
    }
}
