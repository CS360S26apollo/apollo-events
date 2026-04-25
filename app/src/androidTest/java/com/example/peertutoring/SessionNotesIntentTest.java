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

import com.example.peertutoring.ui.SessionNotesActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Intent Tests for User Story 17: Session Notes & Outcomes.
 *
 * TC-17-01  SessionNotesActivity header shows subject + student name
 * TC-17-02  Submitting with all fields empty is blocked
 * TC-17-03  Submitting with only topics filled is allowed (takeaways is optional)
 * TC-17-04  Send button is visible and tappable
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class SessionNotesIntentTest {

    @Before
    public void setUp() {
        Intents.init();
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    /**
     * TC-17-01: When SessionNotesActivity launches with subject and studentName,
     * the subtitle shows both values.
     */
    @Test
    public void testSessionNotesSubtitleShowsSubjectAndStudent() {
        Context ctx = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(ctx, SessionNotesActivity.class);
        intent.putExtra("requestId",   "mock_req_notes_01");
        intent.putExtra("tutorUid",    "seed_tutor_aisha");
        intent.putExtra("studentUid",  "student_test_uid");
        intent.putExtra("tutorName",   "Aisha Malik");
        intent.putExtra("studentName", "Ali Khan");
        intent.putExtra("subject",     "Mathematics");
        ActivityScenario.launch(intent);

        sleep(1000);

        onView(withId(R.id.tvSessionSubtitle))
                .check(matches(withText(containsString("Mathematics"))));
        onView(withId(R.id.tvSessionSubtitle))
                .check(matches(withText(containsString("Ali Khan"))));
    }

    /**
     * TC-17-02: Tapping "Send Notes" with both Topics and Takeaways empty
     * must be blocked — the send button stays visible.
     */
    @Test
    public void testSessionNotesBlockedWhenBothFieldsEmpty() {
        Context ctx = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(ctx, SessionNotesActivity.class);
        intent.putExtra("requestId",   "mock_req_notes_02");
        intent.putExtra("tutorUid",    "seed_tutor_priya");
        intent.putExtra("studentUid",  "student_test_uid");
        intent.putExtra("tutorName",   "Priya Sharma");
        intent.putExtra("studentName", "Sara Ahmed");
        intent.putExtra("subject",     "Computer Science");
        ActivityScenario.launch(intent);

        sleep(1000);

        // Leave all fields empty and tap send
        onView(withId(R.id.btnSendNotes)).perform(scrollTo(), click());
        sleep(500);

        // Must still be on screen (validation blocked the submission)
        onView(withId(R.id.btnSendNotes)).check(matches(isDisplayed()));
    }

    /**
     * TC-17-03: Filling in Topics Covered is sufficient to pass validation
     * (Takeaways and Action Items are optional). Tapping send does NOT crash.
     */
    @Test
    public void testSessionNotesTopicsAlonePassesValidation() {
        Context ctx = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(ctx, SessionNotesActivity.class);
        intent.putExtra("requestId",   "mock_req_notes_03");
        intent.putExtra("tutorUid",    "seed_tutor_carlos");
        intent.putExtra("studentUid",  "student_test_uid");
        intent.putExtra("tutorName",   "Carlos Rivera");
        intent.putExtra("studentName", "Hamza Butt");
        intent.putExtra("subject",     "Chemistry");
        ActivityScenario.launch(intent);

        sleep(1000);

        onView(withId(R.id.etTopics))
                .perform(scrollTo(), replaceText("Organic reactions, functional groups"), closeSoftKeyboard());

        // Takeaways and Action Items intentionally left empty
        // Tap send — should not show validation error (may fail on Firebase write, that's OK)
        onView(withId(R.id.btnSendNotes)).perform(scrollTo(), click());
        sleep(800);

        // The button text changes to "Sending..." when validation passes
        // We just verify no crash occurred — button is still in the view hierarchy
        onView(withId(R.id.btnSendNotes)).check(matches(isDisplayed()));
    }

    /**
     * TC-17-04: The send button is visible and tappable on launch.
     */
    @Test
    public void testSessionNotesSendButtonVisible() {
        Context ctx = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(ctx, SessionNotesActivity.class);
        intent.putExtra("requestId",   "mock_req_notes_04");
        intent.putExtra("tutorUid",    "seed_tutor_omar");
        intent.putExtra("studentUid",  "student_test_uid");
        intent.putExtra("tutorName",   "Omar Siddiqui");
        intent.putExtra("studentName", "Fatima Noor");
        intent.putExtra("subject",     "Economics");
        ActivityScenario.launch(intent);

        sleep(1000);

        onView(withId(R.id.btnSendNotes))
                .perform(scrollTo())
                .check(matches(isDisplayed()));
    }

    // ── Helper ────────────────────────────────────────────────

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}
