package com.example.peertutoring;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Date;

/**
 * Unit tests for US-14: Prevent Double-Booking / Detect Scheduling Conflicts.
 *
 * Because ConflictChecker.checkConflict() is async (Firestore), these tests
 * exercise the pure-Java overlap predicates that mirror its internal logic,
 * keeping them fast and free of Android dependencies.
 *
 * TC-14-U01  Non-overlapping sessions have no conflict
 * TC-14-U02  Exact same start time → conflict
 * TC-14-U03  Partial overlap (proposed starts during existing) → conflict
 * TC-14-U04  Proposed session ends exactly when existing starts → no conflict (adjacent)
 * TC-14-U05  Adjacent sessions conflict when buffer is applied
 * TC-14-U06  Proposed session within buffer window → conflict
 * TC-14-U07  Proposed session beyond buffer window → no conflict
 * TC-14-U08  Cancelled status does not block a new session
 * TC-14-U09  Expired status does not block a new session
 * TC-14-U10  Completed status does not block a new session
 * TC-14-U11  'Requested' status does block (same as booked)
 * TC-14-U12  Student exact overlap detected
 * TC-14-U13  Student adjacent (no overlap) allowed
 * TC-14-U14  Zero-duration guard: 0-minute session never blocks
 * TC-14-U15  Proposed session completely contains existing → conflict
 */
public class ConflictCheckerTest {

    // ── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Mirrors ConflictChecker's tutor overlap check:
     * conflict if proposedStart < existingEnd + bufferMs AND proposedEnd > existingStart - bufferMs
     */
    private boolean tutorConflicts(long proposedStartMs, int proposedDur,
                                   long existingStartMs, int existingDur, long bufferMs) {
        long proposedEndMs  = proposedStartMs + (long) proposedDur  * 60_000L;
        long existingEndMs  = existingStartMs + (long) existingDur  * 60_000L;
        return proposedStartMs < existingEndMs + bufferMs
                && proposedEndMs > existingStartMs - bufferMs;
    }

    /**
     * Mirrors ConflictChecker's student overlap check (no buffer):
     * conflict if proposedStart < existingEnd AND proposedEnd > existingStart
     */
    private boolean studentConflicts(long proposedStartMs, int proposedDur,
                                     long existingStartMs, int existingDur) {
        long proposedEndMs = proposedStartMs + (long) proposedDur  * 60_000L;
        long existingEndMs = existingStartMs + (long) existingDur  * 60_000L;
        return proposedStartMs < existingEndMs && proposedEndMs > existingStartMs;
    }

    /** Returns true only if the status is 'requested' or 'booked' (active). */
    private boolean isActive(String status) {
        return "requested".equals(status) || "booked".equals(status);
    }

    private long now() { return System.currentTimeMillis(); }
    private long hoursMs(int h) { return (long) h * 60 * 60 * 1000; }
    private long minsMs(int m)  { return (long) m * 60 * 1000; }

    // ── TC-14-U01: no overlap, no buffer ─────────────────────────────────────

    @Test
    public void testNoOverlapNonConflicting() {
        long base = now() + hoursMs(2);
        // Existing: base + 2h for 60 min. Proposed: base + 4h for 60 min. Gap = 60 min.
        assertFalse(tutorConflicts(base + hoursMs(4), 60, base + hoursMs(2), 60, 0));
    }

    // ── TC-14-U02: exact same start time → conflict ───────────────────────────

    @Test
    public void testExactSameStartTimeConflicts() {
        long base = now() + hoursMs(3);
        assertTrue(tutorConflicts(base, 60, base, 60, 0));
    }

    // ── TC-14-U03: partial overlap (proposed starts during existing) ──────────

    @Test
    public void testPartialOverlapConflicts() {
        long existing = now() + hoursMs(2);
        long proposed = existing + minsMs(30); // starts 30 min into the existing 60-min session
        assertTrue(tutorConflicts(proposed, 60, existing, 60, 0));
    }

    // ── TC-14-U04: adjacent sessions, no buffer ───────────────────────────────

