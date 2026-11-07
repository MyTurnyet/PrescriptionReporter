package com.softwareascraft.prescriptionreporter.adapter;

import com.softwareascraft.prescriptionreporter.application.ReportSink;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The sink is handed its stream, so these tests exercise the real adapter without swapping
 * {@code System.out}.
 */
@Tag("Functional")
class StdoutReportSinkTests {
    private static final List<String> REPORT =
            List.of("Mark: 2 fills $9 income", "Nick: 0 fills $0 income");

    private final ByteArrayOutputStream captured = new ByteArrayOutputStream();

    private StdoutReportSink sinkWriting(OutputStream target) {
        return new StdoutReportSink(new PrintStream(target, true, StandardCharsets.UTF_8));
    }

    @Test
    void writesEachLineInOrderSeparatedByNewlines() {
        sinkWriting(captured).write(REPORT);

        assertThat(captured.toString(StandardCharsets.UTF_8)).isEqualTo(
                "Mark: 2 fills $9 income" + System.lineSeparator()
                        + "Nick: 0 fills $0 income" + System.lineSeparator());
    }

    @Test
    void writesReportTextAsUtf8RegardlessOfThePlatformDefault() {
        sinkWriting(captured).write(List.of("José: 1 fills $5 income"));

        assertThat(captured.toByteArray())
                .isEqualTo(("José: 1 fills $5 income" + System.lineSeparator())
                        .getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void anEmptyReportWritesNothing() {
        sinkWriting(captured).write(List.of());

        assertThat(captured.toByteArray()).isEmpty();
    }

    /**
     * The defect this guards: {@link PrintStream} swallows {@link IOException} and only records a
     * flag. Unchecked, a broken pipe or full disk meant the process exited 0 having written no
     * report at all.
     */
    @Test
    void aFailedWriteIsReportedInsteadOfBeingSwallowed() {
        final StdoutReportSink sink = sinkWriting(alwaysFailing());

        assertThatThrownBy(() -> sink.write(REPORT))
                .isInstanceOf(UncheckedIOException.class)
                .hasMessageContaining("standard output");
    }

    @Test
    void aSuccessfulWriteDoesNotReportAFailure() {
        assertThatCode(() -> sinkWriting(captured).write(REPORT)).doesNotThrowAnyException();
    }

    private static OutputStream alwaysFailing() {
        return new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                throw new IOException("simulated downstream failure");
            }
        };
    }
}
