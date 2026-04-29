package com.example.peertutoring.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.peertutoring.R;
import com.example.peertutoring.utils.ExpirationUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class TutorHomeActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String tutorUid;

    private TextView tvGreeting, tvTutorName, tvPendingCount, tvEarningsCount, tvRating, tvRequestsBadge;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutor_home);

        db = FirebaseFirestore.getInstance();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        tutorUid = user != null ? user.getUid() : "";

        tvGreeting      = findViewById(R.id.tvGreeting);
        tvTutorName     = findViewById(R.id.tvTutorName);
        tvPendingCount  = findViewById(R.id.tvPendingCount);
        tvEarningsCount = findViewById(R.id.tvEarningsCount);
        tvRating        = findViewById(R.id.tvRating);
        tvRequestsBadge = findViewById(R.id.tvRequestsBadge);

        setupQuickActions();
        setupBottomNav();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!tutorUid.isEmpty()) {
            ExpirationUtils.expireStaleRequestsForTutor(tutorUid, db);
            loadTutorData();
        }
    }

    private void loadTutorData() {
        db.collection("users").document(tutorUid).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    String name = doc.getString("fullName");
                    if (name != null && !name.isEmpty()) {
                        tvGreeting.setText("Welcome back,");
                        tvTutorName.setText(name.split(" ")[0]);
                    }

                    Double rating = doc.getDouble("rating");
                    if (rating != null) {
                        tvRating.setText(String.format("%.1f", rating));
                    }

                    Long earnings = doc.getLong("totalEarnings");
                    if (earnings != null) {
                        tvEarningsCount.setText(String.valueOf(earnings));
                    }
                });

        db.collection("sessionRequests")
                .whereEqualTo("tutorUid", tutorUid)
                .whereEqualTo("status", "requested")
                .get()
                .addOnSuccessListener(snap -> {
                    int count = snap.size();
                    tvPendingCount.setText(String.valueOf(count));
                    tvRequestsBadge.setText(count > 0 ? count + " new" : "Up to date");
                });
    }

    private void setupQuickActions() {
        findViewById(R.id.cardViewRequests).setOnClickListener(v ->
                startActivity(new Intent(this, TutorRequestsActivity.class)));

        findViewById(R.id.cardAvailability).setOnClickListener(v ->
                startActivity(new Intent(this, AvailabilityDashboardActivity.class)));

        findViewById(R.id.cardEarnings).setOnClickListener(v ->
                startActivity(new Intent(this, TutorEarningsActivity.class)));

        // Chat is session-scoped: open a session from TutorRequests, then message from there.
        // Opening MessagingActivity without a conversationId shows an empty screen.
        findViewById(R.id.cardMessages).setOnClickListener(v ->
                startActivity(new Intent(this, TutorRequestsActivity.class)));

        findViewById(R.id.cardOffers).setOnClickListener(v ->
                startActivity(new Intent(this, TutorOffersActivity.class)));

        findViewById(R.id.cardProfile).setOnClickListener(v ->
                startActivity(new Intent(this, EditProfileActivity.class)));

        findViewById(R.id.btnEarnings).setOnClickListener(v ->
                startActivity(new Intent(this, TutorEarningsActivity.class)));

        findViewById(R.id.btnSeedData).setOnClickListener(v ->
                startActivity(new Intent(this, SeedDataActivity.class)));
    }

    private void setupBottomNav() {
        findViewById(R.id.navHome).setOnClickListener(v -> { /* already here */ });

        findViewById(R.id.navRequests).setOnClickListener(v -> {
            startActivity(new Intent(this, TutorRequestsActivity.class));
            overridePendingTransition(0, 0);
        });

        findViewById(R.id.navEarnings).setOnClickListener(v -> {
            startActivity(new Intent(this, TutorEarningsActivity.class));
            overridePendingTransition(0, 0);
        });

        findViewById(R.id.navAvailability).setOnClickListener(v -> {
            startActivity(new Intent(this, AvailabilityDashboardActivity.class));
            overridePendingTransition(0, 0);
        });

        findViewById(R.id.navProfile).setOnClickListener(v -> {
            startActivity(new Intent(this, EditProfileActivity.class));
            overridePendingTransition(0, 0);
        });
    }
}
