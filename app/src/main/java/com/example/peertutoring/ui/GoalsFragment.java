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
 * Fragment for the final step of student onboarding.
 * Role: View component for Step 3 of the student-specific profile creation flow.
 * Purpose: Collects the student's learning goals (e.g., Improve Grades, Homework Help)
 * and triggers the final profile save operation to Firestore.
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

            ((ProfileActivity) requireActivity()).finishStudentRegistration(
                    new ArrayList<>(selectedGoals)
            );
        });
    }

    /**
     * Initializes a goal card with selection logic and visual feedback.
     * @param root The parent view.
     * @param cardId The resource ID of the MaterialCardView.
     * @param goal The string representation of the goal.
     */
    private void setupGoalCard(View root, int cardId, String goal) {
        MaterialCardView card = root.findViewById(cardId);

        if (card != null) {
            card.setOnClickListener(v -> {
                if (selectedGoals.contains(goal)) {
                    selectedGoals.remove(goal);
                    card.setStrokeColor(Color.parseColor("#D9DCE3"));
                    card.setStrokeWidth(1);
                } else {
                    selectedGoals.add(goal);
                    card.setStrokeColor(Color.parseColor("#8A2EFF"));
                    card.setStrokeWidth(4);
                }
            });
        }
    }
}