package com.softwareascraft.prescriptionreporter;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("Unit")
public class StdoutReportSinkTests {
    @Test
    void writesEachLineToStandardOutput() {
        final PrintStream originalOut = System.out;
        final ByteArrayOutputStream captured = new ByteArrayOutputStream();
        System.setOut(new PrintStream(captured, true, StandardCharsets.UTF_8));
        try {
            new StdoutReportSink().write(List.of("Mark: 2 fills $9 income", "Nick: 0 fills $0 income"));
        } finally {
            System.setOut(originalOut);
        }

        assertThat(captured.toString(StandardCharsets.UTF_8)).isEqualTo(
                "Mark: 2 fills $9 income" + System.lineSeparator() + "Nick: 0 fills $0 income" + System.lineSeparator());
    }
}
