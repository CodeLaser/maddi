# Eventual immutability: design improvements for cst-api/cst-impl

> **Status update (2026-08-01, later the same day): §§1–3 are IMPLEMENTED.** Read
> `docs/eventual-info-hierarchy.md` §"The enforcement round" for what was built and, more usefully,
> for the three places where the specification below turned out to be wrong when measured:
> the ratchet baseline is **24, not 254** (this note predates the ungating); rules 3 and 4 need a
> computed **scope** or they produce ~20 violations on deliberately mutable services; and rule 4's
> "constructors must assign defensive copies" is the sweep that measured 24 → 10 — the copy belongs at
> the **call-site/Builder end**. §4 (asserted contracts) and §6 (named transitions) are still proposals.

**Status: proposal (2026-08-01), ready for implementation.** Distilled from the 2026-07-31/08-01
certification arc (`docs/eventual-info-hierarchy.md`, commits `1e020b39..54b895ab`): the composed
dogfood went from 26 to 254 surviving eventual verdicts, and every regression and blocker along the
way was catalogued. This note turns that experience into concrete work items. Sections 1–3 are
specified for another thread to implement without further context; section 4 answers the
"contracted-but-computable" design question; section 5 records what NOT to change.

**Headline finding first:** the design held up. ~254 types were certified with only two structural
source changes (`ProvidesImpl` commit-once, `FactoryImpl.precedenceMap` copy-once) and a handful of
annotations. Everything below is about *enforcement and declaration* of the existing idioms, not
restructuring.

## 0. The evidence (what each proposal is for)

| Incident | Cost | Idiom violated |
|---|---|---|
| `ProvidesImpl.addImplementationResolved` — raw `ArrayList` + adder (commit 22bd062a) | sank the whole `Element` hierarchy: dogfood 42 → 26 survivors, undetected for a week | resolve-once = `SetOnce<List<T>>` |
| `StatementImpl.propertyValueMap`, `TryStatementImpl.CatchClauseImpl.propertyValueMap` — 2 of 9 analysis stores without `@IgnoreModifications` | held the entire statement family at FinalFields-after-mark | every analysis overlay is disclaimed (road §050) |
| `UnaryOperatorImpl.hash` — lazy memo without the slot disclaimer | blocked `UnaryOperatorImpl` and its subtypes | memo slots are `@IgnoreModifications` (the `VariableImpl.cachedFqn/cachedHash` precedent) |
| `FactoryImpl.precedenceMap` — ctor filled a `HashMap` with `put()` | capped `FactoryImpl` (part-of-construction excludes assignments, not content calls) | build a local, `Map.copyOf` into the final field |
| `ZipLists.zip` — uncontracted maddi-util static | blocked the `Statement.withBlocks` abstract union | engine-jar surfaces carry modification contracts (`ListUtil` was annotated; `ZipLists` was not) |

None of these were caught at commit time; all were found days later by dogfood archaeology. The three
proposals close that loop.

## 1. The dogfood ratchet (highest value; do this first)

**Goal.** A CI-checked test that fails, naming the type, when a cst-api/cst-impl type loses its
eventual verdict on the composed dogfood — the day it happens, not a week later.

**Shape.** Mirror the splitclass corpus ratchets: a checked-in baseline file plus a slow test that
re-derives the survivor set and diffs.

- **Baseline file** `dogfood/expected-eventual-survivors.txt`: one fully-qualified type name per
  line, sorted — the survivor set of the composed run (254 entries as of `54b895ab`). Generate with
  `grep -E '^type .*eventual=@' <fpdump> | awk '{print $NF}' | sort`.
- **Allowlist** `dogfood/eventual-survivor-wobble.txt` for the known boundary nondeterminism.
  Current members: `org.e2immu.language.cst.api.info.ImportComputer.ImportDetails` (flips in/out
  run-to-run; the verification-residue boundary — same family as the historical
  `CompilationUnitPrinterImpl` wobble). A type on the allowlist may be present or absent without
  failing the ratchet.
