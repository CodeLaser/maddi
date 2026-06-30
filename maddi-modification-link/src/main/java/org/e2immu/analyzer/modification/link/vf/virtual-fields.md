# Virtual fields

*Reference for the `org.e2immu.analyzer.modification.link.vf` package.*

## 1. The problem they solve

Type-linking and modification-linking need to reason about two properties of a value's type:

1. its **hidden content** — the generic payload it carries (the things you can get *out* of it: the `E` of a
   `List<E>`, the `K`/`V` of a `Map<K,V>`), and
2. its **mutability** — whether the object can be modified, so that a modification can be propagated to everything
   linked to it.

For types whose source we analyse, we have the real fields and can reason directly. For **interfaces** and for
**JDK / shallow-analysed classes** we do *not* parse the body, so there are no fields to reason about. *Virtual
fields* fill that gap: they are ordinary `FieldInfo` objects, **created but never attached to a type**, that stand
in for the hidden content and the mutability of such a type.

A virtual field's name always starts with the marker character **`§`** (`VF_CHAR`). The marker **`$`**
(`VF_CONCRETE`) is used inside names for *concrete* (non-type-parameter) payload.

## 2. The two virtual fields per type

`compute(...)` returns a `VirtualFields(mutable, hiddenContent)` record. Either component may be `null`.

| field | meaning | type |
|-------|---------|------|
| `mutable` (`§m`) | "this object is mutable / can be modified". Propagation reads as *if I am modified, you are modified*. | `java.util.concurrent.atomic.AtomicBoolean` (chosen because it is itself modifiable; the boolean value is irrelevant) |
| `hiddenContent` | the generic payload, with a **multiplicity** (see §3) encoded as array dimensions | depends on the type parameters |

`VirtualFields.toString()` renders as `<mutable> - <hiddenContent>`, using `/` for a `null` component, e.g.
`§m - E[] §es`, or `/ - String §1`, or `/ - /` (= `VirtualFields.NONE`).

### When is `§m` present?

- type has array dimensions, or any of its parameters is mutable → yes (`makeMutable`);
- otherwise it depends on the type's `IMMUTABLE_TYPE` analysis: mutable → yes, immutable → no.

Because shallow JDK types usually have **no immutability analysis loaded**, they default to *mutable* and therefore
get a `§m`. Several tests note this ("the §m is there because we have not done the @Final analysis").

## 3. Multiplicity

Multiplicity is *how deeply nested* the hidden content is, encoded as **array dimensions** on the hidden-content
field and as repeated `s` in its name:

- `Optional<E>` never nests its payload → multiplicity 1 → field `§e` of type `E` (0 extra arrays beyond the level).
- `List<E>` can yield many `E`s → multiplicity 2 → field `§es` of type `E[]`.
- `List<List<E>>` → `§ess` of type `E[][]`.

It is computed as `extraMultiplicity = arrays(pt) + maxMultiplicityFromMethods(typeInfo) - 1`:

- `maxMultiplicityFromMethods` scans the type's constructors and methods and takes the **maximum** multiplicity of
  any return type. `Iterable`/`Iterator` are hard-coded to 2 (see `multi2`); method-level type parameters are
  ignored; `wrapped(...)` unwraps `Iterable`/`Iterator`-assignable types one level.
- `Comparable<X>` returns 0 (it never hands its payload back) → no hidden-content field at all.

## 4. Naming scheme

| situation | hidden-content field | notes |
|-----------|----------------------|-------|
| one type parameter `E`, level 0 | `§e` : `E` | temporary, base of the recursion |
| one type parameter `E`, level *n*≥1 | `§e` + `"s"`×n : `E[]…` | e.g. `List<E>` → `§es` |
| concrete payload (e.g. `String`) | `§$` + `"s"`×n | e.g. `§$s` for an array-of-String level |
| multiple type parameters | a synthetic **container type** `§…` with one field per parameter, then `§…s` | see below |
| bare type parameter array `E[]` | `§es`, plus `§mE` mutable | the mutable is named `§m`+TP to disambiguate |

**Container types (multiple type parameters).** For `Map<K,V>` a synthetic class `§KV` is created
(`makeContainerType`) with fields `§k : K` and `§v : V`; the hidden-content field is `§kvs : §KV[]`. The container's
name is `§` + the uppercased component names; each field is `§`+lowercased-tp-name (`$` for concrete). Containers are
synthetic, public, parented to `Object`, and **not** attached to any package.

## 5. The formal→concrete translation map

`compute(pt, addTranslation=true)` additionally returns a `VirtualFieldTranslationMap` (`formalToConcrete`). It maps
the **formal** type parameters of the type *and of every super-type that shares the same type parameters*
(`hiddenContentHierarchy`) to the **concrete** payload at the use-site. Example, `Optional<String>`:

```
T=TP#0 in Optional [] --> String
```

