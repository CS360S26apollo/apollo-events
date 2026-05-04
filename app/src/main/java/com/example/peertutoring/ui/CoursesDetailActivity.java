package com.example.peertutoring.ui;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.peertutoring.R;
import com.example.peertutoring.utils.EscrowManager;
import com.google.firebase.auth.FirebaseAuth;
import com.example.peertutoring.ui.MessagingActivity;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Full detail view of a crash course.
 * Students can enroll (deducts tokens via escrow).
 * Shows Zoom link if session type is online.
 */
public class CoursesDetailActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private String courseId;
    private String userRole = "student";

    private TextView tvTitle, tvTutor, tvLevel, tvSubject, tvDescription,
            tvDuration, tvSessions, tvSessionMinutes, tvTopics, tvTokens,
            tvSeatsLeft, tvZoomLink, tvEmoji;
    private Button btnEnroll;
    private View layoutZoom;

    // Loaded from Firestore
    private long   totalTokens   = 0;
    private long   enrolledCount = 0;
    private long   maxStudents   = 10;
    private String tutorUid;
    private String sessionType;
    private String zoomLink;
    private String courseStatus;
    private boolean isTutorOwner = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course_detail);

        db          = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        courseId    = getIntent().getStringExtra("courseId");

        if (courseId == null) { finish(); return; }

        bindViews();

        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        // Load user role, then course
        if (currentUser != null) {
            db.collection("users").document(currentUser.getUid()).get()
                    .addOnSuccessListener(doc -> {
                        userRole = doc.getString("role") != null
                                ? doc.getString("role") : "student";
                        loadCourse();
                    });
        } else {
            loadCourse();
        }
    }

    private void bindViews() {
        tvTitle          = findViewById(R.id.tvCourseDetailTitle);
        tvTutor          = findViewById(R.id.tvCourseDetailTutor);
        tvLevel          = findViewById(R.id.tvCourseDetailLevel);
        tvSubject        = findViewById(R.id.tvCourseDetailSubject);
        tvDescription    = findViewById(R.id.tvCourseDetailDescription);
        tvDuration       = findViewById(R.id.tvCourseDetailDuration);
        tvSessions       = findViewById(R.id.tvCourseDetailSessions);
        tvSessionMinutes = findViewById(R.id.tvCourseDetailMinutes);
        tvTopics         = findViewById(R.id.tvCourseDetailTopics);
        tvTokens         = findViewById(R.id.tvCourseDetailTokens);
        tvSeatsLeft      = findViewById(R.id.tvCourseDetailSeats);
        tvZoomLink       = findViewById(R.id.tvCourseZoomLink);
        tvEmoji          = findViewById(R.id.tvCourseDetailEmoji);
        btnEnroll        = findViewById(R.id.btnEnrollCourse);
        layoutZoom       = findViewById(R.id.layoutZoomLink);

        // Ask Tutor button — opens a direct chat with the tutor
        android.widget.Button btnAsk = findViewById(R.id.btnAskTutor);
        if (btnAsk != null) btnAsk.setOnClickListener(v -> openChatWithTutor());
    }

    private void loadCourse() {
        db.collection("courses").document(courseId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) { finish(); return; }

                    String title       = doc.getString("title");
                    String tutor       = doc.getString("tutorName");
                    String level       = doc.getString("level");
                    String subject     = doc.getString("subject");
                    String description = doc.getString("description");
                    String emoji       = doc.getString("thumbnailEmoji");
                    String color       = doc.getString("thumbnailColor");
                    Long   days        = doc.getLong("durationDays");
                    Long   sessWk      = doc.getLong("sessionsPerWeek");
                    Long   mins        = doc.getLong("sessionMinutes");
                    Long   tokens      = doc.getLong("totalTokens");
                    Long   max         = doc.getLong("maxStudents");
                    Long   enrolled    = doc.getLong("enrolledCount");
                    List<String> topics = (List<String>) doc.get("topics");
                    tutorUid    = doc.getString("tutorUid");
                    sessionType = doc.getString("sessionType");
                    zoomLink    = doc.getString("zoomLink");
                    courseStatus = doc.getString("status");
                    isTutorOwner = currentUser != null
                            && currentUser.getUid().equals(tutorUid);
                    if (isTutorOwner) loadEnrolledStudents();

                    totalTokens   = tokens   != null ? tokens   : 0;
                    enrolledCount = enrolled != null ? enrolled : 0;
                    maxStudents   = max      != null ? max      : 10;

                    // Populate views
                    if (tvEmoji   != null) tvEmoji.setText(emoji != null ? emoji : "📚");
                    if (tvTitle   != null) tvTitle.setText(title);
                    if (tvTutor   != null) tvTutor.setText("👤 " + (tutor != null ? tutor : ""));
                    if (tvLevel   != null) tvLevel.setText(level);
                    if (tvSubject != null) tvSubject.setText(subject);
                    if (tvDescription != null) tvDescription.setText(description);
                    if (tvDuration   != null) tvDuration.setText((days != null ? days : 30) + " days");
                    if (tvSessions   != null) tvSessions.setText((sessWk != null ? sessWk : 5) + " sessions/week");
                    if (tvSessionMinutes != null) tvSessionMinutes.setText((mins != null ? mins : 60) + " min each");
                    if (tvTokens != null) tvTokens.setText("🪙 " + totalTokens + " tokens total");
                    if (tvSeatsLeft != null)
                        tvSeatsLeft.setText((maxStudents - enrolledCount) + " / " + maxStudents + " seats remaining");

                    if (topics != null && !topics.isEmpty() && tvTopics != null)
                        tvTopics.setText(String.join(" • ", topics));

                    // Apply thumbnail color to header
                    if (color != null) {
                        View header = findViewById(R.id.courseDetailHeader);
                        if (header != null) {
                            try {
                                android.graphics.drawable.GradientDrawable gd =
                                        new android.graphics.drawable.GradientDrawable(
                                                android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
                                                new int[]{Color.parseColor(color), darken(Color.parseColor(color), 0.7f)});
                                header.setBackground(gd);
                            } catch (Exception ignored) {}
                        }
                    }

                    // Zoom link
                    if ("online".equals(sessionType) && zoomLink != null && !zoomLink.isEmpty()) {
                        if (layoutZoom != null) layoutZoom.setVisibility(View.VISIBLE);
                        if (tvZoomLink != null) {
                            tvZoomLink.setText("🎥 " + zoomLink);
                            tvZoomLink.setOnClickListener(v -> {
                                try {
                                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(zoomLink)));
                                } catch (Exception e) {
                                    Toast.makeText(this, "Cannot open Zoom link.", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }

                    setupEnrollButton();
                });
    }

    private void setupEnrollButton() {
        if (btnEnroll == null) return;

        // Tutors see a delete/edit option for their own course
        if ("tutor".equals(userRole) && currentUser != null
                && currentUser.getUid().equals(tutorUid)) {
            // Tutor sees Edit + Delete
            btnEnroll.setText("✏️ Edit Course");
            btnEnroll.setBackgroundColor(0xFF8A2EFF);
            btnEnroll.setOnClickListener(v -> showEditDialog());

            // Add a separate delete button below
            Button btnDelete = new Button(this);
            btnDelete.setText("🗑 Delete Course");
            btnDelete.setTextColor(0xFFFFFFFF);
            btnDelete.setBackgroundColor(0xFFE53935);
            android.widget.LinearLayout.LayoutParams dlp = new android.widget.LinearLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            dlp.setMargins(0, dpC(8), 0, 0);
            btnDelete.setLayoutParams(dlp);
            btnDelete.setOnClickListener(v -> confirmDelete());
            if (btnEnroll.getParent() instanceof android.widget.LinearLayout) {
                android.widget.LinearLayout parent =
                        (android.widget.LinearLayout) btnEnroll.getParent();
                parent.addView(btnDelete);

                // Complete course button (only if not already completed)
                if (!"completed".equals(courseStatus)) {
                    Button btnComplete = new Button(this);
                    btnComplete.setText("✓ Mark Course Complete");
                    btnComplete.setTextColor(0xFFFFFFFF);
                    btnComplete.setBackgroundColor(0xFF089A3C);
                    android.widget.LinearLayout.LayoutParams clp =
                            new android.widget.LinearLayout.LayoutParams(
                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
                    clp.setMargins(0, dpC(8), 0, 0);
                    btnComplete.setLayoutParams(clp);
                    btnComplete.setOnClickListener(v -> confirmMarkComplete());
                    parent.addView(btnComplete);
                }
            }
            return;
        }

        // Check if full
        if ("full".equals(courseStatus) || enrolledCount >= maxStudents) {
            btnEnroll.setText("Course Full");
            btnEnroll.setEnabled(false);
            btnEnroll.setAlpha(0.5f);
            return;
        }

        // Check if already enrolled
        if (currentUser != null) {
            db.collection("enrollments")
                    .whereEqualTo("courseId", courseId)
                    .whereEqualTo("studentUid", currentUser.getUid())
                    .get()
                    .addOnSuccessListener(snap -> {
                        if (!snap.isEmpty()) {
                            btnEnroll.setText("✅ Already Enrolled");
                            btnEnroll.setEnabled(false);
                            btnEnroll.setAlpha(0.7f);
                        } else {
                            btnEnroll.setText("Enroll — 🪙 " + totalTokens + " tokens");
                            btnEnroll.setOnClickListener(v -> confirmEnroll());
                        }
                    });
        }
    }

    private void confirmEnroll() {
        new AlertDialog.Builder(this)
                .setTitle("Enroll in Course?")
                .setMessage("This will deduct " + totalTokens + " tokens from your account.\n\n"
                        + "You will be enrolled in this crash course and given access to all sessions.")
                .setPositiveButton("Enroll", (d, w) -> processEnrollment())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void processEnrollment() {
        if (currentUser == null) return;
        if (btnEnroll != null) { btnEnroll.setEnabled(false); btnEnroll.setText("Enrolling..."); }

        // Check balance
        db.collection("users").document(currentUser.getUid()).get()
                .addOnSuccessListener(doc -> {
                    Long bal = doc.getLong("tokens");
                    long balance = bal != null ? bal : 0;
                    String studentName = doc.getString("fullName");

                    if (balance < totalTokens) {
                        Toast.makeText(this,
                                "Not enough tokens. You need " + totalTokens
                                        + " but have " + balance + ".",
                                Toast.LENGTH_LONG).show();
                        if (btnEnroll != null) { btnEnroll.setEnabled(true); btnEnroll.setText("Enroll — 🪙 " + totalTokens); }
                        return;
                    }

                    // Deduct tokens
                    EscrowManager.deductFromStudent(db, currentUser.getUid(), (int) totalTokens,
                            () -> {
                                // Record enrollment
                                Map<String, Object> enrollment = new HashMap<>();
                                enrollment.put("courseId",    courseId);
                                enrollment.put("studentUid",  currentUser.getUid());
                                enrollment.put("studentName", studentName != null ? studentName : "Student");
                                enrollment.put("tutorUid",    tutorUid);
                                enrollment.put("tokens",      totalTokens);
                                enrollment.put("enrolledAt",  FieldValue.serverTimestamp());
                                enrollment.put("status",      "active");
                                if (zoomLink != null) enrollment.put("zoomLink", zoomLink);

                                db.collection("enrollments").add(enrollment)
                                        .addOnSuccessListener(ref -> {
                                            // Increment enrolled count
                                            db.collection("courses").document(courseId)
                                                    .update("enrolledCount",
                                                            FieldValue.increment(1));
                                            // Mark full if needed
                                            if (enrolledCount + 1 >= maxStudents) {
                                                db.collection("courses").document(courseId)
                                                        .update("status", "full");
                                            }
                                            Toast.makeText(this,
                                                    "✅ Enrolled successfully!",
                                                    Toast.LENGTH_LONG).show();
                                            finish();
                                        })
                                        .addOnFailureListener(e -> {
                                            EscrowManager.atomicRefund(db, currentUser.getUid(), (int) totalTokens);
                                            Toast.makeText(this, "Enrollment failed: " + e.getMessage(),
                                                    Toast.LENGTH_LONG).show();
                                            if (btnEnroll != null) { btnEnroll.setEnabled(true); }
                                        });
                            },
                            () -> {
                                Toast.makeText(this, "Failed to deduct tokens.", Toast.LENGTH_SHORT).show();
                                if (btnEnroll != null) { btnEnroll.setEnabled(true); }
                            });
                });
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Course?")
                .setMessage("This will permanently delete the course. Enrolled students will not be automatically refunded.")
                .setPositiveButton("Delete", (d, w) -> {
                    db.collection("courses").document(courseId).delete()
                            .addOnSuccessListener(u -> {
                                Toast.makeText(this, "Course deleted.", Toast.LENGTH_SHORT).show();
                                finish();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Opens MessagingActivity with the course tutor.
     * Student can ask questions about the course before enrolling.
     */
    private void openChatWithTutor() {
        if (tutorUid == null || currentUser == null) {
            Toast.makeText(this, "Cannot open chat.", Toast.LENGTH_SHORT).show();
            return;
        }
        // Build stable convId same as TutorDetailActivity
        String myUid = currentUser.getUid();
        String convId = myUid.compareTo(tutorUid) < 0
                ? myUid + "_" + tutorUid
                : tutorUid + "_" + myUid;

        // Get tutor name for chat title
        String tutorDisplayName = tvTutor != null
                ? tvTutor.getText().toString().replace("👤 ", "") : "Tutor";

        Intent intent = new Intent(this, MessagingActivity.class);
        intent.putExtra("requestId",       convId);
        intent.putExtra("otherPersonName", tutorDisplayName);
        intent.putExtra("otherUid",        tutorUid);
        intent.putExtra("tutorUid",        tutorUid);
        intent.putExtra("studentUid",      myUid);
        startActivity(intent);
    }


    // ── Feature 4: Enrolled students (tutor view) ─────────────

    private void loadEnrolledStudents() {
        db.collection("enrollments")
                .whereEqualTo("courseId", courseId)
                .get()
                .addOnSuccessListener(snap -> {
                    android.view.View section = findViewById(R.id.layoutEnrolledStudents);
                    android.widget.LinearLayout list =
                            findViewById(R.id.layoutStudentList);
                    android.widget.TextView tvCount =
                            findViewById(R.id.tvEnrolledCount);

                    if (section != null) section.setVisibility(android.view.View.VISIBLE);
                    if (tvCount != null)
                        tvCount.setText(snap.size() + " student" + (snap.size() != 1 ? "s" : "") + " enrolled");

                    if (list != null) {
                        list.removeAllViews();
                        for (com.google.firebase.firestore.DocumentSnapshot doc : snap.getDocuments()) {
                            String name = doc.getString("studentName");
                            java.util.Date enrolledAt = doc.getDate("enrolledAt");

                            android.widget.LinearLayout row = new android.widget.LinearLayout(this);
                            row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
                            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
                            android.widget.LinearLayout.LayoutParams rp =
                                    new android.widget.LinearLayout.LayoutParams(
                                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                            android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
                            rp.setMargins(0, 0, 0, dpC(10));
                            row.setLayoutParams(rp);

                            // Avatar
                            com.google.android.material.card.MaterialCardView avatar =
                                    new com.google.android.material.card.MaterialCardView(this);
                            android.widget.LinearLayout.LayoutParams ap =
                                    new android.widget.LinearLayout.LayoutParams(dpC(40), dpC(40));
                            ap.setMarginEnd(dpC(12));
                            avatar.setLayoutParams(ap);
                            avatar.setRadius(dpC(20));
                            avatar.setCardElevation(0);
                            avatar.setCardBackgroundColor(0xFF8A2EFF);
                            android.widget.TextView tvInit = new android.widget.TextView(this);
                            String initials = (name != null && !name.isEmpty())
                                    ? String.valueOf(name.charAt(0)).toUpperCase() : "?";
                            tvInit.setText(initials);
                            tvInit.setTextColor(0xFFFFFFFF);
                            tvInit.setTextSize(16f);
                            tvInit.setGravity(android.view.Gravity.CENTER);
                            tvInit.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT));
                            avatar.addView(tvInit);
                            row.addView(avatar);

                            // Name + date
                            android.widget.LinearLayout info = new android.widget.LinearLayout(this);
                            info.setOrientation(android.widget.LinearLayout.VERTICAL);
                            info.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                                    0, android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                            android.widget.TextView tvName = new android.widget.TextView(this);
                            tvName.setText(name != null ? name : "Student");
                            tvName.setTextColor(0xFF071A3D);
                            tvName.setTextSize(14f);
                            tvName.setTypeface(null, android.graphics.Typeface.BOLD);
                            info.addView(tvName);
                            if (enrolledAt != null) {
                                android.widget.TextView tvDate = new android.widget.TextView(this);
                                tvDate.setText("Enrolled: " + new java.text.SimpleDateFormat(
                                        "MMM d", java.util.Locale.getDefault()).format(enrolledAt));
                                tvDate.setTextColor(0xFF8B97A8);
                                tvDate.setTextSize(12f);
                                info.addView(tvDate);
                            }
                            row.addView(info);
                            list.addView(row);
                        }
                        if (snap.isEmpty()) {
                            android.widget.TextView empty = new android.widget.TextView(this);
                            empty.setText("No students enrolled yet.");
                            empty.setTextColor(0xFF8B97A8);
                            empty.setTextSize(13f);
                            list.addView(empty);
                        }
                    }
                });
    }


    // ── Feature 9: Release escrow when course completes ───────

    private void confirmMarkComplete() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Mark Course as Complete?")
                .setMessage("This will:\n\n"
                        + "• Release tokens to your account from all enrolled students\n"
                        + "• Mark the course as completed\n"
                        + "• Students will be prompted to leave a review\n\n"
                        + "This cannot be undone.")
                .setPositiveButton("Complete Course", (d, w) -> processCourseCompletion())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void processCourseCompletion() {
        if (courseId == null || tutorUid == null) return;

        // Get all enrollments for this course
        db.collection("enrollments")
                .whereEqualTo("courseId", courseId)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) {
                        // No students enrolled — just mark complete
                        db.collection("courses").document(courseId)
                                .update("status", "completed");
                        Toast.makeText(this, "Course marked as complete.", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    // Calculate total tokens to release
                    long totalToRelease = 0;
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snap.getDocuments()) {
                        Long t = doc.getLong("tokens");
                        if (t != null) totalToRelease += t;
                    }
                    final long release = totalToRelease;

                    // Release tokens to tutor using atomic increment
                    db.collection("users").document(tutorUid)
                            .update("tokens",
                                    com.google.firebase.firestore.FieldValue.increment(release),
                                    "totalEarnings",
                                    com.google.firebase.firestore.FieldValue.increment(release))
                            .addOnSuccessListener(u -> {
                                // Mark course completed
                                db.collection("courses").document(courseId)
                                        .update("status", "completed");
                                // Update all enrollments
                                for (com.google.firebase.firestore.DocumentSnapshot doc : snap.getDocuments()) {
                                    doc.getReference().update("status", "completed");
                                }
                                // Log earnings
                                java.util.Map<String, Object> earning = new java.util.HashMap<>();
                                earning.put("tutorUid",    tutorUid);
                                earning.put("courseId",    courseId);
                                earning.put("tokens",      release);
                                earning.put("type",        "course_completion");
                                earning.put("createdAt",   com.google.firebase.firestore.FieldValue.serverTimestamp());
                                db.collection("tutorEarnings").add(earning);

                                Toast.makeText(this,
                                        "✅ Course completed! " + release + " tokens released to your account.",
                                        Toast.LENGTH_LONG).show();
                                finish();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Error: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show());
                });
    }

    // ── Feature 5: Edit course (tutor only) ───────────────────

    private void showEditDialog() {
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(dpC(20), dpC(12), dpC(20), dpC(8));

        android.widget.TextView tvLabel = new android.widget.TextView(this);
        tvLabel.setText("Update Zoom Link");
        tvLabel.setTextColor(0xFF8B97A8);
        tvLabel.setTextSize(12f);
        tvLabel.setPadding(0, 0, 0, dpC(4));
        layout.addView(tvLabel);

        android.widget.EditText etZoom = new android.widget.EditText(this);
        etZoom.setHint("https://zoom.us/j/...");
        etZoom.setText(zoomLink != null ? zoomLink : "");
        etZoom.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_URI);
        etZoom.setTextSize(13f);
        layout.addView(etZoom);

        android.widget.TextView tvLabel2 = new android.widget.TextView(this);
        tvLabel2.setText("Update Description");
        tvLabel2.setTextColor(0xFF8B97A8);
        tvLabel2.setTextSize(12f);
        tvLabel2.setPadding(0, dpC(16), 0, dpC(4));
        layout.addView(tvLabel2);

        android.widget.EditText etDesc = new android.widget.EditText(this);
        etDesc.setHint("Course description...");
        String currentDesc = tvDescription != null
                ? tvDescription.getText().toString() : "";
        etDesc.setText(currentDesc);
        etDesc.setMinLines(3);
        etDesc.setGravity(android.view.Gravity.TOP);
        etDesc.setInputType(android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        layout.addView(etDesc);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("✏️ Edit Course")
                .setView(layout)
                .setPositiveButton("Save", (d, w) -> {
                    String newZoom = etZoom.getText().toString().trim();
                    String newDesc = etDesc.getText().toString().trim();
                    java.util.Map<String, Object> updates = new java.util.HashMap<>();
                    if (!newZoom.isEmpty()) {
                        updates.put("zoomLink", newZoom);
                        zoomLink = newZoom;
                    }
                    if (!newDesc.isEmpty()) updates.put("description", newDesc);
                    if (!updates.isEmpty()) {
                        db.collection("courses").document(courseId).update(updates)
                                .addOnSuccessListener(u -> {
                                    Toast.makeText(this, "✅ Course updated!", Toast.LENGTH_SHORT).show();
                                    if (tvDescription != null && !newDesc.isEmpty())
                                        tvDescription.setText(newDesc);
                                    if (!newZoom.isEmpty() && layoutZoom != null) {
                                        layoutZoom.setVisibility(android.view.View.VISIBLE);
                                        if (tvZoomLink != null) tvZoomLink.setText("🎥 " + newZoom);
                                    }
                                });
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private int dpC(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private int darken(int color, float factor) {
        int r = Math.max(0, (int) (Color.red(color)   * factor));
        int g = Math.max(0, (int) (Color.green(color) * factor));
        int b = Math.max(0, (int) (Color.blue(color)  * factor));
        return Color.argb(255, r, g, b);
    }
}