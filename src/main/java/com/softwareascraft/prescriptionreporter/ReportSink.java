package com.softwareascraft.prescriptionreporter;

import java.util.List;

public interface ReportSink {
    void write(List<String> lines);
}
