package com.example.peertutoring.ui;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.example.peertutoring.R;

import java.util.ArrayList;

/**
 * Fragment responsible for collecting academic information during onboarding.
 * Role: View component for Step 2 (Student) or Step 3 (Tutor) of profile creation.
 * Purpose: Captures the user's institution (for students) and subjects of interest.
 * 
 * Outstanding Issues: 
 * - Subject list is currently hardcoded in the layout/code.
 * - Institution validation is basic (non-empty check).
 */
public class AcademicFragment extends Fragment {

    private TextInputEditText institutionEditText;
    private TextInputEditText customSubjectEditText;
    private ChipGroup chipGroup;
    private final ArrayList<String> selectedSubjects = new ArrayList<>();
    private ProfileActivity profileActivity;

    public AcademicFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_academic, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        profileActivity = (ProfileActivity) requireActivity();
        
        institutionEditText = view.findViewById(R.id.editTextInstitution);
        customSubjectEditText = view.findViewById(R.id.editTextCustomSubject);
        chipGroup = view.findViewById(R.id.chipGroupSubjects);
        Button backButton = view.findViewById(R.id.buttonBack);
        Button continueButton = view.findViewById(R.id.buttonContinue);
        Button addSubjectButton = view.findViewById(R.id.btnAddSubject);

        // Adjust UI for Tutor flow (Hide institution section)
        if ("tutor".equals(profileActivity.getRole())) {
            if (institutionEditText != null) {
                try {
                    View textInputLayout = (View) institutionEditText.getParent().getParent();
                    if (textInputLayout != null) {
                        textInputLayout.setVisibility(View.GONE);
                    }
                } catch (Exception e) {
                    institutionEditText.setVisibility(View.GONE);
                }
            }
        }

        setupChip(view, R.id.chipMathematics, "Mathematics");
        setupChip(view, R.id.chipPhysics, "Physics");
        setupChip(view, R.id.chipChemistry, "Chemistry");
        setupChip(view, R.id.chipBiology, "Biology");
        setupChip(view, R.id.chipComputerScience, "Computer Science");
        setupChip(view, R.id.chipEnglish, "English");
        setupChip(view, R.id.chipHistory, "History");
        setupChip(view, R.id.chipEconomics, "Economics");

        addSubjectButton.setOnClickListener(v -> {
            String subject = customSubjectEditText.getText().toString().trim();
            if (!TextUtils.isEmpty(subject)) {
                addNewChip(subject);
                customSubjectEditText.setText("");
            }
        });

        backButton.setOnClickListener(v -> {
            if ("tutor".equals(profileActivity.getRole())) {
                profileActivity.goBackToTutorDetails();
            } else {
                profileActivity.goBackToName();
            }
        });

        continueButton.setOnClickListener(v -> {
            if (selectedSubjects.isEmpty()) {
                Toast.makeText(getContext(), "Please select at least one subject", Toast.LENGTH_SHORT).show();
                return;
            }

            if ("tutor".equals(profileActivity.getRole())) {
                profileActivity.finishTutorRegistration(selectedSubjects);
            } else {
                String institution = institutionEditText.getText() != null
                        ? institutionEditText.getText().toString().trim() : "";

                if (TextUtils.isEmpty(institution)) {
                    institutionEditText.setError("Required");
                    return;
                }
                profileActivity.goToGoals(institution, selectedSubjects);
            }
        });
    }

    /**
     * Initializes a chip for a specific subject and sets up its selection logic.
     * @param view The root view containing the chip.
     * @param chipId Resource ID of the Chip.
     * @param subjectName The name of the subject associated with this chip.
     */
    private void setupChip(View view, int chipId, String subjectName) {
        Chip chip = view.findViewById(chipId);
        if (chip != null) {
            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    if (!selectedSubjects.contains(subjectName)) {
                        selectedSubjects.add(subjectName);
                    }
                    chip.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#089A3C")));
                    chip.setTextColor(Color.WHITE);
                } else {
                    selectedSubjects.remove(subjectName);
                    chip.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#EAF9EE")));
                    chip.setTextColor(Color.parseColor("#089A3C"));
                }
            });
        }
    }

    /**
     * Dynamically adds a new chip to the group for a user-entered subject.
     * @param subjectName The name of the custom subject to add.
     */
    private void addNewChip(String subjectName) {
        Chip chip = new Chip(getContext());
        chip.setText(subjectName);
        chip.setCheckable(true);
        chip.setChecked(true);
        chip.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#089A3C")));
        chip.setTextColor(Color.WHITE);
        
        selectedSubjects.add(subjectName);
        
        chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (!selectedSubjects.contains(subjectName)) {
                    selectedSubjects.add(subjectName);
                }
            } else {
                selectedSubjects.remove(subjectName);
            }
        });
        
        chipGroup.addView(chip);
    }
}