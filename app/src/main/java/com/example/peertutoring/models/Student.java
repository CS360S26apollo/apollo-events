package com.example.peertutoring.models;

import java.util.List;
import java.util.Map;

/**
 * Represents a Student user in the Peer Tutoring system.
 * This class extends {@link User} to include student-specific fields like institution, subjects, and goals.
 * Role: Model class used during the student onboarding flow and profile management.
 */
public class Student extends User {

    private String firstName;
    private String lastName;
    private String fullName;
    private String institution;
    private List<String> subjects;
    private List<String> goals;
    private Map<String, List<Integer>> preferredHours; // Preferred times for tutoring

    /**
     * Default constructor required for Firestore serialization.
     */
    public Student() {
        super();
    }

    /**
     * Constructs a new Student profile.
     * @param uid Unique identifier from Firebase Auth
     * @param email Student's email address
     * @param firstName Student's first name
     * @param lastName Student's last name
     * @param institution The academic institution the student belongs to
     * @param subjects List of subjects the student is interested in learning
     * @param goals List of academic goals or objectives
     */
    public Student(String uid, String email, String firstName, String lastName,
                   String institution, List<String> subjects, List<String> goals) {
        super(uid, email, "student", true);
        this.firstName = firstName;
        this.lastName = lastName;
        this.fullName = firstName + " " + lastName;
        this.institution = institution;
        this.subjects = subjects;
        this.goals = goals;
    }

    /** @return Student's first name. */
    public String getFirstName() {
        return firstName;
    }

    /** @return Student's last name. */
    public String getLastName() {
        return lastName;
    }

    /** @return Student's full name (concatenation of first and last name). */
    public String getFullName() {
        return fullName;
    }

    /** @return Name of the student's institution. */
    public String getInstitution() {
        return institution;
    }

    /** @return List of subjects the student is learning. */
    public List<String> getSubjects() {
        return subjects;
    }

    /** @return List of student's academic goals. */
    public List<String> getGoals() {
        return goals;
    }

    /** @return Preferred hours map. */
    public Map<String, List<Integer>> getPreferredHours() {
        return preferredHours;
    }

    /** @param firstName The first name to set. */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
        updateFullName();
    }

    /** @param lastName The last name to set. */
    public void setLastName(String lastName) {
        this.lastName = lastName;
        updateFullName();
    }

    /** @param institution The institution name to set. */
    public void setInstitution(String institution) {
        this.institution = institution;
    }

    /** @param subjects The list of subjects to set. */
    public void setSubjects(List<String> subjects) {
        this.subjects = subjects;
    }

    /** @param goals The list of goals to set. */
    public void setGoals(List<String> goals) {
        this.goals = goals;
    }

    /** @param preferredHours The preferred hours map to set. */
    public void setPreferredHours(Map<String, List<Integer>> preferredHours) {
        this.preferredHours = preferredHours;
    }

    /**
     * Internal helper to synchronize the fullName field whenever names are modified.
     */
    private void updateFullName() {
        this.fullName = (firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName);
        this.fullName = this.fullName.trim();
    }
}
