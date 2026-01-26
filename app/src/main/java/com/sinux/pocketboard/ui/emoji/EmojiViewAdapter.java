package com.sinux.pocketboard.ui.emoji;

import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sinux.pocketboard.PocketBoardIME;
import com.sinux.pocketboard.R;
import com.sinux.pocketboard.preferences.PreferencesHolder;
import com.sinux.pocketboard.utils.LruList;
import com.sinux.pocketboard.utils.CharacterUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EmojiViewAdapter extends RecyclerView.Adapter<EmojiViewAdapter.EmojiViewHolder> {

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
    private final PreferencesHolder preferencesHolder;
    private final HashSet<Integer> fitzpatrickAwareEmojis;
    private final List<CharSequence> recentEmojiList;
    private final Map<Integer, Integer> recentEmojiShortcutKeys;
    private final PopupWindow popupWindow;
    private final ImageView recentEmojiRemoveArea;

    private RecentEmojiContentViewAdapter recentEmojiContentViewAdapter;

    public EmojiViewAdapter(PocketBoardIME pocketBoardIME, ViewGroup parent) {
        this.pocketBoardIME = pocketBoardIME;
        this.preferencesHolder = pocketBoardIME.getPreferencesHolder();

        fitzpatrickAwareEmojis = Arrays.stream(pocketBoardIME.getResources().getIntArray(R.array.fitzpatrick_aware_emojis))
                .boxed()
                .collect(Collectors.toCollection(HashSet::new));

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

        // Prepare recent emoji shortcut key codes
        int[] shortcutKeys = pocketBoardIME.getResources().getIntArray(R.array.recent_emoji_shortcut_key_codes);
        recentEmojiShortcutKeys = new HashMap<>(recentEmojiList.size());
        for (int i = 0; i < shortcutKeys.length; i++) {
            recentEmojiShortcutKeys.put(shortcutKeys[i], i);
        }

        recentEmojiRemoveArea = parent.findViewById(R.id.emojiRemoveArea);
    }

    @NonNull
    @Override
    public EmojiViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new EmojiViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.emoji_view, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull EmojiViewHolder holder, int position) {
        View itemView = holder.itemView;
        RecyclerView contentView = itemView.findViewById(R.id.contentView);
        AbstractEmojiContentViewAdapter<?> adapter;

        if (position == EMOJI_RECENT_TAB_POSITION) {
            var recentEmojiShortcutLabels = pocketBoardIME.getResources().getStringArray(R.array.recent_emoji_shortcut_labels);
            recentEmojiContentViewAdapter = new RecentEmojiContentViewAdapter(
                    recentEmojiList, recentEmojiShortcutLabels, contentView, recentEmojiRemoveArea, this
            );
            adapter = recentEmojiContentViewAdapter;
            ((ViewGroup) holder.itemView).setClipChildren(false);
            ((ViewGroup) holder.itemView).setClipToPadding(false);
        } else if (EMOJI_CATEGORIES[position][1] > 0) {
            String emojiStr = pocketBoardIME.getString(EMOJI_CATEGORIES[position][1]);
            adapter = new EmojiContentViewAdapter(CharacterUtils.splitToCharacters(emojiStr), this);
        } else {
            adapter = null;
        }

        if (adapter != null) {
            contentView.setLayoutManager(new GridLayoutManager(pocketBoardIME,
                    pocketBoardIME.getResources().getInteger(R.integer.emoji_view_column_count)));
            contentView.setAdapter(adapter);
        }
    }

    @Override
    public int getItemCount() {
        return EMOJI_CATEGORIES.length;
    }

    public int getItemTabDrawableId(int position) {
        return EMOJI_CATEGORIES[position][0];
    }

    public void onEmojiItemClick(CharSequence itemValue) {
        pocketBoardIME.getKeyboardInputHandler().commitEmoji(itemValue);
        // Save recent emoji
        if (!preferencesHolder.isManualRecentEmojiEnabled()) {
            addToRecentEmoji(itemValue);
        }
    }

    public void onEmojiItemLongClick(View emojiItemView, CharSequence itemValue) {
        if (!TextUtils.isEmpty(itemValue)) {
            List<CharSequence> fitzpatrickVariants = CharacterUtils.getAllFitzpatrickVariants(itemValue, fitzpatrickAwareEmojis);
            if (!fitzpatrickVariants.isEmpty()) {
                // Display skin color variants
                var adapter = new FitzpatrickEmojiContentViewAdapter(fitzpatrickVariants, this);
                RecyclerView contentView = popupWindow.getContentView().findViewById(R.id.contentView);
                contentView.setLayoutManager(new GridLayoutManager(pocketBoardIME, fitzpatrickVariants.size()));
                contentView.setAdapter(adapter);
                popupWindow.showAsDropDown(emojiItemView);
            } else {
                // Display add to Recent tab popup
                showAddToRecentEmojiPopup(emojiItemView, itemValue);
            }
        }
    }

    public void showAddToRecentEmojiPopup(View emojiItemView, CharSequence itemValue) {
        if (preferencesHolder.isManualRecentEmojiEnabled() && !recentEmojiList.contains(itemValue)) {
            var adapter = new AddToRecentEmojiContentViewAdapter(List.of(itemValue), this);
            RecyclerView contentView = popupWindow.getContentView().findViewById(R.id.contentView);
            contentView.setLayoutManager(new GridLayoutManager(pocketBoardIME, 1));
            contentView.setAdapter(adapter);
            popupWindow.showAsDropDown(emojiItemView);
        }
    }

    public void addToRecentEmoji(CharSequence itemValue) {
        if (!recentEmojiList.contains(itemValue)) {
            recentEmojiList.add(itemValue);
            saveRecentEmojiList();
            // Refresh recent tab
            if (recentEmojiContentViewAdapter != null) {
                recentEmojiContentViewAdapter.notifyItemRangeChanged(
                        0,
                        recentEmojiContentViewAdapter.getItemCount()
                );
            }
        }
        hidePopup();
    }

    public boolean swapRecentEmojiItem(int from, int to) {
        if (from >= 0 && from < recentEmojiList.size() && to >= 0 && to <= recentEmojiList.size()) {
            CharSequence item = recentEmojiList.remove(from);
            recentEmojiList.add(to, item);
            saveRecentEmojiList();
            return true;
        }

        return false;
    }

    public boolean removeRecentEmojiItem(int position) {
        if (position >= 0 && position < recentEmojiList.size()) {
            recentEmojiList.remove(position);
            saveRecentEmojiList();
            return true;
        }

        return false;
    }

    public void hidePopup() {
        popupWindow.dismiss();
    }

    public boolean handleKeyDown(int keyCode) {
        var recentEmojiPosition = recentEmojiShortcutKeys.get(keyCode);

        if (recentEmojiPosition != null && recentEmojiPosition < recentEmojiList.size()) {
            pocketBoardIME.getKeyboardInputHandler().commitEmoji(recentEmojiList.get(recentEmojiPosition));
            return true;
        }

        return false;
    }

    private void saveRecentEmojiList() {
        pocketBoardIME.getPreferencesHolder().saveRecentEmojiString(String.join("", recentEmojiList));
    }

    public static class EmojiViewHolder extends RecyclerView.ViewHolder {
        public EmojiViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
