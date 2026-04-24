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
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Activity that displays the detailed profile of a specific tutor.
 * Fixed: Message button now opens MessagingActivity with a shared conversation thread.
 * Fixed: Book button opens NewSessionRequestActivity pre-filled with tutor info.
 */
public class TutorDetailActivity extends AppCompatActivity {

    private String tutorUid;
    private String tutorName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutor_detail);

        String name      = getIntent().getStringExtra("name");
        String subject   = getIntent().getStringExtra("subject");
        String rate      = getIntent().getStringExtra("rate");
        String rating    = getIntent().getStringExtra("rating");
        String students  = getIntent().getStringExtra("students");
        boolean isVerified = getIntent().getBooleanExtra("isVerified", false);
        tutorUid  = getIntent().getStringExtra("tutorUid");   // may be null for mock data
        tutorName = name;

        populateViews(name, subject, rate, rating, students, isVerified);
        setupButtons();
    }

    private void populateViews(String name, String subject, String rate,
                               String rating, String students, boolean isVerified) {
        TextView tvInitials = findViewById(R.id.tvTutorInitials);
        if (name != null && !name.isEmpty()) {
            String[] parts = name.split(" ");
            StringBuilder initials = new StringBuilder();
            if (parts.length > 0) initials.append(parts[0].charAt(0));
            if (parts.length > 1) initials.append(parts[1].charAt(0));
            tvInitials.setText(initials.toString().toUpperCase());
        }

        MaterialCardView badge = findViewById(R.id.badgeVerified);
        if (badge != null) badge.setVisibility(isVerified ? View.VISIBLE : View.GONE);

        setText(R.id.tvTutorName,    name);
        setText(R.id.tvTutorSubject, subject);
        setText(R.id.tvRating,       rating);
        setText(R.id.tvStudents,     students);
        setText(R.id.tvRate,         rate);
    }

    private void setupButtons() {
        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        View btnFav = findViewById(R.id.btnFavourite);
        if (btnFav != null) btnFav.setOnClickListener(v ->
                Toast.makeText(this, "Added to favourites!", Toast.LENGTH_SHORT).show());

        View btnShare = findViewById(R.id.btnShare);
        if (btnShare != null) btnShare.setOnClickListener(v ->
                Toast.makeText(this, "Share link copied!", Toast.LENGTH_SHORT).show());

        View btnMsg = findViewById(R.id.btnMessage);
        if (btnMsg != null) btnMsg.setOnClickListener(v -> openMessaging());

        View btnBook = findViewById(R.id.btnBookSession);
        if (btnBook != null) btnBook.setOnClickListener(v -> openNewSessionRequest());
    }

    /**
     * Opens the messaging screen.
     * A conversation thread is identified by combining the two UIDs (sorted),
     * so the same thread is opened regardless of who initiates.
     */
    private void openMessaging() {
        String currentUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

        if (currentUid.isEmpty()) {
            Toast.makeText(this, "Please sign in to message tutors", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a stable conversation ID by sorting the two UIDs
        String convId = tutorUid != null
                ? (currentUid.compareTo(tutorUid) < 0
                ? currentUid + "_" + tutorUid
                : tutorUid + "_" + currentUid)
                : "direct_" + currentUid; // fallback for mock data

        // Ensure the conversation document exists in Firestore
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("conversations").document(convId)
                .set(new java.util.HashMap<String, Object>() {{
                    put("participants", java.util.Arrays.asList(currentUid, tutorUid != null ? tutorUid : ""));
                    put("tutorName", tutorName);
                }}, com.google.firebase.firestore.SetOptions.merge());

        Intent intent = new Intent(this, MessagingActivity.class);
        intent.putExtra("requestId",        convId);  // reuse messages sub-collection pattern
        intent.putExtra("otherPersonName",  tutorName);
        startActivity(intent);
    }

    /**
     * Opens the session request form.
     */
    private void openNewSessionRequest() {
        Intent intent = new Intent(this, NewSessionRequestActivity.class);
        if (tutorName != null) intent.putExtra("tutorName", tutorName);
        if (tutorUid  != null) intent.putExtra("tutorUid",  tutorUid);
        startActivity(intent);
    }

    private void setText(int viewId, String text) {
        TextView tv = findViewById(viewId);
        if (tv != null && text != null) tv.setText(text);
    }
}