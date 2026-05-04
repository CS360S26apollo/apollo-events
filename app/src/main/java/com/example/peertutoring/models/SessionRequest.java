package com.example.peertutoring.models;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class SessionRequest {

    public static final String STATUS_REQUESTED = "requested";
    public static final String STATUS_BOOKED = "booked";
    public static final String STATUS_COMPLETED = "completed";
    public static final String STATUS_CANCELLED = "cancelled";
    public static final String STATUS_NO_SHOW = "no_show";
    public static final String STATUS_EXPIRED = "expired";

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

    /** US 24: escrow state — "HELD", "RELEASED", or "REFUNDED". */
    private String paymentStatus;
    /** US 24: tokens held from student until session is completed. */
    private int escrowBalance;

    @ServerTimestamp
    private Date createdAt;

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
    public void setPaymentStatus(String paymentStatus)   { this.paymentStatus = paymentStatus; }
    public void setEscrowBalance(int escrowBalance)      { this.escrowBalance = escrowBalance; }

    public String getPaymentStatus()  { return paymentStatus; }
    public int    getEscrowBalance()  { return escrowBalance; }

    /**
     * Returns true if this request is still "requested" and older than windowMs milliseconds.
     * Used by ExpirationUtils to identify stale requests client-side.
     */
    public boolean isExpiredRequest(long windowMs) {
        if (!STATUS_REQUESTED.equals(status) || createdAt == null) return false;
        return (System.currentTimeMillis() - createdAt.getTime()) > windowMs;
    }

    /** Returns true only when status is booked AND the scheduled time is still in the future. */
    public boolean isUpcoming() {
        return STATUS_BOOKED.equals(status) && 
               scheduledDate != null && 
               scheduledDate.after(new Date());
    }
}