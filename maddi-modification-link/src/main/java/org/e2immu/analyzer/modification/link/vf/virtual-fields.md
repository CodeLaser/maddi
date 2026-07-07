# Virtual fields

*Reference for the `org.e2immu.analyzer.modification.link.vf` package.*

> **Terminology** — this uses the vocabulary of *road-to-immutability* (`road-to-immutability/src/docs/asciidoc`),
> which is the authoritative source. A quick recap, because it is easy to get wrong:
> - **accessible content** of a type = the objects in its fields' object-graph whose type is actually *accessed*
>   (a method other than an `Object` method is called, or a field is read). **hidden content** = the objects of
>   *hidden* (unbound type parameter) or *transparent* type — never accessed. *A type is not responsible for
>   modifications to its hidden content.*
> - **modification** (`@Modified`/`@NotModified`) is about *assignments somewhere in the object graph*; it is the
>   property that propagates along links.
> - **dependence/independence** is a **separate axis** from immutability: a parameter/return is *dependent* when it
>   is linked to the **accessible** content, *independent* (`@Independent`, or `@Independent(hc=true)`) when it is at
>   most linked to the **hidden** content.
> - **immutable** (`@Immutable`) is a *type-lattice* property (final fields + all fields `@NotModified` + fields
>   private-or-immutable + independence), **not** "has no setters". So in this document *"mutable"* means *"not
>   `@Immutable`"* (its accessible content can be modified), not "has a modifying method". `hc=true` marks a type
>   that carries hidden content.

## 1. The problem they solve

Type-linking and modification-linking need to reason about two things about a value's type:

