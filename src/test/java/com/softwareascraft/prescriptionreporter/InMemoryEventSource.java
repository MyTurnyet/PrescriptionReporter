package com.softwareascraft.prescriptionreporter;

import java.util.List;
import java.util.stream.Stream;

class InMemoryEventSource implements EventSource {
    private final List<String> lines;

    InMemoryEventSource(List<String> lines) {
        this.lines = lines;
    }

    @Override
    public Stream<String> lines() {
        return lines.stream();
    }
}
