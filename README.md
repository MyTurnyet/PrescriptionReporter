# Prescription Report

Reads a stream of pharmacy prescription events and prints, per patient, how many fills they have and how much
income they generated.

## Requirements

- Input is a file passed as an argument (`PrescriptionReporter <Filepath>`) or piped in on stdin
  (`cat <Filepath> | PrescriptionReporter`).
- Each line is `PatientName DrugName EventName`, space-delimited. No field contains spaces.
- Three event types: `created`, `filled`, `returned`.
- Output is one line per patient: total fills, total income.

## Reading the rules

A prescription is identified by (patient, drug) — `Mark B` and `Mark C` are different prescriptions even though
they're the same patient. `filled` and `returned` events that show up before their prescription's `created` event
get discarded. They're not buffered waiting for a `created` to show up later, and they're not retroactively applied
if one never does — if the input never creates that prescription, those events just never happened as far as the
report is concerned. A `returned` cancels exactly one prior fill.

The income rule in the brief ("each return is a $1 loss") doesn't say whether that's net of fills or not, so I
worked backward from the sample numbers instead of guessing:

| Patient | Events             | Net fills | Income                |
|---------|--------------------|-----------|------------------------|
| Mark    | 3 fills, 1 return  | 2         | 2 × $5 − $1 = **$9**  |
| John    | 1 fill, 1 return   | 0         | 0 × $5 − $1 = **-$1** |
| Nick    | none               | 0         | **$0**                |

That only works out if "total fills" means net of returns, and income is `$5 × net fills − $1 × returns` — a
return takes back the $5 *and* costs an extra dollar. And the dollar penalty sticks: filling the same prescription
again later doesn't refund it, it just earns a fresh $5 on top.

## Assumptions

Things the brief didn't pin down, so I picked something reasonable and I'm documenting it rather than guessing
silently:

1. Patient names are unique keys — two lines with `Mark` are the same patient.
2. A patient only shows up in the report if they have at least one `created` prescription. `Paul D filled` with no
   matching `created` is why Paul isn't in the sample's expected output.
3. The report is sorted alphabetically by patient name. The brief's sample output (Mark, John, Nick) isn't
   alphabetical, chronological, or sorted by anything I can find — it reads like whatever order a HashMap happened
   to iterate in for the reference solution. I went with alphabetical so the output is actually deterministic and
   the tests aren't ordering-fragile.
4. Output format follows the brief's *example*, not its prose description. The prose says
   `PatientName TotalFills Income`; the example shows `Mark: 2 fills $9 income` with negative amounts written
   `-$1` (sign before the dollar sign). I went with what the example actually shows. "fills" is always plural,
   even for zero or one.
5. Bad lines — wrong field count, an event name that isn't one of the three, blank lines — get skipped with a
   warning on stderr, and the rest of the file still processes. Warnings go to stderr specifically so stdout stays
   a clean report you can pipe somewhere.
6. Event names match case-insensitively (`Created`, `CREATED`, `created` are all fine). Patient and drug names
   don't get normalized at all — the brief never implies they should be, and case-folding someone's name felt like
   the wrong kind of liberty to take.
