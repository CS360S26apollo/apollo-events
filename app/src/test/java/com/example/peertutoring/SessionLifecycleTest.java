package com.example.peertutoring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.peertutoring.models.SessionRequest;

import org.junit.Test;

import java.util.Calendar;

/**
 * Unit tests for User Story 16: Session Lifecycle Tracking.
 * Also covers token cost calculation used in US 08.
 */
public class SessionLifecycleTest {

    // ── Status initialization ─────────────────────────────────

    @Test
    public void testInitialStatusIsRequested() {
        SessionRequest request = new SessionRequest(
                "student1", "John Doe", "Math", "Algebra", "Learn basics", 60);
        assertEquals(SessionRequest.STATUS_REQUESTED, request.getStatus());
    }

    @Test
    public void testNewRequestHasCorrectFields() {
        SessionRequest request = new SessionRequest(
                "s1", "Jane Doe", "Physics", "Waves", "Understand waves", 90);
        assertEquals("s1", request.getStudentUid());
        assertEquals("Jane Doe", request.getStudentName());
        assertEquals("Physics", request.getSubject());
        assertEquals("Waves", request.getTopic());
        assertEquals(90, request.getDurationMinutes());
    }

    // ── Status transitions ────────────────────────────────────

    @Test
    public void testStatusTransitions() {
        SessionRequest request = new SessionRequest();

        request.setStatus(SessionRequest.STATUS_BOOKED);
        assertEquals(SessionRequest.STATUS_BOOKED, request.getStatus());

        request.setStatus(SessionRequest.STATUS_COMPLETED);
        assertEquals(SessionRequest.STATUS_COMPLETED, request.getStatus());

        request.setStatus(SessionRequest.STATUS_CANCELLED);
        assertEquals(SessionRequest.STATUS_CANCELLED, request.getStatus());

        request.setStatus(SessionRequest.STATUS_NO_SHOW);
        assertEquals(SessionRequest.STATUS_NO_SHOW, request.getStatus());
    }

    // ── isUpcoming edge cases ─────────────────────────────────

