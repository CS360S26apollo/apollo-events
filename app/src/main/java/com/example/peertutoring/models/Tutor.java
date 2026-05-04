package com.example.peertutoring.models;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a tutor user in the Apollo tutoring platform.
 *
 * <p>Extends {@link User} with tutor-specific profile data: personal name, biography,
 * academic level, token rate per session, subjects taught, and ranking signals used
 * by the tutor-search feature (US 06). Weekly availability is stored as a day-to-hours
 * map and is used when matching students to available tutors. Stored in Firestore under
 * {@code users/{uid}} with {@code role = "tutor"}.</p>
 */
public class Tutor extends User {

    private String firstName;
    private String lastName;
    private String fullName;
    private String bio;
    private String level; // e.g., Junior, Senior, Graduate
    private int rate; // Token cost per session
    private List<String> subjects;

    // US 06: Ranking Factors
    private double rating; // Average star rating (0.0 - 5.0)
    private double responsivenessScore; // (0.0 - 1.0) based on reply time
    private Map<String, List<Integer>> availability; // e.g., {"mon": [9, 10], "tue": [14]}

    /** Required no-arg constructor for Firestore deserialization. */
    public Tutor() {
        super();
        this.availability = new HashMap<>();
    }

    /**
     * Creates a fully initialised tutor profile with default ranking values
     * (rating 0.0, responsiveness 1.0, empty availability).
     *
     * @param uid      Firebase Authentication UID
     * @param email    tutor's email address
     * @param firstName given name
     * @param lastName  family name
     * @param bio      short biography displayed on the tutor's public profile
     * @param level    academic level, e.g. {@code "Junior"}, {@code "Senior"}, {@code "Graduate"}
     * @param rate     token cost charged per tutoring session
     * @param subjects list of subjects the tutor is able to teach
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
        this.rating = 0.0;
        this.responsivenessScore = 1.0;
        this.availability = new HashMap<>();
    }

    /**
     * Returns the tutor's given (first) name.
     *
     * @return first name
     */
    public String getFirstName() { return firstName; }

    /**
     * Returns the tutor's family (last) name.
     *
     * @return last name
     */
    public String getLastName() { return lastName; }

    /**
     * Returns the concatenated full name ({@code firstName + " " + lastName}).
     *
     * @return full display name
     */
    public String getFullName() { return fullName; }

    /**
     * Returns the tutor's short biography shown on their public profile.
     *
     * @return biography text
     */
    public String getBio() { return bio; }

    /**
     * Returns the tutor's academic level (e.g. {@code "Junior"}, {@code "Senior"},
     * {@code "Graduate"}).
     *
     * @return level string
     */
    public String getLevel() { return level; }

    /**
     * Returns the token cost the tutor charges per session.
     *
     * @return token rate (positive integer)
     */
    public int getRate() { return rate; }

    /**
     * Returns the list of subjects this tutor is able to teach.
     *
     * @return mutable list of subject names
     */
    public List<String> getSubjects() { return subjects; }

    /**
     * Returns the tutor's average star rating used for search ranking (US 06).
     *
     * @return rating in the range 0.0–5.0
     */
    public double getRating() { return rating; }

    /**
     * Returns the tutor's responsiveness score used for search ranking (US 06).
     * The score is derived from historical reply times; 1.0 is most responsive.
     *
     * @return responsiveness score in the range 0.0–1.0
     */
    public double getResponsivenessScore() { return responsivenessScore; }

    /**
     * Returns the tutor's weekly availability keyed by day abbreviation
     * (e.g. {@code "mon"}) mapped to a list of available hour slots (0–23).
     *
     * @return day-to-hours availability map (never {@code null})
     */
    public Map<String, List<Integer>> getAvailability() { return availability; }

    /**
     * Sets the tutor's first name and refreshes {@code fullName}.
     *
     * @param firstName new given name
     */
    public void setFirstName(String firstName) { this.firstName = firstName; updateFullName(); }

    /**
     * Sets the tutor's last name and refreshes {@code fullName}.
     *
     * @param lastName new family name
     */
    public void setLastName(String lastName) { this.lastName = lastName; updateFullName(); }

    /**
     * Sets the tutor's biography text.
     *
     * @param bio biography to display on the public profile
     */
    public void setBio(String bio) { this.bio = bio; }

    /**
     * Sets the tutor's academic level.
     *
     * @param level e.g. {@code "Junior"}, {@code "Senior"}, {@code "Graduate"}
     */
    public void setLevel(String level) { this.level = level; }

    /**
     * Sets the token rate the tutor charges per session.
     *
     * @param rate token cost (positive integer)
     */
    public void setRate(int rate) { this.rate = rate; }

    /**
     * Replaces the list of subjects this tutor teaches.
     *
     * @param subjects new list of subject names
     */
    public void setSubjects(List<String> subjects) { this.subjects = subjects; }

    /**
     * Sets the tutor's average star rating (US 06 ranking factor).
     *
     * @param rating value in the range 0.0–5.0
     */
    public void setRating(double rating) { this.rating = rating; }

    /**
     * Sets the tutor's responsiveness score (US 06 ranking factor).
     *
     * @param responsivenessScore value in the range 0.0–1.0
     */
    public void setResponsivenessScore(double responsivenessScore) { this.responsivenessScore = responsivenessScore; }

    /**
     * Sets the tutor's weekly availability schedule.
     *
     * @param availability map of day abbreviation to list of available hour slots (0–23)
     */
    public void setAvailability(Map<String, List<Integer>> availability) { this.availability = availability; }

    /**
     * Internal helper to synchronize the fullName field whenever names are modified.
     */
    private void updateFullName() {
        this.fullName = (firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName);
        this.fullName = this.fullName.trim();
    }
}