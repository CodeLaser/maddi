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

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * The JDK in C# ({@link CSharpBcl}, {@code jdk-bcl.csmap}): values, collections and their navigation, concurrency,
 * reflection, services, the compatibility library where the BCL has no counterpart. Each test writes its samples' translations to {@code build/csharp-samples/}.
 */
public class TestJavaToCSharpBcl extends CommonJavaToCSharp {

    @Language("java")
    static final String VALUES = """
            package org.example.values;
            import java.math.BigDecimal;
            import java.net.URI;
            import java.time.Clock;
            import java.time.Duration;
            import java.time.LocalDate;
            import java.time.LocalDateTime;
            import java.time.OffsetDateTime;
            import java.util.Base64;
            import java.util.Map;
            import java.util.Random;
            import java.util.UUID;
            import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
            public class Values {
                static String id() { return UUID.randomUUID().toString(); }
                static UUID parse(String s) { return UUID.fromString(s); }
                static URI uri(String s) { return URI.create(s); }
                static String path(URI uri) { return uri.getPath(); }
                static int compare(Object a, Object b) {
                    return new BigDecimal(a.toString()).compareTo(BigDecimal.valueOf(3L));
                }
                static double number(Object o) { return o instanceof Number n ? n.doubleValue() : 0; }
                static LocalDate date(String s) { return LocalDate.parse(s, ISO_LOCAL_DATE); }
                static LocalDateTime now(Clock clock) { return LocalDateTime.now(clock); }
                static OffsetDateTime at(OffsetDateTime t) { return t; }
                static Duration since(long before, long after) { return Duration.ofNanos(after - before); }
                static int roll(Random random) { return random.nextInt(6); }
                static String encode(byte[] bytes) { return Base64.getEncoder().encodeToString(bytes); }
                static void print(Map<String, Integer> map) { map.forEach((k, v) -> System.out.println(k + v)); }
            }
            """;

    /**
     * The values of java.time, java.net and java.math are the BCL's structs; a Number is an IConvertible; a
     * formatter is a format string; Base64 is Convert.
     */
    @Test
    public void values() {
        String cs = translate("Values", VALUES);
        contains(cs, "internal static string Id() => Guid.NewGuid().ToString();");
        contains(cs, "internal static Guid? Parse(string s) => Guid.Parse(s);");
        contains(cs, "internal static Uri Uri(string s) => new Uri(s, UriKind.RelativeOrAbsolute);");
        contains(cs, "internal static string Path(Uri uri) => uri.AbsolutePath;");
        contains(cs, ".CompareTo((decimal) 3L)");
        contains(cs, "o is IConvertible n ? Convert.ToDouble(n) : 0");
        contains(cs, "DateOnly.ParseExact(s, \"yyyy-MM-dd\", CultureInfo.InvariantCulture)");
        contains(cs, "internal static DateTime? Now(TimeProvider clock) => clock.GetLocalNow().DateTime;");
        contains(cs, "internal static DateTimeOffset? At(DateTimeOffset? t) => t;");
        contains(cs, "TimeSpan.FromTicks((after - before) / 100)");
        contains(cs, "random.Next(6)");
        contains(cs, "Convert.ToBase64String((byte[]) (object) bytes)");
        contains(cs, "map.ForEach((k, v) => Console.Out.WriteLine(k + v))");
    }

    @Language("java")
    private static final String CONCURRENCY = """
            package org.example.concurrency;
            import java.util.List;
            import java.util.concurrent.CompletableFuture;
            import java.util.concurrent.CountDownLatch;
            import java.util.concurrent.Executor;
            import java.util.concurrent.SynchronousQueue;
            import java.util.concurrent.ThreadPoolExecutor;
            import java.util.concurrent.TimeUnit;
            import java.util.concurrent.atomic.AtomicBoolean;
            import java.util.concurrent.locks.ReadWriteLock;
            import java.util.concurrent.locks.ReentrantReadWriteLock;
            public class Work {
                private final Executor executor = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 1, TimeUnit.SECONDS,
                        new SynchronousQueue<>());
                private final CountDownLatch latch = new CountDownLatch(1);
                private final AtomicBoolean done = new AtomicBoolean(false);
                private final ReadWriteLock lock = new ReentrantReadWriteLock(true);
                CompletableFuture<Integer> length(String s) {
                    return CompletableFuture.supplyAsync(() -> s, executor).thenApply(String::length);
                }
                int total(List<CompletableFuture<Integer>> futures) {
                    return futures.stream().mapToInt(CompletableFuture::join).sum();
                }
                void finish() throws InterruptedException {
                    done.compareAndSet(false, true);
                    latch.countDown();
                    latch.await();
                }
                void guarded(Runnable r) {
                    var writeLock = lock.writeLock();
                    writeLock.lock();
                    try { r.run(); } finally { writeLock.unlock(); }
                }
            }
            """;

    /**
     * A future is a task, an executor its task scheduler (a thread pool executor the .NET thread pool), a latch a
     * CountdownEvent; a read-write lock is a ReaderWriterLockSlim; continuations, lock halves and atomics are the
     * compatibility library's.
     */
    @Test
    public void concurrency() {
        String cs = translate("Work", CONCURRENCY);
        contains(cs, "private readonly TaskScheduler executor = TaskScheduler.Default;");
        contains(cs, "private readonly CountdownEvent latch = new CountdownEvent(1);");
        contains(cs, "private readonly AtomicBoolean done = new AtomicBoolean(false);");
        contains(cs, "new ReaderWriterLockSlim(LockRecursionPolicy.SupportsRecursion)");
        contains(cs, "Task.Factory.StartNew(() => s, CancellationToken.None, TaskCreationOptions");
        contains(cs, ".ThenApply<string, int>(");
        contains(cs, ".Result");
        contains(cs, "done.CompareAndSet(false, true);");
        contains(cs, "latch.Signal();");
        contains(cs, "latch.Wait();");
        contains(cs, "var writeLock = @lock.WriteLock();");
        contains(cs, "writeLock.Lock();");
    }

