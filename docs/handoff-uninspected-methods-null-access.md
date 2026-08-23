# A method the analyzer sees has never been inspected: `MethodInfo.access()` is null

*Written 2026-08-23 from `ws/python`; **root cause found and fixed the same day from `ws/server`** — §8, which
supersedes the open question in §4 and §6. The symptom, the mechanism and the 90-second replay below are as
they were established; §5's dead ends are still dead.*

## 8. The root cause (2026-08-23, later the same day) — and the fix

**Every one of the 18 is the method in which the source scan threw.** Matching the 18 names against the log's
`Caught exception in method X` lines is 18 for 18 (`Variable.fullyQualifiedName`, `Codec.decode`,
`MaddiDaemonProcess.ensureStarted`, …); and "all interface methods" (§2) was a coincidence of the majority —
`InspectionImpl.Builder.setAccess` and `MaddiDaemonProcess.ensureStarted` are not. The run dropped **313**
compilation units, not 51 (`parseErrorCount` counts javac diagnostics, `Dropping compilation unit` counts drops);
the drops are the per-module configuration missing `maddi-annotation` / `slf4j` on most modules' class paths
(`package io.codelaser.maddi.annotation is declared in the unnamed module, but module … does not read it`).

Three steps, each established by reading the code and then by a test that runs:

1. **`ScanCompilationUnit.visitMethod` registers before it resolves, and computes the access last.** The method
   is put on its type (`currentType.builder().addMethod`) and into the scanner's symbol map (`typeData.put`,
   ScanCompilationUnit:858) *before* the annotations are converted (the parameter annotation `@Modified` is what
   threw for `Codec.decode`), and `computeAccess()` was the final statement of the method (:1020). A throw in
   between leaves a registered method with no access and no commit. `visitVariable` (fields) and
   `ClassSymbolScanner.addMethodToType` / `addFieldToType` (the symbol path) had the identical order.
2. **The unit is dropped, the type is not.** Phase 2 of `ScanCompilationUnits.scan` never adds the unit's types
   to `primaryTypes` (:274, the throw skips it), so the end-of-scan walk (§4) never visits them. But phase 1 had
   already registered the type with the class-symbol scanner, so it is "loaded for this source set" and stays
   reachable from every surviving signature — and, once committed, it is analysed like any other type.
3. **The commit loop completes the type *around* the abandoned method.** `JavaInspectorImpl` ("copy into CTM",
   :795–804) commits every loaded, uninspected primary type through `ClassSymbolScanner.commitType` →
   `loadType(COMPLETE)`. There, `addMemberToType` (:880–891) **skipped any member whose symbol was already in
   `methodSymbolMap`** — reading "in the map" as "the source scan handled it" — which is exactly the abandoned
   method; the `computeAccess().commit()` one line below was unreachable for it; and `loadType` then committed
   the **type**. The nested-`Builder` majority goes through `COMPLETE_SUB`, where `alwaysLoad` is true but the
   same `!methodSymbolMap.containsKey(ms)` conjunct skips it just the same. ⇒ a committed type holding an
   uncommitted method whose `access()` is null forever, and nothing shouts — `Codec` is NOT among the run's 116
   `Caught exception committing type` lines, because the skip is precisely what made its commit succeed.

   The other shape in the run (`MaddiDaemonProcess`): `commitType` threw on a *later* member whose parameter
   type (`JsonNode`) does not resolve — inside `addMethodToType`, which had registered that member and would
   have computed its access last, too. The type stays uncommitted and reachable; both methods answer null.

**So: "where is a `computeAccess()` missing?"** — not at one site. It was *present but last* in four places, and
*skipped* in one:

