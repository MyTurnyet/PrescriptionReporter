package com.softwareascraft.prescriptionreporter;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("Unit")
public class EventParserTests {
    final RecordingWarnings warnings = new RecordingWarnings();
    final EventParser parser = new EventParser(warnings);
    final Ledger ledger = new Ledger(new IncomeCalculator(), new RecordingWarnings());

    @Test
    void parsesACreatedEvent() {
        final Ledger result = parser.applyLine(ledger, "Mark B created");
        assertThat(result.summaries()).containsExactly(new PatientSummary("Mark", 0, 0));
    }

    @Test
    void parsesAFilledEvent() {
        final Ledger created = parser.applyLine(ledger, "Mark B created");
        final Ledger result = parser.applyLine(created, "Mark B filled");
        assertThat(result.summaries()).containsExactly(new PatientSummary("Mark", 1, 5));
    }

    @Test
    void parsesAReturnedEvent() {
        final Ledger filled = parser.applyLine(parser.applyLine(ledger, "Mark B created"), "Mark B filled");
        final Ledger result = parser.applyLine(filled, "Mark B returned");
        assertThat(result.summaries()).containsExactly(new PatientSummary("Mark", 0, -1));
    }

    @Test
    void eventNameMatchingIsCaseInsensitive() {
        final Ledger created = parser.applyLine(ledger, "Mark B CREATED");
        assertThat(created.summaries()).containsExactly(new PatientSummary("Mark", 0, 0));
        final Ledger filled = parser.applyLine(created, "Mark B Filled");
        assertThat(filled.summaries()).containsExactly(new PatientSummary("Mark", 1, 5));
        final Ledger returned = parser.applyLine(filled, "Mark B ReTuRnEd");
        assertThat(returned.summaries()).containsExactly(new PatientSummary("Mark", 0, -1));
    }

    @Test
    void patientAndDrugNamesAreNotCaseFolded() {
        final Ledger result = parser.applyLine(ledger, "mark B created");
        assertThat(result.summaries()).containsExactly(new PatientSummary("mark", 0, 0));
    }

    @Test
    void multipleSpacesBetweenFieldsAreCollapsed() {
        final Ledger result = parser.applyLine(ledger, "Mark  B   created");
        assertThat(result.summaries()).containsExactly(new PatientSummary("Mark", 0, 0));
    }

    @Test
    void blankLineIsSkippedWithWarning() {
        final Ledger result = parser.applyLine(ledger, "");
        assertThat(result.summaries()).isEmpty();
        assertThat(warnings.messages()).hasSize(1);
    }

    @Test
    void whitespaceOnlyLineIsTreatedAsBlank() {
        final Ledger result = parser.applyLine(ledger, "   ");
        assertThat(result.summaries()).isEmpty();
        assertThat(warnings.messages()).hasSize(1);
    }

    @Test
    void lineWithWrongFieldCountIsSkippedWithWarning() {
        Ledger result = parser.applyLine(ledger, "created");
        result = parser.applyLine(result, "Mark created");
        result = parser.applyLine(result, "Mark B extra created");
        assertThat(result.summaries()).isEmpty();
        assertThat(warnings.messages()).hasSize(3);
    }

    @Test
    void unknownEventNameIsSkippedWithWarning() {
        final Ledger result = parser.applyLine(ledger, "Mark B dispensed");
        assertThat(result.summaries()).isEmpty();
        assertThat(warnings.messages()).hasSize(1);
    }

    @Test
    void crlfLineEndingIsStrippedBeforeParsing() {
        final Ledger result = parser.applyLine(ledger, "Mark B created\r");
        assertThat(result.summaries()).containsExactly(new PatientSummary("Mark", 0, 0));
    }

    @Test
    void trailingWhitespaceIsStrippedBeforeParsing() {
        final Ledger result = parser.applyLine(ledger, "Mark B created \n");
        assertThat(result.summaries()).containsExactly(new PatientSummary("Mark", 0, 0));
    }
}
