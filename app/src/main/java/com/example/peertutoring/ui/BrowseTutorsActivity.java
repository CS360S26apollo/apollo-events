package com.example.peertutoring.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.example.peertutoring.R;

/**
 * Activity for browsing and searching available tutors.
 * Role: View component for the tutor marketplace/search feature.
 * Purpose: Allows students to find tutors by name or subject and view their profiles.
 * 
 * Implementation of User Story 4: Displays verification status in the tutor list
 * to help students identify trusted tutors.
 */
public class BrowseTutorsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browse_tutors);

        setupSearch();
        setupTutorCards();
        setupBottomNav();
    }

    /**
     * Initializes the search input field with a text listener.
     * Note: Full search filtering logic is currently a stub for Phase 3.
     */
    private void setupSearch() {
        EditText etSearch = findViewById(R.id.etSearchTutor);
        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
                @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                    // Filter tutor list based on search query
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }
    }

    /**
     * Sets up click listeners for the tutor cards displayed in the list.
     * Navigates to the detailed view of the selected tutor.
     */
    private void setupTutorCards() {
        // Sarah Johnson (Verified)
        if (findViewById(R.id.cardTutor1) != null) {
            findViewById(R.id.cardTutor1).setOnClickListener(v ->
                    openTutorDetail("Sarah Johnson", "Mathematics Tutor", "45", "4.9", "127", true));
        }

        // Emily Chen (Verified)
        if (findViewById(R.id.cardTutor2) != null) {
            findViewById(R.id.cardTutor2).setOnClickListener(v ->
                    openTutorDetail("Emily Chen", "Physics Tutor", "50", "5.0", "98", true));
        }
    }

    /**
     * Initializes the bottom navigation bar and its destination routing.
     */
    private void setupBottomNav() {
        if (findViewById(R.id.navHome) != null) {
            findViewById(R.id.navHome).setOnClickListener(v -> {
                startActivity(new Intent(this, HomeActivity.class));
                overridePendingTransition(0, 0);
            });
        }

        findViewById(R.id.navBrowse).setOnClickListener(v -> {
            // Already on Browse screen
        });

        if (findViewById(R.id.navProfile) != null) {
            findViewById(R.id.navProfile).setOnClickListener(v -> {
                startActivity(new Intent(this, EditProfileActivity.class));
                overridePendingTransition(0, 0);
            });
        }
    }

    /**
     * Helper to launch the TutorDetailActivity with relevant data.
     * @param name Name of the tutor.
     * @param subject Main subject taught.
     * @param rate Hourly token rate.
     * @param rating Average rating.
     * @param students Number of students taught.
     * @param isVerified Whether the tutor has a verified identity badge.
     */
    private void openTutorDetail(String name, String subject, String rate,
                                 String rating, String students, boolean isVerified) {
        Intent intent = new Intent(this, TutorDetailActivity.class);
        intent.putExtra("name", name);
        intent.putExtra("subject", subject);
        intent.putExtra("rate", rate);
        intent.putExtra("rating", rating);
        intent.putExtra("students", students);
        intent.putExtra("isVerified", isVerified);
        startActivity(intent);
    }
}