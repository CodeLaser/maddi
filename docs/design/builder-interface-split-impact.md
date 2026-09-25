# Splitting the mutable Builder off the read-only Inspection interfaces — impact survey (2026-07)

Companion to [`dynamic-immutability-feasibility.md`](dynamic-immutability-feasibility.md) §"Why
`TypeInspectionImpl` still does not certify". That note established the cap; this one costs the two ways out,
so the API decision can be made on numbers rather than instinct. **No refactor was landed** — both options were
trial-compiled and reverted; `git diff HEAD -- '*.java'` is empty.

## Headline

| | option A: drop `implements XInspection` from the Builders | option B: move the offending members off the interfaces |
|---|---|---|
| compiler errors | **74** | **6** |
| files touched | 10 | 6 |
| structural (need a design decision) | 18 sites | 2 sites |
| mechanical (`@Override` removal) | 56 | 4 |
| measured diff of the fix | — | **+13 / −12 lines** |
| certifies `TypeInspectionImpl` / `MethodInspectionImpl` | (not measured) | **yes, measured** |
| impact on the three parsers | **zero** | **zero** |
| impact outside the repo | **zero** | **zero** |

**Recommendation: option B.** It is an order of magnitude cheaper, it was measured to work, and it is a *more
honest* API besides — see §"Why B is not merely the cheap option".

## The external-surface question, answered first

The user's main concern was consumers outside maddi. **There is no external exposure under either option.**

The five read-only interfaces — `TypeInspection`, `MethodInspection`, `FieldInspection`,
`ParameterInspection`, `TypeParameterInspection`, plus the base `Inspection` — live in
**`maddi-cst-impl`**, not `maddi-cst-api`. Grepping the whole repository for them:

| module | live references |
|---|---|
| `maddi-java-parser` | 0 |
| `maddi-inspection-integration` | 0 |
| `maddi-inspection-parser` | 0 (one hit, inside a commented-out block) |
| `maddi-java-openjdk` | 0 |
| `maddi-inspection-openjdk` | 0 |
| `maddi-kotlin-k2` | 0 |
| `maddi-inspection-kotlin` | 0 |
| `maddi-inspection-mixed` | 0 |
| `maddi-cst-io`, `prepwork`, `modification-*`, `run-*`, plugins, tests, `dogfood/` | 0 |
| `maddi-cst-impl` | all of them |

**Every live reference is inside `maddi-cst-impl`.** The only hit elsewhere is
`maddi-inspection-parser/…/MethodTypeParameterMapImpl.java:195` (`ParameterInspection.Builder`), which sits
inside a `/* … */` block — dead code that does not even name a type that exists (`ParameterInspection` has no
nested `Builder`).

What the parsers actually use is `TypeInfo.Builder` / `MethodInfo.Builder` / … — **`maddi-cst-api` interfaces**,
obtained from `typeInfo.builder()`. `TypeInspectionImpl.Builder implements TypeInspection, TypeInfo.Builder`;
neither option touches the second half of that, so the builder surface the parsers and external consumers
program against is unchanged. The user's suspicion was right: **the shared read-only interface is a nicety, and
an internal one.**

## Option A — drop `implements XInspection` from the Builders

**It is structural, not cosmetic**, and the two-phase field is exactly why. Confirmed for all five families:

| holder | field | variable phase holds |
|---|---|---|
| `TypeInfoImpl:53` | `EventuallyFinalOnDemand<TypeInspection> inspection` | `new TypeInspectionImpl.Builder(this)` |
| `MethodInfoImpl:105` | `EventuallyFinal<MethodInspection> inspection` | `new MethodInspectionImpl.Builder(this)` |
| `FieldInfoImpl:45` | `EventuallyFinal<FieldInspection> inspection` | `new FieldInspectionImpl.Builder(this)` |
| `ParameterInfoImpl:47` | `EventuallyFinal<ParameterInspection> inspection` | `new ParameterInspectionImpl.Builder(this)` |
| `TypeParameterImpl:44` | `EventuallyFinal<TypeParameterInspection> inspection` | `new TypeParameterInspectionImpl.Builder(this)` |

The shared interface is precisely what lets one field hold the builder before `commit()` and the product after.
Drop it and the field's type parameter no longer admits the builder. That is the eventual-immutability pattern
of §060 in its purest form, and option A breaks it.

Trial-compiled (`:maddi-cst-impl:compileJava --rerun-tasks`, all five Builders): **74 errors, 10 files, none
outside `maddi-cst-impl`**.

- **18 structural** — `incompatible types: Builder cannot be converted to XInspection`:
  `TypeInfoImpl:60,67,83`; `FieldInfoImpl:54,286`; `MethodInfoImpl:122`; `MethodInspectionImpl:188`;
  `ParameterInfoImpl:56`; `TypeParameterImpl:51`. Each is a two-phase field assignment or a read through one.
  Fixing them means giving every `EventuallyFinal<XInspection>` a common supertype again (a narrower
  read-only interface), or splitting the field in two, or casting at every read — i.e. re-inventing the
  interface under another name.
- **56 mechanical** — `does not override or implement a method from a supertype`; the methods stay, the
  `@Override` annotations go.

## Option B — move only the offending members off the interfaces

### The minimal offending set is exactly two members

