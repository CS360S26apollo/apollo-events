package com.example.peertutoring.models;

public class User {
    protected String uid;
    protected String email;
    protected String role;
    protected boolean profileVisible;
    protected boolean verified;
    protected String idDocumentUrl;
    protected int tokens; // Starting balance awarded on signup

    public User() {}

    public User(String uid, String email, String role, boolean profileVisible) {
        this.uid            = uid;
        this.email          = email;
        this.role           = role;
        this.profileVisible = profileVisible;
        this.verified       = false;
        this.idDocumentUrl  = null;
        this.tokens         = 100; // every new user gets 100 tokens on signup
    }

    public String getUid()             { return uid; }
    public String getEmail()           { return email; }
    public String getRole()            { return role; }
    public boolean isProfileVisible()  { return profileVisible; }
    public boolean isVerified()        { return verified; }
    public String getIdDocumentUrl()   { return idDocumentUrl; }
    public int getTokens()             { return tokens; }

    public void setUid(String uid)                       { this.uid = uid; }
    public void setEmail(String email)                   { this.email = email; }
    public void setRole(String role)                     { this.role = role; }
    public void setProfileVisible(boolean profileVisible){ this.profileVisible = profileVisible; }
    public void setVerified(boolean verified)            { this.verified = verified; }
    public void setIdDocumentUrl(String idDocumentUrl)   { this.idDocumentUrl = idDocumentUrl; }
    public void setTokens(int tokens)                    { this.tokens = tokens; }
}