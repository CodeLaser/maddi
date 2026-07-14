# Bug report: `CodecImpl.decodeConstructor` throws AIOOBE on an empty `sortedConstructors()`

## RESOLUTION (2026-07-13, kotlin trunk — maddi-cst-io owner)

Fixed in `CodecImpl` by combining suggestions 1 and 2. A single helper
`reinspectIfNeeded(typeInfo, capturedSortedList, index, derive)` now backs the constructor, method **and** field
decode paths:

- if the captured `TypeAndSorted` list already satisfies the index, it is used unchanged (no behaviour change);
- otherwise the referenced type is force-inspected via `runtime.getFullyQualified(fqn, false, sourceSet)` — which
  routes to `CompiledTypesManager.getOrLoad`, loading the members under the openjdk inspector — and the list is
  re-derived from the now-inspected type (same FQN-sorted order the encoder used, so the index stays valid);
- a final bounds check then throws a **contextual `DecoderException`** (type + index + count) instead of a bare
  AIOOBE. The force-load is wrapped so a runtime without on-demand loading (e.g. a synthetic test runtime) degrades
  to that clear error rather than throwing from the loader.

This also fixes the **latent twin**: `decodeMethodInfo` had the same unguarded `.get(index)` (only `decodeFieldInfo`
guarded, and even it did not force-load).

Tests: added `TestCodecInfoInContext.testConstructor` (the previously-missing `C` round-trip, encodes `"C<init>(0)"`)
and `testConstructorOutOfRange` (asserts the clear `DecoderException`). `:maddi-cst-io:test` and
`:maddi-modification-analyzer:test` (which exercises the `LoadAnalysisResults` decode path) are green.

**Caveat for the reporter:** the exact shallow-load trigger (`transform.jar` via the openjdk inspector) is not
reproducible inside maddi-kotlin, so the *force-load* branch is verified by construction (`getFullyQualified` →
`getOrLoad` loads members) rather than end-to-end here. **Please confirm `transform.jar` now decodes on the
`jfocus-stdbase` openjdk branch** and remove the bridge in `codelaser-transform-common` `CommonTest`. Suggestion 3
(encoder emitting `C` for a constructor-less type) was not observed and is not addressed; if it exists it would now
surface as a clear `DecoderException` rather than an AIOOBE.

### FOLLOW-UP FIX on the inspector side (2026-07-13, kotlin trunk)

Root cause confirmed and fixed upstream of the codec. `ClassSymbolScanner.addMemberToType` skipped **all** private
methods -- including constructors -- when loading a compiled type on demand, so a static-utility class with only a
private no-arg constructor (`ArrayUtil`, `org.slf4j.MDC`, ...) ended up with `constructors()` empty. Fix: load
constructors even when private (`ms.isConstructor()`). Verified with `TestPrivateConstructor` (slf4j MDC /
LoggerFactory / Util: 0 constructors before, 1 after) -- commit `49883ac1`.

Caveat on scope: this only recovers constructors of **regular classpath JARs**, whose classfiles carry private
members so javac exposes them. **JDK platform types are still not recoverable**: their javac symbols come from the
stripped `ct.sym`, which contains no private members at all (verified: `java.lang.Math`'s private constructor is
absent from `getAllMembers`, `getEnclosedElements` and the internal member scope even after `complete()`). If an
analyzed-package file ever references a JDK type's private constructor, that needs a different mechanism (read the
real module classfile via ASM, or synthesize). `ArrayUtil` is a regular JAR, so this fix should unblock it; please
re-confirm transform.jar now decodes.

(Unrelated: the maddi-modification-analyzer suite shows pre-existing intermittent flakiness -- a javac
`StarImportScope` NPE / CompilationProblems under parallel test execution -- which occurs with and without this
change, with a different failing set each run. Not caused by this fix.)

### CONFIRMATION #2 from the reporter (2026-07-13, after inspector fix 49883ac1)

**The `ArrayUtil` private-constructor fix works** — that error is gone; the decode now advances past `ArrayUtil`
(and processes `Loop.*`). But `transform.jar` **still doesn't fully decode**; it now hits a *different, later*
failure — a **nested-type resolution** during **linked-variable** decoding:

