package com.example.peertutoring.models;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public Tutor() {
        super();
        this.availability = new HashMap<>();
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
        this.rating = 0.0;
        this.responsivenessScore = 1.0;
        this.availability = new HashMap<>();
    }

    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getFullName() { return fullName; }
    public String getBio() { return bio; }
    public String getLevel() { return level; }
    public int getRate() { return rate; }
    public List<String> getSubjects() { return subjects; }
    public double getRating() { return rating; }
    public double getResponsivenessScore() { return responsivenessScore; }
    public Map<String, List<Integer>> getAvailability() { return availability; }

    public void setFirstName(String firstName) { this.firstName = firstName; updateFullName(); }
    public void setLastName(String lastName) { this.lastName = lastName; updateFullName(); }
    public void setBio(String bio) { this.bio = bio; }
    public void setLevel(String level) { this.level = level; }
    public void setRate(int rate) { this.rate = rate; }
    public void setSubjects(List<String> subjects) { this.subjects = subjects; }
    public void setRating(double rating) { this.rating = rating; }
    public void setResponsivenessScore(double responsivenessScore) { this.responsivenessScore = responsivenessScore; }
    public void setAvailability(Map<String, List<Integer>> availability) { this.availability = availability; }

    private void updateFullName() {
        this.fullName = (firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName);
        this.fullName = this.fullName.trim();
    }
}
