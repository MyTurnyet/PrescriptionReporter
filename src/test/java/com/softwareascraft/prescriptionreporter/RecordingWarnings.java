package com.softwareascraft.prescriptionreporter;

import java.util.ArrayList;
import java.util.List;

class RecordingWarnings implements Warnings {
    private final List<String> messages = new ArrayList<>();

    @Override
    public void warn(String message) {
        messages.add(message);
    }

    List<String> messages() {
        return List.copyOf(messages);
    }
}
