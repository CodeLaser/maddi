# Handoff — surface the `*Info` interfaces' eventual verdict (greatest-fixpoint Part B)

**Audience:** a fresh model implementing this without today's session context.
**Status:** designed, not implemented. One speculative attempt was made and reverted (see §7).
**Base commit:** `66913f72` on branch `ws/eventual`. Tree clean.
**Gate:** everything here lives behind the env gate `EVENTUALCLUSTER` (set = on). Off the gate, all of it must be a no-op.

---

## 0. TL;DR / the one task

The `Info` type family (`TypeInfoImpl`, `MethodInfoImpl`, `FieldInfoImpl`, `ParameterInfoImpl`, `TypeParameterImpl`)
is meant to be **computed** eventually `@Immutable(hc=true)` on maddi's own code. Under the `EVENTUALCLUSTER`
gate an optimistic greatest-fixpoint "seed" makes them reach that verdict, but the **contraction** we just added
(step 2) retracts 12 of the 17 seeded verdicts — the whole flagship family — because they lean on the `*Info`
**interfaces** (`TypeInfo`, `MethodInfo`, …) and the abstract base `InfoImpl` being eventually immutable, and the
analyzer never actually produces those verdicts.

This handoff covers **Part B**: make the `*Info` **interfaces** obtain their own eventual verdict
(`@Immutable(hc=true, after="inspection")`). The blocker is a specific, well-diagnosed gap in the method-level
"eventually non-modifying" analysis. The fix is a reframe of one method, `nonModifyingLabels` /
`receiverAfterLabels`, into a unified helper (call it `commitLabels`). Part A (`InfoImpl` inherits its mark from
its subclasses) is a **separate** follow-up, described in §9 — Part B alone will not drop the retraction to 0, but
it is the harder half and the right place to start.

**Definition of done for Part B:** with `EVENTUALCLUSTER=1`, the dogfood shows the `*Info` interfaces gaining
`eventual=@Immutable(hc=true)(after="inspection")` (and the flagships' interface-typed cross-reference
assumptions therefore discharge), the contraction retraction count drops materially (toward 0, reaching 0 only
once Part A also lands), **and** the gate-OFF corpus A/B (Fernflower FPDUMP) stays byte-identical.

---

## 1. Background: the greatest-fixpoint arc (what's already in place)

Read `docs/eventual-info-hierarchy.md` first — it's the running record. Short version of the pieces already
committed on this branch:

- **The `Info` cluster is a mutual-reference SCC.** `MethodInfoImpl.typeInfo : TypeInfo`,
  `ParameterInfoImpl.methodInfo : MethodInfo`, `TypeInfoImpl.compilationUnitOrEnclosingType :
  Either<CompilationUnit,TypeInfo>`, and every `*InfoImpl` extends the abstract `InfoImpl`. Each type's eventual
  verdict needs the others', so a monotone least-fixpoint concludes nothing.
- **`EventualCluster`** (`maddi-modification-analyzer/.../impl/EventualCluster.java`, gated `EVENTUALCLUSTER`) is
  the optimistic **seed**: it identifies the cluster (types with eventual intent + their supertypes by upward
  closure) and lets two analyzer sites treat a candidate as eventually immutable before its verdict is proven:
  - `TypeImmutableAnalyzerImpl.immutableSuper` — an unproven candidate **supertype** contributes
    `IMMUTABLE_HC`-after-mark instead of dragging the subtype to MUTABLE.
  - `TypeEventualAnalyzerImpl.fieldHoldsCommittableContent` → `isEventuallyImmutableFieldType` — an unproven
    candidate **cross-reference field type** is treated as eventually immutable.
- **Step 1 — witnessing.** `EventualCluster.treatAsEventuallyImmutable(member, candidate, actual)` records the
  edge `member → candidate` in `assumptions()` whenever it answers `true` only because of the seed. This is the
  ledger the contraction walks. (Both call sites thread the `member`.)
- **Step 2 — contraction.** `EventualClusterContraction` runs once at the terminal certification point in
  `IteratingAnalyzerImpl` (just before `logVerdictFingerprint`): `membersToRetract(haveVerdict, assumptions)`
  keeps the largest subset closed under "every candidate I assumed is retained" and **retracts**
  `EVENTUALLY_IMMUTABLE_TYPE` (via `analysis().removeIf`) on the rest. It runs *outside* the monotone loop (a
  weakening `TolerantWrite` would refuse); sound because the seed only ever influenced
  `EVENTUALLY_IMMUTABLE_TYPE`, so clearing that one property is the whole retraction.

