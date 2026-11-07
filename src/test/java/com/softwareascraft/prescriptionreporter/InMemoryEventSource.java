package com.softwareascraft.prescriptionreporter;

import java.util.List;
import java.util.stream.Stream;

class InMemoryEventSource implements EventSource {
    private final List<String> lines;
    private boolean closed;

    InMemoryEventSource(List<String> lines) {
        this.lines = lines;
    }

    @Override
    public String description() {
        return "<memory>";
    }

    @Override
    public Stream<String> lines() {
        return lines.stream().onClose(() -> closed = true);
    }

    /** Lets a test assert the application closes the stream it was handed. */
    boolean wasClosed() {
        return closed;
    }
}
