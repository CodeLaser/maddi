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
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.type.ParameterizedType;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * The JDK → BCL mapping: what a JDK type, a call of a JDK method and a JDK field are in C#.
 *
 * <h2>Types</h2>
 * Java's collection interfaces are C#'s concrete collections: {@code List}, {@code ArrayList}, {@code LinkedList} and
 * the deques are {@code List<T>}; {@code Map} and its hash maps are {@code Dictionary<K, V>}; {@code Set} and its hash
 * sets are {@code HashSet<T>}. That is how C# code declares them; where the modification analysis proves a collection
 * unmodified, a read-only interface ({@code IReadOnlyList<T>}) is the analysis' refinement. {@code Collection} is
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
final class CSharpBcl {

    static final String GENERIC = "System.Collections.Generic";
    static final String LINQ = "System.Linq";
    static final String SYSTEM = "System";
    static final String TEXT = "System.Text";
    static final String IO = "System.IO";
    static final String REGEX = "System.Text.RegularExpressions";
    static final String GLOBALIZATION = "System.Globalization";
    static final String THREADING = "System.Threading";
    static final String COMPAT = CSharpCompat.NAMESPACE;

    private CSharpBcl() {
    }

    // ---------------------------------------------------------------- types

    /**
     * A JDK type in C#. {@code template}: the name, which gets the Java type arguments, or a pattern of them:
     * {@code {0}} is the first, as a type argument ({@code int}), {@code {0?}} as a type ({@code int?}).
     * {@code compat}: a class of the compatibility library, whose members are the Java ones in PascalCase.
     */
    record TypeMapping(String template, String namespace, boolean compat, boolean dropArguments) {
    }

    private static final Map<String, TypeMapping> TYPES = new HashMap<>();

    private static void type(String javaFqn, String template, String namespace) {
        TYPES.put(javaFqn, new TypeMapping(template, namespace, false, false));
    }

    /** A generic JDK type whose C# counterpart is not generic: {@code Class<?>} is {@code Type}. */
    private static void plain(String javaFqn, String name, String namespace) {
        TYPES.put(javaFqn, new TypeMapping(name, namespace, false, true));
    }

    private static void compatType(String javaFqn, String name) {
        TYPES.put(javaFqn, new TypeMapping(name, COMPAT, true, false));
    }

    static {
        // C#'s List, the concrete type: its IList does not implement IReadOnlyList, which the covariant List<? extends T>
        // is (see CSharpTypeName)
        for (String list : List.of("java.util.List", "java.util.ArrayList", "java.util.LinkedList",
                "java.util.AbstractList", "java.util.Deque", "java.util.ArrayDeque", "java.util.Queue",
                "java.util.SequencedCollection")) {
            type(list, "List", GENERIC);
        }
        // Java's Map and Set are implemented by hash and tree collections alike: C#'s interfaces. A HashMap is C#'s
        // Dictionary; a LinkedHashMap keeps its insertion order, as OrderedDictionary does; an EnumMap iterates in
        // the order of its keys, as SortedDictionary does.
        type("java.util.Map", "IDictionary", GENERIC);
        type("java.util.AbstractMap", "IDictionary", GENERIC);
        for (String map : List.of("java.util.HashMap", "java.util.IdentityHashMap", "java.util.concurrent.ConcurrentHashMap")) {
            type(map, "Dictionary", GENERIC);
        }
        type("java.util.LinkedHashMap", "OrderedDictionary", GENERIC);
        type("java.util.EnumMap", "SortedDictionary", GENERIC);
        type("java.util.TreeMap", "SortedDictionary", GENERIC);
        type("java.util.SortedMap", "SortedDictionary", GENERIC);
        type("java.util.Set", "ISet", GENERIC);
        type("java.util.AbstractSet", "ISet", GENERIC);
        // .NET's HashSet enumerates in insertion order as long as nothing is removed
        type("java.util.HashSet", "HashSet", GENERIC);
        type("java.util.LinkedHashSet", "HashSet", GENERIC);
        type("java.util.TreeSet", "SortedSet", GENERIC);
        type("java.util.SortedSet", "SortedSet", GENERIC);
        type("java.util.Collection", "ICollection", GENERIC);
        type("java.util.AbstractCollection", "ICollection", GENERIC);
        type("java.lang.Iterable", "IEnumerable", GENERIC);
        type("java.util.Map.Entry", "KeyValuePair", GENERIC);
        // a class of the program may extend these, which it cannot do with KeyValuePair, a struct
        compatType("java.util.AbstractMap.SimpleEntry", "JavaEntry");
        compatType("java.util.AbstractMap.SimpleImmutableEntry", "JavaEntry");
        type("java.util.stream.Stream", "IEnumerable", GENERIC);
        type("java.util.stream.IntStream", "IEnumerable<int>", GENERIC);
        type("java.util.Optional", "{0?}", null);
        type("java.util.OptionalInt", "int?", null);
        type("java.util.OptionalLong", "long?", null);
        type("java.util.OptionalDouble", "double?", null);
        type("java.time.Instant", "DateTimeOffset", SYSTEM);
        type("java.lang.ThreadLocal", "ThreadLocal", THREADING);
        type("java.util.regex.Pattern", "Regex", REGEX);
        compatType("java.util.regex.Matcher", "JavaMatcher");
        type("java.io.FileInputStream", "FileStream", IO);
        type("java.io.FileOutputStream", "FileStream", IO);
        type("java.io.BufferedInputStream", "BufferedStream", IO);
        type("java.io.BufferedOutputStream", "BufferedStream", IO);
        type("java.io.OutputStreamWriter", "StreamWriter", IO);
        type("java.io.InputStreamReader", "StreamReader", IO);
        type("java.io.BufferedReader", "TextReader", IO);
        type("java.io.Reader", "TextReader", IO);

        type("java.util.function.Function", "Func", SYSTEM);
        type("java.util.function.BiFunction", "Func", SYSTEM);
        type("java.util.function.Supplier", "Func", SYSTEM);
        type("java.util.function.Consumer", "Action", SYSTEM);
        type("java.util.function.BiConsumer", "Action", SYSTEM);
        type("java.util.function.Predicate", "Func<{0}, bool>", SYSTEM);
        type("java.util.function.BiPredicate", "Func<{0}, {1}, bool>", SYSTEM);
        type("java.util.function.UnaryOperator", "Func<{0}, {0}>", SYSTEM);
        type("java.util.function.BinaryOperator", "Func<{0}, {0}, {0}>", SYSTEM);
        type("java.util.function.IntFunction", "Func<int, {0}>", SYSTEM);
        type("java.util.function.ToIntFunction", "Func<{0}, int>", SYSTEM);
        type("java.util.function.IntPredicate", "Func<int, bool>", SYSTEM);
        type("java.lang.Runnable", "Action", SYSTEM);
        type("java.util.concurrent.Callable", "Func", SYSTEM);
        type("java.util.Comparator", "Comparison", SYSTEM);

        type("java.lang.StringBuilder", "StringBuilder", TEXT);
        type("java.lang.AbstractStringBuilder", "StringBuilder", TEXT);
        type("java.lang.CharSequence", "string", null);
        plain("java.lang.Class", "Type", SYSTEM);
        type("java.lang.Comparable", "IComparable", SYSTEM);
        type("java.lang.AutoCloseable", "IDisposable", SYSTEM);
        type("java.io.Closeable", "IDisposable", SYSTEM);
        type("java.lang.Thread", "Thread", THREADING);
        type("java.util.Locale", "CultureInfo", GLOBALIZATION);
        type("java.nio.charset.Charset", "Encoding", TEXT);

        type("java.lang.Throwable", "Exception", SYSTEM);
        type("java.lang.Exception", "Exception", SYSTEM);
        type("java.lang.RuntimeException", "Exception", SYSTEM);
        type("java.lang.Error", "Exception", SYSTEM);
        type("java.lang.AssertionError", "Exception", SYSTEM);
        type("java.lang.IllegalArgumentException", "ArgumentException", SYSTEM);
        type("java.lang.IllegalStateException", "InvalidOperationException", SYSTEM);
        type("java.lang.UnsupportedOperationException", "NotSupportedException", SYSTEM);
        type("java.lang.NullPointerException", "NullReferenceException", SYSTEM);
        type("java.lang.IndexOutOfBoundsException", "ArgumentOutOfRangeException", SYSTEM);
        type("java.lang.StringIndexOutOfBoundsException", "ArgumentOutOfRangeException", SYSTEM);
        type("java.lang.ArrayIndexOutOfBoundsException", "IndexOutOfRangeException", SYSTEM);
        type("java.lang.ArrayIndexOutOfBoundsException", "IndexOutOfRangeException", SYSTEM);
        type("java.lang.ClassCastException", "InvalidCastException", SYSTEM);
        type("java.lang.NumberFormatException", "FormatException", SYSTEM);
        type("java.lang.ArithmeticException", "ArithmeticException", SYSTEM);
        type("java.lang.CloneNotSupportedException", "NotSupportedException", SYSTEM);
        type("java.lang.InterruptedException", "ThreadInterruptedException", THREADING);
        type("java.util.NoSuchElementException", "InvalidOperationException", SYSTEM);
        type("java.io.IOException", "IOException", IO);
        type("java.io.UncheckedIOException", "IOException", IO);
        type("java.io.FileNotFoundException", "FileNotFoundException", IO);

        type("java.io.InputStream", "Stream", IO);
        type("java.io.OutputStream", "Stream", IO);
        type("java.io.PrintStream", "TextWriter", IO);
        type("java.io.PrintWriter", "TextWriter", IO);
        type("java.io.Writer", "TextWriter", IO);
        type("java.io.StringWriter", "StringWriter", IO);

        compatType("java.io.DataInputStream", "DataInputStream");
        compatType("java.io.ByteArrayInputStream", "ByteArrayInputStream");
        compatType("java.io.ByteArrayOutputStream", "ByteArrayOutputStream");
        compatType("java.util.BitSet", "BitSet");
        // an interface, which the program's iterators implement
        compatType("java.util.Iterator", "IJavaIterator");
        compatType("java.util.ListIterator", "IJavaIterator");
        compatType("java.io.File", "JavaFile");
        compatType("java.util.Enumeration", "JavaEnumeration");
        compatType("java.util.zip.ZipFile", "JavaZipFile");
        compatType("java.util.zip.ZipEntry", "JavaZipEntry");
        compatType("java.util.zip.ZipOutputStream", "JavaZipOutputStream");
        compatType("java.util.jar.JarFile", "JavaJarFile");
        compatType("java.util.jar.JarEntry", "JavaZipEntry");
        compatType("java.util.jar.Manifest", "JavaManifest");
    }

