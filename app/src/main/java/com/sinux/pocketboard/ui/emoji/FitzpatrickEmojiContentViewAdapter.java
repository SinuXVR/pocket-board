package com.sinux.pocketboard.ui.emoji;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.sinux.pocketboard.R;

import java.util.List;

public class FitzpatrickEmojiContentViewAdapter extends AbstractEmojiContentViewAdapter<FitzpatrickEmojiContentViewAdapter.FitzpatrickEmojiItemViewHolder> {

    public FitzpatrickEmojiContentViewAdapter(List<CharSequence> data, EmojiViewAdapter emojiViewAdapter) {
        super(data, emojiViewAdapter);
    }

    @NonNull
    @Override
    public FitzpatrickEmojiItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.emoji_grid_item_view, parent, false);
        return new FitzpatrickEmojiItemViewHolder(itemView);
    }

    public class FitzpatrickEmojiItemViewHolder extends AbstractEmojiContentViewAdapter<FitzpatrickEmojiItemViewHolder>.AbstractEmojiItemViewHolder {
        public FitzpatrickEmojiItemViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        @Override
        public boolean onLongClick(View v) {
            emojiViewAdapter.showAddToRecentEmojiPopup(itemView, textView.getText());
            return true;
        }
    }
}
