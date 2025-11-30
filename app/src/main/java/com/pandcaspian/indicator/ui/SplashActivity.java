package com.pandcaspian.indicator.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat;

import com.pandcaspian.indicator.MainActivity;
import com.pandcaspian.indicator.R;

/**
 * Splash screen displaying the animated Pand Caspian logo.
 * The logo animation uses an AnimatedVectorDrawable that draws the outline
 * sequentially, then fills in the shape with a smooth alpha transition.
 */
public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 2800; // Time for logo animation + brief pause
    
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
        logoImage.setScaleX(0.9f);
        logoImage.setScaleY(0.9f);
        
        appNameText.setAlpha(0f);
        appNameText.setTranslationY(40f);
        
        taglineText.setAlpha(0f);
        taglineText.setTranslationY(25f);
        
        glowEffect.setAlpha(0f);
        glowEffect.setScaleX(0.3f);
        glowEffect.setScaleY(0.3f);
        
        loadingIndicator.setAlpha(0f);
    }

    private void startAnimations() {
        // Glow effect animation - subtle background glow
        ObjectAnimator glowFadeIn = ObjectAnimator.ofFloat(glowEffect, "alpha", 0f, 0.5f);
        ObjectAnimator glowScaleX = ObjectAnimator.ofFloat(glowEffect, "scaleX", 0.3f, 1.1f);
        ObjectAnimator glowScaleY = ObjectAnimator.ofFloat(glowEffect, "scaleY", 0.3f, 1.1f);
        
        AnimatorSet glowSet = new AnimatorSet();
        glowSet.playTogether(glowFadeIn, glowScaleX, glowScaleY);
        glowSet.setDuration(600);
        glowSet.setInterpolator(new AccelerateDecelerateInterpolator());

        // Logo fade in - quick fade to reveal the animating vector
        ObjectAnimator logoFadeIn = ObjectAnimator.ofFloat(logoImage, "alpha", 0f, 1f);
        ObjectAnimator logoScaleX = ObjectAnimator.ofFloat(logoImage, "scaleX", 0.9f, 1f);
        ObjectAnimator logoScaleY = ObjectAnimator.ofFloat(logoImage, "scaleY", 0.9f, 1f);
        
        AnimatorSet logoSet = new AnimatorSet();
        logoSet.playTogether(logoFadeIn, logoScaleX, logoScaleY);
        logoSet.setDuration(400);
        logoSet.setInterpolator(new AccelerateDecelerateInterpolator());
        logoSet.setStartDelay(150);
        
        // Start the animated vector drawable when logo appears
        logoSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                startLogoVectorAnimation();
            }
        });

        // App name animation - appears after logo starts drawing
        ObjectAnimator nameFadeIn = ObjectAnimator.ofFloat(appNameText, "alpha", 0f, 1f);
        ObjectAnimator nameSlideUp = ObjectAnimator.ofFloat(appNameText, "translationY", 40f, 0f);
        
        AnimatorSet nameSet = new AnimatorSet();
        nameSet.playTogether(nameFadeIn, nameSlideUp);
        nameSet.setDuration(450);
        nameSet.setInterpolator(new AccelerateDecelerateInterpolator());
        nameSet.setStartDelay(1200); // After logo outline completes

        // Tagline animation
        ObjectAnimator taglineFadeIn = ObjectAnimator.ofFloat(taglineText, "alpha", 0f, 1f);
        ObjectAnimator taglineSlideUp = ObjectAnimator.ofFloat(taglineText, "translationY", 25f, 0f);
        
        AnimatorSet taglineSet = new AnimatorSet();
        taglineSet.playTogether(taglineFadeIn, taglineSlideUp);
        taglineSet.setDuration(400);
        taglineSet.setInterpolator(new AccelerateDecelerateInterpolator());
        taglineSet.setStartDelay(1500);

        // Loading indicator - subtle appearance
        ObjectAnimator loadingFadeIn = ObjectAnimator.ofFloat(loadingIndicator, "alpha", 0f, 1f);
        loadingFadeIn.setDuration(250);
        loadingFadeIn.setStartDelay(1700);

        // Gentle glow pulsing
        ObjectAnimator glowPulse = ObjectAnimator.ofFloat(glowEffect, "alpha", 0.4f, 0.6f);
        glowPulse.setDuration(1200);
        glowPulse.setRepeatMode(ValueAnimator.REVERSE);
        glowPulse.setRepeatCount(ValueAnimator.INFINITE);
        glowPulse.setStartDelay(600);

        // Start all animations
        glowSet.start();
        logoSet.start();
        nameSet.start();
        taglineSet.start();
        loadingFadeIn.start();
        glowPulse.start();

        // Navigate after splash duration
        new Handler(Looper.getMainLooper()).postDelayed(this::navigateNext, SPLASH_DURATION);
    }
    
    private void startLogoVectorAnimation() {
        Drawable drawable = logoImage.getDrawable();
        if (drawable instanceof AnimatedVectorDrawable) {
            ((AnimatedVectorDrawable) drawable).start();
        } else if (drawable instanceof AnimatedVectorDrawableCompat) {
            ((AnimatedVectorDrawableCompat) drawable).start();
        }
    }

    private void navigateNext() {
        // Fade out animation before transitioning
        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(
            getWindow().getDecorView(), "alpha", 1f, 0f
        );
        fadeOut.setDuration(250);
        fadeOut.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                // Check if we should show onboarding
                Intent intent;
                if (OnboardingActivity.shouldShowOnboarding(SplashActivity.this)) {
                    intent = new Intent(SplashActivity.this, OnboardingActivity.class);
                } else {
                    intent = new Intent(SplashActivity.this, MainActivity.class);
                }
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
