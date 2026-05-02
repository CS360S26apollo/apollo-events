package com.example.peertutoring.ui;

import android.content.Intent;
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
import java.util.Date;
import java.util.List;

public class TutorRequestsActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String tutorUid;
    private LinearLayout layoutRequestList;
    private List<DocumentSnapshot> allRequests = new ArrayList<>();
    private String currentFilter = "all";

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

        // Support pre-selected filter (e.g. from "View All" on earnings screen)
        String defaultFilter = getIntent().getStringExtra("defaultFilter");
        if (defaultFilter != null && !defaultFilter.isEmpty()) currentFilter = defaultFilter;

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
        // Reconnect listener if it was removed (e.g. after onStop)
        if (requestsListener == null) loadRequests();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (requestsListener != null) {
            requestsListener.remove();
            requestsListener = null;
        }
    }

    private void setupSearch() {
        EditText etSearch = findViewById(R.id.etSearch);
        if (etSearch == null) return;
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                applySearchAndFilter(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void setupFilterChips() {
        View chipAll       = findViewById(R.id.chipAll);
        View chipPending   = findViewById(R.id.chipPending);
        View chipBooked    = findViewById(R.id.chipBooked);
        View chipCompleted = findViewById(R.id.chipCompleted);
        View chipCancelled = findViewById(R.id.chipCancelled);

        if (chipAll       != null) chipAll.setOnClickListener(v       -> setFilter("all"));
        if (chipPending   != null) chipPending.setOnClickListener(v   -> setFilter("requested"));
        if (chipBooked    != null) chipBooked.setOnClickListener(v    -> setFilter("booked"));
        if (chipCompleted != null) chipCompleted.setOnClickListener(v -> setFilter("completed"));
        if (chipCancelled != null) chipCancelled.setOnClickListener(v -> setFilter("cancelled"));
    }

    private void setFilter(String filter) {
        currentFilter = filter;
        updateChipStyles();
        applySearchAndFilter(getSearchQuery());
    }

    private String getSearchQuery() {
        EditText et = findViewById(R.id.etSearch);
        return et != null ? et.getText().toString() : "";
    }

    private void applySearchAndFilter(String query) {
        List<DocumentSnapshot> filtered = new ArrayList<>();
        for (DocumentSnapshot doc : allRequests) {
            String status = doc.getString("status");
            if (!"all".equals(currentFilter) && !currentFilter.equals(status)) continue;
            if (!query.isEmpty()) {
                String name  = doc.getString("studentName");
                String topic = doc.getString("topic");
                boolean match = (name  != null && name.toLowerCase().contains(query.toLowerCase()))
                        || (topic != null && topic.toLowerCase().contains(query.toLowerCase()));
                if (!match) continue;
            }
            filtered.add(doc);
        }
        displayRequests(filtered);
    }

    private void updateChipStyles() {
        int activeColor   = 0xFF8A2EFF;
        int inactiveColor = 0xFFFFFFFF;
        setChipColor(R.id.chipAll,       "all".equals(currentFilter)        ? activeColor : inactiveColor);
        setChipColor(R.id.chipPending,   "requested".equals(currentFilter)  ? activeColor : inactiveColor);
        setChipColor(R.id.chipBooked,    "booked".equals(currentFilter)     ? activeColor : inactiveColor);
        setChipColor(R.id.chipCompleted, "completed".equals(currentFilter)  ? activeColor : inactiveColor);
        setChipColor(R.id.chipCancelled, "cancelled".equals(currentFilter)  ? activeColor : inactiveColor);
    }

    private void setChipColor(int id, int color) {
        MaterialCardView chip = findViewById(id);
        if (chip != null) chip.setCardBackgroundColor(color);
        // Update text color for active vs inactive
        View child = chip != null ? chip.getChildAt(0) : null;
        if (child instanceof TextView) {
            ((TextView) child).setTextColor(color == 0xFF8A2EFF ? 0xFFFFFFFF : 0xFF4B5D7A);
        }
    }

    private com.google.firebase.firestore.ListenerRegistration requestsListener;

    private void loadRequests() {
        if (tutorUid.isEmpty()) { showEmptyState(); return; }

        requestsListener = db.collection("sessionRequests")
                .whereEqualTo("tutorUid", tutorUid)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) {
                        showEmptyState();
                        return;
                    }
                    allRequests = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        allRequests.add(doc);
                    }
                    // Sort: active sessions first (by date), then completed/cancelled
                    allRequests.sort((a, b) -> {
                        String sa = a.getString("status");
                        String sb = b.getString("status");
                        boolean aActive = "requested".equals(sa) || "booked".equals(sa);
                        boolean bActive = "requested".equals(sb) || "booked".equals(sb);
                        if (aActive != bActive) return aActive ? -1 : 1;
                        Date da = a.getDate("scheduledDate");
                        Date db2 = b.getDate("scheduledDate");
                        if (da == null && db2 == null) return 0;
                        if (da == null) return 1;
                        if (db2 == null) return -1;
                        return da.compareTo(db2);
                    });
                    applySearchAndFilter(getSearchQuery());
                });
    }

    private void displayRequests(List<DocumentSnapshot> list) {
        if (layoutRequestList == null) return;
        layoutRequestList.removeAllViews();
        if (list.isEmpty()) { showEmptyState(); return; }
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
        String status      = doc.getString("status");
        Long   tokens      = doc.getLong("tokens");
        Long   duration    = doc.getLong("durationMinutes");
        String studentUid  = doc.getString("studentUid");

        // Resolve real name if placeholder was stored
        boolean needsNameLookup = studentName == null || studentName.isEmpty()
                || "Student".equals(studentName);
        String displayName = needsNameLookup ? "Loading..." : studentName;

        setText(card, R.id.tvStudentName, displayName);
        setText(card, R.id.tvSubject,     subject);
        setText(card, R.id.tvTopic,       topic);
        setText(card, R.id.tvDate,        date  != null ? date  : "TBD");
        setText(card, R.id.tvTime,        (time != null ? time  : "TBD")
                + (duration != null ? " (" + duration + "m)" : ""));
        setText(card, R.id.tvTokens,      (tokens != null ? tokens : 0) + " Tokens");

        bindStatusBadge(card, status);
        applyAvatarInitials(card, displayName);

        // Subject accent bar color
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

        String requestId  = doc.getId();
        String goals      = doc.getString("goals");
        String studentMsg = doc.getString("studentMessage");
        int    dur        = duration != null ? duration.intValue() : 60;
        int    tok        = tokens   != null ? tokens.intValue()   : 150;
        // Mutable holder so the name resolved from Firestore is available when user taps
        final String[] resolvedName = { studentName != null ? studentName : "" };

        // Single async lookup if stored name is a placeholder
        if (needsNameLookup && studentUid != null && !studentUid.isEmpty()) {
            db.collection("users").document(studentUid).get()
                    .addOnSuccessListener(userDoc -> {
                        String fullName = userDoc.getString("fullName");
                        if (fullName != null && !fullName.isEmpty()) {
                            resolvedName[0] = fullName;
                            setText(card, R.id.tvStudentName, fullName);
                            applyAvatarInitials(card, fullName);
                        }
                    });
        }

        card.setOnClickListener(v -> {
            Intent intent = new Intent(this, RequestDetailActivity.class);
            intent.putExtra("requestId",      requestId);
            intent.putExtra("studentUid",     studentUid);
            intent.putExtra("studentName",    resolvedName[0]);
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

    private void applyAvatarInitials(View card, String name) {
        TextView tvInitials = card.findViewById(R.id.tvStudentInitials);
        if (tvInitials == null || name == null || name.isEmpty()
                || "Loading...".equals(name)) return;
        String[] parts = name.split(" ");
        String initials = parts.length > 1
                ? "" + parts[0].charAt(0) + parts[1].charAt(0)
                : "" + parts[0].charAt(0);
        tvInitials.setText(initials.toUpperCase());
        MaterialCardView avatarCard = (MaterialCardView) tvInitials.getParent();
        avatarCard.setCardBackgroundColor(
                AVATAR_COLORS[Math.abs(initials.hashCode()) % AVATAR_COLORS.length]);
    }

    private void bindStatusBadge(View card, String status) {
        TextView tvStatus     = card.findViewById(R.id.tvStatusBadge);
        MaterialCardView cvStatus = card.findViewById(R.id.cardStatusBadge);
        if (tvStatus == null || cvStatus == null) return;
        switch (status != null ? status : "") {
            case "requested":
                tvStatus.setText("PENDING");
                tvStatus.setTextColor(0xFF007AFF);
                cvStatus.setCardBackgroundColor(0xFFE6F2FF);
                break;
            case "booked":
                tvStatus.setText("BOOKED");
                tvStatus.setTextColor(0xFF34C759);
                cvStatus.setCardBackgroundColor(0xFFEAF9EE);
                break;
            case "completed":
                tvStatus.setText("COMPLETED");
                tvStatus.setTextColor(0xFFAF52DE);
                cvStatus.setCardBackgroundColor(0xFFF3EEFF);
                break;
            case "cancelled":
                tvStatus.setText("CANCELLED");
                tvStatus.setTextColor(0xFFFF3B30);
                cvStatus.setCardBackgroundColor(0xFFFFECEB);
                break;
            case "expired":
                tvStatus.setText("EXPIRED");
                tvStatus.setTextColor(0xFF8E8E93);
                cvStatus.setCardBackgroundColor(0xFFF2F2F7);
                break;
            default:
                tvStatus.setText(status != null ? status.toUpperCase() : "");
        }
    }

    private void showEmptyState() {
        if (layoutRequestList == null) return;
        layoutRequestList.removeAllViews();
        TextView tv = new TextView(this);
        tv.setText("all".equals(currentFilter)
                ? "No session requests yet.\nStudents will appear here when they book you."
                : "No sessions with this status.");
        tv.setTextColor(0xFF8B97A8);
        tv.setTextSize(15f);
        tv.setGravity(android.view.Gravity.CENTER);
        tv.setPadding(0, 80, 0, 0);
        tv.setLineSpacing(8f, 1f);
        layoutRequestList.addView(tv);
    }

    private void setText(View parent, int id, String text) {
        TextView tv = parent.findViewById(id);
        if (tv != null && text != null) tv.setText(text);
    }

    private void setupEarningsButton() {
        View btn = findViewById(R.id.btnOpenEarnings);
        if (btn != null) {
            btn.setOnClickListener(v ->
                    startActivity(new Intent(this, TutorEarningsActivity.class)));
        }
    }

    private void setupBottomNav() {
        View navHome         = findViewById(R.id.navHome);
        View navRequests     = findViewById(R.id.navRequests);
        View navEarnings     = findViewById(R.id.navEarnings);
        View navAvailability = findViewById(R.id.navAvailability);
        View navProfile      = findViewById(R.id.navProfile);

        if (navHome != null) {
            navHome.setOnClickListener(v -> {
                startActivity(new Intent(this, HomeActivity.class));
                overridePendingTransition(0, 0);
                finish();
            });
        }
        if (navRequests != null) navRequests.setOnClickListener(v -> { /* current screen */ });
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
