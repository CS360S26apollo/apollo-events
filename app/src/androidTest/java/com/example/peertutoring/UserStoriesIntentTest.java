package com.example.peertutoring;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;

import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.peertutoring.ui.BrowseTutorsActivity;
import com.example.peertutoring.ui.EditProfileActivity;
import com.example.peertutoring.ui.MainActivity;
import com.example.peertutoring.ui.ProfileActivity;
import com.example.peertutoring.ui.TutorDetailActivity;
import com.google.firebase.auth.FirebaseAuth;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Intent Tests for User Stories 1 through 5.
 * This class uses Espresso to simulate user interactions and verify that
 * the application navigates correctly and displays the expected UI components.
 * 
 * Deliverable: Robust intent tests for completed requirements and US 5.
 * 
 * Fixes for common failure reasons:
 * 1. signOut() in setUp to prevent redirection from MainActivity.
 * 2. scrollTo() for views inside ScrollViews.
 * 3. replaceText() for better stability than typeText().
 * 4. Increased wait times for async operations.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class UserStoriesIntentTest {

    @Before
    public void setUp() {
        Intents.init();
        // Clear session so MainActivity doesn't redirect to HomeActivity
        FirebaseAuth.getInstance().signOut();
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
            
            // Views are in a ScrollView in activity_main.xml
            onView(withId(R.id.editTextEmail)).perform(scrollTo(), replaceText(email), closeSoftKeyboard());
            onView(withId(R.id.editTextPassword)).perform(scrollTo(), replaceText("Password123"), closeSoftKeyboard());
            onView(withId(R.id.editTextConfirmPassword)).perform(scrollTo(), replaceText("Password123"), closeSoftKeyboard());
            onView(withId(R.id.buttonContinue)).perform(scrollTo(), click());
            
            // Wait for registration and auto-login to complete (Firebase is async)
            try { Thread.sleep(8000); } catch (InterruptedException e) {}
        }
    }

    /**
     * Tests US1 & US2: Verifies that registration triggers navigation to the Profile onboarding.
     */
    @Test
    public void testRegistrationIntentToProfileActivity() {
        ActivityScenario.launch(MainActivity.class);
        
        String testEmail = "reg_test_" + System.currentTimeMillis() + "@example.com";
        String testPassword = "Password123";

        onView(withId(R.id.editTextEmail)).perform(scrollTo(), replaceText(testEmail), closeSoftKeyboard());
        onView(withId(R.id.editTextPassword)).perform(scrollTo(), replaceText(testPassword), closeSoftKeyboard());
        onView(withId(R.id.editTextConfirmPassword)).perform(scrollTo(), replaceText(testPassword), closeSoftKeyboard());

        onView(withId(R.id.radioStudent)).perform(scrollTo(), click());
        onView(withId(R.id.buttonContinue)).perform(scrollTo(), click());

        // Wait for Firebase Response and transition
        try { Thread.sleep(8000); } catch (InterruptedException e) {}

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

        // Wait for profile data to load from Firestore
        try { Thread.sleep(5000); } catch (InterruptedException e) {}

        onView(withId(R.id.tabPrivacy)).perform(click());
        onView(withId(R.id.panelPrivacy)).check(matches(isDisplayed()));

        // Perform scroll and click because views are inside a ScrollView
        onView(withId(R.id.switchShowInstitution)).perform(scrollTo(), click());
        
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
        try { Thread.sleep(5000); } catch (InterruptedException e) {}

        onView(withId(R.id.tabVerification)).perform(click());
        
        // Wait for UI panel visibility transition
        try { Thread.sleep(2000); } catch (InterruptedException e) {}

        onView(withId(R.id.panelVerification)).check(matches(isDisplayed()));
        onView(withText("Verify Your Identity")).check(matches(isDisplayed()));
    }

    /**
     * Tests US5: Tutor Recommendations & Browse.
     */
    @Test
    public void testTutorRecommendationsAndSearch() {
        ensureLoggedIn();

        ActivityScenario.launch(BrowseTutorsActivity.class);

        // Wait for tutors to load from Firestore (mocked or real)
        try { Thread.sleep(7000); } catch (InterruptedException e) {}

        // Check if the browse header is visible
        onView(withText("Browse Tutors")).check(matches(isDisplayed()));

        // Perform search - Sarah Johnson is a default tutor in the mock data/layout
        onView(withId(R.id.etSearchTutor)).perform(replaceText("Sarah"), closeSoftKeyboard());
        
        // Brief wait for filtering logic
        try { Thread.sleep(3000); } catch (InterruptedException e) {}
        
        // Verify result count text is updated
        onView(withId(R.id.tvResultCount)).check(matches(withText(containsString("Found"))));
        
        // Click on the tutor card
        onView(withText("Sarah Johnson")).perform(click());
        
        // Verify navigation to TutorDetailActivity
        intended(allOf(
                hasComponent(TutorDetailActivity.class.getName()),
                hasExtra("name", "Sarah Johnson")
        ));
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

        // Wait for Firestore data load
        try { Thread.sleep(5000); } catch (InterruptedException e) {}

        onView(withId(R.id.tabEditProfile)).perform(click());

        onView(withId(R.id.editTextFirstName)).perform(scrollTo(), replaceText("John"), closeSoftKeyboard());
        onView(withId(R.id.editTextLastName)).perform(scrollTo(), replaceText("Doe"), closeSoftKeyboard());
        
        // Toggle to Tutor role to reveal additional fields
        onView(withId(R.id.btnRoleTutor)).perform(scrollTo(), click());
        
        // Wait for role-specific fields to appear
        try { Thread.sleep(2000); } catch (InterruptedException e) {}
        
        onView(withId(R.id.layoutBio)).check(matches(isDisplayed()));
        onView(withId(R.id.editTextBio)).perform(scrollTo(), replaceText("I am a specialized Math and Physics tutor with 4 years of experience."), closeSoftKeyboard());
        
        onView(withId(R.id.btnSaveProfile)).perform(scrollTo(), click());
    }
}
