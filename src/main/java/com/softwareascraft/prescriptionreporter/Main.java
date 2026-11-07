package com.softwareascraft.prescriptionreporter;

import com.softwareascraft.prescriptionreporter.adapter.CommandLineApp;

/**
 * Entry point. Exists only to turn {@link CommandLineApp}'s exit code into a process exit code,
 * which is why there is nothing here worth testing on its own.
 */
public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        System.exit(new CommandLineApp(args).run());
    }
}
