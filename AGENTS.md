# Notes for AI coding assistants

Tool-agnostic entry point (Claude-specific pointers live in [`CLAUDE.md`](CLAUDE.md); both files
stay short and defer to the maintained references).

## Read before acting

1. [`ARCHITECTURE.md`](ARCHITECTURE.md) — pipeline, module map, reading paths by intent.
2. [`road-to-immutability/llm-summary.md`](road-to-immutability/llm-summary.md) — **required
   before reasoning about immutability, modification, independence, linking, or convergence.**
   It is the authoritative condensed vocabulary; do not re-derive the concepts from code.
3. For link-engine work: [`maddi-modification-link/linking-manual.md`](maddi-modification-link/linking-manual.md)
   (start at §5), with `TestLinkMethodCall` as the executable spec.
4. Before running anything on the javac front end:
   [`maddi-inspection-openjdk/parsing-stability.md`](maddi-inspection-openjdk/parsing-stability.md).

## Commands

```bash
./gradlew build                      # compile + fast tests (excludes @Tag("slow"))
./gradlew slowTest                   # large-corpus smoke tests
./gradlew :<module>:test --tests 'TestName'
```

The corpus tests need the external `test-oss` corpus (a sibling of this checkout; managed by
the `CodeLaser/maddi-oss` repo, override with `-Dtest.oss.root`). **Without it they skip via
JUnit assumptions — never report a green `slowTest` as proof the corpora ran; check for
skipped tests.**

## Facts not to re-derive (wrongly)

- `TypeInfo`/`MethodInfo`/`FieldInfo` are single-instance per (FQN, source set) — compare with
  `==`, not `equals`.
- Analyzer properties are write-once; absence = undecided (first-class state, revisited later).
- `unmodifiedField` is content-only by design — do not "fix" it to also cover assignment.
- javac is not thread-safe; `JavacTask` access is single-threaded with
  `-XDuseUnsharedTable=true`.
- The root package `org.e2immu.*` is the project's historical name. Do not rename packages or
  remaining `e2immu` occurrences ad hoc — that migration is coordinated by the maintainer.

## Working style

- Engine/performance changes require a byte-identical `FPDUMP` A/B run on the proving-ground
  corpora before they are accepted; never trade verdict changes for speed silently.
- If you change analyzer semantics, update `road-to-immutability/llm-summary.md` in the same
  change.
- New cross-module design notes go in `docs/` and get an entry in
  [`docs/README.md`](docs/README.md); module-local bug notes go in the module's `notes/`.
- Documentation in `docs/` and `notes/` is dated working material — trust the maintained
  references listed in `ARCHITECTURE.md` §Documentation map when they disagree.
