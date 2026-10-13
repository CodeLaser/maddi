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
final class CSharpBcl implements LibraryMapping {

    /** The JDK's mapping, the last of a translation's {@link CSharpLibraries}. */
    static final CSharpBcl JDK = new CSharpBcl();

    static final String GENERIC = "System.Collections.Generic";
    static final String LINQ = "System.Linq";
    static final String SYSTEM = "System";
    static final String TEXT = "System.Text";
    static final String IO = "System.IO";
    static final String REGEX = "System.Text.RegularExpressions";
    static final String GLOBALIZATION = "System.Globalization";
    static final String THREADING = "System.Threading";
    static final String TASKS = "System.Threading.Tasks";
    static final String REFLECTION = "System.Reflection";
    static final String NUMERICS = "System.Numerics";
    static final String CRYPTOGRAPHY = "System.Security.Cryptography";
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

    // ---------------------------------------------------------------- types

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
        // the navigable and sequenced interfaces are their one implementation's: TreeMap, TreeSet, LinkedHashMap and
        // LinkedHashSet; an EnumSet is a set (C#'s enums printed as classes are not comparable)
        type("java.util.NavigableMap", "SortedDictionary", GENERIC);
        type("java.util.NavigableSet", "SortedSet", GENERIC);
        type("java.util.SequencedMap", "OrderedDictionary", GENERIC);
        type("java.util.SequencedSet", "HashSet", GENERIC);
        type("java.util.EnumSet", "HashSet", GENERIC);
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
        type("java.util.stream.LongStream", "IEnumerable<long>", GENERIC);
        type("java.util.stream.DoubleStream", "IEnumerable<double>", GENERIC);
        type("java.time.Instant", "DateTimeOffset", SYSTEM);
        // the values of java.time, java.net and java.math: BCL structs, as Instant is
        type("java.time.LocalDate", "DateOnly", SYSTEM);
        type("java.time.LocalTime", "TimeOnly", SYSTEM);
        type("java.time.LocalDateTime", "DateTime", SYSTEM);
        type("java.time.Duration", "TimeSpan", SYSTEM);
        type("java.time.Clock", "TimeProvider", SYSTEM);
        // a formatter is a custom format string: the ISO ones are constants (FIELDS)
        type("java.time.format.DateTimeFormatter", "string", null);
        type("java.net.URI", "Uri", SYSTEM);
        type("java.net.URL", "Uri", SYSTEM);
        type("java.util.UUID", "Guid", SYSTEM);
        type("java.math.BigDecimal", "decimal", null);
        type("java.math.BigInteger", "BigInteger", NUMERICS);
        // C#'s numbers have no common base class; IConvertible is what they share (with string and DateTime, which
        // a Java Number is not)
        type("java.lang.Number", "IConvertible", SYSTEM);
        type("java.util.Random", "Random", SYSTEM);
        type("java.util.random.RandomGenerator", "Random", SYSTEM);
        type("java.math.RoundingMode", "MidpointRounding", SYSTEM);
        type("java.lang.StringBuffer", "StringBuilder", TEXT);
        type("java.security.MessageDigest", "HashAlgorithm", CRYPTOGRAPHY);
        type("java.security.NoSuchAlgorithmException", "CryptographicException", CRYPTOGRAPHY);
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
        type("java.util.function.ToLongFunction", "Func<{0}, long>", SYSTEM);
        type("java.util.function.ToDoubleFunction", "Func<{0}, double>", SYSTEM);
        type("java.util.function.ToIntBiFunction", "Func<{0}, {1}, int>", SYSTEM);
        type("java.util.function.ToLongBiFunction", "Func<{0}, {1}, long>", SYSTEM);
        type("java.util.function.ToDoubleBiFunction", "Func<{0}, {1}, double>", SYSTEM);
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
        // Void is a type argument only: a CompletableFuture<Void> has no value
        type("java.lang.Void", "object", null);
        // reflection: java.lang.reflect.Type and its kinds are System.Type (patterns tell the kinds apart, PATTERNS)
        for (String t : List.of("java.lang.reflect.Type", "java.lang.reflect.ParameterizedType",
                "java.lang.reflect.TypeVariable", "java.lang.reflect.GenericArrayType")) {
            plain(t, "Type", SYSTEM);
        }
        compatType("java.lang.reflect.WildcardType", "IJavaWildcardType");
        type("java.lang.reflect.Method", "MethodInfo", REFLECTION);
        type("java.lang.reflect.Field", "FieldInfo", REFLECTION);
        type("java.lang.reflect.Parameter", "ParameterInfo", REFLECTION);
        plain("java.lang.reflect.Constructor", "ConstructorInfo", REFLECTION);
        type("java.lang.reflect.Executable", "MethodBase", REFLECTION);
        type("java.lang.reflect.AccessibleObject", "MemberInfo", REFLECTION);
        type("java.lang.reflect.Member", "MemberInfo", REFLECTION);
        type("java.lang.reflect.AnnotatedElement", "ICustomAttributeProvider", REFLECTION);
        type("java.lang.annotation.Annotation", "Attribute", SYSTEM);
        // a package is its name, a namespace
        type("java.lang.Package", "string", null);
        plain("java.lang.Enum", "Enum", SYSTEM);
        type("java.lang.NoSuchMethodException", "MissingMethodException", SYSTEM);
        type("java.lang.ClassNotFoundException", "TypeLoadException", SYSTEM);
        type("java.lang.NoSuchFieldException", "MissingFieldException", SYSTEM);
        type("java.lang.InstantiationException", "MemberAccessException", SYSTEM);
        type("java.lang.IllegalAccessException", "MemberAccessException", SYSTEM);
        type("java.lang.ReflectiveOperationException", "Exception", SYSTEM);
        type("java.lang.reflect.InvocationTargetException", "TargetInvocationException", REFLECTION);
        // a class loader is the assembly that holds the types; a service loader the implementations they hold
        type("java.lang.ClassLoader", "Assembly", REFLECTION);
        type("java.util.ServiceLoader", "IEnumerable", GENERIC);
        // a path is a FileInfo, not a string: Java overloads on Path and String
        type("java.nio.file.Path", "FileInfo", IO);
        // a future is a task, an executor the scheduler of its tasks
        type("java.util.concurrent.CompletableFuture", "Task", TASKS);
        type("java.util.concurrent.Future", "Task", TASKS);
        for (String executor : List.of("java.util.concurrent.Executor", "java.util.concurrent.ExecutorService",
                "java.util.concurrent.ThreadPoolExecutor")) {
            type(executor, "TaskScheduler", TASKS);
        }
        type("java.util.concurrent.CountDownLatch", "CountdownEvent", THREADING);
        // Java's read-write lock is reentrant; its read and write locks are the compatibility library's IJavaLock
        type("java.util.concurrent.locks.ReadWriteLock", "ReaderWriterLockSlim", THREADING);
        type("java.util.concurrent.locks.ReentrantReadWriteLock", "ReaderWriterLockSlim", THREADING);
        compatType("java.util.concurrent.locks.Lock", "IJavaLock");
        compatType("java.util.concurrent.locks.ReentrantLock", "JavaReentrantLock");
        compatType("java.util.concurrent.atomic.AtomicBoolean", "AtomicBoolean");
        compatType("java.util.concurrent.atomic.AtomicInteger", "AtomicInteger");
        compatType("java.util.concurrent.atomic.AtomicLong", "AtomicLong");
        compatType("java.util.concurrent.atomic.AtomicReference", "AtomicReference");
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
    public PatternMapping pattern(ParameterizedType tested, ParameterizedType expression) {
        if (tested == null || tested.arrays() > 0 || tested.typeInfo() == null) return null;
        String pattern = PATTERNS.get(tested.typeInfo().fullyQualifiedName());
        if (pattern == null) pattern = classPattern(tested, expression);
        return pattern == null ? null : new PatternMapping(pattern, SYSTEM);
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

    private static final Set<String> TYPE_NAMES = computeTypeNames();

    /** The names of the BCL types the translation uses, and the System types any C# code may name. */
    @Override
    public Set<String> typeNames() {
        return TYPE_NAMES;
    }

    private static Set<String> computeTypeNames() {
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

    /** The C# counterpart of a JDK type; null when there is none (yet). */
    @Override
    public TypeMapping type(TypeInfo typeInfo) {
        return TYPES.get(typeInfo.fullyQualifiedName());
    }

    // ---------------------------------------------------------------- members

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
        values();
        concurrency();
        reflection();
        services();
    }

    private static void collections() {
        // List, and the deques, which are C#'s List<T> too
        m("java.util.List.get/1", "$0[$1]");
        m("java.util.Collection.size/0", "$0.Count");
        m("java.util.Map.size/0", "$0.Count");
        m("java.util.Collection.isEmpty/0", "$0.Count == 0");
        m("java.util.Map.isEmpty/0", "$0.Count == 0");
        m("java.util.Collection.add/1", "$0.Add($1)");
        // a read-only collection (CSharpReadOnly) has LINQ's Contains
        chooser("java.util.Collection.contains/1", (method, call) -> readOnlyReceiver(call)
                ? rule("$0.Contains($1)", LINQ) : rule("$0.Contains($1)"));
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
        m("java.util.Map.forEach/1", "$0.ForEach($1)", COMPAT);
        m("java.lang.Iterable.iterator/0", "$0.Iterator()", COMPAT);
        m("java.util.Collection.iterator/0", "$0.Iterator()", COMPAT);
        m("java.util.List.listIterator/0", "$0.Iterator()", COMPAT);

        m("java.util.List.add/2", "$0.Insert($1, $2)");
        ms("java.util.List.remove(int)", "$0.RemoveAtAndGet($1)", "$0.RemoveAt($1)", COMPAT);
        m("java.util.List.remove(Object)", "$0.Remove($1)");
        ms("java.util.List.addAll/1", "$0.AddAll($1)", "$0.AddRange($1)", COMPAT);
        m("java.util.List.addAll/2", "$0.InsertAll($1, $2)", COMPAT);
        ms("java.util.List.set/2", "$0.Set($1, $2)", "$0[$1] = $2", COMPAT);
        chooser("java.util.List.indexOf/1", (method, call) -> readOnlyReceiver(call)
                ? rule("JavaCollections.IndexOf($0, $1)", COMPAT) : rule("$0.IndexOf($1)"));
        m("java.util.List.lastIndexOf/1", "$0.LastIndexOf($1)", COMPAT);
        chooser("java.util.List.subList/2", (method, call) -> readOnlyReceiver(call)
                ? rule("$0.Skip(@1).Take(@2 - @1).ToList()", LINQ) : rule("$0.SubList($1, $2)", COMPAT));
        // Java's sorts are stable, List.Sort is not
        m("java.util.List.sort/1", "JavaCollections.Sort($0, $1)", COMPAT);
        m("java.util.List.getFirst/0", "$0[0]");
        m("java.util.List.getLast/0", "$0[^1]");
        m("java.util.SequencedCollection.getFirst/0", "$0[0]");
        m("java.util.SequencedCollection.getLast/0", "$0[^1]");
        m("java.util.List.of/1", "new List<{R0}> { $* }");
        chooser("java.util.List.of/0", (method, call) -> rule("new List<{R0}>()"));
        m("java.util.List.copyOf/1", "new List<{R0}>($1)");
        chooser("java.util.List.of", (method, call) -> rule("new List<{R0}> { $* }"));
        // the array argument only gives Java the element type
        m("java.util.Collection.toArray/1", "$0.ToArray()", LINQ);
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
        // a read-only dictionary has the BCL's GetValueOrDefault, a value type's absence is null
        chooser("java.util.Map.get/1", (method, call) -> readOnlyReceiver(call)
                ? rule(valueType(call, 1) ? "($0.ContainsKey($1) ? $0[$1] : ({R}) null)" : "$0.GetValueOrDefault($1)")
                : valueType(call, 1) ? rule("$0.GetValueOrNull($1)", COMPAT)
                : interfaceTyped(call) ? rule("$0.Get($1)", COMPAT) : rule("$0.GetValueOrDefault($1)"));
        chooser("java.util.Map.getOrDefault/2", (method, call) -> interfaceTyped(call) && !readOnlyReceiver(call)
                ? rule("$0.GetOrDefault($1, $2)", COMPAT) : rule("$0.GetValueOrDefault($1, $2)"));
        m("java.util.Map.containsKey/1", "$0.ContainsKey($1)");
        m("java.util.Map.containsValue/1", "$0.Values.Contains($1)", LINQ);
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
        m("java.util.Collections.emptyIterator/0", "new JavaIterator<{R0}>(new List<{R0}>())", COMPAT);
        // Java 19's factories of a capacity
        m("java.util.HashMap.newHashMap/1", "new {R}($1)");
        m("java.util.LinkedHashMap.newLinkedHashMap/1", "new {R}($1)");
        m("java.util.HashSet.newHashSet/1", "new {R}($1)");
        m("java.util.LinkedHashSet.newLinkedHashSet/1", "new {R}($1)");
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
        // navigation, by the compatibility library's extensions (JavaNavigable): an entry is a JavaEntry, null when none
        for (String nav : List.of("firstEntry/0", "lastEntry/0", "pollFirstEntry/0", "pollLastEntry/0")) {
            String name = nav.substring(0, nav.indexOf('/'));
            m("java.util.NavigableMap." + nav, "$0." + Character.toUpperCase(name.charAt(0)) + name.substring(1) + "()", COMPAT);
        }
        for (String nav : List.of("floorEntry", "lowerEntry", "ceilingEntry", "higherEntry", "floorKey", "lowerKey",
                "ceilingKey", "higherKey")) {
            m("java.util.NavigableMap." + nav + "/1", "$0." + Character.toUpperCase(nav.charAt(0)) + nav.substring(1) + "($1)",
                    COMPAT);
        }
        m("java.util.SortedMap.firstKey/0", "$0.Keys.First()", LINQ);
        m("java.util.SortedMap.lastKey/0", "$0.Keys.Last()", LINQ);
        m("java.util.SortedMap.headMap/1", "$0.HeadMap($1, false)", COMPAT);
        m("java.util.SortedMap.tailMap/1", "$0.TailMap($1, true)", COMPAT);
        m("java.util.NavigableMap.headMap/2", "$0.HeadMap($1, $2)", COMPAT);
        m("java.util.NavigableMap.tailMap/2", "$0.TailMap($1, $2)", COMPAT);
        m("java.util.NavigableMap.subMap/4", "$0.SubMap($1, $2, $3, $4)", COMPAT);
        m("java.util.SortedSet.first/0", "$0.Min");
        m("java.util.SortedSet.last/0", "$0.Max");
        m("java.util.SortedSet.comparator/0", "$0.Comparer");
        for (String nav : List.of("floor", "lower", "ceiling", "higher")) {
            m("java.util.NavigableSet." + nav + "/1", "$0." + Character.toUpperCase(nav.charAt(0)) + nav.substring(1) + "($1)",
                    COMPAT);
        }
        m("java.util.SequencedMap.sequencedValues/0", "$0.Values");
        m("java.util.SequencedMap.sequencedKeySet/0", "$0.Keys");
        m("java.util.EnumSet.noneOf/1", "new HashSet<{R0}>()");
        m("java.util.EnumSet.copyOf/1", "new HashSet<{R0}>($1)");
        m("java.util.Collections.rotate/2", "JavaCollections.Rotate($1, $2)", COMPAT);
        m("java.util.Collections.shuffle/1", "JavaCollections.Shuffle($1, Random.Shared)", COMPAT, SYSTEM);
        m("java.util.Collections.shuffle/2", "JavaCollections.Shuffle($1, $2)", COMPAT);
        m("java.util.Collections.swap/3", "JavaCollections.Swap($1, $2, $3)", COMPAT);
        // a set backed by a map, an empty one: identity is lost with IdentityHashMap's mapping already
        m("java.util.Collections.newSetFromMap/1", "new HashSet<{R0}>()");
        m("java.util.Collections.unmodifiableSortedSet/1", "$1");
        m("java.util.Collections.unmodifiableSortedMap/1", "$1");
        m("java.util.Collections.reverseOrder/1", "$1.Reversed()", COMPAT);
        m("java.util.Arrays.fill/4", "Array.Fill($1, $4, @2, @3 - @2)", SYSTEM);
        m("java.util.Arrays.deepEquals/2", "JavaArrays.DeepEquals($1, $2)", COMPAT);
        m("java.util.Arrays.deepHashCode/1", "JavaArrays.DeepHashCode($1)", COMPAT);
        m("java.util.Objects.deepEquals/2", "JavaArrays.DeepEquals($1, $2)", COMPAT);
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
        m("java.util.Objects.requireNonNullElseGet/2", "$1 ?? $2()");
        m("java.util.Objects.toString/1", "JavaString.ValueOf($1)", COMPAT);
        m("java.util.Objects.toString/2", "@1?.ToString() ?? $2");
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
        // a boxed number is the value: a cast; a Number is an IConvertible, whose value Convert reads whatever it is
        for (String[] t : new String[][]{{"int", "Int32"}, {"long", "Int64"}, {"double", "Double"}, {"float", "Single"},
                {"short", "Int16"}, {"byte", "SByte"}}) {
            String cast = "(" + ("byte".equals(t[0]) ? "sbyte" : t[0]) + ") @0";
            chooser("java.lang.Number." + t[0] + "Value/0", (method, call) -> numberTyped(call)
                    ? rule("Convert.To" + t[1] + "($0)", SYSTEM) : rule(cast));
        }
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
        m("java.lang.Integer.bitCount/1", "BitOperations.PopCount((uint) $1)", NUMERICS);
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
        // of(T...) given an array: the array itself is the elements
        chooser(st + "of/1", (method, call) -> rule(method.isVarargs() && call != null
                && call.parameterExpressions().size() == 1 && arrayTyped(call.parameterExpressions().getFirst())
                ? "$1" : "new[] { $* }"));
        m(st + "concat/2", "$1.Concat($2)", LINQ);
        m(st + "toArray/0", "$0.ToArray()", LINQ);
        // the generator, String[]::new, is the array's type, which C#'s ToArray knows
        m(st + "toArray/1", "$0.ToArray()", LINQ);
        m(st + "max/1", "$0.Max(Comparer<{R}>.Create($1))", LINQ);
        m(st + "reduce/1", "$0.Reduce($1)", COMPAT);
        m(st + "reduce/2", "$0.Aggregate($1, $2)", LINQ);
        m(st + "min/1", "$0.Min(Comparer<{R}>.Create($1))", LINQ);
        String is = "java.util.stream.IntStream.";
        m(is + "range/2", "Enumerable.Range(@1, @2 - @1)", LINQ);
        m(is + "rangeClosed/2", "Enumerable.Range(@1, @2 - @1 + 1)", LINQ);
        m(is + "sum/0", "$0.Sum()", LINQ);
        m(is + "max/0", "$0.Max()", LINQ);
        m(is + "boxed/0", "$0");
        m("java.util.stream.LongStream.boxed/0", "$0");
        m("java.util.stream.DoubleStream.boxed/0", "$0");
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
        m(o + "orElseThrow/1", "$0 ?? throw $1()");
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
                "java.util.function.ToLongFunction.applyAsLong", "java.util.function.ToDoubleFunction.applyAsDouble",
                "java.util.function.ToIntBiFunction.applyAsInt", "java.util.function.ToLongBiFunction.applyAsLong",
                "java.util.function.ToDoubleBiFunction.applyAsDouble",
                "java.lang.Runnable.run", "java.util.concurrent.Callable.call", "java.util.Comparator.compare")) {
            m(f, "$0($*)");
        }
        String c = "java.util.Comparator.";
        m(c + "comparing/1", "JavaComparator.Comparing<{R0}>($1)", COMPAT);
        m(c + "comparingInt/1", "JavaComparator.Comparing<{R0}>($1)", COMPAT);
        m(c + "comparingLong/1", "JavaComparator.Comparing<{R0}>($1)", COMPAT);
        m(c + "comparingDouble/1", "JavaComparator.Comparing<{R0}>($1)", COMPAT);
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
        m("java.lang.Class.getCanonicalName/0", "JavaClass.CanonicalName($0)", COMPAT);
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
            m(optional + ".orElseGet/1", "$0 ?? $1()");
            m(optional + ".orElseThrow/0", "$0 ?? throw new InvalidOperationException()", SYSTEM);
            m(optional + ".of/1", "({R}) $1");
            m(optional + ".empty/0", "({R}) null");
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

