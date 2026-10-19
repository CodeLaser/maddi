/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details. You should have received a copy of the GNU Lesser General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.codelaser.maddi.cst.print.csharp;

import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.expression.MethodCall;
import io.codelaser.maddi.cst.api.expression.MethodReference;
import io.codelaser.maddi.cst.api.expression.VariableExpression;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.print.translate.CstQueries;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * The JDK → BCL mapping: what a JDK type, a call of a JDK method and a JDK field are in C#. Its rules are data, the
 * file {@code jdk-bcl.csmap} ({@link CSharpMappingFile}); the rules it chooses per call, by an argument's or the
 * receiver's type, are its choosers, which it asks first, key by key.
 *
 * <h2>Types</h2>
 * Java's lists are C#'s concrete {@code List<T>}: {@code List}, {@code ArrayList}, {@code LinkedList} and the deques
 * (C#'s {@code IList<T>} is no {@code IReadOnlyList<T>}, which the covariant {@code List<? extends T>} is). Java's
 * {@code Map} and {@code Set}, which hash and tree collections implement alike, are C#'s interfaces,
 * {@code IDictionary<K, V>} and {@code ISet<T>}; their implementations are C#'s counterparts ({@code HashMap} is
 * {@code Dictionary<K, V>}, {@code LinkedHashMap} {@code OrderedDictionary<K, V>}, {@code TreeSet}
 * {@code SortedSet<T>}). Where the modification analysis proves a collection parameter unmodified, a read-only
 * interface ({@code IReadOnlyList<T>}) is the analysis' refinement ({@link CSharpReadOnly}). {@code Collection} is
 * {@code ICollection<T>}, {@code Iterable} and {@code Stream} are {@code IEnumerable<T>}, and the stream operations are
 * LINQ's. The functional interfaces are delegates ({@code Function<T, R>} is {@code Func<T, R>}, {@code Predicate<T>}
 * is {@code Func<T, bool>}, {@code Comparator<T>} is {@code Comparison<T>}), and a call of their method is an
 * invocation. Exceptions are the BCL's ({@code IllegalStateException} is {@code InvalidOperationException}).
 *
 * <h2>Members</h2>
 * A rule is a template, keyed by the declaring type, the name and the number of parameters
 * ({@code java.util.List.get/1}), or, where Java overloads on parameter types, by those
 * ({@code java.util.List.remove(int)}). A call matches the rule of its method or of a method it overrides, the most
 * specific type first: {@code ArrayList.get} is {@code List.get}. In a template:
 * <ul>
 *   <li>{@code $0} is the receiver, {@code $1}, {@code $2}, … the arguments, {@code $*} all arguments;</li>
 *   <li>{@code @1}, {@code @2}, … an argument as an operand (in parentheses unless it is an atom);</li>
 *   <li>{@code $1.2} the second argument of the first argument, which is a call ({@code Collectors.joining(sep)});</li>
 *   <li>{@code {R}} the call's return type, {@code {R0}}, {@code {R1}} its type arguments.</li>
 * </ul>
 * Where Java's method returns a value that C#'s does not ({@code Map.put} returns the previous value, C#'s indexer
 * nothing), a rule has a second template for a call whose value is not used: the idiomatic one. The first, for a
 * value, may need the compatibility library ({@code Maddi.JavaCompat}, see {@link CSharpCompat}), which has the Java
 * behaviour as extension methods; it also has the classes the BCL lacks ({@code DataInputStream}'s big-endian reads,
 * {@code BitSet}), whose members are the Java ones in PascalCase.
 */
final class CSharpBcl implements LibraryMapping {

    /** The JDK's mapping, the last of a translation's {@link CSharpLibraries}. */
    static final CSharpBcl JDK = new CSharpBcl();

    static final String GENERIC = "System.Collections.Generic";
    static final String LINQ = "System.Linq";
    static final String SYSTEM = "System";
    static final String IO = "System.IO";
    static final String REFLECTION = "System.Reflection";
    static final String COMPAT = CSharpCompat.NAMESPACE;

