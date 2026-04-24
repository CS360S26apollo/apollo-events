package com.example.peertutoring.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.peertutoring.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Main landing screen after successful login or onboarding.
 *
 * Fix: tvTokenBalance is now loaded from Firestore in real-time using a snapshot
 * listener, so whenever tokens are deducted (session request posted, offer accepted),
 * the balance on this screen updates automatically without needing a manual refresh.
 */
public class HomeActivity extends AppCompatActivity {

    private String userRole = "student";
    private TextView tvTokenBalance;
    private ListenerRegistration tokenListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        tvTokenBalance = findViewById(R.id.tvTokenBalance);

        setupTutorCards();
        setupSubjectCards();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserRoleThenSetupNav();
        startTokenListener();   // attach real-time token balance listener
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Detach listener when screen is not visible to avoid unnecessary reads
        if (tokenListener != null) {
            tokenListener.remove();
            tokenListener = null;
        }
    }

    // ── Real-time token balance ───────────────────────────────

    /**
     * Attaches a Firestore snapshot listener to the current user's document.
     * Every time the "tokens" field changes (deduction or top-up), the UI updates instantly.
     */
    private void startTokenListener() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            updateTokenDisplay(100); // default for demo
            return;
        }

        tokenListener = FirebaseFirestore.getInstance()
                .collection("users")
                .document(currentUser.getUid())
                .addSnapshotListener((doc, e) -> {
                    if (e != null || doc == null) return;

                    Long tokens = doc.getLong("tokens");
                    if (tokens == null) {
                        // First-time user: initialise balance to 100 tokens
                        FirebaseFirestore.getInstance()
                                .collection("users")
                                .document(currentUser.getUid())
                                .update("tokens", 100L);
                        updateTokenDisplay(100);
                    } else {
                        updateTokenDisplay(tokens.intValue());
                    }
                });
    }

    /**
     * Updates the token balance TextView with comma-formatted number.
     * e.g. 1250 → "1,250"
     */
    private void updateTokenDisplay(int tokens) {
        if (tvTokenBalance != null) {
            String formatted = NumberFormat.getNumberInstance(Locale.getDefault()).format(tokens);
            tvTokenBalance.setText(formatted);
        }
    }

    // ── Role loading ──────────────────────────────────────────

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