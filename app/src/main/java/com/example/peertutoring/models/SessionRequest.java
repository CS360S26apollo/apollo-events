package com.example.peertutoring.models;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

/**
 * Model representing a tutoring session request and its lifecycle.
 * This class encapsulates the data for User Story 16 (Track session status) and
 * User Story 8 (Request a Session).
 * 
 * Design Pattern: Model (Data Transfer Object) for Firestore.
 * Role: Manages state transitions for tutoring sessions (requested -> booked -> completed/cancelled).
 */
public class SessionRequest {

    /** Status for when a student has first sent a request. */
    public static final String STATUS_REQUESTED = "requested";
    /** Status for when a tutor has accepted the request and scheduled a time. */
    public static final String STATUS_BOOKED = "booked";
    /** Status for when the session has been successfully finished. */
    public static final String STATUS_COMPLETED = "completed";
    /** Status for when a session is cancelled by either party. */
    public static final String STATUS_CANCELLED = "cancelled";
    /** Status for when a student or tutor fails to appear. */
    public static final String STATUS_NO_SHOW = "no_show";

    private String requestId;
    private String studentUid;
    private String studentName;
    private String tutorUid;
    private String tutorName;
    private String subject;
    private String topic;
    private String goals;
    private int durationMinutes;
    private String status; 
    private int tokens;
    private Date scheduledDate;
    
    @ServerTimestamp
    private Date createdAt;

    /** Required empty constructor for Firestore serialization. */
    public SessionRequest() {}

    /**
     * Constructs a new session request with initial student details.
     * @param studentUid ID of the requesting student
     * @param studentName Name of the student
     * @param subject Subject for the session
     * @param topic Specific topic to cover
     * @param goals Academic goals for the session
     * @param durationMinutes Requested length in minutes
     */
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

    /** @return Unique ID of the request. */
    public String getRequestId()       { return requestId; }
    /** @return Student's unique identifier. */
    public String getStudentUid()      { return studentUid; }
    /** @return Student's name for display. */
    public String getStudentName()     { return studentName; }
    /** @return Tutor's unique identifier. */
    public String getTutorUid()        { return tutorUid; }
    /** @return Tutor's name for display. */
    public String getTutorName()       { return tutorName; }
    /** @return The subject of the session. */
    public String getSubject()         { return subject; }
    /** @return Specific topic within the subject. */
    public String getTopic()           { return topic; }
    /** @return Learner's goals for this session. */
    public String getGoals()           { return goals; }
    /** @return Length of session in minutes. */
    public int    getDurationMinutes() { return durationMinutes; }
    /** @return Current status of the lifecycle. */
    public String getStatus()          { return status; }
    /** @return Token cost associated with this session. */
    public int    getTokens()          { return tokens; }
    /** @return Date and time the session is scheduled for. */
    public Date   getScheduledDate()   { return scheduledDate; }
    /** @return Timestamp when request was created. */
    public Date   getCreatedAt()       { return createdAt; }

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
     * Determines if the session is currently booked for a future time.
     * @return True if status is booked and date is in the future.
     */
    public boolean isUpcoming() {
        return STATUS_BOOKED.equals(status) && 
               scheduledDate != null && 
               scheduledDate.after(new Date());
    }
}