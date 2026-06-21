# Accommodating Kotlin in the CST — statements & expressions

*Assessment date: 2026-06-20. Scope: `org.e2immu.language.cst.api.statement` and
`org.e2immu.language.cst.api.expression`. The CST ("Common Syntax Tree") is intended to represent multiple
source languages (Java today; C# and Kotlin planned) with one shared tree.*

## Headline

The statement and expression packages need **very little structural change** to host Kotlin. The
overwhelming majority of Kotlin constructs map onto existing nodes, often for free, thanks to two CST
choices that happen to suit Kotlin:

- **Operators are modelled as `MethodInfo`** (`BinaryOperator.operator()`, `UnaryOperator.operator()`),
  so Kotlin's operator-overloading (`plus`, `times`, `compareTo`, `rangeTo`, `contains`, `get`, `set`,
  `invoke`, …) maps directly to `MethodCall` / the operator nodes.
- **Field access is `VariableExpression(FieldReference)` with an arbitrary `scope()` expression**, so
  Kotlin property access (`a.b`, getter calls) has a home.

The heavy Kotlin lifting is **not** in these packages — it is in `type` (nullable types,
declaration-site variance, function types) and `info` (properties, primary constructors, extension
receivers, top-level functions). See *Boundary* at the end.

Convention assumed throughout: pure **surface-syntax** differences are recorded in
`source().detailedSources()`; **semantic** differences (nullability, argument binding, jump targets) are
*not* — they need a real field or a type/info-layer concept.

---

## Statements

### Map cleanly (no change)
| Kotlin | CST |
|---|---|
| `while`, `do/while` | `WhileStatement`, `DoStatement` |
| `for (x in …)` | `ForEachStatement` (Kotlin has no C-style `for`; `ForStatement` simply goes unused) |
| `break@label`, `continue@label` | `Break/ContinueStatement.goToLabel()` (already present) |
| `throw`, `try/catch/finally` | `ThrowStatement`, `TryStatement` (no resources, no checked exceptions) |
| labels (`loop@`) | `Statement.label()` |
| expression statements | `ExpressionAsStatement` |

### Shoehorn + a `detailedSources` marker
- `val` / `var` → `LocalVariableCreation` (already half-anticipated: `Modifier.isFinal` = `val`).
- `if` as a statement → `IfElseStatement`.
- `when` (statement) → `SwitchStatementNewStyle`. Arms fit `SwitchEntry`; subject-less
  `when { cond -> }` needs a placeholder selector (`EmptyExpression` or boolean `true`). Conditions such
  as `in range`, `is T`, comma-lists already fit `SwitchEntry.conditions()` (each becomes a
  `MethodCall` / `InstanceOf`).
- `assert(...)` / `synchronized(x){}` are *functions* in Kotlin → `MethodCall` (+ `Lambda`); the
  `AssertStatement` / `SynchronizedStatement` nodes simply go unused.

