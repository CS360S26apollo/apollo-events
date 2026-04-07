package com.example.peertutoring;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.peertutoring.ui.SessionRequestsActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

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

    @Test
    public void testSessionDashboardLoading() {

        // Launch activity
        ActivityScenario.launch(SessionRequestsActivity.class);

        // ✅ Check main UI loaded (stronger check)
        onView(withId(R.id.chipPending))
                .check(matches(isDisplayed()));

        // ✅ Click pending chip
        onView(withId(R.id.chipPending))
                .perform(scrollTo(), click());

        // ✅ Verify still visible / active
        onView(withId(R.id.chipPending))
                .check(matches(isDisplayed()));
    }
}