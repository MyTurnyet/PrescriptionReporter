package com.softwareascraft.prescriptionreporter.adapter;

import com.softwareascraft.prescriptionreporter.application.EventSource;

import java.io.InputStream;
import java.util.stream.Stream;

/**
 * Reads event lines from standard input as UTF-8 (see {@link Streams}).
 *
 * <p>The stream is supplied rather than read from {@link System#in} directly, so tests exercise
 * this adapter by handing it an input stream instead of swapping global state.
 */
public final class StdinEventSource implements EventSource {
    private final InputStream in;
    private boolean consumed;

    public StdinEventSource() {
        this(System.in);
    }

    StdinEventSource(InputStream in) {
        this.in = in;
    }

    @Override
    public String description() {
        return "<stdin>";
    }

    /**
     * @throws IllegalStateException on a second call — stdin is one-shot, and a second reader
     *                               would silently split the input between the two
     */
    @Override
    public Stream<String> lines() {
        if (consumed) {
            throw new IllegalStateException("standard input has already been read");
        }
        consumed = true;
        return Streams.reader(in).lines();
    }
}
