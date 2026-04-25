package com.example.peertutoring.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.peertutoring.R;
import com.example.peertutoring.utils.SoundManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class RateReviewActivity extends AppCompatActivity {

    private Button star1, star2, star3, star4, star5, btnSubmit;
    private EditText etReviewText;
    private TextView tvRatingLabel;
    private FirebaseFirestore db;

    private int selectedRating = 0;
    private String tutorUid, tutorName, requestId, studentUid, studentName;

    private static final String[] RATING_LABELS = {
            "Tap a star to rate", "Poor", "Fair", "Good", "Very Good", "Excellent"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rate_review);

        db = FirebaseFirestore.getInstance();

        tutorUid  = getIntent().getStringExtra("tutorUid");
        tutorName = getIntent().getStringExtra("tutorName");
        requestId = getIntent().getStringExtra("requestId");

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        studentUid  = user != null ? user.getUid() : "";
        studentName = (user != null && user.getDisplayName() != null)
                ? user.getDisplayName() : "Student";

        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        TextView tvSubtitle = findViewById(R.id.tvTutorSubtitle);
        if (tvSubtitle != null && tutorName != null)
            tvSubtitle.setText("Your tutor: " + tutorName);

        star1 = findViewById(R.id.star1);
        star2 = findViewById(R.id.star2);
        star3 = findViewById(R.id.star3);
        star4 = findViewById(R.id.star4);
        star5 = findViewById(R.id.star5);
        etReviewText  = findViewById(R.id.etReviewText);
        tvRatingLabel = findViewById(R.id.tvRatingLabel);
        btnSubmit     = findViewById(R.id.btnSubmitReview);

        setupStars();

        if (btnSubmit != null)
            btnSubmit.setOnClickListener(v -> { SoundManager.playClick(this); validateAndSubmit(); });
    }

    private void setupStars() {
        Button[] stars = {star1, star2, star3, star4, star5};
        for (int i = 0; i < stars.length; i++) {
            final int rating = i + 1;
            if (stars[i] != null)
                stars[i].setOnClickListener(v -> setRating(rating));
        }
    }

    private void setRating(int rating) {
        selectedRating = rating;
        Button[] stars = {star1, star2, star3, star4, star5};
        for (int i = 0; i < stars.length; i++) {
            if (stars[i] != null)
                stars[i].setText(i < rating ? "⭐" : "☆");
        }
        if (tvRatingLabel != null)
            tvRatingLabel.setText(RATING_LABELS[rating]);
    }

    private void validateAndSubmit() {
        if (selectedRating == 0) {
            Toast.makeText(this, "Please select a star rating", Toast.LENGTH_SHORT).show();
            return;
        }
        if (studentUid.isEmpty() || tutorUid == null) {
            Toast.makeText(this, "Session data missing", Toast.LENGTH_SHORT).show();
            return;
        }

        if (btnSubmit != null) {
            btnSubmit.setEnabled(false);
            btnSubmit.setText("Submitting...");
        }

        writeReviewAndUpdateRating();
    }

    private void writeReviewAndUpdateRating() {
        String reviewText = etReviewText != null
                ? etReviewText.getText().toString().trim() : "";

        Map<String, Object> review = new HashMap<>();
        review.put("studentUid",  studentUid);
        review.put("studentName", studentName);
        review.put("tutorUid",    tutorUid);
        review.put("requestId",   requestId);
        review.put("rating",      (double) selectedRating);
        review.put("reviewText",  reviewText);
        review.put("createdAt",   FieldValue.serverTimestamp());

        db.collection("reviews").add(review)
                .addOnSuccessListener(docRef -> {
                    markReviewSubmitted();
                    updateTutorAverageRating();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    resetButton();
                });
    }

    private void markReviewSubmitted() {
        if (requestId == null || requestId.startsWith("mock_")) return;
        db.collection("sessionRequests").document(requestId)
                .update("reviewSubmitted", true);
    }

    private void updateTutorAverageRating() {
        db.collection("reviews")
                .whereEqualTo("tutorUid", tutorUid)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap == null || snap.isEmpty()) { onSuccess(); return; }

                    double sum = 0;
                    int count = 0;
                    for (QueryDocumentSnapshot doc : snap) {
                        Double r = doc.getDouble("rating");
                        if (r != null) { sum += r; count++; }
                    }
                    double newAvg = count > 0 ? Math.round((sum / count) * 10.0) / 10.0 : 0;

                    db.collection("users").document(tutorUid)
                            .update("rating", newAvg)
                            .addOnSuccessListener(u -> onSuccess())
                            .addOnFailureListener(e -> onSuccess()); // non-critical
                })
                .addOnFailureListener(e -> onSuccess());
    }

    private void onSuccess() {
        SoundManager.playSuccess(this);
        Toast.makeText(this, "Review submitted! Thank you.", Toast.LENGTH_LONG).show();
        finish();
    }

    private void resetButton() {
        if (btnSubmit != null) {
            btnSubmit.setEnabled(true);
            btnSubmit.setText("Submit Review");
        }
    }
}
