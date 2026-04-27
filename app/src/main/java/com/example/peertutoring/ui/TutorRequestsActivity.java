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
        if (!tutorUid.isEmpty()) {
            ExpirationUtils.expireStaleRequestsForTutor(tutorUid, db);
        }
    }

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

    private void loadRequests() {
        if (tutorUid.isEmpty()) { showEmpty(); return; }

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
                    if (allRequests.isEmpty()) showEmpty();
                    else displayRequests(allRequests);
                })
                .addOnFailureListener(e -> showEmpty());
    }

    private void displayRequests(List<DocumentSnapshot> list) {
        if (layoutRequestList == null) return;
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
        String timeStr = (time != null ? time  : "TBD") + (duration != null ? " (" + duration + "m)" : "");
        setText(card, R.id.tvTime,        timeStr);
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

        card.setOnClickListener(v -> {
            Intent intent = new Intent(this, RequestDetailActivity.class);
            intent.putExtra("requestId",      doc.getId());
            intent.putExtra("studentUid",     doc.getString("studentUid"));
            intent.putExtra("studentName",    studentName);
            intent.putExtra("subject",        subject);
            intent.putExtra("topic",          topic);
            intent.putExtra("date",           date);
            intent.putExtra("time",           time);
            intent.putExtra("duration",       duration != null ? duration.intValue() : 60);
            intent.putExtra("tokens",         tokens   != null ? tokens.intValue()   : 150);
            intent.putExtra("goals",          doc.getString("goals"));
            intent.putExtra("studentMessage", doc.getString("studentMessage"));
            startActivity(intent);
        });
    }

    private void showEmpty() {
        if (layoutRequestList == null) return;
        layoutRequestList.removeAllViews();

        TextView tvTitle = new TextView(this);
        tvTitle.setText("No pending requests");
        tvTitle.setTextColor(Color.parseColor("#071A3D"));
        tvTitle.setTextSize(18f);
        tvTitle.setGravity(android.view.Gravity.CENTER);
        tvTitle.setPadding(40, 100, 40, 8);
        layoutRequestList.addView(tvTitle);
    }

    private void setText(View parent, int id, String text) {
        TextView tv = parent.findViewById(id);
        if (tv != null && text != null) tv.setText(text);
    }
}