    /** The C# counterpart of a JDK type; null when there is none (yet). */
    /** The names of the BCL types the translation uses, and the System types any C# code may name. */
    static Set<String> typeNames() {
        Set<String> names = new java.util.HashSet<>(Set.of("Exception", "Object", "String", "Math", "Console", "Type",
                "Attribute", "Enum", "Array", "Action", "Func", "Task", "Path", "File", "Directory", "Stream", "Encoding",
                "Regex", "Thread", "Monitor", "Debug", "Enumerable", "Comparer", "Random", "Guid", "Uri", "TimeSpan",
                "DateTime", "DateTimeOffset", "Convert", "Environment", "Buffer", "Delegate", "Version", "Index", "Range"));
        for (TypeMapping m : TYPES.values()) {
            String t = m.template();
            int cut = t.indexOf('<');
            if (cut >= 0) t = t.substring(0, cut);
            if (!t.isEmpty() && Character.isUpperCase(t.charAt(0))) names.add(t);
        }
        return names;
    }

    static TypeMapping type(TypeInfo typeInfo) {
        return TYPES.get(typeInfo.fullyQualifiedName());
    }

    // ---------------------------------------------------------------- members

    /** A member's translation: {@code value} for a call whose value is used, {@code statement} for one whose is not. */
    record Rule(String value, String statement, List<String> namespaces) {
        String template(boolean asStatement) {
            return asStatement && statement != null ? statement : value;
        }
    }

    /** A rule chosen per call: by its arguments, by its return type. */
    interface Chooser {
        Rule choose(MethodInfo method, MethodCall call);
    }

    private static final Map<String, Chooser> MEMBERS = new HashMap<>();

    private static void m(String key, String value, String... namespaces) {
        Rule rule = new Rule(value, null, List.of(namespaces));
        MEMBERS.put(key, (method, call) -> rule);
    }

    /** A member with a template for a call whose value is used, and one for a call whose value is not. */
    private static void ms(String key, String value, String statement, String... namespaces) {
        Rule rule = new Rule(value, statement, List.of(namespaces));
        MEMBERS.put(key, (method, call) -> rule);
    }

    private static void chooser(String key, Chooser chooser) {
        MEMBERS.put(key, chooser);
    }

    private static Rule rule(String value, String... namespaces) {
        return new Rule(value, null, List.of(namespaces));
    }

    static {
        collections();
        strings();
        numbers();
        streams();
        system();
    }

