# Rewiring: how it works, and what bites

Reloading and rewiring after a source change. Written as a handoff: what the machinery is, what state it touches,
and the traps — several of which cost real time to find, and none of which are visible in the code.

- **Status: openjdk inspector complete and running end to end** (`RunRewireTests`, 200 iterations). In-house
  inspector: was already there, and two shared bugs were fixed in it on the way. Kotlin inspector: not started.
- Companion: `maddi-modification-analyzer/definitions.md` (concepts), `guard-mode-analysis.md` (guard work).

## Why

The CST is effectively immutable, so a code change cannot be patched in place: the affected type must be rebuilt.
Not everything, though — **only the changed type and what is downstream of it**. Everything upstream stays as it is.
Three states, per primary type (`JavaInspector.InvalidationState`):

| | meaning | what happens to it |
|---|---|---|
| `UNCHANGED` | nothing to do | kept — the *same objects* |
| `INVALID` | its source changed | re-parsed from source: new objects, new compilation unit |
| `REWIRE` | it did not change, but it *reaches* an invalidated type | copied: new objects, **same** compilation unit, references re-pointed at the new ones |
| `REMOVED` | declared, never used by either inspector | (openjdk treats it as INVALID) |

`REWIRE` exists because a type that is itself unchanged still holds references to objects that were replaced. Its
own javadoc says it: *"the type isn't changed at all, but it accesses invalidated (and hence re-parsed, new) type
info objects"*. Two of this session's bugs were that sentence not holding.

## The machinery (cst-api / cst-impl, language-agnostic)

- `TypeInfo.rewirePhase0..3` — the copy protocol. Phase 0 builds shells, 1 does type parameters/hierarchy, 2 does
  members (**explicitly does not commit**), 3 does method bodies and *then* commits. So the builder is still open
  while bodies are rewired — which is what lets phase 3 create types on demand.
- `InfoMapImpl` — old → new for every `Info`, per primary type. `rewireAll()` drives the phases;
  `rewiredTypes()` reports everything built; `typeInfoRecurseAllPhases(t)` rewires a type *not* reached by the
  phases (see "on-demand types").
- `Runtime.newInfoMap(toRewire)` / `newInfoMap(toRewire, rebuilt)` — the second argument is essential, see below.

