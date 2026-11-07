package com.softwareascraft.prescriptionreporter.application;

import com.softwareascraft.prescriptionreporter.domain.DiscardReason;
import com.softwareascraft.prescriptionreporter.domain.EventType;
import com.softwareascraft.prescriptionreporter.domain.ParsedEvent;
import com.softwareascraft.prescriptionreporter.domain.PrescriptionId;

/**
 * Turns one input line into a {@link ParsedEvent}, or into the reason it was discarded.
 *
 * <p>Parsing only. It holds no ledger and no warning port, so it is a pure function of the line
 * and can be tested without building either.
 *
 * <p>Lines are {@code PatientName DrugName EventName}, separated by one or more spaces. Leading
 * and trailing whitespace is stripped first, which also absorbs a trailing {@code \r} from CRLF
 * input. The event name is matched case-insensitively; patient and drug names are left exactly as
 * written.
 */
public final class EventParser {
    private static final int EXPECTED_FIELDS = 3;

    public ParseResult parse(String line) {
        final String trimmed = line.strip();
        if (trimmed.isEmpty()) {
            return new ParseResult.Rejected(new DiscardReason.BlankLine());
        }
        final String[] fields = trimmed.split(" +");
        if (fields.length != EXPECTED_FIELDS) {
            return new ParseResult.Rejected(new DiscardReason.MalformedLine(trimmed));
        }
        return EventType.fromName(fields[2])
                .<ParseResult>map(type -> new ParseResult.Parsed(
                        new ParsedEvent(new PrescriptionId(fields[0], fields[1]), type)))
                .orElseGet(() -> new ParseResult.Rejected(new DiscardReason.UnknownEvent(trimmed)));
    }
}
