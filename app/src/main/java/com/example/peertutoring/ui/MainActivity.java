package com.example.peertutoring.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.example.peertutoring.R;
import com.example.peertutoring.utils.ValidationUtils;

/**
 * MainActivity handles signup and role selection.
 */
public class MainActivity extends AppCompatActivity {

    private EditText emailEditText;
    private EditText passwordEditText;
    private EditText confirmPasswordEditText;
    private RadioGroup roleRadioGroup;
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
        roleRadioGroup = findViewById(R.id.radioGroupRole);
        continueButton = findViewById(R.id.buttonContinue);

        continueButton.setOnClickListener(v -> registerUser());
    }

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

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
