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
 * Activity that displays the detailed profile of a specific tutor.
 * Role: View component for viewing tutor details and initiating bookings/messages.
 * 
 * Implementation of User Story 4: Displays the verification badge if the tutor's
 * identity has been verified by the system administrators.
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

    /**
     * Populates the UI components with the tutor's information.
     * @param name Name of the tutor.
     * @param subject Subject specialization.
     * @param rate Hourly token rate.
     * @param rating Average rating from students.
     * @param students Total number of students tutored.
     * @param isVerified True if the tutor has been verified.
     */
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
        if (badge != null) {
            badge.setVisibility(isVerified ? View.VISIBLE : View.GONE);
        }

        setText(R.id.tvTutorName,    name);
        setText(R.id.tvTutorSubject, subject);
        setText(R.id.tvRating,       rating);
        setText(R.id.tvStudents,     students);
        setText(R.id.tvRate,         rate);
    }

    /** Initializes interactive buttons (Back, Favourite, Message, Book). */
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
        if (btnMsg != null) btnMsg.setOnClickListener(v ->
                Toast.makeText(this, "Opening messages...", Toast.LENGTH_SHORT).show());

        View btnBook = findViewById(R.id.btnBookSession);
        if (btnBook != null) btnBook.setOnClickListener(v ->
                Toast.makeText(this, "Booking session...", Toast.LENGTH_SHORT).show());
    }

    /** Helper to set text to a TextView if it exists. */
    private void setText(int viewId, String text) {
        TextView tv = findViewById(viewId);
        if (tv != null && text != null) tv.setText(text);
    }
}