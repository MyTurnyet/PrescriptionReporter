package com.softwareascraft.prescriptionreporter;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("Unit")
public class StdinEventSourceTests {
    @Test
    void readsLinesFromStandardInput() {
        final InputStream originalIn = System.in;
        System.setIn(new ByteArrayInputStream("Mark B created\nMark B filled\n".getBytes(StandardCharsets.UTF_8)));
        try {
            final EventSource source = new StdinEventSource();
            assertThat(source.lines().toList()).containsExactly("Mark B created", "Mark B filled");
        } finally {
            System.setIn(originalIn);
        }
    }
}
