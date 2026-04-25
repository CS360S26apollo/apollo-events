package com.example.peertutoring.ui;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.peertutoring.R;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * US-07: Student Preference Wizard
 *
 * Multi-step flow (4 steps + results):
 *   Step 1 — Token Budget  (SeekBar 100–2000)
 *   Step 2 — Tutor Level   (Beginner / Intermediate / Expert / Native Speaker)
 *   Step 3 — Languages     (multi-select chips)
 *   Step 4 — Session Type  (Online / In-Person / Hybrid)
 *   Final  — Recommended Tutors list (filtered from Firestore)
 *
 * Preferences are saved to Firestore under users/{uid}/preferences
 * so BrowseTutorsActivity can read them for personalised ranking.
 */
public class StudentPreferencesActivity extends AppCompatActivity {

    // ── State ─────────────────────────────────────────────────
    private int currentStep = 1;
    private static final int TOTAL_STEPS = 4;

    private int selectedBudget = 500;
    private String selectedLevel = null;
    private final List<String> selectedLanguages = new ArrayList<>();
    private String selectedSessionType = null;

    // ── Views ─────────────────────────────────────────────────
    private TextView tvStepTitle, tvStepSubtitle, tvCompletion, tvBudgetValue;
    private LinearProgressIndicator progressBar;
    private LinearLayout layoutStep1, layoutStep2, layoutStep3, layoutStep4, layoutResults;
    private Button btnFindTutor, btnBack;
    private LinearLayout layoutResultList;

    private FirebaseFirestore db;
    private String currentUid;

    // ── Constants ─────────────────────────────────────────────
    private static final String[] LEVELS = {
            "Beginner Friendly", "Intermediate", "Expert", "Native Speaker"
    };
    private static final String[] LEVEL_EMOJIS = {"🌱", "📚", "🎓", "🌍"};

    private static final String[] LANGUAGES = {
            "English", "Spanish", "French", "German",
            "Mandarin", "Japanese", "Korean", "Arabic", "Italian", "Portuguese"
    };

    private static final String[][] SESSION_TYPES = {
            {"Online",     "Video calls via Zoom/Meet",  "📹"},
            {"In-Person",  "Face-to-face sessions",      "👥"},
            {"Hybrid",     "Mix of both options",        "📍"}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_preferences);

        db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        bindViews();
        buildStep1();
        buildStep2();
        buildStep3();
        buildStep4();
        showStep(1);
        if (btnBack != null) btnBack.setOnClickListener(v -> {
            if (currentStep > 1 && currentStep <= 4) {
                showStep(currentStep - 1);
            } else if (currentStep == 5) {
                showStep(4);
            } else {
                finish();
            }
        });

