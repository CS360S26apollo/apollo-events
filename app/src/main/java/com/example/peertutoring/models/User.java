package com.example.peertutoring.models;

/**
 * Base class representing any registered user in the Apollo tutoring platform.
 *
 * <p>Both {@link Student} and {@link Tutor} extend this class. Common identity and
 * account-state fields (uid, email, role, verification status, token balance) are
 * managed here and persisted to Firestore under the {@code users} collection.</p>
 */
public class User {
    protected String uid;
    protected String email;
    protected String role;
    protected boolean profileVisible;
    protected boolean verified;
    protected String idDocumentUrl;
    protected int tokens; // Starting balance awarded on signup

    /** Required no-arg constructor for Firestore deserialization. */
    public User() {}

    /**
     * Creates a new user with default settings: unverified, no ID document, and 100 tokens.
     *
     * @param uid            Firebase Authentication UID
     * @param email          user's email address
     * @param role           account type — {@code "student"} or {@code "tutor"}
     * @param profileVisible whether the profile is publicly discoverable
     */
    public User(String uid, String email, String role, boolean profileVisible) {
        this.uid            = uid;
        this.email          = email;
        this.role           = role;
        this.profileVisible = profileVisible;
        this.verified       = false;
        this.idDocumentUrl  = null;
        this.tokens         = 100; // every new user gets 100 tokens on signup
    }

    /**
     * Returns the Firebase Authentication UID that uniquely identifies this user.
     *
     * @return the user's UID
     */
    public String getUid()             { return uid; }

    /**
     * Returns the user's email address.
     *
     * @return email address
     */
    public String getEmail()           { return email; }

    /**
     * Returns the account role, either {@code "student"} or {@code "tutor"}.
     *
     * @return role string
     */
    public String getRole()            { return role; }

    /**
     * Returns whether this user's profile is visible to others in search results.
     *
     * @return {@code true} if the profile is publicly discoverable
     */
    public boolean isProfileVisible()  { return profileVisible; }

    /**
     * Returns whether this user's identity has been verified via an ID document.
     *
     * @return {@code true} if verified
     */
    public boolean isVerified()        { return verified; }

    /**
     * Returns the URL of the uploaded identity document, or {@code null} if none uploaded.
     *
     * @return Firebase Storage URL, or {@code null}
     */
    public String getIdDocumentUrl()   { return idDocumentUrl; }

    /**
     * Returns the user's current token balance.
     *
     * @return token balance (non-negative)
     */
    public int getTokens()             { return tokens; }

    /**
     * Sets the Firebase Authentication UID.
     *
     * @param uid the UID to assign
     */
    public void setUid(String uid)                       { this.uid = uid; }

    /**
     * Sets the user's email address.
     *
     * @param email the email to assign
     */
    public void setEmail(String email)                   { this.email = email; }

    /**
     * Sets the account role.
     *
     * @param role {@code "student"} or {@code "tutor"}
     */
    public void setRole(String role)                     { this.role = role; }

    /**
     * Sets whether this user's profile is publicly visible.
     *
     * @param profileVisible {@code true} to make the profile discoverable
     */
    public void setProfileVisible(boolean profileVisible){ this.profileVisible = profileVisible; }

    /**
     * Sets the identity-verification flag.
     *
     * @param verified {@code true} if the user's ID has been verified
     */
    public void setVerified(boolean verified)            { this.verified = verified; }

    /**
     * Sets the Firebase Storage URL of the user's uploaded identity document.
     *
     * @param idDocumentUrl storage URL, or {@code null} to clear
     */
    public void setIdDocumentUrl(String idDocumentUrl)   { this.idDocumentUrl = idDocumentUrl; }

    /**
     * Sets the user's token balance directly. Use the token-transaction service for
     * balance adjustments rather than calling this setter from UI code.
     *
     * @param tokens new token balance
     */
    public void setTokens(int tokens)                    { this.tokens = tokens; }
}
