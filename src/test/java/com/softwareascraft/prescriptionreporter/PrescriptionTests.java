package com.softwareascraft.prescriptionreporter;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

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
        final Prescription returnedPrescription = filledPrescription.returnIt();
        assertThat(returnedPrescription.fillCount()).isZero();
        assertThat(returnedPrescription.returnCount()).isEqualTo(1);
    }
    @Test
    void prescriptionTwiceAndThenReturnedHasOneFills() {
        final Prescription filledPrescription = prescription.fillIt().fillIt();
        assertThat(filledPrescription.fillCount()).isEqualTo(2);
        final Prescription returnedPrescription = filledPrescription.returnIt();
        assertThat(returnedPrescription.fillCount()).isEqualTo(1);
    }

    @Test
    void returningAPrescriptionThatWasNeverFilledGoesNegative() {
        final Prescription returnedPrescription = prescription.returnIt();
        assertThat(returnedPrescription.fillCount()).isEqualTo(-1);
        assertThat(returnedPrescription.returnCount()).isEqualTo(1);
    }
}