package com.softwareascraft.prescriptionreporter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Accumulates prescriptions as events arrive, and summarises them per patient.
 *
 * <p><b>Ownership.</b> A ledger is a mutable accumulator, privately owned by whoever created it
 * and never shared. {@link ReportApplication} makes one per run, folds the input into it, and
 * drops it. The values it holds — {@link Prescription}, {@link PrescriptionId} — stay immutable,
 * so nothing it hands out can be used to mutate it, and {@link #summaries()} returns a snapshot
 * that does not change if more events arrive afterwards.
 *
 * <p>An earlier version handed back a new {@code Ledger} on every accepted event, copying the
 * whole map each time. That is O(events × distinct prescriptions) — measurably quadratic: a
 * 64,000-line input took about 50 seconds. Each event is now O(1).
 *
 * <p><b>Reporting.</b> The ledger decides whether an event counts and <em>returns</em> the reason
 * when it does not. It holds no warning port, so it neither knows nor cares where a discard
 * notice ends up, and the caller — which knows the line number — is the one that reports it.
 */
public final class Ledger {
    private final Map<PrescriptionId, Prescription> prescriptions = new LinkedHashMap<>();

    /**
     * @return empty when the event was applied, or the reason it was discarded
     */
    public Optional<DiscardReason> apply(ParsedEvent event) {
        return switch (event.type()) {
            case CREATED -> create(event.id());
            case FILLED -> fill(event.id());
            case RETURNED -> returnOne(event.id());
        };
    }

    private Optional<DiscardReason> create(PrescriptionId id) {
        if (prescriptions.containsKey(id)) {
            return Optional.of(new DiscardReason.DuplicateCreated(id));
        }
        prescriptions.put(id, new Prescription(id));
        return Optional.empty();
    }

    private Optional<DiscardReason> fill(PrescriptionId id) {
        final Prescription existing = prescriptions.get(id);
        if (existing == null) {
            return Optional.of(new DiscardReason.NoCreatedPrescriptionToFill(id));
        }
        prescriptions.put(id, existing.filled());
        return Optional.empty();
    }

    private Optional<DiscardReason> returnOne(PrescriptionId id) {
        final Prescription existing = prescriptions.get(id);
        if (existing == null) {
            return Optional.of(new DiscardReason.NoValidPriorFillToReturn(id));
        }
        final Optional<Prescription> returned = existing.returned();
        if (returned.isEmpty()) {
            return Optional.of(new DiscardReason.NoValidPriorFillToReturn(id));
        }
        prescriptions.put(id, returned.get());
        return Optional.empty();
    }

    /**
     * Pricing is supplied here rather than held as a field, so the ledger itself never deals in
     * money — it holds prescriptions and counts, and the dollar amounts are applied on the way
     * out.
     *
     * @return one summary per patient with at least one created prescription, in creation order;
     *         a snapshot, unaffected by later events
     */
    public List<PatientSummary> summaries(IncomeCalculator incomeCalculator) {
        return byPatient().entrySet().stream()
                .map(entry -> summaryFor(incomeCalculator, entry.getKey(), entry.getValue()))
                .toList();
    }

    private Map<String, List<Prescription>> byPatient() {
        return prescriptions.values().stream()
                .collect(Collectors.groupingBy(Prescription::patientName, LinkedHashMap::new, Collectors.toList()));
    }

    /**
     * Net fills are summed as {@code long} and narrowed with {@link Math#toIntExact}, so an
     * absurd input fails loudly instead of silently wrapping to a negative count.
     */
    private static PatientSummary summaryFor(IncomeCalculator incomeCalculator, String patientName,
                                             List<Prescription> patientPrescriptions) {
        final long netFills = patientPrescriptions.stream().mapToLong(Prescription::netFillCount).sum();
        final long income = patientPrescriptions.stream().mapToLong(incomeCalculator::incomeFor).sum();
        return new PatientSummary(patientName, Math.toIntExact(netFills), income);
    }
}
