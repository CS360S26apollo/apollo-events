package com.example.peertutoring.ui;

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
import com.example.peertutoring.utils.SoundManager;
import com.example.peertutoring.models.SessionRequest;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity for students to book a session with a specific tutor.
 *
 * Fixes:
 * 1. Subject dropdown only shows subjects the tutor actually teaches
 *    (fetched from their Firestore profile). Falls back to full list if unavailable.
 * 2. Token cost = ceil(duration / 60) × tutorRatePerHour (tutor's real rate).
 * 3. Live token cost preview shown before submission.
 */
public class NewSessionRequestActivity extends AppCompatActivity {

    private AutoCompleteTextView spinnerSubject;
    private EditText etTopic, etGoals, etCustomDuration;
    private Button btn30, btn45, btn60, btn90, btnSubmit;
    private TextView tvCostPreview, tvTutorSubjectsLabel;
    private int selectedDuration = 60;

    private FirebaseFirestore db;
    private String currentUid;
    private String currentUserName;

    // Tutor info passed from TutorDetailActivity
    private String tutorUid;
    private String tutorName;
    private int tutorRatePerHour = 100; // default, overridden by Intent

    // Fallback full subject list (used only if tutor profile unavailable)
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

        // Read tutor info from Intent
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

        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        if (tvTutorSubjectsLabel != null && tutorName != null && !tutorName.isEmpty()) {
            tvTutorSubjectsLabel.setText("Book with " + tutorName);
        }

        setupDurationButtons();

        if (btnSubmit != null) btnSubmit.setOnClickListener(v -> { SoundManager.playClick(this); checkTokensAndPost(); });

        // Load tutor's actual subjects from Firestore
        loadTutorSubjects();
    }

    /**
     * Fetches the tutor's subjects from Firestore and populates the spinner
     * with ONLY those subjects. This prevents students from requesting a subject
     * the tutor doesn't teach.
     */
    private void loadTutorSubjects() {
        if (tutorUid == null || tutorUid.isEmpty()) {
            setupSubjectDropdown(getAllSubjectsList());
            return;
        }

        db.collection("users").document(tutorUid).get()
                .addOnSuccessListener(doc -> {
                    List<String> subjects = null;
                    if (doc.exists()) {
                        //noinspection unchecked
                        subjects = (List<String>) doc.get("subjects");
                    }
                    if (subjects == null || subjects.isEmpty()) {
                        subjects = getAllSubjectsList();
                    }
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
            // Auto-select if only one subject
            if (subjects.size() == 1) {
                spinnerSubject.setText(subjects.get(0), false);
            }
        }
    }

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

    /** Shows the calculated token cost live as duration changes. */
    private void updateCostPreview() {
        if (tvCostPreview == null) return;
        int cost = calculateTokenCost(selectedDuration);
        tvCostPreview.setText("Cost: " + cost + " tokens  ("
                + tutorRatePerHour + " tokens/hr)");
    }

    /**
     * Token cost = ceil(durationMinutes / 60.0) × tutorRatePerHour.
     * e.g. 45 min @ 80/hr = 80 tokens (1 full hour billed)
     *      90 min @ 80/hr = 160 tokens (2 hours)
     */
    private int calculateTokenCost(int durationMinutes) {
        return Math.max(1, (int) Math.ceil(durationMinutes / 60.0) * tutorRatePerHour);
    }

    private void checkTokensAndPost() {
        String subject   = spinnerSubject != null ? spinnerSubject.getText().toString().trim() : "";
        String topic     = etTopic != null ? etTopic.getText().toString().trim() : "";
        String goals     = etGoals != null ? etGoals.getText().toString().trim() : "";
        String customDur = etCustomDuration != null
                ? etCustomDuration.getText().toString().trim() : "";

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
        if (currentUid == null || currentUid.isEmpty()) {
            Toast.makeText(this, "Please sign in first", Toast.LENGTH_SHORT).show();
            return;
        }

        final int tokenCost    = calculateTokenCost(finalDuration);
        final int dur          = finalDuration;
        final String subFinal  = subject;
        final String topFinal  = topic;
        final String goalFinal = goals;

        if (btnSubmit != null) {
            btnSubmit.setEnabled(false);
            btnSubmit.setText("Checking balance...");
        }

        db.collection("users").document(currentUid).get()
                .addOnSuccessListener(doc -> {
                    Long bal = doc.getLong("tokens");
                    long currentTokens = (bal != null) ? bal : 100L;

                    if (currentTokens < tokenCost) {
                        SoundManager.playError(this);
                        Toast.makeText(this,
                                "❌ Need " + tokenCost + " tokens, you have " + currentTokens,
                                Toast.LENGTH_LONG).show();
                        resetSubmitButton();
                        return;
                    }

                    deductAndPost(currentTokens - tokenCost, tokenCost,
                            subFinal, topFinal, goalFinal, dur);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    resetSubmitButton();
                });
    }

    private void deductAndPost(long newBalance, int tokenCost, String subject,
                               String topic, String goals, int duration) {
        if (btnSubmit != null) btnSubmit.setText("Posting...");

        db.collection("users").document(currentUid)
                .update("tokens", newBalance)
                .addOnSuccessListener(unused -> {
                    SessionRequest req = new SessionRequest(
                            currentUid, currentUserName, subject, topic, goals, duration);
                    req.setStatus(SessionRequest.STATUS_REQUESTED);
                    req.setTokens(tokenCost);
                    if (tutorUid  != null && !tutorUid.isEmpty())  req.setTutorUid(tutorUid);
                    if (tutorName != null && !tutorName.isEmpty()) req.setTutorName(tutorName);

                    db.collection("sessionRequests").add(req)
                            .addOnSuccessListener(docRef -> {
                                docRef.update("requestId", docRef.getId());
                                SoundManager.playSuccess(this);
                                Toast.makeText(this,
                                        "✅ Booked! " + tokenCost + " tokens reserved.",
                                        Toast.LENGTH_LONG).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                // Refund on failure
                                db.collection("users").document(currentUid)
                                        .update("tokens", newBalance + tokenCost);
                                Toast.makeText(this, "Error: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                                resetSubmitButton();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    resetSubmitButton();
                });
    }

    private void resetSubmitButton() {
        if (btnSubmit != null) {
            btnSubmit.setEnabled(true);
            btnSubmit.setText("Post Session Request");
        }
    }
}