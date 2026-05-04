package com.example.peertutoring.models;

import com.google.firebase.firestore.FieldValue;
import java.util.Date;
import java.util.List;

/**
 * Model for a tutor-created crash course offered on the Apollo platform.
 *
 * <p>A crash course is a structured, multi-session programme created by a single tutor
 * and open for enrolment up to {@code maxStudents} capacity. It differs from a one-off
 * {@link SessionRequest} in that it spans multiple weeks, follows a defined topic list,
 * and has a single upfront token cost ({@code totalTokens}). Documents are stored in
 * Firestore under {@code courses/{courseId}}.</p>
 *
 * <p>Outstanding issues: the {@code FieldValue} import is currently unused and should
 * be removed once atomic increment logic is moved to the repository layer.</p>
 */
public class CrashCourse {
    private String courseId;
    private String tutorUid;
    private String tutorName;
    private String title;           // e.g. "A-Level Maths Crash Course"
    private String subject;
    private String description;
    private String level;           // "O-Level", "A-Level", "University", "Any"
    private int    durationDays;    // e.g. 30 (one month)
    private int    sessionsPerWeek; // e.g. 5 (daily)
    private int    sessionMinutes;  // per session, e.g. 60
    private int    totalTokens;     // total cost to enroll
    private int    maxStudents;     // e.g. 10
    private int    enrolledCount;
    private String thumbnailColor;  // gradient color for thumbnail
    private String thumbnailEmoji;  // subject emoji
    private List<String> topics;    // e.g. ["Integration", "Differentiation", ...]
    private Date   startDate;
    private Date   createdAt;
    private String status;          // "open", "full", "completed"
    private String sessionType;     // "online" or "takehome"
    private String zoomLink;        // Zoom link for online courses

    /** Required no-arg constructor for Firestore deserialization. */
    public CrashCourse() {}

    /**
     * Returns the Firestore document ID for this course.
     *
     * @return course ID, or {@code null} if not yet persisted
     */
    public String getCourseId()       { return courseId; }

    /**
     * Sets the Firestore document ID for this course.
     *
     * @param v course document ID
     */
    public void setCourseId(String v) { this.courseId = v; }

    /**
     * Returns the Firebase UID of the tutor who created this course.
     *
     * @return tutor UID
     */
    public String getTutorUid()       { return tutorUid; }

    /**
     * Sets the Firebase UID of the tutor who created this course.
     *
     * @param v tutor UID
     */
    public void setTutorUid(String v) { this.tutorUid = v; }

    /**
     * Returns the display name of the tutor who created this course.
     *
     * @return tutor display name
     */
    public String getTutorName()      { return tutorName; }

    /**
     * Sets the display name of the tutor who created this course.
     *
     * @param v tutor display name
     */
    public void setTutorName(String v){ this.tutorName = v; }

    /**
     * Returns the course title shown in listings (e.g. "A-Level Maths Crash Course").
     *
     * @return course title
     */
    public String getTitle()          { return title; }

    /**
     * Sets the course title.
     *
     * @param v course title
     */
    public void setTitle(String v)    { this.title = v; }

    /**
     * Returns the primary subject area covered by this course.
     *
     * @return subject name
     */
    public String getSubject()        { return subject; }

    /**
     * Sets the primary subject area covered by this course.
     *
     * @param v subject name
     */
    public void setSubject(String v)  { this.subject = v; }

    /**
     * Returns the full description of the course content shown on the detail screen.
     *
     * @return description text
     */
    public String getDescription()    { return description; }

    /**
     * Sets the full description of the course content.
     *
     * @param v description text
     */
    public void setDescription(String v){ this.description = v; }

    /**
     * Returns the target academic level (e.g. {@code "O-Level"}, {@code "A-Level"},
     * {@code "University"}, {@code "Any"}).
     *
     * @return level string
     */
    public String getLevel()          { return level; }

    /**
     * Sets the target academic level.
     *
     * @param v level string
     */
    public void setLevel(String v)    { this.level = v; }

    /**
     * Returns the total duration of the course in days.
     *
     * @return duration in days (e.g. 30 for one month)
     */
    public int getDurationDays()      { return durationDays; }

    /**
     * Sets the total duration of the course in days.
     *
     * @param v duration in days
     */
    public void setDurationDays(int v){ this.durationDays = v; }

    /**
     * Returns the number of sessions held per week.
     *
     * @return sessions per week
     */
    public int getSessionsPerWeek()   { return sessionsPerWeek; }

