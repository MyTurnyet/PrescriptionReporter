package com.softwareascraft.prescriptionreporter.adapter;

import com.softwareascraft.prescriptionreporter.application.BriefSample;
import com.softwareascraft.prescriptionreporter.application.InMemoryReportSink;
import com.softwareascraft.prescriptionreporter.application.RecordingWarnings;
import com.softwareascraft.prescriptionreporter.application.ReportSink;
import com.softwareascraft.prescriptionreporter.domain.DiscardReason;
import com.softwareascraft.prescriptionreporter.domain.PrescriptionId;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * Runs the real {@link CommandLineApp} — real adapters, real parser, real ledger — against real
 * files in a {@code @TempDir}. Streams are injected rather than swapped globally, so these tests
 * mutate no shared state and are safe to run in parallel.
 */
@Tag("Functional")
class CommandLineAppTests {
    private static final String SAMPLE = "Mark B created\nMark B filled\nMark B filled\n";

    private static final String BRIEF_SAMPLE = BriefSample.text();

    @TempDir
    Path tempDir;

    private final RecordingWarnings warnings = new RecordingWarnings();
    private final InMemoryReportSink sink = new InMemoryReportSink();
    private final ByteArrayOutputStream diagnostics = new ByteArrayOutputStream();

    private int run(String[] args, String stdin) {
        return new CommandLineApp(args, warnings, sink,
                new PrintStream(diagnostics, true, StandardCharsets.UTF_8), bytesOf(stdin)).run();
    }

    private int runWithSink(String[] args, ReportSink replacementSink) {
        return new CommandLineApp(args, warnings, replacementSink,
                new PrintStream(diagnostics, true, StandardCharsets.UTF_8), bytesOf("")).run();
    }

    private int runFile(Path file) {
        return run(new String[]{file.toString()}, "");
    }

    private int runStdin(String input) {
        return run(new String[0], input);
    }

    private Path fileWith(String content) throws IOException {
        final Path file = tempDir.resolve("events.txt");
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }

    private String diagnosticsText() {
        return diagnostics.toString(StandardCharsets.UTF_8);
    }

    private static InputStream bytesOf(String input) {
        return new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
    }

    // --- the happy paths -------------------------------------------------------------------

    @Test
    void readsTheNamedFileAndExitsZero() throws IOException {
        assertThat(runFile(fileWith(SAMPLE))).isEqualTo(CommandLineApp.EXIT_OK);
        assertThat(sink.written()).containsExactly("Mark: 2 fills $10 income");
    }

    @Test
    void readsStandardInputWhenGivenNoArgumentAndExitsZero() {
        assertThat(runStdin(SAMPLE)).isEqualTo(CommandLineApp.EXIT_OK);
        assertThat(sink.written()).containsExactly("Mark: 2 fills $10 income");
    }

    @Test
    void theBriefsSampleProducesTheExpectedReportThroughARealFile() throws IOException {
        assertThat(runFile(fileWith(BRIEF_SAMPLE))).isEqualTo(CommandLineApp.EXIT_OK);
        assertThat(sink.written()).containsExactlyElementsOf(BriefSample.REPORT);
    }

    @Test
    void crlfLineEndingsProduceTheSameReport() throws IOException {
        runFile(fileWith(BRIEF_SAMPLE.replace("\n", "\r\n")));

        assertThat(sink.written()).containsExactlyElementsOf(BriefSample.REPORT);
    }

    @Test
    void aMissingTrailingNewlineStillParsesTheLastLine() throws IOException {
        runFile(fileWith(BRIEF_SAMPLE.stripTrailing()));

        assertThat(sink.written()).containsExactlyElementsOf(BriefSample.REPORT);
    }

    @Test
    void anEmptyFileProducesAnEmptyReportAndExitsZero() throws IOException {
        assertThat(runFile(fileWith(""))).isEqualTo(CommandLineApp.EXIT_OK);
        assertThat(sink.written()).isEmpty();
    }

    // --- encoding: the two input paths must agree -----------------------------------------

    /**
     * The regression test for the defect this suite previously certified as fixed while being
     * unable to detect it: every fixture was ASCII, so file and stdin agreed by accident. With a
     * non-ASCII name they disagreed, because the file decoded as UTF-8 and stdin as the platform
     * default.
     */
    @Test
    void aFileAndStandardInputProduceTheSameReportForNonAsciiNames() throws IOException {
        final String accented = "José B created\nJosé B filled\n";

        runFile(fileWith(accented));
        final List<String> fromFile = sink.written();

        final InMemoryReportSink stdinSink = new InMemoryReportSink();
        new CommandLineApp(new String[0], warnings, stdinSink,
                new PrintStream(diagnostics, true, StandardCharsets.UTF_8), bytesOf(accented)).run();

        assertThat(fromFile).containsExactly("José: 1 fills $5 income");
        assertThat(stdinSink.written()).isEqualTo(fromFile);
    }

    @Test
    void aFileAndStandardInputProduceTheSameReportForAsciiNames() throws IOException {
        runFile(fileWith(SAMPLE));
        final List<String> fromFile = sink.written();

        final InMemoryReportSink stdinSink = new InMemoryReportSink();
        new CommandLineApp(new String[0], warnings, stdinSink,
                new PrintStream(diagnostics, true, StandardCharsets.UTF_8), bytesOf(SAMPLE)).run();

        assertThat(stdinSink.written()).isEqualTo(fromFile);
    }

    // --- bad input: one case per discard reason -------------------------------------------

