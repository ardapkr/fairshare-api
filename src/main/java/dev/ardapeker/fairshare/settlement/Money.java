package dev.ardapeker.fairshare.settlement;

import java.math.BigDecimal;

/**
 * Converts between amounts the API shows ("12.50") and the whole cents the app calculates with.
 *
 * <p>Money is stored as {@code long} cents, never as {@code double}: 0.1 + 0.2 is not 0.3 in floating point,
 * and splitting a bill must add up to the exact cent.
 */
public final class Money {

    private Money() {
    }

    /** "12.50" → 1250. Rejects more than two decimal places instead of silently rounding. */
    public static long toCents(BigDecimal amount) {
        if (amount.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("Amount can have at most 2 decimal places: " + amount);
        }
        return amount.movePointRight(2).longValueExact();
    }

    /** 1250 → 12.50 (always two decimal places). */
    public static BigDecimal fromCents(long cents) {
        return BigDecimal.valueOf(cents, 2);
    }
}
