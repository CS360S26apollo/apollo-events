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
import com.example.peertutoring.models.Tutor;
import com.example.peertutoring.utils.RankingEngine;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    private List<String> studentSubjects = new ArrayList<>();
    private Map<String, List<Integer>> studentPreferredHours = null;
    private int studentBudget = Integer.MAX_VALUE;
    private String studentLevelPref = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browse_tutors);

        userRepository = new UserRepository();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        layoutTutorList = findViewById(R.id.layoutTutorList);
        tvResultCount = findViewById(R.id.tvResultCount);

        setupSearch();
        setupPreferencesButton();
        setupBottomNav();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-fetch every time we come back (picks up preference changes from wizard)
        if (currentUser != null) {
            loadRecommendations();
        } else {
            fetchTutors(new ArrayList<>());
        }
    }

    /**
     * US 5 & 6: Loads recommended tutors based on student's subjects and preferences,
     * then ranks them using the RankingEngine.
     */
    @SuppressWarnings("unchecked")
    private void loadRecommendations() {
        userRepository.getUserProfile(currentUser.getUid(), new UserRepository.LoadCallback<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot doc) {
                List<String> prefs = (List<String>) doc.get("subjects");
                if (prefs != null) studentSubjects = prefs;

                Map<String, List<Integer>> hours = (Map<String, List<Integer>>) doc.get("preferredHours");
                if (hours != null) studentPreferredHours = hours;

                // Read preferences saved by the StudentPreferencesActivity wizard
                Long budget = doc.getLong("budgetTokens");
                if (budget != null) studentBudget = budget.intValue();
                studentLevelPref = doc.getString("tutorLevel");

                fetchTutors(studentSubjects);
            }

            @Override
            public void onFailure(String error) {
                fetchTutors(new ArrayList<>());
            }
        });
    }

    private void fetchTutors(List<String> preferences) {
        userRepository.getRecommendedTutors(preferences, new UserRepository.LoadCallback<List<DocumentSnapshot>>() {
            @Override
            public void onSuccess(List<DocumentSnapshot> docs) {
                // Convert DocumentSnapshots to Tutor models for ranking
                List<Tutor> tutorModels = new ArrayList<>();
                for (DocumentSnapshot doc : docs) {
                    Tutor t = doc.toObject(Tutor.class);
                    if (t != null) {
                        t.setUid(doc.getId());
                        tutorModels.add(t);
                    }
                }

                // Apply US 06 ranking
                List<Tutor> ranked = RankingEngine.rankTutors(
                        tutorModels, studentSubjects, studentPreferredHours);

                // Re-order docs to match ranked order
                List<DocumentSnapshot> rankedDocs = new ArrayList<>();
                java.util.Set<String> rankedIds = new java.util.HashSet<>();
                for (Tutor t : ranked) {
                    for (DocumentSnapshot doc : docs) {
                        if (doc.getId().equals(t.getUid())) {
                            rankedDocs.add(doc);
                            rankedIds.add(doc.getId());
                            break;
                        }
                    }
                }
                // Append any docs that failed deserialization so they still appear
                for (DocumentSnapshot doc : docs) {
                    if (!rankedIds.contains(doc.getId())) {
                        rankedDocs.add(doc);
                    }
                }

                allTutors = rankedDocs;
                displayTutors(rankedDocs);
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
        
        LayoutInflater inflater = LayoutInflater.from(this);

        int displayedCount = 0;
        for (DocumentSnapshot doc : tutors) {
            // Apply budget filter from preferences wizard
            Long rate = doc.getLong("rate");
            if (rate != null && studentBudget < Integer.MAX_VALUE && rate > studentBudget) {
                continue;
            }

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
            Double rating = doc.getDouble("rating");
            Boolean isVerified = doc.getBoolean("verified");
            displayedCount++;

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

            final String tutorUid = doc.getId();
            itemView.setOnClickListener(v -> openTutorDetail(
                    tutorUid,
                    fullName,
                    subjects != null && !subjects.isEmpty() ? subjects.get(0) : "Tutor",
                    String.valueOf(rate != null ? rate : 0),
                    String.valueOf(rating != null ? rating : 0.0),
                    "0",
                    Boolean.TRUE.equals(isVerified)
            ));

            layoutTutorList.addView(itemView);
        }

        if (tvResultCount != null) {
            String label = "Found " + displayedCount + " tutor" + (displayedCount != 1 ? "s" : "");
            if (studentBudget < Integer.MAX_VALUE) {
                label += " within " + studentBudget + " token budget";
            }
            tvResultCount.setText(label);
        }
    }

    private void setupPreferencesButton() {
        View btn = findViewById(R.id.btnFilter);
        if (btn == null) return;
        btn.setOnClickListener(v ->
                startActivity(new Intent(this, StudentPreferencesActivity.class)));
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

    private void openTutorDetail(String tutorUid, String name, String subject, String rate,
                                 String rating, String students, boolean isVerified) {
        Intent intent = new Intent(this, TutorDetailActivity.class);
        intent.putExtra("tutorUid", tutorUid);
        intent.putExtra("name", name);
        intent.putExtra("subject", subject);
        intent.putExtra("rate", rate);
        intent.putExtra("rating", rating);
        intent.putExtra("students", students);
        intent.putExtra("isVerified", isVerified);
        startActivity(intent);
    }
}