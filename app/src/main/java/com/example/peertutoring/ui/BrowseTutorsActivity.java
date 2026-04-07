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
 * Activity for browsing and searching available tutors.
 * Implementation of User Story 5 & 6: Recommended and Ranked tutors.
 */
public class BrowseTutorsActivity extends AppCompatActivity {

    private UserRepository userRepository;
    private FirebaseUser currentUser;
    private LinearLayout layoutTutorList;
    private TextView tvResultCount;
    private List<Tutor> allRankedTutors = new ArrayList<>();

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
            fetchTutors(new ArrayList<>(), null);
        }
    }

    /**
     * US 5 & 6: Loads recommended tutors based on student's subjects and preferences,
     * then ranks them using the RankingEngine.
     */
    private void loadRecommendations() {
        userRepository.getUserProfile(currentUser.getUid(), new UserRepository.LoadCallback<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot doc) {
                List<String> subjects = (List<String>) doc.get("subjects");
                Map<String, List<Integer>> preferredHours = (Map<String, List<Integer>>) doc.get("preferredHours");
                
                if (subjects == null) subjects = new ArrayList<>();
                fetchTutors(subjects, preferredHours);
            }

            @Override
            public void onFailure(String error) {
                fetchTutors(new ArrayList<>(), null); 
            }
        });
    }

    private void fetchTutors(List<String> studentSubjects, Map<String, List<Integer>> preferredHours) {
        userRepository.getRecommendedTutors(studentSubjects, new UserRepository.LoadCallback<List<DocumentSnapshot>>() {
            @Override
            public void onSuccess(List<DocumentSnapshot> snapshots) {
                List<Tutor> tutors = new ArrayList<>();
                for (DocumentSnapshot doc : snapshots) {
                    Tutor t = doc.toObject(Tutor.class);
                    if (t != null) {
                        t.setUid(doc.getId());
                        tutors.add(t);
                    }
                }

                // US 06: Apply Ranking Logic
                allRankedTutors = RankingEngine.rankTutors(tutors, studentSubjects, preferredHours);
                displayTutors(allRankedTutors);
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(BrowseTutorsActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayTutors(List<Tutor> tutors) {
        if (layoutTutorList == null) return;
        layoutTutorList.removeAllViews();
        
        if (tvResultCount != null) {
            tvResultCount.setText("Found " + tutors.size() + " tutors");
        }

        LayoutInflater inflater = LayoutInflater.from(this);

        for (Tutor tutor : tutors) {
            View itemView = inflater.inflate(R.layout.item_tutor, layoutTutorList, false);
            
            TextView tvName = itemView.findViewById(R.id.tvTutorName);
            TextView tvSubjects = itemView.findViewById(R.id.tvTutorSubjects);
            TextView tvRate = itemView.findViewById(R.id.tvTutorRate);
            TextView tvRating = itemView.findViewById(R.id.tvRating);
            TextView tvInitials = itemView.findViewById(R.id.tvTutorInitials);
            View badgeVerified = itemView.findViewById(R.id.badgeVerified);

            if (tvName != null) tvName.setText(tutor.getFullName());
            if (tvSubjects != null && tutor.getSubjects() != null) {
                tvSubjects.setText(String.join(", ", tutor.getSubjects()));
            }
            if (tvRate != null) tvRate.setText(String.valueOf(tutor.getRate()));
            if (tvRating != null) tvRating.setText(String.format("⭐ %.1f", tutor.getRating()));
            
            if (tvInitials != null) {
                String initials = "";
                if (tutor.getFirstName() != null && !tutor.getFirstName().isEmpty()) initials += tutor.getFirstName().charAt(0);
                if (tutor.getLastName() != null && !tutor.getLastName().isEmpty()) initials += tutor.getLastName().charAt(0);
                tvInitials.setText(initials.toUpperCase());
            }

            if (badgeVerified != null) {
                badgeVerified.setVisibility(tutor.isVerified() ? View.VISIBLE : View.GONE);
            }

            itemView.setOnClickListener(v -> {
                openTutorDetail(
                    tutor.getFullName(), 
                    tutor.getSubjects() != null && !tutor.getSubjects().isEmpty() ? tutor.getSubjects().get(0) : "Tutor",
                    String.valueOf(tutor.getRate()),
                    String.valueOf(tutor.getRating()),
                    "0",
                    tutor.isVerified()
                );
            });

            layoutTutorList.addView(itemView);
        }
    }

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

    private void filterTutors(String query) {
        if (query.isEmpty()) {
            displayTutors(allRankedTutors);
            return;
        }
        
        List<Tutor> filtered = new ArrayList<>();
        for (Tutor tutor : allRankedTutors) {
            boolean matchesName = tutor.getFullName() != null && tutor.getFullName().toLowerCase().contains(query.toLowerCase());
            boolean matchesSubject = false;
            if (tutor.getSubjects() != null) {
                for (String s : tutor.getSubjects()) {
                    if (s.toLowerCase().contains(query.toLowerCase())) {
                        matchesSubject = true;
                        break;
                    }
                }
            }
            
            if (matchesName || matchesSubject) {
                filtered.add(tutor);
            }
        }
        displayTutors(filtered);
    }

    private void setupBottomNav() {
        View navHome = findViewById(R.id.navHome);
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
