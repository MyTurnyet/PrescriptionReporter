package com.softwareascraft.prescriptionreporter;

import java.util.List;

public class StdoutReportSink implements ReportSink {
    @Override
    public void write(List<String> lines) {
        lines.forEach(System.out::println);
    }
}
