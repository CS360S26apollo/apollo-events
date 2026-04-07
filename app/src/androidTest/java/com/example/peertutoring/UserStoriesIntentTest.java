package com.example.peertutoring;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.registerIdlingResources;
import static androidx.test.espresso.Espresso.unregisterIdlingResources;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.allOf;

import android.view.View;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.IdlingResource;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.peertutoring.ui.MainActivity;
import com.example.peertutoring.ui.ProfileActivity;
import com.google.firebase.auth.FirebaseAuth;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class UserStoriesIntentTest {

    private static final int TIMEOUT = 10000; // 10 sec max wait

    @Before
    public void setUp() {
        Intents.init();
        FirebaseAuth.getInstance().signOut(); // fresh start
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    @Test
    public void testRegistrationFlow() {

        ActivityScenario.launch(MainActivity.class);

        String testEmail = "test@example.com";

        // Fill inputs
        onView(withId(R.id.editTextEmail))
                .perform(scrollTo(), replaceText(testEmail), closeSoftKeyboard());

        onView(withId(R.id.editTextPassword))
                .perform(scrollTo(), replaceText("Password123"), closeSoftKeyboard());

        onView(withId(R.id.editTextConfirmPassword))
                .perform(scrollTo(), replaceText("Password123"), closeSoftKeyboard());

        // Select role (already student by default but safe)
        onView(withId(R.id.radioStudent))
                .perform(scrollTo(), click());

        // Click continue
        onView(withId(R.id.buttonContinue))
                .perform(scrollTo(), click());

        // ✅ WAIT FOR INTENT (polling instead of sleep)
        waitForIntent();

        // ✅ VERIFY INTENT
        intended(allOf(
                hasComponent(ProfileActivity.class.getName()),
                hasExtra("email", testEmail),
                hasExtra("role", "student")
        ));
    }

    /**
     * Wait until intent is fired (max TIMEOUT)
     */
    private void waitForIntent() {
        long start = System.currentTimeMillis();
        boolean found = false;

        while (System.currentTimeMillis() - start < TIMEOUT) {
            try {
                intended(hasComponent(ProfileActivity.class.getName()));
                found = true;
                break;
            } catch (AssertionError e) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ignored) {}
            }
        }

        if (!found) {
            throw new AssertionError("Intent to ProfileActivity not fired within timeout");
        }
    }
}