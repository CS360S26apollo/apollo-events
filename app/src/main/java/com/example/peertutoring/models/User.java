package com.example.peertutoring.models;

/**
 * Represents a generic user in the Peer Tutoring system.
 * This class serves as a base for specific user types like Students and Tutors.
 * Role: Model class for Firestore persistence.
 */
public class User {
    protected String uid;
    protected String email;
    protected String role;
    protected boolean profileVisible;
    protected boolean verified;
    protected String idDocumentUrl;

    /**
     * Default constructor required for Firestore serialization.
     */
    public User() {
        // Required empty constructor for Firestore
    }

    /**
     * Constructs a new User with essential identification and role information.
     * @param uid Unique identifier from Firebase Auth
     * @param email User's email address
     * @param role User's role (e.g., "student" or "tutor")
     * @param profileVisible Initial privacy setting for profile visibility
     */
    public User(String uid, String email, String role, boolean profileVisible) {
        this.uid = uid;
        this.email = email;
        this.role = role;
        this.profileVisible = profileVisible;
        this.verified = false;
        this.idDocumentUrl = null;
    }

    /** @return The unique identifier of the user. */
    public String getUid() {
        return uid;
    }

    /** @return The user's email address. */
    public String getEmail() {
        return email;
    }

    /** @return The user's role (student/tutor). */
    public String getRole() {
        return role;
    }

    /** @return True if the profile is public, false otherwise. */
    public boolean isProfileVisible() {
        return profileVisible;
    }

    /** @return True if the user has been verified by an admin. */
    public boolean isVerified() {
        return verified;
    }

    /** @return URL to the uploaded identification document. */
    public String getIdDocumentUrl() {
        return idDocumentUrl;
    }

    /** @param uid The unique identifier to set. */
    public void setUid(String uid) {
        this.uid = uid;
    }

    /** @param email The email address to set. */
    public void setEmail(String email) {
        this.email = email;
    }

    /** @param role The role to set. */
    public void setRole(String role) {
        this.role = role;
    }

    /** @param profileVisible The visibility status to set. */
    public void setProfileVisible(boolean profileVisible) {
        this.profileVisible = profileVisible;
    }

    /** @param verified The verification status to set. */
    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    /** @param idDocumentUrl The URL of the ID document to set. */
    public void setIdDocumentUrl(String idDocumentUrl) {
        this.idDocumentUrl = idDocumentUrl;
    }
}