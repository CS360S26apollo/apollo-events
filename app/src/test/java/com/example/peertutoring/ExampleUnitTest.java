package com.example.peertutoring;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.peertutoring.utils.ValidationUtils;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

/**
 * Unit tests for ValidationUtils — pure-Java validation helpers.
 * Note: isValidEmail() uses android.util.Patterns and is tested in instrumented tests only.
 *
 * TC-VU-01  isNonEmpty: null → false
 * TC-VU-02  isNonEmpty: empty string → false
 * TC-VU-03  isNonEmpty: whitespace-only string → false
 * TC-VU-04  isNonEmpty: valid string → true
 * TC-VU-05  isValidPassword: null → false
 * TC-VU-06  isValidPassword: 5-char string → false (below min)
 * TC-VU-07  isValidPassword: 6-char string → true (exactly at min)
 * TC-VU-08  isValidPassword: long string → true
 * TC-VU-09  passwordsMatch: identical strings → true
 * TC-VU-10  passwordsMatch: different strings → false
 * TC-VU-11  passwordsMatch: null first arg → false
 * TC-VU-12  passwordsMatch: null second arg → false
 * TC-VU-13  hasItems: null list → false
 * TC-VU-14  hasItems: empty list → false
 * TC-VU-15  hasItems: list with one item → true
 * TC-VU-16  hasItems: list with multiple items → true
 */
public class ExampleUnitTest {

    // ── TC-VU-01 ──────────────────────────────────────────────

    @Test
    public void testIsNonEmptyNullReturnsFalse() {
        assertFalse(ValidationUtils.isNonEmpty(null));
    }

    // ── TC-VU-02 ──────────────────────────────────────────────

    @Test
    public void testIsNonEmptyEmptyStringReturnsFalse() {
        assertFalse(ValidationUtils.isNonEmpty(""));
    }

    // ── TC-VU-03 ──────────────────────────────────────────────

    @Test
    public void testIsNonEmptyWhitespaceOnlyReturnsFalse() {
        assertFalse(ValidationUtils.isNonEmpty("   "));
        assertFalse(ValidationUtils.isNonEmpty("\t\n"));
    }

    // ── TC-VU-04 ──────────────────────────────────────────────

    @Test
    public void testIsNonEmptyValidStringReturnsTrue() {
        assertTrue(ValidationUtils.isNonEmpty("hello"));
        assertTrue(ValidationUtils.isNonEmpty(" a "));  // trimmed: "a"
    }

    // ── TC-VU-05 ──────────────────────────────────────────────

    @Test
    public void testIsValidPasswordNullReturnsFalse() {
        assertFalse(ValidationUtils.isValidPassword(null));
    }

    // ── TC-VU-06 ──────────────────────────────────────────────

    @Test
    public void testIsValidPasswordFiveCharsReturnsFalse() {
        assertFalse(ValidationUtils.isValidPassword("12345"));
    }

    // ── TC-VU-07 ──────────────────────────────────────────────

    @Test
    public void testIsValidPasswordSixCharsReturnsTrue() {
        assertTrue(ValidationUtils.isValidPassword("123456"));
    }

    // ── TC-VU-08 ──────────────────────────────────────────────

    @Test
    public void testIsValidPasswordLongStringReturnsTrue() {
        assertTrue(ValidationUtils.isValidPassword("SecurePassword!2025"));
    }

    // ── TC-VU-09 ──────────────────────────────────────────────

    @Test
    public void testPasswordsMatchIdenticalReturnsTrue() {
        assertTrue(ValidationUtils.passwordsMatch("pass123", "pass123"));
    }

    // ── TC-VU-10 ──────────────────────────────────────────────

    @Test
    public void testPasswordsMatchDifferentReturnsFalse() {
        assertFalse(ValidationUtils.passwordsMatch("pass123", "different"));
    }

    // ── TC-VU-11 ──────────────────────────────────────────────

    @Test
    public void testPasswordsMatchNullFirstReturnsFalse() {
        assertFalse(ValidationUtils.passwordsMatch(null, "pass123"));
    }

    // ── TC-VU-12 ──────────────────────────────────────────────

    @Test
    public void testPasswordsMatchNullSecondReturnsFalse() {
        // null.equals(...) would throw — implementation must guard against this
        assertFalse(ValidationUtils.passwordsMatch("pass123", null));
    }

    // ── TC-VU-13 ──────────────────────────────────────────────

    @Test
    public void testHasItemsNullReturnsFalse() {
        assertFalse(ValidationUtils.hasItems(null));
    }

    // ── TC-VU-14 ──────────────────────────────────────────────

    @Test
    public void testHasItemsEmptyListReturnsFalse() {
        assertFalse(ValidationUtils.hasItems(Collections.emptyList()));
    }

    // ── TC-VU-15 ──────────────────────────────────────────────

    @Test
    public void testHasItemsSingleItemReturnsTrue() {
        assertTrue(ValidationUtils.hasItems(Collections.singletonList("item")));
    }

    // ── TC-VU-16 ──────────────────────────────────────────────

    @Test
    public void testHasItemsMultipleItemsReturnsTrue() {
        assertTrue(ValidationUtils.hasItems(Arrays.asList("a", "b", "c")));
    }
}
