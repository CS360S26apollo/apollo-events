package com.example.peertutoring.models;

import java.util.List;
import java.util.Map;

public class Student extends User {

    private String firstName;
    private String lastName;
    private String fullName;
    private String institution;
    private List<String> subjects;
    private List<String> goals;
    private Map<String, List<Integer>> preferredHours; // Preferred times for tutoring

    public Student() {
        super();
    }

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

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getFullName() {
        return fullName;
    }

    public String getInstitution() {
        return institution;
    }

    public List<String> getSubjects() {
        return subjects;
    }

    public List<String> getGoals() {
        return goals;
    }

    public Map<String, List<Integer>> getPreferredHours() {
        return preferredHours;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
        updateFullName();
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
        updateFullName();
    }

    public void setInstitution(String institution) {
        this.institution = institution;
    }

    public void setSubjects(List<String> subjects) {
        this.subjects = subjects;
    }

    public void setGoals(List<String> goals) {
        this.goals = goals;
    }

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
