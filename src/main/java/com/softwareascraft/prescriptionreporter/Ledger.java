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

    public Ledger create(PrescriptionId id) {
        if (prescriptions.containsKey(id)) {
            warnings.warn(new DiscardReason.DuplicateCreated(id));
            return this;
        }
        return withPrescription(new Prescription(id.patientName(), id.drugName()));
    }

    public Ledger fill(PrescriptionId id) {
        final Prescription existing = prescriptions.get(id);
        if (existing == null) {
            warnings.warn(new DiscardReason.NoCreatedPrescriptionToFill(id));
            return this;
        }
        return withPrescription(existing.fillIt());
    }

    public Ledger returnOne(PrescriptionId id) {
        final Prescription existing = prescriptions.get(id);
        if (existing == null) {
            return returnIgnored(id);
        }
        return existing.returnIt().map(this::withPrescription).orElseGet(() -> returnIgnored(id));
    }

    private Ledger returnIgnored(PrescriptionId id) {
        warnings.warn(new DiscardReason.NoValidPriorFillToReturn(id));
        return this;
    }

    public List<PatientSummary> summaries() {
        return byPatient().entrySet().stream()
                .map(entry -> summaryFor(entry.getKey(), entry.getValue()))
                .toList();
    }

    private Map<String, List<Prescription>> byPatient() {
        return prescriptions.values().stream()
                .collect(Collectors.groupingBy(Prescription::patientName, LinkedHashMap::new, Collectors.toList()));
    }

    private PatientSummary summaryFor(String patientName, List<Prescription> patientPrescriptions) {
        final int fillCount = patientPrescriptions.stream().mapToInt(Prescription::fillCount).sum();
        final int income = patientPrescriptions.stream().mapToInt(this.incomeCalculator::incomeFor).sum();
        return new PatientSummary(patientName, fillCount, income);
    }

    private Ledger withPrescription(Prescription prescription) {
        final Map<PrescriptionId, Prescription> updated = new LinkedHashMap<>(prescriptions);
        updated.put(prescription.id(), prescription);
        return new Ledger(this.incomeCalculator, this.warnings, Collections.unmodifiableMap(updated));
    }
}
