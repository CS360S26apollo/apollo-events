package com.example.peertutoring.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;
import com.example.peertutoring.R;

/**
 * Step 1 of profile onboarding: collect name (and bio for tutors).
 */
public class NameFragment extends Fragment {

    private TextInputEditText firstNameEditText;
    private TextInputEditText lastNameEditText;
    private TextInputEditText fullNameEditText;
    private TextInputEditText bioEditText;
    private TextView charCountTextView;
    private LinearLayout studentFieldsLayout;
    private LinearLayout tutorFieldsLayout;
    private ProfileActivity profileActivity;

    public NameFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_name, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        profileActivity = (ProfileActivity) requireActivity();
        
        // Initialize Student views
        firstNameEditText = view.findViewById(R.id.editTextFirstName);
        lastNameEditText = view.findViewById(R.id.editTextLastName);
        studentFieldsLayout = view.findViewById(R.id.layoutStudentFields);
        
        // Initialize Tutor views
        fullNameEditText = view.findViewById(R.id.editTextFullName);
        bioEditText = view.findViewById(R.id.editTextBio);
        charCountTextView = view.findViewById(R.id.textViewCharCount);
        tutorFieldsLayout = view.findViewById(R.id.layoutTutorFields);
        
        Button backButton = view.findViewById(R.id.buttonBack);
        Button continueButton = view.findViewById(R.id.buttonContinue);

        // Show/Hide fields based on role
        if ("tutor".equals(profileActivity.getRole())) {
            studentFieldsLayout.setVisibility(View.GONE);
            tutorFieldsLayout.setVisibility(View.VISIBLE);
        } else {
            studentFieldsLayout.setVisibility(View.VISIBLE);
            tutorFieldsLayout.setVisibility(View.GONE);
        }

        // Character count listener for Bio
        bioEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                charCountTextView.setText(s.length() + "/500 characters");
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        backButton.setOnClickListener(v -> {
            if ("tutor".equals(profileActivity.getRole())) {
                profileActivity.goBackToIntro();
            } else {
                requireActivity().finish();
            }
        });

        continueButton.setOnClickListener(v -> {
            if ("tutor".equals(profileActivity.getRole())) {
                String fullName = fullNameEditText.getText() != null
                        ? fullNameEditText.getText().toString().trim() : "";
                String bio = bioEditText.getText() != null
                        ? bioEditText.getText().toString().trim() : "";

                if (TextUtils.isEmpty(fullName)) {
                    fullNameEditText.setError("Full name is required");
                    return;
                }
                if (TextUtils.isEmpty(bio)) {
                    bioEditText.setError("Bio is required");
                    return;
                }
                profileActivity.goToTutorDetails(fullName, bio);
            } else {
                String firstName = firstNameEditText.getText() != null
                        ? firstNameEditText.getText().toString().trim() : "";
                String lastName = lastNameEditText.getText() != null
                        ? lastNameEditText.getText().toString().trim() : "";

                if (TextUtils.isEmpty(firstName)) {
                    firstNameEditText.setError("First name is required");
                    return;
                }
                if (TextUtils.isEmpty(lastName)) {
                    lastNameEditText.setError("Last name is required");
                    return;
                }
                profileActivity.goToNextStepAfterName(firstName, lastName);
            }
        });
    }
}
