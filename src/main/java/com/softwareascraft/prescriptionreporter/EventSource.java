package com.softwareascraft.prescriptionreporter;

import java.util.stream.Stream;

public interface EventSource {
    Stream<String> lines();
}
