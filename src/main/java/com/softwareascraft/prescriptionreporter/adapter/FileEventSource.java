package com.softwareascraft.prescriptionreporter.adapter;

import com.softwareascraft.prescriptionreporter.application.EventSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Reads event lines from a file as UTF-8 (see {@link Streams}).
 *
 * <p>Failures surface as {@link UncheckedIOException} carrying a message already fit to show a
 * user — it names the file and says what went wrong, without an exception class name in it.
 */
public final class FileEventSource implements EventSource {
    private final Path path;

    public FileEventSource(Path path) {
        this.path = path;
    }

    @Override
    public String description() {
        return path.toString();
    }

    @Override
    public Stream<String> lines() {
        final BufferedReader reader = open();
        return reader.lines().onClose(() -> close(reader));
    }

    private BufferedReader open() {
        try {
            return Streams.reader(Files.newInputStream(path));
        } catch (IOException e) {
            throw new UncheckedIOException(cannotRead(e), e);
        }
    }

    private void close(BufferedReader reader) {
        try {
            reader.close();
        } catch (IOException e) {
            throw new UncheckedIOException(cannotRead(e), e);
        }
    }

    private String cannotRead(IOException cause) {
        return "could not read " + path + ": " + reasonFor(cause);
    }

    private static String reasonFor(IOException cause) {
        if (cause instanceof NoSuchFileException) {
            return "no such file";
        }
        if (cause instanceof AccessDeniedException) {
            return "permission denied";
        }
        final String message = cause.getMessage();
        return message == null || message.isBlank() ? "could not be opened" : message;
    }
}
