package com.softwareascraft.prescriptionreporter.application;

import java.util.List;
import java.util.stream.Stream;

/**
 * Hand-rolled fake for the {@link EventSource} port. Public because the adapter tests use it too.
 */
public class InMemoryEventSource implements EventSource {
    private final List<String> lines;
    private boolean closed;

    public InMemoryEventSource(List<String> lines) {
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
    public boolean wasClosed() {
        return closed;
    }
}
