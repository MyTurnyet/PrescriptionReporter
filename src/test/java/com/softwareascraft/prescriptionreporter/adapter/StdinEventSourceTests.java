package com.softwareascraft.prescriptionreporter.adapter;

import com.softwareascraft.prescriptionreporter.application.EventSource;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The adapter is handed its stream, so these tests never touch {@code System.in}.
 */
@Tag("Functional")
class StdinEventSourceTests {
    private static StdinEventSource sourceOf(String input) {
        return new StdinEventSource(bytesOf(input));
    }

    private static InputStream bytesOf(String input) {
        return new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void readsLinesInOrder() {
        try (Stream<String> lines = sourceOf("Mark B created\nMark B filled\n").lines()) {
            assertThat(lines).containsExactly("Mark B created", "Mark B filled");
        }
    }

    @Test
    void describesItselfAsStdinSoWarningsCanNameTheSource() {
        assertThat(sourceOf("").description()).isEqualTo("<stdin>");
    }

    @Test
    void decodesAsUtf8RegardlessOfThePlatformDefault() {
        try (Stream<String> lines = sourceOf("José B created\n").lines()) {
            assertThat(lines).containsExactly("José B created");
        }
    }

    /**
     * The documented malformed-input policy: a bad byte becomes U+FFFD and the run continues,
     * rather than aborting the whole report.
     */
    @Test
    void replacesMalformedBytesInsteadOfFailingTheRun() {
        final InputStream invalidUtf8 = new ByteArrayInputStream(new byte[]{
                'M', 'a', 'r', 'k', ' ', (byte) 0xFF, ' ', 'c', 'r', 'e', 'a', 't', 'e', 'd', '\n'});

        try (Stream<String> lines = new StdinEventSource(invalidUtf8).lines()) {
            assertThat(lines).containsExactly("Mark � created");
        }
    }

    @Test
    void anEmptyInputYieldsNoLines() {
        try (Stream<String> lines = sourceOf("").lines()) {
            assertThat(lines).isEmpty();
        }
    }

    @Test
    void aFinalLineWithoutATrailingNewlineIsStillRead() {
        try (Stream<String> lines = sourceOf("Mark B created").lines()) {
            assertThat(lines).containsExactly("Mark B created");
        }
    }

    @Test
    void readingASecondTimeIsRefusedBecauseStdinIsOneShot() {
        final StdinEventSource source = sourceOf("Mark B created\n");
        source.lines().close();

        assertThatThrownBy(source::lines)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already been read");
    }
}
