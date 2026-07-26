# Handoff to maddi: three open defects, one fixed, and a convention request

**From:** the `ws/standardize` thread working on jfocus-standardize's deduplication intake
**Date:** 2026-07-26
**Status:** all found while diagnosing something else. Issue 1b is fixed here. Issue 1 is worked around
downstream. Issue 3 is untouched and is likely the cheapest to close, with a clear test.

⛔ **Issue 2 (`BottomType`) is CLAIMED by another thread (Bart, 2026-07-26). Do not work on it.** It is kept
here for context only. Issues 1 and 3 are unclaimed, and maddi changes are in scope for whoever picks them
up -- they do not need to be handed further along.

---

## Reply from maddi, 2026-07-26 (`ws/python`)

All three reproduced against the code as described; the line numbers, the mechanism and the two-type
reproduction were accurate.

**Issue 1: fixed, both halves.** The reproduction is now
`maddi-modification-prepwork/src/test/.../TestPrepOnePrimaryTypeAtATime` (caller-first fails, callee-first and
batch pass — exactly your ordering claim). The guard now reads a marker `PrepAnalyzer.PREPPED` that `doType`
itself sets, so a merely-reached type is no longer mistaken for a processed one; `TestReprepKeepType`, which
pins the Tier-2 KEEP-carry behaviour the guard exists for, stays green. Your read of the suggested fixes was
right, and so was your instinct to stop: the *other* suggestion (restricting
`ComputePartOfConstructionFinalField.go`) turns out to be the necessary second half, and it is an engine
change.

What that second half was: fixing *who* gets prepped does not fix *what* was computed for the type that was
wrongly stamped. `go()` derived `PART_OF_CONSTRUCTION` and `FINAL_FIELD` for a merely-reached type from the
subset of its methods that happened to be in the graph, and `isAssigned` returns `false` on a body with no
`VariableData` — so an assignment that would knock a **private** field off "effectively final" was silently
missed, `internalGo`'s own guard blocked recomputation later, and the error ran in the unsound direction that
`DynamicImmutabilityInference` gates on.

Bart's call on the fix, and it is the better one: rather than restricting `go()` to the primary types passed
in — a call convention — leave the values **undecided** for any type that has not been through full source
prepping, which is the actual precondition. `go()` now takes a `bodiesAnalyzed` predicate and `PrepAnalyzer`
passes `PREPPED`. Undecided is the safe state at every consumer (an absent `FINAL_FIELD` reads as "not final",
an absent `PART_OF_CONSTRUCTION` as the empty set), so it costs precision, never soundness.

That framing also catches a population neither of us had in scope: **binary** types. A method kept by
`externalsToAccept` has no body at all, so `isAssigned` reads a null `lastStatement()` and every private
non-final library field read "effectively final" on zero evidence — permanently, rather than
order-dependently. I expected that to be the larger population and instrumented `go()` to check; it is not.
Across the whole jfocus-refactor-service suite at most **one** binary type (a `java.util` collection class) is
left undecided per run, and no source types; jfocus-metrics' cluster/cache tests and maddi's own runs leave
none. The merely-reached source case you found is the one that actually bites. What is *not* measured: your
own intake (jfocus-standardize is not checked out in the `ws/python` worktree set), the two disabled
`TestFootPrint*`, and any corpus with heavy third-party jar dependencies — please shout if a number moves on
your side.

One narrower case stays open, recorded in `docs/prep-analyzer hardening.md` §2: the predicate is per primary
type, but the evidence gap is per method — `doNotRecurseIntoAnonymous` leaves lambda/anonymous methods without
`VariableData` inside a type that *is* prepped.

**Note for your side:** with both halves fixed, per-type prepping is now equivalent to the batch call, so your
`ProjectIntake` workaround is no longer load-bearing — keep it or not on its own merits (it is still the faster
shape, one call graph instead of N). `jfocus-stdbase`'s viewer `Processor:105-110` has the same per-type shape
and needs no change either. What you should expect instead: fewer `FINAL_FIELD` values on **library** types,
because they are no longer inferred from bodies that do not exist. That is a precision loss in the sound
direction, and measured here it is tiny (see above) — but your corpora are not the ones I could measure.

