package com.example.peertutoring.ui;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.peertutoring.R;
import com.example.peertutoring.utils.SoundManager;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.concurrent.TimeUnit;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * US-25: Buy Tokens screen — Students only.
 *
 * Packages: Starter / Popular / Value / Premium
 * Payment methods:
 *   Card payments: Visa, Mastercard, UnionPay, Credit/Debit
 *   Mobile wallets: JazzCash, EasyPaisa, SadaPay
 *
 * Card validation rules:
 *   Visa       → starts with 4,  16 digits
 *   Mastercard → starts with 51-55 or 2221-2720, 16 digits
 *   UnionPay   → starts with 62, 16-19 digits
 *   Credit/Debit → any valid 13-19 digit number
 */
public class BuyTokensActivity extends AppCompatActivity {

    private static final int TOKENS_PER_DOLLAR = 10;

    // Token packages {tokens, priceCents, label, discountPct, emoji}
    private static final Object[][] PACKAGES = {
            {100,    1000, "Starter",  0,  "🌱"},
            {500,    4500, "Popular",  10, "⭐"},
            {1000,   8000, "Value",    20, "🔥"},
            {2500,  17500, "Premium",  30, "💎"},
    };

    // Payment methods {id, label, subtitle, emoji, bgColor}
    private static final String[][] PAY_METHODS = {
            {"visa",       "Visa",        "Starts with 4",    "💳", "#1A1F71"},
            {"mastercard", "Mastercard",  "Starts with 51-55","💳", "#EB001B"},
            {"unionpay",   "UnionPay",    "Starts with 62",   "💳", "#E21836"},
            {"card",       "Credit/Debit","Any card",         "💳", "#4B5D7A"},
            {"jazzcash",   "JazzCash",    "Mobile wallet",    "📱", "#EF4444"},
            {"easypaisa",  "EasyPaisa",   "Mobile wallet",    "📱", "#00A651"},
            {"sadapay",    "SadaPay",     "Mobile wallet",    "📱", "#8A2EFF"},
    };

    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    private TextView tvCurrentBalance;
    private LinearLayout layoutPackages, layoutPayMethods;
    private Button btnPurchase;

    private int selectedPackageIndex = -1;
    private int selectedPayMethodIndex = -1;
    private MaterialCardView[] packageCards;
    private MaterialCardView[] payMethodCards;

