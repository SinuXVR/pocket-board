package com.sinux.pocketboard.ui.emoji;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.sinux.pocketboard.R;

import java.util.List;

public class AddToRecentEmojiContentViewAdapter extends AbstractEmojiContentViewAdapter<AddToRecentEmojiContentViewAdapter.AddToRecentEmojiItemViewHolder> {

    public AddToRecentEmojiContentViewAdapter(List<CharSequence> data, EmojiViewAdapter emojiViewAdapter) {
        super(data, emojiViewAdapter);
    }

    @NonNull
    @Override
    public AddToRecentEmojiItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.add_to_recent_emoji_grid_item_view, parent, false);
        return new AddToRecentEmojiItemViewHolder(itemView);
    }

    public class AddToRecentEmojiItemViewHolder extends AbstractEmojiContentViewAdapter<AddToRecentEmojiItemViewHolder>.AbstractEmojiItemViewHolder {

        public AddToRecentEmojiItemViewHolder(@NonNull View itemView) {
            super(itemView);
            var button = itemView.findViewById(R.id.contentItemButton);
            button.setOnClickListener(this);
            button.setOnLongClickListener(this);
        }

        @Override
        public void onClick(View v) {
            emojiViewAdapter.addToRecentEmoji(textView.getText());
        }

        @Override
        public boolean onLongClick(View v) {
            return true;
        }
    }
}
