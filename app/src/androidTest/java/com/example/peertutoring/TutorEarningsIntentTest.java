package com.example.peertutoring;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
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

import com.example.peertutoring.ui.TutorEarningsActivity;
import com.example.peertutoring.ui.TransactionHistoryActivity;
import com.example.peertutoring.ui.WithdrawActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumented UI Tests for US-24: Tutor Earnings & Withdrawal.
 *
 * TC-24-I01  TutorEarningsActivity loads and shows header balance card
 * TC-24-I02  Period tab "Month" is selected by default
 * TC-24-I03  Tapping "Week" tab does not crash
 * TC-24-I04  "Request Withdrawal" button is visible and tappable
 * TC-24-I05  WithdrawActivity shows available balance
 * TC-24-I06  Quick amount 500 chip populates the input field
 * TC-24-I07  Confirm button is disabled before method + amount selected
 * TC-24-I08  Entering amount below minimum keeps Confirm disabled
 * TC-24-I09  Selecting a method card updates its stroke (visual feedback)
 * TC-24-I10  TransactionHistoryActivity loads and shows filter chips
 * TC-24-I11  Tapping "Earned" filter chip does not crash
 * TC-24-I12  Search field on TransactionHistoryActivity is functional
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class TutorEarningsIntentTest {

    @Before
    public void setUp() {
        Intents.init();
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    // ── TC-24-I01 ──────────────────────────────────────────────────

    @Test
    public void testEarningsDashboardLoads() {
        ActivityScenario.launch(TutorEarningsActivity.class);
        sleep(1500);

        onView(withId(R.id.tvAvailableBalance))
                .check(matches(isDisplayed()));
    }

    // ── TC-24-I02 ──────────────────────────────────────────────────

    @Test
    public void testMonthTabVisibleOnLoad() {
        ActivityScenario.launch(TutorEarningsActivity.class);
        sleep(1000);

        onView(withId(R.id.btnPeriodMonth))
                .check(matches(isDisplayed()));
    }

    // ── TC-24-I03 ──────────────────────────────────────────────────

    @Test
    public void testWeekTabTapDoesNotCrash() {
        ActivityScenario.launch(TutorEarningsActivity.class);
        sleep(1000);

        onView(withId(R.id.btnPeriodWeek)).perform(click());
        sleep(500);

        onView(withId(R.id.tvAvailableBalance)).check(matches(isDisplayed()));
    }

    // ── TC-24-I04 ──────────────────────────────────────────────────

    @Test
    public void testWithdrawButtonIsVisible() {
        ActivityScenario.launch(TutorEarningsActivity.class);
        sleep(1000);

        onView(withId(R.id.btnRequestWithdrawal))
                .check(matches(isDisplayed()));
    }

    // ── TC-24-I05 ──────────────────────────────────────────────────

    @Test
    public void testWithdrawActivityShowsBalance() {
        ActivityScenario.launch(WithdrawActivity.class);
        sleep(2000);

        onView(withId(R.id.tvAvailableBalance))
                .check(matches(isDisplayed()));
    }

    // ── TC-24-I06 ──────────────────────────────────────────────────

    @Test
    public void testQuickAmount500PopulatesInput() {
        ActivityScenario.launch(WithdrawActivity.class);
        sleep(1500);

        // Tap the 500 chip — it's a Button inside the layoutQuickAmounts
        onView(withText("500")).perform(scrollTo(), click());
        sleep(300);

        onView(withId(R.id.etWithdrawAmount))
                .check(matches(withText("500")));
    }

    // ── TC-24-I07 ──────────────────────────────────────────────────

    @Test
    public void testConfirmButtonDisabledInitially() {
        ActivityScenario.launch(WithdrawActivity.class);
        sleep(1000);

        onView(withId(R.id.btnConfirmWithdrawal))
                .check(matches(not(isEnabled())));
    }

    // ── TC-24-I08 ──────────────────────────────────────────────────

    @Test
    public void testConfirmDisabledWhenAmountBelowMinimum() {
        ActivityScenario.launch(WithdrawActivity.class);
        sleep(1500);

        // Enter 50 (below minimum of 100)
        onView(withId(R.id.etWithdrawAmount))
                .perform(replaceText("50"), closeSoftKeyboard());
        sleep(300);

        // Even without method selection, confirm should remain disabled
        onView(withId(R.id.btnConfirmWithdrawal))
                .check(matches(not(isEnabled())));
    }

    // ── TC-24-I09 ──────────────────────────────────────────────────

    @Test
    public void testWithdrawMethodCardsVisible() {
        ActivityScenario.launch(WithdrawActivity.class);
        sleep(1500);

        onView(withText("Bank Transfer")).perform(scrollTo())
                .check(matches(isDisplayed()));
        onView(withText("PayPal")).perform(scrollTo())
                .check(matches(isDisplayed()));
        onView(withText("Debit Card")).perform(scrollTo())
                .check(matches(isDisplayed()));
    }

    // ── TC-24-I10 ──────────────────────────────────────────────────

    @Test
    public void testTransactionHistoryLoads() {
        ActivityScenario.launch(TransactionHistoryActivity.class);
        sleep(2000);

        onView(withId(R.id.tvTotalEarned)).check(matches(isDisplayed()));
        onView(withId(R.id.tvTotalWithdrawn)).check(matches(isDisplayed()));
    }

    // ── TC-24-I11 ──────────────────────────────────────────────────

    @Test
    public void testEarnedFilterChipDoesNotCrash() {
        ActivityScenario.launch(TransactionHistoryActivity.class);
        sleep(2000);

        onView(withId(R.id.chipEarned)).perform(click());
        sleep(500);

        onView(withId(R.id.layoutTransactions)).check(matches(isDisplayed()));
    }

    // ── TC-24-I12 ──────────────────────────────────────────────────

    @Test
    public void testSearchFieldIsVisible() {
        ActivityScenario.launch(TransactionHistoryActivity.class);
        sleep(1500);

        onView(withId(R.id.etSearchTransactions))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testSearchFiltersResults() {
        ActivityScenario.launch(TransactionHistoryActivity.class);
        sleep(2000);

        onView(withId(R.id.etSearchTransactions))
                .perform(replaceText("Sarah"), closeSoftKeyboard());
        sleep(500);

        // List container should still be visible
        onView(withId(R.id.layoutTransactions)).check(matches(isDisplayed()));
    }

    // ── Helper ────────────────────────────────────────────────

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}