    @Language("java")
    private static final String REFLECTION = """
            package org.example.reflection;
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;
            import java.lang.reflect.Field;
            import java.lang.reflect.GenericArrayType;
            import java.lang.reflect.Method;
            import java.lang.reflect.Modifier;
            import java.lang.reflect.ParameterizedType;
            import java.lang.reflect.Type;
            import java.lang.reflect.WildcardType;
            import java.util.ArrayList;
            import java.util.List;
            public class Mirror {
                @Retention(RetentionPolicy.RUNTIME)
                public @interface Tool { String value() default ""; }
                static Class<?> raw(Type type) {
                    if (type instanceof Class<?> clazz) return clazz;
                    if (type instanceof ParameterizedType pt && pt.getRawType() instanceof Class<?> r) return r;
                    if (type instanceof WildcardType w) return raw(w.getUpperBounds()[0]);
                    if (type instanceof GenericArrayType g) return raw(g.getGenericComponentType()).arrayType();
                    return Object.class;
                }
                static List<String> tools(Class<?> c) {
                    List<String> names = new ArrayList<>();
                    for (Method m : c.getDeclaredMethods()) {
                        if (m.isBridge() || !m.isAnnotationPresent(Tool.class)) continue;
                        names.add(m.getName() + m.getAnnotation(Tool.class).value());
                    }
                    return names;
                }
                static boolean instanceField(Field f) { return !Modifier.isStatic(f.getModifiers()); }
                static final Type MAP_TYPE = new ParameterizedType() {
                    @Override public Type[] getActualTypeArguments() { return new Type[] {String.class, Object.class}; }
                    @Override public Type getRawType() { return java.util.Map.class; }
                    @Override public Type getOwnerType() { return null; }
                };
            }
            """;

    /**
     * Class is System.Type, the members of java.lang.reflect are System.Reflection's; the kinds of
     * java.lang.reflect.Type are one System.Type, told apart by property patterns; annotations are attributes.
     */
    @Test
    public void reflection() {
        String cs = translate("Mirror", REFLECTION);
        contains(cs, "if (type is Type { IsGenericParameter: false, IsConstructedGenericType: false } clazz)");
        contains(cs, "type is Type { IsConstructedGenericType: true } pt &&");
        contains(cs, "if (type is IJavaWildcardType w) { return Raw(w.GetUpperBounds()[0]); }");
        contains(cs, "if (type is Type { IsArray: true } g) { return Raw(g.GetElementType()).MakeArrayType(); }");
        contains(cs, "foreach (MethodInfo m in c.GetMethods(BindingFlags.DeclaredOnly");
        contains(cs, "m.Name + m.GetCustomAttribute<ToolAttribute>().Value");
        contains(cs, "!((JavaModifier.Of(f) & 0x0008) != 0)");
        // known-ugly: C# has no bridge methods, isBridge() is the constant false, and the printer does not fold it
        contains(cs, "if (false || !m.IsDefined(typeof(ToolAttribute), false))");
        contains(cs, "internal static readonly Type MAP_TYPE = typeof(IDictionary<string, object>);");
    }

    @Language("java")
    private static final String SERVICES = """
            package org.example.services;
            import java.io.IOException;
            import java.nio.file.Files;
            import java.nio.file.Path;
            import java.util.ServiceLoader;
            public class Services {
                public interface Factory { String name(); int rank(); }
                static Factory factory() {
                    return ServiceLoader.load(Factory.class).findFirst().orElse(null);
                }
                static byte[] read(String dir, String file) throws IOException {
                    return Files.readAllBytes(Path.of(dir, file));
                }
                static String type(Path path) { return path.getFileName().toString(); }
                static String type(String path) { return type(Path.of(path)); }
            }
            """;

    /**
     * A service loader is the implementations in the loaded assemblies, an enumerable; a path a FileInfo (Java
     * overloads on Path and String, which a string path would merge).
     */
    @Test
    public void services() {
        String cs = translate("Services", SERVICES);
        contains(cs, "internal static IFactory Factory() => JavaServiceLoader.Load<IFactory>().FirstOrDefault()");
        contains(cs, "File.ReadAllBytes(new FileInfo(System.IO");
        // known-ugly: getFileName() is a Path, a FileInfo of the name, whose ToString() is the name again
        contains(cs, "internal static string Type(FileInfo path) => (new FileInfo(path.Name)).ToString();");
        contains(cs, "internal static string Type(string path) => Type(new FileInfo(path));");
    }

    @Language("java")
    private static final String DEFAULTS = """
            package org.example.defaults;
            import java.util.Objects;
            public class Limits {
                Integer limit;
                Boolean random;
                Integer getLimit() { return limit; }
                int limit() { return Objects.requireNonNullElse(getLimit(), 400); }
                boolean random() { return Objects.requireNonNullElse(random, true); }
            }
            """;

    /** Objects.requireNonNullElse(i, d) is i ?? d of the int? itself: an int on the left of ?? does not compile. */
    @Test
    public void nonNullElseOfBoxed() {
        String cs = translate("Limits", DEFAULTS);
        contains(cs, "internal int Limit() => (int) (GetLimit() ?? 400);");
        contains(cs, "internal bool Random() => (bool) (random ?? true);");
    }