and `Map<StringBuilder, E>` (where `E` is `List`'s parameter):

```
K=TP#0 in Map [] --> StringBuilder
V=TP#1 in Map [] --> E=TP#0 in List []
```

This map (`VirtualFieldTranslationMapImpl`, a `TranslationMap`) is what rewrites shallow/formal links
(`§e`, `§kvs`, …) into the concrete links seen at a call site, recreating the container fields where needed. The
related `VariableTranslationMap` re-creates virtual fields when their owner/scope changes (virtual fields carry their
owner recursively, so the owner must be kept consistent with the scope's type).

## 6. Key types

| type | role |
|------|------|
| `VirtualFieldComputer` | the engine: `compute(pt, addTranslation) → VfTm(virtualFields, formalToConcrete)` |
| `VirtualFields` | the `(mutable, hiddenContent)` record (also a `Value`, see caveats) |
| `VirtualFieldTranslationMap` (prepwork iface) / `VirtualFieldTranslationMapImpl` | formal→concrete translation |
| `VariableTranslationMap` | re-creates virtual fields with changed owner/scope |
| `Util.virtual / needsVirtual / hasVirtualFields / primary / isContainerType / isVirtualModification` | predicates/helpers used throughout linking |

## 7. Worked examples (all from the test-suite)

| input | `VirtualFields` |
|-------|-----------------|
| `Object` | `/ - Object §0` |
| `String`, `Integer` | `/ - String §n` (immutable, no `§m`) |
| `StringBuilder` | `§m - StringBuilder §n` (mutable) |
| `Comparable<X>` | `/ - /` |
| a utility class (`Math`) | `/ - /` |
| `List<E>` / `ArrayList` / `Deque` | `§m - E[] §es` |
| `Optional<E>` | `/ - T §t` |
| `Optional<String>` | `/ - String §0` |
| `Optional<StringBuilder>` | `§m - StringBuilder §0` (immutable HC outer, but concrete HC is mutable) |
| `Stream<T>` | `§m - T[] §ts` |
| `TreeMap` / `Map<K,V>` | `§m - §KV[] §kvs` |
| `Map<StringBuilder,E>` | `§m - §$E[] §$es` |
| `E[]` (bare TP array) | `§mT - T[] §ts` |
| `interface Three<A,B,C>` | `§m - §ABC §abc`, container `§ABC{§a:A, §b:B, §c:C}` |

## 8. Inconsistencies / open issues (flagged 2026-06)

These were found while documenting; they are covered by tests in `TestVirtualFieldComputer3` (some as
`@Disabled` reminders) so they resurface if touched.

1. **Class-comment vs implementation for ≥3 type parameters.** The header comment of `VirtualFieldComputer`
   describes *pairwise-combination* containers (`TSV(T t, S s, V v, TS ts, SV sv, TV tv)`). The implementation
   (`multipleTypeParameters`) builds a **flat** container with one field per parameter only: `Three<A,B,C>` →
   `§ABC{§a, §b, §c}`. The comment should be corrected (or the feature implemented).

2. **`addModificationFieldEquivalence` looks like a copy-paste bug — but is load-bearing.** It computes
   `immutableFrom = typeImmutable(to.parameterizedType())` (reads `to`, not `from`). Changing it to `from` makes
   the modification equivalence depend on `min(from, to)` rather than `to` alone, which *changes* the link output
   and breaks `TestCast`, `TestInstanceOf` and a `TestList` case. So it is **not** a safe one-line fix: it needs a
   decision on the intended modification semantics, and the affected expected-values updated. Left as-is with an
   in-code note. (`VirtualFieldComputer`, the `M2` method.)

3. **Two overlapping "does this need virtual fields?" predicates.** `Util.needsVirtual(ParameterizedType)`
   excludes **all** functional interfaces (`isFunctionalInterface()`), whereas `compute(...)` only short-circuits
   the `java.util.function` *package* by name. So `Runnable`, `Callable`, and user `@FunctionalInterface` types
   outside that package *do* get virtual fields from `compute` (e.g. `Runnable` → `§m - Runnable §n`) while
   `needsVirtual` says they do not. Decide on one notion of "functional interface → no fields".

4. **Dead code — REMOVED (2026-06).** `VirtualFieldComputer.arrayType(ParameterizedType)` was never called (its
   logic is duplicated inline in the `typeParameter(...)` `arrays>0` branch). Deleted.

5. **Vestigial `Value`/`Property` machinery.** `VirtualFields` implements `Value` and declares
   `VIRTUAL_FIELDS = new PropertyImpl(...)`, but the property is never read/written on any `analysis()` and
   `VirtualFields.encode(...)` throws `UnsupportedOperationException`. Virtual fields are recomputed on demand, so
   the `Value` interface appears unused — either wire it up (cache on the type) or drop it.
