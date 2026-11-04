package com.softwareascraft.prescriptionreporter;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


@Tag("Unit")
public class LedgerTests {
    final PrescriptionId markB = new PrescriptionId("Mark", "B");
    final PrescriptionId markC = new PrescriptionId("Mark", "C");
    final RecordingWarnings warnings = new RecordingWarnings();
    final Ledger ledger = new Ledger(new IncomeCalculator(), warnings);

    @Test
    void newLedgerHasNoPatientSummaries() {
        assertThat(ledger.summaries()).isEmpty();
    }

    @Test
    void creatingAPrescriptionAddsOnePatientSummary() {
        final Ledger ledgerWithOne = ledger.create(markB);
        assertThat(ledgerWithOne.summaries()).hasSize(1);
    }

    @Test
    void createdPrescriptionIsSummarizedWithZeroFillsAndZeroIncome() {
        final Ledger ledgerWithOne = ledger.create(markB);
        assertThat(ledgerWithOne.summaries()).containsExactly(new PatientSummary("Mark", 0, 0));
    }

    @Test
    void fillingACreatedPrescriptionAddsOneFillAndFiveDollars() {
        final Ledger filled = ledger.create(markB).fill(markB);
        assertThat(filled.summaries()).containsExactly(new PatientSummary("Mark", 1, 5));
    }

    @Test
    void prescriptionsForTheSamePatientAreSummedIntoOneSummary() {
        final Ledger result = ledger.create(markB).fill(markB)
                .create(markC).fill(markC);

        assertThat(result.summaries()).containsExactly(new PatientSummary("Mark", 2, 10));
    }

    @Test
    void differentPatientsGetSeparateSummaries() {
        final PrescriptionId johnE = new PrescriptionId("John", "E");
        final Ledger result = ledger.create(markB).fill(markB)
                .create(johnE).fill(johnE).fill(johnE);

        assertThat(result.summaries()).containsExactlyInAnyOrder(
                new PatientSummary("Mark", 1, 5),
                new PatientSummary("John", 2, 10));
    }

    @Test
    void fillingAnUnknownPrescriptionIsIgnoredAndWarned() {
        final Ledger result = ledger.fill(markB);
        assertThat(result.summaries()).isEmpty();
        assertThat(warnings.messages()).hasSize(1);
    }

    @Test
    void discardedFillForOnePrescriptionDoesNotAffectAnotherPatient() {
        final PrescriptionId johnE = new PrescriptionId("John", "E");
        final Ledger result = ledger.create(johnE).fill(johnE).fill(markB);

        assertThat(result.summaries()).containsExactly(new PatientSummary("John", 1, 5));
        assertThat(warnings.messages()).hasSize(1);
    }

    @Test
    void returningWithNoValidPriorFillIsIgnoredAndWarned() {
        final Ledger result = ledger.create(markB).returnOne(markB);
        assertThat(result.summaries()).containsExactly(new PatientSummary("Mark", 0, 0));
        assertThat(warnings.messages()).hasSize(1);
    }

    @Test
    void returningAnUnknownPrescriptionIsIgnoredAndWarned() {
        final Ledger result = ledger.returnOne(markB);
        assertThat(result.summaries()).isEmpty();
        assertThat(warnings.messages()).hasSize(1);
    }

    @Test
    void fillThenReturnThenReturnAgainKeepsNetFillsAtZero() {
        final Ledger result = ledger.create(markB).fill(markB).returnOne(markB).returnOne(markB);
        assertThat(result.summaries()).containsExactly(new PatientSummary("Mark", 0, -1));
        assertThat(warnings.messages()).hasSize(1);
    }

    @Test
    void duplicateCreatedEventIsIgnoredAndWarnedAndKeepsPriorFills() {
        final Ledger result = ledger.create(markB).fill(markB).create(markB);
        assertThat(result.summaries()).containsExactly(new PatientSummary("Mark", 1, 5));
        assertThat(warnings.messages()).hasSize(1);
    }

    @Test
    void summariesAreOrderedByFirstCreation() {
        final PrescriptionId johnE = new PrescriptionId("John", "E");
        final Ledger result = ledger.create(markB).create(johnE);

        assertThat(result.summaries()).extracting(PatientSummary::userName)
                .containsExactly("Mark", "John");
    }
}
