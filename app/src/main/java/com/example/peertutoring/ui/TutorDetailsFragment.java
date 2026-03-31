package com.example.peertutoring.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;
import com.example.peertutoring.R;

/**
 * Fragment responsible for collecting specific professional details for Tutor onboarding.
 * Role: View component for Step 2 of the Tutor profile creation flow.
 * Purpose: Captures the tutor's academic level and desired hourly token rate.
 * 
 * Implementation Details:
 * - Uses a dropdown (AutoCompleteTextView) for academic level selection.
 * - Validates that both level and rate are provided before continuing.
 */
public class TutorDetailsFragment extends Fragment {

    private AutoCompleteTextView levelAutoComplete;
    private TextInputEditText rateEditText;
    private ProfileActivity profileActivity;

    public TutorDetailsFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tutor_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        profileActivity = (ProfileActivity) requireActivity();
        
        levelAutoComplete = view.findViewById(R.id.autoCompleteLevel);
        rateEditText = view.findViewById(R.id.editTextRate);
        Button backButton = view.findViewById(R.id.buttonBack);
        Button continueButton = view.findViewById(R.id.buttonContinue);

        // Setup Level dropdown
        String[] levels = {"Undergraduate - Freshman", "Undergraduate - Sophomore", "Undergraduate - Junior", "Undergraduate - Senior", "Graduate", "PhD Candidate", "Professional"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, levels);
        if (levelAutoComplete != null) {
            levelAutoComplete.setAdapter(adapter);
        }

        if (backButton != null) {
            backButton.setOnClickListener(v -> profileActivity.goBackToName());
        }

        if (continueButton != null) {
            continueButton.setOnClickListener(v -> {
                String level = levelAutoComplete.getText().toString().trim();
                String rateStr = rateEditText.getText() != null ? rateEditText.getText().toString().trim() : "";

                if (TextUtils.isEmpty(level)) {
                    levelAutoComplete.setError("Required");
                    return;
                }
                if (TextUtils.isEmpty(rateStr)) {
                    rateEditText.setError("Required");
                    return;
                }

                try {
                    int rate = Integer.parseInt(rateStr);
                    // bio is already stored in ProfileActivity from NameFragment step
                    profileActivity.goToTutorSubjects(null, level, rate);
                } catch (NumberFormatException e) {
                    rateEditText.setError("Invalid rate");
                }
            });
        }
    }
}