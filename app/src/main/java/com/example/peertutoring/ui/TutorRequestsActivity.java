package com.example.peertutoring.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageButton;
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

public class TutorRequestsActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String tutorUid;
    private LinearLayout layoutRequestList;
    private List<DocumentSnapshot> allRequests = new ArrayList<>();
    private String currentFilter = "all";
    private String searchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutor_requests);

        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

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

    private void setupSearch() {
        EditText etSearch = findViewById(R.id.etSearch);
        if (etSearch == null) return;
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                searchQuery = s.toString();
                refreshDisplay();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void setupFilterChips() {
        setChipClick(R.id.chipAll,       "all");
        setChipClick(R.id.chipPending,   "pending");
        setChipClick(R.id.chipCountered, "countered");
        setChipClick(R.id.chipConfirmed, "confirmed");
        setChipClick(R.id.chipCompleted, "completed");
        setChipClick(R.id.chipCancelled, "cancelled");
        updateChipStyles();
    }

    private void setChipClick(int chipId, String filter) {
        View chip = findViewById(chipId);
        if (chip != null) chip.setOnClickListener(v -> {
            currentFilter = filter;
            updateChipStyles();
            refreshDisplay();
        });
    }

    private void updateChipStyles() {
        int[][] pairs = {
            {R.id.chipAll,       0}, // 0 = "all"
            {R.id.chipPending,   1},
            {R.id.chipCountered, 2},
            {R.id.chipConfirmed, 3},
            {R.id.chipCompleted, 4},
            {R.id.chipCancelled, 5}
        };
        String[] filterKeys = {"all","pending","countered","confirmed","completed","cancelled"};

        for (int[] pair : pairs) {
            MaterialCardView chip = findViewById(pair[0]);
            if (chip == null) continue;
            boolean active = currentFilter.equals(filterKeys[pair[1]]);
            chip.setCardBackgroundColor(active ? 0xFF8A2EFF : 0xFFFFFFFF);
            TextView tv = getFirstTextView(chip);
            if (tv != null) tv.setTextColor(active ? 0xFFFFFFFF : 0xFF4B5D7A);
        }
    }

    private TextView getFirstTextView(MaterialCardView card) {
        for (int i = 0; i < card.getChildCount(); i++) {
            View child = card.getChildAt(i);
            if (child instanceof TextView) return (TextView) child;
        }
        return null;
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
                    allRequests = new ArrayList<>(snap.getDocuments());
                    // Sort: active statuses first, then by scheduledDate descending
                    allRequests.sort((a, b) -> {
                        int priorityA = statusPriority(a.getString("status"));
                        int priorityB = statusPriority(b.getString("status"));
                        if (priorityA != priorityB) return priorityA - priorityB;
                        com.google.firebase.Timestamp tsA = a.getTimestamp("createdAt");
                        com.google.firebase.Timestamp tsB = b.getTimestamp("createdAt");
                        if (tsA == null && tsB == null) return 0;
                        if (tsA == null) return 1;
                        if (tsB == null) return -1;
                        return tsB.compareTo(tsA);
                    });
                    updateNotifBadge();
                    refreshDisplay();
                });
    }

    private void updateNotifBadge() {
        int pendingCount = 0;
        for (DocumentSnapshot doc : allRequests) {
            if ("requested".equals(doc.getString("status"))) pendingCount++;
        }
        View badge = findViewById(R.id.cardNotifBadge);
        TextView tvCount = findViewById(R.id.tvNotifCount);
        if (badge == null || tvCount == null) return;
        if (pendingCount > 0) {
            badge.setVisibility(View.VISIBLE);
            tvCount.setText(String.valueOf(pendingCount));
        } else {
            badge.setVisibility(View.GONE);
        }
    }

    private int statusPriority(String status) {
        if (status == null) return 99;
        switch (status) {
            case "requested":      return 0;
            case "counter_offered":return 1;
            case "booked":         return 2;
            case "completed":      return 3;
            case "cancelled":      return 4;
            case "expired":        return 5;
            default:               return 6;
        }
    }

    private void refreshDisplay() {
        List<DocumentSnapshot> filtered = new ArrayList<>();
        for (DocumentSnapshot doc : allRequests) {
            if (!matchesFilter(doc)) continue;
            if (!matchesSearch(doc)) continue;
            filtered.add(doc);
        }
        displayRequests(filtered);
    }

    private boolean matchesFilter(DocumentSnapshot doc) {
        if ("all".equals(currentFilter)) return true;
        String status = doc.getString("status");
        switch (currentFilter) {
            case "pending":   return "requested".equals(status);
            case "countered": return "counter_offered".equals(status);
            case "confirmed": return "booked".equals(status);
            case "completed": return "completed".equals(status);
            case "cancelled": return "cancelled".equals(status) || "expired".equals(status);
            default:          return true;
        }
    }

    private boolean matchesSearch(DocumentSnapshot doc) {
        if (searchQuery.isEmpty()) return true;
        String q = searchQuery.toLowerCase();
        String name  = doc.getString("studentName");
        String topic = doc.getString("topic");
        return (name  != null && name.toLowerCase().contains(q))
            || (topic != null && topic.toLowerCase().contains(q));
    }

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
        String status      = doc.getString("status");

        setText(card, R.id.tvStudentName, studentName);
        setText(card, R.id.tvSubject,     subject);
        setText(card, R.id.tvTopic,       topic);
        setText(card, R.id.tvDate,        date     != null ? date  : "TBD");
        setText(card, R.id.tvTime,        (time    != null ? time  : "TBD")
                + (duration != null ? " (" + duration + "m)" : ""));
        setText(card, R.id.tvTokens,      (tokens  != null ? tokens : 0) + " Tokens");

        // Status badge
        MaterialCardView cardBadge = card.findViewById(R.id.cardStatusBadge);
        TextView tvBadge = card.findViewById(R.id.tvStatusBadge);
        if (cardBadge != null && tvBadge != null) {
            applyStatusBadge(cardBadge, tvBadge, status);
        }

        // Initials
        TextView tvInitials = card.findViewById(R.id.tvStudentInitials);
        if (tvInitials != null && studentName != null && !studentName.isEmpty()) {
            String[] parts = studentName.split(" ");
            String initials = parts.length > 1
                    ? "" + parts[0].charAt(0) + parts[1].charAt(0)
                    : "" + parts[0].charAt(0);
            tvInitials.setText(initials.toUpperCase());
        }

        // Profile photo
        String sUid = doc.getString("studentUid");
        android.widget.ImageView ivSPhoto = card.findViewById(R.id.ivStudentPhoto);
        if (ivSPhoto != null && sUid != null) {
            db.collection("users").document(sUid).get()
                    .addOnSuccessListener(userDoc -> {
                        Boolean visible = userDoc.getBoolean("profileVisible");
                        String url = userDoc.getString("profilePhotoUrl");
                        if (url != null && !url.isEmpty() && !Boolean.FALSE.equals(visible)) {
                            ivSPhoto.setVisibility(android.view.View.VISIBLE);
                            if (tvInitials != null) tvInitials.setVisibility(android.view.View.GONE);
                            if (url.startsWith("data:image")) {
                                String b64 = url.substring(url.indexOf(",") + 1);
                                byte[] bytes = android.util.Base64.decode(b64, android.util.Base64.NO_WRAP);
                                com.bumptech.glide.Glide.with(this).load(bytes).circleCrop().into(ivSPhoto);
                            } else {
                                com.bumptech.glide.Glide.with(this).load(url).circleCrop().into(ivSPhoto);
                            }
                        }
                    });
        }

        // Accent bar color
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

        // Hide inline action buttons (open detail for actions)
        View layoutActions = card.findViewById(R.id.layoutActionButtons);
        if (layoutActions != null) layoutActions.setVisibility(View.GONE);

        // Click → open detail
        String requestId  = doc.getId();
        String studentUid = doc.getString("studentUid");
        String goals      = doc.getString("goals");
        String studentMsg = doc.getString("studentMessage");
        int    dur        = duration != null ? duration.intValue() : 60;
        int    tok        = tokens   != null ? tokens.intValue()   : 150;

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

    private void applyStatusBadge(MaterialCardView card, TextView tv, String status) {
        if (status == null) return;
        switch (status) {
            case "requested":
                tv.setText("PENDING");
                tv.setTextColor(0xFF007AFF);
                card.setCardBackgroundColor(0xFFE6F2FF);
                break;
            case "counter_offered":
                tv.setText("COUNTERED");
                tv.setTextColor(0xFFFF9500);
                card.setCardBackgroundColor(0xFFFFF3E0);
                break;
            case "booked":
                tv.setText("CONFIRMED");
                tv.setTextColor(0xFF34C759);
                card.setCardBackgroundColor(0xFFEAF9EE);
                break;
            case "completed":
                tv.setText("COMPLETED");
                tv.setTextColor(0xFFAF52DE);
                card.setCardBackgroundColor(0xFFF3EEFF);
                break;
            case "cancelled":
                tv.setText("CANCELLED");
                tv.setTextColor(0xFFFF3B30);
                card.setCardBackgroundColor(0xFFFFECEB);
                break;
            case "expired":
                tv.setText("EXPIRED");
                tv.setTextColor(0xFF8E8E93);
                card.setCardBackgroundColor(0xFFF2F2F7);
                break;
            default:
                tv.setText(status.toUpperCase());
        }
    }

    private void showEmpty() {
        TextView tv = new TextView(this);
        tv.setText("No session requests found for this filter.");
        tv.setTextColor(Color.parseColor("#8B97A8"));
        tv.setTextSize(15f);
        tv.setGravity(android.view.Gravity.CENTER);
        tv.setPadding(0, 80, 0, 0);
        layoutRequestList.addView(tv);
    }

    private void showEmptyState() {
        if (layoutRequestList == null) return;
        layoutRequestList.removeAllViews();
        TextView tv = new TextView(this);
        tv.setText("No session requests yet.\nStudents will appear here when they book you.");
        tv.setTextColor(android.graphics.Color.parseColor("#8B97A8"));
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (requestsListener != null) requestsListener.remove();
    }
}