    @Language("java")
    private static final String VALUE_REFERENCES = """
            package org.example.config;
            import java.time.Duration;
            import java.util.List;
            import java.util.Map;
            import java.util.Objects;
            public class Config {
                enum Style { AND, OR }
                private Style style = null;
                private Duration limit = null;
                Style getStyle() { return style; }
                Style style() { return Objects.requireNonNullElse(style, Style.OR); }
                void setStyle(Style style) { this.style = Objects.requireNonNull(style); }
                boolean unset() { return style == null; }
                long millis() { return limit.toMillis(); }
                int rank() { return style.compareTo(Style.AND); }
                void collect(List<Style> styles) { styles.add(style); }
                Style lookup(Map<String, Style> styles, String key) { return styles.get(key); }
                String name() {
                    switch (style) {
                        case AND: return "and";
                        default: return "or";
                    }
                }
            }
            """;

    @Language("java")
    private static final String VALUE_USES = """
            package org.example.values;
            import java.math.BigDecimal;
            import java.util.List;
            import java.util.function.BinaryOperator;
            import java.util.function.Supplier;
            public class Uses {
                enum Kind { FILTER, JOIN }
                static class Node {
                    private Kind kind = Kind.JOIN;
                    public Kind getKind() { return kind; }
                    Kind other() { return Kind.FILTER; }
                }
                static class Total implements Supplier<BigDecimal> {
                    private BigDecimal a = BigDecimal.ONE;
                    private BigDecimal b = BigDecimal.TEN;
                    @Override public BigDecimal get() { return a.add(b); }
                }
                static BigDecimal[] zeros(int n) { return new BigDecimal[n]; }
                static BigDecimal[] scores(BigDecimal hard, BigDecimal soft) {
                    BigDecimal[] scores = new BigDecimal[] { hard, BigDecimal.ZERO };
                    scores[1] = soft;
                    return scores;
                }
                interface Constructor { String build(Kind kind, Integer weight); }
                static class Named implements Constructor {
                    public String build(Kind kind, Integer weight) { return "named"; }
                }
                static Constructor anonymous() { return (kind, weight) -> "lambda"; }
                static BinaryOperator<BigDecimal> adder() { return BigDecimal::add; }
                static Kind pick(Node node, boolean join) {
                    var kind = Kind.FILTER;
                    if (join) kind = node.getKind();
                    return kind;
                }
            }
            """;

    /** Where C# has the value type itself, an X? is cast, and a type named as a property in scope is qualified. */
    @Test
    public void valueUses() {
        String cs = translate("Uses", VALUE_USES);
        contains(cs, "internal static decimal[] Zeros(int n) => new decimal[n];");
        // an array's element is the value type itself
        contains(cs, "decimal[] scores = new decimal[] { (decimal) hard, 0m };");
        contains(cs, "scores[1] = (decimal) soft;");
        // a functional interface's lambda class passes its Kind? and int? to the delegate's Kind and int
        contains(cs, "public string Build(Kind? kind, int? weight) => f((Kind) kind, (int) weight);");
        contains(cs, "Adder() => (decimal p0, decimal p1) => p0 + p1;");
        // Java's var is the reference, a Kind? that a later assignment fits
        contains(cs, "Kind? kind = Kind.Filter;");
        // in an expression the property Kind hides the type Kind ("Color Color" holds for a Kind, not a Kind?); a
        // declaration's type is looked up as a type only
        contains(cs, "public Kind? Kind { get; } = Org.Example.Values.Uses.Kind.Join;");
        // an operand of a template, and Supplier's T Get() of a decimal: the value type
        contains(cs, "public decimal Get() => ((decimal) a) + ((decimal) b);");
    }

    /**
     * A Java reference that is a C# value type, a simple enum or Duration's TimeSpan, is its X? as an Integer is
     * int?: Java's null fits, and a use as the value casts as an Integer's does.
     */
    @Test
    public void valueReferences() {
        String cs = translate("Config", VALUE_REFERENCES);
        contains(cs, "private Style? style = null;");
        contains(cs, "private TimeSpan? limit = null;");
        contains(cs, "internal Style? StyleValue() => style ?? Style.Or;");
        contains(cs, "this.style = (style ?? throw new NullReferenceException());");
        contains(cs, "internal bool Unset() => style == null;");
        // a member of the value: cast, as Java unboxes
        contains(cs, "internal long Millis() => (long) ((TimeSpan) limit).TotalMilliseconds;");
        contains(cs, "internal int Rank() => ((Style) style).CompareTo(Style.And);");
        // a type argument is the value type itself, and a map's absent key null, not the enum's first member
        contains(cs, "internal void Collect(List<Style> styles) { styles.Add((Style) style); }");
        contains(cs, "=> styles.GetValueOrNull(key);");
        contains(cs, "case Style.And: return \"and\";");
    }

    @Language("java")
    private static final String PUTS = """
            package org.example.puts;
            import java.util.Map;
            public class Registry {
                void add(Map<String, Integer> counts, String key) { counts.put(key, 1); }
                Integer replace(Map<String, Integer> counts, String key) { return counts.put(key, 2); }
            }
            """;

    /** A statement template alone overrides the statement form; the JDK's mapping still has the value form. */
    @Test
    public void statementOnlyOverride() {
        policy = CSharpProgram.Policy.DEFAULT.withLibraries(CSharpLibraries.DEFAULT.withFirst(CSharpMappingFile.parse("""
                mapping client:add-only
                statement java.util.Map.put/2 -> $0.Add($1, $2)
                """, "add-only.csmap")));
        String cs = translate("Registry", PUTS);
        contains(cs, "counts.Add(key, 1);");
        contains(cs, "=> counts.Put(key, 2);");
    }