    /**
     * Sets the number of sessions held per week.
     *
     * @param v sessions per week
     */
    public void setSessionsPerWeek(int v){ this.sessionsPerWeek = v; }

    /**
     * Returns the length of each individual session in minutes.
     *
     * @return session length in minutes
     */
    public int getSessionMinutes()    { return sessionMinutes; }

    /**
     * Sets the length of each individual session in minutes.
     *
     * @param v session length in minutes
     */
    public void setSessionMinutes(int v){ this.sessionMinutes = v; }

    /**
     * Returns the total token cost a student pays to enrol in this course.
     *
     * @return total token cost
     */
    public int getTotalTokens()       { return totalTokens; }

    /**
     * Sets the total token cost for enrolment.
     *
     * @param v total token cost
     */
    public void setTotalTokens(int v) { this.totalTokens = v; }

    /**
     * Returns the maximum number of students who can enrol in this course.
     *
     * @return maximum student capacity
     */
    public int getMaxStudents()       { return maxStudents; }

    /**
     * Sets the maximum number of students who can enrol.
     *
     * @param v maximum student capacity
     */
    public void setMaxStudents(int v) { this.maxStudents = v; }

    /**
     * Returns the number of students currently enrolled.
     *
     * @return current enrolment count
     */
    public int getEnrolledCount()     { return enrolledCount; }

    /**
     * Sets the current enrolment count. Prefer using a Firestore atomic increment
     * via the repository layer rather than calling this setter directly.
     *
     * @param v enrolment count
     */
    public void setEnrolledCount(int v){ this.enrolledCount = v; }

    /**
     * Returns the hex colour string used as the gradient background on the course thumbnail.
     *
     * @return hex colour string (e.g. {@code "#FF5733"})
     */
    public String getThumbnailColor() { return thumbnailColor; }

    /**
     * Sets the hex colour for the course thumbnail background.
     *
     * @param v hex colour string
     */
    public void setThumbnailColor(String v){ this.thumbnailColor = v; }

    /**
     * Returns the emoji character used as the subject icon on the course thumbnail.
     *
     * @return emoji string
     */
    public String getThumbnailEmoji() { return thumbnailEmoji; }

    /**
     * Sets the emoji character for the course thumbnail icon.
     *
     * @param v emoji string
     */
    public void setThumbnailEmoji(String v){ this.thumbnailEmoji = v; }

    /**
     * Returns the ordered list of topics covered by this course
     * (e.g. {@code ["Integration", "Differentiation", ...]}).
     *
     * @return mutable list of topic strings
     */
    public List<String> getTopics()   { return topics; }

    /**
     * Sets the ordered list of topics covered by this course.
     *
     * @param v list of topic strings
     */
    public void setTopics(List<String> v){ this.topics = v; }

    /**
     * Returns the date on which the course is scheduled to start.
     *
     * @return start date, or {@code null} if not yet set
     */
    public Date getStartDate()        { return startDate; }

    /**
     * Sets the date on which the course starts.
     *
     * @param v start date
     */
    public void setStartDate(Date v)  { this.startDate = v; }

    /**
     * Returns the date and time this course document was created in Firestore.
     *
     * @return creation timestamp, or {@code null} before first write
     */
    public Date getCreatedAt()        { return createdAt; }

    /**
     * Sets the creation timestamp.
     *
     * @param v creation date/time
     */
    public void setCreatedAt(Date v)  { this.createdAt = v; }

    /**
     * Returns the current enrolment status of the course:
     * {@code "open"}, {@code "full"}, or {@code "completed"}.
     *
     * @return status string
     */
    public String getStatus()         { return status; }

    /**
     * Sets the enrolment status of the course.
     *
     * @param v {@code "open"}, {@code "full"}, or {@code "completed"}
     */
    public void setStatus(String v)   { this.status = v; }

    /**
     * Returns the session delivery type: {@code "online"} (live Zoom sessions) or
     * {@code "takehome"} (self-paced materials).
     *
     * @return session type string
     */
    public String getSessionType()    { return sessionType; }

    /**
     * Sets the session delivery type.
     *
     * @param v {@code "online"} or {@code "takehome"}
     */
    public void setSessionType(String v){ this.sessionType = v; }

    /**
     * Returns the Zoom meeting link for online courses, or {@code null} if not applicable.
     *
     * @return Zoom URL, or {@code null}
     */
    public String getZoomLink()       { return zoomLink; }

    /**
     * Sets the Zoom meeting link for online courses.
     *
     * @param v Zoom URL
     */
    public void setZoomLink(String v) { this.zoomLink = v; }
}
