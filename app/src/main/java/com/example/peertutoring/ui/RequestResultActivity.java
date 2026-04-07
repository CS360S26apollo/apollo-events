package com.example.peertutoring.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.peertutoring.R;
import com.google.android.material.card.MaterialCardView;

/**
 * Activity that displays a confirmation message after a tutor responds to a request.
 * Role: Feedback View for User Story 09 (Tutor Response).
 * Purpose: Provides visual confirmation to the tutor that their action (Accept, 
 * Decline, or Counter Offer) has been processed and the student has been notified.
 * 
 * Design Pattern: Multi-state View (Context-sensitive UI based on resultType).
 */
public class RequestResultActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_result);

        String resultType    = getIntent().getStringExtra("resultType");
        String studentName   = getIntent().getStringExtra("studentName");
        String subject       = getIntent().getStringExtra("subject");
        String date          = getIntent().getStringExtra("date");
        String time          = getIntent().getStringExtra("time");
        int    duration      = getIntent().getIntExtra("duration", 60);
        int    tokens        = getIntent().getIntExtra("tokens", 150);

        String counterDate     = getIntent().getStringExtra("counterDate");
        String counterTime     = getIntent().getStringExtra("counterTime");
        int    counterDuration = getIntent().getIntExtra("counterDuration", duration);
        int    counterTokens   = getIntent().getIntExtra("counterTokens",   tokens);

        applyResultStyle(resultType);
        populateSummary(studentName, subject, date, time, duration, tokens);

        if ("counter".equals(resultType)) {
            showCounterSummary(counterDate, counterTime, counterDuration, counterTokens);
        }

        Button btnBack = findViewById(R.id.btnBackToRequests);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                Intent intent = new Intent(this, TutorRequestsActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            });

            if ("declined".equals(resultType)) {
                btnBack.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(0xFFAA00AA));
            } else if ("counter".equals(resultType)) {
                btnBack.setBackground(getDrawable(R.drawable.bg_button_gradient));
                btnBack.setBackgroundTintList(null);
            }
        }
    }

    /**
     * Applies specific themes, icons, and messages based on the tutor's action.
     * @param resultType The type of action performed ('accepted', 'declined', or 'counter').
     */
    private void applyResultStyle(String resultType) {
        MaterialCardView iconCard = findViewById(R.id.cardResultIcon);
        TextView tvIcon           = findViewById(R.id.tvResultIcon);
        TextView tvTitle          = findViewById(R.id.tvResultTitle);
        TextView tvSubtitle       = findViewById(R.id.tvResultSubtitle);

        if (resultType == null) return;

        switch (resultType) {
            case "accepted":
                if (iconCard != null) iconCard.setCardBackgroundColor(0xFF00C853);
                if (tvIcon   != null) tvIcon.setText("✓");
                if (tvTitle  != null) tvTitle.setText("Request Accepted!");
                if (tvSubtitle != null) tvSubtitle.setText("The student has been notified");
                break;

            case "declined":
                if (iconCard != null) iconCard.setCardBackgroundColor(0xFFAA00AA);
                if (tvIcon   != null) tvIcon.setText("✕");
                if (tvTitle  != null) tvTitle.setText("Request Declined");
                if (tvSubtitle != null) tvSubtitle.setText("The student has been notified");
                break;

            case "counter":
                if (iconCard != null) iconCard.setCardBackgroundColor(0xFF4527A0);
                if (tvIcon   != null) tvIcon.setText("✈");
                if (tvTitle  != null) tvTitle.setText("Counter Offer Sent!");
                if (tvSubtitle != null) tvSubtitle.setText("Waiting for student response");
                break;
        }
    }

    /** Populates the main summary card with details of the student's original request. */
    private void populateSummary(String studentName, String subject,
                                 String date, String time, int duration, int tokens) {
        TextView tvInitials = findViewById(R.id.tvSummaryInitials);
        if (tvInitials != null && studentName != null) {
            String[] parts = studentName.split(" ");
            String initials = parts.length > 1
                    ? "" + parts[0].charAt(0) + parts[1].charAt(0)
                    : "" + parts[0].charAt(0);
            tvInitials.setText(initials.toUpperCase());
        }

        setText(R.id.tvSummaryName,     studentName);
        setText(R.id.tvSummarySubject,  subject);
        setText(R.id.tvSummaryDate,     date     != null ? date     : "TBD");
        setText(R.id.tvSummaryTime,     time     != null ? time     : "TBD");
        setText(R.id.tvSummaryDuration, duration + " minutes");
        setText(R.id.tvSummaryTokens,   tokens   + " tokens");
    }

    /**
     * Shows and populates the secondary summary card for counter-offers.
     */
    private void showCounterSummary(String date, String time, int duration, int tokens) {
        LinearLayout layout = findViewById(R.id.layoutCounterOfferSummary);
        if (layout != null) layout.setVisibility(View.VISIBLE);

        setText(R.id.tvCounterDate,     date     != null ? date     : "TBD");
        setText(R.id.tvCounterTime,     time     != null ? time     : "TBD");
        setText(R.id.tvCounterDuration, duration + " min");
        setText(R.id.tvCounterTokens,   tokens   + " tokens");
    }

    private void setText(int viewId, String text) {
        TextView tv = findViewById(viewId);
        if (tv != null && text != null) tv.setText(text);
    }
}