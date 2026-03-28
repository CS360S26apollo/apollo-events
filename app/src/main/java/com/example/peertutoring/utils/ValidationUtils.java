package com.example.peertutoring.utils;

import android.util.Patterns;

import java.util.List;

/**
 * Validation helper methods for onboarding.
 */
public class ValidationUtils {

    public static boolean isValidEmail(String email) {
        return email != null && !email.trim().isEmpty()
                && Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches();
    }

    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= 6;
    }

    public static boolean passwordsMatch(String password, String confirmPassword) {
        return password != null && password.equals(confirmPassword);
    }

    public static boolean isNonEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }

    public static boolean hasItems(List<String> items) {
        return items != null && !items.isEmpty();
    }
}
