package com.softwareascraft.prescriptionreporter;

public record Returned(PrescriptionId id) implements PrescriptionEvent {
    @Override
    public Ledger applyTo(Ledger ledger) {
        return ledger.returnOne(id);
    }
}
