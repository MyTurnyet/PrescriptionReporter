package com.softwareascraft.prescriptionreporter;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("Unit")
public class ReportFormatterTests {
    final ReportFormatter formatter = new ReportFormatter();

    @Test
    void formatsAZeroIncomeSummary() {
        final List<String> lines = formatter.format(List.of(new PatientSummary("Nick", 0, 0)));
        assertThat(lines).containsExactly("Nick: 0 fills $0 income");
    }

    @Test
    void formatsAPositiveIncomeSummaryAlwaysUsingPluralFills() {
        final List<String> lines = formatter.format(List.of(new PatientSummary("Mark", 1, 5)));
        assertThat(lines).containsExactly("Mark: 1 fills $5 income");
    }

    @Test
    void formatsANegativeIncomeSummaryWithSignBeforeDollar() {
        final List<String> lines = formatter.format(List.of(new PatientSummary("John", 0, -1)));
        assertThat(lines).containsExactly("John: 0 fills -$1 income");
    }

    @Test
    void sortsSummariesAlphabeticallyByPatientNameRegardlessOfInputOrder() {
        final List<String> lines = formatter.format(List.of(
                new PatientSummary("Mark", 2, 9),
                new PatientSummary("John", 0, -1),
                new PatientSummary("Nick", 0, 0)));

        assertThat(lines).containsExactly(
                "John: 0 fills -$1 income",
                "Mark: 2 fills $9 income",
                "Nick: 0 fills $0 income");
    }
}
