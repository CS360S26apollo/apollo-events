package com.example.peertutoring.models;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

/**
 * Represents a tutoring session request between a student and a tutor.
 *
 * <p>A {@code SessionRequest} moves through a well-defined status lifecycle:
 * {@link #STATUS_REQUESTED} → {@link #STATUS_BOOKED} → {@link #STATUS_COMPLETED}
 * (or {@link #STATUS_CANCELLED} / {@link #STATUS_NO_SHOW} / {@link #STATUS_EXPIRED}).
 * Escrow fields ({@code paymentStatus}, {@code escrowBalance}) implement the token
 * hold-and-release flow defined in US 24. Documents are stored in Firestore under
 * {@code sessionRequests/{requestId}}.</p>
 */
public class SessionRequest {

    /** Initial state — student has submitted a request but no tutor has accepted yet. */
    public static final String STATUS_REQUESTED = "requested";

    /** A tutor has accepted the request and a session time has been scheduled. */
    public static final String STATUS_BOOKED = "booked";

    /** The session took place and was marked complete by both parties. */
    public static final String STATUS_COMPLETED = "completed";

    /** The request or session was cancelled before completion. */
    public static final String STATUS_CANCELLED = "cancelled";

    /** The session was scheduled but one party did not attend. */
    public static final String STATUS_NO_SHOW = "no_show";

    /** The request was in {@link #STATUS_REQUESTED} state and timed out before being accepted. */
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

    /** Required no-arg constructor for Firestore deserialization. */
    public SessionRequest() {}

    /**
     * Creates a new session request from a student. Status is automatically set to
     * {@link #STATUS_REQUESTED}. Tutor fields and scheduling details are filled in
     * later when a tutor accepts the request.
     *
     * @param studentUid      Firebase UID of the requesting student
     * @param studentName     display name of the student
     * @param subject         subject area for the session
     * @param topic           specific topic within the subject
     * @param goals           learning goals the student wants to achieve
     * @param durationMinutes requested session length in minutes
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

    /**
     * Returns the Firestore document ID for this request.
     *
     * @return request ID, or {@code null} if not yet persisted
     */
    public String getRequestId()       { return requestId; }

    /**
     * Returns the Firebase UID of the student who created this request.
     *
     * @return student UID
     */
    public String getStudentUid()      { return studentUid; }

    /**
     * Returns the display name of the student who created this request.
     *
     * @return student display name
     */
    public String getStudentName()     { return studentName; }

    /**
     * Returns the Firebase UID of the tutor assigned to this request, or {@code null}
     * if no tutor has accepted yet.
     *
     * @return tutor UID, or {@code null}
     */
    public String getTutorUid()        { return tutorUid; }

    /**
     * Returns the display name of the assigned tutor, or {@code null} if unassigned.
     *
     * @return tutor display name, or {@code null}
     */
    public String getTutorName()       { return tutorName; }

    /**
     * Returns the subject area for this session.
     *
     * @return subject name
     */
    public String getSubject()         { return subject; }

    /**
     * Returns the specific topic within the subject the student wants to cover.
     *
     * @return topic description
     */
    public String getTopic()           { return topic; }

    /**
     * Returns the learning goals the student stated for this session.
     *
     * @return goals text
     */
    public String getGoals()           { return goals; }

    /**
     * Returns the requested session duration in minutes.
     *
     * @return duration in minutes
     */
    public int    getDurationMinutes() { return durationMinutes; }

    /**
     * Returns the current lifecycle status of this request.
     * One of the {@code STATUS_*} constants defined on this class.
     *
     * @return status string
     */
    public String getStatus()          { return status; }

    /**
     * Returns the number of tokens associated with this session (the agreed cost).
     *
     * @return token amount
     */
    public int    getTokens()          { return tokens; }

    /**
     * Returns the scheduled date and time for the session, or {@code null} if not yet booked.
     *
     * @return scheduled date, or {@code null}
     */
    public Date   getScheduledDate()   { return scheduledDate; }

    /**
     * Returns the server-assigned creation timestamp, or {@code null} before first write.
     *
     * @return Firestore server timestamp, or {@code null}
     */
    public Date   getCreatedAt()       { return createdAt; }

