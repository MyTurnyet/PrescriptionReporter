package com.softwareascraft.prescriptionreporter;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("Unit")
public class CommandLineAppTests {
    @TempDir
    Path tempDir;

    private static final String SAMPLE = "Mark B created\nMark B filled\nMark B filled\n";

    private static final String FULL_SAMPLE = String.join("\n",
            "Nick A created",
            "Mark B created",
            "Mark B filled",
            "Mark C filled",
            "Mark B returned",
            "John E created",
            "Mark B filled",
            "Mark B filled",
            "Paul D filled",
            "John E filled",
            "John E returned") + "\n";

    private static final List<String> FULL_SAMPLE_EXPECTED_REPORT = List.of(
            "John: 0 fills -$1 income",
            "Mark: 2 fills $9 income",
            "Nick: 0 fills $0 income");

    @Test
    void readsFromStdinWhenNoArgumentIsGivenAndReturnsExitCodeZero() {
        final InMemoryReportSink sink = new InMemoryReportSink();
        final InputStream originalIn = System.in;
        System.setIn(new ByteArrayInputStream(SAMPLE.getBytes(StandardCharsets.UTF_8)));
        int exitCode;
        try {
            exitCode = new CommandLineApp(new String[0], new RecordingWarnings(), sink).run();
        } finally {
            System.setIn(originalIn);
        }

        assertThat(exitCode).isZero();
        assertThat(sink.written()).containsExactly("Mark: 2 fills $10 income");
    }

    @Test
    void readsFromTheNamedFileWhenGivenAndReturnsExitCodeZero() throws IOException {
        final Path file = tempDir.resolve("events.txt");
        Files.writeString(file, SAMPLE);
        final InMemoryReportSink sink = new InMemoryReportSink();

        final int exitCode = new CommandLineApp(new String[]{file.toString()}, new RecordingWarnings(), sink).run();

        assertThat(exitCode).isZero();
        assertThat(sink.written()).containsExactly("Mark: 2 fills $10 income");
    }

    @Test
    void aMissingFileReturnsANonZeroExitCodeAndWritesNothing() {
        final InMemoryReportSink sink = new InMemoryReportSink();
        final Path missing = tempDir.resolve("missing.txt");

        final int exitCode = new CommandLineApp(new String[]{missing.toString()}, new RecordingWarnings(), sink).run();

        assertThat(exitCode).isNotZero();
        assertThat(sink.written()).isEmpty();
    }

    @Test
    void fileAndStdinProduceEquivalentOutputForTheSameContent() throws IOException {
        final Path file = tempDir.resolve("events.txt");
        Files.writeString(file, SAMPLE);
        final InMemoryReportSink fileSink = new InMemoryReportSink();
        new CommandLineApp(new String[]{file.toString()}, new RecordingWarnings(), fileSink).run();

        final InMemoryReportSink stdinSink = new InMemoryReportSink();
        final InputStream originalIn = System.in;
        System.setIn(new ByteArrayInputStream(SAMPLE.getBytes(StandardCharsets.UTF_8)));
        try {
            new CommandLineApp(new String[0], new RecordingWarnings(), stdinSink).run();
        } finally {
            System.setIn(originalIn);
        }

        assertThat(fileSink.written()).isEqualTo(stdinSink.written());
    }

    @Test
    void theFullSampleInputFromTheBriefProducesTheExpectedReportViaARealFile() throws IOException {
        final Path file = tempDir.resolve("events.txt");
        Files.writeString(file, FULL_SAMPLE);
        final InMemoryReportSink sink = new InMemoryReportSink();

        final int exitCode = new CommandLineApp(new String[]{file.toString()}, new RecordingWarnings(), sink).run();

        assertThat(exitCode).isZero();
        assertThat(sink.written()).containsExactlyElementsOf(FULL_SAMPLE_EXPECTED_REPORT);
    }

    @Test
    void theFullSampleWithCrlfLineEndingsProducesTheSameReport() throws IOException {
        final Path file = tempDir.resolve("events.txt");
        Files.writeString(file, FULL_SAMPLE.replace("\n", "\r\n"));
        final InMemoryReportSink sink = new InMemoryReportSink();

        new CommandLineApp(new String[]{file.toString()}, new RecordingWarnings(), sink).run();

        assertThat(sink.written()).containsExactlyElementsOf(FULL_SAMPLE_EXPECTED_REPORT);
    }