1. its **hidden content** — the part of the object graph of *hidden* type (the `E` of a `List<E>`, the `K`/`V` of a
   `Map<K,V>`): what can be linked *independently* and what modifications get *propagated into* (rather than being
   the type's own responsibility), and
2. whether its **accessible content can be modified**, so that a modification can be propagated to everything linked
   to it (the modification axis, *not* a statement about the type being `@Immutable`).

For types whose source we analyse, we have the real fields and can reason directly. For **interfaces** and for
**JDK / shallow-analysed classes** we do *not* parse the body, so there are no fields to reason about. *Virtual
fields* fill that gap: they are ordinary `FieldInfo` objects, **created but never attached to a type**, that stand
in for the hidden content and for the modifiability of the accessible content of such a type.

A virtual field's name always starts with the marker character **`§`** (`VF_CHAR`). The marker **`$`**
(`VF_CONCRETE`) is used inside names for *concrete* (non-type-parameter) payload.

## 2. The two virtual fields per type

`compute(...)` returns a `VirtualFields(mutable, hiddenContent)` record. Either component may be `null`.

| field | meaning | type |
|-------|---------|------|
| `mutable` (`§m`) | a **modification marker for the accessible content**: this object's accessible content can be modified, so a modification propagates along links — *if I am modified, you are modified*. (It is **not** a claim that the type "is mutable" in any deeper sense; it tracks the `@Modified` axis.) | `java.util.concurrent.atomic.AtomicBoolean` (chosen because it is itself modifiable; the boolean value is irrelevant) |
| `hiddenContent` | the **hidden content** (the type parameters' payload), with a **multiplicity** (see §3) encoded as array dimensions; this is what *independent* (`hc=true`) linking and modification-propagation-into-hidden-content attach to | depends on the type parameters |

`VirtualFields.toString()` renders as `<mutable> - <hiddenContent>`, using `/` for a `null` component, e.g.
`§m - E[] §es`, or `/ - Object §0`, or `/ - /` (= `VirtualFields.NONE`).

### When is `§m` present?

- type has array dimensions, or any of its parameters has a mutable accessible content → yes (`makeMutable`);
- otherwise it depends on the type's `IMMUTABLE_TYPE` analysis: not `@Immutable` → yes, `@Immutable` → no.

Note this is about the *accessible* content: an `@Immutable` type with **mutable concrete hidden content** still gets
a `§m` — e.g. `Optional<StringBuilder>` is `§m - StringBuilder §0`, because the `StringBuilder` reachable through it
can be modified, even though `Optional` itself is immutable.

Because shallow JDK types usually have **no immutability analysis loaded**, they default to *not `@Immutable`* and
therefore get a `§m`. Several tests note this ("the §m is there because we have not done the @Final analysis").

### When is `hiddenContent` present?

A **deeply `@Immutable` type** (`hc=false` — `Long`, `String`, `Integer`) is a *leaf*: it carries **no hidden
content** at all → `/ - /` (`typeInfoWithoutTypeParametersAndArrays` gates on `immutable.isImmutable()`). An
`@Immutable(hc=true)` type (an immutable *container*, e.g. `Object`) and mutable/unanalysed types **do** have hidden
content.

But a **container or array of a concrete immutable element** still has hidden content — its reachable elements —
even though each element is immutable: `String[]`, `List<String>`, `Stream<String>` → `§$s`, `Optional<String>` →
`§$` (see §4, "concrete payload"). This is deliberate (**Option A**): linking is also consumed by dataflow and
extract-interface tooling that rely on these element links, so the immutable content is carried (modification
analysis simply ignores it). Only the immutable *leaf* itself loses its hidden content, not a container of it.
`List<X>` with an abstract type parameter `X` is unaffected (`X` could be mutable → keeps `§xs`).

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
| concrete payload (e.g. `String`) | `§$` + `"s"`×n | e.g. `§$s` for an array-of-String level. Also covers a deeply-`@Immutable` element whose own type has no hidden content (`singleTypeParameter` carries it as `§$`; `multipleTypeParameters` already used `§$`) — Option A, see §2 |
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
| `VirtualFields` | the `(mutable, hiddenContent)` record |
| `VirtualFieldTranslationMap` (prepwork iface) / `VirtualFieldTranslationMapImpl` | formal→concrete translation |
| `VariableTranslationMap` | re-creates virtual fields with changed owner/scope |
| `Util.virtual / needsVirtual / hasVirtualFields / primary / isContainerType / isVirtualModification` | predicates/helpers used throughout linking |

## 7. Worked examples (all from the test-suite)

| input | `VirtualFields` |
|-------|-----------------|
| `Object` | `/ - Object §0` (`@Immutable(hc=true)`, extensible → keeps HC) |
| `String`, `Integer`, `Long` | `/ - /` (deeply `@Immutable` leaf → **no** hidden content) |
| `StringBuilder` | `§m - StringBuilder §n` (mutable) |
| `Comparable<X>` | `/ - /` |
| a utility class (`Math`) | `/ - /` |
| `List<E>` / `ArrayList` / `Deque` | `§m - E[] §es` |
| `List<String>` / `String[]` / `Stream<String>` | `§m - String[] §$s` (concrete immutable element, Option A) |
| `Optional<E>` | `/ - T §t` |
| `Optional<String>` | `/ - String §$` (concrete immutable element, was `§0`) |
| `Optional<StringBuilder>` | `§m - StringBuilder §0` (immutable HC outer, but concrete HC is mutable) |
| `Stream<T>` | `§m - T[] §ts` |
| `TreeMap` / `Map<K,V>` | `§m - §KV[] §kvs` |
| `Map<StringBuilder,E>` | `§m - §$E[] §$es` |
| `E[]` (bare TP array) | `§mT - T[] §ts` |
| `interface Three<A,B,C>` | `§m - §ABC §abc`, container `§ABC{§a:A, §b:B, §c:C}` |

## 8. Inconsistencies / open issues (flagged 2026-06)

These were found while documenting; they are covered by tests in `TestVirtualFieldComputer3` (some as
`@Disabled` reminders) so they resurface if touched.

1. **Flat vs pairwise-combination container for ≥3 type parameters — a real gap, not just a comment.** The
   implementation (`multipleTypeParameters`) builds a **flat** container with one field per parameter
   (`Three<A,B,C>` → `§ABC{§a, §b, §c}`); the original header comment described *pairwise-combination* containers
   (`TSV{T,S,V,TS,SV,TV}` with `[-1]` slicing). The comment is corrected, but the difference is not cosmetic:
   `TestVirtualFieldTable` (Guava-style `Table<R,C,V>`) shows the flat container handles **singletons** (`get`→`V`
   at `§rcvs[-3]`, `rowKeySet`→`R` at `§rcvs[-1]`, `put` params) and the full triple, but a method returning a
   **proper sub-tuple view** — `row(R)`→`Map<C,V>`, `column(C)`→`Map<R,V>` — gets **no link at all** (treated as
   independent of the table). `ExpandSlice` only reassembles the *full* container (all components), so the missing
   `§cv`/`§rv` components mean these views aren't linked, and a modification to such a live view would not propagate
   back. Only affects types with ≥3 type parameters and sub-tuple-returning methods (no JDK types; Guava-`Table`
   shaped user code). **Decision (2026-06): keep flat.** The gap is accepted and documented; `TestVirtualFieldTable`
   pins the behaviour so it resurfaces if anyone relies on sub-tuple linking. Revisit only if Guava-`Table`-shaped
   types with live sub-tuple views become important.

2. **`addModificationFieldEquivalence` looks like a copy-paste bug — but is load-bearing.** It computes
   `immutableFrom = typeImmutable(to.parameterizedType())` (reads `to`, not `from`). Changing it to `from` makes
   the modification equivalence depend on `min(from, to)` rather than `to` alone, which *changes* the link output
   and breaks `TestCast`, `TestInstanceOf` and a `TestList` case. So it is **not** a safe one-line fix: it needs a
   decision on the intended modification semantics, and the affected expected-values updated. Left as-is with an
   in-code note. (`VirtualFieldComputer`, the `M2` method.)

3. **Two overlapping "does this need virtual fields?" predicates — load-bearing divergence, left in place
   (2026-06).** `Util.needsVirtual(...)` excludes **all** functional interfaces (`isFunctionalInterface()`), while
   `compute(...)` only short-circuits the `java.util.function` *package*. They disagree for every functional
   interface outside `java.util.function` (`TestVirtualFieldComputer3`): e.g. `Callable<V>` -> `compute` gives
   `§m - V §v` but `needsVirtual` says `false`. The road-to-immutability *concept* ("functional interface = abstract
   type; only java.util.function is special") suggests `compute` is the aligned one -- **but the divergence turned
   out load-bearing**: functional-interface values are linked via the SAM/lambda path, not via virtual-field hidden
   content, so `needsVirtual` returning `false` for them is what the modification-propagation path relies on.
   Aligning `needsVirtual` to `compute` keeps the link unit-tests green yet breaks five `TestModificationFunctional`
   cases (propagation through custom functional interfaces such as `ThrowingFunction`). So unifying these needs a
   redesign of how functional-interface values are linked, not a one-line predicate change; left documented, with an
   in-code note on `needsVirtual`.

4. **Dead code — REMOVED (2026-06).** `VirtualFieldComputer.arrayType(ParameterizedType)` was never called (its
   logic is duplicated inline in the `typeParameter(...)` `arrays>0` branch). Deleted.

5. **Vestigial `Value`/`Property` machinery — REMOVED (2026-06).** `VirtualFields` implemented `Value` and declared
   `VIRTUAL_FIELDS = new PropertyImpl(...)`, but the property was never read/written on any `analysis()` and
   `encode(...)` threw `UnsupportedOperationException`. Virtual fields are recomputed on demand, so `VirtualFields`
   is now a plain record (the `Value`/property/encode machinery was dropped).
