package com.softwareascraft.prescriptionreporter;

import java.util.Optional;

public class EventParser {
    private final Warnings warnings;

    public EventParser(Warnings warnings) {
        this.warnings = warnings;
    }

    public Optional<PrescriptionEvent> parse(String line) {
        final String trimmed = line.strip();
        if (trimmed.isEmpty()) {
            return reject("blank line ignored");
        }
        final String[] fields = trimmed.split(" +");
        if (fields.length != 3) {
            return reject("malformed line ignored: " + line);
        }
        return eventFor(fields);
    }

    private Optional<PrescriptionEvent> eventFor(String[] fields) {
        final PrescriptionId id = new PrescriptionId(fields[0], fields[1]);
        return switch (fields[2].toLowerCase()) {
            case "created" -> Optional.of(new Created(id));
            case "filled" -> Optional.of(new Filled(id));
            case "returned" -> Optional.of(new Returned(id));
            default -> reject("unknown event ignored: " + fields[2]);
        };
    }

    private Optional<PrescriptionEvent> reject(String message) {
        warnings.warn(message);
        return Optional.empty();
    }
}
