# Prescription Report

A command-line program that reads a stream of pharmacy prescription events and prints, for each patient, the total
number of fills and the total income.

## Requirements

- Input is a file passed as an argument (`PrescriptionReporter <Filepath>`) or stdin (`cat <Filepath> | PrescriptionReporter`).
- Each line is `PatientName DrugName EventName`, space-delimited, with no spaces inside any field.
- Events: `created`, `filled`, `returned`.
- Output is one line per patient with total fills and total income.

## Interpretation of the Rules

### Prescription identity

A prescription is identified by the pair (patient, drug). `Mark B` and `Mark C` are two different prescriptions for the
same patient.

### Event handling

- `filled` and `returned` events that arrive before the prescription's `created` event are discarded. They are not
  buffered and are not applied retroactively.
- A `returned` event cancels one prior fill.

### Income

The spec's wording ("each return is a $1 loss") is ambiguous, so I worked it out against the sample data:

| Patient | Events counted    | Net fills | Income                |
|---------|-------------------|-----------|-----------------------|
| Mark    | 3 fills, 1 return | 2         | 2 x $5 - $1 = **$9**  |
| John    | 1 fill, 1 return  | 0         | 0 x $5 - $1 = **-$1** |
| Nick    | none              | 0         | **$0**                |

So:

- **Total fills** is net of returns (fills minus returns).
- **Income** is `$5 x net fills - $1 x returns`. In effect a return reverses the $5 and also costs $1.
- The $1 penalty is permanent. A later re-fill earns a fresh $5 and does not refund it.

## Assumptions

These cover things the spec doesn't say.

1. **Patient names are unique keys.** Two lines with the name `Mark` refer to the same patient.
2. **A patient appears in the report only if they have at least one created prescription.** The sample shows
   this: `Paul D filled` has no `created` event, and Paul is absent from the expected output.
3. **Output is sorted alphabetically by patient name.** The sample output in the spec (Mark, John, Nick) follows no
   obvious ordering and is probably an artifact of hash-map iteration. I chose a deterministic order so the output is
   testable and reproducible. The comparison is Java's natural `String` ordering (case-sensitive).
4. **Output format follows the spec's sample, not its prose.** The prose says `PatientName TotalFills Income`, but the
   sample shows `Mark: 2 fills $9 income`, with negative amounts written as `-$1`. I treat the sample as authoritative.
   The word is always "fills", never "fill", even for a count of zero or one.
5. **Malformed or invalid lines are skipped with a warning on stderr, and processing continues.** This covers lines
   with the wrong number of fields, unknown event names, and blank lines. Warnings go to stderr so stdout stays a
   clean report that can be piped.
6. **Event names are matched case-insensitively** (`Created`, `CREATED`, and `created` are all accepted), since the
   spec doesn't state a case convention and real-world input is more likely to vary in case than to contain a
   deliberately different event type. Patient and drug names are matched exactly as given; the spec never implies they
   should be normalized.
7. **A `returned` event with no valid prior fill is discarded entirely, not just clamped.** This includes a return
   whose matching fill was itself discarded for lacking a `created` event. The event is ignored and a warning is
   emitted; neither the fill count nor the $1 penalty is applied. This guarantees net fills can never go negative
   without needing a special case in the math.
8. **A duplicate `created` event for an already-created (patient, drug) pair is ignored and warned**, keeping the
   prescription's existing fill/return history untouched. The spec says this won't happen, but the ledger defends
   against it rather than silently resetting state.
9. **Input source:** a filename argument is read if present, otherwise stdin. A missing or unreadable file produces an
   error message on stderr and a non-zero exit code.
10. **Exit code is 0 whenever a report was produced**, even if some lines were skipped with warnings along the way.
    A non-zero exit code is reserved for a run that couldn't produce a report at all (an unreadable input file).
11. **Money is stored as integer dollars,** never floating point.

## Design

The guiding idea is a small set of single-purpose collaborators wired together at the edge, rather than one large
function and not an over-engineered framework either.

- **Ports.** Input and output are interfaces (`EventSource` and `ReportSink`). `FileEventSource` and
  `StdinEventSource` are the production input adapters; `StdoutReportSink` is the production output adapter. Tests
  use small hand-written in-memory fakes (`InMemoryEventSource`, `InMemoryReportSink`) instead.
- **Events.** `PrescriptionEvent` is an interface with immutable record implementations `Created`, `Filled`, and
  `Returned`, each holding a `PrescriptionId`. Each event applies itself to a `Ledger` via `applyTo(Ledger)`, so
  nothing else needs to `switch` on event type.
- **Parser.** `EventParser` turns a raw line into an `Optional<PrescriptionEvent>`. It owns all malformed-line
  handling (wrong field count, blank lines, unknown event names) and reports rejections through `Warnings`, so the
  domain never sees bad input.
- **Ledger.** An immutable object keyed by `PrescriptionId` (patient + drug). `create`, `fill`, and `returnOne` each
  return a new `Ledger`. It is the single place that knows which events count (a prior `created` exists, a `returned`
  has a valid fill to cancel) and tallies fills and income per patient through `summaries()`.
- **Pricing.** `IncomeCalculator` is the only place that knows the $5 fill and $1 return amounts, kept as `private
  final` instance fields rather than static constants. `Prescription` and `Ledger` know nothing about money.
- **Warnings.** A `Warnings` port (`warn(String)`) decouples "an event was discarded" from "where that gets printed."
  `StderrWarnings` is the production implementation; tests use a hand-written `RecordingWarnings` fake.
- **Report.** `ReportFormatter` sorts summaries by patient name and renders each line, including the `-$1` sign
  placement. Sorting and presentation are kept out of the domain.
