package com.softwareascraft.prescriptionreporter;

/**
 * Where discard notices go. Implemented by the stderr adapter in production and by a recording
 * fake in tests, so nothing in the parsing or ledger path needs to know about either.
 */
public interface Warnings {
    void warn(SourceLocation where, DiscardReason why);
}
