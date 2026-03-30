package com.example.peertutoring.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.peertutoring.R;

/**
 * Home screen — shown after onboarding completes.
 * Displays popular subjects and featured tutors.
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
            // Messages screen — to be implemented
        });
        if (navProfile != null)  navProfile.setOnClickListener(v -> {
            startActivity(new Intent(this, EditProfileActivity.class));
            overridePendingTransition(0, 0);
        });
    }

    private void setupTutorCards() {
        // ✅ Use View (not LinearLayout) — cards in XML are MaterialCardView
        View cardTutor1 = findViewById(R.id.cardTutor1);
        View cardTutor2 = findViewById(R.id.cardTutor2);

        if (cardTutor1 != null)
            cardTutor1.setOnClickListener(v ->
                    openTutorDetail("Sarah Johnson", "Mathematics Tutor", "45", "4.9", "127"));
        if (cardTutor2 != null)
            cardTutor2.setOnClickListener(v ->
                    openTutorDetail("Emily Chen", "Physics Tutor", "50", "5.0", "98"));
    }

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