    /** java.time, java.net.URI, UUID, BigDecimal, Random, Base64, digests: the BCL's structs and helpers. */
    private static void values() {
        String clockNow = "@1.GetLocalNow().DateTime";
        m("java.time.LocalDate.now/0", "DateOnly.FromDateTime(DateTime.Now)", SYSTEM);
        m("java.time.LocalDate.now/1", "DateOnly.FromDateTime(" + clockNow + ")", SYSTEM);
        m("java.time.LocalTime.now/0", "TimeOnly.FromDateTime(DateTime.Now)", SYSTEM);
        m("java.time.LocalTime.now/1", "TimeOnly.FromDateTime(" + clockNow + ")", SYSTEM);
        m("java.time.LocalDateTime.now/0", "DateTime.Now", SYSTEM);
        m("java.time.LocalDateTime.now/1", clockNow, SYSTEM);
        m("java.time.LocalDate.of/3", "new DateOnly($1, $2, $3)", SYSTEM);
        // a tick is 100 ns
        m("java.time.LocalTime.of/2", "new TimeOnly($1, $2)", SYSTEM);
        m("java.time.LocalTime.of/3", "new TimeOnly($1, $2, $3)", SYSTEM);
        m("java.time.LocalTime.of/4", "new TimeOnly(new TimeSpan($1, $2, $3).Ticks + @4 / 100)", SYSTEM);
        m("java.time.LocalDateTime.of/5", "new DateTime($1, $2, $3, $4, $5, 0)", SYSTEM);
        m("java.time.LocalDateTime.of/6", "new DateTime($1, $2, $3, $4, $5, $6)", SYSTEM);
        m("java.time.LocalDateTime.of/7", "new DateTime($1, $2, $3, $4, $5, $6).AddTicks(@7 / 100)", SYSTEM);
        for (String[] t : new String[][]{{"LocalDate", "DateOnly"}, {"LocalTime", "TimeOnly"}, {"LocalDateTime", "DateTime"}}) {
            String j = "java.time." + t[0] + ".";
            m(j + "parse/1", t[1] + ".Parse($1, CultureInfo.InvariantCulture)", SYSTEM, GLOBALIZATION);
            m(j + "parse/2", t[1] + ".ParseExact($1, $2, CultureInfo.InvariantCulture)", SYSTEM, GLOBALIZATION);
            m(j + "format/1", "$0.ToString($1, CultureInfo.InvariantCulture)", GLOBALIZATION);
        }
        m("java.time.LocalDate.getYear/0", "$0.Year");
        m("java.time.LocalDate.getMonthValue/0", "$0.Month");
        m("java.time.LocalDate.getDayOfMonth/0", "$0.Day");
        m("java.time.Duration.ofNanos/1", "TimeSpan.FromTicks(@1 / 100)", SYSTEM);
        m("java.time.Duration.ofMillis/1", "TimeSpan.FromMilliseconds($1)", SYSTEM);
        m("java.time.Duration.ofSeconds/1", "TimeSpan.FromSeconds($1)", SYSTEM);
        m("java.time.Duration.ofMinutes/1", "TimeSpan.FromMinutes($1)", SYSTEM);
        m("java.time.Duration.toMillis/0", "(long) @0.TotalMilliseconds");
        m("java.time.Duration.toNanos/0", "@0.Ticks * 100");
        m("java.time.Duration.getSeconds/0", "(long) @0.TotalSeconds");
        m("java.time.Duration.between/2", "@2 - @1");
        // ISO-8601, PT1H30M, as XML Schema's duration is
        m("java.time.Duration.parse/1", "XmlConvert.ToTimeSpan($1)", "System.Xml");
        m("java.time.Duration.toString/0", "XmlConvert.ToString($0)", "System.Xml");
        m("java.util.Locale.forLanguageTag/1", "CultureInfo.GetCultureInfo($1)", GLOBALIZATION);
        m("java.util.Locale.toLanguageTag/0", "$0.Name");
        m("java.time.Clock.systemDefaultZone/0", "TimeProvider.System", SYSTEM);
        m("java.time.Clock.systemUTC/0", "TimeProvider.System", SYSTEM);
        m("java.time.Clock.instant/0", "$0.GetUtcNow()");
        m("java.time.Clock.millis/0", "$0.GetUtcNow().ToUnixTimeMilliseconds()");

        // Java's URI is relative or absolute
        m("java.net.URI.create/1", "new Uri($1, UriKind.RelativeOrAbsolute)", SYSTEM);
        m("java.net.URI.new(String)", "new Uri($1, UriKind.RelativeOrAbsolute)", SYSTEM);
        m("java.net.URI.getPath/0", "$0.AbsolutePath");
        m("java.net.URI.getScheme/0", "$0.Scheme");
        m("java.net.URI.getHost/0", "$0.Host");
        m("java.net.URI.getPort/0", "$0.Port");
        m("java.net.URI.getQuery/0", "$0.Query");
        m("java.net.URI.isAbsolute/0", "$0.IsAbsoluteUri");
        m("java.util.UUID.randomUUID/0", "Guid.NewGuid()", SYSTEM);
        m("java.util.UUID.fromString/1", "Guid.Parse($1)", SYSTEM);
        m("java.util.UUID.nameUUIDFromBytes/1", "JavaUuid.NameUuidFromBytes($1)", COMPAT);

        // BigDecimal is decimal: its arithmetic, operators
        m("java.math.BigDecimal.valueOf/1", "(decimal) @1");
        m("java.math.BigDecimal.new(String)", "decimal.Parse($1, CultureInfo.InvariantCulture)", GLOBALIZATION);
        m("java.math.BigDecimal.new(int)", "(decimal) @1");
        m("java.math.BigDecimal.new(long)", "(decimal) @1");
        m("java.math.BigDecimal.new(double)", "(decimal) @1");
        m("java.math.BigDecimal.add/1", "@0 + @1");
        m("java.math.BigDecimal.subtract/1", "@0 - @1");
        m("java.math.BigDecimal.multiply/1", "@0 * @1");
        m("java.math.BigDecimal.negate/0", "-@0");
        m("java.math.BigDecimal.doubleValue/0", "(double) @0");
        m("java.math.BigDecimal.intValue/0", "(int) @0");
        m("java.math.BigDecimal.longValue/0", "(long) @0");
        m("java.math.BigDecimal.signum/0", "Math.Sign(@0)", SYSTEM);
        m("java.math.BigDecimal.abs/0", "Math.Abs(@0)", SYSTEM);
        m("java.math.BigDecimal.scale/0", "@0.Scale");
        m("java.math.BigDecimal.stripTrailingZeros/0", "JavaMath.StripTrailingZeros($0)", COMPAT);
        m("java.math.BigDecimal.setScale/2", "Math.Round(@0, $1, $2)", SYSTEM);
        m("java.math.BigDecimal.divide/3", "Math.Round(@0 / @1, $2, $3)", SYSTEM);
        m("java.math.BigInteger.add/1", "@0 + @1");
        m("java.math.BigInteger.subtract/1", "@0 - @1");
        m("java.math.BigInteger.multiply/1", "@0 * @1");
        m("java.math.BigInteger.negate/0", "-@0");
        m("java.math.BigInteger.valueOf/1", "new BigInteger($1)", NUMERICS);

        // Random and the interface it implements since Java 17, RandomGenerator
        for (String r : List.of("java.util.Random.", "java.util.random.RandomGenerator.")) {
            m(r + "nextInt/0", "$0.Next()");
            m(r + "nextInt/1", "$0.Next($1)");
            m(r + "nextInt/2", "$0.Next($1, $2)");
            m(r + "nextLong/0", "$0.NextInt64()");
            m(r + "nextLong/1", "$0.NextInt64($1)");
            m(r + "nextLong/2", "$0.NextInt64($1, $2)");
            m(r + "nextDouble/0", "$0.NextDouble()");
            m(r + "nextDouble/1", "($0.NextDouble() * @1)");
            m(r + "nextFloat/0", "$0.NextSingle()");
            m(r + "nextBoolean/0", "$0.Next(2) == 1");
        }

        // the encoder and decoder are Convert's methods; Java's byte[] is C#'s sbyte[]
        m("java.util.Base64.Encoder.encodeToString/1", "Convert.ToBase64String((byte[]) (object) $1)", SYSTEM);
        m("java.util.Base64.Decoder.decode(String)", "(sbyte[]) (object) Convert.FromBase64String($1)", SYSTEM);
        m("java.util.HexFormat.formatHex(byte[])", "Convert.ToHexStringLower((byte[]) (object) $1)", SYSTEM);
        m("java.security.MessageDigest.getInstance/1", "JavaDigest.GetInstance($1)", COMPAT);
        m("java.security.MessageDigest.digest(byte[])", "(sbyte[]) (object) $0.ComputeHash((byte[]) (object) $1)");

        m("java.lang.String.codePointAt/1", "char.ConvertToUtf32($0, $1)");
        m("java.lang.Character.charCount/1", "(@1 >= 0x10000 ? 2 : 1)");
        m("java.lang.Character.toChars/1", "char.ConvertFromUtf32($1).ToCharArray()");
        m("java.lang.Thread.sleep(long)", "Thread.Sleep(TimeSpan.FromMilliseconds($1))", THREADING, SYSTEM);
        m("java.lang.Thread.interrupt/0", "$0.Interrupt()");
    }

