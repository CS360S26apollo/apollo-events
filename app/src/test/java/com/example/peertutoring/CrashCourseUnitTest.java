package com.example.peertutoring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.peertutoring.models.CrashCourse;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Unit tests for the CrashCourse model and enrollment business rules.
 *
 * TC-CC-U01  Default constructor produces a non-null object
 * TC-CC-U02  Field setters/getters round-trip correctly
 * TC-CC-U03  seatsRemaining = maxStudents - enrolledCount
 * TC-CC-U04  Course is not full when enrolledCount < maxStudents
 * TC-CC-U05  Course is full when enrolledCount == maxStudents
 * TC-CC-U06  Course is full when enrolledCount > maxStudents (defensive)
 * TC-CC-U07  Status transitions: open → full → completed
 * TC-CC-U08  Online session type requires zoom link to be valid
 * TC-CC-U09  Topics list is stored and retrieved intact
 * TC-CC-U10  totalTokens must be positive for a valid course
 * TC-CC-U11  totalSessionMinutes = sessionsPerWeek * durationDays / 7 * sessionMinutes
 * TC-CC-U12  Enrollment guard: student cannot enroll when course is full
 * TC-CC-U13  Enrollment guard: student CAN enroll when one seat remains
 * TC-CC-U14  thumbnailEmoji defaults gracefully (null is handled)
 * TC-CC-U15  startDate is stored and retrieved as a Date object
 */
public class CrashCourseUnitTest {

    private CrashCourse course;

    @Before
    public void setUp() {
        course = new CrashCourse();
        course.setCourseId("course_001");
        course.setTutorUid("tutor_abc");
        course.setTutorName("Aisha Malik");
        course.setTitle("A-Level Maths Crash");
        course.setSubject("Mathematics");
        course.setDescription("Intensive maths prep for A-Level exams.");
        course.setLevel("A-Level");
        course.setDurationDays(30);
        course.setSessionsPerWeek(5);
        course.setSessionMinutes(60);
        course.setTotalTokens(500);
        course.setMaxStudents(10);
        course.setEnrolledCount(0);
        course.setStatus("open");
        course.setSessionType("online");
        course.setZoomLink("https://zoom.us/j/123456");
        course.setThumbnailEmoji("➕");
        course.setThumbnailColor("#8A2EFF");
        course.setTopics(Arrays.asList("Integration", "Differentiation", "Mechanics"));
    }

    // ── TC-CC-U01 ─────────────────────────────────────────────

    @Test
    public void testDefaultConstructorNotNull() {
        assertNotNull(new CrashCourse());
    }

    // ── TC-CC-U02 ─────────────────────────────────────────────

    @Test
    public void testFieldRoundTrip() {
        assertEquals("course_001",              course.getCourseId());
        assertEquals("tutor_abc",               course.getTutorUid());
        assertEquals("Aisha Malik",             course.getTutorName());
        assertEquals("A-Level Maths Crash",     course.getTitle());
        assertEquals("Mathematics",             course.getSubject());
        assertEquals("A-Level",                 course.getLevel());
        assertEquals(30,                        course.getDurationDays());
        assertEquals(5,                         course.getSessionsPerWeek());
        assertEquals(60,                        course.getSessionMinutes());
        assertEquals(500,                       course.getTotalTokens());
        assertEquals(10,                        course.getMaxStudents());
        assertEquals(0,                         course.getEnrolledCount());
        assertEquals("open",                    course.getStatus());
        assertEquals("online",                  course.getSessionType());
        assertEquals("https://zoom.us/j/123456",course.getZoomLink());
        assertEquals("➕",                      course.getThumbnailEmoji());
        assertEquals("#8A2EFF",                 course.getThumbnailColor());
    }

    // ── TC-CC-U03 ─────────────────────────────────────────────

    @Test
    public void testSeatsRemainingCalculation() {
        course.setMaxStudents(10);
        course.setEnrolledCount(3);
        int remaining = course.getMaxStudents() - course.getEnrolledCount();
        assertEquals("10 max - 3 enrolled = 7 seats remaining", 7, remaining);
    }

    @Test
    public void testSeatsRemainingWhenEmpty() {
        course.setEnrolledCount(0);
        int remaining = course.getMaxStudents() - course.getEnrolledCount();
        assertEquals("No enrollments → all seats remain", 10, remaining);
    }

    // ── TC-CC-U04 ─────────────────────────────────────────────

    @Test
    public void testCourseNotFullWhenBelowMax() {
        course.setMaxStudents(10);
        course.setEnrolledCount(9);
        assertFalse("9/10 enrolled → not full", isFull(course));
    }

    // ── TC-CC-U05 ─────────────────────────────────────────────

    @Test
    public void testCourseFullWhenAtMax() {
        course.setMaxStudents(10);
        course.setEnrolledCount(10);
        assertTrue("10/10 enrolled → full", isFull(course));
    }

    // ── TC-CC-U06 ─────────────────────────────────────────────

    @Test
    public void testCourseFullWhenAboveMax() {
        course.setMaxStudents(10);
        course.setEnrolledCount(11); // defensive edge case
        assertTrue("11/10 enrolled → treated as full", isFull(course));
    }

    // ── TC-CC-U07 ─────────────────────────────────────────────