    private static void collections() {
        // List, and the deques, which are C#'s List<T> too
        m("java.util.List.get/1", "$0[$1]");
        m("java.util.Collection.size/0", "$0.Count");
        m("java.util.Map.size/0", "$0.Count");
        m("java.util.Collection.isEmpty/0", "$0.Count == 0");
        m("java.util.Map.isEmpty/0", "$0.Count == 0");
        m("java.util.Collection.add/1", "$0.Add($1)");
        m("java.util.Collection.contains/1", "$0.Contains($1)");
        m("java.util.Collection.remove/1", "$0.Remove($1)");
        m("java.util.Collection.clear/0", "$0.Clear()");
        m("java.util.Map.clear/0", "$0.Clear()");
        ms("java.util.Collection.addAll/1", "$0.AddAll($1)", "$0.AddAll($1)", COMPAT);
        m("java.util.Collection.containsAll/1", "@1.All($0.Contains)", LINQ);
        // a collection of ? is non-generic in C#: LINQ needs its elements as objects
        chooser("java.util.Collection.stream/0", (method, call) -> unboundTyped(call) ? rule("$0.Cast<object>()", LINQ)
                : rule("$0"));
        // a List has RemoveAll; any other collection, a set or a map's entries, the compatibility library's RemoveIf
        chooser("java.util.Collection.removeIf/1", (method, call) -> listTyped(call)
                ? new Rule("$0.RemoveAll(new Predicate<{T0}>($1)) > 0", "$0.RemoveAll(new Predicate<{T0}>($1))", List.of(SYSTEM))
                : rule("$0.RemoveIf($1)", COMPAT));
        ms("java.util.Collection.removeAll/1", "$0.RemoveAllOf($1)", "$0.RemoveAllOf($1)", COMPAT);
        ms("java.util.Collection.retainAll/1", "$0.RetainAll($1)", "$0.RetainAll($1)", COMPAT);
        m("java.lang.Iterable.forEach/1", "$0.ForEach($1)", COMPAT);
        m("java.lang.Iterable.iterator/0", "$0.Iterator()", COMPAT);
        m("java.util.Collection.iterator/0", "$0.Iterator()", COMPAT);
        m("java.util.List.listIterator/0", "$0.Iterator()", COMPAT);

        m("java.util.List.add/2", "$0.Insert($1, $2)");
        ms("java.util.List.remove(int)", "$0.RemoveAtAndGet($1)", "$0.RemoveAt($1)", COMPAT);
        m("java.util.List.remove(Object)", "$0.Remove($1)");
        ms("java.util.List.addAll/1", "$0.AddAll($1)", "$0.AddRange($1)", COMPAT);
        m("java.util.List.addAll/2", "$0.InsertAll($1, $2)", COMPAT);
        ms("java.util.List.set/2", "$0.Set($1, $2)", "$0[$1] = $2", COMPAT);
        m("java.util.List.indexOf/1", "$0.IndexOf($1)");
        m("java.util.List.lastIndexOf/1", "$0.LastIndexOf($1)", COMPAT);
        m("java.util.List.subList/2", "$0.SubList($1, $2)", COMPAT);
        // Java's sorts are stable, List.Sort is not
        m("java.util.List.sort/1", "JavaCollections.Sort($0, $1)", COMPAT);
        m("java.util.List.getFirst/0", "$0[0]");
        m("java.util.List.getLast/0", "$0[^1]");
        m("java.util.SequencedCollection.getFirst/0", "$0[0]");
        m("java.util.SequencedCollection.getLast/0", "$0[^1]");
        m("java.util.List.of/1", "new List<{R0}> { $* }");
        chooser("java.util.List.of/0", (method, call) -> rule("new List<{R0}>()"));
        m("java.util.List.copyOf/1", "new List<{R0}>($1)");
        for (String deque : List.of("java.util.Deque", "java.util.LinkedList", "java.util.ArrayDeque", "java.util.Queue")) {
            m(deque + ".getFirst/0", "$0[0]");
            m(deque + ".getLast/0", "$0[^1]");
            m(deque + ".peekFirst/0", "$0.PeekFirst()", COMPAT);
            m(deque + ".peekLast/0", "$0.PeekLast()", COMPAT);
            m(deque + ".peek/0", "$0.PeekFirst()", COMPAT);
            m(deque + ".addFirst/1", "$0.Insert(0, $1)");
            m(deque + ".push/1", "$0.Insert(0, $1)");
            m(deque + ".addLast/1", "$0.Add($1)");
            ms(deque + ".offer/1", "$0.Offer($1)", "$0.Add($1)", COMPAT);
            ms(deque + ".offerLast/1", "$0.Offer($1)", "$0.Add($1)", COMPAT);
            ms(deque + ".removeFirst/0", "$0.RemoveFirst()", "$0.RemoveAt(0)", COMPAT);
            ms(deque + ".pop/0", "$0.RemoveFirst()", "$0.RemoveAt(0)", COMPAT);
            ms(deque + ".removeLast/0", "$0.RemoveLast()", "$0.RemoveAt($0.Count - 1)", COMPAT);
            m(deque + ".poll/0", "$0.PollFirst()", COMPAT);
            m(deque + ".pollFirst/0", "$0.PollFirst()", COMPAT);
            m(deque + ".pollLast/0", "$0.PollLast()", COMPAT);
            m(deque + ".element/0", "$0[0]");
            ms(deque + ".remove/0", "$0.RemoveFirst()", "$0.RemoveAt(0)", COMPAT);
        }

        // Map
        ms("java.util.Map.put/2", "$0.Put($1, $2)", "$0[$1] = $2", COMPAT);
        // a value-type value is null when absent, as Java's Integer; the BCL's GetValueOrDefault is for a concrete
        // dictionary, which is also read-only one; an IDictionary has the compatibility library's Get
        chooser("java.util.Map.get/1", (method, call) -> valueType(call, 1) ? rule("$0.GetValueOrNull($1)", COMPAT)
                : interfaceTyped(call) ? rule("$0.Get($1)", COMPAT) : rule("$0.GetValueOrDefault($1)"));
        chooser("java.util.Map.getOrDefault/2", (method, call) -> interfaceTyped(call)
                ? rule("$0.GetOrDefault($1, $2)", COMPAT) : rule("$0.GetValueOrDefault($1, $2)"));
        m("java.util.Map.containsKey/1", "$0.ContainsKey($1)");
        m("java.util.Map.containsValue/1", "$0.Values.Contains($1)");
        m("java.util.Map.entrySet/0", "$0");
        m("java.util.Map.keySet/0", "$0.Keys");
        m("java.util.Map.values/0", "$0.Values");
        ms("java.util.Map.remove/1", "$0.RemoveAndGet($1)", "$0.Remove($1)", COMPAT);
        m("java.util.Map.putAll/1", "$0.PutAll($1)", COMPAT);
        ms("java.util.Map.putIfAbsent/2", "$0.PutIfAbsent($1, $2)", "$0.TryAdd($1, $2)", COMPAT);
        m("java.util.Map.computeIfAbsent/2", "$0.ComputeIfAbsent($1, $2)", COMPAT);
        m("java.util.Map.compute/2", "$0.Compute($1, $2)", COMPAT);
        m("java.util.Map.merge/3", "$0.Merge($1, $2, $3)", COMPAT);
        m("java.util.Map.entry/2", "KeyValuePair.Create($1, $2)");
        m("java.util.Map.ofEntries/1", "new Dictionary<{R0}, {R1}>(new[] { $* })");
        chooser("java.util.Map.of", (method, call) -> rule(mapOf(call)));
        m("java.util.Map.Entry.getKey/0", "$0.Key");
        m("java.util.Map.Entry.getValue/0", "$0.Value");

        // Set
        ms("java.util.Set.addAll/1", "$0.AddAll($1)", "$0.UnionWith($1)", COMPAT);
        ms("java.util.Set.removeAll/1", "$0.RemoveAllOf($1)", "$0.ExceptWith($1)", COMPAT);
        ms("java.util.Set.retainAll/1", "$0.RetainAll($1)", "$0.IntersectWith($1)", COMPAT);
        m("java.util.Set.of/1", "new HashSet<{R0}> { $* }");
        chooser("java.util.Set.of", (method, call) -> rule("new HashSet<{R0}> { $* }"));

        // Collections, Arrays, Objects
        m("java.util.Collections.emptyList/0", "new List<{R0}>()");
        m("java.util.Collections.emptySet/0", "new HashSet<{R0}>()");
        m("java.util.Collections.emptyMap/0", "new Dictionary<{R0}, {R1}>()");
        m("java.util.Collections.singletonList/1", "new List<{R0}> { $1 }");
        m("java.util.Collections.singleton/1", "new HashSet<{R0}> { $1 }");
        m("java.util.Collections.singletonMap/2", "new Dictionary<{R0}, {R1}> { [$1] = $2 }");
        m("java.util.Collections.nCopies/2", "Enumerable.Repeat<{R0}>($2, $1).ToList()", LINQ);
        m("java.util.Map.Entry.comparingByKey/0", "JavaComparator.ComparingByKey<{R0.0}, {R0.1}>()", COMPAT);
        m("java.util.Map.Entry.comparingByValue/0", "JavaComparator.ComparingByValue<{R0.0}, {R0.1}>()", COMPAT);
        m("java.util.Collections.sort/1", "JavaCollections.Sort($1)", COMPAT);
        m("java.util.Collections.sort/2", "JavaCollections.Sort($1, $2)", COMPAT);
        // LINQ's Reverse would be a new sequence: in place
        m("java.util.Collections.reverse/1", "JavaCollections.ReverseInPlace($1)", COMPAT);
        m("java.util.Collections.unmodifiableList/1", "$1");
        m("java.util.Collections.unmodifiableSet/1", "$1");
        m("java.util.Collections.unmodifiableMap/1", "$1");
        m("java.util.Collections.unmodifiableCollection/1", "$1");
        m("java.util.Collections.synchronizedMap/1", "$1");
        m("java.util.Collections.addAll/2", "$1.AddAll($+)", COMPAT);
        chooser("java.util.Arrays.asList/1", (method, call) -> call != null && call.parameterExpressions().size() == 1
                                                               && arrayTyped(call.parameterExpressions().getFirst())
                ? rule("new List<{R0}>($1)") : rule("new List<{R0}> { $* }"));
        m("java.util.Arrays.copyOf/2", "JavaArrays.CopyOf($1, $2)", COMPAT);
        m("java.util.Arrays.copyOfRange/3", "$1[@2..@3]");
        m("java.util.Arrays.stream/1", "$1");
        m("java.util.Arrays.equals/2", "JavaArrays.Equals($1, $2)", COMPAT);
        m("java.util.Arrays.hashCode/1", "JavaArrays.HashCode($1)", COMPAT);
        m("java.util.Arrays.toString/1", "JavaArrays.ToString($1)", COMPAT);
        m("java.util.Arrays.fill/2", "Array.Fill($1, $2)", SYSTEM);
        m("java.util.Arrays.sort/1", "Array.Sort($1)", SYSTEM);
        m("java.util.Objects.equals/2", "object.Equals($1, $2)");
        m("java.util.Objects.hash/1", "HashCode.Combine($*)", SYSTEM);
        m("java.util.Objects.hashCode/1", "($1?.GetHashCode() ?? 0)");
        m("java.util.Objects.isNull/1", "$1 == null");
        m("java.util.Objects.nonNull/1", "$1 != null");
        ms("java.util.Objects.requireNonNull/1", "$1 ?? throw new NullReferenceException()",
                "_ = $1 ?? throw new NullReferenceException()", SYSTEM);
        ms("java.util.Objects.requireNonNull/2", "$1 ?? throw new NullReferenceException($2)",
                "_ = $1 ?? throw new NullReferenceException($2)", SYSTEM);
        m("java.util.Objects.requireNonNullElse/2", "$1 ?? $2");
        m("java.util.Objects.toString/1", "JavaString.ValueOf($1)", COMPAT);
    }

