# Handoff: `<nulltype>` at `ClassSymbolScanner:1064`, and the `NYI` message problem

> ## ✅ RESOLVED, 2026-07-26 (`ws/std2`) — read this box before the document
>
> Both issues are fixed. The document below is kept as written, because its §3 is the reason the answer was
> found; but **three of its readings do not survive measurement**, and §6's prescription inverts. Full write-up,
> with the numbers: [`maddi-java-openjdk/notes/nulltype-from-unresolved-annotation-enum-constant.md`](../maddi-java-openjdk/notes/nulltype-from-unresolved-annotation-enum-constant.md).
>
> **What it actually was.** Not the type of `null`, and not a parser gap. javac's
> `ClassReader.AnnotationDeproxy.visitEnumAttributeProxy`
> substitutes `syms.botType` for an **enum constant in an annotation on a class file that it cannot resolve**,
> and reports it as a *warning* — which `MaddiDiagnosticCollector` drops, keeping only ERRORs. That is why the
> parse reported `parse errors: false`. The path is `loadAnnotations → annotationExpression →
> annotationValue(Attribute.Enum) → getOrLoadField → ensureField → convert(vs.type)`, and the swallow site is
> `annotationValue`'s blanket catch at `:567`.
>
> **Where they came from (Q1).** `@org.apiguardian.api.API(status = Status.X)` on junit's own class files. The
> jfocus-standardize fixture put junit-jupiter-api / junit-platform-commons on the analysis class path but not
> apiguardian-api (junit declares it `compileOnly`, so it is not transitive). The corpus was irrelevant: the
> annotations come from the **preloads**, so every test in that module reproduced it. 838 throws over
> `TestIntakeAttrition`'s two tests; all five `API.Status` constants.
>
> **Was the class path incomplete (Q2)?** Yes — §3's instinct and Bart's prior were right. But the evidence is
> not `Type$2`: an incomplete class path produces `ErrorType`s and javac ERROR diagnostics, and `recoveryType`
> did not appear in any scenario measured here.
>
> **What was lost (Q3).** One annotation key/value pair per occurrence, on binary types. The annotation itself
> survived. Bounded, and now reported.
>
> **The fix (Q4).** Recognise the marker in `annotationValue` and skip the value deliberately, with one warning
> per constant naming the missing library. **`convert` deliberately did _not_ get the `BOT` case §6 proposes**:
> `ensureField` would then add a fabricated field, typed "type of the null constant", to a real binary type. All
> 23 bare `NYI` throws in the module now name the value they met. Root cause fixed too, in
> jfocus-transform's `CommonTest`: 838 → 0.
>
> **Corrections to this document:** §2a (a failure marker here, not a real type), §4 (annotation values are *not*
> ruled out — `Attribute.Enum` reaches `convert`; `:567` *is* the swallow site), §5 (javac infers `Object`, not
> the null type, for `id(null)` — checked against the JDK 26 compiler). §3, §7 and §9 were exactly right and are
> what made this cheap.

