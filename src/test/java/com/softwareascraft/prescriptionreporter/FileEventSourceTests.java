package com.softwareascraft.prescriptionreporter;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("Functional")
class FileEventSourceTests {
    @TempDir
    Path tempDir;

    private Path fileWith(String content) throws IOException {
        final Path file = tempDir.resolve("events.txt");
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }

    @Test
    void readsLinesInOrder() throws IOException {
        final EventSource source = new FileEventSource(fileWith("Mark B created\nMark B filled\n"));

        try (Stream<String> lines = source.lines()) {
            assertThat(lines).containsExactly("Mark B created", "Mark B filled");
        }
    }

    @Test
    void describesItselfByPathSoWarningsCanNameTheSource() throws IOException {
        final Path file = fileWith("");

        assertThat(new FileEventSource(file).description()).isEqualTo(file.toString());
    }

    @Test
    void decodesAsUtf8RegardlessOfThePlatformDefault() throws IOException {
        final EventSource source = new FileEventSource(fileWith("José B created\n"));

        try (Stream<String> lines = source.lines()) {
            assertThat(lines).containsExactly("José B created");
        }
    }

    /**
     * The documented malformed-input policy, matching stdin: a bad byte becomes U+FFFD and the
     * run continues. Note {@code Files.lines} would instead throw on the first bad byte.
     */
    @Test
    void replacesMalformedBytesInsteadOfFailingTheRun() throws IOException {
        final Path file = tempDir.resolve("events.txt");
        Files.write(file, new byte[]{'M', 'a', 'r', 'k', ' ', (byte) 0xFF, ' ', 'c', 'r', 'e',
                'a', 't', 'e', 'd', '\n'});

        try (Stream<String> lines = new FileEventSource(file).lines()) {
            assertThat(lines).containsExactly("Mark � created");
        }
    }

    @Test
    void aMissingFileFailsWithAMessageNamingTheFileAndNoExceptionClassName() {
        final Path missing = tempDir.resolve("missing.txt");
        final EventSource source = new FileEventSource(missing);

        assertThatThrownBy(source::lines)
                .isInstanceOf(UncheckedIOException.class)
                .hasMessageContaining(missing.toString())
                .hasMessageContaining("no such file")
                .hasMessageNotContaining("NoSuchFileException");
    }

    @Test
    void aDirectoryGivenInsteadOfAFileFailsWithAMessageNamingIt() {
        final EventSource source = new FileEventSource(tempDir);

        assertThatThrownBy(() -> source.lines().toList())
                .isInstanceOf(UncheckedIOException.class)
                .hasMessageContaining(tempDir.toString());
    }

    /**
     * The stream owns a file handle, so closing it must release the handle. Deleting the file on
     * Windows only succeeds once nothing holds it open, which is what makes this a real check.
     */
    @Test
    void closingTheStreamReleasesTheFile() throws IOException {
        final Path file = fileWith("Mark B created\n");

        try (Stream<String> lines = new FileEventSource(file).lines()) {
            assertThat(lines).hasSize(1);
        }

        assertThat(Files.deleteIfExists(file)).isTrue();
    }

    @Test
    void anEmptyFileYieldsNoLines() throws IOException {
        try (Stream<String> lines = new FileEventSource(fileWith("")).lines()) {
            assertThat(lines).isEmpty();
        }
    }
}
