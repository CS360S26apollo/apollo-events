package com.example.peertutoring.ui;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.peertutoring.R;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * US-24: Transaction History screen.
 * Shows all earnings (+) and withdrawals (-) for the tutor.
 * Filterable by: All, Earned, Withdrawn, Bonus.
 * Searchable by student name or subject.
 */
public class TransactionHistoryActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    private TextView tvTotalEarned, tvTotalWithdrawn;
    private LinearLayout layoutTransactions;
    private List<TxRow> allTx = new ArrayList<>();
    private String currentFilter = "all";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_history);

        db          = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        tvTotalEarned    = findViewById(R.id.tvTotalEarned);
        tvTotalWithdrawn = findViewById(R.id.tvTotalWithdrawn);
        layoutTransactions = findViewById(R.id.layoutTransactions);

        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        setupSearch();
        setupFilterChips();
        loadTransactions();
    }

    // ── Search ────────────────────────────────────────────────

    private void setupSearch() {
        EditText etSearch = findViewById(R.id.etSearchTransactions);
        if (etSearch == null) return;
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                filterAndRender(s.toString().trim());
            }
        });
    }

    // ── Filter chips ──────────────────────────────────────────

    private void setupFilterChips() {
        View chipAll       = findViewById(R.id.chipAll);
        View chipEarned    = findViewById(R.id.chipEarned);
        View chipWithdrawn = findViewById(R.id.chipWithdrawn);
        View chipBonus     = findViewById(R.id.chipBonus);

        if (chipAll       != null) chipAll.setOnClickListener(v       -> applyFilter("all"));
        if (chipEarned    != null) chipEarned.setOnClickListener(v    -> applyFilter("earned"));
        if (chipWithdrawn != null) chipWithdrawn.setOnClickListener(v -> applyFilter("withdrawn"));
        if (chipBonus     != null) chipBonus.setOnClickListener(v     -> applyFilter("bonus"));
    }

    private void applyFilter(String filter) {
        currentFilter = filter;
        filterAndRender("");
    }

    // ── Load ──────────────────────────────────────────────────

    private void loadTransactions() {
        if (currentUser == null) { showEmptyState(); return; }

        // Load completed sessions (earnings)
        db.collection("sessionRequests")
                .whereEqualTo("tutorUid", currentUser.getUid())
                .whereEqualTo("status", "completed")
                .orderBy("scheduledDate", Query.Direction.DESCENDING)
                .limit(30)
                .get()
                .addOnSuccessListener(snap -> {
                    allTx.clear();
                    int totalEarned = 0;
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        String name  = doc.getString("studentName");
                        String subj  = doc.getString("subject");
                        Long   tok   = doc.getLong("tokens");
                        Date   date  = doc.getDate("scheduledDate");
                        if (tok == null) continue;
                        totalEarned += tok;
                        allTx.add(new TxRow("Session with " + name + " - " + subj,
                                date, tok, "earned"));
                    }
                    updateSummary(totalEarned, 0);
                    // Now load withdrawals
                    loadWithdrawals(totalEarned);
                })
                .addOnFailureListener(e -> showEmptyState());
    }

    private void showEmptyState() {
        allTx.clear();
        updateSummary(0, 0);
        filterAndRender("");
    }

    private void loadWithdrawals(int totalEarned) {
        db.collection("withdrawals")
                .whereEqualTo("tutorUid", currentUser.getUid())
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(20)
                .get()
                .addOnSuccessListener(snap -> {
                    int totalWithdrawn = 0;
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        Long   amount = doc.getLong("amount");
                        String method = doc.getString("method");
                        Date   date   = doc.getDate("createdAt");
                        if (amount == null) continue;
                        totalWithdrawn += amount;
                        allTx.add(new TxRow("Withdrawal to " + method, date, amount, "withdrawn"));
                    }
                    updateSummary(totalEarned, totalWithdrawn);
                    filterAndRender("");
                })
                .addOnFailureListener(e -> filterAndRender(""));
    }

    private void loadMockData() {
        allTx.clear();
        allTx.add(new TxRow("Session with Sarah Johnson - Mathematics", null, 150L, "earned"));
        allTx.add(new TxRow("Session with Mike Chen - Physics",         null, 200L, "earned"));
        allTx.add(new TxRow("Withdrawal to Bank Account",               null, 1000L,"withdrawn"));
        allTx.add(new TxRow("Session with Emma Davis - Chemistry",      null, 120L, "earned"));
        allTx.add(new TxRow("Session with James Wilson - Biology",      null, 180L, "earned"));
        allTx.add(new TxRow("Withdrawal to PayPal",                     null, 500L, "withdrawn"));
        allTx.add(new TxRow("Weekly Performance Bonus",                 null, 250L, "bonus"));
        allTx.add(new TxRow("Session with Olivia Brown - English",      null, 140L, "earned"));
        allTx.add(new TxRow("Session with Lucas Martinez - Economics",  null, 160L, "earned"));
        updateSummary(1200, 1500);
        filterAndRender("");
    }

    // ── Render ────────────────────────────────────────────────

    private void filterAndRender(String query) {
        if (layoutTransactions == null) return;
        layoutTransactions.removeAllViews();

        for (TxRow tx : allTx) {
            if (!"all".equals(currentFilter) && !currentFilter.equals(tx.type)) continue;
            if (!query.isEmpty() && !tx.label.toLowerCase().contains(query.toLowerCase())) continue;
            addTxCard(tx);
        }
    }

    private void addTxCard(TxRow tx) {
        MaterialCardView card = new MaterialCardView(this);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cp.setMargins(0, 0, 0, dp(10));
        card.setLayoutParams(cp);
        card.setRadius(dp(16));
        card.setCardElevation(dp(1));
        card.setCardBackgroundColor(0xFFFFFFFF);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(14), dp(14), dp(14));

        // Icon badge
        MaterialCardView iconCard = new MaterialCardView(this);
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(dp(44), dp(44));
        ip.setMarginEnd(dp(14));
        iconCard.setLayoutParams(ip);
        iconCard.setRadius(dp(22));
        iconCard.setCardElevation(0);
        int iconColor = tx.type.equals("withdrawn") ? 0xFF8A2EFF
                : tx.type.equals("bonus") ? 0xFF0062FF : 0xFF00C853;
        iconCard.setCardBackgroundColor(iconColor);
        TextView tvIcon = new TextView(this);
        tvIcon.setText(tx.type.equals("withdrawn") ? "↙" : "↗");
        tvIcon.setTextColor(Color.WHITE);
        tvIcon.setTextSize(18f);
        tvIcon.setGravity(Gravity.CENTER);
        tvIcon.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        iconCard.addView(tvIcon);
        row.addView(iconCard);

        // Label + date
        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvLabel = new TextView(this);
        // Truncate long labels
        String label = tx.label.length() > 36 ? tx.label.substring(0, 33) + "..." : tx.label;
        tvLabel.setText(label);
        tvLabel.setTextColor(0xFF071A3D);
        tvLabel.setTextSize(14f);
        tvLabel.setTypeface(null, Typeface.BOLD);
        info.addView(tvLabel);

        String dateMeta = "🗓 " + (tx.date != null
                ? new SimpleDateFormat("MMM d, yyyy  •  hh:mm a", Locale.getDefault())
                .format(tx.date)
                : "Mar 9, 2026  •  3:45 PM");
        TextView tvDate = new TextView(this);
        tvDate.setText(dateMeta);
        tvDate.setTextColor(0xFF8B97A8);
        tvDate.setTextSize(11f);
        info.addView(tvDate);
        row.addView(info);

        // Amount
        TextView tvAmount = new TextView(this);
        boolean isDebit = tx.type.equals("withdrawn");
        tvAmount.setText((isDebit ? "" : "+") + tx.amount);
        tvAmount.setTextColor(isDebit ? 0xFF8A2EFF : 0xFF00C853);
        tvAmount.setTextSize(15f);
        tvAmount.setTypeface(null, Typeface.BOLD);
        row.addView(tvAmount);

        card.addView(row);
        layoutTransactions.addView(card);
    }

    private void updateSummary(int earned, int withdrawn) {
        NumberFormat nf = NumberFormat.getNumberInstance();
        if (tvTotalEarned    != null) tvTotalEarned.setText(nf.format(earned));
        if (tvTotalWithdrawn != null) tvTotalWithdrawn.setText(nf.format(withdrawn));
    }

    // ── Inner class ───────────────────────────────────────────

    private static class TxRow {
        String label, type;
        Date   date;
        long   amount;
        TxRow(String label, Date date, long amount, String type) {
            this.label = label; this.date = date;
            this.amount = amount; this.type = type;
        }
    }

    private int dp(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}