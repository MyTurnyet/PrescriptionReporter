package com.softwareascraft.prescriptionreporter;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


@Tag("Unit")
public class PrescriptionTests {
        final String userName = "User Name";
        final String drugName = "Drug Name";
    @Test
    void newPrescriptionHasNoFills() {
        final Prescription prescription = Prescription.create(userName, drugName);
        assertThat(prescription.fillCount()).isZero();
    }
    @Test
    void prescriptionFilledOnceHasOneFill() {
        final Prescription prescription = Prescription.create(userName, drugName);
        final Prescription filledPrescription = prescription.fill();
        assertThat(filledPrescription.fillCount()).isEqualTo(1);
    }
    @Test
    void prescriptionFilledTwiceHasTwoFill() {
        final Prescription prescription = Prescription.create(userName, drugName);
        final Prescription filledPrescription = prescription.fill().fill();
        assertThat(filledPrescription.fillCount()).isEqualTo(2);
    }
}