package com.example.peertutoring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
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
    public void testStudentPreferences() {
        List<String> subjects = Arrays.asList("Calculus", "Linear Algebra", "Data Structures");
        List<String> goals = Arrays.asList("Improve grades", "Prepare for finals");
        
        Student student = new Student("S1", "student@lums.edu", "Ali", "Iqbal", 
                                     "LUMS", subjects, goals);
        
        assertEquals(3, student.getSubjects().size());
        assertTrue(student.getSubjects().contains("Calculus"));
        assertEquals(2, student.getGoals().size());
        assertEquals("LUMS", student.getInstitution());
    }

    @Test
    public void testTutorInitialization() {
        List<String> subjects = Arrays.asList("Math", "Physics");
        Tutor tutor = new Tutor("T1", "tutor@lums.edu", "Sarah", "Johnson", 
                                "Experienced tutor with focus on AP Physics", "Expert", 45, subjects);
        
        assertEquals("Sarah Johnson", tutor.getFullName());
        assertEquals(45, tutor.getRate());
        assertEquals(2, tutor.getSubjects().size());
        assertEquals("Expert", tutor.getLevel());
        assertEquals("Experienced tutor with focus on AP Physics", tutor.getBio());
    }

    @Test
    public void testTutorSubjectMatching() {
        List<String> tutorSubjects = Arrays.asList("Python", "Java", "C++");
        Tutor tutor = new Tutor();
        tutor.setSubjects(tutorSubjects);

        List<String> studentInterests = Arrays.asList("Java", "Kotlin");
        
        boolean hasMatch = false;
        for (String interest : studentInterests) {
            if (tutor.getSubjects().contains(interest)) {
                hasMatch = true;
                break;
            }
        }
        assertTrue("Tutor should match student interests", hasMatch);
    }

    @Test
    public void testValidationUtils() {
        assertTrue(ValidationUtils.isNonEmpty("Valid"));
        assertFalse(ValidationUtils.isNonEmpty("  "));
        assertFalse(ValidationUtils.isNonEmpty(null));

        assertTrue(ValidationUtils.passwordsMatch("pass123", "pass123"));
        assertFalse(ValidationUtils.passwordsMatch("pass123", "different"));

        assertTrue(ValidationUtils.isValidPassword("123456"));
        assertFalse(ValidationUtils.isValidPassword("12345"));
        
        assertTrue(ValidationUtils.hasItems(Arrays.asList("Item")));
        assertFalse(ValidationUtils.hasItems(null));
        assertFalse(ValidationUtils.hasItems(Arrays.asList()));
    }
}