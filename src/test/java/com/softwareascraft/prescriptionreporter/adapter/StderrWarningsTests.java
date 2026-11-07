package com.softwareascraft.prescriptionreporter.adapter;

import com.softwareascraft.prescriptionreporter.application.SourceLocation;
import com.softwareascraft.prescriptionreporter.domain.DiscardReason;
import com.softwareascraft.prescriptionreporter.domain.PrescriptionId;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * The class that renders user-facing warning text. Previously untested, and its unguarded
 * fallback labelled any unhandled reason as a blank line.
 */
@Tag("Unit")
class StderrWarningsTests {
    private static final PrescriptionId MARK_B = new PrescriptionId("Mark", "B");

    private final ByteArrayOutputStream captured = new ByteArrayOutputStream();
    private final StderrWarnings warnings =
            new StderrWarnings(new PrintStream(captured, true, StandardCharsets.UTF_8));

    private static Stream<org.junit.jupiter.params.provider.Arguments> everyDiscardReason() {
        return Stream.of(
                arguments(new DiscardReason.DuplicateCreated(MARK_B), "duplicate created"),
                arguments(new DiscardReason.NoCreatedPrescriptionToFill(MARK_B), "no created prescription"),
                arguments(new DiscardReason.NoValidPriorFillToReturn(MARK_B), "no valid prior fill"),
                arguments(new DiscardReason.MalformedLine("Mark created"), "malformed line"),
                arguments(new DiscardReason.UnknownEvent("Mark B dispensed"), "unknown event"),
                arguments(new DiscardReason.BlankLine(), "blank line"));
    }

    /**
     * Every variant gets its own message. If a variant is added without a message, this fails —
     * and so does the production path, loudly, rather than mislabelling it.
     */
    @ParameterizedTest
    @MethodSource("everyDiscardReason")
    void rendersADistinctMessageForEveryDiscardReason(DiscardReason reason, String expectedPhrase) {
        assertThat(StderrWarnings.messageFor(reason)).contains(expectedPhrase);
    }

    /**
     * The real guard on exhaustiveness. {@code DiscardReason} is sealed, so the throwing fallback
     * in {@code messageFor} is unreachable while every permitted variant is handled — which is
     * exactly the property worth pinning. Add a variant without a message and this fails, naming
     * the count mismatch, instead of the variant being silently mislabelled at runtime.
     */
    @Test
    void everyPermittedDiscardReasonVariantHasACase() {
        final int variants = DiscardReason.class.getPermittedSubclasses().length;

        assertThat(everyDiscardReason().count())
                .as("one case per sealed DiscardReason variant")
                .isEqualTo(variants);
    }

    @Test
    void messagesAreDistinctFromEachOther() {
        final List<String> messages = everyDiscardReason()
                .map(args -> StderrWarnings.messageFor((DiscardReason) args.get()[0]))
                .toList();

        assertThat(messages).doesNotHaveDuplicates();
    }

    @Test
    void aWarningNamesTheSourceAndLineItCameFrom() {
        warnings.warn(new SourceLocation("events.txt", 42), new DiscardReason.BlankLine());

        assertThat(captured.toString(StandardCharsets.UTF_8))
                .startsWith("events.txt:42: ")
                .contains("blank line");
    }

    @Test
    void aWarningAboutAPrescriptionNamesThePatientAndDrug() {
        warnings.warn(new SourceLocation("<stdin>", 3), new DiscardReason.DuplicateCreated(MARK_B));

        assertThat(captured.toString(StandardCharsets.UTF_8))
                .startsWith("<stdin>:3: ")
                .contains("Mark B");
    }

    @Test
    void warningTextIsWrittenAsUtf8RegardlessOfThePlatformDefault() {
        final PrescriptionId accented = new PrescriptionId("José", "B");

        warnings.warn(new SourceLocation("events.txt", 1), new DiscardReason.DuplicateCreated(accented));

        assertThat(captured.toString(StandardCharsets.UTF_8)).contains("José");
    }
}
