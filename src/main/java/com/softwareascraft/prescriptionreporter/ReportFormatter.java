package com.softwareascraft.prescriptionreporter;

import java.util.Comparator;
import java.util.List;

/**
 * Renders summaries as report lines: {@code Mark: 2 fills $9 income}.
 *
 * <p>Negative income is written with the sign before the dollar sign ({@code -$1}), and "fills"
 * is always plural, both following the brief's example rather than its prose.
 *
 * <p>Sorted by patient name using {@link String}'s own ordering, which is case-sensitive: an
 * uppercase letter sorts before every lowercase one, so {@code Nick} precedes {@code mark}. That
 * is deterministic and is what the brief's own data implies; since patient names are never
 * normalised, changing it to case-insensitive or locale-aware ordering would be a product
 * decision, not a bug fix.
 */
public final class ReportFormatter {
    public List<String> format(List<PatientSummary> summaries) {
        return summaries.stream()
                .sorted(Comparator.comparing(PatientSummary::patientName))
                .map(ReportFormatter::formatLine)
                .toList();
    }

    private static String formatLine(PatientSummary summary) {
        return summary.patientName() + ": " + summary.netFillCount()
                + " fills " + formatIncome(summary.income()) + " income";
    }

    /**
     * Formats the sign explicitly rather than via {@code Math.abs}, which returns a negative
     * number for the minimum value and would emit a malformed figure.
     */
    private static String formatIncome(long income) {
        return income < 0 ? "-$" + negate(income) : "$" + income;
    }

    private static String negate(long negative) {
        return Long.toString(negative).substring(1);
    }
}
