package com.sinux.pocketboard.ui.emoji;

import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.color.MaterialColors;
import com.sinux.pocketboard.R;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class RecentEmojiContentViewAdapter extends AbstractEmojiContentViewAdapter<RecentEmojiContentViewAdapter.RecentEmojiItemViewHolder> {

    private final String[] recentEmojiShortcutLabels;

    public RecentEmojiContentViewAdapter(
            List<CharSequence> data, String[] recentEmojiShortcutLabels,
            RecyclerView recyclerView, ImageView removeArea, EmojiViewAdapter emojiViewAdapter
    ) {
        super(data, emojiViewAdapter);
        this.recentEmojiShortcutLabels = recentEmojiShortcutLabels;
        bindItemTouchHelper(recyclerView, removeArea);
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

    private void bindItemTouchHelper(RecyclerView recyclerView, ImageView removeArea) {
        var buttonNormalTint = ColorStateList.valueOf(MaterialColors.getColor(recyclerView, androidx.appcompat.R.attr.colorButtonNormal));
        var emojiRemoveTint = ColorStateList.valueOf(ContextCompat.getColor(recyclerView.getContext(), R.color.emoji_remove_color));

        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN |
                        ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT, 0
        ) {
            private RecyclerView.ViewHolder draggingViewHolder = null;

            @Override
            public boolean onMove(@NotNull RecyclerView rv, RecyclerView.ViewHolder src, RecyclerView.ViewHolder target) {
                int from = src.getBindingAdapterPosition();
                int to = target.getBindingAdapterPosition();

                // Swap recent emojis
                if (emojiViewAdapter.swapRecentEmojiItem(from, to)) {
                    RecentEmojiContentViewAdapter.this.notifyItemMoved(from, to);
                    RecentEmojiContentViewAdapter.this.notifyItemRangeChanged(
                            Math.min(from, to),
                            Math.abs(from - to) + 1
                    );
                    return true;
                }

                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            }

            @Override
            public void onChildDraw(
                    @NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder,
                    float dX, float dY, int actionState, boolean isCurrentlyActive
            ) {
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    // Change emoji color and transparency when user tries to remove it
                    boolean isRemoving = isCurrentlyActive && isViewOutside(viewHolder.itemView);
                    viewHolder.itemView.setAlpha(isRemoving ? 0.5f : 1f);
                    ViewCompat.setBackgroundTintList(viewHolder.itemView, isRemoving ? emojiRemoveTint : null);
                    ImageViewCompat.setImageTintList(removeArea, isRemoving ? emojiRemoveTint : buttonNormalTint);
                }

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }

            @Override
            public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
                super.onSelectedChanged(viewHolder, actionState);

                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    draggingViewHolder = viewHolder;
                    removeArea.setVisibility(View.VISIBLE);
                } else if (actionState == ItemTouchHelper.ACTION_STATE_IDLE && draggingViewHolder != null) {
                    // Remove recent emoji item if user released it above the top of recyclerView
                    if (isViewOutside(draggingViewHolder.itemView)) {
                        int position = draggingViewHolder.getBindingAdapterPosition();
                        if (position != RecyclerView.NO_POSITION && emojiViewAdapter.removeRecentEmojiItem(position)) {
                            draggingViewHolder.itemView.setVisibility(View.GONE);
                            notifyItemRemoved(position);
                            notifyItemRangeChanged(position, getItemCount() - position);
                        }
                    }
                    removeArea.setVisibility(View.GONE);
                    draggingViewHolder = null;
                }
            }

            private boolean isViewOutside(View view) {
                return view.getY() + (view.getHeight() / 2f) <= 0;
            }
        };

        new ItemTouchHelper(callback).attachToRecyclerView(recyclerView);
    }

    public class RecentEmojiItemViewHolder extends AbstractEmojiContentViewAdapter<RecentEmojiItemViewHolder>.AbstractEmojiItemViewHolder {

        private final TextView shortcutKeyLabelView;

        public RecentEmojiItemViewHolder(@NonNull View itemView) {
            super(itemView);
            shortcutKeyLabelView = itemView.findViewById(R.id.contentItemShortcutKeyLabel);
        }
    }
}
