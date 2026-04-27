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
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * US-25: Buy Tokens screen.
 *
 * Students purchase token bundles to use for booking sessions.
 * Conversion rate: 10 tokens = $1 USD.
 *
 * Packages:
 *  Starter  — 100  tokens = $10
 *  Popular  — 500  tokens = $45  (10% saving)
 *  Value    — 1000 tokens = $80  (20% saving)
 *  Premium  — 2500 tokens = $175 (30% saving)
 *
 * NOTE: Real payment integration (Stripe / Google Pay) would wrap
 * confirmPurchase(). The UI and Firestore token-credit logic are
 * production-ready; only the payment step is simulated.
 */
public class BuyTokensActivity extends AppCompatActivity {

    private static final int TOKENS_PER_DOLLAR = 10;

    // {tokens, priceCents, label, discountPct, emoji}
    private static final Object[][] PACKAGES = {
            {100,    1000, "Starter",  0,  "🌱"},
            {500,    4500, "Popular",  10, "⭐"},
            {1000,   8000, "Value",    20, "🔥"},
            {2500,  17500, "Premium",  30, "💎"},
    };

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

    // ── Balance ───────────────────────────────────────────────

    private void loadBalance() {
        if (currentUser == null) { updateBalance(100); return; }
        db.collection("users").document(currentUser.getUid()).get()
                .addOnSuccessListener(doc -> {
                    Long bal = doc.getLong("tokens");
                    updateBalance(bal != null ? bal.intValue() : 100);
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
            final int idx      = i;
            int    tokens      = (int)    PACKAGES[i][0];
            int    priceCents  = (int)    PACKAGES[i][1];
            String label       = (String) PACKAGES[i][2];
            int    discount    = (int)    PACKAGES[i][3];
            String emoji       = (String) PACKAGES[i][4];

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

            if (discount > 0) {
                TextView tvSave = new TextView(this);
                tvSave.setText("Save " + discount + "%");
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
            tvPrice.setText(String.format(Locale.getDefault(), "$%.2f", priceCents / 100.0));
            tvPrice.setTextColor(0xFF071A3D);
            tvPrice.setTextSize(20f);
            tvPrice.setTypeface(null, Typeface.BOLD);
            tvPrice.setGravity(Gravity.END);
            priceCol.addView(tvPrice);

            if (discount > 0) {
                int origCents = tokens * (100 / TOKENS_PER_DOLLAR) * 100 / 100;
                // Simple: undiscounted = tokens / 10 * 100 cents
                int undiscountedCents = (tokens / TOKENS_PER_DOLLAR) * 100;
                TextView tvOrig = new TextView(this);
                tvOrig.setText(String.format(Locale.getDefault(),
                        "$%.2f", undiscountedCents / 100.0));
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
            int    tokens     = (int)    PACKAGES[idx][0];
            int    priceCents = (int)    PACKAGES[idx][1];
            String priceStr   = String.format(Locale.getDefault(), "$%.2f", priceCents / 100.0);
            btnPurchase.setText("Buy " + NumberFormat.getNumberInstance().format(tokens)
                    + " Tokens  •  " + priceStr);
            btnPurchase.setEnabled(true);
            btnPurchase.setAlpha(1.0f);
        }
    }

    // ── Purchase ──────────────────────────────────────────────

    private void showConfirmDialog() {
        if (selectedPackageIndex < 0) return;
        int    tokens     = (int)    PACKAGES[selectedPackageIndex][0];
        int    priceCents = (int)    PACKAGES[selectedPackageIndex][1];
        String label      = (String) PACKAGES[selectedPackageIndex][2];
        String priceStr   = String.format(Locale.getDefault(), "$%.2f", priceCents / 100.0);

        new AlertDialog.Builder(this)
                .setTitle("Confirm Purchase")
                .setMessage(label + " Package\n\n"
                        + "🪙 " + NumberFormat.getNumberInstance().format(tokens)
                        + " tokens\n" + priceStr + "\n\n"
                        + "Tokens will be added to your account immediately.")
                .setPositiveButton("Confirm & Pay", (d, w) -> confirmPurchase(tokens, priceCents, label))
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Credits tokens to the student's Firestore balance and records
     * the transaction. In production, call this ONLY after a successful
     * payment callback from your payment processor (Stripe / Google Pay).
     */
    private void confirmPurchase(int tokens, int priceCents, String label) {
        if (currentUser == null) {
            Toast.makeText(this, "Please sign in first.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (btnPurchase != null) {
            btnPurchase.setEnabled(false);
            btnPurchase.setText("Processing...");
        }

        db.collection("users").document(currentUser.getUid()).get()
                .addOnSuccessListener(doc -> {
                    Long current    = doc.getLong("tokens");
                    long newBalance = (current != null ? current : 0L) + tokens;

                    db.collection("users").document(currentUser.getUid())
                            .update("tokens", newBalance)
                            .addOnSuccessListener(u -> {
                                recordTransaction(tokens, priceCents, label);
                                SoundManager.playSuccess(this);
                                navigateToSuccess(tokens, newBalance, label);
                            })
                            .addOnFailureListener(e -> {
                                SoundManager.playError(this);
                                Toast.makeText(this, "Purchase failed: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                                resetButton();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    resetButton();
                });
    }

    private void recordTransaction(int tokens, int priceCents, String label) {
        Map<String, Object> tx = new HashMap<>();
        tx.put("studentUid",  currentUser.getUid());
        tx.put("type",        "purchase");
        tx.put("label",       label);
        tx.put("tokens",      tokens);
        tx.put("priceCents",  priceCents);
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

    // ── Helpers ───────────────────────────────────────────────

    private int badgeColor(int idx) {
        int[] colors = {0xFFEEF2FF, 0xFFFFF8E1, 0xFFFFEBEE, 0xFFF3EEFF};
        return colors[idx % colors.length];
    }

    private int dp(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}