Determined empirically from the dogfood run rather than assumed. A member qualifies when the interface's
abstract method is modifying, which happens when *any* implementation is
(`AbstractMethodAnalyzerImpl.methodNonModifying` takes the meet):

| interface | methods | modifying |
|---|---|---|
| `Inspection` | 11 | 0 |
| `ParameterInspection` | 2 | 0 |
| `TypeParameterInspection` | 5 | 0 |
| `FieldInspection` | 3 | 0 |
| `MethodInspection` | 11 | **1** — `withSynthetic(boolean)` |
| `TypeInspection` | 18 | **1** — `superTypesExcludingJavaLangObject()` |

Two members, in two interfaces. Both are modifying in the Builder and non-modifying in the product:

- **`TypeInspection.superTypesExcludingJavaLangObject()`** — the product returns a stored `final` field
  (`TypeInspectionImpl:92`). The Builder *computes* it (`TypeInspectionImpl:193`) by walking
  `type.parentClass()` / `type.interfacesImplemented()`, which go through other `TypeInfo`s'
  `EventuallyFinalOnDemand.get()` and run the on-demand loader. The modification is real.
  Called from exactly two places: `TypeInfoImpl:300` (delegation) and `TypeInspectionImpl:332` (inside
  `Builder.commit()`, to populate the product's field).
- **`MethodInspection.withSynthetic(boolean)`** — the product returns a **new** `MethodInspectionImpl`
  (a functional with-er, `MethodInspectionImpl:105`). The Builder **mutates itself and returns `this`**
  (`MethodInspectionImpl:186`). These are semantically different operations sharing one signature.
  Called from exactly one place: `MethodInfoImpl:692`.

Everything else outside `maddi-cst-impl` calls `TypeInfo.superTypesExcludingJavaLangObject()` and
`MethodInfo.withSynthetic()` — the **`maddi-cst-api`** methods — never the Inspection ones.

### Cost, trial-compiled

**6 errors, 6 files, +13 / −12 lines.**

- **2 structural**, both a one-branch fix:
  - `TypeInfoImpl:300` delegates unconditionally; it needs to branch on which phase the field is in.
  - `MethodInfoImpl:692` — **already** branches on `inspection.isFinal()` and **already casts** to
    `MethodInspectionImpl.Builder` on the variable path (line 695). Only the final-phase line needs a cast to
    `MethodInspectionImpl`. `Builder.withSynthetic` then becomes unreachable and can be deleted outright: it
    exists solely to satisfy the interface.
- **4 mechanical** — `@Override` removals on the product and Builder of each member.

### It was measured to work

Dogfood, with `--preload-analysis-results-dirs` (mandatory — see `dogfood/README.md`):

| type | baseline | with option B |
|---|---|---|
| `TypeInspection` | `@FinalFields` | **`@Immutable(hc=true)`** |
| `MethodInspection` | `@FinalFields` | **`@Immutable(hc=true)`** |
| `TypeInspectionImpl` | `@Mutable` | **`@Immutable(hc=true)`** |
| `MethodInspectionImpl` | `@Mutable` | **`@Immutable(hc=true)`** |
| `FieldInspection` / `FieldInspectionImpl` | `@FinalFields` / `@Mutable` | unchanged |

Aggregates: `immutableType` **202 → 204** — exactly the two products, nothing else moved.
`eventualMethod` 30, `eventuallyImmutableType` 4, `immutableField` 115, `independentType` 471, all unchanged.

## `FieldInspection` is a different problem — option B does not fix it

`FieldInspection` has **zero** modifying methods, yet is capped at `@FinalFields`, so
`FieldInspectionImpl` is `@Mutable`. The cause is the **independence gate**, not the shared interface:
`FieldInspection` is `@Dependent`, and all three of its methods are dependent —
`analysisOfInitializer()`, `fieldModifiers()`, `initializer()` (all non-modifying, `nonMod=1`).

So the builder split certifies **2 of the 3** blocked inspection types. `FieldInspectionImpl` needs the
independence work instead, and is out of scope here.

## Why B is not merely the cheap option

Option A removes a genuinely useful abstraction — the one that makes the two-phase `EventuallyFinal<XInspection>`
field possible, which is the §060 pattern the whole eventual-immutability effort exists to support. It would
have to be re-invented under another name.

Option B removes two members that arguably should never have been on a *read-only* interface:

- `withSynthetic` means "give me a copy with this flag" on the product and "mutate yourself" on the Builder.
  One signature, two contracts. The analyzer's complaint is a true observation about that.
- `superTypesExcludingJavaLangObject` is a *computation step* on the Builder, needed only at `commit()` time to
  populate the product's field. It is not part of a read-only view's contract; it is build machinery.

The analyzer is not being placated here — it found a real inconsistency, and B fixes the inconsistency rather
than hiding it.

## Method

- Reference searches over every module (`main`, `test`, `dogfood/`), `*.java` and `*.kt`, excluding `build/`.
- Both options trial-compiled with `:maddi-cst-impl:compileJava --rerun-tasks` and reverted. **A cached
  `compileJava` reports zero errors** — the first option-A run did exactly that; `--rerun-tasks` is required.
- Offending-member set read from the dogfood JSON with a **JSON parser**, not a regex: a first regex pass over
  fixed-width windows reported 33 modifying members instead of 2, because the windows truncated. Absence of
  `nonModifyingMethod` means *false*, absence of `immutableType` means `@Mutable` — neither means undecided.
