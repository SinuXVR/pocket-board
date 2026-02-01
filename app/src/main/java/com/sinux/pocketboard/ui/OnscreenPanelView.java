package com.sinux.pocketboard.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import com.sinux.pocketboard.PocketBoardIME;
import com.sinux.pocketboard.R;
import com.sinux.pocketboard.preferences.PreferencesHolder;

import java.util.function.Consumer;


public class OnscreenPanelView extends LinearLayout {

    private final PocketBoardIME pocketBoardIME;

    private boolean virtualTouchpadEnabled;
    private float virtualTouchpadDragThreshold;
    private float virtualTouchpadDragStartX;

    public OnscreenPanelView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        if (context instanceof ContextThemeWrapper) {
            this.pocketBoardIME = (PocketBoardIME) ((ContextThemeWrapper) context).getBaseContext();
        } else {
            this.pocketBoardIME = (PocketBoardIME) context;
        }

        PreferencesHolder preferencesHolder = pocketBoardIME.getPreferencesHolder();

        virtualTouchpadEnabled = preferencesHolder.isVirtualTouchpadEnabled();
        preferencesHolder.registerPreferenceChangeListener(
                context.getString(R.string.ime_virtual_touchpad_prefs_key),
                value -> virtualTouchpadEnabled = (Boolean) value
        );

        Consumer<Integer> setTouchpadDragThreshold = (value) ->
                virtualTouchpadDragThreshold = context.getResources().getDisplayMetrics().density * (
                        context.getResources().getInteger(R.integer.virtual_touchpad_speed_max_value) +
                                context.getResources().getInteger(R.integer.virtual_touchpad_speed_min_value) -
                                value
                );
        setTouchpadDragThreshold.accept(preferencesHolder.getVirtualTouchpadSpeed());
        preferencesHolder.registerPreferenceChangeListener(
                context.getString(R.string.ime_virtual_touchpad_speed_prefs_key),
                value -> setTouchpadDragThreshold.accept((Integer) value)
        );
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (virtualTouchpadEnabled) {
            switch (ev.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    virtualTouchpadDragStartX = ev.getX();
                    break;
                case MotionEvent.ACTION_MOVE:
                    float diffX = ev.getX() - virtualTouchpadDragStartX;
                    if (Math.abs(diffX) > virtualTouchpadDragThreshold) {
                        boolean isShiftPressed = (ev.getMetaState() & KeyEvent.META_SHIFT_ON) != 0;
                        pocketBoardIME.moveCursor((int) (diffX / virtualTouchpadDragThreshold), isShiftPressed);
                        virtualTouchpadDragStartX = ev.getX();
                    }
                    break;
            }
        }

        return super.dispatchTouchEvent(ev);
    }
}
