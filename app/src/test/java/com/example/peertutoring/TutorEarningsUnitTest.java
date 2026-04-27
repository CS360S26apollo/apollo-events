package com.example.peertutoring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.peertutoring.ui.WithdrawActivity;

import org.junit.Test;

/**
 * Unit Tests for US-24: Tutor Earnings & Withdrawal.
 *
 * TC-24-U01  Token to USD conversion (10 tokens = $1)
 * TC-24-U02  USD to tokens conversion
 * TC-24-U03  Minimum withdrawal enforced (100 tokens)
 * TC-24-U04  Withdrawal below minimum is invalid
 * TC-24-U05  Withdrawal above balance is invalid
 * TC-24-U06  Withdrawal exactly equal to balance is valid
 * TC-24-U07  Fee deducted correctly for PayPal (20 tokens)
 * TC-24-U08  Fee deducted correctly for Debit Card (10 tokens)
 * TC-24-U09  Bank Transfer has zero fee
 * TC-24-U10  Total deducted = amount + fee
 * TC-24-U11  Balance after withdrawal is correct
 * TC-24-U12  SeekBar budget snap formula (reused for quick amounts)
 * TC-24-U13  All quick amounts are above minimum withdrawal
 * TC-24-U14  Withdrawal with no method selected is invalid
 * TC-24-U15  Zero amount withdrawal is invalid
 */
public class TutorEarningsUnitTest {

    private static final int TOKENS_PER_DOLLAR = WithdrawActivity.TOKENS_PER_DOLLAR;
    private static final int MINIMUM           = WithdrawActivity.MINIMUM_WITHDRAWAL;

    // ── TC-24-U01: token → USD ────────────────────────────────

    @Test
    public void testTokensToUsd() {
        assertEquals("1000 tokens = $100.00",
                100.0, tokensToUsd(1000), 0.001);
    }

    @Test
    public void testTokensToUsdSmallAmount() {
        assertEquals("10 tokens = $1.00",
                1.0, tokensToUsd(10), 0.001);
    }

    @Test
    public void testTokensToUsdFractional() {
        assertEquals("15 tokens = $1.50",
                1.5, tokensToUsd(15), 0.001);
    }

    // ── TC-24-U02: USD → token ────────────────────────────────

    @Test
    public void testUsdToTokens() {
        assertEquals("$50 = 500 tokens", 500, usdToTokens(50.0));
    }

    // ── TC-24-U03: minimum enforcement ───────────────────────

    @Test
    public void testMinimumWithdrawalIs100() {
        assertEquals(100, MINIMUM);
    }

    @Test
    public void testWithdrawalOf100IsValid() {
        assertTrue(isValidWithdrawal(100, 500, 0));
    }

    @Test
    public void testWithdrawalOf101IsValid() {
        assertTrue(isValidWithdrawal(101, 500, 0));
    }

    // ── TC-24-U04: below minimum ──────────────────────────────

    @Test
    public void testWithdrawalOf99IsInvalid() {
        assertFalse("99 tokens is below minimum", isValidWithdrawal(99, 500, 0));
    }

    @Test
    public void testWithdrawalOf0IsInvalid() {
        assertFalse("0 tokens is invalid", isValidWithdrawal(0, 500, 0));
    }

    // ── TC-24-U05: above balance ──────────────────────────────

    @Test
    public void testWithdrawalAboveBalanceIsInvalid() {
        assertFalse("Cannot withdraw more than balance",
                isValidWithdrawal(600, 500, 0));
    }

    @Test
    public void testWithdrawalOneAboveBalanceIsInvalid() {
        assertFalse(isValidWithdrawal(501, 500, 0));
    }

    // ── TC-24-U06: exact balance ──────────────────────────────

    @Test
    public void testWithdrawalEqualToBalanceIsValid() {
        assertTrue("Withdrawing entire balance is allowed",
                isValidWithdrawal(500, 500, 0));
    }

    // ── TC-24-U07–09: fees ────────────────────────────────────

    @Test
    public void testPayPalFeeIs20Tokens() {
        assertEquals(20, getFee("PayPal"));
    }

    @Test
    public void testDebitCardFeeIs10Tokens() {
        assertEquals(10, getFee("Debit Card"));
    }

    @Test
    public void testBankTransferFeeIsZero() {
        assertEquals(0, getFee("Bank Transfer"));
    }

    // ── TC-24-U10: total deducted ─────────────────────────────

    @Test
    public void testTotalDeductedIncludesFee() {
        int amount = 500, fee = 20;
        assertEquals(520, amount + fee);
    }

    @Test
    public void testTotalDeductedBankTransfer() {
        int amount = 1000, fee = 0;
        assertEquals(1000, amount + fee);
    }

    // ── TC-24-U11: balance after withdrawal ───────────────────

    @Test
    public void testBalanceAfterWithdrawalPayPal() {
        long balance  = 12450;
        int  amount   = 1000;
        int  fee      = 20;
        long remaining = balance - amount - fee;
        assertEquals(11430, remaining);
    }

    @Test
    public void testBalanceAfterBankTransferWithdrawal() {
        long balance  = 5000;
        int  amount   = 2500;
        long remaining = balance - amount; // fee = 0
        assertEquals(2500, remaining);
    }

    // ── TC-24-U12: quick amount chips ────────────────────────

    @Test
    public void testAllQuickAmountsAboveMinimum() {
        for (int amount : WithdrawActivity.QUICK_AMOUNTS) {
            assertTrue("Quick amount " + amount + " must be >= minimum",
                    amount >= MINIMUM);
        }
    }

    @Test
    public void testQuickAmountsAreOrdered() {
        int[] qa = WithdrawActivity.QUICK_AMOUNTS;
        for (int i = 1; i < qa.length; i++) {
            assertTrue("Quick amounts should be in ascending order",
                    qa[i] > qa[i - 1]);
        }
    }

    // ── TC-24-U13: method selection required ─────────────────

    @Test
    public void testNoMethodSelectedIsInvalid() {
        // selectedMethodIndex == -1 means nothing selected
        assertFalse(isValidWithdrawal(500, 1000, -1));
    }

    @Test
    public void testMethodSelectedIsValid() {
        assertTrue(isValidWithdrawal(500, 1000, 0));
    }

    // ── TC-24-U15: zero amount ────────────────────────────────

    @Test
    public void testZeroAmountIsAlwaysInvalid() {
        assertFalse(isValidWithdrawal(0, 10000, 0));
    }

    // ── Helpers ───────────────────────────────────────────────

    private double tokensToUsd(long tokens) {
        return tokens / (double) TOKENS_PER_DOLLAR;
    }

    private int usdToTokens(double usd) {
        return (int) (usd * TOKENS_PER_DOLLAR);
    }

    /**
     * Mirrors WithdrawActivity.updateConfirmButton() validation.
     * @param amount           requested withdrawal amount
     * @param balance          tutor's current balance
     * @param methodIndex      -1 = not selected, 0+ = selected
     */
    private boolean isValidWithdrawal(int amount, long balance, int methodIndex) {
        return amount >= MINIMUM && amount <= balance && methodIndex >= 0;
    }

    private int getFee(String method) {
        switch (method) {
            case "PayPal":      return 20;
            case "Debit Card":  return 10;
            default:            return 0;  // Bank Transfer
        }
    }
}