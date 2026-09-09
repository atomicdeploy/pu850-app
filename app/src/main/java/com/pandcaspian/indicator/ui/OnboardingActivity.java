package com.pandcaspian.indicator.ui;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.pandcaspian.indicator.MainActivity;
import com.pandcaspian.indicator.R;

/**
 * Beautiful onboarding experience shown on first launch
 */
public class OnboardingActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "onboarding_prefs";
    private static final String KEY_ONBOARDING_COMPLETE = "onboarding_complete";
    private static final String KEY_LAST_VERSION = "last_version";

    private ViewPager2 viewPager;
    private LinearLayout indicatorContainer;
    private MaterialButton btnSkip;
    private MaterialButton btnNext;
    private OnboardingAdapter adapter;

    // Onboarding page data
    private final OnboardingPage[] pages = {
            new OnboardingPage(
                    R.drawable.ic_icon_wifi,
                    R.string.onboarding_title_1,
                    R.string.onboarding_desc_1,
                    new int[]{R.string.onboarding_highlight_1_1, R.string.onboarding_highlight_1_2, R.string.onboarding_highlight_1_3}
            ),
            new OnboardingPage(
                    R.drawable.ic_key_settings,
                    R.string.onboarding_title_2,
                    R.string.onboarding_desc_2,
                    new int[]{R.string.onboarding_highlight_2_1, R.string.onboarding_highlight_2_2, R.string.onboarding_highlight_2_3}
            ),
            new OnboardingPage(
                    R.drawable.ic_key_report,
                    R.string.onboarding_title_3,
                    R.string.onboarding_desc_3,
                    new int[]{R.string.onboarding_highlight_3_1, R.string.onboarding_highlight_3_2, R.string.onboarding_highlight_3_3}
            ),
            new OnboardingPage(
                    R.drawable.ic_key_tare,
                    R.string.onboarding_title_4,
                    R.string.onboarding_desc_4,
                    new int[]{R.string.onboarding_highlight_4_1, R.string.onboarding_highlight_4_2, R.string.onboarding_highlight_4_3}
            )
    };

    public static boolean shouldShowOnboarding(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean completed = prefs.getBoolean(KEY_ONBOARDING_COMPLETE, false);
        
        // Also check if this is a new version
        long lastVersion = prefs.getLong(KEY_LAST_VERSION, 0);
        long currentVersion = getCurrentVersionCode(context);
        
        // Show onboarding if never completed OR if major version changed
        return !completed || (currentVersion / 100 > lastVersion / 100);
    }

    private static long getCurrentVersionCode(android.content.Context context) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                return context.getPackageManager()
                        .getPackageInfo(context.getPackageName(), 0).getLongVersionCode();
            } else {
                return context.getPackageManager()
                        .getPackageInfo(context.getPackageName(), 0).versionCode;
            }
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        initViews();
        setupViewPager();
        setupIndicators();
        setupButtons();
    }

    private void initViews() {
        viewPager = findViewById(R.id.viewPager);
        indicatorContainer = findViewById(R.id.indicatorContainer);
        btnSkip = findViewById(R.id.btnSkip);
        btnNext = findViewById(R.id.btnNext);
    }

    private void setupViewPager() {
        adapter = new OnboardingAdapter();
        viewPager.setAdapter(adapter);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateIndicators(position);
                updateButtons(position);
                animatePageContent(position);
            }
        });

        // Add page transformer for smooth transitions
        viewPager.setPageTransformer((page, position) -> {
            float absPos = Math.abs(position);
            page.setAlpha(1 - absPos * 0.3f);
            page.setScaleY(1 - absPos * 0.1f);
            page.setTranslationX(-position * page.getWidth() * 0.1f);
        });
    }

    private void setupIndicators() {
        indicatorContainer.removeAllViews();
        
        for (int i = 0; i < pages.length; i++) {
            View indicator = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    i == 0 ? dpToPx(24) : dpToPx(8),
                    dpToPx(8)
            );
            params.setMargins(dpToPx(4), 0, dpToPx(4), 0);
            indicator.setLayoutParams(params);
            indicator.setBackgroundResource(R.drawable.indicator_onboarding);
            indicator.setSelected(i == 0);
            indicatorContainer.addView(indicator);
        }
    }

    private void updateIndicators(int position) {
        for (int i = 0; i < indicatorContainer.getChildCount(); i++) {
            View indicator = indicatorContainer.getChildAt(i);
            boolean isSelected = i == position;
            indicator.setSelected(isSelected);
            
            // Animate width change using ValueAnimator
            int currentWidth = indicator.getWidth();
            int targetWidth = isSelected ? dpToPx(24) : dpToPx(8);
            
            if (currentWidth != targetWidth && currentWidth > 0) {
                android.animation.ValueAnimator widthAnimator = android.animation.ValueAnimator.ofInt(currentWidth, targetWidth);
                widthAnimator.setDuration(200);
                final View indicatorView = indicator;
                widthAnimator.addUpdateListener(animation -> {
                    ViewGroup.LayoutParams params = indicatorView.getLayoutParams();
                    params.width = (int) animation.getAnimatedValue();
                    indicatorView.setLayoutParams(params);
                });
                widthAnimator.start();
            } else {
                ViewGroup.LayoutParams params = indicator.getLayoutParams();
                params.width = targetWidth;
                indicator.setLayoutParams(params);
            }
        }
    }

    private void updateButtons(int position) {
        boolean isLastPage = position == pages.length - 1;
        
        btnNext.setText(isLastPage ? R.string.onboarding_get_started : R.string.onboarding_next);
        btnSkip.setVisibility(isLastPage ? View.INVISIBLE : View.VISIBLE);
        
        // Animate button change on last page
        if (isLastPage) {
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(btnNext, "scaleX", 1f, 1.1f, 1f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(btnNext, "scaleY", 1f, 1.1f, 1f);
            AnimatorSet set = new AnimatorSet();
            set.playTogether(scaleX, scaleY);
            set.setDuration(300);
            set.setInterpolator(new OvershootInterpolator());
            set.start();
        }
    }

    private void animatePageContent(int position) {
        // Find the page view and animate its contents
        View pageView = viewPager.findViewWithTag("page_" + position);
        if (pageView != null) {
            View iconCard = pageView.findViewById(R.id.iconCard);
            View title = pageView.findViewById(R.id.featureTitle);
            View desc = pageView.findViewById(R.id.featureDescription);
            View highlights = pageView.findViewById(R.id.highlightsContainer);

            if (iconCard != null) {
                iconCard.setAlpha(0f);
                iconCard.setScaleX(0.5f);
                iconCard.setScaleY(0.5f);
                iconCard.animate()
                        .alpha(1f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(400)
                        .setInterpolator(new OvershootInterpolator())
                        .start();
            }

            if (title != null) {
                title.setAlpha(0f);
                title.setTranslationY(30f);
                title.animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setStartDelay(150)
                        .setDuration(350)
                        .setInterpolator(new AccelerateDecelerateInterpolator())
                        .start();
            }

            if (desc != null) {
                desc.setAlpha(0f);
                desc.setTranslationY(20f);
                desc.animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setStartDelay(250)
                        .setDuration(350)
                        .setInterpolator(new AccelerateDecelerateInterpolator())
                        .start();
            }

            if (highlights != null) {
                highlights.setAlpha(0f);
                highlights.setTranslationY(20f);
                highlights.animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setStartDelay(350)
                        .setDuration(350)
                        .setInterpolator(new AccelerateDecelerateInterpolator())
                        .start();
            }
        }
    }

    private void setupButtons() {
        btnSkip.setOnClickListener(v -> completeOnboarding());
        btnNext.setOnClickListener(v -> {
            if (viewPager.getCurrentItem() < pages.length - 1) {
                viewPager.setCurrentItem(viewPager.getCurrentItem() + 1);
            } else {
                completeOnboarding();
            }
        });
    }

    private void completeOnboarding() {
        // Save completion state
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit()
                .putBoolean(KEY_ONBOARDING_COMPLETE, true)
                .putLong(KEY_LAST_VERSION, getCurrentVersionCode(this))
                .apply();

        // Navigate to main activity
        startActivity(new Intent(this, MainActivity.class));
        finish();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, R.anim.fade_in, R.anim.fade_out);
        } else {
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    // Adapter for ViewPager2
    private class OnboardingAdapter extends RecyclerView.Adapter<OnboardingAdapter.PageViewHolder> {

        @NonNull
        @Override
        public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_onboarding_page, parent, false);
            return new PageViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {
            OnboardingPage page = pages[position];
            holder.bind(page, position);
        }

        @Override
        public int getItemCount() {
            return pages.length;
        }

        class PageViewHolder extends RecyclerView.ViewHolder {
            private final ImageView featureIcon;
            private final TextView featureTitle;
            private final TextView featureDescription;
            private final TextView highlightText1;
            private final TextView highlightText2;
            private final TextView highlightText3;

            PageViewHolder(@NonNull View itemView) {
                super(itemView);
                featureIcon = itemView.findViewById(R.id.featureIcon);
                featureTitle = itemView.findViewById(R.id.featureTitle);
                featureDescription = itemView.findViewById(R.id.featureDescription);
                highlightText1 = itemView.findViewById(R.id.highlightText1);
                highlightText2 = itemView.findViewById(R.id.highlightText2);
                highlightText3 = itemView.findViewById(R.id.highlightText3);
            }

            void bind(OnboardingPage page, int position) {
                itemView.setTag("page_" + position);
                featureIcon.setImageResource(page.iconRes);
                featureTitle.setText(page.titleRes);
                featureDescription.setText(page.descRes);
                highlightText1.setText(page.highlightRes[0]);
                highlightText2.setText(page.highlightRes[1]);
                highlightText3.setText(page.highlightRes[2]);
            }
        }
    }

    // Data class for onboarding pages
    private static class OnboardingPage {
        final int iconRes;
        final int titleRes;
        final int descRes;
        final int[] highlightRes;

        OnboardingPage(int iconRes, int titleRes, int descRes, int[] highlightRes) {
            this.iconRes = iconRes;
            this.titleRes = titleRes;
            this.descRes = descRes;
            this.highlightRes = highlightRes;
        }
    }
}
