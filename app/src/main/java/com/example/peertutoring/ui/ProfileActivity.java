package com.example.peertutoring.ui;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.peertutoring.R;
import com.example.peertutoring.data.UserRepository;
import com.example.peertutoring.models.Student;

import java.util.ArrayList;

/**
 * Container activity for the multi-step student onboarding flow.
 */
public class ProfileActivity extends AppCompatActivity {

    private String uid;
    private String email;

    private String firstName;
    private String lastName;
    private String institution;
    private ArrayList<String> subjects = new ArrayList<>();
    private ArrayList<String> goals = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        uid = getIntent().getStringExtra("uid");
        email = getIntent().getStringExtra("email");

        if (savedInstanceState == null) {
            loadFragment(new NameFragment());
        }
    }

    public void goToAcademic(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
        loadFragment(new AcademicFragment());
    }

    public void goToGoals(String institution, ArrayList<String> subjects) {
        this.institution = institution;
        this.subjects = subjects;
        loadFragment(new GoalsFragment());
    }

    public void finishRegistration(ArrayList<String> goals) {
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
                Toast.makeText(ProfileActivity.this,
                        "Student profile created successfully.", Toast.LENGTH_LONG).show();
                finishAffinity();
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(ProfileActivity.this,
                        "Save failed: " + error,
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    public void goBackToName() {
        loadFragment(new NameFragment());
    }

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