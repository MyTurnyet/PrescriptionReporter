package com.softwareascraft.prescriptionreporter;

public sealed interface DiscardReason {
    record DuplicateCreated(PrescriptionId id) implements DiscardReason {}

    record NoCreatedPrescriptionToFill(PrescriptionId id) implements DiscardReason {}

    record NoValidPriorFillToReturn(PrescriptionId id) implements DiscardReason {}

    record BlankLine() implements DiscardReason {}

    record MalformedLine(String line) implements DiscardReason {}

    record UnknownEvent(String line) implements DiscardReason {}
}
