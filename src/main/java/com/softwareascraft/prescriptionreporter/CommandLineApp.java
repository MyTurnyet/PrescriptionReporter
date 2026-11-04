package com.softwareascraft.prescriptionreporter;

import java.io.UncheckedIOException;
import java.nio.file.Path;

public class CommandLineApp {
    private final String[] args;
    private final Warnings warnings;
    private final ReportSink sink;

    public CommandLineApp(String[] args) {
        this(args, new StderrWarnings(), new StdoutReportSink());
    }

    CommandLineApp(String[] args, Warnings warnings, ReportSink sink) {
        this.args = args;
        this.warnings = warnings;
        this.sink = sink;
    }

    public int run() {
        try {
            application().run();
            return 0;
        } catch (UncheckedIOException e) {
            System.err.println("Could not read input: " + e.getMessage());
            return 1;
        }
    }

    private ReportApplication application() {
        final Ledger ledger = new Ledger(new IncomeCalculator(), warnings);
        return new ReportApplication(eventSource(), new EventParser(warnings), ledger, new ReportFormatter(), sink);
    }

    private EventSource eventSource() {
        return args.length == 0 ? new StdinEventSource() : new FileEventSource(Path.of(args[0]));
    }
}
