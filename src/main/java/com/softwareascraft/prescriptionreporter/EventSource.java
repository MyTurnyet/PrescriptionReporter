package com.softwareascraft.prescriptionreporter;

import java.util.stream.Stream;

public interface EventSource {
    /**
     * Not guaranteed to be callable more than once — some implementations (e.g. stdin) wrap a
     * one-shot resource and will reject a second call.
     */
    Stream<String> lines();
}
