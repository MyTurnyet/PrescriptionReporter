# Prescription Report

Reads pharmacy prescription events — `PatientName DrugName created|filled|returned` — and writes one
report line per patient: net fills and income.

```
$ prescriptionreporter events.txt
John: 0 fills -$1 income
Mark: 2 fills $9 income
Nick: 0 fills $0 income
```

Input is the one file argument, or standard input when no file is given.

## Build and run

Needs **JDK 17** to build and Java 17 or newer to run. Build with the wrapper, which pins Maven 3.9.9; the
pom pins every plugin version and sets `maven.compiler.release=17`. Those versions need Maven 3.6.3+, so
prefer `./mvnw` over whatever `mvn` is on the path.

```
./mvnw package                     # writes target/PrescriptionReporter.jar
./prescriptionreporter <file>      # POSIX shells; cat <file> | ./prescriptionreporter
prescriptionreporter <file>        # cmd.exe;      type <file> | prescriptionreporter
```

Both launcher scripts resolve the jar relative to themselves, so they work from any directory and when
symlinked onto `PATH` — which is also how to get the bare `prescriptionreporter` form in a POSIX shell,
since those do not search the current directory. Arguments, stdin and the exit status pass through
untouched, and they exit 127 if the jar is unbuilt or no JVM is reachable. Running the jar directly with
`java -jar target/PrescriptionReporter.jar <file>` does the same thing; every form takes `--help`.

**125 tests**, all passing, in two filterable tiers — 81 `@Tag("Unit")`, 44 `@Tag("Functional")`:

```
./mvnw verify                      # or: ./mvnw test -Dgroups=Unit   (or -Dgroups=Functional)
```

Outside the suite, `scripts/smoke-test.sh` runs the packaged jar *and* the launcher against the brief's
sample, asserting stdout and exit codes, and `scripts/check-line-length.sh` fails over 120 characters.
CI (`.github/workflows/ci.yml`) runs `./mvnw --batch-mode verify` then both scripts, on JDK 17.

### A native binary, optionally

The jar plus the launcher scripts are the intended way to run this. If you want a native binary anyway,
`scripts/build-exe.sh` makes one with `jpackage`, which ships with JDK 14+, so nothing extra to install:

```
./scripts/build-exe.sh             # writes target/dist/PrescriptionReporter/
```

That is a **directory, not a single file**: a launcher beside an `app/` folder and a bundled `runtime/`,
~130 MB, and it will not start without those siblings. A standalone single binary would need GraalVM's
`native-image`, a toolchain this project does not depend on.

On Windows the script passes `--win-console`. Without it `jpackage` emits a GUI-subsystem binary that still
prints correctly into a pipe but shows nothing when a person runs it in a terminal — so it fails only in the
case you would not think to test, and running the binary cannot detect it. The script therefore verifies what
it built: the report, the exit code for an unreadable file, and the PE subsystem itself.

## Behaviour

**Input.** One event per line, three fields separated by one or more spaces; no field contains spaces.
Surrounding whitespace is stripped, which also absorbs a trailing `\r` from CRLF input. Event names match
case-insensitively; patient and drug names are never normalised.

A prescription is a (patient, drug) pair — `Mark B` and `Mark C` are two prescriptions for one patient.
`filled` and `returned` are discarded unless that prescription was already `created`: they are not buffered
awaiting a `created`, and a later `created` does not replay them. A `returned` cancels exactly one prior
fill, and is discarded if there is none to cancel.

