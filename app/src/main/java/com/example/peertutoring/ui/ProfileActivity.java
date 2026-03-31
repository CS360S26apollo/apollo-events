package com.example.peertutoring.ui;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.peertutoring.R;
import com.example.peertutoring.data.UserRepository;
import com.example.peertutoring.models.Student;
import com.example.peertutoring.models.Tutor;

import java.util.ArrayList;

/**
 * ProfileActivity serves as the main host for the multi-step onboarding process.
 * It manages the navigation between different fragments (Name, Academic, Goals, etc.)
 * based on the user's selected role (Student or Tutor).
 * 
 * Role: Navigation Controller / Step Orchestrator for US1 and US2.
 * Design Pattern: Fragment Container.
 */
public class ProfileActivity extends AppCompatActivity {

    private String uid;
    private String email;
    private String role;

    // Common fields
    private String firstName;
    private String lastName;
    private String fullName;
    private ArrayList<String> subjects = new ArrayList<>();

    // Student specific
    private String institution;
    private ArrayList<String> goals = new ArrayList<>();

    // Tutor specific
    private String bio;
    private String level;
    private int rate = 20; // Default rate

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        uid = getIntent().getStringExtra("uid");
        email = getIntent().getStringExtra("email");
        role = getIntent().getStringExtra("role");

        if (savedInstanceState == null) {
            if ("tutor".equals(role)) {
                loadFragment(new TutorIntroFragment());
            } else {
                loadFragment(new NameFragment());
            }
        }
    }

    /** Navigates to the Name collection step. */
    public void goToNameStep() {
        loadFragment(new NameFragment());
    }

    /** 
     * Navigates to the next step after names are provided.
     * Routes to TutorDetails for tutors or Academic info for students.
     */
    public void goToNextStepAfterName(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.fullName = firstName + " " + lastName;
        
        if ("tutor".equals(role)) {
            loadFragment(new TutorDetailsFragment());
        } else {
            loadFragment(new AcademicFragment());
        }
    }

    /** Routes the Tutor flow to the detailed information step. */
    public void goToTutorDetails(String fullName, String bio) {
        this.fullName = fullName;
        this.bio = bio;
        
        String[] parts = fullName.split(" ", 2);
        this.firstName = parts.length > 0 ? parts[0] : "";
        this.lastName = parts.length > 1 ? parts[1] : "";
        
        loadFragment(new TutorDetailsFragment());
    }

    // --- Student Flow Methods ---
    
    /** Routes the Student flow to the learning goals step. */
    public void goToGoals(String institution, ArrayList<String> subjects) {
        this.institution = institution;
        this.subjects = subjects;
        loadFragment(new GoalsFragment());
    }

    /** Finalizes student registration by persisting the profile to Firestore. */
    public void finishStudentRegistration(ArrayList<String> goals) {
        this.goals = goals;

        Student student = new Student(
                uid,
                email,
                firstName,
                lastName,
                institution,
                subjects,
                goals
        );

        UserRepository repository = new UserRepository();
        repository.saveStudentProfile(student, new UserRepository.SaveCallback() {
            @Override
            public void onSuccess() {
                loadFragment(new ProfileCompleteFragment());
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(ProfileActivity.this, "Save failed: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    // --- Tutor Flow Methods ---

    /** Routes the Tutor flow to the subject selection step. */
    public void goToTutorSubjects(String bio, String level, int rate) {
        if (bio != null) this.bio = bio;
        this.level = level;
        this.rate = rate;
        loadFragment(new AcademicFragment());
    }

    /** Finalizes tutor registration by persisting the profile to Firestore. */
    public void finishTutorRegistration(ArrayList<String> subjects) {
        this.subjects = subjects;

        Tutor tutor = new Tutor(
                uid,
                email,
                firstName,
                lastName,
                this.bio,
                level,
                rate,
                subjects
        );

        UserRepository repository = new UserRepository();
        repository.saveTutorProfile(tutor, new UserRepository.SaveCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(ProfileActivity.this,
                        "Tutor profile created successfully.", Toast.LENGTH_LONG).show();
                finishAffinity();
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(ProfileActivity.this, "Save failed: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    // --- Navigation Helpers ---

    /** @return The current user role (student/tutor). */
    public String getRole() {
        return role;
    }

    /** Navigates back to the Tutor introduction screen. */
    public void goBackToIntro() {
        if ("tutor".equals(role)) {
            loadFragment(new TutorIntroFragment());
        }
    }

    /** Navigates back to the Name entry screen. */
    public void goBackToName() {
        loadFragment(new NameFragment());
    }

    /** Navigates back to the Tutor details screen. */
    public void goBackToTutorDetails() {
        loadFragment(new TutorDetailsFragment());
    }

    /** Navigates back to the Academic info screen. */
    public void goBackToAcademic() {
        loadFragment(new AcademicFragment());
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }
}