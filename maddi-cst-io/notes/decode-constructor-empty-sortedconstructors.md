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
