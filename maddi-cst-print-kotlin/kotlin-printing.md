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
  omitted; `override` when the method overrides a supertype method.
- **Type references** — JVM primitives/JDK types mapped to Kotlin (`int`→`Int`, `java.lang.String`→`String`,
  `java.lang.Object`→`Any`, …); arrays → `Array<…>`; generics recurse.
- **Idiomatic reconstruction** (needs the analyzer's **prepwork** phase, which populates `getSetField`):
  - getter/setter methods (non-empty `getSetField`) are collapsed away — the backing field prints as its
    property, avoiding the Kotlin platform-declaration clash of a property *and* its `getX()`;
  - a single constructor whose parameters all name a field becomes the **primary constructor**
    (`class Foo(val id: Int)`); those fields and that constructor are then omitted from the body.

## Requirements / limitations (first slice)

- **Requires prepwork** for the accessor collapse (agreed restriction). Without it, a Kotlin-parsed type prints
  both the property and its `getX()` (a Kotlin clash).
- **Bodies reuse the Java block/expression printing** — valid Kotlin (semicolons are accepted), but not
  idiomatic (`{ return x; }` rather than an expression body `= x`), and Java-only constructs in bodies (`instanceof`,
  ternary, casts, `new`) are not yet translated. A Kotlin expression/statement printer is the next increment.
- Nullability is not tracked in the CST → no `?`. Records print as `class` (not `data class`), `object`
  singletons, companion objects, `sealed`/`enum` bodies, annotations: best-effort / follow-ups.

## Example

```kotlin
// parsed from Kotlin `class Foo(val id: Int) { fun greet(name: String): String = "hi " + name }`,
// prepwork run, printed back as Kotlin:
class Foo(val id: Int) { fun greet(name: String): String { return "hi " + name; } }
```
