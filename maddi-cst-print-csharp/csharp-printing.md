# Printing the CST as C#

`maddi-cst-print-csharp` prints a CST, parsed from Java, as **C#** source. It is the C# counterpart of
`maddi-cst-print-kotlin` (see `kotlin-printing.md`). It reuses the language-neutral `OutputElement` IR and the
`maddi-cst-print` formatter unchanged. The work item is #115.

## Aim

The aim is automatic translation, with no manual editing, into C# that reads as if it had been written in C#. maddi's
analyses drive what makes the output idiomatic:

- **Nullability** (`NullabilityPass`, maddi-mod) decides where `?` goes under `#nullable enable`, and where `!`, `?.`
  or `??` appear.
- **Modification and immutability** decide whether something can be `readonly` or `init`-only, whether a parameter
  can be `IReadOnlyList<T>`, and whether a class can become a `record` or a `readonly struct`.
- **Prepwork** (`getSetField`) collapses getters and setters into properties.

None of these drivers is wired in yet. This first slice covers the syntax and the structure that follow from the
Java alone.

## The pluggable-printer seam

`CSharpTypePrinter`, `CSharpMethodPrinter` and `CSharpFieldPrinter` implement the cst-api `TypePrinter`,
`MethodPrinter` and `FieldPrinter` interfaces, including the factory overload, as the Java and Kotlin printers do.
`CSharpCompilationUnitPrinter` prints a file and returns `CSharpPrintMessage`s alongside the text. The state of one
file's printing (types and methods being printed, messages, `using`s) is in the thread-local `CSharpContext`, because
the factory signatures carry no context.

## Naming (`CSharpNames`)

Every name is a function of its declaration and of the whole program's facts (`CSharpProgram`), so a declaration and
all its uses agree without a renaming pass.

