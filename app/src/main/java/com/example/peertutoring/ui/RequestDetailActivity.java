package com.example.peertutoring.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.peertutoring.R;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Tutor reviews a specific student session request.
 * Actions: Accept Request | Propose Counter Offer | Decline Request
 */
public class RequestDetailActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String requestId, studentName, subject, topic, date, time, goals, studentMessage;
    private int duration, tokens;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_detail);

        db = FirebaseFirestore.getInstance();

        // Receive data
        requestId     = getIntent().getStringExtra("requestId");
        studentName   = getIntent().getStringExtra("studentName");
        subject       = getIntent().getStringExtra("subject");
        topic         = getIntent().getStringExtra("topic");
        date          = getIntent().getStringExtra("date");
        time          = getIntent().getStringExtra("time");
        duration      = getIntent().getIntExtra("duration", 60);
        tokens        = getIntent().getIntExtra("tokens", 150);
        goals         = getIntent().getStringExtra("goals");
        studentMessage = getIntent().getStringExtra("studentMessage");

        populateViews();
        setupButtons();
    }

    private void populateViews() {
        // Avatar initials
        TextView tvInitials = findViewById(R.id.tvStudentInitials);
        if (tvInitials != null && studentName != null) {
            String[] parts = studentName.split(" ");
            String initials = parts.length > 1
                    ? "" + parts[0].charAt(0) + parts[1].charAt(0)
                    : "" + parts[0].charAt(0);
            tvInitials.setText(initials.toUpperCase());
        }

        setText(R.id.tvStudentName,    studentName);
        setText(R.id.tvSubject,        subject);
        setText(R.id.tvTopic,          topic);
        setText(R.id.tvDate,           date != null ? date : "To be confirmed");
        setText(R.id.tvTime,           time != null ? time : "To be confirmed");
        setText(R.id.tvDuration,       duration + " minutes");
        setText(R.id.tvOfferedTokens,  tokens + " Tokens");
        setText(R.id.tvStudentMessage, studentMessage != null ? studentMessage
                : (goals != null ? goals : "No message provided."));
    }

    private void setupButtons() {
        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        Button btnAccept  = findViewById(R.id.btnAcceptRequest);
        Button btnCounter = findViewById(R.id.btnCounterOffer);
        Button btnDecline = findViewById(R.id.btnDeclineRequest);

        if (btnAccept  != null) btnAccept.setOnClickListener(v  -> acceptRequest());
        if (btnCounter != null) btnCounter.setOnClickListener(v -> openCounterOffer());
        if (btnDecline != null) btnDecline.setOnClickListener(v -> declineRequest());
    }

    // ── Accept ────────────────────────────────────────────────

    private void acceptRequest() {
        updateFirestoreStatus("accepted");

        Intent intent = new Intent(this, RequestResultActivity.class);
        intent.putExtra("resultType",   "accepted");
        intent.putExtra("studentName",  studentName);
        intent.putExtra("subject",      subject);
        intent.putExtra("date",         date);
        intent.putExtra("time",         time);
        intent.putExtra("duration",     duration);
        intent.putExtra("tokens",       tokens);
        startActivity(intent);
        finish();
    }

    // ── Counter offer ─────────────────────────────────────────

    private void openCounterOffer() {
        Intent intent = new Intent(this, CounterOfferActivity.class);
        intent.putExtra("requestId",   requestId);
        intent.putExtra("studentName", studentName);
        intent.putExtra("subject",     subject);
        intent.putExtra("topic",       topic);
        intent.putExtra("date",        date);
        intent.putExtra("time",        time);
        intent.putExtra("duration",    duration);
        intent.putExtra("tokens",      tokens);
        startActivity(intent);
    }

    // ── Decline ───────────────────────────────────────────────

    private void declineRequest() {
        updateFirestoreStatus("declined");

        Intent intent = new Intent(this, RequestResultActivity.class);
        intent.putExtra("resultType",  "declined");
        intent.putExtra("studentName", studentName);
        intent.putExtra("subject",     subject);
        intent.putExtra("date",        date);
        intent.putExtra("time",        time);
        intent.putExtra("duration",    duration);
        intent.putExtra("tokens",      tokens);
        startActivity(intent);
        finish();
    }

    // ── Firestore update ──────────────────────────────────────

    private void updateFirestoreStatus(String status) {
        if (requestId == null || requestId.isEmpty()) return;
        Map<String, Object> update = new HashMap<>();
        update.put("status", status);
        db.collection("sessionRequests").document(requestId)
                .update(update)
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Update failed: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    private void setText(int viewId, String text) {
        TextView tv = findViewById(viewId);
        if (tv != null && text != null) tv.setText(text);
    }
}