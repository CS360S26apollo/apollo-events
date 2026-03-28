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

import com.google.android.material.textfield.TextInputEditText;
import com.example.peertutoring.R;

/**
 * Step 1 of profile onboarding: collect first and last name.
 */
public class NameFragment extends Fragment {

    private TextInputEditText firstNameEditText;
    private TextInputEditText lastNameEditText;
    private Button backButton;
    private Button continueButton;

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
        firstNameEditText = view.findViewById(R.id.editTextFirstName);
        lastNameEditText = view.findViewById(R.id.editTextLastName);
        backButton = view.findViewById(R.id.buttonBack);
        continueButton = view.findViewById(R.id.buttonContinue);

        backButton.setOnClickListener(v -> requireActivity().finish());

        continueButton.setOnClickListener(v -> {
            String firstName = firstNameEditText.getText() != null
                    ? firstNameEditText.getText().toString().trim() : "";
            String lastName = lastNameEditText.getText() != null
                    ? lastNameEditText.getText().toString().trim() : "";

            if (TextUtils.isEmpty(firstName) || TextUtils.isEmpty(lastName)) {
                if (TextUtils.isEmpty(firstName)) {
                    firstNameEditText.setError("Required");
                }
                if (TextUtils.isEmpty(lastName)) {
                    lastNameEditText.setError("Required");
                }
                return;
            }

            ((ProfileActivity) requireActivity()).goToAcademic(firstName, lastName);
        });
    }
}
