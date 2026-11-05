package com.softwareascraft.prescriptionreporter;

import java.util.stream.Stream;

public class ReportApplication {
    private final EventSource source;
    private final EventParser parser;
    private final Ledger initialLedger;
    private final ReportFormatter formatter;
    private final ReportSink sink;

    public ReportApplication(EventSource source, EventParser parser, Ledger initialLedger,
                              ReportFormatter formatter, ReportSink sink) {
        this.source = source;
        this.parser = parser;
        this.initialLedger = initialLedger;
        this.formatter = formatter;
        this.sink = sink;
    }

    public void run() {
        sink.write(formatter.format(applyAllLines().summaries()));
    }

    private Ledger applyAllLines() {
        try (Stream<String> lines = source.lines()) {
            return lines.sequential().reduce(initialLedger, this::applyLine, (a, b) -> b);
        }
    }

    private Ledger applyLine(Ledger ledger, String line) {
        return parser.applyLine(ledger, line);
    }
}