    @Language("java")
    private static final String FIELD_NAMESPACES = """
            package org.example.fields;
            import java.math.BigInteger;
            import java.math.RoundingMode;
            public class Constants {
                static boolean zero(Object x) { return BigInteger.ZERO.equals(x); }
                static Object rounding() { return RoundingMode.HALF_UP; }
            }
            """;

    /** A JDK field brings its own namespace, whatever else the file names. */
    @Test
    public void fieldNamespaces() {
        String cs = translate("Constants", FIELD_NAMESPACES);
        contains(cs, "using System;");
        contains(cs, "using System.Numerics;");
        contains(cs, "BigInteger.Zero.Equals(x)");
        contains(cs, "MidpointRounding.AwayFromZero");
    }

    @Language("java")
    private static final String GEOMETRY = """
            package org.example.geometry;
            import java.util.Map;
            public class Geometry {
                static double up(double x) { return Math.ceil(x); }
                static double radians(double degrees) { return Math.toRadians(degrees); }
                static Object pair(String k, int v) { return Map.entry(k, v); }
            }
            """;

    /** Math's methods by name and meaning, not by their PascalCase name; Map.entry has its namespace. */
    @Test
    public void mathAndEntries() {
        String cs = translate("Geometry", GEOMETRY);
        contains(cs, "Math.Ceiling(x)");
        contains(cs, "degrees * (Math.PI / 180.0)");
        contains(cs, "using System.Collections.Generic;");
        contains(cs, "KeyValuePair.Create(k, v)");
    }

    @Language("java")
    private static final String HISTORY = """
            package org.example.history;
            import java.util.ArrayList;
            import java.util.List;
            public class History extends ArrayList<String> {
                @Override
                public String remove(int index) { return super.remove(index); }
                void dropFirst() { remove(0); }
                static void dropFirst(History history) { history.remove(0); }
                static void dropFirst(List<String> list) { list.remove(0); }
                static String clean(String s) { return s.replaceAll("\\\\s+", "\\\\$"); }
            }
            """;

    /**
     * A call on a class that extends ArrayList reaches its overrides, also as a statement, and super's is the removal
     * itself; a plain list's statement is RemoveAt. replaceAll's replacement is in Java's syntax.
     */
    @Test
    public void listSubclassAndReplacement() {
        String cs = translate("History", HISTORY);
        contains(cs, "public override string Remove(int index) => base.Remove(index);");
        contains(cs, "internal static void DropFirst(History history) { history.Remove(0); }");
        contains(cs, "list.RemoveAt(0);");
        contains(cs, "JavaString.ReplaceAll(s, \"\\\\s+\", \"\\\\$\")");
    }

    @Language("java")
    private static final String NAVIGATION = """
            package org.example.navigation;
            import java.util.*;
            public class Navigation {
                static String floor(NavigableMap<Integer, String> map, int key) {
                    Map.Entry<Integer, String> entry = map.floorEntry(key);
                    return entry == null ? null : entry.getValue();
                }
                static Integer next(NavigableSet<Integer> set, int x) { return set.higher(x); }
                static String values(SequencedMap<String, String> map) { return String.join(",", map.sequencedValues()); }
                static void shake(List<String> list, Random random) {
                    Collections.rotate(list, 1);
                    Collections.swap(list, 0, 1);
                    Collections.shuffle(list, random);
                }
                static boolean same(int[][] a, int[][] b) { return Arrays.deepEquals(a, b); }
                static void blank(int[] a) { Arrays.fill(a, 1, 3, 0); }
                static TreeSet<String> byLength(Comparator<String> c) { return new TreeSet<>(c); }
                static SequencedCollection<Object> entities(Object a, Object b) {
                    var set = new LinkedHashSet<Object>();
                    set.add(a);
                    set.add(b);
                    return set;
                }
                static Object first(SequencedCollection<Object> c) { return c.getFirst(); }
                static final class Counter extends Number {
                    private int value;
                    public int intValue() { return value; }
                    public long longValue() { return value; }
                    public float floatValue() { return value; }
                    public double doubleValue() { return value; }
                }
                static long asLong(Number n) { return n.longValue(); }
                interface Listener extends java.io.Closeable { void changed(); default void close() { } }
                static final class Tracker implements Listener { public void changed() { } }
                static void done(Listener l) throws java.io.IOException { l.close(); }
                static final class Bag<V> extends AbstractSet<V> {
                    private final List<V> items = new ArrayList<>();
                    public Iterator<V> iterator() { return items.iterator(); }
                    public int size() { return items.size(); }
                    public boolean add(V v) { return !contains(v) && items.add(v); }
                }
                static int bagged(Set<String> s) { return s.size(); }
                static int bag() { var b = new Bag<String>(); b.add("x"); return bagged(b); }
                static final class Pair<E> implements Set<E> {
                    private final List<E> items = new ArrayList<>();
                    public int size() { return items.size(); }
                    public boolean isEmpty() { return items.isEmpty(); }
                    public boolean contains(Object o) { return items.contains(o); }
                    public Iterator<E> iterator() { return items.iterator(); }
                    public Object[] toArray() { return items.toArray(); }
                    public <T> T[] toArray(T[] a) { return items.toArray(a); }
                    public boolean add(E e) { if (items.contains(e)) return false; items.add(e); return true; }
                    public boolean remove(Object o) { return items.remove(o); }
                    public boolean containsAll(Collection<?> c) { return items.containsAll(c); }
                    public boolean addAll(Collection<? extends E> c) { return false; }
                    public boolean retainAll(Collection<?> c) { return false; }
                    public boolean removeAll(Collection<?> c) { return false; }
                    public void clear() { items.clear(); }
                }
                static ListIterator<String> from(List<String> l, int i) { return l.listIterator(i); }
                static final Map<Object, String> listeners = new WeakHashMap<>();
                static TreeMap<String, Integer> counts() { return new TreeMap<>(Comparator.reverseOrder()); }
                static String lookup(Map<String, String> m, Object k) {
                    if (!m.containsKey(k)) return null;
                    m.remove(k);
                    return m.get(k);
                }
                static String drop(Map<String, String> m, Object k) { return m.remove(k); }
            }
            """;

