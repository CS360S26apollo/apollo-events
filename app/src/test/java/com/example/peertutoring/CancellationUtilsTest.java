package com.example.peertutoring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.example.peertutoring.utils.CancellationUtils;

import org.junit.Test;

import java.util.Date;

/**
 * Unit tests for US-15: Cancellation refund rules.
 *
 * Refund policy:
 *  - More than 24 h before session → 100% refund
 *  - 12 to 24 h before session     → 50% refund
 *  - Less than 12 h before session → 0% refund
 *
 * TC-15-U01  More than 24h ahead → full refund
 * TC-15-U02  Exactly 25h ahead → full refund
 * TC-15-U03  Exactly 24h ahead (boundary) → 50% refund (not > 24h)
 * TC-15-U04  18h ahead (mid window) → 50% refund
 * TC-15-U05  Exactly 12h ahead (boundary) → no refund (not > 12h)
 * TC-15-U06  6h ahead → no refund
 * TC-15-U07  Session in the past → no refund
 * TC-15-U08  scheduledDate is null → full refund
 * TC-15-U09  Odd token amount rounds down at 50%
 * TC-15-U10  Zero tokens → refund is 0
 * TC-15-U11  refundDescription contains token amount for full refund
 * TC-15-U12  refundDescription contains token amount for partial refund
 * TC-15-U13  refundDescription says "No refund" for <12h
 * TC-15-U14  refundDescription handles null scheduledDate
 * TC-15-U15  Constants are correctly defined
 */
public class CancellationUtilsTest {

    private static final int TOKENS = 100;

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Date futureDate(long msFromNow) {
        return new Date(System.currentTimeMillis() + msFromNow);
    }

    private Date pastDate(long msBefore) {
        return new Date(System.currentTimeMillis() - msBefore);
    }

    private long hoursMs(int h) { return (long) h * 60 * 60 * 1000; }

    // ── TC-15-U01 ──────────────────────────────────────────────────────

    @Test
    public void testMoreThan24hFullRefund() {
        int refund = CancellationUtils.calculateRefund(futureDate(hoursMs(48)), TOKENS);
        assertEquals("48h ahead should give full refund", TOKENS, refund);
    }

    // ── TC-15-U02 ──────────────────────────────────────────────────────

    @Test
    public void testExactly25hFullRefund() {
        int refund = CancellationUtils.calculateRefund(futureDate(hoursMs(25)), TOKENS);
        assertEquals("25h ahead should give full refund", TOKENS, refund);
    }

    // ── TC-15-U03: boundary at exactly 24h → NOT >24h → 50% ───────��──

    @Test
    public void testExactly24hBoundaryPartialRefund() {
        // msUntil == FULL_REFUND_CUTOFF_MS, which is NOT > cutoff → falls into partial tier
        Date sessionDate = new Date(
                System.currentTimeMillis() + CancellationUtils.FULL_REFUND_CUTOFF_MS);
        int refund = CancellationUtils.calculateRefund(sessionDate, TOKENS);
        assertEquals("Exactly 24h ahead should give 50% refund", TOKENS / 2, refund);
    }

    // ── TC-15-U04 ──────────────────────────────────────────────────────

    @Test
    public void testEighteenHoursPartialRefund() {
        int refund = CancellationUtils.calculateRefund(futureDate(hoursMs(18)), TOKENS);
        assertEquals("18h ahead should give 50% refund", TOKENS / 2, refund);
    }

    // ── TC-15-U05: boundary at exactly 12h → NOT >12h → no refund ────

    @Test
    public void testExactly12hBoundaryNoRefund() {
        Date sessionDate = new Date(
                System.currentTimeMillis() + CancellationUtils.PARTIAL_REFUND_CUTOFF_MS);
        int refund = CancellationUtils.calculateRefund(sessionDate, TOKENS);
        assertEquals("Exactly 12h ahead should give no refund", 0, refund);
    }

    // ── TC-15-U06 ──────────────────────────────────────────────────────

    @Test
    public void testSixHoursNoRefund() {
        int refund = CancellationUtils.calculateRefund(futureDate(hoursMs(6)), TOKENS);
        assertEquals("6h ahead should give no refund", 0, refund);
    }

    // ── TC-15-U07 ──────────────────────────────────────────────────────

    @Test
    public void testPastSessionNoRefund() {
        int refund = CancellationUtils.calculateRefund(pastDate(hoursMs(1)), TOKENS);
        assertEquals("Past session should give no refund", 0, refund);
    }

    // ── TC-15-U08 ──────────────────────────────────────────────────────

    @Test
    public void testNullScheduledDateFullRefund() {
        int refund = CancellationUtils.calculateRefund(null, TOKENS);
        assertEquals("Null date should give full refund", TOKENS, refund);
    }

    // ── TC-15-U09: integer division floors the 50% ────────────────────

    @Test
    public void testOddTokenAmountRoundsDown() {
        int refund = CancellationUtils.calculateRefund(futureDate(hoursMs(18)), 101);
        assertEquals("101 tokens at 50% should refund 50 (floors)", 50, refund);
    }

    // ── TC-15-U10 ──────────────────────────────────────────────────────

    @Test
    public void testZeroTokensRefundIsZero() {
        int refund = CancellationUtils.calculateRefund(futureDate(hoursMs(48)), 0);
        assertEquals("0 tokens always refunds 0", 0, refund);
    }

    // ── TC-15-U11 ──────────────────────────────────────────────────────

    @Test
    public void testRefundDescriptionFullRefundContainsAmount() {
        String desc = CancellationUtils.refundDescription(futureDate(hoursMs(48)), TOKENS);
        assertTrue("Full refund description should mention token amount",
                desc.contains(String.valueOf(TOKENS)));
        assertTrue("Full refund description should say 'Full'", desc.contains("Full"));
    }

    // ── TC-15-U12 ──────────────────────────────────────────────────────

    @Test
    public void testRefundDescriptionPartialRefundContainsAmount() {
        String desc = CancellationUtils.refundDescription(futureDate(hoursMs(18)), TOKENS);
        assertTrue("Partial refund description should mention refund amount",
                desc.contains(String.valueOf(TOKENS / 2)));
        assertTrue("Partial refund description should say '50%'", desc.contains("50%"));
    }

    // ── TC-15-U13 ──────────────────────────────────────────────────────

    @Test
    public void testRefundDescriptionNoRefundSaysNoRefund() {
        String desc = CancellationUtils.refundDescription(futureDate(hoursMs(6)), TOKENS);
        assertTrue("No-refund description should say 'No refund'",
                desc.contains("No refund"));
    }

    // ── TC-15-U14 ──────────────────────────────────────────────────────

    @Test
    public void testRefundDescriptionNullDateFullRefund() {
        String desc = CancellationUtils.refundDescription(null, TOKENS);
        assertTrue("Null date description should say 'Full refund'",
                desc.contains("Full"));
    }

    // ── TC-15-U15: constants ────────────────────────────────────��─────

    @Test
    public void testFullRefundCutoffIs24Hours() {
        assertEquals("FULL_REFUND_CUTOFF_MS should be 24h",
                24L * 60 * 60 * 1000, CancellationUtils.FULL_REFUND_CUTOFF_MS);
    }

    @Test
    public void testPartialRefundCutoffIs12Hours() {
        assertEquals("PARTIAL_REFUND_CUTOFF_MS should be 12h",
                12L * 60 * 60 * 1000, CancellationUtils.PARTIAL_REFUND_CUTOFF_MS);
    }
}
