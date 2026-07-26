# Handoff to maddi: two defects and a convention request

**From:** the `ws/standardize` thread working on jfocus-standardize's deduplication intake
**Date:** 2026-07-26
**Status:** all three found while diagnosing something else; none is fixed here. Issue 1 is worked around
downstream, issues 2 and 3 are untouched.

---

## Reply from maddi, 2026-07-26 (`ws/python`)

All three reproduced against the code as described; the line numbers, the mechanism and the two-type
reproduction were accurate.

**Issue 1: half fixed, half opened.** The reproduction is now
`maddi-modification-prepwork/src/test/.../TestPrepOnePrimaryTypeAtATime` (caller-first fails, callee-first and
batch pass — exactly your ordering claim). The guard now reads a marker `PrepAnalyzer.PREPPED` that `doType`
itself sets, so a merely-reached type is no longer mistaken for a processed one; `TestReprepKeepType`, which
pins the Tier-2 KEEP-carry behaviour the guard exists for, stays green. Your read of the suggested fixes was
right, and so was your instinct to stop: the *other* suggestion (restricting
`ComputePartOfConstructionFinalField.go`) turns out to be the necessary second half, and it is an engine
change.

What that second half is: fixing *who* gets prepped does not fix *what* was computed for the type that was
wrongly stamped. `go()` still derives `PART_OF_CONSTRUCTION` and `FINAL_FIELD` for a merely-reached type from
the subset of its methods that happened to be in the graph, and `isAssigned` returns `false` on a body with no
`VariableData` — so an assignment that would knock a **private** field off "effectively final" is silently
missed, `internalGo`'s own guard blocks recomputation later, and the error is in the unsound direction that
`DynamicImmutabilityInference` gates on. Demonstrated by `finalFieldOfAReachedType` in the same test class,
left `@Disabled` with the analysis; tracked in `docs/prep-analyzer hardening.md` §2. It needs the FPDUMP A/B
`AGENTS.md` requires, because it changes what library types get computed under `acceptExternalsButNotJdk()`.

**Note for your side:** your `ProjectIntake` batch workaround is still the right shape and should stay — not
because the crash needs it any more, but because per-type prepping is what triggers the open second half above.
`jfocus-stdbase`'s viewer `Processor:105-110` has the same per-type shape and is a candidate for the same
treatment.

Issues 2 and 3: still open, untouched. On issue 2 I can add one datum — the `Attribute.Constant` path is *not*
where the 128 come from: a null-typed constant reaches `annotationConstant`, whose `switch` on the tag falls to
`default -> null` without ever calling `convert`. So the swallow site is still unidentified.

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

## 2. `ClassSymbolScanner.convert` has no case for javac's `BottomType` (the type of `null`)

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

## 3. Request: make the two meanings of `NYI` distinguishable

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