```
java.lang.AssertionError: Cannot find io.codelaser.jfocus.transform.support.Try.TryData
    at org.e2immu.language.cst.io.CodecImpl.decodeSimpleType(CodecImpl.java:382)
    at org.e2immu.analyzer.modification.link.io.LinkCodec$C.decodeSimpleType(LinkCodec.java:289)
    at org.e2immu.language.cst.io.CodecImpl.decodeType(CodecImpl.java:424)
    at ...decodeInfoOutOfContext ... CodecImpl.decodeVariable(CodecImpl.java:500)
    at org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.decodeLink(MethodLinkedVariablesImpl.java:89)
    at ...MethodLinkedVariablesImpl.lambda$decodeLinks$0
```

So `decodeSimpleType` (CodecImpl:382) looks up the **nested** type `Try.TryData` by name and asserts it is found,
but it is not (`Try` is a transform-support class with a nested `TryData` interface, like `Loop.LoopData`). This is
a separate issue from the constructor one: nested-type resolution by name during decode of a method's linked
variables. Likely candidates: the type not (yet) inspected/registered under its nested name, or a `Try.TryData`
vs `Try$TryData` naming mismatch in `decodeSimpleType`.

Non-fatal but related warnings seen just before: `@GetSet: Cannot find field set in Loop.LoopDataImpl.Builder`
and `... field iterator in Loop.LoopDataImpl.Builder` (nested-member resolution).

Net: real progress (constructor path fixed); `transform.jar` still aborts, now at `Try.TryData`, so the bridge
stays and the 23 stdbase tests remain blocked. Same deterministic repro (any Loop/transform-support test's
`beforeEach`). The confirmation below is now **superseded** by this one (it described the pre-49883ac1 state).

### FOLLOW-UP FIX #2 on the inspector side — nested types by dotted FQN (2026-07-13, commit `a6306c30`)

Root cause of the `Try.TryData` failure found and fixed. `ScanCompilationUnits.loadCompiledTypeOrNull` rejected
**any type whose owner is not a package** (`// primary (top-level) types only`), so
`getOrLoad("java.util.Map.Entry")` / `getOrLoad("...Try.TryData")` returned `null` even though javac's
`getTypeElement` *does* resolve the nested symbol. Nested types are loaded as part of their enclosing type, so
the fix: for a nested type, walk up to the top-level enclosing class, load that (which registers all nested
types via `addEnclosedTypeToType`), then return the requested one by its dotted FQN. Verified with
`TestNestedTypeLoad` (`java.util.Map.Entry` and the doubly-nested `java.util.AbstractMap.SimpleEntry` now
resolve; the `$` binary form stays `null`, which is fine -- decode uses dotted names and `getTypeElement` does
not take `$` names). Openjdk suites green.

This is the exact capability `CodecImpl.decodeSimpleType` needs: `findType` → `typeProvider.get("...Try.TryData")`
→ `getFullyQualified` → `getOrLoad`. As before it can't be reproduced end-to-end inside maddi-kotlin, so:
**please re-confirm `transform.jar` on the stdbase openjdk branch** -- the `Try.TryData` assertion should now be
gone (watch for any further nested-member gaps behind it, e.g. the `@GetSet: Cannot find field set/iterator in
Loop.LoopDataImpl.Builder` warnings, which are nested-member resolution and may or may not be fatal).

### CONFIRMATION #3 from the reporter (2026-07-13, after nested-type fix a6306c30)

**The `Try.TryData` resolution fix works** — that `Cannot find` assertion is gone; the decode advances further.
Next offender (again one step deeper), now a **nested type's fields**:

```
org.e2immu.language.cst.api.analysis.Codec$DecoderException:
field index 1 out of range; io.codelaser.jfocus.transform.support.Try.TryDataImpl.Builder has 0 field(s)
```

So the (triple-nested) `Try.TryDataImpl.Builder` now **resolves** as a type, but is loaded with **0 fields** — the
field analog of the original constructor gap, on a nested type. This lines up with the persistent non-fatal
warnings `@GetSet: Cannot find field set / iterator in Loop.LoopDataImpl.Builder`: nested `...Impl.Builder`
classes are registered but their fields (and by extension @GetSet targets) are not populated. Likely the
nested-type load path (`addEnclosedTypeToType`) registers the enclosed type shell without loading its members, so
`decodeFieldInfo`'s `reinspectIfNeeded` finds a type whose `fields()` is still empty.

