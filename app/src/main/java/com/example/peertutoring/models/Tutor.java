package com.example.peertutoring.models;

import java.util.List;

/**
 * Represents a Tutor user in the Peer Tutoring system.
 * This class extends {@link User} to include tutor-specific attributes like bio, academic level, and hourly rate.
 * Role: Model class used during the tutor onboarding flow and for displaying tutor profiles in the marketplace.
 */
public class Tutor extends User {

    private String firstName;
    private String lastName;
    private String fullName;
    private String bio;
    private String level; // e.g., Junior, Senior, Graduate
    private int rate; // Token cost per session
    private List<String> subjects;

    /**
     * Default constructor required for Firestore serialization.
     */
    public Tutor() {
        super();
    }

    /**
     * Constructs a new Tutor profile.
     * @param uid Unique identifier from Firebase Auth
     * @param email Tutor's email address
     * @param firstName Tutor's first name
     * @param lastName Tutor's last name
     * @param bio Short biography or introduction of the tutor
     * @param level Academic level (e.g., Junior, Senior, Graduate)
     * @param rate Token cost per session
     * @param subjects List of subjects the tutor is proficient in
     */
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

    /** @return Tutor's first name. */
    public String getFirstName() { return firstName; }

    /** @return Tutor's last name. */
    public String getLastName() { return lastName; }

    /** @return Tutor's full name. */
    public String getFullName() { return fullName; }

    /** @return Tutor's professional bio. */
    public String getBio() { return bio; }

    /** @return Academic level of the tutor. */
    public String getLevel() { return level; }

    /** @return Token rate per tutoring session. */
    public int getRate() { return rate; }

    /** @return List of subjects this tutor can teach. */
    public List<String> getSubjects() { return subjects; }

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

    /** @param bio The biography to set. */
    public void setBio(String bio) { this.bio = bio; }

    /** @param level The academic level to set. */
    public void setLevel(String level) { this.level = level; }

    /** @param rate The session rate to set. */
    public void setRate(int rate) { this.rate = rate; }

    /** @param subjects The subjects list to set. */
    public void setSubjects(List<String> subjects) { this.subjects = subjects; }

    /**
     * Internal helper to synchronize the fullName field whenever names are modified.
     */
    private void updateFullName() {
        this.fullName = (firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName);
        this.fullName = this.fullName.trim();
    }
}