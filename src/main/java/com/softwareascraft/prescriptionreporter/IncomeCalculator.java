package com.softwareascraft.prescriptionreporter;

/**
 * The only class that knows what a fill earns and what a return costs.
 *
 * <p>Income is {@code $5 × net fills − $1 × returns}: a return takes back the $5 the fill earned
 * <em>and</em> costs a dollar on top. The penalty is permanent — filling the same prescription
 * again later earns a fresh $5 but does not refund the dollar.
 *
 * <p>Money is whole dollars. Totals are {@code long} so that the arithmetic cannot overflow for
 * any input whose per-prescription counts fit in an {@code int}: the widest possible product is
 * {@code Integer.MAX_VALUE × 5}, which is about 1.1e10 and comfortably inside {@code long}.
 */
public final class IncomeCalculator {
    static final int FILL_REWARD_DOLLARS = 5;
    static final int RETURN_PENALTY_DOLLARS = 1;

    public long incomeFor(Prescription prescription) {
        final long fillReward = (long) prescription.netFillCount() * FILL_REWARD_DOLLARS;
        final long returnPenalty = (long) prescription.returnCount() * RETURN_PENALTY_DOLLARS;
        return fillReward - returnPenalty;
    }
}
