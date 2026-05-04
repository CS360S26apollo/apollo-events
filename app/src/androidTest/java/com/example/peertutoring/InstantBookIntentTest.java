package com.example.peertutoring;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;

import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.peertutoring.ui.InstantBookActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Intent Tests for User Story 12: Instant Book.
 *
 * TC-12-01  InstantBookActivity header shows tutor name passed via Intent
 * TC-12-02  Selecting 90-min duration shows updated token cost in preview
 * TC-12-03  Submitting without selecting a time slot is blocked
 * TC-12-04  Submitting without a topic is blocked
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class InstantBookIntentTest {

    private ActivityScenario<?> scenario;

    @Before
    public void setUp() {
        Intents.init();
    }

    @After
    public void tearDown() {
        if (scenario != null) {
            try { scenario.close(); } catch (Exception ignored) {}
            scenario = null;
        }
        Intents.release();
    }

    /**
     * TC-12-01: When InstantBookActivity is launched with tutorName = "Aisha Malik",
     * the header subtitle should contain "Aisha Malik".
     */
    @Test
    public void testInstantBookHeaderShowsTutorName() {
        Context ctx = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(ctx, InstantBookActivity.class);
        intent.putExtra("tutorUid",  "seed_tutor_aisha");
        intent.putExtra("tutorName", "Aisha Malik");
        intent.putExtra("tutorRate", 40);
        scenario = ActivityScenario.launch(intent);

        sleep(1500);

        onView(withId(R.id.tvTutorName))
                .check(matches(withText(containsString("Aisha Malik"))));
    }

    /**
     * TC-12-02: Selecting the 90-min button recalculates the token preview.
     * Formula: ceil(90 * 50 / 60) = ceil(75.0) = 75 tokens.
     */
    @Test
    public void testInstantBookDurationButtonUpdatesCostPreview() {
        Context ctx = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(ctx, InstantBookActivity.class);
        intent.putExtra("tutorUid",  "seed_tutor_priya");
        intent.putExtra("tutorName", "Priya Sharma");
        intent.putExtra("tutorRate", 50);
        scenario = ActivityScenario.launch(intent);

        sleep(1000);

        // Scroll btn30min into view first (same row), then click btn90min directly
        onView(withId(R.id.btn30min)).perform(scrollTo());
        onView(withId(R.id.btn90min)).perform(click());
        sleep(300);

        // 90 min @ 50/hr = ceil(90 * 50 / 60) = ceil(75) = 75 tokens
        onView(withId(R.id.tvCostPreview))
                .check(matches(withText(containsString("75"))));
    }

    /**
     * TC-12-03: Tapping "Book Now" without selecting a time slot should be blocked.
     * The activity must remain visible (no crash, no navigation away).
     */
    @Test
    public void testInstantBookBlockedWithoutSlotSelection() {
        Context ctx = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(ctx, InstantBookActivity.class);
        intent.putExtra("tutorUid",  "seed_tutor_carlos");
        intent.putExtra("tutorName", "Carlos Rivera");
        intent.putExtra("tutorRate", 35);
        scenario = ActivityScenario.launch(intent);

        sleep(2000); // wait for Firestore slot data

        // Fill subject and topic, but deliberately skip slot selection
        onView(withId(R.id.spinnerSubject))
                .perform(replaceText("Chemistry"), closeSoftKeyboard());
        onView(withId(R.id.etTopic))
                .perform(scrollTo(), replaceText("Organic Reactions"), closeSoftKeyboard());

        onView(withId(R.id.btnBookNow)).perform(click());
        sleep(500);

        // Must still be on the Instant Book screen
        onView(withId(R.id.btnBookNow)).check(matches(isDisplayed()));
    }

    /**
     * TC-12-04: Tapping "Book Now" with an empty topic should be blocked.
     * The activity must remain visible.
     */
    @Test
    public void testInstantBookBlockedWithoutTopic() {
        Context ctx = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(ctx, InstantBookActivity.class);
        intent.putExtra("tutorUid",  "seed_tutor_omar");
        intent.putExtra("tutorName", "Omar Siddiqui");
        intent.putExtra("tutorRate", 30);
        scenario = ActivityScenario.launch(intent);

        sleep(2000);

        // Select a subject but leave topic empty
        onView(withId(R.id.spinnerSubject))
                .perform(replaceText("Economics"), closeSoftKeyboard());
        // Do NOT fill etTopic

        onView(withId(R.id.btnBookNow)).perform(click());
        sleep(500);

        // Must still be on screen
        onView(withId(R.id.btnBookNow)).check(matches(isDisplayed()));
    }

    // ── Helper ────────────────────────────────────────────────

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}
