package com.softwareascraft.prescriptionreporter;

public class IncomeCalculator {

    public IncomeCalculator() {
    }

    public int incomeFor(Prescription prescription) {
        return prescription.fillCount() * 5;
    }
}