    @Test
    void aFileWithNoTrailingNewlineStillParsesTheLastLine() throws IOException {
        final Path file = tempDir.resolve("events.txt");
        Files.writeString(file, FULL_SAMPLE.stripTrailing());
        final InMemoryReportSink sink = new InMemoryReportSink();

        new CommandLineApp(new String[]{file.toString()}, new RecordingWarnings(), sink).run();

        assertThat(sink.written()).containsExactlyElementsOf(FULL_SAMPLE_EXPECTED_REPORT);
    }

    @Test
    void anEmptyFileProducesAnEmptyReportAndExitCodeZero() throws IOException {
        final Path file = tempDir.resolve("events.txt");
        Files.writeString(file, "");
        final InMemoryReportSink sink = new InMemoryReportSink();

        final int exitCode = new CommandLineApp(new String[]{file.toString()}, new RecordingWarnings(), sink).run();

        assertThat(exitCode).isZero();
        assertThat(sink.written()).isEmpty();
    }

    private int runFileThrough(String content, InMemoryReportSink sink, RecordingWarnings warnings) throws IOException {
        final Path file = tempDir.resolve("events.txt");
        Files.writeString(file, content);
        return new CommandLineApp(new String[]{file.toString()}, warnings, sink).run();
    }

    @Test
    void aFilledEventWithNoPriorCreatedIsDiscardedAndWarned() throws IOException {
        final InMemoryReportSink sink = new InMemoryReportSink();
        final RecordingWarnings warnings = new RecordingWarnings();
        final int exitCode = runFileThrough("Mark B filled\n", sink, warnings);

        assertThat(exitCode).isZero();
        assertThat(sink.written()).isEmpty();
        assertThat(warnings.messages()).hasSize(1);
    }

    @Test
    void aReturnedEventWithNoPriorCreatedIsDiscardedAndWarned() throws IOException {
        final InMemoryReportSink sink = new InMemoryReportSink();
        final RecordingWarnings warnings = new RecordingWarnings();
        final int exitCode = runFileThrough("Mark B returned\n", sink, warnings);

        assertThat(exitCode).isZero();
        assertThat(sink.written()).isEmpty();
        assertThat(warnings.messages()).hasSize(1);
    }

    @Test
    void aReturnedEventWithNoValidPriorFillIsDiscardedAndWarned() throws IOException {
        final InMemoryReportSink sink = new InMemoryReportSink();
        final RecordingWarnings warnings = new RecordingWarnings();
        runFileThrough("Mark B created\nMark B returned\n", sink, warnings);

        assertThat(sink.written()).containsExactly("Mark: 0 fills $0 income");
        assertThat(warnings.messages()).hasSize(1);
    }

    @Test
    void aDuplicateCreatedEventIsDiscardedAndWarned() throws IOException {
        final InMemoryReportSink sink = new InMemoryReportSink();
        final RecordingWarnings warnings = new RecordingWarnings();
        runFileThrough("Mark B created\nMark B filled\nMark B created\n", sink, warnings);

        assertThat(sink.written()).containsExactly("Mark: 1 fills $5 income");
        assertThat(warnings.messages()).hasSize(1);
    }

    @Test
    void aLineWithTheWrongFieldCountIsSkippedAndWarned() throws IOException {
        final InMemoryReportSink sink = new InMemoryReportSink();
        final RecordingWarnings warnings = new RecordingWarnings();
        runFileThrough("Mark B created\nMark created\nMark B filled\n", sink, warnings);

        assertThat(sink.written()).containsExactly("Mark: 1 fills $5 income");
        assertThat(warnings.messages()).hasSize(1);
    }

    @Test
    void anUnknownEventNameIsSkippedAndWarned() throws IOException {
        final InMemoryReportSink sink = new InMemoryReportSink();
        final RecordingWarnings warnings = new RecordingWarnings();
        runFileThrough("Mark B created\nMark B dispensed\nMark B filled\n", sink, warnings);

        assertThat(sink.written()).containsExactly("Mark: 1 fills $5 income");
        assertThat(warnings.messages()).hasSize(1);
    }

    @Test
    void aBlankLineIsSkippedAndWarned() throws IOException {
        final InMemoryReportSink sink = new InMemoryReportSink();
        final RecordingWarnings warnings = new RecordingWarnings();
        runFileThrough("Mark B created\n\nMark B filled\n", sink, warnings);

        assertThat(sink.written()).containsExactly("Mark: 1 fills $5 income");
        assertThat(warnings.messages()).hasSize(1);
    }
}
