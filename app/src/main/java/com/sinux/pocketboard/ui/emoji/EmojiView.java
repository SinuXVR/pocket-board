package com.sinux.pocketboard.ui.emoji;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.widget.LinearLayout;

import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.sinux.pocketboard.PocketBoardIME;
import com.sinux.pocketboard.R;

public class EmojiView extends LinearLayout {

    private final PocketBoardIME pocketBoardIME;

    private EmojiViewAdapter emojiViewAdapter;
    private ViewPager2 viewPager;
    private TabLayout tabLayout;

    public EmojiView(Context context, AttributeSet attrs) {
        super(context, attrs, 0);
        if (context instanceof ContextThemeWrapper) {
            this.pocketBoardIME = (PocketBoardIME) ((ContextThemeWrapper) context).getBaseContext();
        } else {
            this.pocketBoardIME = (PocketBoardIME) context;
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        viewPager = findViewById(R.id.emojiViewPager);
        tabLayout = findViewById(R.id.emojiTabLayout);

        emojiViewAdapter = new EmojiViewAdapter(pocketBoardIME, this);
        viewPager.setAdapter(emojiViewAdapter);

        // Create tabs
        for (int i = 0; i < emojiViewAdapter.getItemCount(); i++) {
            int drawableId = emojiViewAdapter.getItemTabDrawableId(i);
            tabLayout.addTab(tabLayout.newTab().setIcon(drawableId));
        }

        // Wire tabLayout with viewPager
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
                if (tab.getPosition() == EmojiViewAdapter.EMOJI_RECENT_TAB_POSITION) {
                    viewPager.post(() -> emojiViewAdapter.notifyItemChanged(EmojiViewAdapter.EMOJI_RECENT_TAB_POSITION));
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        // Wire viewPager with tabLayout
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                tabLayout.selectTab(tabLayout.getTabAt(position));
                if (position == EmojiViewAdapter.EMOJI_RECENT_TAB_POSITION) {
                    viewPager.post(() -> emojiViewAdapter.notifyItemChanged(EmojiViewAdapter.EMOJI_RECENT_TAB_POSITION));
                }
            }
        });
    }

    public void hidePopup() {
        if (emojiViewAdapter != null) {
            emojiViewAdapter.hidePopup();
        }
    }
}