    private CSharpBcl() {
    }

    @Override
    public String id() {
        return "jdk";
    }

    private static final Requirements REQUIREMENTS = new Requirements(List.of(),
            List.of(new Source(CSharpCompat.FILE_NAME, CSharpCompat::source)));

    /** The compatibility library, which the translation calls where it needs Java's behaviour. */
    @Override
    public Requirements requirements() {
        return REQUIREMENTS;
    }

    // ---------------------------------------------------------------- what the printers ask of the JDK

    /** Java's list classes, which a class of the program extends as the compatibility library's JavaArrayList. */
    static final Set<String> LIST_BASES = Set.of("java.util.ArrayList", "java.util.AbstractList", "java.util.LinkedList");

    /**
     * Java's marker interfaces, which C# has no counterpart of: cloning is MemberwiseClone, serialization opt-in, an
     * event listener any type. Left out of a type's bases and a type parameter's constraints.
     */
    /**
     * A class that implements Java's {@code Set} itself, without a superclass: as one extending {@code AbstractSet}, it
     * extends the compatibility library's {@code JavaSet}, whose Java methods it overrides ({@link #JAVA_SET_METHODS}).
     */
    static boolean directSet(TypeInfo t) {
        if (t.isInterface() || t.parentClass() != null && !t.parentClass().isJavaLangObject()) return false;
        return t.interfacesImplemented().stream()
                .anyMatch(i -> i.typeInfo() != null && "java.util.Set".equals(i.typeInfo().fullyQualifiedName()));
    }

    /** The Java methods JavaSet declares, abstract or virtual: name/parameter count. */
    static final Set<String> JAVA_SET_METHODS = Set.of("iterator/0", "size/0", "isEmpty/0", "contains/1", "add/1",
            "remove/1", "clear/0", "containsAll/1", "addAll/1", "removeAll/1", "retainAll/1", "toArray/0");

    static final Set<String> MARKERS = Set.of("java.lang.Cloneable", "java.io.Serializable", "java.util.RandomAccess",
            "java.util.EventListener");

    private static final Set<String> OBJECT_OVERRIDABLE = Set.of("equals/1", "hashCode/0", "toString/0");

    /** One of Object's methods that C#'s object has too, to override: {@code Equals}, {@code GetHashCode}, {@code ToString}. */
    static boolean objectOverridable(MethodInfo m) {
        return OBJECT_OVERRIDABLE.contains(m.name() + "/" + m.parameters().size());
    }

    /** NavigableMap's methods that return an entry, the compatibility library's JavaEntry, null when there is none. */
    private static final List<String> NAVIGATION_ENTRIES = List.of("firstEntry", "lastEntry", "pollFirstEntry",
            "pollLastEntry", "floorEntry", "lowerEntry", "ceilingEntry", "higherEntry");

    /** A JDK method that returns a navigable map's entry, which may be null: no KeyValuePair, which cannot. */
    static boolean navigationEntry(MethodInfo m) {
        return NAVIGATION_ENTRIES.contains(m.name()) && m.typeInfo().fullyQualifiedName().startsWith("java.util.");
    }

    // ---------------------------------------------------------------- types and fields

    /** The rules that are data: types, members, fields, overrides and type names. */
    private static final CSharpMappingFile ROWS = CSharpMappingFile.resource("jdk-bcl.csmap");

    /** The C# counterpart of a JDK type; null when there is none (yet). */
    @Override
    public TypeMapping type(TypeInfo typeInfo) {
        return ROWS.type(typeInfo);
    }

    /** A JDK field in C#, and the namespace it needs; null when there is none. */
    @Override
    public CodeMapping field(FieldInfo fieldInfo) {
        return ROWS.field(fieldInfo);
    }

