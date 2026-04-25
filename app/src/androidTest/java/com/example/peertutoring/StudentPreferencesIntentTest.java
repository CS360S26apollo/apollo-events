package com.example.peertutoring;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.peertutoring.ui.BrowseTutorsActivity;
import com.example.peertutoring.ui.StudentPreferencesActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumented UI Tests for US-07: Student Preference Wizard
 *
 * TC-07-01  Screen loads with header and progress bar
 * TC-07-02  Step 1 budget SeekBar shows default 500 tokens
 * TC-07-03  Next button is displayed and enabled on step 1 (budget always set)
 * TC-07-04  Clicking Next on step 1 navigates to step 2 (level grid)
 * TC-07-05  Selecting a level on step 2 enables the Next button
 * TC-07-06  Step 3 language chips are displayed and scrollable
 * TC-07-07  Selecting a language on step 3 enables the Next button
 * TC-07-08  Step 4 shows all three session type cards
 * TC-07-09  Completing all four steps and submitting shows the results screen
 * TC-07-10  Progress label updates correctly as steps advance
 * TC-07-11  Back button on step 2 returns to step 1
 * TC-07-12  Back button on step 3 returns to step 2
 * TC-07-13  Button label changes to "Find My Perfect Tutor" on step 4
 * TC-07-14  Step 4 button is disabled until a session type is selected
 * TC-07-15  Multiple languages can be selected simultaneously on step 3
 * TC-07-16  BrowseTutorsActivity header has a Preferences button
 * TC-07-17  Preferences button in Browse Tutors opens the wizard
 * TC-07-18  Results screen shows a result count after wizard completes
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class StudentPreferencesIntentTest {

    @Before
    public void setUp() {
        Intents.init();
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    // ── TC-07-01 to TC-07-10 (core wizard flow) ──────────────────

    /** TC-07-01: Screen loads with correct header and progress indicator */
    @Test
    public void testScreenLoadsWithHeader() {
        ActivityScenario.launch(StudentPreferencesActivity.class);
        sleep(1000);

        onView(withId(R.id.tvPrefTitle)).check(matches(isDisplayed()));
        onView(withId(R.id.progressCompletion)).check(matches(isDisplayed()));
        onView(withId(R.id.tvCompletion)).check(matches(withText(containsString("%"))));
    }

    /** TC-07-02: Step 1 opens showing the default budget of 500 tokens */
    @Test
    public void testStep1BudgetDefaultValue() {
        ActivityScenario.launch(StudentPreferencesActivity.class);
        sleep(500);

        onView(withId(R.id.tvBudgetValue)).check(matches(withText("500")));
    }

    /** TC-07-03: Next button is visible and enabled on step 1 (budget always has a value) */
    @Test
    public void testNextButtonEnabledOnStep1() {
        ActivityScenario.launch(StudentPreferencesActivity.class);
        sleep(500);

        onView(withId(R.id.btnFindTutor))
                .check(matches(isDisplayed()))
                .check(matches(isEnabled()));
    }

    /** TC-07-04: Clicking Next on step 1 shows the level grid on step 2 */
    @Test
    public void testNavigateToStep2ShowsLevelGrid() {
        ActivityScenario.launch(StudentPreferencesActivity.class);
        sleep(500);

        onView(withId(R.id.btnFindTutor)).perform(click());
        sleep(600);

        onView(withId(R.id.layoutStep2)).check(matches(isDisplayed()));
        onView(withId(R.id.layoutStep1)).check(matches(not(isDisplayed())));
    }

    /** TC-07-05: Selecting a level card on step 2 enables the Next button */
    @Test
    public void testStep2LevelSelectionEnablesNext() {
        ActivityScenario.launch(StudentPreferencesActivity.class);
        sleep(500);

        onView(withId(R.id.btnFindTutor)).perform(click());
        sleep(600);

        // Button should be disabled with no level selected
        onView(withId(R.id.btnFindTutor)).check(matches(not(isEnabled())));

        // Select "Beginner Friendly"
        onView(withText("Beginner Friendly")).perform(scrollTo(), click());
        sleep(300);

        onView(withId(R.id.btnFindTutor)).check(matches(isEnabled()));
    }

    /** TC-07-06: Step 3 language chips are displayed and scrollable */
    @Test
    public void testStep3LanguageChipsDisplayed() {
        ActivityScenario.launch(StudentPreferencesActivity.class);
        sleep(500);

        navigateToStep(3);

        onView(withId(R.id.layoutStep3)).check(matches(isDisplayed()));
        onView(withText("English")).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withText("Spanish")).perform(scrollTo()).check(matches(isDisplayed()));
    }

    /** TC-07-07: Selecting a language on step 3 enables the Next button */
    @Test
    public void testStep3SelectingLanguageEnablesNext() {
        ActivityScenario.launch(StudentPreferencesActivity.class);
        sleep(500);

        navigateToStep(3);

        // Initially disabled
        onView(withId(R.id.btnFindTutor)).check(matches(not(isEnabled())));

        onView(withText("English")).perform(scrollTo(), click());
        sleep(300);

        onView(withId(R.id.btnFindTutor)).check(matches(isEnabled()));
    }

    /** TC-07-08: Step 4 shows all three session type cards */
    @Test
    public void testStep4SessionTypeCardsDisplayed() {
        ActivityScenario.launch(StudentPreferencesActivity.class);
        sleep(500);

        navigateToStep(4);

        onView(withId(R.id.layoutStep4)).check(matches(isDisplayed()));
        onView(withText("Online")).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withText("In-Person")).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withText("Hybrid")).perform(scrollTo()).check(matches(isDisplayed()));
    }

    /** TC-07-09: Completing all four steps and submitting shows the results screen */
    @Test
    public void testCompletingAllStepsShowsResults() {
        ActivityScenario.launch(StudentPreferencesActivity.class);
        sleep(500);

        // Step 1 → 2
        onView(withId(R.id.btnFindTutor)).perform(click());
        sleep(500);

        // Step 2 → select level → 3
        onView(withText("Expert")).perform(scrollTo(), click());
        sleep(300);
        onView(withId(R.id.btnFindTutor)).perform(click());
        sleep(500);

        // Step 3 → select language → 4
        onView(withText("English")).perform(scrollTo(), click());
        sleep(300);
        onView(withId(R.id.btnFindTutor)).perform(click());
        sleep(500);

        // Step 4 → select session type → submit
        onView(withText("Online")).perform(scrollTo(), click());
        sleep(300);
        onView(withId(R.id.btnFindTutor)).perform(scrollTo(), click());

        // Wait for Firestore query (or guest fast-path)
        sleep(5000);

        onView(withId(R.id.layoutResults)).check(matches(isDisplayed()));
    }

    /** TC-07-10: Progress label shows correct percentage at each step */
    @Test
    public void testProgressUpdatesAcrossSteps() {
        ActivityScenario.launch(StudentPreferencesActivity.class);
        sleep(500);

        onView(withId(R.id.tvCompletion)).check(matches(withText("25%")));

        onView(withId(R.id.btnFindTutor)).perform(click());
        sleep(400);
        onView(withId(R.id.tvCompletion)).check(matches(withText("50%")));

        onView(withText("Expert")).perform(scrollTo(), click());
        sleep(300);
        onView(withId(R.id.btnFindTutor)).perform(click());
        sleep(400);
        onView(withId(R.id.tvCompletion)).check(matches(withText("75%")));

        onView(withText("English")).perform(scrollTo(), click());
        sleep(300);
        onView(withId(R.id.btnFindTutor)).perform(click());
        sleep(400);
        onView(withId(R.id.tvCompletion)).check(matches(withText("100%")));
    }

    // ── TC-07-11 to TC-07-15 (back nav, labels, multi-select) ────

    /** TC-07-11: Back button on step 2 returns to step 1 */
    @Test
    public void testBackButtonOnStep2ReturnsToStep1() {
        ActivityScenario.launch(StudentPreferencesActivity.class);
        sleep(500);

        // Advance to step 2
        onView(withId(R.id.btnFindTutor)).perform(click());
        sleep(600);
        onView(withId(R.id.layoutStep2)).check(matches(isDisplayed()));

        // Press back
        onView(withId(R.id.btnBack)).perform(click());
        sleep(400);

        onView(withId(R.id.layoutStep1)).check(matches(isDisplayed()));
        onView(withId(R.id.layoutStep2)).check(matches(not(isDisplayed())));
    }

    /** TC-07-12: Back button on step 3 returns to step 2, not step 1 */
    @Test
    public void testBackButtonOnStep3ReturnsToStep2() {
        ActivityScenario.launch(StudentPreferencesActivity.class);
        sleep(500);

        navigateToStep(3);

        onView(withId(R.id.btnBack)).perform(click());
        sleep(400);

        onView(withId(R.id.layoutStep2)).check(matches(isDisplayed()));
        onView(withId(R.id.layoutStep3)).check(matches(not(isDisplayed())));
    }

    /** TC-07-13: Button label changes to "Find My Perfect Tutor" when on step 4 */
    @Test
    public void testButtonLabelChangesOnStep4() {
        ActivityScenario.launch(StudentPreferencesActivity.class);
        sleep(500);

        // Steps 1–3 show "Next →"
        onView(withId(R.id.btnFindTutor)).check(matches(withText(containsString("Next"))));

        navigateToStep(4);

        // Step 4 shows the final label
        onView(withId(R.id.btnFindTutor))
                .check(matches(withText(containsString("Find My Perfect Tutor"))));
    }

    /** TC-07-14: Step 4 Next button is disabled until a session type card is selected */
    @Test
    public void testStep4ButtonDisabledUntilSessionTypeSelected() {
        ActivityScenario.launch(StudentPreferencesActivity.class);
        sleep(500);

        navigateToStep(4);

        // Nothing selected yet — button should be disabled
        onView(withId(R.id.btnFindTutor)).check(matches(not(isEnabled())));

        // Select "Hybrid"
        onView(withText("Hybrid")).perform(scrollTo(), click());
        sleep(300);

        onView(withId(R.id.btnFindTutor)).check(matches(isEnabled()));
    }

    /** TC-07-15: Multiple languages can be selected simultaneously on step 3 */
    @Test
    public void testMultipleLanguagesCanBeSelectedOnStep3() {
        ActivityScenario.launch(StudentPreferencesActivity.class);
        sleep(500);

        navigateToStep(3);

        // Select English
        onView(withText("English")).perform(scrollTo(), click());
        sleep(300);
        onView(withId(R.id.btnFindTutor)).check(matches(isEnabled()));

        // Also select Spanish — button should remain enabled
        onView(withText("Spanish")).perform(scrollTo(), click());
        sleep(300);
        onView(withId(R.id.btnFindTutor)).check(matches(isEnabled()));

        // Also select French
        onView(withText("French")).perform(scrollTo(), click());
        sleep(300);
        onView(withId(R.id.btnFindTutor)).check(matches(isEnabled()));
    }

    // ── TC-07-16 to TC-07-18 (browse integration) ────────────────

    /** TC-07-16: BrowseTutorsActivity header contains the Preferences button */
    @Test
    public void testBrowseTutorsHasPreferencesButton() {
        ActivityScenario.launch(BrowseTutorsActivity.class);
        sleep(1000);

        onView(withId(R.id.btnFilter)).check(matches(isDisplayed()));
        onView(withText(containsString("Preferences"))).check(matches(isDisplayed()));
    }

    /**
     * TC-07-17: Tapping Preferences in BrowseTutorsActivity opens the wizard.
     * Verified by checking that the wizard header becomes visible.
     */
    @Test
    public void testPreferencesButtonOpensPrefWizard() {
        ActivityScenario.launch(BrowseTutorsActivity.class);
        sleep(1000);

        onView(withId(R.id.btnFilter)).perform(click());
        sleep(1000);

        // StudentPreferencesActivity is now on top — its header must be visible
        onView(withId(R.id.tvPrefTitle)).check(matches(isDisplayed()));
        onView(withId(R.id.progressCompletion)).check(matches(isDisplayed()));

        // Also verify via intent interception
        intended(hasComponent(StudentPreferencesActivity.class.getName()));
    }

    /** TC-07-18: After completing the wizard, the results screen shows a count label */
    @Test
    public void testResultsScreenShowsCountAfterCompletion() {
        ActivityScenario.launch(StudentPreferencesActivity.class);
        sleep(500);

        // Complete all 4 steps
        onView(withId(R.id.btnFindTutor)).perform(click());
        sleep(500);
        onView(withText("Expert")).perform(scrollTo(), click());
        sleep(300);
        onView(withId(R.id.btnFindTutor)).perform(click());
        sleep(500);
        onView(withText("English")).perform(scrollTo(), click());
        sleep(300);
        onView(withId(R.id.btnFindTutor)).perform(click());
        sleep(500);
        onView(withText("In-Person")).perform(scrollTo(), click());
        sleep(300);
        onView(withId(R.id.btnFindTutor)).perform(scrollTo(), click());

        // Wait for results (Firestore or guest fast-path)
        sleep(5000);

        onView(withId(R.id.layoutResults)).check(matches(isDisplayed()));
        onView(withId(R.id.tvResultCount)).check(matches(isDisplayed()));
    }

    // ── Helpers ───────────────────────────────────────────────────

    /**
     * Navigates to the given step number using valid default selections.
     * Call this at the start of a test that needs to test a specific step.
     */
    private void navigateToStep(int targetStep) {
        if (targetStep >= 2) {
            onView(withId(R.id.btnFindTutor)).perform(click());
            sleep(500);
        }
        if (targetStep >= 3) {
            onView(withText("Expert")).perform(scrollTo(), click());
            sleep(300);
            onView(withId(R.id.btnFindTutor)).perform(click());
            sleep(500);
        }
        if (targetStep >= 4) {
            onView(withText("English")).perform(scrollTo(), click());
            sleep(300);
            onView(withId(R.id.btnFindTutor)).perform(click());
            sleep(500);
        }
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}
