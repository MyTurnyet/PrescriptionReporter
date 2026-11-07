package com.softwareascraft.prescriptionreporter.adapter;

import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

/**
 * The program's character-encoding contract, in one place.
 *
 * <p><b>Encoding.</b> Every input and every output is UTF-8, named explicitly. Nothing here
 * depends on the platform default, so a file and the same bytes on stdin decode identically, and
 * the report comes out as the same bytes on every machine. Relying on the default meant a
 * {@code José} read from a file and the same {@code José} piped on stdin produced two different
 * reports on a Windows JVM before Java 18.
 *
 * <p><b>Malformed input policy.</b> Bytes that are not valid UTF-8 are replaced with U+FFFD
 * rather than aborting the run. That matches how the program treats every other kind of bad
 * input — skip what it cannot use, warn, and finish the report — and it is applied to file and
 * stdin alike. Note that {@code Files.lines} would instead throw on the first bad byte, which is
 * why input is decoded here rather than through it.
 */
final class Streams {
    private Streams() {
    }

    static BufferedReader reader(InputStream in) {
        return new BufferedReader(new InputStreamReader(in, lenientUtf8Decoder()));
    }

    static PrintStream stdout() {
        return utf8PrintStream(FileDescriptor.out);
    }

    static PrintStream stderr() {
        return utf8PrintStream(FileDescriptor.err);
    }

    private static PrintStream utf8PrintStream(FileDescriptor target) {
        return new PrintStream(new FileOutputStream(target), true, StandardCharsets.UTF_8);
    }

    private static CharsetDecoder lenientUtf8Decoder() {
        return StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE);
    }
}