    /**
     * The C# name of a translated method that overrides a JDK one, when the BCL's member has another name than the
     * PascalCase of the Java one: {@code close()} of an {@code AutoCloseable} is {@code Dispose()}.
     */
    @Override
    public String overrideName(MethodInfo method) {
        return ROWS.overrideName(method);
    }

    /** The names of the BCL types the translation uses, and the System types any C# code may name. */
    @Override
    public Set<String> typeNames() {
        return ROWS.typeNames();
    }

    /**
     * The kinds of java.lang.reflect.Type are one System.Type: a type test is a property pattern,
     * {@code t is Type { IsConstructedGenericType: true } pt}. A wildcard, which .NET's reflection does not have, is
     * the compatibility library's interface, which no Type implements.
     */
    private static final Map<String, String> PATTERNS = Map.of(
            "java.lang.reflect.ParameterizedType", "Type { IsConstructedGenericType: true }",
            "java.lang.reflect.TypeVariable", "Type { IsGenericParameter: true }",
            "java.lang.reflect.GenericArrayType", "Type { IsArray: true }");

    /** The pattern of a type test against a JDK type; null for the type's name. */
    @Override
    public CodeMapping pattern(ParameterizedType tested, ParameterizedType expression) {
        if (tested == null || tested.arrays() > 0 || tested.typeInfo() == null) return null;
        String pattern = PATTERNS.get(tested.typeInfo().fullyQualifiedName());
        if (pattern == null) pattern = classPattern(tested, expression);
        return pattern == null ? null : new CodeMapping(pattern, SYSTEM);
    }

    /**
     * The pattern of {@code t instanceof Class} where {@code t} is a java.lang.reflect.Type: in .NET every kind is a
     * System.Type, a Class the one that is no type parameter and not constructed. Null for any other type test.
     */
    private static String classPattern(ParameterizedType tested, ParameterizedType expression) {
        if (tested == null || expression == null || tested.arrays() > 0 || expression.arrays() > 0
            || tested.typeInfo() == null || expression.typeInfo() == null
            || !"java.lang.Class".equals(tested.typeInfo().fullyQualifiedName())) {
            return null;
        }
        String e = expression.typeInfo().fullyQualifiedName();
        return e.equals("java.lang.reflect.Type") || PATTERNS.containsKey(e)
                ? "Type { IsGenericParameter: false, IsConstructedGenericType: false }" : null;
    }

    // ---------------------------------------------------------------- choosers

    /** A rule chosen per call: by its arguments, by its receiver's type. */
    interface Chooser {
        Rule choose(MethodInfo method, MethodCall call);
    }

    private static final Map<String, Chooser> CHOOSERS = new HashMap<>();

    private static void chooser(String key, Chooser chooser) {
        CHOOSERS.put(key, chooser);
    }

    private static Rule rule(String value, String... namespaces) {
        return new Rule(value, null, List.of(namespaces));
    }

    /** A rule whose C# member declares its result nullable, as the compatibility library's {@code Map.Get}. */
    private static Rule nullable(String value, String... namespaces) {
        return rule(value, namespaces).withNullableResult();
    }

