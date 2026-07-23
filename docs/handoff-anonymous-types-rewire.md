# Handoff — anonymous types are never registered in the rewire `InfoMap`

**Status:** fixed (2026-07-23, see §10). **Diagnosed:** 2026-07-23.
**Scope:** `maddi-cst-impl` (`TypeInfoImpl.rewirePhase0/1`, `InfoMapImpl`), with the symptom surfacing in
the refactor service's Tier 2 incremental reparse.
**Companions:** [`rewiring.md`](rewiring.md) (parser-level reload/rewire),
[`analysis-rewiring.md`](analysis-rewiring.md) (the carry / fingerprint layer that trips over this).

Found by stress-testing Tier 2 incremental reparse end-to-end from the refactor server's script
runner — the write → read loop the service's own tests cannot do. Full session context:
`jfocus-refactor-server/DSL-EXERCISE-LOG.md`, section "Tier 2".

---

## 1. The bug in one sentence

`TypeInfoImpl.rewirePhase0` and `rewirePhase1` register a type's members in the `InfoMap` by walking
`subTypes()` plus the type's own constructors, methods and fields — but **anonymous types are not in
`subTypes()`**, so their members are never registered, and any later lookup of one fails hard.

## 2. Symptom

Rewiring a carried analysis value that references an anonymous class's member:

```
java.lang.NullPointerException: Cannot find
  org.jetbrains.java.decompiler.modules.decompiler.vars.VarVersionsGraph.$0.getReversePostOrderList()
	at org.e2immu.language.cst.impl.info.InfoMapImpl.methodInfo(InfoMapImpl.java:187)
	at ...
	at org.e2immu.analyzer.modification.prepwork.variable.impl.LinksImpl.rewire(LinksImpl.java:541)
	at org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.rewire(...:189)
```

The first observation of it came through `FieldReferenceImpl.rewire` → `InfoMapImpl.fieldInfo`
instead, so it is not specific to methods: any `Info` kind reached from a carried value can trip it.

## 3. Evidence chain

Each link independently checkable:

1. **The missing member belongs to an anonymous class.** `$0` is the anonymous-type naming.
   `VarVersionsGraph:81` is
   `engine = new GenericDominatorEngine(new IGraph() { public List<? extends IGraphNode> getReversePostOrderList() {…} });`
2. **Anonymous types are never added as subtypes.**
   `FactoryImpl.newAnonymousType(enclosingType, index)` → `new TypeInfoImpl(enclosingType, index)`.
   The parser (`ScanCompilationUnit:2622`) then calls `setEnclosingMethod(currentMethod)` and sets the
   nature/access/parent — but **never `enclosingType.builder().addSubType(...)`**. The enclosing type
   only carries a *count* (`anonymousTypes()` / `getAndIncrementAnonymousTypes()`); the type itself is
   reachable only through the method body.
3. **The rewire walks only `subTypes()`.** `TypeInfoImpl.rewirePhase0` (line ~738) and `rewirePhase1`
   (line ~761) iterate `subTypes()` recursively, then the type's own `constructors()`, `methods()` and
   `fields()`, calling `infoMap.put(...)` for each. Nothing walks anonymous types.
4. **`InfoMapImpl` fails hard exactly in this case.** Each lookup does
   `setOfPrimaryTypesToRewire.get(x.…primaryType())`; if that map is **absent** it returns the argument
   unchanged (the type is not being rewired — fine), and if it is **present** it does
   `requireNonNull(map.get(x))`. An anonymous member's primary type *is* in the rewire set, so the
   lenient branch does not apply and the strict one blows up.

## 4. Reproduction

Deterministic, ~15 s once warm; reproduced identically on every attempt. Needs a project with a
cross-source-set REWIRE cone — a test source set depending on main is enough, so essentially any real
project. fernflower is the one used here.

```
./gradlew :codelaser-refactor-graalpy:scriptRunner -PclProject=fernflower-rw \
    -PclRunnerDir=runner-f1 -PclMode=rw -PclIncremental=true
```

Then, through the runner's inbox: `result = len(query.types())` to warm the prep, followed by any
local rename in a main-set type, e.g.

```python
result = rename.localVariable(
    targetMethods=["org.jetbrains.java.decompiler.struct.consts.ConstantPool.getPrimitiveConstant(int)"],
    currentLocalVariable="cn", newLocalVariable="constant")
```

The log then shows `Incremental reparse: 1 changed in 1 source set(s) (200 types rescanned), 22
rewired` followed by the NPE and `falling back to dropping project data`.

**A negative control comes free:** the same procedure on a project whose reparse reports `0 rewired`
(no cross-set dependents) succeeds — that was the 14-type fixture, where the incremental path works
and is worth ~14× (71 ms against 999 ms).

> **Do not repeat this:** a hand-built minimal two-source-set fixture was attempted and abandoned. It
> foundered on `inputConfiguration` details — the test source set would not resolve the main set,
> first through stale `uri` fields and then by not being parsed at all. That is a harness problem, not
> the bug, and the fernflower recipe above needs no fixture.

## 5. Blast radius

- **Any rewired type containing an anonymous class or lambda.** In practice that is most real code, so
  the Tier 2 incremental path currently never survives on a project with a test source set.
- **The failure is contained.** The service catches it and falls back to dropping all project data, so
  results stay correct — verified byte-identical to a cold analysis. What is lost is the entire
  incremental win: fernflower's post-write read took 148.9 s against a 160.5 s cold baseline.
