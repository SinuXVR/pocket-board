package com.sinux.pocketboard.ui.emoji;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.sinux.pocketboard.R;

import java.util.List;

public class RecentEmojiContentViewAdapter extends AbstractEmojiContentViewAdapter<RecentEmojiContentViewAdapter.RecentEmojiItemViewHolder> {

    private final String[] recentEmojiShortcutLabels;

    public RecentEmojiContentViewAdapter(List<CharSequence> data, String[] recentEmojiShortcutLabels) {
        super(data);
        this.recentEmojiShortcutLabels = recentEmojiShortcutLabels;
    }

    @NonNull
    @Override
    public RecentEmojiItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.recent_emoji_grid_item_view, parent, false);
        return new RecentEmojiItemViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull RecentEmojiItemViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        holder.shortcutKeyLabelView.setText(recentEmojiShortcutLabels[position]);
    }

    public class RecentEmojiItemViewHolder extends AbstractEmojiContentViewAdapter<RecentEmojiItemViewHolder>.AbstractEmojiItemViewHolder {

        private final TextView shortcutKeyLabelView;

        public RecentEmojiItemViewHolder(@NonNull View itemView) {
            super(itemView);
            shortcutKeyLabelView = itemView.findViewById(R.id.contentItemShortcutKeyLabel);
        }

        @Override
        public boolean onLongClick(View v) {
            // Delete emoji on long click
            int position = getLayoutPosition();
            if (itemClickListener != null && itemClickListener.removeRecentEmojiItem(position)) {
                notifyItemRemoved(position);
                notifyItemRangeChanged(position, getItemCount() - position);
                return true;
            }

            return false;
        }
    }
}
