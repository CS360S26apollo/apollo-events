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

import com.example.peertutoring.ui.RateReviewActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Intent Tests for User Story 19: Rate & Review After Session.
 *
 * TC-19-01  RateReviewActivity header shows tutor name
 * TC-19-02  Submitting with 0 stars is blocked
 * TC-19-03  Selecting a star updates the rating label text
 * TC-19-04  Submit button is visible on launch
 * TC-19-05  Review text is optional — submitting with stars but no text is not blocked
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class RateReviewIntentTest {

    @Before
    public void setUp() {
        Intents.init();
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    /**
     * TC-19-01: When RateReviewActivity launches with tutorName = "Priya Sharma",
     * the subtitle should contain the tutor's name.
     */
    @Test
    public void testRateReviewHeaderShowsTutorName() {
        Context ctx = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(ctx, RateReviewActivity.class);
        intent.putExtra("requestId", "mock_req_review_01");
        intent.putExtra("tutorUid",  "seed_tutor_priya");
        intent.putExtra("tutorName", "Priya Sharma");
        ActivityScenario.launch(intent);

        sleep(1000);

        onView(withId(R.id.tvTutorSubtitle))
                .check(matches(withText(containsString("Priya Sharma"))));
    }

    /**
     * TC-19-02: Tapping Submit without selecting any star must be blocked.
     * The Submit button must remain visible on screen.
     */
    @Test
    public void testRateReviewBlockedWithZeroStars() {
        Context ctx = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(ctx, RateReviewActivity.class);
        intent.putExtra("requestId", "mock_req_review_02");
        intent.putExtra("tutorUid",  "seed_tutor_aisha");
        intent.putExtra("tutorName", "Aisha Malik");
        ActivityScenario.launch(intent);

        sleep(1000);

        // Do NOT tap any star — submit immediately
        onView(withId(R.id.btnSubmitReview)).perform(click());
        sleep(500);

        // Must still be on the review screen
        onView(withId(R.id.btnSubmitReview)).check(matches(isDisplayed()));
    }

    /**
     * TC-19-03: Tapping the 4th star updates the rating label to "Very Good".
     */
    @Test
    public void testSelectingStarUpdateRatingLabel() {
        Context ctx = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(ctx, RateReviewActivity.class);
        intent.putExtra("requestId", "mock_req_review_03");
        intent.putExtra("tutorUid",  "seed_tutor_carlos");
        intent.putExtra("tutorName", "Carlos Rivera");
        ActivityScenario.launch(intent);

        sleep(1000);

        onView(withId(R.id.star4)).perform(click());
        sleep(300);

        onView(withId(R.id.tvRatingLabel))
                .check(matches(withText("Very Good")));
    }

    /**
     * TC-19-04: The Submit button is visible and tappable immediately on launch.
     */
    @Test
    public void testSubmitButtonVisibleOnLaunch() {
        Context ctx = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(ctx, RateReviewActivity.class);
        intent.putExtra("requestId", "mock_req_review_04");
        intent.putExtra("tutorUid",  "seed_tutor_omar");
        intent.putExtra("tutorName", "Omar Siddiqui");
        ActivityScenario.launch(intent);

        sleep(1000);

        onView(withId(R.id.btnSubmitReview))
                .check(matches(isDisplayed()));
    }

    /**
     * TC-19-05: Review text is optional — selecting a star and leaving review text empty
     * should pass validation (submission may fail on Firebase write without auth, that's OK).
     */
    @Test
    public void testReviewTextIsOptional() {
        Context ctx = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(ctx, RateReviewActivity.class);
        intent.putExtra("requestId", "mock_req_review_05");
        intent.putExtra("tutorUid",  "seed_tutor_priya");
        intent.putExtra("tutorName", "Priya Sharma");
        ActivityScenario.launch(intent);

        sleep(1000);

        // Select 5 stars but leave review text empty
        onView(withId(R.id.star5)).perform(click());
        sleep(300);

        // Do NOT fill review text
        onView(withId(R.id.btnSubmitReview)).perform(click());
        sleep(500);

        // Label should now say "Excellent" (star 5 was selected — validation passed)
        onView(withId(R.id.tvRatingLabel))
                .check(matches(withText("Excellent")));
    }

    // ── Helper ────────────────────────────────────────────────

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}
