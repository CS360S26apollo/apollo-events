package com.example.peertutoring.ui;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
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
 * Payment methods (with saved details):
 *   Bank Transfer — IBAN / account number, account title
 *   PayPal        — PayPal email
 *   Debit Card    — Card number (last 4 stored), card title
 *   JazzCash      — mobile number
 *   EasyPaisa     — mobile number
 *   SadaPay       — mobile number
 *
 * Flow:
 *  1. Select amount (manual or quick chip)
 *  2. Select payment method
 *  3. If details saved → show saved + "Change" button
 *     If no details → show input form
 *  4. Confirm Withdrawal button
 *  5. PIN dialog → verify → process withdrawal
 */
public class WithdrawActivity extends AppCompatActivity {

    public static final int    TOKENS_PER_DOLLAR  = 10;
    public static final int    MINIMUM_WITHDRAWAL  = 100;
    public static final int[]  QUICK_AMOUNTS       = {100, 500, 1000, 5000};

    // {id, label, subtitle, fee, emoji, color}
    private static final String[][] METHODS = {
            {"bank",       "Bank Transfer", "2-3 business days", "0",  "🏦", "#0062FF"},
            {"paypal",     "PayPal",        "Instant",           "20", "💳", "#8A2EFF"},
            {"debit",      "Debit Card",    "1 business day",    "10", "💚", "#00C853"},
            {"jazzcash",   "JazzCash",      "Instant",           "5",  "📱", "#EF4444"},
            {"easypaisa",  "EasyPaisa",     "Instant",           "5",  "📱", "#00A651"},
            {"sadapay",    "SadaPay",       "Instant",           "0",  "📱", "#8A2EFF"},
    };

    private FirebaseFirestore db;
    private FirebaseUser      currentUser;

    private TextView       tvAvailableBalance, tvUsdEquivalent;
    private EditText       etAmount;
    private Button         btnConfirm;
    private LinearLayout   layoutMethods, layoutPaymentDetails;

    private long   currentBalance      = -1;
    private int    selectedAmount      = 0;
    private int    selectedMethodIndex = -1;
    private MaterialCardView[] methodCards;

