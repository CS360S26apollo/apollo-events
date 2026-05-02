package com.example.peertutoring.ui;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;

import com.example.peertutoring.R;
import com.example.peertutoring.models.SessionRequest;
import com.example.peertutoring.utils.ConflictChecker;
import com.example.peertutoring.utils.EscrowManager;
import com.example.peertutoring.utils.SoundManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class InstantBookActivity extends AppCompatActivity {

    private AutoCompleteTextView spinnerSubject;
    private EditText etTopic, etGoals;
    private Button btn30, btn45, btn60, btn90, btnBookNow;
    private TextView tvCostPreview, tvTutorName, tvRateLabel, tvNoSlots;
    private LinearLayout layoutSlots;

    private int selectedDuration = 60;
    private String selectedSlotDay = null;
    private int selectedSlotHour = -1;
    private Button selectedSlotButton = null;

    private FirebaseFirestore db;
    private String currentUid, currentUserName;
    private String tutorUid, tutorName;
    private int tutorRate;

    private static final String[] ALL_SUBJECTS = {
            "Mathematics", "Physics", "Chemistry", "Biology",
            "Computer Science", "English", "History", "Economics"
    };

    private static final String[] DAY_KEYS  = {"mon","tue","wed","thu","fri","sat","sun"};
    private static final String[] DAY_NAMES = {"Monday","Tuesday","Wednesday","Thursday","Friday","Saturday","Sunday"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_instant_book);

        db = FirebaseFirestore.getInstance();

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUid      = FirebaseAuth.getInstance().getCurrentUser().getUid();
            currentUserName = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
            if (currentUserName == null || currentUserName.isEmpty()) currentUserName = "Student";
        } else {
            currentUid = "";
        }

        tutorUid  = getIntent().getStringExtra("tutorUid");
        tutorName = getIntent().getStringExtra("tutorName");
        tutorRate = getIntent().getIntExtra("tutorRate", 100);

        spinnerSubject = findViewById(R.id.spinnerSubject);
        etTopic        = findViewById(R.id.etTopic);
        etGoals        = findViewById(R.id.etGoals);
        btn30          = findViewById(R.id.btn30min);
        btn45          = findViewById(R.id.btn45min);
        btn60          = findViewById(R.id.btn60min);
        btn90          = findViewById(R.id.btn90min);
        btnBookNow     = findViewById(R.id.btnBookNow);
        tvCostPreview  = findViewById(R.id.tvCostPreview);
        tvTutorName    = findViewById(R.id.tvTutorName);
        tvRateLabel    = findViewById(R.id.tvRateLabel);
        tvNoSlots      = findViewById(R.id.tvNoSlots);
        layoutSlots    = findViewById(R.id.layoutSlots);

        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        if (tvTutorName != null && tutorName != null)
            tvTutorName.setText("with " + tutorName);
        if (tvRateLabel != null)
            tvRateLabel.setText(String.valueOf(tutorRate));

        setupDurationButtons();
        loadTutorData();

        if (btnBookNow != null)
            btnBookNow.setOnClickListener(v -> { SoundManager.playClick(this); validateAndBook(); });
    }

    private void loadTutorData() {
        if (tutorUid == null || tutorUid.isEmpty()) {
            setupSubjectDropdown(getAllSubjectsList());
            showNoSlots();
            return;
        }

        db.collection("users").document(tutorUid).get()
                .addOnSuccessListener(doc -> {
                    List<String> subjects = null;
                    if (doc.exists()) {
                        //noinspection unchecked
                        subjects = (List<String>) doc.get("subjects");
                    }
                    if (subjects == null || subjects.isEmpty()) subjects = getAllSubjectsList();
                    setupSubjectDropdown(subjects);

                    if (doc.exists()) {
                        //noinspection unchecked
                        Map<String, Object> avail = (Map<String, Object>) doc.get("availability");
                        if (avail != null && !avail.isEmpty()) {
                            displaySlots(avail);
                            return;
                        }
                    }
                    showNoSlots();
                })
                .addOnFailureListener(e -> {
                    setupSubjectDropdown(getAllSubjectsList());
                    showNoSlots();
                });
    }

    private void displaySlots(Map<String, Object> avail) {
        if (layoutSlots == null) return;
        layoutSlots.removeAllViews();

        boolean hasAny = false;

        for (int d = 0; d < DAY_KEYS.length; d++) {
            String dayKey  = DAY_KEYS[d];
            String dayName = DAY_NAMES[d];

            Object hoursObj = avail.get(dayKey);
            if (hoursObj == null) continue;

            List<Long> hours = null;
            if (hoursObj instanceof List) {
                //noinspection unchecked
                hours = (List<Long>) hoursObj;
            }
            if (hours == null || hours.isEmpty()) continue;

            hasAny = true;

            // Day label
            TextView dayLabel = new TextView(this);
            dayLabel.setText(dayName);
            dayLabel.setTextColor(Color.parseColor("#071A3D"));
            dayLabel.setTextSize(14f);
            dayLabel.setTypeface(null, Typeface.BOLD);
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            labelParams.setMargins(0, 0, 0, dpToPx(8));
            dayLabel.setLayoutParams(labelParams);
            layoutSlots.addView(dayLabel);

            // Horizontal row of chips
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            for (Long hourLong : hours) {
                if (hourLong == null) continue;
                final int slotHour = hourLong.intValue();
                final String slotDay = dayKey;

                Button chip = new Button(this);
                chip.setText(formatHour(slotHour));
                chip.setTextSize(13f);
                chip.setAllCaps(false);
                chip.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F3EEFF")));
                chip.setTextColor(Color.parseColor("#8A2EFF"));
                LinearLayout.LayoutParams chipParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, dpToPx(40));
                chipParams.setMargins(0, 0, dpToPx(8), 0);
                chip.setLayoutParams(chipParams);
                chip.setPadding(dpToPx(14), 0, dpToPx(14), 0);

                chip.setOnClickListener(v -> {
                    if (selectedSlotButton != null) {
                        selectedSlotButton.setBackgroundTintList(
                                ColorStateList.valueOf(Color.parseColor("#F3EEFF")));
                        selectedSlotButton.setTextColor(Color.parseColor("#8A2EFF"));
                    }
                    chip.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#8A2EFF")));
                    chip.setTextColor(Color.WHITE);
                    selectedSlotDay    = slotDay;
                    selectedSlotHour   = slotHour;
                    selectedSlotButton = chip;
                });

                row.addView(chip);
            }

            // Wrap row in HorizontalScrollView for overflow
            HorizontalScrollView scroll = new HorizontalScrollView(this);
            scroll.setHorizontalScrollBarEnabled(false);
            LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            scrollParams.setMargins(0, 0, 0, dpToPx(16));
            scroll.setLayoutParams(scrollParams);
            scroll.addView(row);
            layoutSlots.addView(scroll);
        }

        if (!hasAny) showNoSlots();
    }

    private void showNoSlots() {
        if (tvNoSlots != null) tvNoSlots.setVisibility(View.VISIBLE);
    }

    private void setupSubjectDropdown(List<String> subjects) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, subjects);
        if (spinnerSubject != null) {
            spinnerSubject.setAdapter(adapter);
            spinnerSubject.setOnClickListener(v -> spinnerSubject.showDropDown());
            if (subjects.size() == 1) spinnerSubject.setText(subjects.get(0), false);
        }
    }

    private void setupDurationButtons() {
        View.OnClickListener listener = v -> {
            resetDurationButtons();
            if      (v.getId() == R.id.btn30min) { selectedDuration = 30;  selectBtn(btn30); }
            else if (v.getId() == R.id.btn45min) { selectedDuration = 45;  selectBtn(btn45); }
            else if (v.getId() == R.id.btn60min) { selectedDuration = 60;  selectBtn(btn60); }
            else if (v.getId() == R.id.btn90min) { selectedDuration = 90;  selectBtn(btn90); }
            updateCostPreview();
        };

        if (btn30 != null) btn30.setOnClickListener(listener);
        if (btn45 != null) btn45.setOnClickListener(listener);
        if (btn60 != null) btn60.setOnClickListener(listener);
        if (btn90 != null) btn90.setOnClickListener(listener);
        selectBtn(btn60);
        updateCostPreview();
    }

    private void selectBtn(Button btn) {
        if (btn == null) return;
        btn.setBackground(AppCompatResources.getDrawable(this, R.drawable.bg_button_gradient));
        btn.setTextColor(Color.WHITE);
    }

    private void resetDurationButtons() {
        for (Button b : new Button[]{btn30, btn45, btn60, btn90}) {
            if (b == null) continue;
            b.setBackground(null);
            b.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F3F4F6")));
            b.setTextColor(Color.parseColor("#4B5D7A"));
        }
    }

    private void updateCostPreview() {
        if (tvCostPreview == null) return;
        int cost = calculateTokenCost(selectedDuration);
        tvCostPreview.setText("Cost: " + cost + " tokens  (" + tutorRate + " tokens/hr)");
    }

    private int calculateTokenCost(int durationMinutes) {
        return Math.max(1, (int) Math.ceil((durationMinutes * (double) tutorRate) / 60.0));
    }

    private void validateAndBook() {
        String subject = spinnerSubject != null ? spinnerSubject.getText().toString().trim() : "";
        String topic   = etTopic  != null ? etTopic.getText().toString().trim()  : "";
        String goals   = etGoals  != null ? etGoals.getText().toString().trim()  : "";

        if (TextUtils.isEmpty(subject)) {
            if (spinnerSubject != null) spinnerSubject.setError("Please select a subject");
            return;
        }
        if (TextUtils.isEmpty(topic)) {
            Toast.makeText(this, "Please enter a topic", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedSlotDay == null || selectedSlotHour < 0) {
            Toast.makeText(this, "Please select a time slot", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentUid == null || currentUid.isEmpty()) {
            Toast.makeText(this, "Please sign in first", Toast.LENGTH_SHORT).show();
            return;
        }

        final int    tokenCost = calculateTokenCost(selectedDuration);
        final String subFinal  = subject;
        final String topFinal  = topic;
        final String goalFinal = goals;
        final Date   slotDate  = nextOccurrenceOf(selectedSlotDay, selectedSlotHour);

        if (btnBookNow != null) {
            btnBookNow.setEnabled(false);
            btnBookNow.setText("Checking conflicts...");
        }

        // US-14: conflict check before deducting any tokens
        ConflictChecker.checkConflict(db, tutorUid, currentUid, slotDate, selectedDuration, null,
                (hasConflict, reason) -> {
                    if (hasConflict) {
                        SoundManager.playError(this);
                        Toast.makeText(this, "Scheduling conflict: " + reason, Toast.LENGTH_LONG).show();
                        resetBookButton();
                        return;
                    }
                    checkBalanceAndBook(tokenCost, subFinal, topFinal, goalFinal, slotDate);
                });
    }

    private void checkBalanceAndBook(int tokenCost, String subject, String topic,
                                     String goals, Date slotDate) {
        if (btnBookNow != null) btnBookNow.setText("Checking balance...");

        db.collection("users").document(currentUid).get()
                .addOnSuccessListener(doc -> {
                    Long bal = doc.getLong("tokens");
                    long currentTokens = (bal != null) ? bal : 100L;

                    if (currentTokens < tokenCost) {
                        SoundManager.playError(this);
                        Toast.makeText(this,
                                "Need " + tokenCost + " tokens, you have " + currentTokens,
                                Toast.LENGTH_LONG).show();
                        resetBookButton();
                        return;
                    }

                    deductAndBook(tokenCost, subject, topic, goals, slotDate);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    resetBookButton();
                });
    }

    private void deductAndBook(int tokenCost,
                               String subject, String topic, String goals, Date scheduledDate) {
        if (btnBookNow != null) btnBookNow.setText("Booking...");

        // US 24: atomic deduct → HELD state
        EscrowManager.deductFromStudent(db, currentUid, tokenCost,
                () -> {
                    SessionRequest req = new SessionRequest(
                            currentUid, currentUserName, subject, topic, goals, selectedDuration);
                    req.setStatus(SessionRequest.STATUS_BOOKED); // skip "requested" — instant!
                    req.setTokens(tokenCost);
                    req.setTutorUid(tutorUid  != null ? tutorUid  : "");
                    req.setTutorName(tutorName != null ? tutorName : "");
                    req.setScheduledDate(scheduledDate);
                    req.setPaymentStatus(EscrowManager.PAYMENT_HELD);
                    req.setEscrowBalance(tokenCost);

                    db.collection("sessionRequests").add(req)
                            .addOnSuccessListener(docRef -> {
                                docRef.update("requestId", docRef.getId());
                                SoundManager.playSuccess(this);
                                Toast.makeText(this,
                                        "Booked! " + tokenCost + " tokens held in escrow.",
                                        Toast.LENGTH_LONG).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                EscrowManager.atomicRefund(db, currentUid, tokenCost);
                                Toast.makeText(this, "Error: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                                resetBookButton();
                            });
                },
                () -> {
                    Toast.makeText(this, "Failed to deduct tokens.", Toast.LENGTH_SHORT).show();
                    resetBookButton();
                });
    }

    private Date nextOccurrenceOf(String dayKey, int hour) {
        int targetCalDay;
        switch (dayKey) {
            case "mon": targetCalDay = Calendar.MONDAY;    break;
            case "tue": targetCalDay = Calendar.TUESDAY;   break;
            case "wed": targetCalDay = Calendar.WEDNESDAY; break;
            case "thu": targetCalDay = Calendar.THURSDAY;  break;
            case "fri": targetCalDay = Calendar.FRIDAY;    break;
            case "sat": targetCalDay = Calendar.SATURDAY;  break;
            default:    targetCalDay = Calendar.SUNDAY;    break;
        }

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        while (cal.get(Calendar.DAY_OF_WEEK) != targetCalDay) {
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
        // If the slot is in the past (same day but hour already passed), push one week
        if (!cal.getTime().after(new Date())) {
            cal.add(Calendar.WEEK_OF_YEAR, 1);
        }
        return cal.getTime();
    }

    private void resetBookButton() {
        if (btnBookNow != null) {
            btnBookNow.setEnabled(true);
            btnBookNow.setText("⚡  Book Now");
        }
    }

    private String formatHour(int hour) {
        if (hour == 0)  return "12:00 AM";
        if (hour < 12)  return hour + ":00 AM";
        if (hour == 12) return "12:00 PM";
        return (hour - 12) + ":00 PM";
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private List<String> getAllSubjectsList() {
        List<String> list = new ArrayList<>();
        for (String s : ALL_SUBJECTS) list.add(s);
        return list;
    }
}