    /** Service loaders, class loaders, paths and files. */
    private static void services() {
        m("java.util.ServiceLoader.load/1", "JavaServiceLoader.Load<{R0}>()", COMPAT);
        m("java.util.ServiceLoader.load/2", "JavaServiceLoader.Load<{R0}>($2)", COMPAT);
        m("java.util.ServiceLoader.findFirst/0", "$0.FirstOrDefault()", LINQ);
        m("java.util.ServiceLoader.stream/0", "$0");
        m("java.lang.Class.getClassLoader/0", "$0.Assembly");
        // resources are the assembly's embedded ones, found by their Java path (JavaResources)
        m("java.lang.Class.getResource/1", "JavaResources.GetResource($0, $1)", COMPAT);
        m("java.lang.Class.getResourceAsStream/1", "JavaResources.GetResourceAsStream($0, $1)", COMPAT);
        m("java.lang.ClassLoader.getResource/1", "JavaResources.GetResource($0, $1)", COMPAT);
        m("java.lang.ClassLoader.getResourceAsStream/1", "JavaResources.GetResourceAsStream($0, $1)", COMPAT);
        m("java.lang.ClassLoader.getSystemResource/1", "JavaResources.GetResource(Assembly.GetEntryAssembly(), $1)",
                COMPAT, REFLECTION);
        m("java.lang.ClassLoader.getSystemResourceAsStream/1",
                "JavaResources.GetResourceAsStream(Assembly.GetEntryAssembly(), $1)", COMPAT, REFLECTION);
        m("java.net.URL.openStream/0", "JavaResources.OpenStream($0)", COMPAT);
        m("java.net.URL.toExternalForm/0", "$0.ToString()");
        m("java.net.URL.toURI/0", "$0");
        m("java.net.URL.getPath/0", "$0.AbsolutePath");
        m("java.net.URL.getProtocol/0", "$0.Scheme");
        m("java.net.URI.toURL/0", "$0");
        m("java.net.URL.new(String)", "new Uri($1)", SYSTEM);
        m("java.lang.ClassLoader.loadClass/1", "$0.GetType($1, true)");
        m("java.lang.Class.asSubclass/1", "$0");
        m("java.util.Optional.stream/0", "$0.AsStream()", COMPAT);

        String p = "java.nio.file.Path.";
        chooser(p + "of/2", (method, call) -> rule(call != null && call.parameterExpressions().size() == 1
                ? "new FileInfo($1)" : "new FileInfo(System.IO.Path.Combine($*))", IO));
        m(p + "of(URI)", "new FileInfo($1.LocalPath)", IO);
        m(p + "getFileName/0", "new FileInfo($0.Name)", IO);
        m(p + "toFile/0", "new JavaFile($0.ToString())", COMPAT);
        m(p + "toAbsolutePath/0", "new FileInfo($0.FullName)", IO);
        m(p + "getParent/0", "$0.Directory is { } d ? new FileInfo(d.FullName) : null", IO);
        m(p + "resolve(String)", "new FileInfo(System.IO.Path.Combine($0.ToString(), $1))", IO);
        m("java.nio.file.Paths.get/2", "new FileInfo(System.IO.Path.Combine($*))", IO);
        String f = "java.nio.file.Files.";
        m(f + "readAllBytes/1", "(sbyte[]) (object) File.ReadAllBytes($1.FullName)", IO);
        m(f + "readString/1", "File.ReadAllText($1.FullName)", IO);
        m(f + "writeString/2", "File.WriteAllText($1.FullName, $2)", IO);
        m(f + "exists/2", "$1.Exists");
        m(f + "isDirectory/2", "Directory.Exists($1.FullName)", IO);
        m(f + "isRegularFile/2", "$1.Exists");
        m(f + "newInputStream/2", "$1.OpenRead()");
        m(f + "newOutputStream/2", "$1.Create()");
    }

