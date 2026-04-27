package com.example.peertutoring.ui;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.peertutoring.R;
import com.example.peertutoring.utils.SoundManager;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * US-24: Tutor Withdrawal Screen.
 *
 * - Shows available balance and USD equivalent (10 tokens = $1)
 * - Tutor enters withdrawal amount or taps a quick-select chip
 * - Selects a withdrawal method (Bank Transfer / PayPal / Debit Card)
 * - Validates: amount > 0, amount <= balance, amount >= 100 (minimum)
 * - On confirm: deducts tokens, records withdrawal in Firestore, navigates to success
 */
public class WithdrawActivity extends AppCompatActivity {

    public static final int    TOKENS_PER_DOLLAR   = 10;
    public static final int    MINIMUM_WITHDRAWAL   = 100;
    public static final int[]  QUICK_AMOUNTS        = {500, 1000, 2500, 5000};

    // Withdrawal methods: {label, subtitle, fee tokens, icon emoji, bg color}
    private static final String[][] METHODS = {
            {"Bank Transfer", "2-3 business days", "0",  "🏦", "#0062FF"},
            {"PayPal",        "Instant",            "20", "💳", "#8A2EFF"},
            {"Debit Card",    "1 business day",     "10", "💚", "#00C853"},
    };

    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    private TextView tvAvailableBalance, tvUsdEquivalent;
    private EditText etAmount;
    private Button btnConfirm;
    private LinearLayout layoutMethods;