Net: two fixes down, decode is much further along but `transform.jar` still aborts, now at
`Try.TryDataImpl.Builder` fields. Bridge stays; 23 stdbase tests still blocked. Same deterministic repro. This is
the frontier — please populate nested-type members (fields, and re-check methods) on load.

### DIAGNOSIS #3 — root cause found, but the one-line fix has cross-cutting blast radius (2026-07-14, kotlin trunk)

**Root cause of `Try.TryDataImpl.Builder has 0 fields` (and it is NOT nested-specific).**
`ClassSymbolScanner.addMemberToType` skips **all private fields** (`if (isNotPrivate ...)`, `:552`) — exactly the
gate the private-**constructor** fix (`49883ac1`) had to relax for constructors. So any compiled type loaded on
demand from **bytecode** has `fields()` limited to its non-private members. A builder's fields are private, so
`Try.TryDataImpl.Builder` loads with 0 fields, and `decodeFieldInfo`'s force-load then indexes past the end →
"field index 1 out of range". (The nested-ness in Confirmation #3 was incidental.)

The deeper asymmetry: **source analysis includes private fields; compiled (bytecode) loading excludes them.** The
`transform.jar` analyzed-package was encoded from source (private fields present, so index 1 exists); decode goes
through the bytecode loader (private excluded) → shorter list → mismatch.

**Why the obvious one-line fix is not landable as-is.** Relaxing the gate to load private fields (excluding only
compiler-synthetics: `load = !isPrivate || !synthetic`) *does* fix the bytecode side — verified on the regular-JAR
class `org.slf4j.helpers.BasicMarker`, `fields()` **0 → 6** (`name`, `referenceList` present; no `this$0`
synthetics). **But it breaks maddi's own committed analysis-hints golden files** (`ANALYZED_RESULTS`): those were
encoded under the *exclude-private* policy, so making the decoder include private fields shifts every field index →
`CodecImpl.decodeFieldInfo` throws in `LoadAnalysisResults`, failing **129/130** modification-analyzer tests in
`beforeEach`. So the change was **reverted** on the kotlin trunk pending a coordinated decision.

**What the real fix requires (decision needed):**
1. **Regenerate** all committed analyzed-package golden files under an include-private policy (maddi's
   `ANALYZED_RESULTS` *and* the stdbase side), so encoder and decoder agree; **or**
2. **Make the codec field-reference scheme policy-insensitive** — reference a field by a stable key
   (name + descriptor) instead of by positional index into a loader-dependent `fields()` list — which also needs a
   format bump + regeneration but is robust to the private/synthetic policy going forward.

Same **ct.sym caveat** either way: JDK platform types carry no private members at all, so a JDK type's private
field stays unrecoverable; `transform.jar` classes are regular JARs, so they *can* be made consistent. Net:
`transform.jar` still blocked; the fix is a format/regeneration change, not a drop-in loader tweak.

### CONFIRMATION from the reporter (2026-07-13, jfocus-stdbase openjdk branch) — SUPERSEDED by #2 above

Rebuilt against the fix. **`transform.jar` still does NOT decode** — but the failure is now the clear, contextual
error (good), which pinpoints the real culprit:

```
org.e2immu.language.cst.api.analysis.Codec$DecoderException:
constructor index 0 out of range; io.codelaser.jfocus.transform.support.ArrayUtil has 0 constructor(s)
```

So the `C0` reference is **legitimate, not a suggestion-3 encoder bug**: `ArrayUtil` really has a constructor —

```java
public class ArrayUtil {
    private ArrayUtil() { }          // the one (private) constructor -> index 0
    public static Iterable<Boolean> iterable(boolean[] array) { ... }
    ...
}
```

The problem is that **after the force-load, `ArrayUtil.constructors()` is still empty**. So `getFullyQualified` →
`getOrLoad` loaded the type but **did not populate its constructor** (note it is `private` — the shape here is a
static-utility class whose *only* member of interest is a private no-arg constructor). The force-load branch
therefore doesn't rescue this case; the real gap is upstream of the codec, in the **openjdk inspector's on-demand
member loading not recording (private?) constructors** for a compiled classpath type.

Net: the guard/DecoderException improvement is confirmed working and useful; but transform.jar still aborts at the
first type (`ArrayUtil`), so no transform-support type is analyzed yet, and **the bridge in
`codelaser-transform-common` `CommonTest` and in stdbase's `CommonTest` must stay for now**. Next step is on the
inspector side: ensure `getOrLoad`/force-inspection populates constructors (incl. private) — or, if constructors
are intentionally lazy, have the codec's `reinspectIfNeeded` trigger the specific load that fills
`typeInfo.constructors()` before indexing.

Repro is deterministic: on the stdbase openjdk branch, any Loop/transform-support test logs the above at
`CommonTest.beforeEach` (the `LoadAnalysisResults(...).go(...)` bridge). `ArrayUtil` is the first offender; there
may be more behind it once its constructor loads.

---


**Reported:** 2026-07-13, from the `openjdk` branch of `jfocus-stdbase`, while loading analyzed-package files
against the openjdk inspector. **For:** the thread that owns `maddi-cst-io`. This is an
analyzed-package **decode** failure: any jar whose content decodes a constructor reference into a type whose
constructors are not (yet) inspected aborts the whole jar's load.

## Summary

`CodecImpl.decodeConstructor` indexes `TypeAndSorted.sortedConstructors()` directly, with no bounds check.
When that list is empty (the referenced type has no constructors loaded at decode time), `.get(0)` throws

```
java.lang.ArrayIndexOutOfBoundsException: Index 0 out of bounds for length 0
```

The sibling `decodeFieldInfo` guards this case; `decodeConstructor` does not.

## Stack (verified)

```
java.lang.ArrayIndexOutOfBoundsException: Index 0 out of bounds for length 0
    at org.e2immu.language.cst.io.CodecImpl.decodeConstructor(CodecImpl.java:141)
    at org.e2immu.language.cst.io.CodecImpl.decodeInfoInContext(CodecImpl.java:197)   // case 'C'
    at org.e2immu.language.cst.io.CodecImpl.decodeInfoInContext(CodecImpl.java:188)
    at org.e2immu.language.cst.impl.analysis.ValueImpl$SetOfInfoImpl.lambda$from$0(ValueImpl.java:999)
    at org.e2immu.language.cst.impl.analysis.ValueImpl$SetOfInfoImpl.from(ValueImpl.java:1000)
    at org.e2immu.language.cst.impl.analysis.ValueImpl.lambda$static$23(ValueImpl.java:1011)
    at org.e2immu.language.cst.io.CodecImpl.lambda$decode$0(CodecImpl.java:120)
    at org.e2immu.language.cst.io.CodecImpl.decode(CodecImpl.java:122)
```

So: while decoding a **`SetOfInfo`-valued analysis property** (`ValueImpl$SetOfInfoImpl.from` →
`decodeInfoInContext` per element), one element is a **constructor reference** (`case 'C'`), and
`decodeConstructor` blows up because `context.currentType()`'s `sortedConstructors()` is empty.

## Where it breaks

`CodecImpl.java:139`:
```java
private MethodInfo decodeConstructor(TypeAndSorted typeAndSorted, String fqnNameIndex) {
    Matcher m = NAME_INDEX_PATTERN.matcher(fqnNameIndex);
    if (m.matches()) {
        int index = Integer.parseInt(m.group(2));
        MethodInfo methodInfo = typeAndSorted.sortedConstructors().get(index);   // <-- line 141, AIOOBE
        assert methodInfo.isConstructor();
        return methodInfo;
    } else {
        throw new UnsupportedOperationException();
    }
}
```

Compare `decodeFieldInfo` (`CodecImpl.java:160`), which **does** guard the index:
```java
if (index >= typeAndSorted.sortedFields().size()) {
    throw new UnsupportedOperationException("Index " + index + " greater than the number of fields in " + typeAndSorted);
}
```

`TypeAndSorted.sortedConstructors()` (`Codec.java:115`) is derived purely from the live type:
```java
typeInfo.constructors().stream().sorted(Comparator.comparing(MethodInfo::fullyQualifiedName)).toList()
```
so it is empty whenever `typeInfo.constructors()` is empty.

## Root cause (analysis)

The decode assumes the referenced type's constructors are populated, but under the **openjdk inspector's lazy
loading** they are not. The type carrying the referenced constructor is on the classpath and *preloaded*
(shallow), but its members/constructors have not been inspected at the moment the `SetOfInfo` element is
decoded — so `typeInfo.constructors()` is empty even though, at **encode** time, the type was fully inspected
and `TypeAndSorted.constructorIndex(...)` produced a valid index. That is an encode/decode-time asymmetry:

- **encode**: `context.currentType()` fully inspected → `sortedConstructors` non-empty → emits `C<index>`.
- **decode**: same type shallow-loaded → `sortedConstructors` empty → `.get(index)` → AIOOBE.

(A secondary possibility to rule out: the encoder emitting a `C` reference for a type that legitimately has no
constructors — e.g. an interface — in which case the fix belongs on the encode side.)

## Reproduction

On the `openjdk` branch of `jfocus-stdbase`, any test whose `CommonTest.beforeEach` runs
`new LoadAnalysisResults(runtime, sources).go(codec, JFocusTransform.ANALYZED_RESULTS)` with the openjdk
`JavaInspectorImpl`:

```
Loaded 167 primary types from .../analyzedPackageFiles/openjdk.jar   OK
Loaded   6 primary types from .../analyzedPackageFiles/libs.jar       OK
ERROR ...processing .../codelaser-transform-support.jar!/.../transform.jar
  -> java.lang.ArrayIndexOutOfBoundsException: Index 0 out of bounds for length 0   (stack above)
```

`openjdk.jar` and `libs.jar` decode cleanly; **`transform.jar`** (the analyzed packages for
`io.codelaser.jfocus.transform.support.*`) trips it. jfocus-transform's own
`codelaser-transform-common/.../CommonTest` already documents and **bridges** this exact failure ("the maddi
analyzed-package decoder currently throws in `CodecImpl.decodeConstructor`, `sortedConstructors()` empty for a
referenced constructor"), so it is not stdbase-specific.

To narrow it inside maddi: decode `transform.jar` alone via `LoadAnalysisResults`, or find the
`SetOfInfo`-valued property (decoded through `ValueImpl$SetOfInfoImpl.from`, registered at `ValueImpl.java:1011`)
that stores a **constructor** among its `Info` elements, and the transform-support type whose constructors are
empty at decode time.

## Impact

A single such reference aborts the **entire jar's** decode, so every type in `transform.jar` is left
unanalyzed. In stdbase that means transform-support types (e.g. `Loop.LoopData`) have no `FINAL_FIELD` /
modification data, so e.g. the `final` field behind `@GetSet("variables")` is treated as non-final and the
standardizer emits a spurious statement-time suffix (`variables$0`). It currently blocks ~23 parse/balance
tests on the stdbase openjdk branch; jfocus-transform tolerates it via a bridge.

## Suggested fixes

1. **Defensive (cheap):** give `decodeConstructor` the same bounds/emptiness guard as `decodeFieldInfo`, so it
   throws a clear, contextual error (type FQN + index + `sortedConstructors().size()`) instead of a bare AIOOBE.
   This does not fix the load, but makes every future occurrence diagnosable.
2. **Real fix (decode symmetry):** before resolving a `C`/`M`/`F`/`P` reference in `decodeInfoInContext`, force
   the `context.currentType()` to be fully inspected (load its constructors/methods/fields), mirroring whatever
   guarantees inspection on the encode side — so `TypeAndSorted` is built over the same member set that was
   encoded. Equivalent index bases on both sides are the invariant being violated.
3. **Rule out an encode-side bug:** confirm the encoder never emits a `C<index>` reference for a type that has
   no constructors (interfaces, etc.); if it can, fix the encoder or the value's element representation.

## Notes

- Found while porting the jfocus-stdbase parser test harness to the openjdk inspector; the standardizer's
  statement-time suffix depends on correct field finality, which depends on this decode succeeding.
- `libs.jar` (6 types) and `openjdk.jar` (167 types) decode fine, so the trigger is specific to content that
  puts a constructor into a `SetOfInfo` property — narrowing which property is the fastest route to the type.
