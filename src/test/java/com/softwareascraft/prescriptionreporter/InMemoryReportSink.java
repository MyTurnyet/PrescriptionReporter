package com.softwareascraft.prescriptionreporter;

import java.util.List;

class InMemoryReportSink implements ReportSink {
    private List<String> written = List.of();

    @Override
    public void write(List<String> lines) {
        this.written = lines;
    }

    List<String> written() {
        return written;
    }
}
