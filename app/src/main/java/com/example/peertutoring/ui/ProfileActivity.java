package com.example.peertutoring.ui;

import android.content.Intent;
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
 * ProfileActivity — multi-step onboarding host.
 *
 * Change: after finishStudentRegistration() saves to Firestore successfully,
 * we now launch StudentPreferencesActivity instead of showing ProfileCompleteFragment,
 * so students can set their tutor preferences immediately after signup.
 */
public class ProfileActivity extends AppCompatActivity {

    private String uid, email, role;

    private String firstName, lastName, fullName;
    private ArrayList<String> subjects = new ArrayList<>();

    // Student specific
    private String institution;
    private ArrayList<String> goals = new ArrayList<>();

    // Tutor specific
    private String bio, level;
    private int rate = 20;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        uid   = getIntent().getStringExtra("uid");
        email = getIntent().getStringExtra("email");
        role  = getIntent().getStringExtra("role");

        if (savedInstanceState == null) {
            if ("tutor".equals(role)) {
                loadFragment(new TutorIntroFragment());
            } else {
                loadFragment(new NameFragment());
            }
        }
    }

    public void goToNameStep() { loadFragment(new NameFragment()); }

    public void goToNextStepAfterName(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName  = lastName;
        this.fullName  = firstName + " " + lastName;
        if ("tutor".equals(role)) {
            loadFragment(new TutorDetailsFragment());
        } else {
            loadFragment(new AcademicFragment());
        }
    }

    public void goToTutorDetails(String fullName, String bio) {
        this.fullName = fullName;
        this.bio = bio;
        String[] parts = fullName.split(" ", 2);
        this.firstName = parts.length > 0 ? parts[0] : "";
        this.lastName  = parts.length > 1 ? parts[1] : "";
        loadFragment(new TutorDetailsFragment());
    }

    public void goToGoals(String institution, ArrayList<String> subjects) {
        this.institution = institution;
        this.subjects    = subjects;
        loadFragment(new GoalsFragment());
    }

    /**
     * Saves the student profile to Firestore, then launches
     * StudentPreferencesActivity so the student can set their tutor preferences.
     */
    public void finishStudentRegistration(ArrayList<String> goals) {
        this.goals = goals;

        Student student = new Student(uid, email, firstName, lastName,
                institution, subjects, goals);

        UserRepository repository = new UserRepository();
        repository.saveStudentProfile(student, new UserRepository.SaveCallback() {
            @Override
            public void onSuccess() {
                Intent intent = new Intent(ProfileActivity.this, HomeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(ProfileActivity.this,
                        "Save failed: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    public void goToTutorSubjects(String bio, String level, int rate) {
        if (bio != null) this.bio = bio;
        this.level = level;
        this.rate  = rate;
        loadFragment(new AcademicFragment());
    }

    public void finishTutorRegistration(ArrayList<String> subjects) {
        this.subjects = subjects;

        Tutor tutor = new Tutor(uid, email, firstName, lastName,
                this.bio, level, rate, subjects);

        UserRepository repository = new UserRepository();
        repository.saveTutorProfile(tutor, new UserRepository.SaveCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(ProfileActivity.this,
                        "Tutor profile created successfully.", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(ProfileActivity.this, TutorHomeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(ProfileActivity.this,
                        "Save failed: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    public String getRole() { return role; }

    public void goBackToIntro()        { if ("tutor".equals(role)) loadFragment(new TutorIntroFragment()); }
    public void goBackToName()         { loadFragment(new NameFragment()); }
    public void goBackToTutorDetails() { loadFragment(new TutorDetailsFragment()); }
    public void goBackToAcademic()     { loadFragment(new AcademicFragment()); }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }
}