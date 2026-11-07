package com.softwareascraft.prescriptionreporter.application;

import com.softwareascraft.prescriptionreporter.domain.DiscardReason;

import java.util.ArrayList;
import java.util.List;

/**
 * Records discard notices so a test can assert which reason was reported, and where from,
 * instead of counting warnings or matching stderr text.
 */
public class RecordingWarnings implements Warnings {
    public record Warning(SourceLocation where, DiscardReason why) {
    }

    private final List<Warning> warnings = new ArrayList<>();

    @Override
    public void warn(SourceLocation where, DiscardReason why) {
        warnings.add(new Warning(where, why));
    }

    public List<Warning> warnings() {
        return List.copyOf(warnings);
    }

    public List<DiscardReason> reasons() {
        return warnings.stream().map(Warning::why).toList();
    }

    public List<SourceLocation> locations() {
        return warnings.stream().map(Warning::where).toList();
    }
}
