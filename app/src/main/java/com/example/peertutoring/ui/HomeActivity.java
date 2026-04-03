package com.example.peertutoring.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.peertutoring.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Main landing screen after successful login or onboarding.
 * Reloads user role from Firestore on every resume so that
 * role changes made in EditProfileActivity take effect immediately.
 */
public class HomeActivity extends AppCompatActivity {

    private String userRole = "student"; // cached role

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        setupTutorCards();
        setupSubjectCards();
        // Nav is wired in onResume so it always reflects the latest role
    }

    // ── Re-check role every time we return to this screen ────
    // This fires after EditProfileActivity finishes, so the
    // role saved there is immediately picked up here.

    @Override
    protected void onResume() {
        super.onResume();
        loadUserRoleThenSetupNav();
    }

    // ── Load role from Firestore ──────────────────────────────

    private void loadUserRoleThenSetupNav() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            setupBottomNav();
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String role = doc.getString("role");
                        if (role != null) userRole = role;
                    }
                    setupBottomNav();
                })
                .addOnFailureListener(e -> setupBottomNav());
    }

    // ── Bottom nav ────────────────────────────────────────────

    private void setupBottomNav() {
        View navHome     = findViewById(R.id.navHome);
        View navBrowse   = findViewById(R.id.navBrowse);
        View navMessages = findViewById(R.id.navMessages);
        View navProfile  = findViewById(R.id.navProfile);

        if (navHome != null) {
            navHome.setOnClickListener(v -> { /* already here */ });
        }

        if (navBrowse != null) {
            navBrowse.setOnClickListener(v -> {
                startActivity(new Intent(this, BrowseTutorsActivity.class));
                overridePendingTransition(0, 0);
            });
        }

        if (navMessages != null) {
            navMessages.setOnClickListener(v -> {
                if ("tutor".equals(userRole)) {
                    startActivity(new Intent(this, TutorRequestsActivity.class));
                } else {
                    startActivity(new Intent(this, SessionRequestsActivity.class));
                }
                overridePendingTransition(0, 0);
            });
        }

        if (navProfile != null) {
            navProfile.setOnClickListener(v -> {
                startActivity(new Intent(this, EditProfileActivity.class));
                overridePendingTransition(0, 0);
            });
        }
    }

    // ── Tutor cards ───────────────────────────────────────────

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

    // ── Subject cards ─────────────────────────────────────────

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

    // ── Helper ────────────────────────────────────────────────

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