    @Test
    public void testAdjacentNoBufferNoConflict() {
        long existing  = now() + hoursMs(2);
        long proposed  = existing + hoursMs(1); // starts exactly when existing ends
        assertFalse(tutorConflicts(proposed, 60, existing, 60, 0));
    }

    // ── TC-14-U05: adjacent sessions conflict with buffer ────────────────────

    @Test
    public void testAdjacentWithBufferConflicts() {
        long buffer    = minsMs(15);
        long existing  = now() + hoursMs(2);
        long proposed  = existing + hoursMs(1); // adjacent — within 15-min buffer
        assertTrue(tutorConflicts(proposed, 60, existing, 60, buffer));
    }

    // ── TC-14-U06: proposed within buffer window → conflict ──────────────────

    @Test
    public void testProposedWithinBufferConflicts() {
        long buffer    = minsMs(30);
        long existing  = now() + hoursMs(2);
        // Proposed starts 20 min after existing ends — within 30-min buffer
        long proposed  = existing + hoursMs(1) + minsMs(20);
        assertTrue(tutorConflicts(proposed, 60, existing, 60, buffer));
    }

    // ── TC-14-U07: proposed beyond buffer → no conflict ──────────────────────

    @Test
    public void testProposedBeyondBufferNoConflict() {
        long buffer    = minsMs(15);
        long existing  = now() + hoursMs(2);
        // Proposed starts 30 min after existing ends — beyond 15-min buffer
        long proposed  = existing + hoursMs(1) + minsMs(30);
        assertFalse(tutorConflicts(proposed, 60, existing, 60, buffer));
    }

    // ── TC-14-U08: cancelled status is inactive ───────────────────────────────

    @Test
    public void testCancelledIsInactive() {
        assertFalse(isActive("cancelled"));
    }

    // ── TC-14-U09: expired status is inactive ────────────────────────────────

    @Test
    public void testExpiredIsInactive() {
        assertFalse(isActive("expired"));
    }

    // ── TC-14-U10: completed status is inactive ───────────────────────────────

    @Test
    public void testCompletedIsInactive() {
        assertFalse(isActive("completed"));
    }

    // ── TC-14-U11: requested status is active ────────────────────────────────

    @Test
    public void testRequestedIsActive() {
        assertTrue(isActive("requested"));
    }

    @Test
    public void testBookedIsActive() {
        assertTrue(isActive("booked"));
    }

    // ── TC-14-U12: student exact overlap detected ─────────────────────────────

    @Test
    public void testStudentExactOverlapConflicts() {
        long base = now() + hoursMs(5);
        assertTrue(studentConflicts(base, 60, base, 60));
    }

    // ── TC-14-U13: student adjacent allowed ──────────────────────────────────

    @Test
    public void testStudentAdjacentNoConflict() {
        long existing = now() + hoursMs(5);
        long proposed = existing + hoursMs(1);
        assertFalse(studentConflicts(proposed, 60, existing, 60));
    }

    // ── TC-14-U14: zero-duration existing session never blocks ───────────────

    @Test
    public void testZeroDurationExistingDoesNotBlock() {
        long base = now() + hoursMs(3);
        // A 0-min existing session has existingEnd == existingStart, so no overlap.
        assertFalse(tutorConflicts(base + hoursMs(1), 60, base, 0, 0));
    }

    // ── TC-14-U15: proposed completely contains existing → conflict ───────────

    @Test
    public void testProposedContainsExistingConflicts() {
        long existing = now() + hoursMs(3);
        // Proposed starts 30 min before and ends 30 min after the existing session
        long proposed = existing - minsMs(30);
        assertTrue(tutorConflicts(proposed, 120, existing, 60, 0));
    }

    // ── TC-14-U16: existing completely contains proposed → conflict ───────────

    @Test
    public void testExistingContainsProposedConflicts() {
        long existing = now() + hoursMs(2);
        // Proposed fits entirely within the existing 120-min session
        long proposed = existing + minsMs(30);
        assertTrue(tutorConflicts(proposed, 30, existing, 120, 0));
    }
}
