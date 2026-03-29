package com.example.peertutoring.models;

import java.util.List;

/**
 * Tutor profile model created during onboarding.
 */
public class Tutor extends User {

    private String firstName;
    private String lastName;
    private String fullName;
    private String bio;
    private String level; // e.g., Junior, Senior, Graduate
    private int rate; // Token cost per session
    private List<String> subjects;

    public Tutor() {
        super();
    }

    public Tutor(String uid, String email, String firstName, String lastName,
                 String bio, String level, int rate, List<String> subjects) {
        super(uid, email, "tutor", true);
        this.firstName = firstName;
        this.lastName = lastName;
        this.fullName = firstName + " " + lastName;
        this.bio = bio;
        this.level = level;
        this.rate = rate;
        this.subjects = subjects;
    }

    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getFullName() { return fullName; }
    public String getBio() { return bio; }
    public String getLevel() { return level; }
    public int getRate() { return rate; }
    public List<String> getSubjects() { return subjects; }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
        updateFullName();
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
        updateFullName();
    }

    public void setBio(String bio) { this.bio = bio; }
    public void setLevel(String level) { this.level = level; }
    public void setRate(int rate) { this.rate = rate; }
    public void setSubjects(List<String> subjects) { this.subjects = subjects; }

    private void updateFullName() {
        this.fullName = (firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName);
        this.fullName = this.fullName.trim();
    }
}
