package com.example.peertutoring.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.peertutoring.R;
import com.example.peertutoring.utils.SoundManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Main landing screen of the application.
 * Synchronized with the updated dashboard layout.
 */
public class HomeActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private String userRole = "student";
    
    private TextView tvUserName, tvTokenBalance, tvGreeting;
    private TextView tvUpcomingSubject, tvUpcomingTime;
    private ListenerRegistration userListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        bindViews();
        setupClickListeners();
        setupBottomNav();
    }

    private void bindViews() {
        tvGreeting = findViewById(R.id.tvGreeting);
        tvUserName = findViewById(R.id.tvUserName);
        tvTokenBalance = findViewById(R.id.tvTokenBalance);
        tvUpcomingSubject = findViewById(R.id.tvUpcomingSubject);
        tvUpcomingTime = findViewById(R.id.tvUpcomingTime);
    }

    private void setupClickListeners() {
        // Search bar opens Browse
        View searchBar = findViewById(R.id.tvSearchHint);
        if (searchBar != null) {
            searchBar.setOnClickListener(v -> {
                SoundManager.playClick(this);
                startActivity(new Intent(this, BrowseTutorsActivity.class));
            });
        }

        // Quick Actions
        View cardBrowse = findViewById(R.id.cardBrowseTutors);
        if (cardBrowse != null) {
            cardBrowse.setOnClickListener(v -> {
                SoundManager.playClick(this);
                startActivity(new Intent(this, BrowseTutorsActivity.class));
            });
        }

        View cardMyRequests = findViewById(R.id.cardMyRequests);
        if (cardMyRequests != null) {
            cardMyRequests.setOnClickListener(v -> {
                SoundManager.playClick(this);
                startActivity(new Intent(this, SessionRequestsActivity.class));
            });
        }

        // Token balance chip in header — tapping shows purchase screen (US-23)
        View chipTokenBalance = findViewById(R.id.chipTokenBalance);
        if (chipTokenBalance != null) {
            chipTokenBalance.setOnClickListener(v -> {
                SoundManager.playClick(this);
                startActivity(new Intent(this, BuyTokensActivity.class));
            });
        }

        // "Need more tokens?" CTA card at the bottom — same destination (US-23)
        View cardBuyTokens = findViewById(R.id.cardBuyTokens);
        if (cardBuyTokens != null) {
            cardBuyTokens.setOnClickListener(v -> {
                SoundManager.playClick(this);
                startActivity(new Intent(this, BuyTokensActivity.class));
            });
        }

        // View All Sessions
        View tvViewAll = findViewById(R.id.tvViewAll);
        if (tvViewAll != null) {
            tvViewAll.setOnClickListener(v -> {
                SoundManager.playClick(this);
                startActivity(new Intent(this, SessionRequestsActivity.class));
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentUser != null) {
            startUserListener();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (userListener != null) {
            userListener.remove();
            userListener = null;
        }
    }

    private void startUserListener() {
        userListener = db.collection("users").document(currentUser.getUid())
                .addSnapshotListener((doc, e) -> {
                    if (e != null || doc == null || !doc.exists()) return;

                    String name = doc.getString("firstName");
                    String fullName = doc.getString("fullName");
                    Long tokens = doc.getLong("tokens");
                    String role = doc.getString("role");
                    
                    if (role != null) userRole = role;

                    if (tvUserName != null) {
                        tvUserName.setText(fullName != null ? fullName : "User");
                    }

                    if (tvTokenBalance != null) {
                        long bal = tokens != null ? tokens : 0;
                        tvTokenBalance.setText(NumberFormat.getNumberInstance(Locale.getDefault()).format(bal));
                    }
                    
                    if (tvGreeting != null && name != null) {
                        tvGreeting.setText("Welcome back, " + name + " 👋");
                    }
                });
    }

    private void setupBottomNav() {
        View navHome     = findViewById(R.id.navHome);
        View navBrowse   = findViewById(R.id.navBrowse);
        View navMessages = findViewById(R.id.navMessages);
        View navProfile  = findViewById(R.id.navProfile);

        if (navHome != null) navHome.setOnClickListener(v -> { /* Already here */ });

        if (navBrowse != null) {
            navBrowse.setOnClickListener(v -> {
                SoundManager.playClick(this);
                startActivity(new Intent(this, BrowseTutorsActivity.class));
                overridePendingTransition(0, 0);
            });
        }

        if (navMessages != null) {
            navMessages.setOnClickListener(v -> {
                SoundManager.playClick(this);
                startActivity(new Intent(this, 
                    "tutor".equals(userRole) ? TutorRequestsActivity.class : SessionRequestsActivity.class));
                overridePendingTransition(0, 0);
            });
        }

        if (navProfile != null) {
            navProfile.setOnClickListener(v -> {
                SoundManager.playClick(this);
                startActivity(new Intent(this, EditProfileActivity.class));
                overridePendingTransition(0, 0);
            });
        }
    }
}