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
import com.example.peertutoring.utils.ConflictChecker;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class CounterOfferActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String requestId, studentName, subject, originalDate, originalTime;
    private String studentUid;
    private int originalDuration, originalTokens;

    private int counterDuration;
    private int counterTokens;

    private String counterSessionType = "online"; // "online" or "takehome"
    private String studentAddress;
    private double studentLat, studentLng;

    private int pickedYear  = -1;
    private int pickedMonth = -1;
    private int pickedDay   = -1;
    private int pickedHour  = -1;
    private int pickedMinute = -1;

    private TextView tvDurationValue, tvTokenValue;
    private TextView tvDateDisplay, tvTimeDisplay;

    private static final int DURATION_STEP = 15;
    private static final int TOKEN_STEP    = 25;
    private static final int MIN_DURATION  = 15;
    private static final int MAX_DURATION  = 240;
    private static final int MIN_TOKENS    = 25;
    private static final int MAX_TOKENS    = 2000;

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
        studentUid       = getIntent().getStringExtra("studentUid");
        studentAddress   = getIntent().getStringExtra("studentAddress");
        studentLat       = getIntent().getDoubleExtra("studentLat", 0);
        studentLng       = getIntent().getDoubleExtra("studentLng", 0);

        counterDuration = originalDuration;
        counterTokens   = originalTokens;

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
        setupSessionTypeSelector();
    }

    private void bindDisplayViews() {
        tvDateDisplay = findViewById(R.id.etDate);
        tvTimeDisplay = findViewById(R.id.etTime);
        tvDurationValue = findViewById(R.id.tvDurationValue);
        tvTokenValue    = findViewById(R.id.tvTokenValue);

        if (tvDateDisplay != null) tvDateDisplay.setText("Tap to select date");
        if (tvTimeDisplay != null) tvTimeDisplay.setText("Tap to select time");

        updateDurationDisplay();
        updateTokenDisplay();
    }

    private void populateHeader() {
        setText(R.id.tvStudentName, studentName);
        setText(R.id.tvSubject,     subject);

        String originalTerms = "Original: "
                + (originalDate != null ? originalDate : "TBD")
                + " at " + (originalTime != null ? originalTime : "TBD")
                + " • " + originalDuration + "m • " + originalTokens + " tokens";
        setText(R.id.tvOriginalTerms, originalTerms);

        TextView tvInitials = findViewById(R.id.tvStudentInitials);
        if (tvInitials != null && studentName != null) {
            String[] parts = studentName.split(" ");
            String initials = parts.length > 1
                    ? "" + parts[0].charAt(0) + parts[1].charAt(0)
                    : "" + parts[0].charAt(0);
            tvInitials.setText(initials.toUpperCase());
        }
    }

    private void showDatePicker() {
        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    pickedYear  = year;
                    pickedMonth = month;
                    pickedDay   = dayOfMonth;
                    String display = MONTHS[month] + " " + dayOfMonth + ", " + year;
                    if (tvDateDisplay != null) tvDateDisplay.setText(display);
                },
                pickedYear, pickedMonth, pickedDay
        );
        dialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        dialog.show();
    }

    private void showTimePicker() {
        TimePickerDialog dialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    pickedHour   = hourOfDay;
                    pickedMinute = minute;
                    String display = String.format("%02d:%02d", hourOfDay, minute);
                    if (tvTimeDisplay != null) tvTimeDisplay.setText(display);
                },
                pickedHour, pickedMinute,
                true
        );
        dialog.show();
    }

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

    private void setupButtons() {
        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        android.view.View dateCard = findViewById(R.id.etDate);
        if (dateCard != null) dateCard.setOnClickListener(v -> showDatePicker());

        android.view.View dateSection = findViewById(R.id.cardDateSection);
        if (dateSection != null) dateSection.setOnClickListener(v -> showDatePicker());

        android.view.View timeCard = findViewById(R.id.etTime);
        if (timeCard != null) timeCard.setOnClickListener(v -> showTimePicker());

        android.view.View timeSection = findViewById(R.id.cardTimeSection);
        if (timeSection != null) timeSection.setOnClickListener(v -> showTimePicker());

        android.widget.Button btnSend = findViewById(R.id.btnSendCounterOffer);
        if (btnSend != null) btnSend.setOnClickListener(v -> sendCounterOffer());
    }

    /**
     * Validates input, checks for scheduling conflicts (US-14), then submits
     * the counter-offer as a Firestore sub-collection document.
     */
    private void sendCounterOffer() {
        if (pickedDay == -1) {
            Toast.makeText(this, "Please select a date", Toast.LENGTH_SHORT).show();
            return;
        }
        if (pickedHour == -1) {
            Toast.makeText(this, "Please select a time", Toast.LENGTH_SHORT).show();
            return;
        }

        String newDate = MONTHS[pickedMonth] + " " + pickedDay + ", " + pickedYear;
        String newTime = String.format("%02d:%02d", pickedHour, pickedMinute);

        android.widget.EditText etMessage = findViewById(R.id.etMessage);
        String message = etMessage != null && etMessage.getText() != null
                ? etMessage.getText().toString().trim() : "";

        String tutorUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

        if (requestId == null || requestId.isEmpty()) {
            navigateToResult(newDate, newTime);
            return;
        }

        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(pickedYear, pickedMonth, pickedDay, pickedHour, pickedMinute, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        java.util.Date proposedDate = cal.getTime();

        android.widget.Button btnSend = findViewById(R.id.btnSendCounterOffer);
        if (btnSend != null) { btnSend.setEnabled(false); btnSend.setText("Checking conflicts..."); }

        // US-14: exclude the current request from the conflict check (it will be updated, not new)
        ConflictChecker.checkConflict(db, tutorUid,
                studentUid != null ? studentUid : "",
                proposedDate, counterDuration, requestId,
                (hasConflict, reason) -> {
                    if (hasConflict) {
                        Toast.makeText(this, "Scheduling conflict: " + reason, Toast.LENGTH_LONG).show();
                        if (btnSend != null) { btnSend.setEnabled(true); btnSend.setText("✈  Send Counter Offer"); }
                        return;
                    }
                    saveCounterOffer(tutorUid, newDate, newTime, message, btnSend);
                });
    }

    private void saveCounterOffer(String tutorUid, String newDate, String newTime,
                                  String message, android.widget.Button btnSend) {
        Map<String, Object> counterOffer = new HashMap<>();
        counterOffer.put("tutorUid",        tutorUid);
        counterOffer.put("proposedDate",    newDate);
        counterOffer.put("proposedTime",    newTime);
        counterOffer.put("proposedDuration", counterDuration);
        counterOffer.put("proposedTokens",  counterTokens);
        counterOffer.put("message",         message);
        counterOffer.put("status",          "pending");
        counterOffer.put("sessionType",     counterSessionType);

        db.collection("sessionRequests").document(requestId)
                .collection("counterOffers")
                .add(counterOffer)
                .addOnSuccessListener(ref ->
                        db.collection("sessionRequests").document(requestId)
                                .update("status", "counter_offered")
                                .addOnSuccessListener(u -> navigateToResult(newDate, newTime)))
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    if (btnSend != null) { btnSend.setEnabled(true); btnSend.setText("✈  Send Counter Offer"); }
                });
    }


    private void setupSessionTypeSelector() {
        android.widget.Button btnOnline   = findViewById(R.id.btnCounterOnline);
        android.widget.Button btnTakeHome = findViewById(R.id.btnCounterTakeHome);
        android.view.View     layoutLoc   = findViewById(R.id.layoutStudentLocation);
        android.widget.TextView tvAddr    = findViewById(R.id.tvStudentLocationAddr);

        if (layoutLoc != null && studentAddress != null && !studentAddress.isEmpty()) {
            layoutLoc.setVisibility(android.view.View.VISIBLE);
            if (tvAddr != null) tvAddr.setText("📍 Student: " + studentAddress);
        }

        android.content.res.ColorStateList sel = android.content.res.ColorStateList.valueOf(
                android.graphics.Color.parseColor("#8A2EFF"));
        android.content.res.ColorStateList def = android.content.res.ColorStateList.valueOf(
                android.graphics.Color.parseColor("#F3F4F6"));

        android.view.View.OnClickListener l = v -> {
            if (v.getId() == R.id.btnCounterOnline) {
                counterSessionType = "online";
                if (btnOnline   != null) { btnOnline.setBackgroundColor(android.graphics.Color.parseColor("#8A2EFF")); btnOnline.setTextColor(android.graphics.Color.WHITE); }
                if (btnTakeHome != null) { btnTakeHome.setBackgroundColor(android.graphics.Color.parseColor("#F3F4F6")); btnTakeHome.setTextColor(android.graphics.Color.parseColor("#4B5D7A")); }
            } else {
                counterSessionType = "takehome";
                if (btnTakeHome != null) { btnTakeHome.setBackgroundColor(android.graphics.Color.parseColor("#8A2EFF")); btnTakeHome.setTextColor(android.graphics.Color.WHITE); }
                if (btnOnline   != null) { btnOnline.setBackgroundColor(android.graphics.Color.parseColor("#F3F4F6")); btnOnline.setTextColor(android.graphics.Color.parseColor("#4B5D7A")); }
            }
        };

        if (btnOnline   != null) { btnOnline.setOnClickListener(l); btnOnline.callOnClick(); }
        if (btnTakeHome != null) btnTakeHome.setOnClickListener(l);
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
        startActivity(intent);
        finish();
    }

    private void setText(int viewId, String text) {
        TextView tv = findViewById(viewId);
        if (tv != null && text != null) tv.setText(text);
    }
}