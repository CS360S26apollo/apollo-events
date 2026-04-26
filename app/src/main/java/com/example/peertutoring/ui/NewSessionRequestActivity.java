package com.example.peertutoring.ui;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;

import com.example.peertutoring.R;
import com.example.peertutoring.utils.ConflictChecker;
import com.example.peertutoring.utils.EscrowManager;
import com.example.peertutoring.utils.SoundManager;
import com.example.peertutoring.models.SessionRequest;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Activity for students to book a session with a specific tutor.
 * US-14: Student proposes a date/time which is conflict-checked (tutor + student)
 * before the request is submitted.
 */
public class NewSessionRequestActivity extends AppCompatActivity {

    private AutoCompleteTextView spinnerSubject;
    private EditText etTopic, etGoals, etCustomDuration;
    private Button btn30, btn45, btn60, btn90, btnSubmit;
    private TextView tvCostPreview, tvTutorSubjectsLabel;
    private TextView tvProposedDate, tvProposedTime;
    private int selectedDuration = 60;

    // Proposed date/time state (mirrors CounterOfferActivity pattern)
    private int pickedYear   = -1;
    private int pickedMonth  = -1;
    private int pickedDay    = -1;
    private int pickedHour   = -1;
    private int pickedMinute = -1;

    private static final String[] MONTHS = {
            "Jan","Feb","Mar","Apr","May","Jun",
            "Jul","Aug","Sep","Oct","Nov","Dec"
    };

    private FirebaseFirestore db;
    private String currentUid;
    private String currentUserName;

    private String tutorUid;
    private String tutorName;
    private int tutorRatePerHour = 100;

    private static final String[] ALL_SUBJECTS = {
            "Mathematics", "Physics", "Chemistry", "Biology",
            "Computer Science", "English", "History", "Economics"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_session_request);

