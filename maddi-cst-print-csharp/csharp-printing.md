# Printing the CST as C#

`maddi-cst-print-csharp` prints a CST, parsed from Java, as **C#** source. It is the C# counterpart of
`maddi-cst-print-kotlin` (see `kotlin-printing.md`). It reuses the language-neutral `OutputElement` IR and the
`maddi-cst-print` formatter unchanged. The work item is #115.

## Aim

The aim is automatic translation, with no manual editing, into C# that reads as if it had been written in C#. maddi's
analyses drive what makes the output idiomatic:

- **Nullability** (`NullabilityPass`, maddi-mod) decides where `?` goes under `#nullable enable`, and where `!`
  asserts what the analysis knows (`CSharpFacts`, `CSharpNullability`).
- **Modification** decides whether a collection parameter can be `IReadOnlyList<T>` (`CSharpReadOnly`).
- **The whole program** (`CSharpProgram`) decides what is open for extension, which functional interfaces are
  delegates, which getters and setters are properties (`CSharpProperties`), and which type tokens are type arguments.

All of these are wired in; without facts from an analysis, the output follows from the Java and the whole program
alone. Not yet: immutability making a class a `record` or a `readonly struct`, or a field `init`-only.

## The pluggable-printer seam

`CSharpTypePrinter`, `CSharpMethodPrinter` and `CSharpFieldPrinter` implement the cst-api `TypePrinter`,
`MethodPrinter` and `FieldPrinter` interfaces, including the factory overload, as the Java and Kotlin printers do.
`CSharpCompilationUnitPrinter` prints a file and returns `PrintMessage<CSharpPrintCode>`s alongside the text. The
state of one file's printing (types and methods being printed, messages, `using`s) is in the thread-local
`CSharpContext`, because the factory signatures carry no context; its language-neutral part (the type, method,
statement and call stacks, referenced types, messages) is maddi-cst-print-translate's `TranslationState`, shared with
the Kotlin printer.

## Naming (`CSharpNames`)

Every name is a function of its declaration and of the whole program's facts (`CSharpProgram`), so a declaration and
all its uses agree without a renaming pass.

- C# has no member named as its type, a type parameter of its type, or a nested type of its type. A method named
  as its class is `Of` when static, `<Name>Value` otherwise: `Metadata.metadata(k, v)` is `Metadata.Of(k, v)`. A
  method named as a nested type, with a field of its Java name, is `<Name>Value` too (`ResultValue()` beside
  `enum Result` and field `result`). A type parameter named as a member takes the C# convention's `T` prefix
  (`EmbeddingMatch<TEmbedded>` with `Embedded()`).
- A type is qualified with its namespace where a member of the printed class (or of a class it is nested in, or of
  their superclasses) hides it (`VideoContent.Video()` hides `Video`; in an expression, `Kind.Filter`, a property
  `Kind` of type `Kind?` hides it too, where C#'s "Color Color" rule lets a `Kind` through), and outside its namespace where its simple
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
- Any other new name that would collide falls back to the Java name: a method's PascalCase name that a field or a
  nested type has, an enum constant's or a record component's that another field or a nested type has.

Library declarations keep their Java names, except where the BCL mapping (below) translates them. A translated
`close()` of an `AutoCloseable` becomes `Dispose()`.

## Types (`CSharpTypeName`)

- Primitives map to C#'s, except that Java's signed `byte` becomes `sbyte`.
- `String` and `Object` become `string` and `object`.
- A boxed type becomes the nullable value type, `Integer` → `int?`. As a type argument it becomes the value type
  itself: `List<Integer>` → `List<int>`, because C# generics are not erased.
- So does every Java reference type that is a C# value type: a simple enum (a C# `enum`), and a library type mapped
  to a struct (`Duration` → `TimeSpan?`, `UUID` → `Guid?`, `BigDecimal` → `decimal?`, the `java.time` types). Java's
  `null` fits there, `private Style style = null`, `requireNonNullElse(style, Style.OR)` (`style ?? Style.Or`), and a
  use as the value casts as an `int?`'s does. As a type argument, and as an array's element (`Enum.GetValues<E>()` is
  an `E[]`), it is the value type itself. Not `Map.Entry`'s `KeyValuePair`: an entry is its map's, and a
  `KeyValuePair<K, V>?` would cast every `e.Key` of a for-each over the map. Java's `var` of such a type is declared
  (`Kind? kind = Kind.Filter`), so that a later `kind = node.getKind()` fits. Where C# has the value type itself, the
  `X?` is cast: a template's operand (`BigDecimal.add`'s `((decimal) a) + ((decimal) b)`), a library's parameter of
  the struct, an override of `T get()` (`decimal Get()`), a lambda's result; a method reference's typed parameters
  are the delegate's type arguments (`(decimal p0, decimal p1) => p0 + p1`).
- In a pattern, a type has no `?`: `o instanceof Integer` becomes `o is int`.
- C# has no wildcards. A collection of `? extends T` is C#'s covariant read-only interface, a collection of `?` its
  non-generic one, a parameter of a program type with wildcards is captured as Java does, and any other wildcard is
  its bound (`WILDCARD_AS_BOUND`): see Wildcards and Self-bounded types under the JDK mapping.
- C# has no raw types either. A raw `Key` becomes `Key<Bound>` (`RAW_TYPE`).
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
  (primitives, and `KeyValuePair`; a simple enum or a struct is `X?` already), a method's own type parameters and types that
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
  compiler's nullable warnings (`nullableWarnings`); maddi-mod's `*-facts` ratchets hold them.
