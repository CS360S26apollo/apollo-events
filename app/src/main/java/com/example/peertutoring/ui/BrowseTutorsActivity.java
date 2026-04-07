package com.example.peertutoring.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.peertutoring.R;
import com.example.peertutoring.data.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity for browsing and searching available tutors in the marketplace.
 * Role: Search and Discovery View for User Story 05 (Recommended Tutors).
 * Purpose: Allows students to find tutors based on their learning preferences 
 * and search queries. Displays tutor ratings, rates, and verification status.
 * 
 * Design Pattern: View-Controller with preference-based data fetching.
 * 
 * Implementation Details:
 * - Automatically loads recommendations based on the student's saved subjects.
 * - Provides real-time filtering of the tutor list as the user types in the search bar.
 */
public class BrowseTutorsActivity extends AppCompatActivity {

    private UserRepository userRepository;
    private FirebaseUser currentUser;
    private LinearLayout layoutTutorList;
    private TextView tvResultCount;
    private List<DocumentSnapshot> allTutors = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browse_tutors);

        userRepository = new UserRepository();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        layoutTutorList = findViewById(R.id.layoutTutorList);
        tvResultCount = findViewById(R.id.tvResultCount);

        setupSearch();
        setupBottomNav();

        if (currentUser != null) {
            loadRecommendations();
        } else {
            fetchTutors(new ArrayList<>());
        }
    }

    /**
     * Fetches the current user's profile to retrieve their subject preferences.
     * Implementation of US 05 personalized discovery.
     */
    private void loadRecommendations() {
        userRepository.getUserProfile(currentUser.getUid(), new UserRepository.LoadCallback<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot doc) {
                List<String> preferences = (List<String>) doc.get("subjects");
                if (preferences == null) preferences = new ArrayList<>();
                fetchTutors(preferences);
            }

            @Override
            public void onFailure(String error) {
                fetchTutors(new ArrayList<>());
            }
        });
    }

    /**
     * Queries the repository for tutors that match the provided preferences.
     * @param preferences List of subjects the student is interested in.
     */
    private void fetchTutors(List<String> preferences) {
        userRepository.getRecommendedTutors(preferences, new UserRepository.LoadCallback<List<DocumentSnapshot>>() {
            @Override
            public void onSuccess(List<DocumentSnapshot> tutors) {
                allTutors = tutors;
                displayTutors(tutors);
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(BrowseTutorsActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Renders the tutor cards into the UI container.
     * Includes handling for initials generation and verified badge visibility.
     */
    private void displayTutors(List<DocumentSnapshot> tutors) {
        if (layoutTutorList == null) return;
        layoutTutorList.removeAllViews();
        
        if (tvResultCount != null) {
            tvResultCount.setText("Found " + tutors.size() + " tutors");
        }

        LayoutInflater inflater = LayoutInflater.from(this);

        for (DocumentSnapshot doc : tutors) {
            View itemView = inflater.inflate(R.layout.item_tutor, layoutTutorList, false);
            
            TextView tvName = itemView.findViewById(R.id.tvTutorName);
            TextView tvSubjects = itemView.findViewById(R.id.tvTutorSubjects);
            TextView tvRate = itemView.findViewById(R.id.tvTutorRate);
            TextView tvRating = itemView.findViewById(R.id.tvRating);
            TextView tvInitials = itemView.findViewById(R.id.tvTutorInitials);
            View badgeVerified = itemView.findViewById(R.id.badgeVerified);

            String firstName = doc.getString("firstName");
            String lastName = doc.getString("lastName");
            String fullName = doc.getString("fullName");
            List<String> subjects = (List<String>) doc.get("subjects");
            Long rate = doc.getLong("rate");
            Double rating = doc.getDouble("rating");
            Boolean isVerified = doc.getBoolean("verified");

            if (tvName != null) tvName.setText(fullName);
            if (tvSubjects != null && subjects != null) {
                tvSubjects.setText(String.join(", ", subjects));
            }
            if (tvRate != null && rate != null) tvRate.setText(String.valueOf(rate));
            if (tvRating != null) tvRating.setText("⭐ " + (rating != null ? rating : "0.0"));
            
            if (tvInitials != null) {
                String initials = "";
                if (firstName != null && !firstName.isEmpty()) initials += firstName.charAt(0);
                if (lastName != null && !lastName.isEmpty()) initials += lastName.charAt(0);
                tvInitials.setText(initials.toUpperCase());
            }

            if (badgeVerified != null) {
                badgeVerified.setVisibility(Boolean.TRUE.equals(isVerified) ? View.VISIBLE : View.GONE);
            }

            itemView.setOnClickListener(v -> {
                openTutorDetail(
                    fullName, 
                    subjects != null && !subjects.isEmpty() ? subjects.get(0) : "Tutor",
                    String.valueOf(rate != null ? rate : 0),
                    String.valueOf(rating != null ? rating : 0.0),
                    "0", 
                    Boolean.TRUE.equals(isVerified)
                );
            });

            layoutTutorList.addView(itemView);
        }
    }

    /** Initializes the search bar with a text watcher for real-time filtering. */
    private void setupSearch() {
        EditText etSearch = findViewById(R.id.etSearchTutor);
        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
                @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                    filterTutors(s.toString());
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }
    }

    /**
     * Filters the local list of tutors based on a search query.
     * @param query The string to match against tutor names or subjects.
     */
    private void filterTutors(String query) {
        if (query.isEmpty()) {
            displayTutors(allTutors);
            return;
        }
        
        List<DocumentSnapshot> filtered = new ArrayList<>();
        for (DocumentSnapshot doc : allTutors) {
            String name = doc.getString("fullName");
            List<String> subjects = (List<String>) doc.get("subjects");
            
            boolean matchesName = name != null && name.toLowerCase().contains(query.toLowerCase());
            boolean matchesSubject = false;
            if (subjects != null) {
                for (String s : subjects) {
                    if (s.toLowerCase().contains(query.toLowerCase())) {
                        matchesSubject = true;
                        break;
                    }
                }
            }
            
            if (matchesName || matchesSubject) {
                filtered.add(doc);
            }
        }
        displayTutors(filtered);
    }

    private void setupBottomNav() {
        View navHome = findViewById(R.id.navHome);
        View navBrowse = findViewById(R.id.navBrowse);
        View navMessages = findViewById(R.id.navMessages);
        View navProfile = findViewById(R.id.navProfile);

        if (navHome != null) {
            navHome.setOnClickListener(v -> {
                startActivity(new Intent(this, HomeActivity.class));
                finish();
                overridePendingTransition(0, 0);
            });
        }

        if (navMessages != null) {
            navMessages.setOnClickListener(v -> {
                startActivity(new Intent(this, SessionRequestsActivity.class));
                overridePendingTransition(0, 0);
            });
        }

        if (navProfile != null) {
            navProfile.setOnClickListener(v -> {
                startActivity(new Intent(this, EditProfileActivity.class));
                overridePendingTransition(0, 0);
            });
        }
    }

    private void openTutorDetail(String name, String subject, String rate,
                                 String rating, String students, boolean isVerified) {
        Intent intent = new Intent(this, TutorDetailActivity.class);
        intent.putExtra("name", name);
        intent.putExtra("subject", subject);
        intent.putExtra("rate", rate);
        intent.putExtra("rating", rating);
        intent.putExtra("students", students);
        intent.putExtra("isVerified", isVerified);
        startActivity(intent);
    }
}