**Golden rule (non-negotiable):** an engine change is accepted only if the certified corpus (Fernflower /
Timefold / Langchain4j) produces a **byte-identical** FPDUMP with the gate OFF. Everything in this handoff is
gated, so gate-OFF must be a literal no-op. See §8.

---

## 2. The observed problem (the dogfood scoreboard)

Run the dogfood with `EVENTUALCLUSTER=1` (recipe in §8). Current state at base commit `66913f72`:

- Log line: `EC contraction: retracted 12 optimistic eventual verdict(s)…`
- Surviving eventual verdicts: **5** (all genuinely self-contained: `ModuleInfo.Provides`/`Uses`, `Variable`,
  `ModuleInfoImpl.ProvidesImpl`/`UsesImpl`).
- Retracted: the entire `Info` flagship family (`TypeInfoImpl`, `MethodInfoImpl`, `FieldInfoImpl`,
  `ParameterInfoImpl`, `TypeParameterImpl`, `CompilationUnitImpl`, …) — all `eventual=null` in the FPDUMP.
- `InfoImpl` and the interfaces `TypeInfo`/`MethodInfo`/`FieldInfo`/`ParameterInfo`/`Info` are all `eventual=null`
  too (`Info` = `@FinalFields`; the other interfaces = `@Mutable`).

The retraction is **correct** — the seeded verdicts genuinely rested on `InfoImpl`/the interfaces being eventual,
which the analyzer never establishes. The fix is to establish them, not to weaken the contraction.

---

## 3. Diagnostic: exactly which methods block the interfaces

FPDUMP was extended (commit `66913f72`) to emit `eventuallyNonMod=[…]` and `eventual=…` on method lines. Set
`FPDUMP=<file>` on the dogfood run and inspect the interface methods.

For an interface (fieldless) to reach `IMMUTABLE_HC`-after-`inspection`, **every** abstract accessor that is
`nonModifying=false` (modifies *before* the mark, through a lazy `inspection.get()`) must carry
`eventuallyNonMod=[inspection]` (so `computeTypeLevel` can excuse it after the mark). Most do. The **holdouts** —
`nonModifying=false` with **no** `eventuallyNonMod` label — are what keep each interface capped:

| interface | holdout count | representative holdouts |
|---|---|---|
| `TypeInfo` | ~19 | `primaryType`, `parentAndInterfacesImplemented`, `recursiveSuperTypeStream`, `constructorAndMethodStream`, `isInnerClass`, `isSealed`, `superTypesExcludingJavaLangObject` |
| `MethodInfo` | 7 | `isFactoryMethod`, `isSAMOfStandardFunctionalInterface`, `topOfOverloadingHierarchy` |
| `Info` | 3 | `descriptor`, `fullyQualifiedName` |
| `FieldInfo` | 3 | (arg-taking methods) |
| `ParameterInfo` | **0** | — (see §9, residual) |

The dogfood FPDUMPs from today are at
`/private/tmp/claude-501/-Users-bnaudts-git-ws-eventual-maddi/5c146558-bd99-44ce-9c81-d85dd4fe16f0/scratchpad/dogfood_diag_fp.txt`
(these are ephemeral scratch files; regenerate with §8 if gone).

Query to list holdouts for an interface `X`:
```
grep -E "^method nonModifying=false " <fpdump> | grep -v eventuallyNonMod | grep -v getset= \
  | grep -oE "cst\.api\.info\.X\.[a-zA-Z]+\(\)" | sort -u
```

---

## 4. Root cause (traced, with two worked examples)

The holdouts are **cross-reference read-through accessors**: they modify (before the mark) through
`this.<cross-ref field>`, where the field's type is a not-yet-proven cluster interface, or through a chain rooted
in an *eventually*-non-modifying `this`-accessor. Two real bodies:

```java
// MethodInfoImpl.isFactoryMethod()
return isStatic() && returnType().typeInfo() != null
       && returnType().typeInfo().isEnclosedIn(typeInfo);   // typeInfo == this.typeInfo (the field)

// TypeInfoImpl.primaryType()
MethodInfo methodInfo = enclosingMethod();
if (methodInfo != null) return methodInfo.primaryType();
return compilationUnitOrEnclosingType.isLeft() ? this
     : compilationUnitOrEnclosingType.getRight().primaryType();
```

