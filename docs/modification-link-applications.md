# Candidate applications for `maddi-modification-link`

**Written 2026-07-30.** A survey of what else the link substrate could be used for, beyond the three
applications currently prioritized. This is a *proposal* document: nothing here is built, and none of it
asks for a change to what the engine concludes.

Read `maddi-modification-link/linking-manual.md` first — §1 for what linking is for, §4 for the notation
used throughout, and `maddi-modification-link/README.md` for the authoritative list of link natures and
the combination table.

The three prioritized applications, for reference:

1. **modification analysis** — `maddi-modification-analyzer`, deciding `@Modified`/`@NotModified`,
   `@Independent`, immutability;
2. **same-type linking** for extracting interfaces — downstream, in the jfocus `extractinterface` stack;
3. **object tracking** — downstream, in the jfocus dataflow stack.

---

## 1. The substrate, and which parts of it nothing reads yet

Linking produces a triple `(from, linkNature, to)` at every expression, summarized per method as a
`MethodLinkedVariables` — how the return value, the parameters and the receiver relate, plus which of them
are modified. That summary is the interprocedural interface: an application does not need to re-walk
bodies, it consumes mlv.

What is worth noticing when looking for new applications is that the link natures are not a modification
lattice. They are a **relational object-graph algebra**, in five families (`README.md` is authoritative):

| family | natures | what it expresses |
|---|---|---|
| identity / assignment | `≡` `←` `→` | the same variable, or one assigned from the other |
| field level | `≺` is-field-of, `≻`, `≈` shares-fields | containment in an object |
| object graph | `≤` is-in, `≥` contains, `∩` overlaps | reachability through an object graph |
| element level | `∈` `∋` `⊆` `⊇` `~` shares-elements, and the hedged `∈?` `∋?` | membership in a container |
| decoration | `↗` `↖` | functional-interface decoration |

Plus the `§` virtual-field / hidden-content model (see
`…/link/vf/virtual-fields.md`), which is what lets the element level say anything about a container whose
contents were never named.

The three prioritized applications consume the first three families and, through them, the modification
flags. **The element-level family and the decoration family are computed and, as far as this survey could
establish, not consumed by anything downstream.** That asymmetry is the main reason to think there is
unclaimed value here rather than just more work.

---

## 2. The criterion that ranks the candidates: presence or absence

Before the list, the thing that decides how much any of these can be trusted today.

Candidate applications divide by whether they need a link to be **present** or **absent**.

- **Presence** is a positive fact the engine derived. `∩` between a parameter and a field means the engine
  found a path.
- **Absence is not a fact.** Two things spoil it:
  - **Saturation.** `handoff-saturated-closure-collapse.md` measures link density across degraded methods
    at **0.55 median, 1.00 at the top**. Where the closure saturates, "unrelated" is not merely rare, it is
    vacuous — and §3 of that document is explicit that the saturation is *semantically correct*, so this is
    not a defect to be fixed away.
  - **Degradation.** A method that exceeds the per-method work ceiling is abandoned and degraded to a
    shallow summary (`DegradedAnalysisException`, `LinkComputerImpl.doMethod`), which reports **fewer**
    links than the truth.

The second is the serious one. For a disjointness-based application, degradation is not imprecision, it is
**unsoundness in the unsafe direction**: the application concludes "these do not interact" *because the
analysis gave up*, and green-lights a transformation on the strength of an absent answer.

So: presence-based applications are buildable on today's engine. Absence-based ones should be gated on the
closure-collapse work, and should never consume a degraded summary without knowing it is degraded.

---

## 3. The six candidates

### 3.1 Semantic preconditions for state-relocating refactorings

*Needs:* `≈` `∩` `≺`, plus `staticSideEffectsMethod`. **Presence-based.**

Downstream refactoring levers (move type, move types to a sub-project, extract companion, split class) each
validate their proposals, and the validation is good at what it does: it reports members that cannot move,
references that would become inaccessible, accessibility that the destination cannot satisfy, name
collisions. Every one of those is a **name-resolution or accessibility** question. Taken together the
validation is a *compilability and reachability* oracle: the result will compile, and nothing became
unreachable.

What it does not establish is that the result still behaves the same.

The reason the gap is structural rather than an oversight: both the conflict checks and the objective
function that *chooses* a split are defined over the **reference graph**. Consider two fields of a class
that point into the same mutable object. Syntactically they are independent fields; no method needs to call
anything to couple them. The edge between them therefore carries **zero weight** in a call-graph-weighted
cut, so the partitioner is free — and, since it is a cheap cut, inclined — to place them on opposite sides
of a split. The result compiles, no conflict fires, and one object that maintained an invariant across both
fields has become two objects each holding half of it. That is `≈` / `∩`, and no reference-level check can
see it, because there is no reference to see.

A second instance with different physics: moving statics between types changes *when* each class
initializer runs, since class initialization is per-class and lazy. If any of them has a side effect,
behaviour changes with nothing visible at the call site. `staticSideEffectsMethod` is the ingredient.

Where it does **not** apply: narrowing a member's visibility is safe iff nothing outside the new scope
references it — a pure reference question, already checked correctly. The criterion is therefore *a lever
needs a semantic precondition exactly when it relocates state, or changes when state is initialized*;
levers that only re-label or re-route references do not.

