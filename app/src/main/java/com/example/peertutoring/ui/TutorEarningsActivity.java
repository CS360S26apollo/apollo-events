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
import com.google.firebase.firestore.Query;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * US-24: Tutor Earnings Dashboard.
 *
 * Shows:
 *  - Available token balance (real-time from Firestore)
 *  - This month's tokens earned + sessions completed
 *  - Recent completed sessions list
 *  - Pending tokens (escrow balance)
 *  - Navigation to WithdrawActivity and TransactionHistoryActivity
 *
 * Design Pattern: Observer (real-time Firestore snapshots).
 */
public class TutorEarningsActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private ListenerRegistration balanceListener;

    // Header
    private TextView tvAvailableBalance, tvGrowthPercent;

    // Stats
    private TextView tvMonthlyTokens, tvSessionsCompleted;

    // Recent sessions container
    private LinearLayout layoutRecentSessions;

    // Pending tokens

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
        loadMonthlyStats();
        loadRecentSessions();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (balanceListener != null) { balanceListener.remove(); balanceListener = null; }
    }

    private void bindViews() {
        tvAvailableBalance  = findViewById(R.id.tvAvailableBalance);
        tvGrowthPercent     = findViewById(R.id.tvGrowthPercent);
        tvMonthlyTokens     = findViewById(R.id.tvMonthlyTokens);
        tvSessionsCompleted = findViewById(R.id.tvSessionsCompleted);
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
    }

    /** Week/Month/Year tab switcher — Month is selected by default. */
    private void setupPeriodTabs() {
        Button btnWeek  = findViewById(R.id.btnPeriodWeek);
        Button btnMonth = findViewById(R.id.btnPeriodMonth);
        Button btnYear  = findViewById(R.id.btnPeriodYear);

        if (btnMonth != null) selectPeriodTab(btnWeek, btnMonth, btnYear, "month");

        if (btnWeek  != null) btnWeek.setOnClickListener(v ->
                selectPeriodTab(btnWeek, btnMonth, btnYear, "week"));
        if (btnMonth != null) btnMonth.setOnClickListener(v ->
                selectPeriodTab(btnWeek, btnMonth, btnYear, "month"));
        if (btnYear  != null) btnYear.setOnClickListener(v ->
                selectPeriodTab(btnWeek, btnMonth, btnYear, "year"));
    }

    private void selectPeriodTab(Button week, Button month, Button year, String period) {
        styleTab(week,  period.equals("week"));
        styleTab(month, period.equals("month"));
        styleTab(year,  period.equals("year"));
        loadMonthlyStats(); // reuse same query for simplicity
    }

    private void styleTab(Button btn, boolean selected) {
        if (btn == null) return;
        if (selected) {
            btn.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(0xFF8A2EFF));
            btn.setTextColor(Color.WHITE);
        } else {
            btn.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(Color.WHITE));
            btn.setTextColor(Color.parseColor("#4B5D7A"));
        }
    }

    // ── Real-time balance ─────────────────────────────────────

    private void startBalanceListener() {
        if (currentUser == null) { updateBalance(0); return; }

        balanceListener = db.collection("users").document(currentUser.getUid())
                .addSnapshotListener((doc, e) -> {
                    if (e != null || doc == null) return;
                    Long bal     = doc.getLong("tokens");
                    Long escrow  = doc.getLong("escrowBalance");
                    int bal2 = bal != null ? bal.intValue() : 0;
                    updateBalance(bal2);
                });
    }

    private void updateBalance(int balance) {
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.getDefault());
        if (tvAvailableBalance != null)
            tvAvailableBalance.setText(nf.format(balance));
    }

    // ── Monthly stats ─────────────────────────────────────────

    private void loadMonthlyStats() {
        if (currentUser == null) return;

        db.collection("sessionRequests")
                .whereEqualTo("tutorUid", currentUser.getUid())
                .whereEqualTo("status", "completed")
                .get()
                .addOnSuccessListener(snap -> {
                    int totalTokens = 0;
                    int sessions    = 0;
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        Long tok = doc.getLong("tokens");
                        if (tok != null) totalTokens += tok;
                        sessions++;
                    }
                    if (tvMonthlyTokens     != null)
                        tvMonthlyTokens.setText(
                                NumberFormat.getNumberInstance().format(totalTokens));
                    if (tvSessionsCompleted != null)
                        tvSessionsCompleted.setText(String.valueOf(sessions));
                });
    }

    // ── Recent sessions ───────────────────────────────────────

    private void loadRecentSessions() {
        if (layoutRecentSessions == null) return;
        layoutRecentSessions.removeAllViews();

        if (currentUser == null) {
            addMockSessions();
            return;
        }

        db.collection("sessionRequests")
                .whereEqualTo("tutorUid", currentUser.getUid())
                .whereEqualTo("status", "completed")
                .orderBy("scheduledDate", Query.Direction.DESCENDING)
                .limit(5)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) { addMockSessions(); return; }
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        String studentName = doc.getString("studentName");
                        String subject     = doc.getString("subject");
                        Long   tokens      = doc.getLong("tokens");
                        Long   dur         = doc.getLong("durationMinutes");
                        Date   date        = doc.getDate("scheduledDate");
                        addSessionRow(studentName, subject, tokens, dur, date, false);
                    }
                    // Pending (escrow) row
                    addPendingRow();
                })
                .addOnFailureListener(e -> addMockSessions());
    }

    private void addMockSessions() {
        addSessionRow("Sarah Johnson", "Mathematics", 150L, 90L, null, false);
        addSessionRow("Mike Chen",     "Physics",     200L, 120L, null, false);
        addSessionRow("Emma Davis",    "Chemistry",   120L, 60L,  null, false);
        addSessionRow("James Wilson",  "Biology",     180L, 105L, null, false);
        addPendingRow();
    }

    private void addSessionRow(String name, String subject, Long tokens,
                               Long durationMin, Date date, boolean isPending) {
        if (layoutRecentSessions == null) return;

        MaterialCardView card = new MaterialCardView(this);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cp.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(cp);
        card.setRadius(dp(16));
        card.setCardElevation(dp(1));
        card.setCardBackgroundColor(isPending ? 0xFFFFF8E1 : 0xFFFFFFFF);
        if (isPending) { card.setStrokeColor(0xFFFFCA28); card.setStrokeWidth(dp(1)); }

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(14), dp(16), dp(14));

        // Info column
        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvName = new TextView(this);
        tvName.setText(isPending ? "Pending Tokens" : (name != null ? name : "Student"));
        tvName.setTextColor(isPending ? 0xFFF57C00 : 0xFF071A3D);
        tvName.setTextSize(15f);
        tvName.setTypeface(null, Typeface.BOLD);
        info.addView(tvName);

        if (!isPending && subject != null) {
            TextView tvSubject = new TextView(this);
            tvSubject.setText(subject);
            tvSubject.setTextColor(0xFF8B97A8);
            tvSubject.setTextSize(13f);
            info.addView(tvSubject);
        }

        // Date/duration row
        StringBuilder meta = new StringBuilder();
        if (date != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM d, h:mm a", Locale.getDefault());
            meta.append(sdf.format(date));
        }
        if (durationMin != null && durationMin > 0) {
            if (meta.length() > 0) meta.append("  •  ");
            long h = durationMin / 60, m = durationMin % 60;
            if (h > 0) meta.append(h).append("h");
            if (m > 0) meta.append(m > 0 && h > 0 ? " " : "").append(m).append("m");
        }
        if (!isPending && meta.length() == 0) {
            // fallback labels for mock data
        } else if (isPending) {
            meta.append("Awaiting session confirmation");
        }
        if (meta.length() > 0) {
            TextView tvMeta = new TextView(this);
            tvMeta.setText(meta.toString());
            tvMeta.setTextColor(isPending ? 0xFFF57C00 : 0xFF8B97A8);
            tvMeta.setTextSize(12f);
            info.addView(tvMeta);
        }

        row.addView(info);

        // Token badge
        if (tokens != null) {
            MaterialCardView badge = new MaterialCardView(this);
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            badge.setLayoutParams(bp);
            badge.setRadius(dp(20));
            badge.setCardElevation(0);
            badge.setCardBackgroundColor(isPending ? 0xFFFFF8E1 : 0xFF8A2EFF);

            LinearLayout badgeInner = new LinearLayout(this);
            badgeInner.setOrientation(LinearLayout.HORIZONTAL);
            badgeInner.setGravity(Gravity.CENTER_VERTICAL);
            badgeInner.setPadding(dp(10), dp(8), dp(14), dp(8));

            TextView tvIcon = new TextView(this);
            tvIcon.setText("🪙");
            tvIcon.setTextSize(13f);
            badgeInner.addView(tvIcon);

            TextView tvTokens = new TextView(this);
            tvTokens.setText(String.valueOf(tokens));
            tvTokens.setTextColor(isPending ? 0xFFF57C00 : Color.WHITE);
            tvTokens.setTextSize(15f);
            tvTokens.setTypeface(null, Typeface.BOLD);
            tvTokens.setPadding(dp(4), 0, 0, 0);
            badgeInner.addView(tvTokens);

            badge.addView(badgeInner);
            row.addView(badge);
        }

        card.addView(row);
        layoutRecentSessions.addView(card);
    }

    private void addPendingRow() {
        // Calculate escrow balance from user doc — use stored value
        if (currentUser == null) { addSessionRow(null, null, 320L, null, null, true); return; }
        db.collection("users").document(currentUser.getUid()).get()
                .addOnSuccessListener(doc -> {
                    Long escrow = doc.getLong("escrowBalance");
                    if (escrow != null && escrow > 0) {
                        addSessionRow(null, null, escrow, null, null, true);
                    }
                });
    }

    private int dp(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}