The method that decides "is this method non-modifying after the mark?" is
`TypeEventualAnalyzerImpl.computeEventuallyNonModifying` (≈ line 280). For each **modifying** call in the body it
asks `nonModifyingLabels(typeInfo, mc)` (≈ line 327) for "the mark labels after which this call cannot modify
`this`", and bails if that returns null. `nonModifyingLabels` delegates the receiver case to
`receiverAfterLabels(typeInfo, mc.object())` (≈ line 351), which tries to prove the **receiver** is a *committed
`this`-field* by walking a chain of **genuinely non-modifying** reads (`isNonModifyingRead`, ≈ line 368) down to a
`this.<committable field>`.

**Why it bails:** the real receiver chains root in a `this`-accessor that is only *eventually* non-modifying —
`returnType()`, `enclosingMethod()` — for which `isNonModifyingRead` is `false`. So the chain cannot be rooted →
`receiverAfterLabels` returns null → `computeEventuallyNonModifying` bails → the accessor never gets an
`EVENTUALLY_NON_MODIFYING` label → the interface keeps a modifying method → its type-level verdict never
surfaces. There is also a separate **parameter guard** in `computeEventuallyNonModifying` (≈ line 309) that bails
on *any* argument referencing `this` (e.g. `isEnclosedIn(this.typeInfo)`), even when that argument is a
committable field.

**The invariant is wrong.** `receiverAfterLabels` insists "prove the receiver is a committed `this`-field."
The correct invariant is: **a call is non-modifying-of-`this` after mark M iff every `this`-derived value it
touches (its receiver *and* its arguments) is committed by M.** A receiver that is not `this`-derived (another
object) cannot modify `this` at all; a `this`-derived receiver/arg is safe once the marks that commit it have
passed.

---

## 5. The fix: a unified `commitLabels(owner, expr)` helper

Replace the receiver-only rooting (`receiverAfterLabels`) and the all-or-nothing parameter guard with **one**
function applied uniformly to a call's receiver and every argument.

### 5.1 Contract

```
Set<String> commitLabels(TypeInfo owner, Expression expr)
```

Returns the mark labels after which `expr`'s value is guaranteed to be **committed** (immutable) or is **not
`this`-derived** — i.e. after which a call receiving `expr` cannot, through `expr`, modify `owner`'s (`this`'s)
accessible state:

- **`∅` (empty set):** `expr` is not `this`-derived (a parameter, local, constant, static reference, a freshly
  constructed object, or a value obtained by reads through an already-immutable object). Nothing to commit.
- **non-empty `L`:** `expr` is `this`-derived and becomes committed after the marks in `L`.
- **`null`:** `expr` is `this`-derived but cannot be shown committable — bail (the enclosing method is not
  eventually-non-modifying via this path). Cases: bare `this` (mid-transition); a `this`-field that is not
  committable content; a modifying `this`-accessor that is not eventually-non-modifying; any expression shape not
  handled below.

### 5.2 Algorithm (recursive over expression shape)

```
commitLabels(owner, expr):
  if not referencesThis(expr):                      # fast path — nothing of this involved
      return ∅

  if expr is VariableExpression ve:
      if ve.variable is This:      return null       # bare this, mid-transition — cannot commit
      if ve.variable is FieldReference fr with scopeIsThis():
          if isOwnField(owner, fr.fieldInfo) and fieldHoldsCommittableContent(fr.fieldInfo):
              return { fr.fieldInfo.name }
          return null                                # a non-committable field of this
      return null                                    # other this-referencing variable shape

  if expr is MethodCall mc:
      acc = ∅

      # --- receiver ---
      if mc.object is VariableExpression over This:          # this.accessor(...)
          accessor = mc.methodInfo
          if isNonModifyingRead(accessor):
              pass                                           # genuinely non-modifying: contributes nothing
          else:
              enm = accessor.analysis().EVENTUALLY_NON_MODIFYING_METHOD  # Set<String>, empty if absent
              if enm is empty: return null                   # modifying this-accessor, not eventually-non-mod
              acc ∪= enm
      else:
          recv = commitLabels(owner, mc.object)
          if recv is null: return null
          acc ∪= recv

      # --- the value this call RETURNS must itself be committable, so downstream reads stay safe ---
      # (skip this check when commitLabels is called on the TOP-LEVEL call being excused — see 5.3;
      #  it applies only to intermediate chain receivers, whose result feeds another read/call)
      if not returnTypeHoldsCommittableContent(mc): return null

      # --- arguments ---
      for arg in mc.parameterExpressions:
          a = commitLabels(owner, arg)
          if a is null: return null
          acc ∪= a
      return acc

  return null                                        # any other this-referencing expression: conservative
```

