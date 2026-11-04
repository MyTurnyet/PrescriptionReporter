package com.softwareascraft.prescriptionreporter;

public class StderrWarnings implements Warnings {
    @Override
    public void warn(String message) {
        System.err.println(message);
    }
}