    private static void strings() {
        String s = "java.lang.String.";
        m(s + "length/0", "$0.Length");
        m(s + "charAt/1", "$0[$1]");
        m(s + "isEmpty/0", "$0.Length == 0");
        m(s + "isBlank/0", "string.IsNullOrWhiteSpace($0)");
        m(s + "substring/1", "$0.Substring($1)");
        m(s + "substring/2", "$0[@1..@2]");
        m(s + "replace/2", "$0.Replace($1, $2)");
        m(s + "contains/1", "$0.Contains($1)");
        m(s + "indexOf(int)", "$0.IndexOf((char) $1)");
        m(s + "indexOf(String)", "$0.IndexOf($1, StringComparison.Ordinal)", SYSTEM);
        m(s + "indexOf(int,int)", "$0.IndexOf((char) $1, $2)");
        m(s + "indexOf(String,int)", "$0.IndexOf($1, $2, StringComparison.Ordinal)", SYSTEM);
        m(s + "lastIndexOf(int)", "$0.LastIndexOf((char) $1)");
        m(s + "lastIndexOf(String)", "$0.LastIndexOf($1, StringComparison.Ordinal)", SYSTEM);
        m(s + "lastIndexOf(int,int)", "$0.LastIndexOf((char) $1, $2)");
        m(s + "lastIndexOf(String,int)", "$0.LastIndexOf($1, $2, StringComparison.Ordinal)", SYSTEM);
        m(s + "startsWith/1", "$0.StartsWith($1, StringComparison.Ordinal)", SYSTEM);
        m(s + "startsWith/2", "$0.Substring($2).StartsWith($1, StringComparison.Ordinal)", SYSTEM);
        m(s + "endsWith/1", "$0.EndsWith($1, StringComparison.Ordinal)", SYSTEM);
        m(s + "split/1", "$0.SplitRegex($1)", COMPAT);
        m(s + "split/2", "$0.SplitRegex($1, $2)", COMPAT);
        m(s + "toLowerCase/0", "$0.ToLowerInvariant()");
        m(s + "toLowerCase/1", "$0.ToLowerInvariant()");
        m(s + "toUpperCase/0", "$0.ToUpperInvariant()");
        m(s + "toUpperCase/1", "$0.ToUpperInvariant()");
        m(s + "trim/0", "$0.Trim()");
        m(s + "strip/0", "$0.Trim()");
        m(s + "replaceAll/2", "Regex.Replace($0, $1, $2)", REGEX);
        m(s + "replaceFirst/2", "new Regex($1).Replace($0, $2, 1)", REGEX);
        m(s + "matches/1", "Regex.IsMatch($0, \"^(?:\" + $1 + \")$\")", REGEX);
        // Java's byte[] is C#'s sbyte[]: the compatibility library converts
        m(s + "getBytes/1", "JavaString.GetBytes($0, $1)", COMPAT);
        m(s + "getBytes/0", "JavaString.GetBytes($0, Encoding.UTF8)", COMPAT, TEXT);
        m(s + "repeat/1", "string.Concat(Enumerable.Repeat($0, $1))", LINQ);
        m(s + "equalsIgnoreCase/1", "string.Equals($0, $1, StringComparison.OrdinalIgnoreCase)", SYSTEM);
        m(s + "compareTo/1", "string.CompareOrdinal($0, $1)");
        m(s + "toCharArray/0", "$0.ToCharArray()");
        m(s + "concat/1", "$0 + $1");
        m(s + "intern/0", "string.Intern($0)");
        m(s + "join/2", "string.Join($*)");
        m(s + "format/2", "JavaString.Format($*)", COMPAT);
        m(s + "formatted/1", "JavaString.Format($0, $*)", COMPAT);
        m(s + "valueOf/1", "JavaString.ValueOf($1)", COMPAT);
        m(s + "valueOf/3", "new string($1, $2, $3)");
        m(s + "chars/0", "$0.Select(c => (int) c)", LINQ);

        for (String sb : List.of("java.lang.StringBuilder.", "java.lang.AbstractStringBuilder.")) {
            m(sb + "append/1", "$0.Append($1)");
            m(sb + "append(CharSequence,int,int)", "$0.Append($1, @2, @3 - @2)");
            m(sb + "append(char[],int,int)", "$0.Append($1, $2, $3)");
            m(sb + "length/0", "$0.Length");
            m(sb + "charAt/1", "$0[$1]");
            m(sb + "setLength/1", "$0.Length = $1");
            m(sb + "setCharAt/2", "$0[$1] = $2");
            m(sb + "insert/2", "$0.Insert($1, $2)");
            m(sb + "delete/2", "$0.Remove(@1, @2 - @1)");
            m(sb + "deleteCharAt/1", "$0.Remove($1, 1)");
            m(sb + "indexOf/1", "$0.ToString().IndexOf($1, StringComparison.Ordinal)", SYSTEM);
            m(sb + "indexOf/2", "$0.ToString().IndexOf($1, $2, StringComparison.Ordinal)", SYSTEM);
            m(sb + "lastIndexOf/1", "$0.ToString().LastIndexOf($1, StringComparison.Ordinal)", SYSTEM);
            m(sb + "reverse/0", "$0.Reverse()", COMPAT);
            m(sb + "isEmpty/0", "$0.Length == 0");
        }

        m("java.lang.Comparable.compareTo/1", "$0.CompareTo($1)");
        m("java.lang.CharSequence.length/0", "$0.Length");
        m("java.lang.CharSequence.charAt/1", "$0[$1]");
        m("java.lang.CharSequence.isEmpty/0", "$0.Length == 0");
        m("java.lang.CharSequence.subSequence/2", "$0[@1..@2]");
        m("java.nio.charset.Charset.defaultCharset/0", "Encoding.UTF8", TEXT);
        // an enum: C#'s, or a class with an Ordinal and a Name (CSharpTypePrinter)
        chooser("java.lang.Enum.ordinal/0", (method, call) -> rule(simpleEnum(call) ? "(int) @0" : "$0.Ordinal"));
        chooser("java.lang.Enum.name/0", (method, call) -> rule(simpleEnum(call) ? "$0.ToString()" : "$0.Name"));
        m("java.lang.Enum.compareTo/1", "$0.CompareTo($1)");
        m("java.lang.Character.isDigit/1", "char.IsDigit((char) $1)");
        m("java.lang.Character.isLetter/1", "char.IsLetter((char) $1)");
        m("java.lang.Character.isLetterOrDigit/1", "char.IsLetterOrDigit((char) $1)");
        m("java.lang.Character.isWhitespace/1", "char.IsWhiteSpace((char) $1)");
        m("java.lang.Character.isUpperCase/1", "char.IsUpper((char) $1)");
        m("java.lang.Character.isLowerCase/1", "char.IsLower((char) $1)");
        m("java.lang.Character.toUpperCase/1", "char.ToUpperInvariant($1)");
        m("java.lang.Character.toLowerCase/1", "char.ToLowerInvariant($1)");
        m("java.lang.Character.isJavaIdentifierStart/1", "JavaCharacter.IsJavaIdentifierStart($1)", COMPAT);
        m("java.lang.Character.isJavaIdentifierPart/1", "JavaCharacter.IsJavaIdentifierPart($1)", COMPAT);
        m("java.lang.Character.isIdentifierIgnorable/1", "JavaCharacter.IsIdentifierIgnorable($1)", COMPAT);
        m("java.lang.Character.charValue/0", "(char) @0");
        m("java.lang.Character.valueOf/1", "$1");
    }

