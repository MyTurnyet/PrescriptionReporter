package com.softwareascraft.prescriptionreporter;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("Unit")
public class PrescriptionEventTests {
    final PrescriptionId markB = new PrescriptionId("Mark", "B");
    final Ledger ledger = new Ledger(new IncomeCalculator(), new RecordingWarnings());

    @Test
    void createdEventCreatesAPrescription() {
        final PrescriptionEvent event = new Created(markB);
        final Ledger result = event.applyTo(ledger);
        assertThat(result.summaries()).containsExactly(new PatientSummary("Mark", 0, 0));
    }

    @Test
    void filledEventFillsAnExistingPrescription() {
        final Ledger created = ledger.create(markB);
        final PrescriptionEvent event = new Filled(markB);
        final Ledger result = event.applyTo(created);
        assertThat(result.summaries()).containsExactly(new PatientSummary("Mark", 1, 5));
    }

    @Test
    void returnedEventReturnsAFilledPrescription() {
        final Ledger filled = ledger.create(markB).fill(markB);
        final PrescriptionEvent event = new Returned(markB);
        final Ledger result = event.applyTo(filled);
        assertThat(result.summaries()).containsExactly(new PatientSummary("Mark", 0, -1));
    }
}
