package com.example.peertutoring.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.peertutoring.R;

/**
 * Main landing screen of the application after successful login or onboarding.
 * Role: View component for the application dashboard.
 * Purpose: Displays a summary of popular subjects and featured tutors to the user.
 * 
 * Implementation Details:
 * - Provides navigation to the tutor marketplace and user profile.
 * - Showcases a curated list of top-rated tutors for quick access.
 */
public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        setupBottomNav();
        setupTutorCards();
        setupSubjectCards();
    }

    /** Initializes the bottom navigation menu and its associated click actions. */
    private void setupBottomNav() {
        View navHome     = findViewById(R.id.navHome);
        View navBrowse   = findViewById(R.id.navBrowse);
        View navMessages = findViewById(R.id.navMessages);
        View navProfile  = findViewById(R.id.navProfile);

        if (navHome != null)     navHome.setOnClickListener(v -> { /* already on home */ });
        if (navBrowse != null)   navBrowse.setOnClickListener(v -> {
            startActivity(new Intent(this, BrowseTutorsActivity.class));
            overridePendingTransition(0, 0);
        });
        if (navMessages != null) navMessages.setOnClickListener(v -> {
            startActivity(new Intent(this, SessionRequestsActivity.class));
            overridePendingTransition(0, 0);
        });
        if (navProfile != null)  navProfile.setOnClickListener(v -> {
            startActivity(new Intent(this, EditProfileActivity.class));
            overridePendingTransition(0, 0);
        });
    }

    /** Sets up click listeners for featured tutor cards to open their detailed views. */
    private void setupTutorCards() {
        View cardTutor1 = findViewById(R.id.cardTutor1);
        View cardTutor2 = findViewById(R.id.cardTutor2);

        if (cardTutor1 != null)
            cardTutor1.setOnClickListener(v ->
                    openTutorDetail("Sarah Johnson", "Mathematics Tutor", "45", "4.9", "127"));
        if (cardTutor2 != null)
            cardTutor2.setOnClickListener(v ->
                    openTutorDetail("Emily Chen", "Physics Tutor", "50", "5.0", "98"));
    }

    /** Configures subject cards to navigate to the browse screen with pre-applied filters. */
    private void setupSubjectCards() {
        int[] subjectCardIds = {
                R.id.cardMath, R.id.cardScience, R.id.cardEnglish,
                R.id.cardCoding, R.id.cardMusic, R.id.cardArt
        };

        for (int id : subjectCardIds) {
            View card = findViewById(id);
            if (card != null) {
                card.setOnClickListener(v -> {
                    startActivity(new Intent(this, BrowseTutorsActivity.class));
                    overridePendingTransition(0, 0);
                });
            }
        }
    }

    /**
     * Helper method to open the TutorDetailActivity with static data (for prototype).
     * @param name Name of the tutor.
     * @param subject Subject specialization.
     * @param rate Hourly rate.
     * @param rating Average rating.
     * @param students Total students count.
     */
    private void openTutorDetail(String name, String subject, String rate,
                                 String rating, String students) {
        Intent intent = new Intent(this, TutorDetailActivity.class);
        intent.putExtra("name",     name);
        intent.putExtra("subject",  subject);
        intent.putExtra("rate",     rate);
        intent.putExtra("rating",   rating);
        intent.putExtra("students", students);
        startActivity(intent);
    }
}