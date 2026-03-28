package com.example.peertutoring.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.example.peertutoring.R;
import com.example.peertutoring.utils.ValidationUtils;

/**
 * MainActivity acts as the first signup screen for User Story 1.
 * It creates the Firebase Auth account before continuing to profile onboarding.
 */
public class MainActivity extends AppCompatActivity {

    private EditText emailEditText;
    private EditText passwordEditText;
    private EditText confirmPasswordEditText;
    private Button continueButton;

    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();

        emailEditText = findViewById(R.id.editTextEmail);
        passwordEditText = findViewById(R.id.editTextPassword);
        confirmPasswordEditText = findViewById(R.id.editTextConfirmPassword);
        continueButton = findViewById(R.id.buttonContinue);

        continueButton.setOnClickListener(v -> registerStudent());
    }

    private void registerStudent() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();

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

        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    if (authResult.getUser() != null) {
                        String uid = authResult.getUser().getUid();

                        Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
                        intent.putExtra("uid", uid);
                        intent.putExtra("email", email);
                        startActivity(intent);
                    }
                })
                .addOnFailureListener(e -> showToast(e.getMessage()));
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}