**Output.** One line per patient, sorted by name, following the brief's *example* (`Mark: 2 fills $9
income`, negative income as `-$1`) rather than its prose; "fills" is always plural. A patient appears only
with at least one created prescription, which is why the sample's `Paul D filled` leaves Paul out.

**Streams.** Only the report goes to stdout, so redirecting it yields a clean report. Discards warn on
stderr, naming the input and line number, and processing continues:

```
events.txt:4: unknown event ignored, expected created/filled/returned: Mark B dispensed
events.txt:6: filled event ignored, no created prescription for Paul D
```

**Exit codes** are part of the contract: `0` a report was produced **and written**, warnings or not; `1` the
input could not be read or the report could not be written; `2` usage error — an unusable path, or more than
one file argument. Messages name the file and the reason (`could not read nope.txt: no such file`), never an
exception class name.

**Encoding.** UTF-8 end to end — file, stdin, stdout, stderr — named explicitly, never the platform
default. Invalid bytes become U+FFFD and the run continues, identically for a file and for stdin,
matching how every other kind of bad input is treated. `Files.lines` would instead throw on the first bad
byte, which is why input is decoded explicitly in `Streams`.

## Deriving the income rules

The brief says a fill earns $5 and "each return is a $1 loss", but not whether that loss is net of the fill.
Rather than guess, I worked backward from the sample's expected numbers:

| Patient | Events            | Net fills | Income                |
|---------|-------------------|-----------|-----------------------|
| Mark    | 3 fills, 1 return | 2         | 2 × $5 − $1 = **$9**  |
| John    | 1 fill, 1 return  | 0         | 0 × $5 − $1 = **-$1** |
| Nick    | none              | 0         | **$0**                |

Those only reconcile if "total fills" means fills net of returns and income is `$5 × net fills − $1 ×
returns` — a return takes back the $5 *and* costs a dollar on top. The penalty is permanent: refilling later
earns a fresh $5 but does not refund the dollar.

## Assumptions

Where the brief is silent, I picked a behaviour and pinned it with a test.

1. Patient names are unique keys: two lines saying `Mark` are the same patient.
2. Sorting uses `String`'s own ordering, so it is case-sensitive and `Nick` precedes `mark`. The sample's
   order is not reproducible from the input, so I chose alphabetical for determinism.
3. A duplicate `created` is ignored and warned, leaving that prescription's history untouched. The brief
   says it will not happen; the ledger defends against it rather than wiping out history if it did.
4. A `returned` with nothing to cancel — including one whose own `filled` was discarded — is dropped
   whole: no fill-count change, no penalty, just a warning. `Prescription` refuses the return itself, so
   net fills cannot go negative by construction.
5. Money is whole dollars in `long`. No `int` fill count can overflow it, the widest product being
   `Integer.MAX_VALUE × 5`, about 1.1e10.

## Design decisions

| Decision | Why | Alternative rejected |
|---|---|---|
| Mutable `Ledger`, owned per run by `ReportApplication` | O(1) per event instead of quadratic (below) | A new `Ledger` per event, copying the whole map |
| Parsing and applying split, each returning its outcome | The caller owns the loop, so warnings name the line | Each stage holding a warnings port, blind to lines |
| An explicit ordered loop over the lines | States the sequential requirement directly | A three-argument `Stream.reduce` (below) |
| UTF-8 named explicitly, on every stream | Same bytes in, same report out, on every machine | The platform default: file and stdin disagreed |
| `StdoutReportSink` checks `checkError()` and throws | Exit 0 now means the report was delivered | Trusting `PrintStream`, which swallows `IOException` |
| Sealed `DiscardReason`, rendered in one exhaustive place | Tests assert *which* rule fired, not a warning count | A warning string, and an unguarded default case |
| Named `$5`/`$1` constants in `IncomeCalculator` | The only class that prices anything; the ledger takes it as a parameter and never sees money | Inline literals, and `Math.abs` for the sign, which is negative at `MIN_VALUE` |
| One `ParsedEvent` record and an `EventType` enum | Three cases, one switch, and it lives in `Ledger` | A four-record `PrescriptionEvent` hierarchy (below) |
| Exhaustiveness enforced by a test | Pattern-matching `switch` is preview on Java 17, so `StderrWarnings` throws on an unrecognised reason and a test counts its cases against `getPermittedSubclasses()` | Requiring `--enable-preview` just to run the tests |

Two rejected designs worth naming. The `reduce` was a three-argument fold whose combiner, `(a, b) -> b`,
never merged two ledgers: it was correct only because a defensive `.sequential()` forced sequential
evaluation, and removing that one call produced an empty report. The abandoned event hierarchy had each
record apply itself to the ledger, but something still had to switch on the event name to build the right
record, so the switch moved one line earlier and cost four files.

**Layering.** Classes sit in `domain` (the rules), `application` (the use case and its ports) and `adapter`
(files, streams, the command line). Dependencies point inward only: `adapter` sees both, `application` sees
`domain`, `domain` sees nothing else. Java packages do not enforce that, so `PackageDependencyTests` does —
it reads the sources and fails if a layer names one it should not, matching the whole file rather than just
the import block so an inline fully-qualified reference is caught too. No architecture-testing library
needed.

## The ledger measurement

The earlier immutable `Ledger` returned a new instance per accepted event, copying the whole prescription
map. This file used to claim "at this scale that cost is nothing". It was quadratic. Both columns measured
the same way — `corretto-17.0.21`, best of three runs, net of ~350 ms JVM startup:

| Lines   | Before  | After  |
|---------|---------|--------|
| 4,000   | 0.5 s   | 0.2 s  |
| 8,000   | 1.1 s   | 0.2 s  |
| 16,000  | 2.3 s   | 0.4 s  |
| 32,000  | 9.7 s   | 0.5 s  |
| 64,000  | 38.9 s  | 0.7 s  |
| 256,000 | —       | 1.3 s  |

Roughly 4× per doubling before, linear after. Immutability stayed where it pays: `Prescription`,
`PrescriptionId` and `PatientSummary` are values, and the summaries the ledger returns are a snapshot. What
moved is the accumulation, into the one object nothing else shares. Lines are still streamed, so memory
tracks distinct prescriptions rather than input size.

**Keying by prescription rather than a per-patient tally.** The brief guarantees every `returned` follows a
`filled`, so a running count per patient would do. Keying by `PrescriptionId` turns "discard anything before
`created`" and "ignore an invalid return" into a map lookup rather than a property I have to trust the input
to hold. `Prescription` owns its own id, so the key is always derived from the value stored under it.

## Tests

Test-first, JUnit 5 and AssertJ, no mocking library: I/O sits behind `EventSource`, `ReportSink` and
`Warnings`, with small hand-written fakes. Every adapter takes its streams through its constructor, so no
test swaps `System.in` or `System.out` and the suite mutates no global state. Repeated cases are
parameterised, and discard tests assert which `DiscardReason` was recorded.

`Unit` covers the rules directly, `Functional` runs against real files and streams, and the smoke script
covers the packaged jar and the launcher. Between them: the brief's full sample end to end, a discarded
event whose `created` arrives later, returns isolated per drug, a mid-input read failure writing no partial
report, stream closure, non-ASCII names through both input paths, undeliverable stdout, malformed bytes, and
mixed-case ordering.