7. A `returned` with no valid fill to cancel — including one whose own `filled` event got discarded for having no
   `created` — is dropped entirely rather than partially applied. No fill-count change, no dollar penalty, just a
   warning. `Prescription` itself refuses the return (it's the one holding the fill/return counts), so the ledger
   never has to trust a caller to pre-check — net fills can't go negative by construction.
8. A duplicate `created` for a prescription that already exists gets ignored and warned, leaving whatever fills
   and returns it already had alone. The brief says this won't happen, but I'd rather the ledger defend against it
   than silently wipe out history if it ever did.
9. No filename argument means read stdin. A filename that can't be opened prints an error and exits non-zero.
10. Exit code is 0 any time a report actually got produced, warnings or not. Non-zero is reserved for runs that
    couldn't produce a report at all (an unreadable file).
11. Money is an `int`, not a float, for the obvious reason.

## How it's put together

I didn't want one big function, but I also didn't want to over-build this into something with five layers of
indirection for three event types. What I landed on:

`EventSource` and `ReportSink` are the input/output seams — interfaces, so the domain doesn't know or care whether
it's reading a file, stdin, or a test fixture. `FileEventSource` and `StdinEventSource` are the real input
adapters, `StdoutReportSink` the real output one; tests swap in hand-written in-memory versions instead.

I did try an event hierarchy at one point — a `PrescriptionEvent` interface with `Created`/`Filled`/`Returned`
records, each knowing how to apply itself to a `Ledger`. It didn't earn its keep: something still had to switch on
the event name string to build the right record in the first place, so the switch I was trying to avoid just moved
one line earlier, at the cost of four extra files for three cases that will never grow a fourth. `EventParser` now
does the obvious thing instead — `applyLine(Ledger, String)` parses a line and calls `ledger.create`/`fill`/
`returnOne` directly in one switch expression. One place has a switch on event type; that's fine.

`Ledger` is immutable and keyed by `PrescriptionId`. `create`, `fill`, and `returnOne` each hand back a new
`Ledger` rather than mutating anything, and it's the one place that knows whether a `created`/`fill` actually
counts — `Prescription.returnIt()` handles the "something to cancel" check itself and hands back an empty
`Optional` if there isn't. `summaries()` groups by patient and adds up fills and income from there.

`IncomeCalculator` is the only class that knows $5 and $1 are the relevant numbers, and they're instance fields,
not static constants — `Prescription` and `Ledger` never touch money at all. Discard decisions go through a small
`Warnings` port instead of printing directly, so `Ledger` and `EventParser` don't need to know or care that it's
stderr on the other end — and what gets discarded is a typed `DiscardReason` (a sealed interface, one record per
case: a duplicate `created`, a fill/return with nothing to apply to, a malformed line, and so on), not a loose
string, so nothing downstream has to parse English to find out what happened. `ReportFormatter` handles sorting
and the `-$1` formatting, kept separate from the domain on purpose.

Wiring lives at the edges: `ReportApplication` folds parsed events into a `Ledger` and writes the result,
`CommandLineApp` decides stdin vs. a named file and turns a missing file into a non-zero exit, and `Main.main` is
one line that hands off to `CommandLineApp` — there's nothing left in it worth testing on its own.

A few constraints I held myself to throughout: methods stay under 8 lines and simple, dependencies point at
interfaces with concrete types only wired up at the edges, everything's immutable with no statics besides `main`,
and objects get asked to do things rather than having their internals pulled out and poked at — though see the
tradeoffs section for where I didn't fully hold that last line.

## Testing

Test-first, starting from the smallest possible case (an empty ledger has no summaries) and building up from
there. No mocking libraries — if something felt like it needed a mock, that was a sign the design needed
rethinking instead. I/O sits behind interfaces, so tests use small hand-rolled fakes: `InMemoryEventSource`,
`InMemoryReportSink`, `RecordingWarnings`. A handful of tests use the real filesystem and real stdin/stdout instead
(`@TempDir`, stream redirection) — that's real behavior worth testing directly, not something to fake away.

Domain rules get tested directly against `Prescription`, `IncomeCalculator`, and `Ledger`. Parsing, formatting, and
the CLI wiring each have their own focused tests on top of that. `ReportApplicationTests` runs the brief's full
sample through the whole pipeline with fakes standing in for I/O.

`CommandLineAppTests`, `FileEventSourceTests`, `StdinEventSourceTests`, and `StdoutReportSinkTests` go a step
further and run against the real filesystem and real streams instead of fakes — these are tagged `@Tag("Functional")`
rather than `@Tag("Unit")`, so the two tiers are actually filterable, not just a description in this file.
`CommandLineAppTests` runs the real `CommandLineApp` — real `FileEventSource`, real `EventParser`, real `Ledger`,
real `ReportFormatter` — against files written to a `@TempDir`.
`theFullSampleInputFromTheBriefProducesTheExpectedReportViaARealFile` writes the brief's sample to disk and checks
the report comes out right end to end. The rest of that file covers the same thing via stdin, a missing file
exiting non-zero, file and stdin agreeing on the same input, a few real-file quirks (CRLF line endings, no
trailing newline, an empty file), and — since "what happens to bad input" is most of the interesting behavior
here — one isolated test per discard scenario: a fill or return with no `created`, a return with nothing to
cancel, a duplicate `created`, a line with the wrong number of fields, an unrecognized event name, a blank line.
I know most of those scenarios are already covered one layer down in `LedgerTests`/`EventParserTests` — I kept
both on purpose rather than trimming the "slower" copy, since the two layers are testing different things (the
rule itself, vs. that the rule still holds once real file I/O is in the loop) and I'd rather pay the small
maintenance cost than lose either guarantee. Each test checks the report and that a warning came out of it; a
representative test per `DiscardReason` in `LedgerTests` and `EventParserTests` goes further and checks *which*
reason was recorded, not just the count. All of it runs under `mvn test` along with everything else.

JUnit 5 plus AssertJ. `containsExactly` where order is part of what's being verified (the final report),
`containsExactlyInAnyOrder` where it isn't.

## Tradeoffs

**Immutable `Ledger` vs. a mutable accumulator.** Handing back a new `Ledger` on every event means copying a small
map each time. At this scale that cost is nothing, and in exchange there's never a half-updated `Ledger` sitting
around to reason about.

**Tracking fills per prescription instead of just per patient.** The brief guarantees every `returned` has a prior
`filled`, so technically a running per-patient tally would be enough — no per-prescription bookkeeping needed. I
keyed the ledger by `PrescriptionId` anyway, because that's what turns "discard anything before `created`" and
"ignore an invalid return" into an ordinary map lookup instead of something I'd have to trust the input to never
violate. `Prescription` owns its own `PrescriptionId` rather than carrying patient/drug as separate fields, so
`Ledger` always derives its map key from the prescription being stored — there's no code path where the key and
the value could disagree with each other.

**Streaming instead of loading everything into memory first.** Lines come in as a `Stream<String>` and get folded
into the ledger one at a time with `reduce`, rather than collected into a list up front. Memory tracks the number
of distinct patients and prescriptions, not the size of the input file.

**Accessors instead of strict "ask, don't tell."** `Ledger` reads a prescription's patient name through a plain
getter, the same way `IncomeCalculator` reads its fill and return counts. Having `Prescription` hand back its own
`PatientSummary` would be more in the spirit of "tell, don't ask," but it would mean `Prescription` needs to know
about reporting — which is exactly why pricing lives in `IncomeCalculator` instead of on `Prescription` too. I'd
rather have a couple of plain getters than a domain object that knows about things outside its job.

**`returnIt()` refuses the call instead of trusting whoever's calling it.** My first pass had `Ledger` check
`fillCount() > 0` before ever calling `Prescription.returnIt()` — which meant the one rule that actually matters
("can't return more than you filled") lived one hop away from the object that owns the counts, and anything else
calling `returnIt()` directly would've had to remember to redo that check itself. `returnIt()` now returns
`Optional<Prescription>` and hands back empty when there's nothing to cancel, so the invariant travels with the
object instead of living in whoever happens to call it.

**Warnings fired at the point of discard, not collected and reported later.** `Ledger` and `EventParser` each hold
a `Warnings` reference and call it the moment they decide something's getting thrown away, instead of returning a
list of rejections for someone else to report. The upside is the warning is written right next to the code that
knows *why*; the downside is both classes now depend on the same small interface just to do that. The reason
itself is a typed `DiscardReason`, not a string — mostly so a test (or a future caller) can check *what kind* of
thing got discarded without string-matching stderr output. One wrinkle: I wanted `StderrWarnings` to switch over
the sealed interface with pattern-matching case labels, but that's still a preview feature on Java 17 — the
`javac` that ships with this JDK won't compile it without `--enable-preview`, which I didn't want to force on
anyone just to run `mvn test`. It falls back to a chain of `instanceof` checks instead, which is stable since
Java 16 and does the same job.

## Prerequisites

- Java 17
- Maven 3.x

## Running the tests

```
mvn test
```

## Running the program

Build the jar:

```
mvn package
```

That produces `target/PrescriptionReporter.jar` with a `Main-Class` manifest entry, so it's runnable directly:

```
java -jar target/PrescriptionReporter.jar <Filepath>
```

or pipe events in on stdin:

```
cat <Filepath> | java -jar target/PrescriptionReporter.jar
```

There's no shell/batch wrapper yet, so `PrescriptionReporter <Filepath>` (the usage line in the Requirements
section above) isn't a literal command you can type — `java -jar target/PrescriptionReporter.jar <Filepath>` is
what it actually maps to today. `mvn compile` plus
`java -cp target/classes com.softwareascraft.prescriptionreporter.Main <Filepath>` also still works if you'd
rather skip the jar step while iterating.

Skipped lines get reported on stderr as they happen; the report itself only ever goes to stdout, so redirecting
stdout to a file gets you a clean report with the warnings left out. A file that's missing or can't be read prints
an error and exits non-zero.
