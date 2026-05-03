package com.example.peertutoring.ui;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.peertutoring.R;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Students browse available crash courses with attractive thumbnail cards.
 * Tutors see their own courses and can create new ones.
 */
public class CoursesActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private LinearLayout layoutCourses;
    private ListenerRegistration coursesListener;
    private String userRole = "student";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_courses);

        db          = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        layoutCourses = findViewById(R.id.layoutCourses);

        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        // FAB for tutors to create a course
        View fabCreate = findViewById(R.id.fabCreateCourse);

        // Load role then decide what to show
        if (currentUser != null) {
            db.collection("users").document(currentUser.getUid()).get()
                    .addOnSuccessListener(doc -> {
                        userRole = doc.getString("role") != null
                                ? doc.getString("role") : "student";
                        if (fabCreate != null)
                            fabCreate.setVisibility(
                                    "tutor".equals(userRole) ? View.VISIBLE : View.GONE);
                        loadCourses();
                    });
        } else {
            if (fabCreate != null) fabCreate.setVisibility(View.GONE);
            loadCourses();
        }

        if (fabCreate != null) {
            fabCreate.setOnClickListener(v ->
                    startActivity(new Intent(this, CreateCourseActivity.class)));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (coursesListener != null) coursesListener.remove();
    }

    // ── Load courses ──────────────────────────────────────────

    private void loadCourses() {
        // Students see all open courses; tutors see their own courses
        if ("tutor".equals(userRole) && currentUser != null) {
            coursesListener = db.collection("courses")
                    .whereEqualTo("tutorUid", currentUser.getUid())
                    .addSnapshotListener((snap, e) -> {
                        if (snap != null) renderCourses(snap.getDocuments());
                    });
        } else {
            coursesListener = db.collection("courses")
                    .whereEqualTo("status", "open")
                    .addSnapshotListener((snap, e) -> {
                        if (snap != null) renderCourses(snap.getDocuments());
                    });
        }
    }

    // ── Render ────────────────────────────────────────────────

    private void renderCourses(java.util.List<DocumentSnapshot> courses) {
        if (layoutCourses == null) return;
        layoutCourses.removeAllViews();

        if (courses.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("tutor".equals(userRole)
                    ? "You haven't created any courses yet.\nTap + to create your first course!"
                    : "No courses available yet.\nCheck back soon!");
            empty.setTextColor(0xFF8B97A8);
            empty.setTextSize(15f);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(60), 0, 0);
            empty.setLineSpacing(dp(6), 1f);
            layoutCourses.addView(empty);
            return;
        }

        // Two-column grid
        LinearLayout row = null;
        for (int i = 0; i < courses.size(); i++) {
            if (i % 2 == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                rp.setMargins(0, 0, 0, dp(14));
                row.setLayoutParams(rp);
                layoutCourses.addView(row);
            }
            buildCourseCard(courses.get(i), row, i % 2 == 0);
        }
    }

    private void buildCourseCard(DocumentSnapshot doc, LinearLayout row, boolean isLeft) {
        String courseId      = doc.getId();
        String title         = doc.getString("title");
        String subject       = doc.getString("subject");
        String level         = doc.getString("level");
        String tutorName     = doc.getString("tutorName");
        String description   = doc.getString("description");
        String emoji         = doc.getString("thumbnailEmoji");
        String color         = doc.getString("thumbnailColor");
        String status        = doc.getString("status");
        Long   totalTokens   = doc.getLong("totalTokens");
        Long   durationDays  = doc.getLong("durationDays");
        Long   sessionsPerWk = doc.getLong("sessionsPerWeek");
        Long   enrolled      = doc.getLong("enrolledCount");
        Long   maxStudents   = doc.getLong("maxStudents");

        if (emoji == null) emoji = "📚";
        if (color == null) color = "#8A2EFF";

        int parsedColor;
        try { parsedColor = Color.parseColor(color); }
        catch (Exception e) { parsedColor = 0xFF8A2EFF; }

        // Card
        MaterialCardView card = new MaterialCardView(this);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        if (isLeft) cp.setMarginEnd(dp(8));
        else cp.setMarginStart(dp(8));
        card.setLayoutParams(cp);
        card.setRadius(dp(20));
        card.setCardElevation(dp(4));
        card.setCardBackgroundColor(0xFFFFFFFF);
        card.setClickable(true);
        card.setFocusable(true);

        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.VERTICAL);

        // ── Thumbnail (gradient header) ───────────────────────
        LinearLayout thumb = new LinearLayout(this);
        thumb.setOrientation(LinearLayout.VERTICAL);
        thumb.setGravity(Gravity.CENTER);
        thumb.setPadding(dp(16), dp(20), dp(16), dp(16));

        // Create a gradient-like background using the color
        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
                new int[]{parsedColor, darken(parsedColor, 0.7f)});
        gd.setCornerRadii(new float[]{dp(20), dp(20), dp(20), dp(20), 0, 0, 0, 0});
        thumb.setBackground(gd);

        // Big emoji
        TextView tvEmoji = new TextView(this);
        tvEmoji.setText(emoji);
        tvEmoji.setTextSize(42f);
        tvEmoji.setGravity(Gravity.CENTER);
        thumb.addView(tvEmoji);

        // Level badge
        MaterialCardView levelBadge = new MaterialCardView(this);
        LinearLayout.LayoutParams lbp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lbp.setMargins(0, dp(8), 0, 0);
        levelBadge.setLayoutParams(lbp);
        levelBadge.setRadius(dp(10));
        levelBadge.setCardElevation(0);
        levelBadge.setCardBackgroundColor(0x33FFFFFF);
        TextView tvLevel = new TextView(this);
        tvLevel.setText(level != null ? level : "");
        tvLevel.setTextColor(0xFFFFFFFF);
        tvLevel.setTextSize(11f);
        tvLevel.setTypeface(null, Typeface.BOLD);
        tvLevel.setPadding(dp(8), dp(4), dp(8), dp(4));
        levelBadge.addView(tvLevel);
        thumb.addView(levelBadge);

        inner.addView(thumb);

        // ── Course info ───────────────────────────────────────
        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(dp(12), dp(12), dp(12), dp(14));

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextColor(0xFF071A3D);
        tvTitle.setTextSize(14f);
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setMaxLines(2);
        tvTitle.setEllipsize(android.text.TextUtils.TruncateAt.END);
        info.addView(tvTitle);

        TextView tvTutor = new TextView(this);
        tvTutor.setText("👤 " + (tutorName != null ? tutorName : ""));
        tvTutor.setTextColor(0xFF8B97A8);
        tvTutor.setTextSize(11f);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tlp.setMargins(0, dp(4), 0, dp(6));
        tvTutor.setLayoutParams(tlp);
        info.addView(tvTutor);

        // Stats row
        LinearLayout statsRow = new LinearLayout(this);
        statsRow.setOrientation(LinearLayout.HORIZONTAL);
        statsRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView tvDuration = new TextView(this);
        tvDuration.setText("📅 " + (durationDays != null ? durationDays : 30) + "d");
        tvDuration.setTextColor(0xFF4B5D7A);
        tvDuration.setTextSize(11f);
        statsRow.addView(tvDuration);

        TextView tvSessions = new TextView(this);
        tvSessions.setText(" · " + (sessionsPerWk != null ? sessionsPerWk : 5) + "x/wk");
        tvSessions.setTextColor(0xFF4B5D7A);
        tvSessions.setTextSize(11f);
        statsRow.addView(tvSessions);
        info.addView(statsRow);

        // Enrollment bar
        if (maxStudents != null && maxStudents > 0) {
            long enrolledCount = enrolled != null ? enrolled : 0;
            float fillRatio = Math.min(1f, (float) enrolledCount / maxStudents);

            LinearLayout barBg = new LinearLayout(this);
            barBg.setBackgroundColor(0xFFEEEEEE);
            LinearLayout.LayoutParams bbp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(4));
            bbp.setMargins(0, dp(8), 0, dp(6));
            barBg.setLayoutParams(bbp);
            barBg.setOrientation(LinearLayout.HORIZONTAL);

            View fill = new View(this);
            fill.setBackgroundColor(parsedColor);
            LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.MATCH_PARENT, fillRatio);
            fill.setLayoutParams(fp);
            barBg.addView(fill);
            info.addView(barBg);

            TextView tvSeats = new TextView(this);
            tvSeats.setText((maxStudents - enrolledCount) + " seats left");
            tvSeats.setTextColor(enrolledCount >= maxStudents ? 0xFFE53935 : 0xFF8B97A8);
            tvSeats.setTextSize(10f);
            info.addView(tvSeats);
        }

        // Price + enroll row
        LinearLayout priceRow = new LinearLayout(this);
        priceRow.setOrientation(LinearLayout.HORIZONTAL);
        priceRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams prp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        prp.setMargins(0, dp(10), 0, 0);
        priceRow.setLayoutParams(prp);

        TextView tvPrice = new TextView(this);
        tvPrice.setText("🪙 " + (totalTokens != null
                ? NumberFormat.getNumberInstance().format(totalTokens) : "?"));
        tvPrice.setTextColor(parsedColor);
        tvPrice.setTextSize(13f);
        tvPrice.setTypeface(null, Typeface.BOLD);
        tvPrice.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        priceRow.addView(tvPrice);

        // Status badge
        MaterialCardView statusBadge = new MaterialCardView(this);
        statusBadge.setRadius(dp(8));
        statusBadge.setCardElevation(0);
        boolean isFull = "full".equals(status);
        statusBadge.setCardBackgroundColor(isFull ? 0xFFFFEBEE : 0xFFE8F5E9);
        TextView tvStatus = new TextView(this);
        tvStatus.setText(isFull ? "Full" : "Open");
        tvStatus.setTextColor(isFull ? 0xFFE53935 : 0xFF2E7D32);
        tvStatus.setTextSize(10f);
        tvStatus.setTypeface(null, Typeface.BOLD);
        tvStatus.setPadding(dp(8), dp(3), dp(8), dp(3));
        statusBadge.addView(tvStatus);
        priceRow.addView(statusBadge);

        info.addView(priceRow);
        inner.addView(info);
        card.addView(inner);

        // Click → course detail
        final String fCourseId = courseId;
        card.setOnClickListener(v -> {
            Intent intent = new Intent(this, CoursesDetailActivity.class);
            intent.putExtra("courseId", fCourseId);
            startActivity(intent);
        });

        if (row != null) row.addView(card);
    }

    private int darken(int color, float factor) {
        int r = Math.max(0, (int) (Color.red(color)   * factor));
        int g = Math.max(0, (int) (Color.green(color) * factor));
        int b = Math.max(0, (int) (Color.blue(color)  * factor));
        return Color.argb(255, r, g, b);
    }

    private int dp(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}