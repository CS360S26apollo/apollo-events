package com.example.peertutoring.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.example.peertutoring.R;

/**
 * Browse Tutors screen — lists all available tutors with search.
 */
public class BrowseTutorsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browse_tutors);

        setupSearch();
        setupTutorCards();
        setupBottomNav();
    }

    private void setupSearch() {
        EditText etSearch = findViewById(R.id.etSearchTutor);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                // Filter tutor list — extend with RecyclerView for dynamic results
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void setupTutorCards() {
        if (findViewById(R.id.cardTutor1) != null) {
            findViewById(R.id.cardTutor1).setOnClickListener(v ->
                    openTutorDetail("Sarah Johnson", "Mathematics Tutor", "45", "4.9", "127"));
        }
        if (findViewById(R.id.cardTutor2) != null) {
            findViewById(R.id.cardTutor2).setOnClickListener(v ->
                    openTutorDetail("Emily Chen", "Physics Tutor", "50", "5.0", "98"));
        }
    }

    private void setupBottomNav() {
        findViewById(R.id.navHome).setOnClickListener(v -> {
            startActivity(new Intent(this, HomeActivity.class));
            overridePendingTransition(0, 0);
        });

        findViewById(R.id.navBrowse).setOnClickListener(v -> {
            // Already on Browse
        });

        findViewById(R.id.navMessages).setOnClickListener(v -> {
            // Messages — to be implemented
        });

        findViewById(R.id.navProfile).setOnClickListener(v -> {
            startActivity(new Intent(this, EditProfileActivity.class));
            overridePendingTransition(0, 0);
        });
    }

    private void openTutorDetail(String name, String subject, String rate,
                                 String rating, String students) {
        Intent intent = new Intent(this, TutorDetailActivity.class);
        intent.putExtra("name", name);
        intent.putExtra("subject", subject);
        intent.putExtra("rate", rate);
        intent.putExtra("rating", rating);
        intent.putExtra("students", students);
        startActivity(intent);
    }
}