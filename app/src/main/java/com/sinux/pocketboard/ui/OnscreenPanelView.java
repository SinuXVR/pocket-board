package com.sinux.pocketboard.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import com.google.android.material.color.MaterialColors;
import com.sinux.pocketboard.PocketBoardIME;
import com.sinux.pocketboard.R;
import com.sinux.pocketboard.preferences.PreferencesHolder;

import java.util.function.Consumer;

public class OnscreenPanelView extends LinearLayout {

    private final PocketBoardIME pocketBoardIME;
    private final PreferencesHolder preferencesHolder;

    private boolean virtualTouchpadEnabled;
    private float virtualTouchpadDragThreshold;
    private float virtualTouchpadDragStartX;

    private final Paint paint;
    private boolean shouldPlayTouchpadAnimation;

    public OnscreenPanelView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        if (context instanceof ContextThemeWrapper) {
            this.pocketBoardIME = (PocketBoardIME) ((ContextThemeWrapper) context).getBaseContext();
        } else {
            this.pocketBoardIME = (PocketBoardIME) context;
        }

        preferencesHolder = pocketBoardIME.getPreferencesHolder();

        virtualTouchpadEnabled = preferencesHolder.isVirtualTouchpadEnabled();
        preferencesHolder.registerPreferenceChangeListener(
                context.getString(R.string.ime_virtual_touchpad_prefs_key),
                value -> {
                    virtualTouchpadEnabled = (Boolean) value;
                    shouldPlayTouchpadAnimation = virtualTouchpadEnabled;
                }
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

        paint = new Paint();
        shouldPlayTouchpadAnimation = virtualTouchpadEnabled && !preferencesHolder.isVirtualTouchpadAnimationShown();
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

    public void onStartInputView(boolean isVisible) {
        if (isVisible && shouldPlayTouchpadAnimation) {
            preferencesHolder.saveVirtualTouchpadAnimationShownValue(true);
            shouldPlayTouchpadAnimation = false;
            playVirtualTouchpadAnimation();
        }
    }

    private void playVirtualTouchpadAnimation() {
        if (paint.getShader() != null)
            return;

        var matrix = new Matrix();
        var baseColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnPrimary);
        var highlightColor = MaterialColors.getColor(this, androidx.appcompat.R.attr.colorButtonNormal);
        var shader = new LinearGradient(
                0, 0, getResources().getDisplayMetrics().widthPixels / 2f, 0,
                new int[] { baseColor, highlightColor, baseColor },
                new float[] { 0f, 0.5f, 1f },
                Shader.TileMode.CLAMP
        );
        paint.setShader(shader);
        setWillNotDraw(false);

        var animator = ValueAnimator.ofFloat(-1f, 1f);
        animator.setDuration(1500);
        animator.setRepeatCount(2);

        animator.addUpdateListener(animation -> {
            float translateX = (float) animation.getAnimatedValue() * getWidth();
            matrix.setTranslate(translateX, 0);
            shader.setLocalMatrix(matrix);
            invalidate();
        });

        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                paint.setShader(null);
                setWillNotDraw(true);
                invalidate();
                animator.removeAllListeners();
                animator.removeAllUpdateListeners();
            }
        });

        animator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
    }
}
