package com.example.peertutoring.ui;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.peertutoring.R;
import com.example.peertutoring.utils.LowBalanceDialog;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

/**
 * Fixes:
 * 1. Tutor's actual rate passed to NewSessionRequestActivity for correct token deduction.
 * 2. Tutors cannot book sessions — Book Session button hidden for tutor role.
 * 3. Messaging no longer crashes — conversation ID built safely without lambdas.
 */
public class TutorDetailActivity extends AppCompatActivity {

    private String tutorUid;
    private String tutorName;
    private int tutorRate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutor_detail);

        tutorName = getIntent().getStringExtra("name");
        tutorUid  = getIntent().getStringExtra("tutorUid");

        String subject    = getIntent().getStringExtra("subject");
        String rateStr    = getIntent().getStringExtra("rate");
        String rating     = getIntent().getStringExtra("rating");
        String students   = getIntent().getStringExtra("students");
        boolean isVerified = getIntent().getBooleanExtra("isVerified", false);

        // Parse tutor's actual token rate
        try {
            tutorRate = (rateStr != null) ? Integer.parseInt(rateStr.trim()) : 100;
        } catch (NumberFormatException e) {
            tutorRate = 100;
        }

        populateViews(tutorName, subject, rateStr, rating, students, isVerified);
        checkRoleAndSetupButtons();
        if (tutorUid != null && !tutorUid.isEmpty()) {
            loadReviews();
            loadTutorProfile();
        }
    }

    private void populateViews(String name, String subject, String rate,
                               String rating, String students, boolean isVerified) {
        TextView tvInitials = findViewById(R.id.tvTutorInitials);
        if (name != null && !name.isEmpty()) {
            String[] parts = name.split(" ");
            StringBuilder initials = new StringBuilder();
            if (parts.length > 0) initials.append(parts[0].charAt(0));
            if (parts.length > 1) initials.append(parts[1].charAt(0));
            if (tvInitials != null) tvInitials.setText(initials.toString().toUpperCase());
        }

        MaterialCardView badge = findViewById(R.id.badgeVerified);
        if (badge != null) badge.setVisibility(isVerified ? View.VISIBLE : View.GONE);

        setText(R.id.tvTutorName,    name);
        setText(R.id.tvTutorSubject, subject);
        setText(R.id.tvRating,       rating);
        setText(R.id.tvStudents,     students);
        setText(R.id.tvRate,         rate);
    }

    private void loadTutorProfile() {
        FirebaseFirestore.getInstance()
                .collection("users").document(tutorUid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;
                    String bio = doc.getString("bio");
                    TextView tvBio = findViewById(R.id.tvBio);
                    if (tvBio != null) {
                        tvBio.setText(bio != null && !bio.isEmpty() ? bio : "No bio provided.");
                    }
                });
    }

    /**
     * Reads the logged-in user's role from Firestore.
     * Tutors get the Book button hidden. Both roles can message.
     */
    private void checkRoleAndSetupButtons() {
        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        View btnFav = findViewById(R.id.btnFavourite);
        if (btnFav != null) btnFav.setOnClickListener(v ->
                Toast.makeText(this, "Added to favourites!", Toast.LENGTH_SHORT).show());

        View btnShare = findViewById(R.id.btnShare);
        if (btnShare != null) btnShare.setOnClickListener(v ->
                Toast.makeText(this, "Share link copied!", Toast.LENGTH_SHORT).show());

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            setupMessageButton("");
            setupBookButton(false);
            setupInstantBookButton(false);
            return;
        }

        final String currentUid = currentUser.getUid();

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(currentUid)
                .get()
                .addOnSuccessListener(doc -> {
                    String role = doc.getString("role");
                    boolean isTutor = "tutor".equals(role);
                    setupMessageButton(currentUid);
                    setupBookButton(isTutor);
                    setupInstantBookButton(isTutor);
                })
                .addOnFailureListener(e -> {
                    setupMessageButton(currentUid);
                    setupBookButton(false);
                    setupInstantBookButton(false);
                });
    }

    /**
     * Message button — opens MessagingActivity with a stable conversation thread.
     * Conversation ID is built by sorting the two UIDs alphabetically so both
     * users always land on the same thread.
     */
    private void setupMessageButton(final String currentUid) {
        View btnMsg = findViewById(R.id.btnMessage);
        if (btnMsg == null) return;

        btnMsg.setOnClickListener(v -> {
            if (currentUid == null || currentUid.isEmpty()) {
                Toast.makeText(this, "Please sign in to send messages.", Toast.LENGTH_SHORT).show();
                return;
            }

            String safeTutorUid = (tutorUid != null && !tutorUid.isEmpty())
                    ? tutorUid : "unknown_tutor";

            // Stable ID — same for both users
            String convId = currentUid.compareTo(safeTutorUid) < 0
                    ? currentUid + "_" + safeTutorUid
                    : safeTutorUid + "_" + currentUid;

            // Write conversation metadata to Firestore
            // Store both names so either participant can identify the other
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            String senderName = "Me";

            FirebaseFirestore.getInstance()
                    .collection("users").document(currentUid).get()
                    .addOnSuccessListener(userDoc -> {
                        String studentFullName = userDoc.exists()
                                ? userDoc.getString("fullName") : null;
                        if (studentFullName == null || studentFullName.isEmpty())
                            studentFullName = user != null && user.getDisplayName() != null
                                    ? user.getDisplayName() : "Student";

                        Map<String, Object> convData = new HashMap<>();
                        convData.put("participantA", currentUid);
                        convData.put("participantB", safeTutorUid);
                        convData.put("tutorUid",     safeTutorUid);
                        convData.put("studentUid",   currentUid);
                        convData.put("tutorName",    tutorName != null ? tutorName : "Tutor");
                        convData.put("studentName",  studentFullName);

                        FirebaseFirestore.getInstance()
                                .collection("conversations")
                                .document(convId)
                                .set(convData, SetOptions.merge());

                        Intent intent = new Intent(TutorDetailActivity.this, MessagingActivity.class);
                        intent.putExtra("requestId",       convId);
                        intent.putExtra("otherPersonName", tutorName != null ? tutorName : "Tutor");
                        intent.putExtra("currentUserName", studentFullName);
                        startActivity(intent);
                    });
            // Early return — intent launched inside callback above

            // Intent launched inside Firestore callback above
        });
    }

    /**
     * Book Session button — hidden entirely for tutors.
     * Checks wallet balance before navigating; shows LowBalanceDialog if insufficient.
     */
    private void setupBookButton(boolean isTutor) {
        View btnBook = findViewById(R.id.btnBookSession);
        if (btnBook == null) return;

        if (isTutor) { btnBook.setVisibility(View.GONE); return; }

        btnBook.setVisibility(View.VISIBLE);
        btnBook.setOnClickListener(v -> checkBalanceAndNavigate(false));
    }

    private void setupInstantBookButton(boolean isTutor) {
        View btnInstant = findViewById(R.id.btnInstantBook);
        if (btnInstant == null) return;

        if (isTutor) { btnInstant.setVisibility(View.GONE); return; }

        btnInstant.setVisibility(View.VISIBLE);
        btnInstant.setOnClickListener(v -> checkBalanceAndNavigate(true));
    }

    /**
     * Fetches the student's token balance before launching a booking screen.
     * If the balance is below the tutor's hourly rate (the minimum session cost),
     * shows LowBalanceDialog instead of navigating.
     */
    private void checkBalanceAndNavigate(boolean isInstantBook) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { launchSession(isInstantBook); return; }

        FirebaseFirestore.getInstance()
                .collection("users").document(user.getUid()).get()
                .addOnSuccessListener(doc -> {
                    long balance = doc.getLong("tokens") != null
                            ? doc.getLong("tokens") : 100L;
                    if (balance < tutorRate) {
                        LowBalanceDialog.show(TutorDetailActivity.this,
                                (int) balance, tutorRate);
                    } else {
                        launchSession(isInstantBook);
                    }
                })
                .addOnFailureListener(e -> launchSession(isInstantBook));
    }

    private void launchSession(boolean isInstantBook) {
        Intent intent = new Intent(this,
                isInstantBook ? InstantBookActivity.class : NewSessionRequestActivity.class);
        intent.putExtra("tutorUid",  tutorUid  != null ? tutorUid  : "");
        intent.putExtra("tutorName", tutorName != null ? tutorName : "");
        intent.putExtra("tutorRate", tutorRate);
        startActivity(intent);
    }

    // ── US 19: Load and display public reviews ────────────────

    private void loadReviews() {
        FirebaseFirestore.getInstance()
                .collection("reviews")
                .whereEqualTo("tutorUid", tutorUid)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(5)
                .get()
                .addOnSuccessListener(snap -> {
                    LinearLayout container = findViewById(R.id.layoutReviews);
                    TextView tvCount = findViewById(R.id.tvReviewCount);
                    TextView tvNone  = findViewById(R.id.tvNoReviews);
                    if (container == null) return;

                    container.removeAllViews();

                    if (snap == null || snap.isEmpty()) {
                        if (tvNone  != null) tvNone.setVisibility(View.VISIBLE);
                        if (tvCount != null) tvCount.setText("No reviews yet");
                        return;
                    }

                    if (tvNone  != null) tvNone.setVisibility(View.GONE);
                    int total = snap.size();
                    if (tvCount != null)
                        tvCount.setText(total + " review" + (total != 1 ? "s" : ""));

                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : snap) {
                        Double r    = doc.getDouble("rating");
                        String name = doc.getString("studentName");
                        String text = doc.getString("reviewText");
                        if (r == null) continue;

                        int stars = (int) Math.round(r);
                        String starStr = "⭐".repeat(Math.max(0, Math.min(5, stars)));

                        // Review item view
                        LinearLayout item = new LinearLayout(this);
                        item.setOrientation(LinearLayout.VERTICAL);
                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                        lp.setMargins(0, 0, 0, dpToPx(16));
                        item.setLayoutParams(lp);

                        // Stars + name row
                        LinearLayout row = new LinearLayout(this);
                        row.setOrientation(LinearLayout.HORIZONTAL);
                        row.setLayoutParams(new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

                        TextView tvStars = new TextView(this);
                        tvStars.setText(starStr);
                        tvStars.setTextSize(14f);
                        tvStars.setLayoutParams(new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                        row.addView(tvStars);

                        if (name != null && !name.isEmpty()) {
                            TextView tvName = new TextView(this);
                            tvName.setText("  " + name);
                            tvName.setTextColor(Color.parseColor("#8B97A8"));
                            tvName.setTextSize(12f);
                            tvName.setTypeface(null, Typeface.BOLD);
                            tvName.setLayoutParams(new LinearLayout.LayoutParams(
                                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                            row.addView(tvName);
                        }
                        item.addView(row);

                        // Review text
                        if (text != null && !text.isEmpty()) {
                            TextView tvText = new TextView(this);
                            tvText.setText(text);
                            tvText.setTextColor(Color.parseColor("#4B5D7A"));
                            tvText.setTextSize(13f);
                            LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                            textLp.setMargins(0, dpToPx(4), 0, 0);
                            tvText.setLayoutParams(textLp);
                            item.addView(tvText);
                        }

                        // Divider (except last)
                        if (!doc.getId().equals(snap.getDocuments()
                                .get(snap.size() - 1).getId())) {
                            View divider = new View(this);
                            divider.setBackgroundColor(Color.parseColor("#F0F0F0"));
                            LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(1));
                            divLp.setMargins(0, dpToPx(8), 0, dpToPx(8));
                            divider.setLayoutParams(divLp);
                            item.addView(divider);
                        }

                        container.addView(item);
                    }
                });
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private void setText(int viewId, String text) {
        TextView tv = findViewById(viewId);
        if (tv != null && text != null) tv.setText(text);
    }
}