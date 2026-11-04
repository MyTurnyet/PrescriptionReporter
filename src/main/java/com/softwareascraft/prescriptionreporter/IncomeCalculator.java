package com.softwareascraft.prescriptionreporter;

public class IncomeCalculator {

    public int incomeFor(Prescription prescription) {
        final int filledTotal = prescription.fillCount() * 5;
        final int returnTotal = prescription.returnCount();
        return filledTotal - returnTotal;
    }
}
