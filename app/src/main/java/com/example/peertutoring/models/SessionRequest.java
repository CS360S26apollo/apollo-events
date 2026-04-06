package com.example.peertutoring.models;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

/**
 * Model for a tutoring session request and its lifecycle.
 * US 16: Track session status (requested -> booked -> completed/cancelled/no-show)
 */
public class SessionRequest {

    // Status Constants
    public static final String STATUS_REQUESTED = "requested";
    public static final String STATUS_BOOKED = "booked";
    public static final String STATUS_COMPLETED = "completed";
    public static final String STATUS_CANCELLED = "cancelled";
    public static final String STATUS_NO_SHOW = "no_show";

    private String requestId;
    private String studentUid;
    private String studentName;
    private String tutorUid;    // Added for US 16
    private String tutorName;   // Added for US 16
    private String subject;
    private String topic;
    private String goals;
    private int durationMinutes;
    private String status; // requested, booked, completed, cancelled, no_show
    private int tokens;    // Price for the session
    
    private Date scheduledDate; // The agreed date/time for the session
    
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
        this.status          = STATUS_REQUESTED;
    }

    // ── Getters ──────────────────────────────────────────────

    public String getRequestId()       { return requestId; }
    public String getStudentUid()      { return studentUid; }
    public String getStudentName()     { return studentName; }
    public String getTutorUid()        { return tutorUid; }
    public String getTutorName()       { return tutorName; }
    public String getSubject()         { return subject; }
    public String getTopic()           { return topic; }
    public String getGoals()           { return goals; }
    public int    getDurationMinutes() { return durationMinutes; }
    public String getStatus()          { return status; }
    public int    getTokens()          { return tokens; }
    public Date   getScheduledDate()   { return scheduledDate; }
    public Date   getCreatedAt()       { return createdAt; }

    // ── Setters ──────────────────────────────────────────────

    public void setRequestId(String requestId)           { this.requestId = requestId; }
    public void setStudentUid(String studentUid)         { this.studentUid = studentUid; }
    public void setStudentName(String studentName)       { this.studentName = studentName; }
    public void setTutorUid(String tutorUid)             { this.tutorUid = tutorUid; }
    public void setTutorName(String tutorName)           { this.tutorName = tutorName; }
    public void setSubject(String subject)               { this.subject = subject; }
    public void setTopic(String topic)                   { this.topic = topic; }
    public void setGoals(String goals)                   { this.goals = goals; }
    public void setDurationMinutes(int durationMinutes)  { this.durationMinutes = durationMinutes; }
    public void setStatus(String status)                 { this.status = status; }
    public void setTokens(int tokens)                    { this.tokens = tokens; }
    public void setScheduledDate(Date scheduledDate)     { this.scheduledDate = scheduledDate; }
    public void setCreatedAt(Date createdAt)             { this.createdAt = createdAt; }

    /**
     * Helper to check if a session is upcoming.
     * US 16 requirement for status tracking.
     */
    public boolean isUpcoming() {
        return STATUS_BOOKED.equals(status) && 
               scheduledDate != null && 
               scheduledDate.after(new Date());
    }
}