# Prescription Report

Reads a stream of pharmacy prescription events — `PatientName DrugName created|filled|returned` — and
writes one report line per patient: net fills and income.

```
$ prescriptionreporter events.txt
John: 0 fills -$1 income
Mark: 2 fills $9 income
Nick: 0 fills $0 income
```

Input is a file named as the one argument (`prescriptionreporter <file>`) or piped in on standard input
(`cat <file> | prescriptionreporter`).

## Build and run

Needs **JDK 17** to build, and Java 17 or newer to run. Build with the Maven Wrapper, which pins Maven
3.9.9; the pom pins every plugin version and sets `maven.compiler.release=17`. Those plugin versions need
Maven 3.6.3 or newer, so use the wrapper rather than whatever `mvn` is on the path.

```
./mvnw package           # writes target/PrescriptionReporter.jar; ./mvnw verify runs the tests too
```

Then run it through the launcher at the repository root — `prescriptionreporter`, or
`prescriptionreporter.cmd` on Windows:

```
./prescriptionreporter <file>          # POSIX shells
cat <file> | ./prescriptionreporter
prescriptionreporter <file>            # cmd.exe
type <file> | prescriptionreporter
```

Either script resolves the jar relative to itself, so it works from any working directory and when symlinked
onto `PATH` — which is also how to get the bare `prescriptionreporter` form in a POSIX shell, since those do
not look in the current directory. Arguments, stdin and the program's exit status pass through untouched,
and the script exits 127 with a message if the jar is not built or no JVM is reachable. Without a launcher,
`java -jar target/PrescriptionReporter.jar <file>` does the same thing; either form takes `--help`.

125 tests, all passing, in two filterable tiers — 81 `@Tag("Unit")` and 44 `@Tag("Functional")`:

```
./mvnw test                      # everything
./mvnw test -Dgroups=Unit        # or -Dgroups=Functional
```

Outside the suite, `scripts/smoke-test.sh` runs the packaged jar against the brief's sample and asserts
stdout and exit codes, and `scripts/check-line-length.sh` fails over 120 characters. GitHub Actions
(`.github/workflows/ci.yml`) runs `./mvnw --batch-mode verify` and then both scripts, on JDK 17.

### A native launcher, optionally

The jar plus the launcher scripts are the intended way to run this: small, cross-platform, and already the
invocation the brief asks for. If you want a native binary anyway, `scripts/build-exe.sh` makes one with
`jpackage`, which ships with JDK 14 and newer, so there is nothing extra to install:

```
./scripts/build-exe.sh           # writes target/dist/PrescriptionReporter/
```

It produces a **directory, not a single file** — a launcher (`PrescriptionReporter.exe` on Windows) beside
`app/` folder and a bundled `runtime/`, about 130 MB in total. The launcher will not start without those
siblings, so the whole folder is the unit you distribute. A genuinely standalone single binary needs
GraalVM's `native-image`, a separate toolchain this project does not depend on.

The script passes `--win-console` on Windows, which matters more than it looks: without it `jpackage` emits a
GUI-subsystem binary that still prints correctly into a pipe but shows nothing when a person runs it in a
terminal — it fails only in the case you would not think to test. So the script checks what it built rather
than trusting it: it runs the binary against the brief's sample, asserts the report and the exit code for an
unreadable file, and on Windows reads the PE subsystem directly, since no amount of running the binary can
detect that particular mistake.

## Behaviour

**Input.** One event per line, `PatientName DrugName EventName`, separated by one or more spaces; no field
contains spaces. Surrounding whitespace is stripped, which also absorbs a trailing `\r` from CRLF input.
Event names match case-insensitively; patient and drug names are never normalised.

A prescription is a (patient, drug) pair — `Mark B` and `Mark C` are two prescriptions for one patient.
`filled` and `returned` are discarded unless that prescription has already been `created`: they are not
buffered waiting for a `created` to arrive later, and a `created` further down the file does not replay
them. A `returned` cancels exactly one prior fill, and is discarded if there is none to cancel.

