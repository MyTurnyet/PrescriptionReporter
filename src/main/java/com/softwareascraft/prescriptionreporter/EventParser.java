package com.softwareascraft.prescriptionreporter;

import java.util.Locale;

public class EventParser {
    private final Warnings warnings;

    public EventParser(Warnings warnings) {
        this.warnings = warnings;
    }

    public Ledger applyLine(Ledger ledger, String line) {
        final String trimmed = line.strip();
        if (trimmed.isEmpty()) {
            return rejectLine(ledger, "blank line ignored");
        }
        final String[] fields = trimmed.split(" +");
        return fields.length == 3 ? applyFields(ledger, fields, trimmed) : rejectLine(ledger, "malformed line ignored: " + trimmed);
    }

    private Ledger applyFields(Ledger ledger, String[] fields, String line) {
        final PrescriptionId id = new PrescriptionId(fields[0], fields[1]);
        return switch (fields[2].toLowerCase(Locale.ROOT)) {
            case "created" -> ledger.create(id);
            case "filled" -> ledger.fill(id);
            case "returned" -> ledger.returnOne(id);
            default -> rejectLine(ledger, "unknown event ignored: " + line);
        };
    }

    private Ledger rejectLine(Ledger ledger, String message) {
        warnings.warn(message);
        return ledger;
    }
}
