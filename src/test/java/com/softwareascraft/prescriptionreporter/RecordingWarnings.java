package com.softwareascraft.prescriptionreporter;

import java.util.ArrayList;
import java.util.List;

class RecordingWarnings implements Warnings {
    record Warning(SourceLocation where, DiscardReason why) {
    }

    private final List<Warning> warnings = new ArrayList<>();

    @Override
    public void warn(SourceLocation where, DiscardReason why) {
        warnings.add(new Warning(where, why));
    }

    List<Warning> warnings() {
        return List.copyOf(warnings);
    }

    List<DiscardReason> reasons() {
        return warnings.stream().map(Warning::why).toList();
    }

    List<SourceLocation> locations() {
        return warnings.stream().map(Warning::where).toList();
    }
}
