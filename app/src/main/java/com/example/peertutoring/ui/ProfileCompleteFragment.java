package com.example.peertutoring.ui;

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
 * Final step of student onboarding: Profile Complete success screen.
 */
public class ProfileCompleteFragment extends Fragment {

    public ProfileCompleteFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile_complete, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Button findTutorButton = view.findViewById(R.id.buttonFindTutor);
        Button viewDashboardButton = view.findViewById(R.id.buttonViewDashboard);

        findTutorButton.setOnClickListener(v -> {
            // Logic to navigate to Tutor Discovery
            requireActivity().finishAffinity();
        });

        viewDashboardButton.setOnClickListener(v -> {
            // Logic to navigate to Dashboard
            requireActivity().finishAffinity();
        });
    }
}
