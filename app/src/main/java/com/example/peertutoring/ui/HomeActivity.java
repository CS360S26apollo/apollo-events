package com.example.peertutoring.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;

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
        findViewById(R.id.navHome).setOnClickListener(v -> {
            // Already on home
        });

        findViewById(R.id.navBrowse).setOnClickListener(v -> {
            startActivity(new Intent(this, BrowseTutorsActivity.class));
            overridePendingTransition(0, 0);
        });

        findViewById(R.id.navMessages).setOnClickListener(v -> {
            // Messages screen — to be implemented
        });

        findViewById(R.id.navProfile).setOnClickListener(v -> {
            startActivity(new Intent(this, EditProfileActivity.class));
            overridePendingTransition(0, 0);
        });
    }

    private void setupTutorCards() {
        // Click on featured tutor cards → open TutorDetailActivity
        LinearLayout cardTutor1 = findViewById(R.id.cardTutor1);
        LinearLayout cardTutor2 = findViewById(R.id.cardTutor2);

        if (cardTutor1 != null) {
            cardTutor1.setOnClickListener(v -> openTutorDetail("Sarah Johnson", "Mathematics Tutor", "45", "4.9", "127"));
        }
        if (cardTutor2 != null) {
            cardTutor2.setOnClickListener(v -> openTutorDetail("Emily Chen", "Physics Tutor", "50", "5.0", "98"));
        }
    }

    private void setupSubjectCards() {
        // Subject cards navigate to Browse with filter
        int[] subjectCardIds = {R.id.cardMath, R.id.cardScience, R.id.cardEnglish,
                R.id.cardCoding, R.id.cardMusic, R.id.cardArt};

        for (int id : subjectCardIds) {
            if (findViewById(id) != null) {
                findViewById(id).setOnClickListener(v -> {
                    startActivity(new Intent(this, BrowseTutorsActivity.class));
                    overridePendingTransition(0, 0);
                });
            }
        }
    }

    private void openTutorDetail(String name, String subject, String rate,
                                 String rating, String students) {
        Intent intent = new Intent(this, TutorDetailActivity.class);
        intent.putExtra("name", name);
        intent.putExtra("subject", subject);
        intent.putExtra("rate", rate);
        intent.putExtra("rating", rating);
        intent.putExtra("students", students);
        startActivity(intent);
    }
}