### Needs a small field addition (or an accepted marker)
- **Labeled non-local `return@lambda`** — `ReturnStatement` has no jump target (`label()` is the
  statement's *own* label). Stashing the target in `detailedSources` works for printing, but if the
  analyzer must resolve non-local returns, a `goToLabel()`-style field is cleaner. *Minor.*

### Genuine gaps (modest)
- **Local function declarations** (`fun foo() {}` inside a body). No "local method declaration"
  statement exists — only `LocalTypeDeclaration` (types). Java cannot do this, so the CST never needed
  it. Options: a small new `LocalMethodDeclaration` statement (preferred), or shoehorn as a
  `LocalTypeDeclaration` of a synthetic holder type.
- **Destructuring** `val (a, b) = p` — partially fits `LocalVariableCreation` (it has
  `otherLocalVariables()`), but binding is by `componentN()` decomposition, not a shared comma
  declaration. Shoehornable with a marker + synthetic initializers; semantically a stretch but workable.

---

## Expressions

### Map cleanly (no change), often elegantly
- All literals → the constant nodes; `null` → `NullConstant`.
- **All operators** → `MethodCall` / operator nodes (see Headline). `a[i]`, `a..b`, `a in b`, `a()` are
  all `MethodCall`.
- `is` / `!is` → `InstanceOf` (negation via marker).
- `as` → `Cast`.
- lambdas incl. trailing-lambda call syntax → `Lambda` + `MethodCall`; anonymous `fun` → `Lambda`.
- `::foo`, `Foo::bar` → `MethodReference`.
- `object : I {}` → `ConstructorCall` with `anonymousClass()`.
- `if` / `when` / (most) `try` as expressions → `InlineConditional` / `SwitchExpression`.
- `Foo()` construction (no `new`) → `ConstructorCall` (drop `new` on print).
- property access `a.b` → `VariableExpression(FieldReference scope=a)` or `MethodCall` (getter).
- `this`, `super` → `VariableExpression` (`This` variable exists).

### Shoehorn + marker
- Elvis `a ?: b` → `InlineConditional`.
- string templates `"$x ${e}"` → `StringConcat` (mark segment boundaries).
- `X::class` → `ClassExpression` (mark `KClass` vs `.class`).
- named args `f(x = 1)`, spread `*a`, labeled `this@Outer` → markers on `MethodCall` /
  `VariableExpression`.

### Needs care — a surface marker is *not* enough
- **Null-safety operators** `?.`, `!!`, `as?`. They print fine as markers but carry **semantic** weight
  (nullability) that does not belong in `detailedSources()`. Preferred approach: model nullability in the
  **type** layer (`String?`) and desugar the operators in the front-end — `a?.b` →
  `if (a != null) a.b else null` (`InlineConditional`); `a!!` → a checked cast / intrinsic. So: **no new
  expression node**, but a real type-system change behind them.
- **`try` as an expression** (yielding a value) — no node yields a value from `try`. Rare; desugar to a
  helper or accept a small new node if it actually shows up.

---

## Bottom line

- **Statements:** ~1 genuinely new node worth adding (local `fun`), 1 small field (non-local `return@`
  target); the rest shoehorns. Subject-less `when` is the only mildly awkward reuse.
- **Expressions:** essentially **zero new nodes required**. Null-safety is the only thing needing real
  model support, and that support belongs in the **type** layer, not here.
- **Caveat:** `detailedSources()` is the right tool for *reproducing source*, but should not become the
  home for *semantics* the analyzer must reason about (nullability, named-argument binding, non-local
  return targets). Those few cases warrant a real field or a `type`/`info` concept.

## Boundary (out of scope for statements & expressions, where the real work is)

Expected structural Kotlin work lives elsewhere:
- **`type`** — nullable types (`T?`), declaration-site variance (`out`/`in`), function types
  `(A) -> B`, `Nothing`/`Unit`, flexible/platform types.
- **`info`** — properties (with getters/setters), primary constructors, extension functions &
  receivers, top-level functions, `data`/`sealed`/`object`/companion, `suspend`.

---

# Accommodating Kotlin in the CST — info objects

*Assessment date: 2026-06-20. Scope: `org.e2immu.language.cst.api.info` (TypeInfo, MethodInfo,
FieldInfo, ParameterInfo, TypeParameter) and `org.e2immu.language.cst.api.element.ModuleInfo`.
This is where the statement/expression document said "the real work is".*

## Headline

The `info` package needs **substantial but targeted additions** to host Kotlin. The shared structure
(`Info`, `Access`, `compilationUnit`, `fullyQualifiedName`, builder lifecycle) carries over cleanly.
The gaps cluster around four Kotlin concepts that have no Java counterpart:

1. **Properties** — Kotlin source declares `val`/`var`; the CST has only `FieldInfo` (no
   getter/setter body on the field, no delegate slot).
2. **Extension functions** — `fun T.foo()` has an extra receiver type that does not appear anywhere
   in `MethodInfo`.
3. **Declaration-site variance** — `out T` / `in T` on `TypeParameter` does not exist.
4. **Suspend functions & coroutine-specific modifiers** — `suspend`, `inline`, `crossinline`,
   `reified`, `lateinit` are absent from every modifier interface.

Everything else is missing a keyword or a `TypeNature` value, but does not require new structural
nodes — it can be modelled by extending existing enums/interfaces with Kotlin-specific members.

---

## `Info` — base interface

### Maps cleanly
`hasBeenInspected()`, `simpleName()`, `fullyQualifiedName()`, `descriptor()`, `isSynthetic()`,
`hasBeenAnalyzed()`, `javaDoc()`, `translate()`, and the builder lifecycle all port without change.

### Needs a small extension
- **`Access`** has `private / package / protected / public` (levels 0–3). Kotlin adds
  **`internal`** (module-private), which sits between `package` and `public` in effective
  accessibility, but is a module boundary — not a package boundary. Inserting it as level 2 and
  bumping `protected` and `public` is one option; alternatively make `Access` open to
  language-specific singletons.
- **KDoc** (`/** [Foo.bar] */`) is structurally close enough to JavaDoc that `javaDoc()` can hold
  it. Rendering differences (`[ref]` vs `{@link}`) belong in the printer, not the model.

---

## `TypeInfo`

### Maps cleanly
Hierarchy navigation (`parentClass()`, `interfacesImplemented()`, `superTypesExcludingJavaLangObject()`),
sub-type/enclosing-type relationships, `singleAbstractMethod()` for `fun interface`, `isSealed()`
+ `permittedWhenSealed()` (Kotlin seals by co-location in one file; the permitted list still needs
to be populated by the front-end), and `typeParameters()` all map without change.

### Needs keyword/enum additions
- **`TypeNature`** currently covers `class`, `interface`, `enum`, `record`, `annotation`.  Kotlin
  adds: `object` (singleton), `companion object`, `data class`, `data object`, `enum class`, `annotation class`.  
  `data class` and `data object` can be marked with a `TypeModifier` (see below), but `object` is
  semantically different from a class (no constructor, instant singleton) and deserves its own
  `TypeNature` value.
- **`TypeModifier`** currently covers `public / private / protected / abstract / final / static /
  sealed / non-sealed`. Kotlin adds:
  - `open` (the default in Kotlin is *final*; `open` un-seals it — the inverse of Java)
  - `internal` (module visibility)
  - `data` (data class/object — generates `equals`/`hashCode`/`copy`/`componentN`)
  - `inline` / `value` (value classes, `@JvmInline`)
  - `fun` (on `interface`, marks it as a functional interface — Kotlin's analogue of
    `@FunctionalInterface`)
  - `inner` (non-static nested class; Java's `isInnerClass()` default derives this from `!isStatic()` +
    enclosing type, so this is already inferable, but Kotlin spells it explicitly as a keyword)

### Needs a structural concept: primary constructor
Kotlin types have at most one **primary constructor** declared in the class header:

```kotlin
class Point(val x: Int, val y: Int)  // primary constructor + two properties
```

`TypeInfo.constructors()` is a flat list with no distinction between primary and secondary
constructors. Introducing `primaryConstructor(): MethodInfo` (nullable) is the cleanest addition;
the existing list stays for secondary constructors.

### Needs a structural concept: properties
Kotlin source declares `val`/`var` — not raw fields. A property compiles to a backing field plus
accessor methods in bytecode, but at the source level it is a single declaration with an optional
custom getter and/or setter body and an optional delegate. The existing `FieldInfo` has no slot for
these bodies.

Practical options (cheapest first):
1. **Shoehorn**: represent the property's backing field as a `FieldInfo`; attach its generated
   getter/setter as ordinary `MethodInfo` entries; use a naming convention or a synthetic
   `isProperty()` flag on `MethodInfo` to link them. Works for bytecode-only import; breaks down
   when the source has a custom getter body that must be preserved.
2. **New node**: add a `PropertyInfo extends Info` that holds the field type, modifiers, initializer,
   optional getter `MethodInfo`, optional setter `MethodInfo`, and optional delegate `Expression`.
   `TypeInfo.Builder` grows `addProperty(PropertyInfo)`. This is the correct long-term model.

---

## `MethodInfo`

### Maps cleanly
`isConstructor()`, `isStatic()`, `isAbstract()`, `isFinal()`, `returnType()`, `parameters()`,
`methodBody()`, `typeParameters()`, `exceptionTypes()` (always empty for Kotlin),
`MethodType.isStaticInitializer()`, `isCompactConstructor()` (unused for Kotlin), overrides
resolution, commutation data, pre/post conditions — all map without change.

`isInfix()` and `isPostfix()` are already present and cover Kotlin's `infix` modifier directly.

### Needs additions to `MethodModifier`
`MethodModifier` covers `public / private / protected / abstract / default / synchronized / final /
static`. Kotlin adds:
- **`open`** — inverse of Java's default; Kotlin's default is `final`.
- **`internal`** — module visibility.
- **`override`** — Kotlin spells this as an explicit modifier (Java infers it). The override set on
  `MethodInfo` already tracks overrides, but the *source keyword* is a modifier.
- **`operator`** — marks a method as an operator function. `isInfix()` exists; `isOperator()` does
  not.
- **`suspend`** — coroutine modifier; critical for the analyzer to reason about continuation
  passing.
- **`inline`** — the method body is inlined at call sites; parameters may be `noinline` /
  `crossinline` (see `ParameterInfo`).
- **`external`** — native implementation (analogous to Java `native`).
- **`tailrec`** — compiler transforms tail-calls into loops; a hint, not a structural change.

### Genuine gap: extension receiver
Kotlin extension functions have an extra dispatch parameter, the **extension receiver**:

```kotlin
fun String.shout() = uppercase() + "!"   // receiver type is String
```

`MethodInfo` has no `extensionReceiverType(): ParameterizedType` field. This is a first-class
source concept — the receiver appears as `this` inside the body and is visible to callers as the
syntactic target (`"hello".shout()`). It cannot be modelled as a regular parameter without
losing call-site semantics. A new field is required.

### Genuine gap: suspend functions
`isSuspend()` does not exist. A suspend function compiles to a method with a hidden
`Continuation<T>` parameter and a different return type in bytecode, but at the CST level the
source signature (`suspend fun foo(): T`) must be preserved. An `isSuspend()` flag on `MethodInfo`
(or a `MethodModifier.SUSPEND`) is needed.

---

## `FieldInfo`

### Maps cleanly (for bytecode-imported fields)
`name()`, `owner()`, `type()`, `isStatic()`, `isFinal()`, `isTransient()`, `isVolatile()`,
`initializer()` all have Kotlin analogues at the bytecode level. A Kotlin `const val` compiles to
`static final`, which maps directly.

### `FieldModifier` needs additions
Currently: `static / final / volatile / transient / public / private / protected`.  Kotlin adds:
- **`open`** (on a property, overridable)
- **`internal`**
- **`override`**
- **`lateinit`** — the property is non-null but not initialised in the constructor; the compiler
  inserts a `null` check on every access. Semantically important for the analyzer: a `lateinit var`
  has a non-nullable type but its backing field is null until initialisation.
- **`const`** — compile-time constant (`const val`). Functionally `static final`, but with
  constraints (primitive/String, initializer must be a constant expression). Derivable from
  `isStatic() && isFinal()` + a flag or modifier.

### Not a gap — but acknowledged
Kotlin properties with custom getter/setter bodies or delegates are **not** `FieldInfo`. See the
property model under `TypeInfo` above.

---

## `ParameterInfo`

### Maps cleanly
`index()`, `name()`, `isVarArgs()` (`vararg` in Kotlin), `isFinal()` (always true in Kotlin —
parameters are immutable), `isUnnamed()` (`_` in Kotlin), `methodInfo()`, analysis data
(`isUnmodified()`, `assignedToField()`) all carry over cleanly.

### Genuine gap: default parameter values
Kotlin parameters can carry a default value:

```kotlin
fun greet(name: String = "World") { … }
```

There is no `defaultValue(): Expression` on `ParameterInfo`. This is a source-level concept (not
bytecode) and is needed to re-emit Kotlin source or to reason about which parameters are required
at call sites. Adding `defaultValue(): Expression` (nullable) is straightforward.

### Needs modifier additions
Parameters inside `inline` functions can carry `noinline` or `crossinline` modifiers. These affect
how lambda arguments are treated (not inlined / allowed to non-locally return). There is currently
no modifier slot on `ParameterInfo`. A small `ParameterModifier` interface or two boolean flags
would cover this.

---

## `TypeParameter`

### Maps cleanly
`getIndex()`, `getOwner()` (either `TypeInfo` or `MethodInfo`), `typeBounds()`, `typeBoundsAreSet()` — all fine. Kotlin's upper bound syntax `T : UpperBound` maps directly to `typeBounds()`.

### Genuine gap: declaration-site variance
Kotlin supports **declaration-site variance** on type parameters:

```kotlin
class Box<out T>   // covariant — T can only appear in `out` position
class Sink<in T>   // contravariant — T can only appear in `in` position
```

There is no `variance()` field on `TypeParameter`. This is a structural gap: call-site (use-site)
variance is handled by wildcards in `ParameterizedType`, but declaration-site variance lives on the
`TypeParameter` itself. A `Variance { INVARIANT, COVARIANT, CONTRAVARIANT }` enum and a
`variance()` accessor are needed.

### Genuine gap: `reified` type parameters
Kotlin inline functions can use `reified` type parameters (the type is available at run-time):

```kotlin
inline fun <reified T> isType(value: Any) = value is T
```

There is no `isReified()` flag on `TypeParameter`. This is a first-class source concept: `reified`
changes how the parameter can be used (type checks, reflection) and is only allowed on type
parameters of `inline` functions.

---

## `ModuleInfo`

### Maps cleanly
`ModuleInfo` represents a Java 9 `module-info.java` and its directives (`requires`, `exports`,
`opens`, `uses`, `provides`). Kotlin JVM projects consume the Java module system directly — a
Kotlin module that uses JPMS will have a `module-info.java` (or a synthetic equivalent) and it
maps without change.

Kotlin Multiplatform's `expect`/`actual` mechanism is a *source-level* concept unrelated to the
JPMS. It does not belong in `ModuleInfo`; it belongs in a future `KotlinMultiplatformInfo` or
similar node outside the current scope.

---

## Bottom line

| Object | Gaps | Severity |
|---|---|---|
| `Info` (base) | `Access` needs `internal` | Minor |
| `TypeInfo` | `TypeNature` needs `object`; `TypeModifier` needs `open/internal/data/inline/value/fun`; no primary-constructor distinction; no property model | **Major** (property model) / Moderate (rest) |
| `MethodInfo` | No extension receiver; no `isSuspend()`; `MethodModifier` needs `open/internal/override/operator/suspend/inline/external/tailrec` | **Major** (extension receiver, suspend) |
| `FieldInfo` | `FieldModifier` needs `open/internal/override/lateinit/const`; no property getter/setter/delegate | Moderate (modifiers) / **Major** (property — shared with `TypeInfo`) |
| `ParameterInfo` | No default parameter values; no `noinline`/`crossinline` modifier | Moderate (default values) / Minor (modifiers) |
| `TypeParameter` | No declaration-site variance; no `reified` | **Major** (variance) / Moderate (reified) |
| `ModuleInfo` | None for JVM Kotlin | Clean |

**Priority order for implementation:**
1. Declaration-site variance on `TypeParameter` — affects every generic Kotlin type.
2. Extension receiver on `MethodInfo` — affects a large fraction of idiomatic Kotlin APIs.
3. `suspend` on `MethodInfo` — needed for any coroutine-using code.
4. Property model on `TypeInfo`/`FieldInfo` — needed for round-trip source fidelity.
5. `TypeNature` and modifier extensions — needed for correct classification of `object`, `data class`, etc.
6. Default parameter values on `ParameterInfo` — needed for call-site analysis.
7. `internal` in `Access` — needed for correct visibility reasoning.
8. `lateinit` in `FieldModifier`, primary constructor pointer on `TypeInfo` — useful but lower urgency.
