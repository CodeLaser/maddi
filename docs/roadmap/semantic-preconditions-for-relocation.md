# Semantic preconditions for state-relocating refactorings

**Written 2026-09-25.** Expands [`modification-link-applications.md`](modification-link-applications.md)
§3.1 from a candidate into a design. It is still a *proposal*: nothing here is built, and nothing here asks
the engine to conclude anything different. Sections 1 and 3 are survey results, measured against the tree
on the date above; sections 2 and 4–8 are design.

**The short version.**

- The downstream levers that relocate state validate only reference-level facts.
- §3.1's own headline example, two aliasing fields split apart, does not change behaviour as stated (§2.1).
- The hazards that do change behaviour are narrower and more concrete (§2): `this` changes identity,
  a monitor gets split, a mutable field gets duplicated, and static initialisation gets re-timed.
- Three of those four hazards can be decided from data maddi already writes. The fourth is not available
  yet: static side effects are gated off by default, and they are not computed for field initialisers.
- The rule that makes all of it trustworthy is a **three-valued result**: found with a witness, clean, or
  not checked. Of the three, *not checked* is the one to watch. Degradation spoils presence-based checks
  as well, which §2 of the parent document does not say (§5 below).

---

## 1. What the levers check today

Survey of the downstream refactoring stack, 2026-09-25. Levers are named by their verb.

| lever | relocates | validation today | reads maddi analysis? |
|---|---|---|---|
| move type / move types to sub-project | a type (no state) | name collisions, accessibility, package cycles, resources | no |
| extract companion | static members to a new or existing holder | collisions, retained↔moved references, accessibility, a `static{}` block that must travel; **warns** on a moved static field with a non-constant initialiser | no |
| move static members | static members to an existing type | extract companion's checks, plus batch interference | no |
| split class | instance fields and methods, into parts | **none**; failures surface as exceptions | no |
| attach state | instance fields and methods, into an attachment object | retained↔moved references, dynamic type tests, collisions | no |
| method object | a method's locals, into fields of a new object | unreachable private members, uneditable signatures | no |
| pull up member | a declaration (no state) | supertype editable, not already declared | no |

The levers that *do* read maddi (extract interface, record/builder conversion, freeze class, generics,
library confinement, modification flow) are not state-relocating.

Four facts in the survey shape the design:

1. **Split class has no validation step.** It is the lever with the most freedom to relocate state and the
   only one with no place to put a finding.
2. **The partitioner never sees a field.** Clustering runs over methods only. A method-method edge weighs
   *presence* of a call-graph edge (a fixed 500), plus a user prior, plus a name-similarity prior. Fields are
   attached *after* clustering, to the part(s) whose methods access them. A field accessed from several
   parts goes to a common successor. A service-typed field is **duplicated** into each part that needs it.
3. **Warnings do not bind.** 307 modularization scripts pass `ignoreWarnings=True`, and two verbs document
   that it is "normally required". Only a `conflict` stops a write. A semantic finding filed as a warning
   is, in practice, a log line.
4. **The one semantic check is syntactic.** Extract companion warns when a moved static field has a
   non-constant initialiser (JLS 4.12.4), because the initialiser will now run during the destination's
   class initialisation. It cannot tell a harmless `new HashMap<>()` from a registration side effect, so
   it warns on both, and the warning is then ignored on both.

---

## 2. The hazards, stated precisely

### 2.1 A correction to §3.1's example

§3.1 says two fields pointing into the same mutable object, split across parts, become "two objects each
holding half" of an invariant. As stated this overreaches. A split moves **references, not objects**. After
the split, part A's field and part B's field still hold the same instance, every mutation is still visible
to both, and every method still runs the same statements. Aliasing survives a split.

What does not survive is the premise that one object *mediates* all access to the shared state. That
matters behaviourally in exactly two cases, §2.3 and §2.4. Otherwise it is a design-quality concern: the
invariant is now maintained across two classes. That concern belongs in the partitioner's objective (§6),
not in a precondition. The corrected criterion reads as follows. **Aliasing is never itself the hazard.
It is the reason a monitor split (§2.3) or a field duplication (§2.4) reaches further than the moved field.**

