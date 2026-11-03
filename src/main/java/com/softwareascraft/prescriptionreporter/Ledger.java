package com.softwareascraft.prescriptionreporter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Ledger {
    private final List<Prescription> prescriptions;


    private Ledger(List<Prescription> prescriptions) {
        this.prescriptions = prescriptions;
    }

    public Ledger() {
        this(List.of());
    }

    public List<PatientSummary> summaries() {
        if(prescriptions.isEmpty()) {
            return List.of();
        }
        return List.of(new PatientSummary("User Name", 0, 0));

    }

    public Ledger register(Prescription prescription) {
        List<Prescription> updated = new ArrayList<>(prescriptions);
        updated.add(prescription);
        return new Ledger(List.copyOf(updated));
    }
}
