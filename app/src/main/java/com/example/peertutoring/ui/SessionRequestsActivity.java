package com.example.peertutoring.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.peertutoring.R;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class SessionRequestsActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String currentUid;
    private ListenerRegistration requestsListener;

    private LinearLayout layoutRequestList;
    private TextView tvCountAll, tvCountPending, tvCountCounter, tvCountAccepted, tvCountDeclined, tvCountExpired;
    private List<MockDoc> allRequests = new ArrayList<>();
    private String currentFilter = "all";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_requests);

        db = FirebaseFirestore.getInstance();
        currentUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

        layoutRequestList = findViewById(R.id.layoutRequestList);
        tvCountAll = findViewById(R.id.tvCountAll);
        tvCountPending = findViewById(R.id.tvCountPending);
        tvCountCounter = findViewById(R.id.tvCountCounter);
        tvCountAccepted = findViewById(R.id.tvCountAccepted);
        tvCountDeclined = findViewById(R.id.tvCountDeclined);
        tvCountExpired = findViewById(R.id.tvCountExpired);

        setupFilterChips();
        setupBottomNav();
        startRealTimeListener();
    }

    private void setupBottomNav() {
        View navHome = findViewById(R.id.navHome);
        if (navHome != null) navHome.setOnClickListener(v -> {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
        });
        View navBrowse = findViewById(R.id.navBrowse);
        if (navBrowse != null) navBrowse.setOnClickListener(v -> {
            startActivity(new Intent(this, BrowseTutorsActivity.class));
            finish();
        });
        View navProfile = findViewById(R.id.navProfile);
        if (navProfile != null) navProfile.setOnClickListener(v -> {
            startActivity(new Intent(this, EditProfileActivity.class));
        });
    }

    private void setupFilterChips() {
        findViewById(R.id.chipAll).setOnClickListener(v -> applyFilter("all"));
        findViewById(R.id.chipPending).setOnClickListener(v -> applyFilter("waiting"));
        findViewById(R.id.chipCounter).setOnClickListener(v -> applyFilter("counter"));
        findViewById(R.id.chipAccepted).setOnClickListener(v -> applyFilter("accepted"));
        findViewById(R.id.chipDeclined).setOnClickListener(v -> applyFilter("declined"));
        findViewById(R.id.chipExpired).setOnClickListener(v -> applyFilter("expired"));
    }

    private void applyFilter(String filter) {
        currentFilter = filter;
        updateListUI();
        updateTabStyles(filter);
    }

    private void startRealTimeListener() {
        if (currentUid.isEmpty()) { loadMockData(); return; }

        requestsListener = db.collection("sessionRequests")
                .whereEqualTo("studentUid", currentUid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (snap != null && !snap.isEmpty()) {
                        allRequests.clear();
                        for (DocumentSnapshot d : snap.getDocuments()) {
                            allRequests.add(new MockDoc(d));
                        }
                        updateCounts();
                        updateListUI();
                    } else {
                        loadMockData();
                    }
                });
    }

    private void loadMockData() {
        allRequests.clear();
        allRequests.add(new MockDoc("Math Tutoring Session", "waiting", 120, 250, "Sarah Chen", "Education"));
        allRequests.add(new MockDoc("Essay Proofreading", "counter", 60, 150, "Dr. James Wilson", "Writing"));
        allRequests.add(new MockDoc("Physics Lab Prep", "accepted", 90, 200, "Emily Chen", "Science"));
        allRequests.add(new MockDoc("History Review", "declined", 60, 100, "Marcus Smith", "History"));
        allRequests.add(new MockDoc("Calculus Exam", "expired", 120, 300, "Zain ul Abideen", "Education"));
        
        updateCounts();
        updateListUI();
    }

    private void updateCounts() {
        int pending = 0, counter = 0, accepted = 0, declined = 0, expired = 0;
        for (MockDoc doc : allRequests) {
            if ("waiting".equals(doc.status)) pending++;
            else if ("counter".equals(doc.status)) counter++;
            else if ("accepted".equals(doc.status)) accepted++;
            else if ("declined".equals(doc.status)) declined++;
            else if ("expired".equals(doc.status)) expired++;
        }
        if (tvCountAll != null) tvCountAll.setText(" " + allRequests.size());
        if (tvCountPending != null) tvCountPending.setText(" " + pending);
        if (tvCountCounter != null) tvCountCounter.setText(" " + counter);
        if (tvCountAccepted != null) tvCountAccepted.setText(" " + accepted);
        if (tvCountDeclined != null) tvCountDeclined.setText(" " + declined);
        if (tvCountExpired != null) tvCountExpired.setText(" " + expired);
    }

    private void updateListUI() {
        layoutRequestList.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (MockDoc doc : allRequests) {
            if (!"all".equals(currentFilter) && !currentFilter.equals(doc.status)) continue;

            View card = inflater.inflate(R.layout.item_session_request, layoutRequestList, false);
            bindCard(card, doc);
            layoutRequestList.addView(card);
        }
    }

    private void bindCard(View card, MockDoc doc) {
        TextView tvTopic = card.findViewById(R.id.tvTopic);
        TextView tvStatusBadge = card.findViewById(R.id.tvStatusBadge);
        MaterialCardView cardStatusBadge = card.findViewById(R.id.cardStatusBadge);
        TextView tvDuration = card.findViewById(R.id.tvDuration);
        TextView tvTokens = card.findViewById(R.id.tvTokens);
        TextView tvProvider = card.findViewById(R.id.tvProviderName);
        TextView tvCategory = card.findViewById(R.id.tvCategory);
        TextView tvExpired = card.findViewById(R.id.tvExpired);

        if (tvTopic != null) tvTopic.setText(doc.topic);
        if (tvCategory != null) tvCategory.setText(doc.category);
        if (tvDuration != null) tvDuration.setText((doc.duration / 60) + " hours");
        if (tvTokens != null) tvTokens.setText(String.valueOf(doc.tokens));
        if (tvProvider != null) tvProvider.setText(doc.provider);

        if (tvStatusBadge != null && cardStatusBadge != null) {
            String displayStatus = doc.status.toUpperCase();
            if ("waiting".equals(doc.status)) displayStatus = "PENDING";
            tvStatusBadge.setText(displayStatus);
            
            if ("waiting".equals(doc.status)) {
                tvStatusBadge.setTextColor(Color.parseColor("#007AFF"));
                cardStatusBadge.setCardBackgroundColor(Color.parseColor("#E6F2FF"));
            } else if ("counter".equals(doc.status)) {
                tvStatusBadge.setTextColor(Color.parseColor("#AF52DE"));
                cardStatusBadge.setCardBackgroundColor(Color.parseColor("#F3EEFF"));
                tvStatusBadge.setText("COUNTER OFFER");
            } else if ("accepted".equals(doc.status)) {
                tvStatusBadge.setTextColor(Color.parseColor("#34C759"));
                cardStatusBadge.setCardBackgroundColor(Color.parseColor("#EAF9EE"));
            } else if ("declined".equals(doc.status)) {
                tvStatusBadge.setTextColor(Color.parseColor("#FF3B30"));
                cardStatusBadge.setCardBackgroundColor(Color.parseColor("#FFECEB"));
            } else if ("expired".equals(doc.status)) {
                tvStatusBadge.setTextColor(Color.parseColor("#8B97A8"));
                cardStatusBadge.setCardBackgroundColor(Color.parseColor("#F3F4F6"));
                if (tvExpired != null) tvExpired.setVisibility(View.VISIBLE);
            }
        }

        card.setOnClickListener(v -> {
            Intent intent = new Intent(this, RequestDetailActivity.class);
            intent.putExtra("requestId", doc.id);
            intent.putExtra("topic", doc.topic);
            intent.putExtra("status", doc.status);
            intent.putExtra("provider", doc.provider);
            intent.putExtra("category", doc.category);
            intent.putExtra("duration", doc.duration);
            intent.putExtra("tokens", doc.tokens);
            intent.putExtra("isStudentView", true);
            startActivity(intent);
        });
    }

    private static class MockDoc {
        String id, topic, status, provider, category;
        int duration, tokens;

        MockDoc(String t, String s, int d, int tok, String p, String c) {
            id = "mock_" + t; topic = t; status = s; duration = d; tokens = tok; provider = p; category = c;
        }
        MockDoc(DocumentSnapshot d) {
            id = d.getId();
            topic = d.getString("topic");
            status = d.getString("status");
            provider = d.getString("tutorName");
            category = d.getString("subject");
            Long dur = d.getLong("durationMinutes");
            duration = dur != null ? dur.intValue() : 60;
            Long tok = d.getLong("tokens");
            tokens = tok != null ? tok.intValue() : 0;
        }
    }

    private void updateTabStyles(String activeFilter) {
        int inactiveBg = Color.parseColor("#33FFFFFF");
        int activeBg = Color.WHITE;
        int inactiveText = Color.parseColor("#CCFFFFFF");
        int activeText = Color.parseColor("#071A3D");

        setTabStyle(R.id.chipAll, "all".equals(activeFilter) ? activeBg : activeBg, activeText); 
        setTabStyle(R.id.chipPending, "waiting".equals(activeFilter) ? activeBg : inactiveBg, "waiting".equals(activeFilter) ? activeText : Color.WHITE);
        setTabStyle(R.id.chipCounter, "counter".equals(activeFilter) ? activeBg : inactiveBg, "counter".equals(activeFilter) ? activeText : Color.WHITE);
        setTabStyle(R.id.chipAccepted, "accepted".equals(activeFilter) ? activeBg : inactiveBg, "accepted".equals(activeFilter) ? activeText : Color.WHITE);
        setTabStyle(R.id.chipDeclined, "declined".equals(activeFilter) ? activeBg : inactiveBg, "declined".equals(activeFilter) ? activeText : Color.WHITE);
        setTabStyle(R.id.chipExpired, "expired".equals(activeFilter) ? activeBg : inactiveBg, "expired".equals(activeFilter) ? activeText : Color.WHITE);
    }

    private void setTabStyle(int id, int bgColor, int textColor) {
        MaterialCardView card = findViewById(id);
        if (card != null) {
            card.setCardBackgroundColor(bgColor);
            View child = card.getChildAt(0);
            if (child instanceof LinearLayout) {
                LinearLayout layout = (LinearLayout) child;
                for (int i = 0; i < layout.getChildCount(); i++) {
                    View v = layout.getChildAt(i);
                    if (v instanceof TextView) {
                        ((TextView) v).setTextColor(textColor);
                    }
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (requestsListener != null) requestsListener.remove();
    }
}