package com.pandcaspian.indicator;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.ext.junit.rules.ActivityScenarioRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

import com.pandcaspian.indicator.ui.SplashActivity;

/**
 * Instrumented tests for the SplashActivity.
 */
@RunWith(AndroidJUnit4.class)
public class SplashActivityTest {

    @Rule
    public ActivityScenarioRule<SplashActivity> activityRule = 
            new ActivityScenarioRule<>(SplashActivity.class);

    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("com.pandcaspian.indicator", appContext.getPackageName());
    }

    @Test
    public void splashActivityLaunches() {
        activityRule.getScenario().onActivity(activity -> {
            assertNotNull(activity);
            assertFalse(activity.isFinishing());
        });
    }

    @Test
    public void splashHasRequiredViews() {
        activityRule.getScenario().onActivity(activity -> {
            assertNotNull(activity.findViewById(R.id.logoImage));
            assertNotNull(activity.findViewById(R.id.appNameText));
            assertNotNull(activity.findViewById(R.id.taglineText));
            assertNotNull(activity.findViewById(R.id.loadingIndicator));
            assertNotNull(activity.findViewById(R.id.glowEffect));
        });
    }
}
