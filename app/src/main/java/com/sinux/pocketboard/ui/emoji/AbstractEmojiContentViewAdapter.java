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
    protected EmojiItemClickListener itemClickListener;

    public AbstractEmojiContentViewAdapter(List<CharSequence> data) {
        this.data = data;
    }

    @Override
    public void onBindViewHolder(@NonNull V holder, int position) {
        holder.textView.setText(data.get(position));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public void setItemClickListener(EmojiItemClickListener itemClickListener) {
        this.itemClickListener = itemClickListener;
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
