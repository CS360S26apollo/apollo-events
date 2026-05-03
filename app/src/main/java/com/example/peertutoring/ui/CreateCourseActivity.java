package com.example.peertutoring.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.peertutoring.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Tutors create a crash course here.
 * Stored in Firestore: courses/{courseId}
 */
public class CreateCourseActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    private EditText etTitle, etDescription, etDurationDays,
            etSessionsPerWeek, etSessionMinutes, etTotalTokens,
            etMaxStudents, etTopics, etZoomLink;
    private AutoCompleteTextView spinnerSubject, spinnerLevel, spinnerType;
    private Button btnCreate;

    private static final String[] SUBJECTS = {
            "Mathematics", "Physics", "Chemistry", "Biology",
            "Computer Science", "English", "History", "Economics",
            "Urdu", "Statistics", "Further Mathematics"
    };
    private static final String[] LEVELS = {
            "O-Level", "A-Level", "Matric", "FSc", "University", "Any Level"
    };
    private static final String[] TYPES = { "Online (Zoom)", "In-Person" };

    // Thumbnail combos: {emoji, gradientStartColor}
    private static final String[][] SUBJECT_THEMES = {
            {"📐", "#8A2EFF"}, {"⚛️", "#0062FF"}, {"🧪", "#00C853"},
            {"🧬", "#FF6B35"}, {"💻", "#4ECDC4"}, {"📖", "#FF8C42"},
            {"🏛️", "#E63946"}, {"📈", "#2EC4B6"}, {"✍️", "#8338EC"},
            {"📊", "#FB5607"}, {"∑", "#3A86FF"}, {"🎓", "#8A2EFF"}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_course);

        db          = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) { finish(); return; }

        bindViews();
        setupDropdowns();

        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        if (btnCreate != null) btnCreate.setOnClickListener(v -> validateAndCreate());
    }

    private void bindViews() {
        etTitle           = findViewById(R.id.etCourseTitle);
        etDescription     = findViewById(R.id.etCourseDescription);
        etDurationDays    = findViewById(R.id.etDurationDays);
        etSessionsPerWeek = findViewById(R.id.etSessionsPerWeek);
        etSessionMinutes  = findViewById(R.id.etSessionMinutes);
        etTotalTokens     = findViewById(R.id.etTotalTokens);
        etMaxStudents     = findViewById(R.id.etMaxStudents);
        etTopics          = findViewById(R.id.etTopics);
        etZoomLink        = findViewById(R.id.etZoomLink);
        spinnerSubject    = findViewById(R.id.spinnerCourseSubject);
        spinnerLevel      = findViewById(R.id.spinnerCourseLevel);
        spinnerType       = findViewById(R.id.spinnerCourseType);
        btnCreate         = findViewById(R.id.btnCreateCourse);
    }

    private void setupDropdowns() {
        if (spinnerSubject != null)
            spinnerSubject.setAdapter(new ArrayAdapter<>(this,
                    android.R.layout.simple_dropdown_item_1line, SUBJECTS));
        if (spinnerLevel != null)
            spinnerLevel.setAdapter(new ArrayAdapter<>(this,
                    android.R.layout.simple_dropdown_item_1line, LEVELS));
        if (spinnerType != null) {
            spinnerType.setAdapter(new ArrayAdapter<>(this,
                    android.R.layout.simple_dropdown_item_1line, TYPES));
            spinnerType.setText(TYPES[0], false);
            // Show/hide Zoom link field based on type
            spinnerType.setOnItemClickListener((parent, view, pos, id) -> {
                if (etZoomLink != null) {
                    android.view.View zoomRow = findViewById(R.id.layoutZoomLink);
                    if (zoomRow != null)
                        zoomRow.setVisibility(pos == 0
                                ? android.view.View.VISIBLE : android.view.View.GONE);
                }
            });
        }
    }

    private void validateAndCreate() {
        String title       = getText(etTitle);
        String description = getText(etDescription);
        String subject     = spinnerSubject != null ? spinnerSubject.getText().toString().trim() : "";
        String level       = spinnerLevel   != null ? spinnerLevel.getText().toString().trim() : "";
        String type        = spinnerType    != null ? spinnerType.getText().toString().trim() : "";
        String topics      = getText(etTopics);
        String zoomLink    = getText(etZoomLink);

        if (title.isEmpty())       { etTitle.setError("Enter course title");         return; }
        if (subject.isEmpty())     { spinnerSubject.setError("Select a subject");    return; }
        if (level.isEmpty())       { spinnerLevel.setError("Select a level");        return; }
        if (description.isEmpty()) { etDescription.setError("Describe your course"); return; }

        int durationDays    = parseInt(etDurationDays,    30);
        int sessionsPerWeek = parseInt(etSessionsPerWeek, 5);
        int sessionMinutes  = parseInt(etSessionMinutes,  60);
        int totalTokens     = parseInt(etTotalTokens,     500);
        int maxStudents     = parseInt(etMaxStudents,      10);

        if (totalTokens < 1)  { etTotalTokens.setError("Enter total tokens");       return; }

        // Determine thumbnail theme from subject
        int themeIdx = subjectThemeIndex(subject);
        String emoji     = SUBJECT_THEMES[themeIdx][0];
        String color     = SUBJECT_THEMES[themeIdx][1];
        String sessionTypeVal = type.contains("Online") ? "online" : "inperson";

        if (btnCreate != null) { btnCreate.setEnabled(false); btnCreate.setText("Creating..."); }

        // Fetch tutor name
        db.collection("users").document(currentUser.getUid()).get()
                .addOnSuccessListener(doc -> {
                    String tutorName = doc.getString("fullName");
                    if (tutorName == null || tutorName.isEmpty()) tutorName = "Tutor";

                    Map<String, Object> course = new HashMap<>();
                    course.put("tutorUid",        currentUser.getUid());
                    course.put("tutorName",        tutorName);
                    course.put("title",            title);
                    course.put("subject",          subject);
                    course.put("level",            level);
                    course.put("description",      description);
                    course.put("durationDays",     durationDays);
                    course.put("sessionsPerWeek",  sessionsPerWeek);
                    course.put("sessionMinutes",   sessionMinutes);
                    course.put("totalTokens",      totalTokens);
                    course.put("maxStudents",      maxStudents);
                    course.put("enrolledCount",    0);
                    course.put("thumbnailEmoji",   emoji);
                    course.put("thumbnailColor",   color);
                    course.put("sessionType",      sessionTypeVal);
                    course.put("status",           "open");
                    course.put("createdAt",        FieldValue.serverTimestamp());
                    if (!zoomLink.isEmpty()) course.put("zoomLink", zoomLink);

                    // Topics: split by comma
                    if (!topics.isEmpty()) {
                        course.put("topics", Arrays.asList(topics.split("\\s*,\\s*")));
                    }

                    db.collection("courses").add(course)
                            .addOnSuccessListener(ref -> {
                                ref.update("courseId", ref.getId());
                                Toast.makeText(this,
                                        "✅ Course created successfully!",
                                        Toast.LENGTH_LONG).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                                if (btnCreate != null) {
                                    btnCreate.setEnabled(true);
                                    btnCreate.setText("Create Course");
                                }
                            });
                });
    }

    private String getText(EditText et) {
        return et != null && et.getText() != null ? et.getText().toString().trim() : "";
    }

    private int parseInt(EditText et, int defaultVal) {
        try { return Integer.parseInt(getText(et)); }
        catch (NumberFormatException e) { return defaultVal; }
    }

    private int subjectThemeIndex(String subject) {
        String[] keys = {"Math","Phys","Chem","Bio","Comp","Eng","Hist","Econ","Urdu","Stat","Further"};
        for (int i = 0; i < keys.length; i++)
            if (subject.toLowerCase().contains(keys[i].toLowerCase())) return i;
        return SUBJECT_THEMES.length - 1;
    }
}