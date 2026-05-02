package com.example.peertutoring.ui;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
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
 */
public class TransactionHistoryActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    private TextView tvTotalEarned, tvTotalWithdrawn;
    private LinearLayout layoutTransactions;
    private List<TxRow> allTx = new ArrayList<>();
    private String currentFilter = "all";

    private static final SimpleDateFormat DATE_TIME_FMT =
            new SimpleDateFormat("MMM d, yyyy  •  hh:mm a", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_history);

        db          = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        tvTotalEarned      = findViewById(R.id.tvTotalEarned);
        tvTotalWithdrawn   = findViewById(R.id.tvTotalWithdrawn);
        layoutTransactions = findViewById(R.id.layoutTransactions);

        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        setupSearch();
        setupFilterChips();
    }

    @Override
    protected void onResume() {
        super.onResume();
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

        if (chipAll       != null) chipAll.setOnClickListener(v       -> applyFilter("all",       chipAll, chipEarned, chipWithdrawn, chipBonus));
        if (chipEarned    != null) chipEarned.setOnClickListener(v    -> applyFilter("earned",    chipAll, chipEarned, chipWithdrawn, chipBonus));
        if (chipWithdrawn != null) chipWithdrawn.setOnClickListener(v -> applyFilter("withdrawn", chipAll, chipEarned, chipWithdrawn, chipBonus));
        if (chipBonus     != null) chipBonus.setOnClickListener(v     -> applyFilter("bonus",     chipAll, chipEarned, chipWithdrawn, chipBonus));

        // Highlight "All" by default
        updateChipStyle(chipAll, true);
        updateChipStyle(chipEarned, false);
        updateChipStyle(chipWithdrawn, false);
        updateChipStyle(chipBonus, false);
    }

    private void applyFilter(String filter, View... chips) {
        currentFilter = filter;
        // Update chip highlight styles
        String[] types = { "all", "earned", "withdrawn", "bonus" };
        for (int i = 0; i < chips.length && i < types.length; i++) {
            updateChipStyle(chips[i], types[i].equals(filter));
        }
        filterAndRender(getSearchQuery());
    }

    private void updateChipStyle(View chip, boolean active) {
        if (!(chip instanceof MaterialCardView)) return;
        MaterialCardView cv = (MaterialCardView) chip;
        cv.setCardBackgroundColor(active ? 0xFF8A2EFF : 0xFFFFFFFF);
        if (cv.getChildCount() > 0 && cv.getChildAt(0) instanceof TextView) {
            ((TextView) cv.getChildAt(0)).setTextColor(active ? 0xFFFFFFFF : 0xFF4B5D7A);
        }
    }

    private String getSearchQuery() {
        EditText et = findViewById(R.id.etSearchTransactions);
        return et != null ? et.getText().toString().trim() : "";
    }

    // ── Load ──────────────────────────────────────────────────

    private void loadTransactions() {
        if (currentUser == null) { showEmptyState(); return; }

        // Single-field filter only — avoids composite index requirement
        // Client-side filter for status == "completed"
        db.collection("sessionRequests")
                .whereEqualTo("tutorUid", currentUser.getUid())
                .get()
                .addOnSuccessListener(snap -> {
                    allTx.clear();
                    int totalEarned = 0;

                    List<DocumentSnapshot> completed = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        if ("completed".equals(doc.getString("status")))
                            completed.add(doc);
                    }

                    // Sort by scheduledDate descending
                    completed.sort((a, b) -> {
                        Date da = a.getDate("scheduledDate");
                        Date db2 = b.getDate("scheduledDate");
                        if (da == null && db2 == null) return 0;
                        if (da == null) return 1;
                        if (db2 == null) return -1;
                        return db2.compareTo(da);
                    });

                    for (DocumentSnapshot doc : completed) {
                        String name  = doc.getString("studentName");
                        String subj  = doc.getString("subject");
                        Long   tok   = doc.getLong("tokens");
                        Date   date  = doc.getDate("scheduledDate");
                        if (tok == null) continue;
                        totalEarned += tok;

                        String label = "Session";
                        if (name != null && !name.isEmpty() && !"Student".equals(name))
                            label = "Session with " + name;
                        if (subj != null && !subj.isEmpty())
                            label += " - " + subj;

                        allTx.add(new TxRow(label, date, tok, "earned",
                                doc.getString("studentUid")));
                    }

                    final int earned = totalEarned;
                    updateSummary(earned, 0);
                    loadWithdrawals(earned);
                })
                .addOnFailureListener(e -> showEmptyState());
    }

    private void loadWithdrawals(int totalEarned) {
        // Single-field filter, sort client-side — avoids composite index
        db.collection("withdrawals")
                .whereEqualTo("tutorUid", currentUser.getUid())
                .get()
                .addOnSuccessListener(snap -> {
                    int totalWithdrawn = 0;

                    List<DocumentSnapshot> docs = new ArrayList<>(snap.getDocuments());
                    // Sort by createdAt descending
                    docs.sort((a, b) -> {
                        com.google.firebase.Timestamp ta = a.getTimestamp("createdAt");
                        com.google.firebase.Timestamp tb = b.getTimestamp("createdAt");
                        if (ta == null && tb == null) return 0;
                        if (ta == null) return 1;
                        if (tb == null) return -1;
                        return tb.compareTo(ta);
                    });

                    for (DocumentSnapshot doc : docs) {
                        Long   amount = doc.getLong("amount");
                        String method = doc.getString("method");
                        Date   date   = doc.getDate("createdAt");
                        if (amount == null) continue;
                        totalWithdrawn += amount;
                        String label = method != null && !method.isEmpty()
                                ? "Withdrawal to " + method : "Withdrawal";
                        allTx.add(new TxRow(label, date, amount, "withdrawn", null));
                    }

                    // Re-sort all transactions by date descending (mix earned + withdrawn)
                    allTx.sort((a, b) -> {
                        if (a.date == null && b.date == null) return 0;
                        if (a.date == null) return 1;
                        if (b.date == null) return -1;
                        return b.date.compareTo(a.date);
                    });

                    updateSummary(totalEarned, totalWithdrawn);
                    filterAndRender(getSearchQuery());
                })
                .addOnFailureListener(e -> {
                    // Still render earnings even if withdrawal fetch fails
                    filterAndRender(getSearchQuery());
                });
    }

    private void showEmptyState() {
        allTx.clear();
        updateSummary(0, 0);
        filterAndRender("");
    }

    // ── Render ────────────────────────────────────────────────

    private void filterAndRender(String query) {
        if (layoutTransactions == null) return;
        layoutTransactions.removeAllViews();

        List<TxRow> visible = new ArrayList<>();
        for (TxRow tx : allTx) {
            if (!"all".equals(currentFilter) && !currentFilter.equals(tx.type)) continue;
            if (!query.isEmpty() && !tx.label.toLowerCase().contains(query.toLowerCase())) continue;
            visible.add(tx);
        }

        if (visible.isEmpty()) {
            TextView tv = new TextView(this);
            tv.setText("No transactions found");
            tv.setTextColor(0xFF8B97A8);
            tv.setTextSize(14f);
            tv.setGravity(Gravity.CENTER);
            tv.setPadding(0, dp(40), 0, dp(40));
            layoutTransactions.addView(tv);
            return;
        }

        for (TxRow tx : visible) addTxCard(tx);
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
        int iconColor = "withdrawn".equals(tx.type) ? 0xFF8A2EFF
                : "bonus".equals(tx.type) ? 0xFF0062FF : 0xFF00C853;
        iconCard.setCardBackgroundColor(iconColor);
        TextView tvIcon = new TextView(this);
        tvIcon.setText("withdrawn".equals(tx.type) ? "↙" : "↗");
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
        tvLabel.setText(tx.label);
        tvLabel.setTextColor(0xFF071A3D);
        tvLabel.setTextSize(14f);
        tvLabel.setTypeface(null, Typeface.BOLD);
        tvLabel.setMaxLines(2);
        info.addView(tvLabel);

        TextView tvDate = new TextView(this);
        tvDate.setText(tx.date != null
                ? "🗓 " + DATE_TIME_FMT.format(tx.date)
                : "Date not recorded");
        tvDate.setTextColor(0xFF8B97A8);
        tvDate.setTextSize(11f);
        info.addView(tvDate);
        row.addView(info);

        // Amount
        TextView tvAmount = new TextView(this);
        boolean isDebit = "withdrawn".equals(tx.type);
        tvAmount.setText((isDebit ? "−" : "+") + tx.amount);
        tvAmount.setTextColor(isDebit ? 0xFF8A2EFF : 0xFF00C853);
        tvAmount.setTextSize(15f);
        tvAmount.setTypeface(null, Typeface.BOLD);
        tvAmount.setPadding(dp(8), 0, 0, 0);
        row.addView(tvAmount);

        card.addView(row);
        layoutTransactions.addView(card);

        // Resolve student name if placeholder
        if (tx.studentUid != null && tx.label.contains("with ") && tx.label.contains("Student")) {
            db.collection("users").document(tx.studentUid).get()
                    .addOnSuccessListener(ud -> {
                        String fn = ud.getString("fullName");
                        if (fn != null && !fn.isEmpty()) {
                            String newLabel = tx.label.replace("Student", fn);
                            tvLabel.setText(newLabel);
                        }
                    });
        }
    }

    private void updateSummary(int earned, int withdrawn) {
        NumberFormat nf = NumberFormat.getNumberInstance();
        if (tvTotalEarned    != null) tvTotalEarned.setText(nf.format(earned));
        if (tvTotalWithdrawn != null) tvTotalWithdrawn.setText(nf.format(withdrawn));
    }

    // ── Inner class ───────────────────────────────────────────

    private static class TxRow {
        String label, type, studentUid;
        Date   date;
        long   amount;
        TxRow(String label, Date date, long amount, String type, String studentUid) {
            this.label = label; this.date = date;
            this.amount = amount; this.type = type;
            this.studentUid = studentUid;
        }
    }

    private int dp(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
