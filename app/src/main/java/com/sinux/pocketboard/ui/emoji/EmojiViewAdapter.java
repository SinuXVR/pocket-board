package com.sinux.pocketboard.ui.emoji;

import android.os.Build;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sinux.pocketboard.PocketBoardIME;
import com.sinux.pocketboard.R;
import com.sinux.pocketboard.utils.LruList;
import com.sinux.pocketboard.utils.CharacterUtils;

import java.util.List;

public class EmojiViewAdapter extends RecyclerView.Adapter<EmojiViewAdapter.EmojiViewHolder>
        implements EmojiItemClickListener {

    public static final int EMOJI_RECENT_TAB_POSITION = 0;

    // List of emoji categories ({tab icon id, content string id} pairs)
    private static final int[][] EMOJI_CATEGORIES = {
            {R.drawable.emoji_recent, -1},
            {R.drawable.emoji_symbols, R.string.emoji_symbols},
            {R.drawable.emoji_emotions, R.string.emoji_emotions},
            {R.drawable.emoji_nature, R.string.emoji_nature},
            {R.drawable.emoji_food, R.string.emoji_food},
            {R.drawable.emoji_sports, R.string.emoji_sports},
            {R.drawable.emoji_transport, R.string.emoji_transport},
            {R.drawable.emoji_objects, R.string.emoji_objects},
            {R.drawable.emoji_others, R.string.emoji_others}
    };

    private final PocketBoardIME pocketBoardIME;
    private final List<CharSequence> recentEmojiList;
    private final PopupWindow popupWindow;

    public EmojiViewAdapter(PocketBoardIME pocketBoardIME, ViewGroup parent) {
        this.pocketBoardIME = pocketBoardIME;

        // Load recent emoji
        recentEmojiList = new LruList<>(pocketBoardIME.getResources().getInteger(R.integer.recent_emoji_max_count));
        String recentEmojiString = pocketBoardIME.getPreferencesHolder().getRecentEmojiString();
        recentEmojiList.clear();
        if (!TextUtils.isEmpty(recentEmojiString)) {
            recentEmojiList.addAll(CharacterUtils.splitToCharacters(recentEmojiString));
        }

        // Prepare emoji variation select popup window
        LinearLayout popupContent = (LinearLayout) LayoutInflater.from(parent.getContext()).inflate(R.layout.emoji_view, parent, false);
        popupWindow = new PopupWindow(parent.getContext());
        popupWindow.setContentView(popupContent);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            popupWindow.setTouchModal(true);
        }
        popupWindow.setOutsideTouchable(true);
        popupWindow.setBackgroundDrawable(ContextCompat.getDrawable(pocketBoardIME, R.drawable.emoji_popup_background));
        popupWindow.setElevation(10);
        popupWindow.setOverlapAnchor(true);
    }

    @NonNull
    @Override
    public EmojiViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new EmojiViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.emoji_view, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull EmojiViewHolder holder, int position) {
        View itemView = holder.itemView;
        EmojiContentViewAdapter adapter = null;

        if (position == EMOJI_RECENT_TAB_POSITION) {
            adapter = new EmojiContentViewAdapter(recentEmojiList);
        } else if (EMOJI_CATEGORIES[position][1] > 0) {
            String emojiStr = pocketBoardIME.getString(EMOJI_CATEGORIES[position][1]);
            adapter = new EmojiContentViewAdapter(CharacterUtils.splitToCharacters(emojiStr));
        }

        if (adapter != null) {
            RecyclerView contentView = itemView.findViewById(R.id.contentView);
            contentView.setLayoutManager(new GridLayoutManager(pocketBoardIME,
                    pocketBoardIME.getResources().getInteger(R.integer.emoji_view_column_count)));
            contentView.setAdapter(adapter);
            adapter.setItemClickListener(this);
        }
    }

    @Override
    public int getItemCount() {
        return EMOJI_CATEGORIES.length;
    }

    public int getItemTabDrawableId(int position) {
        return EMOJI_CATEGORIES[position][0];
    }

    @Override
    public void onEmojiItemClick(CharSequence itemValue) {
        pocketBoardIME.getKeyboardInputHandler().commitEmoji(itemValue);
        // Save recent emoji
        recentEmojiList.add(itemValue);
        pocketBoardIME.getPreferencesHolder().saveRecentEmojiString(String.join("", recentEmojiList));
        hidePopup();
    }

    @Override
    public void onEmojiItemLongClick(View emojiItemView, CharSequence itemValue) {
        if (!TextUtils.isEmpty(itemValue)) {
            int[] fitzpatrickAwareEmojis = pocketBoardIME.getResources().getIntArray(R.array.fitzpatrick_aware_emojis);
            List<CharSequence> fitzpatrickVariants = CharacterUtils.getAllFitzpatrickVariants(itemValue, fitzpatrickAwareEmojis);
            if (!fitzpatrickVariants.isEmpty()) {
                EmojiContentViewAdapter adapter = new EmojiContentViewAdapter(fitzpatrickVariants);
                RecyclerView contentView = popupWindow.getContentView().findViewById(R.id.contentView);
                contentView.setLayoutManager(new GridLayoutManager(pocketBoardIME, fitzpatrickVariants.size()));
                contentView.setAdapter(adapter);
                adapter.setItemClickListener(this);
                popupWindow.showAsDropDown(emojiItemView);
            }
        }
    }

    public void hidePopup() {
        popupWindow.dismiss();
    }

    public static class EmojiViewHolder extends RecyclerView.ViewHolder {
        public EmojiViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
