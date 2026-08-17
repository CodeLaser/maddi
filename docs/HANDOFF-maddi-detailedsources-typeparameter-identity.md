# Handover: maddi `DetailedSources` is identity-keyed → type-parameter usages get lost after analysis

**Audience:** whoever owns maddi (`maddi-cst-impl` / the parser + analysis).
**Goal:** fix the *root cause* in maddi so downstream consumers don't need the identity workaround we added in `jfocus-refactor-service`.
**Status:** currently *worked around* caller-side in the rename module (commit `40ee3366` on branch `fix/rename` of `jfocus-refactor-service`). The maddi bug itself is unfixed.

---

## TL;DR

`DetailedSourcesImpl` (maddi) stores "where is this CST node written in source?" in an **`IdentityHashMap`** — lookups are by object identity (`==`).

maddi's analysis (prepwork/linking, run in parallel on `ForkJoinPool.commonPool`) **re-instantiates `TypeParameter` objects** when it resolves/rewires generic method signatures. The re-instantiated object is semantically the same type parameter (same name, owner, index) but a **different instance** than the one the parser recorded in `DetailedSources`.

Consequence: any consumer that looks up detailed sources for a type parameter it obtained from an *analyzed* method signature gets a **miss** — `details(tp)` returns `List.of()` — even though the token clearly exists in the source. Because the re-instantiation happens on another thread, *whether it has happened yet* is a **race**, so at small scale the symptom is **flaky**; at corpus scale it's deterministic.

We hit this in the rename tool: renaming a class type parameter `T` left its uses in method parameter/return types (`add(T tuple)`, `next(T tuple)`) un-renamed → "cannot find symbol". We worked around it by translating any type parameter back to its canonical declared instance before the lookup. **maddi should not require that.**

---

## The reproducing test (self-contained)

