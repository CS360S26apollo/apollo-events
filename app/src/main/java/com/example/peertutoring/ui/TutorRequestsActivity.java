package com.example.peertutoring.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.peertutoring.R;
import com.example.peertutoring.utils.ExpirationUtils;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity for tutors to view and manage incoming tutoring session requests from students.
 * Role: Request Management View for User Story 09 (Tutor Response).
 * Purpose: Displays a list of pending student proposals, allowing tutors to filter
 * by priority (token amount) and search for specific topics or students.
 *
 * Design Pattern: View-Controller with dynamic list rendering.
 *
 * Outstanding Issues:
 * - Real-time listener is not yet implemented (uses one-time get() with mock fallback).
 */
public class TutorRequestsActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String tutorUid;
    private LinearLayout layoutRequestList;
    private List<DocumentSnapshot> allRequests = new ArrayList<>();

    private static final int[] AVATAR_COLORS = {
            0xFFD0C4F7, 0xFF4ECDC4, 0xFFFFB7B2, 0xFFB5EAD7, 0xFFC7CEEA
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutor_requests);

        db       = FirebaseFirestore.getInstance();
        tutorUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

        layoutRequestList = findViewById(R.id.layoutRequestList);

        setupSearch();
        setupFilterChips();
        setupEarningsButton();
        setupBottomNav();
        loadRequests();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ExpirationUtils.expireStaleRequestsForTutor(tutorUid, db);
    }

    /**
     * Initializes the search input field with a listener to filter the list by student name or topic.
     */
    private void setupSearch() {
        EditText etSearch = findViewById(R.id.etSearch);
        if (etSearch == null) return;
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                filterBySearch(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void filterBySearch(String query) {
        if (query.isEmpty()) { displayRequests(allRequests); return; }
        List<DocumentSnapshot> filtered = new ArrayList<>();
        for (DocumentSnapshot doc : allRequests) {
            String name  = doc.getString("studentName");
            String topic = doc.getString("topic");
            boolean match = (name  != null && name.toLowerCase().contains(query.toLowerCase()))
                    || (topic != null && topic.toLowerCase().contains(query.toLowerCase()));
            if (match) filtered.add(doc);
        }
        displayRequests(filtered);
    }

    /**
     * Sets up priority-based filter chips (High, Medium, Low) based on the session's token value.
     */
    private void setupFilterChips() {
        View chipAll    = findViewById(R.id.chipAll);
        View chipHigh   = findViewById(R.id.chipHighPriority);
        View chipMedium = findViewById(R.id.chipMediumPriority);
        View chipLow    = findViewById(R.id.chipLowPriority);

        if (chipAll    != null) chipAll.setOnClickListener(v    -> displayRequests(allRequests));
        if (chipHigh   != null) chipHigh.setOnClickListener(v   -> filterByPriority("high"));
        if (chipMedium != null) chipMedium.setOnClickListener(v -> filterByPriority("medium"));
        if (chipLow    != null) chipLow.setOnClickListener(v    -> filterByPriority("low"));
    }

    private void filterByPriority(String priority) {
        List<DocumentSnapshot> filtered = new ArrayList<>();
        for (DocumentSnapshot doc : allRequests) {
            Long tokens = doc.getLong("tokens");
            if (tokens == null) continue;
            boolean match = false;
            if ("high".equals(priority)   && tokens > 200)  match = true;
            if ("medium".equals(priority) && tokens >= 100 && tokens <= 200) match = true;
            if ("low".equals(priority)    && tokens < 100)  match = true;
            if (match) filtered.add(doc);
        }
        displayRequests(filtered);
    }

    /**
     * Fetches all pending session requests from Firestore where status is 'waiting'.
     * Falls back to mock data if the collection is empty or if user is not logged in.
     */
    private void loadRequests() {
        if (tutorUid.isEmpty()) { showEmpty(); return; }

        // Query by tutorUid only — filtering by status + orderBy would require a
        // composite Firestore index. Filter and sort in-memory instead.
        db.collection("sessionRequests")
                .whereEqualTo("tutorUid", tutorUid)
                .get()
                .addOnSuccessListener(snap -> {
                    allRequests = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        if ("requested".equals(doc.getString("status"))) {
                            allRequests.add(doc);
                        }
                    }
                    // Sort by createdAt descending (nulls last)
                    allRequests.sort((a, b) -> {
                        com.google.firebase.Timestamp ta = a.getTimestamp("createdAt");
                        com.google.firebase.Timestamp tb = b.getTimestamp("createdAt");
                        if (ta == null && tb == null) return 0;
                        if (ta == null) return 1;
                        if (tb == null) return -1;
                        return tb.compareTo(ta);
                    });
                    if (allRequests.isEmpty()) showEmpty();
                    else displayRequests(allRequests);
                })
                .addOnFailureListener(e -> showEmpty());
    }

    /**
     * Renders the list of session request cards into the scrollable container.
     * @param list The list of document snapshots to display.
     */
    private void displayRequests(List<DocumentSnapshot> list) {
        layoutRequestList.removeAllViews();
        if (list.isEmpty()) { showEmpty(); return; }
        LayoutInflater inflater = LayoutInflater.from(this);
        for (DocumentSnapshot doc : list) {
            View card = inflater.inflate(R.layout.item_tutor_request_card, layoutRequestList, false);
            bindCard(card, doc);
            layoutRequestList.addView(card);
        }
    }

    private void bindCard(View card, DocumentSnapshot doc) {
        String studentName = doc.getString("studentName");
        String subject     = doc.getString("subject");
        String topic       = doc.getString("topic");
        String date        = doc.getString("sessionDate");
        String time        = doc.getString("sessionTime");
        Long   tokens      = doc.getLong("tokens");
        Long   duration    = doc.getLong("durationMinutes");

        setText(card, R.id.tvStudentName, studentName);
        setText(card, R.id.tvSubject,     subject);
        setText(card, R.id.tvTopic,       topic);
        setText(card, R.id.tvDate,        date  != null ? date  : "TBD");
        setText(card, R.id.tvTime,        (time != null ? time  : "TBD")
                + (duration != null ? " (" + duration + "m)" : ""));
        setText(card, R.id.tvTokens,      (tokens != null ? tokens : 0) + " Tokens");

        TextView tvInitials = card.findViewById(R.id.tvStudentInitials);
        if (tvInitials != null && studentName != null && !studentName.isEmpty()) {
            String[] parts = studentName.split(" ");
            String initials = parts.length > 1
                    ? "" + parts[0].charAt(0) + parts[1].charAt(0)
                    : "" + parts[0].charAt(0);
            tvInitials.setText(initials.toUpperCase());
            MaterialCardView avatarCard = (MaterialCardView) tvInitials.getParent();
            avatarCard.setCardBackgroundColor(
                    AVATAR_COLORS[Math.abs(initials.hashCode()) % AVATAR_COLORS.length]);
        }

        View accentBar = card.findViewById(R.id.viewAccentBar);
        if (accentBar != null && subject != null) {
            int color;
            switch (subject.toLowerCase()) {
                case "physics":          color = 0xFF0062FF; break;
                case "chemistry":        color = 0xFF00C853; break;
                case "computer science": color = 0xFF4ECDC4; break;
                default:                 color = 0xFF8A2EFF; break;
            }
            accentBar.setBackgroundColor(color);
        }

        String requestId    = doc.getId();
        String studentUid   = doc.getString("studentUid");
        String goals        = doc.getString("goals");
        String studentMsg   = doc.getString("studentMessage");
        int    dur          = duration != null ? duration.intValue() : 60;
        int    tok          = tokens   != null ? tokens.intValue()   : 150;

        card.setOnClickListener(v -> {
            Intent intent = new Intent(this, RequestDetailActivity.class);
            intent.putExtra("requestId",      requestId);
            intent.putExtra("studentUid",     studentUid);
            intent.putExtra("studentName",    studentName);
            intent.putExtra("subject",        subject);
            intent.putExtra("topic",          topic);
            intent.putExtra("date",           date);
            intent.putExtra("time",           time);
            intent.putExtra("duration",       dur);
            intent.putExtra("tokens",         tok);
            intent.putExtra("goals",          goals);
            intent.putExtra("studentMessage", studentMsg);
            startActivity(intent);
        });
    }

    private void showEmpty() {
        layoutRequestList.removeAllViews();

        TextView tvIcon = new TextView(this);
        tvIcon.setText("📭");
        tvIcon.setTextSize(48f);
        tvIcon.setGravity(android.view.Gravity.CENTER);
        tvIcon.setPadding(0, 80, 0, 8);
        layoutRequestList.addView(tvIcon);

        TextView tvTitle = new TextView(this);
        tvTitle.setText("No pending requests");
        tvTitle.setTextColor(Color.parseColor("#071A3D"));
        tvTitle.setTextSize(18f);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitle.setGravity(android.view.Gravity.CENTER);
        tvTitle.setPadding(40, 0, 40, 8);
        layoutRequestList.addView(tvTitle);

        TextView tvSub = new TextView(this);
        tvSub.setText("Students who book your profile will appear here. Use the Seed Data tool from the home screen to add test requests.");
        tvSub.setTextColor(Color.parseColor("#8B97A8"));
        tvSub.setTextSize(13f);
        tvSub.setGravity(android.view.Gravity.CENTER);
        tvSub.setPadding(40, 0, 40, 0);
        layoutRequestList.addView(tvSub);
    }


    private void setText(View parent, int id, String text) {
        TextView tv = parent.findViewById(id);
        if (tv != null && text != null) tv.setText(text);
    }
    // ── Earnings button (header) ──────────────────────────────

    private void setupEarningsButton() {
        View btn = findViewById(R.id.btnOpenEarnings);
        if (btn != null) {
            btn.setOnClickListener(v ->
                    startActivity(new Intent(this, TutorEarningsActivity.class)));
        }
    }

    // ── Bottom navigation ─────────────────────────────────────

    private void setupBottomNav() {
        View navHome         = findViewById(R.id.navHome);
        View navRequests     = findViewById(R.id.navRequests);
        View navEarnings     = findViewById(R.id.navEarnings);
        View navAvailability = findViewById(R.id.navAvailability);
        View navProfile      = findViewById(R.id.navProfile);

        if (navHome != null) {
            navHome.setOnClickListener(v -> {
                startActivity(new Intent(this, TutorHomeActivity.class));
                overridePendingTransition(0, 0);
                finish();
            });
        }

        if (navRequests != null) {
            // Already on this screen
            navRequests.setOnClickListener(v -> { /* current screen */ });
        }

        if (navEarnings != null) {
            navEarnings.setOnClickListener(v -> {
                startActivity(new Intent(this, TutorEarningsActivity.class));
                overridePendingTransition(0, 0);
            });
        }

        if (navAvailability != null) {
            navAvailability.setOnClickListener(v -> {
                startActivity(new Intent(this, AvailabilityDashboardActivity.class));
                overridePendingTransition(0, 0);
            });
        }

        if (navProfile != null) {
            navProfile.setOnClickListener(v -> {
                startActivity(new Intent(this, EditProfileActivity.class));
                overridePendingTransition(0, 0);
            });
        }
    }


}