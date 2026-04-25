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
import com.google.firebase.firestore.Query;

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
        if (tutorUid.isEmpty()) { loadMockData(); return; }

        db.collection("sessionRequests")
                .whereEqualTo("tutorUid", tutorUid)
                .whereEqualTo("status", "requested")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(20)
                .get()
                .addOnSuccessListener(snap -> {
                    allRequests = snap.getDocuments();
                    if (allRequests.isEmpty()) loadMockData();
                    else displayRequests(allRequests);
                })
                .addOnFailureListener(e -> loadMockData());
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
        TextView tv = new TextView(this);
        tv.setText("No new session requests yet.");
        tv.setTextColor(Color.parseColor("#8B97A8"));
        tv.setTextSize(15f);
        tv.setGravity(android.view.Gravity.CENTER);
        tv.setPadding(0, 80, 0, 0);
        layoutRequestList.addView(tv);
    }

    private void loadMockData() {
        String[][] mocks = {
                {"Emily Chen",       "Mathematics",       "Calculus II - Integration Techniques", "Mar 12", "14:00", "60",  "150",
                        "Exam Prep", "Hi! I need help understanding u-substitution and integration by parts. I have an exam next week."},
                {"Marcus Johnson",   "Physics",           "Quantum Mechanics - Wave Functions",   "Mar 10", "16:30", "90",  "225",
                        "Concept Review", "Need help with wave function normalisation and probability density."},
                {"Sophia Rodriguez", "Chemistry",         "Organic Chemistry - Reaction...",      "Mar 11", "10:00", "75",  "190",
                        "Homework Help", "Struggling with SN1 vs SN2 reaction mechanisms for my assignment."},
                {"Alex Kim",         "Computer Science",  "Data Structures - Binary Trees",       "Mar 13", "18:00", "60",  "150",
                        "Problem Solving", "Need to understand BST insertion, deletion and traversal algorithms."},
        };

        layoutRequestList.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (String[] m : mocks) {
            View card = inflater.inflate(R.layout.item_tutor_request_card, layoutRequestList, false);

            setText(card, R.id.tvStudentName, m[0]);
            setText(card, R.id.tvSubject,     m[1]);
            setText(card, R.id.tvTopic,       m[2]);
            setText(card, R.id.tvDate,        m[3]);
            setText(card, R.id.tvTime,        m[4] + " (" + m[5] + "m)");
            setText(card, R.id.tvTokens,      m[6] + " Tokens");

            TextView tvInitials = card.findViewById(R.id.tvStudentInitials);
            if (tvInitials != null) {
                String[] parts = m[0].split(" ");
                String initials = parts.length > 1
                        ? "" + parts[0].charAt(0) + parts[1].charAt(0)
                        : "" + parts[0].charAt(0);
                tvInitials.setText(initials.toUpperCase());
                MaterialCardView avatarCard = (MaterialCardView) tvInitials.getParent();
                avatarCard.setCardBackgroundColor(
                        AVATAR_COLORS[Math.abs(initials.hashCode()) % AVATAR_COLORS.length]);
            }

            View accentBar = card.findViewById(R.id.viewAccentBar);
            if (accentBar != null) {
                int color;
                switch (m[1].toLowerCase()) {
                    case "physics":          color = 0xFF0062FF; break;
                    case "chemistry":        color = 0xFF00C853; break;
                    case "computer science": color = 0xFF4ECDC4; break;
                    default:                 color = 0xFF8A2EFF; break;
                }
                accentBar.setBackgroundColor(color);
            }

            final String[] row = m;
            card.setOnClickListener(v -> {
                Intent intent = new Intent(this, RequestDetailActivity.class);
                intent.putExtra("studentName",    row[0]);
                intent.putExtra("subject",        row[1]);
                intent.putExtra("topic",          row[2]);
                intent.putExtra("date",           row[3]);
                intent.putExtra("time",           row[4]);
                intent.putExtra("duration",       Integer.parseInt(row[5]));
                intent.putExtra("tokens",         Integer.parseInt(row[6]));
                intent.putExtra("goals",          row[7]);
                intent.putExtra("studentMessage", row[8]);
                startActivity(intent);
            });

            layoutRequestList.addView(card);
        }
    }

    private void setText(View parent, int id, String text) {
        TextView tv = parent.findViewById(id);
        if (tv != null && text != null) tv.setText(text);
    }
}