**To:** the `ws/standardize` thread, who reported this and is getting write access to maddi
**From:** the `ws/python` thread
**Date:** 2026-07-26
**Covers:** issues 2 and 3 of `handoff-from-jfocus-standardize.md`. Issue 1 is **fixed** — see
[§8](#8-what-already-happened-to-issue-1) — this document is only about the other two.
**Status:** investigated, not fixed. Everything below was measured or read out of source, and where I could
not establish something it says so explicitly. One of your findings I could **not** reproduce; that is
[§3](#3-what-i-could-not-reproduce-read-this-before-planning), and it is the most important section.

---

## 1. The site

`maddi-java-openjdk/src/main/java/org/e2immu/language/java/openjdk/ClassSymbolScanner.java`,
`convert(Type, Set<Type>)` declared at `:975` (public single-argument entry at `:971`). It is a chain of
`instanceof` branches ending at:

```java
:1063   if ("none".equals(type.toString()) || type instanceof Type.PackageType) return null; // parent of Object
:1064   throw new UnsupportedOperationException("NYI");
```

Two more bare `NYI` throws live in the same file at `:983` (an `IntersectionClassType` with no
`supertype_field`) and `:1205`. They are not this issue, but they have the same message problem.

Note what `:1063` already is: a **string match against one javac type's `toString()`**. That is the shape of
the whole problem, and §2 explains why it is the wrong member of its family to have singled out.

## 2. The two types that arrive there are unrelated to each other

Verified against the JDK sources shipped on the dev machine
(`$(/usr/libexec/java_home)/lib/src.zip`, openjdk 26.0.1 — adjust the path for your JDK):

### 2a. `Type.BottomType` — the type of `null`. A real type.

`jdk.compiler/com/sun/tools/javac/code/Type.java:2284`:

```java
static class BottomType extends Type implements NullType {
    public BottomType() { super(null, List.nil()); }
    @Override public TypeTag getTag() { return BOT; }
    @Override public TypeKind getKind() { return TypeKind.NULL; }
    ...
}
```

`jdk.compiler/com/sun/tools/javac/code/Symtab.java:105` creates the singleton `botType`, and `:502` does
`initType(botType, "<nulltype>")` — which is exactly the `tsym=<nulltype>` your probe printed. **Your
observation was right.** It reaches `:1064` because it is not a `ClassType`, `ArrayType`, `TypeVar`,
`WildcardType` or anything else the chain tests, and its `toString()` is `<nulltype>`, not `none`.

maddi already has the CST counterpart: `ParameterizedTypeImpl.NULL_CONSTANT`
(`maddi-cst-impl/.../type/ParameterizedTypeImpl.java:42`), handed out by
`FactoryImpl.parameterizedTypeNullConstant()` (`:1143`) and recognised by identity in
`ParameterizedTypeImpl.isTypeOfNullConstant()` (`:475`).

### 2b. `Type$2` — javac's **recovery** type. Not a type at all; a failure marker.

Your identification is correct, and here is the proof rather than the inference. `Type.java` declares three
anonymous `JCNoType` subclasses, in this order:

| line | field | `toString()` | compiles to |
|---|---|---|---|
| `:94` | `noType` | `"none"` | `Type$1` |
| `:103` | `recoveryType` | `"recovery"` | `Type$2` |
| `:110` | `stuckType` | `"stuck"` | `Type$3` |

`recoveryType`'s own javadoc: *"special type to be used during recovery of deferred expressions"*. So
`Type$2` means **attribution failed**, and `:1063`'s guard catches its sibling `Type$1` by string-matching
`"none"` while `Type$2` and `Type$3` fall through into a message that says "not implemented yet".

This is the strongest argument for issue 3 in the whole codebase: one line, two meanings, and the existing
guard already demonstrates the confusion.

## 3. What I could **not** reproduce. Read this before planning

I instrumented `:1064` to print the javac class, tag, `tsym` and a nine-frame `org.e2immu` origin stack
(deduplicated, counted), confirmed the probe was live — it fires on the first `convert` call — and ran it
over everything reachable from the `ws/python` worktree set:

| run | what it parses | NYI hits |
|---|---|---|
| `:maddi-run-openjdk:slowTest --tests '*TestFernflower*'` | the **same** fernflower corpus, 227 types | **0** |
| `:maddi-java-openjdk:test` + `:maddi-inspection-openjdk:test` | the whole openjdk front-end suite | **0** |
| jfocus-refactor-service, full `test` task | 1212 tests, incl. generated/transformed source | **0** |

**The throw is unreachable in every path maddi itself exercises.** That is also why it has survived with no
context attached to the message — nothing in the project's own testing has ever hit it.

Your 128 are not in doubt; they are simply not reachable from here. I could not run
`TestFernflowerDedup` / `TestIntakeAttrition` because **jfocus-standardize is not checked out in the
`ws/python` worktree set**. So the population is specific to your intake pipeline or its configuration, not
to "parsing unmodified fernflower" as such — which is a meaningful narrowing, because it means the trigger is
something your run does differently, and finding *that* is probably more informative than the missing
`switch` case.

The hypothesis I would test first: **an incomplete classpath**. It would produce `recoveryType` (`Type$2`)
directly, and degraded inference is also the most plausible way for a type variable to resolve to
`<nulltype>` — which would explain both populations at one line in one run, and neither in ours. You noted
the recovery type in your own run came from invalid generated source; same mechanism, different trigger.

## 4. Paths ruled out by reading — do not re-walk these

- **Annotation values.** A null-typed `Attribute.Constant` reaches `annotationConstant` (`:572`), whose
  `switch` on `c.type.getTag()` has no `BOT` case and falls to `default -> null`. It never calls `convert`.
  So the blanket `catch (RuntimeException re) → return null` in `annotationValue` (`:567`) is **not** the
  swallow site, nor are the annotation catches at `:491`, `:517`, `:522`.
- **The `null` literal in source.** `ScanCompilationUnit:2283` handles it directly —
  `case BOT -> runtime.newNullConstant(comments, source)` — without asking `convert` for a type.

## 5. Where it must come from, then

`ScanCompilationUnit` has **21** `convertType.convert(...)` call sites. Most pass a *declared* type; the
interesting ones pass a resolved **expression** type, which is the only way a `<nulltype>` can appear:

```
:2000  binary.type          :2401  mr.type                :2665/:2673  newArray.type
:2101  anyPattern.type      :2412  returnType             :2743        newTypeExpression.type
:2154  lambda.target        :2456  fieldAccess.type       :2800        newClass.clazz.type
:2162  sd.instantiatedType().restype                      :2560/:2570  methodInvocation.type
```

`methodInvocation.type` is the candidate I would look at first: a generic method whose type variable javac
infers to the null type. But this is reasoning, not measurement — the origin stack from §3's probe will tell
you in one run, which is why the probe is reproduced verbatim in §7.

## 6. The fix, and why it is not two lines

The obvious case is:

```java
if (type.hasTag(TypeTag.BOT)) return runtime.parameterizedTypeNullConstant();
```

The caveat: `NULL_CONSTANT` is a singleton `ParameterizedTypeImpl` with **no `typeInfo` and no
`typeParameter`**. Eight call sites dereference the result of `convert` immediately:

```
ClassSymbolScanner:233, :926, :943, :1452, :1504     convert(...).typeInfo()
ScanCompilationUnit:438, :2396                       .typeInfo()
ScanCompilationUnit:2299                             .bestTypeInfo()
```

At any of those, the case would convert a loud `UnsupportedOperationException` into an NPE or a failed assert
somewhere less obvious. None of them *can* legitimately see a `<nulltype>` — they convert class symbols and
declared types — so in practice the case is safe, but "return the null constant" is only *correct* for the
expression-type callers in §5. If you want belt and braces, add the case and assert at those eight sites, or
route the expression-type callers through a variant that tolerates it.

**Treat the recovery/stuck types oppositely.** They mean resolution failed; converting them silently would
bury the real diagnostic. They want a message naming the unresolved symbol. That is where issue 3 pays for
itself:

```java
throw new UnsupportedOperationException("unexpected " + type.getClass().getName()
        + " tag=" + type.getTag() + " tsym=" + type.tsym);
```

I would land that context line **regardless of the rest**, because identifying the type currently costs a
patched build and a rerun — which is precisely the expense you reported, and the reason the issue arrived as
"maybe a parser gap" instead of a named type.

## 7. The probe, so you can rerun it in one step

Paste into `ClassSymbolScanner`, replacing the throw at `:1064`. It deduplicates by origin stack, so a corpus
run prints one line per distinct call path rather than 128 lines.

```java
if ("none".equals(type.toString()) || type instanceof Type.PackageType) return null;
XXPROBE(type);
throw new UnsupportedOperationException("NYI");
}

private static final java.util.Map<String, Integer> XXSEEN = new java.util.concurrent.ConcurrentHashMap<>();

private static void XXPROBE(Type type) {
    StringBuilder sb = new StringBuilder();
    sb.append("class=").append(type.getClass().getName())
            .append(" tag=").append(type.getTag())
            .append(" tsym=").append(type.tsym == null ? "null" : type.tsym.toString())
            .append(" | ");
    StackTraceElement[] st = new Throwable().getStackTrace();
    int n = 0;
    for (int i = 1; i < st.length && n < 9; i++) {
        String cn = st[i].getClassName();
        if (cn.startsWith("org.e2immu")) {
            sb.append(cn.substring(cn.lastIndexOf('.') + 1)).append('.')
                    .append(st[i].getMethodName()).append(':').append(st[i].getLineNumber()).append(" <- ");
            n++;
        }
    }
    String key = sb.toString();
    int c = XXSEEN.merge(key, 1, Integer::sum);
    if (c == 1) System.out.println("XXNYI " + key);
    if (c % 50 == 0) System.out.println("XXNYI-COUNT " + c + " :: " + key.substring(0, Math.min(90, key.length())));
}
```

Two traps that cost me time:

- **Use `System.out`, not `System.err`.** In `maddi-run-openjdk`'s test tasks the XML's `system-err` came
  back empty while `system-out` held 224 KB. I nearly concluded "no hits" from a capture artifact.
- **Verify the probe is live before believing a zero.** Add a one-shot print on the first `convert` call. A
  silent probe and an unreachable throw look identical.

Read the results out of the JUnit XML rather than the console:

```bash
python3 -c "
import xml.etree.ElementTree as ET, glob
for f in glob.glob('*/build/test-results/*/TEST-*.xml'):
    r = ET.parse(f).getroot()
    for tag in ('system-out', 'system-err'):
        e = r.find(tag)
        if e is None or not e.text: continue
        for l in e.text.splitlines():
            if 'XXNYI' in l: print(r.get('name').split('.')[-1], '|', l[:300])
"
```

## 8. What already happened to issue 1

Fixed in both halves, on `ws/python` — merge before you start so you are not looking at the old code.

- `4c84de8a` — `PrepAnalyzer.doType`'s idempotency guard now reads a real `PREPPED` marker that `doType`
  sets on itself, not `PART_OF_CONSTRUCTION`. Your diagnosis and your two-type reproduction were both exact;
  the reproduction is now `TestPrepOnePrimaryTypeAtATime`.
- `8a224ab2` — the second half, which your report did not reach and which was the more interesting one:
  fixing *who* gets prepped does not fix *what* was computed for the type that was wrongly stamped.
  `ComputePartOfConstructionFinalField.go` now takes a `bodiesAnalyzed` predicate and leaves
  `PART_OF_CONSTRUCTION` / `FINAL_FIELD` **undecided** for any type whose bodies were not analysed, instead
  of deciding them on `isAssigned`'s inability to distinguish "no assignment" from "no body".

**One consequence for your side:** `ProjectIntake`'s batch workaround is no longer load-bearing — per-type
prepping is now equivalent to the batch call. Keep it or not on its own merits (it is still one call graph
instead of N). What you *should* expect is marginally fewer `FINAL_FIELD` values on **library** types, since
they are no longer inferred from bodies that do not exist. Measured here that is tiny — at most one binary
type per run across jfocus-refactor-service's whole suite — but your corpora are not the ones I could
measure. If a jfocus-standardize number moves, that is the first thing to check.

## 9. Working in maddi: the ground rules that are not obvious

Read `AGENTS.md` (§Commands, §Facts not to re-derive, §Working style) and `CLAUDE.md` first. The ones that
bite hardest:

- **A green corpus run is not evidence.** `AGENTS.md` §Commands lists four ways a `slowTest` can be green
  without running anything: served from cache, corpus absent and assumed away, analysed zero types, or
  heap-starved. Force it (`--rerun-tasks`) and read the **per-test roll-call**, not the build outcome. I hit
  the caching one during issue 1 and had to redo a run.
- **Corpora resolve through locators, never hardcoded paths.** `CloneBenchCorpus` (`TESTARCHIVE_ROOT` /
  `-Dtestarchive.root`) for clone-bench; `test-oss` via `-Dtest.oss.root`. maddi is upstream OSS, so it does
  **not** use jfocus's `CodeLaserCorpus` / `OssCorpus` — keep maddi's corpus handling self-contained.
  Unlike the jfocus repos, maddi's build **does** forward these `-D` properties to the test worker
  (`maddi-modification-prepwork/build.gradle.kts:69`, `maddi-run-openjdk/build.gradle.kts:97`).
- **`TestShadowCloneBench` is currently red, and it is not you.** 855 divergences / 263 reverse against a
  2026-07-19 baseline. `testarchive` merged its `analyzed` branch into main on 2026-07-26 and carries ~190
  uncommitted modifications. The test's own comment says a change in these numbers means re-baseline **and
  reclassify**, not bump. Verify against a stash of your own changes before believing you caused it.
- **Engine changes need a byte-identical FPDUMP A/B** on the proving-ground corpora before acceptance
  (`AGENTS.md` §Working style). Adding a `BottomType` case is arguably not an engine change; changing what
  happens on a recovery type is closer to one.
- **Do not rename `org.e2immu.*` packages** or stray `e2immu` occurrences — that migration is coordinated by
  the maintainer.
- Analyzer properties are **write-once**; absence means *undecided*, a first-class state, not "false".
  `GuardAnalyzerImpl`'s phrase for the trap is worth internalising: *"a default is not a decision"*.

## 10. Open questions, in the order I would answer them

1. **Where do the 128 come from?** Run §7's probe on `TestFernflowerDedup` / `TestIntakeAttrition`. The origin
   stack answers it in one run and turns this from a code-reading exercise into a fact.
2. **Is the classpath complete in that run?** If `Type$2` (recovery) and `<nulltype>` appear together, suspect
   the configuration before the parser — that was Bart's prior on the original report, and it was right.
3. **Where are they swallowed, and what is lost?** Still unanswered, and §4 removes the obvious candidates.
   The raw parse reported `parse errors: false`, so something downgrades them; until that is found, the
   damage is genuinely unquantified. It may be nothing.
4. **Only then, the fix.** `BOT` case per §6, recovery/stuck kept as a diagnostic, plus the context on the
   throw — which is worth landing first and separately, since it makes questions 1–3 cheaper for whoever
   hits this next.