    /** NavigableMap, NavigableSet and SequencedMap on C#'s sorted and ordered collections, by the compatibility library. */
    @Test
    public void navigation() {
        String cs = translate("Navigation", NAVIGATION);
        contains(cs, "SortedDictionary<int, string> map");
        // a navigation's entry may be null: the local is the JavaEntry, not a KeyValuePair
        contains(cs, "var entry = map.FloorEntry(key);");
        contains(cs, "set.Higher(x)");
        contains(cs, "map.Values");
        contains(cs, "JavaCollections.Rotate(list, 1);");
        contains(cs, "JavaCollections.Swap(list, 0, 1);");
        contains(cs, "JavaCollections.Shuffle(list, random);");
        contains(cs, "JavaArrays.DeepEquals(a, b)");
        contains(cs, "Array.Fill(a, 0, 1, 3 - 1);");
        // a SequencedCollection is a List's or a LinkedHashSet's: ICollection
        contains(cs, "internal static ICollection<object> Entities(object a, object b)");
        contains(cs, "c.First()");
        contains(cs, "l.Iterator(i)");
        contains(cs, "new Dictionary<object, string>()");
        // a class extending Number: the compatibility library's JavaNumber, an IConvertible
        contains(cs, "internal sealed class Counter : JavaNumber {");
        contains(cs, "public override int IntValue() => value;");
        // a default close() of a Closeable implements IDisposable's Dispose explicitly, which one named Dispose hides
        contains(cs, "void IDisposable.Dispose() { }");
        contains(cs, "l.Dispose();");
        // a class extending AbstractSet: the compatibility library's JavaSet, an ISet on its Java methods
        contains(cs, "internal sealed class Bag<V> : JavaSet<V> {");
        contains(cs, "public override int Size() => items.Count;");
        // one implementing Set itself likewise, overriding JavaSet's methods
        contains(cs, "internal sealed class Pair<E> : JavaSet<E> {");
        // contains(Object), remove(Object) of a List<E>: the compatibility library's, which tests the object's type
        contains(cs, "public override bool Contains(object o) => items.ContainsObject(o);");
        contains(cs, "public override bool Remove(object o) => items.RemoveObject(o);");
        contains(cs, "if (!m.ContainsKeyObject(k)) { return null; }");
        contains(cs, "m.RemoveKeyObject(k);");
        contains(cs, "return m.GetObject(k);");
        contains(cs, "=> m.RemoveKeyObject(k);");
        // a Comparator is a Comparison delegate, the sorted collections take an IComparer
        contains(cs, "new SortedSet<string>(Comparer<string>.Create(c))");
        contains(cs, "new SortedDictionary<string, int>(Comparer<string>.Create(");
        contains(cs, "JavaComparator.ReverseOrder<string>()));");
    }

    @Language("java")
    private static final String TEMPORALS = """
            package org.example.temporals;
            import java.time.*;
            import java.time.temporal.*;
            public class Temporals<T extends Temporal & Comparable<? super T>> {
                private final T from;
                private final TemporalUnit unit;
                Temporals(T from, T to, TemporalUnit unit) {
                    this.from = from;
                    this.unit = unit;
                    if (!from.isSupported(unit)) throw new IllegalArgumentException("unit " + unit);
                    long space = from.until(to, unit);
                    Temporal expected = from.plus(space, unit);
                    try {
                        expected = from.plus(space + 1, unit);
                    } catch (DateTimeException e) {
                        throw new IllegalArgumentException(e);
                    }
                }
                @SuppressWarnings("unchecked")
                T get(long index) { return (T) from.plus(index, unit); }
                static Temporals<LocalDate> days(LocalDate from, LocalDate to) {
                    return new Temporals<>(from, to, ChronoUnit.DAYS);
                }
                static Period total(Period a, Period b) { return a.plus(b).minus(Period.ZERO); }
                static LocalDate later(LocalDate d, Period p) { return d.plus(p); }
                static long weeks(LocalDate a, LocalDate b) { return ChronoUnit.WEEKS.between(a, b); }
            }
            """;

    /** java.time.temporal: Temporal is IComparable, which JavaTemporal's Plus and Until take; a Period is JavaPeriod. */
    @Test
    public void temporals() {
        String cs = translate("Temporals", TEMPORALS);
        contains(cs, "where T : IComparable");
        contains(cs, "private readonly JavaChronoUnit? unit;");
        contains(cs, "if (!JavaTemporal.IsSupported(from, (JavaChronoUnit) unit))");
        contains(cs, "long space = JavaTemporal.Until(from, to, (JavaChronoUnit) unit);");
        contains(cs, "IComparable expected = JavaTemporal.Plus(from, space, (JavaChronoUnit) unit);");
        contains(cs, "catch (ArgumentOutOfRangeException e)");
        contains(cs, "(T) JavaTemporal.Plus(from, index, (JavaChronoUnit) unit)");
        // a T created as a struct takes the value type
        contains(cs, "new Temporals<DateOnly>(\n(DateOnly) from,");
        contains(cs, "JavaChronoUnit.Days");
        contains(cs, "((JavaPeriod) a).Plus(b).Minus(JavaPeriod.Zero);");
        contains(cs, "JavaTemporal.Plus(((DateOnly) d), p)");
        contains(cs, "JavaTemporal.Until(a, b, JavaChronoUnit.Weeks)");
    }