- A dereference of a value that may be null has a `!` (`CSharpNullability`): `x!.M()`, `x!.f`, `a![i]`,
  `foreach (… in xs!)`. Java throws a NullPointerException there when it is null, and C# a NullReferenceException; the
  `!` only tells the compiler so (CS8602), it does not throw. A value may be null when it is the result of a call
  whose method may return null (unless the analysis knows this call's is not), or a field, parameter or local the
  analysis finds nullable, unless its use-site facts know it is not null where the statement or the call starts (after
  `if (v == null) return;`, inside `if (v != null)`), which C#'s flow analysis knows too. The facts for locals and use
  sites come from the analysis's report, through `CSharpFacts`; a local the analysis asserts at its declaration
  (`assertedAtDeclaration`) has its initializer asserted instead: `var block = blocks.GetWithKey(j)!;`. So does a local
  the analysis finds not null there although C# declares its initializer nullable: `string? v = m.Get(k)!;` after
  `m.ContainsKey(k)`.
- An argument that may be null, for a parameter that is not nullable, has a `!` too (CS8604): `list.Add(x!)`. Java
  passes the null on, and so does C#. A program's parameter is nullable when the analysis says so, a library's when
  its hints do. C# prints no `?` in a type argument (`List<Exprent>`), so that a collection's element or key is not
  nullable whatever the hints: Java's `contains(Object)` takes null, C#'s `Contains(T)` does not, and even a `null`
  there is `null!`. This is the write the analysis asserts under its Kotlin policy (`assertContentWrites`). A value of
  a type parameter C# leaves unconstrained may be `string?` to C#: into a reference type that is not a type parameter
  it is asserted whatever the analysis knows, `marshaller.Marshal(root!, w)`.
- The same holds for the other places a value goes (CS8603, CS8625, CS8601): a `return` of a value that may be null,
  or of `null`, from a method the analysis does not find nullable, or from a lambda, whose result is a delegate's type
  argument; a write into an array's element, which C# prints without `?` (`arr[i] = null!`, `{ x, null! }`: the array
  holds the null, as Java's does); an assignment to a field the analysis finds non-null.
- A reference cast of a value that may be null is to `T?`, `(Exprent?) o`: the cast lets the null through, in C# as
  in Java (CS8600). So is a cast of `null`, which picks an overload: `(AiMessage?) null`. A method's own type
  parameter has no `T?` there, and its operand is asserted instead: `(T) m.Get(k)!`.
- The result of a library call may be null where C# declares it so, whatever the analysis says of the Java method:
  `object.ToString()` is `string?`, `Class.getName()`'s `Type.FullName` too (`Class::getName` is
  `p0 => p0.FullName!`; a method reference of the program's method with a nullable result, to a delegate whose
  result is not, likewise a lambda with `!` where its receiver is a type, `this` or its own parameter,
  `() => this.Replayed()!`), Jackson's `node.get(k)` is `node[k]`, a `JsonNode?`, and reflection's lookups
  (`GetConstructor`, `GetMethod`, `GetElementType`, `Invoke`) return null where Java throws. The printer knows these
  from the mapping's C# template (`CSharpExpressionPrinter.nullableResult`), or from the rule itself: a mapping
  file's `->?` (`member java.util.Deque.poll/0 ->? $0.PollFirst()`), for the compatibility library's members that
  return `T?` (`Put`, `PollFirst`, the navigable lookups, `GetProperty`) and the BCL's nullable properties
  (`DeclaringType`, `DocumentElement`, JAXB's `GetNode()`); `CSharpBcl`'s `nullable(...)` for a chooser's
  (`Map.get`'s `$0.Get($1)`). A class's enumerator over an `iterator()` the analysis finds nullable asserts it:
  `Iterator()!.AsEnumerator()`, where Java's for-each throws.
- A method's result is nullable where an override's may be null: C# lets an override narrow `T?` to `T`, not widen
  `T` to `T?` (CS8764, CS8766), and a call through the base reaches the override. The analysis decides each method
  alone; the printer takes the override family (`FamilyFacts`): `protected internal abstract S? CreateUpcoming();`,
  and a call where null may not go is asserted, `CreateUpcoming()!`.
- An unboxing cast, `(int) x`, has a `!` on a value that may be null (CS8629, CS8605): `(int) map.GetValueOrNull(k)!`.
  Java's unboxing throws a NullPointerException there, C#'s cast an InvalidOperationException. An `Integer` is C#'s
  `int?` whatever the analysis says of its declaration, so a variable has the `!` unless a use-site fact makes it
  non-null, and a call unless its result is the value type itself (`List<int>`'s `l[0]`) and not nullable.

## Declarations

### Access

  So is a `decimal?` or a `Kind?` boxed into a reference parameter, `Validate(total!)` of an `IConvertible` (CS8604),
  and a functional interface's lambda class passing one to its delegate, `f((Kind) kind!)`.
- **Access.** Every declaration states its access, except the public members of an interface, where C# writes none.
  - Package access becomes `internal`, because a translated code base is one assembly.
  - `protected` becomes `protected internal`, because Java's `protected` also grants package access.
  - Access is read from the declared modifiers, not from `access()`. For fields and types, `access()` is combined
    with the enclosing type's access.
  - Java's `private` covers the whole top-level type; C#'s covers the type itself and its nested types.
    `CSharpAccess` finds the private members and nested types that code outside their owner reaches. Those, and only
    those, become `internal`.
  - An override keeps the access of the class method it overrides: Java may widen access, C# may not.
- **Exposed types.** Java's public method may name a less accessible type, C#'s may not: a nested type is as visible
  as the members whose signatures name it (a record's components count as its properties), capped by the visibility
  of the members' own types, and so are the types it is nested in (`CSharpAccess`).
- **Bases of public types.** Java's public class may extend a package-private one; C#'s base class or interface is
  at least as accessible as the type. Every supertype of a public type, transitively, and the types it is nested in,
  is printed public (`CSharpProgram.exposedBases`).

### Classes and inheritance

- **Classes.**
  - Java's utility-class idiom becomes a `static class`: static members only, and one constructor that is private,
    parameterless and empty.
  - A nested class never gets `static`.
  - `throws` is dropped.
  - A single-`return` body becomes an expression body, `=> expr;`.
  - A constructor's `super(…)` or `this(…)` becomes `: base(…)` or `: this(…)`.
  - A static initializer becomes the static constructor. An instance initializer is not translated
    (`INSTANCE_INITIALIZER`).
  - `synchronized` becomes `[MethodImpl(MethodImplOptions.Synchronized)]`.
  - Varargs become `params T[]`.
- **Inheritance.** C#'s classes and methods are open only when declared so; Java's are open unless declared final.
  - With the whole program (`CSharpProgram`), a class nothing extends is `sealed`, and a method nothing overrides is
    not `virtual`. The policy `Inheritance.OPEN_PUBLIC_API` keeps public classes and their public and protected
    methods open for code outside the program; `Inheritance.OPEN`, and a printer without the program, keep
    everything Java leaves open: a method that is not final, static or private is `virtual`, a class that is not
    final is not `sealed`.
  - An override of a class method becomes `override`, or `sealed override` when it is final. Implementing an
    interface method needs neither. An override and an explicit interface implementation inherit their constraints
    and do not repeat them.
- **Interface implementations.** C# is stricter than Java in three places:
  - a method may implement an interface's method with a narrower return type in Java; C# implements the interface's
    method explicitly as well, calling the method: `IFailure IFailure.WithCode(int code) => WithCode(code);`;
  - a default method that implements an inherited interface's method is an explicit implementation:
    `Type IListener<string>.EventClass() => typeof(string);`; so is one of a JDK interface's method that C# names
    otherwise, `default void close()` of a `Closeable`: `void IDisposable.Dispose() { }`, which a `Dispose()` of its
    own would hide;
  - an abstract class that leaves an interface's method to its subclasses redeclares it,
    `public abstract string Embed(string text);`, and the subclasses' implementations are `override`s; a
    compatibility interface's too, `Iterator`'s `public abstract bool HasNext();`, whose `S Next()` a
    `SelectionIterator<int>`'s override has as `int Next()`. A method of a
    superclass with the interface method's signature implements it, in C# as in Java, although Java's overrides()
    does not link the two: the class in between does not redeclare it.

  A generic interface method (Java may implement `<T> Class<T> eventClass()` raw) is left as it is.
- **Default methods.** A C# class does not inherit its interfaces' default methods: a call on a class goes through
  the interface, `((IResult) this).Failed()`.
- **Java's object protocol.** C#'s `object` has `ToString`, `Equals` and `GetHashCode` to override, not `clone`:
  Java's `clone()` is a method of its own, and `super.clone()` is `MemberwiseClone()`. The marker interfaces
  `Cloneable`, `Serializable`, `RandomAccess` and `EventListener` are dropped, from a type parameter's constraints
  too (`Support<E extends EventListener>` is `Support<E>`).
- **Subclasses of collections.** C#'s `List<T>` has no virtual methods. A class extending `ArrayList` extends the
  compatibility library's `JavaArrayList<E>`, a `List<E>` whose Java methods (`Add`, `Remove`, `Set`, `AddAll`,
  `Clear`, `Clone`, …) are virtual. A call on a receiver of such a class goes through them, also where its value is
  not used (`Remove(int)`, not `RemoveAt`). A call through a `List<E>`-typed reference reaches `List`'s method, not
  the override: `LIST_SUBCLASS`, a behaviour change. (Declaring Java's `List` as `IList<T>` would dispatch, but
  `IList<T>` is not an `IReadOnlyList<T>`, which the covariant `List<? extends T>` needs.)
- **Subclasses of `AbstractSet`.** C#'s `ISet<T>` has no virtual methods. A class extending `AbstractSet` extends the
  compatibility library's `JavaSet<E>`, whose `Iterator()` and `Size()` are abstract and whose `Contains`, `Add`,
  `Remove`, `Clear`, `ContainsAll`, `AddAll`, `RemoveAll` and `RetainAll` are virtual, with `AbstractCollection`'s
  defaults; it implements `ISet<E>` on them. A class implementing `Set` itself, without a superclass, extends it
  too, and its methods of those names override them.
- **An `Object` where the element or key type is expected.** Java's `contains(Object)`, `remove(Object)`,
  `containsKey(Object)` and `get(Object)` take any object; C#'s take the element's or the key's type. Given an
  `Object` where the collection's first type argument is not `Object`, the call is the compatibility library's
  `ContainsObject`, `RemoveObject`, `ContainsKeyObject`, `GetObject` or `RemoveKeyObject`, which test the object's
  type: one of another type is not there, as in Java. (Not a `get` of a value-type value, nor on a read-only receiver.)
- **Subclasses of `Number`.** A `Number` is C#'s `IConvertible`, which has no `intValue()` to override. A class
  extending `Number` extends the compatibility library's `JavaNumber`, whose `IntValue()`, `LongValue()`,
  `FloatValue()` and `DoubleValue()` are abstract and which implements `IConvertible` on them, so that
  `Convert.ToInt64(n)` reads it as it reads an `Integer`.
- **Iterables.** A class implementing Java's `Iterable` is C#'s `IEnumerable<T>`: it gets a `GetEnumerator()` that
  walks its `Iterator()`, so that `foreach` and LINQ work on it. An interface that extends `Iterable` declares
  `IJavaIterator<T> Iterator();` and is enumerable through default implementations over it
  (`IEnumerator<T> IEnumerable<T>.GetEnumerator() => …`), so that every implementation is; an abstract class that
  leaves `iterator()` to its subclasses declares it abstract, once, and the subclasses' `Iterator()` override it. A
  class that reaches two such interfaces, neither extending the other (directly, or through an interface that
  extends both), declares its own `GetEnumerator()`: C#
  finds no most specific of their defaults (CS8705). Its subclasses inherit it. An interface that extends two such
  redeclares `new IJavaIterator<T> Iterator();`: each of them declares one, and a call through it is ambiguous
  (CS0121).

### Members

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
  - A lone getter qualifies only when nothing overrides it, it overrides nothing, it implements no subclass's
    interface method, no method reference names it, and its name is not taken in its class, the classes above it,
    or those that extend it.
  - Methods that override one another are one property when all of them are parameterless with one return type,
    in classes and interfaces that are not delegates:
    - an interface's method becomes `T Name { get; }`, or `T Name => expr;` with a default body;
    - an abstract one becomes `abstract T Name { get; }`, also where an abstract class leaves an interface's
      property to its subclasses;
    - a getter of a field takes the field's place;
    - any other body becomes a computed property: `T Name => expr;`, or `T Name { get { … } }`.
  - A functional interface's lambda adapter implements its property: `public double Area => f();`.
  - Records keep their positional properties, and enums their methods.
- **Member names** are under Naming above.

### Kinds of types

- **Enums.** An enum of constants only becomes a C# `enum`. One with fields, methods or constructors becomes a sealed
  class with a `public static readonly` instance per constant (`ENUM_AS_CLASS`), initialised with its `Name` and
  `Ordinal`, and with `Values()` and `ValueOf(string)`. As Java's enum it is comparable by its ordinal,
  `IComparable<Planet>` with `CompareTo`. On a C# enum, `values()` becomes `Enum.GetValues<T>()` and
  `ordinal()` a cast to `int`. A constant with a body, an anonymous subclass of its enum, is not translated
  (`ANONYMOUS_CLASS`). A switch on such a class is under Switch statements.
- **Records.** A record becomes a positional `sealed record Point(int X, int Y)`, and `p.x()` becomes `p.X`. Its
  parameter list does not see the record's nested types, which are qualified there: `record Id(long Key, Id.Kind? kind)`. One with
  a canonical or compact constructor, which a positional record cannot have, declares get-only properties and that
  constructor. Another constructor with as many parameters as components would clash with the positional one and is
  left out (`RECORD_CONSTRUCTOR`). An accessor that implements an interface's method, `T response()`, is the property:
  the interface's method is implemented explicitly, `T IResult<T>.Response() => Response;`. A record's
  `equals(Object)` is `public bool Equals(Point? o)`: C# synthesizes `Equals(object)`, which may not be declared
  (CS0111), and it calls this one with the other record or null. A record with `equals` but no `hashCode` keeps
  Java's hash of its components, `public override int GetHashCode() => HashCode.Combine(Name, N);` (CS8851).
- **Annotations.** An annotation type of the program is a sealed attribute class, `ToolAttribute : Attribute`
  (`CSharpAttributes`): its elements are properties with their defaults, a `value` element also the constructor's
  parameter (`params` for an array), and `@Target` is `[AttributeUsage]`. Its nested types are the attribute's,
  `PlanningVariable.NullComparator`, a `Class` element's default, is `PlanningVariableAttribute.INullComparator<>`. Reading an element, `tool.name()`, reads
  the property, `tool.Name`. A use on a type, method, field or parameter is an attribute,
  `[Tool("Adds", Name = "add")]`; `@Deprecated` is `[Obsolete]`. Other JDK annotations have no C# counterpart, and
  those of other libraries are what their mapping makes of them, or dropped.
- **Functional interfaces.** A translated functional interface is a C# `delegate` when the whole program allows it:
  one abstract method and nothing else, and no class or interface of the program implementing or extending it (an
  anonymous class that becomes a lambda does not count). Its lambdas are plain lambdas, a call `f.apply(x)` is an
  invocation `f(x)`, and a method reference `f::apply` is `f.Invoke`. `CSharpProgram` makes that decision once,
  over all the translated types, before the files are printed. Any other functional interface stays an interface
  and gets a nested adapter, `public sealed class Lambda(Func<Exprent, int> f) : IExprentIterator { … }`, around its
  lambdas and method references: `new IExprentIterator.Lambda(e => 0)`. The policy `FunctionalInterfaces.ADAPTER`,
  and a printer that is not given the program (`CSharpProgram.NONE`), use this form for all of them. The JDK's
  functional interfaces are the BCL's delegates (see the JDK mapping). One that extends a JDK functional interface,
  `MovableFilter<Solution_> extends BiPredicate<Solution_, Object>`, cannot extend the delegate: it declares the method
  itself in PascalCase, `bool Test(Solution_ arg0, object arg1);`, and a call through it is `filter.Test(s, e)`, not
  the delegate's invocation.
- **Anonymous classes.** One of either kind of functional interface that only implements the method, without fields
  and without using itself, becomes a lambda. Any other is hoisted (`CSharpAnonymous`): a private sealed nested class
  of the type whose code creates it, named after what it extends (`GraphImpl`). The local variables and parameters
  it uses become its constructor's arguments and its fields, and so does the enclosing instance, `outer`, when it
  uses one of its instance members; its fields' initializers move into that constructor, after the captures. Its
  superclass's constructor arguments come first. A generic method's type parameters become the class's. An
  anonymous class that is neither is not translated (`ANONYMOUS_CLASS`).
- **Local classes.** One that captures nothing is lifted: a private nested type of the enclosing type
  (`CSharpLocalTypes`). One that captures a local variable, a parameter or the enclosing instance is not translated
  (`LOCAL_CLASS`).
- **Inner classes.** A Java inner class, a non-static member class, has an enclosing instance; a C# nested class
  has none. One that uses it (`CSharpInner`) gets it as a field, `private readonly Range outer;`, which every
  constructor takes first and passes on (`: this(outer, …)`, `: base(outer, …)` to a superclass that takes one too).
  The enclosing instance's members are used through it (`outer.to`, `outer.Width()`), `Range.this` is `outer`, and a
  creation passes it: `new Upcoming(this)` in the enclosing type's code, `new Upcoming(outer)` in an inner class's,
  `new Upcoming(other)` for `other.new Upcoming()`, `p0 => new Stepped(this, p0)` for `Stepped::new`. Its instance
  fields' initializers move into the constructors, after `this.outer = outer;`, where Java runs them: a C# field
  initializer cannot use `outer`. So do a class's whose initializer uses `this`, explicitly or through an instance
  member (`getLogger(getClass())`): Java runs them after `super(…)`, C# before the base constructor. Where no chain
  of `outer` leads to the instance, `OUTER_THIS`.
- **Nested types of generic types.** C# makes a nested type generic in its enclosing types' parameters, which a
  Java static nested type is not. A static nested type of a generic type is therefore printed beside its primary
  type, in the namespace, by its simple name; private members of the outer type it uses become `internal`. When
  another type of the namespace has that name too, the name is prefixed with its enclosing types' names
  (`EmbeddingStoreRequestContextAdd`), a whole-program fact of `CSharpProgram`. An inner (non-static) class of a
  generic type is not translated (`NESTED_IN_GENERIC`): C# names it `Outer<E>.Inner`.
- **Static members of generic types.** C# reaches a generic class's static members through a constructed type,
  `Response<T>.From(x)`; Java through the class, `Response.from(x)`. Java's static members never use the type's
  parameters, so they are declared in a non-generic static class of the same name beside the generic type, the C#
  idiom of `Tuple` and `Tuple<T>`: `public static class Response { public static Response<T> From<T>(T content)
  … }`, which `Response.From(x)` calls as Java does. The generic type's own code qualifies them (`Response.Count()`);
  the private members either side reaches become `internal`, as the two are siblings, and so do the protected ones,
  which a static class cannot have (CS1057) and Java's subclasses reach. A static member whose code
  declares an anonymous or local class keeps the type without a companion.

### Generics

- **Type tokens.** Java passes a `Class<T>` where C# has the type argument: `<T> T fromJson(String json, Class<T>
  type)` is `T FromJson<T>(string json)`, its body reads `typeof(T)` for `type`, and `fromJson(s, Foo.class)` is
  `FromJson<Foo>(s)`. `CSharpProgram` decides it over the whole program: `T` is the method's only type parameter,
  every call passes a `Class<X>` of a known `X` (not a `Class<?>`), no method reference names the method, and the
  methods it overrides or that override it qualify as well. Otherwise the token stays a `Type` parameter.
- **Generic methods overridden by erasure.** Java overrides `<J> J mapping()` with a method that is not generic,
  `TriFunction<…> mapping()`; C# does not. A class's method whose result is its own type parameter, overridden so,
  is printed with it erased to its bound, `object Mapping()`, and so are its generic overrides; the others override
  it with a covariant result, and a call through it casts the result to the type Java inferred.
- **Self-bounded type parameters only a result mentions.** `<Score_ extends Score<Score_>> InnerScore<Score_>
  getScore()`, bounded by a program type (not `Comparable<? super R>`, whose type argument C# infers from a method
  group's target): where Java infers a raw or captured `Score_`, no C# type argument satisfies the constraint. Such a
  method, with all its overrides, is printed with the type parameter erased, a wildcard in its signature and body:
  `InnerScore<IScore> GetScore()`, `IInnerScoreDirector<Solution_> GetScoreDirector()` (the wildcard interface). A
  call where Java's type is writable casts to it through `object`,
  `(InnerScore<SimpleScore>) (object) scope.GetScore()`, since C# relates no two instantiations of a class.
- **Type arguments C# cannot infer.** Java infers a method type parameter that no parameter's type mentions,
  `<V> V getStore(int i)`, from the call's target; C# does not. The call gets all its type arguments as Java
  inferred them, `tuple.GetStore<Bucket<…>>(i)`, unless one is not a type C# can write. So does a method group,
  whose type arguments C# does not infer from the delegate it converts to: `AbstractMinMaxSlot::minState` as a
  supplier is `AbstractMinMaxSlot.MinState<Result_>`. A `Class<T>` parameter is C#'s `Type`, from which C# infers
  nothing: `T` from a `Class<? extends Acceptor>` is `IAcceptor`, from a `Class<?>` its own bound.
- **Diamonds.** `new X<>(…)` whose type arguments the parser left as `X`'s own type parameters, out of scope where
  it is created, gets the ones its constructor's arguments give; as a call's argument, else the ones Java inferred
  for its parameter: `share(new Bridge<>(parent), group::setBridge)`'s `S` is `Bridge<NewA>`, which the method
  reference binds; through the parameter's whole type and a supertype the class declares with its own type
  parameters (`newSetFromMap(new HashMap<>())` is `new Dictionary<T, bool>()` for `Map<E, Boolean>`), and an outer
  creation's type arguments as printed (`new State<>(true, new TreeMap<>())`), also as a conditional's branch.
- **Constructor references.** A raw `Triple::new` creates the type Java inferred for its result,
  `(p0, p1, p2) => new Triple<A, B, C>(p0, p1, p2)`, also through a supertype the class declares with its own type
  parameters: `HashSet::new` as an `IntFunction<Set<V>>` is `p0 => new HashSet<V>(p0)`.
- **Type parameters in scope.** Where the printer writes a type Java inferred (a cast after an erased call, explicit
  type arguments, a diamond), each type parameter in it must be one C# can name there: a method's being printed, or
  the current type's or an enclosing one's, not in the non-generic companion class of the static members.
- **Delegate variance.** Java passes a `Function<A, P>` where a `Function<A, ?>` is expected; C#'s `Func` is
  covariant only in a reference type, which a type parameter is not known to be. A variable is wrapped in a lambda
  that C# converts with the boxing, `x0_ => leftMapping(x0_)`, a parameter that differs cast,
  `x0_ => mapping((T) x0_)`; any other expression is left, the lambda would evaluate it at every call.
- **Casts to type parameters.** C# casts to a type parameter only from `object`, an interface or another type
  parameter: `(T) this` in a self-typed builder is `(T) (object) this`.

### The C# errors these rules avoid

| C# error | what Java allows that C# does not | rule |
|---|---|---|
| CS0060, CS0061 | a public type whose base is less accessible | Bases of public types |
| CS0050, CS0051 | a public member naming a less accessible type | Exposed types |
| CS0236 | a field initializer that uses the enclosing instance | Inner classes |
| CS0027 | a field initializer that uses `this` | Inner classes |
| CS0411 | a type argument inferred from the call's target | Type arguments C# cannot infer, Self-bounded type parameters |
| CS0460 | an override repeating its constraints | Inheritance |
| CS1503 | a `Func<A, P>` for a `Func<A, object>` | Delegate variance |
| CS0030 | a cast to a type parameter from a class | Casts to type parameters |
| CS0542 | a member named as its type | Naming |
| CS9135 | a `case` on an enum translated to a class | Switch statements |

## Statements and expressions

C#'s statements, operators and precedence are mostly Java's, so most of the code prints as it does in Java. The
differences:

- **Loops and statements.**
  - `for (T x : xs)` becomes `foreach (T x in xs)`. One whose body assigns the loop variable iterates over `vItem`
    and declares `v` as its copy: C#'s iteration variable is read-only.
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
  - On an enum C# declares as a class, the constants are static instances, not constants, so a case is a property
    pattern on the instance's `Name`, the Java constant's: `case { Name: "FAST" }:`, `{ Name: "FULL" } => …`. A
    fall-through into such a section, which `goto case` cannot name, jumps to a statement label at its start:
    `goto case_FULL;` … `case_FULL: ;`.
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
  declared type. Otherwise it keeps its type. Java's `var` of a conditional whose branches differ is their common
  supertype, which C#'s `var` does not infer (CS0173): it is declared, `Node<A> node = c ? new Indexed<A>() : new
  Unindexed<A>()`, and C# types the conditional from it; not when C# cannot write that type, with a wildcard or as
  `object` for an intersection of the program's types.
- **Type tests.** `instanceof` becomes `is`, `x instanceof T t` becomes `x is T t`, and `!(x instanceof T)` becomes
  `x is not T`. A record pattern of the program's record is a property pattern, in a type test and in a `case` alike:
  `o instanceof Line(Point(var x1, var y1), Point to)` becomes `o is Line { From: Point { X: var x1, Y: var y1 }, To:
  var to }`. A component's own type is `var`, a narrower one a type test; `_` leaves the component out. A library's
  record has no such properties (`SWITCH_FORM`). C# cannot test against an open generic type, and
  `o is Pair<object, object>` does not match a `Pair<string, int>`: in the class itself, `o instanceof Pair<?, ?> p`
  (its `equals`) becomes `o is Pair<A, B> p`, with the class's own type parameters.
- **Reference identity.** `==` between two Strings is identity in Java and content equality in C#, so it becomes
  `object.ReferenceEquals(a, b)`. So does `==` on a type parameter's value, which C# does not allow (except against
  `null`).
- **Lambdas and method references.**
  - A lambda becomes `(a, b) => …`.
  - A method reference becomes a method group (`Owner.Method`, `receiver.Method`). For an unbound receiver or a
    constructor it becomes a lambda: `(p0, p1) => p0.M(p1)` or `p0 => new T(p0)`. A result that is a boxed value
    (`Long`, `long?`) into a functional interface whose result is a type parameter (`Func<State, long>`) is unboxed in
    a lambda, where the receiver is a type, `this` or the lambda's parameter: `(State p0) => (long) p0.Result()`.
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

## Messages (`PrintMessage<CSharpPrintCode>`)

Every message has a severity: INFO, BEHAVIOUR_CHANGE, LOSS or ERROR. An ERROR means the file will not compile.
`TestJavaToCSharpMessages` asserts them; each code's javadoc says when it is given.

- **Not translated:** a local class that captures (`LOCAL_CLASS`), an instance initializer (`INSTANCE_INITIALIZER`),
  an enum constant's body and an anonymous class that is neither a lambda nor hoisted (`ANONYMOUS_CLASS`),
  `Outer.this` where no `outer` leads to it (`OUTER_THIS`), `new int[a][b]` (`MULTI_DIMENSIONAL_ARRAY`), a switch
  form C# cannot say (`SWITCH_FORM`), an inner class of a generic type (`NESTED_IN_GENERIC`), an annotation type
  that is not translated (`ANNOTATION_TYPE`), and every JDK type or member without a mapping (`UNMAPPED_JDK`, the
  mapping's work list).
- **Losses:** wildcards (`WILDCARD_AS_BOUND`) and raw types (`RAW_TYPE`), a record's constructor that would clash
  with the positional one (`RECORD_CONSTRUCTOR`), and a JAXB-bound field that `XmlSerializer` cannot write
  (`XML_UNBOUND`).
- **Behaviour changes:** a call through a `List`-typed reference to a subclass of `ArrayList` (`LIST_SUBCLASS`).
- **Information:** an enum printed as a class (`ENUM_AS_CLASS`), a parameter non-nullable as its method's
  precondition (`PRECONDITION_NON_NULL`).
- **Unknown forms:** a form the printer does not know prints as Java, with `JAVA_FALLBACK`.

## Library mappings (`LibraryMapping`, `CSharpLibraries`)

What a library's types, members and fields become in C# is a `LibraryMapping`. The JDK's is `CSharpBcl.JDK`;
slf4j, Jackson, JAXB and Micrometer have mappings of their own, mapping files beside the printer's classes. A translation asks an ordered list of mappings,
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
| micrometer | `system-diagnostics-metrics` | `System.Diagnostics.Metrics`, through the compatibility library's `JavaMetrics`: a global registry that keeps gauges by name and tags (`JavaTags`, equal by content), so that they can be read back and removed as Micrometer's, and exports each name as an `ObservableGauge` of the meter `Maddi.JavaCompat.Micrometer`. Counters and long task timers are that meter's `Counter<double>` and `Histogram<double>`; `JvmMemoryMetrics` reports the managed heap. |
| jaxb | `xml-serialization` | `System.Xml.Serialization`'s `XmlSerializer`, through the binding described below. JAXB's runtime (`JAXBContext`, `Marshaller`, `Unmarshaller` with a schema and validation events, `XmlAdapter`), and the JAXP that goes with it, keep Java's shape in the compatibility library's `JavaXml.cs`: DOM is `XmlDocument`, a parser is `XmlReader`, a `Schema` is an `XmlSchemaSet`, a `Transformer` is an `XslCompiledTransform` or the identity. Where the meanings differ, the library follows Java's: `getNamespaceURI` gives null for no namespace, `renameNode` replaces the element, and output declares the encoding Java declares. |
| jdk | `bcl` | The BCL, plus the compatibility library where Java's behaviour differs. |

A library's annotation becomes what its mapping makes of it (`annotation(use, target, print)`, or `annotation` lines
in a file). Without a mapping, it is dropped.

A mapping is Java (`LibraryMapping`) or text, a `.csmap` file that `CSharpMappingFile` reads: one directive per
line. This is how a client overrides rules without Java:

```
mapping client:uuid-as-string
type java.util.UUID -> string
member java.util.UUID.randomUUID/0 -> Guid.NewGuid().ToString() using System
member org.slf4j.Logger.warn/2 ?lastArgumentThrowable -> $0.LogWarning($2, $1)
member org.slf4j.Logger.warn/2 -> $0.LogWarning($1, $2)
statement java.util.Map.put/2 -> $0.Add($1, $2)
```

- **Directives.** The first line is `mapping <library>:<option>`, the mapping's id.
  - `type`, `plain` (a generic type whose counterpart is not generic) and `pascal` (a class whose members are the
    Java ones in PascalCase) map types; a type, field or override is mapped once per file.
  - `member` and `statement` map calls (`owner.new` for a constructor). A `statement` without a `member` for its key
    overrides only the statement form: the next mapping still translates the call whose value is used.
  - `field` maps fields, and `override` gives a C# name to a translated method that overrides a library method.
  - `annotation <type> [guards] -> <attribute> ][ <attribute>` maps a use of an annotation to attributes, `-` to
    none. Its guards are `?on=<target>` (`type`, `field`, `method`, `constructor`, `parameter`), `?<element>` or
    `?has=<element>` (the use has the element), and `?<element>=<value>` (a constant's name or a string's text); its
    attributes write an element's value as `$<element>`, and apply only when the use has those elements. The rules
    of one annotation are tried in their order.
  - `name <CSharpType>`: a C# type name the mapping writes besides those of its types, so that a type of the
    program with that name is qualified.
  - `nuget <package> <version>` and `compat <file>`: what the translated code needs (below).
  - `exclude <type or package>`: a program's type left out (below).
  - `#` starts a comment, at the start of a line or after a space, outside a string literal.
- **Keys and templates** are the JDK table's.
- **Nullable results.** `->?` instead of `->` says that C# declares the member's result nullable where Java's is not
  known to be: its value is asserted where it goes into what is not nullable.
- **Guards.** A member rule with a guard, `?name`, applies only when the guard holds, and the rules of one key are
  tried in order. The guards are named predicates of the call: `arguments=N`, `receiver=fqn`,
  `lastArgumentThrowable`, `methodReference`. `CSharpMappingFile.guard` registers more.
- **Errors** name the file and the line: an unknown directive or guard, a missing `->`, a second line for a type.

A mapping also states what the translated code needs when the mapping is used (`requirements()`): its NuGet packages
(`nuget <package> <version>` in a file) and the C# sources that go with the translation (`compat <file>`, beside the
mapping file). The JDK mapping's source is the compatibility library. `CSharpLibraries.projectFile` writes a project
that references the packages. The ratchet writes it beside the translation, restores the packages, and gives their
assemblies to the judge.

**Excluded program types, the stubbing mechanism.** A mapping may leave the program's own primary types out:
`exclude <type or package>` (`LibraryMapping.excludes`). An excluded type is not printed, and is no longer the
program's (`CSharpNames.translated`): its uses go through the mapping's rules as a library's do, and those without a
rule are in the census of unmapped uses. This is how code that has no .NET counterpart leaves the translation, with
the calls the rest of the program makes into it: a rule gives the value .NET has (`isGizmoSupported()` is `false`),
and a call that cannot work throws as Java would without the library, with the type the call has,
`JavaUnsupported.Value<{R}>("Gizmo's member accessors")` (`JavaUnsupported.Call` for a statement), from the
compatibility library. A corpus's own mapping is a resource beside the ratchet (`JavaToCSharpRatchet.withMapping`):
`timefold-gizmo.csmap` leaves out Gizmo's bytecode generation.

**What a library adds to a program type.** Some libraries give a program type a meaning that attributes alone do
not carry. A mapping's `additions(type)` are attributes on the type and members in it. Its `memberAttributes(field)`
are attributes on a public field or property, or on an enum's constant. The answers of all mappings are put together;
no single answer wins over the others. JAXB is the case (`JaxbXmlSerialization`). JAXB binds a type's fields, private
ones too, while `XmlSerializer` binds public properties. So every field JAXB binds gets a public property, `XmlName`,
that reads and writes the field and carries the attributes of the field's annotations:

```
[XmlElement("termination", Order = 11)]
public TerminationConfig XmlTerminationConfig { get => this.terminationConfig; set => this.terminationConfig = value; }
```

- **Which types are bound.** A type is bound when it, one of its fields or a superclass has a JAXB annotation.
- **Which fields are bound.** With `@XmlAccessorType(FIELD)` (inherited, or the package's), every instance field
  that is not `transient` or `@XmlTransient` is bound. Otherwise only the annotated fields and the public ones are.
- **Field annotations.**
  - `@XmlElement` becomes `XmlElement`, and `@XmlElements` one `XmlElement(name, typeof(T))` per type.
  - `@XmlElementWrapper` becomes `XmlArray`/`XmlArrayItem`.
  - `@XmlAttribute` becomes `XmlAttribute`, and `@XmlValue` becomes `XmlText`.
  - `propOrder` gives the `Order`.
- **Type annotations.**
  - `@XmlRootElement` becomes `XmlRoot`, `@XmlType` becomes `XmlType` and `@XmlSeeAlso` becomes `XmlInclude`.
  - The namespace is the `@XmlType`'s, or else that of the package's `@XmlSchema` when the schema qualifies its
    elements (`CSharpProgram.packageInfo`).
- **The property's type** is the field's, or what `XmlSerializer` can write:
  - An enum that is a C# enum keeps its type, and its members get `[XmlEnum("NAME")]`.
  - An enum that is a class in C# becomes its name, through `Name` and `ValueOf`.
  - An interface, a self-bounded class's wildcard interface, or a field with `@XmlElements` becomes `object`.
  - With `@XmlJavaTypeAdapter(A.class)`, the property has the adapter's value type and goes through
    `new A().Marshal`/`Unmarshal`.
  - A collection whose elements are converted becomes an array. `XmlSerializer` assigns an array once it has read
    all of it, but it fills a list through its getter, so a converted list would lose what was read.
- **Null values.** A boxed primitive (`int?`), a simple enum or a struct (`Mode?`) is written only when it holds a
  value (`ShouldSerializeXmlName`); as an attribute, which `XmlSerializer` cannot write nullable, it is not bound.
  `XmlSerializer` would write a null as `xsi:nil`, where JAXB leaves the element out.
- **Public members.** The public members of a bound type are `[XmlIgnore]`, because the added properties take their
  place.
- **What stays out.** `XmlSerializer` cannot write some fields: a map without adapter, `@XmlIDREF`, `@XmlAny…`. Such
  a field is left out with an `XML_UNBOUND` message.

The mapping also lists the C# type names it writes (`typeNames()`). A type of the program with one of those names is
qualified, as one whose name two namespaces declare is.

## The JDK → BCL mapping (`CSharpBcl`)

The translated code uses the BCL, not a port of the JDK. The table follows fernflower's JDK use: the census in the
ratchet's report (see below) lists what is left. Its rows are a mapping file, `jdk-bcl.csmap` beside the printer's
classes, in the format a client's `.csmap` has; `CSharpBcl` reads it, and asks its own choosers first, the rules
that depend on the call: a receiver that is a read-only parameter or a subclass of `ArrayList`, a value type's absence
from a map, a stream's collector.

- **Collections.** `List`, `ArrayList`, `LinkedList` and the deques become `List<T>`. `Map` becomes
  `IDictionary<K, V>` and `Set` becomes `ISet<T>`, because Java implements them with hash and tree collections
  alike. `HashMap` becomes `Dictionary<K, V>`, `LinkedHashMap` the insertion-ordered `OrderedDictionary<K, V>`,
  `TreeMap` and `EnumMap` `SortedDictionary<K, V>`, and the hash sets `HashSet<T>`. Where the modification analysis
  proves a collection unmodified, a read-only interface (`IReadOnlyList<T>`) is the analysis's refinement. `Collection` becomes `ICollection<T>`, `Iterable` becomes `IEnumerable<T>`, and `Map.Entry`
  becomes `KeyValuePair<K, V>`.
- **Navigable and sequenced collections.** `NavigableMap` and `NavigableSet` are their one implementation's,
  `SortedDictionary<K, V>` and `SortedSet<T>`; `SequencedMap` and `SequencedSet` are `OrderedDictionary<K, V>` and
  `HashSet<T>`, an `EnumSet` a `HashSet<T>`. The navigation is the compatibility library's (`JavaNavigable`):
  `floorEntry`, `higherKey`, `subMap`, … on a `SortedDictionary`, `floor`, `higher`, … on a `SortedSet`. An entry it
  returns is a `JavaEntry`, null when there is none, and a local it initializes is `var` (a `KeyValuePair` cannot be
  null). Two differences: a `SortedDictionary` searches by order in a walk over its keys, not in log time, and a
  sub-map is a copy, not a view; a key or element of a value type is its default, not null, when there is none.
- **Wildcards.** Java cannot add to a `List<? extends Node>`: it is C#'s covariant read-only interface,
  `IReadOnlyList<Node>`, which a `List<Block>` is. `Collection` and `Set` of `? extends T` are
  `IReadOnlyCollection<T>`, `Iterable` is `IEnumerable<T>`. A collection of `?` is C#'s non-generic interface, which every generic collection implements: `List<?>` is
  `IList`, `Collection<?>` and `Set<?>` are `ICollection`, `Iterable<?>` is `IEnumerable`, `Map<?, ?>` is
  `IDictionary`, `Comparable<?>` is `IComparable`. A method's parameter of a program type with wildcards, `Context<?> c`, is captured as Java does
  in the body: the method gets a type parameter of its own, `void OnRequest<TEmbedded>(Context<TEmbedded> c)`,
  constrained by the wildcard's bound or the declared one (`CSharpWildcards`). Not in a constructor, a functional
  interface's method, an override of a library method, or when the bound is no valid C# constraint (string, a
  delegate, a sealed class, a type parameter). Any other `?` of a program type is the type's wildcard interface, as a
  self-bounded type's is (below): the type parameters the program declares with `?`, in a field, a constructor's
  parameter, a result, a local, a cast, a bound or a type argument, are removed. `TupleLifecycle<?>` is
  `ITupleLifecycle` beside `ITupleLifecycle<Tuple_>`, which `ITupleLifecycle<IUniTuple<A>>` is and
  `ITupleLifecycle<ITuple>` is not. A class's wildcard interface declares its internal members too, Java's
  package-private ones, and the overrides of a library's methods; a result C# writes without arguments (`Class<A>`'s
  `Type`) is kept, and the fields the code reads through the wildcard, as properties of the field's name
  (`Type IMoveSelectorConfig.filterClass => filterClass;`). Only where it declares every member the code reaches
  through such a wildcard: a field written through it, or a method it cannot declare, keeps the type's wildcards their
  bound. An interface whose statics have a companion, of
  the same name (`ITupleLifecycle`), has them in its wildcard interface, which C# lets have static members. Where a
  class's superclass is expected, its wildcard interface is cast to it, `(AbstractNode) node`: the interface cannot
  extend the class. A wildcard of a library type,
  and `? extends B` (what the code stores there is a `B`), is its bound (`WILDCARD_AS_BOUND`).
