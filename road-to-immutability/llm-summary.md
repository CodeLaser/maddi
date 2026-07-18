# Road to Immutability — condensed summary for LLMs

Dense, load-bearing summary of `src/docs/asciidoc/` (the authoritative terminology source for maddi's
immutability/modification/linking vocabulary). Read this instead of the full book; open a specific
chapter only when detail is missing here.

## Core vocabulary

- **maddi** — static analyzer for Java (and Kotlin via a shared CST) that computes modification,
  independence and immutability properties.
- **Mutable** means "not immutable at any positive level"; `@Mutable` is the explicit bottom verdict.
- **Content** of an object = the objects reachable from its fields (the object graph), split into:
  - **accessible content** — reachable through fields/accessors the analyzer can see and name;
  - **hidden content** — content that exists but cannot be named structurally: type parameters,
    extensible (non-final) types, the elements of shallowly-analyzed containers. Leaf immutables
    (String, primitives, boxed) have NO hidden content; containers of immutables DO.
- **Modification**: a method is *modifying* when it assigns, or modifies the content of, something in
  the object graph of its receiver's fields. Marked `@Modified` / `@NotModified`.
  - A *static* method mutating a parameter does not have a receiver to modify: the parameter is marked
    modified instead.
  - **All non-trivial constructors are modifying** (they assign fields).
  - Field property `unmodified` is **content-only**: it says the field's *content* is never modified;
    field *assignment* is tracked separately via effective finality (rule 0 below).
- **Independence** is a separate axis from immutability: it captures whether a method's return
  value/parameters expose (parts of) the receiver's content. `@Independent` (nothing shared),
  `@Independent(hc=true)` (only hidden content shared), dependent (mutable content shared).
- **Container** (`@Container`): a type whose methods do not modify their parameters.

## Immutability levels and rules

Levels (property `IMMUTABLE_TYPE`), ordered: `NO_VALUE(-1, undecided)` < `MUTABLE(0)` <
`FINAL_FIELDS(1)` < `IMMUTABLE_HC(2)` = `@Immutable(hc=true)` < `IMMUTABLE(3)` = `@Immutable`.

Rules (each level requires the previous):
- **Rule 0 (→ FINAL_FIELDS)**: all instance fields effectively final — never assigned outside the
  construction phase. "Construction phase" = constructors + methods only reachable from constructors
  (*part of construction*). Assignments to a nested type's fields by the ENCLOSING type, a sibling
  nested type, or a lambda anywhere in the primary type all count (whole-primary-type scan).
  A synthetic/annotated setter also breaks finality.
- **Rule 1 (→ level 2)**: no field's content is modified by the type's own methods.
- **Rule 2 (→ level 2)**: every field is private OR of (at least hc-)immutable type.
- **Level 2 vs 3 (hidden content)**: `@Immutable` (hc-free, level 3) additionally requires that every
  instance field's type is itself deeply immutable (level 3), the type is not extensible, and
  independence holds. A private, never-modified, never-exposed field of a mutable type is *hidden
  content*, not absence of content → level 2, not 3. Static fields do NOT count toward instance
  immutability (they belong to the class).
- Exposure matters: a type that stores externally supplied mutable objects (dependent constructor) or
  returns its mutable content (dependent accessor, e.g. record accessors) caps at FINAL_FIELDS.
- A mutable supertype makes the subtype mutable; an undecided supertype blocks the decision (see cycle
  breaking).

## Eventual immutability — DESIGN ONLY

`@Mark`, `@Only(before/after)`, `@BeforeMark`, `@TestMark`, `@Immutable(after="...")` describe types
that transition once from mutable to immutable (builders, freeze patterns, `SetOnce`/`FirstThen`
support classes). Implemented in an earlier analyzer generation; the CURRENT engine does not compute
them. The design chapter (060) is retained; the analyzer's own code still follows the pattern
(builder-commit CST, write-once property maps).

## The link system (chapter 105; module maddi-modification-link)

A **link** is `(from, nature, to)` between variables (incl. fields of variables, array elements,
synthetic variables). Purpose: the relational substrate from which modification/independence/
immutability verdicts are derived.

- **Natures** (notation used in engine output): `≡` identity; `←`/`→` assigned from/to; `∈`/`∋`
  element of / contains; `⊆`/`⊇` subset/superset; `~` shares elements; `≺`/`≻`/`≈` field-level;
  `≤`/`≥`/`∩` object-graph (weakest); `↗`/`↖` decoration (functional interfaces). Reciprocals appear
  in pairs. `*` after a variable = modified there. `0:`/`1:` = parameter index.
- **Two field models**: parsed types link to real fields (`box.t`); shallow/JDK types link to
  **virtual fields** `§xs`/`§es`/`§kvs` (= named hidden content). `§m` = the modification component
  ("the mutable aspect, wherever it lives").
- **Method link summary (mlv)**: per method — return links, per-parameter links, modified set; e.g. a
  setter: `[0:box*.t←1:x*] --> -  MOD[0:box,1:x]`. Applied at each call site by translating formal
  this/params to actual receiver/args (`LinkMethodCall` is the central class).
