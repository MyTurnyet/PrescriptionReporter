package com.softwareascraft.prescriptionreporter;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


@Tag("Unit")
public class PrescriptionTests {
    @Test
    void newPrescriptionHasNoFills() {
        final String userName = "User Name";
        final String drugName = "Drug Name";
        final Prescription prescription = new Prescription(userName, drugName, 0);
        assertThat(prescription.fillCount()).isZero();
    }
    @Test
    void prescriptionFilledOnceHasOneFill() {
        final String userName = "User Name";
        final String drugName = "Drug Name";
        final Prescription prescription = new Prescription(userName, drugName, 0);
        final Prescription filledPrescription = prescription.fill();
        assertThat(filledPrescription.fillCount()).isEqualTo(1);
    }
}