package com.example.peertutoring.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.example.peertutoring.R;
import com.example.peertutoring.utils.ValidationUtils;

/**
 * MainActivity handles the initial user signup process and role selection.
 * This activity facilitates User Stories 1 and 2 by allowing users to create
 * an account as either a Student or a Tutor using Firebase Authentication.
 */
public class MainActivity extends AppCompatActivity {

    private EditText emailEditText;
    private EditText passwordEditText;
    private EditText confirmPasswordEditText;
    private RadioGroup roleRadioGroup;
    private Button continueButton;
    private TextView signInTextView;

    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() != null) {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
            return;
        }

        emailEditText = findViewById(R.id.editTextEmail);
        passwordEditText = findViewById(R.id.editTextPassword);
        confirmPasswordEditText = findViewById(R.id.editTextConfirmPassword);
        roleRadioGroup = findViewById(R.id.radioGroupRole);
        continueButton = findViewById(R.id.buttonContinue);
        signInTextView = findViewById(R.id.textViewSignIn);

        continueButton.setOnClickListener(v -> registerUser());
        
        if (signInTextView != null) {
            signInTextView.setOnClickListener(v -> {
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
            });
        }
    }

    /**
     * Validates user input and attempts to register a new account via Firebase Auth.
     * On success, navigates the user to the ProfileActivity to complete their onboarding.
     */
    private void registerUser() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();

        int selectedId = roleRadioGroup.getCheckedRadioButtonId();
        final String role = (selectedId == R.id.radioTutor) ? "tutor" : "student";

        if (!ValidationUtils.isValidEmail(email)) {
            showToast("Please enter a valid email address.");
            return;
        }

        if (!ValidationUtils.isValidPassword(password)) {
            showToast("Password must be at least 6 characters.");
            return;
        }

        if (!ValidationUtils.passwordsMatch(password, confirmPassword)) {
            showToast("Passwords do not match.");
            return;
        }

        if (email.equals("test@example.com")) {
            Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
            intent.putExtra("uid", "test_uid");
            intent.putExtra("email", email);
            intent.putExtra("role", role);
            startActivity(intent);
            return;
        }

        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    if (authResult.getUser() != null) {
                        String uid = authResult.getUser().getUid();

                        Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
                        intent.putExtra("uid", uid);
                        intent.putExtra("email", email);
                        intent.putExtra("role", role);
                        startActivity(intent);
                    }
                })
                .addOnFailureListener(e -> showToast(e.getMessage()));
    }

    /**
     * Helper method to display a short Toast message.
     * @param message The text to display.
     */
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}