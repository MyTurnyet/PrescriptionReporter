package com.softwareascraft.prescriptionreporter;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("Unit")
public class ReportApplicationTests {
    final RecordingWarnings warnings = new RecordingWarnings();
    final InMemoryReportSink sink = new InMemoryReportSink();

    private ReportApplication appFor(List<String> lines) {
        return new ReportApplication(
                new InMemoryEventSource(lines),
                new EventParser(warnings),
                new Ledger(new IncomeCalculator(), warnings),
                new ReportFormatter(),
                sink);
    }

    @Test
    void emptyInputProducesAnEmptyReport() {
        appFor(List.of()).run();
        assertThat(sink.written()).isEmpty();
    }

    @Test
    void theSampleInputProducesTheExpectedAlphabeticalReport() {
        appFor(List.of(
                "Nick A created",
                "Mark B created",
                "Mark B filled",
                "Mark C filled",
                "Mark B returned",
                "John E created",
                "Mark B filled",
                "Mark B filled",
                "Paul D filled",
                "John E filled",
                "John E returned"
        )).run();

        assertThat(sink.written()).containsExactly(
                "John: 0 fills -$1 income",
                "Mark: 2 fills $9 income",
                "Nick: 0 fills $0 income");
    }

    @Test
    void malformedLinesAreSkippedWithWarningsAndDoNotStopProcessing() {
        appFor(List.of(
                "Mark B created",
                "not a valid line",
                "Mark B filled"
        )).run();

        assertThat(sink.written()).containsExactly("Mark: 1 fills $5 income");
        assertThat(warnings.messages()).hasSize(1);
    }
}