    @Test
    public void testStatusTransitionOpenToFull() {
        course.setStatus("open");
        assertEquals("open", course.getStatus());
        course.setStatus("full");
        assertEquals("full", course.getStatus());
    }

    @Test
    public void testStatusTransitionFullToCompleted() {
        course.setStatus("full");
        course.setStatus("completed");
        assertEquals("completed", course.getStatus());
    }

    @Test
    public void testStatusFullMatchesFirestoreValue() {
        course.setStatus("full");
        assertTrue("Status check must match exact Firestore string",
                "full".equals(course.getStatus()));
    }

    // ── TC-CC-U08 ─────────────────────────────────────────────

    @Test
    public void testOnlineTypeHasZoomLink() {
        course.setSessionType("online");
        course.setZoomLink("https://zoom.us/j/999");
        assertTrue("Online course should have a non-empty zoom link",
                course.getZoomLink() != null && !course.getZoomLink().isEmpty());
    }

    @Test
    public void testTakeHomeTypeDoesNotNeedZoomLink() {
        course.setSessionType("takehome");
        course.setZoomLink(null);
        assertNull("Take-home course zoom link should be null", course.getZoomLink());
    }

    // ── TC-CC-U09 ─────────────────────────────────────────────

    @Test
    public void testTopicsListStoredCorrectly() {
        List<String> topics = course.getTopics();
        assertNotNull(topics);
        assertEquals(3, topics.size());
        assertTrue(topics.contains("Integration"));
        assertTrue(topics.contains("Differentiation"));
        assertTrue(topics.contains("Mechanics"));
    }

    @Test
    public void testEmptyTopicsListIsAllowed() {
        course.setTopics(Arrays.asList());
        assertNotNull(course.getTopics());
        assertTrue(course.getTopics().isEmpty());
    }

    // ── TC-CC-U10 ─────────────────────────────────────────────

    @Test
    public void testTotalTokensIsPositive() {
        assertTrue("A valid course must have positive token cost",
                course.getTotalTokens() > 0);
    }

    @Test
    public void testTotalTokensUpdateable() {
        course.setTotalTokens(1200);
        assertEquals(1200, course.getTotalTokens());
    }

    // ── TC-CC-U11: total session minutes across course duration ──

    @Test
    public void testTotalSessionMinutesCalculation() {
        // 30 days, 5 sessions/week, 60 min each
        // Approx weeks = 30/7 ≈ 4.28 → floor = 4 full weeks + partial
        // Simple formula used in UI: sessionsPerWeek * (durationDays / 7) * sessionMinutes
        int weeks = course.getDurationDays() / 7;     // 4
        int totalSessions = course.getSessionsPerWeek() * weeks; // 20
        int totalMinutes  = totalSessions * course.getSessionMinutes(); // 1200
        assertEquals("30 days / 7 = 4 weeks × 5 sessions × 60 min = 1200 min",
                1200, totalMinutes);
    }

    // ── TC-CC-U12: enrollment guard when full ──────────────────

    @Test
    public void testCannotEnrollWhenFull() {
        course.setMaxStudents(5);
        course.setEnrolledCount(5);
        assertFalse("Student must not be able to enroll when course is full",
                canEnroll(course));
    }

    // ── TC-CC-U13: enrollment guard when one seat remains ──────

    @Test
    public void testCanEnrollWhenOneSeatRemains() {
        course.setMaxStudents(5);
        course.setEnrolledCount(4);
        assertTrue("Student must be able to enroll when one seat remains",
                canEnroll(course));
    }

    @Test
    public void testCannotEnrollWhenStatusIsFull() {
        course.setMaxStudents(10);
        course.setEnrolledCount(5); // seats still available
        course.setStatus("full");   // but status already set to full
        assertFalse("If status is 'full', enrollment must be rejected regardless of count",
                canEnrollByStatus(course));
    }

    // ── TC-CC-U14 ─────────────────────────────────────────────

    @Test
    public void testNullEmojiHandledGracefully() {
        course.setThumbnailEmoji(null);
        // Simulates the null-safe check: emoji != null ? emoji : "📚"
        String emoji = course.getThumbnailEmoji() != null ? course.getThumbnailEmoji() : "📚";
        assertEquals("Null emoji must fall back to 📚", "📚", emoji);
    }

    // ── TC-CC-U15 ─────────────────────────────────────────────

    @Test
    public void testStartDateStoredAsDate() {
        Date now = new Date();
        course.setStartDate(now);
        assertNotNull(course.getStartDate());
        assertEquals(now.getTime(), course.getStartDate().getTime());
    }

    @Test
    public void testCreatedAtStoredAsDate() {
        Date now = new Date();
        course.setCreatedAt(now);
        assertNotNull(course.getCreatedAt());
        assertEquals(now.getTime(), course.getCreatedAt().getTime());
    }

    // ── Helpers (mirror CoursesDetailActivity logic) ──────────

    private boolean isFull(CrashCourse c) {
        return c.getEnrolledCount() >= c.getMaxStudents();
    }

    private boolean canEnroll(CrashCourse c) {
        return c.getEnrolledCount() < c.getMaxStudents();
    }

    private boolean canEnrollByStatus(CrashCourse c) {
        return !"full".equals(c.getStatus()) && c.getEnrolledCount() < c.getMaxStudents();
    }
}
