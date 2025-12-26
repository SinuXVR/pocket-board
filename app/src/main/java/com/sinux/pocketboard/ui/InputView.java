package com.sinux.pocketboard.ui;

import android.content.Context;
import android.graphics.Paint;
import android.os.Build;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Size;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InlineSuggestion;
import android.view.inputmethod.InputMethodSubtype;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.annotation.RequiresApi;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.sinux.pocketboard.PocketBoardIME;
import com.sinux.pocketboard.R;
import com.sinux.pocketboard.input.MetaKeyManager;
import com.sinux.pocketboard.input.MetaKeyStateChangeListener;
import com.sinux.pocketboard.preferences.PreferencesHolder;
import com.sinux.pocketboard.ui.emoji.EmojiView;
import com.sinux.pocketboard.utils.InputUtils;
import com.sinux.pocketboard.utils.VoiceInputUtils;

import java.util.ArrayList;
import java.util.List;

public class InputView extends RelativeLayout implements MetaKeyStateChangeListener {

    private static final String META_BUTTON_ALT_TAG = "alt";
    private static final String META_BUTTON_NUMERIC_TAG = "123";
    private static final String INPUT_METHOD_DISPLAY_TAG = "DisplayTag";

    private final PocketBoardIME pocketBoardIME;
    private final PreferencesHolder preferencesHolder;
    private final List<SuggestionView> suggestions;

    private ViewGroup emojiPanelView;
    private EmojiView emojiView;
    private ViewGroup mainInputView;
    private ViewGroup suggestionsView;
    private ViewGroup inlineSuggestionsViewWrapper;
    private ViewGroup inlineSuggestionsView;

    private ImageButton hideInlineSuggestionsButton;
    private ImageButton emojiButton;
    private ImageButton fontButton;
    private Button metaLayoutButton;
    private ImageButton voiceButton;

    private boolean inlineSuggestionsCancelled;
    private String currentInputMethodTag = "?";
    private boolean isCapsEnabled = false;

