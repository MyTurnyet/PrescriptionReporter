package com.softwareascraft.prescriptionreporter;

import java.util.ArrayList;
import java.util.List;

class RecordingWarnings implements Warnings {
    private final List<DiscardReason> reasons = new ArrayList<>();

    @Override
    public void warn(DiscardReason reason) {
        reasons.add(reason);
    }

    List<DiscardReason> reasons() {
        return List.copyOf(reasons);
    }
}
