package com.example.peertutoring.ui;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.peertutoring.R;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * Tutor proposes a counter offer: new date, time, duration, token amount, optional message.
 * Date is selected via DatePickerDialog (no invalid dates possible).
 * Time is selected via TimePickerDialog (no invalid times possible).
 */
public class CounterOfferActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String requestId, studentName, subject, originalDate, originalTime;
    private int originalDuration, originalTokens;

    // Stepper state
    private int counterDuration;
    private int counterTokens;

    // Picked date/time values (stored separately for validation)
    private int pickedYear  = -1;
    private int pickedMonth = -1; // 0-based (Calendar.JANUARY = 0)
    private int pickedDay   = -1;
    private int pickedHour  = -1;
    private int pickedMinute = -1;

    private TextView tvDurationValue, tvTokenValue;
    private TextView tvDateDisplay, tvTimeDisplay; // display labels on the picker buttons

    private static final int DURATION_STEP = 15;
    private static final int TOKEN_STEP    = 25;
    private static final int MIN_DURATION  = 15;
    private static final int MAX_DURATION  = 240;
    private static final int MIN_TOKENS    = 25;
    private static final int MAX_TOKENS    = 2000;

    // Month names for display
    private static final String[] MONTHS = {
            "Jan","Feb","Mar","Apr","May","Jun",
            "Jul","Aug","Sep","Oct","Nov","Dec"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_counter_offer);

        db = FirebaseFirestore.getInstance();

        requestId        = getIntent().getStringExtra("requestId");
        studentName      = getIntent().getStringExtra("studentName");
        subject          = getIntent().getStringExtra("subject");
        originalDate     = getIntent().getStringExtra("date");
        originalTime     = getIntent().getStringExtra("time");
        originalDuration = getIntent().getIntExtra("duration", 60);
        originalTokens   = getIntent().getIntExtra("tokens", 150);

        counterDuration = originalDuration;
        counterTokens   = originalTokens;

        // Pre-seed pickers to today so they open on a sensible default
        Calendar today = Calendar.getInstance();
        pickedYear   = today.get(Calendar.YEAR);
        pickedMonth  = today.get(Calendar.MONTH);
        pickedDay    = today.get(Calendar.DAY_OF_MONTH);
        pickedHour   = today.get(Calendar.HOUR_OF_DAY);
        pickedMinute = 0;

        bindDisplayViews();
        populateHeader();
        bindSteppers();
        setupButtons();
    }

    // ── Bind display TextViews ────────────────────────────────

    private void bindDisplayViews() {
        // These are the TextViews inside the date/time picker cards that show chosen value
        tvDateDisplay = findViewById(R.id.etDate);
        tvTimeDisplay = findViewById(R.id.etTime);
        tvDurationValue = findViewById(R.id.tvDurationValue);
        tvTokenValue    = findViewById(R.id.tvTokenValue);

        // Show "Tap to select" as hint until user picks
        if (tvDateDisplay != null) tvDateDisplay.setText("Tap to select date");
        if (tvTimeDisplay != null) tvTimeDisplay.setText("Tap to select time");

        updateDurationDisplay();
        updateTokenDisplay();
    }

    // ── Header ────────────────────────────────────────────────

    private void populateHeader() {
        setText(R.id.tvStudentName, studentName);
        setText(R.id.tvSubject,     subject);

        String originalTerms = "Original: "
                + (originalDate != null ? originalDate : "TBD")
                + " at " + (originalTime != null ? originalTime : "TBD")
                + " • " + originalDuration + "m • " + originalTokens + " tokens";
        setText(R.id.tvOriginalTerms, originalTerms);

        // Initials
        TextView tvInitials = findViewById(R.id.tvStudentInitials);
        if (tvInitials != null && studentName != null) {
            String[] parts = studentName.split(" ");
            String initials = parts.length > 1
                    ? "" + parts[0].charAt(0) + parts[1].charAt(0)
                    : "" + parts[0].charAt(0);
            tvInitials.setText(initials.toUpperCase());
        }
    }

    // ── Date Picker ───────────────────────────────────────────

    private void showDatePicker() {
        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    pickedYear  = year;
                    pickedMonth = month;
                    pickedDay   = dayOfMonth;

                    // Display as "Mar 12, 2026"
                    String display = MONTHS[month] + " " + dayOfMonth + ", " + year;
                    if (tvDateDisplay != null) tvDateDisplay.setText(display);
                },
                pickedYear, pickedMonth, pickedDay
        );

        // Prevent selecting past dates
        dialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        dialog.show();
    }

    // ── Time Picker ───────────────────────────────────────────

    private void showTimePicker() {
        TimePickerDialog dialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    pickedHour   = hourOfDay;
                    pickedMinute = minute;

                    // Display as "14:30"
                    String display = String.format("%02d:%02d", hourOfDay, minute);
                    if (tvTimeDisplay != null) tvTimeDisplay.setText(display);
                },
                pickedHour, pickedMinute,
                true // 24-hour format
        );
        dialog.show();
    }

    // ── Steppers ──────────────────────────────────────────────

    private void bindSteppers() {
        MaterialCardView btnDurationMinus = findViewById(R.id.btnDurationMinus);
        MaterialCardView btnDurationPlus  = findViewById(R.id.btnDurationPlus);
        MaterialCardView btnTokenMinus    = findViewById(R.id.btnTokenMinus);
        MaterialCardView btnTokenPlus     = findViewById(R.id.btnTokenPlus);

        if (btnDurationMinus != null) btnDurationMinus.setOnClickListener(v -> {
            if (counterDuration - DURATION_STEP >= MIN_DURATION) {
                counterDuration -= DURATION_STEP;
                updateDurationDisplay();
            }
        });

        if (btnDurationPlus != null) btnDurationPlus.setOnClickListener(v -> {
            if (counterDuration + DURATION_STEP <= MAX_DURATION) {
                counterDuration += DURATION_STEP;
                updateDurationDisplay();
            }
        });

        if (btnTokenMinus != null) btnTokenMinus.setOnClickListener(v -> {
            if (counterTokens - TOKEN_STEP >= MIN_TOKENS) {
                counterTokens -= TOKEN_STEP;
                updateTokenDisplay();
            }
        });

        if (btnTokenPlus != null) btnTokenPlus.setOnClickListener(v -> {
            if (counterTokens + TOKEN_STEP <= MAX_TOKENS) {
                counterTokens += TOKEN_STEP;
                updateTokenDisplay();
            }
        });
    }

    private void updateDurationDisplay() {
        if (tvDurationValue != null)
            tvDurationValue.setText(String.valueOf(counterDuration));
    }

    private void updateTokenDisplay() {
        if (tvTokenValue != null)
            tvTokenValue.setText(String.valueOf(counterTokens));
    }

    // ── Buttons ───────────────────────────────────────────────

    private void setupButtons() {
        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        // Date card → open DatePickerDialog
        android.view.View dateCard = findViewById(R.id.etDate);
        if (dateCard != null) dateCard.setOnClickListener(v -> showDatePicker());

        // Also make the whole date section card tappable
        android.view.View dateSection = findViewById(R.id.cardDateSection);
        if (dateSection != null) dateSection.setOnClickListener(v -> showDatePicker());

        // Time card → open TimePickerDialog
        android.view.View timeCard = findViewById(R.id.etTime);
        if (timeCard != null) timeCard.setOnClickListener(v -> showTimePicker());

        android.view.View timeSection = findViewById(R.id.cardTimeSection);
        if (timeSection != null) timeSection.setOnClickListener(v -> showTimePicker());

        android.widget.Button btnSend = findViewById(R.id.btnSendCounterOffer);
        if (btnSend != null) btnSend.setOnClickListener(v -> sendCounterOffer());
    }

    // ── Send ──────────────────────────────────────────────────

    private void sendCounterOffer() {
        // Validate date was picked
        if (pickedDay == -1) {
            Toast.makeText(this, "Please select a date", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate time was picked
        if (pickedHour == -1) {
            Toast.makeText(this, "Please select a time", Toast.LENGTH_SHORT).show();
            return;
        }

        // Format final strings
        String newDate = MONTHS[pickedMonth] + " " + pickedDay + ", " + pickedYear;
        String newTime = String.format("%02d:%02d", pickedHour, pickedMinute);

        android.widget.EditText etMessage = findViewById(R.id.etMessage);
        String message = etMessage != null && etMessage.getText() != null
                ? etMessage.getText().toString().trim() : "";

        // Save to Firestore
        if (requestId != null && !requestId.isEmpty()) {
            String tutorUid = FirebaseAuth.getInstance().getCurrentUser() != null
                    ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

            Map<String, Object> counterOffer = new HashMap<>();
            counterOffer.put("tutorUid",          tutorUid);
            counterOffer.put("proposedDate",       newDate);
            counterOffer.put("proposedTime",       newTime);
            counterOffer.put("proposedDuration",   counterDuration);
            counterOffer.put("proposedTokens",     counterTokens);
            counterOffer.put("message",            message);
            counterOffer.put("status",             "pending");

            db.collection("sessionRequests")
                    .document(requestId)
                    .collection("counterOffers")
                    .add(counterOffer)
                    .addOnSuccessListener(ref ->
                            db.collection("sessionRequests")
                                    .document(requestId)
                                    .update("status", "counter_offered")
                                    .addOnSuccessListener(u -> navigateToResult(newDate, newTime)))
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show());
        } else {
            navigateToResult(newDate, newTime);
        }
    }

    private void navigateToResult(String newDate, String newTime) {
        Intent intent = new Intent(this, RequestResultActivity.class);
        intent.putExtra("resultType",      "counter");
        intent.putExtra("studentName",     studentName);
        intent.putExtra("subject",         subject);
        intent.putExtra("date",            newDate);
        intent.putExtra("time",            newTime);
        intent.putExtra("duration",        counterDuration);
        intent.putExtra("tokens",          counterTokens);
        intent.putExtra("counterDate",     newDate);
        intent.putExtra("counterTime",     newTime);
        intent.putExtra("counterDuration", counterDuration);
        intent.putExtra("counterTokens",   counterTokens);
        startActivity(intent);
        finish();
    }

    private void setText(int viewId, String text) {
        TextView tv = findViewById(viewId);
        if (tv != null && text != null) tv.setText(text);
    }
}