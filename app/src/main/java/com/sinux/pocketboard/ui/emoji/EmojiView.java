package com.sinux.pocketboard.ui.emoji;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.widget.LinearLayout;

import androidx.recyclerview.widget.RecyclerView;
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
        super(context, attrs);
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
        viewPager.setOffscreenPageLimit(emojiViewAdapter.getItemCount() - 1);

        // Workaround to prevent removing recent emoji item clipping
        if (viewPager.getChildAt(0) instanceof RecyclerView rv) {
            rv.setClipChildren(false);
            rv.setClipToPadding(false);
            rv.setDefaultFocusHighlightEnabled(false);
        }

        // Create tabs
        for (int i = 0; i < emojiViewAdapter.getItemCount(); i++) {
            int drawableId = emojiViewAdapter.getItemTabDrawableId(i);
            var tab = tabLayout.newTab();
            tab.setIcon(drawableId);
            tab.view.setClipToPadding(false);
            tabLayout.addTab(tab);
        }

        // Wire tabLayout with viewPager
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
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
            }
        });
    }

    public void hidePopup() {
        if (emojiViewAdapter != null) {
            emojiViewAdapter.hidePopup();
        }
    }

    public boolean handleKeyDown(int keyCode) {
        return viewPager != null && viewPager.getCurrentItem() == EmojiViewAdapter.EMOJI_RECENT_TAB_POSITION &&
                emojiViewAdapter != null && emojiViewAdapter.handleKeyDown(keyCode);
    }
}