    /** Futures as tasks, executors as task schedulers, latches and locks. */
    private static void concurrency() {
        String f = "java.util.concurrent.CompletableFuture.";
        String startNew = "Task.Factory.StartNew($1, CancellationToken.None, TaskCreationOptions.None, $2)";
        m(f + "supplyAsync/1", "Task.Run($1)", TASKS);
        m(f + "supplyAsync/2", startNew, TASKS, THREADING);
        m(f + "runAsync/1", "Task.Run($1)", TASKS);
        m(f + "runAsync/2", startNew, TASKS, THREADING);
        m(f + "completedFuture/1", "Task.FromResult($1)", TASKS);
        m(f + "allOf/1", "Task.WhenAll($*)", TASKS);
        // Task<T> is invariant, and C# infers R from the lambda where Java has it from the target: explicit; a
        // future of Void (allOf's) is a plain Task
        chooser(f + "thenApply/1", (method, call) -> rule(voidFuture(call) ? "$0.ThenApply<{R0}>($1)"
                : "$0.ThenApply<{T0}, {R0}>($1)", COMPAT));
        chooser(f + "thenCompose/1", (method, call) -> rule(voidFuture(call) ? "$0.ThenCompose<{R0}>($1)"
                : "$0.ThenCompose<{T0}, {R0}>($1)", COMPAT));
        m(f + "thenAccept/1", "$0.ThenAccept($1)", COMPAT);
        m(f + "join/0", "$0.Result");
        m(f + "get/0", "$0.Result");
        m(f + "isDone/0", "$0.IsCompleted");
        m("java.util.concurrent.Future.get/0", "$0.Result");
        m("java.util.concurrent.Future.isDone/0", "$0.IsCompleted");
        m("java.util.concurrent.Executor.execute/1",
                "Task.Factory.StartNew($1, CancellationToken.None, TaskCreationOptions.None, $0)", TASKS, THREADING);
        m("java.util.concurrent.ExecutorService.submit/1",
                "Task.Factory.StartNew($1, CancellationToken.None, TaskCreationOptions.None, $0)", TASKS, THREADING);
        // the pool's sizes and keep-alive are the .NET thread pool's
        m("java.util.concurrent.ThreadPoolExecutor.new/5", "TaskScheduler.Default", TASKS);
        m("java.util.concurrent.ThreadPoolExecutor.new/6", "TaskScheduler.Default", TASKS);
        m("java.util.concurrent.ThreadPoolExecutor.new/7", "TaskScheduler.Default", TASKS);
        for (String pool : List.of("newCachedThreadPool/0", "newFixedThreadPool/1", "newWorkStealingPool/0",
                "newVirtualThreadPerTaskExecutor/0")) {
            m("java.util.concurrent.Executors." + pool, "TaskScheduler.Default", TASKS);
        }

        String l = "java.util.concurrent.CountDownLatch.";
        m(l + "countDown/0", "$0.Signal()");
        m(l + "await/0", "$0.Wait()");
        m(l + "getCount/0", "(long) @0.CurrentCount");
        m("java.util.concurrent.locks.ReentrantReadWriteLock.new/0",
                "new ReaderWriterLockSlim(LockRecursionPolicy.SupportsRecursion)", THREADING);
        m("java.util.concurrent.locks.ReentrantReadWriteLock.new/1",
                "new ReaderWriterLockSlim(LockRecursionPolicy.SupportsRecursion)", THREADING);
        m("java.util.concurrent.locks.ReadWriteLock.readLock/0", "$0.ReadLock()", COMPAT);
        m("java.util.concurrent.locks.ReadWriteLock.writeLock/0", "$0.WriteLock()", COMPAT);
    }

