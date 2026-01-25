package com.sinux.pocketboard.ui.emoji;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sinux.pocketboard.R;

import java.util.List;

public abstract class AbstractEmojiContentViewAdapter<V extends AbstractEmojiContentViewAdapter<?>.AbstractEmojiItemViewHolder>
        extends RecyclerView.Adapter<V> {

    private final List<CharSequence> data;
    protected final EmojiViewAdapter emojiViewAdapter;

    public AbstractEmojiContentViewAdapter(List<CharSequence> data, EmojiViewAdapter emojiViewAdapter) {
        this.data = data;
        this.emojiViewAdapter = emojiViewAdapter;
    }

    @Override
    public void onBindViewHolder(@NonNull V holder, int position) {
        holder.textView.setText(data.get(position));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public class AbstractEmojiItemViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener, View.OnLongClickListener {

        protected final TextView textView;

        public AbstractEmojiItemViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.contentItemValue);
            textView.setOnClickListener(this);
            textView.setOnLongClickListener(this);
        }

        @Override
        public void onClick(View v) {
            emojiViewAdapter.onEmojiItemClick(textView.getText());
        }

        @Override
        public boolean onLongClick(View v) {
            emojiViewAdapter.onEmojiItemLongClick(itemView, textView.getText());
            return true;
        }
    }
}
