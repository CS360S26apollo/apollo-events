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
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * US 08: Post a new session request.
 * Updated for US 16 status tracking.
 */
public class NewSessionRequestActivity extends AppCompatActivity {

    private AutoCompleteTextView spinnerSubject;
    private EditText etTopic, etGoals, etCustomDuration;
    private Button btn30, btn45, btn60, btn90, btnSubmit;
    private int selectedDuration = 60;

    private FirebaseFirestore db;
    private String currentUid;
    private String currentUserName;

    // Subjects list for the dropdown
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

        // Bind Views
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
        btnSubmit.setOnClickListener(v -> postRequest());
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

    private void postRequest() {
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

        // Creating using the SessionRequest model for consistency
        SessionRequest newReq = new SessionRequest(currentUid, currentUserName, subject, topic, goals, finalDuration);
        newReq.setStatus(SessionRequest.STATUS_REQUESTED);

        btnSubmit.setEnabled(false);
        btnSubmit.setText("Posting...");

        db.collection("sessionRequests")
                .add(newReq)
                .addOnSuccessListener(docRef -> {
                    // Update the document with the ID generated by Firestore
                    docRef.update("requestId", docRef.getId());
                    Toast.makeText(this, "✅ Request posted!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnSubmit.setEnabled(true);
                    btnSubmit.setText("Post Session Request");
                });
    }
}