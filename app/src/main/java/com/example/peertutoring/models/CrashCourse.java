package com.example.peertutoring.models;

import com.google.firebase.firestore.FieldValue;
import java.util.Date;
import java.util.List;

/**
 * Model for a tutor-created crash course.
 * Stored in Firestore: courses/{courseId}
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

    public CrashCourse() {}

    // Getters and setters
    public String getCourseId()       { return courseId; }
    public void setCourseId(String v) { this.courseId = v; }
    public String getTutorUid()       { return tutorUid; }
    public void setTutorUid(String v) { this.tutorUid = v; }
    public String getTutorName()      { return tutorName; }
    public void setTutorName(String v){ this.tutorName = v; }
    public String getTitle()          { return title; }
    public void setTitle(String v)    { this.title = v; }
    public String getSubject()        { return subject; }
    public void setSubject(String v)  { this.subject = v; }
    public String getDescription()    { return description; }
    public void setDescription(String v){ this.description = v; }
    public String getLevel()          { return level; }
    public void setLevel(String v)    { this.level = v; }
    public int getDurationDays()      { return durationDays; }
    public void setDurationDays(int v){ this.durationDays = v; }
    public int getSessionsPerWeek()   { return sessionsPerWeek; }
    public void setSessionsPerWeek(int v){ this.sessionsPerWeek = v; }
    public int getSessionMinutes()    { return sessionMinutes; }
    public void setSessionMinutes(int v){ this.sessionMinutes = v; }
    public int getTotalTokens()       { return totalTokens; }
    public void setTotalTokens(int v) { this.totalTokens = v; }
    public int getMaxStudents()       { return maxStudents; }
    public void setMaxStudents(int v) { this.maxStudents = v; }
    public int getEnrolledCount()     { return enrolledCount; }
    public void setEnrolledCount(int v){ this.enrolledCount = v; }
    public String getThumbnailColor() { return thumbnailColor; }
    public void setThumbnailColor(String v){ this.thumbnailColor = v; }
    public String getThumbnailEmoji() { return thumbnailEmoji; }
    public void setThumbnailEmoji(String v){ this.thumbnailEmoji = v; }
    public List<String> getTopics()   { return topics; }
    public void setTopics(List<String> v){ this.topics = v; }
    public Date getStartDate()        { return startDate; }
    public void setStartDate(Date v)  { this.startDate = v; }
    public Date getCreatedAt()        { return createdAt; }
    public void setCreatedAt(Date v)  { this.createdAt = v; }
    public String getStatus()         { return status; }
    public void setStatus(String v)   { this.status = v; }
    public String getSessionType()    { return sessionType; }
    public void setSessionType(String v){ this.sessionType = v; }
    public String getZoomLink()       { return zoomLink; }
    public void setZoomLink(String v) { this.zoomLink = v; }
}