    @Language("java")
    private static final String MEMBERS = """
            package org.example.members;
            import java.lang.reflect.Field;
            import java.lang.reflect.Method;
            import java.util.*;
            public class Members {
                static int read(Field f, Object o) throws IllegalAccessException {
                    f.setAccessible(true);
                    return f.isSynthetic() ? 0 : f.getInt(o);
                }
                static void write(Field f, Object o) throws IllegalAccessException { f.setLong(o, 1L); }
                static Method find(Class<?> c) throws NoSuchMethodException { return c.getDeclaredMethod("run"); }
                static Method find(Class<?> c, Class<?> p) throws NoSuchMethodException {
                    return c.getDeclaredMethod("apply", p, int.class);
                }
                static Field field(Class<?> c) throws NoSuchFieldException { return c.getDeclaredField("x"); }
                static String describe(Class<?> c) { return c.isRecord() + " " + c.isMemberClass() + " " + c.getTypeName(); }
                static String trimmed(String s) { return s.stripTrailing() + s.stripLeading(); }
                static String last(List<String> list) {
                    ListIterator<String> it = list.listIterator(list.size());
                    while (it.hasPrevious()) {
                        int at = it.previousIndex();
                        String s = it.previous();
                        if (s.isEmpty()) it.remove();
                        else if (at == 0) it.set(s.trim());
                    }
                    list.addFirst("first");
                    list.removeFirst();
                    return it.hasNext() ? it.next() : null;
                }
                static void sized(ArrayList<String> list) { list.ensureCapacity(10); list.trimToSize(); }
            }
            """;

    /** Reflection's members, a String's strips, a List's SequencedCollection methods and its ListIterator. */
    @Test
    public void members() {
        String cs = translate("Members", MEMBERS);
        contains(cs, "JavaClass.SetAccessible(f, true);");
        contains(cs, "JavaClass.IsSynthetic(f) ? 0 : Convert.ToInt32(f.GetValue(o))");
        contains(cs, "f.SetValue(o, 1L);");
        // Java throws where GetMethod is null: the compatibility library's, which throws too
        contains(cs, "JavaClass.DeclaredMethod(c, \"run\")");
        contains(cs, "JavaClass.DeclaredMethod(c, \"apply\", p, typeof(int))");
        contains(cs, "JavaClass.DeclaredField(c, \"x\")");
        contains(cs, "JavaClass.IsRecord(c) + \" \" + c.IsNested + \" \" + c.ToString()");
        contains(cs, "s.TrimEnd() + s.TrimStart()");
        contains(cs, "IJavaListIterator<string> it = list.Iterator(list.Count);");
        contains(cs, "while (it.HasPrevious())");
        contains(cs, "int at = it.PreviousIndex();");
        contains(cs, "it.Set(s.Trim());");
        contains(cs, "list.Insert(0, \"first\");");
        contains(cs, "list.RemoveAt(0);");
        contains(cs, "list.EnsureCapacity(10);");
        contains(cs, "list.TrimExcess();");
    }

    @Language("java")
    private static final String SEQUENCES = """
            package org.example.sequences;
            import java.math.BigInteger;
            import java.util.*;
            import java.util.function.*;
            import java.util.stream.*;
            public class Sequences {
                static List<String> all(Iterable<String> it) { return StreamSupport.stream(it.spliterator(), false).toList(); }
                static long count(Iterator<String> it, int n) {
                    return StreamSupport.stream(Spliterators.spliterator(it, n, Spliterator.ORDERED), false).count();
                }
                static Iterator<String> iterate(Stream<String> s) { return s.iterator(); }
                static int sum(int[] values) { return IntStream.of(values).sum() + IntStream.of(1, 2).sum(); }
                static List<Double> random(Random r) { return Stream.generate(r::nextDouble).limit(3).toList(); }
                static boolean both(BiPredicate<String, Integer> a, BiPredicate<String, Integer> b) { return a.and(b).test("x", 1); }
                static boolean either(Predicate<String> a, Predicate<String> b) { return a.or(b.negate()).test("x"); }
                static boolean ready(BooleanSupplier s) { return s.getAsBoolean(); }
                static Integer bump(Map<String, Integer> m) { return m.computeIfPresent("k", (k, v) -> v > 1 ? v - 1 : null); }
                static String rename(Map<String, String> m) { return m.computeIfPresent("k", (k, v) -> v + k); }
                static String lower(String s) { return s + Locale.getDefault(); }
                static BigInteger half(BigInteger b) { return b.divide(BigInteger.TWO); }
                static Object first(Object array) {
                    return java.lang.reflect.Array.getLength(array) > 0 ? java.lang.reflect.Array.get(array, 0) : null;
                }
                static boolean primitive(Class<?> c) { return c == Integer.TYPE || c == Boolean.TYPE; }
                static long distinct(String[] a) { return Arrays.stream(Objects.requireNonNull(a)).distinct().count(); }
            }
            """;

