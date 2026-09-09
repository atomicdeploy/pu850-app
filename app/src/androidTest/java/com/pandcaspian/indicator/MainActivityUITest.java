package com.pandcaspian.indicator;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.ext.junit.rules.ActivityScenarioRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Instrumented tests for the MainActivity UI components.
 */
@RunWith(AndroidJUnit4.class)
public class MainActivityUITest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule = 
            new ActivityScenarioRule<>(MainActivity.class);

    @Test
    public void useAppContext() {
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("com.pandcaspian.indicator", appContext.getPackageName());
    }

    @Test
    public void mainActivityLaunches() {
        activityRule.getScenario().onActivity(activity -> {
            assertNotNull(activity);
            assertFalse(activity.isFinishing());
        });
    }

    @Test
    public void hasWeightDisplayCard() {
        activityRule.getScenario().onActivity(activity -> {
            View weightDisplayCard = activity.findViewById(R.id.weightDisplayCard);
            assertNotNull("Weight display card should exist", weightDisplayCard);
            assertEquals(View.VISIBLE, weightDisplayCard.getVisibility());
        });
    }

    @Test
    public void hasWeightTextView() {
        activityRule.getScenario().onActivity(activity -> {
            TextView textViewWeight = activity.findViewById(R.id.textViewWeight);
            assertNotNull("Weight TextView should exist", textViewWeight);
            assertEquals(View.VISIBLE, textViewWeight.getVisibility());
        });
    }

    @Test
    public void hasMessageTextView() {
        activityRule.getScenario().onActivity(activity -> {
            TextView textViewMessage = activity.findViewById(R.id.textViewMessage);
            assertNotNull("Message TextView should exist", textViewMessage);
            assertEquals(View.VISIBLE, textViewMessage.getVisibility());
        });
    }

    @Test
    public void hasAllActionCards() {
        activityRule.getScenario().onActivity(activity -> {
            // Check all 9 action cards exist
            assertNotNull("Tare card should exist", activity.findViewById(R.id.cardTare));
            assertNotNull("Show card should exist", activity.findViewById(R.id.cardShow));
            assertNotNull("Hide card should exist", activity.findViewById(R.id.cardHide));
            assertNotNull("Power card should exist", activity.findViewById(R.id.cardPower));
            assertNotNull("Develop card should exist", activity.findViewById(R.id.cardDevelop));
            assertNotNull("Connect card should exist", activity.findViewById(R.id.cardConnect));
            assertNotNull("Receipt card should exist", activity.findViewById(R.id.cardReceipt));
            assertNotNull("Report card should exist", activity.findViewById(R.id.cardReport));
            assertNotNull("Settings card should exist", activity.findViewById(R.id.cardSettings));
        });
    }

    @Test
    public void hasAllActionButtons() {
        activityRule.getScenario().onActivity(activity -> {
            // Check all 9 action buttons exist
            assertNotNull("Tare button should exist", activity.findViewById(R.id.buttonTare));
            assertNotNull("Show button should exist", activity.findViewById(R.id.buttonShow));
            assertNotNull("Hide button should exist", activity.findViewById(R.id.buttonHide));
            assertNotNull("Power button should exist", activity.findViewById(R.id.buttonPower));
            assertNotNull("Develop button should exist", activity.findViewById(R.id.buttonDevelop));
            assertNotNull("Connect button should exist", activity.findViewById(R.id.buttonConnect));
            assertNotNull("Receipt button should exist", activity.findViewById(R.id.buttonReceipt));
            assertNotNull("Report button should exist", activity.findViewById(R.id.buttonReport));
            assertNotNull("Settings button should exist", activity.findViewById(R.id.buttonSettings));
        });
    }

    @Test
    public void buttonsAreClickable() {
        activityRule.getScenario().onActivity(activity -> {
            Button buttonTare = activity.findViewById(R.id.buttonTare);
            Button buttonConnect = activity.findViewById(R.id.buttonConnect);
            Button buttonSettings = activity.findViewById(R.id.buttonSettings);
            
            assertTrue("Tare button should be clickable", buttonTare.isClickable());
            assertTrue("Connect button should be clickable", buttonConnect.isClickable());
            assertTrue("Settings button should be clickable", buttonSettings.isClickable());
        });
    }

    @Test
    public void hasTopStatusBar() {
        activityRule.getScenario().onActivity(activity -> {
            View topStatusBar = activity.findViewById(R.id.topStatusBar);
            assertNotNull("Top status bar should exist", topStatusBar);
            assertEquals(View.VISIBLE, topStatusBar.getVisibility());
        });
    }

    @Test
    public void hasServerAddressTextView() {
        activityRule.getScenario().onActivity(activity -> {
            TextView serverAddr = activity.findViewById(R.id.textViewServerAddr);
            assertNotNull("Server address TextView should exist", serverAddr);
        });
    }

    @Test
    public void hasUserInfo() {
        activityRule.getScenario().onActivity(activity -> {
            assertNotNull("User icon card should exist", activity.findViewById(R.id.imageViewUserIconCard));
            assertNotNull("User name TextView should exist", activity.findViewById(R.id.textViewUserName));
        });
    }

    @Test
    public void hasLogoImage() {
        activityRule.getScenario().onActivity(activity -> {
            View logo = activity.findViewById(R.id.imageViewLogo);
            assertNotNull("Logo ImageView should exist", logo);
            assertEquals(View.VISIBLE, logo.getVisibility());
        });
    }
}
