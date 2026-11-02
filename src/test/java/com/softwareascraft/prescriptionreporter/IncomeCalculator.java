package com.softwareascraft.prescriptionreporter;

public class IncomeCalculator {

    public IncomeCalculator() {
    }

    public int incomeFor(Prescription prescription) {
        final int filledTotal = prescription.fillCount() * 5;
        final int returnTotal = prescription.returnCount() * -1;
        return filledTotal + returnTotal;
    }
}