    // Card input fields (shown for card methods)
    private LinearLayout layoutCardInputs;
    private EditText etCardNumber, etCardHolder, etExpiry, etCVV;
    // Mobile wallet input (shown for wallet methods)
    private LinearLayout layoutWalletInput;
    private EditText etPhoneNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buy_tokens);

        db          = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        tvCurrentBalance  = findViewById(R.id.tvCurrentBalance);
        layoutPackages    = findViewById(R.id.layoutPackages);
        layoutPayMethods  = findViewById(R.id.layoutPayMethods);
        layoutCardInputs  = findViewById(R.id.layoutCardInputs);
        layoutWalletInput = findViewById(R.id.layoutWalletInput);
        btnPurchase       = findViewById(R.id.btnPurchase);
        etCardNumber      = findViewById(R.id.etCardNumber);
        etCardHolder      = findViewById(R.id.etCardHolder);
        etExpiry          = findViewById(R.id.etExpiry);
        etCVV             = findViewById(R.id.etCVV);
        etPhoneNumber     = findViewById(R.id.etPhoneNumber);

        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        buildPackageCards();
        buildPayMethodCards();
        setupCardFormatting();
        loadBalance();

        if (btnPurchase != null) {
            btnPurchase.setEnabled(false);
            btnPurchase.setAlpha(0.4f);
            btnPurchase.setOnClickListener(v -> {
                SoundManager.playClick(this);
                validateAndPurchase();
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

    // ── Package cards ─────────────────────────────────────────

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
            cp.setMargins(0, 0, 0, dp(12));
            card.setLayoutParams(cp);
            card.setRadius(dp(16));
            card.setCardElevation(dp(2));
            card.setCardBackgroundColor(0xFFFFFFFF);
            card.setStrokeWidth(dp(1));
            card.setStrokeColor(0xFFE4D5FF);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(16), dp(14), dp(16), dp(14));

            // Emoji badge
            MaterialCardView badge = new MaterialCardView(this);
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(dp(52), dp(52));
            bp.setMarginEnd(dp(14));
            badge.setLayoutParams(bp);
            badge.setRadius(dp(14));
            badge.setCardElevation(0);
            badge.setCardBackgroundColor(badgeColor(i));
            TextView tvEmoji = new TextView(this);
            tvEmoji.setText(emoji);
            tvEmoji.setTextSize(22f);
            tvEmoji.setGravity(Gravity.CENTER);
            tvEmoji.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            badge.addView(tvEmoji);
            row.addView(badge);

            // Info
            LinearLayout info = new LinearLayout(this);
            info.setOrientation(LinearLayout.VERTICAL);
            info.setLayoutParams(new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView tvLabel = new TextView(this);
            tvLabel.setText(label);
            tvLabel.setTextColor(0xFF071A3D);
            tvLabel.setTextSize(15f);
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

            // Price
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
                int undiscountedCents = (tokens / TOKENS_PER_DOLLAR) * 100;
                TextView tvOrig = new TextView(this);
                tvOrig.setText(String.format(Locale.getDefault(), "$%.2f", undiscountedCents / 100.0));
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
        updatePurchaseButton();
    }

    // ── Payment method cards ──────────────────────────────────

    private void buildPayMethodCards() {
        if (layoutPayMethods == null) return;
        layoutPayMethods.removeAllViews();
        payMethodCards = new MaterialCardView[PAY_METHODS.length];

        // Two-column grid layout
        LinearLayout row = null;
        for (int i = 0; i < PAY_METHODS.length; i++) {
            final int idx    = i;
            String[] m       = PAY_METHODS[i];
            String methodId  = m[0];
            String label     = m[1];
            String subtitle  = m[2];
            String emoji     = m[3];
            int    bgColor   = Color.parseColor(m[4]);

            if (i % 2 == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                rp.setMargins(0, 0, 0, dp(10));
                row.setLayoutParams(rp);
                layoutPayMethods.addView(row);
            }

            MaterialCardView card = new MaterialCardView(this);
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                    0, dp(90), 1f);
            if (i % 2 == 0) cp.setMarginEnd(dp(8));
            card.setLayoutParams(cp);
            card.setRadius(dp(16));
            card.setCardElevation(dp(2));
            card.setCardBackgroundColor(0xFFFFFFFF);
            card.setStrokeWidth(dp(1));
            card.setStrokeColor(0xFFE4D5FF);

            LinearLayout inner = new LinearLayout(this);
            inner.setOrientation(LinearLayout.VERTICAL);
            inner.setGravity(Gravity.CENTER);
            inner.setPadding(dp(10), dp(12), dp(10), dp(12));

            // Colored emoji badge
            MaterialCardView iconBadge = new MaterialCardView(this);
            LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(dp(36), dp(36));
            ip.setMargins(0, 0, 0, dp(6));
            iconBadge.setLayoutParams(ip);
            iconBadge.setRadius(dp(10));
            iconBadge.setCardElevation(0);
            iconBadge.setCardBackgroundColor(bgColor);
            TextView tvIcon = new TextView(this);
            tvIcon.setText(emoji);
            tvIcon.setTextSize(16f);
            tvIcon.setGravity(Gravity.CENTER);
            tvIcon.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            iconBadge.addView(tvIcon);
            inner.addView(iconBadge);

            TextView tvLabel = new TextView(this);
            tvLabel.setText(label);
            tvLabel.setTextColor(0xFF071A3D);
            tvLabel.setTextSize(12f);
            tvLabel.setTypeface(null, Typeface.BOLD);
            tvLabel.setGravity(Gravity.CENTER);
            inner.addView(tvLabel);

            TextView tvSub = new TextView(this);
            tvSub.setText(subtitle);
            tvSub.setTextColor(0xFF8B97A8);
            tvSub.setTextSize(10f);
            tvSub.setGravity(Gravity.CENTER);
            inner.addView(tvSub);

            card.addView(inner);
            card.setOnClickListener(v -> selectPayMethod(idx));
            payMethodCards[i] = card;
            if (row != null) row.addView(card);
        }
    }

    private void selectPayMethod(int idx) {
        SoundManager.playClick(this);
        selectedPayMethodIndex = idx;

        for (int i = 0; i < payMethodCards.length; i++) {
            if (payMethodCards[i] == null) continue;
            boolean sel = (i == idx);
            payMethodCards[i].setStrokeColor(sel ? 0xFF8A2EFF : 0xFFE4D5FF);
            payMethodCards[i].setStrokeWidth(sel ? dp(2) : dp(1));
            payMethodCards[i].setCardBackgroundColor(sel ? 0xFFF9F5FF : 0xFFFFFFFF);
        }

        String methodId = PAY_METHODS[idx][0];
        boolean isWallet = methodId.equals("jazzcash")
                || methodId.equals("easypaisa")
                || methodId.equals("sadapay");

        // Show/hide correct input section
        if (layoutCardInputs  != null) layoutCardInputs.setVisibility(isWallet ? View.GONE : View.VISIBLE);
        if (layoutWalletInput != null) layoutWalletInput.setVisibility(isWallet ? View.VISIBLE : View.GONE);

        // Update card number hint based on method
        if (!isWallet && etCardNumber != null) {
            switch (methodId) {
                case "visa":
                    etCardNumber.setHint("Card Number (starts with 4)");
                    break;
                case "mastercard":
                    etCardNumber.setHint("Card Number (starts with 51-55)");
                    break;
                case "unionpay":
                    etCardNumber.setHint("Card Number (starts with 62)");
                    break;
                default:
                    etCardNumber.setHint("Card Number");
                    break;
            }
        }

        if (isWallet && etPhoneNumber != null) {
            switch (methodId) {
                case "jazzcash":
                    etPhoneNumber.setHint("JazzCash mobile number (03xx-xxxxxxx)");
                    break;
                case "easypaisa":
                    etPhoneNumber.setHint("EasyPaisa mobile number (03xx-xxxxxxx)");
                    break;
                case "sadapay":
                    etPhoneNumber.setHint("SadaPay mobile number (03xx-xxxxxxx)");
                    break;
            }
        }

        updatePurchaseButton();
    }

    // ── Card number formatting ────────────────────────────────

    private void setupCardFormatting() {

        // ── Card number: format XXXX XXXX XXXX XXXX + live prefix validation ──
        if (etCardNumber != null) {
            etCardNumber.addTextChangedListener(new TextWatcher() {
                private boolean isFormatting = false;
                @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
                @Override public void afterTextChanged(Editable s) {}
                @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                    if (isFormatting) return;
                    isFormatting = true;
                    String digits = s.toString().replaceAll("[^\\d]", "");
                    // Enforce max length per network
                    int maxLen = getMaxCardLength();
                    if (digits.length() > maxLen) digits = digits.substring(0, maxLen);
                    // Format groups of 4
                    StringBuilder fmt = new StringBuilder();
                    for (int i = 0; i < digits.length(); i++) {
                        if (i > 0 && i % 4 == 0) fmt.append(" ");
                        fmt.append(digits.charAt(i));
                    }
                    etCardNumber.setText(fmt);
                    etCardNumber.setSelection(fmt.length());
                    isFormatting = false;
                    // Live prefix check — show error as user types
                    validateCardPrefix(digits);
                    updatePurchaseButton();
                }
            });
        }

        // ── Expiry: format MM/YY, block invalid months in real time ──
        if (etExpiry != null) {
            etExpiry.addTextChangedListener(new TextWatcher() {
                private boolean isFormatting = false;
                @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
                @Override public void afterTextChanged(Editable s) {}
                @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                    if (isFormatting) return;
                    isFormatting = true;
                    String digits = s.toString().replaceAll("[^\\d]", "");
                    if (digits.length() > 4) digits = digits.substring(0, 4);
                    String formatted = digits;
                    if (digits.length() >= 2) {
                        // Validate month in real time
                        int month = Integer.parseInt(digits.substring(0, 2));
                        if (month < 1 || month > 12) {
                            etExpiry.setError("Month must be 01–12");
                        } else {
                            etExpiry.setError(null);
                        }
                        formatted = digits.substring(0, 2) + "/" + digits.substring(2);
                    }
                    etExpiry.setText(formatted);
                    etExpiry.setSelection(formatted.length());
                    isFormatting = false;
                    updatePurchaseButton();
                }
            });
        }

        // ── CVV: digits only, 3-4 chars max ──
        if (etCVV != null) {
            etCVV.setFilters(new InputFilter[]{
                    new InputFilter.LengthFilter(4),
                    // Allow only digits
                    (source, start, end, dest, dstart, dend) -> {
                        for (int i = start; i < end; i++) {
                            if (!Character.isDigit(source.charAt(i))) return "";
                        }
                        return null;
                    }
            });
            etCVV.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
                @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
                @Override public void afterTextChanged(Editable s) {
                    String cvv = s.toString();
                    if (!cvv.isEmpty() && cvv.length() < 3) {
                        etCVV.setError("CVV must be 3–4 digits");
                    } else {
                        etCVV.setError(null);
                    }
                    updatePurchaseButton();
                }
            });
        }

        // ── Cardholder: letters and spaces only, min 2 chars ──
        if (etCardHolder != null) {
            etCardHolder.setFilters(new InputFilter[]{
                    new InputFilter.LengthFilter(50),
                    (source, start, end, dest, dstart, dend) -> {
                        for (int i = start; i < end; i++) {
                            char ch = source.charAt(i);
                            if (!Character.isLetter(ch) && ch != ' ' && ch != '-') return "";
                        }
                        return null;
                    }
            });
            etCardHolder.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
                @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
                @Override public void afterTextChanged(Editable s) {
                    String name = s.toString().trim();
                    if (!name.isEmpty() && name.length() < 3) {
                        etCardHolder.setError("Enter full cardholder name");
                    } else if (!name.isEmpty() && !name.contains(" ")) {
                        etCardHolder.setError("Enter first and last name");
                    } else {
                        etCardHolder.setError(null);
                    }
                    updatePurchaseButton();
                }
            });
        }

        // ── Phone number: digits/dashes only, Pakistani format ──
        if (etPhoneNumber != null) {
            etPhoneNumber.setFilters(new InputFilter[]{
                    new InputFilter.LengthFilter(13),
                    (source, start, end, dest, dstart, dend) -> {
                        for (int i = start; i < end; i++) {
                            char ch = source.charAt(i);
                            if (!Character.isDigit(ch) && ch != '-') return "";
                        }
                        return null;
                    }
            });
            etPhoneNumber.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
                @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
                @Override public void afterTextChanged(Editable s) {
                    String phone = s.toString().replaceAll("[^\\d]", "");
                    if (phone.length() >= 2 && !phone.startsWith("03")) {
                        etPhoneNumber.setError("Must start with 03 (e.g. 0300-1234567)");
                    } else if (phone.length() > 0 && phone.length() < 11) {
                        etPhoneNumber.setError("Mobile number must be 11 digits (e.g. 03001234567)");
                    } else {
                        etPhoneNumber.setError(null);
                    }
                    updatePurchaseButton();
                }
            });
        }
    }

    /** Returns max allowed card digits based on selected network. */
    private int getMaxCardLength() {
        if (selectedPayMethodIndex < 0) return 19;
        String methodId = PAY_METHODS[selectedPayMethodIndex][0];
        if (methodId.equals("unionpay")) return 19;
        return 16; // Visa, Mastercard, general card
    }

    /** Shows inline error if card prefix doesn't match selected network while typing. */
    private void validateCardPrefix(String digits) {
        if (etCardNumber == null || digits.length() < 2 || selectedPayMethodIndex < 0) return;
        String methodId = PAY_METHODS[selectedPayMethodIndex][0];
        String error = null;
        switch (methodId) {
            case "visa":
                if (!digits.startsWith("4"))
                    error = "Visa cards must start with 4";
                break;
            case "mastercard":
                if (digits.length() >= 2) {
                    int p2 = Integer.parseInt(digits.substring(0, 2));
                    boolean ok = (p2 >= 51 && p2 <= 55);
                    if (!ok && digits.length() >= 4) {
                        int p4 = Integer.parseInt(digits.substring(0, 4));
                        ok = (p4 >= 2221 && p4 <= 2720);
                    }
                    if (!ok) error = "Mastercard starts with 51–55 or 2221–2720";
                }
                break;
            case "unionpay":
                if (!digits.startsWith("62"))
                    error = "UnionPay cards start with 62";
                break;
        }
        etCardNumber.setError(error);
    }

    // ── Validation ────────────────────────────────────────────

    private void updatePurchaseButton() {
        boolean ready = selectedPackageIndex >= 0
                && selectedPayMethodIndex >= 0
                && isPaymentInputFilled();
        if (btnPurchase != null) {
            btnPurchase.setEnabled(ready);
            btnPurchase.setAlpha(ready ? 1.0f : 0.4f);
            if (ready && selectedPackageIndex >= 0) {
                int    tokens     = (int)    PACKAGES[selectedPackageIndex][0];
                int    priceCents = (int)    PACKAGES[selectedPackageIndex][1];
                String payLabel   = PAY_METHODS[selectedPayMethodIndex][1];
                btnPurchase.setText("Pay " + String.format(Locale.getDefault(),
                        "$%.2f", priceCents / 100.0) + " via " + payLabel);
            } else {
                btnPurchase.setText("Select package & payment");
            }
        }
    }

    private boolean isPaymentInputFilled() {
        if (selectedPayMethodIndex < 0) return false;
        String methodId = PAY_METHODS[selectedPayMethodIndex][0];
        boolean isWallet = methodId.equals("jazzcash")
                || methodId.equals("easypaisa")
                || methodId.equals("sadapay");

        if (isWallet) {
            String phone = etPhoneNumber != null
                    ? etPhoneNumber.getText().toString().replaceAll("[^\\d]", "") : "";
            // Revalidate directly — don't rely on getError() state
            boolean lengthOk = phone.length() == 11;
            boolean prefixOk = phone.startsWith("03");
            return lengthOk && prefixOk;
        } else {
            String cardNum = etCardNumber != null
                    ? etCardNumber.getText().toString().replaceAll(" ", "") : "";
            String holder  = etCardHolder != null
                    ? etCardHolder.getText().toString().trim() : "";
            String expiry  = etExpiry != null ? etExpiry.getText().toString() : "";
            String cvv     = etCVV    != null ? etCVV.getText().toString()    : "";

            // Check all lengths
            int minCard = methodId.equals("unionpay") ? 16 : 16;
            int maxCard = methodId.equals("unionpay") ? 19 : 16;
            boolean cardOk   = cardNum.length() >= minCard && cardNum.length() <= maxCard;
            boolean holderOk = holder.length() >= 3 && holder.contains(" ");
            boolean expiryOk = expiry.length() == 5 && expiry.contains("/");
            boolean cvvOk    = cvv.length() >= 3 && cvv.length() <= 4;

            // Only check lengths here — prefix/Luhn/expiry errors shown on submit
            return cardOk && holderOk && expiryOk && cvvOk;
        }
    }

    /**
     * Validates card number prefix matches selected network.
     * Visa:       starts with 4
     * Mastercard: starts with 51-55 or 2221-2720
     * UnionPay:   starts with 62
     */
    private String validateCardNumber(String digits) {
        if (selectedPayMethodIndex < 0) return "Please select a payment method.";
        String methodId = PAY_METHODS[selectedPayMethodIndex][0];

        // ── Prefix + length checks ──
        switch (methodId) {
            case "visa":
                if (!digits.startsWith("4"))
                    return "Visa cards must start with 4. Your number starts with '"
                            + (digits.isEmpty() ? "?" : digits.charAt(0)) + "'.";
                if (digits.length() != 16)
                    return "Visa cards must be exactly 16 digits (you entered " + digits.length() + ").";
                break;
            case "mastercard":
                if (digits.length() < 2)
                    return "Enter your Mastercard number.";
                int p2 = Integer.parseInt(digits.substring(0, 2));
                int p4 = digits.length() >= 4 ? Integer.parseInt(digits.substring(0, 4)) : 0;
                boolean isMC = (p2 >= 51 && p2 <= 55) || (p4 >= 2221 && p4 <= 2720);
                if (!isMC)
                    return "Mastercard numbers start with 51–55 or 2221–2720. "
                            + "Yours starts with " + digits.substring(0, Math.min(4, digits.length())) + ".";
                if (digits.length() != 16)
                    return "Mastercard must be exactly 16 digits (you entered " + digits.length() + ").";
                break;
            case "unionpay":
                if (!digits.startsWith("62"))
                    return "UnionPay cards must start with 62.";
                if (digits.length() < 16 || digits.length() > 19)
                    return "UnionPay cards are 16–19 digits (you entered " + digits.length() + ").";
                break;
            case "card":
                if (digits.length() < 13 || digits.length() > 19)
                    return "Enter a valid card number (13–19 digits).";
                break;
        }

        // ── Luhn algorithm to verify card number is mathematically valid ──
        if (!luhnCheck(digits)) {
            return "This card number is invalid. Please check and try again.";
        }

        return null; // all good
    }

    /**
     * Luhn algorithm — standard check used by all card networks.
     * Catches random/made-up card numbers instantly.
     */
    private boolean luhnCheck(String digits) {
        int sum = 0;
        boolean alternate = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int n = Character.getNumericValue(digits.charAt(i));
            if (alternate) {
                n *= 2;
                if (n > 9) n -= 9;
            }
            sum += n;
            alternate = !alternate;
        }
        return sum % 10 == 0;
    }

    private String validateExpiry(String expiry) {
        if (expiry.length() != 5 || !expiry.contains("/"))
            return "Enter expiry as MM/YY (e.g. 08/26).";
        String[] parts = expiry.split("/");
        if (parts.length != 2 || parts[0].length() != 2 || parts[1].length() != 2)
            return "Enter expiry as MM/YY (e.g. 08/26).";
        int month, year;
        try {
            month = Integer.parseInt(parts[0]);
            year  = Integer.parseInt(parts[1]) + 2000;
        } catch (NumberFormatException e) {
            return "Invalid expiry date.";
        }
        if (month < 1 || month > 12)
            return "Month must be between 01 and 12.";
        if (year > 2040)
            return "Expiry year seems too far in the future.";
        java.util.Calendar cal = java.util.Calendar.getInstance();
        int curYear  = cal.get(java.util.Calendar.YEAR);
        int curMonth = cal.get(java.util.Calendar.MONTH) + 1;
        if (year < curYear || (year == curYear && month < curMonth))
            return "This card has expired. Please use a valid card.";
        return null;
    }

    private String validatePhone(String phone) {
        String digits = phone.replaceAll("[^\\d]", "");
        if (digits.isEmpty())
            return "Please enter your mobile number.";
        if (!digits.startsWith("03"))
            return "Mobile number must start with 03 (e.g. 0300-1234567).";
        if (digits.length() != 11)
            return "Mobile number must be exactly 11 digits (e.g. 03001234567).";
        // Pakistani operators: 030x, 031x, 032x, 033x, 034x, 035x
        String thirdDigit = digits.substring(2, 3);
        java.util.Set<String> validOps = new java.util.HashSet<>(
                java.util.Arrays.asList("0","1","2","3","4","5","6","7","8","9"));
        if (!validOps.contains(thirdDigit))
            return "Invalid mobile number format.";
        return null;
    }

    // ── Purchase flow ─────────────────────────────────────────

    private void validateAndPurchase() {
        if (selectedPackageIndex < 0) {
            Toast.makeText(this, "Please select a token package.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedPayMethodIndex < 0) {
            Toast.makeText(this, "Please select a payment method.", Toast.LENGTH_SHORT).show();
            return;
        }

        String methodId = PAY_METHODS[selectedPayMethodIndex][0];
        boolean isWallet = methodId.equals("jazzcash")
                || methodId.equals("easypaisa")
                || methodId.equals("sadapay");

        if (isWallet) {
            // Validate phone first
            String phone = etPhoneNumber != null
                    ? etPhoneNumber.getText().toString().trim() : "";
            String phoneError = validatePhone(phone);
            if (phoneError != null) {
                SoundManager.playError(this);
                if (etPhoneNumber != null) etPhoneNumber.setError(phoneError);
                return;
            }
            // Phone valid → send OTP
            sendOtpAndVerify();
            return;
        } else {
            // Validate card
            String cardNum = etCardNumber != null
                    ? etCardNumber.getText().toString().replaceAll(" ", "") : "";
            String cardError = validateCardNumber(cardNum);
            if (cardError != null) {
                SoundManager.playError(this);
                if (etCardNumber != null) etCardNumber.setError(cardError);
                Toast.makeText(this, cardError, Toast.LENGTH_LONG).show();
                return;
            }

            String expiry = etExpiry != null ? etExpiry.getText().toString() : "";
            String expiryError = validateExpiry(expiry);
            if (expiryError != null) {
                SoundManager.playError(this);
                if (etExpiry != null) etExpiry.setError(expiryError);
                return;
            }

            String cvv = etCVV != null ? etCVV.getText().toString() : "";
            if (cvv.length() < 3) {
                if (etCVV != null) etCVV.setError("Enter a valid CVV.");
                return;
            }

            String holder = etCardHolder != null ? etCardHolder.getText().toString().trim() : "";
            if (holder.length() < 2) {
                if (etCardHolder != null) etCardHolder.setError("Enter the cardholder name.");
                return;
            }
        }

        // All validations passed — send OTP regardless of payment method
        sendOtpAndVerify();
    }

    // ── OTP flow (all payment methods) ───────────────────────

    private String pendingOtp        = null; // used for card OTP (simulated)
    private String pendingVerifId    = null; // used for Firebase Phone Auth (wallet)
    private PhoneAuthProvider.ForceResendingToken resendToken = null;

    /**
     * Simulates sending an OTP to the mobile number.
     * In production, replace with your SMS gateway (Twilio, Firebase Phone Auth, etc.)
     * Generates a random 6-digit OTP, stores it, shows verification dialog.
     */
    private void sendOtpAndVerify() {
        String methodId = PAY_METHODS[selectedPayMethodIndex][0];
        boolean isWallet = methodId.equals("jazzcash")
                || methodId.equals("easypaisa") || methodId.equals("sadapay");
        String payDisplayLabel = PAY_METHODS[selectedPayMethodIndex][1];

        if (isWallet) {
            // ── REAL Firebase Phone Auth for wallet payments ──────────────
            String phone = etPhoneNumber != null
                    ? etPhoneNumber.getText().toString().replaceAll("[^\\d]", "") : "";
            // Convert Pakistani number to E.164: 03001234567 → +923001234567
            String e164 = "+92" + phone.substring(1);
            String contactHint = phone.substring(0, 4) + "***" + phone.substring(phone.length() - 4);

            if (btnPurchase != null) {
                btnPurchase.setEnabled(false);
                btnPurchase.setText("Sending OTP...");
            }

            PhoneAuthOptions.Builder builder = PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
                    .setPhoneNumber(e164)
                    .setTimeout(60L, TimeUnit.SECONDS)
                    .setActivity(this)
                    .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                        @Override
                        public void onVerificationCompleted(PhoneAuthCredential credential) {
                            // Auto-verification (device auto-reads SMS on some phones)
                            if (btnPurchase != null) {
                                btnPurchase.setEnabled(true);
                                updatePurchaseButton();
                            }
                            Toast.makeText(BuyTokensActivity.this,
                                    "Phone verified automatically!", Toast.LENGTH_SHORT).show();
                            showConfirmDialog();
                        }

                        @Override
                        public void onVerificationFailed(FirebaseException e) {
                            if (btnPurchase != null) {
                                btnPurchase.setEnabled(true);
                                updatePurchaseButton();
                            }
                            String msg = e.getMessage() != null ? e.getMessage() : "";
                            // If Phone Auth is not enabled → fall back to simulation
                            if (msg.contains("not allowed") || msg.contains("operation-not-allowed")
                                    || msg.contains("OPERATION_NOT_ALLOWED")
                                    || msg.contains("CONFIGURATION_NOT_FOUND")) {
                                Toast.makeText(BuyTokensActivity.this,
                                        "Using demo OTP (Firebase Phone Auth not enabled)",
                                        Toast.LENGTH_SHORT).show();
                                fallbackToSimulatedOtp(contactHint, payDisplayLabel);
                            } else {
                                SoundManager.playError(BuyTokensActivity.this);
                                if (msg.contains("TOO_LONG") || msg.contains("INVALID_PHONE"))
                                    msg = "Invalid phone number. Use format 03xx-xxxxxxx.";
                                else if (msg.contains("quota"))
                                    msg = "SMS quota exceeded. Please try again later.";
                                else if (msg.contains("BLOCKED"))
                                    msg = "Too many attempts. Please wait and try again.";
                                else
                                    msg = "OTP failed: " + msg;
                                Toast.makeText(BuyTokensActivity.this, msg, Toast.LENGTH_LONG).show();
                            }
                        }

                        @Override
                        public void onCodeSent(String verificationId,
                                               PhoneAuthProvider.ForceResendingToken token) {
                            pendingVerifId = verificationId;
                            resendToken    = token;
                            if (btnPurchase != null) {
                                btnPurchase.setEnabled(true);
                                updatePurchaseButton();
                            }
                            showOtpDialog(contactHint, payDisplayLabel, true);
                        }
                    });

            // Use resend token if available (Resend OTP tapped)
            if (resendToken != null) builder.setForceResendingToken(resendToken);
            PhoneAuthProvider.verifyPhoneNumber(builder.build());

        } else {
            // ── Simulated OTP for card payments ──────────────────────────
            String cardNum = etCardNumber != null
                    ? etCardNumber.getText().toString().replaceAll(" ", "") : "";
            String contactHint = "card ending in "
                    + (cardNum.length() >= 4 ? cardNum.substring(cardNum.length() - 4) : "****");

            pendingOtp = String.format("%06d", (int)(Math.random() * 1000000));

            if (btnPurchase != null) {
                btnPurchase.setEnabled(false);
                btnPurchase.setText("Sending OTP...");
            }
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                if (btnPurchase != null) { btnPurchase.setEnabled(true); updatePurchaseButton(); }
                showOtpDialog(contactHint, payDisplayLabel, false);
            }, 1000);
        }
    }

    /** Called when Firebase Phone Auth is not enabled — uses local simulation instead. */
    private void fallbackToSimulatedOtp(String contactHint, String payDisplayLabel) {
        pendingOtp      = String.format("%06d", (int)(Math.random() * 1000000));
        pendingVerifId  = null; // null signals simulation mode in verify logic
        showOtpDialog(contactHint, payDisplayLabel, true);
    }

    private void showOtpDialog(String contactHint, String payLabel, boolean isWallet) {
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(dp(24), dp(16), dp(24), dp(8));

        // Masked phone display
        TextView tvMsg = new TextView(this);
        String sentTo = isWallet
                ? "A 6-digit OTP has been sent to\n" + contactHint
                : "A 6-digit OTP has been sent to your\n" + contactHint;
        tvMsg.setText(sentTo + "\nvia " + payLabel + ".");
        tvMsg.setTextColor(0xFF4B5D7A);
        tvMsg.setTextSize(14f);
        tvMsg.setPadding(0, 0, 0, dp(16));
        layout.addView(tvMsg);

        // OTP input
        EditText etOtp = new EditText(this);
        etOtp.setHint("Enter 6-digit OTP");
        etOtp.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        etOtp.setFilters(new InputFilter[]{new InputFilter.LengthFilter(6)});
        etOtp.setTextSize(22f);
        etOtp.setGravity(android.view.Gravity.CENTER);
        etOtp.setTextColor(0xFF071A3D);
        etOtp.setLetterSpacing(0.3f);
        etOtp.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(0xFF8A2EFF));
        layout.addView(etOtp);

        // Resend option
        TextView tvResend = new TextView(this);
        tvResend.setText("Didn't receive it? Resend OTP");
        tvResend.setTextColor(0xFF8A2EFF);
        tvResend.setTextSize(13f);
        tvResend.setPadding(0, dp(12), 0, 0);
        tvResend.setGravity(android.view.Gravity.CENTER);
        tvResend.setClickable(true);
        tvResend.setFocusable(true);
        layout.addView(tvResend);

        // Show dev OTP label for simulated cases (card or wallet fallback)
        // pendingVerifId == null means Firebase not active → simulation mode
        boolean isSimulated = !isWallet || pendingVerifId == null;
        if (isSimulated && pendingOtp != null) {
            TextView tvDebug = new TextView(this);
            tvDebug.setText("[Dev] OTP: " + pendingOtp + "  (remove before production)");
            tvDebug.setTextColor(0xFFCC0000);
            tvDebug.setTextSize(12f);
            tvDebug.setTypeface(null, android.graphics.Typeface.BOLD);
            tvDebug.setPadding(0, dp(8), 0, 0);
            tvDebug.setGravity(android.view.Gravity.CENTER);
            layout.addView(tvDebug);
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Verify OTP")
                .setView(layout)
                .setPositiveButton("Verify & Pay", null) // set manually to control dismiss
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(d -> {
            android.widget.Button btnVerify = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            btnVerify.setOnClickListener(v -> {
                String entered = etOtp.getText().toString().trim();
                if (entered.length() != 6) {
                    etOtp.setError("Enter the 6-digit OTP");
                    return;
                }

                if (isWallet && pendingVerifId != null) {
                    // ── Firebase credential verification ──────────────────
                    btnVerify.setEnabled(false);
                    btnVerify.setText("Verifying...");
                    PhoneAuthCredential credential =
                            PhoneAuthProvider.getCredential(pendingVerifId, entered);
                    FirebaseAuth.getInstance().signInWithCredential(credential)
                            .addOnSuccessListener(result -> {
                                SoundManager.playSuccess(BuyTokensActivity.this);
                                dialog.dismiss();
                                showConfirmDialog();
                            })
                            .addOnFailureListener(e -> {
                                SoundManager.playError(BuyTokensActivity.this);
                                btnVerify.setEnabled(true);
                                btnVerify.setText("Verify & Pay");
                                etOtp.setError("Incorrect OTP. Please try again.");
                                etOtp.setText("");
                            });
                } else {
                    // ── Simulated OTP check (cards + wallet fallback) ──────
                    if (pendingOtp == null || !entered.equals(pendingOtp)) {
                        SoundManager.playError(BuyTokensActivity.this);
                        etOtp.setError("Incorrect OTP. Please try again.");
                        etOtp.setText("");
                        return;
                    }
                    SoundManager.playSuccess(BuyTokensActivity.this);
                    dialog.dismiss();
                    showConfirmDialog();
                }
            });

            tvResend.setOnClickListener(v -> {
                dialog.dismiss();
                if (isWallet && pendingVerifId != null) {
                    // Firebase resend using stored token
                    sendOtpAndVerify();
                } else {
                    // Simulated resend
                    pendingOtp = String.format("%06d", (int)(Math.random() * 1000000));
                    Toast.makeText(BuyTokensActivity.this,
                            "New OTP: " + pendingOtp, Toast.LENGTH_LONG).show();
                    showOtpDialog(contactHint, payLabel, isWallet);
                }
            });
        });

        dialog.show();
    }

    private void showConfirmDialog() {
        int    tokens     = (int)    PACKAGES[selectedPackageIndex][0];
        int    priceCents = (int)    PACKAGES[selectedPackageIndex][1];
        String label      = (String) PACKAGES[selectedPackageIndex][2];
        String payLabel   = PAY_METHODS[selectedPayMethodIndex][1];
        String priceStr   = String.format(Locale.getDefault(), "$%.2f", priceCents / 100.0);

        new AlertDialog.Builder(this)
                .setTitle("Confirm Purchase")
                .setMessage(label + " Package\n\n"
                        + "🪙 " + NumberFormat.getNumberInstance().format(tokens) + " tokens\n"
                        + priceStr + " via " + payLabel + "\n\n"
                        + "Tokens will be added to your account immediately.")
                .setPositiveButton("Confirm & Pay", (d, w) -> confirmPurchase(tokens, priceCents, label, payLabel))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmPurchase(int tokens, int priceCents, String label, String payLabel) {
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
                                recordTransaction(tokens, priceCents, label, payLabel);
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

    private void recordTransaction(int tokens, int priceCents, String label, String payLabel) {
        Map<String, Object> tx = new HashMap<>();
        tx.put("studentUid",  currentUser.getUid());
        tx.put("type",        "purchase");
        tx.put("label",       label);
        tx.put("tokens",      tokens);
        tx.put("priceCents",  priceCents);
        tx.put("payMethod",   payLabel);
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
            btnPurchase.setText("Select package & payment");
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