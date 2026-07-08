# Printing the CST as Kotlin

`maddi-cst-print-kotlin` prints a (language-agnostic) CST as **Kotlin** source. It is the Kotlin counterpart of
the Java printers in `maddi-cst-impl` (`TypePrinterImpl`/`MethodPrinterImpl`/`FieldPrinterImpl`), and reuses the
language-neutral `OutputElement` IR and the `maddi-cst-print` formatter unchanged.

## Why a separate printer (not a Runtime dialect / origin flag)

The target language is a **print-time choice**, independent of how the CST was parsed (there is no per-element
origin) and independent of the Runtime (there is one, Java-focused, and that's fine). So "print as Kotlin" is
simply *which printer you invoke*:

- **any CST → Java** — the existing `print()` / `runtime.new…Printer` (already works; a Kotlin-parsed CST is
  JVM-shaped, so it prints as idiomatic Java out of the box).
- **any CST → Kotlin** — this module.

No change to the Runtime, to `print()` signatures, or to the ~80 Java `print()` methods.

## The pluggable-printer seam (as in Java)

`KotlinTypePrinter implements` the cst-api `TypePrinter` interface, including the factory overload:

```java
print(ImportData, doTypeDeclaration, MethodPrinterFactory, FieldPrinterFactory, EnclosedTypePrinterFactory)
```

The factories default to `KotlinMethodPrinter`/`KotlinFieldPrinter`/`KotlinTypePrinter`, but a caller can supply
its own — exactly as for the Java `TypePrinterImpl`. The Kotlin printers implement the *same* cst-api interfaces
(`MethodPrinter`/`FieldPrinter`/`TypePrinter`), so custom printers interoperate across languages.

## What it does

- **Declarations** — `class`/`interface`/`enum class`; `public`/`final` omitted (Kotlin defaults), a non-final
  class is `open`; supertypes via `:` (a class parent as a constructor call `Super()`); type parameters `<T>`.
- **Properties** — fields print as `val` (final) / `var` (non-final) `name: Type [= init]`.
- **Functions** — `[vis] [override|abstract] fun [<T>] name(p: T, …)[: ReturnType] body`; `Unit`/void return
  omitted; `override` when the method overrides a supertype method. A single-`return` body prints as an
  **expression body** (`fun f() = expr`); the implicit no-arg default constructor is suppressed.
- **Data classes** — a Java record, or a Kotlin data class (detected by its generated `componentN()` accessors),
  prints as `data class …`, and the regenerated `componentN`/`copy` methods are suppressed.
- **Block layout** — members and block statements are `NEWLINE`-separated (Kotlin has no `;`), so multi-statement
  output stays valid regardless of formatter width.
- **Statements / expressions** (`KotlinStatementPrinter` / `KotlinExpressionPrinter`) — no semicolons; `val`/`var`
  local declarations; and the Java-only forms are translated by recursion: `new Foo(a)`→`Foo(a)`, `(T) x`→
  `x as T`, `x instanceof T`→`x is T`, `c ? t : f`→`if (c) t else f`. The operator families (binary operators,
  `&&`/`||`, unary, negation incl. `!=`) also **recurse into their operands** with the same precedence-based
  parenthesisation as the Java printer, so a Java-only form nested inside an operator translates too (e.g.
  `x is String && …`). True leaves (constants, variable references) delegate to the Java `print()`.
- **Type references** — JVM primitives/JDK types mapped to Kotlin (`int`→`Int`, `java.lang.String`→`String`,
  `java.lang.Object`→`Any`, …); arrays → `Array<…>`; generics recurse; a **nullable** type (the front-end
  records `NullableState.NULLABLE` on the `ParameterizedType`) gets a trailing `?`.
- **Control flow** — `while`/`do`-`while`/`for (x in …)`/`throw`; `switch`→`when (sel) { c -> …; else -> … }`
  (statement and expression, arms unwrapped); `try`/`catch (e: T)`/`finally`. C-style `for` and try-with-resources
  are follow-ups.
- **Lambdas** — `{ p1, p2 -> body }` (single-expression body inlined).
- **Idioms via structure** — `!(x is T)`→`x !is T`; and elvis `a ?: b` recovered from the desugared
  `InlineConditional` marked `NULL_COALESCING` in `DetailedSources` (rather than `if (a == null) b else a`).
- **Idiomatic reconstruction** (needs the analyzer's **prepwork** phase, which populates `getSetField`):
  - getter/setter methods (non-empty `getSetField`) are collapsed away — the backing field prints as its
    property, avoiding the Kotlin platform-declaration clash of a property *and* its `getX()`;
  - a single constructor whose parameters all name a field becomes the **primary constructor**
    (`class Foo(val id: Int)`); those fields and that constructor are then omitted from the body.

## Requirements / limitations (first slice)

- **Requires prepwork** for the accessor collapse (agreed restriction). Without it, a Kotlin-parsed type prints
  both the property and its `getX()` (a Kotlin clash).
- **Expression/statement coverage is incremental.** Handled: block, return, expression-statement, `val`/`var`,
  `if`/`else`, `while`/`do`/`for-in`/`throw`, `when`, `try`/`catch`/`finally`, `yield`; new/cast/instanceof/
  ternary/elvis/`!is`, lambdas, and the binary/logical/unary/negation operator families (operand recursion).
  Not yet: C-style `for`, try-with-resources, old-style (fall-through) `switch`; anything else falls back to the
  Java `print()`.
- **Language-specific hints live in `DetailedSources`.** The Kotlin parser records source-form markers there
  (e.g. `NULL_COALESCING` for elvis `?:`); a printer reaches them via `element.source().detailedSources()` and
  can reconstruct the idiomatic Kotlin form. This is the channel for things the (JVM-shaped) CST does not
  otherwise capture — nullability (`?`), `when` vs `if`, expression-body-ness, elvis, etc.
- Records print as `class` (not `data class`); `object` singletons, companion objects, `sealed`/`enum` bodies,
  annotations, and `val`-vs-`var` for locals (front-end does not always flag final) are best-effort / follow-ups.

## Example

```kotlin
// parsed from Kotlin `class Foo(val id: Int) { fun greet(name: String): String = "hi " + name }`,
// prepwork run, printed back as Kotlin:
class Foo(val id: Int) { fun greet(name: String): String { return "hi " + name; } }
```
