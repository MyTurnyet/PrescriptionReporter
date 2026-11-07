package com.softwareascraft.prescriptionreporter;

import java.util.List;

/**
 * Captures the report instead of writing it.
 *
 * <p>Fails loudly on a second write rather than silently replacing the first — a fake that loses
 * a call hides the bug it was meant to catch.
 */
class InMemoryReportSink implements ReportSink {
    private List<String> written;

    @Override
    public void write(List<String> lines) {
        if (written != null) {
            throw new IllegalStateException("write() called twice; the first report would be lost");
        }
        this.written = List.copyOf(lines);
    }

    List<String> written() {
        return written == null ? List.of() : written;
    }

    boolean wasWritten() {
        return written != null;
    }
}
