package com.softwareascraft.prescriptionreporter;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


@Tag("Unit")
public class IncomeCalculatorTests {
    final Prescription prescription = new Prescription("User Name", "Drug Name");

    @Test
    void calculatorReturnsZeroForUnfulfilledPrescription() {
        final IncomeCalculator incomeCalculator = new IncomeCalculator();
        final int income = incomeCalculator.incomeFor(prescription);
        assertThat(income).isEqualTo(0);
    }

    @Test
    void incomeForPrescriptionFilledOnceIsFive() {
        final Prescription fillPrescription = prescription.fillIt();
        final int totalAmount = new IncomeCalculator().incomeFor(fillPrescription);
        assertThat(totalAmount).isEqualTo(5);
    }

    @Test
    void incomeForPrescriptionFilledTwiceIsTen() {
        final Prescription fillPrescription = prescription.fillIt().fillIt();
        final int totalAmount = new IncomeCalculator().incomeFor(fillPrescription);
        assertThat(totalAmount).isEqualTo(10);
    }

    @Test
    void prescriptionFilledThenReturnedLosesOneDollar() {
        final Prescription returnedPrescription = prescription.fillIt().returnIt().orElseThrow();
        assertThat(new IncomeCalculator().incomeFor(returnedPrescription)).isEqualTo(-1);
    }
    @Test
    void returnPenaltyIsKeptWhenPrescriptionIsFilledAgain() {
        final Prescription finalPrescription = prescription.fillIt().returnIt().orElseThrow().fillIt();

        assertThat(new IncomeCalculator().incomeFor(finalPrescription)).isEqualTo(4);
    }
}