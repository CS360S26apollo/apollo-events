package com.example.peertutoring.models;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;
import java.util.List;

/**
 * Model for a student's tutoring session request.
 * Stored in Firestore under "sessionRequests" collection.
 * US8: Student requests a session with topic, goals, and duration.
 */
public class SessionRequest {

    private String requestId;
    private String studentUid;
    private String studentName;
    private String subject;
    private String topic;
    private String goals;
    private int durationMinutes;
    private String status; // "waiting", "has_offers", "accepted", "completed"
    private int offerCount;
    private int bestRate; // lowest token rate offered so far
    @ServerTimestamp
    private Date createdAt;

    /** Required empty constructor for Firestore. */
    public SessionRequest() {}

    public SessionRequest(String studentUid, String studentName, String subject,
                          String topic, String goals, int durationMinutes) {
        this.studentUid      = studentUid;
        this.studentName     = studentName;
        this.subject         = subject;
        this.topic           = topic;
        this.goals           = goals;
        this.durationMinutes = durationMinutes;
        this.status          = "waiting";
        this.offerCount      = 0;
        this.bestRate        = 0;
    }

    // ── Getters ──────────────────────────────────────────────

    public String getRequestId()       { return requestId; }
    public String getStudentUid()      { return studentUid; }
    public String getStudentName()     { return studentName; }
    public String getSubject()         { return subject; }
    public String getTopic()           { return topic; }
    public String getGoals()           { return goals; }
    public int    getDurationMinutes() { return durationMinutes; }
    public String getStatus()          { return status; }
    public int    getOfferCount()      { return offerCount; }
    public int    getBestRate()        { return bestRate; }
    public Date   getCreatedAt()       { return createdAt; }

    // ── Setters ──────────────────────────────────────────────

    public void setRequestId(String requestId)           { this.requestId = requestId; }
    public void setStudentUid(String studentUid)         { this.studentUid = studentUid; }
    public void setStudentName(String studentName)       { this.studentName = studentName; }
    public void setSubject(String subject)               { this.subject = subject; }
    public void setTopic(String topic)                   { this.topic = topic; }
    public void setGoals(String goals)                   { this.goals = goals; }
    public void setDurationMinutes(int durationMinutes)  { this.durationMinutes = durationMinutes; }
    public void setStatus(String status)                 { this.status = status; }
    public void setOfferCount(int offerCount)            { this.offerCount = offerCount; }
    public void setBestRate(int bestRate)                { this.bestRate = bestRate; }
    public void setCreatedAt(Date createdAt)             { this.createdAt = createdAt; }
}