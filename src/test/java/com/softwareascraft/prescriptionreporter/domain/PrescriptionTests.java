package com.softwareascraft.prescriptionreporter.domain;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("Unit")
class PrescriptionTests {
    private static final String PATIENT = "Mark";
    private static final String DRUG = "B";

    private final Prescription prescription = new Prescription(PATIENT, DRUG);

    @Test
    void aNewPrescriptionHasNoFillsAndNoReturns() {
        assertThat(prescription.netFillCount()).isZero();
        assertThat(prescription.returnCount()).isZero();
        assertThat(prescription.patientName()).isEqualTo(PATIENT);
        assertThat(prescription.id()).isEqualTo(new PrescriptionId(PATIENT, DRUG));
    }

    @Test
    void eachFillAddsOneToTheNetCount() {
        assertThat(prescription.filled().netFillCount()).isEqualTo(1);
        assertThat(prescription.filled().filled().netFillCount()).isEqualTo(2);
    }

    @Test
    void aReturnCancelsExactlyOneFill() {
        final Prescription returned = prescription.filled().filled().returned().orElseThrow();

        assertThat(returned.netFillCount()).isEqualTo(1);
        assertThat(returned.returnCount()).isEqualTo(1);
    }

    @Test
    void returningTheOnlyFillLeavesNoNetFillsButRecordsTheReturn() {
        final Prescription returned = prescription.filled().returned().orElseThrow();

        assertThat(returned.netFillCount()).isZero();
        assertThat(returned.returnCount()).isEqualTo(1);
    }

    @Test
    void aPrescriptionThatWasNeverFilledRefusesAReturn() {
        assertThat(prescription.returned()).isEmpty();
    }

    @Test
    void aPrescriptionWithEveryFillAlreadyReturnedRefusesAFurtherReturn() {
        final Prescription allReturned = prescription.filled().returned().orElseThrow();

        final Optional<Prescription> again = allReturned.returned();

        assertThat(again).isEmpty();
    }

    @Test
    void fillingAndReturningLeaveTheOriginalUnchanged() {
        prescription.filled().filled().returned();

        assertThat(prescription.netFillCount()).isZero();
        assertThat(prescription.returnCount()).isZero();
    }
}