Issues 2 and 3: still open, and now written up in full for you in
[`handoff-nulltype-classsymbolscanner.md`](handoff-nulltype-classsymbolscanner.md), since you are taking them
on with access to maddi. The headline from that investigation: `Type$2` is javac's **recovery** type
(confirmed from the JDK sources — `Type.java` declares `noType`/`recoveryType`/`stuckType` in that order, so
the `"none"` string-match at `:1063` catches the first and misses the other two), and the throw at `:1064` is
**unreachable in every maddi and jfocus-refactor-service path measured here** — fernflower corpus included,
0 hits. So the 128 come from something your intake does differently, most plausibly an incomplete classpath,
and finding that is likely worth more than the missing `switch` case. The document carries the probe, the
paths already ruled out by reading, and maddi's non-obvious working rules.

Everything below was measured, not inferred, unless explicitly marked. Where I am guessing, it says so.

---

## 1. `PART_OF_CONSTRUCTION` is stamped on types that were never prepped  (highest impact)

**Symptom.** `LinkComputerImpl$SourceMethodComputer.doStatement` (`:664`) trips its bare `assert vd != null`,
because `VariableDataImpl.of(statement)` returns null for a statement in a *source* type.

**Cause.** `PrepAnalyzer.doType` opens with an idempotency guard (`:182`):

```java
if (typeInfo.analysis().haveAnalyzedValueFor(ComputePartOfConstructionFinalField.PART_OF_CONSTRUCTION)) return;
```

but `ComputePartOfConstructionFinalField.go(G<Info> cg)` (`:60`) groups **every method vertex in the call
graph** by `MethodInfo::primaryType` and sets the marker on each (`:90`) — including primary types that were
merely *reached* through the graph, never passed to `doType`.

So a type can be marked "already processed" without ever having been processed. Any subsequent
`doPrimaryType` on it returns immediately, and its statements keep no `VariableData` — while remaining fully
reachable, so the link computer descends into them and asserts.

**It is order-dependent**, which is what makes it nasty: prepping types one at a time works if callees happen
to sort first and fails if callers do.

**Reproduction.** Two source primary types in one parse, prepped one at a time with a *fresh* `PrepAnalyzer`
each (`doPrimaryType(DD)`, then `doPrimaryType(F)`), then analysed:

```java
public class DD {
    public int from = 0;
    @Override public String toString() {
        String sep = proj.F.sep();
        return "from: " + from + sep;
    }
}
public class F {
    public static String sep() {
        String s = System.lineSeparator();
        return s;
    }
}
```

`doPrimaryType(DD)` preps `DD`, builds a call graph that reaches `proj.F.sep()`, and stamps `F`. The later
`doPrimaryType(F)` hits the guard and returns. Probing at the phase boundary, *after phase 1 had completed
for both types*:

```
XXORDER  proj.DD infos=[proj.DD.<init>(), proj.F, proj.F.sep(), proj.DD.toString(), proj.DD.from, proj.DD]
XXPHASE1 proj.DD.toString() stmt0 hasVD=true
XXPHASE1 proj.F.sep()      stmt0 hasVD=false      <-- never prepped, but stamped
```

Rename so the callee sorts first (`A` calling nothing, `Z` calling `A`) and it passes. Each type alone
passes. A single batch `doPrimaryTypes(Set.of(DD, F))` also passes, because `doPrimaryTypes` runs `doType`
over the whole set *before* building the graph and stamping.

**Workaround applied downstream**, not a fix here: jfocus-standardize's `ProjectIntake` now does one batch
`doPrimaryTypes(allTypes)` instead of a call per type. Measured effect on the fernflower corpus:
**166 → 36 analysis failures**, 20 → 150 types analysed; guava went to 0.

**Suggested fix, for you to judge.** The guard needs a marker that means "this type was prepped", which
`PART_OF_CONSTRUCTION` does not — it means "this type appeared in a call graph". Either stamp a separate
per-type prepped-marker inside `doType`, or restrict `ComputePartOfConstructionFinalField.go` to the primary
types actually passed in. I did not attempt either: the marker is `INTRINSIC` tier and the guard's comment
describes a Tier-2 incremental-reparse contract I do not know well enough to touch.

Related, already in `docs/prep-analyzer hardening.md` §2 but **not the same item**: "method-less primary types
never get `PART_OF_CONSTRUCTION` computed" is the opposite direction (types with no method vertices are
skipped by `go`), and "early return assumes part-of-construction and final-field are set together" is about
`internalGo`'s own guard at `:84-87`. This one is about the *`PrepAnalyzer`* guard keyed on that marker.

---

## 1b. FIXED HERE, listed for context: `EvalInstanceOf` folded `instanceof` to false for `?`

Not a request -- this one is fixed in this repo (`c272ece0`), and is recorded because it is the most
damaging of the set and shows the shape the others may share.