- **Functional interfaces**: capture site wraps a lambda/method-ref into a synthetic FI variable
  (`$_fi`, `Λ`) carrying its links; application site (`forEach`, `map`, SAM invocation) *lifts* those
  links onto the concrete receiver; unknown-lambda application is recorded as `$_afi` and resolved at
  the caller that supplies the lambda. Links to CAPTURED enclosing-method parameters pass through
  untranslated.
- **Closure**: links compose via a binary operator on natures (weakest results compose to nothing →
  finite). Incremental fixpoint engine: each derived fact carries a **witness** (its composing pair);
  removal invalidates exactly the dependent facts. Modification demotes containment: `⊇` → `~` at the
  modification statement, only for links the modified variable owns.
- **Shared-variable (sv) collapse** — fixes the part-of link explosion (locals × fields quadratic
  growth). Two equivalence tiers: *modification identity* (`≡` on `§m`) and *assignment groups*
  (whole-object assignment chains). The graph stores edges on group representatives; **reconstruction**
  projects facts back onto real params/fields/return at summary extraction, governed by assignment
  DIRECTION (source→recipient only), field-level mirroring, and a transitive-redundancy pass.
- Authoritative technical refs (in `maddi-modification-link/`): `linking-manual.md` (start at §5
  LinkMethodCall + §6 worked examples; TestLinkMethodCall is the spec-by-example), `README.md` (nature
  combination table), `vf/virtual-fields.md`, `sv-reconstruction-techniques.md`.

## Convergence — the iterating analyzer (chapter 108; module maddi-modification-analyzer)

- **Property discipline**: verdicts are property values on methods/fields/types (`element.analysis()`).
  Write-once; absence = undecided (first-class state, revisit later); the guarded `TolerantWrite`
  allows only monotone refinement (used by cycle breaking).
- **Ordering**: analysis order from call graph + hierarchy; *part of construction* computed per type;
  shallow summaries / annotated APIs for non-source types.
- **Iteration**: passes over the analysis order counting property changes; after the first pass a
  **worklist** narrows to dirty elements (mapped to their enclosing analysis-order element). Optional
  parallel execution (min(8, cores−2) threads); known non-confluent residue: constructor modification
  verdicts can flip with scheduling (conservative side is correct); tracked, not hidden.
- **Certification**: after quiescence, verification passes recompute everything frozen and require
  zero changes → the published fixpoint is path-independent. The proving ground (timefold, langchain4j,
  fernflower, guava, activemq, jenkins-core, camel-core modules) is kept certified & crash-free.
- **Cycle breaking**: at the certification point, if immutability-undecided types remain, one more full
  pass runs with undecided inputs resolved by strategy (undecided supertype floored at FINAL_FIELDS;
  no-information treated as non-modifying), then re-certifies. Conclusions that rest on broken cycles
  are capped at hc-level (2), never granted hc-free (3).
- **Analysis hints** (annotated APIs): curated JDK properties (String=@Immutable, Object=@ImmutableHC,
  collections mutable, functional interfaces with link summaries), loaded AFTER parse via
  `--preload-analysis-results-dirs`, pre-seeding the property store. Without them the positive
  immutability side barely concludes.
- **Fault tolerance**: statement/method-level containment (degrade to shallow summary), tight-ceiling
  cycle protection (throw + shallow fallback; generous ceilings cause downstream grind), producer-side
  skip guards for unrepresentable link shapes (e.g. stacked `x.§m.§m` faces).
- **Env gates** (opt-outs/diagnostics): `NOWORKLIST`, `PARALLEL`, `NOCYCLEBREAKING`, `FPDUMP=<file>`
  (per-element verdict dump), `MLTRACE`, many link-module gates (read once via `Gate`).
- **Golden rule**: engine/performance changes are accepted only with a byte-identical FPDUMP A/B
  (modulo the documented constructor non-confluence). Speed never buys verdict changes.

## Engine facts an LLM should not re-derive wrongly

- Compare `TypeInfo`/`MethodInfo`/`FieldInfo` with `==` (single instance per FQN + source set).
- `unmodifiedField` is content-only by design; do not "fix" it to include assignment.
- Record accessors expose components → records of mutable types are at best FINAL_FIELDS (dependent),
  or IMMUTABLE_HC when unexposed; never hc-free @Immutable.
- Interfaces: extensible ⇒ hidden content ⇒ at best `@Immutable(hc=true)`; a constants-only interface
  deserves immutability (no instance fields).
- Stateless lambdas/anonymous classes currently stay at FINAL_FIELDS (known conservative gap).
- javac is not thread-safe: all JavacTask access must be single-threaded
  (`-XDuseUnsharedTable=true`, synchronized lazy `getOrLoad`).

## Document map (src/docs/asciidoc/sections/)

Tutorial: 010 introduction · 020 final fields · 030 modification · 040 containers ·
045 linking & dependence · 050 immutability · 060 eventual (design-only, see note) ·
070 modification part 2 · 080 hidden content.
Technical: **105 the link system** · **108 convergence (iterating analyzer)**.
Practice: 110 support classes · 115 the analyzer's own code · 120 other annotations ·
appendix A annotation overview. (090/100 exist on disk but are excluded: pre-sv terminology.)
