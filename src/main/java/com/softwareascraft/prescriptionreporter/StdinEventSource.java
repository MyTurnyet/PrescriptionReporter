package com.softwareascraft.prescriptionreporter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.stream.Stream;

public class StdinEventSource implements EventSource {
    @Override
    public Stream<String> lines() {
        return new BufferedReader(new InputStreamReader(System.in)).lines();
    }
}
