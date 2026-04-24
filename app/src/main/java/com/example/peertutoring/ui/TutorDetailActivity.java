package com.example.peertutoring.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.peertutoring.R;
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
                })
                .addOnFailureListener(e -> {
                    setupMessageButton(currentUid);
                    setupBookButton(false);
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
            Map<String, Object> convData = new HashMap<>();
            convData.put("participantA", currentUid);
            convData.put("participantB", safeTutorUid);
            convData.put("tutorName", tutorName != null ? tutorName : "Tutor");

            FirebaseFirestore.getInstance()
                    .collection("conversations")
                    .document(convId)
                    .set(convData, SetOptions.merge());

            // Resolve sender display name
            String senderName = "Me";
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null && user.getDisplayName() != null
                    && !user.getDisplayName().isEmpty()) {
                senderName = user.getDisplayName();
            }

            Intent intent = new Intent(TutorDetailActivity.this, MessagingActivity.class);
            intent.putExtra("requestId",       convId);
            intent.putExtra("otherPersonName", tutorName != null ? tutorName : "Tutor");
            intent.putExtra("currentUserName", senderName);
            startActivity(intent);
        });
    }

    /**
     * Book Session button — hidden entirely for tutors.
     * Passes the tutor's actual rate so NewSessionRequestActivity
     * deducts the correct number of tokens.
     */
    private void setupBookButton(boolean isTutor) {
        View btnBook = findViewById(R.id.btnBookSession);
        if (btnBook == null) return;

        if (isTutor) {
            btnBook.setVisibility(View.GONE);
            return;
        }

        btnBook.setVisibility(View.VISIBLE);
        btnBook.setOnClickListener(v -> {
            Intent intent = new Intent(TutorDetailActivity.this, NewSessionRequestActivity.class);
            intent.putExtra("tutorUid",  tutorUid  != null ? tutorUid  : "");
            intent.putExtra("tutorName", tutorName != null ? tutorName : "");
            intent.putExtra("tutorRate", tutorRate); // actual tokens/hr — key fix
            startActivity(intent);
        });
    }

    private void setText(int viewId, String text) {
        TextView tv = findViewById(viewId);
        if (tv != null && text != null) tv.setText(text);
    }
}