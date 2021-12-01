package com.sinux.pocketboard.ui.emoji;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sinux.pocketboard.R;

import java.util.List;

public class EmojiContentViewAdapter extends RecyclerView.Adapter<EmojiContentViewAdapter.EmojiItemViewHolder> {

    private final List<CharSequence> data;
    private EmojiItemClickListener itemClickListener;

    public EmojiContentViewAdapter(List<CharSequence> data) {
        this.data = data;
    }

    @NonNull
    @Override
    public EmojiItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.emoji_grid_item_view, parent, false);
        return new EmojiItemViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull EmojiItemViewHolder holder, int position) {
        holder.textView.setText(data.get(position));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public void setItemClickListener(EmojiItemClickListener itemClickListener) {
        this.itemClickListener = itemClickListener;
    }

    public class EmojiItemViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener, View.OnLongClickListener {

        private final TextView textView;

        public EmojiItemViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.contentItemValue);
            textView.setOnClickListener(this);
            textView.setOnLongClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (itemClickListener != null) {
                itemClickListener.onEmojiItemClick(textView.getText());
            }
        }

        @Override
        public boolean onLongClick(View v) {
            if (itemClickListener != null) {
                itemClickListener.onEmojiItemLongClick(itemView, textView.getText());
                return true;
            }

            return false;
        }
    }
}