- **Test** `TestEventualRatchet`, in `maddi-run-openjdk`, tagged `@Tag("slow")` (there is no separate
  `slowTest` source set — `slowTest` is a task that selects that tag out of `src/test/java`):
  1. Requires the generated `dogfood/cst-impl/build/inputConfiguration.json`; fail with a pointed
     message ("run `e2immu-write-input-configuration`, see dogfood/README.md") if absent — do NOT
     silently skip, a vacuous green ratchet is the failure mode `AGENTS.md` §Commands warns about.
  2. Enables the gates **programmatically**, not via env: `EventualCluster.ENABLED = true` (it is
     non-final for exactly this) and the modification-via-reachability option on the analyzer
     configuration (`setModificationViaReachability(true)`, the option `RunAnalyzer` maps `MODREACH`
     to). Restore both in a finally block.
  3. Runs prep+modification with the standard preloads (`maddi-aapi-archive` jdk, libs/test,
     libs/log — the same three directories every dogfood invocation uses; without preloading every
     figure reads zero, the documented trap).
  4. Collects the survivor set (types whose `EVENTUALLY_IMMUTABLE_TYPE` is eventual after the
     contraction — read the property, do not parse an FPDUMP), subtracts the allowlist from both
     sides, and asserts equality with the baseline. On failure print the missing and the new names
     separately; the message for a MISSING type should say "a commit has cost this type its eventual
     verdict — see docs/eventual-info-hierarchy.md §the drift round for the diagnosis recipe".
  5. New survivors are progress: the failure message for ADDED types should say "update the
     baseline".
- **Runtime budget:** one composed dogfood run, ~60–90 s — fine for `slowTest`.
- **Update workflow:** regenerate the baseline file from the test's own output (have the test write
  `build/eventual-survivors-actual.txt` on every run, so updating is `cp`).

**Non-goals.** Do not ratchet enm/eup counts or labels (too churny); the survivor *set* is the
signal. Do not run in the plain `test` task (needs the dogfood input configuration).

## 2. Conformance tests in cst-impl

**Goal.** Enforce the four idioms mechanically, in the ordinary `test` task, so drift fails at commit
time with a one-line diagnosis.

**Where.** As built: `maddi-inspection-openjdk/src/test/java/...` (`TestEventualConformance`), NOT
cst-impl — rules 3 and 4 need method bodies, so they need maddi's own `JavaInspector`, which cst-impl's
test source set does not have. The paragraph below assumed reflection would do; it will not. First check the retention
of `org.e2immu.annotation.rare.IgnoreModifications`: if `RUNTIME`, plain reflection over the
production classes is enough (walk the jar/classes dir, `Class.getDeclaredFields()`); if only
`CLASS`, reuse the byte-code route maddi already has (the inspector reads these annotations from
jars — the maddi-support classes prove it). Reflection is preferred for simplicity.

**Scope.** All production types in `org.e2immu.language.cst.impl.*`, excluding nested `Builder`
types (setter-bearing by design, the before-state face) and test fixtures.

**The four rules.**

1. **Every `PropertyValueMap`-typed field carries `@IgnoreModifications`.** (The audit that found the
   `StatementImpl`/`CatchClauseImpl` gap: 9 stores, 7 annotated.) Also apply to any future
   analysis-overlay type.
2. **Every non-final instance field is either `@IgnoreModifications` (a disclaimed memo slot) or of a
   `SetOnce`/`EventuallyFinal`-family type.** Current legitimate memos, all annotated:
   `VariableImpl.cachedFqn`, `VariableImpl.cachedHash`, `UnaryOperatorImpl.hash`. A new non-final
   field without the disclaimer is exactly the shape that sinks a type.