- A method named as its class (C# allows no such member) is `Of` when static, `<Name>Value` otherwise:
  `Metadata.metadata(k, v)` is `Metadata.Of(k, v)`.
- A type is qualified with its namespace where a member of the printed class (or of a class it is nested in, or of
  their superclasses) hides it (`VideoContent.Video()` hides `Video`), and outside its namespace where its simple
  name is ambiguous: declared by two namespaces of the program, or by the BCL (`CSharpProgram`).
- A namespace segment that is also the name of a type, of the program or of the BCL, is plural: the namespace
  `Dev.Langchain4j.Exception` would hide `System.Exception` from the code in `Dev.Langchain4j`, and
  `Dev.Langchain4j.Exceptions` is .NET's naming (`CSharpProgram`).
- A namespace is the package with each segment in PascalCase: `org.example.util` becomes `Org.Example.Util`.
- A translated method is PascalCase: `getName` becomes `GetName`. `toString`, `equals` and `hashCode` become
  `ToString`, `Equals` and `GetHashCode`. An override takes the name of the method it overrides.
- A translated interface gets an `I` prefix (`Visitor` becomes `IVisitor`) unless its name already has that shape.
- An enum constant or a record component is PascalCase: `NOT_FOUND` becomes `NotFound`, and component `x` becomes
  property `X`.
- An identifier that is a C# keyword is escaped: `System.out` becomes `System.@out`.
- A new name that would collide falls back to the Java name. It can collide with its enclosing type (C# forbids a
  member named after its type), with a field, or with a nested type.

Library declarations keep their Java names, except where the BCL mapping (below) translates them. A translated
`close()` of an `AutoCloseable` becomes `Dispose()`.

## Types (`CSharpTypeName`)

- Primitives map to C#'s, except that Java's signed `byte` becomes `sbyte`.
- `String` and `Object` become `string` and `object`.
- A boxed type becomes the nullable value type, `Integer` → `int?`. As a type argument it becomes the value type
  itself: `List<Integer>` → `List<int>`, because C# generics are not erased.
- In a pattern, a type has no `?`: `o instanceof Integer` becomes `o is int`.
- C# has no wildcards. `? extends T` and `? super T` become `T`. An unbound `?` becomes the type parameter's bound, so
  that the constraint still holds.
- C# has no raw types either. A raw `Key` becomes `Key<Bound>`.
- Bounds become `where T : …` constraints.
- `typeof(List<>)` is the unbound generic type.

## Declarations

- **Access.** Every declaration states its access, except the public members of an interface, where C# writes none.
  - Package access becomes `internal`, because a translated code base is one assembly.
  - `protected` becomes `protected internal`, because Java's `protected` also grants package access.
  - Access is read from the declared modifiers, not from `access()`. For fields and types, `access()` is combined
    with the enclosing type's access.
  - Java's `private` covers the whole top-level type; C#'s covers the type itself and its nested types.
    `CSharpAccess` finds the private members and nested types that code outside their owner reaches. Those, and only
    those, become `internal`.
  - An override keeps the access of the class method it overrides: Java may widen access, C# may not.
- **Anonymous classes.** One that does not become a lambda is hoisted (`CSharpAnonymous`): a private sealed nested
  class of the type whose code creates it, named after what it extends (`GraphImpl`). The local variables and
  parameters it uses become its constructor's arguments and its fields, and so does the enclosing instance, `outer`,
  when it uses one of its instance members; its fields' initializers move into that constructor, after the captures.
  Its superclass's constructor arguments come first. A generic method's type parameters become the class's.
- **Inheritance.**
  - A Java method can be overridden unless it is final, static or private. In a class that can be extended, such a
    method becomes `virtual`.
  - An override of a class method becomes `override`, or `sealed override` when it is final. Implementing an
    interface method needs neither.
  - A final class becomes `sealed`.
- **Classes.**
  - Java's utility-class idiom becomes a `static class`: static members only, and one constructor that is private,
    parameterless and empty.
  - A nested class never gets `static`.
  - `throws` is dropped.
  - A single-`return` body becomes an expression body, `=> expr;`.
  - A constructor's `super(…)` or `this(…)` becomes `: base(…)` or `: this(…)`.
  - A static initializer becomes the static constructor.
  - `synchronized` becomes `[MethodImpl(MethodImplOptions.Synchronized)]`.
  - Varargs become `params T[]`.
- **Fields.** A static final primitive or String with a constant initializer becomes `const`. Any other final field
  becomes `readonly`.
- **Enums.** An enum of constants only becomes a C# `enum`. One with fields, methods or constructors becomes a sealed
  class with a `public static readonly` instance per constant (`ENUM_AS_CLASS`), initialised with its `Name` and
  `Ordinal`, and with `Values()` and `ValueOf(string)`. On a C# enum, `values()` becomes `Enum.GetValues<T>()` and
  `ordinal()` a cast to `int`.
- **Nested types of generic types.** C# makes a nested type generic in its enclosing types' parameters, which a
  Java static nested type is not. A static nested type of a generic type is therefore printed beside its primary
  type, in the namespace, by its simple name; private members of the outer type it uses become `internal`.
- **Functional interfaces.** A translated functional interface is a C# `delegate` when the whole program allows it:
  one abstract method and nothing else, and no class or interface of the program implementing or extending it (an
  anonymous class that becomes a lambda does not count). Its lambdas are plain lambdas, a call `f.apply(x)` is an
  invocation `f(x)`, and a method reference `f::apply` is `f.Invoke`. `CSharpProgram` makes that decision once,
  over all the translated types, before the files are printed. Its policy can force the other form:
  `FunctionalInterfaces.ADAPTER`.
  Any other functional interface stays an interface and gets a nested adapter, `public sealed class
  Lambda(Func<Exprent, int> f) : IExprentIterator { … }`, around its lambdas and method references:
  `new IExprentIterator.Lambda(e => 0)`. A printer that is not given the program (`CSharpProgram.NONE`) uses this
  form for all of them. An anonymous class of either kind that only implements the method, without fields and
  without using itself, becomes a lambda.
- **Inheritance.** C#'s classes and methods are open only when declared so; Java's are open unless declared
  final. With the whole program (`CSharpProgram`), a class nothing extends is `sealed`, and a method nothing overrides
  is not `virtual`. The policy `Inheritance.OPEN_PUBLIC_API` keeps public classes and their public and protected
  methods open for code outside the program; `Inheritance.OPEN`, and a printer without the program, keep everything
  Java leaves open.
- **Annotations.** An annotation type of the program is a sealed attribute class, `ToolAttribute : Attribute`
  (`CSharpAttributes`): its elements are properties with their defaults, a `value` element also the constructor's
  parameter (`params` for an array), and `@Target` is `[AttributeUsage]`. Reading an element, `tool.name()`, reads
  the property, `tool.Name`. A use on a type, method, field or parameter is an attribute,
  `[Tool("Adds", Name = "add")]`; `@Deprecated` is `[Obsolete]`. Other JDK annotations have no C# counterpart, and
  those of other libraries are dropped.
- **Default methods.** A C# class does not inherit its interfaces' default methods: a call on a class goes through
  the interface, `((IResult) this).Failed()`.
- **Casts to type parameters.** C# casts to a type parameter only from `object`, an interface or another type
  parameter: `(T) this` in a self-typed builder is `(T) (object) this`.
- **Iterables.** A class implementing Java's `Iterable` is C#'s `IEnumerable<T>`: it gets a `GetEnumerator()` that walks
  its `Iterator()`, so that `foreach` and LINQ work on it.
- **Exposed types.** Java's public method may name a less accessible type, C#'s may not: a nested type is as visible
  as the members whose signatures name it (a record's components count as its properties), capped by the visibility
  of the members' own types, and so are the types it is nested in (`CSharpAccess`).
- **Subclasses of collections.** C#'s `List<T>` has no virtual methods. A class extending `ArrayList` extends the
  compatibility library's `JavaArrayList<E>`, a `List<E>` whose Java methods (`Add`, `Remove`, `AddAll`, `Clear`,
  `Clone`, …) are virtual. A call through a `List<E>`-typed reference reaches `List`'s method, not the override:
  `LIST_SUBCLASS`, a behaviour change. (Declaring Java's `List` as `IList<T>` would dispatch, but `IList<T>` is not an
  `IReadOnlyList<T>`, which the covariant `List<? extends T>` needs.)
- **Java's object protocol.** C#'s `object` has `ToString`, `Equals` and `GetHashCode` to override, not `clone`:
  Java's `clone()` is a method of its own, and `super.clone()` is `MemberwiseClone()`. The marker interfaces
  `Cloneable`, `Serializable` and `RandomAccess` are dropped.
- **Records.** A record becomes a positional `sealed record Point(int X, int Y)`, and `p.x()` becomes `p.X`. One with
  a canonical or compact constructor, which a positional record cannot have, declares get-only properties and that
  constructor. An accessor that implements an interface's method, `T response()`, is the property: the interface's
  method is implemented explicitly, `T IResult<T>.Response() => Response;`.

## Statements and expressions

C#'s statements, operators and precedence are mostly Java's, so most of the code prints as it does in Java. The
differences:

- A for-each loop whose body assigns the loop variable iterates over `vItem` and declares `v` as its copy: C#'s
  iteration variable is read-only.

- **Loops and statements.**
  - `for (T x : xs)` becomes `foreach (T x in xs)`.
  - `synchronized (o)` becomes `lock (o)`.
  - `assert c : m` becomes `Debug.Assert(c, m)`.
- **Labelled jumps.** These become `goto`. `break outer` jumps to `outer_break: ;` after the loop. `continue outer`
  jumps to `outer_continue: ;` at the end of its body, so the loop's update still runs.
- **Switch statements.**
  - A C# section may not fall through. A Java section whose end is reachable ends in `goto case X;` or
    `goto default;` to the next section, and the last section ends in `break;`.
  - `endsInJump` is conservative: a `break;` too many is only an "unreachable code" warning.
  - An enum case label is qualified: `case Color.Red:`.
  - An arrow switch statement becomes sections whose labels share the arm's statements.
- **Switch expressions.**
  - They become `sel switch { "a" or "b" => 1, _ => throw … }`.
  - An arm that is a block with `yield` becomes a lambda that is called at once:
    `new Func<T>(() => { …; return v; })()`. This is exact, because Java allows no `return`, `break` or `continue`
    out of a switch expression.
- **Try statements.**
  - Try-with-resources becomes stacked `using (T r = …)` statements, wrapped in `try` when there are catch or finally
    clauses.
  - A multi-catch becomes `catch (Exception e) when (e is A || e is B)`.
- **Local variables.** A local is declared `var` when Java says `var`, or when its initializer constructs exactly the
  declared type. Otherwise it keeps its type.
- **Type tests.** `instanceof` becomes `is`, `x instanceof T t` becomes `x is T t`, and `!(x instanceof T)` becomes
  `x is not T`.
- **String identity.** `==` between two Strings is identity in Java and content equality in C#, so it becomes
  `object.ReferenceEquals(a, b)`.
- **Lambdas and method references.**
  - A lambda becomes `(a, b) => …`.
  - A method reference becomes a method group (`Owner.Method`, `receiver.Method`). For an unbound receiver or a
    constructor it becomes a lambda: `(p0, p1) => p0.M(p1)` or `p0 => new T(p0)`.
- **Other expressions.**
  - `X.class` becomes `typeof(X)`.
  - `super.m()` becomes `base.M()`.
  - An array's `length` becomes `Length`.
  - Diamonds get their type arguments written out.
  - `>>>` is C# 11's.

## `using` directives

Java imports types and C# imports namespaces. The directives follow from the types the printed code names by their
simple names, which `CSharpTypeName` records:

- A type in another namespace needs `using Namespace;`.
- A nested type from another file, and a Java static import, need `using static Namespace.Type;`, which brings that
  type's static members and nested types into scope.
- The BCL namespaces the printed code needs (`System.Diagnostics` for `Debug.Assert`, `System` for `Func` and
  `Exception`) are added too.

The file uses a file-scoped `namespace X;`.

## Messages (`CSharpPrintMessage`)

Every message has a severity: INFO, BEHAVIOUR_CHANGE, LOSS or ERROR. An ERROR means the file will not compile.

- **Not translated yet:**
  - local classes that capture a local variable, a parameter or the enclosing instance. One that captures nothing
    is lifted: printed as a private nested type of the enclosing type (`CSharpLocalTypes`);
  - instance initializers;
  - `Outer.this`, because a C# nested class has no enclosing instance;
  - `new int[a][b]`;
  - record patterns;
  - inner (non-static) classes of a generic type (`NESTED_IN_GENERIC`): C# names them `Outer<E>.Inner`. A static
    one is hoisted (see Declarations).
- **Recorded as losses:** wildcards and raw types, and a record's constructor of as many parameters as components
  that is not its canonical one.
- **Unknown forms:** a form the printer does not know prints as Java, with `JAVA_FALLBACK`.

## The JDK → BCL mapping (`CSharpBcl`)

The translated code uses the BCL, not a port of the JDK. The table follows fernflower's JDK use: the census in the
ratchet's report (see below) lists what is left.

- **Collections.** `List`, `ArrayList`, `LinkedList` and the deques become `List<T>`. `Map` becomes
  `IDictionary<K, V>` and `Set` becomes `ISet<T>`, because Java implements them with hash and tree collections
  alike. `HashMap` becomes `Dictionary<K, V>`, `LinkedHashMap` the insertion-ordered `OrderedDictionary<K, V>`,
  `TreeMap` and `EnumMap` `SortedDictionary<K, V>`, and the hash sets `HashSet<T>`. Where the modification analysis
  proves a collection unmodified, a read-only interface (`IReadOnlyList<T>`) is the analysis's refinement. `Collection` becomes `ICollection<T>`, `Iterable` becomes `IEnumerable<T>`, and `Map.Entry`
  becomes `KeyValuePair<K, V>`.
- **Wildcards.** Java cannot add to a `List<? extends Node>`: it is C#'s covariant read-only interface,
  `IReadOnlyList<Node>`, which a `List<Block>` is. `Collection` and `Set` of `? extends T` are
  `IReadOnlyCollection<T>`, `Iterable` is `IEnumerable<T>`. Any other wildcard is its bound (`WILDCARD_AS_BOUND`).
- **Streams and Optional.** Streams become LINQ over `IEnumerable<T>`: `filter`/`map`/`collect(toList())` become
  `Where`/`Select`/`ToList()`. `Optional<T>` becomes the value itself or null: `orElse(x)` becomes `?? x`.
- **Functional interfaces.** These become delegates: `Function<T, R>` → `Func<T, R>`, `Predicate<T>` →
  `Func<T, bool>`, `Runnable` → `Action`, `Comparator<T>` → `Comparison<T>`. Calling their method becomes an
  invocation: `f.apply(x)` becomes `f(x)`.
- **Exceptions.** These become the BCL's: `IllegalStateException` → `InvalidOperationException`, `RuntimeException`
  → `Exception`, and so on. Their constructor of a cause alone becomes `(cause?.ToString(), cause)`, Java's message.
- **Conversions.** C# does not unbox implicitly: an `Integer` (`int?`) where an `int` is expected (an assignment, a
  compound one too, an argument, a return value, a conditional's branch, an arithmetic operand) gets a cast, and
  `(Boolean) o` used as a condition becomes `(bool) o`. A lambda's parameter is the delegate's value type already. A
  `char` switch's `int` labels are `char` literals. Java's `null` where a type parameter is expected becomes
  `default`.
- **Static members.** C# finds a static member by its simple name only in the type itself, the types it is nested
  in, and their base classes. Any other, a static import or an interface's constant used in an implementing class,
  is qualified with its type: `ExitExprent.EXIT_THROW`.
- **Members.** A member rule is a template keyed by the declaring type, the name and the arity, or by the parameter
  types where Java overloads on them: `List.get/1` → `$0[$1]`, `String.substring/2` → `$0[$1..$2]`,
  `List.remove(int)`.
  - A call matches the rule of the method or of a method it overrides, the most specific type first.
  - Where Java's method returns a value C#'s does not, the rule has a second template for a call whose value is
    unused, which is the idiomatic one: `map.put(k, v);` becomes `map[k] = v;`.
  - A constructor has a rule keyed `owner.new/arity`: `new FileOutputStream(f)` becomes
    `new FileStream(f.ToString(), FileMode.Create)`.
  - A method reference to a mapped member becomes a lambda around its template.
  - A template whose result is not an atom (`list.Count == 0`) is parenthesised as an operand.
  - Java's sorts are stable, `List.Sort` is not: `list.sort(c)` and `Collections.sort` are the compatibility
    library's stable sort. `Collections.reverse` reverses in place (LINQ's `Reverse` is a new sequence).
  - `String`'s `indexOf`, `startsWith` and `endsWith` compare ordinally, as Java's do. Case conversions are
    invariant.

