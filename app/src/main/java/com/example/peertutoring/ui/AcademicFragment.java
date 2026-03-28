package com.example.peertutoring.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;
import com.example.peertutoring.R;

import java.util.ArrayList;

/**
 * Step 2 of profile onboarding: collect institution and subjects.
 */
public class AcademicFragment extends Fragment {

    private TextInputEditText institutionEditText;
    private final ArrayList<String> selectedSubjects = new ArrayList<>();

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
        institutionEditText = view.findViewById(R.id.editTextInstitution);
        Button backButton = view.findViewById(R.id.buttonBack);
        Button continueButton = view.findViewById(R.id.buttonContinue);

        setupChip(view, R.id.chipMathematics, "Mathematics");
        setupChip(view, R.id.chipPhysics, "Physics");
        setupChip(view, R.id.chipChemistry, "Chemistry");
        setupChip(view, R.id.chipBiology, "Biology");
        setupChip(view, R.id.chipComputerScience, "Computer Science");
        setupChip(view, R.id.chipEnglish, "English");
        setupChip(view, R.id.chipHistory, "History");
        setupChip(view, R.id.chipEconomics, "Economics");

        backButton.setOnClickListener(v -> ((ProfileActivity) requireActivity()).goBackToName());

        continueButton.setOnClickListener(v -> {
            String institution = institutionEditText.getText() != null
                    ? institutionEditText.getText().toString().trim() : "";

            if (TextUtils.isEmpty(institution)) {
                institutionEditText.setError("Required");
                return;
            }

            if (selectedSubjects.isEmpty()) {
                return;
            }

            ((ProfileActivity) requireActivity()).goToGoals(institution, selectedSubjects);
        });
    }

    private void setupChip(View view, int chipId, String subjectName) {
        Chip chip = view.findViewById(chipId);
        chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (!selectedSubjects.contains(subjectName)) {
                    selectedSubjects.add(subjectName);
                }
            } else {
                selectedSubjects.remove(subjectName);
            }
        });
    }
}
