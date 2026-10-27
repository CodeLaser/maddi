# Road to Immutability — condensed summary for LLMs

Dense, load-bearing summary of `src/docs/asciidoc/` (the authoritative terminology source for maddi's
immutability/modification/linking vocabulary). Read this instead of the full book; open a specific
chapter only when detail is missing here. Last checked against the code and the book: 2026-10-23 (release 0.9.5).

## Core vocabulary

- **maddi** — whole-program static analyzer for Java (and Kotlin via a shared CST). Its *linking engine* (which
  objects flow where, which share state) feeds three applications: modification/independence/immutability (this
  file's core), **nullability** (§Nullability, since 2026-10) and the Java→Kotlin / Java→C# **translations**, which
  read the verdicts (read-only collection types from the structural twins and unmodifiable views; `?`/`!!` from
  nullability). Apache-2.0 licensed.
- **Mutable** means "not immutable at any positive level"; `@Mutable` is the explicit bottom verdict.
- **Content** of an object = the objects reachable from its fields (the object graph), split into:
  - **accessible content** — reachable through fields/accessors the analyzer can see and name;
  - **hidden content** — content that exists but cannot be named structurally: type parameters,
    extensible (non-final) types, the elements of shallowly-analyzed containers. Leaf immutables
    (String, primitives, boxed) have NO hidden content; containers of immutables DO.
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
    The book's §"Propagating modifications" (080) describes it; the step back from a modified container to its
    elements (`useAddNewLine`) is still not computed.
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
  `@StaticSideEffects` is for modifying static state the primary type (with its enclosing and parent
  types) does NOT own -- directly, or through any callee (static, instance, constructor) that modifies its own
  type's static state: `Client` calling `Counter.next()` is one, `Counter.next()` itself is not
  (CodeLaser/maddi-mod#65; computed from the per-method set `STATIC_STATE_MODIFIED_METHOD`). A singleton follows the normal rules unless `@IgnoreModifications` is on the field giving
  access to it. Since 2026-09-23 the hc-free check (`instanceFieldTypesDeeplyImmutable`) counts statics too.
- **Self-referencing fields** (declared type = the type itself) are skipped for rules 1 and 2, for the hc label, and
  in the independence that FEEDS the immutability rule (`independentIgnoringSelfFields`) -- never for rule 0
  (assignability). The PUBLISHED independence still counts them: dependent while the type is undecided, and after
  it is decided independent if the type is immutable, dependent if not (a mutable `Node` returning `next`).
- Exposure matters: a type that stores externally supplied mutable objects (dependent constructor) or
  returns its mutable content (dependent accessor, e.g. record accessors) caps at FINAL_FIELDS.
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
Mark labels are field *names* (a mark is often inherited). Eventuality is deliberately kept OUT of the
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
  pass runs with undecided inputs resolved by strategy (undecided supertype floored at FINAL_FIELDS;
  no-information treated as non-modifying), then re-certifies. Conclusions that rest on broken cycles
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
  re-derives and re-certifies; up to `MODREACH_ROUNDS`=3. Seeds: a walkable body (in order, non-empty, not
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
- **Fault tolerance**: statement/method-level containment (degrade to shallow summary), tight-ceiling
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
- **Java output** (`NullabilityDecorator`, flavours JSpecify/Checker/JetBrains/JSR-305/maddi): TOP-LEVEL state
  only today (an array's elements in a TYPE_USE flavour; type arguments `List<@Nullable String>` not yet);
  `@NullUnmarked` for an UNSPECIFIED method only in JSpecify; an UNSPECIFIED field is written `@Nullable`.
  Not exposed on the CLI/plugins in 0.9.5 (reaches users through `--translate`).
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
  `@UtilityClass` and `@Finalizer` are read as *contracts* by `AnnotationToProperty` (and shown by
  `DecoratorImpl`) but never inferred; `@UtilityClass` forces IMMUTABLE + INDEPENDENT on the type. `@Singleton`
  and `@ExtensionClass` exist in `maddi-annotation` but have no property and are read by nothing. Not in the
  book at all, but live (read by `AnnotationToProperty`): `@Commutable`, `@GetSet`, `@Docstring(s)`. `@NotNull`/`@Nullable` left the chapter on 2026-10-23: they ARE computed, by the nullability
  pass (chapter 085 below).
- **The reachability pass (MODREACH) follows no element link for a parameter**: its E1/E2 projections walk
  assignment links only, so under MODREACH the published DEEP `UNMODIFIED_PARAMETER` of a walkable body never
  records an element-only modification (`for (Item i : items) i.touch()` reads unmodified); the fixpoint's
  FALSE is reverse-upgraded at the cutover. Fields keep that evidence through the statement-level seeds.
  Measured 2026-10-10 (`TestStructuralModification`), not changed: the structural twins make the distinction
  explicit instead.
- **`@Finalizer`'s modification semantics ARE implemented and load-bearing** (its three life-cycle
  sequencing rules are not): a call to a finalizer NEVER marks the receiver modified — the guard
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
- **No `java.util.function` special case in the source analysis.** The implicit `@IgnoreModifications` on
  functional-interface parameters is a SHALLOW default (`ShallowMethodAnalyzer.computeParameterIgnoreModifications`,
  `FunctionTypes.isLibraryFunctionType`: the standard interfaces plus any `@FunctionalInterface` extending or shaped
  like one); source lambdas are handled by link lifting (§The link system).
- **Transparent types are not detected**: book 050's `OddPair` is `@FinalFields @Container` (dependent constructor
  and getters), not `@ImmutableContainer(hc=true)`; the substitution by a type parameter is the programmer's.
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
045 linking & dependence · 050 immutability (incl. §Unmodifiable views) · 060 eventual (computed; §Eventual) ·
070 modification part 2 · 080 hidden content · **085 nullability** (computed; this file's §Nullability).
Technical: **105 the link system** · **108 convergence (iterating analyzer)**.
Practice: 110 support classes · 115 the analyzer's own code · 120 other annotations ·
appendix A annotation overview. (090/100 exist on disk but are excluded: pre-sv terminology.)