This lives in `jfocus-refactor-service`:
`codelaser-refactor-renamemodule/src/test/java/io/codelaser/jfocus/refactor/plan/renameidentifier/TestRenameTypeParameter.java`
as `testTypeParamUsedInSignatureWithCaller` (distilled from timefold fuzz index #175, `TupleList<T extends Tuple>`).

> It is **green today** because of the caller-side workaround. To see the *maddi* bug, remove the workaround first (see "How to make it fail again" below) — then this test fails ~50% of runs.

### The test

```java
// Renaming a CLASS type parameter T: its uses in method parameter/return types are dropped when the class is
// used by another type. `add(T tuple)` and `next(T tuple)` must become `add(S tuple)` / `next(S tuple)`.
@DisplayName("Renaming class type parameter T: method param/return usages dropped when the class is used elsewhere")
@Test
public void testTypeParamUsedInSignatureWithCaller() throws Exception {
    @Language("java")
    String TUPLE = """
        package a;
        interface Tuple {
            <X> X getStore(int i);
            void setStore(int i, Object v);
        }
        """;
    @Language("java")
    String A = """
        package a;
        class A<T extends Tuple> {
            private T first;

            public void add(T tuple) {
                tuple.setStore(0, first);
                first = tuple;
            }

            public T next(T tuple) {
                return tuple.getStore(1);
            }
        }
        """;
    // The presence of a THIRD type that USES A and calls its generic methods is what triggers the bug:
    // cross-type method resolution re-instantiates A's method-signature type parameters.
    @Language("java")
    String USER = """
        package a;
        class User {
            void use(A<Tuple> list, Tuple t) {
                list.add(t);
                Tuple n = list.next(t);
            }
        }
        """;

    Map<String, String> sourcesByFqn = Map.of("a.Tuple", TUPLE, "a.A", A, "a.User", USER);
    RenameTypeParameter.Result r = makeRenameTypeParameter(sourcesByFqn, "a.A", "T", "S", false);

    @Language("java")
    String expectedA = """
        package a;
        class A<S extends Tuple> {
            private S first;

            public void add(S tuple) {
                tuple.setStore(0, first);
                first = tuple;
            }

            public S next(S tuple) {
                return tuple.getStore(1);
            }
        }
        """;

    test(r, "a.A", A, expectedA);
}
```

### Helpers it uses (same test class + `CommonTest`)

```java
// TestRenameTypeParameter#makeRenameTypeParameter -- parse the sources, look up the type parameter by name, rename it.
private RenameTypeParameter.Result makeRenameTypeParameter(Map<String, String> sourcesByFqn,
                                                           String fqn, String typeParameterName,
                                                           String newName, boolean isMethod) throws Exception {
    Parameter newNameParameter = new ParameterImpl("new name", "new type parameter name",
            ParameterType.TYPE_PARAMETER_NAME, newName);
    TextIndex textIndex = new TextIndexComputer().go(init(sourcesByFqn).primaryTypes());   // init() parses in ONE source set
    RenameTypeParameter renameTypeParameter = new RenameTypeParameter(textIndex);
    TypeInfo typeInfo = /* parseResult */.findType(fqn);
    TypeParameter tp = typeInfo.typeParameters().stream()
            .filter(t -> typeParameterName.equals(t.simpleName())).findFirst().orElseThrow();
    return renameTypeParameter.compute(tp, newNameParameter);
}

// CommonTest#test(RenameTypeParameter.Result, typeFqn, source, expected) -- apply the edit actions to `source`
// and assert the result equals `expected`. A missed usage shows up here as a literal mismatch (`T` vs `S`).
```

(The exact `makeRenameTypeParameter` in the repo re-uses a single `ParseResult`; the snippet above is trimmed for clarity — read the real method for the precise setup.)

### How to run

```
cd jfocus-refactor-service
./gradlew :codelaser-refactor-renamemodule:test \
  --tests 'io.codelaser.jfocus.refactor.plan.renameidentifier.TestRenameTypeParameter.testTypeParamUsedInSignatureWithCaller'
```

### How to make it fail again (i.e. see the *maddi* bug, not the workaround)

Temporarily revert the caller-side workaround in
`codelaser-refactor-renamemodule/.../renameidentifier/RenameTypeParameter.java` so `handleType` does the plain
identity lookup again:

```java
// BEFORE the workaround (what handleType effectively did) -- put this back to reproduce:
return parameterizedType.extractTypeParameters().stream()
        .filter(tp -> tp.simpleName().equals(typeParameterName))
        .flatMap(tp -> {
            List<Source> sources = source.detailedSources().details(tp);   // <-- identity lookup; misses after analysis
            return sources.stream().map(s -> new LocationImpl(typeFQN, methodFQN, s));
        });
```

Then run the test **repeatedly** (it's a race — one run is not enough):

```bash
for i in $(seq 1 12); do
  ./gradlew --quiet --rerun-tasks :codelaser-refactor-renamemodule:test \
    --tests '...TestRenameTypeParameter.testTypeParamUsedInSignatureWithCaller' >/dev/null 2>&1 \
    && echo "run $i PASS" || echo "run $i FAIL"
done
```

Observed without the workaround: **~5/12 PASS, ~7/12 FAIL** (flaky). With the workaround: **12/12 PASS**.
The buggy output on a failing run is `class A<S ...>` + `S first` renamed, but `add(T tuple)` / `next(T tuple)` left as `T`.

---

## Root cause (detailed)

### 1. The store is identity-keyed

`maddi-cst-impl/src/main/java/io/codelaser/maddi/cst/impl/element/DetailedSourcesImpl.java`

```java
public class DetailedSourcesImpl implements DetailedSources {
    private final IdentityHashMap<Object, Object> identityHashMap;   // <-- identity, not value
    ...
    @Override
    public List<Source> details(Object object) {
        Object o = identityHashMap.get(object);   // identity get: only hits for the SAME instance
        if (o == null) return List.of();
        return o instanceof List ? (List<Source>) o : List.of((Source) o);
    }
}
```

### 2. The parser records the *parser's* `TypeParameter` instance

`maddi-java-parser/src/main/java/io/codelaser/maddi/parser/java/ParseType.java:202`

```java
detailedSourcesBuilder.put(withoutTypeParameters.typeParameter(), details.pop());
```

So the source position of `T` (wherever it's written) is filed under the `TypeParameter` object that existed **at parse time**.

### 3. Analysis re-instantiates the type parameter on method signatures

When another type *uses* `A` (`list.add(t)`, `list.next(t)`), maddi resolves/translates `A`'s generic method
signatures for that usage. That translation produces **new** `ParameterizedType` / `TypeParameter` objects for the
signature's type parameters (same name/owner/index, different identity). This is the piece to pin down precisely in
maddi — it's in the prepwork/link analysis path (`E2ImmuPrep.make` → `PrepAnalyzer` / the link computation), via the
`TranslationMap` machinery (`maddi-cst-impl/.../translate/TranslationMapImpl.java`). After it runs, the `TypeParameter`
you get off `mi.returnType()` / `pi.parameterizedType()` (`ParameterizedType.extractTypeParameters()`) is **not** the
object the parser filed in step 2.

### 4. The lookup misses → dropped edit → (parallel) race

The consumer (`RenameTypeParameter.handleType`) does `detailedSources().details(tp)` with the *analyzed* `tp` → identity
miss → `List.of()` → no rename location emitted → the `T` in the signature is left untouched.

Because maddi analyses types in parallel (`ForkJoinPool.commonPool`), *whether the signature type parameters have been
re-instantiated yet* when the consumer reads them is timing-dependent → the miss is intermittent → **flaky** at unit
scale. Fields (`fi.source()`) and method-body locals (`lvc.source()`) keep the parser's original instance, so they
always match — which is why only the *cross-type signature* usages are affected. At corpus scale the rewiring is
guaranteed, so it fails deterministically.

---

## The caller-side workaround we shipped (and what maddi should make unnecessary)

`jfocus-refactor-service` — `RenameTypeParameter.java`, `handleType`:

```java
// Identity lookup first (fast path); on a miss, fall back to the CANONICAL declared instance -- the owner's
// type-parameter list, by index -- which IS the instance the parser recorded. Makes the lookup stable regardless
// of re-instantiation.
private static List<Source> sourcesForTypeParameter(DetailedSources detailedSources, TypeParameter tp) {
    List<Source> byInstance = detailedSources.details(tp);
    if (byInstance != null && !byInstance.isEmpty()) return byInstance;
    TypeParameter canonical = canonicalInstance(tp);
    if (canonical != null && canonical != tp) {
        List<Source> byCanonical = detailedSources.details(canonical);
        if (byCanonical != null) return byCanonical;
    }
    return List.of();
}

private static TypeParameter canonicalInstance(TypeParameter tp) {
    var owner = tp.getOwner();                         // Either<TypeInfo, MethodInfo>
    List<TypeParameter> declared = owner.isLeft()
            ? owner.getLeft().typeParameters()
            : owner.getRight().typeParameters();
    int idx = tp.getIndex();
    return idx >= 0 && idx < declared.size() ? declared.get(idx) : null;   // the declared (parser) instance
}
```

This works because a `TypeParameter` always knows its owner (`getOwner()` → `TypeInfo`/`MethodInfo`) and zero-based
`getIndex()`, so we can recover the declared instance — the one the parser filed. But every consumer of
`DetailedSources` for type parameters would need the same trick; that's a code smell pointing at maddi.

Relevant `TypeParameter` API (`maddi-cst-api/.../info/TypeParameter.java`): `int getIndex()`,
`Either<TypeInfo, MethodInfo> getOwner()`.

---

## Where/how to fix it in maddi

Two viable directions (pick one):

### Option A — make `DetailedSources` not depend on `TypeParameter` object identity (recommended, contained)
Keep the fast `IdentityHashMap`, but add a **value-based fallback** for keys whose identity is unstable. For a
`TypeParameter`, a stable value key is `(owner FQN, index)` (or name+owner). On an identity miss, look up by that
stable key. Contained to `DetailedSourcesImpl` (+ builder), fixes *all* consumers at once, and mirrors exactly what our
caller-side fallback does — just in the right place.
- Files: `maddi-cst-impl/.../element/DetailedSourcesImpl.java` (add a secondary index for type-parameter keys);
  `maddi-cst-api/.../element/DetailedSources.java` if the contract needs a note.
- Watch: `merge`, `addAll`, `withSources`, `copy` must maintain both indices; other identity-keyed entry types
  (`TypeInfo`, `ParameterizedType`, plain tokens) are unaffected.

### Option B — stop re-instantiating; keep the parser's `TypeParameter` instance on analyzed signatures
Ensure the analysis/translation of a generic method signature **reuses** the declared `TypeParameter` instance instead
of allocating a fresh one (so identity stays stable end-to-end). Conceptually the cleanest, but it's surgery in maddi's
hot analysis/translation path (`PrepAnalyzer` / link computation / `TranslationMapImpl`) and higher-risk. First step:
find the exact allocation site — instrument where `mi.returnType()`/`pi.parameterizedType()` diverges from
`typeInfo.typeParameters().get(index)` after `E2ImmuPrep.make(...)`.

**Determinism note (either option):** the *flakiness* is a symptom of analysis running in parallel
(`ForkJoinPool.commonPool`). Fixing identity (Option A) or instance-stability (Option B) makes the result correct and
deterministic regardless of thread scheduling. Do **not** "fix" it by serializing analysis — that hides the bug and
costs performance.

---

## Verification (after the maddi fix)

1. In `jfocus-refactor-service`, **remove the caller-side workaround** (revert `sourcesForTypeParameter` /
   `canonicalInstance`; make `handleType` do the plain `details(tp)` lookup again).
2. Run `testTypeParamUsedInSignatureWithCaller` **repeatedly** (12+ times, `--rerun-tasks`) — it must be **12/12 green**
   with no workaround.
3. Grep the rename module for other `detailedSources().details(...)` / `.detail(...)` call sites and confirm none still
   need a canonical-instance dance.
4. (Optional, strongest) run the corpus fuzz `TestTimefoldRandomRenames` at index 175
   (`RANGE_START=RANGE_END=175`, `renameFuzz`) and confirm the renamed `.../bavet/common/tuple/TupleList.java` compiles.

---

## Appendix — file map

| What | Path |
|---|---|
| Identity-keyed store (the bug) | `maddi/maddi-cst-impl/src/main/java/io/codelaser/maddi/cst/impl/element/DetailedSourcesImpl.java` |
| `DetailedSources` contract | `maddi/maddi-cst-api/src/main/java/io/codelaser/maddi/cst/api/element/DetailedSources.java` |
| Parser records the type-parameter source | `maddi/maddi-java-parser/src/main/java/io/codelaser/maddi/parser/java/ParseType.java` (~line 202) |
| Translation machinery (re-instantiation) | `maddi/maddi-cst-impl/src/main/java/io/codelaser/maddi/cst/impl/translate/TranslationMapImpl.java` |
| `TypeParameter` API (`getOwner`/`getIndex`) | `maddi/maddi-cst-api/src/main/java/io/codelaser/maddi/cst/api/info/TypeParameter.java` |
| Consumer + current workaround | `jfocus-refactor-service/codelaser-refactor-renamemodule/.../renameidentifier/RenameTypeParameter.java` (`handleType`, `sourcesForTypeParameter`, `canonicalInstance`) |
| Reproducing test | `jfocus-refactor-service/codelaser-refactor-renamemodule/.../renameidentifier/TestRenameTypeParameter.java` (`testTypeParamUsedInSignatureWithCaller`) |

**Parsing caveat when reproducing at scale:** maddi's javac-backed parse is thread-hostile; parse serially in one
`JavaInspector` and only parallelize the analysis (see `maddi/maddi-inspection-openjdk/parsing-stability.md`). The
`ForkJoinPool` parallelism referenced above is the *analysis* phase, which is where the re-instantiation race lives.
