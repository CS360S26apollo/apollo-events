package com.example.peertutoring.ui;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.peertutoring.R;
import com.example.peertutoring.utils.SoundManager;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * US-23: Buy Tokens / Load Wallet screen.
 *
 * Token packages are defined as {@code PACKAGES[i] = {tokens, price_rs, full_price_rs}}.
 * {@code full_price_rs - price_rs} is the saving displayed on discounted bundles.
 *
 * Static helper methods ({@link #isValidCardNumber}, {@link #isValidExpiry},
 * {@link #detectCardType}) are unit-tested in BuyTokensUnitTest and must stay public + static.
 */
public class BuyTokensActivity extends AppCompatActivity {

    /**
     * Package definitions: each row is {tokens, discountedPriceRs, fullPriceRs}.
     * Public so BuyTokensUnitTest can access the values directly.
     */
    public static final int[][] PACKAGES = {
            {  50,  500,  500},   // Starter  — Rs. 500  (no discount)
            { 100,  900, 1000},   // Standard — Rs. 900  (save Rs. 100)
            { 250, 2000, 2500},   // Pro      — Rs. 2000 (save Rs. 500)
            { 500, 3500, 5000},   // Premium  — Rs. 3500 (save Rs. 1500)
    };

    /** Display labels aligned 1:1 with PACKAGES rows. */
    public static final String[] PACKAGE_NAMES = {"Starter", "Standard", "Pro", "Premium"};

    private static final String[] PACKAGE_EMOJIS = {"🌱", "⭐", "🔥", "💎"};

    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    private TextView tvCurrentBalance;
    private LinearLayout layoutPackages;
    private Button btnPurchase;

    private int selectedPackageIndex = -1;
    private MaterialCardView[] packageCards;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buy_tokens);

        db          = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        tvCurrentBalance = findViewById(R.id.tvCurrentBalance);
        layoutPackages   = findViewById(R.id.layoutPackages);
        btnPurchase      = findViewById(R.id.btnPurchase);

        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        buildPackageCards();
        loadBalance();

        if (btnPurchase != null) {
            btnPurchase.setEnabled(false);
            btnPurchase.setAlpha(0.4f);
            btnPurchase.setOnClickListener(v -> {
                SoundManager.playClick(this);
                showConfirmDialog();
            });
        }
    }

    // ── Balance display ───────────────────────────────────────

    private void loadBalance() {
        if (currentUser == null) { updateBalance(0); return; }
        db.collection("users").document(currentUser.getUid()).get()
                .addOnSuccessListener(doc -> {
                    Long bal = doc.getLong("tokens");
                    updateBalance(bal != null ? bal.intValue() : 0);
                });
    }

    private void updateBalance(int balance) {
        if (tvCurrentBalance != null)
            tvCurrentBalance.setText(
                    NumberFormat.getNumberInstance(Locale.getDefault()).format(balance));
    }

    // ── Package cards (built programmatically) ────────────────

    private void buildPackageCards() {
        if (layoutPackages == null) return;
        layoutPackages.removeAllViews();
        packageCards = new MaterialCardView[PACKAGES.length];

        for (int i = 0; i < PACKAGES.length; i++) {
            final int idx        = i;
            int       tokens     = PACKAGES[i][0];
            int       priceRs    = PACKAGES[i][1];
            int       fullPriceRs= PACKAGES[i][2];
            int       saveRs     = fullPriceRs - priceRs;
            String    label      = PACKAGE_NAMES[i];
            String    emoji      = PACKAGE_EMOJIS[i];

            MaterialCardView card = new MaterialCardView(this);
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cp.setMargins(0, 0, 0, dp(14));
            card.setLayoutParams(cp);
            card.setRadius(dp(20));
            card.setCardElevation(dp(2));
            card.setCardBackgroundColor(0xFFFFFFFF);
            card.setStrokeWidth(dp(1));
            card.setStrokeColor(0xFFE4D5FF);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(18), dp(16), dp(18), dp(16));

            // Emoji badge
            MaterialCardView badge = new MaterialCardView(this);
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(dp(56), dp(56));
            bp.setMarginEnd(dp(16));
            badge.setLayoutParams(bp);
            badge.setRadius(dp(16));
            badge.setCardElevation(0);
            badge.setCardBackgroundColor(badgeColor(i));
            TextView tvEmoji = new TextView(this);
            tvEmoji.setText(emoji);
            tvEmoji.setTextSize(24f);
            tvEmoji.setGravity(Gravity.CENTER);
            tvEmoji.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            badge.addView(tvEmoji);
            row.addView(badge);

            // Info column
            LinearLayout info = new LinearLayout(this);
            info.setOrientation(LinearLayout.VERTICAL);
            info.setLayoutParams(new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView tvLabel = new TextView(this);
            tvLabel.setText(label);
            tvLabel.setTextColor(0xFF071A3D);
            tvLabel.setTextSize(16f);
            tvLabel.setTypeface(null, Typeface.BOLD);
            info.addView(tvLabel);

            TextView tvTokens = new TextView(this);
            tvTokens.setText("🪙 " + NumberFormat.getNumberInstance().format(tokens) + " tokens");
            tvTokens.setTextColor(0xFF8A2EFF);
            tvTokens.setTextSize(13f);
            info.addView(tvTokens);

            if (saveRs > 0) {
                TextView tvSave = new TextView(this);
                tvSave.setText("Save Rs. " + saveRs);
                tvSave.setTextColor(0xFF00C853);
                tvSave.setTextSize(12f);
                tvSave.setTypeface(null, Typeface.BOLD);
                info.addView(tvSave);
            }
            row.addView(info);

            // Price column
            LinearLayout priceCol = new LinearLayout(this);
            priceCol.setOrientation(LinearLayout.VERTICAL);
            priceCol.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);

            TextView tvPrice = new TextView(this);
            tvPrice.setText("Rs. " + priceRs);
            tvPrice.setTextColor(0xFF071A3D);
            tvPrice.setTextSize(20f);
            tvPrice.setTypeface(null, Typeface.BOLD);
            tvPrice.setGravity(Gravity.END);
            priceCol.addView(tvPrice);

            if (saveRs > 0) {
                TextView tvOrig = new TextView(this);
                tvOrig.setText("Rs. " + fullPriceRs);
                tvOrig.setTextColor(0xFF8B97A8);
                tvOrig.setTextSize(12f);
                tvOrig.setGravity(Gravity.END);
                tvOrig.setPaintFlags(tvOrig.getPaintFlags()
                        | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                priceCol.addView(tvOrig);
            }
            row.addView(priceCol);

            card.addView(row);
            card.setOnClickListener(v -> selectPackage(idx));
            packageCards[i] = card;
            layoutPackages.addView(card);
        }
    }

    private void selectPackage(int idx) {
        SoundManager.playClick(this);
        selectedPackageIndex = idx;

        for (int i = 0; i < packageCards.length; i++) {
            if (packageCards[i] == null) continue;
            boolean sel = (i == idx);
            packageCards[i].setStrokeColor(sel ? 0xFF8A2EFF : 0xFFE4D5FF);
            packageCards[i].setStrokeWidth(sel ? dp(2) : dp(1));
            packageCards[i].setCardBackgroundColor(sel ? 0xFFF9F5FF : 0xFFFFFFFF);
        }

        if (btnPurchase != null) {
            int tokens  = PACKAGES[idx][0];
            int priceRs = PACKAGES[idx][1];
            btnPurchase.setText("Buy " + NumberFormat.getNumberInstance().format(tokens)
                    + " Tokens  •  Rs. " + priceRs);
            btnPurchase.setEnabled(true);
            btnPurchase.setAlpha(1.0f);
        }
    }

    // ── Purchase ──────────────────────────────────────────────

    private void showConfirmDialog() {
        if (selectedPackageIndex < 0) return;
        int    tokens  = PACKAGES[selectedPackageIndex][0];
        int    priceRs = PACKAGES[selectedPackageIndex][1];
        String label   = PACKAGE_NAMES[selectedPackageIndex];

        new AlertDialog.Builder(this)
                .setTitle("Confirm Purchase")
                .setMessage(label + " Package\n\n"
                        + "🪙 " + NumberFormat.getNumberInstance().format(tokens)
                        + " tokens\nRs. " + priceRs + "\n\n"
                        + "Tokens will be added to your account immediately.")
                .setPositiveButton("Confirm & Pay", (d, w) -> confirmPurchase(tokens, priceRs, label))
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Credits tokens to the student's Firestore balance and records the transaction.
     * Uses FieldValue.increment for an atomic credit — no negative-balance risk on adds.
     * In production, call ONLY after a successful payment-processor callback.
     */
    private void confirmPurchase(int tokens, int priceRs, String label) {
        if (currentUser == null) {
            Toast.makeText(this, "Please sign in first.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (btnPurchase != null) {
            btnPurchase.setEnabled(false);
            btnPurchase.setText("Processing...");
        }

        // Atomic increment — no stale-read risk for a credit operation
        db.collection("users").document(currentUser.getUid())
                .update("tokens", FieldValue.increment(tokens))
                .addOnSuccessListener(u -> {
                    recordTransaction(tokens, priceRs, label);
                    SoundManager.playSuccess(this);
                    db.collection("users").document(currentUser.getUid()).get()
                            .addOnSuccessListener(doc -> {
                                Long bal = doc.getLong("tokens");
                                long newBalance = bal != null ? bal : tokens;
                                navigateToSuccess(tokens, newBalance, label);
                            })
                            .addOnFailureListener(e -> navigateToSuccess(tokens, tokens, label));
                })
                .addOnFailureListener(e -> {
                    SoundManager.playError(this);
                    Toast.makeText(this, "Purchase failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    resetButton();
                });
    }

    private void recordTransaction(int tokens, int priceRs, String label) {
        Map<String, Object> tx = new HashMap<>();
        tx.put("studentUid",  currentUser.getUid());
        tx.put("type",        "purchase");
        tx.put("label",       label);
        tx.put("tokens",      tokens);
        tx.put("priceRs",     priceRs);
        tx.put("createdAt",   FieldValue.serverTimestamp());
        db.collection("tokenPurchases").add(tx);
    }

    private void navigateToSuccess(int tokens, long newBalance, String label) {
        Intent intent = new Intent(this, PurchaseSuccessActivity.class);
        intent.putExtra("tokens",     tokens);
        intent.putExtra("newBalance", newBalance);
        intent.putExtra("label",      label);
        startActivity(intent);
        finish();
    }

    private void resetButton() {
        if (btnPurchase != null) {
            btnPurchase.setEnabled(true);
            btnPurchase.setText("Complete Purchase");
        }
    }

    // ── Static validators (tested by BuyTokensUnitTest) ──────

    /**
     * Returns true iff the card number contains exactly 16 digits (spaces ignored).
     */
    public static boolean isValidCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.isEmpty()) return false;
        String digits = cardNumber.replace(" ", "");
        return digits.length() == 16 && digits.matches("\\d+");
    }

    /**
     * Returns true iff expiry is in MM/YY format, month is 1–12, and the
     * date is not in the past (year interpreted as 20YY).
     */
    public static boolean isValidExpiry(String expiry) {
        if (expiry == null || expiry.isEmpty()) return false;
        String[] parts = expiry.split("/");
        if (parts.length != 2 || parts[0].length() != 2 || parts[1].length() != 2) return false;
        try {
            int month = Integer.parseInt(parts[0]);
            int year  = Integer.parseInt(parts[1]); // 2-digit: 26 = 2026
            if (month < 1 || month > 12) return false;
            Calendar now = Calendar.getInstance();
            int nowYear  = now.get(Calendar.YEAR) % 100;
            int nowMonth = now.get(Calendar.MONTH) + 1; // Calendar.MONTH is 0-based
            if (year < nowYear) return false;
            if (year == nowYear && month < nowMonth) return false;
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Returns "Visa" for cards starting with 4, "Mastercard" for cards starting
     * with 5, and "" for all other prefixes or null/empty input.
     */
    public static String detectCardType(String cardNumber) {
        if (cardNumber == null || cardNumber.isEmpty()) return "";
        String clean = cardNumber.replace(" ", "");
        if (clean.startsWith("4")) return "Visa";
        if (clean.startsWith("5")) return "Mastercard";
        return "";
    }

    // ── Helpers ───────────────────────────────────────────────

    private int badgeColor(int idx) {
        int[] colors = {0xFFEEF2FF, 0xFFFFF8E1, 0xFFFFEBEE, 0xFFF3EEFF};
        return colors[idx % colors.length];
    }

    private int dp(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