Helpers to reuse / add:
- `referencesThis`, `isOwnField`, `isNonModifyingRead`, `fieldHoldsCommittableContent` — all already exist in
  `TypeEventualAnalyzerImpl`. `fieldHoldsCommittableContent` already carries the `EventualCluster` seed **and**
  the step-1 witnessing (via `isEventuallyImmutableFieldType(member, fieldType)`), so reusing it means the new
  path records its assumptions automatically — do not bypass it.
- `returnTypeHoldsCommittableContent(mc)` — **new**, a small twin of `fieldHoldsCommittableContent` operating on
  `mc.concreteReturnType()` (or `mc.methodInfo().returnType()`): the concrete return type holds committable
  content when it is itself eventually-immutable / a cluster candidate (seed) or an immutable single-indirection
  wrapper (`Either`, `Option`) of such — same logic, same seed+witness. For `returnType()` (returns
  `ParameterizedType`, a cluster candidate) this is true; for `typeInfo()` on a `ParameterizedType` (returns
  `TypeInfo`, a candidate) this is true.

### 5.3 Integrating into `computeEventuallyNonModifying`

In the per-call handling (≈ lines 297–315), replace the `nonModifyingLabels(...)` receiver logic **and** the
parameter guard with:

```java
if (e instanceof MethodCall mc) {
    // (1) transition calls belong to the mark, not "after" it
    Value.Eventual cev = eventualOf(mc.methodInfo());
    if (cev.isMark() || (cev.isOnly() && Boolean.FALSE.equals(cev.after()))) { bail(); return false; }

    // (2) receiver: only a MODIFYING callee can modify its receiver
    Value.Bool nm = mc.methodInfo().analysis().getOrNull(NON_MODIFYING_METHOD, ...);
    boolean calleeModifies = nm == null || nm.isFalse();
    if (calleeModifies) {
        if (mc.object() is a `this` VariableExpression) {
            // this.<accessor>() forward: the accessor itself must be eventually-non-modifying
            Set<String> enm = mc.methodInfo().analysis().EVENTUALLY_NON_MODIFYING_METHOD (empty if absent);
            if (enm.isEmpty()) { bail(); return false; }
            labels.addAll(enm);
        } else {
            Set<String> recv = commitLabels(typeInfo, mc.object());   // top-level: no returnType check on mc
            if (recv == null) { bail(); return false; }
            labels.addAll(recv);
        }
    }

    // (3) arguments: a call may modify a this-derived argument (a @Modified param); commit each
    for (Expression arg : mc.parameterExpressions()) {
        Set<String> a = commitLabels(typeInfo, arg);
        if (a == null) { bail(); return false; }
        labels.addAll(a);
    }
}
```

