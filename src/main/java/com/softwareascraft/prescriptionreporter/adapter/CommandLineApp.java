package com.softwareascraft.prescriptionreporter.adapter;

import com.softwareascraft.prescriptionreporter.application.EventParser;
import com.softwareascraft.prescriptionreporter.application.EventSource;
import com.softwareascraft.prescriptionreporter.application.ReportApplication;
import com.softwareascraft.prescriptionreporter.application.ReportFormatter;
import com.softwareascraft.prescriptionreporter.application.ReportSink;
import com.softwareascraft.prescriptionreporter.application.Warnings;
import com.softwareascraft.prescriptionreporter.domain.IncomeCalculator;

import java.io.InputStream;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;

/**
 * The command line: decides where input comes from, and turns failures into exit codes.
 *
 * <p><b>Streams.</b> Standard output carries the report and nothing else, so it can be redirected
 * or piped and still be a clean report. Usage text, discard warnings, and error messages all go to
 * standard error.
 *
 * <p><b>Exit codes.</b> They are part of the contract:
 * <ul>
 *   <li>{@code 0} — a report was produced <em>and written</em>, warnings or not</li>
 *   <li>{@code 1} — the input could not be read, or the report could not be written</li>
 *   <li>{@code 2} — the command line itself was wrong</li>
 * </ul>
 */
public final class CommandLineApp {
    static final int EXIT_OK = 0;
    static final int EXIT_IO_FAILURE = 1;
    static final int EXIT_USAGE = 2;

    private static final String PROGRAM = "PrescriptionReporter";
    private static final List<String> HELP_FLAGS = List.of("--help", "-h");

    private final List<String> args;
    private final Warnings warnings;
    private final ReportSink sink;
    private final PrintStream diagnostics;
    private final InputStream stdin;

    public CommandLineApp(String[] args) {
        this(args, Streams.stderr());
    }

    /**
     * Warnings and diagnostics share one stream. Two {@link PrintStream}s over the same file
     * descriptor each buffer independently, so their output can interleave mid-line.
     */
    private CommandLineApp(String[] args, PrintStream stderr) {
        this(args, new StderrWarnings(stderr), new StdoutReportSink(), stderr, System.in);
    }

    CommandLineApp(String[] args, Warnings warnings, ReportSink sink, PrintStream diagnostics,
                   InputStream stdin) {
        this.args = List.of(args);
        this.warnings = warnings;
        this.sink = sink;
        this.diagnostics = diagnostics;
        this.stdin = stdin;
    }

    public int run() {
        if (args.stream().anyMatch(HELP_FLAGS::contains)) {
            printUsage();
            return EXIT_OK;
        }
        if (args.size() > 1) {
            return usageError("expected at most one file argument, got " + args.size());
        }
        try {
            new ReportApplication(eventSource(), new EventParser(), new IncomeCalculator(),
                    new ReportFormatter(), sink, warnings).run();
            return EXIT_OK;
        } catch (BadUsage e) {
            return usageError(e.getMessage());
        } catch (UncheckedIOException e) {
            diagnostics.println(PROGRAM + ": " + e.getMessage());
            return EXIT_IO_FAILURE;
        }
    }

    private EventSource eventSource() {
        if (args.isEmpty()) {
            return new StdinEventSource(stdin);
        }
        return new FileEventSource(pathOf(args.get(0)));
    }

    /**
     * {@code Path.of} rejects strings a platform cannot express as a path by throwing
     * {@link InvalidPathException}, which is not an {@link java.io.IOException} and so escaped the
     * I/O handler entirely — the user got a stack trace. It is a bad argument, so it is reported
     * as one.
     */
    private static Path pathOf(String argument) {
        try {
            return Path.of(argument);
        } catch (InvalidPathException e) {
            throw new BadUsage("not a usable file path: " + argument);
        }
    }

    private int usageError(String problem) {
        diagnostics.println(PROGRAM + ": " + problem);
        printUsage();
        return EXIT_USAGE;
    }

    private void printUsage() {
        diagnostics.println("usage: " + PROGRAM + " [<file>]");
        diagnostics.println();
        diagnostics.println("Reads prescription events and writes one report line per patient.");
        diagnostics.println("Reads the named file, or standard input when no file is given.");
        diagnostics.println("Input lines are: PatientName DrugName created|filled|returned");
        diagnostics.println();
        diagnostics.println("The report goes to standard output; warnings go to standard error.");
        diagnostics.println("Exit codes: 0 report written, 1 input/output failure, 2 usage error.");
    }

    /** A bad command line, raised from deeper in the wiring so it can be reported as exit 2. */
    private static final class BadUsage extends RuntimeException {
        private static final long serialVersionUID = 1L;

        BadUsage(String message) {
            super(message);
        }
    }
}