| site | was | now |
|---|---|---|
| `ScanCompilationUnit.visitMethod` | `computeAccess()` after annotations, parameters, body | right after `flagHelper.method(...)` |
| `ScanCompilationUnit.visitVariable` | after annotations, type, initializer | right after `flagHelper.field(...)` |
| `ClassSymbolScanner.addMethodToType` | after annotations, parameter types, overrides | right after `flagHelper.method(...)` |
| `ClassSymbolScanner.addFieldToType` | in the caller, after annotations | right after `flagHelper.field(...)` |
| `ClassSymbolScanner.addMemberToType` | a member already in the map is skipped; the type commits | an in-map, uninspected member is **finished** (`finishAbandonedMethod` and its field twin): access if null, `noSource()`, `emptyBlock()` / empty initializer, overrides from the symbol, commit |

`computeAccess` reads only the modifiers and the method type, both fixed by then; nothing between the old and the
new site adds a modifier. The COMPLETE modes run from `commitType` and `loadCompiledTypeOrNull`, both after the
source set's scan, so an in-map, uninspected member they meet can only be an abandoned one — not one still being
built. Both halves are needed: the early access answers `access()` whatever happens next (including a second
throw at commit); the finishing step is what stops a committed type from carrying uncommitted members.

**Evidence.** `maddi-java-openjdk/…/other/TestDroppedUnitMethodAccess` replays step 3 with the product's own
predicate and call, on four fixtures (throw in an outer method, in a nested `Builder` method, in a field's
annotation, and "the scan throws in one method and the commit in another"); all four are red on the old code
and green on the new. Corpus, §1's replay, same `maddi-permodule.json`, same 313 dropped units:

