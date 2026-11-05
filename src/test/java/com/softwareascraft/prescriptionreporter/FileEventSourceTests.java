package com.softwareascraft.prescriptionreporter;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("Functional")
public class FileEventSourceTests {
    @TempDir
    Path tempDir;

    @Test
    void readsLinesFromAnExistingFile() throws IOException {
        final Path file = tempDir.resolve("events.txt");
        Files.writeString(file, "Mark B created\nMark B filled\n");

        final EventSource source = new FileEventSource(file);

        assertThat(source.lines().toList()).containsExactly("Mark B created", "Mark B filled");
    }

    @Test
    void throwsUncheckedIOExceptionForAMissingFile() {
        final EventSource source = new FileEventSource(tempDir.resolve("missing.txt"));

        assertThatThrownBy(() -> source.lines().toList()).isInstanceOf(UncheckedIOException.class);
    }
}