- **Self-bounded types.** A wildcard of a type whose parameter is bounded by itself, `Score<?>` of
  `Score<Score_ extends Score<Score_>>`, has no C# type argument: none satisfies `where Score_ : IScore<Score_>`. As
  C#'s `IComparable` beside `IComparable<T>`, such a type has a wildcard interface without its self-bounded parameters
  (`CSharpWildcardInterfaces`): `IScore` beside `IScore<Score_>`, `IDirector<Solution_>` beside
  `IDirector<Solution_, Score_>`; a class's or a record's wildcard interface is the interface `I` + its name, a
  record's with its components as properties (`IScore Raw { get; }`). The wildcard, and the raw type, is the wildcard
  interface. It declares the type's own members that do not take the removed parameter, with a result of it erased to
  its bound's wildcard interface (`IScore Negate();`); the type implements them explicitly, calling its own
  (`IScore IScore.Negate() => Negate();`, a default implementation in an interface), so that every `IScore<S>` is an
  `IScore`. In an instance member of the type itself, a cast to or a local of its wildcard is the type with its own
  type parameters, which has the private members the wildcard interface has not: Java's `equals`,
  `MinCollector<?, ?> that = (MinCollector<?, ?>) o`, is `MinCollector<A, P> that = (MinCollector<A, P>) o`, after the
  `GetType()` comparison that C# makes with the type arguments. A type that is `Comparable<Score_>`, or comparable
  with itself (`InnerScore<Score_>` a `Comparable<InnerScore<Score_>>`), has C#'s
  non-generic `IComparable` in its wildcard interface, which it implements by a cast
  (`int IComparable.CompareTo(object other) => CompareTo((Score_) other!);`). A member whose result has the removed
  parameter inside another type (`List<Score_>`) is not the wildcard interface's. The wildcard interface extends those
  of the type's self-bounded supertypes, and its plain interfaces. (The *companion* is another thing: the static class
  beside a generic type, for its static members.)
