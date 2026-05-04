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
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.peertutoring.R;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows all crash courses a student has enrolled in.
 * Accessible from HomeActivity and SessionRequestsActivity.
 */
public class MyCoursesActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private LinearLayout layoutMyCourses;
    private ListenerRegistration enrollmentsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_courses);

        db          = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) { finish(); return; }

        layoutMyCourses = findViewById(R.id.layoutMyCourses);

        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        loadMyCourses();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (enrollmentsListener != null) enrollmentsListener.remove();
    }

    private void loadMyCourses() {
        enrollmentsListener = db.collection("enrollments")
                .whereEqualTo("studentUid", currentUser.getUid())
                .addSnapshotListener((snap, e) -> {
                    if (snap == null || layoutMyCourses == null) return;
                    layoutMyCourses.removeAllViews();

                    if (snap.isEmpty()) {
                        TextView empty = new TextView(this);
                        empty.setText("You haven't enrolled in any courses yet.\n\nBrowse crash courses to get started!");
                        empty.setTextColor(0xFF8B97A8);
                        empty.setTextSize(15f);
                        empty.setGravity(Gravity.CENTER);
                        empty.setPadding(0, dp(60), 0, 0);
                        empty.setLineSpacing(dp(6), 1f);
                        layoutMyCourses.addView(empty);
                        return;
                    }

                    // Fetch each course document
                    List<String> courseIds = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        String cid = doc.getString("courseId");
                        if (cid != null) courseIds.add(cid);
                    }
                    fetchCourseDetails(courseIds);
                });
    }

    private void fetchCourseDetails(List<String> courseIds) {
        if (courseIds.isEmpty()) return;
        List<DocumentSnapshot> loaded = new ArrayList<>();

        for (String cid : courseIds) {
            db.collection("courses").document(cid).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) loaded.add(doc);
                        if (loaded.size() == courseIds.size()) renderCourses(loaded);
                    });
        }
    }

    private void renderCourses(List<DocumentSnapshot> courses) {
        if (layoutMyCourses == null) return;
        layoutMyCourses.removeAllViews();

        for (DocumentSnapshot doc : courses) {
            String courseId    = doc.getId();
            String title       = doc.getString("title");
            String subject     = doc.getString("subject");
            String level       = doc.getString("level");
            String tutorName   = doc.getString("tutorName");
            String emoji       = doc.getString("thumbnailEmoji");
            String color       = doc.getString("thumbnailColor");
            String status      = doc.getString("status");
            String startDateStr = doc.getString("startDateStr");
            Long   tokens      = doc.getLong("totalTokens");
            String zoomLink    = doc.getString("zoomLink");

            if (emoji == null) emoji = "📚";
            int parsedColor;
            try { parsedColor = Color.parseColor(color != null ? color : "#8A2EFF"); }
            catch (Exception ex) { parsedColor = 0xFF8A2EFF; }

            MaterialCardView card = new MaterialCardView(this);
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cp.setMargins(0, 0, 0, dp(14));
            card.setLayoutParams(cp);
            card.setRadius(dp(20));
            card.setCardElevation(dp(3));
            card.setCardBackgroundColor(0xFFFFFFFF);
            card.setClickable(true);
            card.setFocusable(true);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setPadding(dp(16), dp(16), dp(16), dp(16));

            // Color thumbnail
            MaterialCardView thumb = new MaterialCardView(this);
            LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(dp(64), dp(64));
            tp.setMarginEnd(dp(14));
            thumb.setLayoutParams(tp);
            thumb.setRadius(dp(18));
            thumb.setCardElevation(0);
            thumb.setCardBackgroundColor(parsedColor);
            TextView tvEmoji = new TextView(this);
            tvEmoji.setText(emoji);
            tvEmoji.setTextSize(26f);
            tvEmoji.setGravity(android.view.Gravity.CENTER);
            tvEmoji.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            thumb.addView(tvEmoji);
            row.addView(thumb);

            // Info
            LinearLayout info = new LinearLayout(this);
            info.setOrientation(LinearLayout.VERTICAL);
            info.setLayoutParams(new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView tvTitle = new TextView(this);
            tvTitle.setText(title != null ? title : "Course");
            tvTitle.setTextColor(0xFF071A3D);
            tvTitle.setTextSize(15f);
            tvTitle.setTypeface(null, Typeface.BOLD);
            tvTitle.setMaxLines(2);
            tvTitle.setEllipsize(android.text.TextUtils.TruncateAt.END);
            info.addView(tvTitle);

            TextView tvTutor = new TextView(this);
            tvTutor.setText("👤 " + (tutorName != null ? tutorName : ""));
            tvTutor.setTextColor(0xFF8B97A8);
            tvTutor.setTextSize(12f);
            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            tlp.setMargins(0, dp(3), 0, dp(4));
            tvTutor.setLayoutParams(tlp);
            info.addView(tvTutor);

            // Level + start date row
            LinearLayout tagsRow = new LinearLayout(this);
            tagsRow.setOrientation(LinearLayout.HORIZONTAL);
            tagsRow.setGravity(android.view.Gravity.CENTER_VERTICAL);

            if (level != null) {
                MaterialCardView lvlBadge = new MaterialCardView(this);
                lvlBadge.setRadius(dp(8));
                lvlBadge.setCardElevation(0);
                lvlBadge.setCardBackgroundColor(0xFFF3EEFF);
                TextView tvLevel = new TextView(this);
                tvLevel.setText(level);
                tvLevel.setTextColor(parsedColor);
                tvLevel.setTextSize(11f);
                tvLevel.setTypeface(null, Typeface.BOLD);
                tvLevel.setPadding(dp(8), dp(3), dp(8), dp(3));
                lvlBadge.addView(tvLevel);
                LinearLayout.LayoutParams lbp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lbp.setMarginEnd(dp(8));
                lvlBadge.setLayoutParams(lbp);
                tagsRow.addView(lvlBadge);
            }

            if (startDateStr != null) {
                TextView tvStart = new TextView(this);
                tvStart.setText("📅 " + startDateStr);
                tvStart.setTextColor(0xFF4B5D7A);
                tvStart.setTextSize(11f);
                tagsRow.addView(tvStart);
            }
            info.addView(tagsRow);

            // Zoom link chip (if available)
            if (zoomLink != null && !zoomLink.isEmpty()) {
                TextView tvZoom = new TextView(this);
                tvZoom.setText("🎥 Zoom available");
                tvZoom.setTextColor(0xFF2563EB);
                tvZoom.setTextSize(11f);
                LinearLayout.LayoutParams zlp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                zlp.setMargins(0, dp(4), 0, 0);
                tvZoom.setLayoutParams(zlp);
                info.addView(tvZoom);
            }

            row.addView(info);

            // Status badge
            MaterialCardView statusBadge = new MaterialCardView(this);
            statusBadge.setRadius(dp(8));
            statusBadge.setCardElevation(0);
            boolean completed = "completed".equals(status);
            statusBadge.setCardBackgroundColor(completed ? 0xFFE8F5E9 : 0xFFF3EEFF);
            TextView tvStatus = new TextView(this);
            tvStatus.setText(completed ? "✓ Done" : "Active");
            tvStatus.setTextColor(completed ? 0xFF2E7D32 : parsedColor);
            tvStatus.setTextSize(11f);
            tvStatus.setTypeface(null, Typeface.BOLD);
            tvStatus.setPadding(dp(8), dp(4), dp(8), dp(4));
            statusBadge.addView(tvStatus);
            LinearLayout.LayoutParams sbp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            sbp.setMarginStart(dp(8));
            statusBadge.setLayoutParams(sbp);
            row.addView(statusBadge);

            card.addView(row);

            // Open course detail on tap
            final String fCourseId = courseId;
            card.setOnClickListener(v -> {
                Intent intent = new Intent(this, CoursesDetailActivity.class);
                intent.putExtra("courseId", fCourseId);
                startActivity(intent);
            });

            layoutMyCourses.addView(card);
        }
    }

    private int dp(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}