        if (btnBack != null) btnBack.setOnClickListener(v -> {
            if (currentStep > 1 && currentStep <= 4) {
                showStep(currentStep - 1);
            } else if (currentStep == 5) {
                showStep(4);
            } else {
                finish();
            }
        });
    }

    private void bindViews() {
        tvStepTitle    = findViewById(R.id.tvPrefTitle);
        tvStepSubtitle = findViewById(R.id.tvPrefSubtitle);
        tvCompletion   = findViewById(R.id.tvCompletion);
        progressBar    = findViewById(R.id.progressCompletion);
        tvBudgetValue  = findViewById(R.id.tvBudgetValue);
        layoutStep1    = findViewById(R.id.layoutStep1);
        layoutStep2    = findViewById(R.id.layoutStep2);
        layoutStep3    = findViewById(R.id.layoutStep3);
        layoutStep4    = findViewById(R.id.layoutStep4);
        layoutResults  = findViewById(R.id.layoutResults);
        layoutResultList = findViewById(R.id.layoutResultList);
        btnFindTutor   = findViewById(R.id.btnFindTutor);
        btnBack        = findViewById(R.id.btnBack);
    }

    // ── Step 1: Budget ────────────────────────────────────────

    private void buildStep1() {
        SeekBar seekBar = findViewById(R.id.seekBarBudget);
        if (seekBar == null) return;

        seekBar.setMax(1900); // represents 100–2000
        seekBar.setProgress(400); // default 500
        updateBudgetLabel(500);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                // Snap to nearest 100
                selectedBudget = ((progress / 100) * 100) + 100;
                updateBudgetLabel(selectedBudget);
                checkAllComplete();
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });
    }

    private void updateBudgetLabel(int budget) {
        if (tvBudgetValue != null) tvBudgetValue.setText(String.valueOf(budget));
    }

    // ── Step 2: Tutor Level ───────────────────────────────────

    private void buildStep2() {
        LinearLayout grid = findViewById(R.id.layoutLevelGrid);
        if (grid == null) return;
        grid.removeAllViews();

        // 2-column grid
        LinearLayout row = null;
        for (int i = 0; i < LEVELS.length; i++) {
            if (i % 2 == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                rp.setMargins(0, 0, 0, dp(12));
                row.setLayoutParams(rp);
                grid.addView(row);
            }

            final String level = LEVELS[i];
            final String emoji = LEVEL_EMOJIS[i];

            MaterialCardView card = new MaterialCardView(this);
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            if (i % 2 == 0) cp.setMarginEnd(dp(8));
            else cp.setMarginStart(dp(8));
            card.setLayoutParams(cp);
            card.setRadius(dp(16));
            card.setCardElevation(dp(1));
            card.setCardBackgroundColor(0xFFFFFFFF);
            card.setStrokeWidth(dp(2));
            card.setStrokeColor(0xFFE4D5FF);

            LinearLayout inner = new LinearLayout(this);
            inner.setOrientation(LinearLayout.VERTICAL);
            inner.setGravity(Gravity.CENTER_HORIZONTAL);
            inner.setPadding(dp(12), dp(20), dp(12), dp(20));

            TextView tvEmoji = new TextView(this);
            tvEmoji.setText(emoji);
            tvEmoji.setTextSize(28);
            tvEmoji.setGravity(Gravity.CENTER);

            TextView tvLabel = new TextView(this);
            tvLabel.setText(level);
            tvLabel.setTextSize(13);
            tvLabel.setTextColor(0xFF071A3D);
            tvLabel.setTypeface(null, android.graphics.Typeface.BOLD);
            tvLabel.setGravity(Gravity.CENTER);
            tvLabel.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));

            inner.addView(tvEmoji);
            inner.addView(tvLabel);
            card.addView(inner);

            card.setOnClickListener(v -> {
                selectedLevel = level;
                updateLevelSelection(grid, card);
                checkAllComplete();
            });

            if (row != null) row.addView(card);
        }
    }

    private void updateLevelSelection(LinearLayout grid, MaterialCardView selected) {
        for (int i = 0; i < grid.getChildCount(); i++) {
            View rowView = grid.getChildAt(i);
            if (!(rowView instanceof LinearLayout)) continue;
            LinearLayout r = (LinearLayout) rowView;
            for (int j = 0; j < r.getChildCount(); j++) {
                View child = r.getChildAt(j);
                if (child instanceof MaterialCardView) {
                    MaterialCardView c = (MaterialCardView) child;
                    boolean isSelected = c == selected;
                    c.setCardBackgroundColor(isSelected ? 0xFFF3EEFF : 0xFFFFFFFF);
                    c.setStrokeColor(isSelected ? 0xFF8A2EFF : 0xFFE4D5FF);
                    c.setStrokeWidth(isSelected ? dp(2) : dp(1));
                }
            }
        }
    }

    // ── Step 3: Languages ─────────────────────────────────────

    private void buildStep3() {
        LinearLayout chipContainer = findViewById(R.id.layoutLanguageChips);
        if (chipContainer == null) return;
        chipContainer.removeAllViews();

        // Flow layout using nested horizontal LinearLayouts (3 per row)
        LinearLayout row = null;
        for (int i = 0; i < LANGUAGES.length; i++) {
            if (i % 3 == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                rp.setMargins(0, 0, 0, dp(8));
                row.setLayoutParams(rp);
                chipContainer.addView(row);
            }

            final String lang = LANGUAGES[i];
            MaterialCardView chip = new MaterialCardView(this);
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            cp.setMarginEnd(dp(8));
            chip.setLayoutParams(cp);
            chip.setRadius(dp(20));
            chip.setCardElevation(0);
            chip.setCardBackgroundColor(0xFFF3F4F6);
            chip.setStrokeWidth(dp(1));
            chip.setStrokeColor(0xFFE0E0E0);

            TextView tv = new TextView(this);
            tv.setText(lang);
            tv.setTextSize(13);
            tv.setTextColor(0xFF4B5D7A);
            tv.setPadding(dp(14), dp(8), dp(14), dp(8));
            chip.addView(tv);

            chip.setOnClickListener(v -> {
                if (selectedLanguages.contains(lang)) {
                    selectedLanguages.remove(lang);
                    chip.setCardBackgroundColor(0xFFF3F4F6);
                    chip.setStrokeColor(0xFFE0E0E0);
                    tv.setTextColor(0xFF4B5D7A);
                } else {
                    selectedLanguages.add(lang);
                    chip.setCardBackgroundColor(0xFFF3EEFF);
                    chip.setStrokeColor(0xFF8A2EFF);
                    tv.setTextColor(0xFF8A2EFF);
                }
                checkAllComplete();
            });

            if (row != null) row.addView(chip);
        }
    }

    // ── Step 4: Session Type ──────────────────────────────────

    private void buildStep4() {
        LinearLayout container = findViewById(R.id.layoutSessionType);
        if (container == null) return;
        container.removeAllViews();

        for (String[] type : SESSION_TYPES) {
            String label = type[0], desc = type[1], emoji = type[2];

            MaterialCardView card = new MaterialCardView(this);
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            cp.setMargins(0, 0, 0, dp(12));
            card.setLayoutParams(cp);
            card.setRadius(dp(16));
            card.setCardElevation(dp(1));
            card.setCardBackgroundColor(0xFFFFFFFF);
            card.setStrokeWidth(dp(1));
            card.setStrokeColor(0xFFE4D5FF);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(16), dp(16), dp(16), dp(16));

            // Icon box
            MaterialCardView iconBox = new MaterialCardView(this);
            LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(dp(48), dp(48));
            ip.setMarginEnd(dp(16));
            iconBox.setLayoutParams(ip);
            iconBox.setRadius(dp(12));
            iconBox.setCardElevation(0);
            iconBox.setCardBackgroundColor(0xFFF3F4F6);
            TextView tvIcon = new TextView(this);
            tvIcon.setText(emoji);
            tvIcon.setTextSize(20);
            tvIcon.setGravity(Gravity.CENTER);
            tvIcon.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT));
            iconBox.addView(tvIcon);
            row.addView(iconBox);

            LinearLayout textCol = new LinearLayout(this);
            textCol.setOrientation(LinearLayout.VERTICAL);
            textCol.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView tvLabel = new TextView(this);
            tvLabel.setText(label);
            tvLabel.setTextColor(0xFF071A3D);
            tvLabel.setTextSize(15);
            tvLabel.setTypeface(null, android.graphics.Typeface.BOLD);
            textCol.addView(tvLabel);

            TextView tvDesc = new TextView(this);
            tvDesc.setText(desc);
            tvDesc.setTextColor(0xFF8B97A8);
            tvDesc.setTextSize(13);
            textCol.addView(tvDesc);

            row.addView(textCol);
            card.addView(row);

            card.setOnClickListener(v -> {
                selectedSessionType = label;
                updateSessionSelection(container, card);
                checkAllComplete();
            });

            container.addView(card);
        }
    }

    private void updateSessionSelection(LinearLayout container, MaterialCardView selected) {
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            if (child instanceof MaterialCardView) {
                MaterialCardView c = (MaterialCardView) child;
                boolean isSelected = c == selected;
                c.setCardBackgroundColor(isSelected ? 0xFFF3EEFF : 0xFFFFFFFF);
                c.setStrokeColor(isSelected ? 0xFF8A2EFF : 0xFFE4D5FF);
                c.setStrokeWidth(isSelected ? dp(2) : dp(1));
            }
        }
    }

    // ── Navigation ────────────────────────────────────────────

    private void showStep(int step) {
        currentStep = step;
        int pct = (step * 100) / TOTAL_STEPS;
        if (tvCompletion != null) tvCompletion.setText(pct + "%");
        if (progressBar  != null) progressBar.setProgress(pct);

        if (layoutStep1 != null) layoutStep1.setVisibility(step == 1 ? View.VISIBLE : View.GONE);
        if (layoutStep2 != null) layoutStep2.setVisibility(step == 2 ? View.VISIBLE : View.GONE);
        if (layoutStep3 != null) layoutStep3.setVisibility(step == 3 ? View.VISIBLE : View.GONE);
        if (layoutStep4 != null) layoutStep4.setVisibility(step == 4 ? View.VISIBLE : View.GONE);
        if (layoutResults != null) layoutResults.setVisibility(step == 5 ? View.VISIBLE : View.GONE);

        // Show/hide the bottom Find Tutor button
        View btnRow = findViewById(R.id.layoutBtnRow);
        if (btnRow != null) btnRow.setVisibility(step <= 4 ? View.VISIBLE : View.GONE);

        // Step-specific header text
        String[] titles = {"", "Token Budget", "Tutor Level", "Languages", "Session Type"};
        if (tvStepTitle != null && step <= 4) tvStepTitle.setText(titles[step]);

        // Update button label to show next step hint
        if (btnFindTutor != null) {
            if (step < 4) {
                btnFindTutor.setText("Next →");
                btnFindTutor.setOnClickListener(v -> showStep(currentStep + 1));
            } else {
                btnFindTutor.setText("Find My Perfect Tutor →");
                btnFindTutor.setOnClickListener(v -> saveAndShowResults());
            }
        }

        checkAllComplete();
    }



    /**
     * Enables "Find My Perfect Tutor" only when all 4 steps are complete.
     */
    private void checkAllComplete() {
        // Budget always has a value (default 500)
        // For each active step, check if selection is made
        boolean ok;
        switch (currentStep) {
            case 1: ok = true; break;                              // budget always set
            case 2: ok = selectedLevel != null; break;
            case 3: ok = !selectedLanguages.isEmpty(); break;
            case 4: ok = selectedSessionType != null; break;
            default: ok = true;
        }

        if (btnFindTutor != null) {
            btnFindTutor.setEnabled(ok);
            btnFindTutor.setAlpha(ok ? 1.0f : 0.4f);
        }
    }

    // ── Save & Results ────────────────────────────────────────

    /**
     * Saves preferences to Firestore then queries matching tutors.
     * Persists to both the preferences subcollection AND the main user document
     * so BrowseTutorsActivity can read budget/level filters without an extra query.
     */
    private void saveAndShowResults() {
        if (currentUid == null) {
            showResults();
            return;
        }

        Map<String, Object> prefs = new HashMap<>();
        prefs.put("budgetTokens",  selectedBudget);
        prefs.put("tutorLevel",    selectedLevel);
        prefs.put("languages",     selectedLanguages);
        prefs.put("sessionType",   selectedSessionType);

        // Write to subcollection (canonical store)
        db.collection("users").document(currentUid)
                .collection("preferences").document("tutorPrefs")
                .set(prefs, SetOptions.merge());

        // Mirror key fields to the main user doc for use by BrowseTutorsActivity
        db.collection("users").document(currentUid)
                .update(prefs)
                .addOnSuccessListener(u -> showResults())
                .addOnFailureListener(e -> showResults()); // proceed anyway
    }

    /**
     * Queries Firestore for tutors matching the student's preferences,
     * then renders the results list.
     */
    private void showResults() {
        currentStep = 5;
        if (layoutResults  != null) layoutResults.setVisibility(View.VISIBLE);
        if (layoutStep1    != null) layoutStep1.setVisibility(View.GONE);
        if (layoutStep2    != null) layoutStep2.setVisibility(View.GONE);
        if (layoutStep3    != null) layoutStep3.setVisibility(View.GONE);
        if (layoutStep4    != null) layoutStep4.setVisibility(View.GONE);
        View btnRow = findViewById(R.id.layoutBtnRow);
        if (btnRow != null) btnRow.setVisibility(View.GONE);

        // Update header
        if (tvStepTitle    != null) tvStepTitle.setText("Recommended Tutors");
        if (tvStepSubtitle != null) tvStepSubtitle.setText("Tutors that match your preferences");
        if (tvCompletion   != null) tvCompletion.setVisibility(View.GONE);
        if (progressBar    != null) progressBar.setVisibility(View.GONE);

        if (layoutResultList != null) {
            TextView loading = new TextView(this);
            loading.setText("Finding your perfect tutors...");
            loading.setTextColor(0xFF8B97A8);
            loading.setGravity(Gravity.CENTER);
            loading.setPadding(0, dp(24), 0, dp(24));
            layoutResultList.addView(loading);
        }

        db.collection("users")
                .whereEqualTo("role", "tutor")
                .limit(30)
                .get()
                .addOnSuccessListener(snap -> {
                    List<com.google.firebase.firestore.DocumentSnapshot> matched = new ArrayList<>();

                    for (com.google.firebase.firestore.DocumentSnapshot doc : snap.getDocuments()) {
                        Long rate = doc.getLong("rate");
                        String level = doc.getString("level");

                        // Budget filter
                        if (rate != null && rate > selectedBudget) continue;

                        // Level filter (loose match on tutor's level field)
                        if (selectedLevel != null && level != null) {
                            String lv = level.toLowerCase();
                            boolean levelMatch;
                            switch (selectedLevel) {
                                case "Beginner Friendly":
                                    levelMatch = lv.contains("beginner") || lv.contains("freshman")
                                            || lv.contains("sophomore") || lv.contains("intro");
                                    break;
                                case "Intermediate":
                                    levelMatch = lv.contains("intermediate") || lv.contains("junior")
                                            || lv.contains("senior");
                                    break;
                                case "Expert":
                                    levelMatch = lv.contains("expert") || lv.contains("graduate")
                                            || lv.contains("phd") || lv.contains("professional")
                                            || lv.contains("advanced");
                                    break;
                                default: // "Native Speaker" — no expertise restriction
                                    levelMatch = true;
                                    break;
                            }
                            if (!levelMatch) continue;
                        }

                        matched.add(doc);
                    }

                    renderResults(matched);
                })
                .addOnFailureListener(e -> {
                    if (layoutResultList != null) {
                        layoutResultList.removeAllViews();
                        TextView err = new TextView(this);
                        err.setText("Could not load tutors. Please try again.");
                        err.setTextColor(0xFFFF3B30);
                        err.setGravity(Gravity.CENTER);
                        layoutResultList.addView(err);
                    }
                });
    }

    private void renderResults(List<com.google.firebase.firestore.DocumentSnapshot> tutors) {
        if (layoutResultList == null) return;
        layoutResultList.removeAllViews();

        TextView tvCount = findViewById(R.id.tvResultCount);
        if (tvCount != null) tvCount.setText(tutors.size() + " tutors match your preferences");

        if (tutors.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No tutors match your exact preferences.\nTry adjusting your budget or level.");
            empty.setTextColor(0xFF8B97A8);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(16), dp(32), dp(16), dp(32));
            layoutResultList.addView(empty);
            return;
        }

        for (com.google.firebase.firestore.DocumentSnapshot doc : tutors) {
            String fullName = doc.getString("fullName");
            Double rating   = doc.getDouble("rating");
            Long   rate     = doc.getLong("rate");
            Boolean verified = doc.getBoolean("verified");
            String level    = doc.getString("level");
            @SuppressWarnings("unchecked")
            List<String> subjects = (List<String>) doc.get("subjects");

            MaterialCardView card = new MaterialCardView(this);
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            cp.setMargins(0, 0, 0, dp(14));
            card.setLayoutParams(cp);
            card.setRadius(dp(20));
            card.setCardElevation(dp(2));
            card.setCardBackgroundColor(0xFFFFFFFF);

            LinearLayout inner = new LinearLayout(this);
            inner.setOrientation(LinearLayout.VERTICAL);
            inner.setPadding(dp(16), dp(16), dp(16), dp(16));

            // Row 1: Avatar + name + rating
            LinearLayout row1 = new LinearLayout(this);
            row1.setOrientation(LinearLayout.HORIZONTAL);
            row1.setGravity(Gravity.CENTER_VERTICAL);

            // Avatar
            MaterialCardView avatar = new MaterialCardView(this);
            LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(dp(56), dp(56));
            ap.setMarginEnd(dp(14));
            avatar.setLayoutParams(ap);
            avatar.setRadius(dp(28));
            avatar.setCardElevation(0);
            avatar.setCardBackgroundColor(0xFF8A2EFF);
            TextView tvInit = new TextView(this);
            tvInit.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
            tvInit.setGravity(Gravity.CENTER);
            tvInit.setTextColor(0xFFFFFFFF);
            tvInit.setTextSize(18);
            tvInit.setTypeface(null, android.graphics.Typeface.BOLD);
            String initials = "";
            if (fullName != null) {
                String[] parts = fullName.split(" ");
                if (parts.length > 0) initials += parts[0].charAt(0);
                if (parts.length > 1) initials += parts[parts.length - 1].charAt(0);
            }
            tvInit.setText(initials.toUpperCase());
            avatar.addView(tvInit);
            row1.addView(avatar);

            // Name + meta
            LinearLayout nameCol = new LinearLayout(this);
            nameCol.setOrientation(LinearLayout.VERTICAL);
            nameCol.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView tvName = new TextView(this);
            tvName.setText(fullName != null ? fullName : "Tutor");
            tvName.setTextColor(0xFF071A3D);
            tvName.setTextSize(16);
            tvName.setTypeface(null, android.graphics.Typeface.BOLD);
            nameCol.addView(tvName);

            TextView tvRating = new TextView(this);
            tvRating.setText("⭐ " + (rating != null ? rating : "0.0")
                    + "  •  " + (level != null ? level : ""));
            tvRating.setTextColor(0xFF8B97A8);
            tvRating.setTextSize(12);
            nameCol.addView(tvRating);

            row1.addView(nameCol);

            // Verified badge
            if (Boolean.TRUE.equals(verified)) {
                MaterialCardView badge = new MaterialCardView(this);
                LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                badge.setLayoutParams(bp);
                badge.setRadius(dp(12));
                badge.setCardElevation(0);
                badge.setCardBackgroundColor(0xFF0062FF);
                TextView tvV = new TextView(this);
                tvV.setText("✓ Verified");
                tvV.setTextColor(0xFFFFFFFF);
                tvV.setTextSize(11);
                tvV.setPadding(dp(8), dp(4), dp(8), dp(4));
                badge.addView(tvV);
                row1.addView(badge);
            }

            inner.addView(row1);

            // Subjects row
            if (subjects != null && !subjects.isEmpty()) {
                LinearLayout subRow = new LinearLayout(this);
                subRow.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams srp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                srp.setMargins(0, dp(10), 0, 0);
                subRow.setLayoutParams(srp);

                for (int i = 0; i < Math.min(3, subjects.size()); i++) {
                    MaterialCardView subChip = new MaterialCardView(this);
                    LinearLayout.LayoutParams scp = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
                    scp.setMarginEnd(dp(8));
                    subChip.setLayoutParams(scp);
                    subChip.setRadius(dp(12));
                    subChip.setCardElevation(0);
                    subChip.setCardBackgroundColor(0xFFF3EEFF);
                    TextView stv = new TextView(this);
                    stv.setText(subjects.get(i));
                    stv.setTextColor(0xFF8A2EFF);
                    stv.setTextSize(12);
                    stv.setPadding(dp(10), dp(4), dp(10), dp(4));
                    subChip.addView(stv);
                    subRow.addView(subChip);
                }

                // Rate on right
                TextView tvRate = new TextView(this);
                tvRate.setText("🪙 " + (rate != null ? rate : "?") + " tokens");
                tvRate.setTextColor(0xFF089A3C);
                tvRate.setTextSize(13);
                tvRate.setTypeface(null, android.graphics.Typeface.BOLD);
                tvRate.setLayoutParams(new LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                tvRate.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
                subRow.addView(tvRate);

                inner.addView(subRow);
            }

            card.addView(inner);

            // Click → TutorDetailActivity
            final String tUid     = doc.getId();
            final String tName    = fullName;
            final String tRate    = rate != null ? String.valueOf(rate) : "100";
            final String tRating  = rating != null ? String.valueOf(rating) : "0.0";
            final boolean tVerif  = Boolean.TRUE.equals(verified);
            final String tSubject = subjects != null && !subjects.isEmpty() ? subjects.get(0) : "";

            card.setOnClickListener(v -> {
                Intent intent = new Intent(this, TutorDetailActivity.class);
                intent.putExtra("name",       tName);
                intent.putExtra("tutorUid",   tUid);
                intent.putExtra("subject",    tSubject);
                intent.putExtra("rate",       tRate);
                intent.putExtra("rating",     tRating);
                intent.putExtra("isVerified", tVerif);
                startActivity(intent);
            });

            layoutResultList.addView(card);
        }

        // Browse all button
        Button btnBrowseAll = new Button(this);
        btnBrowseAll.setText("Browse All Tutors");
        btnBrowseAll.setTextColor(0xFF8A2EFF);
        btnBrowseAll.setBackground(null);
        btnBrowseAll.setOnClickListener(v ->
                startActivity(new Intent(this, BrowseTutorsActivity.class)));
        layoutResultList.addView(btnBrowseAll);
    }

    private int dp(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}