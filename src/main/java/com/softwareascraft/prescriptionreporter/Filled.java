package com.softwareascraft.prescriptionreporter;

public record Filled(PrescriptionId id) implements PrescriptionEvent {
    @Override
    public Ledger applyTo(Ledger ledger) {
        return ledger.fill(id);
    }
}
