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
 * US 10: Request Detail Tracking
 * Handles both Student view (Tracking) and Tutor view (Responding).
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
        
        // Receive full data from Intent
        requestId = getIntent().getStringExtra("requestId");
        String intentTopic = getIntent().getStringExtra("topic");
        String intentStatus = getIntent().getStringExtra("status");
        String intentProvider = getIntent().getStringExtra("provider");
        String intentCategory = getIntent().getStringExtra("category");
        int intentDuration = getIntent().getIntExtra("duration", 60);
        int intentTokens = getIntent().getIntExtra("tokens", 0);
        isStudentView = getIntent().getBooleanExtra("isStudentView", false);

        layoutOfferContainer = findViewById(R.id.layoutOfferContainer);

        // Populate views immediately with Intent data
        populateFromIntent(intentTopic, intentStatus, intentProvider, intentCategory, intentDuration, intentTokens);
        
        setupButtons();
        
        // Sync with Firestore if not mock data
        if (requestId != null && !requestId.startsWith("mock_")) {
            loadRequestData();
            if (isStudentView) {
                loadProviderOffers();
            }
        }
    }

    private void populateFromIntent(String topic, String status, String provider, String category, int duration, int tokens) {
        setText(R.id.tvTopic, topic != null ? topic : "Request Detail");
        setText(R.id.tvProviderName, provider != null ? provider : "Tutor Pending");
        setText(R.id.tvCategory, category != null ? category : "Education");
        setText(R.id.tvTokens, String.valueOf(tokens));
        setText(R.id.tvDuration, (duration / 60) + " hours");
        updateStatusUI(status);
    }

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

    private void updateFirestoreStatus(String status) {
        if (requestId == null || requestId.startsWith("mock_")) return;
        db.collection("sessionRequests").document(requestId).update("status", status);
    }

    private void setText(int viewId, String text) {
        TextView tv = findViewById(viewId);
        if (tv != null && text != null) tv.setText(text);
    }
}