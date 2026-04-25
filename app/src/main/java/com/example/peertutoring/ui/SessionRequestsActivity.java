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
import com.example.peertutoring.models.SessionRequest;
import com.example.peertutoring.utils.ExpirationUtils;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Activity for students to view and track all their tutoring session requests.
 * Role: Tracking View for User Story 10 (Track Request Status) 
 * and User Story 16 (Session Lifecycle Tracking).
 * 
 * Purpose: Provides a comprehensive list of all requests (Requested, Booked, 
 * Completed, Cancelled) with real-time updates from Firestore. Aligned 
 * with the visual dashboard requirements from Figma.
 * 
 * Design Pattern: Observer pattern (Real-time Firestore snapshots).
 */
public class SessionRequestsActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String currentUid;
    private ListenerRegistration requestsListener;

    private LinearLayout layoutRequestList;
    private TextView tvCountUpcoming, tvCountCompleted, tvCountTotal;
    private List<SessionDoc> allSessions = new ArrayList<>();
    private String currentFilter = "all";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_requests);

        db = FirebaseFirestore.getInstance();
        currentUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

        layoutRequestList = findViewById(R.id.layoutRequestList);

        tvCountUpcoming = findViewById(R.id.tvCountUpcoming);
        tvCountCompleted = findViewById(R.id.tvCountCompleted);
        tvCountTotal = findViewById(R.id.tvCountTotal);

        setupFilterChips();
        setupBottomNav();
        startRealTimeListener();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ExpirationUtils.expireStaleRequestsForStudent(currentUid, db);
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

    /** Initializes the horizontal filter bar for categorizing session requests. */
    private void setupFilterChips() {
        findViewById(R.id.chipAll).setOnClickListener(v -> applyFilter("all"));
        findViewById(R.id.chipPending).setOnClickListener(v -> applyFilter(SessionRequest.STATUS_REQUESTED));
        findViewById(R.id.chipCounter).setOnClickListener(v -> applyFilter(SessionRequest.STATUS_BOOKED));
        findViewById(R.id.chipAccepted).setOnClickListener(v -> applyFilter(SessionRequest.STATUS_COMPLETED));
        findViewById(R.id.chipDeclined).setOnClickListener(v -> applyFilter(SessionRequest.STATUS_CANCELLED));
        findViewById(R.id.chipExpired).setOnClickListener(v -> applyFilter(SessionRequest.STATUS_EXPIRED));
    }

    /**
     * Filters the displayed list of sessions based on the selected status.
     * @param filter The status string to filter by.
     */
    private void applyFilter(String filter) {
        currentFilter = filter;
        updateListUI();
        updateTabStyles(filter);
    }

    /**
     * Establishes a real-time connection to the Firestore sessionRequests collection.
     * Implementation of US 10 real-time tracking.
     */
    private void startRealTimeListener() {
        if (currentUid.isEmpty()) { loadMockData(); return; }

        requestsListener = db.collection("sessionRequests")
                .whereEqualTo("studentUid", currentUid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (snap != null && !snap.isEmpty()) {
                        allSessions.clear();
                        for (DocumentSnapshot d : snap.getDocuments()) {
                            allSessions.add(new SessionDoc(d));
                        }
                        updateCounts();
                        updateListUI();
                    } else {
                        loadMockData();
                    }
                });
    }

    /** Loads local mock data if the user is not authenticated (for demo purposes). */
    private void loadMockData() {
        allSessions.clear();
        Calendar cal = Calendar.getInstance();

        cal.add(Calendar.DAY_OF_YEAR, 2);
        allSessions.add(new SessionDoc("Mathematics", SessionRequest.STATUS_BOOKED, 60, 50, "Dr. Sarah Johnson", "Mathematics", cal.getTime()));

        cal.add(Calendar.DAY_OF_YEAR, -1);
        allSessions.add(new SessionDoc("Physics", SessionRequest.STATUS_REQUESTED, 90, 82, "Prof. Michael Chen", "Physics", cal.getTime()));

        cal.add(Calendar.DAY_OF_YEAR, -5);
        allSessions.add(new SessionDoc("Chemistry", SessionRequest.STATUS_COMPLETED, 60, 45, "Emma Rodriguez", "Chemistry", cal.getTime()));
        
        updateCounts();
        updateListUI();
    }

    /** Recalculates the summary statistics displayed in the screen header. */
    private void updateCounts() {
        int upcoming = 0, completed = 0;
        Date now = new Date();
        for (SessionDoc doc : allSessions) {
            if (SessionRequest.STATUS_BOOKED.equals(doc.status) && doc.scheduledDate != null && doc.scheduledDate.after(now)) {
                upcoming++;
            } else if (SessionRequest.STATUS_COMPLETED.equals(doc.status)) {
                completed++;
            }
        }
        if (tvCountUpcoming != null) tvCountUpcoming.setText(String.valueOf(upcoming));
        if (tvCountCompleted != null) tvCountCompleted.setText(String.valueOf(completed));
        if (tvCountTotal != null) tvCountTotal.setText(String.valueOf(allSessions.size()));
    }

    private void updateListUI() {
        layoutRequestList.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (SessionDoc doc : allSessions) {
            if (!"all".equals(currentFilter) && !currentFilter.equals(doc.status)) continue;

            View card = inflater.inflate(R.layout.item_session_request, layoutRequestList, false);
            bindCard(card, doc);
            layoutRequestList.addView(card);
        }
    }

    private void bindCard(View card, SessionDoc doc) {
        TextView tvTopic = card.findViewById(R.id.tvTopic);
        TextView tvStatusBadge = card.findViewById(R.id.tvStatusBadge);
        MaterialCardView cardStatusBadge = card.findViewById(R.id.cardStatusBadge);
        TextView tvDuration = card.findViewById(R.id.tvDuration);
        TextView tvTokens = card.findViewById(R.id.tvTokens);
        TextView tvProvider = card.findViewById(R.id.tvProviderName);
        TextView tvCategory = card.findViewById(R.id.tvCategory);
        TextView tvLocation = card.findViewById(R.id.tvLocation);

        if (tvTopic != null) tvTopic.setText(doc.topic);
        if (tvCategory != null) tvCategory.setText(doc.category);
        if (tvDuration != null) tvDuration.setText(doc.duration + " min");
        if (tvTokens != null) tvTokens.setText(String.valueOf(doc.tokens));
        if (tvProvider != null) tvProvider.setText(doc.provider);

        if (doc.scheduledDate != null && tvLocation != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy  HH:mm", Locale.getDefault());
            tvLocation.setText(" " + sdf.format(doc.scheduledDate));
        }

        if (tvStatusBadge != null && cardStatusBadge != null) {
            String displayStatus = doc.status.toUpperCase();
            tvStatusBadge.setText(displayStatus);
            
            if (SessionRequest.STATUS_REQUESTED.equals(doc.status)) {
                tvStatusBadge.setTextColor(Color.parseColor("#007AFF"));
                cardStatusBadge.setCardBackgroundColor(Color.parseColor("#E6F2FF"));
                tvStatusBadge.setText("Requested");
            } else if (SessionRequest.STATUS_BOOKED.equals(doc.status)) {
                tvStatusBadge.setTextColor(Color.parseColor("#34C759"));
                cardStatusBadge.setCardBackgroundColor(Color.parseColor("#EAF9EE"));
                tvStatusBadge.setText("Booked");
            } else if (SessionRequest.STATUS_COMPLETED.equals(doc.status)) {
                tvStatusBadge.setTextColor(Color.parseColor("#AF52DE"));
                cardStatusBadge.setCardBackgroundColor(Color.parseColor("#F3EEFF"));
                tvStatusBadge.setText("Completed");
            } else if (SessionRequest.STATUS_CANCELLED.equals(doc.status)) {
                tvStatusBadge.setTextColor(Color.parseColor("#FF3B30"));
                cardStatusBadge.setCardBackgroundColor(Color.parseColor("#FFECEB"));
                tvStatusBadge.setText("Cancelled");
            } else if (SessionRequest.STATUS_EXPIRED.equals(doc.status)) {
                tvStatusBadge.setTextColor(Color.parseColor("#8E8E93"));
                cardStatusBadge.setCardBackgroundColor(Color.parseColor("#F2F2F7"));
                tvStatusBadge.setText("Expired");
            }
        }
    }

    /** Internal wrapper class to simplify document processing from both Firestore and Mock sources. */
    private static class SessionDoc {
        String id, topic, status, provider, category;
        int duration, tokens;
        Date scheduledDate;

        SessionDoc(String t, String s, int d, int tok, String p, String c, Date date) {
            id = "mock_" + t; topic = t; status = s; duration = d; tokens = tok; provider = p; category = c; scheduledDate = date;
        }
        SessionDoc(DocumentSnapshot d) {
            id = d.getId();
            topic = d.getString("topic");
            status = d.getString("status");
            provider = d.getString("tutorName");
            category = d.getString("subject");
            Long dur = d.getLong("durationMinutes");
            duration = dur != null ? dur.intValue() : 60;
            Long tok = d.getLong("tokens");
            tokens = tok != null ? tok.intValue() : 0;
            scheduledDate = d.getDate("scheduledDate");
        }
    }

    private void updateTabStyles(String activeFilter) {
        int activeBg = Color.parseColor("#34C759");
        setTabStyle(R.id.chipAll, "all".equals(activeFilter) ? activeBg : Color.WHITE);
        setTabStyle(R.id.chipPending, SessionRequest.STATUS_REQUESTED.equals(activeFilter) ? activeBg : Color.WHITE);
        setTabStyle(R.id.chipCounter, SessionRequest.STATUS_BOOKED.equals(activeFilter) ? activeBg : Color.WHITE);
        setTabStyle(R.id.chipAccepted, SessionRequest.STATUS_COMPLETED.equals(activeFilter) ? activeBg : Color.WHITE);
        setTabStyle(R.id.chipDeclined, SessionRequest.STATUS_CANCELLED.equals(activeFilter) ? activeBg : Color.WHITE);
        setTabStyle(R.id.chipExpired, SessionRequest.STATUS_EXPIRED.equals(activeFilter) ? activeBg : Color.WHITE);
    }

    private void setTabStyle(int id, int bgColor) {
        MaterialCardView card = findViewById(id);
        if (card != null) {
            card.setCardBackgroundColor(bgColor);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (requestsListener != null) requestsListener.remove();
    }
}