package com.example.peertutoring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.peertutoring.models.Student;
import com.example.peertutoring.models.Tutor;
import com.example.peertutoring.models.User;
import com.example.peertutoring.utils.ValidationUtils;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/**
 * Unit tests for Model classes (User, Student, Tutor) and Utility classes.
 * This covers the "Runnable tests for your model and control classes" requirement.
 */
public class ModelUnitTest {

    @Test
    public void testUserCreation() {
        User user = new User("123", "test@example.com", "student", true);
        assertEquals("123", user.getUid());
        assertEquals("test@example.com", user.getEmail());
        assertEquals("student", user.getRole());
        assertTrue(user.isProfileVisible());
        assertFalse(user.isVerified());
    }

    @Test
    public void testStudentFullNameUpdate() {
        Student student = new Student();
        student.setFirstName("John");
        student.setLastName("Doe");
        assertEquals("John Doe", student.getFullName());

        student.setLastName(null);
        assertEquals("John", student.getFullName());
    }

    @Test
    public void testTutorInitialization() {
        List<String> subjects = Arrays.asList("Math", "Physics");
        Tutor tutor = new Tutor("T1", "tutor@lums.edu", "Ali", "Iqbal", "Experienced tutor", "Senior", 50, subjects);
        
        assertEquals("Ali Iqbal", tutor.getFullName());
        assertEquals(50, tutor.getRate());
        assertEquals(2, tutor.getSubjects().size());
        assertEquals("Senior", tutor.getLevel());
    }

    @Test
    public void testValidationUtils() {
        // Email validation
        // Note: Patterns.EMAIL_ADDRESS is an Android framework class and might return null in local JVM tests
        // unless using a mock library or Robolectric. For basic unit tests, we test the logic we can.
        assertTrue(ValidationUtils.isNonEmpty("Valid"));
        assertFalse(ValidationUtils.isNonEmpty("  "));
        assertFalse(ValidationUtils.isNonEmpty(null));

        // Password matching
        assertTrue(ValidationUtils.passwordsMatch("pass123", "pass123"));
        assertFalse(ValidationUtils.passwordsMatch("pass123", "different"));

        // Password length
        assertTrue(ValidationUtils.isValidPassword("123456"));
        assertFalse(ValidationUtils.isValidPassword("12345"));
        
        // List check
        assertTrue(ValidationUtils.hasItems(Arrays.asList("Item")));
        assertFalse(ValidationUtils.hasItems(null));
    }
}