    private static Stream<Arguments> discardScenarios() {
        final PrescriptionId markB = new PrescriptionId("Mark", "B");
        return Stream.of(
                arguments("a fill with no created",
                        "Mark B filled\n",
                        List.of(),
                        new DiscardReason.NoCreatedPrescriptionToFill(markB)),
                arguments("a return with no created",
                        "Mark B returned\n",
                        List.of(),
                        new DiscardReason.NoValidPriorFillToReturn(markB)),
                arguments("a return with no fill left to cancel",
                        "Mark B created\nMark B returned\n",
                        List.of("Mark: 0 fills $0 income"),
                        new DiscardReason.NoValidPriorFillToReturn(markB)),
                arguments("a duplicate created",
                        "Mark B created\nMark B filled\nMark B created\n",
                        List.of("Mark: 1 fills $5 income"),
                        new DiscardReason.DuplicateCreated(markB)),
                arguments("a line with the wrong field count",
                        "Mark B created\nMark created\nMark B filled\n",
                        List.of("Mark: 1 fills $5 income"),
                        new DiscardReason.MalformedLine("Mark created")),
                arguments("an unknown event name",
                        "Mark B created\nMark B dispensed\nMark B filled\n",
                        List.of("Mark: 1 fills $5 income"),
                        new DiscardReason.UnknownEvent("Mark B dispensed")),
                arguments("a blank line",
                        "Mark B created\n\nMark B filled\n",
                        List.of("Mark: 1 fills $5 income"),
                        new DiscardReason.BlankLine()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("discardScenarios")
    void aBadEntryIsDiscardedWithItsOwnReasonAndTheRestStillReports(
            String scenario, String input, List<String> expectedReport, DiscardReason expectedReason)
            throws IOException {

        assertThat(runFile(fileWith(input))).isEqualTo(CommandLineApp.EXIT_OK);
        assertThat(sink.written()).containsExactlyElementsOf(expectedReport);
        assertThat(warnings.reasons()).containsExactly(expectedReason);
    }

    @Test
    void everyDiscardReasonVariantHasAnEndToEndScenario() {
        final int variants = DiscardReason.class.getPermittedSubclasses().length;

        assertThat(discardScenarios().count()).isGreaterThanOrEqualTo(variants);
    }

    @Test
    void aDiscardedEventIsNotReplayedWhenItsCreatedArrivesLater() throws IOException {
        runFile(fileWith("Mark B filled\nMark B created\n"));

        assertThat(sink.written()).containsExactly("Mark: 0 fills $0 income");
        assertThat(warnings.reasons())
                .containsExactly(new DiscardReason.NoCreatedPrescriptionToFill(new PrescriptionId("Mark", "B")));
    }

    // --- failures and exit codes -----------------------------------------------------------

    @Test
    void aMissingFileExitsOneWritesNoReportAndNamesTheFile() {
        final Path missing = tempDir.resolve("missing.txt");

        assertThat(runFile(missing)).isEqualTo(CommandLineApp.EXIT_IO_FAILURE);
        assertThat(sink.wasWritten()).isFalse();
        assertThat(diagnosticsText())
                .contains(missing.toString())
                .contains("no such file")
                .doesNotContain("NoSuchFileException");
    }

    /**
     * {@code Path.of} throws {@link java.nio.file.InvalidPathException}, which is not an
     * {@code IOException} and previously escaped the handler as a stack trace.
     */
    @Test
    void anUnusableFilePathIsReportedAsAUsageErrorRatherThanAStackTrace() {
        assertThat(run(new String[]{"bad\u0000path.txt"}, "")).isEqualTo(CommandLineApp.EXIT_USAGE);
        assertThat(sink.wasWritten()).isFalse();
        assertThat(diagnosticsText())
                .contains("not a usable file path")
                .doesNotContain("InvalidPathException");
    }

    /**
     * The defect: a swallowed write failure meant exit 0 with nothing delivered.
     */
    @Test
    void anUndeliverableReportExitsOneRatherThanClaimingSuccess() {
        final ReportSink failing = lines -> {
            throw new UncheckedIOException("could not write the report to standard output",
                    new IOException("simulated"));
        };

        assertThat(runWithSink(new String[0], failing)).isEqualTo(CommandLineApp.EXIT_IO_FAILURE);
        assertThat(diagnosticsText()).contains("standard output");
    }

    @Test
    void tooManyArgumentsIsAUsageErrorRatherThanBeingSilentlyIgnored() {
        assertThat(run(new String[]{"one.txt", "two.txt"}, "")).isEqualTo(CommandLineApp.EXIT_USAGE);
        assertThat(sink.wasWritten()).isFalse();
        assertThat(diagnosticsText()).contains("at most one file argument").contains("usage:");
    }

    @ParameterizedTest
    @ValueSource(strings = {"--help", "-h"})
    void askingForHelpPrintsUsageAndExitsZero(String flag) {
        assertThat(run(new String[]{flag}, "")).isEqualTo(CommandLineApp.EXIT_OK);
        assertThat(sink.wasWritten()).isFalse();
        assertThat(diagnosticsText())
                .contains("usage:")
                .contains("created|filled|returned")
                .contains("Exit codes:");
    }

    @Test
    void usageAndWarningsStayOffTheReportSoStdoutIsOnlyEverTheReport() throws IOException {
        runFile(fileWith("Mark B created\nrubbish\n"));

        assertThat(sink.written()).containsExactly("Mark: 0 fills $0 income");
        assertThat(warnings.reasons()).hasSize(1);
    }
}
