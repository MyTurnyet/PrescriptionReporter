package com.softwareascraft.prescriptionreporter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class FileEventSource implements EventSource {
    private final Path path;

    public FileEventSource(Path path) {
        this.path = path;
    }

    @Override
    public Stream<String> lines() {
        try {
            return Files.lines(path);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