### 2.2 Identity of `this` changes (split class, attach state, method object)

A method that moves to a new object gets a new `this`. Behaviour changes when the old `this` was
*observable* from inside the method:

- it is returned: fluent `return this`, which is `FLUENT_METHOD`/`IDENTITY_METHOD`, or a return link `≡ this`;
- it is stored somewhere that outlives the call: registered as a listener, put into a map, handed to a
  callee that keeps it (a parameter link `0:x.§xs ∋ this` on the callee's summary, translated at the call);
- it is used as a monitor: `synchronized(this)`, or the method is `synchronized`;
- it is compared by identity, or it feeds `equals`/`hashCode` of a collection it sits in.

The first two are **link facts on the moved method's own summary**. The monitor is §2.3. Identity comparison
is a reference fact the levers could already check.

**Witness:** the moved method, and the link that carries `this` out, as the summary prints it. For example,
`register: 0:listeners.§xs∋this`.

### 2.3 A monitor is split (split class, attach state)

Methods that synchronise on `this` exclude each other. After a split, a `synchronized` method in part A and
one in part B lock different objects, so they no longer exclude each other. If both modify the same state,
a race appears where there was none. That is a behaviour change, and neither the compiler nor any
reference check can see it.

"The same state" is where aliasing earns its keep. Method `m` in A modifies `this.a`'s content. Method `n`
in B modifies `this.b`'s content. If `a` and `b` alias (`this.a.§m ≡ this.b.§m`), `m` and `n` modify the
same object while holding different locks. A field-level check would pass this. The modification-component
link catches it.

**Precondition:** for every pair of monitor-holding methods that end up in different parts, their modified
sets must be disjoint *modulo `§m` identity*. Violations are conflicts.

This is the one precondition here with an **absence** in it: "disjoint" is the claim. It is safe anyway,
for a specific reason. The lever is not asked to prove disjointness. It is asked to refuse on a found
overlap, and to report *not checked* (§5) when either method's summary is degraded. The absence never
becomes a green light by itself.

**Witness:** the two methods, the variable each modifies, and the `≡` link that equates them.

### 2.4 A mutable field is duplicated (split class)

The partitioner duplicates service-typed fields into each part that needs them. A duplicated field is two
fields from then on. That is harmless when the field is **effectively final and assigned once, from one
value, in the constructor**: both copies hold the same reference, and content mutations are shared,
because of aliasing. It changes behaviour when the field is ever **reassigned**: the part that reassigns
sees the new value, and the other keeps the old one.

**Precondition:** a field may be duplicated only if it is effectively final (`FINAL_FIELD`) and every
constructor assigns it once. Content modification is *not* a reason to refuse.

This is almost entirely a finality fact. It is listed here because the obvious rule, "never duplicate a
mutable field", is wrong in the conservative direction. It would refuse every service holding a cache.
The link view explains why that duplication is safe.

### 2.5 Static initialisation is re-timed (extract companion, move static members)

Moving a static field or a `static{}` block changes two things:

- **when** it runs: at the destination's class initialisation, which is lazy and triggered by first use;
- **its order** relative to the static initialisers left behind.

That changes behaviour only if the initialiser *does* something observable:

- a side effect on state outside the moved set, which is `STATIC_SIDE_EFFECTS_METHOD`;
- a read of mutable static state whose value depends on timing;
- a construction whose constructor has either of the above.

A constant-folded or pure initialiser can move freely.

This hazard differs from the other three in one respect. The data that should decide it is **not
available in a usable form today** (§3). The lever's current syntactic warning is more conservative than
anything maddi can offer until that is fixed, so this precondition is last in the build order (§7).

### 2.6 Where no semantic precondition applies

Move type, move types to a sub-project, and pull up member relocate declarations, not state. Class
initialisation of a moved type is unchanged, because the class is still initialised on first use. These
need no precondition from this document. This is §3.1's criterion, restated as a test that can be applied
mechanically: *a lever needs a semantic precondition exactly when the set of objects that hold a piece of
state, or the moment that state is initialised, changes.*

---

## 3. What maddi provides today, fact by fact

| needed fact | where it lives | status |
|---|---|---|
| a method lets `this` escape through its return | `METHOD_LINKS` → `ofReturnValue()`; `FLUENT_METHOD`, `IDENTITY_METHOD` | available |
| a method lets `this` escape through an argument | `METHOD_LINKS` → `ofParameters()` (links may name `this` and its fields) | available |
| what a method modifies, including via callees | `METHOD_LINKS` → `modified()` | available |
| which own-field slots a method assigns | `METHOD_LINKS` → `assigned()` | available; **not populated across `this(...)`/`super(...)`** |
| two fields share a mutable object | field `LINKS` (`LinksImpl.LINKS` on `fieldInfo.analysis()`), `§m ≡` between fields | available; **no tested example of a direct `this.a ≡ this.b` entry**; recoverable by composing through a shared parameter (`0:p→this.a`, `0:p→this.b`) |
| object-graph overlap `∩ ≤ ≥` | closure | **off** under `LinkComputer.Options.PRODUCTION` (on in the analyzer's default options); do not depend on it |
| the method is synchronized / has a `synchronized` block | `MethodInfo.isSynchronized()`, `SynchronizedStatement` | available (CST, not analysis) |
| a field is effectively final | `FINAL_FIELD` | available |
| a static initialiser has a side effect | `STATIC_SIDE_EFFECTS_METHOD` on `<static_N>` methods | **gated** (env `SSE`, off by default); **field initialisers not examined**; constructor calls not examined; a bodyless callee with no contract counts as FALSE |
| a summary is degraded | `DEGRADED_ANALYSIS_METHOD` | available; the *reason* (work ceiling vs expansion rounds) is logged, not stored |

Two consequences for the design:

- **Build on `≡`, `←`, `≈` and `§m`, never on `∩`.** The object-graph natures are exactly the ones production
  switches off to keep the closure tractable. They are also the ones saturation fills. Everything §2
  needs is expressible without them.
- **`STATIC_SIDE_EFFECTS_METHOD` may only be read as TRUE.** FALSE is the value for "no body and no
  contract". Reading FALSE as "pure" is the degraded-summary mistake in another form. So §2.5 can
  *escalate* a warning to a conflict when a side effect is found. It can never *suppress* the syntactic
  warning.

---

## 4. The query surface

Downstream consumers read maddi properties directly today, and each re-derives what a link means. For these
preconditions that is the wrong split of responsibility. Several rules are engine knowledge, and a consumer
reimplementing them will get them subtly wrong:

- `§m` is the modification component;
- the `☷` pass variant of `≡` excepts some methods;
- `RedundantLinks` folds `≈ ≺ ≻` into `≈`;
- parameter links are the only place a constructor's field wiring is visible.

Proposal: one small, read-only class in `maddi-modification-analyzer`, tested on maddi fixtures, with one
method per hazard:

```java
Finding thisEscapes(MethodInfo moved);                                  // §2.2
Finding monitorOverlap(MethodInfo m, MethodInfo n);                     // §2.3
Finding duplicationSafe(FieldInfo f);                                   // §2.4
Finding staticInitEffect(TypeInfo origin, Set<Info> moved);             // §2.5 (TRUE-only)

sealed interface Finding {
    record Found(String witness, List<Info> involved) implements Finding { }
    record Clean() implements Finding { }
    record NotChecked(String why, List<Info> involved) implements Finding { }  // degraded, shallow, gated off
}
```

The levers own the policy. They decide what counts as a conflict, what the message says, and how
`NotChecked` is surfaced. maddi owns what the facts mean. The `Finding` type is also the place to settle
the witness question once (parent §4).

---

## 5. Three values, and why presence is not enough

Parent §2 ranks candidates by presence versus absence, and concludes that presence-based checks are sound
under saturation. That is right about saturation. **It is not right about degradation, and the difference
matters here.**

A degraded method gets a shallow summary that reports *fewer* links than the truth. A presence check over
that summary finds nothing and returns clean. For a precondition, "found nothing" is the green light. So a
presence-based precondition over a degraded summary green-lights the transformation because the analysis
gave up, which is exactly the unsafe direction §2 warns about for absence-based applications.

So every query returns one of three values:

- **Found** carries a witness. The lever files a conflict.
- **Clean** is returned only when every involved method has a non-degraded, source-analysed summary, and
  the relevant analysis is enabled.
- **NotChecked** names what was missing: a degraded or shallow method, a gated-off analysis, or an
  undecided property.

Policy choice for the levers, not for maddi: `NotChecked` has to be visible in a way that survives
`ignoreWarnings=True`, or it has no force. The `precondition` severity already exists in the response
contract. Using it for `NotChecked` would keep it out of the ignored warning stream without making it
block the write.

---

## 6. The objective-function side

Parent §3.1 names two gaps: the checks and the objective function that chooses a split. Preconditions close
the first. They refuse a bad split after it has been chosen. The second is about not choosing it.

The partitioner clusters methods only, so a field-field edge would do nothing. The aliasing signal has to be
translated into **method-method weight**. Two methods that modify content in the same `§m` class, directly or
through aliasing fields, get an edge, as if one called the other. That keeps the invariant-maintaining methods
of §2.1 together without making aliasing a refusal, which is the right strength for a design-quality concern.
It also makes §2.3 rarer, since the monitor-holding modifiers of one object end up in one part.

This is a scoring change downstream and needs its own measurement: how many existing splits move, and whether
the moved ones read better. It is listed here so the two halves of §3.1 are not conflated. The preconditions
are needed first, since a partitioner nudge prevents nothing.

---

## 7. Order of work

| | item | lever(s) | data | gate |
|---|---|---|---|---|
| 1 | `thisEscapes` (§2.2) | split class, attach state, method object | `METHOD_LINKS`, fluent/identity | none |
| 2 | `duplicationSafe` (§2.4) | split class | `FINAL_FIELD`, constructor assignments | none |
| 3 | `monitorOverlap` (§2.3) | split class, attach state | `METHOD_LINKS` modified sets, `§m ≡`, `isSynchronized` | a helper that resolves `§m` classes across fields |
| 4 | a validation step in split class, carrying 1–3 | split class | — | downstream |
| 5 | method-method weight from shared `§m` (§6) | split class partitioner | as 3 | measurement |
| 6 | `staticInitEffect` (§2.5), escalate-only | extract companion, move static members | `STATIC_SIDE_EFFECTS_METHOD` | SSE on by default; field initialisers and constructor calls covered |

Items 1 and 2 are small, use only data maddi already writes in production, and have obvious witnesses. Item 6 is
last because it needs engine work first, and until then the existing syntactic warning is the safer check.

---

## 8. How to know it works

- **Fixtures, one per hazard, in maddi**: a fluent method, a listener registration, a `synchronized` pair over
  aliased fields, a duplicated field reassigned in a setter, a `static{}` block that registers with a global
  registry. Each has a positive variant and a harmless twin that must come back `Clean`. The harmless twins
  are the two-field service holding a shared cache (§2.4) and aliased fields with no monitor (§2.1).
- **A degraded twin of each positive fixture** (forced with a low `maddi.workCeiling`) that must come back
  `NotChecked`, never `Clean`. This test pins §5, and it is the one most likely to regress silently.
- **maddi's own CST** as a no-corpus proving ground. It is builder-heavy and uses fluent returns throughout,
  so `thisEscapes` has plenty to find.
- **Replay of recorded modularization runs**, downstream. Count how many committed splits and companion
  extractions each check would have flagged, and hand-sample the flags for false positives. A precondition
  that fires on most real splits will be ignored like the warnings are, so the false-positive rate is a
  design constraint, not a metric to report afterwards.

## 9. Open questions

- Should `NotChecked` block when the involved method is on the moved side, and not block when it is only on
  the retained side? The asymmetry is plausible but unargued.
- `assigned()` is not populated across `this(...)`/`super(...)`, so §2.4's "assigned once in the constructor"
  needs its own walk for delegating constructors.
- Field `LINKS` has no tested direct field-field entry. Before §2.3 relies on it, a fixture should establish
  whether `this.a.§m ≡ this.b.§m` is written for `this.a = p; this.b = p;`, or has to be composed from the
  constructor's parameter links.