    private static void numbers() {
        for (String[] t : new String[][]{{"Integer", "int"}, {"Long", "long"}, {"Short", "short"}, {"Byte", "sbyte"},
                {"Double", "double"}, {"Float", "float"}}) {
            String n = "java.lang." + t[0] + ".";
            m(n + "toString/1", "$1.ToString()");
            m(n + "compare/2", "$1.CompareTo($2)");
            m(n + "hashCode/1", "$1.GetHashCode()");
        }
        m("java.lang.Number.intValue/0", "(int) @0");
        m("java.lang.Number.longValue/0", "(long) @0");
        m("java.lang.Number.doubleValue/0", "(double) @0");
        m("java.lang.Number.floatValue/0", "(float) @0");
        m("java.lang.Number.shortValue/0", "(short) @0");
        m("java.lang.Number.byteValue/0", "(sbyte) @0");
        m("java.lang.Integer.intValue/0", "(int) @0");
        m("java.lang.Long.longValue/0", "(long) @0");
        m("java.lang.Double.doubleValue/0", "(double) @0");
        m("java.lang.Float.floatValue/0", "(float) @0");
        m("java.lang.Boolean.booleanValue/0", "(bool) @0");
        m("java.lang.Integer.parseInt/1", "int.Parse($1, CultureInfo.InvariantCulture)", GLOBALIZATION);
        m("java.lang.Integer.parseInt/2", "Convert.ToInt32($1, $2)", SYSTEM);
        m("java.lang.Long.parseLong/1", "long.Parse($1, CultureInfo.InvariantCulture)", GLOBALIZATION);
        m("java.lang.Double.parseDouble/1", "double.Parse($1, CultureInfo.InvariantCulture)", GLOBALIZATION);
        m("java.lang.Float.parseFloat/1", "float.Parse($1, CultureInfo.InvariantCulture)", GLOBALIZATION);
        m("java.lang.Boolean.parseBoolean/1", "string.Equals($1, \"true\", StringComparison.OrdinalIgnoreCase)", SYSTEM);
        m("java.lang.Integer.valueOf(int)", "$1");
        m("java.lang.Integer.valueOf(String)", "int.Parse($1, CultureInfo.InvariantCulture)", GLOBALIZATION);
        m("java.lang.Long.valueOf(long)", "$1");
        m("java.lang.Boolean.valueOf(boolean)", "$1");
        m("java.lang.Double.valueOf(double)", "$1");
        m("java.lang.Float.valueOf(float)", "$1");
        m("java.lang.Integer.toHexString/1", "Convert.ToString($1, 16)", SYSTEM);
        m("java.lang.Integer.toBinaryString/1", "Convert.ToString($1, 2)", SYSTEM);
        m("java.lang.Long.toHexString/1", "Convert.ToString($1, 16)", SYSTEM);
        m("java.lang.Integer.bitCount/1", "BitOperations.PopCount((uint) $1)", "System.Numerics");
        m("java.lang.Double.isNaN/1", "double.IsNaN($1)");
        m("java.lang.Float.isNaN/1", "float.IsNaN($1)");
        m("java.lang.Double.isInfinite/1", "double.IsInfinity($1)");
        m("java.lang.Float.isInfinite/1", "float.IsInfinity($1)");
        m("java.lang.Float.floatToIntBits/1", "BitConverter.SingleToInt32Bits($1)", SYSTEM);
        m("java.lang.Float.floatToRawIntBits/1", "BitConverter.SingleToInt32Bits($1)", SYSTEM);
        m("java.lang.Float.intBitsToFloat/1", "BitConverter.Int32BitsToSingle($1)", SYSTEM);
        m("java.lang.Double.doubleToLongBits/1", "BitConverter.DoubleToInt64Bits($1)", SYSTEM);
        m("java.lang.Double.doubleToRawLongBits/1", "BitConverter.DoubleToInt64Bits($1)", SYSTEM);
        m("java.lang.Double.longBitsToDouble/1", "BitConverter.Int64BitsToDouble($1)", SYSTEM);
        m("java.lang.Math.round/1", "JavaMath.Round($1)", COMPAT);
        m("java.lang.Math.floorMod/2", "JavaMath.FloorMod($1, $2)", COMPAT);
        m("java.lang.Math.floorDiv/2", "JavaMath.FloorDiv($1, $2)", COMPAT);
    }