        db = FirebaseFirestore.getInstance();

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUid      = FirebaseAuth.getInstance().getCurrentUser().getUid();
            currentUserName = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
            if (currentUserName == null || currentUserName.isEmpty()) currentUserName = "Student";
        } else {
            currentUid = "";
        }

        tutorUid         = getIntent().getStringExtra("tutorUid");
        tutorName        = getIntent().getStringExtra("tutorName");
        tutorRatePerHour = getIntent().getIntExtra("tutorRate", 100);

        spinnerSubject      = findViewById(R.id.spinnerSubject);
        etTopic             = findViewById(R.id.etTopic);
        etGoals             = findViewById(R.id.etGoals);
        etCustomDuration    = findViewById(R.id.etCustomDuration);
        btn30               = findViewById(R.id.btn30min);
        btn45               = findViewById(R.id.btn45min);
        btn60               = findViewById(R.id.btn60min);
        btn90               = findViewById(R.id.btn90min);
        btnSubmit           = findViewById(R.id.btnSubmitRequest);
        tvCostPreview       = findViewById(R.id.tvTokenCost);
        tvTutorSubjectsLabel = findViewById(R.id.tvScreenTitle);
        tvProposedDate      = findViewById(R.id.tvProposedDate);
        tvProposedTime      = findViewById(R.id.tvProposedTime);

        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        if (tvTutorSubjectsLabel != null && tutorName != null && !tutorName.isEmpty()) {
            tvTutorSubjectsLabel.setText("Book with " + tutorName);
        }

        setupDateTimePickers();
        setupDurationButtons();

        if (btnSubmit != null) btnSubmit.setOnClickListener(v -> { SoundManager.playClick(this); checkAndPost(); });

        loadTutorSubjects();
    }

    // ── Date / time pickers ──────────────────────────────────────────────────

    private void setupDateTimePickers() {
        View dateCard = findViewById(R.id.cardProposedDate);
        if (dateCard != null) dateCard.setOnClickListener(v -> showDatePicker());
        if (tvProposedDate != null) tvProposedDate.setOnClickListener(v -> showDatePicker());

        View timeCard = findViewById(R.id.cardProposedTime);
        if (timeCard != null) timeCard.setOnClickListener(v -> showTimePicker());
        if (tvProposedTime != null) tvProposedTime.setOnClickListener(v -> showTimePicker());
    }

    private void showDatePicker() {
        Calendar today = Calendar.getInstance();
        int initYear  = pickedYear  > 0 ? pickedYear  : today.get(Calendar.YEAR);
        int initMonth = pickedMonth >= 0 ? pickedMonth : today.get(Calendar.MONTH);
        int initDay   = pickedDay   > 0 ? pickedDay   : today.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dlg = new DatePickerDialog(this,
                (view, year, month, day) -> {
                    pickedYear  = year;
                    pickedMonth = month;
                    pickedDay   = day;
                    if (tvProposedDate != null)
                        tvProposedDate.setText(MONTHS[month] + " " + day + ", " + year);
                }, initYear, initMonth, initDay);
        dlg.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        dlg.show();
    }

    private void showTimePicker() {
        Calendar now = Calendar.getInstance();
        int initHour   = pickedHour   >= 0 ? pickedHour   : now.get(Calendar.HOUR_OF_DAY);
        int initMinute = pickedMinute >= 0 ? pickedMinute : 0;

        new TimePickerDialog(this,
                (view, hour, minute) -> {
                    pickedHour   = hour;
                    pickedMinute = minute;
                    if (tvProposedTime != null)
                        tvProposedTime.setText(String.format("%02d:%02d", hour, minute));
                }, initHour, initMinute, true).show();
    }

    /** Builds a Date from the user's picked year/month/day/hour/minute. */
    private Date buildProposedDate() {
        Calendar cal = Calendar.getInstance();
        cal.set(pickedYear, pickedMonth, pickedDay, pickedHour, pickedMinute, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    // ── Subject dropdown ─────────────────────────────────────────────────────

    private void loadTutorSubjects() {
        if (tutorUid == null || tutorUid.isEmpty()) {
            setupSubjectDropdown(getAllSubjectsList());
            return;
        }
        db.collection("users").document(tutorUid).get()
                .addOnSuccessListener(doc -> {
                    List<String> subjects = null;
                    if (doc.exists()) //noinspection unchecked
                        subjects = (List<String>) doc.get("subjects");
                    if (subjects == null || subjects.isEmpty()) subjects = getAllSubjectsList();
                    setupSubjectDropdown(subjects);
                })
                .addOnFailureListener(e -> setupSubjectDropdown(getAllSubjectsList()));
    }

    private List<String> getAllSubjectsList() {
        List<String> list = new ArrayList<>();
        for (String s : ALL_SUBJECTS) list.add(s);
        return list;
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

    // ── Duration buttons ─────────────────────────────────────────────────────

    private void setupDurationButtons() {
        View.OnClickListener listener = v -> {
            resetDurationButtons();
            if      (v.getId() == R.id.btn30min) { selectedDuration = 30; selectBtn(btn30); }
            else if (v.getId() == R.id.btn45min) { selectedDuration = 45; selectBtn(btn45); }
            else if (v.getId() == R.id.btn60min) { selectedDuration = 60; selectBtn(btn60); }
            else if (v.getId() == R.id.btn90min) { selectedDuration = 90; selectBtn(btn90); }
            if (etCustomDuration != null) etCustomDuration.setText("");
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
            b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    Color.parseColor("#F3F4F6")));
            b.setTextColor(Color.parseColor("#4B5D7A"));
        }
    }

    private void updateCostPreview() {
        if (tvCostPreview == null) return;
        int cost = calculateTokenCost(selectedDuration);
        tvCostPreview.setText("Cost: " + cost + " tokens  (" + tutorRatePerHour + " tokens/hr)");
    }

    private int calculateTokenCost(int durationMinutes) {
        return Math.max(1, (int) Math.ceil(durationMinutes / 60.0) * tutorRatePerHour);
    }

    // ── Submit flow ───────────────────────────────────────────────────────────

    private void checkAndPost() {
        String subject   = spinnerSubject != null ? spinnerSubject.getText().toString().trim() : "";
        String topic     = etTopic != null ? etTopic.getText().toString().trim() : "";
        String goals     = etGoals != null ? etGoals.getText().toString().trim() : "";
        String customDur = etCustomDuration != null ? etCustomDuration.getText().toString().trim() : "";

        int finalDuration = selectedDuration;
        if (!TextUtils.isEmpty(customDur)) {
            try { finalDuration = Integer.parseInt(customDur); }
            catch (NumberFormatException ignored) {}
        }

        if (TextUtils.isEmpty(subject)) {
            if (spinnerSubject != null) spinnerSubject.setError("Please select a subject");
            return;
        }
        if (TextUtils.isEmpty(topic)) {
            if (etTopic != null) etTopic.setError("Topic is required");
            return;
        }
        if (pickedDay < 0 || pickedHour < 0) {
            Toast.makeText(this, "Please select a proposed date and time", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentUid == null || currentUid.isEmpty()) {
            Toast.makeText(this, "Please sign in first", Toast.LENGTH_SHORT).show();
            return;
        }

        Date proposedDate = buildProposedDate();
        if (!proposedDate.after(new Date())) {
            Toast.makeText(this, "Proposed time must be in the future", Toast.LENGTH_SHORT).show();
            return;
        }

        final int    tokenCost = calculateTokenCost(finalDuration);
        final int    dur       = finalDuration;
        final String subFinal  = subject;
        final String topFinal  = topic;
        final String goalFinal = goals;

        setBtnState(false, "Checking conflicts...");

        ConflictChecker.checkConflict(db, tutorUid, currentUid, proposedDate, dur, null,
                (hasConflict, reason) -> {
                    if (hasConflict) {
                        SoundManager.playError(this);
                        Toast.makeText(this, "Scheduling conflict: " + reason, Toast.LENGTH_LONG).show();
                        resetSubmitButton();
                        return;
                    }
                    checkTokensAndPost(proposedDate, tokenCost, dur, subFinal, topFinal, goalFinal);
                });
    }

    private void checkTokensAndPost(Date proposedDate, int tokenCost, int dur,
                                    String subject, String topic, String goals) {
        setBtnState(false, "Checking balance...");
        db.collection("users").document(currentUid).get()
                .addOnSuccessListener(doc -> {
                    Long bal = doc.getLong("tokens");
                    long currentTokens = (bal != null) ? bal : 100L;
                    if (currentTokens < tokenCost) {
                        SoundManager.playError(this);
                        Toast.makeText(this,
                                "Need " + tokenCost + " tokens, you have " + currentTokens,
                                Toast.LENGTH_LONG).show();
                        resetSubmitButton();
                        return;
                    }
                    deductAndPost(tokenCost, proposedDate, subject, topic, goals, dur);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    resetSubmitButton();
                });
    }

    private void deductAndPost(int tokenCost, Date proposedDate,
                               String subject, String topic, String goals, int duration) {
        setBtnState(false, "Posting...");
        // US 24: atomic deduct → HELD state; avoids stale-read race in read-modify-write
        EscrowManager.deductFromStudent(db, currentUid, tokenCost,
                () -> {
                    SessionRequest req = new SessionRequest(
                            currentUid, currentUserName, subject, topic, goals, duration);
                    req.setStatus(SessionRequest.STATUS_REQUESTED);
                    req.setTokens(tokenCost);
                    req.setScheduledDate(proposedDate);
                    req.setPaymentStatus(EscrowManager.PAYMENT_HELD);
                    req.setEscrowBalance(tokenCost);
                    if (tutorUid  != null && !tutorUid.isEmpty())  req.setTutorUid(tutorUid);
                    if (tutorName != null && !tutorName.isEmpty()) req.setTutorName(tutorName);

                    db.collection("sessionRequests").add(req)
                            .addOnSuccessListener(docRef -> {
                                docRef.update("requestId", docRef.getId());
                                SoundManager.playSuccess(this);
                                Toast.makeText(this,
                                        "Request sent! " + tokenCost + " tokens held in escrow.",
                                        Toast.LENGTH_LONG).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                EscrowManager.atomicRefund(db, currentUid, tokenCost);
                                Toast.makeText(this, "Error: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                                resetSubmitButton();
                            });
                },
                () -> {
                    Toast.makeText(this, "Failed to deduct tokens.", Toast.LENGTH_SHORT).show();
                    resetSubmitButton();
                });
    }

    private void setBtnState(boolean enabled, String text) {
        if (btnSubmit != null) {
            btnSubmit.setEnabled(enabled);
            btnSubmit.setText(text);
        }
    }

    private void resetSubmitButton() {
        setBtnState(true, "Post Session Request");
    }
}
