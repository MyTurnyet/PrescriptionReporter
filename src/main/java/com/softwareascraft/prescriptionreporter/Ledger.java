package com.softwareascraft.prescriptionreporter;

import java.util.ArrayList;
import java.util.List;

public class Ledger {
    private final List<Prescription> prescriptions;
    private final IncomeCalculator incomeCalculator;


    private Ledger(IncomeCalculator incomeCalculator, List<Prescription> prescriptions) {
        this.prescriptions = prescriptions;
        this.incomeCalculator = incomeCalculator;
    }

    public Ledger(IncomeCalculator incomeCalculator) {
        this(incomeCalculator, List.of());
    }

    public Ledger() {
        this(new IncomeCalculator());
    }

    public List<PatientSummary> summaries() {
        if (prescriptions.isEmpty()) {
            return List.of();
        }
        return prescriptions.stream().map(currentPrescription ->
                new PatientSummary(
                        currentPrescription.userName(),
                        currentPrescription.fillCount(),
                        this.incomeCalculator.incomeFor(currentPrescription))).toList();

    }

    public Ledger register(Prescription prescription) {
        List<Prescription> updated = new ArrayList<>(prescriptions);
        updated.add(prescription);
        return new Ledger(this.incomeCalculator, List.copyOf(updated));
    }
}