    static {
        // collections
        // Java's contains(Object), remove(Object), containsKey(Object), get(Object) of an Object where the collection's
        // C# method takes its element or key type: the compatibility library's, which tests the object's type;
        // a read-only collection (CSharpReadOnly) has LINQ's Contains
        chooser("java.util.Collection.contains/1", (method, call) -> objectArgument(call) && !readOnlyReceiver(call)
                ? rule("$0.ContainsObject($1)", COMPAT) : readOnlyReceiver(call)
                ? rule("$0.Contains($1)", LINQ) : rule("$0.Contains($1)"));
        Chooser remove = (method, call) -> objectArgument(call) ? rule("$0.RemoveObject($1)", COMPAT) : rule("$0.Remove($1)");
        chooser("java.util.Collection.remove/1", remove);
        chooser("java.util.List.remove(Object)", remove);
        chooser("java.util.Map.containsKey/1", (method, call) -> objectArgument(call)
                ? rule("$0.ContainsKeyObject($1)", COMPAT) : rule("$0.ContainsKey($1)"));
        chooser("java.util.Map.remove/1", (method, call) -> objectArgument(call)
                ? new Rule("$0.RemoveKeyObject($1)", "$0.RemoveKeyObject($1)", List.of(COMPAT)).withNullableResult()
                : new Rule("$0.RemoveAndGet($1)", "$0.Remove($1)", List.of(COMPAT)).withNullableResult());
        // a collection of ? is non-generic in C#: LINQ needs its elements as objects
        chooser("java.util.Collection.stream/0", (method, call) -> unboundTyped(call) ? rule("$0.Cast<object>()", LINQ)
                : rule("$0"));
        // a List has RemoveAll; any other collection, a set or a map's entries, the compatibility library's RemoveIf
        chooser("java.util.Collection.removeIf/1", (method, call) -> listTyped(call)
                ? new Rule("$0.RemoveAll(new Predicate<{T0}>($1)) > 0", "$0.RemoveAll(new Predicate<{T0}>($1))", List.of(SYSTEM))
                : rule("$0.RemoveIf($1)", COMPAT));
        // a program class that extends ArrayList is a JavaArrayList: its virtual Remove(int) reaches an override of
        // remove, where List's RemoveAt would not
        Rule removeAt = new Rule("$0.RemoveAtAndGet($1)", "$0.RemoveAt($1)", List.of(COMPAT));
        Rule removeAtVirtual = rule("$0.Remove($1)");
        chooser("java.util.List.remove(int)", (method, call) -> arrayListSubclass(call) ? removeAtVirtual : removeAt);
        Rule set = new Rule("$0.Set($1, $2)", "$0[$1] = $2", List.of(COMPAT));
        Rule setVirtual = rule("$0.Set($1, $2)", COMPAT);
        chooser("java.util.List.set/2", (method, call) -> arrayListSubclass(call) ? setVirtual : set);
        chooser("java.util.List.indexOf/1", (method, call) -> readOnlyReceiver(call)
                ? rule("JavaCollections.IndexOf($0, $1)", COMPAT) : rule("$0.IndexOf($1)"));
        chooser("java.util.List.subList/2", (method, call) -> readOnlyReceiver(call)
                ? rule("$0.Skip(@1).Take(@2 - @1).ToList()", LINQ) : rule("$0.SubList($1, $2)", COMPAT));
        chooser("java.util.List.of/0", (method, call) -> rule("new List<{R0}>()"));
        chooser("java.util.List.of", (method, call) -> rule("new List<{R0}> { $* }"));
        // a value-type value is null when absent, as Java's Integer; the BCL's GetValueOrDefault is for a concrete
        // dictionary, which is also read-only one; an IDictionary has the compatibility library's Get
        // a read-only dictionary has the BCL's GetValueOrDefault, a value type's absence is null
        chooser("java.util.Map.get/1", (method, call) -> objectArgument(call) && !valueType(call, 1) && !readOnlyReceiver(call)
                ? nullable("$0.GetObject($1)", COMPAT) : readOnlyReceiver(call)
                ? rule(valueType(call, 1) ? "($0.ContainsKey($1) ? $0[$1] : ({R}) null)" : "$0.GetValueOrDefault($1)")
                : valueType(call, 1) ? nullable("$0.GetValueOrNull($1)", COMPAT)
                : interfaceTyped(call) ? nullable("$0.Get($1)", COMPAT) : rule("$0.GetValueOrDefault($1)"));
        // a value type's function gives null to remove, as Java's Integer: the int? variant
        chooser("java.util.Map.computeIfPresent/2", (method, call) -> valueType(call, 1)
                ? nullable("$0.ComputeIfPresentValue($1, $2)", COMPAT) : nullable("$0.ComputeIfPresent($1, $2)", COMPAT));
        chooser("java.util.Map.getOrDefault/2", (method, call) -> interfaceTyped(call) && !readOnlyReceiver(call)
                ? rule("$0.GetOrDefault($1, $2)", COMPAT) : rule("$0.GetValueOrDefault($1, $2)"));
        chooser("java.util.Map.of", (method, call) -> rule(mapOf(call)));
        chooser("java.util.Set.of", (method, call) -> rule("new HashSet<{R0}> { $* }"));
        chooser("java.util.Arrays.asList/1", (method, call) -> call != null && call.parameterExpressions().size() == 1
                                                               && arrayTyped(call.parameterExpressions().getFirst())
                ? rule("new List<{R0}>($1)") : rule("new List<{R0}> { $* }"));

        // strings and numbers
        // an enum: C#'s, or a class with an Ordinal and a Name (CSharpTypePrinter)
        chooser("java.lang.Enum.ordinal/0", (method, call) -> rule(simpleEnum(call) ? "(int) @0" : "$0.Ordinal"));
        chooser("java.lang.Enum.name/0", (method, call) -> rule(simpleEnum(call) ? "$0.ToString()" : "$0.Name"));
        // a boxed number is the value: a cast; a Number is an IConvertible, whose value Convert reads whatever it is
        for (String[] t : new String[][]{{"int", "Int32"}, {"long", "Int64"}, {"double", "Double"}, {"float", "Single"},
                {"short", "Int16"}, {"byte", "SByte"}}) {
            String cast = "(" + ("byte".equals(t[0]) ? "sbyte" : t[0]) + ") @0";
            chooser("java.lang.Number." + t[0] + "Value/0", (method, call) -> numberTyped(call)
                    ? rule("Convert.To" + t[1] + "($0)", SYSTEM) : rule(cast));
        }

        // streams
        // of(T...) given an array: the array itself is the elements
        chooser("java.util.stream.Stream.of/1", (method, call) -> rule(method.isVarargs() && call != null
                && call.parameterExpressions().size() == 1 && arrayTyped(call.parameterExpressions().getFirst())
                ? "$1" : "new[] { $* }"));
        chooser("java.util.stream.IntStream.of/1", (method, call) -> rule(method.isVarargs() && call != null
                && call.parameterExpressions().size() == 1 && arrayTyped(call.parameterExpressions().getFirst())
                ? "$1" : "new int[] { $* }"));
        chooser("java.util.stream.Stream.collect/1", (method, call) -> rule(collect(call), LINQ, COMPAT));

        // system
        // super.clone(): the shallow copy C#'s object makes
        chooser("java.lang.Object.clone/0", (method, call) -> call != null && arrayTyped(call.object())
                ? rule("({R}) @0.Clone()") : rule("$0.MemberwiseClone()"));

        // services
        chooser("java.nio.file.Path.of/2", (method, call) -> rule(call != null && call.parameterExpressions().size() == 1
                ? "new FileInfo($1)" : "new FileInfo(System.IO.Path.Combine($*))", IO));

        // concurrency
        // Task<T> is invariant, and C# infers R from the lambda where Java has it from the target: explicit; a
        // future of Void (allOf's) is a plain Task
        chooser("java.util.concurrent.CompletableFuture.thenApply/1", (method, call) -> rule(voidFuture(call)
                ? "$0.ThenApply<{R0}>($1)" : "$0.ThenApply<{T0}, {R0}>($1)", COMPAT));
        chooser("java.util.concurrent.CompletableFuture.thenCompose/1", (method, call) -> rule(voidFuture(call)
                ? "$0.ThenCompose<{R0}>($1)" : "$0.ThenCompose<{T0}, {R0}>($1)", COMPAT));

        // reflection
        // the parameter types are varargs: none, or an array
        chooser("java.lang.Class.getMethod/2", (method, call) -> rule(call != null && call.parameterExpressions().size() == 1
                ? "$0.GetMethod($1, Type.EmptyTypes)" : "$0.GetMethod($*)", SYSTEM));
        chooser("java.lang.Class.getDeclaredConstructor/1", (method, call) -> rule("$0.GetConstructor("
                + "BindingFlags.Instance | BindingFlags.Public | BindingFlags.NonPublic, "
                + (call != null && call.parameterExpressions().isEmpty() ? "Type.EmptyTypes" : "$*") + ")",
                REFLECTION, SYSTEM));
        chooser("java.lang.reflect.Constructor.newInstance/1", (method, call) -> rule(
                call != null && call.parameterExpressions().isEmpty() ? "$0.Invoke(null)" : "$0.Invoke(new object[] { $* })"));
        chooser("java.lang.reflect.Method.invoke/2", (method, call) -> rule(call != null && call.parameterExpressions().size() == 1
                ? "$0.Invoke($1, null)" : "$0.Invoke($1, new object[] { $+ })"));
        // a key is a chooser's or a row's, not both: the row would never be asked
        CHOOSERS.keySet().forEach(key -> {
            if (ROWS.hasMember(key)) throw new IllegalStateException("jdk-bcl.csmap has a row for chooser " + key);
        });
    }

