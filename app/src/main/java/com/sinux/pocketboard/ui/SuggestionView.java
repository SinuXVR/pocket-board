package com.sinux.pocketboard.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sinux.pocketboard.R;

public class SuggestionView extends LinearLayout {

    private final TextView textView;

    public SuggestionView(Context context) {
        super(context);
        inflate(context, R.layout.suggestion_layout, this);
        textView = findViewById(R.id.suggestionText);
    }

    public void setDividerVisible(boolean dividerIsVisible) {
        View dividerView = findViewById(R.id.suggestionDivider);
        dividerView.setVisibility(dividerIsVisible ? VISIBLE : GONE);
    }

    public void setText(CharSequence text, boolean isRecommended) {
        textView.setText(text);
        textView.setTypeface(isRecommended ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        textView.setPressed(isRecommended);
    }

    public CharSequence getText() {
        return textView.getText();
    }
}
