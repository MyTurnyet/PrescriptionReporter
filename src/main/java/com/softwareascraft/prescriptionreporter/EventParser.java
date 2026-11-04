package com.softwareascraft.prescriptionreporter;

import java.util.Locale;
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
        return fields.length == 3 ? eventFor(fields, trimmed) : reject("malformed line ignored: " + trimmed);
    }

    private Optional<PrescriptionEvent> eventFor(String[] fields, String line) {
        final PrescriptionId id = new PrescriptionId(fields[0], fields[1]);
        return switch (fields[2].toLowerCase(Locale.ROOT)) {
            case "created" -> Optional.of(new Created(id));
            case "filled" -> Optional.of(new Filled(id));
            case "returned" -> Optional.of(new Returned(id));
            default -> reject("unknown event ignored: " + line);
        };
    }

    private Optional<PrescriptionEvent> reject(String message) {
        warnings.warn(message);
        return Optional.empty();
    }
}
