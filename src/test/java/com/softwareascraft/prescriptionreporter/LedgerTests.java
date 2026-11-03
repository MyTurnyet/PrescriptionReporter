package com.softwareascraft.prescriptionreporter;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;


@Tag("Unit")
public class LedgerTests {
    final Prescription prescription = Prescription.create("User Name", "Drug Name");

    @Test
    void newLedgerHasNoPatientSummaries() {
        final Ledger ledger = new Ledger();
        final List summaries = ledger.summaries();
        assertThat(summaries).isEmpty();
    }

    @Test
    void registeringAPrescriptionAddsOnePatientSummary() {
        final Ledger ledger = new Ledger();
        final Ledger ledgerWithOne = ledger.register(prescription);
        assertThat(ledgerWithOne.summaries()).hasSize(1);
    }
}