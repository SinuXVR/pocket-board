package com.sinux.pocketboard.ui.emoji;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.sinux.pocketboard.R;

import java.util.List;

public class EmojiContentViewAdapter extends AbstractEmojiContentViewAdapter<EmojiContentViewAdapter.EmojiItemViewHolder> {

    public EmojiContentViewAdapter(List<CharSequence> data, EmojiViewAdapter emojiViewAdapter) {
        super(data, emojiViewAdapter);
    }

    @NonNull
    @Override
    public EmojiItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.emoji_grid_item_view, parent, false);
        return new EmojiItemViewHolder(itemView);
    }

    public class EmojiItemViewHolder extends AbstractEmojiContentViewAdapter<EmojiItemViewHolder>.AbstractEmojiItemViewHolder {
        public EmojiItemViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