- **Streams and Optional.** Streams become LINQ over `IEnumerable<T>`: `filter`/`map`/`collect(toList())` become
  `Where`/`Select`/`ToList()`. `Optional<T>` becomes the value itself or null: `orElse(x)` becomes `?? x`. In a
  nullable context its declarations are `T?`, whatever the analysis says of the Optional itself, and `get()` is
  asserted where its value is dereferenced: `Find(l)!.Length`.
- **Functional interfaces.** These become delegates: `Function<T, R>` → `Func<T, R>`, `Predicate<T>` →
  `Func<T, bool>`, `Runnable` → `Action`, `Comparator<T>` → `Comparison<T>`. Calling their method becomes an
  invocation: `f.apply(x)` becomes `f(x)`. A class of the program that implements one does not derive from the
  delegate, a sealed type (CS0509); where the interface is expected, its instance is the method group:
  `list.sort(new ByLength())` becomes `JavaCollections.Sort(list, new ByLength().Compare)`.
- **Values.** The values of `java.time`, `java.net` and `java.math` become BCL structs, as `Instant` →
  `DateTimeOffset` does:
  - `LocalDate` → `DateOnly`, `LocalTime` → `TimeOnly`, `LocalDateTime` → `DateTime`, `OffsetDateTime` →
    `DateTimeOffset`, `Duration` → `TimeSpan`.
  - Those structs share no interface for `java.time.temporal`'s arithmetic. A `Temporal` becomes `IComparable`, which
    they all are, and `t.plus(n, unit)`, `t.until(end, unit)` and `t.isSupported(unit)` become the compatibility
    library's `JavaTemporal.Plus`, `Until` and `IsSupported`, which dispatch on the value's type with Java's rules (a
    month that is not complete does not count). A `TemporalUnit` or `ChronoUnit` becomes the enum `JavaChronoUnit`,
    a `Period` the record struct `JavaPeriod`, a `DateTimeException` the BCL's `ArgumentOutOfRangeException`.
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
  `(Boolean) o` used as a condition becomes `(bool) o`. A member of an `X?`'s value casts its receiver:
  `((TimeSpan) limit).TotalMilliseconds`, `((Style) style).CompareTo(Style.And)`; not an enum's constant or a
  library's struct result (`TimeSpan.FromSeconds(1)`), which are the value type itself. A lambda's parameter is the delegate's value type already. A
  `char` switch's `int` labels are `char` literals. Java's `null` where a type parameter is expected becomes
  `default`. An argument of a template's own type parameter keeps its `int?`: `Objects.requireNonNullElse(i, 400)`
  is `i ?? 400`, where `(int) i` would not compile.
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

