package com.softwareascraft.prescriptionreporter;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


@Tag("Unit")
public class IncomeCalculatorTests {
        final Prescription prescription = Prescription.create("User Name", "Drug Name");
    @Test
    void CalculatorReturnsZeroForUnfulfilledPrescription() {
        final IncomeCalculator incomeCalculator = new IncomeCalculator();
        final int income = incomeCalculator.incomeFor(prescription);
        assertThat(income).isEqualTo(0);
    }
    @Test
    void IncomeForPrescriptionFilledOnceIsFive() {
        final Prescription fillPrescription = prescription.fillIt();
        final int totalAmount = new IncomeCalculator().incomeFor(fillPrescription);
        assertThat(totalAmount).isEqualTo(5);
    }

    @Test
    void IncomeForPrescriptionFilledTwiceIsTen() {
        final Prescription fillPrescription = prescription.fillIt().fillIt();
        final int totalAmount = new IncomeCalculator().incomeFor(fillPrescription);
        assertThat(totalAmount).isEqualTo(10);
    }

    @Test
    void prescriptionFilledThenReturnedLosesOneDollar() {
        final Prescription returnedPrescription = prescription.fillIt().returnIt();
        assertThat(new IncomeCalculator().incomeFor(returnedPrescription)).isEqualTo(-1);
    }
}