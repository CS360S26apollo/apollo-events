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
 * Initial screen for Tutor onboarding flow.
 */
public class TutorIntroFragment extends Fragment {

    public TutorIntroFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tutor_intro, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Button getStartedButton = view.findViewById(R.id.buttonGetStarted);
        getStartedButton.setOnClickListener(v -> {
            ((ProfileActivity) requireActivity()).goToNameStep();
        });
    }
}