The compatibility library is C# that goes with the translation, the resources of this module that the mappings
name with `compat`: `JavaCompat.cs` with the JDK's mapping, `JavaLogging.cs` with slf4j's `ms-logging`,
`JavaJson.cs` (Jackson's `TypeReference`) with either Jackson option, `JavaMetrics.cs` with Micrometer's, and
`JavaXml.cs` (JAXB's runtime and JAXP) with JAXB's. All of them are `#nullable enable`: a member that returns null
by contract is `T?`, and a mapping rule that calls one says `->?`. `JavaCompat.cs`'s parts:

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
- `JavaResources` stands for `Class.getResource(AsStream)` and `ClassLoader.getResource(AsStream)`. A resource is
  one of the assembly's embedded resources, and is found by its Java path in one of two ways. Either the manifest
  name is the path itself (an `EmbeddedResource` whose `LogicalName` is the Java path), or the manifest name ends with
  the path, written with dots as MSBuild writes it. A relative name prefers the match whose folders are most like the
  class's namespace. Its `java.net.URL`, a `Uri`, is `resource://assembly/name`, which `OpenStream` reads.
- Java's `byte[]` is `sbyte[]` in C#, so these classes take and give `sbyte[]` and reinterpret it as `byte[]` for the
  BCL (`JavaBytes`).

The translation calls the library only where it needs that behaviour, so idiomatic code does not depend on it.

## The ratchet

`TestJavaToCSharpFernflower`, `TestJavaToCSharpLangchain4j` and `TestJavaToCSharpTimefold` (maddi-run-openjdk,
`slowTest`) translate the main sources of fernflower, langchain4j's core and timefold's solver. Each judges its
translation with `tools/csharp-check`, a small .NET tool on Roslyn that `JavaToCSharpRatchet` builds with
`dotnet build`, so a .NET 10 SDK must be on the `PATH`. The tool parses each file on its own for the syntax errors,
then compiles the syntax-clean files together against the BCL and the mappings' packages. The numbers are held in
`src/test/resources/j2cs/<corpus>.ratchet`: printer crashes, syntax errors, syntax-clean files, compiling files
(files without an error in that compilation), and `unmappedJdkUses`. maddi-mod's `TestJavaToCSharp*Facts` translate
the same corpora with the analyses' facts, under C#'s nullable reference types, and hold `nullableWarnings` too. A
ratchet fails on a regression, and on progress until the better value is written into its file, with a dated note
of the commit that earned it: the ratchet files are this printer's changelog.

The printer reports every JDK type or member it prints without a BCL counterpart as `UNMAPPED_JDK`. Their count is
ratcheted, and `build/j2cs/<corpus>/report.txt` lists them by frequency. That list, together with the names and
members the compiler does not know, is the BCL mapping's work list. The error total is reported but not ratcheted:
as the mapping lets the compiler bind more, it gets to report errors it could not see before.

## Tests

The unit tests need no .NET SDK; the ratchets are what Roslyn judges.

- `TestJavaToCSharpStructure`, `…Bcl`, `…Libraries` and `…Facts` translate one sample per test and assert the
  fragments that are the rule, `contains`. Each writes its samples to `build/csharp-samples/<test>-<sample>.cs`.
- The structural samples are also compared whole with a golden file, `src/test/resources/…/golden/`: a new one is
  written and its test fails until it has been looked at; `-PupdateGolden` rewrites them after an intended change.
- `TestJavaToCSharpMessages` asserts the messages, `TestJavaToCSharpPolicies` the policies besides the default, and
  `TestCSharpMappingFile` a mapping file's errors and the parts of the format only a client's file uses.