    /** java.lang.Class and java.lang.reflect: System.Type and System.Reflection. */
    private static void reflection() {
        String c = "java.lang.Class.";
        String declared = "BindingFlags.DeclaredOnly | BindingFlags.Public | BindingFlags.NonPublic"
                          + " | BindingFlags.Instance | BindingFlags.Static";
        m(c + "isAssignableFrom/1", "$0.IsAssignableFrom($1)");
        m(c + "isInstance/1", "$0.IsInstanceOfType($1)");
        m(c + "cast/1", "({R}) @1");
        m(c + "isArray/0", "$0.IsArray");
        m(c + "isEnum/0", "$0.IsEnum");
        m(c + "isInterface/0", "$0.IsInterface");
        m(c + "isPrimitive/0", "$0.IsPrimitive");
        m(c + "getSuperclass/0", "$0.BaseType");
        m(c + "getInterfaces/0", "$0.GetInterfaces()");
        m(c + "getComponentType/0", "$0.GetElementType()");
        m(c + "arrayType/0", "$0.MakeArrayType()");
        m(c + "getPackage/0", "$0.Namespace");
        m("java.lang.Package.getName/0", "$0");
        m(c + "getEnumConstants/0", "$0.GetEnumValues().Cast<object>().ToArray()", LINQ);
        m(c + "getDeclaredMethods/0", "$0.GetMethods(" + declared + ")", REFLECTION);
        m(c + "getDeclaredFields/0", "$0.GetFields(" + declared + ")", REFLECTION);
        m(c + "getMethods/0", "$0.GetMethods()");
        m(c + "getFields/0", "$0.GetFields()");
        // the parameter types are varargs: none, or an array
        chooser(c + "getMethod/2", (method, call) -> rule(call != null && call.parameterExpressions().size() == 1
                ? "$0.GetMethod($1, Type.EmptyTypes)" : "$0.GetMethod($*)", SYSTEM));
        chooser(c + "getDeclaredConstructor/1", (method, call) -> rule("$0.GetConstructor("
                + "BindingFlags.Instance | BindingFlags.Public | BindingFlags.NonPublic, "
                + (call != null && call.parameterExpressions().isEmpty() ? "Type.EmptyTypes" : "$*") + ")",
                REFLECTION, SYSTEM));
        m(c + "getModifiers/0", "JavaModifier.Of($0)", COMPAT);
        chooser("java.lang.reflect.Constructor.newInstance/1", (method, call) -> rule(
                call != null && call.parameterExpressions().isEmpty() ? "$0.Invoke(null)" : "$0.Invoke(new object[] { $* })"));
        // annotations are attributes
        String a = "java.lang.reflect.AnnotatedElement.";
        m(a + "getAnnotation/1", "$0.GetCustomAttribute<{R}>()", REFLECTION);
        m(a + "isAnnotationPresent/1", "$0.IsDefined($1, false)");
        m("java.lang.annotation.Annotation.annotationType/0", "$0.GetType()");

        for (String member : List.of("java.lang.reflect.Method", "java.lang.reflect.Field", "java.lang.reflect.Parameter",
                "java.lang.reflect.Executable", "java.lang.reflect.Member")) {
            m(member + ".getName/0", "$0.Name");
        }
        String me = "java.lang.reflect.Method.";
        m(me + "getDeclaringClass/0", "$0.DeclaringType");
        m("java.lang.reflect.Member.getDeclaringClass/0", "$0.DeclaringType");
        m(me + "getReturnType/0", "$0.ReturnType");
        m(me + "getGenericReturnType/0", "$0.ReturnType");
        m("java.lang.reflect.Executable.getParameters/0", "$0.GetParameters()");
        m("java.lang.reflect.Executable.getParameterCount/0", "$0.GetParameters().Length");
        m(me + "getParameterTypes/0", "$0.GetParameters().Select(p => p.ParameterType).ToArray()", LINQ);
        m("java.lang.reflect.Executable.getParameterTypes/0", "$0.GetParameters().Select(p => p.ParameterType).ToArray()",
                LINQ);
        // Java's compiler-made bridges have no counterpart in C#; its synthetic members are compiler-generated
        m(me + "isBridge/0", "false");
        m("java.lang.reflect.Executable.isVarArgs/0",
                "($0.GetParameters().LastOrDefault()?.IsDefined(typeof(ParamArrayAttribute), false) ?? false)", LINQ, SYSTEM);
        m(c + "forName/1", "Type.GetType($1, true)", SYSTEM);
        m("java.lang.reflect.Executable.isSynthetic/0",
                "$0.IsDefined(typeof(CompilerGeneratedAttribute), false)", "System.Runtime.CompilerServices");
        m(me + "getModifiers/0", "JavaModifier.Of($0)", COMPAT);
        m("java.lang.reflect.Field.getModifiers/0", "JavaModifier.Of($0)", COMPAT);
        chooser(me + "invoke/2", (method, call) -> rule(call != null && call.parameterExpressions().size() == 1
                ? "$0.Invoke($1, null)" : "$0.Invoke($1, new object[] { $+ })"));
        String f = "java.lang.reflect.Field.";
        m(f + "getType/0", "$0.FieldType");
        m(f + "getGenericType/0", "$0.FieldType");
        m(f + "get/1", "$0.GetValue($1)");
        m(f + "set/2", "$0.SetValue($1, $2)");
        m("java.lang.reflect.Parameter.getType/0", "$0.ParameterType");
        m("java.lang.reflect.Parameter.getParameterizedType/0", "$0.ParameterType");
        m("java.lang.reflect.ParameterizedType.getRawType/0", "$0.GetGenericTypeDefinition()");
        m("java.lang.reflect.ParameterizedType.getActualTypeArguments/0", "$0.GetGenericArguments()");
        m("java.lang.reflect.TypeVariable.getBounds/0", "$0.GetGenericParameterConstraints()");
        m("java.lang.reflect.GenericArrayType.getGenericComponentType/0", "$0.GetElementType()");
        m("java.lang.reflect.Proxy.isProxyClass/1", "typeof(DispatchProxy).IsAssignableFrom($1)", REFLECTION);
        for (String[] mod : new String[][]{{"isPublic", "0x0001"}, {"isPrivate", "0x0002"}, {"isProtected", "0x0004"},
                {"isStatic", "0x0008"}, {"isFinal", "0x0010"}, {"isAbstract", "0x0400"}, {"isInterface", "0x0200"}}) {
            m("java.lang.reflect.Modifier." + mod[0] + "/1", "(@1 & " + mod[1] + ") != 0");
        }
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
    @Override
    public Rule constructor(MethodInfo constructor) {
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

    private static final Map<String, String> FIELDS = Map.ofEntries(
            Map.entry("java.lang.System.out", "Console.Out"),
            Map.entry("java.lang.System.err", "Console.Error"),
            Map.entry("java.lang.Integer.MAX_VALUE", "int.MaxValue"),
            Map.entry("java.math.BigDecimal.ZERO", "0m"),
            Map.entry("java.math.BigDecimal.ONE", "1m"),
            Map.entry("java.math.BigDecimal.TWO", "2m"),
            Map.entry("java.math.BigDecimal.TEN", "10m"),
            Map.entry("java.math.BigInteger.ZERO", "BigInteger.Zero"),
            Map.entry("java.math.BigInteger.ONE", "BigInteger.One"),
            Map.entry("java.math.BigInteger.TWO", "new BigInteger(2)"),
            Map.entry("java.math.BigInteger.TEN", "new BigInteger(10)"),
            // a directed rounding: FLOOR, CEILING and DOWN round every value, as Java's do; HALF_UP and HALF_EVEN
            // a midpoint; UP and HALF_DOWN have no MidpointRounding, and stay unmapped
            Map.entry("java.math.RoundingMode.FLOOR", "MidpointRounding.ToNegativeInfinity"),
            Map.entry("java.math.RoundingMode.CEILING", "MidpointRounding.ToPositiveInfinity"),
            Map.entry("java.math.RoundingMode.DOWN", "MidpointRounding.ToZero"),
            Map.entry("java.math.RoundingMode.HALF_UP", "MidpointRounding.AwayFromZero"),
            Map.entry("java.math.RoundingMode.HALF_EVEN", "MidpointRounding.ToEven"),
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
            // .NET's F is a digit that is not written when it is a trailing zero, its separator not when all are
            Map.entry("java.time.format.DateTimeFormatter.ISO_LOCAL_DATE", "\"yyyy-MM-dd\""),
            Map.entry("java.time.format.DateTimeFormatter.ISO_LOCAL_TIME", "\"HH:mm:ss.FFFFFFF\""),
            Map.entry("java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME", "\"yyyy-MM-dd'T'HH:mm:ss.FFFFFFF\""),
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
    @Override
    public FieldMapping field(FieldInfo fieldInfo) {
        String csharp = FIELDS.get(fieldInfo.owner().fullyQualifiedName() + "." + fieldInfo.name());
        if (csharp == null) return null;
        String first = csharp.contains(".") ? csharp.substring(0, csharp.indexOf('.')) : "";
        return new FieldMapping(csharp, FIELD_NAMESPACES.get(first));
    }

    /**
     * The C# name of a translated method that overrides a JDK one, when the BCL's member has another name than the
     * PascalCase of the Java one: {@code close()} of an {@code AutoCloseable} is {@code Dispose()}.
     */
    @Override
    public String overrideName(MethodInfo method) {
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

    /** The receiver is a future of Void, as CompletableFuture.allOf's: a Task without a value. */
    private static boolean voidFuture(MethodCall call) {
        if (call == null || call.object() == null || call.object().parameterizedType() == null) return false;
        List<ParameterizedType> args = call.object().parameterizedType().parameters();
        return args.size() == 1 && args.getFirst().typeInfo() != null
               && "java.lang.Void".equals(args.getFirst().typeInfo().fullyQualifiedName());
    }

    /** The receiver is a collection parameter that is read-only in C# ({@link CSharpReadOnly}). */
    private static boolean readOnlyReceiver(MethodCall call) {
        return call != null && call.object() != null
               && CSharpExpressionPrinter.unwrap(call.object()) instanceof Expression e
               && e instanceof io.codelaser.maddi.cst.api.expression.VariableExpression ve
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
