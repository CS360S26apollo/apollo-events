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
            btnEnroll.setText("Delete Course");
            btnEnroll.setBackgroundColor(0xFFE53935);
            btnEnroll.setOnClickListener(v -> confirmDelete());
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

    private int darken(int color, float factor) {
        int r = Math.max(0, (int) (Color.red(color)   * factor));
        int g = Math.max(0, (int) (Color.green(color) * factor));
        int b = Math.max(0, (int) (Color.blue(color)  * factor));
        return Color.argb(255, r, g, b);
    }
}