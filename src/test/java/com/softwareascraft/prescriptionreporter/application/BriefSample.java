package com.softwareascraft.prescriptionreporter.application;

import java.util.List;

/**
 * The sample input from the brief and the report it must produce.
 *
 * <p>Shared by the application and adapter tests. It lives in its own fixture rather than as
 * static fields on one test class that another test class reaches into.
 */
public final class BriefSample {
    private BriefSample() {
    }

    public static final List<String> LINES = List.of(
            "Nick A created",
            "Mark B created",
            "Mark B filled",
            "Mark C filled",
            "Mark B returned",
            "John E created",
            "Mark B filled",
            "Mark B filled",
            "Paul D filled",
            "John E filled",
            "John E returned");

    public static final List<String> REPORT = List.of(
            "John: 0 fills -$1 income",
            "Mark: 2 fills $9 income",
            "Nick: 0 fills $0 income");

    /** The same input as file content, with a trailing newline. */
    public static String text() {
        return String.join("\n", LINES) + "\n";
    }
}
