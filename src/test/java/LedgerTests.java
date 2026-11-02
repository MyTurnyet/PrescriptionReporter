import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;


@Tag("Unit")
public class LedgerTests {
    @Test
    void newLedgerHasNoPatientSummaries() {
        final Ledger ledger = new Ledger();
        final Set<PatientSummary> summaries = ledger.summaries();
        assertThat(summaries).isEmpty();
    }
}