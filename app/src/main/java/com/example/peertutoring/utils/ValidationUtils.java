package com.example.peertutoring.utils;

import android.util.Patterns;

import java.util.List;

/**
 * Utility class providing static methods for data validation.
 * Used across the application to ensure consistency in input validation
 * for emails, passwords, and form fields.
 * 
 * Role: Utility/Helper for data integrity.
 */
public class ValidationUtils {

    /**
     * Validates if a string is a correctly formatted email address.
     * @param email The string to validate.
     * @return True if valid, false otherwise.
     */
    public static boolean isValidEmail(String email) {
        return email != null && !email.trim().isEmpty()
                && Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches();
    }

    /**
     * Validates if a password meets the minimum length requirement (6 characters).
     * @param password The string to validate.
     * @return True if length >= 6, false otherwise.
     */
    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= 6;
    }

    /**
     * Checks if two password strings are identical.
     * @param password The first password.
     * @param confirmPassword The second password to compare against.
     * @return True if they match exactly.
     */
    public static boolean passwordsMatch(String password, String confirmPassword) {
        return password != null && password.equals(confirmPassword);
    }

    /**
     * Checks if a string is not null and contains non-whitespace characters.
     * @param value The string to check.
     * @return True if non-empty.
     */
    public static boolean isNonEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /**
     * Checks if a list is not null and contains at least one item.
     * @param items The list to check.
     * @return True if the list has elements.
     */
    public static boolean hasItems(List<String> items) {
        return items != null && !items.isEmpty();
    }
}