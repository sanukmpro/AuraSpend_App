package com.auraspend.app;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private View ambientGlowTop;
    private View ambientGlowBottom;
    private View logoContainer;
    private View splashHaloRing;
    private View splashLogo;
    private View splashTitle;
    private View splashSubtitle;
    private View splashBottomBar;

    private ObjectAnimator haloPulseAnimator;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean hasNavigated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        initViews();
        startBootAnimation();
    }

    private void initViews() {
        ambientGlowTop = findViewById(R.id.ambientGlowTop);
        ambientGlowBottom = findViewById(R.id.ambientGlowBottom);
        logoContainer = findViewById(R.id.logoContainer);
        splashHaloRing = findViewById(R.id.splashHaloRing);
        splashLogo = findViewById(R.id.splashLogo);
        splashTitle = findViewById(R.id.splashTitle);
        splashSubtitle = findViewById(R.id.splashSubtitle);
        splashBottomBar = findViewById(R.id.splashBottomBar);

        // Initial setup for animated properties
        logoContainer.setScaleX(0.5f);
        logoContainer.setScaleY(0.5f);
        logoContainer.setAlpha(0f);

        splashHaloRing.setScaleX(0.5f);
        splashHaloRing.setScaleY(0.5f);
        splashHaloRing.setAlpha(0f);

        splashTitle.setTranslationY(40f);
        splashTitle.setAlpha(0f);

        splashSubtitle.setTranslationY(30f);
        splashSubtitle.setAlpha(0f);

        splashBottomBar.setAlpha(0f);
    }

    private void startBootAnimation() {
        // 1. Ambient Background Glow Orbs Fade In
        ambientGlowTop.animate()
                .alpha(0.45f)
                .setDuration(900)
                .start();

        ambientGlowBottom.animate()
                .alpha(0.45f)
                .setDuration(900)
                .start();

        // 2. Halo Expansion behind Logo
        splashHaloRing.animate()
                .scaleX(1.3f)
                .scaleY(1.3f)
                .alpha(0.75f)
                .setDuration(800)
                .setInterpolator(new DecelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        // Start gentle continuous breathing pulse
                        startHaloBreathingPulse();
                    }
                })
                .start();

        // 3. Central Logo Pop & Spring (Overshoot)
        logoContainer.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .alpha(1.0f)
                .setDuration(750)
                .setInterpolator(new OvershootInterpolator(1.4f))
                .start();

        // 4. App Title Slide-Up & Fade-In (300ms delay)
        splashTitle.animate()
                .translationY(0f)
                .alpha(1.0f)
                .setStartDelay(300)
                .setDuration(600)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        // 5. Subtitle Slide-Up & Fade-In (450ms delay)
        splashSubtitle.animate()
                .translationY(0f)
                .alpha(1.0f)
                .setStartDelay(450)
                .setDuration(600)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        // 6. Bottom Bar Fade-In (600ms delay)
        splashBottomBar.animate()
                .alpha(1.0f)
                .setStartDelay(600)
                .setDuration(500)
                .start();

        // 7. Transition to MainActivity after boot-up animation completes
        handler.postDelayed(this::navigateToMain, 1850);
    }

    private void startHaloBreathingPulse() {
        if (isFinishing() || isDestroyed()) return;

        PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1.3f, 1.45f);
        PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.3f, 1.45f);
        PropertyValuesHolder alpha = PropertyValuesHolder.ofFloat(View.ALPHA, 0.75f, 0.45f);

        haloPulseAnimator = ObjectAnimator.ofPropertyValuesHolder(splashHaloRing, scaleX, scaleY, alpha);
        haloPulseAnimator.setDuration(800);
        haloPulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        haloPulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
        haloPulseAnimator.start();
    }

    private void navigateToMain() {
        if (hasNavigated) return;
        hasNavigated = true;

        if (haloPulseAnimator != null) {
            haloPulseAnimator.cancel();
        }

        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        startActivity(intent);
        if (android.os.Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, android.R.anim.fade_in, android.R.anim.fade_out);
        } else {
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }
        finish();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (haloPulseAnimator != null) {
            haloPulseAnimator.cancel();
        }
        super.onDestroy();
    }
}
