package com.softwareascraft.prescriptionreporter;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("Unit")
public class EventParserTests {
    final RecordingWarnings warnings = new RecordingWarnings();
    final EventParser parser = new EventParser(warnings);
    final PrescriptionId markB = new PrescriptionId("Mark", "B");

    @Test
    void parsesACreatedEvent() {
        assertThat(parser.parse("Mark B created")).contains(new Created(markB));
    }

    @Test
    void parsesAFilledEvent() {
        assertThat(parser.parse("Mark B filled")).contains(new Filled(markB));
    }

    @Test
    void parsesAReturnedEvent() {
        assertThat(parser.parse("Mark B returned")).contains(new Returned(markB));
    }

    @Test
    void eventNameMatchingIsCaseInsensitive() {
        assertThat(parser.parse("Mark B CREATED")).contains(new Created(markB));
        assertThat(parser.parse("Mark B Filled")).contains(new Filled(markB));
    }

    @Test
    void blankLineIsSkippedWithWarning() {
        assertThat(parser.parse("")).isEqualTo(Optional.empty());
        assertThat(warnings.messages()).hasSize(1);
    }

    @Test
    void lineWithWrongFieldCountIsSkippedWithWarning() {
        assertThat(parser.parse("Mark created")).isEqualTo(Optional.empty());
        assertThat(parser.parse("Mark B extra created")).isEqualTo(Optional.empty());
        assertThat(warnings.messages()).hasSize(2);
    }

    @Test
    void unknownEventNameIsSkippedWithWarning() {
        assertThat(parser.parse("Mark B dispensed")).isEqualTo(Optional.empty());
        assertThat(warnings.messages()).hasSize(1);
    }

    @Test
    void crlfLineEndingIsStrippedBeforeParsing() {
        assertThat(parser.parse("Mark B created\r")).contains(new Created(markB));
    }

    @Test
    void trailingWhitespaceIsStrippedBeforeParsing() {
        assertThat(parser.parse("Mark B created \n")).contains(new Created(markB));
    }
}
