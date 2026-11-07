package com.softwareascraft.prescriptionreporter.application;

/**
 * Where a discarded line came from, so a warning can point at it.
 *
 * @param source     the input's name — a file path, or {@code <stdin>}
 * @param lineNumber 1-based line number within that input
 */
public record SourceLocation(String source, int lineNumber) {
    @Override
    public String toString() {
        return source + ":" + lineNumber;
    }
}
