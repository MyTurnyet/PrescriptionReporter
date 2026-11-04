package com.softwareascraft.prescriptionreporter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.stream.Stream;

public class StdinEventSource implements EventSource {
    private boolean consumed = false;

    @Override
    public Stream<String> lines() {
        if (consumed) {
            throw new IllegalStateException("StdinEventSource.lines() can only be called once");
        }
        consumed = true;
        return new BufferedReader(new InputStreamReader(System.in)).lines();
    }
}
