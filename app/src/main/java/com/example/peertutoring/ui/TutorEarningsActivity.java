package com.example.peertutoring.ui;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.peertutoring.R;
import com.example.peertutoring.utils.SoundManager;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TutorEarningsActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private ListenerRegistration balanceListener;

    private TextView tvAvailableBalance;
    private TextView tvMonthlyTokens, tvSessionsCompleted, tvPeriodLabel;
    private LinearLayout layoutRecentSessions;
    private String currentPeriod = "month";

    private static final SimpleDateFormat DATE_FMT =
            new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutor_earnings);

        db          = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        bindViews();
        setupButtons();
        setupPeriodTabs();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startBalanceListener();
        loadStats();
        loadRecentSessions();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (balanceListener != null) { balanceListener.remove(); balanceListener = null; }
    }

    private void bindViews() {
        tvAvailableBalance   = findViewById(R.id.tvAvailableBalance);
        tvMonthlyTokens      = findViewById(R.id.tvMonthlyTokens);
        tvSessionsCompleted  = findViewById(R.id.tvSessionsCompleted);
        tvPeriodLabel        = findViewById(R.id.tvPeriodLabel);
        layoutRecentSessions = findViewById(R.id.layoutRecentSessions);
    }

    private void setupButtons() {
        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        View btnWithdraw = findViewById(R.id.btnRequestWithdrawal);
        if (btnWithdraw != null) btnWithdraw.setOnClickListener(v -> {
            SoundManager.playClick(this);
            startActivity(new Intent(this, WithdrawActivity.class));
        });

        View btnHistory = findViewById(R.id.btnViewHistory);
        if (btnHistory != null) btnHistory.setOnClickListener(v -> {
            SoundManager.playClick(this);
            startActivity(new Intent(this, TransactionHistoryActivity.class));
        });

        // "View All" → all sessions filtered to Completed
        View btnViewAll = findViewById(R.id.btnViewAll);
        if (btnViewAll != null) btnViewAll.setOnClickListener(v -> {
            Intent intent = new Intent(this, TutorRequestsActivity.class);
            intent.putExtra("defaultFilter", "completed");
            startActivity(intent);
        });
    }

    private void setupPeriodTabs() {
        Button btnWeek  = findViewById(R.id.btnPeriodWeek);
        Button btnMonth = findViewById(R.id.btnPeriodMonth);
        Button btnYear  = findViewById(R.id.btnPeriodYear);

        if (btnMonth != null) selectPeriodTab(btnWeek, btnMonth, btnYear, "month");
        if (btnWeek  != null) btnWeek.setOnClickListener(v  -> selectPeriodTab(btnWeek, btnMonth, btnYear, "week"));
        if (btnMonth != null) btnMonth.setOnClickListener(v -> selectPeriodTab(btnWeek, btnMonth, btnYear, "month"));
        if (btnYear  != null) btnYear.setOnClickListener(v  -> selectPeriodTab(btnWeek, btnMonth, btnYear, "year"));
    }

    private void selectPeriodTab(Button week, Button month, Button year, String period) {
        currentPeriod = period;
        styleTab(week,  "week".equals(period));
        styleTab(month, "month".equals(period));
        styleTab(year,  "year".equals(period));
        if (tvPeriodLabel != null) {
            switch (period) {
                case "week": tvPeriodLabel.setText("📅  This week"); break;
                case "year": tvPeriodLabel.setText("📅  This year"); break;
                default:     tvPeriodLabel.setText("📅  This month"); break;
            }
        }
        loadStats();
    }

    private void styleTab(Button btn, boolean selected) {
        if (btn == null) return;
        btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                selected ? 0xFF8A2EFF : Color.WHITE));
        btn.setTextColor(selected ? Color.WHITE : Color.parseColor("#4B5D7A"));
    }

    private void startBalanceListener() {
        if (currentUser == null) return;
        balanceListener = db.collection("users").document(currentUser.getUid())
                .addSnapshotListener((doc, e) -> {
                    if (e != null || doc == null || !doc.exists()) return;
                    Long bal = doc.getLong("tokens");
                    if (tvAvailableBalance != null)
                        tvAvailableBalance.setText(NumberFormat.getNumberInstance(Locale.getDefault())
                                .format(bal != null ? bal : 0));
                });
    }

    private Date getPeriodStart() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        switch (currentPeriod) {
            case "week":  cal.add(Calendar.DAY_OF_YEAR, -7);   break;
            case "year":  cal.add(Calendar.DAY_OF_YEAR, -365); break;
            default:      cal.add(Calendar.DAY_OF_YEAR, -30);  break; // month = last 30 days
        }
        return cal.getTime();
    }

    // Load stats for the selected period — single field filter, client-side filtering avoids composite index
    private void loadStats() {
        if (currentUser == null) return;
        Date periodStart = getPeriodStart();
        db.collection("sessionRequests")
                .whereEqualTo("tutorUid", currentUser.getUid())
                .get()
                .addOnSuccessListener(snap -> {
                    int totalTokens = 0, sessions = 0;
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        if (!"completed".equals(doc.getString("status"))) continue;
                        Date sessionDate = doc.getDate("scheduledDate");
                        if (sessionDate != null && sessionDate.before(periodStart)) continue;
                        Long tok = doc.getLong("tokens");
                        if (tok != null) totalTokens += tok;
                        sessions++;
                    }
                    if (tvMonthlyTokens != null)
                        tvMonthlyTokens.setText(NumberFormat.getNumberInstance().format(totalTokens));
                    if (tvSessionsCompleted != null)
                        tvSessionsCompleted.setText(String.valueOf(sessions));
                });
    }

    // Load recent sessions — single filter, client-side status + sort; avoids composite index
    private void loadRecentSessions() {
        if (layoutRecentSessions == null || currentUser == null) {
            showEmptySessionsState();
            return;
        }
        layoutRecentSessions.removeAllViews();

        db.collection("sessionRequests")
                .whereEqualTo("tutorUid", currentUser.getUid())
                .get()
                .addOnSuccessListener(snap -> {
                    List<DocumentSnapshot> completed = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        if ("completed".equals(doc.getString("status")))
                            completed.add(doc);
                    }
                    if (completed.isEmpty()) { showEmptySessionsState(); return; }

                    // Sort by scheduledDate descending (newest first)
                    completed.sort((a, b) -> {
                        Date da = a.getDate("scheduledDate");
                        Date db2 = b.getDate("scheduledDate");
                        if (da == null && db2 == null) return 0;
                        if (da == null) return 1;
                        if (db2 == null) return -1;
                        return db2.compareTo(da);
                    });

                    int limit = Math.min(3, completed.size());
                    for (int i = 0; i < limit; i++) {
                        DocumentSnapshot doc = completed.get(i);
                        addSessionRow(
                                doc.getString("studentName"),
                                doc.getString("subject"),
                                doc.getString("topic"),
                                doc.getLong("tokens"),
                                doc.getLong("durationMinutes"),
                                doc.getDate("scheduledDate"),
                                doc.getString("studentUid")
                        );
                    }
                })
                .addOnFailureListener(e -> showEmptySessionsState());
    }

    private void showEmptySessionsState() {
        if (layoutRecentSessions == null) return;
        layoutRecentSessions.removeAllViews();
        TextView tv = new TextView(this);
        tv.setText("No completed sessions yet");
        tv.setTextColor(0xFF8B97A8);
        tv.setTextSize(14f);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(0, dp(24), 0, dp(24));
        layoutRecentSessions.addView(tv);
    }

    private void addSessionRow(String name, String subject, String topic,
                               Long tokens, Long durationMin, Date date, String studentUid) {
        MaterialCardView card = new MaterialCardView(this);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cp.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(cp);
        card.setRadius(dp(16));
        card.setCardElevation(dp(1));
        card.setCardBackgroundColor(0xFFFFFFFF);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(14), dp(14), dp(14));

        // Avatar initials circle
        MaterialCardView avatar = new MaterialCardView(this);
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(dp(46), dp(46));
        ap.setMarginEnd(dp(14));
        avatar.setLayoutParams(ap);
        avatar.setRadius(dp(23));
        avatar.setCardElevation(0);
        avatar.setCardBackgroundColor(0xFFF3EEFF);
        TextView tvInitials = new TextView(this);
        tvInitials.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        tvInitials.setGravity(Gravity.CENTER);
        tvInitials.setTextColor(0xFF8A2EFF);
        tvInitials.setTextSize(16f);
        tvInitials.setTypeface(null, Typeface.BOLD);
        String displayName = (name != null && !name.isEmpty() && !"Student".equals(name))
                ? name : null;
        if (displayName != null) {
            String[] parts = displayName.split(" ");
            String ini = parts.length > 1
                    ? "" + parts[0].charAt(0) + parts[1].charAt(0)
                    : "" + parts[0].charAt(0);
            tvInitials.setText(ini.toUpperCase());
        } else {
            tvInitials.setText("?");
        }
        avatar.addView(tvInitials);
        row.addView(avatar);

        // Info column
        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvName = new TextView(this);
        tvName.setText(displayName != null ? displayName : "Student");
        tvName.setTextColor(0xFF071A3D);
        tvName.setTextSize(15f);
        tvName.setTypeface(null, Typeface.BOLD);
        info.addView(tvName);

        // Subject / topic
        String subjLine = subject != null ? subject : "";
        if (topic != null && !topic.isEmpty())
            subjLine = subjLine.isEmpty() ? topic : subjLine + " • " + topic;
        if (!subjLine.isEmpty()) {
            TextView tvSubj = new TextView(this);
            tvSubj.setText(subjLine);
            tvSubj.setTextColor(0xFF8A2EFF);
            tvSubj.setTextSize(12f);
            tvSubj.setPadding(0, dp(2), 0, 0);
            info.addView(tvSubj);
        }

        // Date • duration
        String meta = "";
        if (date != null) meta = DATE_FMT.format(date);
        if (durationMin != null) meta += (meta.isEmpty() ? "" : "  •  ") + durationMin + " min";
        if (!meta.isEmpty()) {
            TextView tvMeta = new TextView(this);
            tvMeta.setText(meta);
            tvMeta.setTextColor(0xFF8B97A8);
            tvMeta.setTextSize(11f);
            tvMeta.setPadding(0, dp(2), 0, 0);
            info.addView(tvMeta);
        }

        row.addView(info);

        // Token badge
        TextView tvTok = new TextView(this);
        tvTok.setText("+" + (tokens != null ? tokens : 0));
        tvTok.setTextColor(0xFF00C853);
        tvTok.setTextSize(15f);
        tvTok.setTypeface(null, Typeface.BOLD);
        row.addView(tvTok);

        card.addView(row);
        layoutRecentSessions.addView(card);

        // If name was placeholder, resolve from Firestore
        if (displayName == null && studentUid != null && !studentUid.isEmpty()) {
            db.collection("users").document(studentUid).get()
                    .addOnSuccessListener(ud -> {
                        String fn = ud.getString("fullName");
                        if (fn != null && !fn.isEmpty()) {
                            tvName.setText(fn);
                            String[] parts = fn.split(" ");
                            String ini = parts.length > 1
                                    ? "" + parts[0].charAt(0) + parts[1].charAt(0)
                                    : "" + parts[0].charAt(0);
                            tvInitials.setText(ini.toUpperCase());
                        }
                    });
        }
    }

    private int dp(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