    public InputView(Context context, AttributeSet attrs) {
        super(context, attrs, 0);
        if (context instanceof ContextThemeWrapper) {
            this.pocketBoardIME = (PocketBoardIME) ((ContextThemeWrapper) context).getBaseContext();
        } else {
            this.pocketBoardIME = (PocketBoardIME) context;
        }
        preferencesHolder = pocketBoardIME.getPreferencesHolder();
        suggestions = new ArrayList<>(pocketBoardIME.getResources().getInteger(R.integer.suggestions_count));
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        emojiPanelView = findViewById(R.id.emojiPanel);
        emojiView = findViewById(R.id.emojiView);
        mainInputView = findViewById(R.id.inputView);
        suggestionsView = findViewById(R.id.suggestionsView);
        inlineSuggestionsViewWrapper = findViewById(R.id.inlineSuggestionsViewWrapper);
        inlineSuggestionsView = findViewById(R.id.inlineSuggestionsView);

        hideInlineSuggestionsButton = findViewById(R.id.hideInlineSuggestions);
        hideInlineSuggestionsButton.setOnClickListener(b -> cancelInlineSuggestions());

        // Create suggestions views
        int suggestionsCount = pocketBoardIME.getResources().getInteger(R.integer.suggestions_count);
        for (int i = 0; i < suggestionsCount; i++) {
            SuggestionView suggestion = new SuggestionView(getContext());
            suggestion.setDividerVisible(i < suggestionsCount - 1);
            suggestion.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, 1));
            suggestion.setOnClickListener(pocketBoardIME.getSuggestionsManager());
            suggestions.add(suggestion);
        }

        // Put suggestions views to the container in custom order (first element must be in the middle of screen)
        for (int i = 1; i < suggestionsCount; i++) {
            suggestionsView.addView(suggestions.get(i));
        }
        suggestionsView.addView(suggestions.get(0), suggestionsCount / 2);

        emojiButton = findViewById(R.id.emojiButton);
        emojiButton.setOnClickListener(b -> toggleEmojiPanel());

        fontButton = findViewById(R.id.fontButton);
        fontButton.setOnClickListener(b -> toggleCapsStyle());

        metaLayoutButton = findViewById(R.id.metaLayoutButton);
        metaLayoutButton.setOnClickListener(b -> {
            MetaKeyManager metaKeyManager = pocketBoardIME.getMetaKeyManager();
            if (metaKeyManager.isAltEnabled()) {
                metaKeyManager.reset();
            } else {
                pocketBoardIME.switchToNextInputMethod(true);
            }
        });
        metaLayoutButton.setOnLongClickListener(b -> {
            PreferencesHolder.launchInputMethodSubtypeSettings(pocketBoardIME, pocketBoardIME.getInputMethodManager());
            return true;
        });

        voiceButton = findViewById(R.id.voiceButton);
        voiceButton.setOnClickListener(b -> VoiceInputUtils.launchVoiceIME(pocketBoardIME));

        // Insets handling
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ViewCompat.setOnApplyWindowInsetsListener(this, (v, insets) -> {
                Insets imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime());
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(
                        systemBars.left,
                        systemBars.top,
                        systemBars.right,
                        imeInsets.bottom + systemBars.bottom
                );

                return insets;
            });
        }
    }

    public void onStartInputView(EditorInfo attribute, InputMethodSubtype currentInputMethodSubtype,
                                 boolean suggestionsAllowed) {
        inlineSuggestionsCancelled = false;
        inlineSuggestionsViewWrapper.setVisibility(GONE);
        hideEmojiPanel();
        clearInlineSuggestionsView();

        boolean showEmojiButton = preferencesHolder.isShowEmojiEnabled();
        boolean showMetaLayoutButton = preferencesHolder.isShowMetaLayoutEnabled();
        boolean showVoiceButton = preferencesHolder.isShowVoiceEnabled();
        boolean showMainInputView = (showEmojiButton || suggestionsAllowed || showMetaLayoutButton || showVoiceButton) &&
                preferencesHolder.isShowPanelEnabled();

        // Display current layout tag
        if (InputUtils.isNumericEditor(attribute)) {
            currentInputMethodTag = META_BUTTON_NUMERIC_TAG;
            updateMetaLayoutButtonText(pocketBoardIME.getMetaKeyManager());
        } else {
            onInputMethodSubtypeChanged(currentInputMethodSubtype, suggestionsAllowed);
        }

        // Update controls visibility
        if (showMainInputView) {
            emojiButton.setVisibility(showEmojiButton ? VISIBLE : GONE);
            suggestionsView.setVisibility(suggestionsAllowed ? VISIBLE : GONE);
            metaLayoutButton.setVisibility(showMetaLayoutButton ? VISIBLE : GONE);
            voiceButton.setVisibility(showVoiceButton ? VISIBLE : GONE);
            mainInputView.setVisibility(VISIBLE);
        } else {
            mainInputView.setVisibility(GONE);
        }

        // Invalidate view to prevent glitches
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            post(() -> {
                requestLayout();
                invalidate();
            });
        }
    }

    @Override
    public void onMetaKeyStateChanged(MetaKeyManager metaKeyManager) {
        updateMetaLayoutButtonText(metaKeyManager);
    }

    public void onInputMethodSubtypeChanged(InputMethodSubtype inputMethodSubtype, boolean suggestionsAllowed) {
        suggestionsView.setVisibility(suggestionsAllowed ? VISIBLE : GONE);
        String displayTag = inputMethodSubtype.getExtraValueOf(INPUT_METHOD_DISPLAY_TAG);
        if (TextUtils.isEmpty(displayTag)) {
            displayTag = inputMethodSubtype.getLanguageTag();
        }
        if (TextUtils.isEmpty(displayTag)) {
            displayTag = "?";
        }
        currentInputMethodTag = displayTag;
        updateMetaLayoutButtonText(pocketBoardIME.getMetaKeyManager());
    }

    @RequiresApi(Build.VERSION_CODES.R)
    public boolean setInlineSuggestions(List<InlineSuggestion> inlineSuggestions) {
        if (!inlineSuggestionsCancelled) {
            float height =
                    TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP,
                            getResources().getDimension(R.dimen.inline_suggestions_height),
                            getResources().getDisplayMetrics()
                    );
            Size suggestionSize = new Size(ViewGroup.LayoutParams.WRAP_CONTENT, (int) height);
            clearInlineSuggestionsView();
            boolean hasItemsToDisplay = false;

            for (InlineSuggestion inlineSuggestion : inlineSuggestions) {
                // Skip pinned action because it doesn't work properly with small keyboard input view
                if (inlineSuggestion.getInfo().isPinned()) {
                    continue;
                }

                try {
                    inlineSuggestion.inflate(
                            getContext(),
                            suggestionSize,
                            getContext().getMainExecutor(),
                            inlineContentView -> {
                                inlineContentView.setOnClickListener(e -> inlineSuggestionsViewWrapper.setVisibility(GONE));
                                inlineSuggestionsView.addView(inlineContentView);
                            });
                    hasItemsToDisplay = true;
                } catch (Exception ignored) {
                }
            }

            inlineSuggestionsViewWrapper.setVisibility(hasItemsToDisplay ? VISIBLE : GONE);

            return hasItemsToDisplay;
        }

        return false;
    }

    public boolean isInlineSuggestionsShown() {
        return inlineSuggestionsViewWrapper.getVisibility() == VISIBLE;
    }

    public void cancelInlineSuggestions() {
        inlineSuggestionsCancelled = true;
        inlineSuggestionsViewWrapper.setVisibility(GONE);
    }

    public void setSuggestions(List<CharSequence> values, boolean hasRecommendedSuggestion) {
        int valuesCount = values.size();
        for (int i = 0; i < suggestions.size(); i++) {
            SuggestionView suggestion = suggestions.get(i);
            if (i < valuesCount) {
                suggestion.setText(values.get(i), hasRecommendedSuggestion && i == 0);
            } else {
                suggestion.setText("", false);
            }
        }
    }

    private void toggleCapsStyle() {
        isCapsEnabled = !isCapsEnabled;
        pocketBoardIME.setCapsEnabled(isCapsEnabled);
        updateMetaLayoutButtonText(pocketBoardIME.getMetaKeyManager());
        fontButton.setSelected(isCapsEnabled);
    }

    private void updateMetaLayoutButtonText(MetaKeyManager metaKeyManager) {
        String displayTag = currentInputMethodTag;

        if (metaKeyManager.isAltEnabled()) {
            displayTag = META_BUTTON_ALT_TAG;
        }

        if (isCapsEnabled || metaKeyManager.isShiftFixed()) {
            displayTag = displayTag.toUpperCase();
        } else if (metaKeyManager.isShiftEnabled()) {
            displayTag = displayTag.substring(0, 1).toUpperCase() + displayTag.substring(1).toLowerCase();
        }

        if (metaKeyManager.isAltFixed()) {
            metaLayoutButton.setPaintFlags(metaLayoutButton.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        } else {
            metaLayoutButton.setPaintFlags(metaLayoutButton.getPaintFlags() & (~Paint.UNDERLINE_TEXT_FLAG));
        }

        metaLayoutButton.setText(displayTag);
    }

    private void clearInlineSuggestionsView() {
        inlineSuggestionsView.removeAllViews();
        inlineSuggestionsView.addView(hideInlineSuggestionsButton);
    }

    public boolean isEmojiPanelVisible() {
        return emojiPanelView != null && emojiPanelView.getVisibility() == VISIBLE;
    }

    public void toggleEmojiPanel() {
        if (isEmojiPanelVisible()) {
            hideEmojiPanel();
        } else {
            showEmojiPanel();
        }
    }

    public void hideEmojiPanel() {
        if (emojiPanelView != null) {
            emojiView.hidePopup();
            emojiPanelView.setVisibility(GONE);
        }
    }

    private void showEmojiPanel() {
        if (emojiPanelView  != null) {
            emojiPanelView.setVisibility(VISIBLE);
        }
    }
}
