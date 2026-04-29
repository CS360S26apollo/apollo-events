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
import java.util.List;
import java.util.Locale;

/**
 * US-24: Tutor Earnings Dashboard.
 */
public class TutorEarningsActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private ListenerRegistration balanceListener;

    private TextView tvAvailableBalance;
    private TextView tvMonthlyTokens, tvSessionsCompleted;
    private LinearLayout layoutRecentSessions;

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
        tvAvailableBalance   = findViewById(R.id.tvAvailableBalance);
        tvMonthlyTokens      = findViewById(R.id.tvMonthlyTokens);
        tvSessionsCompleted  = findViewById(R.id.tvSessionsCompleted);
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

    private void setupPeriodTabs() {
        Button btnWeek  = findViewById(R.id.btnPeriodWeek);
        Button btnMonth = findViewById(R.id.btnPeriodMonth);
        Button btnYear  = findViewById(R.id.btnPeriodYear);

        if (btnMonth != null) selectPeriodTab(btnWeek, btnMonth, btnYear, "month");

        if (btnWeek  != null) btnWeek.setOnClickListener(v -> selectPeriodTab(btnWeek, btnMonth, btnYear, "week"));
        if (btnMonth != null) btnMonth.setOnClickListener(v -> selectPeriodTab(btnWeek, btnMonth, btnYear, "month"));
        if (btnYear  != null) btnYear.setOnClickListener(v -> selectPeriodTab(btnWeek, btnMonth, btnYear, "year"));
    }

    private void selectPeriodTab(Button week, Button month, Button year, String period) {
        styleTab(week,  period.equals("week"));
        styleTab(month, period.equals("month"));
        styleTab(year,  period.equals("year"));
        loadMonthlyStats();
    }

    private void styleTab(Button btn, boolean selected) {
        if (btn == null) return;
        if (selected) {
            btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF8A2EFF));
            btn.setTextColor(Color.WHITE);
        } else {
            btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.WHITE));
            btn.setTextColor(Color.parseColor("#4B5D7A"));
        }
    }

    private void startBalanceListener() {
        if (currentUser == null) return;
        balanceListener = db.collection("users").document(currentUser.getUid())
                .addSnapshotListener((doc, e) -> {
                    if (e != null || doc == null || !doc.exists()) return;
                    Long bal = doc.getLong("tokens");
                    updateBalance(bal != null ? bal.intValue() : 0);
                });
    }

    private void updateBalance(int balance) {
        if (tvAvailableBalance != null) {
            tvAvailableBalance.setText(NumberFormat.getNumberInstance(Locale.getDefault()).format(balance));
        }
    }

    private void loadMonthlyStats() {
        if (currentUser == null) return;
        db.collection("sessionRequests")
                .whereEqualTo("tutorUid", currentUser.getUid())
                .whereEqualTo("status", "completed")
                .get()
                .addOnSuccessListener(snap -> {
                    int totalTokens = 0;
                    int sessions = 0;
                    for (DocumentSnapshot doc : snap.getDocuments()) {
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

    private void loadRecentSessions() {
        if (layoutRecentSessions == null) return;
        layoutRecentSessions.removeAllViews();

        if (currentUser == null) {
            showEmptySessionsState();
            return;
        }

        db.collection("sessionRequests")
                .whereEqualTo("tutorUid", currentUser.getUid())
                .whereEqualTo("status", "completed")
                .orderBy("scheduledDate", Query.Direction.DESCENDING)
                .limit(5)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) { showEmptySessionsState(); return; }
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        addSessionRow(doc.getString("studentName"), doc.getString("subject"),
                                     doc.getLong("tokens"), doc.getLong("durationMinutes"),
                                     doc.getDate("scheduledDate"), false);
                    }
                })
                .addOnFailureListener(e -> showEmptySessionsState());
    }

    private void showEmptySessionsState() {
        if (layoutRecentSessions == null) return;
        TextView tv = new TextView(this);
        tv.setText("No completed sessions yet");
        tv.setTextColor(0xFF8B97A8);
        tv.setTextSize(14f);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(0, dp(24), 0, dp(24));
        layoutRecentSessions.addView(tv);
    }

    private void addSessionRow(String name, String subject, Long tokens, Long durationMin, Date date, boolean isPending) {
        MaterialCardView card = new MaterialCardView(this);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cp.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(cp);
        card.setRadius(dp(16));
        card.setCardElevation(dp(1));
        card.setCardBackgroundColor(isPending ? 0xFFFFF8E1 : 0xFFFFFFFF);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(14), dp(16), dp(14));

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvName = new TextView(this);
        tvName.setText(isPending ? "Pending Tokens" : (name != null ? name : "Student"));
        tvName.setTextColor(0xFF071A3D);
        tvName.setTextSize(15f);
        tvName.setTypeface(null, Typeface.BOLD);
        info.addView(tvName);

        row.addView(info);
        card.addView(row);
        layoutRecentSessions.addView(card);
    }

    private int dp(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}