package com.softwareascraft.prescriptionreporter;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;


@Tag("Unit")
public class PrescriptionTests {
    final String patientName = "User Name";
    final String drugName = "Drug Name";
    final Prescription prescription = new Prescription(patientName, drugName);

    @Test
    void newPrescriptionHasNoFills() {
        assertThat(prescription.fillCount()).isZero();
        assertThat(prescription.returnCount()).isZero();
        assertThat(prescription.patientName()).isEqualTo(patientName);
    }
    @Test
    void prescriptionFilledOnceHasOneFill() {
        final Prescription filledPrescription = prescription.fillIt();
        assertThat(filledPrescription.fillCount()).isEqualTo(1);
    }
    @Test
    void prescriptionFilledTwiceHasTwoFill() {
        final Prescription filledPrescription = prescription.fillIt().fillIt();
        assertThat(filledPrescription.fillCount()).isEqualTo(2);
    }
    @Test
    void prescriptionFilledAndThenReturnedHasZeroFills() {
        final Prescription filledPrescription = prescription.fillIt();
        assertThat(filledPrescription.fillCount()).isEqualTo(1);
        final Optional<Prescription> returnedPrescription = filledPrescription.returnIt();
        assertThat(returnedPrescription).isPresent();
        assertThat(returnedPrescription.get().fillCount()).isZero();
        assertThat(returnedPrescription.get().returnCount()).isEqualTo(1);
    }
    @Test
    void prescriptionTwiceAndThenReturnedHasOneFills() {
        final Prescription filledPrescription = prescription.fillIt().fillIt();
        assertThat(filledPrescription.fillCount()).isEqualTo(2);
        final Optional<Prescription> returnedPrescription = filledPrescription.returnIt();
        assertThat(returnedPrescription).isPresent();
        assertThat(returnedPrescription.get().fillCount()).isEqualTo(1);
    }

    @Test
    void returningAValidFillReturnsPopulatedOptionalWithUpdatedCounts() {
        final Prescription filledTwice = prescription.fillIt().fillIt();
        final Optional<Prescription> returnedPrescription = filledTwice.returnIt();
        assertThat(returnedPrescription).isPresent();
        assertThat(returnedPrescription.get().fillCount()).isEqualTo(1);
        assertThat(returnedPrescription.get().returnCount()).isEqualTo(1);
    }

    @Test
    void returningAPrescriptionThatWasNeverFilledIsRefused() {
        final Optional<Prescription> returnedPrescription = prescription.returnIt();
        assertThat(returnedPrescription).isEmpty();
    }
}