package com.example.peertutoring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for the Buy Tokens pricing logic (US: Token Purchase).
 *
 * Mirrors the private constants and formulas in BuyTokensActivity:
 *   TOKENS_PER_DOLLAR = 10
 *   PACKAGES = { {100, 1000, "Starter", 0}, {500, 4500, "Popular", 10},
 *                {1000, 8000, "Value", 20}, {2500, 17500, "Premium", 30} }
 *
 * TC-BT-U01  Base rate: 100 tokens cost $10.00 (no discount)
 * TC-BT-U02  Popular package: 500 tokens at 10% discount = $45.00
 * TC-BT-U03  Value package: 1000 tokens at 20% discount = $80.00
 * TC-BT-U04  Premium package: 2500 tokens at 30% discount = $175.00
 * TC-BT-U05  Discount makes each package cheaper than undiscounted base rate
 * TC-BT-U06  Token-to-USD conversion: tokensToUsd(100) = $10.00
 * TC-BT-U07  Token-to-USD conversion: tokensToUsd(500) = $50.00 (undiscounted)
 * TC-BT-U08  Package price is always less than or equal to undiscounted price
 * TC-BT-U09  All package token amounts are positive
 * TC-BT-U10  All package prices are positive
 * TC-BT-U11  Packages are in ascending token-count order
 * TC-BT-U12  Packages are in ascending price order
 * TC-BT-U13  Discount percentages are non-negative
 * TC-BT-U14  Starter package has 0% discount
 * TC-BT-U15  Premium package has the highest discount
 * TC-BT-U16  Custom amount: purchase validation (must be > 0)
 * TC-BT-U17  Cents-to-dollars formatting accuracy
 */
public class BuyTokensUnitTest {

    // Mirrors BuyTokensActivity constants
    private static final int TOKENS_PER_DOLLAR = 10;

    // {tokens, priceCents, discountPct}
    private static final int[][] PACKAGES = {
            {100,   1000,  0},
            {500,   4500, 10},
            {1000,  8000, 20},
            {2500, 17500, 30},
    };

    // ── TC-BT-U01 ─────────────────────────────────────────────

    @Test
    public void testStarterPackage100TokensCosts10Dollars() {
        // 100 tokens, no discount → 100/10 * 100 cents = 1000 cents = $10.00
        assertEquals("Starter: 100 tokens = $10.00", 1000, PACKAGES[0][1]);
        assertEquals(0, PACKAGES[0][2]); // no discount
    }

    // ── TC-BT-U02 ─────────────────────────────────────────────

    @Test
    public void testPopularPackage500TokensAt10PercentDiscount() {
        int tokens        = PACKAGES[1][0];
        int priceCents    = PACKAGES[1][1];
        int discountPct   = PACKAGES[1][2];

        assertEquals(500, tokens);
        assertEquals(10,  discountPct);

        // Undiscounted = 500/10 * 100 = 5000 cents; with 10% off = 4500 cents
        int undiscountedCents = (tokens / TOKENS_PER_DOLLAR) * 100;
        int expectedCents     = (int) (undiscountedCents * (1 - discountPct / 100.0));
        assertEquals("Popular: 500 tokens at 10% off = $45.00", expectedCents, priceCents);
    }

    // ── TC-BT-U03 ─────────────────────────────────────────────

    @Test
    public void testValuePackage1000TokensAt20PercentDiscount() {
        int tokens      = PACKAGES[2][0];
        int priceCents  = PACKAGES[2][1];
        int discountPct = PACKAGES[2][2];

        assertEquals(1000, tokens);
        assertEquals(20,   discountPct);

        int undiscountedCents = (tokens / TOKENS_PER_DOLLAR) * 100;
        int expectedCents     = (int) (undiscountedCents * (1 - discountPct / 100.0));
        assertEquals("Value: 1000 tokens at 20% off = $80.00", expectedCents, priceCents);
    }

    // ── TC-BT-U04 ─────────────────────────────────────────────

    @Test
    public void testPremiumPackage2500TokensAt30PercentDiscount() {
        int tokens      = PACKAGES[3][0];
        int priceCents  = PACKAGES[3][1];
        int discountPct = PACKAGES[3][2];

        assertEquals(2500, tokens);
        assertEquals(30,   discountPct);

        int undiscountedCents = (tokens / TOKENS_PER_DOLLAR) * 100;
        int expectedCents     = (int) (undiscountedCents * (1 - discountPct / 100.0));
        assertEquals("Premium: 2500 tokens at 30% off = $175.00", expectedCents, priceCents);
    }

    // ── TC-BT-U05 ─────────────────────────────────────────────

    @Test
    public void testDiscountedPriceIsCheaperThanUndiscounted() {
        for (int[] pkg : PACKAGES) {
            int tokens          = pkg[0];
            int priceCents      = pkg[1];
            int discountPct     = pkg[2];
            int undiscountedCents = (tokens / TOKENS_PER_DOLLAR) * 100;

            assertTrue("Package with " + tokens + " tokens must not cost more than undiscounted",
                    priceCents <= undiscountedCents);
        }
    }