| | before | after |
|---|---|---|
| `access()`-null crashes in the result | 57 | **0** |
| `analysis crashed on …` findings (any cause) | 201 | **0** |
| element annotations | 23,826 | 23,938 |
| `Caught exception committing type` | 116 | 117 (`FactoryImpl`: the GAP #12 "already committed, second definition from a compiled artifact" family — the per-module config puts `maddi-cst-api/build/classes` on dependants' class paths; a separate issue) |

⚠ **The harness runs javac with lombok's processor; the product, for a project without lombok, with `-proc:none`.**
That decided whether the reproduction reproduced: with the processor and one unresolvable annotation in the source
set, javac handed the scan a *class* whose method bodies were not attributed, and it died on its implicit
constructor (`Unexpected null symbol for unqualified call to 'super'`) before any field was reached; with
`-proc:none` the same source attributed completely, as in the product. `CommonTest.annotationProcessing = false`
is the switch. Another instrument measuring itself first.

§4's suggestion — "add `.computeAccess()` at `scanJavaDocsAndCommit:450`" — is not done here. It is no longer
needed for this defect (the access now exists before anything can throw), and the leftovers that loop commits are
synthetics whose access was SET, not computed: `RecordSynthetics` adds `methodModifierPublic` alongside
`setAccess(PUBLIC)`, so recomputing would be harmless for those, but I did not audit every synthetic producer, and
an unconditional recompute overwrites a set value. If anything is added there, make it
`if (access() == null) computeAccess()` plus the assert the field loop already has.

Residual, deliberately not touched: a type whose `commitType` throws (117 in this run) is still reachable and still
half-built in every respect other than access. Whether such a type should be finished anyway or pruned from the
survivors' signatures is §6.3's policy question, unchanged.

---

*What follows is the note as written before the root cause was found.*

## 0. The three-line summary

An interface method reaches the modification analyzer with `hasBeenInspected() == false`. Reading its
`access()` then returns **null**, because `MethodInfoImpl` seeds `inspection` with a *Builder* and the builder's
access stays null until `computeAccess()` runs. It is fatal in the guard phase and merely isolated during
analysis, so whether a run dies is luck.

```
java.lang.NullPointerException: Cannot invoke "io.codelaser.maddi.cst.api.info.Access.isPrivate()"
  because the return value of "io.codelaser.maddi.cst.api.info.MethodInfo.access()" is null
    at AnnotationToProperty.lambda$simpleComputeIndependent$0(AnnotationToProperty.java:404)
    at AnnotationToProperty.simpleComputeIndependent(AnnotationToProperty.java:405)
    at ContractReader.contracts(ContractReader.java:38)
    at GuardAnalyzerImpl.guardType(GuardAnalyzerImpl.java:418)
    at IteratingAnalyzerImpl.analyze(IteratingAnalyzerImpl.java:715)
    at WarmAnalysisService.analyze(WarmAnalysisService.java:132)
```

## 1. Replay it (about 90 seconds, no IDE)

Everything lives in `jfocus-refactor-server/work/codelaser/pipeline/` in the **ws/python** workspace.

```bash
S=/Users/bnaudts/git/ws/python/jfocus-refactor-server/work/codelaser/pipeline
B=/Users/bnaudts/git/ws/python/maddi
export GRADLE_USER_HOME=/Users/bnaudts/git/ws/python/.gradle-home

# 1. the two temporary diagnostics that produced every number below
git -C $B apply $S/null-access-diagnostics.patch

# 2. build the daemon
box run --mb 8g --wait -- $B/gradlew -p $B :maddi-ide-daemon:installDist

# 3. drive it against maddi-as-one-project, in the per-module form the IntelliJ plugin sends
box run --mb 14g --wait --timeout 60m -- python3 $S/daemon-drive.py \
    $B/maddi-ide-daemon/build/install/maddi-ide-daemon \
    $S/maddi-permodule.json --xmx-mb 12288 --log ./daemon.log

grep -ao "NULLACCESS [^ ]*" daemon.log | sort -u        # the uninspected methods
grep -ac "NULLACCESSCOMMIT" daemon.log                  # commits that leave access null: ZERO, see §4
```

⭐ `maddi-permodule.json` is generated by `cli-to-permodule.py` from the javac-log input configuration, so
**no IntelliJ is needed** to reproduce an IDE-only shape:

```bash
python3 $S/cli-to-permodule.py \
    /Users/bnaudts/git/ws/python/jfocus-refactor-server/work/codelaser/build.inputConfiguration.json \
    $S/maddi-permodule.json --prefix "maddi/"
```

⚠ Remember to `git -C $B checkout -- maddi-cst-impl/...` afterwards: the patch adds `System.err` prints to
`MethodInfoImpl.access()` and `MethodInspectionImpl.Builder.commit()` and must not be committed.

## 2. What was measured

| | |
|---|---|
| distinct uninspected methods | **18** |
| `access()` calls returning null | 104 |
| `elementAnnotations` in that run | 23,826 |
| `parseErrorCount` in that run | **51** |
| commits leaving access null | **0** |

Every one of the 18 is an **interface** method, and most are in nested `Builder` interfaces:

```
io.codelaser.maddi.cst.api.analysis.Codec.decode(Codec.Context,PropertyValueMap)
io.codelaser.maddi.cst.api.element.CompilationUnit.Builder.addImportStatement(ImportStatement)
io.codelaser.maddi.cst.api.element.DetailedSources.details(Object)
io.codelaser.maddi.cst.api.expression.VariableExpression.Builder.setVariable(Variable)
io.codelaser.maddi.cst.api.info.ParameterInfo.Builder.setIsFinal(boolean)
io.codelaser.maddi.cst.api.variable.Variable.fullyQualifiedName()
io.codelaser.maddi.inspection.api.resource.InputConfiguration.Builder.setWorkingDirectory(String)
…
```

That they are all interface methods matters: `MethodInspectionImpl.Builder.computeAccess` has a branch that
makes an interface method PUBLIC, so had it ever run, these would be right. It never ran.

## 3. The mechanism (this part IS established)

`MethodInfoImpl`:

```java
private final EventuallyFinal<MethodInspection> inspection = new EventuallyFinal<>();   // :106
inspection.setVariable(new MethodInspectionImpl.Builder(this));                          // :123, in the ctor
public Access access()            { return inspection.get().access(); }                  // :460
public boolean hasBeenInspected() { return inspection.isFinal(); }                       // :95
```

Before commit, `inspection.get()` returns **the Builder**, so `access()` reads the builder's field. `commit()`
is the only thing that makes it final, and `MethodInspectionImpl.Builder.computeAccess()` is a SEPARATE
explicit call — not part of `commit()`. A method that is never committed therefore answers `access()` with
null forever, and nothing shouts.

## 4. Why the obvious fix is not the root cause

`ScanCompilationUnits.scanJavaDocsAndCommit` — the end-of-scan walk — already commits leftovers:

```java
if (!methodInfo.hasBeenInspected()) {
    methodInfo.builder().commit();                 // :450  ← no computeAccess()
} // possible: sythetics
...
assert fieldInfo.access() != null : "Null access for " + fieldInfo;   // ← the FIELD loop already asserts it
fieldInfo.builder().commit();
```

Adding `.computeAccess()` at :450 (plus the matching assert) is right and should be done. **But it cannot be
what is happening here**, because the commit-site diagnostic fired **zero** times: these 18 methods are never
committed by anything. So the walk never reaches them.

Two ways that happens, and deciding between them IS the open question:

- the walk is entered only `if (!primaryType.hasBeenInspected())` (`ScanCompilationUnits:291`), or
- the type was **dropped**: on a throw inside the walk the caller does `ptIt.remove()` (`:305`), removing it
  from `primaryTypes` while the `TypeInfo` stays reachable from other types' signatures.

The run had **51 parse errors**, which makes the second the leading hypothesis — but it is a hypothesis.

## 5. Dead ends — already tested, do not repeat

| candidate | why it is not it |
|---|---|
| `ScanCompilationUnit.recursivelyCommit:1272` commits methods without `computeAccess()` | True, and worth fixing, but it only handles **local and anonymous** types. None of the 18 is one. |
| `ensureMethod` → `addMethodToType` forgets access | `addMethodToType` calls `builder.computeAccess()` **unconditionally** (~:1188), and `computeAccess` assigns on every branch. |
| `RecordSynthetics.createAccessor` | Calls `setAccess(runtime.accessPublic())` (`:88`). |
| `ClassSymbolScanner.addMemberToType` (bytecode path) | Calls `computeAccess()` at `:876`, and again for enclosed types at `:913`. |

## 6. The open question, and how to attack it

**Why are these 18 never committed?** Suggested order:

1. Extend the patch to print a stack trace at *creation* (`MethodInfoImpl`'s constructor, or `put(ms, method)`
   in `addMethodToType`) for methods that are still uninspected at the end of parse. Creation is where the
   information is; `access()` and `commit()` are both too late, which is what cost this session two rounds.
2. Print, for each of the 18, whether its owning primary type was dropped at `ScanCompilationUnits:305`, and
   the failure that dropped it. That decides §4's two branches directly.
3. If it is the dropped-type branch: the question becomes a policy one — a dropped type's `TypeInfo` remains
   referenced by types that survive, so either it must be finished anyway, or references to it must be pruned.

⛔ **Do not "fix" this by defaulting a null access.** `computeAccess()` gives interface methods PUBLIC and
everything else its modifier's access; a blanket PACKAGE default would silently weaken independence and
container verdicts on exactly the types that are hardest to notice.

## 7. Why it surfaced now, and what it costs

Not a regression in the inspection code. Two recent IDE-daemon changes made it *reachable*:

- **Per-module input configurations** (`ide-todo.md` §5 A) removed the OOM, so the run now converges —
  17 iterations, `done? true` — and reaches `GuardAnalyzerImpl`, which runs **after** the fixpoint.
- **Partial-parse analysis** (`ide-todo.md` §5 C) analyses a tree that has parse errors instead of returning
  findings-only. Before C, a run with 51 parse errors never reached the analyzer at all.

⚠ The same null access also throws inside `TypeContainerAnalyzerImpl.lambda$go$0:46` **20 times** during
analysis, where `setFaultTolerant(true)` isolates it. Only the guard phase has no such isolation. So a run
either dies or silently loses those elements depending on where the null lands — which is why the headless
replay in §1 *completed* (23,826 annotations) while the IntelliJ run on the same code died.

Related: `ide-todo.md` **#29** — when the daemon throws, `applyResult` never runs, so the IDE keeps the
previous run's tree and a converged, complete analysis is discarded over one method.
