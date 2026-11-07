package com.softwareascraft.prescriptionreporter;

import java.util.Locale;
import java.util.Optional;

/**
 * The three event names the input may carry. Matched case-insensitively.
 */
public enum EventType {
    CREATED,
    FILLED,
    RETURNED;

    public static Optional<EventType> fromName(String name) {
        return switch (name.toLowerCase(Locale.ROOT)) {
            case "created" -> Optional.of(CREATED);
            case "filled" -> Optional.of(FILLED);
            case "returned" -> Optional.of(RETURNED);
            default -> Optional.empty();
        };
    }
}