    // ── TC-BT-U06 ─────────────────────────────────────────────

    @Test
    public void testTokensToUsd100() {
        assertEquals("100 tokens = $10.00", 10.0, tokensToUsd(100), 0.001);
    }

    // ── TC-BT-U07 ─────────────────────────────────────────────

    @Test
    public void testTokensToUsd500() {
        assertEquals("500 tokens = $50.00 (undiscounted)", 50.0, tokensToUsd(500), 0.001);
    }

    @Test
    public void testTokensToUsd1000() {
        assertEquals("1000 tokens = $100.00 (undiscounted)", 100.0, tokensToUsd(1000), 0.001);
    }

    @Test
    public void testTokensToUsd2500() {
        assertEquals("2500 tokens = $250.00 (undiscounted)", 250.0, tokensToUsd(2500), 0.001);
    }

    // ── TC-BT-U08 ─────────────────────────────────────────────

    @Test
    public void testActualPriceAlwaysLeOrEqualUndiscounted() {
        for (int[] pkg : PACKAGES) {
            double actualUsd       = pkg[1] / 100.0;
            double undiscountedUsd = tokensToUsd(pkg[0]);
            assertTrue("Package actual price must be ≤ undiscounted price",
                    actualUsd <= undiscountedUsd + 0.001);
        }
    }

    // ── TC-BT-U09 ─────────────────────────────────────────────

    @Test
    public void testAllPackageTokenAmountsArePositive() {
        for (int[] pkg : PACKAGES) {
            assertTrue("Token amount must be positive", pkg[0] > 0);
        }
    }

    // ── TC-BT-U10 ─────────────────────────────────────────────

    @Test
    public void testAllPackagePricesArePositive() {
        for (int[] pkg : PACKAGES) {
            assertTrue("Package price must be positive", pkg[1] > 0);
        }
    }

    // ── TC-BT-U11 ─────────────────────────────────────────────

    @Test
    public void testPackagesInAscendingTokenOrder() {
        for (int i = 1; i < PACKAGES.length; i++) {
            assertTrue("Package " + i + " must have more tokens than package " + (i - 1),
                    PACKAGES[i][0] > PACKAGES[i - 1][0]);
        }
    }

    // ── TC-BT-U12 ─────────────────────────────────────────────

    @Test
    public void testPackagesInAscendingPriceOrder() {
        for (int i = 1; i < PACKAGES.length; i++) {
            assertTrue("Package " + i + " must cost more than package " + (i - 1),
                    PACKAGES[i][1] > PACKAGES[i - 1][1]);
        }
    }

    // ── TC-BT-U13 ─────────────────────────────────────────────

    @Test
    public void testDiscountPercentagesAreNonNegative() {
        for (int[] pkg : PACKAGES) {
            assertTrue("Discount percentage must be ≥ 0", pkg[2] >= 0);
        }
    }

    // ── TC-BT-U14 ─────────────────────────────────────────────

    @Test
    public void testStarterPackageHasZeroDiscount() {
        assertEquals("Starter package must have 0% discount", 0, PACKAGES[0][2]);
    }

    // ── TC-BT-U15 ─────────────────────────────────────────────

    @Test
    public void testPremiumHasHighestDiscount() {
        int maxDiscount = 0;
        for (int[] pkg : PACKAGES) {
            if (pkg[2] > maxDiscount) maxDiscount = pkg[2];
        }
        assertEquals("Premium must have the highest discount",
                maxDiscount, PACKAGES[PACKAGES.length - 1][2]);
    }

    // ── TC-BT-U16: custom amount validation ──────────────────

    @Test
    public void testCustomAmountZeroIsInvalid() {
        assertFalse("Custom amount of 0 is invalid", isValidCustomAmount(0));
    }

    @Test
    public void testCustomAmountNegativeIsInvalid() {
        assertFalse("Negative custom amount is invalid", isValidCustomAmount(-50));
    }

    @Test
    public void testCustomAmountPositiveIsValid() {
        assertTrue("Positive custom amount is valid", isValidCustomAmount(1));
        assertTrue("Large custom amount is valid",  isValidCustomAmount(10000));
    }

    // ── TC-BT-U17: cents-to-dollar formatting ────────────────

    @Test
    public void testCentsToDollarFormattingStarterPackage() {
        // 1000 cents = $10.00
        String formatted = centsToDollars(1000);
        assertEquals("$10.00", formatted);
    }

    @Test
    public void testCentsToDollarFormattingPopularPackage() {
        String formatted = centsToDollars(4500);
        assertEquals("$45.00", formatted);
    }

    @Test
    public void testCentsToDollarFormattingPremiumPackage() {
        String formatted = centsToDollars(17500);
        assertEquals("$175.00", formatted);
    }

    // ── Helpers ───────────────────────────────────────────────

    private double tokensToUsd(int tokens) {
        return tokens / (double) TOKENS_PER_DOLLAR;
    }

    private boolean isValidCustomAmount(int amount) {
        return amount > 0;
    }

    private String centsToDollars(int cents) {
        return String.format("$%.2f", cents / 100.0);
    }
}
