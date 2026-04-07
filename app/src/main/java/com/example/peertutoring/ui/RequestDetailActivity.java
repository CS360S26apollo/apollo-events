package com.example.peertutoring.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.peertutoring.R;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Activity that displays the full details of a specific session request.
 * Role: Tracking/Management View for User Story 10 (Track Request Status) 
 * and User Story 09 (Tutor Response).
 * 
 * Purpose: Provides a dual-purpose interface. Students use it to track the 
 * progress of their pending requests, while tutors use it to review and 
 * potentially accept or decline incoming session proposals.
 * 
 * Design Pattern: Dynamic View-Model populated by Intent or Firestore.
 */
public class RequestDetailActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String requestId;
    private boolean isStudentView;
    private LinearLayout layoutOfferContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_detail);

        db = FirebaseFirestore.getInstance();
        
        requestId = getIntent().getStringExtra("requestId");
        String intentTopic = getIntent().getStringExtra("topic");
        String intentStatus = getIntent().getStringExtra("status");
        String intentProvider = getIntent().getStringExtra("provider");
        String intentCategory = getIntent().getStringExtra("category");
        int intentDuration = getIntent().getIntExtra("duration", 60);
        int intentTokens = getIntent().getIntExtra("tokens", 0);
        isStudentView = getIntent().getBooleanExtra("isStudentView", false);

        layoutOfferContainer = findViewById(R.id.layoutOfferContainer);

        populateFromIntent(intentTopic, intentStatus, intentProvider, intentCategory, intentDuration, intentTokens);
        
        setupButtons();
        
        if (requestId != null && !requestId.startsWith("mock_")) {
            loadRequestData();
            if (isStudentView) {
                loadProviderOffers();
            }
        }
    }

    /**
     * Quickly populates the UI using data passed through the navigation Intent.
     */
    private void populateFromIntent(String topic, String status, String provider, String category, int duration, int tokens) {
        setText(R.id.tvTopic, topic != null ? topic : "Request Detail");
        setText(R.id.tvProviderName, provider != null ? provider : "Tutor Pending");
        setText(R.id.tvCategory, category != null ? category : "Education");
        setText(R.id.tvTokens, String.valueOf(tokens));
        setText(R.id.tvDuration, (duration / 60) + " hours");
        updateStatusUI(status);
    }

    /**
     * Subscribes to real-time updates for the session request document in Firestore.
     */
    private void loadRequestData() {
        db.collection("sessionRequests").document(requestId)
                .addSnapshotListener((doc, e) -> {
                    if (e != null || doc == null || !doc.exists()) return;
                    String status = doc.getString("status");
                    String provider = doc.getString("tutorName");
                    setText(R.id.tvProviderName, provider != null ? provider : "Searching...");
                    updateStatusUI(status);
                });
    }

    /**
     * Adjusts the visual badge (color and text) based on the current status of the request.
     * @param status The current state string (e.g., 'waiting', 'accepted', 'cancelled').
     */
    private void updateStatusUI(String status) {
        TextView tvStatusBadge = findViewById(R.id.tvStatusBadge);
        MaterialCardView cardStatusBadge = findViewById(R.id.cardStatusBadge);
        if (tvStatusBadge != null && cardStatusBadge != null) {
            String displayStatus = status != null ? status.toUpperCase() : "PENDING";
            if ("waiting".equals(status)) displayStatus = "PENDING";
            tvStatusBadge.setText(displayStatus);
            
            if ("waiting".equals(status) || "pending".equals(status)) {
                tvStatusBadge.setTextColor(Color.parseColor("#007AFF"));
                cardStatusBadge.setCardBackgroundColor(Color.parseColor("#E6F2FF"));
            } else if ("accepted".equals(status)) {
                tvStatusBadge.setTextColor(Color.parseColor("#34C759"));
                cardStatusBadge.setCardBackgroundColor(Color.parseColor("#EAF9EE"));
            } else if ("cancelled".equals(status) || "declined".equals(status)) {
                tvStatusBadge.setTextColor(Color.parseColor("#FF3B30"));
                cardStatusBadge.setCardBackgroundColor(Color.parseColor("#FFECEB"));
            }
        }
    }

    /**
     * Fetches and displays any counter-offers or bids from tutors (for student view).
     */
    private void loadProviderOffers() {
        db.collection("sessionRequests").document(requestId).collection("offers")
                .addSnapshotListener((snap, e) -> {
                    if (snap != null) displayOffers(snap.getDocuments());
                });
    }

    private void displayOffers(java.util.List<DocumentSnapshot> offers) {
        if (layoutOfferContainer == null) return;
        layoutOfferContainer.removeAllViews();
        if (offers.isEmpty()) {
            findViewById(R.id.layoutProviderList).setVisibility(View.GONE);
            return;
        }
        findViewById(R.id.layoutProviderList).setVisibility(View.VISIBLE);
        LayoutInflater inflater = LayoutInflater.from(this);
        for (DocumentSnapshot doc : offers) {
            View offerView = inflater.inflate(R.layout.item_tutor_offer, layoutOfferContainer, false);
            TextView tvName = offerView.findViewById(R.id.tvTutorName);
            if (tvName != null) tvName.setText(doc.getString("tutorName"));
            layoutOfferContainer.addView(offerView);
        }
    }

    private void setupButtons() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        findViewById(R.id.btnCancelRequest).setOnClickListener(v -> {
            if (requestId != null && requestId.startsWith("mock_")) {
                updateStatusUI("cancelled");
                Toast.makeText(this, "Request Cancelled", Toast.LENGTH_SHORT).show();
                v.postDelayed(this::finish, 500);
            } else {
                updateFirestoreStatus("cancelled");
                finish();
            }
        });

        findViewById(R.id.btnAcceptRequest).setOnClickListener(v -> updateFirestoreStatus("accepted"));
        findViewById(R.id.btnDeclineRequest).setOnClickListener(v -> updateFirestoreStatus("declined"));
    }

    /**
     * Updates the status field of the session request in Firestore.
     * @param status The new status value to persist.
     */
    private void updateFirestoreStatus(String status) {
        if (requestId == null || requestId.startsWith("mock_")) return;
        db.collection("sessionRequests").document(requestId).update("status", status);
    }

    private void setText(int viewId, String text) {
        TextView tv = findViewById(viewId);
        if (tv != null && text != null) tv.setText(text);
    }
}