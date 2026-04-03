package com.example.peertutoring.ui;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.peertutoring.R;
import com.example.peertutoring.models.SessionRequest;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

/**
 * US8: Displays the student's session requests dashboard.
 * Shows stats (sessions, hours, tokens), Active Requests / History tabs,
 * and a FAB to post a new request.
 */
public class SessionRequestsActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String currentUid;

    private LinearLayout layoutRequestList;
    private Button tabActive, tabHistory;
    private boolean showingActive = true;

    private List<DocumentSnapshot> activeRequests  = new ArrayList<>();
    private List<DocumentSnapshot> historyRequests = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_requests);

        db         = FirebaseFirestore.getInstance();
        currentUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

        layoutRequestList = findViewById(R.id.layoutRequestList);
        tabActive         = findViewById(R.id.tabActiveRequests);
        tabHistory        = findViewById(R.id.tabHistory);

        setupTabs();
        setupFab();
        loadRequests();
    }

    // ── Tabs ──────────────────────────────────────────────────

    private void setupTabs() {
        tabActive.setOnClickListener(v -> {
            showingActive = true;
            activateTab(tabActive, tabHistory);
            displayRequests(activeRequests);
        });

        tabHistory.setOnClickListener(v -> {
            showingActive = false;
            activateTab(tabHistory, tabActive);
            displayRequests(historyRequests);
        });
    }

    private void activateTab(Button active, Button inactive) {
        active.setBackground(getDrawable(R.drawable.bg_button_gradient));
        active.setTextColor(Color.WHITE);
        inactive.setBackground(null);
        inactive.setTextColor(Color.parseColor("#4B5D7A"));
    }

    // ── FAB ───────────────────────────────────────────────────

    private void setupFab() {
        MaterialCardView fab = findViewById(R.id.fabNewRequest);
        fab.setOnClickListener(v ->
                startActivity(new Intent(this, NewSessionRequestActivity.class)));
    }

    // ── Load from Firestore ───────────────────────────────────

    private void loadRequests() {
        if (currentUid.isEmpty()) {
            loadMockData(); // demo fallback
            return;
        }

        db.collection("sessionRequests")
                .whereEqualTo("studentUid", currentUid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    activeRequests.clear();
                    historyRequests.clear();

                    if (snap.isEmpty()) {
                        loadMockData();
                        return;
                    }

                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        String status = doc.getString("status");
                        if ("completed".equals(status) || "cancelled".equals(status)) {
                            historyRequests.add(doc);
                        } else {
                            activeRequests.add(doc);
                        }
                    }
                    displayRequests(activeRequests);
                })
                .addOnFailureListener(e -> loadMockData());
    }

    // ── Display ───────────────────────────────────────────────

    private void displayRequests(List<DocumentSnapshot> requests) {
        layoutRequestList.removeAllViews();

        if (requests.isEmpty()) {
            showEmptyState();
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        for (DocumentSnapshot doc : requests) {
            View card = inflater.inflate(R.layout.item_session_request, layoutRequestList, false);
            bindRequestCard(card, doc);
            layoutRequestList.addView(card);
        }
    }

    private void bindRequestCard(View card, DocumentSnapshot doc) {
        TextView tvTopic      = card.findViewById(R.id.tvTopic);
        TextView tvDuration   = card.findViewById(R.id.tvDuration);
        TextView tvBestRate   = card.findViewById(R.id.tvBestRate);
        TextView tvBadge      = card.findViewById(R.id.tvStatusBadge);
        MaterialCardView badge = card.findViewById(R.id.cardStatusBadge);
        LinearLayout rateBar  = card.findViewById(R.id.layoutRateBar);

        String topic    = doc.getString("topic");
        String status   = doc.getString("status");
        Long duration   = doc.getLong("durationMinutes");
        Long offerCount = doc.getLong("offerCount");
        Long bestRate   = doc.getLong("bestRate");

        if (tvTopic   != null && topic    != null) tvTopic.setText(topic);
        if (tvDuration != null && duration != null) tvDuration.setText(duration + "m");

        // Badge color and text
        if (tvBadge != null && badge != null) {
            if ("waiting".equals(status) || offerCount == null || offerCount == 0) {
                tvBadge.setText("Waiting");
                badge.setCardBackgroundColor(ColorStateList.valueOf(Color.parseColor("#FF9800")));
                if (rateBar != null) rateBar.setVisibility(View.GONE);
            } else {
                tvBadge.setText(offerCount + " Offer" + (offerCount > 1 ? "s" : ""));
                badge.setCardBackgroundColor(ColorStateList.valueOf(Color.parseColor("#00BFA5")));
                if (rateBar != null) rateBar.setVisibility(View.VISIBLE);
                if (tvBestRate != null && bestRate != null && bestRate > 0) {
                    tvBestRate.setText(String.valueOf(bestRate));
                }
            }
        }

        // Click → open offers screen
        String requestId = doc.getId();
        String goals     = doc.getString("goals");
        int    dur       = duration != null ? duration.intValue() : 60;

        card.setOnClickListener(v -> {
            Intent intent = new Intent(this, TutorOffersActivity.class);
            intent.putExtra("requestId", requestId);
            intent.putExtra("topic",     topic);
            intent.putExtra("goals",     goals);
            intent.putExtra("duration",  dur);
            startActivity(intent);
        });
    }

    private void showEmptyState() {
        TextView tv = new TextView(this);
        tv.setText(showingActive
                ? "No active requests.\nTap + to post a new session request!"
                : "No completed sessions yet.");
        tv.setTextColor(Color.parseColor("#8B97A8"));
        tv.setTextSize(15f);
        tv.setGravity(android.view.Gravity.CENTER);
        tv.setPadding(0, 80, 0, 0);
        layoutRequestList.addView(tv);
    }

    // ── Mock data for demo (no Firebase needed) ───────────────

    private void loadMockData() {
        layoutRequestList.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        String[][] mocks = {
                // topic, status, duration, offerCount, bestRate
                {"Calculus II - Integration",  "has_offers", "60", "3", "45"},
                {"Principles of Microeconomics", "has_offers", "45", "1", "45"},
                {"Python Data Structures",      "has_offers", "90", "2", "60"},
                {"Organic Chemistry",           "waiting",    "45", "0", "0"},
        };

        for (String[] m : mocks) {
            View card = inflater.inflate(R.layout.item_session_request, layoutRequestList, false);

            TextView tvTopic    = card.findViewById(R.id.tvTopic);
            TextView tvDuration = card.findViewById(R.id.tvDuration);
            TextView tvBestRate = card.findViewById(R.id.tvBestRate);
            TextView tvBadge    = card.findViewById(R.id.tvStatusBadge);
            MaterialCardView badge = card.findViewById(R.id.cardStatusBadge);
            LinearLayout rateBar   = card.findViewById(R.id.layoutRateBar);

            if (tvTopic    != null) tvTopic.setText(m[0]);
            if (tvDuration != null) tvDuration.setText(m[2] + "m");

            int offerCount = Integer.parseInt(m[3]);
            if (tvBadge != null && badge != null) {
                if (offerCount == 0) {
                    tvBadge.setText("Waiting");
                    badge.setCardBackgroundColor(
                            ColorStateList.valueOf(Color.parseColor("#FF9800")));
                    if (rateBar != null) rateBar.setVisibility(View.GONE);
                } else {
                    tvBadge.setText(offerCount + " Offer" + (offerCount > 1 ? "s" : ""));
                    badge.setCardBackgroundColor(
                            ColorStateList.valueOf(Color.parseColor("#00BFA5")));
                    if (rateBar != null) rateBar.setVisibility(View.VISIBLE);
                    if (tvBestRate != null) tvBestRate.setText(m[4]);
                }
            }

            final String topic = m[0];
            final int    dur   = Integer.parseInt(m[2]);
            card.setOnClickListener(v -> {
                Intent intent = new Intent(this, TutorOffersActivity.class);
                intent.putExtra("topic",    topic);
                intent.putExtra("goals",    "Need help with " + topic);
                intent.putExtra("duration", dur);
                startActivity(intent);
            });

            layoutRequestList.addView(card);
        }
    }
}