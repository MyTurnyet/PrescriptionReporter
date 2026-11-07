package com.softwareascraft.prescriptionreporter.adapter;

import com.softwareascraft.prescriptionreporter.application.SourceLocation;
import com.softwareascraft.prescriptionreporter.application.Warnings;
import com.softwareascraft.prescriptionreporter.domain.DiscardReason;
import com.softwareascraft.prescriptionreporter.domain.PrescriptionId;

import java.io.PrintStream;

/**
 * Renders discard notices to an error stream as {@code <source>:<line>: <what happened>}.
 *
 * <p>Warnings go to stderr, never stdout, so that redirecting stdout yields a clean report.
 */
public final class StderrWarnings implements Warnings {
    private final PrintStream err;

    public StderrWarnings() {
        this(Streams.stderr());
    }

    StderrWarnings(PrintStream err) {
        this.err = err;
    }

    @Override
    public void warn(SourceLocation where, DiscardReason why) {
        err.println(where + ": " + messageFor(why));
    }

    /**
     * Every {@link DiscardReason} is handled explicitly and an unrecognised one throws, so adding
     * a variant without adding a message here fails loudly instead of being mislabelled as
     * whichever case happened to be last.
     *
     * <p>Pattern matching for {@code switch} would let the compiler enforce this, but it is a
     * preview feature on Java 17 and the project's baseline is Java 17, so the exhaustiveness is
     * enforced at runtime instead.
     */
    static String messageFor(DiscardReason why) {
        if (why instanceof DiscardReason.DuplicateCreated r) {
            return "duplicate created event ignored for " + describe(r.id());
        }
        if (why instanceof DiscardReason.NoCreatedPrescriptionToFill r) {
            return "filled event ignored, no created prescription for " + describe(r.id());
        }
        if (why instanceof DiscardReason.NoValidPriorFillToReturn r) {
            return "returned event ignored, no valid prior fill for " + describe(r.id());
        }
        if (why instanceof DiscardReason.MalformedLine r) {
            return "malformed line ignored, expected 3 space-separated fields: " + r.line();
        }
        if (why instanceof DiscardReason.UnknownEvent r) {
            return "unknown event ignored, expected created/filled/returned: " + r.line();
        }
        if (why instanceof DiscardReason.BlankLine) {
            return "blank line ignored";
        }
        throw new IllegalStateException("no warning message defined for discard reason: " + why);
    }

    private static String describe(PrescriptionId id) {
        return id.patientName() + " " + id.drugName();
    }
}
