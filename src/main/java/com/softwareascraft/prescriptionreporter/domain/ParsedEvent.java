package com.softwareascraft.prescriptionreporter.domain;

/**
 * One well-formed input line: which prescription, and what happened to it.
 *
 * <p>This is a single record plus a single enum, not a class hierarchy. Parsing produces it and
 * {@link Ledger} consumes it; the one switch over {@link EventType} lives in the ledger.
 */
public record ParsedEvent(PrescriptionId id, EventType type) {
}
