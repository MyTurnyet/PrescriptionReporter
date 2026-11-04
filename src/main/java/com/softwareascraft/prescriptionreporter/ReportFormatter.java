package com.softwareascraft.prescriptionreporter;

import java.util.Comparator;
import java.util.List;

public class ReportFormatter {
    public List<String> format(List<PatientSummary> summaries) {
        return summaries.stream()
                .sorted(Comparator.comparing(PatientSummary::patientName))
                .map(this::formatLine)
                .toList();
    }

    private String formatLine(PatientSummary summary) {
        return summary.patientName() + ": " + summary.fillCount() + " fills " + formatIncome(summary.totalAmount()) + " income";
    }

    private String formatIncome(int amount) {
        return amount < 0 ? "-$" + Math.abs(amount) : "$" + amount;
    }
}
