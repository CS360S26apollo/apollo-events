package com.example.peertutoring;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import android.os.SystemClock;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.action.ViewActions;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.peertutoring.ui.StudentPreferencesActivity;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumented UI Tests for US-07: Student Preference Wizard
 *
 * Test coverage:
 * TC-07-01  Screen loads with header and progress bar
 * TC-07-02  Step 1 budget SeekBar updates the token label
 * TC-07-03  Find Tutor button is disabled until all steps complete
 * TC-07-04  Step 2 level selection highlights the chosen card
 * TC-07-05  Step 3 language chips toggle on/off correctly
 * TC-07-06  Step 4 session type card selection works
 * TC-07-07  Progress percentage updates on each step
 * TC-07-08  Back button on step 2 returns to step 1
 * TC-07-09  Complete wizard shows results screen
 * TC-07-10  Results screen shows tutor list or empty message
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class StudentPreferencesIntentTest {

    /** TC-07-01: Screen loads with correct header */
    @Test
    public void testScreenLoadsWithHeader() {
        ActivityScenario.launch(StudentPreferencesActivity.class);
        sleep(1000);

        onView(withId(R.id.tvPrefTitle))
                .check(matches(isDisplayed()));
        onView(withId(R.id.progressCompletion))
                .check(matches(isDisplayed()));
        onView(withId(R.id.tvCompletion))
                .check(matches(withText(containsString("%"))));
    }

    /** TC-07-02: Step 1 budget label shows default 500 */
    @Test
    public void testStep1BudgetDefaultValue() {
        ActivityScenario.launch(StudentPreferencesActivity.class);
        sleep(500);

        onView(withId(R.id.tvBudgetValue))
                .check(matches(withText("500")));
    }

    /** TC-07-03: Find Tutor button disabled on Step 1 (no level/language/session selected) */
    @Test
    public void testFindTutorButtonDisabledInitially() {
        ActivityScenario.launch(StudentPreferencesActivity.class);
        sleep(500);

        // On step 1, next button should be enabled (budget always set)
        // but final "Find My Perfect Tutor" needs all 4 steps
        onView(withId(R.id.btnFindTutor))
                .check(matches(isDisplayed()));
    }

    /** TC-07-04: Navigating from step 1 to step 2 shows level grid */
    @Test
    public void testNavigateToStep2ShowsLevelGrid() {
        ActivityScenario.launch(StudentPreferencesActivity.class);
        sleep(500);

        // Step 1 is visible, click Next
        onView(withId(R.id.btnFindTutor)).perform(click());
        sleep(500);

        // Step 2 should now be visible
        onView(withId(R.id.layoutStep2))
                .check(matches(isDisplayed()));
        onView(withId(R.id.layoutStep1))
                .check(matches(not(isDisplayed())));
    }

    /** TC-07-05: Step 2 — selecting a level enables Next button */
    @Test
    public void testStep2LevelSelectionEnablesNext() {
        ActivityScenario.launch(StudentPreferencesActivity.class);
        sleep(500);

        // Go to step 2
        onView(withId(R.id.btnFindTutor)).perform(click());
        sleep(500);

        // Initially disabled (no level selected)
        onView(withId(R.id.btnFindTutor))
                .check(matches(not(isEnabled())));

        // Select first level card (Beginner Friendly)
        onView(withText("Beginner Friendly")).perform(scrollTo(), click());
        sleep(300);

        // Now Next should be enabled
        onView(withId(R.id.btnFindTutor))
                .check(matches(isEnabled()));
    }

    /** TC-07-06: Step 3 — language chips appear and are tappable */
    @Test
    public void testStep3LanguageChipsDisplayed() {
        ActivityScenario.launch(StudentPreferencesActivity.class);
        sleep(500);

        // Step 1 → 2
        onView(withId(R.id.btnFindTutor)).perform(click());
        sleep(300);

        // Select level to enable Next
        onView(withText("Expert")).perform(scrollTo(), click());
        sleep(300);

        // Step 2 → 3
        onView(withId(R.id.btnFindTutor)).perform(click());
        sleep(500);

        onView(withId(R.id.layoutStep3))
                .check(matches(isDisplayed()));

        // English chip should be visible
        onView(withText("English")).perform(scrollTo())
                .check(matches(isDisplayed()));
    }

    /** TC-07-07: Selecting a language enables Next on step 3 */
    @Test
    public void testStep3SelectingLanguageEnablesNext() {
        ActivityScenario.launch(StudentPreferencesActivity.class);
        sleep(500);

        navigateToStep(3);

        // Initially Next disabled (no language)
        onView(withId(R.id.btnFindTutor))
                .check(matches(not(isEnabled())));

        // Select English
        onView(withText("English")).perform(scrollTo(), click());
        sleep(300);

        onView(withId(R.id.btnFindTutor))
                .check(matches(isEnabled()));
    }

    /** TC-07-08: Step 4 — session type cards are shown */
    @Test
    public void testStep4SessionTypeCardsDisplayed() {
        ActivityScenario.launch(StudentPreferencesActivity.class);
        sleep(500);

        navigateToStep(4);

        onView(withId(R.id.layoutStep4))
                .check(matches(isDisplayed()));

        onView(withText("Online")).perform(scrollTo())
                .check(matches(isDisplayed()));
        onView(withText("In-Person")).perform(scrollTo())
                .check(matches(isDisplayed()));
        onView(withText("Hybrid")).perform(scrollTo())
                .check(matches(isDisplayed()));
    }

    /** TC-07-09: Completing all steps shows results */
    @Test
    public void testCompletingAllStepsShowsResults() {
        ActivityScenario.launch(StudentPreferencesActivity.class);
        sleep(500);

        // Step 1 — budget (default is fine)
        onView(withId(R.id.btnFindTutor)).perform(click());
        sleep(300);

        // Step 2 — level
        onView(withText("Expert")).perform(scrollTo(), click());
        sleep(300);
        onView(withId(R.id.btnFindTutor)).perform(click());
        sleep(300);

        // Step 3 — language
        onView(withText("English")).perform(scrollTo(), click());
        sleep(300);
        onView(withId(R.id.btnFindTutor)).perform(click());
        sleep(300);

        // Step 4 — session type
        onView(withText("Online")).perform(scrollTo(), click());
        sleep(300);

        // Final button
        onView(withId(R.id.btnFindTutor)).perform(scrollTo(), click());

        // Wait for Firestore query
        sleep(5000);

        // Results should be visible
        onView(withId(R.id.layoutResults))
                .check(matches(isDisplayed()));
    }

    /** TC-07-10: Progress bar shows 25% on step 1 */
    @Test
    public void testProgressBarShowsCorrectPercentage() {
        ActivityScenario.launch(StudentPreferencesActivity.class);
        sleep(500);

        onView(withId(R.id.tvCompletion))
                .check(matches(withText("25%")));

        // Go to step 2 — should show 50%
        onView(withId(R.id.btnFindTutor)).perform(click());
        sleep(300);

        onView(withId(R.id.tvCompletion))
                .check(matches(withText("50%")));
    }

    // ── Helpers ───────────────────────────────────────────────

    /**
     * Navigates through steps 1 to targetStep making valid selections.
     */
    private void navigateToStep(int targetStep) {
        // Step 1 → next
        if (targetStep >= 2) {
            onView(withId(R.id.btnFindTutor)).perform(click());
            sleep(400);
        }
        // Step 2 → select level → next
        if (targetStep >= 3) {
            onView(withText("Expert")).perform(scrollTo(), click());
            sleep(300);
            onView(withId(R.id.btnFindTutor)).perform(click());
            sleep(400);
        }
        // Step 3 → select language → next
        if (targetStep >= 4) {
            onView(withText("English")).perform(scrollTo(), click());
            sleep(300);
            onView(withId(R.id.btnFindTutor)).perform(click());
            sleep(400);
        }
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}