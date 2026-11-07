package com.softwareascraft.prescriptionreporter.application;

import com.softwareascraft.prescriptionreporter.domain.DiscardReason;
import com.softwareascraft.prescriptionreporter.domain.ParsedEvent;

/**
 * What parsing one line produced: either an event, or the reason the line was discarded.
 *
 * <p>Parsing reports its outcome rather than writing a warning itself, so the caller — which is
 * the only thing that knows where the line came from — attaches the source and line number.
 */
public sealed interface ParseResult {
    record Parsed(ParsedEvent event) implements ParseResult {}

    record Rejected(DiscardReason reason) implements ParseResult {}
}
