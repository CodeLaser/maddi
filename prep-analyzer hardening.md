# Prep-analyzer hardening roadmap

Aspects of the prep analyzer (`maddi-modification-prepwork`) that still need hardening, from a
code audit on 2026-06-27. Paths are relative to
`maddi-modification-prepwork/src/main/java/org/e2immu/analyzer/modification/prepwork/` unless noted.

## Status / framing
- **Validated reliable** (owner): reads, assignments, always-escapes, core `VariableData`. Best-tested paths.
- **Stale docs to fix:** `PrepAnalyzer` header comment lists type-level "hidden-content / immutability /
  modification" — none are computed in this module. `PrepAnalyzer.java:166-167` claims the "more complicated
  order" is set up below, but below only sorts getters first; the real order is the call-graph linearization
  (`ComputeAnalysisOrder`).
- **Scope note:** linked-variable *computation* is **not** in prepwork — it lives in `maddi-modification-link`
  (~4,800 lines). Prepwork holds only the data model (`LinksImpl`) and a storage slot.

Severity: **H** high, **M** medium, **L** low.

---

## 1. Linked variables — data model bugs + least validated  (H)
- [ ] **H** `LinksImpl.equals`/`hashCode` compare only `primary`, ignoring `linkSet`
  (`variable/impl/LinksImpl.java:112-121`). `VariableInfoImpl.setLinkedVariables`
  (`variable/impl/VariableInfoImpl.java:53-64`) uses this for change-detection → an update with same primary,
  different links is silently dropped; a primary change hits `overwriteAllowed` (`Links.java:83-86`, true only
  when both empty) which **throws** otherwise.
  **NOT contained (correction, 2026-06-27):** `setLinkedVariables` is driven from `LinkComputerImpl` (`:698,725`)
  inside the link module's fixpoint (the `propertiesChanged` counter re-runs computation across methods; note
  link changes do *not* increment that counter). The primary-only `equals` is therefore likely **load-bearing**
  — it silently absorbs non-idempotent re-sets. Fixing `equals` alone would turn "silently keep first" into a
  hard throw whenever links refine across iterations. Doing this safely also requires reworking the overwrite
  semantics (allow link refinement, and probably drive convergence off link changes), spanning
  `maddi-modification-link`, with the link test-suite as the safety net. Treat as a link-module task, not a
  contained prepwork edit.
- [ ] **M** `isDefault()` / `merge` also key off `primary` only (`LinksImpl.java:103-105,142-145`); `merge`
  doesn't assert matching primaries or dedup links.
- [ ] **M** Convert assertion-only invariants to real validation/tests: virtual-field rules
  (`LinksImpl.java:250-281`), part-of / primary-equality (`:173,184-185`). Invisible in prod without `-ea`.
- [ ] **M** Author-flagged fragility in the `link` engine: `LinkGraph.java:422` (`// TODO shaky code, dedicated
  to TestStaticBiFunction,6`), `LinkGraph.java:236` (`// FIXME add the current type`),
  `LinkMethodCall.java:115` (`// FIXME hard-coded`), `MethodLinkedVariablesImpl.java:150` (link restrictions
  "not implementing… complicated"), `ShallowMethodLinkComputer` library heuristics (`:68,165,189,346`).
- [ ] **M** `ExpressionVisitor.java:85` hard-throws on unknown expression types (no graceful degrade).
- Tests: **zero linked-variable tests in prepwork**; engine tests live in `maddi-modification-link`.

## 2. Part-of-construction / final-field detection  (H)
- [x] **H** ~~`isAssigned` builds `VariableData` from only the method's last statement → branch/loop/early-return
  assignments missed.~~ **Investigated + fixed 2026-06-27.** The audit's stated mechanism was *wrong*: the
  last-statement `VariableData` is the cumulative merge of the whole body, so a field reference survives across
  branches, loops and early returns (verified — early-return, switch-expression and trailing-assignment cases
  all detect correctly). The **real** bug: a field assigned inside a **lambda / anonymous / local type** enclosed
  by the field's owner is wrongly reported effectively final. Root cause: `ComputeCallGraph.handleFieldAccess`
  only creates the `field -> method` edge when the accessing method's `typeInfo() == owner`; for a nested-scope
  method it creates a `method -> field` edge instead, which `computeEffectivelyFinalFields` (walking edges *from*
  the field) never sees. Fixing the edge direction in the call graph ripples into the link engine + analysis
  order (broke ~40 link tests), so the fix stays in finality: `computeEffectivelyFinalFields` now also scans
  `constructorsAndMethodsOfPrimaryType` for methods whose type `isEnclosedIn` the field owner (but isn't the
  owner) and calls `isAssigned` on them directly; `isAssigned` now returns `false` (instead of `assert`) when a
  body has no `VariableData` (e.g. `doNotRecurseIntoAnonymous`). Regression test
  `TestFinalFieldBranchAssignment` (lambda, anonymous, early-return, switch-expr, + final positive control).
  Note: link tests on the `openjdk` branch were **already failing before** this change (pre-existing).
