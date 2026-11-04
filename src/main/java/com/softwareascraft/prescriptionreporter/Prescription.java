package com.softwareascraft.prescriptionreporter;

import java.util.Optional;

public class Prescription {
    private final PrescriptionId id;
    private final int timesFilled;
    private final int timesReturned;

    private Prescription(PrescriptionId id, int timesFilled, int timesReturned) {
        this.id = id;
        this.timesFilled = timesFilled;
        this.timesReturned = timesReturned;
    }

    public Prescription(String patientName, String drugName) {
        this(new PrescriptionId(patientName, drugName), 0, 0);
    }

    public PrescriptionId id() {
        return id;
    }

    public int fillCount() {
        return this.timesFilled - this.timesReturned;
    }

    public Prescription fillIt() {
        return new Prescription(this.id, this.timesFilled + 1, this.timesReturned);
    }

    public Optional<Prescription> returnIt() {
        if (fillCount() <= 0) {
            return Optional.empty();
        }
        return Optional.of(new Prescription(this.id, this.timesFilled, this.timesReturned + 1));
    }

    public int returnCount() {
        return this.timesReturned;
    }

    public String patientName() {
        return id.patientName();
    }
}
