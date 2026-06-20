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

## Boundary (out of scope here, where the real work is)

Expected structural Kotlin work lives elsewhere:
- **`type`** — nullable types (`T?`), declaration-site variance (`out`/`in`), function types
  `(A) -> B`, `Nothing`/`Unit`, flexible/platform types.
- **`info`** — properties (with getters/setters), primary constructors, extension functions &
  receivers, top-level functions, `data`/`sealed`/`object`/companion, `suspend`.
