# Rewiring: how it works, and what bites

Reloading and rewiring after a source change. Written as a handoff: what the machinery is, what state it touches,
and the traps — several of which cost real time to find, and none of which are visible in the code.

- **Status: openjdk inspector complete and running end to end** (`RunRewireTests`, 200 iterations). In-house
  inspector: was already there, and two shared bugs were fixed in it on the way. Kotlin inspector: not started.
- Companion: `maddi-modification-analyzer/definitions.md` (concepts), `guard-mode-analysis.md` (guard work),
  `analysis-rewiring.md` (the analysisFingerprint — skipping link+analyzer recomputation, the sequel to this doc).

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
2. The caller turns that into an `Invalidated` (see `RunRewireTests`): changed → INVALID; their dependents, from
   `prepwork/PrimaryTypeUseGraph.dependentsOf` → REWIRE; the rest → UNCHANGED.
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

7. **`IMPLEMENTATIONS` points against the direction of rewiring, and structural equality made it self-sealing.**
   `MethodAnalyzer.addImplementation` (`:356-360`) writes it onto the *overridden* abstract method, which lives in a
   supertype — **upstream**. So the analysis has a back-edge the CST does not, and the closure argument below (no
   UNCHANGED type reaches an INVALID one) does **not** protect it: interface `I` stays UNCHANGED, class `C` is
   REWIRE, and `I.m`'s `IMPLEMENTATIONS` keeps the `C.m` that was replaced — for ever, because `I` is never rewired.
   Re-running prep did not repair it: the set is mutable (`ValueImpl:1168`) and `MethodInfo.equals` is structural
   (fqn + source set, `MethodInfoImpl:135-141`), so `add(newC.m)` on a set holding `oldC.m` found them equal, kept
   the incumbent, and returned false. **The BASIC RULE that makes rewiring work is what made this set never update.**
   Fixed by making `add` replace rather than insert; regression test `prepwork/TestImplementationsAfterRewire`.
   Still open: nothing *prunes* the set, so an implementation deleted from the source stays listed.

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
| who-depends-on-whom | `prepwork/callgraph/PrimaryTypeUseGraph` (projects + transposes `ComputeCallGraph`'s `G<Info>`), test `prepwork/TestPrimaryTypeUseGraph` |
| API | `JavaInspector` (`InvalidationState`, `Invalidated`, `NOT_INVALIDATED`, `ReloadResult`, `reloadSources`), `InfoMap` |
| in-house | `integration/JavaInspectorImpl.parse` (phase 1 decides per file; the InfoMap is built after phase 3), `integration/CompiledTypesManagerImpl` |
| openjdk | `openjdk/JavaInspectorImpl.reparse` + `actionFor` + `reloadSources` + `listSourceFiles`, `openjdk/CompiledTypesManagerImpl`, `InfoByFqn` |
| fingerprints | `ScanCompilationUnits.fingerPrintOf` (openjdk), `MD5FingerPrint` |
| driver | `run-rewire/RunRewireTests` — one copy, shared by `run-main` and `run-openjdk`, wired via `--analysis-steps rewire-tests` |
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

**Dropping is mostly correct, not merely safe.** An earlier version of this doc called it "wasteful". That was wrong,
and the reason is a one-line argument:

> REWIRE is *defined* as "reaches an INVALID type".

So a rewired type's **inter-type derived** values (`LINKS`, `IMMUTABLE_TYPE`, `INDEPENDENT_*`) are exactly the ones
whose inputs changed. Carrying them would re-point the references at the new objects while keeping a conclusion
computed against source that no longer exists — a wrong answer wearing fresh references. Dropping them is right.

The same closure gives the other half for free: **no UNCHANGED type can reach an INVALID one** (if it did, the use
graph would make it reachable from the changed set, and it would have been REWIRE). So UNCHANGED types keep their
objects *and* their analysis stays sound, with nothing to do. The two states are already exactly right.

The only prize is the **intrinsic** layer: `VARIABLE_DATA` is computed from the method's own body, which by
definition of REWIRE did not change, and it is the bulk of prepwork's cost. It has exactly one cross-type
dependency — `ApplyGetSetTranslation:67` reads `GET_SET_FIELD` off the *called* method — so carrying it is sound
unless a method it calls in an INVALID type changed its getter/setter status. Narrow and checkable. That is the
business case; everything else in analysis should simply be recomputed.

### The hook exists, and is orphaned

`Value.rewire(InfoMap)` and `PropertyValueMapImpl.rewire(InfoMap)` (`:37-41`, maps every value through
`value.rewire`) are both written. **The only callers are two expression sites** (`ConstructorCallImpl:435`,
`MethodCallImpl:437`) — no `rewirePhase` calls it at any Info level. Hence the bare `// analysis?`. Where the values
stand:

| tier | classes |
|---|---|
| mapped | `GetSetValueImpl`, `FieldBooleanMapImpl`, `VariableBooleanMapImpl`, `AssignedToFieldImpl`, `PostConditionsImpl`, `PreconditionImpl`, `GetSetEquivalentImpl`, `IndependentImpl` |
| `return this` (plain: ints, strings) | `BoolImpl`, `ImmutableImpl`, `NotNullImpl`, `MessageImpl`, `ScopeImpl`, `CommutableDataImpl`, `IndicesOfEscapesImpl`, `SetOfStringsImpl` |
| throws `NYI` (holds Info, not written) | `SetOfInfoImpl` (`PART_OF_CONSTRUCTION`), `VariableToTypeInfoSetImpl`, `SetOfTypeInfoImpl`, `SetOfMethodInfoImpl` (`IMPLEMENTATIONS`), `ParameterParSeqImpl`, `VariableDataImpl` (+`Builder`), `LinksImpl`, `MethodLinkedVariablesImpl`, `VariableInfoMap`, `ListOfLinksImpl` |

There used to be a fourth, worst tier — **silently identity**: `Value.rewire` defaulted to `return this`, so a value
holding `Info` that forgot to override passed stale references through without a sound. That was Trap 1 one level
up. The default now throws, and every implementation states its choice, so the tiers above are exhaustive by
construction.

**`Independent` is not quite a plain lattice level, despite the name**: `IndependentImpl` carries
`List<MethodInfo> dependentExceptions`. An allow-list built from property names — which this doc previously proposed
— would have waved it through. It is narrower than it looks, though: the list is written from the annotated API only
(`AnnotationToProperty`), never by the modification analyzer's computations, and in practice for a single case, the
`remove()` of the `Iterator` returned by `java.lang.Iterable`, which enhanced for-loops depend on. That method lives
in a library type, which is never rewired, so `infoMap` hands it back unchanged. `rewire` maps it anyway (nothing in
the value guarantees the return type is not a source type) and returns `this` when the list is empty, which is both
the common case and what keeps the `DEPENDENT`/`INDEPENDENT_HC`/`INDEPENDENT` singletons intact. Test:
`prepwork/TestIndependentRewire`.

The useful split is not "plain vs Info-holding" (that is only *mechanical*); it is **"does its computation read
another type's analysis?"**, which is what decides whether carrying is sound at all. The two questions are
orthogonal: `VARIABLE_DATA` is stuffed with `Info` refs *and* carryable; `IMMUTABLE_TYPE` is a bare int *and* not.

Remaining points to settle:

1. Statement-level data is the bulk of it (`VARIABLE_DATA` per statement), and statements are rewired in phase 3 by
   `Block.rewire` — statement `analysis()` is not carried there (measured above).
2. `translate`'s `TranslationMap.isClearAnalysis()` is the precedent for the *opt-in shape*, but not for a working
   carry: when it does not clear, it does a raw `analysis().setAll(analysis())` — a verbatim copy that maps nothing.
   For a rewire that is precisely Traps 1 and 2. (Its polarity is also inconsistent: the interface default is
   `true`, `TranslationMapImpl.Builder`'s field default is `false`.)
3. Whoever consumes analysis after a reload must not silently see an empty map where it expects values. Today the
   runners re-run prep over `summary.parseResult()`, so nothing notices — that will change the moment prep data is
   meant to survive.

## Parked mid-flight (2026-07-16) — carrying analysis across a rewire

Started the prepwork/analysis carry. **The mechanism is committed as parked WIP but not yet validated** (the full
suite must run on a quiet host first — see the blocker below). Resume here.

**Done, committed unvalidated** (3 files, `cst-api` + `cst-analysis`):

- `Property.carryOnRewire()` — new dimension, **default `false`** (drop). A property that opts in claims its value
  is carry-worthy *and* that its `Value.rewire` maps every Info/Variable reference it holds.
- `PropertyImpl` — 3-arg constructor `(key, defaultValue, carryOnRewire)`; the 2-arg delegates with `false`.
- `PropertyValueMapImpl.rewire(InfoMap)` — now **filters**: carries only `key.carryOnRewire()`, drops the rest.
  This is the one behaviour change on a live path: the two expression callers (`ConstructorCallImpl:435`,
  `MethodCallImpl:437`) previously carried *every* value; they now drop non-carry ones. That is intended — a stale
  value is no more welcome in a rewired `MethodCall` than on a rewired method — but it is why the full suite must
  run before this is trusted.

Nothing is opted in yet, so behaviour is drop-everything, i.e. identical to today. `VARIABLE_DATA` is the first
intended opt-in.

**The property split is three-way, not two** — and one leg is a *correctness* issue the "Open" section above
undersells (it says "everything else should simply be recomputed"; that is false for the parse-time leg):

| category | examples | on a rewire | why |
|---|---|---|---|
| parse-time | `GET_SET_FIELD` (set by `FactoryImpl.setGetSetField` ← record synthetics, `KotlinScan`) | **must carry** | a REWIRE type is *never re-parsed*, so if not carried it is **lost** — prep will not re-derive it |
| intrinsic (prepwork) | `VARIABLE_DATA`, `PART_OF_CONSTRUCTION`, `FINAL_FIELD`, `RECURSIVE_METHOD`, `INSTANCEOF_SCOPE` | carry (the prize) | computed from the type's own body, which by definition of REWIRE did not change |
| cross-type derived | `LINKS`, `METHOD_LINKS`, `IMMUTABLE_*`, `INDEPENDENT_*`, `IMPLEMENTATIONS` | must drop | inputs are exactly what changed |

**`VariableData` is where the boundary is clean, and it is the module boundary.** It is a prepwork skeleton the
link module fills in; a per-field split of `VariableInfoImpl`:

| field | written by | on a rewire |
|---|---|---|
| `variable` | prepwork | carry, via `Variable.rewire(infoMap)` (exists) |
| `assignments` (`String`, `String[]`), `reads` (`List<String>`), `variableNature`, `isVariableInClosure` | prepwork | carry verbatim — no Info refs |
| `linkedVariables` (a `Links`) | **link** (`LinkComputerImpl`) | **drop** |
| `UNMODIFIED_VARIABLE` | **link** (`LinkComputerImpl`, `WriteLinksAndModification`) | **drop** |

Dropping the link fills is not just safe, it is *required*: `setLinkedVariables` has an overwrite guard, so a
carried non-null would fight the link re-run rather than be replaced.

**Two traps waiting in `VariableData` for whoever writes `rewire`:**

1. `VariableInfoContainerImpl` asserts **identity** (`evaluation.variable() == variable`), so its rewire must build
   the `VariableInfoImpl` and the container from the *same* mapped `Variable`, not map twice.
2. `previousOrInitial` is `Either<VariableInfoContainer, VariableInfoImpl>` — a `Left` points at **another
   statement's** container (`MethodAnalyzer:540`, `Either.left(vic)` over the previous statement's VD). So a
   per-statement `VARIABLE_DATA.rewire` is **not** independent per statement; the previous container must already
   be mapped. Carry statement analysis in statement order, or resolve the `Left` lazily through the InfoMap.

**Next actions to resume:**

1. Implement `VariableDataImpl.rewire` / `VariableInfoContainerImpl` / `VariableInfoImpl.rewire` — carry the
   prepwork fields mapped through `InfoMap`, drop `linkedVariables`; mind the two traps above.
2. Opt `VARIABLE_DATA` in (`carryOnRewire=true`); likewise `GET_SET_FIELD` (parse-time — its loss is a bug), and
   the plain intrinsics.
3. Wire statement-level carry: statements are rewired in phase 3 by `Block.rewire`, and `analysis()` is not carried
   there. The precedent to mirror is `Statement.rewireAnnotations(infoMap)` — a **default method on the `Statement`
   interface** (`Statement.java:173`) that every statement's `rewire` already calls to pass its annotations. Add a
   sibling `rewireAnalysis(infoMap)` there and have each statement carry its analysis through it (there are ~20
   statement `rewire` bodies, each already threading `rewireAnnotations`).
4. Tests both halves: a rewired method's `VARIABLE_DATA` survives with re-pointed variables; its link-written
   fields (`linkedVariables`, `UNMODIFIED_VARIABLE`, `LINKS`, `METHOD_LINKS`) come back empty.

**Blocker — validation deferred to a pristine Gradle host.** The full suite is currently flaky: an intermittent
javac NPE, `com.sun.tools.javac.code.Scope$StarImportScope.isFilled() ... tree.starImportScope is null`, thrown
inside `task.analyze()` at `ScanCompilationUnits.scan:110` — the **parse** phase, ~1 run in 4–5, across
`modification-link` / `-analyzer` / `java-openjdk`. **Not caused by the carry work**: an analysis-map filter cannot
crash javac's enter phase, and it still reproduced with the `carryOnRewire` change present *and* the openjdk lazy
loader (`setLazyLoader`) disabled. It is almost certainly the shared build host running several threads' Gradle
daemons at once (contention on javac state); `sv-integration` on Fable never sees it, and that branch has **no**
lazy-loader wiring at all. Diagnosis parked until the host is quiet — re-run the modification suites there before
reading any red as real, and before trusting the carry work green.

## Not done

- **Kotlin inspector**: no reload/rewire. `InfoByFqn` is already shared with it (`KotlinInspector`,
  `KotlinCompiledTypesManager`), and the CST machinery is language-agnostic, so the shape should follow the openjdk
  one. Its anonymous types come from `KotlinScan`/`KotlinBodyConverter`, which already call
  `getAndIncrementAnonymousTypes` like the Java front-ends.
  **The gate is the `JavaInspector` interface, not module structure.** `KotlinInspector` is a standalone class, not
  a `JavaInspector`, and the driver needs exactly three things it does not have: `reloadSources`,
  `parse(ParseOptions)` and `runtime()`. `kotlin-parser-plan.md` §3 already assigns "implement `JavaInspector`" to
  `maddi-inspection-kotlin`, and M5c lists "the full `JavaInspector` surface" as outstanding. Once that lands, the
  driver is reusable as-is — it is interface-typed throughout, `ComputeCallGraph` walks the language-agnostic CST,
  and even the `// some comment` edit it uses to move a fingerprint is valid Kotlin. Fingerprints per source file
  are the other prerequisite (openjdk's is `ScanCompilationUnits.fingerPrintOf`); `PrimaryTypeUseGraph` already
  works for any language.
  Not answered by the Java design: a **mixed Java+Kotlin module** is mutually referential and so is *one* unit,
  processed Kotlin-first (`kotlin-parser-plan.md` §5). Editing a `.java` file there can require re-running the
  Kotlin scan, because Kotlin resolved against that Java source — a dependency the CST call graph does not record,
  and which today's `Invalidated` (primary type → state, one front-end re-parsing) cannot express.
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
