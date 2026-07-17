# Bug report: `RuntimeImpl.getSetVariable` NPEs on an indexed `List` element-getter

**Reported:** 2026-07-16, from the `openjdk` branch of `jfocus-stdbase`, while re-baselining the standardizer test
suite after the GetSet `list` change.
**For:** the thread that owns `maddi-cst-impl` / the GetSet analysis.
**Context:** this surfaced *together with* the new `boolean list` component on `ValueImpl.GetSetValueImpl`
(indexed element-getters now print `list=true`, whole-collection getters `list=false`). The crash is in the same
list-getter feature; it was previously masked in the stdbase test by an earlier (expected) `toString` mismatch.

## Summary

`RuntimeImpl.getSetVariable` (`:327`), resolving a call to an **indexed `List` element-getter** to the variable
it denotes (`field[index]`), assumes `java.util.List.get(int)` itself carries a non-null GetSet **field** and
dereferences it unconditionally:

```java
// RuntimeImpl.getSetVariable, the getSetFieldType.arrays()==0 branch (:345-360)
TypeInfo list = getFullyQualified(List.class, true);
MethodInfo get = list.findUniqueMethod("get", 1);
Value.FieldValue fv = Objects.requireNonNull(get.getSetField());
...
FieldInfo field = Objects.requireNonNull(fv.field(), "Called on wrong method");   // <-- :359 NPE
```

