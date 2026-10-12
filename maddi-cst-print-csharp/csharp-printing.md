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

### Facts from the analysis (`CSharpFacts`, `CSharpReadOnly`)

The modification and nullability analyses can say more than Java's types do. The printer asks a `CSharpFacts`
(`Policy.facts`); the default, `STORED`, answers from what maddi-mod's analyzer stored on the declarations, which is
nothing when no analysis ran, so that the output is then the same as without facts. A client with a report of its own
implements the interface.

- A collection parameter that the method does not modify becomes read-only: `List<T>` → `IReadOnlyList<T>`,
  `Collection<T>` → `IReadOnlyCollection<T>`, `Set<T>` → `IReadOnlySet<T>`, `Map<K, V>` →
  `IReadOnlyDictionary<K, V>`. A caller's `List<T>` is one, so the calls do not change.
  - The C# has to compile whatever the analysis's precision, so the parameter's uses decide too: each must be a call of
    a library method on it, the collection of a `foreach`, or an argument where any enumerable is accepted (another
    read-only parameter, a copying constructor, `addAll`). A use that stores or returns it keeps Java's type.
  - The callers have to compile too. Java's `List` family prints as the class `List<T>`, which implements the
    read-only interfaces, but `Map`, `Set` and `Collection` print as `IDictionary`, `ISet` and `ICollection`, which do
    not. Each argument must be of a class whose C# counterpart implements the read-only interface (`HashMap` →
    `Dictionary`, `TreeSet` → `SortedSet`, …), `null`, or a read-only parameter itself.
  - Every method of an override family has the parameter read-only, or none does. Delegates, a functional interface's
    method, records and a method that a method reference names keep Java's types.
  - The calls on a read-only collection are those of the read-only interface: `contains` uses LINQ, `indexOf` is
    `JavaCollections.IndexOf`, `subList` is `Skip`/`Take`, `get` on a map is `GetValueOrDefault` (or a `ContainsKey`
    test when the value is a value type).
