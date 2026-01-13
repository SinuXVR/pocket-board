package com.sinux.pocketboard.ui.emoji;

import android.view.View;

public interface EmojiItemClickListener {

    void onEmojiItemClick(CharSequence itemValue);

    void onEmojiItemLongClick(View emojiItemView, CharSequence itemValue);

    default boolean removeRecentEmojiItem(int position) {
        return false;
    }

}
