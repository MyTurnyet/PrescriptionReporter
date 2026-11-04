package com.softwareascraft.prescriptionreporter;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Ledger {
    private final Map<PrescriptionId, Prescription> prescriptions;
    private final IncomeCalculator incomeCalculator;
    private final Warnings warnings;

    private Ledger(IncomeCalculator incomeCalculator, Warnings warnings, Map<PrescriptionId, Prescription> prescriptions) {
        this.incomeCalculator = incomeCalculator;
        this.warnings = warnings;
        this.prescriptions = prescriptions;
    }

    public Ledger(IncomeCalculator incomeCalculator, Warnings warnings) {
        this(incomeCalculator, warnings, Map.of());
    }

    public Ledger() {
        this(new IncomeCalculator(), new StderrWarnings());
    }

    public Ledger create(PrescriptionId id) {
        if (prescriptions.containsKey(id)) {
            warnings.warn("duplicate created event ignored for " + id);
            return this;
        }
        return withPrescription(id, new Prescription(id.userName(), id.drugName()));
    }

    public Ledger fill(PrescriptionId id) {
        final Prescription existing = prescriptions.get(id);
        if (existing == null) {
            warnings.warn("filled event ignored, no created prescription for " + id);
            return this;
        }
        return withPrescription(id, existing.fillIt());
    }

    public Ledger returnOne(PrescriptionId id) {
        final Prescription existing = prescriptions.get(id);
        if (existing == null || existing.fillCount() <= 0) {
            warnings.warn("returned event ignored, no valid prior fill for " + id);
            return this;
        }
        return withPrescription(id, existing.returnIt());
    }

    public List<PatientSummary> summaries() {
        return byPatient().entrySet().stream()
                .map(entry -> summaryFor(entry.getKey(), entry.getValue()))
                .toList();
    }

    private Map<String, List<Prescription>> byPatient() {
        return prescriptions.values().stream()
                .collect(Collectors.groupingBy(Prescription::userName, LinkedHashMap::new, Collectors.toList()));
    }

    private PatientSummary summaryFor(String userName, List<Prescription> patientPrescriptions) {
        final int fillCount = patientPrescriptions.stream().mapToInt(Prescription::fillCount).sum();
        final int income = patientPrescriptions.stream().mapToInt(this.incomeCalculator::incomeFor).sum();
        return new PatientSummary(userName, fillCount, income);
    }

    private Ledger withPrescription(PrescriptionId id, Prescription prescription) {
        final Map<PrescriptionId, Prescription> updated = new LinkedHashMap<>(prescriptions);
        updated.put(id, prescription);
        return new Ledger(this.incomeCalculator, this.warnings, Collections.unmodifiableMap(updated));
    }
}
