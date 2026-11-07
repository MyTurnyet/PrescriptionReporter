package com.softwareascraft.prescriptionreporter.application;

import com.softwareascraft.prescriptionreporter.domain.DiscardReason;
import com.softwareascraft.prescriptionreporter.domain.IncomeCalculator;
import com.softwareascraft.prescriptionreporter.domain.PrescriptionId;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.UncheckedIOException;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("Unit")
class ReportApplicationTests {
    private final RecordingWarnings warnings = new RecordingWarnings();
    private final InMemoryReportSink sink = new InMemoryReportSink();

    private ReportApplication appReading(EventSource source) {
        return new ReportApplication(source, new EventParser(), new IncomeCalculator(),
                new ReportFormatter(), sink, warnings);
    }

    private ReportApplication appReading(List<String> lines) {
        return appReading(new InMemoryEventSource(lines));
    }

    @Test
    void emptyInputProducesAnEmptyReport() {
        appReading(List.of()).run();

        assertThat(sink.written()).isEmpty();
        assertThat(warnings.warnings()).isEmpty();
    }

    @Test
    void theBriefsSampleProducesTheExpectedReport() {
        appReading(BriefSample.LINES).run();

        assertThat(sink.written()).containsExactlyElementsOf(BriefSample.REPORT);
    }

    @Test
    void theBriefsSampleWarnsOnceForEachOfItsTwoOrphanedEvents() {
        appReading(BriefSample.LINES).run();

        assertThat(warnings.reasons()).containsExactly(
                new DiscardReason.NoCreatedPrescriptionToFill(new PrescriptionId("Mark", "C")),
                new DiscardReason.NoCreatedPrescriptionToFill(new PrescriptionId("Paul", "D")));
    }

    @Test
    void badLinesAreSkippedAndProcessingContinues() {
        appReading(List.of("Mark B created", "not a valid line", "Mark B filled")).run();

        assertThat(sink.written()).containsExactly("Mark: 1 fills $5 income");
        assertThat(warnings.reasons()).containsExactly(new DiscardReason.MalformedLine("not a valid line"));
    }

    /**
     * The point of making parsing and the ledger report outcomes upward: the loop is the only
     * thing that knows the line number, so it is the thing that attaches it.
     */
    @Test
    void aWarningNamesTheSourceAndTheOneBasedLineNumberItCameFrom() {
        appReading(List.of("Mark B created", "", "Mark B filled", "Mark B dispensed")).run();

        assertThat(warnings.locations()).containsExactly(
                new SourceLocation("<memory>", 2),
                new SourceLocation("<memory>", 4));
    }

    @Test
    void lineNumbersCountEveryLineIncludingTheOnesThatWereAccepted() {
        appReading(List.of("a", "b", "c", "Mark B created", "d")).run();

        assertThat(warnings.locations()).extracting(SourceLocation::lineNumber)
                .containsExactly(1, 2, 3, 5);
    }

    @Test
    void theInputStreamIsClosedAfterTheReportIsWritten() {
        final InMemoryEventSource source = new InMemoryEventSource(BriefSample.LINES);

        appReading(source).run();

        assertThat(source.wasClosed()).isTrue();
    }

    /**
     * A read that fails midway must not emit a partial report — a truncated report that looks
     * complete is worse than none.
     */
    @Test
    void aFailureMidwayThroughTheInputWritesNoReportAtAll() {
        final ReportApplication app = appReading(new FailingMidwayEventSource());

        assertThatThrownBy(app::run).isInstanceOf(UncheckedIOException.class);

        assertThat(sink.wasWritten()).isFalse();
    }

    @Test
    void aFailureMidwayThroughTheInputStillClosesTheStream() {
        final FailingMidwayEventSource source = new FailingMidwayEventSource();

        assertThatThrownBy(() -> appReading(source).run()).isInstanceOf(UncheckedIOException.class);

        assertThat(source.wasClosed()).isTrue();
    }

    /** Yields two good lines, then fails, the way a truncated or unreadable file would. */
    private static final class FailingMidwayEventSource implements EventSource {
        private boolean closed;

        @Override
        public String description() {
            return "<failing>";
        }

        @Override
        public Stream<String> lines() {
            return Stream.of("Mark B created", "Mark B filled", "boom")
                    .map(line -> {
                        if (line.equals("boom")) {
                            throw new UncheckedIOException(
                                    "could not read <failing>: simulated midway failure",
                                    new java.io.IOException("simulated"));
                        }
                        return line;
                    })
                    .onClose(() -> closed = true);
        }

        boolean wasClosed() {
            return closed;
        }
    }
}