*Why this one first:* it needs no new product surface — it adds entries to conflict/message lists that
proposals already return — and it reuses all three prioritized applications rather than competing with
them. It is presence-based, so it is sound under saturation.

### 3.2 Seam audit for modularization

*Needs:* `≤` `≥` `∩` across a proposed module boundary. **Presence-based.**

A carve is currently justified graph-theoretically: derive an up-set, show it has no incoming edges, show
nothing left behind requires it. That proof is arithmetic and it holds — for *references*.

It says nothing about a mutable object handed across the seam that both sides keep writing. A dependency
edge says "A names B"; `∩` between a field on one side and a parameter on the other says "both sides mutate
the same instance". The second coupling survives the carve, is invisible to every existing lever, and is
exactly the kind of thing that makes a module boundary nominal.

This is the candidate that plugs most directly into modularization work in progress, and it is the natural
next query method alongside the existing modification-flow one.

### 3.3 Immutability / record-conversion advisor

*Needs:* `←` `≡` `≺` between constructor parameters and fields, plus `finalField`, `unmodifiedField`,
`independentMethod`. **Presence-based.**

"This class is one defensive copy away from being immutable" is an actionable, explainable claim. The part
that a simpler analysis cannot supply is whether a constructor parameter merely *initializes* a field or
**aliases caller-visible state** into it — the difference between a class that is already effectively
immutable and one whose invariant a caller can break from outside. That is precisely a link question, and
it is the one that decides whether the advice is sound.

Everything this asks is a presence question, which makes it the candidate with the clearest external story
per unit of risk.

### 3.4 Escape and defensive-copy audit

*Needs:* `≺` `≥` from a field to a returned value. **Presence-based in one direction, absence-based in the
other.**

The first half — a mutable internal reaching the outside through a return value or an argument — is a
classic defect class, and the ranking that makes it actionable (which callers actually mutate what they
received) is a modification question over links that already exist. Sound today.

The mirror half — flagging defensive copies as *unnecessary* because the source never escapes and is never
mutated — is a disjointness claim, and inherits everything in §2. Worth building the first half without
waiting for the second; worth being explicit in the output about which half a finding comes from.

### 3.5 Builder-leak detector

*Needs:* `≥` `∩` between builder fields and the object returned by the build method. **Presence-based.**

A `build()` that returns an object still sharing state with its builder is a real and nasty bug class: the
built object mutates when the builder is reused, and nothing about the code looks wrong. No linter catches
it, because it is an aliasing fact.

It is also the sharpest available demonstration, since maddi's own CST is builder-heavy — see
`builder-interface-split-impact.md` and `handoff-builder-leans.md` — so the detector can be evaluated on
this repository without any external corpus.

### 3.6 Disjointness → parallelisation and statement reordering

*Needs:* `∩` **absent** between the object graphs two calls touch. **Absence-based.**

Partly seeded already: the analyzer computes `commutableMethods` and `parallelParameterGroups`, so the
direction is on the roadmap. Two calls that touch disjoint object graphs can be reordered, deduplicated, or
run concurrently.

This is the most attractive-sounding candidate and the one to hold back. It is the only purely
absence-based item here, so it inherits the saturation ceiling as a *correctness* risk rather than a
performance one. The ordering is: closure collapse first, then a **witnessed** disjointness result that can
distinguish "proved unrelated" from "gave up", and only then this.

---

## 4. Suggested order, and the one requirement common to all of them

| | candidate | presence/absence | gate |
|---|---|---|---|
| 1 | semantic preconditions (§3.1) | presence | none |
| 2 | seam audit (§3.2) | presence | none |
| 3 | immutability advisor (§3.3) | presence | none |
| 4 | escape audit, first half (§3.4) | presence | none |
| 5 | builder leak (§3.5) | presence | none |
| 6 | disjointness (§3.6) | **absence** | closure collapse + witnesses |

**Every one of these has to report a witness, not a verdict.** A blocking conflict that nobody can check
becomes a suppressed warning — downstream scripts already pass "ignore warnings" routinely, which is
evidence that a validation nobody can audit has no force. The witness for §3.1 is the concrete field-to-field
aliasing path, or the named static initializer with the side effect; for §3.5 it is the path from a builder
field to the returned object.

This is the same question `handoff-saturated-closure-collapse.md` raises about a collapsed group — *what is
the witness of a fact that stands for N² facts?* — and it is worth settling once, for the engine and its
applications together, rather than twice.

---

## 5. What this document does not claim

- **The consumption gap in §1 is inferred**, from module structure and property names, not from a full trace
  of every downstream reader. The element-level and decoration families may well be consumed somewhere this
  survey did not look; if so, that narrows the argument for novelty but changes none of the six.
- **No costing.** None of these has been sized, and several (§3.2, §3.3) need a downstream surface to be
  useful at all, which is not a maddi decision.
- **Nothing here proposes changing what the engine concludes.** These are consumers of the existing
  substrate. Where a candidate is limited, §2 says the limit is in what absence of a link can mean — not in
  the correctness of the links themselves.
