package com.example.peertutoring.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.peertutoring.R;

/**
 * Fragment that displays a success message upon completion of the student onboarding.
 * Role: View component for the final confirmation step of User Story 1.
 * Purpose: Confirms profile creation and provides navigation to the main application dashboard.
 */
public class ProfileCompleteFragment extends Fragment {

    public ProfileCompleteFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile_complete, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (view.findViewById(R.id.buttonFindTutor) != null) {
            view.findViewById(R.id.buttonFindTutor).setOnClickListener(v -> goToHome());
        }
        if (view.findViewById(R.id.buttonViewDashboard) != null) {
            view.findViewById(R.id.buttonViewDashboard).setOnClickListener(v -> goToHome());
        }
    }

    /**
     * Navigates the user to the HomeActivity and clears the activity stack.
     */
    private void goToHome() {
        Intent intent = new Intent(requireActivity(), HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}