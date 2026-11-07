package com.softwareascraft.prescriptionreporter.domain;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("Unit")
class LedgerTests {
    private static final PrescriptionId MARK_B = new PrescriptionId("Mark", "B");
    private static final PrescriptionId MARK_C = new PrescriptionId("Mark", "C");
    private static final PrescriptionId JOHN_E = new PrescriptionId("John", "E");

    private final Ledger ledger = new Ledger();

    private Optional<DiscardReason> apply(PrescriptionId id, EventType type) {
        return ledger.apply(new ParsedEvent(id, type));
    }

    private void applyAccepted(PrescriptionId id, EventType type) {
        assertThat(apply(id, type)).isEmpty();
    }

    @Test
    void aNewLedgerHasNoSummaries() {
        assertThat(ledger.summaries(new IncomeCalculator())).isEmpty();
    }

    @Test
    void aCreatedPrescriptionIsSummarisedWithNoFillsAndNoIncome() {
        applyAccepted(MARK_B, EventType.CREATED);

        assertThat(ledger.summaries(new IncomeCalculator())).containsExactly(new PatientSummary("Mark", 0, 0));
    }

    @Test
    void fillingACreatedPrescriptionEarnsFiveDollars() {
        applyAccepted(MARK_B, EventType.CREATED);
        applyAccepted(MARK_B, EventType.FILLED);

        assertThat(ledger.summaries(new IncomeCalculator())).containsExactly(new PatientSummary("Mark", 1, 5));
    }

    @Test
    void prescriptionsForOnePatientAreSummedIntoOneSummary() {
        applyAccepted(MARK_B, EventType.CREATED);
        applyAccepted(MARK_B, EventType.FILLED);
        applyAccepted(MARK_C, EventType.CREATED);
        applyAccepted(MARK_C, EventType.FILLED);

        assertThat(ledger.summaries(new IncomeCalculator())).containsExactly(new PatientSummary("Mark", 2, 10));
    }

    @Test
    void differentPatientsGetSeparateSummaries() {
        applyAccepted(MARK_B, EventType.CREATED);
        applyAccepted(MARK_B, EventType.FILLED);
        applyAccepted(JOHN_E, EventType.CREATED);
        applyAccepted(JOHN_E, EventType.FILLED);
        applyAccepted(JOHN_E, EventType.FILLED);

        assertThat(ledger.summaries(new IncomeCalculator())).containsExactlyInAnyOrder(
                new PatientSummary("Mark", 1, 5),
                new PatientSummary("John", 2, 10));
    }

    @Test
    void summariesAreOrderedByFirstCreation() {
        applyAccepted(MARK_B, EventType.CREATED);
        applyAccepted(JOHN_E, EventType.CREATED);

        assertThat(ledger.summaries(new IncomeCalculator())).extracting(PatientSummary::patientName)
                .containsExactly("Mark", "John");
    }

    @Test
    void fillingAnUncreatedPrescriptionIsRefusedWithAReason() {
        assertThat(apply(MARK_B, EventType.FILLED))
                .contains(new DiscardReason.NoCreatedPrescriptionToFill(MARK_B));
        assertThat(ledger.summaries(new IncomeCalculator())).isEmpty();
    }

    @Test
    void returningAnUncreatedPrescriptionIsRefusedWithAReason() {
        assertThat(apply(MARK_B, EventType.RETURNED))
                .contains(new DiscardReason.NoValidPriorFillToReturn(MARK_B));
        assertThat(ledger.summaries(new IncomeCalculator())).isEmpty();
    }

    @Test
    void returningWithNoFillToCancelIsRefusedWithAReason() {
        applyAccepted(MARK_B, EventType.CREATED);

        assertThat(apply(MARK_B, EventType.RETURNED))
                .contains(new DiscardReason.NoValidPriorFillToReturn(MARK_B));
        assertThat(ledger.summaries(new IncomeCalculator())).containsExactly(new PatientSummary("Mark", 0, 0));
    }

    @Test
    void aSecondReturnWithNothingLeftToCancelIsRefusedAndChangesNothing() {
        applyAccepted(MARK_B, EventType.CREATED);
        applyAccepted(MARK_B, EventType.FILLED);
        applyAccepted(MARK_B, EventType.RETURNED);

        assertThat(apply(MARK_B, EventType.RETURNED))
                .contains(new DiscardReason.NoValidPriorFillToReturn(MARK_B));
        assertThat(ledger.summaries(new IncomeCalculator())).containsExactly(new PatientSummary("Mark", 0, -1));
    }

    @Test
    void aDuplicateCreatedIsRefusedAndKeepsTheFillsItAlreadyHad() {
        applyAccepted(MARK_B, EventType.CREATED);
        applyAccepted(MARK_B, EventType.FILLED);

        assertThat(apply(MARK_B, EventType.CREATED))
                .contains(new DiscardReason.DuplicateCreated(MARK_B));
        assertThat(ledger.summaries(new IncomeCalculator())).containsExactly(new PatientSummary("Mark", 1, 5));
    }

    @Test
    void aRefusedEventForOnePrescriptionLeavesTheOthersAlone() {
        applyAccepted(JOHN_E, EventType.CREATED);
        applyAccepted(JOHN_E, EventType.FILLED);

        assertThat(apply(MARK_B, EventType.FILLED)).isPresent();
        assertThat(ledger.summaries(new IncomeCalculator())).containsExactly(new PatientSummary("John", 1, 5));
    }

    /**
     * Returns are cancelled against the prescription they name, not pooled per patient — the
     * reason the ledger is keyed by (patient, drug) rather than by patient.
     */
    @Test
    void aReturnCannotCancelAFillOnTheSamePatientsOtherDrug() {
        applyAccepted(MARK_B, EventType.CREATED);
        applyAccepted(MARK_B, EventType.FILLED);
        applyAccepted(MARK_C, EventType.CREATED);

        assertThat(apply(MARK_C, EventType.RETURNED))
                .contains(new DiscardReason.NoValidPriorFillToReturn(MARK_C));
        assertThat(ledger.summaries(new IncomeCalculator())).containsExactly(new PatientSummary("Mark", 1, 5));
    }

    /**
     * The README's rule that discarded events are neither buffered nor retroactively applied.
     */
    @Test
    void anEventDiscardedBeforeCreationIsNotReplayedWhenTheCreatedArrivesLater() {
        assertThat(apply(MARK_B, EventType.FILLED)).isPresent();
        applyAccepted(MARK_B, EventType.CREATED);

        assertThat(ledger.summaries(new IncomeCalculator())).containsExactly(new PatientSummary("Mark", 0, 0));
    }

    /**
     * The ledger is a mutable accumulator, so what matters is that nothing it hands out aliases
     * its state: a summary list taken earlier must not change when more events arrive.
     */
    @Test
    void summariesAreASnapshotUnaffectedByLaterEvents() {
        applyAccepted(MARK_B, EventType.CREATED);
        final List<PatientSummary> before = ledger.summaries(new IncomeCalculator());

        applyAccepted(MARK_B, EventType.FILLED);

        assertThat(before).containsExactly(new PatientSummary("Mark", 0, 0));
        assertThat(ledger.summaries(new IncomeCalculator())).containsExactly(new PatientSummary("Mark", 1, 5));
    }
}
