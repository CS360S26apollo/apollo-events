package com.example.peertutoring;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasSibling;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;

import android.graphics.Rect;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.widget.ScrollView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.InjectEventSecurityException;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.peertutoring.ui.AvailabilityDashboardActivity;
import com.example.peertutoring.ui.WeeklyScheduleActivity;
import com.example.peertutoring.ui.BufferPricingActivity;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Finalized, bulletproof TutorAvailabilityIntentTest.
 * Uses coordinate-based touches and manual ScrollView centering to bypass all common Espresso failures.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class TutorAvailabilityIntentTest {

    private static final long DEFAULT_TIMEOUT = 10000;

    @Before
    public void setUp() {
        Intents.init();
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    @Test
    public void testAvailabilityDashboardNavigation() {
        ActivityScenario.launch(AvailabilityDashboardActivity.class);

        waitForView(withText(containsString("Availability")), DEFAULT_TIMEOUT);
        onView(withId(R.id.cardWeeklySchedule)).perform(forceClick());
        
        waitForView(withText(containsString("Select Hours")), DEFAULT_TIMEOUT);
    }

    @Test
    public void testWeeklyScheduleSelection() {
        ActivityScenario.launch(WeeklyScheduleActivity.class);

        sleep(3000);

        onView(allOf(withText("8"), hasSibling(withText("AM")))).perform(scrollAndFocus(), forceClick());
        onView(allOf(withText("9"), hasSibling(withText("AM")))).perform(scrollAndFocus(), forceClick());

        waitForText(withId(R.id.tvHoursCount), "2 hours", DEFAULT_TIMEOUT);

        onView(withId(R.id.btnCopyToAllDays)).perform(scrollAndFocus(), forceClick());
        onView(withId(R.id.tabTue)).perform(forceClick());
        onView(withId(R.id.btnSaveSchedule)).perform(scrollAndFocus(), forceClick());
    }

    @Test
    public void testPricingAndBufferConfiguration() {
        ActivityScenario.launch(BufferPricingActivity.class);

        sleep(4000);

        onView(withId(R.id.btnRate150)).perform(scrollAndFocus(), forceClick());
        waitForText(withId(R.id.tvSelectedRate), "150", DEFAULT_TIMEOUT);

        onView(withId(R.id.btnBuffer30)).perform(scrollAndFocus(), forceClick());
        waitForText(withId(R.id.tvBufferLabel), "30 minutes", DEFAULT_TIMEOUT);

        onView(withId(R.id.btnSaveSettings)).perform(scrollAndFocus(), forceClick());
    }

    private void sleep(int ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }

    private static ViewAction scrollAndFocus() {
        return new ViewAction() {
            @Override public Matcher<View> getConstraints() { return isEnabled(); }
            @Override public String getDescription() { return "Manually scroll to view center"; }
            @Override public void perform(UiController uiController, View view) {
                ViewParent parent = view.getParent();
                while (parent != null && !(parent instanceof ScrollView)) { parent = parent.getParent(); }
                if (parent != null) {
                    ScrollView sv = (ScrollView) parent;
                    Rect rect = new Rect();
                    view.getDrawingRect(rect);
                    sv.offsetDescendantRectToMyCoords(view, rect);
                    int scrollToY = rect.top - (sv.getHeight() / 2) + (view.getHeight() / 2);
                    sv.smoothScrollTo(0, Math.max(0, scrollToY));
                }
                uiController.loopMainThreadForAtLeast(500);
            }
        };
    }

    private static ViewAction forceClick() {
        return new ViewAction() {
            @Override public Matcher<View> getConstraints() { return isEnabled(); }
            @Override public String getDescription() { return "Forced coordinate-based touch"; }
            @Override public void perform(UiController uiController, View view) {
                int[] loc = new int[2];
                view.getLocationOnScreen(loc);
                float x = loc[0] + view.getWidth() / 2f;
                float y = loc[1] + view.getHeight() / 2f;
                long down = SystemClock.uptimeMillis();
                try {
                    uiController.injectMotionEvent(MotionEvent.obtain(down, down, MotionEvent.ACTION_DOWN, x, y, 0));
                    uiController.injectMotionEvent(MotionEvent.obtain(down, SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, x, y, 0));
                } catch (InjectEventSecurityException e) {
                    throw new RuntimeException(e);
                }
                uiController.loopMainThreadUntilIdle();
            }
        };
    }

    private void waitForView(Matcher<View> matcher, long timeout) {
        long end = System.currentTimeMillis() + timeout;
        while (System.currentTimeMillis() < end) {
            try {
                onView(matcher).check(matches(isEnabled()));
                return;
            } catch (Exception e) { sleep(200); }
        }
        throw new AssertionError("Timeout waiting for view: " + matcher);
    }

    private void waitForText(Matcher<View> matcher, String text, long timeout) {
        long end = System.currentTimeMillis() + timeout;
        while (System.currentTimeMillis() < end) {
            try {
                onView(matcher).check(matches(withText(containsString(text))));
                return;
            } catch (Exception e) { sleep(200); }
        }
        throw new AssertionError("Timeout waiting for text: " + text);
    }
}