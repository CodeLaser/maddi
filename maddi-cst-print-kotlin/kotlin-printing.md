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
- **Enum constants** — the enum's own static-final self-typed fields print as Kotlin entries (`RED, GREEN, BLUE`,
  with `(args)` when a constant has constructor arguments), `;`-terminated when other members follow — not as
  `val RED = Color()` properties.
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
  (statement and expression, arms unwrapped); `try`/`catch (e: T)`/`finally`; labels (`outer@ for`,
  `continue@outer`); `synchronized(x) { }`; `assert(c) { msg }`; local classes. Java forms without a Kotlin
  counterpart (`KotlinStatementPrinter`'s javadoc has the details):
  - **C-style `for`** → `for (i in a until n)` / `downTo` / `step` when that is the same loop (one integral
    counter the body does not assign, a bound it cannot change: a range reads its bound once); otherwise
    `run { init; while (cond) { body; updates } }`, and when the body `continue`s, a `while (true)` that runs
    the updates at the top of every iteration but the first;
  - **old-style `switch`** → `when`: a falling-through case gets the following cases' statements copied, a
    case's final `break` goes, a `break` in the middle becomes `return@label` out of a `run label@{ }` around the
    `when` (a Kotlin `break` there would leave the enclosing loop); enum case labels are qualified;
  - **try-with-resources** → `resource.use { r -> … }`, nested per resource, inside a `try` when there are
    `catch`/`finally` clauses; **multi-catch** → one `catch` per type;
  - **constructors** → `constructor(…) : super(…)` / `: this(…)` from the explicit constructor call; the
    superclass is called in the header (`: Base()`) only when there is no secondary constructor.
- **Expressions without a Kotlin counterpart** — `(int) l`→`l.toInt()` (a primitive cast converts; `as` would
  throw), `(int) c`→`c.code`; `&`/`|`/`^`/`<<`/`~`→`and`/`or`/`xor`/`shl`/`inv()`; `==` on references→`===`;
  `new int[n]`→`IntArray(n)`, `new T[n]`→`arrayOfNulls<T>(n)`, `{1, 2}`→`intArrayOf(1, 2)`; `X.class`→
  `X::class.java`; an assignment used as a value→`v.also { x = it }`; an anonymous class→`object : T(…) { }`;
  `super.m()` and `Outer.this` keep their qualifier (`super.m()`, `this@Outer`); `$` in literals is escaped.
- **Names** — a Java identifier that is a Kotlin hard keyword (`fun`, `in`, `object`, …) is written in
  backticks; a field without initializer gets Java's default (`= 0`, `= false`) or `lateinit`; a non-final,
  non-private method of an open class is `open`.
- **Line breaks** — the formatter may break *after* a binary operator and never before one: Kotlin ends a
  statement at a newline in front of `+` or `or` (`SpaceEnum.ONE_NO_SPLIT_BEFORE`, `KotlinSymbols`).
- **Lambdas** — `{ p1, p2 -> body }` (single-expression body inlined); a block body's last `return x` is the value
  `x`, any other `return` is `return@lambda` out of a `lambda@ { }`.
- **Structure** (Java source only; a type parsed from a `.kt` file keeps its own shape) —
  - static members go to a `companion object` (`const val` for a constant primitive or String, `@JvmStatic` on a
    protected method); a static member written unqualified is qualified with its owner unless that owner, or one
    around it, is being printed: Kotlin inherits no statics. A static of a JDK type Kotlin maps is reached by its
    Java name (`java.lang.Integer.parseInt`);
  - one constructor becomes the primary constructor, its `super(…)` the superclass call, its body an `init` block
    (where final fields may be assigned); a field of the same name as a parameter is `this.x` in initializers.
    With more constructors, final fields without initializer become `var`s with Java's default or `lateinit`;
  - a record is a `data class` with its components as `val`s; `p.x()` is `p.x`;
  - a non-static nested class is `inner`; a private member or nested type of a nested class is `internal`
    (Java lets the outer class see it);
  - `x instanceof T t`: `t` prints as `x` (smart cast) when `x` is a local or parameter, else as `(x as T)`;
  - a parameter the body assigns gets `var p = p`; `equals(Object)` overrides with `Any?`.
- **Types and members Kotlin maps** — `java.util.List`/`Map`/`Set`/`Collection`/`Iterator` print as
  `MutableList`/… and are not imported; `s.length()`→`s.length`, `c.size()`→`c.size`, `m.entrySet()`→
  `m.entries`, `e.getKey()`→`e.key`, `n.intValue()`→`n.toInt()`, `s.charAt(i)`→`s[i]`, `list.remove(int)`→
  `removeAt`, `s.replaceAll(r, x)`→`s.replace(r.toRegex(), x)`, `equalsIgnoreCase`, `getFirst`… (`KotlinMappedMembers`).
- **Functional interfaces** — a Java functional interface is a `fun interface`; a lambda that is not an argument
  names its interface (`Runnable { … }`).
- **Implicit conversions** — Java widens `short`→`int`, `int`→`long`, `char`→`int` silently; Kotlin does not, so
  arguments, assignments, initializers, `==` and arithmetic on a `char` get the conversion. A `when` statement over
  an enum or boolean gets `else -> {}`: Kotlin requires it to be exhaustive.
- **Nullability** — the printer does not decide it; `KotlinPrintOptions` carries `NullabilityVerdicts` (fields,
  parameters, returns, locals by declaring element), computed by maddi-mod's `NullabilityPass`. A NULLABLE verdict
  is the `?` on the declaration; a nullable field without initializer is `= null`. Where Kotlin types a value as
  nullable (a NULLABLE declaration, `Map.get`, `Queue.poll`, …) and its use needs it non-null (a receiver, an
  argument for a non-null parameter of translated code, a non-null return or declaration), the `NullCheck`
  policy writes `x!!` (`ASSERT`, the default) or `x?.m()` on a receiver (`SAFE_CALL`). Without verdicts every
  declaration is non-null, as Kotlin reads a platform type. An `Integer` overload of an `int` one takes `Int?`.
- **Files** — `KotlinCompilationUnitPrinter`: `package`, the imports the import computer finds (a static import
  is an ordinary Kotlin import), and the types.
- **Idioms via structure** — `!(x is T)`→`x !is T`; an `else` branch that is a lone `if/else` flattens to
  `else if …` (not `else { if … }`); and elvis `a ?: b` recovered from the desugared `InlineConditional`
  marked `NULL_COALESCING` in `DetailedSources` (rather than `if (a == null) b else a`).
- **Idiomatic reconstruction** (needs the analyzer's **prepwork** phase, which populates `getSetField`):
  - getter/setter methods (non-empty `getSetField`) are collapsed away — the backing field prints as its
    property, avoiding the Kotlin platform-declaration clash of a property *and* its `getX()`;
  - a single constructor whose parameters all name a field becomes the **primary constructor**
    (`class Foo(val id: Int)`); those fields and that constructor are then omitted from the body.

## Round-trip validation

`TestKotlinPrinterRoundTrip` parses a construct-rich **Java** class with the openjdk front-end, runs prepwork,
and prints it as Kotlin — a scale check beyond hand-picked snippets. Real Java comes out idiomatic:

```kotlin
open class Rich<T> (val id: Int, var name: String) {               // non-final class -> open; final field -> val, mutable -> var
    fun check(x: Any): Boolean = x is String && (x as String).length() > 0   // instanceof/cast nested in &&
    fun pick(n: Int): String = if (n > 0) "pos" else "neg"          // ternary -> if/else expression body
    fun sum(xs: List<Int>): Int { … for (x in xs) { … } … }
    fun risky() { try { throw RuntimeException("x") } catch (e: RuntimeException) { … } }
    fun adder(k: Int): Function<Int, Int> = { x -> x + k }          // lambda
}
```

Further classes guard the constructs the first did not reach — an `interface` (abstract + `default` methods),
an `enum` (`enum class Color { RED, GREEN, BLUE; … }`), and an operator/array grab-bag (`||`, `!`, `%`, string
concat, `int[]`→`Array<Int>`) all come out idiomatic. A second class (`Calc`) covers:

```kotlin
open class Calc {
    fun describe(n: Int): String = when (n) { 0 -> "zero"; 1 -> "one"; else -> "many" }  // arrow switch -> when
    fun notString(x: Any): Boolean = x !is String                                        // !(x instanceof T) -> x !is T
    fun rounds(n: Int): Int { … do { … } while (n > 0) … }                               // do-while
    fun grade(score: Int): String { if (…) { … } else if (…) { … } else { … } }          // else-if chain flattens
    fun <U> identity(u: U): U = u                                                        // generic method
    fun counts(): Map<String, Int> = HashMap<String, Int>()                              // diamond filled + JDK type mapped
}
```

(Both examples predate the Java-only rules above: today the methods of an open class are `open`, and Java's `Map`
is a `MutableMap`.)

## Requirements / limitations (first slice)

- **Requires prepwork** for the accessor collapse (agreed restriction). Without it, a Kotlin-parsed type prints
  both the property and its `getX()` (a Kotlin clash).
- **Syntax coverage is complete for fernflower** (maddi-run-openjdk's `TestJavaToKotlinFernflower` compiles the
  translation with kotlinc and ratchets the result: 0 syntax errors in 199 files, 73 of which compiled against
  fernflower's own classes; 84 with the nullability seam and no verdicts). Most of what keeps the rest from
  compiling is `null` into a declaration no verdict made nullable. Also not done: wildcard-typed overrides (`addAll(Collection<? extends E>)`), Java's
  `String.split` semantics.
- **Language-specific hints live in `DetailedSources`.** The Kotlin parser records source-form markers there
  (e.g. `NULL_COALESCING` for elvis `?:`); a printer reaches them via `element.source().detailedSources()` and
  can reconstruct the idiomatic Kotlin form. This is the channel for things the (JVM-shaped) CST does not
  otherwise capture — nullability (`?`), `when` vs `if`, expression-body-ness, elvis, etc.
- `object` singletons, companion objects, `sealed` hierarchies, annotations, and `val`-vs-`var` for locals
  (front-end does not always flag final) are best-effort / follow-ups.

## Example

```kotlin
// parsed from Kotlin `class Foo(val id: Int) { fun greet(name: String): String = "hi " + name }`,
// prepwork run, printed back as Kotlin:
class Foo(val id: Int) { fun greet(name: String): String { return "hi " + name; } }
```