    // Saved payment details from Firestore
    private Map<String, Object> savedPaymentDetails = new HashMap<>();
    private boolean detailsLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_withdraw);

        db          = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        tvAvailableBalance = findViewById(R.id.tvAvailableBalance);
        tvUsdEquivalent    = findViewById(R.id.tvUsdEquivalent);
        etAmount           = findViewById(R.id.etWithdrawAmount);
        btnConfirm         = findViewById(R.id.btnConfirmWithdrawal);
        layoutMethods      = findViewById(R.id.layoutWithdrawMethods);
        layoutPaymentDetails = findViewById(R.id.layoutPaymentDetails);

        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        setupQuickAmounts();
        buildMethodCards();
        setupAmountInput();
        loadBalanceAndDetails();

        if (btnConfirm != null) {
            btnConfirm.setEnabled(false);
            btnConfirm.setAlpha(0.4f);
            btnConfirm.setOnClickListener(v -> {
                SoundManager.playClick(this);
                showPinDialog();
            });
        }
    }

    // ── Load balance + saved payment details ──────────────────

    private void loadBalanceAndDetails() {
        if (currentUser == null) { updateBalanceUI(12450); return; }

        db.collection("users").document(currentUser.getUid()).get()
                .addOnSuccessListener(doc -> {
                    Long bal = doc.getLong("tokens");
                    updateBalanceUI(bal != null ? bal : 0);

                    // Load saved payment details
                    Object details = doc.get("paymentDetails");
                    if (details instanceof Map) {
                        //noinspection unchecked
                        savedPaymentDetails = (Map<String, Object>) details;
                    }
                    detailsLoaded = true;

                    // Load saved PIN hash
                    String savedPin = doc.getString("withdrawalPin");
                    if (savedPin != null) pinHash = savedPin;
                });
    }

    private void updateBalanceUI(long balance) {
        currentBalance = balance;
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.getDefault());
        if (tvAvailableBalance != null) tvAvailableBalance.setText(nf.format(balance));
        if (tvUsdEquivalent != null)
            tvUsdEquivalent.setText(String.format(Locale.getDefault(),
                    "$%.2f", balance / (double) TOKENS_PER_DOLLAR));
        updateConfirmButton();
    }

    // ── Quick amount chips ────────────────────────────────────

    private void setupQuickAmounts() {
        LinearLayout container = findViewById(R.id.layoutQuickAmounts);
        if (container == null) return;
        container.removeAllViews();
        for (int amount : QUICK_AMOUNTS) {
            Button chip = new Button(this);
            chip.setText(String.valueOf(amount));
            chip.setTextSize(13f);
            chip.setAllCaps(false);
            chip.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#EEF2FF")));
            chip.setTextColor(Color.parseColor("#8A2EFF"));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            lp.setMarginEnd(dp(8));
            chip.setLayoutParams(lp);
            chip.setPadding(dp(4), dp(6), dp(4), dp(6));
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
        etAmount.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(android.text.Editable s) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                try { selectedAmount = Integer.parseInt(s.toString().trim()); }
                catch (NumberFormatException e) { selectedAmount = 0; }
                updateConfirmButton();
            }
        });
    }

    // ── Method cards ──────────────────────────────────────────

    private void buildMethodCards() {
        if (layoutMethods == null) return;
        layoutMethods.removeAllViews();
        methodCards = new MaterialCardView[METHODS.length];

        // Two-column grid
        LinearLayout row = null;
        for (int i = 0; i < METHODS.length; i++) {
            final int idx = i;
            String[] m = METHODS[i];

            if (i % 2 == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                rp.setMargins(0, 0, 0, dp(10));
                row.setLayoutParams(rp);
                layoutMethods.addView(row);
            }

            MaterialCardView card = new MaterialCardView(this);
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
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
            inner.setPadding(dp(8), dp(12), dp(8), dp(12));

            MaterialCardView iconBadge = new MaterialCardView(this);
            LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(dp(36), dp(36));
            ip.setMargins(0, 0, 0, dp(6));
            iconBadge.setLayoutParams(ip);
            iconBadge.setRadius(dp(10));
            iconBadge.setCardElevation(0);
            iconBadge.setCardBackgroundColor(Color.parseColor(m[5]));
            TextView tvIcon = new TextView(this);
            tvIcon.setText(m[4]);
            tvIcon.setTextSize(16f);
            tvIcon.setGravity(Gravity.CENTER);
            tvIcon.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            iconBadge.addView(tvIcon);
            inner.addView(iconBadge);

            TextView tvLabel = new TextView(this);
            tvLabel.setText(m[1]);
            tvLabel.setTextColor(0xFF071A3D);
            tvLabel.setTextSize(12f);
            tvLabel.setTypeface(null, Typeface.BOLD);
            tvLabel.setGravity(Gravity.CENTER);
            inner.addView(tvLabel);

            // Fee label
            TextView tvFee = new TextView(this);
            String fee = m[3];
            tvFee.setText("Fee: " + fee + (fee.equals("0") ? "" : " tokens"));
            tvFee.setTextColor(0xFF8B97A8);
            tvFee.setTextSize(10f);
            tvFee.setGravity(Gravity.CENTER);
            inner.addView(tvFee);

            card.addView(inner);
            card.setOnClickListener(v -> selectMethod(idx));
            methodCards[i] = card;
            if (row != null) row.addView(card);
        }
    }

    private void selectMethod(int idx) {
        SoundManager.playClick(this);
        selectedMethodIndex = idx;

        for (int i = 0; i < methodCards.length; i++) {
            if (methodCards[i] == null) continue;
            boolean sel = (i == idx);
            methodCards[i].setStrokeColor(sel ? 0xFF8A2EFF : 0xFFE4D5FF);
            methodCards[i].setStrokeWidth(sel ? dp(2) : dp(1));
            methodCards[i].setCardBackgroundColor(sel ? 0xFFF9F5FF : 0xFFFFFFFF);
        }

        showPaymentDetailsSection(idx);
        updateConfirmButton();
    }

    // ── Payment details section ───────────────────────────────

    private void showPaymentDetailsSection(int idx) {
        if (layoutPaymentDetails == null) return;
        layoutPaymentDetails.removeAllViews();
        layoutPaymentDetails.setVisibility(View.VISIBLE);

        String methodId = METHODS[idx][0];
        String label    = METHODS[idx][1];

        // Check if this method has saved details
        String savedSummary = getSavedSummary(methodId);

        if (savedSummary != null) {
            // Show saved details with Change button
            showSavedDetails(methodId, label, savedSummary);
        } else {
            // Show input form
            showDetailInputForm(methodId, label, false);
        }
    }

    /** Returns a display summary of saved details for this method, or null if none. */
    private String getSavedSummary(String methodId) {
        if (savedPaymentDetails == null) return null;
        Object details = savedPaymentDetails.get(methodId);
        if (details == null) return null;
        if (details instanceof Map) {
            //noinspection unchecked
            Map<String, Object> d = (Map<String, Object>) details;
            switch (methodId) {
                case "bank":
                    String iban  = (String) d.get("iban");
                    String title = (String) d.get("title");
                    if (iban == null) return null;
                    return "Account: " + maskIban(iban) + (title != null ? "\n" + title : "");
                case "paypal":
                    String email = (String) d.get("email");
                    return email != null ? "PayPal: " + email : null;
                case "debit":
                    String last4 = (String) d.get("last4");
                    String name  = (String) d.get("name");
                    return last4 != null ? "Card ending in " + last4
                            + (name != null ? "\n" + name : "") : null;
                case "jazzcash":
                case "easypaisa":
                case "sadapay":
                    String phone = (String) d.get("phone");
                    return phone != null ? "Mobile: " + phone : null;
            }
        }
        return null;
    }

    private void showSavedDetails(String methodId, String label, String summary) {
        // Header
        TextView tvHeader = new TextView(this);
        tvHeader.setText(label + " — Saved Details");
        tvHeader.setTextColor(0xFF071A3D);
        tvHeader.setTextSize(14f);
        tvHeader.setTypeface(null, Typeface.BOLD);
        tvHeader.setPadding(0, 0, 0, dp(10));
        layoutPaymentDetails.addView(tvHeader);

        // Details card
        MaterialCardView detailCard = new MaterialCardView(this);
        detailCard.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        detailCard.setRadius(dp(14));
        detailCard.setCardBackgroundColor(0xFFF3EEFF);
        detailCard.setCardElevation(0);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(12), dp(14), dp(12));

        TextView tvSummary = new TextView(this);
        tvSummary.setText(summary);
        tvSummary.setTextColor(0xFF071A3D);
        tvSummary.setTextSize(14f);
        tvSummary.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(tvSummary);

        // Change button
        Button btnChange = new Button(this);
        btnChange.setText("Change");
        btnChange.setAllCaps(false);
        btnChange.setTextColor(0xFF8A2EFF);
        btnChange.setTextSize(13f);
        btnChange.setBackground(null);
        btnChange.setOnClickListener(v -> {
            SoundManager.playClick(this);
            // Ask for PIN first before allowing changes
            showChangePinDialog(methodId, label);
        });
        row.addView(btnChange);

        detailCard.addView(row);
        layoutPaymentDetails.addView(detailCard);
    }

    private void showDetailInputForm(String methodId, String label, boolean isChange) {
        // Header
        TextView tvHeader = new TextView(this);
        tvHeader.setText(isChange ? "Update " + label + " Details" : "Enter " + label + " Details");
        tvHeader.setTextColor(0xFF071A3D);
        tvHeader.setTextSize(14f);
        tvHeader.setTypeface(null, Typeface.BOLD);
        tvHeader.setPadding(0, 0, 0, dp(10));
        layoutPaymentDetails.addView(tvHeader);

        // Build input fields per method
        boolean isMobile = methodId.equals("jazzcash")
                || methodId.equals("easypaisa") || methodId.equals("sadapay");

        if (isMobile) {
            EditText etPhone = addInputField("Mobile Number (03xx-xxxxxxx)", InputType.TYPE_CLASS_PHONE);
            // Prefill if changing
            if (isChange) prefillField(etPhone, methodId, "phone");

            Button btnSave = addSaveButton();
            btnSave.setOnClickListener(v -> {
                String phone = etPhone.getText().toString().trim();
                String err = validatePhone(phone);
                if (err != null) { etPhone.setError(err); return; }
                Map<String, Object> d = new HashMap<>();
                d.put("phone", phone);
                savePaymentDetail(methodId, d);
            });

        } else if (methodId.equals("paypal")) {
            EditText etEmail = addInputField("PayPal Email (e.g. user@gmail.com)", InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
            etEmail.addTextChangedListener(new android.text.TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
                @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
                @Override public void afterTextChanged(android.text.Editable s) {
                    etEmail.setError(validatePayPalEmail(s.toString().trim()));
                }
            });
            if (isChange) prefillField(etEmail, methodId, "email");

            Button btnSave = addSaveButton();
            btnSave.setOnClickListener(v -> {
                String email = etEmail.getText().toString().trim().toLowerCase();
                String err = validatePayPalEmail(email);
                if (err != null) { etEmail.setError(err); return; }
                Map<String, Object> d = new HashMap<>();
                d.put("email", email);
                savePaymentDetail(methodId, d);
            });

        } else if (methodId.equals("bank")) {
            // IBAN field with real-time format validation
            EditText etIban  = addInputField("Pakistani IBAN (PK + 22 digits = 24 chars)", InputType.TYPE_CLASS_TEXT);
            etIban.setFilters(new InputFilter[]{
                    new InputFilter.LengthFilter(24),
                    (source, start, end, dest, dstart, dend) -> source.toString().toUpperCase()
            });
            etIban.addTextChangedListener(new android.text.TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
                @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
                @Override public void afterTextChanged(android.text.Editable s) {
                    String err = validateIban(s.toString().trim());
                    etIban.setError(err); // null clears the error
                }
            });

            EditText etTitle = addInputField("Account Title", InputType.TYPE_CLASS_TEXT);
            EditText etBank  = addBankDropdown(); // autocomplete bank selector
            if (isChange) {
                prefillField(etIban,  methodId, "iban");
                prefillField(etTitle, methodId, "title");
                prefillField(etBank,  methodId, "bank");
            }

            Button btnSave = addSaveButton();
            btnSave.setOnClickListener(v -> {
                String iban  = etIban.getText().toString().trim().toUpperCase();
                String title = etTitle.getText().toString().trim();
                String bank  = etBank.getText().toString().trim();
                String ibanErr = validateIban(iban);
                if (ibanErr != null)   { etIban.setError(ibanErr);                    return; }
                if (title.isEmpty())   { etTitle.setError("Enter account title.");     return; }
                String bankErr = validateBankName(bank);
                if (bankErr != null)   { etBank.setError(bankErr);                    return; }
                Map<String, Object> d = new HashMap<>();
                d.put("iban",  iban);
                d.put("title", title);
                d.put("bank",  bank);
                savePaymentDetail(methodId, d);
            });

        } else if (methodId.equals("debit")) {
            EditText etCard  = addInputField("Card Number (16 digits)", InputType.TYPE_CLASS_NUMBER);
            EditText etName  = addInputField("Cardholder Name", InputType.TYPE_CLASS_TEXT);
            if (isChange) prefillField(etName, methodId, "name");

            Button btnSave = addSaveButton();
            btnSave.setOnClickListener(v -> {
                String card = etCard.getText().toString().replaceAll(" ", "");
                String name = etName.getText().toString().trim();
                if (card.length() < 13) { etCard.setError("Enter a valid card number."); return; }
                if (name.isEmpty())     { etName.setError("Enter cardholder name."); return; }
                // Only store last 4 digits for security
                Map<String, Object> d = new HashMap<>();
                d.put("last4", card.substring(card.length() - 4));
                d.put("name",  name);
                savePaymentDetail(methodId, d);
            });
        }
    }

    private EditText addInputField(String hint, int inputType) {
        MaterialCardView card = new MaterialCardView(this);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cp.setMargins(0, 0, 0, dp(10));
        card.setLayoutParams(cp);
        card.setRadius(dp(14));
        card.setCardBackgroundColor(0xFFFFFFFF);
        card.setCardElevation(dp(1));

        EditText et = new EditText(this);
        et.setHint(hint);
        et.setInputType(inputType);
        et.setTextColor(0xFF071A3D);
        et.setTextSize(14f);
        et.setPadding(dp(14), dp(14), dp(14), dp(14));
        et.setBackground(null);
        card.addView(et);
        layoutPaymentDetails.addView(card);
        return et;
    }

    private Button addSaveButton() {
        Button btn = new Button(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(48));
        lp.setMargins(0, dp(4), 0, dp(8));
        btn.setLayoutParams(lp);
        btn.setText("Save Details");
        btn.setAllCaps(false);
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(14f);
        try { btn.setBackground(getDrawable(R.drawable.bg_button_gradient)); } catch (Exception ignored) {}
        layoutPaymentDetails.addView(btn);
        return btn;
    }

    private void prefillField(EditText et, String methodId, String key) {
        if (savedPaymentDetails == null) return;
        Object details = savedPaymentDetails.get(methodId);
        if (details instanceof Map) {
            //noinspection unchecked
            Object val = ((Map<String, Object>) details).get(key);
            if (val != null && et != null) et.setText(val.toString());
        }
    }

    private void savePaymentDetail(String methodId, Map<String, Object> details) {
        if (currentUser == null) return;

        // Store under users/{uid}/paymentDetails/{methodId}
        savedPaymentDetails.put(methodId, details);

        Map<String, Object> update = new HashMap<>();
        update.put("paymentDetails." + methodId, details);

        db.collection("users").document(currentUser.getUid())
                .set(update, SetOptions.merge())
                .addOnSuccessListener(u -> {
                    Toast.makeText(this, "✅ Payment details saved!", Toast.LENGTH_SHORT).show();
                    // Refresh the section to show saved state
                    showPaymentDetailsSection(selectedMethodIndex);
                    updateConfirmButton();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to save: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    // ── PIN management ────────────────────────────────────────

    private String pinHash = null; // loaded from Firestore

    /** Called when user taps "Change" on saved details — verify PIN first. */
    private void showChangePinDialog(String methodId, String label) {
        if (pinHash == null) {
            // No PIN set yet — let them set one first
            showSetPinDialog(() -> showDetailInputForm(methodId, label, true));
            return;
        }

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(24), dp(16), dp(24), dp(8));

        TextView tvMsg = new TextView(this);
        tvMsg.setText("Enter your withdrawal PIN to make changes.");
        tvMsg.setTextColor(0xFF4B5D7A);
        tvMsg.setTextSize(14f);
        tvMsg.setPadding(0, 0, 0, dp(14));
        layout.addView(tvMsg);

        EditText etPin = new EditText(this);
        etPin.setHint("4-digit PIN");
        etPin.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        etPin.setFilters(new InputFilter[]{new InputFilter.LengthFilter(4)});
        etPin.setTextSize(20f);
        etPin.setGravity(Gravity.CENTER);
        etPin.setLetterSpacing(0.3f);
        layout.addView(etPin);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Verify PIN")
                .setView(layout)
                .setPositiveButton("Verify", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(d -> {
            Button btnVerify = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            btnVerify.setOnClickListener(v -> {
                String entered = etPin.getText().toString().trim();
                if (entered.length() != 4) {
                    etPin.setError("Enter 4-digit PIN");
                    return;
                }
                if (!hashPin(entered).equals(pinHash)) {
                    SoundManager.playError(this);
                    etPin.setError("Incorrect PIN");
                    etPin.setText("");
                    return;
                }
                SoundManager.playSuccess(this);
                dialog.dismiss();
                // PIN correct — allow changes
                layoutPaymentDetails.removeAllViews();
                showDetailInputForm(methodId, label, true);
            });
        });
        dialog.show();
    }

    /** First-time PIN setup or reset. */
    private void showSetPinDialog(Runnable onSuccess) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(24), dp(16), dp(24), dp(8));

        TextView tvMsg = new TextView(this);
        tvMsg.setText("Create a 4-digit withdrawal PIN to protect your payment details.");
        tvMsg.setTextColor(0xFF4B5D7A);
        tvMsg.setTextSize(14f);
        tvMsg.setPadding(0, 0, 0, dp(14));
        layout.addView(tvMsg);

        EditText etPin1 = new EditText(this);
        etPin1.setHint("New 4-digit PIN");
        etPin1.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        etPin1.setFilters(new InputFilter[]{new InputFilter.LengthFilter(4)});
        etPin1.setTextSize(20f);
        etPin1.setGravity(Gravity.CENTER);
        etPin1.setLetterSpacing(0.3f);
        layout.addView(etPin1);

        EditText etPin2 = new EditText(this);
        etPin2.setHint("Confirm PIN");
        etPin2.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        etPin2.setFilters(new InputFilter[]{new InputFilter.LengthFilter(4)});
        etPin2.setTextSize(20f);
        etPin2.setGravity(Gravity.CENTER);
        etPin2.setLetterSpacing(0.3f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(10), 0, 0);
        etPin2.setLayoutParams(lp);
        layout.addView(etPin2);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Set Withdrawal PIN")
                .setView(layout)
                .setPositiveButton("Set PIN", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(d -> {
            Button btnSet = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            btnSet.setOnClickListener(v -> {
                String p1 = etPin1.getText().toString().trim();
                String p2 = etPin2.getText().toString().trim();
                if (p1.length() != 4) { etPin1.setError("Enter 4 digits"); return; }
                if (!p1.equals(p2))   { etPin2.setError("PINs don't match"); return; }

                pinHash = hashPin(p1);
                // Save hashed PIN to Firestore
                if (currentUser != null) {
                    db.collection("users").document(currentUser.getUid())
                            .update("withdrawalPin", pinHash);
                }
                dialog.dismiss();
                Toast.makeText(this, "✅ PIN set!", Toast.LENGTH_SHORT).show();
                if (onSuccess != null) onSuccess.run();
            });
        });
        dialog.show();
    }

    /** Shows PIN dialog before confirming withdrawal. */
    private void showPinDialog() {
        if (pinHash == null) {
            // No PIN set — set one first, then confirm
            showSetPinDialog(this::processWithdrawal);
            return;
        }

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(24), dp(16), dp(24), dp(8));

        TextView tvMsg = new TextView(this);
        String payLabel = selectedMethodIndex >= 0 ? METHODS[selectedMethodIndex][1] : "";
        tvMsg.setText("Enter your PIN to confirm withdrawal of "
                + selectedAmount + " tokens via " + payLabel + ".");
        tvMsg.setTextColor(0xFF4B5D7A);
        tvMsg.setTextSize(14f);
        tvMsg.setPadding(0, 0, 0, dp(14));
        layout.addView(tvMsg);

        EditText etPin = new EditText(this);
        etPin.setHint("4-digit PIN");
        etPin.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        etPin.setFilters(new InputFilter[]{new InputFilter.LengthFilter(4)});
        etPin.setTextSize(22f);
        etPin.setGravity(Gravity.CENTER);
        etPin.setLetterSpacing(0.3f);
        layout.addView(etPin);

        // Forgot PIN link
        TextView tvForgot = new TextView(this);
        tvForgot.setText("Forgot PIN? Reset it");
        tvForgot.setTextColor(0xFF8A2EFF);
        tvForgot.setTextSize(13f);
        tvForgot.setPadding(0, dp(10), 0, 0);
        tvForgot.setGravity(Gravity.CENTER);
        layout.addView(tvForgot);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Confirm Withdrawal")
                .setView(layout)
                .setPositiveButton("Confirm", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(d -> {
            Button btnOk = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            btnOk.setOnClickListener(v -> {
                String entered = etPin.getText().toString().trim();
                if (entered.length() != 4) { etPin.setError("Enter 4-digit PIN"); return; }
                if (!hashPin(entered).equals(pinHash)) {
                    SoundManager.playError(this);
                    etPin.setError("Incorrect PIN");
                    etPin.setText("");
                    return;
                }
                SoundManager.playSuccess(this);
                dialog.dismiss();
                processWithdrawal();
            });

            tvForgot.setOnClickListener(v -> {
                dialog.dismiss();
                // Reset PIN — requires re-entering details
                pinHash = null;
                if (currentUser != null) {
                    db.collection("users").document(currentUser.getUid())
                            .update("withdrawalPin", null);
                }
                showSetPinDialog(this::processWithdrawal);
            });
        });
        dialog.show();
    }

    // ── Validation + process ──────────────────────────────────

    private void updateConfirmButton() {
        boolean balanceLoaded = currentBalance >= 0;
        boolean hasDetails    = selectedMethodIndex >= 0
                && getSavedSummary(METHODS[selectedMethodIndex][0]) != null;
        int fee = selectedMethodIndex >= 0
                ? Integer.parseInt(METHODS[selectedMethodIndex][3]) : 0;
        boolean valid = balanceLoaded
                && selectedAmount >= MINIMUM_WITHDRAWAL
                && (selectedAmount + fee) <= currentBalance
                && selectedMethodIndex >= 0
                && hasDetails;

        if (btnConfirm != null) {
            btnConfirm.setEnabled(valid);
            btnConfirm.setAlpha(valid ? 1.0f : 0.4f);
        }
    }

    private void processWithdrawal() {
        if (selectedMethodIndex < 0 || currentUser == null) return;

        if (btnConfirm != null) { btnConfirm.setEnabled(false); btnConfirm.setText("Processing..."); }

        int    feeCost       = Integer.parseInt(METHODS[selectedMethodIndex][3]);
        int    totalDeduct   = selectedAmount + feeCost;
        String method        = METHODS[selectedMethodIndex][1];
        String processingTime = METHODS[selectedMethodIndex][2];

        db.collection("users").document(currentUser.getUid())
                .update("tokens", com.google.firebase.firestore.FieldValue.increment(-totalDeduct))
                .addOnSuccessListener(u -> recordAndNavigate(method, processingTime, feeCost))
                .addOnFailureListener(e -> {
                    SoundManager.playError(this);
                    Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    if (btnConfirm != null) {
                        btnConfirm.setEnabled(true);
                        btnConfirm.setText("Confirm Withdrawal");
                    }
                });
    }

    private void recordAndNavigate(String method, String processingTime, int fee) {
        Map<String, Object> record = new HashMap<>();
        record.put("tutorUid",       currentUser.getUid());
        record.put("amount",         selectedAmount);
        record.put("fee",            fee);
        record.put("method",         method);
        record.put("processingTime", processingTime);
        record.put("status",         "pending");
        record.put("createdAt",      FieldValue.serverTimestamp());

        db.collection("withdrawals").add(record)
                .addOnSuccessListener(ref -> {
                    SoundManager.playSuccess(this);
                    Intent intent = new Intent(this, WithdrawSuccessActivity.class);
                    intent.putExtra("amount",         selectedAmount);
                    intent.putExtra("method",         method);
                    intent.putExtra("processingTime", processingTime);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    // Tokens already deducted — still show success
                    SoundManager.playSuccess(this);
                    Intent intent = new Intent(this, WithdrawSuccessActivity.class);
                    intent.putExtra("amount",         selectedAmount);
                    intent.putExtra("method",         method);
                    intent.putExtra("processingTime", processingTime);
                    startActivity(intent);
                    finish();
                });
    }

    // ── Helpers ───────────────────────────────────────────────


    // Pakistani bank list for autocomplete + validation
    private static final String[] PAKISTANI_BANKS = {
            "Allied Bank Limited (ABL)",
            "Askari Bank",
            "Bank Alfalah",
            "Bank AL Habib",
            "Bank Islami Pakistan",
            "Dubai Islamic Bank Pakistan",
            "Faysal Bank",
            "First Women Bank",
            "Habib Bank Limited (HBL)",
            "Habib Metropolitan Bank",
            "JS Bank",
            "MCB Bank Limited",
            "MCB Islamic Bank",
            "Meezan Bank",
            "National Bank of Pakistan (NBP)",
            "NIB Bank",
            "Samba Bank",
            "Silkbank",
            "Sindh Bank",
            "SME Bank",
            "Soneri Bank",
            "Standard Chartered Bank Pakistan",
            "Summit Bank",
            "The Bank of Khyber",
            "The Bank of Punjab (BOP)",
            "United Bank Limited (UBL)",
            "Zarai Taraqiati Bank (ZTBL)",
            "Al Baraka Bank Pakistan",
            "Burj Bank",
            "Mobilink Microfinance Bank",
            "Telenor Microfinance Bank (Easypaisa)",
            "U Microfinance Bank",
            "NRSP Microfinance Bank"
    };

    /**
     * Adds an AutoCompleteTextView pre-filled with all Pakistani banks.
     * User can type to filter or scroll the dropdown.
     */
    private EditText addBankDropdown() {
        MaterialCardView card = new MaterialCardView(this);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cp.setMargins(0, 0, 0, dp(10));
        card.setLayoutParams(cp);
        card.setRadius(dp(14));
        card.setCardBackgroundColor(0xFFFFFFFF);
        card.setCardElevation(dp(1));

        android.widget.AutoCompleteTextView acTextView =
                new android.widget.AutoCompleteTextView(this);
        acTextView.setHint("Bank Name — type to search");
        acTextView.setTextColor(0xFF071A3D);
        acTextView.setTextSize(14f);
        acTextView.setPadding(dp(14), dp(14), dp(14), dp(14));
        acTextView.setBackground(null);
        acTextView.setThreshold(1); // start suggesting after 1 char
        acTextView.setDropDownHeight(dp(200));

        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                PAKISTANI_BANKS);
        acTextView.setAdapter(adapter);

        // Validate on text change
        acTextView.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void afterTextChanged(android.text.Editable s) {
                String err = validateBankName(s.toString().trim());
                acTextView.setError(err);
            }
        });

        card.addView(acTextView);
        layoutPaymentDetails.addView(card);
        return acTextView; // EditText is parent of AutoCompleteTextView
    }

    /**
     * Validates bank name against the official list of Pakistani banks.
     * Case-insensitive partial match — catches typos while being flexible.
     */
    private String validateBankName(String bank) {
        if (bank.isEmpty()) return "Enter your bank name.";
        if (bank.length() < 3) return "Enter at least 3 characters of the bank name.";
        String lower = bank.toLowerCase();
        for (String b : PAKISTANI_BANKS) {
            if (b.toLowerCase().contains(lower) || lower.contains(b.toLowerCase().substring(0, Math.min(5, b.length())))) {
                return null; // match found
            }
        }
        // Check common abbreviations / alternate names
        java.util.Map<String, String> aliases = new java.util.HashMap<>();
        aliases.put("hbl", "Habib Bank Limited (HBL)");
        aliases.put("ubl", "United Bank Limited (UBL)");
        aliases.put("mcb", "MCB Bank Limited");
        aliases.put("nbp", "National Bank of Pakistan (NBP)");
        aliases.put("abl", "Allied Bank Limited (ABL)");
        aliases.put("bop", "The Bank of Punjab (BOP)");
        aliases.put("scb", "Standard Chartered Bank Pakistan");
        aliases.put("bah", "Bank AL Habib");
        aliases.put("bafl", "Bank Alfalah");
        aliases.put("alfalah", "Bank Alfalah");
        aliases.put("meezan", "Meezan Bank");
        aliases.put("askari", "Askari Bank");
        aliases.put("faysal", "Faysal Bank");
        for (String alias : aliases.keySet()) {
            if (lower.contains(alias)) return null;
        }
        return "Bank not recognised. Please select from the dropdown list or verify spelling.";
    }

    private String hashPin(String pin) {
        // Simple SHA-256 hash — good enough for a PIN
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(pin.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return pin; // fallback (shouldn't happen)
        }
    }

    private String maskIban(String iban) {
        if (iban.length() <= 6) return iban;
        return iban.substring(0, 4) + "****" + iban.substring(iban.length() - 4);
    }

    private String validatePhone(String phone) {
        String digits = phone.replaceAll("[^\\d]", "");
        if (digits.isEmpty())        return "Please enter your mobile number.";
        if (!digits.startsWith("03")) return "Must start with 03 (e.g. 03001234567).";
        if (digits.length() != 11)   return "Must be exactly 11 digits (e.g. 03001234567).";
        // Check valid Pakistani operator prefixes
        String op = digits.substring(0, 4);
        java.util.Set<String> validOps = new java.util.HashSet<>(java.util.Arrays.asList(
                "0300","0301","0302","0303","0304","0305","0306","0307","0308","0309", // Zong
                "0310","0311","0312","0313","0314","0315","0316","0317","0318","0319", // Zong
                "0320","0321","0322","0323","0324","0325","0326","0327","0328","0329", // Telenor
                "0330","0331","0332","0333","0334","0335","0336","0337","0338","0339", // Telenor
                "0340","0341","0342","0343","0344","0345","0346","0347","0348","0349", // Ufone
                "0350","0351",                                                          // Ufone
                "0360","0361","0362","0363","0364","0365","0366","0367","0368","0369", // Warid/Jazz
                "0370","0371","0372","0373","0374","0375","0376","0377","0378","0379",
                "0380","0381","0382","0383","0384","0385","0386","0387","0388","0389",
                "0390","0391","0392","0393","0394","0395","0396","0397","0398","0399"
        ));
        if (!validOps.contains(op)) return "Invalid mobile operator prefix: " + op + ".";
        return null;
    }

    /** IBAN validation — Pakistani IBANs are exactly 24 chars: PK + 2 check digits + 20 alphanumeric */
    private String validateIban(String iban) {
        if (iban.isEmpty()) return "Enter your IBAN.";
        String clean = iban.replaceAll("\\s", "").toUpperCase();
        if (!clean.startsWith("PK"))
            return "Pakistani IBAN must start with PK (e.g. PK36SCBL0000001123456702).";
        if (clean.length() != 24)
            return "IBAN must be exactly 24 characters (PK + 22 digits). Yours: " + clean.length() + " chars.";
        // Validate characters — only letters and digits after PK
        if (!clean.substring(2).matches("[A-Z0-9]+"))
            return "IBAN must contain only letters and numbers after PK.";
        // Validate check digits using MOD-97 algorithm
        if (!ibanMod97Check(clean))
            return "IBAN check digits are invalid. Please verify your IBAN carefully.";
        return null;
    }

    /** MOD-97 IBAN checksum validation — industry standard used by all banks */
    private boolean ibanMod97Check(String iban) {
        // Move first 4 chars to end, convert letters to numbers, check mod 97 == 1
        String rearranged = iban.substring(4) + iban.substring(0, 4);
        StringBuilder numericIban = new StringBuilder();
        for (char c : rearranged.toCharArray()) {
            if (Character.isLetter(c)) {
                numericIban.append(c - 'A' + 10);
            } else {
                numericIban.append(c);
            }
        }
        // Compute mod 97 in chunks to avoid overflow
        String numStr = numericIban.toString();
        int remainder = 0;
        for (int i = 0; i < numStr.length(); i++) {
            remainder = (remainder * 10 + (numStr.charAt(i) - '0')) % 97;
        }
        return remainder == 1;
    }

    /** PayPal email — valid email format + not a disposable/temp domain */
    private String validatePayPalEmail(String email) {
        if (email.isEmpty()) return "Enter your PayPal email address.";
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches())
            return "Enter a valid email address.";
        // Check domain part
        String domain = email.contains("@") ? email.split("@")[1].toLowerCase() : "";
        // Block known disposable email domains
        java.util.Set<String> disposable = new java.util.HashSet<>(java.util.Arrays.asList(
                "mailinator.com","guerrillamail.com","10minutemail.com","tempmail.com",
                "throwaway.email","sharklasers.com","yopmail.com","trashmail.com",
                "dispostable.com","fakeinbox.com","maildrop.cc","getairmail.com"
        ));
        if (disposable.contains(domain))
            return "Disposable email addresses are not accepted for PayPal.";
        // Must have a recognised TLD
        if (!domain.contains("."))
            return "Enter a valid email domain (e.g. gmail.com).";
        return null;
    }

    private int dp(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}