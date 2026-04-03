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
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/**
 * US8: Displays tutor offers for a specific session request.
 * Student can view each tutor's pitch and accept an offer.
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

        // Receive request data from Intent
        requestId   = getIntent().getStringExtra("requestId");
        String topic    = getIntent().getStringExtra("topic");
        String goals    = getIntent().getStringExtra("goals");
        int    duration = getIntent().getIntExtra("duration", 60);

        // Populate header summary
        TextView tvTopic    = findViewById(R.id.tvRequestTopic);
        TextView tvGoals    = findViewById(R.id.tvRequestGoals);
        TextView tvDuration = findViewById(R.id.tvRequestDuration);

        if (tvTopic    != null && topic    != null) tvTopic.setText(topic);
        if (tvGoals    != null && goals    != null) tvGoals.setText(goals);
        if (tvDuration != null) tvDuration.setText(duration + " minutes");

        // Back button
        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        // Load offers
        if (requestId != null && !requestId.isEmpty()) {
            loadOffersFromFirestore(requestId, topic);
        } else {
            loadMockOffers(topic);
        }
    }

    // ── Load from Firestore ───────────────────────────────────

    private void loadOffersFromFirestore(String reqId, String topic) {
        db.collection("sessionRequests")
                .document(reqId)
                .collection("offers")
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) {
                        loadMockOffers(topic); // fallback
                    } else {
                        for (DocumentSnapshot doc : snap.getDocuments()) {
                            String subjects = doc.getString("subjects");
                            // Simple filtering: if topic contains keywords, check against tutor subjects
                            if (shouldShowOffer(topic, subjects)) {
                                addOfferCard(
                                        doc.getString("tutorName"),
                                        doc.getString("tutorInitials"),
                                        doc.getString("rating"),
                                        doc.getString("reviewCount"),
                                        doc.getLong("rate") != null
                                                ? doc.getLong("rate").intValue() : 0,
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

    // ── Mock offer data for demo ──────────────────────────────

    private void loadMockOffers(String topic) {
        Object[][] allOffers = {
                {"Amjad Iqbal", "AI", "4.8", "56", 40, "Mathematics", "I have 5 years of experience teaching Mathematics to college students.", true},
                {"Kamran Saeed", "KS", "4.6", "32", 35, "Chemistry", "Chemistry is easy when you understand the logic. Let's work together!", true},
                {"Hassan Fayyaz", "HF", "4.9", "88", 45, "Mathematics, Economics", "Specialist in Advanced Math and Economics principles.", true},
                {"Abdullah Iqbal", "AI", "4.7", "41", 38, "Mathematics, Physics", "Expert in Mathematical Physics and problem solving techniques.", true},
                {"Zain ul Abideen", "ZA", "4.5", "27", 30, "Computer Science, English", "Passionate about coding and literature.", false},
                {"Ali Iqbal", "AI", "4.8", "64", 42, "Mathematics, Computer Science", "Bridging the gap between Pure Math and Algorithm design.", true},
                {"Abdullah Khaliq", "AK", "4.7", "19", 35, "Mathematics, Accounting", "Master your numbers in both Mathematics and Business Accounting.", true}
        };

        for (Object[] o : allOffers) {
            if (shouldShowOffer(topic, (String) o[5])) {
                addOfferCard(
                        (String)  o[0], (String) o[1], (String) o[2], (String) o[3],
                        (Integer) o[4], (String) o[5], (String) o[6],
                        (Boolean) o[7], "mock_" + o[0]
                );
            }
        }
    }

    // ── Build offer card ──────────────────────────────────────

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
        if (btnAccept != null) btnAccept.setOnClickListener(v -> acceptOffer(name, offerId, rate));

        layoutOfferList.addView(card);
    }

    private void acceptOffer(String tutorName, String offerId, int rate) {
        if (currentUid.isEmpty()) {
            Toast.makeText(this, "Please sign in", Toast.LENGTH_SHORT).show();
            return;
        }

        // Token subtraction logic
        db.collection("users").document(currentUid).get().addOnSuccessListener(doc -> {
            Long currentTokens = doc.getLong("tokens");
            if (currentTokens == null) currentTokens = 1000L; // default fallback

            if (currentTokens < rate) {
                Toast.makeText(this, "❌ Insufficient tokens! You need " + rate + " tokens.", Toast.LENGTH_LONG).show();
                return;
            }

            final long newBalance = currentTokens - rate;
            
            // 1. Update User Tokens
            db.collection("users").document(currentUid).update("tokens", newBalance);
            
            // 2. Update Request Status
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

    private void setText(View parent, int viewId, String text) {
        TextView tv = parent.findViewById(viewId);
        if (tv != null && text != null) tv.setText(text);
    }
}