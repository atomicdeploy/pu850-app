package com.pandcaspian.indicator.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import com.pandcaspian.indicator.MainActivity;
import com.pandcaspian.indicator.R;

/**
 * Elegant splash screen with stunning animations
 */
public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 2500;
    
    private ImageView logoImage;
    private TextView appNameText;
    private TextView taglineText;
    private View glowEffect;
    private View loadingIndicator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Handle the splash screen transition
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Keep the splash screen visible until our custom animation starts
        splashScreen.setKeepOnScreenCondition(() -> false);

        // Disable back button during splash
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Do nothing - back button disabled during splash
            }
        });

        initViews();
        startAnimations();
    }

    private void initViews() {
        logoImage = findViewById(R.id.logoImage);
        appNameText = findViewById(R.id.appNameText);
        taglineText = findViewById(R.id.taglineText);
        glowEffect = findViewById(R.id.glowEffect);
        loadingIndicator = findViewById(R.id.loadingIndicator);

        // Initial state - invisible
        logoImage.setAlpha(0f);
        logoImage.setScaleX(0.5f);
        logoImage.setScaleY(0.5f);
        
        appNameText.setAlpha(0f);
        appNameText.setTranslationY(50f);
        
        taglineText.setAlpha(0f);
        taglineText.setTranslationY(30f);
        
        glowEffect.setAlpha(0f);
        glowEffect.setScaleX(0.3f);
        glowEffect.setScaleY(0.3f);
        
        loadingIndicator.setAlpha(0f);
    }

    private void startAnimations() {
        // Glow effect animation
        ObjectAnimator glowFadeIn = ObjectAnimator.ofFloat(glowEffect, "alpha", 0f, 0.6f);
        ObjectAnimator glowScaleX = ObjectAnimator.ofFloat(glowEffect, "scaleX", 0.3f, 1.2f);
        ObjectAnimator glowScaleY = ObjectAnimator.ofFloat(glowEffect, "scaleY", 0.3f, 1.2f);
        
        AnimatorSet glowSet = new AnimatorSet();
        glowSet.playTogether(glowFadeIn, glowScaleX, glowScaleY);
        glowSet.setDuration(800);
        glowSet.setInterpolator(new AccelerateDecelerateInterpolator());

        // Logo animation with overshoot
        ObjectAnimator logoFadeIn = ObjectAnimator.ofFloat(logoImage, "alpha", 0f, 1f);
        ObjectAnimator logoScaleX = ObjectAnimator.ofFloat(logoImage, "scaleX", 0.5f, 1f);
        ObjectAnimator logoScaleY = ObjectAnimator.ofFloat(logoImage, "scaleY", 0.5f, 1f);
        
        AnimatorSet logoSet = new AnimatorSet();
        logoSet.playTogether(logoFadeIn, logoScaleX, logoScaleY);
        logoSet.setDuration(600);
        logoSet.setInterpolator(new OvershootInterpolator(1.5f));
        logoSet.setStartDelay(200);

        // App name animation
        ObjectAnimator nameFadeIn = ObjectAnimator.ofFloat(appNameText, "alpha", 0f, 1f);
        ObjectAnimator nameSlideUp = ObjectAnimator.ofFloat(appNameText, "translationY", 50f, 0f);
        
        AnimatorSet nameSet = new AnimatorSet();
        nameSet.playTogether(nameFadeIn, nameSlideUp);
        nameSet.setDuration(500);
        nameSet.setInterpolator(new AccelerateDecelerateInterpolator());
        nameSet.setStartDelay(500);

        // Tagline animation
        ObjectAnimator taglineFadeIn = ObjectAnimator.ofFloat(taglineText, "alpha", 0f, 1f);
        ObjectAnimator taglineSlideUp = ObjectAnimator.ofFloat(taglineText, "translationY", 30f, 0f);
        
        AnimatorSet taglineSet = new AnimatorSet();
        taglineSet.playTogether(taglineFadeIn, taglineSlideUp);
        taglineSet.setDuration(400);
        taglineSet.setInterpolator(new AccelerateDecelerateInterpolator());
        taglineSet.setStartDelay(700);

        // Loading indicator
        ObjectAnimator loadingFadeIn = ObjectAnimator.ofFloat(loadingIndicator, "alpha", 0f, 1f);
        loadingFadeIn.setDuration(300);
        loadingFadeIn.setStartDelay(900);

        // Continuous glow pulsing
        ObjectAnimator glowPulse = ObjectAnimator.ofFloat(glowEffect, "alpha", 0.4f, 0.7f);
        glowPulse.setDuration(1000);
        glowPulse.setRepeatMode(ValueAnimator.REVERSE);
        glowPulse.setRepeatCount(ValueAnimator.INFINITE);
        glowPulse.setStartDelay(800);

        // Start all animations
        glowSet.start();
        logoSet.start();
        nameSet.start();
        taglineSet.start();
        loadingFadeIn.start();
        glowPulse.start();

        // Navigate to main activity after delay
        new Handler(Looper.getMainLooper()).postDelayed(this::navigateToMain, SPLASH_DURATION);
    }

    private void navigateToMain() {
        // Fade out animation before transitioning
        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(
            getWindow().getDecorView(), "alpha", 1f, 0f
        );
        fadeOut.setDuration(300);
        fadeOut.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                startActivity(intent);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, R.anim.fade_in, R.anim.fade_out);
                } else {
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                }
                finish();
            }
        });
        fadeOut.start();
    }
}
