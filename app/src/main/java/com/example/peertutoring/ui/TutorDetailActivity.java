package com.example.peertutoring.ui;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.peertutoring.R;

/**
 * Tutor Detail screen — shows full tutor profile.
 * Data passed in via Intent extras from Browse / Home screens.
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

        populateViews(name, subject, rate, rating, students);
        setupButtons();
    }

    private void populateViews(String name, String subject, String rate,
                               String rating, String students) {
        // Avatar initials
        TextView tvInitials = findViewById(R.id.tvTutorInitials);
        if (name != null && !name.isEmpty()) {
            String[] parts = name.split(" ");
            String initials = "";
            if (parts.length > 0) initials += parts[0].charAt(0);
            if (parts.length > 1) initials += parts[1].charAt(0);
            tvInitials.setText(initials.toUpperCase());
        }

        // Name, subject, rating, students, rate
        setText(R.id.tvTutorName,    name);
        setText(R.id.tvTutorSubject, subject);
        setText(R.id.tvRating,       rating);
        setText(R.id.tvStudents,     students);
        setText(R.id.tvRate,         rate);
    }

    private void setupButtons() {
        // Back
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // Favourite
        findViewById(R.id.btnFavourite).setOnClickListener(v ->
                Toast.makeText(this, "Added to favourites!", Toast.LENGTH_SHORT).show());

        // Share
        findViewById(R.id.btnShare).setOnClickListener(v ->
                Toast.makeText(this, "Share link copied!", Toast.LENGTH_SHORT).show());

        // Message
        findViewById(R.id.btnMessage).setOnClickListener(v ->
                Toast.makeText(this, "Opening messages…", Toast.LENGTH_SHORT).show());

        // Book Session
        findViewById(R.id.btnBookSession).setOnClickListener(v ->
                Toast.makeText(this, "Booking session…", Toast.LENGTH_SHORT).show());
    }

    private void setText(int viewId, String text) {
        TextView tv = findViewById(viewId);
        if (tv != null && text != null) tv.setText(text);
    }
}