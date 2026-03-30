package com.example.peertutoring.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.peertutoring.R;
import com.google.android.material.card.MaterialCardView;

/**
 * Tutor Detail screen — shows full tutor profile.
 * Data passed in via Intent extras from Browse / Home screens.
 * US 04: Displays verification badge if verified.
 */
public class TutorDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutor_detail);

        // Get data from Intent
        String name      = getIntent().getStringExtra("name");
        String subject   = getIntent().getStringExtra("subject");
        String rate      = getIntent().getStringExtra("rate");
        String rating    = getIntent().getStringExtra("rating");
        String students  = getIntent().getStringExtra("students");
        boolean isVerified = getIntent().getBooleanExtra("isVerified", false);

        populateViews(name, subject, rate, rating, students, isVerified);
        setupButtons();
    }

    private void populateViews(String name, String subject, String rate,
                               String rating, String students, boolean isVerified) {
        // Avatar initials
        TextView tvInitials = findViewById(R.id.tvTutorInitials);
        if (name != null && !name.isEmpty()) {
            String[] parts = name.split(" ");
            StringBuilder initials = new StringBuilder();
            if (parts.length > 0) initials.append(parts[0].charAt(0));
            if (parts.length > 1) initials.append(parts[1].charAt(0));
            tvInitials.setText(initials.toString().toUpperCase());
        }

        // US 04: Show/Hide Verified Badge
        MaterialCardView badge = findViewById(R.id.badgeVerified);
        if (badge != null) {
            badge.setVisibility(isVerified ? View.VISIBLE : View.GONE);
        }

        // Name, subject, rating, students, rate
        setText(R.id.tvTutorName,    name);
        setText(R.id.tvTutorSubject, subject);
        setText(R.id.tvRating,       rating);
        setText(R.id.tvStudents,     students);
        setText(R.id.tvRate,         rate);
    }

    private void setupButtons() {
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        findViewById(R.id.btnFavourite).setOnClickListener(v ->
                Toast.makeText(this, "Added to favourites!", Toast.LENGTH_SHORT).show());

        findViewById(R.id.btnShare).setOnClickListener(v ->
                Toast.makeText(this, "Share link copied!", Toast.LENGTH_SHORT).show());

        findViewById(R.id.btnMessage).setOnClickListener(v ->
                Toast.makeText(this, "Opening messages...", Toast.LENGTH_SHORT).show());

        findViewById(R.id.btnBookSession).setOnClickListener(v ->
                Toast.makeText(this, "Booking session...", Toast.LENGTH_SHORT).show());
    }

    private void setText(int viewId, String text) {
        TextView tv = findViewById(viewId);
        if (tv != null && text != null) tv.setText(text);
    }
}