# Road to Immutability — condensed summary for LLMs

Dense, load-bearing summary of `src/docs/asciidoc/` (the authoritative terminology source for maddi's
immutability/modification/linking vocabulary). Read this instead of the full book; open a specific
chapter only when detail is missing here. Last checked against the book: 2026-11-24 (a consistency read-through of
the whole booklet, at release 0.9.7). Statements about the code carry their own dates; open questions are listed at
the end.

## Core vocabulary

- **maddi** — whole-program static analyzer for Java (and Kotlin via a shared CST). Its *linking engine* (which
  objects flow where, which share state) feeds three applications: modification/independence/immutability (this
  file's core), **nullability** (§Nullability, since 2026-10) and the Java→Kotlin / Java→C# **translations**, which
  read the verdicts (read-only collection types from the structural twins and unmodifiable views; `?`/`!!` from
  nullability). Apache-2.0 licensed.
- **Mutable** means "not immutable at any positive level": `MUTABLE` is the bottom of the lattice. There is no
  `@Mutable` annotation in `maddi-annotation`; the `@Mutable` in test output is a label of the test helpers.
- **Content** of an object = the objects reachable from its fields (the object graph), split into:
  - **accessible content** — what the DECLARED type (and its supertypes) gives access to: its fields and methods;
  - **hidden content** — everything else (book 045, by declared type since 2026-11-24): all of an object held
    through an unbound type parameter or `Object`, and whatever a subtype or implementation adds to a declared
    type. Leaf immutables (String, primitives, boxed) have NO hidden content; containers of immutables DO.
  - **OPEN, CodeLaser/maddi-mod#159**: are the elements of a container that its HOLDER instantiated with a concrete
    type (`Map<String, Counter>` in `Counters`) accessible or hidden for that holder? Accessible: book 045
    (`Counters` is `@FinalFields`), 060, and the independence code (`FieldAnalyzerImpl`: "the transported content
    decides dependence, not the container"; `add(StringBuilder)` into a list field is `@Dependent`). Hidden: book 080
    `structural-definition` and `Util.accessibleTypes` (`clearLeaves`, `touchAll(Bag)`, `reset(St)` not structural).
    The `List.copyOf`/`Map.copyOf` shapes come out `@Immutable(hc=true)` even when mutable elements are handed out.
    Do not treat either reading as settled.
- **`@IgnoreModifications` = manual hidden content** (road §050 "Ignoring modifications as manual hidden
  content"): a field of concrete (mutable) type whose modifications the author disclaims. Same lattice effect
  as a type-parameter field (→ at best `@Immutable(hc=true)`, never hc-free); it differs only in *provenance* —
  erasure *derives* confinement, `@IgnoreModifications` *asserts* it, so soundness must be **checked** not
  assumed. **Confinement guard**: a modification reached through the field must stay in the *ignored stratum*
  (transitive content reachable only via that field). Writing into the stratum, and referencing accessible
  content from it, are confined and fine (this is why an analyzer filling an `@IgnoreModifications analysis()`
  overlay is legal); an *escape* into the type's own accessible content is **reported** as the warning
  `ignore-modifications-not-confined` (the separation check; `GuardAnalyzerImpl` WARNS, never caps: the designer
  decides), an escape into global/static state is a **`@StaticSideEffects`** — does NOT cap (not your field) but
  flags a real outward effect. The SSE *computation* on source methods (`StaticSideEffectAnalyzerImpl`) runs by
  default (it was behind the env gate `SSE` until 2026-10-25, CodeLaser/maddi-mod#65). SSE is thus the global-escape arm of the same guard; `@IgnoreModifications` on
  the *target* global field downgrades an SSE to a sanctioned disclaimer (the `System.out` story).
- **Modification**: a method is *modifying* when it assigns, or modifies the content of, something in
  the object graph of its receiver's fields. Marked `@Modified` / `@NotModified`.
  - A *static* method mutating a parameter does not have a receiver to modify: the parameter is marked
    modified instead.
  - **All non-trivial constructors are modifying** (they assign fields).
  - Field property `unmodified` is **content-only**: it says the field's *content* is never modified;
    field *assignment* is tracked separately via effective finality (rule 0 below).
  - **Structural (shallow) twins** (2026-10-10, CodeLaser/maddi-mod#25): `STRUCTURALLY_NON_MODIFYING_METHOD`,
    `STRUCTURALLY_UNMODIFIED_PARAMETER`, `STRUCTURALLY_UNMODIFIED_FIELD`, and `structurallyUnmodifiedVariable` at
    statement level. A modification of the object itself or of its accessible content (real fields, the scope chain)
    counts; one of its hidden content (an element: the `∈`/`∋` links, a map value, a consumer applied to the
    elements) does not. Implied by the deep property; readers (`isStructurallyUnmodified()`,
    `isStructurallyNonModifying()`) fall back to the deep one when the twin is absent, so a contract or a hint
    counts as structural. Computed beside the deep verdict by the link computer (the same decision over the
    evaluation's modified set, without the `∋` propagation and with the deep-only evidence left out: an argument
    to a parameter that is modified yet structurally unmodified, a callee summary's `deepOnlyModified()` entries),
    by the abstract-method fold, and by a second closure of the reachability pass over the structural seeds.
    Written and read as `@Modified(hc=true)` (modified in the hidden content only; a contract on a bodiless
    declaration, `AnnotationToProperty`/`DecoratorImpl`/`SourceContractMaterializer`). Consumer: the Java→Kotlin
    printer's `List`/`MutableList` choice. Immutability, independence and `@Container` keep the deep property.
    The book's §"Propagating modifications" (080) describes it. A modified element reached through `∈`/`∋`
    (hidden content) counts structurally when it may also be the owner's accessible content (maddi-mod#130, Bart 2026-11-19): the definition is "the written
    object is reachable from p by SOME path of accessible fields", accessible judged by DECLARED types (a field only a
    subclass declares is hidden; type-parameter/`Object` fields and container elements are hidden).
    `Util.accessibleTypes` = declared types reachable through accessible fields at any depth;
    `Util.mayBeAccessibleContent` = the written type is assignable either way to one of them (gate
    `NOACCESSIBLEREACH`; applied in `ShadowModificationPass.project` and the engine's `mayAliasAccessibleField`;
    widens #117's one-field `Util.hiddenAliasOwner`). Approximations (accepted): (a) a call's modification compares
    the object plus the application objects it holds, not its JDK containers (else `clearList` loses its read-only
    `List`); (b) the owner's own type counts only through a field back to it (`Node.first` yes; `Leaf.children` no:
    `clearLeaves` not structural); (c) the fixpoint engine can't tell a direct write from a call, so it is coarser
    than the reachability pass. Book §080 `structural-definition` (the definition), §109 `structural-approximations` (a-c); tests `TestAccessibleReach`,
    `TestBook080Elements`). The step back from a modified container to its elements is computed within one method
    (maddi-mod#113: a co-element of a modified element is modified, in the element's mode: structurally when the
    element's own state is written, maddi-mod#130), not through a call
    (`useAddNewLine`).
- **Unmodifiable views** (2026-10-15, CodeLaser/maddi-mod#52; book §050 "Unmodifiable views"): a fact beside
  immutability, never derived from it. `UNMODIFIABLE_TYPE`: rule 0 + the structural rule 1, decided BEFORE the rule-3
  (dependence) cap that stops `Collections.unmodifiableList`'s view type at FINAL_FIELDS (which cannot tell "rule 1
  failed" from "only rule 3 failed"); implied by IMMUTABLE_HC; says nothing about hidden content or `@Container`.
  `UNMODIFIABLE_VIEW_METHOD` / `@UnmodifiableView` (METHOD only; JetBrains' name): the result is such an object. A
  contract on bodiless methods (the 11 `Collections.unmodifiable*` in the archive), computed from the returned values
  for a body (a call with the property, an instance of an unmodifiable type, an immutable-HC value, a field proven to
  hold one, `null`), folded over implementations. Read by nothing in the immutability/independence/container
  analyses; consumer: the Kotlin printer's `List` vs `MutableList` (`ModificationFacts.unmodifiableResult`), the call
  staying Java's (`listOf` would copy).
- **Link vs dependence** (book wording since 2026-11-24): the tutorial chapters use *dependent* / *independent with
  hidden content* (*content dependence*) / *independent* for the verdicts, never "linked". A **link** is only the
  mechanism: the analyzer's record of where an object flows (§The link system), which ownership and nullability
  follow too. Chapter 045 is titled "Dependence" (anchor `linking-dependence`), 070 "How to compute dependence",
  080 "Content dependence" (anchor `content-linking`).
- **Independence** is a separate axis from immutability: it captures whether a method's return
  value/parameters expose (parts of) the receiver's content. `@Independent` (nothing shared),
  `@Independent(hc=true)` (only hidden content shared), dependent (mutable content shared).
- **Container** (`@Container`): a type whose methods do not modify their parameters.

## Immutability levels and rules

Levels (property `IMMUTABLE_TYPE`), ordered: `NO_VALUE(-1, undecided)` < `MUTABLE(0)` <
`FINAL_FIELDS(1)` < `IMMUTABLE_HC(2)` = `@Immutable(hc=true)` < `IMMUTABLE(3)` = `@Immutable`.

Rules (each level requires the previous):
- **Rule 0 (→ FINAL_FIELDS)**: all fields effectively final (static ones too, see below) — never assigned outside the
  construction phase. "Construction phase" = constructors + methods only reachable from constructors
  (*part of construction*). Assignments to a nested type's fields by the ENCLOSING type, a sibling
  nested type, or a lambda anywhere in the primary type all count (whole-primary-type scan).
  A synthetic/annotated setter also breaks finality.
- **Rule 1 (→ level 2)**: no field's content is modified by the type's own methods.
- **Rule 2 (→ level 2)**: every field is private OR of (at least hc-)immutable type.
- **Rule 3 (→ level 2)**: no parameter of a non-private method or constructor, and no return value of a non-private
  method, is dependent on the fields. A type that stores externally supplied mutable objects (dependent constructor)
  or returns its mutable content (dependent accessor, e.g. record accessors) caps at FINAL_FIELDS.
- **`hc=true` is a level of its own**, not decoration (book 050): the type guarantees the part it controls, its
  hidden content can still be modified by others. A subtype cannot be better than its supertypes, EXCEPT for the
  hidden content that extensibility alone brings (`Object` is hc, a final class deriving from it need not be).
- **Eventually**: rules 0 and 1 by a mark; rule 3 when the exposed object is itself eventually immutable (computed)
  or when the field is emptied at the transition (`Lazy`'s `supplier = null`: a null field has no content; the book's
  argument in 110, NOT computed, the analyzer says dependent). Rule 2 is never eventual.
- **Overriding**: from `@Modified` to `@NotModified` is allowed, the converse never (book 050 §Inheritance). Enforced
  by `GuardAnalyzerImpl`: `contract-conflict` for an authored `@Modified` over an inherited `@NotModified` (with or
  without a body), `contract-violation` for a modifying implementation or override; since 2026-11-24
  (`guardLibraryContract`, `TestOverrideWeakensContract`) also against the ANNOTATED APIs: a modifying `size()`,
  `equals()` or `toString()` in any class is reported. Against a library contract the authored conflict is an error but
  the COMPUTED violation only a warning: the 92 errors on timefold (gate 2026-11-24) were Arrays.deep*/hash defaults
  (array parameters assumed modified) and stateful iterator()/list accessors. A library contract is checked, it does not DECIDE a bodiless
  source method (`ContractResolution` reads source annotations only). `override-weakens-computed` is the warning for
  an override that modifies where the parent is computed, not contracted, `@NotModified`.
- **A parameter whose type is a type parameter is `@NotModified` by default** (a type parameter counts as
  immutable-hc; `ShallowMethodAnalyzer.computeParameterUnmodified`): `Collection.add(E)`, `Comparable.compareTo(T)`
  carry nothing. `Consumer.accept(@Modified T t)`, `Function.apply(@Modified T t)` are written out in the archive.
  The argument passed to a functional parameter stays unmodified in that method; the call site supplying a concrete
  function gets the modification (`PassedFunction`).
- **An abstract method of a source interface with NO implementation is computed `@NotModified`**, and such an
  interface `@Immutable(hc=true)`: the fold runs over zero implementations. The book's "abstract methods are
  `@Modified` unless annotated" describes the annotated APIs. This, not hidden content, is why the book's `Counters`
  (045) is computed immutable in `TestBook045Linking`; with a concrete mutable element class it is `@FinalFields`.
- **Level 2 vs 3 (hidden content)**: `@Immutable` (hc-free, level 3) additionally requires that every
  instance field's type is itself deeply immutable (level 3) -- INHERITED instance fields included, of source
  and jar superclasses alike (a superclass's own hc verdict is not consulted: `Record`/`Enum` are hc because
  extensible, which is not content) -- the type is not extensible, and independence holds. A private,
  never-modified, never-exposed field of a mutable type is *hidden content*, not absence of content → level 2,
  not 3.
- **Static fields are NOT exempt** (road §050 "Static side effects"; corrected 2026-09-23 -- this summary used to
  say the opposite, and a change built on it had to be reverted): "the definitions make no distinction between
  static and instance fields"; inside the primary type, "modifications are modifications". A type that modifies
  its own static counter is `@FinalFields` (the book's `CountAccess`). The only exemptions: `@IgnoreModifications`
  on the static field, and modifications inside a static initializer block (the static fields' constructor).
  The METHOD is `@Modified` too (`CountAccess.getK()`, a `++count` in an instance or static method): since
  2026-10-29 `NON_MODIFYING_METHOD` is false when the method modifies static state its type owns (maddi-mod
  `OwnStaticState`), and a getter guard that does is not inert, so such a method is no getter.
  `@StaticSideEffects` is for modifying static state the primary type (with its enclosing and parent
  types) does NOT own -- directly, or through any callee (static, instance, constructor) that modifies its own
  type's static state: `Client` calling `Counter.next()` is one, `Counter.next()` itself is not
  (CodeLaser/maddi-mod#65; computed from the per-method set `STATIC_STATE_MODIFIED_METHOD`). A singleton follows the normal rules unless `@IgnoreModifications` is on the field giving
  access to it. Since 2026-09-23 the hc-free check (`instanceFieldTypesDeeplyImmutable`) counts statics too.
- **Self-referencing fields** (declared type = the type itself) are skipped for rules 1 and 2, for the hc label, and
  in the independence that FEEDS the immutability rule (`independentIgnoringSelfFields`) -- never for rule 0
  (assignability). The PUBLISHED independence still counts them: dependent while the type is undecided, and after
  it is decided independent if the type is immutable, dependent if not (a mutable `Node` returning `next`).
- A mutable supertype makes the subtype mutable; an undecided supertype blocks the decision (see cycle
  breaking). **Superclasses and jar/hints supertypes cap by their verdict; SOURCE INTERFACES are WALKED**
  (2026-09-23, `TypeImmutableAnalyzerImpl`, gate `INTERFACEWALK=0` for A/B): an interface's FINAL_FIELDS comes from
  its abstract methods, which fold over ALL implementations, so passing it down let a stateful sibling cap every
  other implementation (`io.vavr.collection.Iterator` is an `io.vavr.Value`). Instead, each abstract method the type
  inherits that nothing on its path overrides is folded over the implementations in the type's CONE only; its own
  verdict is kept when it is contracted or when the cone has no implementation. The type-immutability rule reads
  only own fields + own abstract methods (+ this walk); concrete/default methods count through the fields they
  modify. After-mark (eventual) mode keeps the verdict rule. Independence still takes the min over supertypes.

## Eventual immutability — COMPUTED

`@Mark`, `@Only(before/after)`, `@BeforeMark`, `@TestMark`, `@Immutable(after="...")` describe types
that transition once from mutable to immutable (builders, freeze patterns, `SetOnce`/`FirstThen`
support classes). The CURRENT engine reads them as contracts (`Value.Eventual`/`Value.EventuallyImmutable`;
properties `EVENTUAL_METHOD`, `EVENTUAL_PARAMETER`, `EVENTUALLY_IMMUTABLE_TYPE`,
`EVENTUALLY_FINAL_FIELD`) **and computes them**, in `TypeEventualAnalyzer` (phase 4.3), without
reviving preconditions: a type holding a field of eventually immutable type inherits the mark, because
the callee's own contract says which side of the transition it belongs to. Marks travel from an
implementation to its abstract method (`AbstractMethodAnalyzerImpl.methodEventual`), so interfaces are
certified too. `@TestMark` implies `@NotModified` — observing the state is not changing it.
Independence is relaxed after the mark only when the leaked object is ITSELF eventually immutable.
Mark labels name the transition: computed ones are the guarding field *names*, comma-separated (a mark is often
inherited); a contract on a type without fields chooses a name; all annotations of one transition agree. Eventuality is deliberately kept OUT of the
`IMMUTABLE_TYPE` lattice for now. `@BeforeMark` is not read yet. The analyzer's own code follows the
pattern heavily (builder-commit CST, write-once property maps).
Plan and staging: `docs/design/eventual-immutability.md`.

## The link system (chapter 105; module maddi-modification-link)

A **link** is `(from, nature, to)` between variables (incl. fields of variables, array elements,
synthetic variables). Purpose: the relational substrate from which modification/independence/
immutability verdicts are derived.

- **Natures** (notation used in engine output): `≡` identity; `←`/`→` assigned from/to; `∈`/`∋`
  element of / contains; `⊆`/`⊇` subset/superset; `~` shares elements; `≺`/`≻`/`≈` field-level;
  `≤`/`≥`/`∩` object-graph (weakest); `↗`/`↖` decoration (functional interfaces). Reciprocals appear
  in pairs. `*` after a variable = modified there. `0:`/`1:` = parameter index.
- **Two field models**: parsed types link to real fields (`box.t`); shallow/JDK types link to
  **virtual fields** (= the container type's hidden content, named): for a type-parameter payload `§` + the
  lower-cased names + `s` (`§xs`, `§ts`, `§es`, `§kvs`), for a concrete payload `§$` + `s` per level (`§$s` for
  `List<String>`, `§$$s` for a map; `vf/virtual-fields.md`). `§m` = the modification component
  ("the mutable aspect, wherever it lives").
- **Method link summary (mlv)**: per method — return links, per-parameter links, modified set; e.g.
  `void set(Box<X> box, X x) { box.set(x); }`: `[0:box*.t←1:x*, 1:x*→0:box*.t] --> -   MOD[0:box, 1:x]`
  (linking-manual §5; book 105). Applied at each call site by translating formal
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
- **Alternatives are linked apart, then joined** (since 2026-09): the then- and else-blocks of an if/else
  are each linked from their own copy of the method's graph as it stood before the statement; the graph
  after the statement is the UNION of the states the blocks end in, plus the state before when there is no
  else. An assignment is a strong update (it erases its target's edges), and linked into one shared graph
  the else-block erased what the then-block established — a parameter stored in one branch read as
  `@Independent` once any statement followed. The union is not re-closed (a then∘else composition describes
  no execution). Assignment groups are identity claims, so a variable stays a member of a group only when
  every alternative puts it there; otherwise it is EVICTED (`SharedVariables.planJoin`): the alternative
  it was a member in gets an assignment edge to the group's rep instead, and its records stay as DETACHED
  provenance, inherited at extraction and dropped when the variable is next assigned. An evicted member
  still holds the group's object in one alternative, so a later modification of the group reaches it, and
  the reverse (`detachedAliases`); and the member's field faces are copied onto the rep (`rep.value ←
  h.value`), which a membership gave for free -- never the rep's onto the member, whose name after the join
  also stands for what it held elsewhere (a reassigned parameter: the caller's object). A branch that exits (return, throw) is still joined into
  what follows; switches, loops and `try` are still linked sequentially. Gate `NOFORK` restores sequential
  linking.
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
  pass runs with undecided inputs resolved by strategy (the default, `CycleBreakingStrategy.NONE`, is pessimistic: a
  type with an undecided supertype concludes FINAL_FIELDS; `NO_INFORMATION_IS_NON_MODIFYING`, which reads an undecided
  supertype as IMMUTABLE_HC, is set by two tests only), then re-certifies. Conclusions that rest on broken cycles
  are capped at hc-level (2), never granted hc-free (3). Since 2026-07-19 the breaking pass also
  resolves the field/abstract-method path: a verdict that will never arrive (a field's unmodified
  status, a NON-PRIVATE field's type immutability, an abstract method's modification — each rooted in
  external unannotated types, and transitively CASCADING through field types) is pessimistic, and the
  type concludes FINAL_FIELDS instead of staying undecided. This eliminated the "type-null clusters"
  (51% of elasticsearch's 45k types; 100% coverage on guava after).
- **After the fixpoint — the reachability passes** (book 108 §After the fixpoint). (1) **MODREACH**
  (`ShadowModificationPass`, ON by default in `AnalysisEngineImpl`; the diagnostics harnesses and refactor-service
  do not turn it on): re-derives NON_MODIFYING_METHOD / UNMODIFIED_PARAMETER / UNMODIFIED_FIELD from primitive
  evidence + converged links; a *cutover* downgrades frozen optimistic TRUEs past `TolerantWrite` (the one
  sanctioned non-monotone write), freezes the three, clears the derived immutability/independence/container family,
  re-derives and re-certifies; up to `MODREACH_ROUNDS`=10 (a safety bound; the rounds stop when one writes nothing). Seeds: a walkable body (in order, non-empty, not
  union-over-implementations, not opaque) is seeded from primitive evidence, not from its callers' summaries
  (b265dc8f, 2026-10-20, maddi-mod#55: a parameter a recursion cycle only reads stays unmodified); an array
  expression (`DependentVariable` without `arrayVariable()`) taints instead of failing the pass (5512762c).
  Diagnostics: `MODREACH_EXPLAIN`, `MODREACH_TAINT=<substring>`. (2) **NullabilityPass**, one-shot after the
  cutover (§Nullability).
- **Analysis hints** (annotated APIs): curated JDK properties (String=@Immutable, Object=@ImmutableHC,
  collections mutable, functional interfaces with link summaries), loaded AFTER parse — by default since 0.9.5
  (the shipped `openjdk.jar` + `libs.jar`; `--preload-analysis-results-dirs none` opts out, `default,<dir>` adds),
  pre-seeding the property store. Without them the positive
  immutability side barely concludes.
- **Fault tolerance**: method-level containment (a failing method is marked degraded, see below), tight-ceiling
  cycle protection (throw + shallow fallback; generous ceilings cause downstream grind), producer-side
  skip guards for unrepresentable link shapes (e.g. stacked `x.§m.§m` faces).
  A degraded method (`DEGRADED_ANALYSIS_METHOD`) carries no links, so the field analyzer does not read
  "no links" from it as independence or non-modification: a field referred to by a degraded method keeps
  the independence of its type, and a degraded non-constructor method counts as modifying (since 2026-09).
- **Env gates** (opt-outs/diagnostics): `NOWORKLIST`, `PARALLEL`, `NOCYCLEBREAKING`, `FPDUMP=<file>`
  (per-element verdict dump), `MLTRACE`, many link-module gates (read once via `Gate`).
- **Golden rule**: engine/performance changes are accepted only with a byte-identical FPDUMP A/B
  (modulo the documented constructor non-confluence). Speed never buys verdict changes.

## Nullability (chapter 085; `maddi-modification-analyzer` package `nullability`, docs/design/nullability.md in maddi-mod)

- **Three states** (`NullableState`): `NULLABLE`, `NONNULL`, `UNSPECIFIED` ("nobody said"; NOT a synonym for
  nullable). Properties `NULLABILITY_FIELD/PARAMETER/METHOD` (value `Value.Nullability`: the state of the
  value and, recursively, of each type argument; an array's one argument is its elements: `String?[]` is
  `N(Q)`). `NOT_NULL_*` is GONE (2026-10-06). Locals get verdicts too (`Report.local`).
- **Reachability over the converged link facts**, a one-shot pass AFTER certification (`NullabilityPass`,
  modelled on the modification reachability pass): seeds (null literals; fields that can hold the default
  value; `new T[n]`; library nullable results from the hints; nullable contracts; by default a parameter/field
  compared with null — `Policy.nullTests`) → edges (direct links in each method's variable data, per-call
  argument links, override chains: one parameter nullability per chain, returns narrow downward) → reached =
  NULLABLE, unreached = `Policy.unreached` (NONNULL in the `@NullMarked` reading). Rounds: an unreached
  analysed return is trusted non-null at its call sites next round (monotone, ≤5).
- **Use-site facts** (`NonNullFacts`, flow-sensitive forward walk per method): known-non-null locals,
  parameters and fields of `this` per statement (null tests, dereferences, `requireNonNull`, null-check
  predicates `NullPredicates`, assignments of non-null values; `&&`/`||`/`?:` inside a statement). An edge
  is not taken where its source is guarded. `kotlinSmartCasts()` restricts to what Kotlin derives.
- **Content**: `Content(node)` for array elements, `Arg(node, i)` per type argument, from the hidden-content
  variables (`list.§$s` = arg 0, `map.§$$s[-1]`/`[-2]` = keys/values, `box.t`) and writes into a generic
  receiver (`add`, `put`); flows between generic values tie the slots both ways (invariance).
- **World** (`Policy.world`): `CLOSED` (default; a parameter no analysed call passes null to is NONNULL),
  `OPEN` (outside-callable parameters UNSPECIFIED unless a *precondition*: dereferenced/required/rejected on
  every normal exit). Preconditions are computed per override family and reported with the calls that may
  pass null (`Report.preconditions`, `nullArguments`).
- **Contracts**: JSpecify/JetBrains/Checker/JSR-305/Lombok annotations and `@NullMarked` scope are read
  (`DeclaredNullability`); a nullable contract seeds, a non-null one stops null (the caller's error). The JDK
  hints carry JSpecify's annotated JDK, one level down (`nullableArguments`).
- **Java output** (`NullabilityDecorator`, flavours JSpecify/Checker/JetBrains/JSR-305/maddi): the TOP-LEVEL state, and an
  array's elements in a TYPE_USE flavour; type arguments reach Java only through maddi's own `nullableArguments`
  (`List<@Nullable String>` not yet);
  `@NullUnmarked` for an UNSPECIFIED method only in JSpecify; an UNSPECIFIED field is written `@Nullable`.
  Not an option of the CLI (it reaches users through `--translate`); the IDE daemon reads it (`AnnotationTagger`,
  `WarmAnalysisService`, seen 2026-11-24).
- **Presets**: `NULL_MARKED` (Java annotations), `KOTLIN` (= NULL_MARKED + `assertContentWrites`,
  `callResults`, `assertAtDeclaration`, `libraryCallbacks`: the printer asserts `!!` at a content write or a
  declaration instead of making the slot nullable; each moves an NPE, never removes one), C# = KOTLIN +
  `preconditionsNonNull`. `CAUTIOUS` leaves unreached UNSPECIFIED.
- **Engine support, inert for modification** (ch. 105 §Null as a value): null constant markers `$_ceN`;
  `LinkComputer.Options.nullConstantReturns` keeps a marker-only return in the summary; a marker composes
  as assigned-from only, never as a member; `Link.derived()` (direct vs closure-composed), the pass reads
  direct links; `trackObjectCreations` for per-call argument links. All switched on by the analyzer's
  `Configuration.nullability()`; off by default (FPDUMP golden rule).
- **Oracle**: guava's own JSpecify annotations, scored as *unsafe* (inferred NONNULL, declared nullable)
  and *noise* (the reverse); baselines all-non-null 2,515 unsafe, all-nullable 13,272 noise, over 15,787
  top-level positions. First closed-world table (2026-10-07): 13,306 agree / 354 unsafe / 233 noise, OPEN 166
  unsafe; after type arguments, elements and the JSpecify-generated JDK hints (2026-10-13): NULL_MARKED 341
  unsafe / 1,740 noise, OPEN 211 unsafe. Second oracle: kotlinc/Roslyn on the translated corpora (the J2K/C#
  ratchets: fernflower, langchain4j, guava, nacos, jenkins, timefold).

## Ownership (chapter 055, before eventual immutability; finalizers are its §Finalizers; maddi-mod `analyzer/ownership`, `docs/roadmap/ownership.md`)

- **Who holds an object**, not who modifies it. A pass after convergence: always on in the IDE daemon (closed world
  a per-project setting) and the MCP server (`configure_project closedWorld`); CLI `--ownership` / `--closed-world`
  (and the build plugins' `ownership` / `closedWorld`), or the gates `OWNERSHIP` (`OWNERSHIP=<file>` also writes a
  census TSV) and `OWNERSHIP_WORLD=closed`. A negative verdict's reason is `OWNERSHIP_CULPRIT` (with culprits on); a
  hand-written annotation the verdict does not uphold is a `contract-violation`. First consumer:
  the Java→Rust printer. One fact feeds back into the fixpoint (an ownership round at the terminal point, gate
  `NOOWNERSHIPFEED`): a parameter every caller hands over into an owned field is no dependence; the elements'
  independence counts in its place (`java.util` containers and arrays only). Stored as
  `HANDED_OVER_PARAMETER`; an incremental run starts from it and falls back to cold when the recomputed feed differs.
- **`@Fresh`** (method, `FRESH_METHOD`): every returned object was created during the call (or is `null`) and
  nothing else holds it. A lambda/method reference is never fresh. `List.copyOf`, `Collections.emptyList` are not.
- **`@Borrowed`** (parameter, `BORROWED_PARAMETER`): nothing that outlives the call holds the argument's object;
  keeping its hidden content is fine (`addAll(c)` borrows `c`), storing an immutable object is not (a `String`
  stored is kept, though independent). Degraded method: nothing borrowed.
- **`@Owned`** (field, `OWNED_FIELD`; `elements=true` = `OWNED_ELEMENTS_FIELD`): reachable only through its owner.
  Rules: arrives fresh (creation, fresh call, `null`, or a *transferred* parameter), never leaves (borrowed callees
  and *lent* results allowed), private or effectively private (closed world), no degraded referrer.
  `SHARED_IMMUTABLE_FIELD` has no annotation.
- **Transfer**: every call site (known: private or closed world -- package-private does not count since 2026-11-24, a package is not closed --, no override, no method
  reference) hands over a fresh object or a local not used after. `@Finalizer` is transfer of the receiver; with
  the gate on it is computed for methods moving an owned field out, and contracted finalizers' call sites are
  checked (`finalizer-violation` warnings).
- `@Fresh`/`@Borrowed` are contracts on bodiless and library methods (a few JDK ones: `Arrays.copyOf`,
  `toArray()`, `ArrayList.clone`, `Map.entry`); `@Owned` is never read as a contract. Absence of links must read
  as shared, never owned. Book examples pinned by `TestBook055Ownership`. Chapter 060
  §"Ownership and eventual immutability": with a private constructor the type is immutable without help; with a
  public constructor in a closed world the feed (above) makes the handed-over parameter no dependence, and the type
  `@Immutable(hc=true)`. A builder's `build()` is computed `@Finalizer` only in a closed world: its callers are
  elsewhere, and in a library `@Finalizer` on it is a contract. The book's sequencing rules are two: a finalizer on a
  field only inside a finalizer of its owner; not on a parameter unless it is transferred.

## Engine facts an LLM should not re-derive wrongly

- Compare `TypeInfo`/`MethodInfo`/`FieldInfo` with `==` (single instance per FQN + source set).
- `unmodifiedField` is content-only by design; do not "fix" it to include assignment.
- Assigning a field of an EXPRESSION's result (`requireNonNull(tail).next = node`, `self().next = x`) modifies that
  result, and through its links what it stands for, exactly as a modifying call on it would; the scope-chain rule
  only saw scope VARIABLES (since 2026-09).
- `RedundantLinks` drops a link as transitively redundant only through earlier links of its OWN nature group
  (`←`, `→`, `∈`, `∋`, `⊆/⊇/~`, `∩/≤/≥`, `≺/≻/≈`). Through the union of all groups, a `≈` path dropped the
  assignment `this.t ← 1:t`, the field's only link to its parameter (since 2026-09).
- Field independence counts a link to a REAL field of a parameter or return value (`this.f ← 0:p.g`) as a link
  to that variable: the field shares the caller's object. Parts reached through a virtual (`§`) field — the content
  of a copied collection — are judged by the transported-content rules instead (since 2026-09).
- **A captured parameter is a field of the instance for independence** (2026-10-13, CodeLaser/maddi-mod#39): a method
  of a local or anonymous class, or a lambda, whose return value or parameter links to a parameter it captured from
  the enclosing method is judged as if that parameter were a field of its own type (`LinkToField.isCaptured`): the
  captured variable is the object's synthetic final field. Guava's `new ForwardingDeque<T>() { delegate() { return
  delegate; } }` is dependent, so the abstract `delegate()` it implements is, and the mutators through it modify. The
  modification side counted captured variables already (the closure set in `copyModificationsIntoMethod`). Not yet:
  a captured LOCAL (its link never reaches the summary) and the creation site of the anonymous class, which links
  the new object to nothing it captured (CodeLaser/maddi-mod#43). Gate `NOCAPTURED` for an A/B.
- **A view factory's result shares its argument's modification component** (2026-10-13, CodeLaser/maddi-mod#40): a
  library parameter `@Independent(dependentReturnValue = true)` gets `rv.§m ≡ p.§m` from the shallow link computer
  (`sharedModification`), so `Collections.synchronizedList(list).add(x)` modifies `list` while the call alone does not
  (the JDK's 19 view factories are `@NotModified @Independent(dependentReturnValue = true)`). The reachability pass
  does not follow plain `≡` from a local; a local initialised with such a call follows the callee's `rv.§m ≡ p.§m`
  link to the argument (`ShadowModificationPass.projectViewLocal`, gate `NOVIEWLOCAL`).
  The unmodifiable views (`Collections.unmodifiable*`, 11) carry the same parameter annotation (CodeLaser/maddi-mod#60),
  and independence judges a plain `≡` between modification faces as the whole field (`LinkToField.linkedType`):
  `return Collections.unmodifiableList(this.list)` is dependent, a read-only view of a mutable list.
- A variable's links at a statement are null only while UNDECIDED. A merge (after an if/else, loop, …) whose
  evaluation and sub-blocks carry no links for a variable gets EMPTY links, not null: left null, a field only read in
  the condition of a method's last statement kept the field's links undecided until cycle breaking wrote them EMPTY,
  an optimistic @Independent (since 2026-09).
- **Chapter "Other annotations" is mostly aspirational — do not assume those annotations work.**
  Only `@Identity`/`@Fluent` are computed there (`TypeModIndyAnalyzerImpl`; `@Fluent` also via `@GetSet`).
  `@UtilityClass` is read as a *contract* by `AnnotationToProperty` (and shown by `DecoratorImpl`) but never
  inferred; it forces IMMUTABLE + INDEPENDENT on the type. (`@Finalizer` is in chapter 055, not here: a contract, and
  computed by the ownership pass, see §Ownership.) `@Singleton`
  and `@ExtensionClass` exist in `maddi-annotation` but have no property and are read by nothing. Not in the
  book at all, but live (read by `AnnotationToProperty`): `@Commutable`, `@GetSet`, `@Docstring(s)`. `@NotNull`/`@Nullable` left the chapter on 2026-10-23: they ARE computed, by the nullability
  pass (chapter 085 below).
- **The reachability pass (MODREACH) follows no element link for a parameter**: its E1/E2 projections walk
  assignment links only, so under MODREACH the published DEEP `UNMODIFIED_PARAMETER` of a walkable body never
  records an element-only modification (`for (Item i : items) i.touch()` reads unmodified); the fixpoint's
  FALSE is reverse-upgraded at the cutover. Fields keep that evidence through the statement-level seeds.
  Measured 2026-10-10 (`TestStructuralModification`), not changed: the structural twins make the distinction
  explicit instead.
- **`@Finalizer`'s modification semantics ARE implemented and load-bearing** (its two
  sequencing rules are checked by the ownership pass only, as `finalizer-violation` warnings; the old rule 1, on the
  finality of fields, left the book on 2026-11-24: an owner may replace what it holds): a call to a finalizer NEVER marks the receiver modified — the guard
  `!methodInfo.isFinalizer()` in `MethodModification.go`, mirrored in `ShallowMethodLinkComputer`
  and in the `ShadowModificationPass` boundary seeds (2026-07-23). The annotated APIs mark the
  stream operations (`filter`/`map`/`collect`/…) `@Finalizer` with NO `@NotModified`; only the
  exemption keeps `this.field.stream().map(...)` chains from conservatively modifying the field
  and `this`. Any component that re-derives modification at call sites MUST mirror this guard.
- Record accessors expose components → records of mutable types are at best FINAL_FIELDS (dependent),
  or IMMUTABLE_HC when unexposed; never hc-free @Immutable.
- Interfaces: extensible ⇒ hidden content ⇒ at best `@Immutable(hc=true)`; a constants-only interface
  deserves immutability (no instance fields).
- Stateless lambdas/anonymous classes currently stay at FINAL_FIELDS (known conservative gap).
- **Loggers are `@Container`s, not immutable** (2026-10-22, maddi b1cba7ec1; slf4j, java.util.logging,
  System.Logger): a log call writes into buffers we do not care about, so the log methods carry method-level
  `@IgnoreModifications` (IGNORE_MODIFICATION_METHOD: receiver modification skipped, parameters unaffected);
  queries (`isDebugEnabled`, `getName`, `atLevel`…) are `@NotModified`; `setLevel` and configuration stay plain
  modifying. `@StaticSideEffects` is ONLY for a modification of static state the caller does not own
  (`System.setOut`) — never contracted on an instance method like `logger.info`, nor on `java.util.logging`.
  The book follows since 2026-11-24: its logger examples of an escape (`setLevel`, `addAppender`) left 050; the
  examples there are a stream and `System.setOut`.
- **No `java.util.function` special case in the source analysis.** The implicit `@IgnoreModifications` on
  functional-interface parameters is a SHALLOW default (`ShallowMethodAnalyzer.computeParameterIgnoreModifications`,
  `FunctionTypes.isLibraryFunctionType`: `java.util.function`, `Runnable`, Kotlin's function types, plus an interface
  with ONE abstract method that is annotated `@FunctionalInterface` or extends a standard one -- NOT any SAM type:
  `Iterable`, `Comparable`, `Executor` stay out); source lambdas are handled by link lifting (§The link system).
  The book (080, anchor `function-types`) defines *function type* this way and states the convention for EVERY method,
  with or without a body (Bart, 2026-11-24); that the analyzer applies it to library methods only is a deviation (109).
- **There are no "transparent types"** (the term left the book on 2026-11-24): accessible vs hidden goes by the
  DECLARED type, never by whether the type's methods are actually called. Book 050's `OddPair` is `@FinalFields
  @Container` (dependent constructor and getters), not `@ImmutableContainer(hc=true)`; replacing a concrete type by
  a type parameter is the programmer's refactoring.
- **When immutability is the focus, analyze as many dependencies as SOURCE as possible, not as jars.**
  A type read shallowly (a compiled jar without an annotated API) gets conservative modification
  verdicts: an unannotated read accessor never establishes `@NotModified`, so a field read *directly*
  through it (`this.store.getOrDefault(...)`) is conservatively `@Modified`, which caps the enclosing
  type's immutability. The shallow/annotated-API path under-approximates the immutable side; source does
  not. (Concretely: `ParameterInfoImpl.analysis` was capped only because `PropertyValueMap.getOrDefault`
  lived in a jar — see `docs/design/eventual-info-hierarchy.md`.)
- javac is not thread-safe: all JavacTask access must be single-threaded
  (`-XDuseUnsharedTable=true`, synchronized lazy `getOrLoad`).

## Document map (src/docs/asciidoc/sections/)

Tutorial: 010 introduction · 020 final fields · 030 modification · 040 containers ·
045 dependence (anchor `linking-dependence`) · 050 immutability (incl. §Unmodifiable views) · **055 ownership** (computed behind
`OWNERSHIP`, incl. §Finalizers; this file's §Ownership) · 060 eventual (computed; §Eventual) ·
070 modification part 2 · 080 hidden content · **085 nullability** (computed; this file's §Nullability).
Technical: **105 the link system** · **108 convergence (iterating analyzer)** · **109 the analyzer today** (status per
annotation, switches, deviations from the definitions, measurements, pinning tests: the ONLY chapter that talks about
the current implementation; the tutorial chapters state principles and carry no issue numbers, dates, gates or test names).
Practice: 110 support classes · 115 the analyzer's own code · 120 other annotations ·
appendix A annotation overview. (090/100 exist on disk but are excluded: pre-sv terminology.)

## Open questions (2026-11-24)

- CodeLaser/maddi-mod#159: container elements of a concrete type, accessible or hidden for the holder (see §Core
  vocabulary); with it the `copyOf` shapes, the receiver gap of the fixpoint engine's structural verdict, and whether
  a modification of hidden content only (`list.forEach(consumer)` on a field) should count against rule 1.
- CodeLaser/maddi-mod#160: the function-type convention (implicit `@IgnoreModifications`) in source code.
- CodeLaser/maddi-mod#161: an explicit `@IgnoreModifications` on a parameter of a bodiless source method is read
  as `@Modified`.
- Book 045 `Counters` and 080 `Leaf`/`Bag`/`reset` still disagree until #159 is decided.