`EvalInstanceOf`, having established the tested value is a `VariableExpression`, concluded `constantFalse`
when the value's type was not assignable to the test type. An **unbounded wildcard** has neither a
`typeInfo` nor a `typeParameter`, so nothing is assignable from it and every test against it folded to
false. For a `List<?>` parameter, `list.get(i) instanceof String` became statically false and everything it
guarded was eliminated as dead code: a six-statement method reduced to one operation. The same method
taking `List<Object>` or a raw `List` was unaffected.

It hides well: `EvalInstanceOf` returns early when a pattern variable is present, so the folding branch is
only reached once the pattern has been split into a separate boolean, i.e. downstream of parsing.

maddi 2814/0/56 after the fix; jfocus-standardize and jfocus-stdbase unchanged.

---

## 2. `ClassSymbolScanner.convert` has no case for javac's `BottomType` — ⛔ CLAIMED, another thread

`convert(Type, Set<Type>)` falls through to `throw new UnsupportedOperationException("NYI")` at `:1064`.
Instrumenting that line while parsing **unmodified upstream fernflower** (199 files, the raw parse, no
transformation of ours involved) gives:

```
128  type=[<nulltype>]  class=com.sun.tools.javac.code.Type$BottomType  tag=BOT  isErroneous=false  tsym=<nulltype>
```

That is javac's type for the `null` literal, on clean third-party source. maddi already has the CST
counterpart — `ParameterizedTypeImpl.NULL_CONSTANT`, handed out by `FactoryImpl.parameterizedTypeNullConstant()`
— so a `Type.BottomType` case returning that looks like the natural fix.

**What I did not establish:** where those 128 throws are caught. The raw parse reported `parse errors: false`,
so they are swallowed or downgraded somewhere I did not trace, and I have **not** quantified what is lost as a
result. It may be nothing. But 128 swallowed `UnsupportedOperationException`s on clean input seemed worth your
attention rather than mine.

**One caution if you do add the case.** A second, different type also reaches that same line: javac's
*recovery* type (`com.sun.tools.javac.code.Type$2`, tag `NONE`, `tsym == null`), which appears when
**resolution failed**. In our runs that one was genuine — it came from invalid source we generated — and it
should probably produce a resolution diagnostic naming the unresolved symbol, not be quietly converted. The
two cases want different treatment; today they are indistinguishable at the throw site.

---

## 3. `ImportComputer` never emits static imports, so printed source does not re-parse

`ImportComputerImpl` never calls `setIsStatic`; it carries a literal `// IMPROVE static fields and methods`
at `:130`. The CST is not the problem — `ImportStatement.isStatic()` exists and the parse retains it — the
computer just does not produce them.

That makes `print2(cu, null, importComputer)` **not round-trip-safe** for any compilation unit relying on
static imports: the references print unqualified, no static import is emitted, and the result does not
re-parse. Measured on fernflower: 11 of 199 source files use static imports (`StructMethod` alone has 74),
and **zero** of the 199 printed files have any. It is a direct cause of unresolved symbols such as
`Type opc_iload not found` and `Type TYPE_OBJECT not found`, and it accounts for 5 of the 9 compilation
units still lost in our intake.

Either fix works for us: emit the static imports, or fully qualify the references at print time. The first
preserves the source shape, which matters more to a deduplicator than to a compiler.

This one is bounded and testable — print any type that uses a static import and re-parse the result — so it
is probably the cheapest of the three to close.

---

## 4. Request: make the two meanings of `NYI` distinguishable

`UnsupportedOperationException("NYI")` is used both for "not implemented yet" and for "this state should be
unreachable". Those call for opposite responses — implement the missing case, versus find out why the input is
malformed.

This cost real time here. Seeing `NYI` at `ClassSymbolScanner:1064`, I concluded the openjdk inspector had a
feature gap and reported it as a parser defect. It was not: the trigger was invalid Java that *our* loop
transform had emitted, and the marker was the "unreachable" sense. Bart caught it — his prior was that a
parser which has digested a 3M-line corpus including elasticsearch is unlikely to be the broken party, and he
was right.

A distinct message would settle it at the throw site: `"NYI"` for a genuine gap versus something like
`"unreachable: unexpected " + type.getClass()` for the invariant, ideally including the offending value. The
throw at `:1064` is a good candidate — it currently carries no context at all, which is why identifying the
type required patching maddi and re-running.

---

## Reproducing any of this

Issues 1 and 2 both surface through jfocus-standardize's `TestFernflowerDedup` / `TestIntakeAttrition`
(deduplication module), which need the `fernflower` OSS corpus. Issue 1 has the self-contained two-type
reproduction above and needs no corpus. All the numbers quoted here come from those runs on 2026-07-26.