Note the asymmetry flagged in the pseudocode's `returnTypeHoldsCommittableContent` comment: the **top-level** call
being excused does not need its own return type committable (we only care that it doesn't modify `this`), so call
`commitLabels` on its receiver/args, not on the call itself. The `returnTypeHoldsCommittableContent` gate applies
only to **intermediate** receiver calls reached by recursion inside `commitLabels`.

### 5.4 Worked traces (use these as tests, see §6)

- `MethodInfoImpl.isFactoryMethod` → `returnType().typeInfo().isEnclosedIn(this.typeInfo)`:
  - receiver `returnType().typeInfo()`: `commitLabels` → `typeInfo()` call, receiver `returnType()` (a
    `this`-accessor, eventually-non-mod → `{inspection}`, return type `ParameterizedType` committable), then
    `typeInfo()` return type `TypeInfo` committable → `{inspection}`.
  - arg `this.typeInfo`: committable field → `{typeInfo}`.
  - excused after `{inspection, typeInfo}`. ✅
- `TypeInfoImpl.primaryType` → `compilationUnitOrEnclosingType.getRight().primaryType()`:
  - receiver `this.compilationUnitOrEnclosingType.getRight()`: field committable (`Either` wrapper of candidate
    `TypeInfo`) + `getRight()` non-modifying read → `{compilationUnitOrEnclosingType}`.
  - the `enclosingMethod()` branch: `enclosingMethod()` is a `this`-accessor (eventually-non-mod → `{inspection}`),
    then `methodInfo.primaryType()` where `methodInfo` is a **local** (not `this`-derived) → `∅`.
  - excused after `{inspection, compilationUnitOrEnclosingType}`. ✅ (matches `TypeInfoImpl`'s known mark)

---

## 6. Soundness argument and caveats (READ THIS)

**Why it's sound.** After a type's mark, all of its fields are committed (immutable). A call inside a method
modifies `this` only if it can reach `this`'s *mutable* accessible state — through its receiver (if the receiver
is, or aliases, a mutable part of `this`) or an argument (ditto). `commitLabels` returns non-null only when every
`this`-derived receiver/arg bottoms out at (a) a `this`-field holding committable content, or (b) a chain of
non-/eventually-non-modifying reads whose intermediate results are themselves committable (immutable) — an
immutable value cannot alias a *mutable* part of `this` in any way a later call could exploit. A non-`this`-derived
receiver/arg (`∅`) can't reach `this` at all. Bare `this` and non-committable `this`-fields return `null` (bail).

**Caveat 1 — the aliasing trap.** The `returnTypeHoldsCommittableContent` gate on intermediate calls is
load-bearing: without it, a genuinely-non-modifying accessor that returns a **mutable** `this`-field (e.g. a
`List<X> getItems()` returning `this.items`) would be treated as `∅`, and a downstream `.add(...)` would modify
`this` after the mark — unsound. Keep the gate. If in doubt, be conservative (return `null`); the dogfood
scoreboard will show under-approximation as "interface still not surfacing", which is safe, whereas
over-approximation shows as a *wrong* surviving verdict (see the validation in §8).

**Caveat 2 — do not touch `labelsOfReceiver`.** There is a *different* method, `labelsOfReceiver` (≈ line 424),
used by `computeEventual` for `@Mark`/`@Only` propagation (a separate concern from eventually-non-modifying).
Leave it alone.

**Caveat 3 — keep it gated.** `commitLabels`' new leniency (following eventually-non-modifying chains, excusing
committable-field args) must be reachable **only** under `EVENTUALCLUSTER`, so the gate-OFF verdict is unchanged.
The cleanest way: guard the new branches with `EventualCluster.ENABLED`, falling back to the *old* behaviour off
the gate — old `receiverAfterLabels` for the receiver, and the old "bail on any `this`-referencing arg" parameter
guard. (Off the gate, `fieldHoldsCommittableContent` already returns true only for *genuinely* eventual fields,
so the receiver path is unchanged; the risk is the new arg-excusal and the eventually-non-mod chain step, which
must be gated.) Confirm with §8's gate-OFF A/B.

---

## 7. What was already tried and reverted (don't repeat)

A quick patch that only extended the **parameter guard** (excuse `this.<committable field>` args via
`receiverAfterLabels`) was implemented and reverted. It did **not** help: the dogfood retraction went 12→13 and
no interface surfaced, because the blockers bail on the **receiver** chain (`returnType().typeInfo()`) *before*
the parameter guard is even reached. The receiver reframe (§5) is the actual fix; the parameter part is
necessary too but insufficient alone. Both are folded into §5.3.

---

## 8. Validation workflow (do all three)

