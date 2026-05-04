package com.example.peertutoring.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.peertutoring.R;
import com.example.peertutoring.utils.SoundManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Main landing screen of the application.
 * Synchronized with the updated dashboard layout.
 */
public class HomeActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private String userRole = "student";

    private TextView tvUserName, tvTokenBalance, tvGreeting;
    private TextView tvUpcomingSubject, tvUpcomingTime;
    private ListenerRegistration userListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        bindViews();
        setupClickListeners();
        setupBottomNav();
    }

    private void bindViews() {
        tvGreeting        = findViewById(R.id.tvGreeting);
        tvUserName        = findViewById(R.id.tvUserName);
        tvTokenBalance    = findViewById(R.id.tvTokenBalance);
        tvUpcomingSubject = findViewById(R.id.tvUpcomingSubject);
        tvUpcomingTime    = findViewById(R.id.tvUpcomingTime);
    }

    private void setupClickListeners() {

        // Search bar opens Browse Tutors
        View searchBar = findViewById(R.id.tvSearchHint);
        if (searchBar != null) {
            searchBar.setOnClickListener(v -> {
                SoundManager.playClick(this);
                startActivity(new Intent(this, BrowseTutorsActivity.class));
            });
        }

        // Browse Tutors card
        View cardBrowse = findViewById(R.id.cardBrowseTutors);
        if (cardBrowse != null) {
            cardBrowse.setOnClickListener(v -> {
                SoundManager.playClick(this);
                startActivity(new Intent(this, BrowseTutorsActivity.class));
            });
        }

        // My Sessions card
        View cardMyRequests = findViewById(R.id.cardMyRequests);
        if (cardMyRequests != null) {
            cardMyRequests.setOnClickListener(v -> {
                SoundManager.playClick(this);
                startActivity(new Intent(this, SessionRequestsActivity.class));
            });
        }

        // Crash Courses banner — browse all available courses
        View cardCourses = findViewById(R.id.cardCrashCourses);
        if (cardCourses != null) {
            cardCourses.setOnClickListener(v -> {
                SoundManager.playClick(this);
                startActivity(new Intent(this, CoursesActivity.class));
            });
        }

        // My Courses card — see courses the student is enrolled in
        View cardMyCourses = findViewById(R.id.cardMyCourses);
        if (cardMyCourses != null) {
            cardMyCourses.setOnClickListener(v -> {
                SoundManager.playClick(this);
                startActivity(new Intent(this, MyCoursesActivity.class));
            });
        }

        // Token balance chip — opens purchase screen (US-23)
        View chipTokenBalance = findViewById(R.id.chipTokenBalance);
        if (chipTokenBalance != null) {
            chipTokenBalance.setOnClickListener(v -> {
                SoundManager.playClick(this);
                startActivity(new Intent(this, BuyTokensActivity.class));
            });
        }

        // "Need more tokens?" CTA card
        View cardBuyTokens = findViewById(R.id.cardBuyTokens);
        if (cardBuyTokens != null) {
            cardBuyTokens.setOnClickListener(v -> {
                SoundManager.playClick(this);
                startActivity(new Intent(this, BuyTokensActivity.class));
            });
        }

        // View All Sessions
        View tvViewAll = findViewById(R.id.tvViewAll);
        if (tvViewAll != null) {
            tvViewAll.setOnClickListener(v -> {
                SoundManager.playClick(this);
                startActivity(new Intent(this, SessionRequestsActivity.class));
            });
        }

        // Upcoming session card — tappable
        View cardUpcoming = findViewById(R.id.cardUpcomingSession);
        if (cardUpcoming != null) {
            cardUpcoming.setOnClickListener(v ->
                    startActivity(new Intent(this, SessionRequestsActivity.class)));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentUser != null) {
            startUserListener();
            loadUpcomingSession();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (userListener != null) {
            userListener.remove();
            userListener = null;
        }
    }

    // ── Live user data ────────────────────────────────────────

    private void startUserListener() {
        userListener = db.collection("users").document(currentUser.getUid())
                .addSnapshotListener((doc, e) -> {
                    if (e != null || doc == null || !doc.exists()) return;

                    String name     = doc.getString("firstName");
                    String fullName = doc.getString("fullName");
                    Long   tokens   = doc.getLong("tokens");
                    String role     = doc.getString("role");

                    if (role != null) userRole = role;

                    // Tutor landed here — redirect to their dashboard
                    if ("tutor".equals(role)) {
                        startActivity(new Intent(HomeActivity.this, TutorHomeActivity.class)
                                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                        finish();
                        return;
                    }

                    if (tvUserName != null)
                        tvUserName.setText(fullName != null ? fullName : "User");

                    if (tvTokenBalance != null) {
                        long bal = tokens != null ? tokens : 0;
                        tvTokenBalance.setText(
                                NumberFormat.getNumberInstance(Locale.getDefault()).format(bal));
                    }

                    if (tvGreeting != null && name != null && !name.isEmpty()) {
                        int hour = java.util.Calendar.getInstance()
                                .get(java.util.Calendar.HOUR_OF_DAY);
                        String timeGreet = hour < 12 ? "Good morning"
                                : hour < 17 ? "Good afternoon" : "Good evening";
                        tvGreeting.setText(timeGreet + ", " + name + " 👋");
                    }
                });
    }

    // ── Upcoming session ──────────────────────────────────────

    private void loadUpcomingSession() {
        if (currentUser == null) return;
        db.collection("sessionRequests")
                .whereEqualTo("studentUid", currentUser.getUid())
                .whereEqualTo("status", "booked")
                .limit(5)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) return;
                    java.util.Date now = new java.util.Date();
                    com.google.firebase.firestore.DocumentSnapshot soonest = null;
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snap.getDocuments()) {
                        java.util.Date d = doc.getDate("sessionDate");
                        if (d == null) d = doc.getDate("scheduledDate");
                        if (d != null && d.after(now)) {
                            if (soonest == null) soonest = doc;
                        }
                    }
                    if (soonest == null) soonest = snap.getDocuments().get(0);
                    String subject = soonest.getString("subject");
                    String tutor   = soonest.getString("tutorName");
                    java.util.Date date = soonest.getDate("sessionDate");
                    if (date == null) date = soonest.getDate("scheduledDate");
                    if (tvUpcomingSubject != null)
                        tvUpcomingSubject.setText(
                                (subject != null ? subject : "Session")
                                        + (tutor != null ? " with " + tutor : ""));
                    if (tvUpcomingTime != null && date != null)
                        tvUpcomingTime.setText(
                                new java.text.SimpleDateFormat("EEE, MMM d • h:mm a",
                                        Locale.getDefault()).format(date));
                    else if (tvUpcomingTime != null)
                        tvUpcomingTime.setText("Tap to view details");
                });
    }

    // ── Bottom nav ────────────────────────────────────────────

    private void setupBottomNav() {
        View navHome     = findViewById(R.id.navHome);
        View navBrowse   = findViewById(R.id.navBrowse);
        View navMessages = findViewById(R.id.navMessages);
        View navProfile  = findViewById(R.id.navProfile);

        if (navHome != null) navHome.setOnClickListener(v -> { /* already here */ });

        if (navBrowse != null) {
            navBrowse.setOnClickListener(v -> {
                SoundManager.playClick(this);
                startActivity(new Intent(this, BrowseTutorsActivity.class));
                overridePendingTransition(0, 0);
            });
        }

        if (navMessages != null) {
            navMessages.setOnClickListener(v -> {
                SoundManager.playClick(this);
                startActivity(new Intent(this,
                        "tutor".equals(userRole)
                                ? TutorRequestsActivity.class
                                : SessionRequestsActivity.class));
                overridePendingTransition(0, 0);
            });
        }

        if (navProfile != null) {
            navProfile.setOnClickListener(v -> {
                SoundManager.playClick(this);
                startActivity(new Intent(this, EditProfileActivity.class));
                overridePendingTransition(0, 0);
            });
        }
    }
}