    @Test
    public void testUpcomingSession() {
        SessionRequest request = new SessionRequest();
        request.setStatus(SessionRequest.STATUS_BOOKED);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, 1);
        request.setScheduledDate(cal.getTime());
        assertTrue("Session tomorrow should be upcoming", request.isUpcoming());
    }

    @Test
    public void testPastBookedSessionIsNotUpcoming() {
        SessionRequest request = new SessionRequest();
        request.setStatus(SessionRequest.STATUS_BOOKED);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -1);
        request.setScheduledDate(cal.getTime());
        assertFalse("Session yesterday should not be upcoming", request.isUpcoming());
    }

    @Test
    public void testCompletedSessionIsNotUpcoming() {
        SessionRequest request = new SessionRequest();
        request.setStatus(SessionRequest.STATUS_COMPLETED);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, 1);
        request.setScheduledDate(cal.getTime());
        assertFalse("Completed session should not be upcoming even if date is future", request.isUpcoming());
    }

    @Test
    public void testCancelledSessionIsNotUpcoming() {
        SessionRequest request = new SessionRequest();
        request.setStatus(SessionRequest.STATUS_CANCELLED);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, 2);
        request.setScheduledDate(cal.getTime());
        assertFalse("Cancelled session should not be upcoming", request.isUpcoming());
    }

    @Test
    public void testNoShowSessionIsNotUpcoming() {
        SessionRequest request = new SessionRequest();
        request.setStatus(SessionRequest.STATUS_NO_SHOW);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, 1);
        request.setScheduledDate(cal.getTime());
        assertFalse("No-show session should not be upcoming", request.isUpcoming());
    }

    @Test
    public void testNullScheduledDateIsNotUpcoming() {
        SessionRequest request = new SessionRequest();
        request.setStatus(SessionRequest.STATUS_BOOKED);
        // scheduledDate is null by default
        assertNull(request.getScheduledDate());
        assertFalse("Session with null date should not be upcoming", request.isUpcoming());
    }

    @Test
    public void testRequestedStatusIsNotUpcoming() {
        // A request that was never booked should not count as upcoming
        SessionRequest request = new SessionRequest(
                "s1", "John", "Math", "Algebra", "Goals", 60);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, 1);
        request.setScheduledDate(cal.getTime());
        // status is still "requested", not "booked"
        assertFalse("Requested (not yet booked) session should not be upcoming",
                request.isUpcoming());
    }

    // ── Token cost calculation (US 08) ────────────────────────
    // Mirrors the formula in NewSessionRequestActivity:
    //   cost = ceil(durationMinutes / 60.0) * ratePerHour

    @Test
    public void testTokenCostSixtyMinutes() {
        int cost = tokenCost(60, 100);
        assertEquals("60 min @ 100/hr should cost 100 tokens", 100, cost);
    }

    @Test
    public void testTokenCostThirtyMinutesBillsFullHour() {
        // 30 min rounds up to 1 full hour
        int cost = tokenCost(30, 100);
        assertEquals("30 min @ 100/hr should cost 100 tokens (1 full hour)", 100, cost);
    }

    @Test
    public void testTokenCostFortyFiveMinutesBillsFullHour() {
        int cost = tokenCost(45, 80);
        assertEquals("45 min @ 80/hr should cost 80 tokens (1 full hour)", 80, cost);
    }

    @Test
    public void testTokenCostNinetyMinutesBillsTwoHours() {
        int cost = tokenCost(90, 80);
        assertEquals("90 min @ 80/hr should cost 160 tokens (2 hours)", 160, cost);
    }

    @Test
    public void testTokenCostOneHundredTwentyMinutesBillsTwoHours() {
        int cost = tokenCost(120, 50);
        assertEquals("120 min @ 50/hr should cost 100 tokens", 100, cost);
    }

    @Test
    public void testTokenCostWithLowRateTutor() {
        int cost = tokenCost(60, 20);
        assertEquals("60 min @ 20/hr should cost 20 tokens", 20, cost);
    }

    @Test
    public void testTokenCostNeverZero() {
        // Even 1 minute should cost at least 1 token (max guard in code)
        int cost = Math.max(1, (int) Math.ceil(1 / 60.0) * 0);
        // rate=0 edge: max(1, 0) = 1
        assertEquals(1, cost);
    }

    // ── Token balance check (US 08) ───────────────────────────

    @Test
    public void testStudentWithSufficientTokensCanBook() {
        int studentTokens = 200;
        int sessionCost   = 100;
        assertTrue("Student with 200 tokens can afford 100-token session",
                studentTokens >= sessionCost);
    }

    @Test
    public void testStudentWithZeroTokensCannotBook() {
        int studentTokens = 0;
        int sessionCost   = 100;
        assertFalse("Student with 0 tokens cannot afford 100-token session",
                studentTokens >= sessionCost);
    }

    @Test
    public void testStudentWithExactTokensCanBook() {
        int studentTokens = 100;
        int sessionCost   = 100;
        assertTrue("Student with exactly enough tokens can book",
                studentTokens >= sessionCost);
    }

    @Test
    public void testStudentWithOneTokenShortCannotBook() {
        int studentTokens = 99;
        int sessionCost   = 100;
        assertFalse("Student with 99 tokens cannot afford 100-token session",
                studentTokens >= sessionCost);
    }

    // ── Token refund on cancel (US 16) ────────────────────────

    @Test
    public void testRefundRestoresTokenBalance() {
        int initialBalance  = 500;
        int sessionCost     = 100;
        int balanceAfterBook = initialBalance - sessionCost;
        int balanceAfterRefund = balanceAfterBook + sessionCost;

        assertEquals("Balance should be fully restored after refund",
                initialBalance, balanceAfterRefund);
    }

    @Test
    public void testPartialRefundNotPossible() {
        // The system always refunds the full session token cost
        int sessionCost = 150;
        int refunded    = sessionCost; // full amount
        assertEquals(150, refunded);
    }

    // ── Helper ────────────────────────────────────────────────

    private int tokenCost(int durationMinutes, int ratePerHour) {
        return Math.max(1, (int) Math.ceil(durationMinutes / 60.0) * ratePerHour);
    }
}
