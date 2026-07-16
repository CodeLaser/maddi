# Bug report: modification via an inherited interface `default` method is not propagated to the implementation's field

**Reported:** 2026-07-12, from the `openjdk` branch of `maddi-aapi`, during an independent hardening sweep of
`maddi-modification-analyzer`. **For:** the linking thread (`sv-integration`). This is a false negative in
`UNMODIFIED_FIELD` and is rooted in the linking / virtual-field machinery (`link.vf.VirtualFieldComputer`,
`link.impl.LinkComputerImpl`), so it's yours rather than the analyzer's.

## Summary

A class implements an interface. The interface has an abstract accessor and a `default` method that mutates the
accessor's result. The implementation overrides the accessor to return one of its fields. That field is therefore
modifiable through the inherited `default` method — but the analyzer reports the field as **not modified**
(`FieldInfo.isModified() == false`, i.e. `UNMODIFIED_FIELD == true`).

Writing the *identical* method body as a **concrete** method in the implementation is analyzed correctly (field
`isModified() == true`), so the gap is specific to *inherited* `default` methods.

## Minimal reproducer (verified)

```java
package a.b;
import java.util.List;
class X {
    interface Buffer {
        List<String> items();
        default void add(String s) { items().add(s); }   // mutates the result of the abstract accessor
    }
    static class Impl implements Buffer {
        private final List<String> list = new java.util.ArrayList<>();
        public List<String> items() { return list; }      // accessor returns the field
    }
}
```

`Impl.list` is modifiable via the inherited `add()`, yet:

```
Impl.getFieldByName("list", true).isModified()  ==  false      // WRONG (should be true)
```

Control — the same body as a concrete method in `Impl` gives the correct result:

```java
interface Buffer { List<String> items(); void add(String s); }
class Impl implements Buffer {
    private final List<String> list = new java.util.ArrayList<>();
    public List<String> items() { return list; }
    public void add(String s) { items().add(s); }          // concrete -> list.isModified() == true
}
```

## Where it breaks (probed state, after `analyzer.go(...)`)

| Query | Value | Comment |
|---|---|---|
| `Impl.methods()` | `[items]` | the inherited `default add` is **not** among Impl's methods |
| `Impl.methodStream()` has own `add` | `false` | Impl never re-declares/re-analyzes `add` |
| `Impl.items().getSetField().field()` | `a.b.X.Impl.list` | the concrete accessor **is** correctly linked to the field |
| `Buffer.items().getSetField().field()` | `null` | the abstract accessor backs no field in `Buffer` |
| `Buffer.add.isModifying()` | `false` | modifying `items()`'s result isn't recorded as modifying anything |
| `Buffer.add` `METHOD_LINKS` | `[-] --> -` | empty; the "modifies the result of `items()`" fact is discarded |

So the fact *"`add` modifies whatever `items()` returns"* is **never produced**. `Buffer.add` is analyzed once, in
`Buffer`'s context, where `items()` returns an unmodelled `List` that backs no field, so the modification is dropped
at link time. `Impl` then inherits a method recorded as non-modifying, and its field analysis (which only looks at
`Impl`'s own declared methods) sees nothing that touches `list`.

## Suggested fix (two parts, both in the linking / virtual-field layer)

1. **Model the abstract accessor as a virtual field of the abstract type.** `Buffer.items()` is a getter with no
   concrete backing field; treat its result as a virtual field (the `§m` / `VirtualFieldComputer` mechanism) so that
   `Buffer.add`'s `items().add(s)` is recorded as **modifying that virtual field** (rather than being discarded as a
   modification of an unlinked local).

2. **Propagate the virtual-field modification onto each implementation's real field.** `Impl.items()` is already
   linked to `Impl.list` (`getSetField`), so once `Buffer.add` modifies the virtual field, an
   implementation-side step (analogous to the existing phase-6 implementation↔abstract copy) can map that modification
   onto `Impl.list`. Note `Impl` never re-analyzes the inherited `default`, so this has to be driven from the
   virtual-field link, not from re-running the body per implementation (unless you prefer that heavier route).

## Impact / severity

**Narrow.** The type-level results are all correct on this shape: `Impl` still computes to `@FinalFields`,
`@Dependent`, `container=true` — because the accessor `items()` already exposes the mutable field, which forces
`@FinalFields`/dependent independently of the missed field modification. So `@Immutable` / `@Independent` /
`@Container` are unaffected; only the field-level `UNMODIFIED_FIELD` is unsound, for this specific
inherited-`default`-method + overridden-accessor pattern. A consumer that reads `UNMODIFIED_FIELD` directly (get/set,
a refactoring tool, etc.) could be misled.

## Marker test

`maddi-aapi`, `maddi-modification-analyzer`:
`.../analyzer/nolink/TestDefaultMethodModification.java`
- `concreteMethod()` — passing positive control (concrete method, field detected as modified).
- `inheritedDefaultMethod()` — `@Disabled` reproducer asserting `Impl.list.isModified()`; enable once fixed.

The reproducer is small and parser-agnostic; it reproduces directly under either parser.

## Notes

- Found while writing modification-link-independent (`nolink`) tests for the analyzer; not touched from that side.
- Not to be confused with the (correct) behaviour where `count++` on a primitive field leaves the field
  `isModified() == false` while the enclosing type is still `@Mutable` via the assignable field — that one is fine.
