package com.softwareascraft.prescriptionreporter;

import java.util.stream.Stream;

/**
 * Where event lines come from. The domain does not know whether that is a file, stdin, or a test
 * fixture.
 */
public interface EventSource {
    /**
     * A name for this input, used to locate warnings — a file path, or {@code <stdin>}.
     */
    String description();

    /**
     * @return the lines, in order. The caller owns the stream and must close it; closing it
     *         releases the underlying resource.
     *         <p>Not guaranteed to be callable more than once — some implementations (stdin) wrap
     *         a one-shot resource and reject a second call.
     */
    Stream<String> lines();
}
