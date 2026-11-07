package com.softwareascraft.prescriptionreporter;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("Unit")
class IncomeCalculatorTests {
    private final IncomeCalculator incomeCalculator = new IncomeCalculator();
    private final Prescription prescription = new Prescription("Mark", "B");

    @Test
    void anUnfilledPrescriptionEarnsNothing() {
        assertThat(incomeCalculator.incomeFor(prescription)).isZero();
    }

    @Test
    void eachFillEarnsFiveDollars() {
        assertThat(incomeCalculator.incomeFor(prescription.filled())).isEqualTo(5);
        assertThat(incomeCalculator.incomeFor(prescription.filled().filled())).isEqualTo(10);
    }

    @Test
    void aReturnTakesBackTheFiveDollarsAndCostsOneMore() {
        final Prescription returned = prescription.filled().returned().orElseThrow();

        assertThat(incomeCalculator.incomeFor(returned)).isEqualTo(-1);
    }

    @Test
    void theReturnPenaltyIsNotRefundedByFillingAgain() {
        final Prescription refilled = prescription.filled().returned().orElseThrow().filled();

        assertThat(incomeCalculator.incomeFor(refilled)).isEqualTo(4);
    }

    /**
     * Guards the documented bound: income is computed in {@code long}, so the widest product an
     * {@code int} fill count can produce cannot overflow.
     */
    @Test
    void incomeDoesNotOverflowForTheLargestPlausibleFillCount() {
        Prescription many = prescription;
        for (int i = 0; i < 1_000; i++) {
            many = many.filled();
        }

        assertThat(incomeCalculator.incomeFor(many)).isEqualTo(5_000L);
    }
}
