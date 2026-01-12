package com.sinux.pocketboard.input;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.UserDictionary;
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
import com.sinux.pocketboard.utils.CharacterUtils;
import com.sinux.pocketboard.utils.InputUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SuggestionsManager implements SuggestionView.OnClickListener,
        SpellCheckerSession.SpellCheckerSessionListener {

    private static final String AOSP_SPELLCHECKER_PACKAGE = "com.android.inputmethod.latin";
    private static final String AOSP_SPELLCHECKER_PACKAGE_OPENBOARD = "org.dslul.openboard.inputmethod.latin";

    private final PocketBoardIME pocketBoardIME;
    private final PreferencesHolder preferencesHolder;
    private final KeyboardInputHandler keyboardInputHandler;

    private final int suggestionsCount;
    private final List<CharSequence> dictionarySuggestions;
    private final List<CharSequence> spellcheckerSuggestions;

    private InputView inputView;
    private SpellCheckerSession spellCheckerSession;

    private boolean dictionarySuggestionsAllowed;
    private boolean spellcheckerSuggestionsAllowed;
    private boolean aospSpellchecker;
    private boolean hasRecommendedSpellcheckerSuggestion;
    private CharSequence lastRecommendedSuggestion;

    public SuggestionsManager(PocketBoardIME pocketBoardIME, KeyboardInputHandler keyboardInputHandler) {
        this.pocketBoardIME = pocketBoardIME;
        this.preferencesHolder = pocketBoardIME.getPreferencesHolder();
        this.keyboardInputHandler = keyboardInputHandler;

        suggestionsCount = pocketBoardIME.getResources().getInteger(R.integer.suggestions_count);
        dictionarySuggestions = new ArrayList<>(suggestionsCount);
        spellcheckerSuggestions = new ArrayList<>(suggestionsCount);
    }

    public void setInputView(InputView inputView) {
        this.inputView = inputView;
    }

    public void onStartInput(EditorInfo attribute, InputMethodSubtype currentInputMethodSubtype) {
        disallowSuggestions();
        var suggestionAllowedEditor = InputUtils.isSuggestionAllowedEditor(attribute) && !InputUtils.isNumericEditor(attribute);
        var suggestionsPanelVisible = pocketBoardIME.isShouldShowIme() && preferencesHolder.isShowSuggestionsEnabled();
        dictionarySuggestionsAllowed = suggestionAllowedEditor && (suggestionsPanelVisible || preferencesHolder.isDictShortcutsEnabled());
        spellcheckerSuggestionsAllowed = suggestionAllowedEditor && (suggestionsPanelVisible || preferencesHolder.isAutoCorrectionEnabled()) &&
                startSpellCheckerSession(currentInputMethodSubtype);
    }

    public void onStartInputView(InputMethodSubtype currentInputMethodSubtype) {
        if (spellcheckerSuggestionsAllowed && spellCheckerSession == null) {
            startSpellCheckerSession(currentInputMethodSubtype);
        }
    }

    public void onFinishInput() {
        closeSpellCheckerSession();
    }

    public void disallowSuggestions() {
        clear();
        closeSpellCheckerSession();
        dictionarySuggestionsAllowed = false;
        spellcheckerSuggestionsAllowed = false;
    }

    public boolean isSuggestionsAllowed() {
        return spellcheckerSuggestionsAllowed || dictionarySuggestionsAllowed;
    }

    public void clear() {
        dictionarySuggestions.clear();
        spellcheckerSuggestions.clear();
        hasRecommendedSpellcheckerSuggestion = false;
        showSuggestions();
    }

    public void update() {
        var composingText = keyboardInputHandler.getCurrentComposingText();

        if (TextUtils.isEmpty(composingText)) {
            clear();
            return;
        }

        if (dictionarySuggestionsAllowed) {
            updateDictionarySuggestions(composingText);
        }

        if (spellcheckerSuggestionsAllowed) {
            if (spellCheckerSession != null && !spellCheckerSession.isSessionDisconnected()) {
                // Magic trick to make the AOSP spellchecker give more satisfying results
                if (aospSpellchecker) {
                    composingText += "#";
                }
                TextInfo[] textInfos = {new TextInfo(composingText, 0, composingText.length(), 0, 0)};
                spellCheckerSession.getSentenceSuggestions(textInfos, suggestionsCount);
            }
        }
    }

    public void update(CompletionInfo[] completions) {
        if (spellcheckerSuggestionsAllowed) {
            spellcheckerSuggestions.clear();
            hasRecommendedSpellcheckerSuggestion = false;
            if (completions != null) {
                if (spellCheckerSession != null) {
                    spellCheckerSession.cancel();
                }
                for (int i = 0; i < completions.length && i < suggestionsCount; i++) {
                    if (!TextUtils.isEmpty(completions[i].getText())) {
                        spellcheckerSuggestions.add(completions[i].getText().toString());
                    }
                }
            }
            showSuggestions();
        }
    }

    private void updateDictionarySuggestions(CharSequence composingText) {
        dictionarySuggestions.clear();

        if (TextUtils.isEmpty(composingText)) {
            return;
        }

        ContentResolver resolver = pocketBoardIME.getContentResolver();
        Uri contentUri = UserDictionary.Words.CONTENT_URI;
        String[] projection = { UserDictionary.Words.WORD };
        String selection = UserDictionary.Words.SHORTCUT + " LIKE ?";
        String[] selectionArgs = { String.valueOf(composingText) };
        String sortOrder = UserDictionary.Words.FREQUENCY + " DESC";

        try (Cursor cursor = resolver.query(
                contentUri,
                projection,
                selection,
                selectionArgs,
                sortOrder
        )) {
            if (cursor != null && cursor.getCount() > 0) {
                var capitalizationType = CharacterUtils.getCapitalizationType(composingText);
                int wordIndex = cursor.getColumnIndex(UserDictionary.Words.WORD);
                while (cursor.moveToNext()) {
                    String word = cursor.getString(wordIndex);
                    if (!TextUtils.isEmpty(word)) {
                        switch (capitalizationType) {
                            case ALL_UPPER -> dictionarySuggestions.add(word.toUpperCase());
                            case FIRST_UPPER -> dictionarySuggestions.add(CharacterUtils.capitalizeFirstLetter(word));
                            case NONE -> dictionarySuggestions.add(word);
                        }
                    }
                }
            }
        } catch (Exception ignored) { }

        showSuggestions();
    }

    public CharSequence getCurrentDictSuggestion() {
        if (!dictionarySuggestions.isEmpty()) {
            return dictionarySuggestions.get(0);
        }

        return null;
    }

    public CharSequence getCurrentSpellcheckerRecommendedSuggestion() {
        if (hasRecommendedSpellcheckerSuggestion && !spellcheckerSuggestions.isEmpty()) {
            lastRecommendedSuggestion = spellcheckerSuggestions.get(0);
            return lastRecommendedSuggestion;
        }

        return null;
    }

    public void onInputMethodSubtypeChanged(InputMethodSubtype inputMethodSubtype) {
        closeSpellCheckerSession();
        if (spellcheckerSuggestionsAllowed) {
            spellcheckerSuggestionsAllowed = startSpellCheckerSession(inputMethodSubtype);
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
        if (spellcheckerSuggestionsAllowed) {
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
                    spellcheckerSuggestions.clear();
                    spellcheckerSuggestions.addAll(tempSuggestions);
                    if (hasRecommended &&
                            lastRecommendedSuggestion != null &&
                            lastRecommendedSuggestion.equals(tempSuggestions.get(0))) {
                        hasRecommendedSpellcheckerSuggestion = false;
                    } else {
                        hasRecommendedSpellcheckerSuggestion = hasRecommended;
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
            var merged = Stream.concat(dictionarySuggestions.stream(), spellcheckerSuggestions.stream())
                    .limit(3)
                    .collect(Collectors.toList());
            var hasRecommended = !dictionarySuggestions.isEmpty() || hasRecommendedSpellcheckerSuggestion;
            inputView.setSuggestions(merged, hasRecommended);
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