- **Wiring.** `ReportApplication` folds every parsed event into a `Ledger` and writes the formatted report to a
  `ReportSink`. `CommandLineApp` decides which `EventSource` to use (stdin vs. a named file) and maps a missing file to
  a non-zero exit code. `Main.main` is a single line that delegates to `CommandLineApp` — there's nothing left in it
  worth unit testing directly.

### Code style constraints

- Methods of at most 8 lines, with low cognitive complexity.
- Dependencies point at interfaces (dependency inversion) and composition is preferred over inheritance.
- Objects are immutable, with no static methods and no mutable shared state (other than `Main.main` itself).
- "Ask, don't tell": objects are asked to do work rather than having their state pulled out and acted on.

## Testing Strategy

- **Test-driven.** Each behavior started as a failing test, from the smallest possible step (an empty ledger has no
  summaries) up through full end-to-end runs of the sample input.
- **No mocking frameworks.** If a mock were needed, the design would be wrong. I/O is behind interfaces
  (`EventSource`, `ReportSink`, `Warnings`), so tests use small hand-written in-memory fakes instead
  (`InMemoryEventSource`, `InMemoryReportSink`, `RecordingWarnings`). A few tests exercise the real file system and
  real stdin/stdout via `@TempDir` and stream redirection, since that's real behavior, not something to fake.
- **Layers.** Domain rules are tested directly against `Prescription`, `IncomeCalculator`, and `Ledger`. Parsing,
  formatting (including the `-$1` placement and alphabetical ordering), and the command-line wiring each have their
  own focused tests. `ReportApplicationTests` runs the full sample input through the whole pipeline with fakes.
- **Functional tests.** `CommandLineAppTests` runs the *real* `CommandLineApp` (so `FileEventSource`, `EventParser`,
  `Ledger`, and `ReportFormatter` are all real, not fakes) against actual files written to a JUnit `@TempDir`.
  `theFullSampleInputFromTheBriefProducesTheExpectedReportViaARealFile` writes the brief's complete sample input to a
  real file and asserts the exact expected report. The rest of that file cover: the same report produced from stdin,
  a missing file returning a non-zero exit code, file and stdin producing identical output for the same content, and
  three real-file edge cases (CRLF line endings, a file with no trailing newline, and an empty file). They run with
  the rest of the suite — `mvn test` — nothing separate is needed to exercise them.
- **Framework.** JUnit 5 for structure and tagging, AssertJ (`assertThat(...)`) for fluent assertions.
- **Order matters for exact-match assertions.** `containsExactly` is used wherever the result order is itself part of
  the contract (the final report); `containsExactlyInAnyOrder` is used where it isn't (e.g. two patients' summaries
  from a ledger before formatting).

## Tradeoffs

- **Immutable ledger versus a mutable accumulator.** Returning a new `Ledger` per event costs a small map copy, but
  makes behavior easy to reason about and test — there's never a half-updated `Ledger` to worry about. At this
  problem's scale the allocation cost doesn't matter.
- **Per-prescription fill tracking versus per-patient tallies only.** The spec guarantees every return has a prior
  fill, so per-patient tallies alone would technically be enough. I keyed the ledger by `PrescriptionId` (patient +
  drug) and track each prescription's own fill/return counts instead, because that's what lets the ledger *structurally*
  enforce "discard before `created`" and "ignore a return with no valid prior fill" — those rules fall out of a map
  lookup rather than being special-cased trust in well-formed input. `Prescription` owns its own `PrescriptionId`
  (rather than duplicating `patientName`/`drugName` as separate fields), so `Ledger.withPrescription` derives the map
  key from the prescription itself — there's no way to hand it a mismatched key/value pair.
- **Streaming.** Input is processed one line at a time via `Stream<String>` (`EventSource.lines()`), folded into the
  ledger with `reduce`, rather than being collected into a list first. Memory scales with the number of distinct
  prescriptions and patients, not with the size of the input file.
- **Accessors versus "ask, don't tell".** `Ledger` reads a prescription's patient name through an accessor
  (`patientName()`), the same way `IncomeCalculator` reads `fillCount()` and `returnCount()`. Having `Prescription`
  build its own `PatientSummary` would follow "ask, don't tell" more closely, but it would pull a reporting concept
  into the domain object — the same reason pricing lives in `IncomeCalculator` and not on `Prescription`. I accepted
  the getters as a pragmatic compromise.
- **Warnings as a port, injected where the discard decision is made.** `Ledger` and `EventParser` each take a
  `Warnings` dependency and call it at the exact point they decide to discard something, rather than collecting
  rejections and reporting them later. This keeps the warning message next to the reason for it, at the cost of
  `Ledger` and `EventParser` both depending on the same small interface.

## Prerequisites

- Java 17
- Maven 3.x

## Running the tests

```
mvn test
```

## Running the program

Compile once:

```
mvn compile
```

Then run against a file argument:

```
PrescriptionReporter <Filepath>
```

or pipe events in on stdin:

```
cat <Filepath> | PrescriptionReporter
```

`PrescriptionReporter` isn't packaged as its own executable yet (`pom.xml` has no jar/shade plugin), so today that
usage line maps to:

```
java -cp target/classes com.softwareascraft.prescriptionreporter.Main <Filepath>
```

and, for stdin:

```
cat <Filepath> | java -cp target/classes com.softwareascraft.prescriptionreporter.Main
```

Malformed or discarded lines are reported on stderr as they're skipped; the report itself goes to stdout, so
redirecting stdout to a file keeps the report clean. A missing or unreadable file argument prints an error and exits
non-zero.
