package com.softwareascraft.prescriptionreporter;

public record Created(PrescriptionId id) implements PrescriptionEvent {
    @Override
    public Ledger applyTo(Ledger ledger) {
        return ledger.create(id);
    }
}
