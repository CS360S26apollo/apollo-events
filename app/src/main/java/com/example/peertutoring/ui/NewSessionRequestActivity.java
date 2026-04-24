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
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;

import com.example.peertutoring.R;
import com.example.peertutoring.models.SessionRequest;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Activity for students to create and submit a new request for a tutoring session.
 * Role: Request Creation View for User Story 08 (Request a Session) and
 * User Story 16 (Track Session Status).
 *
 * Fix: Tokens are now deducted from the student's balance at the time of posting
 * the request, not just when accepting an offer. The token cost is calculated as
 * (durationMinutes / 60) * tutor's default rate (100 tokens/hr if unknown).
 */
public class NewSessionRequestActivity extends AppCompatActivity {

    private AutoCompleteTextView spinnerSubject;
    private EditText etTopic, etGoals, etCustomDuration;
    private Button btn30, btn45, btn60, btn90, btnSubmit;
    private int selectedDuration = 60;

    private FirebaseFirestore db;
    private String currentUid;
    private String currentUserName;

    private static final int DEFAULT_RATE_PER_HOUR = 100; // tokens per hour

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
            currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            currentUserName = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
            if (currentUserName == null || currentUserName.isEmpty()) {
                currentUserName = "Student";
            }
        } else {
            currentUid = "";
        }

        spinnerSubject = findViewById(R.id.spinnerSubject);
        etTopic = findViewById(R.id.etTopic);
        etGoals = findViewById(R.id.etGoals);
        etCustomDuration = findViewById(R.id.etCustomDuration);
        btn30 = findViewById(R.id.btn30min);
        btn45 = findViewById(R.id.btn45min);
        btn60 = findViewById(R.id.btn60min);
        btn90 = findViewById(R.id.btn90min);
        btnSubmit = findViewById(R.id.btnSubmitRequest);
        ImageButton btnBack = findViewById(R.id.btnBack);

        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        setupSubjectDropdown();
        setupDurationButtons();
        btnSubmit.setOnClickListener(v -> checkTokensAndPost());
    }

    private void setupSubjectDropdown() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                SUBJECTS
        );
        spinnerSubject.setAdapter(adapter);
        spinnerSubject.setOnClickListener(v -> spinnerSubject.showDropDown());
    }

    private void setupDurationButtons() {
        View.OnClickListener listener = v -> {
            resetDurationButtons();
            if (v.getId() == R.id.btn30min) { selectedDuration = 30; selectBtn(btn30); }
            else if (v.getId() == R.id.btn45min) { selectedDuration = 45; selectBtn(btn45); }
            else if (v.getId() == R.id.btn60min) { selectedDuration = 60; selectBtn(btn60); }
            else if (v.getId() == R.id.btn90min) { selectedDuration = 90; selectBtn(btn90); }
            etCustomDuration.setText("");
        };

        btn30.setOnClickListener(listener);
        btn45.setOnClickListener(listener);
        btn60.setOnClickListener(listener);
        btn90.setOnClickListener(listener);

        selectBtn(btn60);
    }

    private void selectBtn(Button btn) {
        btn.setBackground(AppCompatResources.getDrawable(this, R.drawable.bg_button_gradient));
        btn.setTextColor(Color.WHITE);
    }

    private void resetDurationButtons() {
        Button[] btns = {btn30, btn45, btn60, btn90};
        for (Button b : btns) {
            b.setBackground(null);
            b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#F3F4F6")));
            b.setTextColor(Color.parseColor("#4B5D7A"));
        }
    }

    /**
     * Validates form input, then checks the student's token balance before posting.
     * Tokens are deducted upfront to reserve the session cost.
     */
    private void checkTokensAndPost() {
        String topic = etTopic.getText().toString().trim();
        String goals = etGoals.getText().toString().trim();
        String subject = spinnerSubject.getText().toString().trim();
        String customDur = etCustomDuration.getText().toString().trim();

        int finalDuration = selectedDuration;
        if (!TextUtils.isEmpty(customDur)) {
            try {
                finalDuration = Integer.parseInt(customDur);
            } catch (NumberFormatException ignored) {}
        }

        if (TextUtils.isEmpty(subject)) {
            spinnerSubject.setError("Please select a subject");
            return;
        }
        if (TextUtils.isEmpty(topic)) {
            etTopic.setError("Topic is required");
            return;
        }
        if (currentUid.isEmpty()) {
            Toast.makeText(this, "Please sign in first", Toast.LENGTH_SHORT).show();
            return;
        }

        // Calculate token cost: (duration / 60) * rate, minimum 1 token
        final int tokenCost = Math.max(1, (int) Math.ceil(finalDuration / 60.0) * DEFAULT_RATE_PER_HOUR);
        final int dur = finalDuration;
        final String subjectFinal = subject;
        final String topicFinal = topic;
        final String goalsFinal = goals;

        btnSubmit.setEnabled(false);
        btnSubmit.setText("Checking balance...");

        // Check student's current token balance
        db.collection("users").document(currentUid).get()
                .addOnSuccessListener(doc -> {
                    Long currentTokens = doc.getLong("tokens");
                    if (currentTokens == null) currentTokens = 1000L; // default starting balance

                    if (currentTokens < tokenCost) {
                        Toast.makeText(this,
                                "❌ Insufficient tokens! You need " + tokenCost
                                        + " tokens but have " + currentTokens + ".",
                                Toast.LENGTH_LONG).show();
                        btnSubmit.setEnabled(true);
                        btnSubmit.setText("Post Session Request");
                        return;
                    }

                    // Deduct tokens and post the request atomically
                    final long newBalance = currentTokens - tokenCost;
                    deductAndPost(newBalance, tokenCost, subjectFinal, topicFinal, goalsFinal, dur);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error checking balance: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnSubmit.setEnabled(true);
                    btnSubmit.setText("Post Session Request");
                });
    }

    /**
     * Deducts tokens from the student's account and posts the session request.
     * Both operations are performed sequentially to keep state consistent.
     */
    private void deductAndPost(long newBalance, int tokenCost, String subject,
                               String topic, String goals, int duration) {
        btnSubmit.setText("Posting...");

        // Step 1: Deduct tokens
        db.collection("users").document(currentUid)
                .update("tokens", newBalance)
                .addOnSuccessListener(unused -> {
                    // Step 2: Post the session request
                    SessionRequest newReq = new SessionRequest(
                            currentUid, currentUserName, subject, topic, goals, duration);
                    newReq.setStatus(SessionRequest.STATUS_REQUESTED);
                    newReq.setTokens(tokenCost);

                    db.collection("sessionRequests")
                            .add(newReq)
                            .addOnSuccessListener(docRef -> {
                                docRef.update("requestId", docRef.getId());
                                Toast.makeText(this,
                                        "✅ Request posted! " + tokenCost + " tokens reserved.",
                                        Toast.LENGTH_LONG).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                // Refund tokens if request posting fails
                                db.collection("users").document(currentUid)
                                        .update("tokens", newBalance + tokenCost);
                                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                btnSubmit.setEnabled(true);
                                btnSubmit.setText("Post Session Request");
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to reserve tokens: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnSubmit.setEnabled(true);
                    btnSubmit.setText("Post Session Request");
                });
    }
}