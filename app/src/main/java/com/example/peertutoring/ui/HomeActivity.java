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
import com.example.peertutoring.utils.SoundManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * HomeActivity — main landing screen.
 *
 * Fixes:
 * 1. Search bar now queries Firestore in real-time (by tutor name and subjects).
 *    Results appear in the tutor cards area as the user types.
 * 2. Subject cards (Math, Science, etc.) filter tutors by that subject only.
 * 3. "See All" navigates to BrowseTutorsActivity.
 * 4. Token balance loaded from Firestore via real-time listener.
 * 5. Sound effects on all interactive elements.
 */
public class HomeActivity extends AppCompatActivity {

    private String userRole = "student";
    private TextView tvTokenBalance;
    private LinearLayout layoutSearchResults;
    private EditText etSearch;
    private ListenerRegistration tokenListener;
    private FirebaseFirestore db;
    private List<DocumentSnapshot> allTutors = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        db = FirebaseFirestore.getInstance();

        tvTokenBalance    = findViewById(R.id.tvTokenBalance);
        etSearch          = findViewById(R.id.etSearch);
        layoutSearchResults = findViewById(R.id.layoutSearchResults);

        setupSearch();
        setupSubjectCards();
        setupTutorCards();
        setupSeeAll();
        setupSeedDataButton();
    }

    private void setupSeedDataButton() {
        View btnSeed = findViewById(R.id.btnSeedData);
        if (btnSeed != null)
            btnSeed.setOnClickListener(v ->
                    startActivity(new Intent(this, SeedDataActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserRoleThenSetupNav();
        startTokenListener();
        prefetchTutors(); // cache tutors for instant search
        setupFindTutorBanner();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (tokenListener != null) { tokenListener.remove(); tokenListener = null; }
    }

    // ── Preferences Banner ────────────────────────────────────

    /**
     * Shows the "Find My Perfect Tutor" banner only for students.
     * Tapping it opens the StudentPreferencesActivity wizard.
     */
    private void setupFindTutorBanner() {
        View banner = findViewById(R.id.cardFindTutor);
        if (banner == null) return;
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) { banner.setVisibility(View.GONE); return; }

        db.collection("users").document(currentUser.getUid()).get()
                .addOnSuccessListener(doc -> {
                    String role = doc.exists() ? doc.getString("role") : null;
                    if ("student".equals(role)) {
                        banner.setVisibility(View.VISIBLE);
                        SoundManager.attachClick(this, banner);
                        banner.setOnClickListener(v -> {
                            SoundManager.playClick(this);
                            startActivity(new Intent(this, StudentPreferencesActivity.class));
                        });
                    } else {
                        banner.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> banner.setVisibility(View.GONE));
    }

    // ── Search ────────────────────────────────────────────────

    /**
     * As the user types, filter the cached tutor list by name or subject.
     * Results show in layoutSearchResults (a dynamic card list).
     */
    private void setupSearch() {
        if (etSearch == null) return;

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                if (query.isEmpty()) {
                    hideSearchResults();
                } else {
                    filterAndShowTutors(query, null);
                }
            }
        });
    }

    /**
     * Fetches all tutors once and caches them so search is instant.
     */
    private void prefetchTutors() {
        db.collection("users")
                .whereEqualTo("role", "tutor")
                .limit(50)
                .get()
                .addOnSuccessListener(snap -> {
                    allTutors = snap.getDocuments();
                });
    }

    /**
     * Filters the cached tutor list by query string (name or subject match)
     * and optionally by a specific subject (for subject card clicks).
     */
    private void filterAndShowTutors(String query, String subjectFilter) {
        if (layoutSearchResults == null) return;

        List<DocumentSnapshot> results = new ArrayList<>();

        for (DocumentSnapshot doc : allTutors) {
            String fullName = doc.getString("fullName");
            @SuppressWarnings("unchecked")
            List<String> subjects = (List<String>) doc.get("subjects");

            // Subject card filter — must match exactly
            if (subjectFilter != null) {
                boolean hasSubject = false;
                if (subjects != null) {
                    for (String s : subjects) {
                        if (s.toLowerCase().contains(subjectFilter.toLowerCase())) {
                            hasSubject = true; break;
                        }
                    }
                }
                if (!hasSubject) continue;
            }

            // Text search filter
            if (query != null && !query.isEmpty()) {
                boolean nameMatch = fullName != null
                        && fullName.toLowerCase().contains(query.toLowerCase());
                boolean subjectMatch = false;
                if (subjects != null) {
                    for (String s : subjects) {
                        if (s.toLowerCase().contains(query.toLowerCase())) {
                            subjectMatch = true; break;
                        }
                    }
                }
                if (!nameMatch && !subjectMatch) continue;
            }

            results.add(doc);
        }

        showSearchResults(results, subjectFilter != null
                ? subjectFilter + " Tutors" : "Results for \"" + query + "\"");
    }

    /**
     * Renders search/filter results as cards in layoutSearchResults.
     */
    private void showSearchResults(List<DocumentSnapshot> results, String title) {
        if (layoutSearchResults == null) return;
        layoutSearchResults.removeAllViews();
        layoutSearchResults.setVisibility(View.VISIBLE);

        // Header
        TextView tvHeader = new TextView(this);
        tvHeader.setText(title + " (" + results.size() + ")");
        tvHeader.setTextSize(16);
        tvHeader.setTextColor(0xFF071A3D);
        tvHeader.setTypeface(null, android.graphics.Typeface.BOLD);
        tvHeader.setPadding(dp(4), dp(8), dp(4), dp(12));
        layoutSearchResults.addView(tvHeader);

        if (results.isEmpty()) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("No tutors found. Try a different search.");
            tvEmpty.setTextColor(0xFF8B97A8);
            tvEmpty.setTextSize(14);
            tvEmpty.setPadding(dp(4), dp(4), dp(4), dp(4));
            layoutSearchResults.addView(tvEmpty);
            return;
        }

        for (DocumentSnapshot doc : results) {
            String fullName = doc.getString("fullName");
            @SuppressWarnings("unchecked")
            List<String> subjects = (List<String>) doc.get("subjects");
            Double rating  = doc.getDouble("rating");
            Long   rate    = doc.getLong("rate");
            Boolean verified = doc.getBoolean("verified");

            // Simple result row card
            com.google.android.material.card.MaterialCardView card =
                    new com.google.android.material.card.MaterialCardView(this);
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            cp.setMargins(0, 0, 0, dp(10));
            card.setLayoutParams(cp);
            card.setRadius(dp(16));
            card.setCardElevation(dp(2));
            card.setCardBackgroundColor(0xFFFFFFFF);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setPadding(dp(16), dp(12), dp(16), dp(12));

            // Initials avatar
            com.google.android.material.card.MaterialCardView avatar =
                    new com.google.android.material.card.MaterialCardView(this);
            LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(dp(44), dp(44));
            ap.setMarginEnd(dp(12));
            avatar.setLayoutParams(ap);
            avatar.setRadius(dp(22));
            avatar.setCardElevation(0);
            avatar.setCardBackgroundColor(0xFF8A2EFF);
            TextView tvInit = new TextView(this);
            tvInit.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
            tvInit.setGravity(android.view.Gravity.CENTER);
            tvInit.setTextColor(0xFFFFFFFF);
            tvInit.setTextSize(16);
            tvInit.setTypeface(null, android.graphics.Typeface.BOLD);
            String initials = "";
            if (fullName != null) {
                String[] parts = fullName.split(" ");
                if (parts.length > 0) initials += parts[0].charAt(0);
                if (parts.length > 1) initials += parts[parts.length - 1].charAt(0);
            }
            tvInit.setText(initials.toUpperCase());
            avatar.addView(tvInit);
            row.addView(avatar);

            // Info column
            LinearLayout info = new LinearLayout(this);
            info.setOrientation(LinearLayout.VERTICAL);
            info.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView tvName = new TextView(this);
            tvName.setText(fullName != null ? fullName : "Tutor");
            tvName.setTextColor(0xFF071A3D);
            tvName.setTextSize(15);
            tvName.setTypeface(null, android.graphics.Typeface.BOLD);
            info.addView(tvName);

            TextView tvSubs = new TextView(this);
            tvSubs.setText(subjects != null ? String.join(", ", subjects) : "");
            tvSubs.setTextColor(0xFF8A2EFF);
            tvSubs.setTextSize(12);
            info.addView(tvSubs);

            TextView tvMeta = new TextView(this);
            tvMeta.setText("⭐ " + (rating != null ? rating : "0.0")
                    + "  •  🪙 " + (rate != null ? rate : "?") + "/hr"
                    + (Boolean.TRUE.equals(verified) ? "  ✓" : ""));
            tvMeta.setTextColor(0xFF8B97A8);
            tvMeta.setTextSize(12);
            info.addView(tvMeta);

            row.addView(info);
            card.addView(row);

            // Click → TutorDetailActivity
            final String tName  = fullName;
            final String tUid   = doc.getId();
            final String tRate  = rate != null ? String.valueOf(rate) : "100";
            final String tRating = rating != null ? String.valueOf(rating) : "0.0";
            final boolean tVer  = Boolean.TRUE.equals(verified);
            final String tSubj  = subjects != null && !subjects.isEmpty() ? subjects.get(0) : "";

            card.setOnClickListener(v -> {
                SoundManager.playClick(this);
                Intent intent = new Intent(this, TutorDetailActivity.class);
                intent.putExtra("name",      tName);
                intent.putExtra("tutorUid",  tUid);
                intent.putExtra("subject",   tSubj);
                intent.putExtra("rate",      tRate);
                intent.putExtra("rating",    tRating);
                intent.putExtra("isVerified", tVer);
                startActivity(intent);
            });

            layoutSearchResults.addView(card);
        }
    }

    private void hideSearchResults() {
        if (layoutSearchResults != null) {
            layoutSearchResults.removeAllViews();
            layoutSearchResults.setVisibility(View.GONE);
        }
    }

    // ── Subject Cards ─────────────────────────────────────────

    /**
     * Each subject card filters tutors by that subject when clicked.
     */
    private void setupSubjectCards() {
        setupSubjectCard(R.id.cardMath,    "Mathematics");
        setupSubjectCard(R.id.cardScience, "Physics");
        setupSubjectCard(R.id.cardEnglish, "English");
        setupSubjectCard(R.id.cardCoding,  "Computer Science");
        setupSubjectCard(R.id.cardMusic,   "Music");
        setupSubjectCard(R.id.cardArt,     "Art");
    }

    private void setupSubjectCard(int cardId, String subject) {
        View card = findViewById(cardId);
        if (card == null) return;
        SoundManager.attachClick(this, card);
        card.setOnClickListener(v -> {
            SoundManager.playClick(this);
            // Clear search text, show filtered results
            if (etSearch != null) etSearch.setText("");
            if (allTutors.isEmpty()) {
                // Tutors not yet loaded — fetch with subject filter directly
                db.collection("users")
                        .whereEqualTo("role", "tutor")
                        .whereArrayContains("subjects", subject)
                        .limit(20)
                        .get()
                        .addOnSuccessListener(snap -> {
                            allTutors = snap.getDocuments();
                            filterAndShowTutors(null, subject);
                        });
            } else {
                filterAndShowTutors(null, subject);
            }
        });
    }

    // ── See All ───────────────────────────────────────────────

    private void setupSeeAll() {
        View tvSeeAll = findViewById(R.id.tvSeeAll);
        if (tvSeeAll != null) {
            SoundManager.attachClick(this, tvSeeAll);
            tvSeeAll.setOnClickListener(v -> {
                SoundManager.playClick(this);
                startActivity(new Intent(this, BrowseTutorsActivity.class));
                overridePendingTransition(0, 0);
            });
        }
    }

    // ── Featured Tutor Cards ──────────────────────────────────

    private void setupTutorCards() {
        db.collection("users")
                .whereEqualTo("role", "tutor")
                .limit(2)
                .get()
                .addOnSuccessListener(snap -> {
                    List<com.google.firebase.firestore.DocumentSnapshot> docs = snap.getDocuments();
                    int[] cardIds = {R.id.cardTutor1, R.id.cardTutor2};
                    for (int i = 0; i < cardIds.length && i < docs.size(); i++) {
                        com.google.firebase.firestore.DocumentSnapshot doc = docs.get(i);
                        View card = findViewById(cardIds[i]);
                        if (card == null) continue;

                        String tutorUid  = doc.getId();
                        String fullName  = doc.getString("fullName");
                        Long   rateLong  = doc.getLong("rate");
                        Double rating    = doc.getDouble("rating");
                        Boolean verified = doc.getBoolean("verified");
                        @SuppressWarnings("unchecked")
                        List<String> subs = (List<String>) doc.get("subjects");
                        String subject   = (subs != null && !subs.isEmpty()) ? subs.get(0) : "";
                        String rate      = rateLong != null ? String.valueOf(rateLong) : "100";
                        String ratingStr = rating != null ? String.valueOf(rating) : "0.0";

                        // Update card text views if present
                        TextView tvName = card.findViewById(R.id.tvTutorName);
                        TextView tvSub  = card.findViewById(R.id.tvTutorSubject);
                        TextView tvRate = card.findViewById(R.id.tvTutorRate);
                        TextView tvRat  = card.findViewById(R.id.tvRating);
                        if (tvName != null) tvName.setText(fullName);
                        if (tvSub  != null) tvSub.setText(subject);
                        if (tvRate != null) tvRate.setText(rate + " tokens/hr");
                        if (tvRat  != null) tvRat.setText("⭐ " + ratingStr);

                        SoundManager.attachClick(this, card);
                        final String fUid = tutorUid, fName = fullName, fSubj = subject,
                                fRate = rate, fRating = ratingStr;
                        final boolean fVer = Boolean.TRUE.equals(verified);
                        card.setOnClickListener(v -> {
                            SoundManager.playClick(this);
                            Intent intent = new Intent(this, TutorDetailActivity.class);
                            intent.putExtra("tutorUid",   fUid);
                            intent.putExtra("name",       fName);
                            intent.putExtra("subject",    fSubj);
                            intent.putExtra("rate",       fRate);
                            intent.putExtra("rating",     fRating);
                            intent.putExtra("isVerified", fVer);
                            startActivity(intent);
                        });
                    }
                });
    }

    // ── Token Balance ─────────────────────────────────────────

    private void startTokenListener() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) { updateTokenDisplay(100); return; }

        tokenListener = db.collection("users")
                .document(currentUser.getUid())
                .addSnapshotListener((doc, e) -> {
                    if (e != null || doc == null) return;
                    Long tokens = doc.getLong("tokens");
                    if (tokens == null) {
                        db.collection("users").document(currentUser.getUid())
                                .update("tokens", 100L);
                        updateTokenDisplay(100);
                    } else {
                        updateTokenDisplay(tokens.intValue());
                    }
                });
    }

    private void updateTokenDisplay(int tokens) {
        if (tvTokenBalance != null) {
            tvTokenBalance.setText(
                    NumberFormat.getNumberInstance(Locale.getDefault()).format(tokens));
        }
    }

    // ── Role & Nav ────────────────────────────────────────────

    private void loadUserRoleThenSetupNav() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) { setupBottomNav(); return; }

        db.collection("users").document(currentUser.getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String role = doc.getString("role");
                        if (role != null) userRole = role;
                    }
                    setupBottomNav();
                })
                .addOnFailureListener(e -> setupBottomNav());
    }

    private void setupBottomNav() {
        View navHome     = findViewById(R.id.navHome);
        View navBrowse   = findViewById(R.id.navBrowse);
        View navMessages = findViewById(R.id.navMessages);
        View navProfile  = findViewById(R.id.navProfile);

        if (navHome != null) {
            SoundManager.attachClick(this, navHome);
            navHome.setOnClickListener(v -> { /* already here */ });
        }
        if (navBrowse != null) {
            SoundManager.attachClick(this, navBrowse);
            navBrowse.setOnClickListener(v -> {
                SoundManager.playClick(this);
                startActivity(new Intent(this, BrowseTutorsActivity.class));
                overridePendingTransition(0, 0);
            });
        }
        if (navMessages != null) {
            SoundManager.attachClick(this, navMessages);
            navMessages.setOnClickListener(v -> {
                SoundManager.playClick(this);
                startActivity(new Intent(this,
                        "tutor".equals(userRole)
                                ? TutorRequestsActivity.class
                                : SessionRequestsActivity.class));
                overridePendingTransition(0, 0);
            });
        }
        if (navProfile != null) {
            SoundManager.attachClick(this, navProfile);
            navProfile.setOnClickListener(v -> {
                SoundManager.playClick(this);
                startActivity(new Intent(this, EditProfileActivity.class));
                overridePendingTransition(0, 0);
            });
        }
    }

    private int dp(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}