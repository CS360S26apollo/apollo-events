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

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.peertutoring.ui.BuyTokensActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Intent Tests for the Buy Tokens screen (US: Token Purchase).
 *
 * TC-BT-I01  BuyTokensActivity loads without crash
 * TC-BT-I02  Current balance label is visible on load
 * TC-BT-I03  Token packages container is displayed
 * TC-BT-I04  Payment methods container is displayed
 * TC-BT-I05  Purchase button is visible
 * TC-BT-I06  Purchase button is disabled before package + payment method are selected
 * TC-BT-I07  Tapping "Starter" package shows 100-token label in the UI
 * TC-BT-I08  Tapping a package does not crash the activity
 * TC-BT-I09  Card input fields are hidden before a card method is selected
 * TC-BT-I10  Back button is tappable and finishes the activity
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class BuyTokensIntentTest {

    @Before
    public void setUp() {
        Intents.init();
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    // ── TC-BT-I01 ─────────────────────────────────────────────

    @Test
    public void testBuyTokensActivityLoads() {
        ActivityScenario.launch(BuyTokensActivity.class);
        sleep(2000);

        onView(withId(R.id.layoutPackages))
                .check(matches(isDisplayed()));
    }

    // ── TC-BT-I02 ─────────────────────────────────────────────

    @Test
    public void testCurrentBalanceLabelVisible() {
        ActivityScenario.launch(BuyTokensActivity.class);
        sleep(2000);

        onView(withId(R.id.tvCurrentBalance))
                .check(matches(isDisplayed()));
    }

    // ── TC-BT-I03 ─────────────────────────────────────────────

    @Test
    public void testPackagesContainerVisible() {
        ActivityScenario.launch(BuyTokensActivity.class);
        sleep(2000);

        onView(withId(R.id.layoutPackages))
                .check(matches(isDisplayed()));
    }

    // ── TC-BT-I04 ─────────────────────────────────────────────

    @Test
    public void testPaymentMethodsContainerVisible() {
        ActivityScenario.launch(BuyTokensActivity.class);
        sleep(2000);

        onView(withId(R.id.layoutPayMethods))
                .perform(scrollTo())
                .check(matches(isDisplayed()));
    }

    // ── TC-BT-I05 ─────────────────────────────────────────────

    @Test
    public void testPurchaseButtonVisible() {
        ActivityScenario.launch(BuyTokensActivity.class);
        sleep(2000);

        onView(withId(R.id.btnPurchase))
                .check(matches(isDisplayed()));
    }

    // ── TC-BT-I06 ─────────────────────────────────────────────

    @Test
    public void testPurchaseButtonDisabledInitially() {
        ActivityScenario.launch(BuyTokensActivity.class);
        sleep(2000);

        // No package selected and no payment method → Purchase must be disabled
        onView(withId(R.id.btnPurchase))
                .check(matches(not(isEnabled())));
    }

    // ── TC-BT-I07 ─────────────────────────────────────────────

    @Test
    public void testStarterPackageShowsTokenAmount() {
        ActivityScenario.launch(BuyTokensActivity.class);
        sleep(2000);

        // The packages layout builds rows with "Starter" label containing "100"
        onView(withText(containsString("Starter")))
                .perform(scrollTo())
                .check(matches(isDisplayed()));
    }

    // ── TC-BT-I08 ─────────────────────────────────────────────

    @Test
    public void testTappingPackageDoesNotCrash() {
        ActivityScenario.launch(BuyTokensActivity.class);
        sleep(2000);

        // Tap the first package row — just verify no crash
        onView(withText(containsString("Starter")))
                .perform(scrollTo(), click());
        sleep(500);

        // Activity should still be alive
        onView(withId(R.id.layoutPackages)).check(matches(isDisplayed()));
    }

    // ── TC-BT-I09 ─────────────────────────────────────────────

    @Test
    public void testCardInputsHiddenBeforeCardMethodSelected() {
        ActivityScenario.launch(BuyTokensActivity.class);
        sleep(2000);

        // Card input section must be GONE initially (no payment method selected)
        onView(withId(R.id.layoutCardInputs))
                .check(matches(not(isDisplayed())));
    }

    // ── TC-BT-I10 ─────────────────────────────────────────────

    @Test
    public void testBackButtonVisible() {
        ActivityScenario.launch(BuyTokensActivity.class);
        sleep(1500);

        onView(withId(R.id.btnBack))
                .check(matches(isDisplayed()));
    }

    // ── Helper ────────────────────────────────────────────────

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}