- **`InfoMapImpl.seed` looks vulnerable in the same way.** It is used for already-rebuilt (not rewired)
  primary types and has the identical shape — type, `constructorsAndMethods()`, parameters, `fields()`,
  recurse into `subTypes()`. That path did not fire in these runs, but if a rebuilt type's anonymous
  members are ever looked up, it will fail the same way. Worth fixing together.
- **Lambdas.** `TypeInfo.isAnonymous()` is documented as "a lambda, or created with `new T() {…}`", so
  lambdas are presumably created by the same route and share the gap. Not separately confirmed here.

## 6. The design question the fix has to answer

Not a one-liner, which is why this is a handoff rather than a patch: **where does the traversal of
anonymous types belong?**

They are reachable only through method bodies, while `rewirePhase0/1` are deliberately structural —
they run *before* bodies are rewired, precisely so that body rewiring can resolve references through
the map. Some options, in the order they occurred to me, none endorsed:

- give `TypeInfo` a way to enumerate its anonymous types (the parser knows them; only the count is
  kept today) and walk it alongside `subTypes()` in both phases;
- register anonymous members lazily, when body rewiring creates them, and make `InfoMapImpl` tolerant
  of a member it has not seen yet;
- keep the strictness but have `InfoMapImpl` fall back to identity for anonymous owners, which is a
  smaller change and possibly sound given anonymous types cannot be referenced from outside their
  enclosing method — that soundness argument needs checking, not assuming.

The third is tempting because it is small; I would want someone who owns this code to say whether a
carried link can legitimately outlive the anonymous type it names.

## 7. Verifying a fix

1. The reproduction in §4 should log `Reloaded + reparsed incrementally after rename.localVariable`
   with no fallback.
2. The post-write read should drop from ~150 s to something in the seconds range, and its result must
   stay byte-identical to a cold run of the same post-edit sources — the soundness gate used
   throughout the Tier 2 stress test.
3. A regression test belongs next to the existing rewire tests in `maddi-run-openjdk`
   (`TestEarlyCutoffSkip`, `TestEarlyCutoffWorklistDriver`), over a type containing an anonymous class
   in the REWIRE cone.

## 8. Already landed

`1cbb13ee` — `InfoMapImpl.fieldInfo` and `parameterInfo` now carry the same `"Cannot find …"` message
their `typeInfo`/`methodInfo` siblings already had, plus owner and primary type. Message-only, no
behaviour change. It is what turned an unattributed `NullPointerException` into a named member in a
single run, and it is worth keeping whichever way the fix goes.

## 9. Resolution

Fixed in `InfoMapImpl` by making the strict lookups recover instead of failing, keeping the existing lazy
design (anonymous types stay out of the structural phases). Two recovery routes, one per root cause the
diagnosis turned up:

- **Rebuilt (rescanned) owner — the dominant production trigger.** `seed` maps a rebuilt primary's members
  onto themselves but never walks its anonymous types, so carrying a re-scanned sibling's analysis through an
  anonymous member missed the map. A rebuilt object *is* the new object, so `typeInfo`/`methodInfo`/
  `fieldInfo`/`parameterInfo` now return the argument unchanged (identity) when the owner's primary type is
  rebuilt (`isRebuilt`: present in the map but not in `toRewire`). This is the §5 "seed looks vulnerable"
  path, and it is what the fernflower run (200 rescanned types) actually hit.
- **Rewired owner.** For a type we rewire, the anonymous owner is registered on demand via
  `typeInfoRecurseAllPhases` (`registerAnonymousOnDemand`), which is idempotent and — after a small change to
  resolve its enclosing type recursively — walks the enclosing chain so nested anonymous types resolve too.
  Defensive against any ordering race the per-primary phase-3 sequencing could still create (a carried value
  naming an anonymous member in a type whose body has not yet been rewired).

`typeInfoNullIfAbsent` is deliberately left strict (it is the "is this being rewired?" probe used in
assertions). The soundness question §6 raised — can a carried link legitimately outlive the anonymous type it
names — is answered by construction: we never hand back a stale object; we register (or, for rebuilt types,
identity-map onto) the live one.

**Regression test:** `TestRewireAnonymousType` in `maddi-run-openjdk`, next to the early-cutoff tests.
Deterministic, no fixture-resolution problems (the §4 abandoned-fixture trap): it reproduces the seed crash
directly at the `InfoMap` level — a rebuilt type's anonymous member looked up through a seeded map (NPE
"Cannot find a.T.$0.get()" before the fix, identity after) — and checks a full rewire still resolves
anonymous members to fresh copies. Confirmed red before / green after.

## 10. Separately observed, not diagnosed

On the incremental path only (zero occurrences on a cold run of the same sources), the analyzer logs
`TolerantWrite - Keeping immutableType=@FinalFields, refusing downgrade to @Mutable` — 24 times over 5
*carried* types, never the edited one. Clear-before-recompute exists to prevent exactly this. It is
not a tier misclassification (`immutableType` is `CROSS_TYPE_DERIVED`, hence inside
`CROSS_TYPE_DERIVED_ONLY`). Unproven hypothesis:
`ModificationAnalysisResource.clearCrossTypeDerived(t)` clears only the type about to be recomputed,
while analysing `t` can write properties onto other types that still hold carried values. No
divergence was observed — the result matched cold exactly — but a refused downgrade is precisely how a
stale-high verdict would survive, and the fixture was too small to force one.
