package com.softwareascraft.prescriptionreporter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

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
        return prescriptions.stream()
                .collect(Collectors.groupingBy(Prescription::userName, LinkedHashMap::new, Collectors.toList()))
                .entrySet().stream()
                .map(entry -> new PatientSummary(
                        entry.getKey(),
                        entry.getValue().stream().mapToInt(Prescription::fillCount).sum(),
                        entry.getValue().stream().mapToInt(this.incomeCalculator::incomeFor).sum()))
                .toList();
    }

    public Ledger register(Prescription prescription) {
        List<Prescription> updated = new ArrayList<>(prescriptions);
        updated.add(prescription);
        return new Ledger(this.incomeCalculator, List.copyOf(updated));
    }
}