    // ---------------------------------------------------------------- lookup

    /** The rule of a call of {@code method} (null: the call of a method reference, or none); null when there is none. */
    @Override
    public Rule call(MethodInfo method, MethodCall call) {
        for (MethodInfo candidate : candidates(method)) {
            TypeInfo owner = candidate.typeInfo();
            String base = owner.fullyQualifiedName() + "." + candidate.name();
            for (String key : List.of(base + "(" + signature(candidate) + ")",
                    base + "/" + candidate.parameters().size(), base)) {
                Rule rule = rule(key, method, call);
                if (rule != null) return rule;
            }
            TypeMapping mapping = ROWS.type(owner);
            if (mapping != null && mapping.compat()) {
                // a compatibility class has the Java members, in PascalCase
                String name = CSharpNames.pascal(candidate.name());
                return rule(candidate.isStatic() ? mapping.template() + "." + name + "($*)" : "$0." + name + "($*)",
                        COMPAT);
            }
        }
        return null;
    }

    /** The rule of a JDK constructor, keyed {@code owner.new/arity} or {@code owner.new(signature)}; null without. */
    @Override
    public Rule constructor(MethodInfo constructor) {
        String base = constructor.typeInfo().fullyQualifiedName() + ".new";
        for (String key : List.of(base + "(" + signature(constructor) + ")", base + "/" + constructor.parameters().size())) {
            Rule rule = rule(key, constructor, null);
            if (rule != null) return rule;
        }
        return null;
    }

