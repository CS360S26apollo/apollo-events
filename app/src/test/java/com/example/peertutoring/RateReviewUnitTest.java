package com.example.peertutoring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for User Story 19: Rate & Review After Session.
 *
 * Covers:
 *  - Rating range validation (only 1–5 accepted)
 *  - Average rating recalculation logic
 *  - "Only verified student" gate (completed session required)
 *  - Rounding of average to 1 decimal place
 *  - Edge cases: single review, all same rating, mixed ratings
 */
public class RateReviewUnitTest {

    // ── Rating validation ──────────────────────────────────────

    @Test
    public void testZeroStarsIsInvalid() {
        assertFalse("0 stars must be rejected", isValidRating(0));
    }

    @Test
    public void testOneStarIsValid() {
        assertTrue("1 star must be accepted", isValidRating(1));
    }

    @Test
    public void testFiveStarsIsValid() {
        assertTrue("5 stars must be accepted", isValidRating(5));
    }

    @Test
    public void testSixStarsIsInvalid() {
        assertFalse("6 stars must be rejected", isValidRating(6));
    }

    @Test
    public void testNegativeRatingIsInvalid() {
        assertFalse("Negative rating must be rejected", isValidRating(-1));
    }

    // ── Average rating calculation ─────────────────────────────

    @Test
    public void testSingleReviewAverageEqualsSelf() {
        double avg = computeAverage(new double[]{4.0});
        assertEquals("Average of a single 4-star review is 4.0", 4.0, avg, 0.01);
    }

    @Test
    public void testTwoReviewsAverageIsCorrect() {
        double avg = computeAverage(new double[]{4.0, 5.0});
        assertEquals("Average of 4 and 5 stars is 4.5", 4.5, avg, 0.01);
    }

    @Test
    public void testThreeReviewsAverageRoundsToOneDecimal() {
        // (4 + 5 + 3) / 3 = 4.0
        double avg = computeAverage(new double[]{4.0, 5.0, 3.0});
        assertEquals("Average of 4, 5, 3 should be 4.0", 4.0, avg, 0.01);
    }

    @Test
    public void testMixedRatingsAverageRoundsCorrectly() {
        // (5 + 4 + 3 + 2 + 1) / 5 = 3.0
        double avg = computeAverage(new double[]{5.0, 4.0, 3.0, 2.0, 1.0});
        assertEquals("Average of 1–5 stars is 3.0", 3.0, avg, 0.01);
    }

    @Test
    public void testAllFiveStarAverageIsFive() {
        double avg = computeAverage(new double[]{5.0, 5.0, 5.0});
        assertEquals("All 5-star reviews average to 5.0", 5.0, avg, 0.01);
    }

    @Test
    public void testRatingAverageRoundedToOneDecimal() {
        // (4 + 5 + 4) / 3 = 4.333... → rounds to 4.3
        double avg = computeAverage(new double[]{4.0, 5.0, 4.0});
        assertEquals("4.333 should round to 4.3", 4.3, avg, 0.01);
    }

    // ── "Only verified student" gate ──────────────────────────

    @Test
    public void testStudentWithCompletedSessionCanReview() {
        assertTrue("Student with a completed session is eligible to review",
                canStudentReview("completed", false));
    }

    @Test
    public void testStudentWithRequestedSessionCannotReview() {
        assertFalse("Student with only a requested session cannot review",
                canStudentReview("requested", false));
    }

    @Test
    public void testStudentWithBookedSessionCannotReview() {
        assertFalse("Student with only a booked (not yet completed) session cannot review",
                canStudentReview("booked", false));
    }

    @Test
    public void testStudentWhoAlreadyReviewedCannotReviewAgain() {
        assertFalse("Student who already submitted a review cannot submit again",
                canStudentReview("completed", true));
    }

    @Test
    public void testStudentWithCancelledSessionCannotReview() {
        assertFalse("Student with a cancelled session cannot review",
                canStudentReview("cancelled", false));
    }

    // ── Helpers ───────────────────────────────────────────────

    private boolean isValidRating(int rating) {
        return rating >= 1 && rating <= 5;
    }

    /** Mirrors the average calculation in RateReviewActivity.updateTutorAverageRating(). */
    private double computeAverage(double[] ratings) {
        if (ratings.length == 0) return 0;
        double sum = 0;
        for (double r : ratings) sum += r;
        return Math.round((sum / ratings.length) * 10.0) / 10.0;
    }

    /** Mirrors the gate check in RequestDetailActivity.setupRateReviewButton(). */
    private boolean canStudentReview(String sessionStatus, boolean alreadyReviewed) {
        return "completed".equals(sessionStatus) && !alreadyReviewed;
    }
}
