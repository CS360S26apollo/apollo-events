package com.example.peertutoring.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.peertutoring.R;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Activity that displays the full details of a specific session request.
 * Role: Tracking/Management View for User Story 10 (Track Request Status) 
 * and User Story 09 (Tutor Response).
 * 
 * Purpose: Provides a dual-purpose interface. Students use it to track the 
 * progress of their pending requests, while tutors use it to review and 
 * potentially accept or decline incoming session proposals.
 * 
 * Design Pattern: Dynamic View-Model populated by Intent or Firestore.
 */
public class RequestDetailActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String requestId;
    private boolean isStudentView;
    private LinearLayout layoutOfferContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_detail);

        db = FirebaseFirestore.getInstance();
        
        requestId = getIntent().getStringExtra("requestId");
        String intentTopic = getIntent().getStringExtra("topic");
        String intentStatus = getIntent().getStringExtra("status");
        String intentProvider = getIntent().getStringExtra("provider");
        String intentCategory = getIntent().getStringExtra("category");
        int intentDuration = getIntent().getIntExtra("duration", 60);
        int intentTokens = getIntent().getIntExtra("tokens", 0);
        isStudentView = getIntent().getBooleanExtra("isStudentView", false);

        layoutOfferContainer = findViewById(R.id.layoutOfferContainer);

        populateFromIntent(intentTopic, intentStatus, intentProvider, intentCategory, intentDuration, intentTokens);
        
        setupButtons();
        
        if (requestId != null && !requestId.startsWith("mock_")) {
            loadRequestData();
            if (isStudentView) {
                loadProviderOffers();
            }
        }
    }

    /**
     * Quickly populates the UI using data passed through the navigation Intent.
     */
    private void populateFromIntent(String topic, String status, String provider, String category, int duration, int tokens) {
        setText(R.id.tvTopic, topic != null ? topic : "Request Detail");
        setText(R.id.tvProviderName, provider != null ? provider : "Tutor Pending");
        setText(R.id.tvCategory, category != null ? category : "Education");
        setText(R.id.tvTokens, String.valueOf(tokens));
        setText(R.id.tvDuration, (duration / 60) + " hours");
        updateStatusUI(status);
    }

    /**
     * Subscribes to real-time updates for the session request document in Firestore.
     * Also drives the post-completion action buttons for US 17 and US 19.
     */
    private void loadRequestData() {
        String currentUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

        db.collection("sessionRequests").document(requestId)
                .addSnapshotListener((doc, e) -> {
                    if (e != null || doc == null || !doc.exists()) return;

                    String status      = doc.getString("status");
                    String provider    = doc.getString("tutorName");
                    String docTutorUid = doc.getString("tutorUid");
                    String docStudentUid = doc.getString("studentUid");

                    setText(R.id.tvProviderName, provider != null ? provider : "Searching...");
                    updateStatusUI(status);

                    // Post-completion actions — only for completed sessions
                    if ("completed".equals(status)) {
                        boolean isTutor   = currentUid.equals(docTutorUid);
                        boolean isStudent = currentUid.equals(docStudentUid);

                        if (isTutor) {
                            boolean notesAdded = Boolean.TRUE.equals(doc.getBoolean("notesAdded"));
                            setupAddNotesButton(
                                    docStudentUid,
                                    docTutorUid,
                                    doc.getString("tutorName"),
                                    doc.getString("studentName"),
                                    doc.getString("subject"),
                                    notesAdded);
                        } else if (isStudent) {
                            boolean reviewed = Boolean.TRUE.equals(doc.getBoolean("reviewSubmitted"));
                            setupRateReviewButton(docTutorUid, doc.getString("tutorName"), reviewed);
                        }
                    }
                });
    }

    // ── US 17: Add Session Notes (tutor only, completed sessions) ────────────

    private void setupAddNotesButton(String studentUid, String tutorUid,
                                     String tutorName, String studentName,
                                     String subject, boolean alreadySent) {
        Button btn = findViewById(R.id.btnAddNotes);
        if (btn == null) return;

        btn.setVisibility(View.VISIBLE);

        if (alreadySent) {
            btn.setText("📝  Notes Sent ✓");
            btn.setEnabled(false);
            btn.setAlpha(0.6f);
            return;
        }

        btn.setOnClickListener(v -> {
            Intent intent = new Intent(this, SessionNotesActivity.class);
            intent.putExtra("requestId",   requestId);
            intent.putExtra("tutorUid",    tutorUid);
            intent.putExtra("studentUid",  studentUid);
            intent.putExtra("tutorName",   tutorName);
            intent.putExtra("studentName", studentName);
            intent.putExtra("subject",     subject);
            startActivity(intent);
        });
    }

    // ── US 19: Rate & Review (student only, completed sessions) ──────────────

    private void setupRateReviewButton(String tutorUid, String tutorName, boolean alreadyReviewed) {
        Button btn = findViewById(R.id.btnRateReview);
        if (btn == null) return;

        btn.setVisibility(View.VISIBLE);

        if (alreadyReviewed) {
            btn.setText("⭐  Reviewed ✓");
            btn.setEnabled(false);
            btn.setAlpha(0.6f);
            return;
        }

        btn.setOnClickListener(v -> {
            Intent intent = new Intent(this, RateReviewActivity.class);
            intent.putExtra("requestId", requestId);
            intent.putExtra("tutorUid",  tutorUid);
            intent.putExtra("tutorName", tutorName);
            startActivity(intent);
        });
    }

    /**
     * Adjusts the visual badge (color and text) based on the current status of the request.
     * @param status The current state string (e.g., 'waiting', 'accepted', 'cancelled').
     */
    private void updateStatusUI(String status) {
        TextView tvStatusBadge = findViewById(R.id.tvStatusBadge);
        MaterialCardView cardStatusBadge = findViewById(R.id.cardStatusBadge);
        if (tvStatusBadge != null && cardStatusBadge != null) {
            if ("requested".equals(status)) {
                tvStatusBadge.setText("PENDING");
                tvStatusBadge.setTextColor(Color.parseColor("#007AFF"));
                cardStatusBadge.setCardBackgroundColor(Color.parseColor("#E6F2FF"));
            } else if ("booked".equals(status)) {
                tvStatusBadge.setText("BOOKED");
                tvStatusBadge.setTextColor(Color.parseColor("#34C759"));
                cardStatusBadge.setCardBackgroundColor(Color.parseColor("#EAF9EE"));
            } else if ("completed".equals(status)) {
                tvStatusBadge.setText("COMPLETED");
                tvStatusBadge.setTextColor(Color.parseColor("#AF52DE"));
                cardStatusBadge.setCardBackgroundColor(Color.parseColor("#F3EEFF"));
            } else if ("cancelled".equals(status)) {
                tvStatusBadge.setText("CANCELLED");
                tvStatusBadge.setTextColor(Color.parseColor("#FF3B30"));
                cardStatusBadge.setCardBackgroundColor(Color.parseColor("#FFECEB"));
            } else if ("expired".equals(status)) {
                tvStatusBadge.setText("EXPIRED");
                tvStatusBadge.setTextColor(Color.parseColor("#8E8E93"));
                cardStatusBadge.setCardBackgroundColor(Color.parseColor("#F2F2F7"));
            } else if (status != null) {
                tvStatusBadge.setText(status.toUpperCase());
            }
        }
    }

    /**
     * Fetches and displays any counter-offers or bids from tutors (for student view).
     */
    private void loadProviderOffers() {
        db.collection("sessionRequests").document(requestId).collection("offers")
                .addSnapshotListener((snap, e) -> {
                    if (snap != null) displayOffers(snap.getDocuments());
                });
    }

    private void displayOffers(java.util.List<DocumentSnapshot> offers) {
        if (layoutOfferContainer == null) return;
        layoutOfferContainer.removeAllViews();
        if (offers.isEmpty()) {
            findViewById(R.id.layoutProviderList).setVisibility(View.GONE);
            return;
        }
        findViewById(R.id.layoutProviderList).setVisibility(View.VISIBLE);
        LayoutInflater inflater = LayoutInflater.from(this);
        for (DocumentSnapshot doc : offers) {
            View offerView = inflater.inflate(R.layout.item_tutor_offer, layoutOfferContainer, false);
            TextView tvName = offerView.findViewById(R.id.tvTutorName);
            if (tvName != null) tvName.setText(doc.getString("tutorName"));
            layoutOfferContainer.addView(offerView);
        }
    }

    private void setupButtons() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        findViewById(R.id.btnCancelRequest).setOnClickListener(v -> {
            if (requestId != null && requestId.startsWith("mock_")) {
                updateStatusUI("cancelled");
                Toast.makeText(this, "Request Cancelled", Toast.LENGTH_SHORT).show();
            } else {
                cancelAndRefund();
            }
        });

        findViewById(R.id.btnAcceptRequest).setOnClickListener(v -> updateFirestoreStatus("booked"));
        findViewById(R.id.btnDeclineRequest).setOnClickListener(v -> cancelAndRefund());
    }

    /**
     * Updates the status field of the session request in Firestore.
     * @param status The new status value to persist.
     */
    private void updateFirestoreStatus(String status) {
        if (requestId == null || requestId.startsWith("mock_")) return;
        db.collection("sessionRequests").document(requestId)
                .update("status", status)
                .addOnSuccessListener(u -> {
                    updateStatusUI(status);
                    Toast.makeText(this,
                            "booked".equals(status) ? "Session accepted!" : "Request updated.",
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    /**
     * Cancels the request and refunds the tokens to the student who paid.
     */
    private void cancelAndRefund() {
        if (requestId == null || requestId.startsWith("mock_")) return;

        db.collection("sessionRequests").document(requestId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    String studentUid = doc.getString("studentUid");
                    Long tokensToRefund = doc.getLong("tokens");

                    db.collection("sessionRequests").document(requestId)
                            .update("status", "cancelled")
                            .addOnSuccessListener(u -> {
                                updateStatusUI("cancelled");
                                Toast.makeText(this, "Request cancelled.", Toast.LENGTH_SHORT).show();

                                if (studentUid != null && tokensToRefund != null && tokensToRefund > 0) {
                                    refundTokens(studentUid, tokensToRefund);
                                } else {
                                    finish();
                                }
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Error: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void refundTokens(String studentUid, long amount) {
        db.collection("users").document(studentUid).get()
                .addOnSuccessListener(doc -> {
                    Long current = doc.getLong("tokens");
                    long newBalance = (current != null ? current : 0) + amount;
                    db.collection("users").document(studentUid)
                            .update("tokens", newBalance)
                            .addOnSuccessListener(u -> {
                                Toast.makeText(this,
                                        amount + " tokens refunded to student.",
                                        Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e -> finish());
                })
                .addOnFailureListener(e -> finish());
    }

    private void setText(int viewId, String text) {
        TextView tv = findViewById(viewId);
        if (tv != null && text != null) tv.setText(text);
    }
}