    /** The rule of one key: its chooser's choice, or its row's. */
    private static Rule rule(String key, MethodInfo method, MethodCall call) {
        Chooser chooser = CHOOSERS.get(key);
        return chooser != null ? chooser.choose(method, call) : ROWS.memberRule(key, method, call);
    }

    /** The method and what it overrides, the most specific declaring type first: ArrayList's, List's, Collection's. */
    static List<MethodInfo> candidates(MethodInfo method) {
        return Stream.concat(Stream.of(method), method.overrides().stream()
                        .sorted(Comparator.comparingInt((MethodInfo m) -> -m.typeInfo().superTypesExcludingJavaLangObject().size())
                                .thenComparing(m -> m.typeInfo().fullyQualifiedName())))
                .distinct().toList();
    }

    /** {@code int,String}: the simple names of the erased parameter types. */
    static String signature(MethodInfo method) {
        StringBuilder sb = new StringBuilder();
        for (ParameterInfo p : method.parameters()) {
            if (!sb.isEmpty()) sb.append(',');
            ParameterizedType t = p.parameterizedType();
            String name = t.isTypeParameter() || t.typeInfo() == null ? "Object" : t.typeInfo().simpleName();
            sb.append(name).append("[]".repeat(t.arrays()));
        }
        return sb.toString();
    }