3. **No adder method mutates a final collection field.** Concretely: a method whose body contains
   `<finalCollectionField>.add(...)`/`.put(...)`/`.remove(...)` where the receiver is a final field
   of the declaring (non-Builder) type. This is the `addImplementationResolved` shape. Implementation
   note: reflection cannot see bodies; either (a) parse cst-impl with maddi's own `JavaInspector`
   (dogfooding — the test-harness `scan(fqn, content)` route exists) and walk for
   `MethodCall` on a `FieldReference` to a final collection field with a mutating callee name, or
   (b) settle for the weaker signature heuristic (public `addX`/`setX` methods on non-Builder types
   whose body is not a `SetOnce.set` forward) — (a) is strongly preferred and is a ~50-line visitor.
4. **Collection/map fields are assigned defensive copies.** Every constructor assignment
   `this.f = <expr>` where `f` is a final `List`/`Set`/`Map` field must have `<expr>` be
   `List.copyOf`/`Set.copyOf`/`Map.copyOf`/`Map.entry`-built or a parameter documented as
   already-immutable. Same implementation route as rule 3. (The `precedenceMap` finding: ctor-`put()`
   maps read as mutable; build immutably.)

**Escape hatch.** A single annotation-free suppression list inside the test (fqn + rule + one-line
justification), so a deliberate exception is visible in review rather than silent.

**Extension (cheap, recommended):** the same test class asserts that every public static method in
`maddi-util` carries `@NotModified`/`@Independent` contracts on parameters/return where the
parameters are mutable types — the `ZipLists.zip` gap. Scope it to `org.e2immu.util.internal.util`.

## 3. New support types (maddi-support)

**Goal.** Make the memo idiom declarable once, at the source, instead of per-field.

### 3a. `Memo<T>` and `IntMemo`

The idempotent lazy cache (`cachedFqn`, `cachedHash`, `hash`) as a first-class support type:

```java
// org.e2immu.support.Memo — annotate the CLASS with @IgnoreModifications (see engine note)
public final class Memo<T> {
    private volatile T value;                      // idempotent slot: any two writers write equal values
    public T get(Supplier<? extends T> compute);   // return cached or compute-and-cache
    public boolean isSet();
}
public final class IntMemo {                       // primitive twin, 0 = unset (the hash idiom)
    private volatile int value;
    public int get(IntSupplier compute);
}
```

Semantics to document on the class: the slot is **manual hidden content** (road §050) — writes are
idempotent (the supplier must be pure w.r.t. the outcome), so the mutation is observationally
invisible; `volatile` because the CST is read concurrently (the analyzer's type loop is parallel; the
existing bare-field memos rely on benign int/reference races, `Memo` should not).

**Engine note (small, required for the "declare once" payoff).** Today `@IgnoreModifications` is
read on *fields*. Extend `SourceContractMaterializer`/`AnnotationToProperty` so that a field whose
**type** carries a class-level `@IgnoreModifications` is treated as if the field were annotated.
This is *not* the rejected "skip `PropertyValueMap` by type" hack: that keyed on a type the author
never marked; here the disclaimer is declared, once, on the class whose entire purpose it is. Add
`TYPE` to the annotation's `@Target` if missing. Gate-off inert by construction (annotation-driven);
the standard Fernflower A/B applies.

**Migration:** `VariableImpl`, `UnaryOperatorImpl` switch to `IntMemo`/`Memo<String>`; the per-field
annotations disappear; conformance rule 2 then simplifies to "every non-final field is
`@IgnoreModifications`" with zero current members.

### 3b. Resolve-once: pattern, not a new type

`SetOnce<List<T>>` with a local accumulator and one `set(List.copyOf(local))` — the `ProvidesImpl`
fix — is sufficient; a dedicated `ResolveOnce` type would add API without adding safety. Conformance
rule 3 is the enforcement. Document the pattern in maddi-support's package-info (accumulate locally,
commit once, `getOrDefault(List.of())` for the not-yet-resolved read).

## 4. Contracts on cst-api: the "asserted contract" compromise

The question: some accessors deserve declarations (`fieldModifiers()` really is
`@Independent(hc=true)`; the analysis stores really are disclaimed), but a trusted contract on
*source* code cuts against the project's philosophy ("modification is computed, never trusted from a
source annotation" — `SourceContractMaterializer`). Is there a middle form: *"this is contracted, but
you could as well have computed it"*?