- [ ] **M** Method-less primary types (interface with only constants) never get `FINAL_FIELD` /
  `PART_OF_CONSTRUCTION` computed (`:58-74`).
- [ ] **M** Cross-type / inherited-field assignment not modeled by the same-static-type narrowing
  (`:131-136`); static initializers (`<clinit>`) not traversed.
- [ ] **L** Early-return assumes part-of-construction and final-field are set together (`:82-85`); a partial
  prior run leaves permanently inconsistent state.
- Tests: branch/loop/early-return false-final, method-less type, cross-class finality — all uncovered.

## 3. Object-creation tracking (`trackObjectCreations`)  (M)
- [ ] **M** Only plain `ConstructorCall` recorded; **anonymous-class creations explicitly excluded**
  (`MethodAnalyzer.java:937-949`); array creations / factory calls / autoboxing not tracked.
- [ ] **M** OC variables behave like pre-method (field-like) vars and leak into every downstream merge instead
  of being block-local (`MethodAnalyzer.java:701-712,739-748`; `ObjectCreationVariableImpl.java`).
- [ ] **L** OC fqn keyed on `cc.source().compact()` → collisions for generated/macro code
  (`ObjectCreationVariableImpl.java:47`).

## 4. Merge across complex control flow  (M, systemic)
- [ ] **M** Merge completeness is entirely string-index coupled (`Util.atSameLevel:36-41`,
  `Assignments.lastAssignmentIsMergeInBlockOf:246-250`); any index-format change breaks merges silently.
- [x] **M** ~~New-style switch completeness ignores whether a `default` exists
  (`variable/impl/Assignments.java:222`): "all entries assign" ≠ definite assignment without default.~~
  **Fixed 2026-06-27.** Confirmed: a classic `switch(int)` with arms but no `default` wrongly got the `=M`
  merge marker (`hasBeenDefined` true). `assignmentsRequiredForMerge` now sets the target to
  `Integer.MAX_VALUE` (never complete) unless the switch is exhaustive: explicit `default` arm (condition is an
  `EmptyExpression`) **or** a pattern arm (empty condition list — a pattern switch *statement* only compiles
  when exhaustive, JLS 14.11.1.1). Verified maddi inserts **no** synthetic default for exhaustive sealed
  switches, so the pattern-arm branch is needed to avoid regressing them. Regression test
  `TestSwitchNoDefaultMerge` (no-default classic → not defined; with-default → defined; exhaustive sealed →
  defined). Caveat: a modern exhaustive *enum* arrow-switch without `default` (all constants) is treated
  conservatively as non-exhaustive — safe direction, matches classic Java DA.
- [ ] **M** Labeled break targeting an outer loop while inside an old-style switch is mis-attributed to the
  switch break variable (`MethodAnalyzer.java:795-800`). Untested.
- [ ] **M** Switch-**expression** branch assignments to outer vars treated as always-taken; the computed
  sub-block `VariableData` is discarded (`MethodAnalyzer.java:1004-1029`).
- [ ] **M** Negative-pattern scope uses `!`-literal parity (`MethodAnalyzer.java:1180`), so `!=`, `==false`,
  short-circuit-derived negations get the wrong pattern-variable scope.
- [ ] **M** Old-style switch fall-through (`MethodAnalyzer.java:528-587,684-699`) — most bespoke/order-sensitive
  code; low external test density.
- [ ] **L** `continue` (esp. labeled) never adjusts `breakCountsInLoop` (`MethodAnalyzer.java:184-191`).
- [ ] **L** `Assignments.java:238` `UnsupportedOperationException("NYI")` — latent trap for any future
  sub-block statement type not in the enumerated list.