    private long currentBalance = 0;
    private int  selectedAmount = 0;
    private int  selectedMethodIndex = -1;
    private MaterialCardView[] methodCards;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_withdraw);

        db          = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        tvAvailableBalance    = findViewById(R.id.tvAvailableBalance);
        tvUsdEquivalent       = findViewById(R.id.tvUsdEquivalent);
        etAmount              = findViewById(R.id.etWithdrawAmount);
        btnConfirm            = findViewById(R.id.btnConfirmWithdrawal);
        layoutMethods         = findViewById(R.id.layoutWithdrawMethods);

        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        setupQuickAmounts();
        buildMethodCards();
        setupAmountInput();

        if (btnConfirm != null) {
            btnConfirm.setEnabled(false);
            btnConfirm.setAlpha(0.4f);
            btnConfirm.setOnClickListener(v -> { SoundManager.playClick(this); confirmWithdrawal(); });
        }

        loadBalance();
    }

    // ── Balance ───────────────────────────────────────────────

    private void loadBalance() {
        if (currentUser == null) { updateBalanceUI(12450); return; }
        db.collection("users").document(currentUser.getUid()).get()
                .addOnSuccessListener(doc -> {
                    Long bal = doc.getLong("tokens");
                    updateBalanceUI(bal != null ? bal : 0);
                });
    }

    private void updateBalanceUI(long balance) {
        currentBalance = balance;
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.getDefault());
        if (tvAvailableBalance != null) tvAvailableBalance.setText(nf.format(balance));
        if (tvUsdEquivalent != null)
            tvUsdEquivalent.setText(String.format(Locale.getDefault(),
                    "$%.2f", balance / (double) TOKENS_PER_DOLLAR));
    }

    // ── Quick amount chips ────────────────────────────────────

    private void setupQuickAmounts() {
        LinearLayout container = findViewById(R.id.layoutQuickAmounts);
        if (container == null) return;
        container.removeAllViews();

        for (int amount : QUICK_AMOUNTS) {
            Button chip = new Button(this);
            chip.setText(String.valueOf(amount));
            chip.setTextSize(14f);
            chip.setAllCaps(false);
            chip.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#EEF2FF")));
            chip.setTextColor(Color.parseColor("#8A2EFF"));

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(dp(10));
            chip.setLayoutParams(lp);
            chip.setPadding(dp(16), dp(6), dp(16), dp(6));

            chip.setOnClickListener(v -> {
                if (etAmount != null) etAmount.setText(String.valueOf(amount));
                selectedAmount = amount;
                updateConfirmButton();
            });

            container.addView(chip);
        }
    }

    // ── Amount input ──────────────────────────────────────────

    private void setupAmountInput() {
        if (etAmount == null) return;
        etAmount.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                try {
                    selectedAmount = Integer.parseInt(s.toString().trim());
                } catch (NumberFormatException e) {
                    selectedAmount = 0;
                }
                updateConfirmButton();
            }
        });
    }

    // ── Method cards ──────────────────────────────────────────

    private void buildMethodCards() {
        if (layoutMethods == null) return;
        layoutMethods.removeAllViews();
        methodCards = new MaterialCardView[METHODS.length];

        for (int i = 0; i < METHODS.length; i++) {
            final int idx = i;
            String[] m = METHODS[i];

            MaterialCardView card = new MaterialCardView(this);
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cp.setMargins(0, 0, 0, dp(12));
            card.setLayoutParams(cp);
            card.setRadius(dp(16));
            card.setCardElevation(dp(1));
            card.setCardBackgroundColor(0xFFFFFFFF);
            card.setStrokeWidth(dp(1));
            card.setStrokeColor(0xFFE4D5FF);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(16), dp(14), dp(16), dp(14));

            // Icon
            MaterialCardView iconBox = new MaterialCardView(this);
            LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(dp(48), dp(48));
            ip.setMarginEnd(dp(14));
            iconBox.setLayoutParams(ip);
            iconBox.setRadius(dp(14));
            iconBox.setCardElevation(0);
            iconBox.setCardBackgroundColor(Color.parseColor(m[4]));
            TextView tvIcon = new TextView(this);
            tvIcon.setText(m[3]);
            tvIcon.setTextSize(20f);
            tvIcon.setGravity(Gravity.CENTER);
            tvIcon.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            iconBox.addView(tvIcon);
            row.addView(iconBox);

            // Labels
            LinearLayout labels = new LinearLayout(this);
            labels.setOrientation(LinearLayout.VERTICAL);
            labels.setLayoutParams(new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView tvLabel = new TextView(this);
            tvLabel.setText(m[0]);
            tvLabel.setTextColor(0xFF071A3D);
            tvLabel.setTextSize(15f);
            tvLabel.setTypeface(null, Typeface.BOLD);
            labels.addView(tvLabel);

            TextView tvSub = new TextView(this);
            tvSub.setText(m[1]);
            tvSub.setTextColor(0xFF8B97A8);
            tvSub.setTextSize(12f);
            labels.addView(tvSub);
            row.addView(labels);

            // Fee column
            LinearLayout feeCol = new LinearLayout(this);
            feeCol.setOrientation(LinearLayout.VERTICAL);
            feeCol.setGravity(Gravity.END);

            TextView tvFeeLabel = new TextView(this);
            tvFeeLabel.setText("Fee");
            tvFeeLabel.setTextColor(0xFF8B97A8);
            tvFeeLabel.setTextSize(11f);
            tvFeeLabel.setGravity(Gravity.END);
            feeCol.addView(tvFeeLabel);

            TextView tvFee = new TextView(this);
            tvFee.setText(m[2] + " tokens");
            tvFee.setTextColor(0xFF071A3D);
            tvFee.setTextSize(13f);
            tvFee.setTypeface(null, Typeface.BOLD);
            tvFee.setGravity(Gravity.END);
            feeCol.addView(tvFee);
            row.addView(feeCol);

            card.addView(row);
            card.setOnClickListener(v -> selectMethod(idx));
            methodCards[i] = card;
            layoutMethods.addView(card);
        }
    }

    private void selectMethod(int idx) {
        selectedMethodIndex = idx;
        for (int i = 0; i < methodCards.length; i++) {
            if (methodCards[i] == null) continue;
            boolean sel = i == idx;
            methodCards[i].setStrokeColor(sel ? 0xFF8A2EFF : 0xFFE4D5FF);
            methodCards[i].setStrokeWidth(sel ? dp(2) : dp(1));
            methodCards[i].setCardBackgroundColor(sel ? 0xFFF9F5FF : 0xFFFFFFFF);
        }
        updateConfirmButton();
    }

    // ── Validation ────────────────────────────────────────────

    private void updateConfirmButton() {
        boolean valid = selectedAmount >= MINIMUM_WITHDRAWAL
                && selectedAmount <= currentBalance
                && selectedMethodIndex >= 0;
        if (btnConfirm != null) {
            btnConfirm.setEnabled(valid);
            btnConfirm.setAlpha(valid ? 1.0f : 0.4f);
        }
    }

    // ── Confirm withdrawal ────────────────────────────────────

    /**
     * Validates the withdrawal, deducts tokens from Firestore,
     * records the withdrawal document, then opens WithdrawSuccessActivity.
     */
    private void confirmWithdrawal() {
        if (selectedAmount < MINIMUM_WITHDRAWAL) {
            SoundManager.playError(this);
            Toast.makeText(this, "Minimum withdrawal is " + MINIMUM_WITHDRAWAL + " tokens",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedAmount > currentBalance) {
            SoundManager.playError(this);
            Toast.makeText(this, "Insufficient token balance", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedMethodIndex < 0) {
            Toast.makeText(this, "Please select a withdrawal method", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentUser == null) {
            // Demo mode — skip Firestore
            navigateToSuccess(selectedAmount, METHODS[selectedMethodIndex][0],
                    METHODS[selectedMethodIndex][1]);
            return;
        }

        btnConfirm.setEnabled(false);
        btnConfirm.setText("Processing...");

        int feeCost  = Integer.parseInt(METHODS[selectedMethodIndex][2]);
        int total    = selectedAmount + feeCost;
        String method = METHODS[selectedMethodIndex][0];
        String processingTime = METHODS[selectedMethodIndex][1];

        // Deduct tokens
        db.collection("users").document(currentUser.getUid())
                .update("tokens", com.google.firebase.firestore.FieldValue.increment(-total))
                .addOnSuccessListener(u -> recordWithdrawal(method, processingTime, feeCost))
                .addOnFailureListener(e -> {
                    SoundManager.playError(this);
                    Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    btnConfirm.setEnabled(true);
                    btnConfirm.setText("Confirm Withdrawal");
                });
    }

    private void recordWithdrawal(String method, String processingTime, int fee) {
        Map<String, Object> record = new HashMap<>();
        record.put("tutorUid",       currentUser.getUid());
        record.put("amount",         selectedAmount);
        record.put("fee",            fee);
        record.put("method",         method);
        record.put("processingTime", processingTime);
        record.put("status",         "pending");
        record.put("createdAt",      FieldValue.serverTimestamp());

        String txId = "WD-" + new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(new Date()) + "-" + selectedAmount;

        db.collection("withdrawals").add(record)
                .addOnSuccessListener(ref -> {
                    SoundManager.playSuccess(this);
                    navigateToSuccess(selectedAmount, method, processingTime);
                })
                .addOnFailureListener(e -> {
                    // Non-critical — tokens already deducted, still show success
                    SoundManager.playSuccess(this);
                    navigateToSuccess(selectedAmount, method, processingTime);
                });
    }

    private void navigateToSuccess(int amount, String method, String processingTime) {
        Intent intent = new Intent(this, WithdrawSuccessActivity.class);
        intent.putExtra("amount",         amount);
        intent.putExtra("method",         method);
        intent.putExtra("processingTime", processingTime);
        startActivity(intent);
        finish();
    }

    private int dp(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}