    // ---------------------------------------------------------------- what the choosers ask of a call

    /** The call's type argument {@code index} (of its return type) is a value type in C#: a boxed primitive, an enum, a struct. */
    /**
     * The call's argument is an Object where the receiver's element or key type, its first type argument, is not:
     * {@code set.contains(o)} of a {@code Set<V>}.
     */
    private static boolean objectArgument(MethodCall call) {
        if (call == null || call.object() == null || call.parameterExpressions().size() != 1) return false;
        ParameterizedType receiver = call.object().parameterizedType();
        ParameterizedType argument = call.parameterExpressions().getFirst().parameterizedType();
        if (receiver == null || receiver.parameters().isEmpty() || argument == null || !argument.isJavaLangObject()) {
            return false;
        }
        ParameterizedType element = receiver.parameters().getFirst();
        return !element.isJavaLangObject() && (element.wildcard() == null || !element.wildcard().isUnbound());
    }

    private static boolean valueType(MethodCall call, int index) {
        if (call == null || call.object() == null) return false;
        ParameterizedType receiver = call.object().parameterizedType();
        if (receiver == null || receiver.parameters().size() <= index) return false;
        ParameterizedType v = receiver.parameters().get(index);
        return CSharpTypeName.valueReference(v);
    }

    /** The receiver is a collection of {@code ?}, which C# has as a non-generic interface. */
    private static boolean unboundTyped(MethodCall call) {
        if (call == null || call.object() == null || call.object().parameterizedType() == null) return false;
        List<ParameterizedType> args = call.object().parameterizedType().parameters();
        return !args.isEmpty() && args.stream().allMatch(a -> a.wildcard() != null && a.wildcard().isUnbound());
    }

    /** The receiver is a C# List. */
    private static boolean listTyped(MethodCall call) {
        if (call == null || call.object() == null || call.object().parameterizedType() == null) return false;
        TypeInfo t = call.object().parameterizedType().typeInfo();
        TypeMapping mapping = t == null ? null : ROWS.type(t);
        return mapping != null && "List".equals(mapping.template());
    }

    /** The receiver is declared as Java's Map interface: C#'s IDictionary, without the BCL's read-only extensions. */
    private static boolean interfaceTyped(MethodCall call) {
        if (call == null || call.object() == null) return true;
        ParameterizedType receiver = call.object().parameterizedType();
        if (receiver == null || receiver.typeInfo() == null) return true;
        TypeMapping mapping = ROWS.type(receiver.typeInfo());
        return mapping == null || mapping.isInterface();
    }

    /** The receiver is an enum C# declares as an enum (only constants), not as a class. */
    private static boolean simpleEnum(MethodCall call) {
        if (call == null || call.object() == null || call.object().parameterizedType() == null) return true;
        TypeInfo t = call.object().parameterizedType().typeInfo();
        return t == null || !t.typeNature().isEnum() || CSharpNames.simpleEnum(t);
    }

    /** The receiver is a future of Void, as CompletableFuture.allOf's: a Task without a value. */
    private static boolean voidFuture(MethodCall call) {
        if (call == null || call.object() == null || call.object().parameterizedType() == null) return false;
        List<ParameterizedType> args = call.object().parameterizedType().parameters();
        return args.size() == 1 && args.getFirst().typeInfo() != null
               && "java.lang.Void".equals(args.getFirst().typeInfo().fullyQualifiedName());
    }

    /** The receiver's type is a class of the program that extends ArrayList, which C# has as a JavaArrayList. */
    private static boolean arrayListSubclass(MethodCall call) {
        if (call == null || call.object() == null || call.object().parameterizedType() == null) return false;
        TypeInfo t = call.object().parameterizedType().typeInfo();
        if (t == null || !CSharpNames.translated(t)) return false;
        return CstQueries.classChain(t).stream().anyMatch(s -> "java.util.ArrayList".equals(s.fullyQualifiedName()));
    }

