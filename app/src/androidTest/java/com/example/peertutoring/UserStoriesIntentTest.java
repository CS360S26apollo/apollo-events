package com.example.peertutoring;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.peertutoring.ui.EditProfileActivity;
import com.example.peertutoring.ui.MainActivity;
import com.example.peertutoring.ui.ProfileActivity;
import com.google.firebase.auth.FirebaseAuth;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Intent Tests for User Stories 1 through 4.
 * This class uses Espresso to simulate user interactions and verify that
 * the application navigates correctly and displays the expected UI components.
 * 
 * Deliverable Requirement #4: Intent tests for completed requirements.
 * Completed User Stories:
 * - US1: Student Account Creation
 * - US2: Tutor Account Creation
 * - US3: Profile Editing & Privacy
 * - US4: Identity Verification UI
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class UserStoriesIntentTest {

    @Before
    public void setUp() {
        Intents.init();
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    /**
     * Helper method to ensure a user is logged in before testing authenticated activities.
     */
    private void ensureLoggedIn() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            ActivityScenario.launch(MainActivity.class);
            String email = "test" + System.currentTimeMillis() + "@example.com";
            onView(withId(R.id.editTextEmail)).perform(typeText(email), closeSoftKeyboard());
            onView(withId(R.id.editTextPassword)).perform(typeText("Password123"), closeSoftKeyboard());
            onView(withId(R.id.editTextConfirmPassword)).perform(typeText("Password123"), closeSoftKeyboard());
            onView(withId(R.id.buttonContinue)).perform(click());
            
            // Wait for registration and auto-login to complete
            try { Thread.sleep(5000); } catch (InterruptedException e) {}
        }
    }

    /**
     * Tests US1 & US2: Verifies that registration triggers navigation to the Profile onboarding.
     */
    @Test
    public void testRegistrationIntentToProfileActivity() {
        ActivityScenario.launch(MainActivity.class);
        
        String testEmail = "testuser" + System.currentTimeMillis() + "@example.com";
        String testPassword = "Password123";

        onView(withId(R.id.editTextEmail)).perform(typeText(testEmail), closeSoftKeyboard());
        onView(withId(R.id.editTextPassword)).perform(typeText(testPassword), closeSoftKeyboard());
        onView(withId(R.id.editTextConfirmPassword)).perform(typeText(testPassword), closeSoftKeyboard());

        onView(withId(R.id.radioStudent)).perform(click());
        onView(withId(R.id.buttonContinue)).perform(click());

        // Wait for Firebase Response
        try { Thread.sleep(5000); } catch (InterruptedException e) {}

        intended(allOf(
                hasComponent(ProfileActivity.class.getName()),
                hasExtra("email", testEmail),
                hasExtra("role", "student")
        ));
    }

    /**
     * Tests US3: Verifies that the privacy tab and its switches are accessible and functional.
     */
    @Test
    public void testEditProfilePrivacyToggle() {
        ensureLoggedIn();
        
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, EditProfileActivity.class);
        ActivityScenario.launch(intent);

        // Wait for profile to load
        try { Thread.sleep(2000); } catch (InterruptedException e) {}

        onView(withId(R.id.tabPrivacy)).perform(click());
        onView(withId(R.id.panelPrivacy)).check(matches(isDisplayed()));

        onView(withId(R.id.switchShowInstitution)).perform(click());
        
        onView(withId(R.id.tabEditProfile)).perform(click());
        onView(withId(R.id.panelEditProfile)).check(matches(isDisplayed()));
    }

    /**
     * Tests US4: Verifies that the Verification tab displays the identity verification UI.
     */
    @Test
    public void testVerificationTabNavigation() {
        ensureLoggedIn();

        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, EditProfileActivity.class);
        ActivityScenario.launch(intent);

        // Wait for activity setup
        try { Thread.sleep(2000); } catch (InterruptedException e) {}

        onView(withId(R.id.tabVerification)).perform(click());
        
        // Brief wait for UI thread to update visibility
        try { Thread.sleep(1000); } catch (InterruptedException e) {}

        onView(withId(R.id.panelVerification)).check(matches(isDisplayed()));
        onView(withText("Verify Your Identity")).check(matches(isDisplayed()));
    }

    /**
     * Tests realistic data input during the profile completion flow.
     */
    @Test
    public void testEditProfileFieldInput() {
        ensureLoggedIn();

        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, EditProfileActivity.class);
        ActivityScenario.launch(intent);

        // Wait for data
        try { Thread.sleep(2000); } catch (InterruptedException e) {}

        onView(withId(R.id.tabEditProfile)).perform(click());

        onView(withId(R.id.editTextFirstName)).perform(replaceText("John"), closeSoftKeyboard());
        onView(withId(R.id.editTextLastName)).perform(replaceText("Doe"), closeSoftKeyboard());
        
        onView(withId(R.id.btnRoleTutor)).perform(scrollTo(), click());
        onView(withId(R.id.layoutBio)).check(matches(isDisplayed()));
        onView(withId(R.id.editTextBio)).perform(replaceText("I am a specialized Math and Physics tutor with 4 years of experience."), closeSoftKeyboard());
        
        onView(withId(R.id.btnSaveProfile)).perform(scrollTo(), click());
    }
}