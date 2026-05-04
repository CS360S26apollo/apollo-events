package com.example.peertutoring.models;

import java.util.List;
import java.util.Map;

/**
 * Represents a student user in the Apollo tutoring platform.
 *
 * <p>Extends {@link User} with student-specific profile data: personal name,
 * institution, academic subjects of interest, learning goals, and preferred
 * tutoring hours. Stored in Firestore under {@code users/{uid}} with
 * {@code role = "student"}.</p>
 */
public class Student extends User {

    private String firstName;
    private String lastName;
    private String fullName;
    private String institution;
    private List<String> subjects;
    private List<String> goals;
    private Map<String, List<Integer>> preferredHours; // Preferred times for tutoring

    /** Required no-arg constructor for Firestore deserialization. */
    public Student() {
        super();
    }

    /**
     * Creates a fully initialised student profile.
     *
     * @param uid         Firebase Authentication UID
     * @param email       student's email address
     * @param firstName   given name
     * @param lastName    family name
     * @param institution name of the school or university the student attends
     * @param subjects    list of subjects the student wants help with
     * @param goals       list of learning goals or objectives
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

    /**
     * Returns the student's given (first) name.
     *
     * @return first name
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * Returns the student's family (last) name.
     *
     * @return last name
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * Returns the concatenated full name ({@code firstName + " " + lastName}).
     *
     * @return full display name
     */
    public String getFullName() {
        return fullName;
    }

    /**
     * Returns the name of the institution (school or university) the student attends.
     *
     * @return institution name
     */
    public String getInstitution() {
        return institution;
    }

    /**
     * Returns the list of subjects the student is seeking tutoring in.
     *
     * @return mutable list of subject names
     */
    public List<String> getSubjects() {
        return subjects;
    }

    /**
     * Returns the student's stated learning goals or objectives.
     *
     * @return mutable list of goal strings
     */
    public List<String> getGoals() {
        return goals;
    }

    /**
     * Returns the student's preferred tutoring hours keyed by day abbreviation
     * (e.g. {@code "mon"}) mapped to a list of preferred hour slots (0–23).
     *
     * @return day-to-hours map, or {@code null} if not set
     */
    public Map<String, List<Integer>> getPreferredHours() {
        return preferredHours;
    }

    /**
     * Sets the student's first name and refreshes {@code fullName}.
     *
     * @param firstName new given name
     */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
        updateFullName();
    }

    /**
     * Sets the student's last name and refreshes {@code fullName}.
     *
     * @param lastName new family name
     */
    public void setLastName(String lastName) {
        this.lastName = lastName;
        updateFullName();
    }

    /**
     * Sets the institution the student attends.
     *
     * @param institution institution name
     */
    public void setInstitution(String institution) {
        this.institution = institution;
    }

    /**
     * Replaces the student's list of subjects.
     *
     * @param subjects new list of subject names
     */
    public void setSubjects(List<String> subjects) {
        this.subjects = subjects;
    }

    /**
     * Replaces the student's list of learning goals.
     *
     * @param goals new list of goal strings
     */
    public void setGoals(List<String> goals) {
        this.goals = goals;
    }

    /**
     * Sets the student's preferred tutoring hours.
     *
     * @param preferredHours map of day abbreviation to list of preferred hour slots (0–23)
     */
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
