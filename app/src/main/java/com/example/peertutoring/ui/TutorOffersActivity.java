package com.example.peertutoring.ui;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.peertutoring.R;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Activity for students to view competing offers from multiple tutors for their session request.
 * Role: Selection View for User Story 08 (Request a Session) and User Story 05 (Tutor Recommendations).
 *
 * Purpose: Allows students to compare tutor profiles, ratings, and personalized messages
 * before committing tokens to accept a specific offer. Includes token balance validation.
 *
 * Design Pattern: View-Controller with conditional mock data injection.
 *
 * Outstanding Issues:
 * - Token deduction logic is client-side; should be moved to a Cloud Function for security.
 */
public class TutorOffersActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private LinearLayout layoutOfferList;
    private String requestId;
    private String currentUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutor_offers);

        db             = FirebaseFirestore.getInstance();
        currentUid     = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
        layoutOfferList = findViewById(R.id.layoutOfferList);

        requestId   = getIntent().getStringExtra("requestId");
        String topic    = getIntent().getStringExtra("topic");
        String goals    = getIntent().getStringExtra("goals");
        int    duration = getIntent().getIntExtra("duration", 60);

        TextView tvTopic    = findViewById(R.id.tvRequestTopic);
        TextView tvGoals    = findViewById(R.id.tvRequestGoals);
        TextView tvDuration = findViewById(R.id.tvRequestDuration);

        if (tvTopic    != null && topic    != null) tvTopic.setText(topic);
        if (tvGoals    != null && goals    != null) tvGoals.setText(goals);
        if (tvDuration != null) tvDuration.setText(duration + " minutes");

        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        if (requestId != null && !requestId.isEmpty()) {
            loadOffersFromFirestore(requestId, topic);
        } else {
            loadMockOffers(topic);
        }
    }

    /**
     * Fetches offer sub-collection from the specific session request document.
     * Implementation details for US 08.
     */
    private void loadOffersFromFirestore(String reqId, String topic) {
        db.collection("sessionRequests")
                .document(reqId)
                .collection("counterOffers")
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) {
                        loadMockOffers(topic);
                    } else {
                        for (DocumentSnapshot doc : snap.getDocuments()) {
                            String subjects = doc.getString("subjects");
                            if (shouldShowOffer(topic, subjects)) {
                                Long rateLong = doc.getLong("rate");
                                int rate = (rateLong != null) ? rateLong.intValue() : 0;

                                addOfferCard(
                                        doc.getString("tutorName"),
                                        doc.getString("tutorInitials"),
                                        doc.getString("rating"),
                                        doc.getString("reviewCount"),
                                        rate,
                                        subjects,
                                        doc.getString("message"),
                                        Boolean.TRUE.equals(doc.getBoolean("verified")),
                                        doc.getId()
                                );
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> loadMockOffers(topic));
    }

    /**
     * Basic logic to filter tutor offers based on keyword relevance to the requested topic.
     * @param topic Student's requested topic.
     * @param subjects Tutor's expertise subjects.
     * @return True if the offer is relevant.
     */
    private boolean shouldShowOffer(String topic, String subjects) {
        if (topic == null || subjects == null) return true;
        String t = topic.toLowerCase();
        String s = subjects.toLowerCase();

        if (t.contains("math") || t.contains("calculus") || t.contains("integration")) {
            return s.contains("math") || s.contains("calculus");
        }
        if (t.contains("chem")) {
            return s.contains("chem");
        }
        if (t.contains("computer") || t.contains("python") || t.contains("coding")) {
            return s.contains("computer") || s.contains("science") || s.contains("english");
        }
        if (t.contains("economics") || t.contains("microeconomics")) {
            return s.contains("economics");
        }
        return true;
    }

    /** No real counter offers found — show empty state. Never show fake data. */
    private void loadMockOffers(String topic) {
        if (layoutOfferList == null) return;
        layoutOfferList.removeAllViews();
        android.widget.TextView tv = new android.widget.TextView(this);
        tv.setText("No counter offers yet.\nTutors will appear here once they send you a counter offer.");
        tv.setTextColor(android.graphics.Color.parseColor("#8B97A8"));
        tv.setTextSize(15f);
        tv.setGravity(android.view.Gravity.CENTER);
        tv.setPadding(0, 80, 0, 0);
        tv.setLineSpacing(8f, 1f);
        layoutOfferList.addView(tv);
    }

    /**
     * Programmatically creates and adds an offer card to the UI.
     * Implementation details for US 05/08 comparisons.
     */
    private void addOfferCard(String name, String initials, String rating,
                              String reviewCount, int rate, String subjectsStr,
                              String message, boolean verified, String offerId) {

        View card = LayoutInflater.from(this)
                .inflate(R.layout.item_tutor_offer, layoutOfferList, false);

        TextView tvInitials = card.findViewById(R.id.tvTutorInitials);
        if (tvInitials != null && initials != null) tvInitials.setText(initials);

        MaterialCardView avatarCard = card.findViewById(R.id.cardAvatar);
        int[] avatarColors = {Color.parseColor("#7C3AED"), Color.parseColor("#0062FF"), Color.parseColor("#00BFA5"), Color.parseColor("#E53935")};
        if (avatarCard != null && initials != null && !initials.isEmpty()) {
            avatarCard.setCardBackgroundColor(avatarColors[Math.abs(initials.hashCode()) % avatarColors.length]);
        }

        MaterialCardView badge = card.findViewById(R.id.badgeVerified);
        if (badge != null) badge.setVisibility(verified ? View.VISIBLE : View.GONE);

        setText(card, R.id.tvTutorName,    name);
        setText(card, R.id.tvRating,       rating);
        setText(card, R.id.tvReviewCount,  reviewCount != null ? " (" + reviewCount + ")" : "");
        setText(card, R.id.tvRate,         String.valueOf(rate));
        setText(card, R.id.tvTutorMessage, message);

        LinearLayout tagLayout = card.findViewById(R.id.layoutSubjectTags);
        if (tagLayout != null && subjectsStr != null) {
            for (String s : subjectsStr.split(",")) {
                String sub = s.trim();
                if (sub.isEmpty()) continue;
                MaterialCardView tag = new MaterialCardView(this);
                tag.setCardBackgroundColor(ColorStateList.valueOf(Color.parseColor("#EEF2FF")));
                tag.setRadius(20f * getResources().getDisplayMetrics().density);
                tag.setCardElevation(0);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                lp.setMarginEnd((int)(8 * getResources().getDisplayMetrics().density));
                tag.setLayoutParams(lp);
                TextView tv = new TextView(this);
                int pad = (int)(8 * getResources().getDisplayMetrics().density), vpad = (int)(4 * getResources().getDisplayMetrics().density);
                tv.setPadding(pad, vpad, pad, vpad); tv.setText(sub); tv.setTextColor(Color.parseColor("#4B5D7A")); tv.setTextSize(12f);
                tag.addView(tv); tagLayout.addView(tag);
            }
        }

        View btnAccept = card.findViewById(R.id.btnAcceptOffer);
        if (btnAccept != null) btnAccept.setOnClickListener(v -> acceptOffer(offerId, rate));

        layoutOfferList.addView(card);
    }

    /**
     * Finalizes the session by deducting tokens from the student and marking the request accepted.
     * Implementation of US 08 transaction logic.
     * @param offerId The ID of the offer to accept.
     * @param rate The token rate to deduct.
     */
    private void acceptOffer(String offerId, int rate) {
        if (currentUid.isEmpty()) {
            Toast.makeText(this, "Please sign in", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users").document(currentUid).get().addOnSuccessListener(doc -> {
            Long currentTokens = doc.getLong("tokens");
            if (currentTokens == null) currentTokens = 1000L;

            if (currentTokens < rate) {
                Toast.makeText(this, "❌ Insufficient tokens! You need " + rate + " tokens.", Toast.LENGTH_LONG).show();
                return;
            }

            final long newBalance = currentTokens - rate;
            db.collection("users").document(currentUid).update("tokens", newBalance);

            if (requestId != null && !requestId.isEmpty()) {
                db.collection("sessionRequests").document(requestId)
                        .update("status", "accepted", "acceptedOfferId", offerId)
                        .addOnSuccessListener(unused -> {
                            Toast.makeText(this, "✅ Offer accepted! " + rate + " tokens deducted.", Toast.LENGTH_LONG).show();
                            finish();
                        });
            } else {
                Toast.makeText(this, "✅ Offer accepted! (Demo Mode)", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    /**
     * Helper to set text to a TextView within a parent view.
     * @param parent The container view.
     * @param viewId The resource ID of the TextView.
     * @param text The text to set.
     */
    private void setText(View parent, int viewId, String text) {
        TextView tv = parent.findViewById(viewId);
        if (tv != null && text != null) tv.setText(text);
    }
}