    /** Spliterators as sequences, IntStream.of, Stream.generate, the predicates' combinators and smaller members. */
    @Test
    public void sequences() {
        String cs = translate("Sequences", SEQUENCES);
        contains(cs, "it.ToList()");
        contains(cs, "it.AsEnumerable().LongCount()");
        contains(cs, "s.Iterator()");
        contains(cs, "values.Sum() + (new int[]{ 1, 2 }).Sum()");
        contains(cs, "JavaFunctions.Generate(");
        contains(cs, "a.And(b)(\"x\", 1)");
        contains(cs, "a.Or(b.Negate())(\"x\")");
        contains(cs, "s()");
        // a value type's function gives null to remove: the int? variant
        contains(cs, "m.ComputeIfPresentValue(\"k\",");
        contains(cs, "m.ComputeIfPresent(\"k\", (k, v) => v + k)");
        contains(cs, "s + CultureInfo.CurrentCulture");
        contains(cs, "b / ");
        contains(cs, "(((Array) array).Length) > 0 ? (((Array) array).GetValue(0)) : null");
        contains(cs, "c == typeof(int) || c == typeof(bool)");
        // Arrays.stream(a) is a: as a receiver, in parentheses where a is not an atom
        contains(cs, "(a ?? throw new NullReferenceException()).Distinct().LongCount()");
    }

    @Language("java")
    private static final String CONCURRENT = """
            package org.example.concurrent;
            import java.util.*;
            import java.util.concurrent.atomic.*;
            import java.util.concurrent.locks.ReentrantLock;
            import java.util.function.*;
            import java.util.stream.*;
            public class Concurrent {
                static int bump(AtomicInteger i) { return i.updateAndGet(v -> v + 1) + i.getAndUpdate(v -> v * 2); }
                static String swap(AtomicReference<String> r) { return r.updateAndGet(v -> v + "!"); }
                static boolean held(ReentrantLock lock) { return lock.isLocked() && lock.isHeldByCurrentThread(); }
                static int below(BitSet bits, int i) { return bits.previousClearBit(i) + bits.previousSetBit(i); }
                static int apply(IntUnaryOperator op, IntBinaryOperator add) { return add.applyAsInt(op.applyAsInt(1), 2); }
                static Set<String> ordered(Stream<String> s) { return s.collect(Collectors.toCollection(LinkedHashSet::new)); }
                static Map<String, Integer> lengths(List<String> l) {
                    return l.stream().collect(Collectors.toMap(x -> x, String::length, Integer::sum));
                }
                static Map<String, String> firsts(List<String> l) {
                    return l.stream().collect(Collectors.toMap(x -> x.substring(0, 1), x -> x, (a, b) -> a, LinkedHashMap::new));
                }
            }
            """;

    /** Atomic updates, a lock's queries, a BitSet's previous bits, primitive operators, toMap with a merge. */
    @Test
    public void concurrent() {
        String cs = translate("Concurrent", CONCURRENT);
        contains(cs, "i.UpdateAndGet(v => v + 1) + i.GetAndUpdate(v => v * 2)");
        contains(cs, "r.UpdateAndGet(v => v + \"!\")");
        contains(cs, "@lock.IsLocked() && @lock.IsHeldByCurrentThread()");
        contains(cs, "bits.PreviousClearBit(i) + bits.PreviousSetBit(i)");
        contains(cs, "Func<int, int> op, Func<int, int, int> add) => add(op(1), 2)");
        contains(cs, "s.ToCollection(");
        contains(cs, "l.ToMap(x => x, ");
        // the supplier's type arguments are the result's
        contains(cs, "=> new OrderedDictionary<string, string>());");
        contains(cs, "s.ToCollection(() => new HashSet<string>())");
    }

    @Language("java")
    private static final String SMALLER = """
            package org.example.smaller;
            import java.math.*;
            import java.util.*;
            import java.util.stream.*;
            public class Smaller {
                static String indented(String s) { return s.indent(4); }
                static String trimmed(StringBuilder b) { return b.replace(b.length() - 2, b.length(), "").toString(); }
                static String letters(Random random) {
                    StringBuilder value = new StringBuilder();
                    random.ints(97, 122).limit(4).forEach(value::appendCodePoint);
                    return value.toString();
                }
                static void doubled(List<Integer> l, Map<String, Integer> m) { l.replaceAll(x -> x * 2); m.replaceAll((k, v) -> v + 1); }
                static Integer last(TreeSet<Integer> s) { return s.removeLast(); }
                static int below(TreeSet<Integer> s) { return s.subSet(1, 5).size(); }
                static int down(TreeSet<Integer> s) { int t = 0; for (int i : s.descendingSet()) t = t * 10 + i; return t; }
                static NavigableSet<String> keys(TreeMap<String, Integer> m) { return m.navigableKeySet(); }
                static boolean finite(double d) { return Double.isFinite(d); }
                static BigDecimal rest(BigDecimal a, BigDecimal b) { return a.remainder(b); }
                static BigDecimal half(BigDecimal a) { return a.divide(BigDecimal.TWO, RoundingMode.HALF_EVEN); }
                static BigInteger larger(BigInteger a, BigInteger b) { return a.max(b); }
                static long total(List<String> l) { return l.stream().mapToLong(String::length).sum(); }
                static int[] sortedSquares(int[] a) { return IntStream.of(a).map(x -> x * x).sorted().toArray(); }
                static Comparator<String> order(TreeMap<String, Integer> m) { return m.comparator(); }
                static BigDecimal exact(BigInteger i) { return new BigDecimal(i); }
            }
            """;

