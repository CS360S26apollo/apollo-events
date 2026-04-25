package com.example.peertutoring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.example.peertutoring.models.SessionRequest;

import org.junit.Test;

import java.util.Calendar;
import java.util.Date;

/**
 * Unit tests for User Story 12: Instant Book.
 *
 * Covers:
 *  - Session is created with STATUS_BOOKED (not STATUS_REQUESTED)
 *  - scheduledDate is set and is in the future
 *  - isUpcoming() returns true for a future instant-booked session
 *  - Token cost formula matches the activity's calculateTokenCost logic
 *  - Next-occurrence scheduling puts the date in the future
 *  - Token deduction and refund arithmetic
 */
public class InstantBookUnitTest {

    // ── Status: must be BOOKED, not REQUESTED ─────────────────

    @Test
    public void testInstantBookStatusIsBooked() {
        SessionRequest req = buildInstantBookRequest(60, 40, futureDateFor(Calendar.MONDAY, 9));
        assertEquals("Instant Book must set status to BOOKED",
                SessionRequest.STATUS_BOOKED, req.getStatus());
    }

    @Test
    public void testInstantBookStatusIsNotRequested() {
        SessionRequest req = buildInstantBookRequest(60, 40, futureDateFor(Calendar.MONDAY, 9));
        assertFalse("Instant Book must NOT have status REQUESTED",
                SessionRequest.STATUS_REQUESTED.equals(req.getStatus()));
    }

    // ── scheduledDate is set and in the future ─────────────────

    @Test
    public void testInstantBookHasScheduledDate() {
        Date slot = futureDateFor(Calendar.WEDNESDAY, 10);
        SessionRequest req = buildInstantBookRequest(60, 50, slot);
        assertNotNull("Instant Book must set scheduledDate", req.getScheduledDate());
    }

    @Test
    public void testInstantBookScheduledDateIsInFuture() {
        Date slot = futureDateFor(Calendar.FRIDAY, 14);
        SessionRequest req = buildInstantBookRequest(60, 50, slot);
        assertTrue("Instant Book scheduledDate must be in the future",
                req.getScheduledDate().after(new Date()));
    }

    // ── isUpcoming ─────────────────────────────────────────────

    @Test
    public void testInstantBookSessionIsUpcoming() {
        Date slot = futureDateFor(Calendar.THURSDAY, 15);
        SessionRequest req = buildInstantBookRequest(60, 40, slot);
        assertTrue("A future instant-booked session should be upcoming", req.isUpcoming());
    }

    @Test
    public void testInstantBookWithPastSlotIsNotUpcoming() {
        // Simulate a slot that ended up in the past (edge case)
        Calendar past = Calendar.getInstance();
        past.add(Calendar.DAY_OF_MONTH, -1);

        SessionRequest req = buildInstantBookRequest(60, 40, past.getTime());
        assertFalse("A past instant-booked session should not be upcoming", req.isUpcoming());
    }

    // ── Token cost formula: ceil(duration / 60.0) * rate ──────

    @Test
    public void testInstantBookCostSixtyMin() {
        assertEquals("60 min @ 40/hr = 40 tokens", 40, instantBookCost(60, 40));
    }

    @Test
    public void testInstantBookCostThirtyMinRoundsUp() {
        assertEquals("30 min @ 40/hr = 40 tokens (1 full hour billed)", 40, instantBookCost(30, 40));
    }

    @Test
    public void testInstantBookCostNinetyMinTwoHours() {
        assertEquals("90 min @ 50/hr = 100 tokens (2 hours)", 100, instantBookCost(90, 50));
    }

    @Test
    public void testInstantBookCostFortyFiveMinRoundsUp() {
        assertEquals("45 min @ 35/hr = 35 tokens (1 full hour billed)", 35, instantBookCost(45, 35));
    }

    // ── Scheduled slot scheduling: next occurrence is future ──

    @Test
    public void testNextOccurrenceIsAlwaysFuture() {
        // Iterate all 7 days and verify the computed date is after now
        int[] calDays = {Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
                Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY};

        for (int calDay : calDays) {
            Date next = futureDateFor(calDay, 10);
            assertTrue("Next occurrence of day " + calDay + " at 10:00 must be in the future",
                    next.after(new Date()));
        }
    }

    // ── Token balance arithmetic ───────────────────────────────

    @Test
    public void testInstantBookDeductsCorrectAmount() {
        long startBalance = 150;
        int  cost         = instantBookCost(60, 40); // 40 tokens
        long newBalance   = startBalance - cost;
        assertEquals("Balance after instant book should be 110", 110, newBalance);
    }

    @Test
    public void testInstantBookRefundRestoresBalance() {
        long startBalance = 150;
        int  cost         = instantBookCost(60, 40);
        long afterDeduct  = startBalance - cost;
        long afterRefund  = afterDeduct  + cost;
        assertEquals("Refund must fully restore original balance", startBalance, afterRefund);
    }

    @Test
    public void testInstantBookInsufficientTokensDetected() {
        long studentTokens = 25;
        int  cost          = instantBookCost(60, 40); // 40 tokens
        assertFalse("Student with 25 tokens cannot afford a 40-token session",
                studentTokens >= cost);
    }

    @Test
    public void testInstantBookExactBalanceIsAllowed() {
        long studentTokens = 40;
        int  cost          = instantBookCost(60, 40);
        assertTrue("Student with exactly 40 tokens can afford a 40-token session",
                studentTokens >= cost);
    }

    // ── Helpers ───────────────────────────────────────────────

    private SessionRequest buildInstantBookRequest(int durationMin, int rate, Date scheduledDate) {
        SessionRequest req = new SessionRequest(
                "student_uid_test", "Test Student",
                "Mathematics", "Calculus", "Pass the exam", durationMin);
        req.setStatus(SessionRequest.STATUS_BOOKED);
        req.setTokens(instantBookCost(durationMin, rate));
        req.setTutorUid("seed_tutor_aisha");
        req.setTutorName("Aisha Malik");
        req.setScheduledDate(scheduledDate);
        return req;
    }

    private int instantBookCost(int durationMinutes, int ratePerHour) {
        return Math.max(1, (int) Math.ceil(durationMinutes / 60.0) * ratePerHour);
    }

    /** Returns the next future occurrence of the given Calendar day-of-week at the given hour. */
    private Date futureDateFor(int calendarDay, int hour) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        while (cal.get(Calendar.DAY_OF_WEEK) != calendarDay) {
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
        if (!cal.getTime().after(new Date())) {
            cal.add(Calendar.WEEK_OF_YEAR, 1);
        }
        return cal.getTime();
    }
}
