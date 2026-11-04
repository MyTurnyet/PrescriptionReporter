package com.softwareascraft.prescriptionreporter;

public interface PrescriptionEvent {
    Ledger applyTo(Ledger ledger);
}