For the input below, `java.util.List.get(1).getSetField().field()` is **null**, so the `requireNonNull` at
`:359` throws `NullPointerException: Called on wrong method`. The outer `getSetField` (the user's getter) is a
perfectly valid indexed list-getter — `field()` non-null, `list=true`, `parameterIndexOfIndex=0` — so the early
`return null` guard at `:329` does not fire; execution reaches the JDK-`List.get` lookup and dies there.

## Why it matters (observed crash)

`jfocus-stdbase`'s parser resolves getter/setter calls to the variables they denote
(`ExpressionVisitor.getterSetter:954` → `runtime.getterVariable(methodCall)`), so any method that **calls** an
indexed list element-getter crashes at parse time:

```
java.lang.NullPointerException: Called on wrong method
    at org.e2immu.language.cst.impl.runtime.RuntimeImpl.getSetVariable(RuntimeImpl.java:359)
    at org.e2immu.language.cst.impl.runtime.RuntimeImpl.getterVariable(RuntimeImpl.java:314)
    at io.codelaser.jfocus.standardize.analyzer.parse.ExpressionVisitor.getterSetter(ExpressionVisitor.java:954)
    at io.codelaser.jfocus.standardize.analyzer.parse.ExpressionVisitor.continueMethodCall(ExpressionVisitor.java:885)
    at io.codelaser.jfocus.standardize.analyzer.parse.ExpressionVisitor.handleMethodCall(ExpressionVisitor.java:878)
    ...
```

## Repro

A `List<Integer>` field with an indexed element-getter (`get(int)`), *called* from another method:

```java
import java.util.List;
import java.util.ArrayList;
class X {
    private final List<Integer> myList = new ArrayList<>();

    public Integer get(int i) { return myList.get(i); }   // indexed element-getter: list=true, paramIndexOfIndex=0

    public boolean method(int pos) {
        return myList.get(pos).equals(get(pos));           // calling get(pos) -> getterVariable -> :359 NPE
    }
}
```

- `X.get(int)` is analysed correctly as `GetSetValueImpl[field=X.myList, setter=false, parameterIndexOfIndex=0, list=true]`.
- The crash is only about resolving a **call** to it to a variable; the analysis of the getter itself is fine.

## Analysis

The intended result (per the consuming test's expectation) is the element access
`this.myList._synthetic_list$0[pos]` — i.e. the synthetic `_synthetic_list` element field of the `List`, indexed.
The code obtains that synthetic field from `java.util.List.get(1)`'s own GetSet (`fv.field()`), but in this run
that field is null. So either:

- `java.util.List.get(int)` is expected to carry a synthetic-list GetSet field here and it is not being set up
  (an analysis/setup gap for the JDK `List.get`), **or**
- `getSetVariable` should not depend on `List.get`'s GetSet at `:359` and should build/obtain the
  `_synthetic_list` element field another way (and degrade gracefully — `return null` — rather than
  `requireNonNull`-crash — when it is genuinely absent).

The `"Called on wrong method"` message suggests the author expected `List.get` to always be a getter with a
field here; the observed null means that assumption does not hold in this configuration. You own the
synthetic-list GetSet machinery, so I have not guessed at the fix — flagging the exact null and path.

## stdbase impact

Blocks `parse.TestGetSet.test1` (the dedicated list-getter test; expects `myList._synthetic_list$0[pos]` at
`STOP_AFTER_PARSING`). No stdbase-side workaround is appropriate — `getterVariable` is maddi's to resolve. Any
other stdbase code parsing calls to indexed `List`/collection element-getters would hit the same crash; today it
is the one test because that shape is otherwise rare in the corpus. Re-baselining `TestGetSet` is on hold until
this is resolved: it also needs two clean, unrelated re-baselines once it parses again — the `GetSetValueImpl`
`toString` now carries `, list=true`/`, list=false`, and an indexed getter argument prints as `get(pos)` rather
than `this.get(pos)`.

## RESOLUTION (2026-07-16, kotlin trunk — GetSet analysis owner)

Root cause confirmed: a refactor (`a54eb89c`, "removed … the concept of synthetic list variables") dropped the
hard-coded `java.util.List.get(int)`/`set(int,Object)` watcher from `GetSetUtil` (now
`CreateSyntheticFieldsForGetSet`). That watcher was the only thing that created `_synthetic_list` on the real
`java.util.List` at inspection time, because the JDK bytecode/openjdk sources carry no physical `@GetSet` (it
lives only in the AAPI archive). Without it, `List.get`'s `GET_SET_FIELD` was empty and `RuntimeImpl.getSetVariable`
NPE'd resolving a user's indexed list-getter (field type `List<T>`, `arrays()==0`), which needs
`java.util.List._synthetic_list` to build the element reference.

Fix: restored the watcher in `CreateSyntheticFieldsForGetSet.createSyntheticFields`, keyed on the exact interface
FQNs (`LIST_GET`/`LIST_SET`), run pre-commit in every inspection backend (bytecode/openjdk/parser). Matched the
exact `java.util.List` FQN, NOT `isList()` (override-inclusive), so the field lives once on the interface and is
NOT duplicated onto `ArrayList`/`Vector` — a variable typed `List` standardizes, one typed `ArrayList` does not,
which is the pre-existing intent (see prepwork `TestGetSet.test5b`, "NO synthetics for ArrayList!!!").

Gate: the behaviour is controlled by `JavaInspector.ParseOptions.syntheticListField` (default **true** =
synthetic-arrays direction). Off → the leaner model, no `_synthetic_list`. Wired through the openjdk backend
(the path stdbase uses); the Kotlin front-end inherits it because it shares the bytecode-authoritative
`CompiledTypesManager` load of `java.util.List`.

Tests (all pass, run on a quiet host with `--no-daemon`):
- openjdk `type/TestListSyntheticGetSet`: `List.get`/`set` carry `_synthetic_list` (gate on); gate off suppresses it.
- prepwork `variable/TestGetSet2.test1getterVariable`: end-to-end `getterVariable(get(pos))` resolves to
  `list._synthetic_list[pos]` instead of NPE'ing.
- prepwork `variable/TestGetSet` test1/test5/test6: updated from the removal-era assertions (the `assertThrows(NPE)`
  and the synthetic-free variable sets) to the restored behaviour.
- inspection-kotlin `TestKotlinSyntheticListField`: Kotlin's `List<Int>` resolves to the same shared
  `java.util.List` carrying `_synthetic_list`.

stdbase can re-baseline `TestGetSet` now: `myList._synthetic_list[pos]` resolves at `STOP_AFTER_PARSING`.
NB: the resolved element reference is `field._synthetic_list[index]` (no `$0` suffix — the field is named exactly
`_synthetic_list`).
