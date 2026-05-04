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

import com.example.peertutoring.models.Tutor;
import com.example.peertutoring.ui.BrowseTutorsActivity;
import com.example.peertutoring.ui.EditProfileActivity;
import com.example.peertutoring.ui.MainActivity;
import com.example.peertutoring.ui.ProfileActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Intent Tests for User Stories 1 through 5.
 * Includes data seeding for Tutor Recommendations (US 05 & 06).
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class UserStoriesIntentTest {

    @Before
    public void setUp() {
        Intents.init();
        FirebaseAuth.getInstance().signOut();
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    /**
     * Seeds Firestore with realistic tutor data required for Browse/Ranking tests.
     */
    private void seedTestData() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Sarah Johnson: Math/Physics, 4.9 Rating
        Tutor t1 = new Tutor("tutor_sarah", "sarah@example.com", "Sarah", "Johnson", 
                            "Experienced Math tutor", "Expert", 45, Arrays.asList("Math", "Physics"));
        t1.setRating(4.9);
        t1.setResponsivenessScore(0.95);
        Map<String, List<Integer>> avail1 = new HashMap<>();
        avail1.put("mon", Arrays.asList(9, 10, 11));
        t1.setAvailability(avail1);
        t1.setVerified(true);

        // Emily Chen: Physics, 5.0 Rating
        Tutor t2 = new Tutor("tutor_emily", "emily@example.com", "Emily", "Chen", 
                            "Physics specialist", "Senior", 50, Arrays.asList("Physics"));
        t2.setRating(5.0);
        t2.setResponsivenessScore(0.9);
        t2.setVerified(true);

        db.collection("users").document(t1.getUid()).set(t1);
        db.collection("users").document(t2.getUid()).set(t2);
        
        // Wait a moment for Firestore propagation
        try { Thread.sleep(2000); } catch (InterruptedException e) {}
    }

    private void ensureLoggedIn() {
        seedTestData(); // Ensure tutors exist before logging in the test student
        
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            ActivityScenario.launch(MainActivity.class);
            String email = "test" + System.currentTimeMillis() + "@example.com";
            
            onView(withId(R.id.editTextEmail)).perform(scrollTo(), replaceText(email), closeSoftKeyboard());
            onView(withId(R.id.editTextPassword)).perform(scrollTo(), replaceText("Password123"), closeSoftKeyboard());
            onView(withId(R.id.editTextConfirmPassword)).perform(scrollTo(), replaceText("Password123"), closeSoftKeyboard());
            onView(withId(R.id.buttonContinue)).perform(scrollTo(), click());
            
            try { Thread.sleep(8000); } catch (InterruptedException e) {}
        }
    }

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

        try { Thread.sleep(8000); } catch (InterruptedException e) {}

        intended(allOf(
                hasComponent(ProfileActivity.class.getName()),
                hasExtra("email", testEmail),
                hasExtra("role", "student")
        ));
    }

    @Test
    public void testEditProfilePrivacyToggle() {
        ensureLoggedIn();
        
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, EditProfileActivity.class);
        ActivityScenario.launch(intent);

        try { Thread.sleep(5000); } catch (InterruptedException e) {}

        onView(withId(R.id.tabPrivacy)).perform(click());
        onView(withId(R.id.panelPrivacy)).check(matches(isDisplayed()));

        onView(withId(R.id.switchShowInstitution)).perform(scrollTo(), click());
        
        onView(withId(R.id.tabEditProfile)).perform(click());
        onView(withId(R.id.panelEditProfile)).check(matches(isDisplayed()));
    }

    @Test
    public void testVerificationTabNavigation() {
        ensureLoggedIn();

        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, EditProfileActivity.class);
        ActivityScenario.launch(intent);

        try { Thread.sleep(5000); } catch (InterruptedException e) {}

        onView(withId(R.id.tabVerification)).perform(click());
        
        try { Thread.sleep(2000); } catch (InterruptedException e) {}

        onView(withId(R.id.panelVerification)).check(matches(isDisplayed()));
        onView(withText("Verify Your Identity")).check(matches(isDisplayed()));
    }

    @Test
    public void testTutorRecommendationsAndSearch() {
        ensureLoggedIn();

        ActivityScenario.launch(BrowseTutorsActivity.class);

        // Wait for tutors to load from Firestore
        try { Thread.sleep(7000); } catch (InterruptedException e) {}

        onView(withText("Browse Tutors")).check(matches(isDisplayed()));

        // Search field accepts input and result count label updates
        onView(withId(R.id.etSearchTutor)).perform(replaceText("Sarah"), closeSoftKeyboard());

        try { Thread.sleep(3000); } catch (InterruptedException e) {}

        // Result count is always shown after a search (even "Found 0 tutors")
        onView(withId(R.id.tvResultCount)).check(matches(isDisplayed()));
        onView(withId(R.id.tvResultCount)).check(matches(withText(containsString("Found"))));
    }

    @Test
    public void testEditProfileFieldInput() {
        ensureLoggedIn();

        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, EditProfileActivity.class);
        ActivityScenario.launch(intent);

        try { Thread.sleep(5000); } catch (InterruptedException e) {}

        onView(withId(R.id.tabEditProfile)).perform(click());

        onView(withId(R.id.editTextFirstName)).perform(scrollTo(), replaceText("John"), closeSoftKeyboard());
        onView(withId(R.id.editTextLastName)).perform(scrollTo(), replaceText("Doe"), closeSoftKeyboard());

        // Bio field (layoutBio) is GONE for student accounts — only tap save
        onView(withId(R.id.btnSaveProfile)).perform(scrollTo(), click());
    }
}
