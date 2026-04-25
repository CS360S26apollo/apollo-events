package com.example.peertutoring;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.peertutoring.ui.CounterOfferActivity;
import com.example.peertutoring.ui.NewSessionRequestActivity;
import com.example.peertutoring.ui.RequestDetailActivity;
import com.example.peertutoring.ui.SessionRequestsActivity;
import com.example.peertutoring.ui.TutorRequestsActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Intent Tests for US 08 (Request a Session), US 09 (Tutor Response), US 10 (Track Status).
 *
 * TC-08-01  NewSessionRequestActivity shows tutor name in title
 * TC-08-02  Submitting with empty topic shows validation error
 * TC-08-03  Submitting with empty subject shows validation error
 * TC-08-04  Duration button selection updates cost preview text
 * TC-08-05  90-minute button shows higher cost than 30-minute button
 * TC-09-01  TutorRequestsActivity loads without crash and shows list or empty state
 * TC-09-02  CounterOfferActivity launches with correct student name from Intent
 * TC-09-03  CounterOfferActivity date/time cards are tappable
 * TC-09-04  RequestDetailActivity shows correct status badge for 'requested'
 * TC-09-05  RequestDetailActivity cancel button triggers status update
 * TC-10-01  SessionRequestsActivity filter chips display
 * TC-10-02  Pending chip filter applies without crash
 * TC-10-03  All-filter chip restores full list
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class SessionFlowIntentTest {

    @Before
    public void setUp() {
        Intents.init();
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    // ── US 08: Request a Session ──────────────────────────────

    /**
     * TC-08-01: When NewSessionRequestActivity is launched with a tutor name,
     * the screen title updates to "Book with <name>".
     */
    @Test
    public void testNewSessionRequestShowsTutorName() {
        Context ctx = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(ctx, NewSessionRequestActivity.class);
        intent.putExtra("tutorUid",  "tutor_test_uid");
        intent.putExtra("tutorName", "Sarah Johnson");
        intent.putExtra("tutorRate", 50);
        ActivityScenario.launch(intent);

        sleep(1000);

        onView(withId(R.id.tvScreenTitle))
                .check(matches(withText(containsString("Sarah Johnson"))));
    }

    /**
     * TC-08-02: Submitting the form with an empty topic field shows an error
     * and does NOT navigate away.
     */
    @Test
    public void testEmptyTopicBlocksSubmission() {
        Context ctx = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(ctx, NewSessionRequestActivity.class);
        intent.putExtra("tutorUid",  "tutor_test_uid");
        intent.putExtra("tutorName", "Test Tutor");
        intent.putExtra("tutorRate", 50);
        ActivityScenario.launch(intent);

        sleep(1500); // wait for subject dropdown to load

        // Select a subject so that dropdown isn't the blocker
        onView(withId(R.id.spinnerSubject)).perform(click());
        sleep(500);
        // Type directly into the autocomplete to set a value
        onView(withId(R.id.spinnerSubject)).perform(replaceText("Mathematics"), closeSoftKeyboard());
        sleep(300);

        // Leave topic empty, submit
        onView(withId(R.id.btnSubmitRequest)).perform(scrollTo(), click());

        // Activity should still be displayed (not finished)
        onView(withId(R.id.btnSubmitRequest)).check(matches(isDisplayed()));
    }

    /**
     * TC-08-03: Submitting with empty subject shows validation and stays on screen.
     */
    @Test
    public void testEmptySubjectBlocksSubmission() {
        Context ctx = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(ctx, NewSessionRequestActivity.class);
        intent.putExtra("tutorUid",  "tutor_test_uid");
        intent.putExtra("tutorName", "Test Tutor");
        intent.putExtra("tutorRate", 50);
        ActivityScenario.launch(intent);

        sleep(1500);

        // Fill topic but leave subject empty
        onView(withId(R.id.etTopic)).perform(scrollTo(), replaceText("Integration"), closeSoftKeyboard());
        onView(withId(R.id.btnSubmitRequest)).perform(scrollTo(), click());

        // Should still be here
        onView(withId(R.id.btnSubmitRequest)).check(matches(isDisplayed()));
    }

    /**
     * TC-08-04: Selecting the 30-min button shows a cost preview containing "tokens".
     */
    @Test
    public void testDurationButtonShowsCostPreview() {
        Context ctx = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(ctx, NewSessionRequestActivity.class);
        intent.putExtra("tutorUid",  "tutor_test_uid");
        intent.putExtra("tutorName", "Test Tutor");
        intent.putExtra("tutorRate", 80);
        ActivityScenario.launch(intent);

        sleep(1000);

        onView(withId(R.id.btn30min)).perform(scrollTo(), click());
        sleep(300);

        onView(withId(R.id.tvTokenCost))
                .check(matches(withText(containsString("tokens"))));
    }

    /**
     * TC-08-05: 90-min cost text is different from 30-min cost text (higher cost).
     * Verifies that the cost formula changes with duration.
     */
    @Test
    public void testNinetyMinCostDifferentFromThirtyMin() {
        Context ctx = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(ctx, NewSessionRequestActivity.class);
        intent.putExtra("tutorUid",  "tutor_test_uid");
        intent.putExtra("tutorName", "Test Tutor");
        intent.putExtra("tutorRate", 100);
        ActivityScenario.launch(intent);

        sleep(1000);

        // Get 30 min cost
        onView(withId(R.id.btn30min)).perform(scrollTo(), click());
        sleep(300);
        // 30 min @ 100/hr → ceil(30/60) * 100 = 100 tokens
        onView(withId(R.id.tvTokenCost))
                .check(matches(withText(containsString("100"))));

        // Switch to 90 min
        onView(withId(R.id.btn90min)).perform(scrollTo(), click());
        sleep(300);
        // 90 min @ 100/hr → ceil(90/60) * 100 = 200 tokens
        onView(withId(R.id.tvTokenCost))
                .check(matches(withText(containsString("200"))));
    }

    // ── US 09: Tutor Response ─────────────────────────────────

    /**
     * TC-09-01: TutorRequestsActivity loads without crash.
     * Shows either real requests or the empty state message.
     */
    @Test
    public void testTutorRequestsActivityLoads() {
        ActivityScenario.launch(TutorRequestsActivity.class);

        sleep(4000); // allow Firestore or mock to load

        // The request list container should always be present
        onView(withId(R.id.layoutRequestList))
                .check(matches(isDisplayed()));
    }

    /**
     * TC-09-02: CounterOfferActivity launched with student extras shows student name.
     */
    @Test
    public void testCounterOfferShowsStudentName() {
        Context ctx = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(ctx, CounterOfferActivity.class);
        intent.putExtra("requestId",   "mock_req_001");
        intent.putExtra("studentName", "Alex Kim");
        intent.putExtra("subject",     "Mathematics");
        intent.putExtra("duration",    60);
        intent.putExtra("tokens",      150);
        ActivityScenario.launch(intent);

        sleep(1000);

        onView(withId(R.id.tvStudentName))
                .check(matches(withText("Alex Kim")));
    }

    /**
     * TC-09-03: CounterOfferActivity date and time cards are tappable
     * (open system date/time pickers).
     */
    @Test
    public void testCounterOfferDateCardIsTappable() {
        Context ctx = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(ctx, CounterOfferActivity.class);
        intent.putExtra("requestId",   "mock_req_001");
        intent.putExtra("studentName", "Alex Kim");
        intent.putExtra("subject",     "Physics");
        intent.putExtra("duration",    60);
        intent.putExtra("tokens",      150);
        ActivityScenario.launch(intent);

        sleep(1000);

        onView(withId(R.id.etDate)).perform(scrollTo(), click());
        sleep(500);

        // Dismiss the dialog without selecting
        onView(withText("Cancel")).perform(click());

        // Back to counter offer screen
        onView(withId(R.id.etDate)).check(matches(isDisplayed()));
    }

    /**
     * TC-09-04: RequestDetailActivity launched with status "requested"
     * shows PENDING badge.
     */
    @Test
    public void testRequestDetailShowsPendingBadge() {
        Context ctx = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(ctx, RequestDetailActivity.class);
        intent.putExtra("requestId",  "mock_req_001");
        intent.putExtra("topic",      "Integration Techniques");
        intent.putExtra("status",     "requested");
        intent.putExtra("duration",   60);
        intent.putExtra("tokens",     100);
        ActivityScenario.launch(intent);

        sleep(1000);

        onView(withId(R.id.tvStatusBadge))
                .check(matches(withText("PENDING")));
    }

    /**
     * TC-09-05: For a mock request, cancel button shows "Request Cancelled" toast
     * (the mock path, which we also exercised in our cancel+refund fix).
     */
    @Test
    public void testRequestDetailCancelMockRequest() {
        Context ctx = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(ctx, RequestDetailActivity.class);
        intent.putExtra("requestId",  "mock_req_cancel");
        intent.putExtra("topic",      "Calculus");
        intent.putExtra("status",     "requested");
        intent.putExtra("duration",   60);
        intent.putExtra("tokens",     100);
        ActivityScenario.launch(intent);

        sleep(1000);

        onView(withId(R.id.btnCancelRequest)).perform(scrollTo(), click());
        sleep(800);

        // Status badge should update to CANCELLED
        onView(withId(R.id.tvStatusBadge))
                .check(matches(withText("CANCELLED")));
    }

    // ── US 10: Track Request Status ───────────────────────────

    /**
     * TC-10-01: SessionRequestsActivity loads and all filter chips are visible.
     */
    @Test
    public void testSessionDashboardAllChipsVisible() {
        ActivityScenario.launch(SessionRequestsActivity.class);

        sleep(3000);

        onView(withId(R.id.chipAll)).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withId(R.id.chipPending)).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withId(R.id.chipCounter)).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withId(R.id.chipAccepted)).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withId(R.id.chipDeclined)).perform(scrollTo()).check(matches(isDisplayed()));
    }

    /**
     * TC-10-02: Clicking the Pending chip applies filter without crashing.
     */
    @Test
    public void testPendingChipFilterApplies() {
        ActivityScenario.launch(SessionRequestsActivity.class);

        sleep(3000);

        onView(withId(R.id.chipPending)).perform(scrollTo(), click());
        sleep(500);

        // Container is still visible (filter applied, not crashed)
        onView(withId(R.id.layoutRequestList)).check(matches(isDisplayed()));
    }

    /**
     * TC-10-03: After applying a filter, clicking "All" chip restores the full list view.
     */
    @Test
    public void testAllChipRestoresListAfterFilter() {
        ActivityScenario.launch(SessionRequestsActivity.class);

        sleep(3000);

        // Apply a filter
        onView(withId(R.id.chipDeclined)).perform(scrollTo(), click());
        sleep(300);

        // Reset to All
        onView(withId(R.id.chipAll)).perform(scrollTo(), click());
        sleep(300);

        onView(withId(R.id.layoutRequestList)).check(matches(isDisplayed()));
    }

    // ── Helpers ───────────────────────────────────────────────

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}
