package com.softwareascraft.prescriptionreporter.application;

import com.softwareascraft.prescriptionreporter.domain.DiscardReason;
import com.softwareascraft.prescriptionreporter.domain.EventType;
import com.softwareascraft.prescriptionreporter.domain.ParsedEvent;
import com.softwareascraft.prescriptionreporter.domain.PrescriptionId;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Parsing is a pure function of the line, so these tests need no ledger and no warning recorder.
 */
@Tag("Unit")
class EventParserTests {
    private final EventParser parser = new EventParser();

    private ParsedEvent parseSuccessfully(String line) {
        final ParseResult result = parser.parse(line);
        assertThat(result).isInstanceOf(ParseResult.Parsed.class);
        return ((ParseResult.Parsed) result).event();
    }

    private DiscardReason rejectionFor(String line) {
        final ParseResult result = parser.parse(line);
        assertThat(result).isInstanceOf(ParseResult.Rejected.class);
        return ((ParseResult.Rejected) result).reason();
    }

    @ParameterizedTest
    @CsvSource({
            "Mark B created,  CREATED",
            "Mark B filled,   FILLED",
            "Mark B returned, RETURNED",
    })
    void parsesEachEventType(String line, EventType expected) {
        assertThat(parseSuccessfully(line))
                .isEqualTo(new ParsedEvent(new PrescriptionId("Mark", "B"), expected));
    }

    @ParameterizedTest
    @ValueSource(strings = {"CREATED", "created", "Created", "cReAtEd"})
    void theEventNameIsMatchedCaseInsensitively(String eventName) {
        assertThat(parseSuccessfully("Mark B " + eventName).type()).isEqualTo(EventType.CREATED);
    }

    @Test
    void patientAndDrugNamesKeepTheirCase() {
        assertThat(parseSuccessfully("mark b created").id())
                .isEqualTo(new PrescriptionId("mark", "b"));
    }

    @Test
    void runsOfSpacesBetweenFieldsCollapse() {
        assertThat(parseSuccessfully("Mark  B   created").id())
                .isEqualTo(new PrescriptionId("Mark", "B"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Mark B created\r", "Mark B created \n", "  Mark B created  "})
    void surroundingWhitespaceAndCrlfAreStrippedBeforeParsing(String line) {
        assertThat(parseSuccessfully(line).id()).isEqualTo(new PrescriptionId("Mark", "B"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\t", "\r"})
    void aBlankLineIsRejectedAsBlank(String line) {
        assertThat(rejectionFor(line)).isEqualTo(new DiscardReason.BlankLine());
    }

    @ParameterizedTest
    @ValueSource(strings = {"created", "Mark created", "Mark B extra created"})
    void aLineWithTheWrongFieldCountIsRejectedAsMalformed(String line) {
        assertThat(rejectionFor(line)).isEqualTo(new DiscardReason.MalformedLine(line));
    }

    @Test
    void anUnrecognisedEventNameIsRejectedAsUnknown() {
        assertThat(rejectionFor("Mark B dispensed"))
                .isEqualTo(new DiscardReason.UnknownEvent("Mark B dispensed"));
    }

    @Test
    void aRejectionReportsTheStrippedLineSoTheMessageHasNoStrayWhitespace() {
        assertThat(rejectionFor("  Mark B dispensed  "))
                .isEqualTo(new DiscardReason.UnknownEvent("Mark B dispensed"));
    }
}