### The compatibility library (`Maddi.JavaCompat`)

`JavaCompat.cs`, a resource of this module, is C# that goes with the translation. Its parts:

- Extension methods with Java's behaviour where it differs in a way that can be observed: `Map.put` returns the
  previous value, `Deque.removeFirst` the element, `String.split` takes a regular expression and drops trailing
  empty strings, and `String.format`'s conversions differ from .NET's.
- The classes the BCL lacks: `DataInputStream` (big-endian), `BitSet`, the byte-array streams, `IJavaIterator<T>`
  (Java's `Iterator`, which the program's iterators implement; `JavaIterator<T>` walks a C# collection, with
  `remove`), `JavaFile` (`java.io.File`), `JavaMatcher` (a `Regex` applied step by
  step, as `java.util.regex.Matcher`), `JavaEnumeration<T>`, and `java.util.zip`/`java.util.jar` over
  `System.IO.Compression` (`JavaZipFile`, `JavaJarFile`, `JavaZipEntry`, `JavaZipOutputStream`, `JavaManifest`).
- Java's `byte[]` is `sbyte[]` in C#, so these classes take and give `sbyte[]` and reinterpret it as `byte[]` for the
  BCL.

The translation calls the library only where it needs that behaviour, so idiomatic code does not depend on it.

