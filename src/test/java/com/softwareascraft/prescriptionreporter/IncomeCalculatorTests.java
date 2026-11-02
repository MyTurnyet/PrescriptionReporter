package com.softwareascraft.prescriptionreporter;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


@Tag("Unit")
public class IncomeCalculatorTests {
    @Test
    void CalculatorReturnsZeroForUnfulfilledPrescription() {
        final Prescription prescription = Prescription.create("User Name", "Drug Name");
        final IncomeCalculator incomeCalculator = new IncomeCalculator();
        final int income = incomeCalculator.incomeFor(prescription);
        assertThat(income).isEqualTo(0);
    }
}