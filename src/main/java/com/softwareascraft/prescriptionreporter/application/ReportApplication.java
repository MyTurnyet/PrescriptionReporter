package com.softwareascraft.prescriptionreporter.application;

import com.softwareascraft.prescriptionreporter.domain.DiscardReason;
import com.softwareascraft.prescriptionreporter.domain.IncomeCalculator;
import com.softwareascraft.prescriptionreporter.domain.Ledger;

import java.util.Iterator;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Reads the input, folds it into a ledger, and writes the report.
 *
 * <p>This is the layer that owns the loop, and with it the three things only the loop knows: the
 * accumulator (a {@link Ledger} it creates per run and never shares), the line number, and where
 * discard notices go. {@link EventParser} and {@link Ledger} each report their outcome upward
 * instead of writing warnings themselves, which is what lets a warning name the line it came
 * from.
 *
 * <p>An earlier version folded the lines with a three-argument {@code Stream.reduce} whose
 * combiner was {@code (a, b) -> b}. That combiner never merged two ledgers, so the fold was
 * correct only because a defensive {@code .sequential()} forced sequential evaluation — remove
 * that one call and the report came out empty. An ordered loop states the requirement directly.
 *
 * <p>Input is streamed, not buffered: memory tracks the number of distinct prescriptions, not the
 * size of the input.
 */
public final class ReportApplication {
    private final EventSource source;
    private final EventParser parser;
    private final IncomeCalculator incomeCalculator;
    private final ReportFormatter formatter;
    private final ReportSink sink;
    private final Warnings warnings;

    public ReportApplication(EventSource source, EventParser parser, IncomeCalculator incomeCalculator,
                             ReportFormatter formatter, ReportSink sink, Warnings warnings) {
        this.source = source;
        this.parser = parser;
        this.incomeCalculator = incomeCalculator;
        this.formatter = formatter;
        this.sink = sink;
        this.warnings = warnings;
    }

    public void run() {
        sink.write(formatter.format(accumulate().summaries(incomeCalculator)));
    }

    private Ledger accumulate() {
        final Ledger ledger = new Ledger();
        try (Stream<String> lines = source.lines()) {
            int lineNumber = 0;
            for (Iterator<String> each = lines.iterator(); each.hasNext(); ) {
                final String line = each.next();
                apply(ledger, line, new SourceLocation(source.description(), ++lineNumber));
            }
        }
        return ledger;
    }

    private void apply(Ledger ledger, String line, SourceLocation where) {
        discardReasonFor(ledger, line).ifPresent(why -> warnings.warn(where, why));
    }

    private Optional<DiscardReason> discardReasonFor(Ledger ledger, String line) {
        final ParseResult parsed = parser.parse(line);
        if (parsed instanceof ParseResult.Rejected rejected) {
            return Optional.of(rejected.reason());
        }
        return ledger.apply(((ParseResult.Parsed) parsed).event());
    }
}