    private static void streams() {
        String st = "java.util.stream.Stream.";
        m(st + "filter/1", "$0.Where($1)", LINQ);
        m(st + "map/1", "$0.Select($1)", LINQ);
        m(st + "mapToInt/1", "$0.Select($1)", LINQ);
        m(st + "mapToObj/1", "$0.Select($1)", LINQ);
        m(st + "flatMap/1", "$0.SelectMany($1)", LINQ);
        m(st + "anyMatch/1", "$0.Any($1)", LINQ);
        m(st + "allMatch/1", "$0.All($1)", LINQ);
        m(st + "noneMatch/1", "!$0.Any($1)", LINQ);
        m(st + "count/0", "$0.LongCount()", LINQ);
        m(st + "distinct/0", "$0.Distinct()", LINQ);
        m(st + "sorted/0", "$0.Order()", LINQ);
        m(st + "sorted/1", "$0.Sorted($1)", COMPAT);
        m(st + "toList/0", "$0.ToList()", LINQ);
        m(st + "findFirst/0", "$0.FirstOrDefault()", LINQ);
        m(st + "findAny/0", "$0.FirstOrDefault()", LINQ);
        m(st + "forEach/1", "$0.ForEach($1)", COMPAT);
        m(st + "limit/1", "$0.Take((int) $1)", LINQ);
        m(st + "skip/1", "$0.Skip((int) $1)", LINQ);
        m(st + "empty/0", "Enumerable.Empty<{R0}>()", LINQ);
        m(st + "of/1", "new[] { $* }");
        m(st + "concat/2", "$1.Concat($2)", LINQ);
        m(st + "toArray/0", "$0.ToArray()", LINQ);
        m(st + "max/1", "$0.Max(Comparer<{R}>.Create($1))", LINQ);
        m(st + "min/1", "$0.Min(Comparer<{R}>.Create($1))", LINQ);
        String is = "java.util.stream.IntStream.";
        m(is + "range/2", "Enumerable.Range(@1, @2 - @1)", LINQ);
        m(is + "rangeClosed/2", "Enumerable.Range(@1, @2 - @1 + 1)", LINQ);
        m(is + "sum/0", "$0.Sum()", LINQ);
        m(is + "max/0", "$0.Max()", LINQ);
        m(is + "boxed/0", "$0");
        m(is + "mapToObj/1", "$0.Select($1)", LINQ);
        m(is + "filter/1", "$0.Where($1)", LINQ);
        m(is + "anyMatch/1", "$0.Any($1)", LINQ);
        m(is + "toArray/0", "$0.ToArray()", LINQ);
        chooser(st + "collect/1", (method, call) -> rule(collect(call), LINQ));

        // Optional is the value itself, or null
        String o = "java.util.Optional.";
        m(o + "isPresent/0", "$0 != null");
        m(o + "isEmpty/0", "$0 == null");
        m(o + "get/0", "$0");
        m(o + "orElseThrow/0", "$0 ?? throw new InvalidOperationException()", SYSTEM);
        m(o + "orElse/1", "$0 ?? $1");
        m(o + "orElseGet/1", "$0 ?? $1()");
        m(o + "of/1", "$1");
        m(o + "ofNullable/1", "$1");
        m(o + "empty/0", "null");
        m(o + "ifPresent/1", "$0.IfPresent($1)", COMPAT);
        m(o + "map/1", "$0.Map($1)", COMPAT);
        m(o + "filter/1", "$0.Filter($1)", COMPAT);

        // the functional interfaces are delegates: their method is an invocation
        for (String f : List.of("java.util.function.Function.apply", "java.util.function.BiFunction.apply",
                "java.util.function.Supplier.get", "java.util.function.Consumer.accept",
                "java.util.function.BiConsumer.accept", "java.util.function.Predicate.test",
                "java.util.function.BiPredicate.test", "java.util.function.IntFunction.apply",
                "java.util.function.ToIntFunction.applyAsInt", "java.util.function.IntPredicate.test",
                "java.lang.Runnable.run", "java.util.concurrent.Callable.call", "java.util.Comparator.compare")) {
            m(f, "$0($*)");
        }
        String c = "java.util.Comparator.";
        m(c + "comparing/1", "JavaComparator.Comparing<{R0}>($1)", COMPAT);
        m(c + "comparingInt/1", "JavaComparator.Comparing<{R0}>($1)", COMPAT);
        m(c + "comparingLong/1", "JavaComparator.Comparing<{R0}>($1)", COMPAT);
        m(c + "thenComparing/1", "$0.ThenComparing($1)", COMPAT);
        m(c + "thenComparingInt/1", "$0.ThenComparing($1)", COMPAT);
        m(c + "thenComparingLong/1", "$0.ThenComparing($1)", COMPAT);
        m(c + "reversed/0", "$0.Reversed()", COMPAT);
        m(c + "naturalOrder/0", "Comparer<{R0}>.Default.Compare");
    }

