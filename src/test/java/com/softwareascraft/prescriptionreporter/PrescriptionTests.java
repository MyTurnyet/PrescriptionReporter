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
        final Prescription prescription = new Prescription(userName, drugName);
assertThat(prescription.fillCount()).isZero();

    }
}