**The BASIC RULE OF REWIRING** (`InfoMapImpl`'s own comment): the fqn and source set stay the same, so conceptually
the object stays the same, so *it does not matter which object is used as a key*. This is load-bearing: all four
`Info` implementations are structurally equal (fqn + source set; parameters by index + method), which is why seeding
the map with the **new** objects makes lookups of the **old** ones resolve to them.

## The flow

1. `reloadSources(inputConfiguration, sourcesByTestProtocolURIString)` → `ReloadResult(problems, sourceHasChanged)`.
   Compares each source file's stored fingerprint against a fresh MD5. Parses nothing, invalidates nothing: it only
   answers *what changed*. Requires `computeFingerPrints` (both runners pass `true`).
2. The caller turns that into an `Invalidated` (see `RunRewireTests`): changed → INVALID; their dependents, from the
   call graph's primary-type use graph → REWIRE; the rest → UNCHANGED.
3. `parse(options.setInvalidated(...))` re-parses the INVALID, rewires the REWIRE, keeps the rest.

### The two inspectors differ in granularity

- **in-house** (`maddi-inspection-integration`): per **source file**. It has a `sourceFiles` map seeded at
  `initialize()`, and re-parses individual files.
- **openjdk** (`maddi-inspection-openjdk`): per **source set**, because that is javac's unit — one `JavacTask` over
  a whole set. A source set holding any INVALID type is re-scanned *in full*, so its unchanged types are rebuilt
  too. This was a deliberate choice (a coarser first step); a selective CST build within a set is the obvious next
  refinement. Consequence to remember: **in a single-source-set project nothing is ever rewired** — everything is
  re-scanned. You need ≥2 source sets to exercise REWIRE at all.

## The state that must be kept in step

| what | in-house | openjdk |
|---|---|---|
| type registry | `CompiledTypesManagerImpl` (a trie, keyed by fqn parts) | `CompiledTypesManagerImpl` (a flat fqn map) **+ `InfoByFqn`** |
| `invalidate(primaryType)` | clears the trie entries | clears both registries |
| `setRewiredType(t)` | re-points **one** type | re-points **one** type |
| source files | `Map<SourceFile, List<TypeInfo>>`, seeded in `initialize()` | same map, filled after every scan |

`SourceFile` equality is `(uri, sourceSet)` only — path and fingerprint are excluded, which is what lets a re-parse
overwrite an entry whose fingerprint changed.

## Traps

Every one of these was found the hard way. They share a shape: **something re-derives what another component already
knows**, or **a default is indistinguishable from a decision**.

1. **`InfoMap.typeInfo(t)` returns `t` unchanged when `t.primaryType()` is not a key.** So an InfoMap built from the
   REWIRE set alone leaves every reference to a re-parsed type pointing at the object that was replaced — REWIRE
   silently does nothing. Fix: `newInfoMap(toRewire, rebuilt)`, seeding the re-parsed types. *Both* inspectors had
   this, and both test suites passed: nothing asserted it.
2. **`recursiveSubTypeStream()` is not "every type under this one".** It lists the *declared* subtypes. Phase 3
   rewires three kinds on demand that are not in it — anonymous classes (`ConstructorCallImpl`), local classes
   (`LocalTypeDeclarationImpl`), lambdas (`LambdaImpl`) — via `typeInfoRecurseAllPhases`. Walking the rewired type
   to re-register it therefore misses them, and the registry keeps handing out the replaced object (two live
   `TypeInfo` for one fqn, against the invariant `InfoByFqn` documents). Fix: ask `InfoMap.rewiredTypes()`.
   The same walk in `invalidate` left anonymous types behind, and the *next* re-scan threw
   `Duplicating type ...$0`. There, ask each type instead: `primaryType().equals(theInvalidatedOne)` — it climbs
   enclosing types **and enclosing methods**, so it claims anonymous types, and it subsumes the source-set check.
3. **`ParseOptions.invalidated()` is never null** — the Builder defaults it to `NOT_INVALIDATED` (a named constant,
   matched by identity). A `== null` check for "did the caller ask for incremental?" is dead code. Getting this
   wrong made *every* parse incremental; combined with `onlyPreload()` (which parses a warm-up type and so records
   its source set), a subsequent full parse found the set "known and unchanged" and scanned **nothing**: 97
   failures across the modification suites.
4. **A default is not a decision.** `FieldInfo.isPropertyFinal()` and `isIgnoreModifications()` read their property
   with `getOrDefault(…, FALSE)`, so an *undecided* field reads as "not final". The computing analyzers may use
   them (a wrong guess only delays a fixed point); anything that reports must use `getOrNull`.
5. **Rewiring reuses the `CompilationUnit`** (`rewirePhase0`: `new TypeInfoImpl(compilationUnit(), simpleName)`).
   So fingerprints survive a rewire for free, and `assertSame(pt1.compilationUnit(), pt2.compilationUnit())` is the
   way to tell a rewire from a re-parse.
6. **The `RunRewireTests` driver must reload after restoring.** It edits a file, re-parses, restores it — leaving
   the analyzer's fingerprint one edit ahead of the disk. The edit is deterministic, so a file picked twice hashes
   to exactly what was recorded, `reloadSources` correctly says "unchanged", and the driver's assertion fires. Worse
   quietly: every touched file stayed "changed" for ever, so eventually everything was invalidated and nothing was
   ever rewired — the loop stopped testing the thing it exists for. It now reloads after restoring too (reverting is
   a rewire in its own right). Symptom to recognise: `N of M changed` where N > 1 for a single edit.

### Testing traps

- **Gradle aborts the whole run at the first failing task**, so later modules serve **stale** test XML. Two
  "failures" I reported this session were a previous run's results. Re-run the module alone before believing red.
- **A dependent source set resolves through javac's class path**, i.e. against the *compiled artifact* of the set it
  depends on — not against another set's in-memory sources. A two-source-set in-memory test cannot work; write real
  files and compile the dependency to a class directory (`TestInvalidate`), or take the artifacts off the test's own
  class path (`TestJavaInspector6MultiProject.artifactOf`).
- Assertions in `RunRewireTests` are `assert`, so they only fire under `-ea` (Gradle tests: on; production: off).

## Where to look

| | |
|---|---|
| protocol | `TypeInfoImpl.rewirePhase0..3`, `InfoMapImpl`, `FactoryImpl.newInfoMap` |
| API | `JavaInspector` (`InvalidationState`, `Invalidated`, `NOT_INVALIDATED`, `ReloadResult`, `reloadSources`), `InfoMap` |
| in-house | `integration/JavaInspectorImpl.parse` (phase 1 decides per file; the InfoMap is built after phase 3), `integration/CompiledTypesManagerImpl` |
| openjdk | `openjdk/JavaInspectorImpl.reparse` + `actionFor` + `reloadSources` + `listSourceFiles`, `openjdk/CompiledTypesManagerImpl`, `InfoByFqn` |
| fingerprints | `ScanCompilationUnits.fingerPrintOf` (openjdk), `MD5FingerPrint` |
| driver | `run-openjdk/RunRewireTests` = `run-main/RunRewireTests` (identical), wired via `--analysis-steps rewire-tests` |
| tests | openjdk `TestInvalidate`, `TestReloadSources`, `TestReloadSourcesFromDisk`, `TestFingerPrint`, `TestReloadPrerequisites`, `TestJavaInspector6MultiProject.testRewirePerSourceSet`, `run-openjdk/TestRewireTests`; in-house `invalidate/TestInvalidate` |

## Open: rewiring and prepwork/analysis data

**A rewire drops all analysis data.** No rewire phase copies `analysis()`; `MethodInfoImpl.rewirePhase3` ends on a
bare `// analysis?`. Measured — set a property, rewire, read it back:

```
before: type=@Immutable  method=true  statement=true
after : type=null        method=null  statement=null
analysis empty after rewire? type=true method=true statement=true
```

So a rewired type comes back with an empty `analysis()`, at every level — including **statements**, which is where
prepwork keeps `VariableDataImpl.VARIABLE_DATA`, and where the link module keeps its own. Everything computed about
a rewired type is lost, and must be recomputed.

Whether that is right is the open question:

- It is *safe*: a rewired type's data was computed against objects that no longer exist, and values like
  `IMPLEMENTATIONS` (`Value.SetOfMethodInfo`) or `LINKS` hold `Info` references that would need rewiring themselves
  — a `Value` would need a `rewire(InfoMap)` of its own. Copying them naively would smuggle stale objects into the
  new CST, which is the very bug class in "Traps" 1 and 2.
- It is *wasteful*: REWIRE means "unchanged". Recomputing the prepwork of every downstream type is most of the work
  a reload was meant to avoid — and it is the reason for the whole exercise.

Note `translate` already has an answer where rewiring does not: `TranslationMap.isClearAnalysis()`, and
`TypeInfoImpl`/`MethodInfoImpl` consult it. That is the precedent to look at first.

Points to settle before writing code:

1. Which properties can be carried as they are (plain values: `Bool`, `Immutable`, `Independent`), and which hold
   `Info` references and would need rewiring (`IMPLEMENTATIONS`, `LINKS`, `METHOD_LINKS`, `GET_SET_FIELD`,
   `PARAMETER_ASSIGNED_TO_FIELD`, `VARIABLE_DATA`, …). The second group is the whole problem.
2. Where the hook goes: a `Value.rewire(InfoMap)` (mirroring `Value.translate`), or a per-property allow-list, or
   `clearAnalysis`-style opt-in on the InfoMap.
3. Statement-level data is the bulk of it (`VARIABLE_DATA` per statement), and statements are rewired in phase 3 by
   `Block.rewire` — check whether statement `analysis()` is carried there at all (measured above: it is not).
4. Whoever consumes analysis after a reload must not silently see an empty map where it expects values. Today the
   runners re-run prep over `summary.parseResult()`, so nothing notices — that will change the moment prep data is
   meant to survive.

## Not done

- **Kotlin inspector**: no reload/rewire. `InfoByFqn` is already shared with it (`KotlinInspector`,
  `KotlinCompiledTypesManager`), and the CST machinery is language-agnostic, so the shape should follow the openjdk
  one. Its anonymous types come from `KotlinScan`/`KotlinBodyConverter`, which already call
  `getAndIncrementAnonymousTypes` like the Java front-ends.
- **openjdk selectivity**: a source set is re-scanned whole. Letting javac parse the set but building CST only for
  the INVALID units (unchanged types keep their objects) is the refinement; `singleSourceSet` already puts source
  roots on `SOURCE_PATH` for `restrictToPackages`, which is the mechanism.
- **A list of anonymous types on `TypeInfo`** — considered, deferred. All nine creation sites across the three
  front-ends funnel through two factories (`newAnonymousType`, `newTypeInfo(enclosingMethod, …)`), each called right
  after `Builder.getAndIncrementAnonymousTypes()` — a counter that is the degenerate form of the list. It would
  retire `IsolateMethod:642` ("TypeInfo.visit() is unsupported, so descend into the bodies of its members
  ourselves"). Against: a second source of truth for three front-ends, the rewire and `translate` to maintain — and
  the rewire bypasses both factories (`rewirePhase0` constructs directly), so it would not remove the hook
  `rewiredTypes()` needed.
- `InfoByFqn.removeAllSources()` calls `removeIf` on the lists in `multiTypeByFqn`, which are immutable
  (`List.of`/`toList`). It throws the moment that map is non-empty; it survives only because the tested set-ups
  never populate it. `removeType` avoids the hazard by rebuilding.