    private static void system() {
        m("java.lang.System.arraycopy/5", "Array.Copy($1, $2, $3, $4, $5)", SYSTEM);
        m("java.lang.System.currentTimeMillis/0", "DateTimeOffset.UtcNow.ToUnixTimeMilliseconds()", SYSTEM);
        m("java.lang.System.nanoTime/0", "Stopwatch.GetTimestamp() * 1_000_000_000L / Stopwatch.Frequency", "System.Diagnostics");
        m("java.lang.System.lineSeparator/0", "Environment.NewLine", SYSTEM);
        m("java.lang.System.getProperty/1", "JavaSystem.GetProperty($1)", COMPAT);
        m("java.lang.System.getProperty/2", "JavaSystem.GetProperty($1, $2)", COMPAT);
        m("java.lang.System.exit/1", "Environment.Exit($1)", SYSTEM);
        m("java.lang.System.identityHashCode/1", "RuntimeHelpers.GetHashCode($1)", "System.Runtime.CompilerServices");
        m("java.io.PrintStream.println/1", "$0.WriteLine($1)");
        m("java.io.PrintStream.println/0", "$0.WriteLine()");
        m("java.io.PrintStream.print/1", "$0.Write($1)");
        m("java.io.PrintStream.flush/0", "$0.Flush()");
        m("java.io.PrintWriter.println/1", "$0.WriteLine($1)");
        m("java.io.PrintWriter.print/1", "$0.Write($1)");
        m("java.lang.Throwable.getMessage/0", "$0.Message");
        m("java.lang.Throwable.getCause/0", "$0.InnerException");
        m("java.lang.Throwable.printStackTrace/0", "Console.Error.WriteLine($0)", SYSTEM);
        m("java.lang.Throwable.printStackTrace/1", "$1.WriteLine($0)");
        m("java.lang.Object.getClass/0", "$0.GetType()");
        m("java.lang.Object.notifyAll/0", "Monitor.PulseAll($0)", THREADING);
        m("java.lang.Object.notify/0", "Monitor.Pulse($0)", THREADING);
        m("java.lang.Object.wait/0", "Monitor.Wait($0)", THREADING);
        // super.clone(): the shallow copy C#'s object makes
        chooser("java.lang.Object.clone/0", (method, call) -> call != null && arrayTyped(call.object())
                ? rule("({R}) @0.Clone()") : rule("$0.MemberwiseClone()"));
        m("java.lang.Class.getName/0", "$0.FullName");
        m("java.lang.Class.getSimpleName/0", "$0.Name");
        m("java.lang.Thread.currentThread/0", "Thread.CurrentThread", THREADING);
        m("java.lang.AutoCloseable.close/0", "$0.Dispose()");
        m("java.io.Closeable.close/0", "$0.Dispose()");
        m("java.io.InputStream.close/0", "$0.Dispose()");
        m("java.io.OutputStream.close/0", "$0.Dispose()");
        m("java.io.OutputStream.write(int)", "$0.WriteByte((byte) $1)");
        m("java.io.OutputStream.write(byte[])", "$0.Write((byte[]) (object) $1)");
        m("java.io.OutputStream.write/3", "$0.Write((byte[]) (object) $1, $2, $3)");
        m("java.io.InputStream.read/0", "$0.ReadByte()");
        m("java.io.InputStream.read/1", "$0.Read((byte[]) (object) $1)");
        m("java.io.InputStream.read/3", "$0.Read((byte[]) (object) $1, $2, $3)");
        m("java.io.InputStream.readAllBytes/0", "JavaStreams.ReadAllBytes($0)", COMPAT);
        m("java.io.InputStream.skip/1", "JavaStreams.Skip($0, $1)", COMPAT);
        m("java.io.Writer.write/1", "$0.Write($1)");
        m("java.io.Writer.flush/0", "$0.Flush()");
        m("java.io.Writer.close/0", "$0.Dispose()");
        m("java.io.Reader.close/0", "$0.Dispose()");
        m("java.io.BufferedReader.readLine/0", "$0.ReadLine()");
        // file streams: a java.io.File or a path
        // an EnumMap orders by its keys, as a SortedDictionary does; its Class argument is the key type's
        m("java.util.EnumMap.new(Class)", "new {R}()", GENERIC);
        m("java.io.FileOutputStream.new/1", "new FileStream($1.ToString(), FileMode.Create)", IO);
        m("java.io.FileOutputStream.new/2", "new FileStream($1.ToString(), $2 ? FileMode.Append : FileMode.Create)", IO);
        m("java.io.FileInputStream.new/1", "File.OpenRead($1.ToString())", IO);
        m("java.io.BufferedOutputStream.new/1", "new BufferedStream($1)", IO);
        m("java.io.BufferedOutputStream.new/2", "new BufferedStream($1, $2)", IO);
        m("java.io.BufferedInputStream.new/1", "new BufferedStream($1)", IO);
        m("java.io.OutputStreamWriter.new/1", "new StreamWriter($1)", IO);
        m("java.io.OutputStreamWriter.new/2", "new StreamWriter($1, $2)", IO);
        m("java.io.InputStreamReader.new/1", "new StreamReader($1)", IO);
        m("java.io.InputStreamReader.new/2", "new StreamReader($1, $2)", IO);
        m("java.io.BufferedReader.new/1", "$1");
        // java.time, threads, regular expressions
        m("java.time.Instant.now/0", "DateTimeOffset.UtcNow", SYSTEM);
        m("java.time.Instant.toEpochMilli/0", "$0.ToUnixTimeMilliseconds()");
        m("java.lang.ThreadLocal.get/0", "$0.Value");
        m("java.lang.ThreadLocal.set/1", "$0.Value = $1");
        m("java.lang.ThreadLocal.remove/0", "$0.Value = default");
        m("java.lang.ThreadLocal.withInitial/1", "new ThreadLocal<{R0}>($1)", THREADING);
        m("java.util.regex.Pattern.compile/1", "new Regex($1)", REGEX);
        m("java.util.regex.Pattern.matcher/1", "new JavaMatcher($0, $1)", COMPAT);
        m("java.util.regex.Pattern.matches/2", "Regex.IsMatch($2, \"^(?:\" + $1 + \")$\")", REGEX);
        m("java.util.regex.Pattern.quote/1", "Regex.Escape($1)", REGEX);
        m("java.util.regex.Pattern.pattern/0", "$0.ToString()");
        // OptionalInt and its kin: a nullable value
        for (String optional : List.of("java.util.OptionalInt", "java.util.OptionalLong", "java.util.OptionalDouble")) {
            m(optional + ".isPresent/0", "$0.HasValue");
            m(optional + ".isEmpty/0", "!$0.HasValue");
            m(optional + ".orElse/1", "$0 ?? $1");
            m(optional + ".getAsInt/0", "$0.Value");
            m(optional + ".getAsLong/0", "$0.Value");
            m(optional + ".getAsDouble/0", "$0.Value");
        }
        m("java.lang.Boolean.toString/1", "JavaString.ValueOf($1)", COMPAT);
        m("java.lang.Byte.toUnsignedInt/1", "(@1 & 0xFF)");
        m("java.lang.Character.getType/1", "JavaCharacter.GetType($1)", COMPAT);
        m("java.lang.AbstractStringBuilder.substring/1", "$0.ToString(@1, $0.Length - @1)");
        m("java.lang.AbstractStringBuilder.substring/2", "$0.ToString(@1, @2 - @1)");
        m("java.io.OutputStream.flush/0", "$0.Flush()");
    }

    // ---------------------------------------------------------------- lookup

    /** The rule of a call of {@code method} (null: the call of a method reference, or none); null when there is none. */
    static Rule call(MethodInfo method, MethodCall call) {
        for (MethodInfo candidate : candidates(method)) {
            TypeInfo owner = candidate.typeInfo();
            String base = owner.fullyQualifiedName() + "." + candidate.name();
            for (String key : List.of(base + "(" + signature(candidate) + ")",
                    base + "/" + candidate.parameters().size(), base)) {
                Chooser chooser = MEMBERS.get(key);
                if (chooser != null) {
                    Rule rule = chooser.choose(method, call);
                    if (rule != null) return rule;
                }
            }
            TypeMapping mapping = TYPES.get(owner.fullyQualifiedName());
            if (mapping != null && mapping.compat()) {
                // a compatibility class has the Java members, in PascalCase
                String name = CSharpNames.pascal(candidate.name());
                return rule(candidate.isStatic() ? mapping.template() + "." + name + "($*)" : "$0." + name + "($*)",
                        COMPAT);
            }
            if ("java.lang.Math".equals(owner.fullyQualifiedName())) {
                return rule("Math." + CSharpNames.pascal(candidate.name()) + "($*)", SYSTEM);
            }
        }
        return null;
    }

    /** The rule of a JDK constructor, keyed {@code owner.new/arity} or {@code owner.new(signature)}; null without. */
    static Rule constructor(MethodInfo constructor) {
        String base = constructor.typeInfo().fullyQualifiedName() + ".new";
        for (String key : List.of(base + "(" + signature(constructor) + ")", base + "/" + constructor.parameters().size())) {
            Chooser chooser = MEMBERS.get(key);
            if (chooser != null) {
                Rule rule = chooser.choose(constructor, null);
                if (rule != null) return rule;
            }
        }
        return null;
    }

    /** The method and what it overrides, the most specific declaring type first: ArrayList's, List's, Collection's. */
    private static List<MethodInfo> candidates(MethodInfo method) {
        return Stream.concat(Stream.of(method), method.overrides().stream()
                        .sorted(Comparator.comparingInt((MethodInfo m) -> -m.typeInfo().superTypesExcludingJavaLangObject().size())
                                .thenComparing(m -> m.typeInfo().fullyQualifiedName())))
                .distinct().toList();
    }

    /** {@code int,String}: the simple names of the erased parameter types. */
    private static String signature(MethodInfo method) {
        StringBuilder sb = new StringBuilder();
        for (ParameterInfo p : method.parameters()) {
            if (!sb.isEmpty()) sb.append(',');
            ParameterizedType t = p.parameterizedType();
            String name = t.isTypeParameter() || t.typeInfo() == null ? "Object" : t.typeInfo().simpleName();
            sb.append(name).append("[]".repeat(t.arrays()));
        }
        return sb.toString();
    }