    /** Smaller JDK members: String.indent, a StringBuilder's replace, replaceAll, a sorted set's ends and ranges. */
    @Test
    public void smaller() {
        String cs = translate("Smaller", SMALLER);
        contains(cs, "JavaString.Indent(s, 4)");
        contains(cs, "b.ReplaceRange(b.Length - 2, b.Length, \"\")");
        contains(cs, "random.Ints(97, 122).Take((int) 4)");
        contains(cs, "l.ReplaceAll(x => x * 2);");
        contains(cs, "m.ReplaceAll((k, v) => v + 1);");
        contains(cs, "s.RemoveLast()");
        contains(cs, "s.SubSet(1, 5).Count");
        contains(cs, "s.Reverse()");
        contains(cs, "m.NavigableKeySet()");
        contains(cs, "double.IsFinite(d)");
        contains(cs, "((decimal) a) % ((decimal) b)");
        contains(cs, "Math.Round(((decimal) a) / 2m, ((decimal) a).Scale,\nMidpointRounding.ToEven)");
        contains(cs, "BigInteger.Max(a, b)");
        contains(cs, "a.Select(x => x * x).Order().ToArray()");
        // a Comparator is a Comparison: the comparer's method group
        contains(cs, "new Comparison<string>(m.Comparer\n.Compare)");
        contains(cs, "(decimal) i");
    }

    @Language("java")
    private static final String EXECUTORS = """
            package org.example.executors;
            import java.util.List;
            import java.util.concurrent.*;
            public class Jobs {
                static final class Named implements ThreadFactory {
                    public Thread newThread(Runnable r) {
                        return Thread.ofPlatform().name("job").priority(Thread.NORM_PRIORITY).daemon(false).unstarted(r);
                    }
                }
                private final ExecutorService pool = Executors.newFixedThreadPool(2, new Named());
                private final ExecutorService single = Executors.newSingleThreadExecutor();
                Future<String> run(Callable<String> job) { return pool.submit(job); }
                CompletableFuture<Void> change(List<CompletableFuture<Void>> pending) {
                    var future = new CompletableFuture<Void>();
                    pending.forEach(f -> f.cancel(false));
                    future.complete(null);
                    return future;
                }
                void fail(CompletableFuture<Void> future, Exception e) { future.completeExceptionally(e); }
                CompletableFuture<Void> consume(Runnable r) {
                    return CompletableFuture.runAsync(r, single).whenComplete((unused, throwable) -> System.out.println(throwable));
                }
                boolean stop(CountDownLatch latch) throws InterruptedException {
                    pool.shutdownNow();
                    single.shutdown();
                    return latch.await(100, TimeUnit.MILLISECONDS) && single.awaitTermination(1, TimeUnit.SECONDS);
                }
                boolean interrupted() { return Thread.currentThread().isInterrupted(); }
            }
            """;

    /**
     * A CompletableFuture is a Task, completed and cancelled from outside when it was made empty; an ExecutorService
     * a TaskScheduler, the executors' own shut down; a ThreadFactory, Thread's builder, TimeUnit, a thread's interrupt.
     */
    @Test
    public void executors() {
        String cs = translate("Jobs", EXECUTORS);
        contains(cs, "internal sealed class Named : IJavaThreadFactory {");
        contains(cs, "public Thread NewThread(Action r) => JavaThreads.OfPlatform().Name(\"job\").Priority(5).Daemon(false).Unstarted(r);");
        contains(cs, "private readonly TaskScheduler pool = new JavaExecutorService(2, new Named());");
        contains(cs, "private readonly TaskScheduler single = new JavaExecutorService(1);");
        contains(cs, "pool.Submit(job)");
        contains(cs, "var future = JavaTasks.Incomplete<object>();");
        contains(cs, "f.Cancel(false)");
        contains(cs, "future.Complete(default);");
        contains(cs, "future.CompleteExceptionally(e);");
        contains(cs, "JavaTasks.RunAsync(r, single).WhenComplete(\n(unused, throwable) =>");
        contains(cs, "pool.ShutdownNow();");
        contains(cs, "single.Shutdown();");
        contains(cs, "latch.Wait(JavaTimeUnit.Milliseconds.ToTimeSpan(100)) && single.AwaitTermination(1, JavaTimeUnit.Seconds)");
        contains(cs, "JavaThreads.IsInterrupted(Thread.CurrentThread)");
    }

    @Language("java")
    private static final String DELEGATE_CLASSES = """
            package org.example.delegates;
            import java.util.Comparator;
            import java.util.List;
            import java.util.function.Supplier;
            class ByLength implements Comparator<String> {
                public int compare(String a, String b) { return Integer.compare(a.length(), b.length()); }
            }
            class Fixed implements Supplier<String> {
                public String get() { return "x"; }
            }
            class Sorting {
                static void sort(List<String> l) { l.sort(new ByLength()); }
                static Comparator<String> by() { return new ByLength(); }
                static int direct(ByLength c) { return c.compare("a", "bb"); }
                static String value() {
                    Supplier<String> s = new Fixed();
                    return s.get();
                }
            }
            """;

    /**
     * A class that implements a functional interface C# has as a delegate does not derive from the delegate, a sealed
     * type; where the interface is expected, its instance is the method group.
     */
    @Test
    public void delegateClasses() {
        String cs = translate("Sorting", DELEGATE_CLASSES);
        contains(cs, "internal sealed class ByLength {");
        contains(cs, "internal sealed class Fixed {");
        contains(cs, "JavaCollections.Sort(l, new ByLength().Compare);");
        contains(cs, "internal static Comparison<string> By() => new ByLength().Compare;");
        contains(cs, "c.Compare(\"a\", \"bb\")");
        contains(cs, "Func<string> s = new Fixed().Get;");
    }
}
