package com.softwareascraft.prescriptionreporter.application;

import java.io.UncheckedIOException;
import java.util.List;

/**
 * Where the finished report goes.
 */
public interface ReportSink {
    /**
     * Writes every line, in order.
     *
     * @throws UncheckedIOException if the report could not be written in full. Implementations
     *                              must not report success for a write that did not land —
     *                              the process's exit status depends on it.
     */
    void write(List<String> lines);
}