    private static final Map<String, String> FIELDS = Map.ofEntries(
            Map.entry("java.lang.System.out", "Console.Out"),
            Map.entry("java.lang.System.err", "Console.Error"),
            Map.entry("java.lang.Integer.MAX_VALUE", "int.MaxValue"),
            Map.entry("java.lang.Integer.MIN_VALUE", "int.MinValue"),
            Map.entry("java.lang.Long.MAX_VALUE", "long.MaxValue"),
            Map.entry("java.lang.Long.MIN_VALUE", "long.MinValue"),
            Map.entry("java.lang.Short.MAX_VALUE", "short.MaxValue"),
            Map.entry("java.lang.Short.MIN_VALUE", "short.MinValue"),
            Map.entry("java.lang.Byte.MAX_VALUE", "sbyte.MaxValue"),
            Map.entry("java.lang.Byte.MIN_VALUE", "sbyte.MinValue"),
            Map.entry("java.lang.Character.MAX_VALUE", "char.MaxValue"),
            Map.entry("java.lang.Float.MAX_VALUE", "float.MaxValue"),
            Map.entry("java.lang.Float.MIN_VALUE", "float.Epsilon"),
            Map.entry("java.lang.Float.MIN_NORMAL", "1.17549435E-38f"),
            Map.entry("java.lang.Float.NaN", "float.NaN"),
            Map.entry("java.lang.Float.POSITIVE_INFINITY", "float.PositiveInfinity"),
            Map.entry("java.lang.Float.NEGATIVE_INFINITY", "float.NegativeInfinity"),
            Map.entry("java.lang.Double.MAX_VALUE", "double.MaxValue"),
            Map.entry("java.lang.Double.MIN_VALUE", "double.Epsilon"),
            Map.entry("java.lang.Double.MIN_NORMAL", "2.2250738585072014E-308"),
            Map.entry("java.lang.Double.NaN", "double.NaN"),
            Map.entry("java.lang.Double.POSITIVE_INFINITY", "double.PositiveInfinity"),
            Map.entry("java.lang.Double.NEGATIVE_INFINITY", "double.NegativeInfinity"),
            Map.entry("java.lang.Boolean.TRUE", "true"),
            Map.entry("java.lang.Boolean.FALSE", "false"),
            Map.entry("java.lang.Math.PI", "Math.PI"),
            Map.entry("java.lang.Math.E", "Math.E"),
            Map.entry("java.util.Locale.ENGLISH", "CultureInfo.InvariantCulture"),
            Map.entry("java.util.Locale.ROOT", "CultureInfo.InvariantCulture"),
            Map.entry("java.util.Locale.US", "CultureInfo.InvariantCulture"),
            Map.entry("java.nio.charset.StandardCharsets.UTF_8", "Encoding.UTF8"),
            Map.entry("java.nio.charset.StandardCharsets.US_ASCII", "Encoding.ASCII"),
            Map.entry("java.nio.charset.StandardCharsets.ISO_8859_1", "Encoding.Latin1"),
            Map.entry("java.io.File.separator", "Path.DirectorySeparatorChar.ToString()"),
            Map.entry("java.io.File.separatorChar", "Path.DirectorySeparatorChar"),
            Map.entry("java.io.File.pathSeparator", "Path.PathSeparator.ToString()"),
            Map.entry("java.util.jar.JarFile.MANIFEST_NAME", "JavaJarFile.MANIFEST_NAME"),
            Map.entry("java.io.File.pathSeparatorChar", "Path.PathSeparator"),
            Map.entry("java.lang.Character.UNASSIGNED", "JavaCharacter.UNASSIGNED"),
            Map.entry("java.lang.Character.CONTROL", "JavaCharacter.CONTROL"),
            Map.entry("java.lang.Character.FORMAT", "JavaCharacter.FORMAT"),
            Map.entry("java.lang.Character.PRIVATE_USE", "JavaCharacter.PRIVATE_USE"),
            Map.entry("java.lang.Character.SURROGATE", "JavaCharacter.SURROGATE"),
            Map.entry("java.lang.Character.LINE_SEPARATOR", "JavaCharacter.LINE_SEPARATOR"),
            Map.entry("java.lang.Character.PARAGRAPH_SEPARATOR", "JavaCharacter.PARAGRAPH_SEPARATOR"),
            Map.entry("java.lang.Character.SPACE_SEPARATOR", "JavaCharacter.SPACE_SEPARATOR"));

    private static final Map<String, String> FIELD_NAMESPACES = Map.of("Console", SYSTEM, "Math", SYSTEM,
            "CultureInfo", GLOBALIZATION, "Encoding", TEXT, "Path", IO, "JavaCharacter", COMPAT,
            "JavaJarFile", COMPAT);

    /** A JDK field in C#, and the namespace it needs; null when there is none. */
    static String[] field(FieldInfo fieldInfo) {
        String csharp = FIELDS.get(fieldInfo.owner().fullyQualifiedName() + "." + fieldInfo.name());
        if (csharp == null) return null;
        String first = csharp.contains(".") ? csharp.substring(0, csharp.indexOf('.')) : "";
        return new String[]{csharp, FIELD_NAMESPACES.get(first)};
    }

    /**
     * The C# name of a translated method that overrides a JDK one, when the BCL's member has another name than the
     * PascalCase of the Java one: {@code close()} of an {@code AutoCloseable} is {@code Dispose()}.
     */
    static String overrideName(MethodInfo method) {
        for (MethodInfo overridden : method.overrides()) {
            String fqn = overridden.typeInfo().fullyQualifiedName();
            if (("java.lang.AutoCloseable".equals(fqn) || "java.io.Closeable".equals(fqn))
                && "close".equals(overridden.name())) {
                return "Dispose";
            }
        }
        return null;
    }

    // ---------------------------------------------------------------- choosers

    /** The call's type argument {@code index} (of its return type) is a boxed primitive: a value type in C#. */
    private static boolean valueType(MethodCall call, int index) {
        if (call == null || call.object() == null) return false;
        ParameterizedType receiver = call.object().parameterizedType();
        if (receiver == null || receiver.parameters().size() <= index) return false;
        ParameterizedType v = receiver.parameters().get(index);
        return v.typeInfo() != null && v.arrays() == 0 && v.isBoxedExcludingVoid();
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
        TypeMapping mapping = t == null ? null : TYPES.get(t.fullyQualifiedName());
        return mapping != null && "List".equals(mapping.template());
    }

    /** The receiver is declared as Java's Map interface: C#'s IDictionary, without the BCL's read-only extensions. */
    private static boolean interfaceTyped(MethodCall call) {
        if (call == null || call.object() == null) return true;
        ParameterizedType receiver = call.object().parameterizedType();
        if (receiver == null || receiver.typeInfo() == null) return true;
        TypeMapping mapping = TYPES.get(receiver.typeInfo().fullyQualifiedName());
        return mapping == null || mapping.template().startsWith("I");
    }

    /** The receiver is an enum C# declares as an enum (only constants), not as a class. */
    private static boolean simpleEnum(MethodCall call) {
        if (call == null || call.object() == null || call.object().parameterizedType() == null) return true;
        TypeInfo t = call.object().parameterizedType().typeInfo();
        return t == null || !t.typeNature().isEnum() || CSharpTypePrinter.simpleEnum(t);
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
                case "toMap/2", "toMap/3":
                    return "$0.ToDictionary($1.1, $1.2)";
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
