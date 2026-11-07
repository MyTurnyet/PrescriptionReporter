package com.softwareascraft.prescriptionreporter.domain;

import java.util.Optional;

/**
 * One prescription — a (patient, drug) pair — and its fill/return history.
 *
 * <p>Immutable. {@link #filled()} and {@link #returned()} hand back a new instance, so a
 * prescription value can never be observed half-updated. The "you cannot return a fill that was
 * never made" rule lives here, in the object that owns the counts, rather than in whoever happens
 * to call it: {@link #returned()} refuses the call by returning an empty {@link Optional}.
 */
public final class Prescription {
    private final PrescriptionId id;
    private final int timesFilled;
    private final int timesReturned;

    private Prescription(PrescriptionId id, int timesFilled, int timesReturned) {
        this.id = id;
        this.timesFilled = timesFilled;
        this.timesReturned = timesReturned;
    }

    public Prescription(PrescriptionId id) {
        this(id, 0, 0);
    }

    public Prescription(String patientName, String drugName) {
        this(new PrescriptionId(patientName, drugName));
    }

    public PrescriptionId id() {
        return id;
    }

    public String patientName() {
        return id.patientName();
    }

    /** Fills net of returns: what the report counts. Never negative, by construction. */
    public int netFillCount() {
        return timesFilled - timesReturned;
    }

    /** Returns made against this prescription, each of which carries a penalty. */
    public int returnCount() {
        return timesReturned;
    }

    public Prescription filled() {
        return new Prescription(id, timesFilled + 1, timesReturned);
    }

    /**
     * @return the updated prescription, or empty when there is no fill left to cancel
     */
    public Optional<Prescription> returned() {
        if (netFillCount() <= 0) {
            return Optional.empty();
        }
        return Optional.of(new Prescription(id, timesFilled, timesReturned + 1));
    }
}
