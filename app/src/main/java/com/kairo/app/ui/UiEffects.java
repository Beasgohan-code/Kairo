package com.kairo.app.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LayoutAnimationController;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.view.animation.AlphaAnimation;
import android.view.animation.AnimationSet;

/**
 * Lightweight motion + blur helpers. Uses platform APIs only (no extra libraries).
 * Blur via {@link RenderEffect} on API 31+; older devices get soft glass-style overlays.
 */
public final class UiEffects {
    private UiEffects() {
    }

    public static void fadeIn(View view, long durationMs) {
        if (view == null) return;
        view.setAlpha(0f);
        view.setVisibility(View.VISIBLE);
        view.animate()
                .alpha(1f)
                .setDuration(durationMs)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    public static void fadeOut(View view, long durationMs, Runnable endAction) {
        if (view == null) {
            if (endAction != null) endAction.run();
            return;
        }
        view.animate()
                .alpha(0f)
                .setDuration(durationMs)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> {
                    if (endAction != null) endAction.run();
                })
                .start();
    }

    /** Cross-fade style content swap on a container. */
    public static void transitionContent(ViewGroup container, Runnable rebuild) {
        if (container == null) {
            if (rebuild != null) rebuild.run();
            return;
        }
        container.animate().cancel();
        container.animate()
                .alpha(0f)
                .setDuration(140)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> {
                    if (rebuild != null) rebuild.run();
                    container.setAlpha(0f);
                    container.animate()
                            .alpha(1f)
                            .setDuration(200)
                            .setInterpolator(new DecelerateInterpolator())
                            .start();
                })
                .start();
    }

    public static void slideUpFadeIn(View view, long durationMs) {
        if (view == null) return;
        view.setAlpha(0f);
        view.setTranslationY(24f);
        view.setVisibility(View.VISIBLE);
        view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(durationMs)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    /** Soft blur on API 31+; no-op on older APIs (callers should use translucent backgrounds). */
    public static void applyBlur(View view, float radiusPx) {
        if (view == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                view.setRenderEffect(RenderEffect.createBlurEffect(radiusPx, radiusPx, Shader.TileMode.CLAMP));
            } catch (Exception ignored) {
            }
        }
    }

    public static void clearBlur(View view) {
        if (view == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            view.setRenderEffect(null);
        }
    }

    public static void enableLayoutTransitions(ViewGroup group) {
        if (group == null) return;
        group.setLayoutTransition(new android.animation.LayoutTransition());
        android.animation.LayoutTransition lt = group.getLayoutTransition();
        if (lt != null) {
            lt.enableTransitionType(android.animation.LayoutTransition.CHANGING);
            lt.setDuration(180);
        }
    }
}
