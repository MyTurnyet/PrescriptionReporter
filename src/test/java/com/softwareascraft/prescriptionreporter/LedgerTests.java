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
        final List<PatientSummary> summaries = ledger.summaries();
        assertThat(summaries).isEmpty();
    }

    @Test
    void registeringAPrescriptionAddsOnePatientSummary() {
        final Ledger ledger = new Ledger();
        final Ledger ledgerWithOne = ledger.register(prescription);
        assertThat(ledgerWithOne.summaries()).hasSize(1);
    }

    @Test
    void registeredPrescriptionIsSummarizedWithZeroFillsAndZeroIncome() {
        Ledger ledger = new Ledger().register(prescription);

        final PatientSummary patientSummary = new PatientSummary("User Name", 0, 0);
        assertThat(ledger.summaries().size()).isEqualTo(1);
        assertThat(ledger.summaries().get(0)).isEqualTo(patientSummary);
    }
}