### 8.1 Unit tests (cheap, do first)
`commitLabels` is mostly pure over expression shapes. Add a test class in
`maddi-modification-analyzer/src/test/java/.../eventual/` that parses small synthetic types and asserts the
labels for: `this.committableField` → `{field}`; a non-`this` local/param → `∅`; bare `this` → bail; a
non-committable field → bail; the two worked chains in §5.4. Model it on the existing eventual tests
(`TestEventualClusterAssumptions`, `TestEventualClusterContraction`). Also extend `TestEventualPropagation`.
Run: `./gradlew :maddi-modification-analyzer:test` (allowed — it's a normal `test`, not `slowTest`).

### 8.2 Gate-ON dogfood (the scoreboard)
Recipe (input configuration already generated; the `run` task recompiles the analyzer). AAPI preload dirs are
mandatory. Exit code **5 is expected** (`ANALYSER_ERROR` on a few printer methods); results are still written.
```
AAPI=$PWD/maddi-aapi-archive/src/main/resources/org/e2immu/analyzer/aapi/archive/analyzedPackageFiles
EVENTUALCLUSTER=1 FPDUMP=/tmp/ec_fp.txt ./gradlew :maddi-run-openjdk:run --args="\
  --input-configuration $PWD/dogfood/cst-impl/build/inputConfiguration.json \
  --preload-analysis-results-dirs $AAPI/jdk,$AAPI/libs/test,$AAPI/libs/log \
  --analysis-steps prep,modification --analysis-results-dir /tmp/ec_out"
```
(If `dogfood/cst-impl/build/inputConfiguration.json` is missing, regenerate per `dogfood/README.md`.)
Check:
- `grep "EC contraction" <run log>` — retraction count should **drop** from 12 (toward 0; 0 needs Part A too).
- `grep -E "^type .*cst\.api\.info\.(Info|MethodInfo|FieldInfo|TypeInfo|ParameterInfo) " /tmp/ec_fp.txt` — the
  interfaces should now show `eventual=@Immutable(hc=true)(after="inspection")`. **Verify the mark label is
  `inspection`** (not garbage) — a wrong label is the signature of an over-approximation bug (Caveat 1).
- Re-run once; the set must be **stable** across two runs (per-file JSON is non-deterministic — compare aggregate
  counts and the eventual set only, never a file diff).

### 8.3 Gate-OFF corpus A/B (the golden rule — mandatory before merge)
Fernflower is the fast vehicle (~3–4 min/run). It needs the `test-oss` corpus (sibling checkout) and the `slowTest`
task (tag-gated). Compare the change vs. base with the gate **off**:
```
# B = your branch, gate off (no EVENTUALCLUSTER):
FPDUMP=/tmp/B.txt ./gradlew :maddi-run-openjdk:slowTest \
  --tests "org.e2immu.analyzer.run.openjdkmain.TestFernflower" --rerun-tasks
# A = base commit 66913f72 (git stash your change), same command → /tmp/A.txt
sort /tmp/A.txt > /tmp/A.s ; sort /tmp/B.txt > /tmp/B.s ; diff /tmp/A.s /tmp/B.s | wc -l   # must be 0
```
A 0-line diff is required. If non-zero, the new leniency is leaking off-gate — tighten the `EventualCluster.ENABLED`
guards (Caveat 3).

---

## 9. After Part B: the residuals and Part A

Part B alone will **not** take the retraction to 0. Remaining, in order:

1. **`ParameterInfo` residual.** It has **0** unexcused no-arg holdouts yet stays `@Mutable`/`eventual=null` — a
   different, smaller blocker (likely an arg-taking cross-ref accessor the no-arg scan missed, or a builder
   method). Re-diagnose with the FPDUMP holdout query (§3) after Part B; it may resolve for free once
   `commitLabels` handles arg-side `this.methodInfo` references.
2. **`TypeInfo`'s stream accessors.** `constructorAndMethodStream`, `recursiveSuperTypeStream`, etc. iterate the
   *whole hierarchy* (collections of candidate-typed `TypeInfo`s), not a single field or a simple chain.
   `commitLabels` as specified handles single receiver/arg chains; a stream/lambda that pulls candidate content
   out of `this` and calls methods on the elements needs the same "committable content" reasoning applied to the
   stream's element type. Expect a few of `TypeInfo`'s 19 holdouts to remain after §5; extend `commitLabels` (or
   accept them as a documented residual) guided by the dogfood.
3. **Part A — `InfoImpl` subclass→superclass mark inheritance.** Independent of Part B and also required: `InfoImpl`
   is abstract, holds only the `@IgnoreModifications` analysis store, and has **no mark of its own** (the
   `inspection` field and `@Mark` methods live on each concrete subclass, all marking a field named `inspection`).
   Give an abstract class with no mark of its own, all of whose concrete subclasses carry an eventual verdict with
   a **common mark label** `L`, the inherited verdict `@Immutable(hc, after=L)`. This is the historical
   `approvedPreconditionsFromParent` in its subclass→parent direction (see `docs/eventual-immutability.md` §"Not
   done"). Implementation notes: there is no built-in "who extends me", so scan the analysis order for
   `t.parentClass() == InfoImpl`; it is circular with `immutableSuper` (subclasses lean on `InfoImpl`, `InfoImpl`
   inherits from subclasses), which the existing seed+contraction machinery already resolves — implement it as a
   new optimistic computation *inside* the seeded pass, witnessing its assumption like the others, and let the
   contraction validate. With Part A + Part B both in, the flagships' superclass **and** interface assumptions all
   discharge → contraction retracts **0** → the 17 survive soundly.
4. **Ungate (fixpoint step 3).** Only after the dogfood shows retraction 0 and the corpus A/B is byte-identical
   off-gate: take the cluster result off `EVENTUALCLUSTER` and make it default, behind a fresh byte-identical
   corpus A/B.

---

## 10. File / symbol index (base commit `66913f72`)

- `maddi-modification-analyzer/src/main/java/org/e2immu/analyzer/modification/analyzer/impl/`
  - `TypeEventualAnalyzerImpl.java` — **the file to change.** Key methods: `computeEventuallyNonModifying`
    (~280), `nonModifyingLabels` (~327), `receiverAfterLabels` (~351), `isNonModifyingRead` (~368),
    `fieldHoldsCommittableContent` (~380), `immutableOf`/`eventualOf` (fallbacks for jar types), `referencesThis`
    (~404), `isEventuallyImmutableFieldType(member, fieldType)` (the seed+witness entry), `labelsOfReceiver`
    (~424, **do not touch**).
  - `EventualCluster.java` — the seed + `assumptions()` ledger + `treatAsEventuallyImmutable(member, candidate,
    actual)`. `ENABLED` is non-final (tests flip it).
  - `EventualClusterContraction.java` — step-2 contraction; `membersToRetract` (pure/generic) + `retract`.
  - `TypeImmutableAnalyzerImpl.java` — `immutableSuper(member, superType, afterMark)` (the other seed site);
    `loopOverFieldsAndMethods`; `AfterMark`.
  - `IteratingAnalyzerImpl.java` — `logVerdictFingerprint` (FPDUMP, ~196–261, now emits `eventuallyNonMod`); the
    terminal certification point where the contraction is invoked (~520).
  - `SingleIterationAnalyzerImpl.java` — constructs & shares the `EventualCluster`; `eventualCluster()` accessor.
- Property keys: `EVENTUALLY_IMMUTABLE_TYPE`, `EVENTUALLY_NON_MODIFYING_METHOD`, `EVENTUAL_METHOD`,
  `NON_MODIFYING_METHOD`, `IMMUTABLE_TYPE` — in
  `maddi-cst-analysis/.../analysis/PropertyImpl.java`. Value types in `ValueImpl.java`
  (`EventuallyImmutableImpl`, `SetOfStringsImpl`, `EventualImpl`, `BoolImpl`, `ImmutableImpl`).
- Docs: `docs/eventual-info-hierarchy.md` (running record — update it), `docs/eventual-immutability.md` (stage-2
  design + §"Interfaces" + `approvedPreconditionsFromParent`), `road-to-immutability/llm-summary.md` and
  `road-to-immutability/src/docs/asciidoc/sections/050-immutability.adoc` (§060 eventual immutability, §050
  ignore-modifications).
- The `Info` sources being dog-fooded: `maddi-cst-impl/.../info/{InfoImpl,TypeInfoImpl,MethodInfoImpl,
  FieldInfoImpl,ParameterInfoImpl}.java`, `maddi-cst-impl/.../type/TypeParameterImpl.java`; interfaces in
  `maddi-cst-api/.../info/{Info,TypeInfo,MethodInfo,FieldInfo,ParameterInfo}.java`.

---

## 11. Working style reminders (from `AGENTS.md` / `CLAUDE.md`)

- A green `slowTest`/corpus run can be cached, skipped, vacuous, or heap-starved — force `--rerun-tasks` and read
  the per-test roll-call; verify the FPDUMP was actually written and non-empty.
- Compare dogfood **aggregate counts / the eventual set** only — never a per-file JSON diff (non-deterministic).
- Keep the change gated; the gate-OFF corpus A/B byte-identity is the acceptance gate.
- Commit each landed piece with a clear message; update `docs/eventual-info-hierarchy.md` as you go.