- A field, parameter or method return that the nullability analysis finds nullable has the type `T?`. Value types
  (primitives, simple enums, the BCL's structs such as `KeyValuePair`), a method's own type parameters and types that
  already end in `?` are left alone: on a value type `?` would make it `Nullable<T>`, another type. A class's type
  parameter takes it, `T? held`: an unconstrained `T?` is `T` itself for a value type. So is a library type without a
  mapping (UNMAPPED_JDK): C# reads its `X?` as `Nullable<X>`, and every use of the variable becomes another error.
- A nullable parameter or result of a method's own type parameter's type is `T?` in a static method: C# infers `T`
  without the `?` from an argument that may be null, so that langchain4j's `EnsureNotNull<T>(T? o, …)` returns a
  non-null `T`. Not in another method: an override's `T?` is `Nullable<T>` unless it says `where T : default`.
- A field the analysis finds non-null that C# cannot see assigned in the constructor gets `= null!`: the `Name` of a
  class translated from an enum, which each constant's initializer sets, and an attribute's required property (an
  annotation element without a default).
- When the analysis covered the whole program under a null-marked policy (`CSharpFacts.nullabilityAnalysed()`,
  `STORED_NULL_MARKED`), every other declaration is not null, and C# checks it: the translation's project file has
  `<Nullable>enable</Nullable>`. A local's explicit reference type is then `T?`, as `var`'s is: the compiler follows
  its null state through the flow, so that the declaration does not have to know it. The ratchet counts the
  compiler's nullable warnings (`nullableWarnings`); maddi-mod's `fernflower-facts` ratchet holds them.
- A dereference of a value that may be null has a `!` (`CSharpNullability`): `x!.M()`, `x!.f`, `a![i]`,
  `foreach (… in xs!)`. Java throws a NullPointerException there when it is null, and C# a NullReferenceException; the
  `!` only tells the compiler so (CS8602), it does not throw. A value may be null when it is the result of a call
  whose method may return null (unless the analysis knows this call's is not), or a field, parameter or local the
  analysis finds nullable, unless its use-site facts know it is not null where the statement or the call starts (after
  `if (v == null) return;`, inside `if (v != null)`), which C#'s flow analysis knows too. The facts for locals and use
  sites come from the analysis's report, through `CSharpFacts`; a local the analysis asserts at its declaration
  (`assertedAtDeclaration`) has its initializer asserted instead: `var block = blocks.GetWithKey(j)!;`.
- An argument that may be null, for a parameter that is not nullable, has a `!` too (CS8604): `list.Add(x!)`. Java
  passes the null on, and so does C#. A program's parameter is nullable when the analysis says so, a library's when
  its hints do. C# prints no `?` in a type argument (`List<Exprent>`), so that a collection's element or key is not
  nullable whatever the hints: Java's `contains(Object)` takes null, C#'s `Contains(T)` does not, and even a `null`
  there is `null!`. This is the write the analysis asserts under its Kotlin policy (`assertContentWrites`).
- The same holds for the other places a value goes (CS8603, CS8625, CS8601): a `return` of a value that may be null,
  or of `null`, from a method the analysis does not find nullable, or from a lambda, whose result is a delegate's type
  argument; a write into an array's element, which C# prints without `?` (`arr[i] = null!`, `{ x, null! }`: the array
  holds the null, as Java's does); an assignment to a field the analysis finds non-null.
- A reference cast of a value that may be null is to `T?`, `(Exprent?) o`: the cast lets the null through, in C# as
  in Java (CS8600). So is a cast of `null`, which picks an overload: `(AiMessage?) null`.
- The result of a library call may be null where C# declares it so, whatever the analysis says of the Java method:
  `object.ToString()` is `string?`, Jackson's `node.get(k)` is `node[k]`, a `JsonNode?`, and reflection's lookups
  (`GetConstructor`, `GetMethod`, `GetElementType`, `Invoke`) return null where Java throws. The printer knows these
  from the mapping's C# template (`CSharpExpressionPrinter.nullableResult`).
- An unboxing cast, `(int) x`, has a `!` on a value that may be null (CS8629, CS8605): `(int) map.GetValueOrNull(k)!`.
  Java's unboxing throws a NullPointerException there, C#'s cast an InvalidOperationException. An `Integer` is C#'s
  `int?` whatever the analysis says of its declaration, so a variable has the `!` unless a use-site fact makes it
  non-null, and a call unless its result is the value type itself (`List<int>`'s `l[0]`) and not nullable.

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
- **Inner classes.** A Java inner class, a non-static member class, has an enclosing instance; a C# nested class
  has none. One that uses it (`CSharpInner`) gets it as a field, `private readonly Range outer;`, which every
  constructor takes first and passes on (`: this(outer, …)`, `: base(outer, …)` to a superclass that takes one too).
  The enclosing instance's members are used through it (`outer.to`, `outer.Width()`), `Range.this` is `outer`, and a
  creation passes it: `new Upcoming(this)` in the enclosing type's code, `new Upcoming(outer)` in an inner class's,
  `new Upcoming(other)` for `other.new Upcoming()`. Its instance fields' initializers move into the constructors,
  after `this.outer = outer;`, where Java runs them: a C# field initializer cannot use `outer` (CS0236).
  So do a class's whose initializer uses `this`, explicitly or through an instance member
  (`getLogger(getClass())`, CS0027): Java runs them after `super(…)`, C# before the base constructor.
- **Switch on an enum C# declares as a class.** Its constants are static instances, not constants, so a case is a
  property pattern on the instance's `Name`, the Java constant's: `case { Name: "FAST" }:`, `{ Name: "FULL" } => …`
  (CS9135). A fall-through into such a section, which `goto case` cannot name, jumps to a statement label at its
  start: `goto case_FULL;` … `case_FULL: ;`.
- **Type arguments C# cannot infer.** Java infers a method type parameter that no parameter's type mentions,
  `<V> V getStore(int i)`, from the call's target; C# does not (CS0411). The call gets all its type arguments as Java
  inferred them, `tuple.GetStore<Bucket<…>>(i)`, unless one is not a type C# can write.
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
- **Properties.** A getter that only returns a field of its class, of the same type, becomes the property that
  takes the field's place (`CSharpProperties`, whole-program; `Policy.accessors` can keep Java's methods).
  - `getName()`, `isDone()` and `name()` become `Name`, `Done` and `Name`. `x.getName()` becomes `x.Name`, and every
    reference to the field becomes a reference to the property.
  - A setter `this.name = name` becomes the property's `set`, and `x.setName(v)` becomes `x.Name = v`. A field
    assigned only in its class's constructors gets `{ get; }`, even when it isn't `final`. Any other field gets
    `{ get; private set; }`, with the access the field's writes need.
  - The property is as visible as its getter, and an accessor that is less visible says so. A getter that outside
    code may override, as the policy has it, makes the property `virtual`.
  - The getter's annotations move to the property. One whose attribute only applies to methods goes on the
    accessor: `{ [Experimental] get; }`.
  - A lone getter qualifies only when nothing overrides it, it overrides nothing, no method reference names it, and
    its name is not taken in its class, the classes above it, or those that extend it.
  - Methods that override one another are one property when all of them are parameterless with one return type,
    in classes and interfaces that are not delegates:
    - an interface's method becomes `T Name { get; }`, or `T Name => expr;` with a default body;
    - an abstract one becomes `abstract T Name { get; }`, also where an abstract class leaves an interface's
      property to its subclasses;
    - a getter of a field takes the field's place;
    - any other body becomes a computed property: `T Name => expr;`, or `T Name { get { … } }`.
  - A functional interface's lambda adapter implements its property: `public double Area => f();`.
  - Records keep their positional properties, and enums their methods.
- **Enums.** An enum of constants only becomes a C# `enum`. One with fields, methods or constructors becomes a sealed
  class with a `public static readonly` instance per constant (`ENUM_AS_CLASS`), initialised with its `Name` and
  `Ordinal`, and with `Values()` and `ValueOf(string)`. On a C# enum, `values()` becomes `Enum.GetValues<T>()` and
  `ordinal()` a cast to `int`.
- **Nested types of generic types.** C# makes a nested type generic in its enclosing types' parameters, which a
  Java static nested type is not. A static nested type of a generic type is therefore printed beside its primary
  type, in the namespace, by its simple name; private members of the outer type it uses become `internal`. When
  another type of the namespace has that name too, the name is prefixed with its enclosing types' names
  (`EmbeddingStoreRequestContextAdd`), a whole-program fact of `CSharpProgram`.
- **Static members of generic types.** C# reaches a generic class's static members through a constructed type,
  `Response<T>.From(x)`; Java through the class, `Response.from(x)`. Java's static members never use the type's
  parameters, so they are declared in a non-generic static class of the same name beside the generic type, the C#
  idiom of `Tuple` and `Tuple<T>`: `public static class Response { public static Response<T> From<T>(T content)
  … }`, which `Response.From(x)` calls as Java does. The generic type's own code qualifies them (`Response.Count()`);
  the private members either side reaches become `internal`, as the two are siblings. A static member whose code
  declares an anonymous or local class keeps the type without a companion.
- **Type tokens.** Java passes a `Class<T>` where C# has the type argument: `<T> T fromJson(String json, Class<T>
  type)` is `T FromJson<T>(string json)`, its body reads `typeof(T)` for `type`, and `fromJson(s, Foo.class)` is
  `FromJson<Foo>(s)`. `CSharpProgram` decides it over the whole program: `T` is the method's only type parameter,
  every call passes a `Class<X>` of a known `X` (not a `Class<?>`), no method reference names the method, and the
  methods it overrides or that override it qualify as well. Otherwise the token stays a `Type` parameter.
- **Member names.** C# has no member named as its type, a type parameter of its type, or a nested type of its
  type. A method named as its class is `Of` (static) or `PascalValue`; a type parameter named as a member takes the
  C# convention's `T` prefix (`EmbeddingMatch<TEmbedded>` with `Embedded()`); a method named as a nested type, with
  a field of its Java name, is `PascalValue` (`ResultValue()` beside `enum Result` and field `result`).
- **Interface implementations.** C# is stricter than Java in three places:
  - a method may implement an interface's method with a narrower return type in Java; C# implements the interface's
    method explicitly as well, calling the method: `IFailure IFailure.WithCode(int code) => WithCode(code);`;
  - a default method that implements an inherited interface's method is an explicit implementation:
    `Type IListener<string>.EventClass() => typeof(string);`;
  - an abstract class that leaves an interface's method to its subclasses redeclares it,
    `public abstract string Embed(string text);`, and the subclasses' implementations are `override`s.

  A generic interface method (Java may implement `<T> Class<T> eventClass()` raw) is left as it is.
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
  - `Outer.this` where no `outer` leads to it (an inner class's and a hoisted anonymous class's are translated);
  - `new int[a][b]`;
  - record patterns;
  - inner (non-static) classes of a generic type (`NESTED_IN_GENERIC`): C# names them `Outer<E>.Inner`. A static
    one is hoisted (see Declarations).
- **Recorded as losses:** wildcards and raw types, and a record's constructor of as many parameters as components
  that is not its canonical one.
- **Unknown forms:** a form the printer does not know prints as Java, with `JAVA_FALLBACK`.

## Library mappings (`LibraryMapping`, `CSharpLibraries`)

What a library's types, members and fields become in C# is a `LibraryMapping`. The JDK's is `CSharpBcl.JDK`; slf4j
and Jackson are other libraries with mappings of their own. A translation asks an ordered list of mappings,
`CSharpLibraries`, carried by `CSharpProgram.Policy`, and the first one that answers wins. Every method of a mapping
answers null for what it does not know. The list is how a client decides:

- **Choose an option of a library** by putting that mapping in the list. The id names the library and the option:
  `slf4j:ms-logging`.
- **Override single rules** with a small mapping in front: `CSharpLibraries.DEFAULT.withFirst(mine)`.
- **Leave a library untranslated** by leaving out its mapping. Its uses keep their Java names, with an
  `UNMAPPED_JDK` message.

The printer has options per library, chosen with `CSharpLibraries.choose("slf4j=serilog,jackson=none")`. The first
option of each library is the default, and `none` leaves the library out:

| library | options | |
|---|---|---|
| slf4j | `ms-logging`, `serilog` | `ILogger` from Microsoft.Extensions.Logging, or Serilog's. A message's `{}` holes become a message template's holes, named after their arguments (`"output: {}", x` → `"output: {X}"`). A trailing exception becomes the first argument. With `ms-logging`, `getLogger` is the compatibility library's `JavaLogging`, which the application connects to its host's `ILoggerFactory`. |
| jackson | `system-text-json`, `newtonsoft` | An `ObjectMapper` becomes the options of `JsonSerializer` (or `JsonConvert`), and the tree model becomes `JsonNode` (or `JToken`). Annotations become attributes, on the targets C# allows: `@JsonSubTypes` → `[JsonDerivedType]`, a class-level `@JsonInclude` → an option or `[JsonObject]`. Jackson's configuration of its framework (modules, introspectors, custom serializers, builders) stays unmapped. |
| jdk | `bcl` | The BCL, plus the compatibility library where Java's behaviour differs. |

A library's annotation becomes what its mapping makes of it (`annotation(use, target, print)`, or `annotation` lines in
a file, with the guards `?on=field` and `?element=VALUE`). Without a mapping, it is dropped.

A mapping is Java (`LibraryMapping`) or text, a `.csmap` file that `CSharpMappingFile` reads: one directive per
line. This is how a client overrides rules without Java:

```
mapping client:uuid-as-string
type java.util.UUID -> string
member java.util.UUID.randomUUID/0 -> Guid.NewGuid().ToString() using System
member org.slf4j.Logger.warn/2 ?lastArgumentThrowable -> $0.LogWarning($2, $1)
member org.slf4j.Logger.warn/2 -> $0.LogWarning($1, $2)
statement java.util.Map.put/2 -> $0[$1] = $2
```

- **Directives.** `type`, `plain` (a generic type whose counterpart is not generic) and `pascal` (a class whose
  members are the Java ones in PascalCase) map types. `member` and `statement` map calls (`owner.new` for a
  constructor), `field` maps fields, and `override` gives a C# name to a translated method that overrides a library
  method.
- **Keys and templates** are the JDK table's.
- **Guards.** A rule with a guard, `?name`, applies only when the guard holds, and the rules of one key are tried in
  order. The guards are named predicates of the call: `arguments=N`, `receiver=fqn`, `lastArgumentThrowable`,
  `methodReference`. `CSharpMappingFile.guard` registers more.

A mapping also states what the translated code needs when the mapping is used (`requirements()`): its NuGet packages
(`nuget <package> <version>` in a file) and the C# sources that go with the translation (`compat <file>`, beside the
mapping file). The JDK mapping's source is the compatibility library. `CSharpLibraries.projectFile` writes a project
that references the packages. The ratchet writes it beside the translation, restores the packages, and gives their
assemblies to the judge.

The mapping also lists the C# type names it writes (`typeNames()`). A type of the program with one of those names is
qualified, as one whose name two namespaces declare is.

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
  `IReadOnlyCollection<T>`, `Iterable` is `IEnumerable<T>`. A collection of `?` is C#'s non-generic interface, which every generic collection implements: `List<?>` is
  `IList`, `Collection<?>` and `Set<?>` are `ICollection`, `Iterable<?>` is `IEnumerable`, `Map<?, ?>` is
  `IDictionary`, `Comparable<?>` is `IComparable`. A method's parameter of a program type with wildcards, `Context<?> c`, is captured as Java does
  in the body: the method gets a type parameter of its own, `void OnRequest<TEmbedded>(Context<TEmbedded> c)`,
  constrained by the wildcard's bound or the declared one (`CSharpWildcards`). Not in a constructor, a functional
  interface's method, an override of a library method, or when the bound is no valid C# constraint (string, a
  delegate, a sealed class, a type parameter). Any other wildcard is its bound (`WILDCARD_AS_BOUND`).
- **Self-bounded types.** A wildcard of a type whose parameter is bounded by itself, `Score<?>` of
  `Score<Score_ extends Score<Score_>>`, has no C# type argument: none satisfies `where Score_ : IScore<Score_>`. As
  C#'s `IComparable` beside `IComparable<T>`, such a type has a companion without its self-bounded parameters
  (`CSharpCompanions`): `IScore` beside `IScore<Score_>`, `IDirector<Solution_>` beside `IDirector<Solution_, Score_>`;
  a class's companion is the interface `I` + its name. The wildcard, and the raw type, is the companion. The companion
  declares the type's own members that do not take the removed parameter, with a result of it erased to its bound's
  companion (`IScore Negate();`); the type implements them explicitly, calling its own
  (`IScore IScore.Negate() => Negate();`, a default implementation in an interface), so that every `IScore<S>` is an
  `IScore`. A member whose result has the removed parameter inside another type (`List<Score_>`) is not the
  companion's. The companion extends the companions of the type's self-bounded supertypes, and its plain interfaces.
- **Streams and Optional.** Streams become LINQ over `IEnumerable<T>`: `filter`/`map`/`collect(toList())` become
  `Where`/`Select`/`ToList()`. `Optional<T>` becomes the value itself or null: `orElse(x)` becomes `?? x`.
- **Functional interfaces.** These become delegates: `Function<T, R>` → `Func<T, R>`, `Predicate<T>` →
  `Func<T, bool>`, `Runnable` → `Action`, `Comparator<T>` → `Comparison<T>`. Calling their method becomes an
  invocation: `f.apply(x)` becomes `f(x)`.
- **Values.** The values of `java.time`, `java.net` and `java.math` become BCL structs, as `Instant` →
  `DateTimeOffset` does:
  - `LocalDate` → `DateOnly`, `LocalTime` → `TimeOnly`, `LocalDateTime` → `DateTime`, `Duration` → `TimeSpan`.
  - `Clock` → `TimeProvider`, so `LocalDateTime.now(clock)` becomes `clock.GetLocalNow().DateTime`.
  - A `DateTimeFormatter` becomes a custom format string. The ISO constants are .NET patterns
    (`ISO_LOCAL_DATE` → `"yyyy-MM-dd"`), and `parse(s, f)` becomes `ParseExact(s, f, CultureInfo.InvariantCulture)`.
  - `URI` → `Uri`, created `RelativeOrAbsolute` as Java's may be relative. `UUID` → `Guid`.
  - `BigDecimal` → `decimal`, with arithmetic as operators. `Random`, `Base64` and `HexFormat` become `Random` and
    `Convert`. A `MessageDigest` becomes a `HashAlgorithm`.
  - C#'s numbers have no common base class, so `Number` becomes `IConvertible`, and `n.doubleValue()` becomes
    `Convert.ToDouble(n)`. This is wider than Java's type: string and `DateTime` are `IConvertible` too.
  - These structs are not nullable where Java's references are: a `return null` of a `UUID` does not compile.
- **Concurrency.** A `CompletableFuture<T>` becomes a `Task<T>`:
  - `supplyAsync(s, executor)` becomes `Task.Factory.StartNew` on the executor, and `join()` becomes `.Result`.
  - The continuations (`ThenApply`, `ThenCompose`) are the compatibility library's. They get explicit type
    arguments: `Task<T>` is invariant, and C# would infer the result type from the lambda where Java has it from
    the target.
  - An `Executor` becomes a `TaskScheduler`; a `ThreadPoolExecutor` and the `Executors` pools become the .NET thread
    pool, `TaskScheduler.Default`.
  - `CountDownLatch` becomes `CountdownEvent`, and `ReadWriteLock` a reentrant `ReaderWriterLockSlim`.
  - Its halves, Java's `Lock`, are the compatibility library's `IJavaLock`, as are the atomics.
- **Reflection.** `Class` becomes `System.Type`, and the members of `java.lang.reflect` become
  `System.Reflection`'s: `Method` → `MethodInfo`, `Field` → `FieldInfo`, `Parameter` → `ParameterInfo`.
  - Annotations are attributes: `getAnnotation(Tool.class)` becomes `GetCustomAttribute<ToolAttribute>()`, and
    `isAnnotationPresent` becomes `IsDefined`.
  - `getDeclaredMethods()` becomes `GetMethods` with `DeclaredOnly` and every visibility.
  - `getModifiers()` gives Java's bits, from the compatibility library's `JavaModifier`, so `Modifier.isStatic(m)`
    stays a bit test.
  - The kinds of `java.lang.reflect.Type` are one `System.Type`. A type test against a kind is a property pattern:
    `t is Type { IsConstructedGenericType: true } pt`, `IsGenericParameter` for a type variable, `IsArray` for a
    generic array.
  - `instanceof Class` on such a value matches a type that is neither a type parameter nor constructed.
  - .NET has no wildcards, so `WildcardType` becomes the compatibility library's interface, which no `Type`
    implements.
  - An anonymous `ParameterizedType` whose methods return constants is how Java writes a type such as
    `Map<String, Object>` as a value. It becomes `typeof(IDictionary<string, object>)`.
  - `isSealed()` and `getPermittedSubclasses()`, Java's sealed hierarchies, have no counterpart: they stay unmapped.
- **Services and files.**
  - A `ServiceLoader<T>` becomes the implementations in the loaded assemblies: `JavaServiceLoader.Load<T>()` from
    the compatibility library, an `IEnumerable<T>`, so `findFirst()` becomes `FirstOrDefault()`.
  - A `ClassLoader` becomes the `Assembly` that holds the types: `loadClass(n)` becomes `GetType(n, true)`.
  - A `Path` becomes a `FileInfo`, not a string, because Java overloads on `Path` and `String` and a string path
    would merge the overloads. `Files.readAllBytes(p)` becomes `File.ReadAllBytes(p.FullName)`.
  - `HttpURLConnection` stays unmapped: `HttpClient` is a rewrite, not a mapping.
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
  `System.IO.Compression` (`JavaZipFile`, `JavaJarFile`, `JavaZipEntry`, `JavaZipOutputStream`, `JavaManifest`),
  `JavaUuid` (the MD5 name-based UUID), `JavaDigest` (`MessageDigest.getInstance`'s algorithm names), `JavaTasks`
  (`CompletableFuture`'s continuations), `IJavaLock` and `JavaReentrantLock`, and the atomics (`AtomicBoolean`,
  `AtomicInteger`, `AtomicLong`, `AtomicReference<T>`), `IJavaWildcardType` and `JavaModifier`,
  `JavaServiceLoader`.
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
