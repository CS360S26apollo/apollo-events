package com.example.peertutoring.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import com.example.peertutoring.R;
import com.example.peertutoring.utils.CancellationUtils;
import com.example.peertutoring.utils.ConflictChecker;
import com.example.peertutoring.utils.EscrowManager;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Displays full details for a session request.
 * US-14: Tutor accept checks for scheduling conflicts before booking.
 * US-15: Student cancel uses penalty-aware refund rules; reschedule button
 *         shown for booked sessions.
 */
public class RequestDetailActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String  requestId;
    private String  currentUid;
    private LinearLayout layoutOfferContainer;

    // Populated by the real-time snapshot listener for use in button handlers.
    private String sessionStudentUid;
    private String sessionTutorUid;
    private String sessionStudentName;
    private String sessionSubject;
    private Date   sessionScheduledDate;
    private int    sessionTokens;
    private int    sessionDuration;
    private String sessionStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_detail);

        db         = FirebaseFirestore.getInstance();
        currentUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

        requestId = getIntent().getStringExtra("requestId");

        // Pre-populate from intent while Firestore loads
        populateFromIntent();

        layoutOfferContainer = findViewById(R.id.layoutOfferContainer);

        setupStaticButtons();

        if (requestId != null && !requestId.startsWith("mock_")) {
            loadRequestData();
        }
    }

    // ── Initial display from intent ───────────────────────────────��──────────

    private void populateFromIntent() {
        String topic    = getIntent().getStringExtra("topic");
        String status   = getIntent().getStringExtra("status");
        String provider = getIntent().getStringExtra("provider");
        String category = getIntent().getStringExtra("category");
        // "subject" key used by TutorRequestsActivity; fall back to "category"
        if (category == null) category = getIntent().getStringExtra("subject");
        int duration    = getIntent().getIntExtra("duration", 60);
        int tokens      = getIntent().getIntExtra("tokens", 0);

        setText(R.id.tvTopic,        topic    != null ? topic    : "Request Detail");
        setText(R.id.tvProviderName, provider != null ? provider : "Tutor Pending");
        setText(R.id.tvCategory,     category != null ? category : "Education");
        setText(R.id.tvTokens,       String.valueOf(tokens));
        setText(R.id.tvDuration,     duration + " min");
        updateStatusUI(status);
    }

    // ── Back + static wiring (dynamic buttons wired after snapshot loads) ────

    private void setupStaticButtons() {
        View back = findViewById(R.id.btnBack);
        if (back != null) back.setOnClickListener(v -> finish());

        // Cancel — logic depends on snapshot data loaded later
        Button btnCancel = findViewById(R.id.btnCancelRequest);
        if (btnCancel != null)
            btnCancel.setOnClickListener(v -> onCancelClicked());

        // Reschedule — launched after snapshot confirms booked + student
        Button btnReschedule = findViewById(R.id.btnReschedule);
        if (btnReschedule != null)
            btnReschedule.setOnClickListener(v -> launchReschedule());

        // Tutor actions
        Button btnAccept = findViewById(R.id.btnAcceptRequest);
        if (btnAccept != null)
            btnAccept.setOnClickListener(v -> acceptWithConflictCheck());

        Button btnCounter = findViewById(R.id.btnCounterOffer);
        if (btnCounter != null)
            btnCounter.setOnClickListener(v -> launchCounterOffer());

        Button btnDecline = findViewById(R.id.btnDeclineRequest);
        if (btnDecline != null)
            btnDecline.setOnClickListener(v -> declineAndRefund());

        Button btnMarkComplete = findViewById(R.id.btnMarkComplete);
        if (btnMarkComplete != null)
            btnMarkComplete.setOnClickListener(v -> onMarkCompleteClicked());
    }

    // ── Real-time snapshot ─────────────────────────────────��─────────────────

    private void loadRequestData() {
        db.collection("sessionRequests").document(requestId)
                .addSnapshotListener((doc, e) -> {
                    if (e != null || doc == null || !doc.exists()) return;

                    // Store for button handlers
                    sessionStudentUid   = doc.getString("studentUid");
                    sessionTutorUid     = doc.getString("tutorUid");
                    sessionStudentName  = doc.getString("studentName");
                    sessionSubject      = doc.getString("subject");
                    sessionScheduledDate = doc.getDate("scheduledDate");
                    Long tok = doc.getLong("tokens");
                    sessionTokens   = tok  != null ? tok.intValue()  : 0;
                    Long dur = doc.getLong("durationMinutes");
                    sessionDuration = dur  != null ? dur.intValue()  : 60;
                    sessionStatus   = doc.getString("status");

                    // Update displayed provider name and status badge
                    String tutorName = doc.getString("tutorName");
                    setText(R.id.tvProviderName, tutorName != null ? tutorName : "Searching...");
                    updateStatusUI(sessionStatus);

                    // Show scheduled time in the location row if set
                    if (sessionScheduledDate != null) {
                        SimpleDateFormat sdf =
                                new SimpleDateFormat("MMM dd, yyyy  HH:mm", Locale.getDefault());
                        setText(R.id.tvLocation, " " + sdf.format(sessionScheduledDate));
                    }

                    boolean isStudent = currentUid.equals(sessionStudentUid);
                    boolean isTutor   = currentUid.equals(sessionTutorUid);

                    updateButtonVisibility(isStudent, isTutor);

                    // Post-completion actions
                    if ("completed".equals(sessionStatus)) {
                        if (isTutor) {
                            boolean notesAdded = Boolean.TRUE.equals(doc.getBoolean("notesAdded"));
                            setupAddNotesButton(sessionStudentUid, sessionTutorUid,
                                    doc.getString("tutorName"), sessionStudentName,
                                    sessionSubject, notesAdded);
                        } else if (isStudent) {
                            boolean reviewed = Boolean.TRUE.equals(doc.getBoolean("reviewSubmitted"));
                            setupRateReviewButton(sessionTutorUid, doc.getString("tutorName"), reviewed);
                        }
                    }

                    // Student: load counter-offers
                    if (isStudent) loadProviderOffers();
                });
    }

    private void updateButtonVisibility(boolean isStudent, boolean isTutor) {
        Button btnCancel    = findViewById(R.id.btnCancelRequest);
        Button btnReschedule = findViewById(R.id.btnReschedule);
        View   tutorActions = findViewById(R.id.layoutTutorActions);

        boolean canCancel = isStudent &&
                ("requested".equals(sessionStatus) || "booked".equals(sessionStatus));

        if (btnCancel != null) {
            btnCancel.setVisibility(canCancel ? View.VISIBLE : View.GONE);
            if (canCancel) {
                int refund = CancellationUtils.calculateRefund(sessionScheduledDate, sessionTokens);
                btnCancel.setText("✕  Cancel  (" + refund + " tokens back)");
            }
        }

        boolean canReschedule = isStudent && "booked".equals(sessionStatus)
                && sessionScheduledDate != null;
        if (btnReschedule != null)
            btnReschedule.setVisibility(canReschedule ? View.VISIBLE : View.GONE);

        boolean showTutorActions = isTutor && "requested".equals(sessionStatus);
        if (tutorActions != null)
            tutorActions.setVisibility(showTutorActions ? View.VISIBLE : View.GONE);

        // Always hide counter-offer section for tutors
        if (isTutor) {
            View pl = findViewById(R.id.layoutProviderList);
            if (pl != null) pl.setVisibility(View.GONE);
            if (layoutOfferContainer != null) layoutOfferContainer.removeAllViews();
        }

        // US 24: tutor can mark complete on booked sessions to release escrow
        Button btnMarkComplete = findViewById(R.id.btnMarkComplete);
        boolean showMarkComplete = isTutor && "booked".equals(sessionStatus);
        if (btnMarkComplete != null)
            btnMarkComplete.setVisibility(showMarkComplete ? View.VISIBLE : View.GONE);
    }

    // ── Cancel (US-15) ────────────────────────────────��──────────────────────

    private void onCancelClicked() {
        if (requestId == null || requestId.startsWith("mock_")) {
            updateStatusUI("cancelled");
            Toast.makeText(this, "Request cancelled", Toast.LENGTH_SHORT).show();
            return;
        }
        // Show confirmation dialog with refund amount
        int refund = CancellationUtils.calculateRefund(sessionScheduledDate, sessionTokens);
        String msg = CancellationUtils.refundDescription(sessionScheduledDate, sessionTokens);
        new AlertDialog.Builder(this)
                .setTitle("Cancel Session?")
                .setMessage(msg)
                .setPositiveButton("Yes, Cancel", (d, which) -> performCancel(refund))
                .setNegativeButton("Keep Session", null)
                .show();
    }

    private void performCancel(int tokensToRefund) {
        db.collection("sessionRequests").document(requestId)
                .update("status", "cancelled")
                .addOnSuccessListener(u -> {
                    updateStatusUI("cancelled");
                    if (sessionStudentUid != null && tokensToRefund > 0) {
                        EscrowManager.refundToStudent(db, requestId, sessionStudentUid,
                                tokensToRefund, () -> {
                                    Toast.makeText(this,
                                            "Session cancelled. " + tokensToRefund + " tokens refunded.",
                                            Toast.LENGTH_SHORT).show();
                                    finish();
                                });
                    } else {
                        Toast.makeText(this, "Session cancelled.", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // ── Reschedule (US-15) ──────────────────��────────────────────────────────

    private void launchReschedule() {
        Intent intent = new Intent(this, RescheduleActivity.class);
        intent.putExtra("requestId",       requestId);
        intent.putExtra("tutorUid",        sessionTutorUid);
        intent.putExtra("tutorName",       getIntent().getStringExtra("provider"));
        intent.putExtra("studentUid",      sessionStudentUid);
        intent.putExtra("durationMinutes", sessionDuration);
        if (sessionScheduledDate != null)
            intent.putExtra("currentDateMs", sessionScheduledDate.getTime());
        startActivity(intent);
    }

    // ── Tutor: Accept with conflict check (US-14) ────────────────────────────

    private void acceptWithConflictCheck() {
        if (requestId == null || requestId.startsWith("mock_")) {
            updateFirestoreStatus("booked");
            return;
        }
        if (sessionScheduledDate == null) {
            // No proposed time — accept immediately
            updateFirestoreStatus("booked");
            return;
        }

        Button btnAccept = findViewById(R.id.btnAcceptRequest);
        if (btnAccept != null) { btnAccept.setEnabled(false); btnAccept.setText("Checking..."); }

        ConflictChecker.checkConflict(db,
                sessionTutorUid != null ? sessionTutorUid : currentUid,
                sessionStudentUid != null ? sessionStudentUid : "",
                sessionScheduledDate, sessionDuration, requestId,
                (hasConflict, reason) -> {
                    if (hasConflict) {
                        Toast.makeText(this,
                                "Cannot accept: " + reason + "\nUse Counter Offer to propose a new time.",
                                Toast.LENGTH_LONG).show();
                        if (btnAccept != null) { btnAccept.setEnabled(true); btnAccept.setText("Accept"); }
                        return;
                    }
                    updateFirestoreStatus("booked");
                    if (btnAccept != null) { btnAccept.setEnabled(true); btnAccept.setText("Accept"); }
                });
    }

    // ── Tutor: Counter Offer ────────────────────────────��────────────────────

    private void launchCounterOffer() {
        Intent intent = new Intent(this, CounterOfferActivity.class);
        intent.putExtra("requestId",   requestId);
        intent.putExtra("studentName", sessionStudentName);
        intent.putExtra("subject",     sessionSubject);
        intent.putExtra("studentUid",  sessionStudentUid);
        intent.putExtra("duration",    sessionDuration);
        intent.putExtra("tokens",      sessionTokens);
        startActivity(intent);
    }

    // ── Tutor: Decline (always full refund) ────────────────────────────���─────

    private void declineAndRefund() {
        if (requestId == null || requestId.startsWith("mock_")) return;

        db.collection("sessionRequests").document(requestId)
                .update("status", "cancelled")
                .addOnSuccessListener(u -> {
                    updateStatusUI("cancelled");
                    if (sessionStudentUid != null && sessionTokens > 0) {
                        EscrowManager.refundToStudent(db, requestId, sessionStudentUid,
                                sessionTokens, () -> {
                                    Toast.makeText(this,
                                            "Request declined. " + sessionTokens + " tokens refunded.",
                                            Toast.LENGTH_SHORT).show();
                                    finish();
                                });
                    } else {
                        Toast.makeText(this, "Request declined.", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // ── Mark Complete (US 24) ─────────────────────────────────────────────────

    private void onMarkCompleteClicked() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Complete Session?")
                .setMessage("Mark this session as complete? The escrowed tokens will be released to your wallet.")
                .setPositiveButton("Complete", (d, w) -> performMarkComplete())
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Sets session status to "completed", then releases the escrowed tokens to the
     * tutor via EscrowManager (HELD → RELEASED).
     */
    private void performMarkComplete() {
        if (requestId == null) return;
        Button btn = findViewById(R.id.btnMarkComplete);
        if (btn != null) { btn.setEnabled(false); btn.setText("Processing..."); }

        db.collection("sessionRequests").document(requestId)
                .update("status", "completed")
                .addOnSuccessListener(u -> {
                    updateStatusUI("completed");
                    if (sessionTutorUid != null && !sessionTutorUid.isEmpty()) {
                        EscrowManager.releaseToTutor(db, requestId, sessionTutorUid, () ->
                                Toast.makeText(this,
                                        "Session complete! Tokens added to your wallet.",
                                        Toast.LENGTH_LONG).show());
                    }
                })
                .addOnFailureListener(e -> {
                    if (btn != null) { btn.setEnabled(true); btn.setText("✓  Mark as Complete"); }
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // ── Post-completion: US-17 & US-19 ───────────────────────────────────────

    private void setupAddNotesButton(String studentUid, String tutorUid,
                                     String tutorName, String studentName,
                                     String subject, boolean alreadySent) {
        Button btn = findViewById(R.id.btnAddNotes);
        if (btn == null) return;
        btn.setVisibility(View.VISIBLE);
        if (alreadySent) {
            btn.setText("Notes Sent");
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

    private void setupRateReviewButton(String tutorUid, String tutorName, boolean alreadyReviewed) {
        Button btn = findViewById(R.id.btnRateReview);
        if (btn == null) return;
        btn.setVisibility(View.VISIBLE);
        if (alreadyReviewed) {
            btn.setText("Reviewed");
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

    // ── Offers (counter-offers shown to student) ────────────────────────��────

    private void loadProviderOffers() {
        if (requestId == null || requestId.startsWith("mock_")) return;

        // Ensure offer section is completely hidden for tutors
        View providerList = findViewById(R.id.layoutProviderList);
        if (providerList != null) providerList.setVisibility(android.view.View.GONE);
        if (layoutOfferContainer != null) layoutOfferContainer.removeAllViews();

        // Only students see counter offers
        if (!currentUid.equals(sessionStudentUid)) return;

        db.collection("sessionRequests").document(requestId)
                .collection("counterOffers")
                .addSnapshotListener((snap, e) -> {
                    if (snap == null) return;
                    // Only show pending offers — accepted ones disappear
                    java.util.List<com.google.firebase.firestore.DocumentSnapshot> pending =
                            new java.util.ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snap.getDocuments()) {
                        if ("pending".equals(doc.getString("status"))) {
                            pending.add(doc);
                        }
                    }
                    displayOffers(pending);
                });
    }

    private void displayOffers(java.util.List<com.google.firebase.firestore.DocumentSnapshot> offers) {
        if (layoutOfferContainer == null) return;
        layoutOfferContainer.removeAllViews();
        View providerList = findViewById(R.id.layoutProviderList);

        if (offers.isEmpty()) {
            if (providerList != null) providerList.setVisibility(View.GONE);
            return;
        }
        if (providerList != null) providerList.setVisibility(View.VISIBLE);

        for (com.google.firebase.firestore.DocumentSnapshot doc : offers) {
            String offerId        = doc.getId();
            String proposedDate   = doc.getString("proposedDate");
            String proposedTime   = doc.getString("proposedTime");
            Long   proposedDur    = doc.getLong("proposedDuration");
            Long   proposedTokens = doc.getLong("proposedTokens");
            String message        = doc.getString("message");

            // Build card
            com.google.android.material.card.MaterialCardView card =
                    new com.google.android.material.card.MaterialCardView(this);
            android.widget.LinearLayout.LayoutParams cp =
                    new android.widget.LinearLayout.LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            cp.setMargins(0, 0, 0, dp(12));
            card.setLayoutParams(cp);
            card.setRadius(dp(16));
            card.setCardBackgroundColor(0xFFF9F5FF);
            card.setStrokeColor(0xFF8A2EFF);
            card.setStrokeWidth(dp(1));
            card.setCardElevation(dp(2));

            android.widget.LinearLayout inner = new android.widget.LinearLayout(this);
            inner.setOrientation(android.widget.LinearLayout.VERTICAL);
            inner.setPadding(dp(16), dp(14), dp(16), dp(14));

            // Header
            TextView tvHeader = new TextView(this);
            tvHeader.setText("Counter Offer from Tutor");
            tvHeader.setTextColor(0xFF8A2EFF);
            tvHeader.setTextSize(14f);
            tvHeader.setTypeface(null, android.graphics.Typeface.BOLD);
            inner.addView(tvHeader);

            // Details
            StringBuilder sb = new StringBuilder();
            if (proposedDate   != null) sb.append("Date: ").append(proposedDate);
            if (proposedTime   != null) sb.append("  ").append(proposedTime);
            if (proposedDur    != null) sb.append("Duration: ").append(proposedDur).append(" min");
            if (proposedTokens != null) sb.append("Tokens: ").append(proposedTokens);
            if (message != null && !message.isEmpty()) sb.append("Message: ").append(message);

            TextView tvDetails = new TextView(this);
            tvDetails.setText(sb.toString().trim());
            tvDetails.setTextColor(0xFF4B5D7A);
            tvDetails.setTextSize(13f);
            android.widget.LinearLayout.LayoutParams dlp =
                    new android.widget.LinearLayout.LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            dlp.setMargins(0, dp(8), 0, dp(12));
            tvDetails.setLayoutParams(dlp);
            inner.addView(tvDetails);

            // Accept button
            android.widget.Button btnAccept = new android.widget.Button(this);
            btnAccept.setText("Accept This Offer");
            btnAccept.setAllCaps(false);
            btnAccept.setTextColor(android.graphics.Color.WHITE);
            btnAccept.setTextSize(14f);
            try {
                btnAccept.setBackground(
                        androidx.core.content.ContextCompat.getDrawable(
                                this, R.drawable.bg_button_gradient));
            } catch (Exception ignored) {}
            android.widget.LinearLayout.LayoutParams blp =
                    new android.widget.LinearLayout.LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT, dp(48));
            btnAccept.setLayoutParams(blp);

            final int    fTokens = proposedTokens != null ? proposedTokens.intValue() : sessionTokens;
            final int    fDur    = proposedDur    != null ? proposedDur.intValue()    : sessionDuration;
            final String fDate   = proposedDate;
            final String fTime   = proposedTime;
            btnAccept.setOnClickListener(v ->
                    acceptCounterOffer(offerId, fDate, fTime, fDur, fTokens, btnAccept));

            inner.addView(btnAccept);
            card.addView(inner);
            layoutOfferContainer.addView(card);
        }
    }

    /**
     * Student accepts a counter offer:
     * 1. Marks offer doc status → "accepted" (pending filter removes it from view instantly)
     * 2. Updates session status → "booked" with proposed schedule
     *    (session automatically moves to booked list in SessionRequestsActivity)
     */
    private void acceptCounterOffer(String offerId, String proposedDate, String proposedTime,
                                    int durationMinutes, int tokens, android.widget.Button btn) {
        if (requestId == null) return;
        if (btn != null) { btn.setEnabled(false); btn.setText("Accepting..."); }

        // Step 1: mark this specific offer as accepted → disappears from pending filter
        db.collection("sessionRequests").document(requestId)
                .collection("counterOffers").document(offerId)
                .update("status", "accepted")
                .addOnSuccessListener(u -> {
                    // Step 2: update session to BOOKED with proposed details
                    java.util.Map<String, Object> updates = new java.util.HashMap<>();
                    updates.put("status",          "booked");
                    updates.put("durationMinutes", durationMinutes);
                    updates.put("tokens",          tokens);
                    if (proposedDate != null) updates.put("sessionDate", proposedDate);
                    if (proposedTime != null) updates.put("sessionTime", proposedTime);

                    db.collection("sessionRequests").document(requestId)
                            .update(updates)
                            .addOnSuccessListener(u2 -> {
                                Toast.makeText(this,
                                        "Offer accepted! Session is now booked.",
                                        Toast.LENGTH_LONG).show();
                                finish(); // return to session list — it will show as Booked
                            })
                            .addOnFailureListener(e2 -> {
                                Toast.makeText(this, "Error: " + e2.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                                if (btn != null) {
                                    btn.setEnabled(true);
                                    btn.setText("Accept This Offer");
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    if (btn != null) {
                        btn.setEnabled(true);
                        btn.setText("Accept This Offer");
                    }
                });
    }

    private int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    // ── Shared helpers ───────────────────────────────────────────────────────

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

    private void updateStatusUI(String status) {
        TextView tvStatusBadge = findViewById(R.id.tvStatusBadge);
        MaterialCardView cardStatusBadge = findViewById(R.id.cardStatusBadge);
        if (tvStatusBadge == null || cardStatusBadge == null) return;
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

    private void setText(int viewId, String text) {
        TextView tv = findViewById(viewId);
        if (tv != null && text != null) tv.setText(text);
    }
}