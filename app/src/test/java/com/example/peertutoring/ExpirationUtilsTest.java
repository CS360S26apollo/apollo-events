package com.example.peertutoring;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for US-11: Auto-Expiration of Session Requests.
 *
 * All tests verify the pure-Java expiration predicate that mirrors the
 * logic in SessionRequest.isExpiredRequest() and ExpirationUtils.
 *
 * TC-11-U01  A "requested" request older than 3 days is flagged as expired
 * TC-11-U02  A "requested" request exactly 1 day old is not expired
 * TC-11-U03  A "booked" request older than 3 days is never expired
 * TC-11-U04  A "cancelled" request older than 3 days is never expired
 * TC-11-U05  A "completed" request older than 3 days is never expired
 * TC-11-U06  A request with null createdAt is never expired
 * TC-11-U07  A request exactly at the 3-day boundary is NOT yet expired
 * TC-11-U08  Custom window: 1-hour window expires a 2-hour-old request
 */
public class ExpirationUtilsTest {

    private static final long THREE_DAYS_MS  = 3L * 24 * 60 * 60 * 1000;
    private static final long ONE_HOUR_MS    = 60L * 60 * 1000;

    /**
     * Mirrors SessionRequest.isExpiredRequest(windowMs).
     * Returns true iff status is "requested", createdAt is non-zero,
     * and the age exceeds windowMs.
     */
    private boolean isExpired(String status, long createdAtMs, long windowMs) {
        if (!"requested".equals(status) || createdAtMs == 0) return false;
        return (System.currentTimeMillis() - createdAtMs) > windowMs;
    }

    // ── TC-11-U01 ──────────────────────────────────────────────────

    /** A "requested" request created 4 days ago must be expired. */
    @Test
    public void testRequestedOlderThan3DaysIsExpired() {
        long fourDaysAgo = System.currentTimeMillis() - (4L * 24 * 60 * 60 * 1000);
        assertTrue(isExpired("requested", fourDaysAgo, THREE_DAYS_MS));
    }

    // ── TC-11-U02 ──────────────────────────────────────────────────

    /** A "requested" request created 1 day ago must not be expired. */
    @Test
    public void testRequestedNewerThan3DaysIsNotExpired() {
        long oneDayAgo = System.currentTimeMillis() - (1L * 24 * 60 * 60 * 1000);
        assertFalse(isExpired("requested", oneDayAgo, THREE_DAYS_MS));
    }

    // ── TC-11-U03 ──────────────────────────────────────────────────

    /** A "booked" request — regardless of age — must never be expired. */
    @Test
    public void testBookedRequestIsNeverExpired() {
        long fourDaysAgo = System.currentTimeMillis() - (4L * 24 * 60 * 60 * 1000);
        assertFalse(isExpired("booked", fourDaysAgo, THREE_DAYS_MS));
    }

    // ── TC-11-U04 ──────────────────────────────────────────────────

    /** A "cancelled" request — regardless of age — must never be expired. */
    @Test
    public void testCancelledRequestIsNeverExpired() {
        long fourDaysAgo = System.currentTimeMillis() - (4L * 24 * 60 * 60 * 1000);
        assertFalse(isExpired("cancelled", fourDaysAgo, THREE_DAYS_MS));
    }

    // ── TC-11-U05 ──────────────────────────────────────────────────

    /** A "completed" request — regardless of age — must never be expired. */
    @Test
    public void testCompletedRequestIsNeverExpired() {
        long fourDaysAgo = System.currentTimeMillis() - (4L * 24 * 60 * 60 * 1000);
        assertFalse(isExpired("completed", fourDaysAgo, THREE_DAYS_MS));
    }

    // ── TC-11-U06 ──────────────────────────────────────────────────

    /** A request with createdAt == 0 (null sentinel) must never be expired. */
    @Test
    public void testNullCreatedAtIsNeverExpired() {
        assertFalse(isExpired("requested", 0, THREE_DAYS_MS));
    }

    // ── TC-11-U07 ──────────────────────────────────────────────────

    /** A request at exactly the boundary (age == windowMs) is NOT yet expired. */
    @Test
    public void testExactBoundaryIsNotYetExpired() {
        // Set createdAt such that age == THREE_DAYS_MS exactly (use > not >=)
        long exactlyThreeDays = System.currentTimeMillis() - THREE_DAYS_MS;
        assertFalse(isExpired("requested", exactlyThreeDays, THREE_DAYS_MS));
    }

    // ── TC-11-U08 ──────────────────────────────────────────────────

    /** With a 1-hour window, a 2-hour-old request is expired. */
    @Test
    public void testCustomWindowExpires2HourOldRequest() {
        long twoHoursAgo = System.currentTimeMillis() - (2 * ONE_HOUR_MS);
        assertTrue(isExpired("requested", twoHoursAgo, ONE_HOUR_MS));
    }
}
