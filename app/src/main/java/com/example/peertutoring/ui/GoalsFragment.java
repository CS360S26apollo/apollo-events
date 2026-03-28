package com.example.peertutoring.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;
import com.example.peertutoring.R;

import java.util.ArrayList;

/**
 * Step 3 of profile onboarding: collect learning goals and save profile.
 */
public class GoalsFragment extends Fragment {

    private final ArrayList<String> selectedGoals = new ArrayList<>();

    public GoalsFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_goals, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Button backButton = view.findViewById(R.id.buttonBack);
        Button finishButton = view.findViewById(R.id.buttonFinish);

        setupGoalCard(view, R.id.cardImproveGrades, "Improve Grades");
        setupGoalCard(view, R.id.cardExamPreparation, "Exam Preparation");
        setupGoalCard(view, R.id.cardConceptClarity, "Concept Clarity");
        setupGoalCard(view, R.id.cardHomeworkHelp, "Homework Help");

        backButton.setOnClickListener(v ->
                ((ProfileActivity) requireActivity()).goBackToAcademic()
        );

        finishButton.setOnClickListener(v -> {
            if (selectedGoals.isEmpty()) {
                Toast.makeText(requireContext(),
                        "Please select at least one learning goal.",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            ((ProfileActivity) requireActivity()).finishRegistration(
                    new ArrayList<>(selectedGoals)
            );
        });
    }

    private void setupGoalCard(View root, int cardId, String goal) {
        MaterialCardView card = root.findViewById(cardId);

        card.setClickable(true);
        card.setFocusable(true);

        card.setOnClickListener(v -> {
            if (selectedGoals.contains(goal)) {
                selectedGoals.remove(goal);
                card.setStrokeWidth(2);
                card.setStrokeColor(Color.parseColor("#D9DCE3"));
            } else {
                selectedGoals.add(goal);
                card.setStrokeWidth(6);
                card.setStrokeColor(Color.parseColor("#6C4CCF"));
            }

            Toast.makeText(requireContext(),
                    "Selected goals: " + selectedGoals.size(),
                    Toast.LENGTH_SHORT).show();
        });
    }
}