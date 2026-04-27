package com.example.peertutoring.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.peertutoring.R;
import com.example.peertutoring.utils.SoundManager;
import com.example.peertutoring.utils.ValidationUtils;

/**
 * LoginActivity handles the user authentication for existing accounts.
 */
public class LoginActivity extends AppCompatActivity {

    private EditText emailEditText;
    private EditText passwordEditText;
    private Button signInButton;
    private TextView signUpTextView;
    private TextView forgotPasswordTextView;

    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();

        emailEditText = findViewById(R.id.editTextEmail);
        passwordEditText = findViewById(R.id.editTextPassword);
        signInButton = findViewById(R.id.buttonSignIn);
        signUpTextView = findViewById(R.id.textViewSignUp);
        forgotPasswordTextView = findViewById(R.id.textViewForgotPassword);

        signInButton.setOnClickListener(v -> { SoundManager.playClick(this); loginUser(); });
        signUpTextView.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        });

        forgotPasswordTextView.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            if (TextUtils.isEmpty(email)) {
                Toast.makeText(this, "Enter your email to reset password", Toast.LENGTH_SHORT).show();
                return;
            }
            auth.sendPasswordResetEmail(email)
                    .addOnSuccessListener(unused -> Toast.makeText(this, "Reset email sent!", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });
    }

    private void routeByRole(String uid) {
        FirebaseFirestore.getInstance().collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    Class<?> target = "tutor".equals(doc.getString("role"))
                            ? TutorHomeActivity.class : HomeActivity.class;
                    startActivity(new Intent(this, target));
                    finish();
                })
                .addOnFailureListener(e -> {
                    startActivity(new Intent(this, HomeActivity.class));
                    finish();
                });
    }

    private void loginUser() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (TextUtils.isEmpty(email) || !ValidationUtils.isValidEmail(email)) {
            Toast.makeText(this, "Please enter a valid email.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please enter your password.", Toast.LENGTH_SHORT).show();
            return;
        }

        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    SoundManager.playSuccess(LoginActivity.this);
                    if (authResult.getUser() != null) routeByRole(authResult.getUser().getUid());
                    else { startActivity(new Intent(this, HomeActivity.class)); finish(); }
                })
                .addOnFailureListener(e -> { SoundManager.playError(LoginActivity.this); Toast.makeText(LoginActivity.this, "Login failed: " + e.getMessage(), Toast.LENGTH_LONG).show(); });
    }
}