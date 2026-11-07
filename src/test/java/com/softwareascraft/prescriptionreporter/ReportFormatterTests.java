package com.softwareascraft.prescriptionreporter;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("Unit")
class ReportFormatterTests {
    private final ReportFormatter formatter = new ReportFormatter();

    private String formatOne(PatientSummary summary) {
        final List<String> lines = formatter.format(List.of(summary));
        assertThat(lines).hasSize(1);
        return lines.get(0);
    }

    @Test
    void formatsZeroIncome() {
        assertThat(formatOne(new PatientSummary("Nick", 0, 0))).isEqualTo("Nick: 0 fills $0 income");
    }

    @Test
    void formatsPositiveIncomeAndAlwaysPluralisesFills() {
        assertThat(formatOne(new PatientSummary("Mark", 1, 5))).isEqualTo("Mark: 1 fills $5 income");
    }

    @Test
    void formatsNegativeIncomeWithTheSignBeforeTheDollar() {
        assertThat(formatOne(new PatientSummary("John", 0, -1))).isEqualTo("John: 0 fills -$1 income");
    }

    @Test
    void sortsByPatientNameRegardlessOfInputOrder() {
        final List<String> lines = formatter.format(List.of(
                new PatientSummary("Mark", 2, 9),
                new PatientSummary("John", 0, -1),
                new PatientSummary("Nick", 0, 0)));

        assertThat(lines).containsExactly(
                "John: 0 fills -$1 income",
                "Mark: 2 fills $9 income",
                "Nick: 0 fills $0 income");
    }

    /**
     * Pins the documented ordering rule: {@link String}'s own comparison is case-sensitive, so
     * every uppercase initial sorts before every lowercase one. Deterministic, and a product
     * decision rather than a defect — this test exists so a change to it is deliberate.
     */
    @Test
    void sortsCaseSensitivelySoUppercaseInitialsComeFirst() {
        final List<String> lines = formatter.format(List.of(
                new PatientSummary("mark", 0, 0),
                new PatientSummary("Nick", 0, 0),
                new PatientSummary("Mark", 0, 0)));

        assertThat(lines).extracting(line -> line.substring(0, line.indexOf(':')))
                .containsExactly("Mark", "Nick", "mark");
    }

    /**
     * {@code Math.abs(Integer.MIN_VALUE)} is negative, which would have produced {@code -$-...}.
     * Income is a {@code long} and the sign is formatted without {@code abs}, so the extreme
     * values format correctly.
     */
    @Test
    void formatsExtremeIncomeValuesWithoutASignGlitch() {
        assertThat(formatOne(new PatientSummary("Low", 0, Integer.MIN_VALUE)))
                .isEqualTo("Low: 0 fills -$2147483648 income");
        assertThat(formatOne(new PatientSummary("High", 0, Long.MAX_VALUE)))
                .isEqualTo("High: 0 fills $9223372036854775807 income");
        assertThat(formatOne(new PatientSummary("Lowest", 0, Long.MIN_VALUE)))
                .isEqualTo("Lowest: 0 fills -$9223372036854775808 income");
    }

    @Test
    void anEmptyReportProducesNoLines() {
        assertThat(formatter.format(List.of())).isEmpty();
    }
}
