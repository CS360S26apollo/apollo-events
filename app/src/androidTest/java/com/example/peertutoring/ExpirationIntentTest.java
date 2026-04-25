package com.example.peertutoring;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.peertutoring.ui.SessionRequestsActivity;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumented UI Tests for US-11: Auto-Expire Session Requests.
 *
 * TC-11-I01  Session requests screen loads and shows the request list
 * TC-11-I02  Expired chip is visible in the filter bar (scrollable)
 * TC-11-I03  Tapping Expired chip does not crash the app
 * TC-11-I04  Status badge color for expired requests renders correctly
 *            (verified via the mock data path — Expired text visible if any exist)
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class ExpirationIntentTest {

    // ── TC-11-I01 ──────────────────────────────────────────────────

    /**
     * TC-11-I01: SessionRequestsActivity loads its main list container.
     * This verifies the screen opens without crash and the list area is displayed.
     */
    @Test
    public void testSessionRequestsScreenLoads() {
        ActivityScenario.launch(SessionRequestsActivity.class);
        sleep(1000);

        onView(withId(R.id.layoutRequestList)).check(matches(isDisplayed()));
    }

    // ── TC-11-I02 ──────────────────────────────────────────────────

    /**
     * TC-11-I02: The "Expired" filter chip is present in the horizontal chip bar.
     * The bar is inside a HorizontalScrollView so scrollTo() is required.
     */
    @Test
    public void testExpiredChipIsVisibleInFilterBar() {
        ActivityScenario.launch(SessionRequestsActivity.class);
        sleep(500);

        onView(withId(R.id.chipExpired))
                .perform(scrollTo())
                .check(matches(isDisplayed()));
    }

    // ── TC-11-I03 ──────────────────────────────────────────────────

    /**
     * TC-11-I03: Tapping the Expired chip filters the list without crashing.
     * After tapping, the list container should still be displayed (empty or not).
     */
    @Test
    public void testTappingExpiredChipDoesNotCrash() {
        ActivityScenario.launch(SessionRequestsActivity.class);
        sleep(500);

        // Match the inner TextView by text, not the MaterialCardView by ID.
        // Clicking the card container by ID gets swallowed by MaterialCardView's
        // ripple touch handler; matching by text targets the child TextView and
        // the event bubbles up to the card's OnClickListener — the same pattern
        // used by every other chip/button click in this test suite.
        onView(withText("Expired")).perform(scrollTo(), click());
        sleep(500);

        // App must still be running — list container visible (even if empty)
        onView(withId(R.id.layoutRequestList)).check(matches(isDisplayed()));
    }

    // ── TC-11-I04 ──────────────────────────────────────────────────

    /**
     * TC-11-I04: The filter bar contains both "Cancelled" and "Expired" text labels,
     * confirming the new chip was added alongside the existing ones.
     */
    @Test
    public void testFilterBarHasBothCancelledAndExpiredChips() {
        ActivityScenario.launch(SessionRequestsActivity.class);
        sleep(500);

        onView(withText(containsString("Cancelled"))).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withText(containsString("Expired"))).perform(scrollTo()).check(matches(isDisplayed()));
    }

    // ── Helper ─────────────────────────────────────────────────────

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}