## 5. Call graph & analysis order  (M)
- [ ] **M** `ComputeCallGraph.java:177` FIXME — static-qualifier type edges pollute ordering/cycles.
- [ ] **M** `ComputeAnalysisOrder.java:34` TODO — no intra-cycle prioritization; subgraph keeps hierarchy +
  code-structure edges (`:50`) that the call-graph comment warns "can cause cycles," relying on the linearizer.
- [ ] **M** Mutual recursion (A↔B, cross-type) not flagged `RECURSIVE_METHOD` — only self/enclosing
  (`ComputeCallGraph.java:379-397`).
- [ ] **M** Lambdas get no call edge (`:332-337`, commented out) → behavior inside lambdas not linked into the
  caller (feeds §2).
- [ ] **M** Annotation types filtered by `externalsToAccept` instead of `accept` (`:260-263`); possible NPE on
  type-variable `TypeExpression` (`:339`).

## 6. Getter/setter classification  (M)
- [ ] **M** `isSetter` returns true for **any void method**
  (`maddi-modification-common/.../getset/GetSetHelper.java:144`) — `clear()`/`close()` classify as setters
  (scope-limited today, fragile). `isComputeFluent` treats any `return this` as fluent (`:147-155`);
  `parameterIndexOfIndex` picks the index param purely by `isInt()` (`:157-167`).
- [ ] **M** Misses validating/multi-statement setters, defensive-copy setters, wither-style, `return (B) this`
  cast-fluent; indexed getters with constant/expression index.
- [ ] **L** `doGetSetAnalysis` caches/mutates `FLUENT_METHOD` as a side effect; not refreshed if the body
  changes after translation (`:59,67,137`).

## 7. Serialization IO  (M→H robustness)
- [ ] **H** No version/schema marker; `LoadAnalysisResults` reads by fixed positional index with unchecked
  casts (`io/LoadAnalysisResults.java:159-184`) → format drift = `ClassCastException`.
- [ ] **H** A stale/renamed `Info` on load aborts the whole file (`:167-170`); no skip-and-continue.
- [ ] **H** Properties the codec can't encode are silently dropped, no log (`io/WriteAnalysisResults.java:115`).
  `PrepWorkCodec` registers exactly one maddi property (`io/PrepWorkCodec.java:56-57`) — easy to forget new ones.
- [ ] **M** Jar reads use platform charset vs UTF-8 elsewhere (`io/LoadAnalysisResults.java:105`).
- [ ] **M** `DecoratorImpl.importsNeeded` is mutable instance state — not thread-safe if reused
  (`io/DecoratorImpl.java:66,391-395`).
- Tests: no negative/round-trip IO tests in prepwork (only indirect, in downstream modules).

## 8. Parallel mode  (M, unverified)
- [ ] **M** `PrepAnalyzer.typesProcessed` is a non-atomic `int` mutated under `parallelStream()`
  (`PrepAnalyzer.java:61,132,172`) — data race (diagnostic only).
- [ ] **M** One shared `MethodAnalyzer` serves all parallel threads (`PrepAnalyzer.java:58,95,170-171`); safety
  depends on it + the CST `analysis()` stores being thread-safe. Unverified, no `parallel=true` test.

## Cross-cutting
- [ ] Many real invariants are `assert`-only and vanish in prod → NPE / silent-skip (`LinksImpl` virtual-field
  rules; `MethodAnalyzer.java:392,503`; `doInitializerExpression:408`).
- [x] **H** `options.doNotRecurseIntoAnonymous()` NPEs (`MethodAnalyzer.copyReadsFromAnonymousMethod:1124`) —
  shipped option, zero tests, crashes. **Fixed 2026-06-27:** null-guard in `copyReadsFromAnonymousMethod`
  (covers method bodies and the field-initializer path); regression test `TestDoNotRecurseIntoAnonymous`.
- [ ] String-coupling everywhere (statement-index stages, `oc:` prefix, `§`/`§m` virtual fields, `END='~'`) —
  schema-less, unenforced.

---

## Suggested priority order
1. **Linked-variables data model** — `LinksImpl.equals`/`hashCode` + `setLinkedVariables` (silent-stale /
   hard-throw). §1.
2. **`isAssigned` last-statement-only** → false-final fields. §2.
3. **`doNotRecurseIntoAnonymous` NPE** + a test. Cross-cutting.
4. **Switch merge completeness** (`default`-aware) and **labeled-break-in-old-switch** mis-attribution. §4.
5. **IO robustness** (version marker + skip-unknown/stale) before the format is depended on more widely. §7.
6. **Parallel-mode thread-safety** audit before enabling broadly. §8.
