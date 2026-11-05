package com.softwareascraft.prescriptionreporter;

public class StderrWarnings implements Warnings {
    @Override
    public void warn(DiscardReason reason) {
        System.err.println(messageFor(reason));
    }

    private String messageFor(DiscardReason reason) {
        if (reason instanceof DiscardReason.DuplicateCreated r) return "duplicate created event ignored for " + r.id();
        if (reason instanceof DiscardReason.NoCreatedPrescriptionToFill r) return "filled event ignored, no created prescription for " + r.id();
        if (reason instanceof DiscardReason.NoValidPriorFillToReturn r) return "returned event ignored, no valid prior fill for " + r.id();
        if (reason instanceof DiscardReason.MalformedLine r) return "malformed line ignored: " + r.line();
        if (reason instanceof DiscardReason.UnknownEvent r) return "unknown event ignored: " + r.line();
        return "blank line ignored";
    }
}
