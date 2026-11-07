package com.softwareascraft.prescriptionreporter;

import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * Writes the report to standard output as UTF-8 (see {@link Streams}).
 *
 * <p><b>Why the error check matters.</b> {@link PrintStream} swallows {@link IOException} by
 * design and records it in an internal flag. Without checking that flag, a broken pipe
 * ({@code | head}), a full disk, or a closed handle produced a silent success: the process exited
 * 0 having delivered no report at all. A zero exit status now means the report was written.
 */
public final class StdoutReportSink implements ReportSink {
    private final PrintStream out;

    public StdoutReportSink() {
        this(Streams.stdout());
    }

    StdoutReportSink(PrintStream out) {
        this.out = out;
    }

    /**
     * @throws UncheckedIOException if the report could not be written in full
     */
    @Override
    public void write(List<String> lines) {
        lines.forEach(out::println);
        out.flush();
        if (out.checkError()) {
            throw new UncheckedIOException(
                    "could not write the report to standard output",
                    new IOException("the output stream reported a write failure"));
        }
    }
}