Yes — and the engine already contains all three ingredients. Propose a three-mode reading of source
annotations:

| Mode | Meaning | Who uses it |
|---|---|---|
| **Computed** (no annotation) | the analyzer derives everything | the default, unchanged |
| **Asserted** (plain annotation on source) | *a checked expectation*: the analyzer computes as today, ignores the annotation as input, and the **guard** compares computed vs. written — divergence is a finding (`assertion-diverges`), agreement is silence | API documentation + regression pinning: `fieldModifiers()` `@Independent(hc=true)`, `Info.access()` `@NotModified(after="inspection")` — the reader learns the invariant, and a commit that breaks it fails the guard, not a dogfood archaeology session |
| **Contract** (`contract = true`, or any annotation on a jar/aapi surface) | imposed as input, as today | genuinely uncomputable facts: jar leaves (`SetOnce.get`), runtime-type knowledge (`Set.copyOf` immutability), `@IgnoreModifications` |

Implementation sketch:

- The e2immu annotation set already carries `boolean contract()` on most annotations; the split
  costs no new annotation surface.
- The comparison is a guard pass: for each source element with plain annotations, parse them with
  the existing `AnnotationToProperty`, render the computed state with the existing `DecoratorImpl`
  inverse, compare per property. Both directions exist and are tested; the pass is a diff loop.
- **No change to which annotations are inputs today.** The currently-materialized source contracts
  (`@Mark`/`@Only`/`@TestMark`, `@IgnoreModifications`, `@StaticSideEffects`) keep their semantics;
  migrating any of them to asserted mode is a separate, per-annotation decision with a dogfood A/B.
- The asserted mode is also the honest home for the middle cases: where computation is possible *in
  principle* but the engine is not there yet, the assertion documents the intent and downgrades
  gracefully — the guard reports `assertion-unverifiable` (the existing `contract-unverifiable`
  polarity) instead of silently trusting.
- For the genuinely-uncomputable contracts, pair each with a **verification arm** where one exists —
  the `guardIgnoreModificationsSeparation`/`Containment` precedent: `@Independent(hc=true)` on an
  accessor returning a `Set.copyOf` field can be checked *syntactically* (every assignment to the
  field is a `copyOf`), which is weaker than computing immutability but much stronger than trust.

This gives the compromise a crisp answer: **annotate cst-api freely; nothing is trusted that could
have been computed; everything written is either verified, checked, or explicitly flagged as a
trusted leaf.** The dogfood then defends the API documentation the same way the ratchet defends the
verdicts.

## 5. Deliberately unchanged (trade-offs to keep)

- **Builders implementing the parent interface** (`TypeInspectionImpl.Builder implements
  TypeInspection`): analysis-expensive (the Builder-lean quests), but it is what lets the pre-commit
  object flow through the same APIs — the essence of the style. The engine models it now.
- **Lazy inspection** (`EventuallyFinalOnDemand.get()` modifying pre-mark): the reason the enm layer
  had to exist, and the feature. Keep.
- **Fieldless mixins** (`ExpressionWrapper`): clean; the analyzer handles them a-fortiori.
- **`ParameterInfoImpl` not extending `InfoImpl`**: settled via `@IgnoreModifications`; explicitly
  not to be reopened (recorded 2026-07-22).

## 6. One ergonomic follow-up: name the transition

The joint after-labels are unions of field names, so `Expression` reads
`@Immutable(after="anonymousClass,arrayInitializer,…")` — forty labels; the `Info` family stayed
legible only because the carrier field is uniformly named `inspection`. Two options, in increasing
ambition: a naming convention (transition-carrying fields share a name across a hierarchy), or a
first-class *named transition* ("committed") that a hierarchy declares once and the labels collapse
into. The second is an engine feature (label aliasing at the type level plus decorator rendering) and
pairs naturally with the asserted-contract work: `@Immutable(after="committed")` is what a reader —
and the IDE inlay — actually wants to see.