    /** The receiver is a collection parameter that is read-only in C# ({@link CSharpReadOnly}). */
    private static boolean readOnlyReceiver(MethodCall call) {
        return call != null && call.object() != null
               && CSharpExpressionPrinter.unwrap(call.object()) instanceof Expression e
               && e instanceof VariableExpression ve
               && ve.variable() instanceof ParameterInfo p && CSharpContext.program().readOnly(p);
    }

    /** The receiver is declared as a Number: C#'s IConvertible. */
    private static boolean numberTyped(MethodCall call) {
        return call != null && call.object() != null && call.object().parameterizedType() != null
               && call.object().parameterizedType().typeInfo() != null
               && "java.lang.Number".equals(call.object().parameterizedType().typeInfo().fullyQualifiedName());
    }

    private static boolean arrayTyped(Expression e) {
        return e != null && e.parameterizedType() != null && e.parameterizedType().arrays() > 0;
    }

    /** {@code Map.of(k1, v1, k2, v2)}: a dictionary with an index initializer. */
    private static String mapOf(MethodCall call) {
        if (call == null) return "new Dictionary<{R0}, {R1}>()";
        StringBuilder sb = new StringBuilder("new Dictionary<{R0}, {R1}> { ");
        int n = call.parameterExpressions().size();
        for (int i = 0; i + 1 < n; i += 2) {
            if (i > 0) sb.append(", ");
            sb.append("[$").append(i + 1).append("] = $").append(i + 2);
        }
        return sb.append(n == 0 ? "}" : " }").toString();
    }

    /** {@code LinkedHashMap::new}: a reference to a constructor without parameters. */
    private static boolean newOfNothing(Expression e) {
        return CSharpExpressionPrinter.unwrap(e) instanceof MethodReference mr && mr.methodInfo().isConstructor()
               && mr.methodInfo().parameters().isEmpty() && mr.scope().parameterizedType().arrays() == 0;
    }

        /** {@code stream.collect(Collectors.toList())}: LINQ's ToList(), and so on; the compatibility library otherwise. */
    private static String collect(MethodCall call) {
        if (call != null && call.parameterExpressions().size() == 1
            && CSharpExpressionPrinter.unwrap(call.parameterExpressions().getFirst()) instanceof MethodCall collector
            && "java.util.stream.Collectors".equals(collector.methodInfo().typeInfo().fullyQualifiedName())) {
            int n = collector.parameterExpressions().size();
            switch (collector.methodInfo().name() + "/" + n) {
                case "toList/0", "toUnmodifiableList/0":
                    return "$0.ToList()";
                case "toSet/0", "toUnmodifiableSet/0":
                    return "$0.ToHashSet()";
                case "joining/0":
                    return "string.Concat($0)";
                case "joining/1":
                    return "string.Join($1.1, $0)";
                case "joining/3":
                    return "$1.2 + string.Join($1.1, $0) + $1.3";
                case "toMap/2":
                    return "$0.ToDictionary($1.1, $1.2)";
                // a merge function for a duplicate key, a map supplier: the compatibility library's
                case "toMap/3":
                    return "$0.ToMap($1.1, $1.2, $1.3)";
                // LinkedHashMap::new, whose type arguments the reference does not have: the result's, which Java
                // infers from it
                case "toMap/4":
                    return "$0.ToMap($1.1, $1.2, $1.3, " + (newOfNothing(collector.parameterExpressions().get(3))
                            ? "() => new {R}())" : "$1.4)");
                case "toCollection/1":
                    return "$0.ToCollection(" + (newOfNothing(collector.parameterExpressions().getFirst())
                            ? "() => new {R}())" : "$1.1)");
                case "groupingBy/1":
                    return "$0.GroupBy($1.1).ToDictionary(g => g.Key, g => g.ToList())";
                case "counting/0":
                    return "$0.LongCount()";
                default:
                    break;
            }
        }
        return "$0.Collect($1)";
    }
}
