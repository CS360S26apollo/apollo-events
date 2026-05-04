package com.example.peertutoring.utils;

import java.util.Date;

/**
 * US-15: Computes token refunds when a student cancels a booked session.
 *
 * Refund policy (time remaining until session start):
 *  - More than 24 hours  → 100% refund
 *  - 12 to 24 hours      → 50% refund
 *  - Less than 12 hours  → no refund (0%)
 *
 * If scheduledDate is null (session has no confirmed time), a full refund is given.
 */
public class CancellationUtils {

    public static final long FULL_REFUND_CUTOFF_MS    = 24L * 60 * 60 * 1000;
    public static final long PARTIAL_REFUND_CUTOFF_MS = 12L * 60 * 60 * 1000;

    public static int calculateRefund(Date scheduledDate, int totalTokens) {
        if (scheduledDate == null) return totalTokens;
        long msUntil = scheduledDate.getTime() - System.currentTimeMillis();
        if (msUntil > FULL_REFUND_CUTOFF_MS)    return totalTokens;
        if (msUntil > PARTIAL_REFUND_CUTOFF_MS) return totalTokens / 2;
        return 0;
    }

    public static String refundDescription(Date scheduledDate, int totalTokens) {
        if (scheduledDate == null)
            return "Full refund (" + totalTokens + " tokens)";
        long msUntil = scheduledDate.getTime() - System.currentTimeMillis();
        if (msUntil > FULL_REFUND_CUTOFF_MS)
            return "Full refund (" + totalTokens + " tokens)\nMore than 24h before session.";
        if (msUntil > PARTIAL_REFUND_CUTOFF_MS)
            return "50% refund (" + (totalTokens / 2) + " tokens)\nBetween 12–24h before session.";
        return "No refund (0 tokens)\nLess than 12h before session.";
    }
}