## The ratchet

`TestJavaToCSharpFernflower` (maddi-run-openjdk, `slowTest`) translates fernflower's main sources, and
`TestJavaToCSharpLangchain4j` langchain4j's core (records, builders, default methods, annotations, `Optional`,
streams), the Kotlin printer's second corpus too. Each judges its translation
with `tools/csharp-check`, a small .NET tool on Roslyn that `JavaToCSharpRatchet` builds with `dotnet build`, so a
.NET 10 SDK must be on the `PATH`. The tool parses each file on its own for the syntax errors, then compiles the
syntax-clean files together against the BCL. The numbers are held in `src/test/resources/j2cs/<corpus>.ratchet`:
printer crashes, syntax errors, syntax-clean files, compiling files (files without an error in that compilation),
and `unmappedJdkUses`.

The printer reports every JDK type or member it prints without a BCL counterpart as `UNMAPPED_JDK`. Their count is
ratcheted, and `build/j2cs/fernflower/report.txt` lists them by frequency. That list, together with the names and
members the compiler does not know, is the BCL mapping's work list. The error total is reported but not ratcheted:
as the mapping lets the compiler bind more, it gets to report errors it could not see before.

## Status (first slice)

`TestJavaToCSharpTranslation` has one test per rule. It writes each sample to `build/csharp-samples/`, and all of
them compile with `dotnet build` (.NET 10) apart from their JDK references. A smoke run over fernflower's 199 main
files gives:

- no printer crash and no Java fallback;
- **no syntax error** from Roslyn;
- 1,189 errors in all, 1,169 of them JDK types C# does not know, which is the BCL mapping's work;
- 20 others:
  - the generic-nested types listed above (12);
  - `clone()` overrides, for the BCL mapping (2);
  - three shapes that need structural work: a record accessor that implements an interface method (3), a covariant
    return in an interface implementation (1), and a public member whose signature names a package-private nested
    type (2), which C# rejects as inconsistent accessibility.

Next, in #115's order:

1. A `JavaToCSharpRatchet` on fernflower.
2. The JDK → BCL mapping (types, members, exceptions, `AutoCloseable` → `IDisposable`, `Iterable` →
   `IEnumerable`, functional interfaces → delegates).
3. The hoisting of anonymous, local and generic-nested types.
4. The analyses' verdicts.