    /**
     * Returns the escrow payment status (US 24): {@code "HELD"}, {@code "RELEASED"},
     * or {@code "REFUNDED"}.
     *
     * @return payment status string, or {@code null} if escrow not yet initialised
     */
    public String getPaymentStatus()  { return paymentStatus; }

    /**
     * Returns the number of tokens currently held in escrow (US 24).
     *
     * @return escrowed token amount
     */
    public int    getEscrowBalance()  { return escrowBalance; }

    /**
     * Sets the Firestore document ID for this request.
     *
     * @param requestId document ID
     */
    public void setRequestId(String requestId)           { this.requestId = requestId; }

    /**
     * Sets the Firebase UID of the student who created this request.
     *
     * @param studentUid student's UID
     */
    public void setStudentUid(String studentUid)         { this.studentUid = studentUid; }

    /**
     * Sets the display name of the requesting student.
     *
     * @param studentName student's display name
     */
    public void setStudentName(String studentName)       { this.studentName = studentName; }

    /**
     * Sets the Firebase UID of the tutor assigned to this request.
     *
     * @param tutorUid tutor's UID
     */
    public void setTutorUid(String tutorUid)             { this.tutorUid = tutorUid; }

    /**
     * Sets the display name of the assigned tutor.
     *
     * @param tutorName tutor's display name
     */
    public void setTutorName(String tutorName)           { this.tutorName = tutorName; }

    /**
     * Sets the subject area for this session.
     *
     * @param subject subject name
     */
    public void setSubject(String subject)               { this.subject = subject; }

    /**
     * Sets the specific topic within the subject.
     *
     * @param topic topic description
     */
    public void setTopic(String topic)                   { this.topic = topic; }

    /**
     * Sets the student's learning goals for this session.
     *
     * @param goals goals text
     */
    public void setGoals(String goals)                   { this.goals = goals; }

    /**
     * Sets the requested session duration.
     *
     * @param durationMinutes duration in minutes
     */
    public void setDurationMinutes(int durationMinutes)  { this.durationMinutes = durationMinutes; }

    /**
     * Sets the lifecycle status of this request.
     * Use one of the {@code STATUS_*} constants defined on this class.
     *
     * @param status new status string
     */
    public void setStatus(String status)                 { this.status = status; }

    /**
     * Sets the token cost for this session.
     *
     * @param tokens token amount
     */
    public void setTokens(int tokens)                    { this.tokens = tokens; }

    /**
     * Sets the scheduled date and time for the session.
     *
     * @param scheduledDate agreed session date/time
     */
    public void setScheduledDate(Date scheduledDate)     { this.scheduledDate = scheduledDate; }

    /**
     * Sets the creation timestamp (normally populated by Firestore's {@code @ServerTimestamp}).
     *
     * @param createdAt creation date/time
     */
    public void setCreatedAt(Date createdAt)             { this.createdAt = createdAt; }

    /**
     * Sets the escrow payment status (US 24).
     *
     * @param paymentStatus {@code "HELD"}, {@code "RELEASED"}, or {@code "REFUNDED"}
     */
    public void setPaymentStatus(String paymentStatus)   { this.paymentStatus = paymentStatus; }

    /**
     * Sets the number of tokens currently held in escrow (US 24).
     *
     * @param escrowBalance token amount to hold
     */
    public void setEscrowBalance(int escrowBalance)      { this.escrowBalance = escrowBalance; }

    /**
     * Returns {@code true} if this request is still in {@link #STATUS_REQUESTED} state
     * and older than {@code windowMs} milliseconds. Used by ExpirationUtils to identify
     * stale requests client-side.
     *
     * @param windowMs expiry window in milliseconds
     * @return {@code true} if the request has exceeded the expiry window
     */
    public boolean isExpiredRequest(long windowMs) {
        if (!STATUS_REQUESTED.equals(status) || createdAt == null) return false;
        return (System.currentTimeMillis() - createdAt.getTime()) > windowMs;
    }

    /**
     * Returns {@code true} only when the status is {@link #STATUS_BOOKED} and the
     * scheduled session time is still in the future.
     *
     * @return {@code true} if the session is booked and upcoming
     */
    public boolean isUpcoming() {
        return STATUS_BOOKED.equals(status) &&
               scheduledDate != null &&
               scheduledDate.after(new Date());
    }
}