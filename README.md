# Prescription Report

A command-line program that reads a stream of pharmacy prescription events and prints, for each patient, the total
number of fills and the total income.

> **Status:** work in progress, built test-first. The sections below marked *(planned)* describe design decisions that
> have been made but are not yet implemented.

## Requirements

- Input is a file passed as an argument (`command filename.txt`) or stdin (`cat filename.txt | command`).
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
5. **Malformed or invalid lines are skipped with a warning on stderr, and processing continues.** This covers lines with
   the wrong number of fields, unknown event names, blank lines, and out-of-order events (a fill or return
   before `created`). Warnings go to stderr so stdout stays a clean report that can be piped.
6. **Input source:** a filename argument is read if present, otherwise stdin. A missing or unreadable file produces an
   error message and a non-zero exit code.
7. **Money is stored as integer dollars,** never floating point.

*(To confirm as the implementation lands: case sensitivity of event names, handling of a `returned` event with no valid
prior fill, and exit codes after skipped lines.)*

## Design *(planned)*

The guiding idea is a small set of single-purpose collaborators wired together at the edge, rather than one large
function and not an over-engineered framework either.

- **Ports.** Input and output are interfaces (`EventSource` and `ReportSink`). Production implementations read a file or
  stdin and write to stdout. Tests use simple hand-written in-memory implementations.
- **Events.** `PrescriptionEvent` is an interface with immutable record implementations for `Created`, `Filled`,
  and `Returned`. Each event knows how to apply itself to the ledger, so there is no `switch` on event type.
- **Parser.** A parser turns a raw line into an event or a rejection. It owns all malformed-line handling, so the domain
  never sees bad input.
- **Ledger.** An immutable object. `apply(event)` returns a new `Ledger`. It is the single place that knows which events
  count (a prior `created` exists) and tallies fills and returns per patient. It exposes `summaries()`,
  a `Set<PatientSummary>`, and is deliberately order-agnostic.
- **Pricing.** The $5 fill and $1 return amounts live in named constants (or a small policy object), not scattered
  literals.
- **Report.** The formatter sorts summaries by patient name and renders each line. Sorting and presentation are kept out
  of the domain.
- **Main.** Only wiring and exit codes.

### Code style constraints

- Methods of at most 8 lines, with low cognitive complexity.
- Dependencies point at interfaces (dependency inversion) and composition is preferred over inheritance.
- Objects are immutable, with no static methods and no mutable shared state.
- "Ask, don't tell": objects are asked to do work rather than having their state pulled out and acted on.

## Testing Strategy

- **Test-driven.** Each behavior starts as a failing test, and the first tests are deliberately tiny (an empty ledger
  has no summaries) so each step is a single red-green-refactor cycle.
- **No mocking frameworks.** If a mock were needed, the design would be wrong. I/O is behind interfaces, so tests use
  in-memory fakes instead.
- **Layers.** Domain rules are tested directly against the ledger. Parsing, formatting (including `-$1`), and sorting
  are tested on their own. A small number of end-to-end tests run the full sample input through the whole pipeline.
- **Framework.** JUnit 5 only.

Acceptance cases being driven out, in roughly this order:

1. An empty ledger has no summaries.
2. A created prescription with no activity gives `0 fills, $0`.
3. A fill gives `1 fill, $5`.
4. A fill with no prior `created` is discarded, and the patient does not appear.
5. A fill followed by a return gives `0 fills, -$1`.
6. A fill, return, then fill gives `1 fill, $4`.
7. Multiple prescriptions per patient are summed correctly, and discarded events for one don't affect another.
8. Malformed and out-of-order lines are skipped with warnings.
9. The full sample input produces the expected report, in alphabetical order.

## Tradeoffs *(planned)*

- **Immutable ledger versus a mutable accumulator.** Returning a new ledger per event costs some allocation but makes
  behavior easy to reason about and test. At this problem's scale the cost doesn't matter.
- **Per-prescription fill tracking versus per-patient tallies only.** The spec guarantees every return has a prior fill,
  so per-patient tallies plus a set of created prescriptions are enough. Tracking fills per prescription would let the
  ledger defend against bad returns, but at the cost of more state for a case the spec rules out.
- **Streaming.** Input is processed one line at a time, so memory scales with the number of patients and prescriptions,
  not the file size.
- **Accessors versus "ask, don't tell".** `Ledger` reads a prescription's patient name through an
  accessor (`userName()`), the same way the income calculator reads `fillCount()` and `returnCount()`.
  Having `Prescription` build its own `PatientSummary` would follow "ask, don't tell" more closely, but it would pull a
  reporting concept into the domain object. That's the same reason pricing lives in `IncomeCalculator` and not
  on `Prescription`. I accepted the getters as a pragmatic compromise.
-

## Requirements

- Java 17
- Maven 3.x

Run the tests with:

```
mvn test
```

*(Run instructions for the program itself will be added once `Main` exists.)*