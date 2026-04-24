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
import com.example.peertutoring.models.SessionRequest;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Activity for students to create and submit a new tutoring session request.
 *
 * Fix: Token cost is now calculated using the TUTOR'S actual rate (passed via
 * Intent from TutorDetailActivity), not a hardcoded 100/hr default.
 * Formula: tokenCost = ceil(durationMinutes / 60.0) * tutorRatePerHour
 */
public class NewSessionRequestActivity extends AppCompatActivity {

    private AutoCompleteTextView spinnerSubject;
    private EditText etTopic, etGoals, etCustomDuration;
    private Button btn30, btn45, btn60, btn90, btnSubmit;
    private int selectedDuration = 60;

    private FirebaseFirestore db;
    private String currentUid;
    private String currentUserName;

    // Tutor details passed from TutorDetailActivity
    private String tutorUid;
    private String tutorName;
    private int tutorRatePerHour; // actual token rate charged by this tutor

    private static final String[] SUBJECTS = {
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

        // Read tutor info passed from TutorDetailActivity
        tutorUid         = getIntent().getStringExtra("tutorUid");
        tutorName        = getIntent().getStringExtra("tutorName");
        tutorRatePerHour = getIntent().getIntExtra("tutorRate", 100); // defaults to 100 if not passed

        spinnerSubject   = findViewById(R.id.spinnerSubject);
        etTopic          = findViewById(R.id.etTopic);
        etGoals          = findViewById(R.id.etGoals);
        etCustomDuration = findViewById(R.id.etCustomDuration);
        btn30            = findViewById(R.id.btn30min);
        btn45            = findViewById(R.id.btn45min);
        btn60            = findViewById(R.id.btn60min);
        btn90            = findViewById(R.id.btn90min);
        btnSubmit        = findViewById(R.id.btnSubmitRequest);

        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        // Show tutor name in header if available
        if (tutorName != null && !tutorName.isEmpty()) {
            TextView tvHeader = findViewById(R.id.tvScreenTitle);
            if (tvHeader != null) tvHeader.setText("Book session with " + tutorName);
        }

        setupSubjectDropdown();
        setupDurationButtons();

        if (btnSubmit != null) btnSubmit.setOnClickListener(v -> checkTokensAndPost());
    }

    private void setupSubjectDropdown() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, SUBJECTS);
        spinnerSubject.setAdapter(adapter);
        spinnerSubject.setOnClickListener(v -> spinnerSubject.showDropDown());
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
        Button[] btns = {btn30, btn45, btn60, btn90};
        for (Button b : btns) {
            if (b == null) continue;
            b.setBackground(null);
            b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    Color.parseColor("#F3F4F6")));
            b.setTextColor(Color.parseColor("#4B5D7A"));
        }
    }

    /**
     * Shows a live token cost preview so the student knows how much they'll spend
     * before submitting the request.
     */
    private void updateCostPreview() {
        int cost = calculateTokenCost(selectedDuration);
        TextView tvCost = findViewById(R.id.tvTokenCost);
        if (tvCost != null) {
            tvCost.setText("Cost: " + cost + " tokens  (rate: "
                    + tutorRatePerHour + " tokens/hr)");
        }
    }

    /**
     * Calculates the token cost based on the tutor's actual hourly rate.
     * Sessions are billed in whole hours (rounded up).
     * e.g. 45 min at 80 tokens/hr = 80 tokens (1 hr rounded up)
     *      90 min at 80 tokens/hr = 160 tokens (2 hrs)
     */
    private int calculateTokenCost(int durationMinutes) {
        int hours = (int) Math.ceil(durationMinutes / 60.0);
        return Math.max(1, hours * tutorRatePerHour);
    }

    private void checkTokensAndPost() {
        String subject   = spinnerSubject.getText().toString().trim();
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
            spinnerSubject.setError("Please select a subject"); return;
        }
        if (TextUtils.isEmpty(topic)) {
            if (etTopic != null) etTopic.setError("Topic is required"); return;
        }
        if (currentUid.isEmpty()) {
            Toast.makeText(this, "Please sign in first", Toast.LENGTH_SHORT).show(); return;
        }

        // Use tutor's actual rate for cost calculation
        final int tokenCost   = calculateTokenCost(finalDuration);
        final int dur         = finalDuration;
        final String subFinal = subject;
        final String topFinal = topic;
        final String goalFinal = goals;

        if (btnSubmit != null) {
            btnSubmit.setEnabled(false);
            btnSubmit.setText("Checking balance...");
        }

        db.collection("users").document(currentUid).get()
                .addOnSuccessListener(doc -> {
                    Long currentTokens = doc.getLong("tokens");
                    if (currentTokens == null) currentTokens = 100L;

                    if (currentTokens < tokenCost) {
                        Toast.makeText(this,
                                "❌ Insufficient tokens! Need " + tokenCost
                                        + " but you have " + currentTokens + ".",
                                Toast.LENGTH_LONG).show();
                        resetSubmitButton();
                        return;
                    }

                    deductAndPost(currentTokens - tokenCost, tokenCost,
                            subFinal, topFinal, goalFinal, dur);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error checking balance: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    resetSubmitButton();
                });
    }

    private void deductAndPost(long newBalance, int tokenCost, String subject,
                               String topic, String goals, int duration) {
        if (btnSubmit != null) btnSubmit.setText("Posting...");

        // Step 1: deduct tokens
        db.collection("users").document(currentUid)
                .update("tokens", newBalance)
                .addOnSuccessListener(unused -> {

                    // Step 2: write session request
                    SessionRequest req = new SessionRequest(
                            currentUid, currentUserName, subject, topic, goals, duration);
                    req.setStatus(SessionRequest.STATUS_REQUESTED);
                    req.setTokens(tokenCost);
                    if (tutorUid  != null && !tutorUid.isEmpty())  req.setTutorUid(tutorUid);
                    if (tutorName != null && !tutorName.isEmpty()) req.setTutorName(tutorName);

                    db.collection("sessionRequests")
                            .add(req)
                            .addOnSuccessListener(docRef -> {
                                docRef.update("requestId", docRef.getId());
                                Toast.makeText(this,
                                        "✅ Request posted! " + tokenCost + " tokens reserved.",
                                        Toast.LENGTH_LONG).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                // Refund tokens if request write fails
                                db.collection("users").document(currentUid)
                                        .update("tokens", newBalance + tokenCost);
                                Toast.makeText(this, "Error: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                                resetSubmitButton();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to reserve tokens: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
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