**Output.** One line per patient, sorted by name, in the format the brief's *example* shows (`Mark: 2 fills
$9 income`, negative income as `-$1`) rather than the format its prose describes; "fills" is always plural.
A patient appears only with at least one created prescription, which is why the sample's `Paul D filled`
leaves Paul out of the expected report.

**Streams.** Nothing but the report goes to stdout, so redirecting it gives a clean report. Discarded lines
warn on stderr, naming the input and line number, and processing continues:

```
events.txt:4: unknown event ignored, expected created/filled/returned: Mark B dispensed
events.txt:6: filled event ignored, no created prescription for Paul D
```

**Exit codes** are part of the contract: `0` a report was produced **and written**, warnings or not; `1` the
input could not be read or the report could not be written; `2` usage error — an unusable path, or more than
one file argument. Error messages name the file and the reason (`could not read nope.txt: no such file`),
never an exception class name.

**Encoding.** UTF-8 end to end — file input, stdin, stdout, stderr — named explicitly, never the platform
default. Invalid bytes become U+FFFD and the run continues, identically for a file and for stdin, which
matches how every other kind of bad input is treated. `Files.lines` would instead throw on the first bad
byte, which is why input is decoded explicitly in `Streams`.

## Deriving the income rules

The brief says a fill earns $5 and "each return is a $1 loss", but not whether that loss is net of the fill.
Rather than guess, I worked backward from the sample's expected numbers:

| Patient | Events            | Net fills | Income                |
|---------|-------------------|-----------|-----------------------|
| Mark    | 3 fills, 1 return | 2         | 2 × $5 − $1 = **$9**  |
| John    | 1 fill, 1 return  | 0         | 0 × $5 − $1 = **-$1** |
| Nick    | none              | 0         | **$0**                |

Those three only reconcile if "total fills" means fills net of returns and income is `$5 × net fills −
$1 × returns` — a return takes back the $5 *and* costs a dollar on top. The penalty is permanent:
refilling the same prescription later earns a fresh $5 but does not refund the dollar.

## Assumptions

Where the brief is silent, I picked a behaviour and pinned it with a test.

1. Patient names are unique keys: two lines saying `Mark` are the same patient.
2. Sorting uses `String`'s own ordering, so it is case-sensitive and `Nick` precedes `mark`. The sample's
   order is not reproducible from the input, so I chose alphabetical for determinism.
3. A duplicate `created` is ignored and warned, leaving that prescription's fills and returns untouched. The
   brief says it will not happen; the ledger defends against it rather than wiping out history if it did.
4. A `returned` with no valid fill to cancel — including one whose own `filled` was discarded — is dropped
   whole: no fill-count change, no penalty, just a warning. `Prescription` refuses the return itself, so net
   fills cannot go negative by construction.
5. Money is whole dollars in `long`: no `int` fill count can overflow it, the widest product being
   `Integer.MAX_VALUE × 5`, about 1.1e10.
6. No file argument means read stdin; more than one is a usage error rather than silently ignored.

## Decisions

| Decision | Why | Alternative rejected |
|---|---|---|
| Mutable `Ledger`, owned per run by `ReportApplication` | O(1) per event: 64,000 lines, ~50 s to ~1.0 s | A new `Ledger` per event — quadratic copying |
| Parsing and applying split, each returning its outcome | The caller owns the loop, so warnings name the line | Each stage holding a warnings port, blind to lines |
| An explicit ordered loop over the lines | States the sequential requirement directly | A three-argument `Stream.reduce` (below) |
| UTF-8 named explicitly, on every stream | Same bytes in, same report out, on every machine | The platform default: file and stdin disagreed |
| `StdoutReportSink` checks `checkError()` and throws | Exit 0 now means the report was delivered | Trusting `PrintStream`, which swallows `IOException` |
| Sealed `DiscardReason`, rendered in one exhaustive place | Tests can assert *which* rule fired | A warning string, and an unguarded default case |
| Named `$5`/`$1` constants in `IncomeCalculator` | The only class that prices anything; the ledger takes it as a parameter and never sees money | Inline literals, and `Math.abs` for the sign |
| One `ParsedEvent` record and an `EventType` enum | Three cases, one switch, and it lives in `Ledger` | A four-record `PrescriptionEvent` hierarchy (below) |

The `reduce` was a three-argument fold whose combiner, `(a, b) -> b`, never merged two ledgers: it was
correct only because a defensive `.sequential()` forced sequential evaluation, and removing that one call
produced an empty report. The abandoned event hierarchy had each record apply itself to the ledger, but
something still had to switch on the event name to build the right record, so the switch moved one line
earlier and cost four files.

The classes sit in three packages — `domain` for the rules, `application` for the use case and its ports,
`adapter` for files, streams and the command line. Dependencies point inward only: `adapter` sees
`application` and `domain`, `application` sees `domain`, and `domain` sees nothing else.

Java packages do not enforce that by themselves, so `PackageDependencyTests` does. It reads the sources and
fails if any layer names one it should not, which keeps the split a constraint rather than a convention that
decays on the next edit. It matches the whole file, not just the import block, so an inline fully-qualified
reference is caught too. Checking it needs no architecture-testing library.

## Tradeoffs

**The ledger measurement.** The earlier immutable `Ledger` returned a new instance per accepted event,
copying the whole prescription map. I had written in this file that "at this scale that cost is nothing". It
was quadratic. Measured on `corretto-17.0.21`, net of ~400 ms JVM startup:

| Lines  | Before  | After  |
|--------|---------|--------|
| 4,000  | 0.5 s   | —      |
| 8,000  | 0.9 s   | —      |
| 16,000 | 2.6 s   | —      |
| 32,000 | 9.9 s   | —      |
| 64,000 | 49.8 s  | 1.0 s  |
| 256,000| —       | 1.6 s  |

Roughly 4× per doubling before, linear after. Immutability stayed where it pays — `Prescription`,
`PrescriptionId` and `PatientSummary` are values, and the summaries the ledger hands back are a snapshot.
What moved is the accumulation, into the one object nothing else shares. Lines are still streamed, so memory
tracks distinct prescriptions rather than input size.

**Keying by prescription, not a per-patient tally.** The brief guarantees every `returned` follows a
`filled`, so a running count per patient would do. Keying by `PrescriptionId` turns "discard anything before
`created`" and "ignore an invalid return" into a map lookup rather than a property I have to trust the input
to hold. `Prescription` owns its own id, so the key is always derived from the value stored under it.

**Exhaustiveness checked by a test, not the compiler.** Pattern matching for `switch` would let `javac`
prove every `DiscardReason` is handled, but it is a preview feature on Java 17, so `StderrWarnings` handles
each variant explicitly, throws on an unrecognised one, and has a test comparing its case count against
`DiscardReason.class.getPermittedSubclasses()`.

## Tests

Test-first, JUnit 5 and AssertJ, no mocking library: I/O sits behind `EventSource`, `ReportSink` and
`Warnings`, and tests use small hand-written fakes. Every adapter takes its streams through its constructor,
so no test swaps `System.in` or `System.out` and the suite mutates no global state. Repeated cases are
parameterised, and discard tests assert which `DiscardReason` was recorded rather than counting warnings.

`Unit` covers the rules directly, `Functional` runs against real files and real streams, and the smoke
script covers the packaged jar. Between them: the brief's full sample end to end, a discarded event whose
`created` arrives later, returns isolated per drug, a mid-input read failure writing no partial report,
non-ASCII names asserted at the byte level through both input paths, undeliverable stdout, and malformed
bytes.
