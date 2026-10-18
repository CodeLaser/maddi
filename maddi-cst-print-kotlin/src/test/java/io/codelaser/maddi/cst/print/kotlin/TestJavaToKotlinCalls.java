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

package io.codelaser.maddi.cst.print.kotlin;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Calls and types that Kotlin writes differently: varargs given an array, raw types, type arguments a constructor
 * call cannot take, JDK members with another Kotlin name, char labels in a {@code when} over an int.
 */
public class TestJavaToKotlinCalls extends CommonJavaToKotlin {

    private static void contains(String kotlin, String expected) {
        assertTrue(strip(kotlin).contains(strip(expected)), () -> "expected\n" + expected + "\nin\n" + kotlin);
    }

    private static String strip(String s) {
        return s.lines().map(String::strip).collect(Collectors.joining("\n"));
    }

    @Language("java")
    private static final String VARARGS = """
            package a;
            import java.util.*;
            class C {
                static boolean any(String s, String... names) { return names.length > 0; }
                void m(Queue<String> queue, String[] names, String line) {
                    Collections.addAll(queue, names);
                    List<String> list = Arrays.asList(names);
                    List<String> words = new ArrayList<>(Arrays.asList(line.split("\\\\s+")));
                    boolean b = any("x", names) || any("y", "a", "b") || any("z");
                }
            }
            """;

    /** An array passed where Java takes varargs is spread; {@code String.split} takes a regex and returns an array. */
    @Test
    public void varargs() {
        String kotlin = kotlin(VARARGS);
        contains(kotlin, "Collections.addAll(queue, *names)");
        contains(kotlin, "Arrays.asList(*names)");
        contains(kotlin, "Arrays.asList(*line.split(\"\\\\s+\".toRegex())");
        contains(kotlin, ".dropLastWhile { it.isEmpty() }.toTypedArray()");
        contains(kotlin, "any(\"x\", *names) || any(\"y\", \"a\", \"b\") || any(\"z\")");
    }

    @Language("java")
    private static final String TYPES = """
            package a;
            import java.util.*;
            class C<E> {
                static class Box<T> { }
                private final int[][] next = new int[3][];
                private final Box[] boxes = new Box[2];
                private static final Map<String, Integer> CACHE = Collections.synchronizedMap(new HashMap<>());
                boolean m(Object o, String name, List<? extends Number> numbers) throws Exception {
                    Class cls = Class.forName(name);
                    List<? extends Number> copy = new ArrayList<>(numbers);
                    Set<String> set = new HashSet<>(List.of("a"));
                    return o instanceof Box && cls != null;
                }
            }
            """;

    /** Raw types get {@code <*>}; a constructor call takes no projections, nor the type's own parameters. */
    @Test
    public void types() {
        String kotlin = kotlin(TYPES);
        contains(kotlin, "arrayOfNulls<IntArray>(3)");
        contains(kotlin, "arrayOfNulls<Box<*>>(2)");
        contains(kotlin, "Collections.synchronizedMap(HashMap())");
        contains(kotlin, "val cls: Class<*> = ");
        contains(kotlin, "ArrayList(numbers)");
        contains(kotlin, "o is Box<*>");
    }

    @Language("java")
    private static final String LIBRARY = """
            package a;
            import java.util.*;
            import java.util.stream.*;
            class C {
                private final Object lock = new Object();
                String m(List<String> list, Map<String, Integer> map, long val, Boolean flag, String s) {
                    list.sort(Comparator.naturalOrder());
                    synchronized (lock) { lock.notifyAll(); }
                    String joined = map.entrySet().stream().map(Map.Entry::getValue).map(String::valueOf)
                            .collect(Collectors.joining(" "));
                    long count = list.stream().map(x -> flag).filter(Boolean::booleanValue).count();
                    long total = Stream.of(list).flatMap(Collection::stream).count();
                    if (val == -1 || val != 2) return s.concat("x");
                    return "%s and %s".formatted(joined, count);
                }
                String n(String format, Object... args) {
                    return format.formatted(args);
                }
            }
            """;

    /** JDK members Kotlin hides or renames. */
    @Test
    public void library() {
        String kotlin = kotlin(LIBRARY);
        contains(kotlin, "list.sortWith(Comparator.naturalOrder())");
        contains(kotlin, "(lock as java.lang.Object).notifyAll()");
        contains(kotlin, ".map({ it.value })");
        contains(kotlin, ".filter({ it })");
        contains(kotlin, ".flatMap({ it.stream() })");
        contains(kotlin, "`val` == -1L || `val` != 2L");
        contains(kotlin, "s.plus(\"x\")");
        contains(kotlin, "\"%s and %s\".format(joined, count)");
        contains(kotlin, "format.format(*args)");
    }

    @Language("java")
    private static final String CHAR_LABELS = """
            package a;
            class C {
                String m(int tag, char c) {
                    switch (tag) {
                        case 'e': return "enum";
                        case 'c': return "class";
                    }
                    return switch (c) { case 66 -> "B"; default -> "?"; };
                }
            }
            """;

    /** Java compares a char with an int by its code; Kotlin compares types first. */
    @Test
    public void charLabels() {
        String kotlin = kotlin(CHAR_LABELS);
        contains(kotlin, "'e'.code -> { return \"enum\" }");
        contains(kotlin, "when (c.code) {\n66 -> \"B\"");
    }

    @Language("java")
    private static final String MOCKITO_MATCHERS = """
            package org.mockito;
            class ArgumentMatchers {
                static <T> T any() { return null; }
                static int anyInt() { return 0; }
            }
            class UseMatchers {
                interface Listener { void onRequest(String request, int n); }
                void verify(Listener listener) { listener.onRequest(ArgumentMatchers.any(), ArgumentMatchers.anyInt()); }
            }
            """;

    /**
     * A Mockito matcher returns null into what Kotlin declares non-null, and Kotlin checks the platform value: "any(...)
     * must not be null". The file's private helper casts it, unchecked; a primitive matcher's 0 needs nothing.
     */
    @Test
    public void mockitoMatchers() {
        String kotlin = kotlin(MOCKITO_MATCHERS);
        contains(kotlin, "listener.onRequest(mockitoMatched(ArgumentMatchers.any()), ArgumentMatchers.anyInt())");
        contains(kotlin, "private fun <T> mockitoMatched(value: T?): T = value as T");
    }

    @Language("java")
    private static final String COLLECTION_BESIDE_OBJECT = """
            package a;
            import java.util.*;
            class T {
                String apply(Object value) { return apply(Collections.singletonMap("it", value)); }
                String apply(Map<String, Object> variables) { return variables.toString(); }
                String use() { return apply(Collections.singletonMap("name", "Klaus")); }
            }
            """;

    /** Java's Map overload beside apply(Object): Kotlin's invariant MutableMap would lose to Any without the cast. */
    @Test
    public void collectionBesideObject() {
        String kotlin = kotlin(COLLECTION_BESIDE_OBJECT);
        contains(kotlin, "apply((Collections.singletonMap(\"name\", \"Klaus\") as MutableMap<String, Any>))");
    }

    @Language("java")
    private static final String SMALL_GAPS = """
            package a;
            import java.util.*;
            import java.util.concurrent.CompletableFuture;
            import java.util.stream.*;
            class G {
                static class Failure { boolean retry; boolean retry() { return retry; } }
                CompletableFuture<Void> all(List<CompletableFuture<String>> fs) {
                    return CompletableFuture.allOf(fs.toArray(new CompletableFuture[0]));
                }
                List<String> orEmpty(List<String> l) { return Optional.ofNullable(l).orElseGet(List::of); }
                static <T> T getOrDefault(T value, T defaultValue) { return value != null ? value : defaultValue; }
                static <T> T getOrDefault(T value, java.util.function.Supplier<T> s) { return value != null ? value : s.get(); }
                static class D { }
                static int pick(String text) { return 1; }
                static int pick(Integer number) { return 2; }
                static int none() { return pick((String) null); }
                D orNew(D d) { return getOrDefault(d, D::new); }
                List<String> listed(String... ss) { return Optional.ofNullable(ss).map(List::of).orElse(null); }
                void byScore(List<String> fused, Map<String, Double> scores) {
                    fused.sort(Comparator.comparingDouble(scores::get).reversed());
                }
                boolean anyRetry(List<Failure> fs) { return fs.stream().anyMatch(Failure::retry); }
                @SuppressWarnings("unchecked")
                boolean greater(Object actual, Object expected) { return ((Comparable) actual).compareTo(expected) > 0; }
            }
            """;

    @Language("java")
    private static final String RESULT_ONLY_TYPE_PARAMETERS = """
            package a;
            import java.util.*;
            interface R {
                class Failure { String message() { return ""; } }
                <F extends Failure> List<F> failures();
                default int count() { return failures().size(); }
                default String first() { List<Failure> fs = failures(); return fs.get(0).message(); }
                default void put(Map<String, Object> m) { m.put("items", Collections.emptyMap()); }
                default Object images(Map<String, Object> m) { return m.getOrDefault("images", List.of()); }
                default List<String> none() { return List.of(); }
            }
            """;

    /**
     * Type parameters only the result mentions: Kotlin needs the type arguments where Java takes the bound, or where
     * nothing is expected.
     */
    @Test
    public void resultOnlyTypeParameters() {
        String kotlin = kotlin(RESULT_ONLY_TYPE_PARAMETERS);
        contains(kotlin, "failures<Failure>().size");
        contains(kotlin, "val fs: MutableList<Failure> = failures<Failure>()");
        contains(kotlin, "Collections.emptyMap<Any, Any>())");
        contains(kotlin, "java.util.List.of<Any>())");
        // the expected type fixes E
        contains(kotlin, "= java.util.List.of()");
    }

    /** toArray(T[]), a static reference into a mapped type, a property-function clash, a raw Comparable. */
    @Test
    public void smallGaps() {
        String kotlin = kotlin(SMALL_GAPS);
        contains(kotlin, ".toTypedArray<CompletableFuture<*>>())");
        contains(kotlin, "{ java.util.List.of() })");
        contains(kotlin, "{ java.util.List.of(*it) })");
        contains(kotlin, "getOrDefault(d, java.util.function.Supplier<D>(::D))");
        // into a non-null parameter the cast null stays as it was
        contains(kotlin, "pick(null as String)");
        contains(kotlin, "Comparator.comparingDouble<Any>( { scores.get(it)!! }).reversed()");
        contains(kotlin, "anyMatch({ it.retry() })");
        contains(kotlin, "(actual as Comparable<Any?>).compareTo(expected)");
    }

    @Language("java")
    private static final String PROMOTIONS = """
            package a;
            import java.util.concurrent.Callable;
            class G {
                static <T> T withRetry(Callable<T> action, int n) throws Exception { return action.call(); }
                static void withRetry(Runnable action, int n) { action.run(); }
                static <T> T mapped(Callable<T> action) throws Exception { return withRetry(() -> action.call(), 1); }
                static boolean ascii(char ch) { return ch < 128; }
                static int low(byte b) { return b & 0xFF; }
                static long wide(int i, long mask) { return i & mask; }
                static int shifted(byte b, long d) { return b >>> d; }
                static Throwable unwrap(Exception e) { if (e.getCause() != null) return e.getCause(); else return e; }
                static Class<?>[] types() { return new Class<?>[] { int.class, Integer.class }; }
            }
            """;

    /**
     * Java promotes a char to int in a comparison, a byte to int (an int to long) in a bit operation; a lambda beside an
     * overload taking another interface names Java's; Kotlin's Throwable.cause is an open Throwable?, never smart-cast.
     */
    @Test
    public void promotions() {
        String kotlin = kotlin(PROMOTIONS);
        contains(kotlin, "withRetry(Callable<T> { action.call() }, 1)");
        contains(kotlin, "ch.code < 128");
        contains(kotlin, "b.toInt() and 255");
        contains(kotlin, "i.toLong() and mask");
        contains(kotlin, "b.toInt() ushr d.toInt()");
        contains(kotlin, "return e.cause!!");
        contains(kotlin, "Int::class.javaPrimitiveType!!,\nInt::class.javaObjectType)");
    }

    @Language("java")
    private static final String COVARIANT_ARRAYS = """
            package a;
            class U {
                static boolean empty(Object[] a) { return a == null || a.length == 0; }
                static boolean any(String... xs) { return !empty(xs); }
                static void fill(Object[] a) { a[0] = "x"; }
                static <T> T[] check(T[] a) { return a; }
                @SafeVarargs static <T> T first(T... xs) { check(xs); return xs[0]; }
                @java.lang.annotation.Target(java.lang.annotation.ElementType.TYPE_USE) @interface Nullable { }
                final String join(@Nullable Object[] parts) {
                    java.util.List<?> partsList = java.util.Arrays.<@Nullable Object>asList(parts);
                    return partsList.toString();
                }
                static int count(java.lang.reflect.Type[] types) {
                    int n = 0;
                    for (java.lang.reflect.Type t : types) if (t != null) n++;
                    return n + types.length + types[0].hashCode();
                }
                static String bounds(java.lang.reflect.Type[] upper) { return new U().join(upper) + count(upper); }
            }
            """;

    /** Java's arrays are covariant: an Object[] parameter whose elements the method does not assign is Array<out Any>. */
    @Test
    public void covariantArrays() {
        String kotlin = kotlin(COVARIANT_ARRAYS);
        contains(kotlin, "fun empty(a: Array<out Any>): Boolean");
        contains(kotlin, "fun fill(a: Array<Any>)");
        // a vararg parameter is an Array<out T>: into an invariant Array<T> parameter, cast
        contains(kotlin, "check((xs as Array<T>))");
        // guava's Joiner.join(Object[]) and an array only read, of any element type
        contains(kotlin, "fun join(parts: Array<out Any");
        contains(kotlin, "fun count(types: Array<out java.lang.reflect.Type>): Int");
        contains(kotlin, "fun bounds(upper: Array<out java.lang.reflect.Type>)");
        compiles(kotlin);
    }

    @Language("java")
    private static final String CAST_INTO_TYPE_VARIABLE = """
            package a;
            class E<G> {
                <C extends G> void take(Class<C> c) { }
                @SuppressWarnings("unchecked")
                void fire(G guardrail) { take((Class<G>) guardrail.getClass()); }
            }
            """;

    /** A generic cast keeps its own type arguments where the parameter's are the callee's type variables. */
    @Test
    public void castIntoTypeVariable() {
        String kotlin = kotlin(CAST_INTO_TYPE_VARIABLE);
        contains(kotlin, "as Class<G>");
    }

    @Language("java")
    private static final String WIDENED_ARRAY_ELEMENTS = """
            package a;
            class W {
                static double[] d() { return new double[]{1, 2}; }
                static long[] l(int x) { long[] l = {1, x}; return l; }
                static double[][] dd() { return new double[][]{{1}, {2.5, 3}}; }
                static float[] f(char c) { return new float[]{c, 1}; }
            }
            """;

    /** Java widens an array initializer's elements to the element type, Kotlin does not: doubleArrayOf(1, 2) fails. */
    @Test
    public void widenedArrayElements() {
        String kotlin = kotlin(WIDENED_ARRAY_ELEMENTS);
        contains(kotlin, "doubleArrayOf(1.toDouble(), 2.toDouble())");
        contains(kotlin, "longArrayOf(1, x.toLong())");
        contains(kotlin, "doubleArrayOf(2.5, 3.toDouble())");
        contains(kotlin, "floatArrayOf(c.code.toFloat(), 1.toFloat())");
        compiles(kotlin);
    }

    /** guava's views: {@code c.toArray()} on a Java collection, which Kotlin's collection types do not have. */
    @Test
    public void toArrayWithoutArgument() {
        String kotlin = kotlin("""
                package a;
                import java.util.*;
                class Snap {
                    static Object[] snap(Map<String, Integer> m, List<String> l) {
                        Object[] keys = m.keySet().toArray();
                        return l.isEmpty() ? keys : l.toArray();
                    }
                }
                """);
        contains(kotlin, "m.keys.toTypedArray<Any>()");
        contains(kotlin, "l.toTypedArray<Any>()");
        compiles(kotlin);
    }

    /** guava's escapers: a char used as an array index, which Java widens to int and Kotlin does not. */
    @Test
    public void charIndexIsWidened() {
        String kotlin = kotlin("""
                package a;
                class Esc {
                    static char[] escape(char[][] replacements, char c, byte b) {
                        replacements[c] = new char[0];
                        return c < replacements.length ? replacements[c] : replacements[b];
                    }
                }
                """);
        contains(kotlin, "replacements[c.code] = CharArray(0)");
        contains(kotlin, "replacements[c.code]");
        contains(kotlin, "replacements[b.toInt()]");
        compiles(kotlin);
    }

    /**
     * guava's EnumMultiset, Platform, CycleDetectingLockFactory: Enum.getDeclaringClass() is Kotlin's declaringJavaClass;
     * Strings.repeat, CharEscaper: String.getChars(…) is Java's only.
     */
    @Test
    public void enumDeclaringClassAndGetChars() {
        String kotlin = kotlin("""
                package a;
                class Names {
                    enum Color { RED }
                    static <E extends Enum<E>> Class<E> type(E e) { return e.getDeclaringClass(); }
                    static String rank(Enum<?> rank) { return rank.getDeclaringClass().getSimpleName() + "." + rank.name(); }
                    static char[] chars(String s) {
                        char[] dest = new char[s.length()];
                        s.getChars(0, s.length(), dest, 0);
                        return dest;
                    }
                }
                """);
        contains(kotlin, "e.declaringJavaClass");
        contains(kotlin, "(s as java.lang.String).getChars(");
        compiles(kotlin);
    }

    /**
     * guava's ImmutableRangeMap: {@code SortedLists.binarySearch(ranges, Range::lowerBound, …)} beside an overload taking
     * a T. The SAM constructor that keeps Java's overload carries the reference's type arguments, else Kotlin infers
     * none for the lambda inside.
     */
    @Test
    public void samConstructorTyped() {
        String kotlin = kotlin("""
                package a;
                import java.util.*;
                interface Fn<F, T> { T apply(F input); }
                class Range<C> { C lower; C lowerBound() { return lower; } }
                class Search {
                    static <E, K> int search(List<? extends E> list, Fn<? super E, K> key, K target) { return 0; }
                    static <E> int search(List<? extends E> list, E key, E target) { return 1; }
                    static <C> int find(List<Range<C>> ranges, C c) { return search(ranges, Range::lowerBound, c); }
                }
                """);
        contains(kotlin, "Fn<Range<C>, C>(");
        compiles(kotlin);
    }

    /**
     * guava's BloomFilter.Strategy declares {@code int ordinal()}, which the BloomFilterStrategies enum implements by
     * java.lang.Enum's: a property in Kotlin, which Kotlin's Enum implements, and read as one.
     */
    @Test
    public void interfaceOrdinalImplementedByAnEnum() {
        String kotlin = kotlin("""
                package a;
                class Bloom {
                    interface Strategy { int ordinal(); String name(); }
                    enum Strategies implements Strategy { MURMUR, OTHER }
                    static final class Custom implements Strategy {
                        @Override public int ordinal() { return -1; }
                        @Override public String name() { return "custom"; }
                    }
                    static int write(Strategy strategy) { return strategy.ordinal() + strategy.name().length(); }
                }
                """);
        contains(kotlin, "val ordinal: Int");
        contains(kotlin, "strategy.ordinal + strategy.name.length");
        compiles(kotlin);
    }

    /**
     * {@code get(type)} of a translated Forwarding<K, V> superclass, into a B: the unchecked cast is to B, as the
     * superclass's V is out of scope.
     */
    @Test
    public void inheritedTypeVariableResultCastToTheTarget() {
        String kotlin = kotlin("""
                package a;
                import java.util.*;
                abstract class Forwarding<K, V> extends AbstractMap<K, V> {
                    final Map<K, V> delegate = new HashMap<>();
                    @Override public V get(Object key) { return delegate.get(key); }
                    @Override public Set<Entry<K, V>> entrySet() { return delegate.entrySet(); }
                }
                class Instances<B> extends Forwarding<Class<? extends B>, B> {
                    B instance(Class<?> type) { return get(type); }
                }
                """);
        contains(kotlin, "(get(type) as B)");
        compiles(kotlin);
    }

    /**
     * guava's MultimapBuilder.enumKeys: {@code new EnumMap<K0, Collection<V>>(keyClass)} in the createMap of the
     * anonymous class it returns, K0 the enclosing method's: the type arguments stay, or Kotlin infers no V.
     */
    @Test
    public void enclosingMethodsTypeArgumentsOfAConstructorCall() {
        String kotlin = kotlin("""
                package a;
                import java.util.*;
                class Builder {
                    interface Factory { <V> Map<?, Collection<V>> create(); }
                    static <K0 extends Enum<K0>> Factory enumKeys(Class<K0> keyClass) {
                        return new Factory() {
                            @Override
                            public <V> Map<?, Collection<V>> create() { return new EnumMap<K0, Collection<V>>(keyClass); }
                        };
                    }
                }
                """);
        contains(kotlin, "EnumMap<K0, MutableCollection<V>>(keyClass)");
        compiles(kotlin);
    }

    /**
     * guava's Equivalence.wrap: {@code new Wrapper<S>(this, reference)} for an {@code S extends @Nullable T}, into
     * Wrapper's {@code Equivalence<? super @NonNull T>}: an {@code Equivalence<in S & Any>}, which an Equivalence<T>
     * is.
     */
    @Test
    public void nonNullWildcardBoundOfAConstructorParameter() {
        String kotlin = kotlinInWorld("""
                package a;
                import org.jspecify.annotations.NonNull;
                import org.jspecify.annotations.Nullable;
                abstract class Equivalence<T> {
                    final <S extends @Nullable T> Wrapper<S> wrap(S reference) { return new Wrapper<S>(this, reference); }
                    static final class Wrapper<T extends @Nullable Object> {
                        private final Equivalence<? super @NonNull T> equivalence;
                        private final T reference;
                        private Wrapper(Equivalence<? super @NonNull T> equivalence, T reference) {
                            this.equivalence = equivalence;
                            this.reference = reference;
                        }
                    }
                }
                """, KotlinPrintOptions.DEFAULT, t -> { });
        contains(kotlin, "Equivalence<in T & Any>");
    }

    /**
     * guava's MutableClassToInstanceMap extends ForwardingMap<Class<? extends @NonNull B>, B>: {@code out B}, as its
     * overrides' parameters say, or the header's MutableMap<K, V> takes two different K.
     */
    @Test
    public void nonNullExtendsWildcardStaysOutB() {
        String kotlin = kotlinInWorld("""
                package a;
                import java.util.*;
                import org.jspecify.annotations.NonNull;
                import org.jspecify.annotations.Nullable;
                class Instances<B extends @Nullable Object> extends HashMap<Class<? extends @NonNull B>, B> {
                    @Override public B put(Class<? extends @NonNull B> key, B value) { return super.put(key, value); }
                }
                """, KotlinPrintOptions.DEFAULT, t -> { });
        contains(kotlin, "HashMap<Class<out B>, B>()");
    }

    /**
     * guava's Ordering.ARBITRARY_ORDERING: a field of {@code Ordering<@Nullable Object>}, which a method returns as
     * one; the field's declared type argument is an Any? too.
     */
    @Test
    public void fieldKeepsDeclaredNullableArguments() {
        String kotlin = kotlinInWorld("""
                package a;
                import java.util.*;
                import org.jspecify.annotations.Nullable;
                class Holder {
                    static final List<@Nullable Object> ALL = new ArrayList<>();
                    static List<@Nullable Object> all() { return ALL; }
                }
                """, KotlinPrintOptions.DEFAULT, t -> { });
        contains(kotlin, "ALL: MutableList<Any?>");
    }

    /**
     * guava's ImmediateFuture.NULL: {@code new ImmediateFuture<@Nullable Object>(null)} in a static field, where the
     * class's V is out of scope: the null goes into the call's own type argument.
     */
    @Test
    public void nullIntoAnExplicitlyNullableTypeArgument() {
        String kotlin = kotlinInWorld("""
                package a;
                import org.jspecify.annotations.Nullable;
                class Immediate<V extends @Nullable Object> {
                    static final Immediate<?> NULL = new Immediate<@Nullable Object>(null);
                    final V value;
                    Immediate(V value) { this.value = value; }
                }
                """, KotlinPrintOptions.DEFAULT, t -> { });
        contains(kotlin, "Immediate<Any?>(null)");
    }

    /**
     * guava's CollectCollectors.toImmutableMap: {@code toMap(keyFunction, valueFunction, mergeFunction,
     * LinkedHashMap::new)} handed to a copy of a {@code Map<? extends K, ? extends V>}: the map constructed is a
     * LinkedHashMap<K, V>.
     */
    @Test
    public void constructorReferenceBesideCapturedKeys() {
        String kotlin = kotlin("""
                package a;
                import java.util.*;
                import java.util.function.*;
                import java.util.stream.*;
                class Collect {
                    static <T, K, V> Collector<T, ?, Map<K, V>> toLinkedMap(Function<? super T, ? extends K> keyFunction,
                            Function<? super T, ? extends V> valueFunction, BinaryOperator<V> mergeFunction) {
                        return Collectors.collectingAndThen(
                                Collectors.toMap(keyFunction, valueFunction, mergeFunction, LinkedHashMap::new), Collect::copy);
                    }
                    static <K, V> Map<K, V> copy(Map<? extends K, ? extends V> map) { return new HashMap<>(map); }
                }
                """);
        contains(kotlin, "Supplier<LinkedHashMap<K, V>>(::LinkedHashMap)");
        compiles(kotlin);
    }

    /**
     * guava's CollectCollectors.TO_IMMUTABLE_ENUM_SET: {@code (Collector) toImmutableEnumSetGeneric()}, a raw cast of a
     * call whose E extends Enum<E> only the target would give: Nothing, which satisfies the bound.
     */
    @Test
    @SuppressWarnings("rawtypes")
    public void rawCastOfACallWithASelfBoundedResult() {
        String kotlin = kotlin("""
                package a;
                import java.util.*;
                class Collect {
                    @SuppressWarnings({"rawtypes", "unchecked"})
                    static final List<Enum<?>> ALL = (List) generic();
                    static <E extends Enum<E>> List<E> generic() { return new ArrayList<>(); }
                }
                """);
        contains(kotlin, "generic<Nothing>()");
        compiles(kotlin);
    }

    /**
     * guava's Streams.findLast: {@code return java.util.Optional.empty();} into an Optional<T> whose T may be null: cast,
     * and the operand of the cast has no expected type to infer empty's T from.
     */
    @Test
    public void optionalEmptyIntoANullableTypeVariable() {
        String kotlin = kotlin("""
                package a;
                import java.util.Optional;
                import java.util.stream.Stream;
                class Streams {
                    static <T> Optional<T> findLast(Stream<T> stream) {
                        return Optional.empty();
                    }
                }
                """);
        compiles(kotlin);
    }

    /**
     * guava's FluentIterable.first(): {@code Optional<@NonNull E>} of {@code Optional.of(iterator.next())}, guava's own
     * Optional, whose of throws on null as java.util's: asserted, so the result is an Optional<E & Any>.
     */
    @Test
    public void guavaOptionalOfATypeVariable() {
        String kotlin = kotlinInWorld("""
                package com.google.common.base;
                import java.util.*;
                import org.jspecify.annotations.NonNull;
                abstract class Optional<T> {
                    static <T> Optional<T> of(T reference) { return new Present<>(Objects.requireNonNull(reference)); }
                    static <T> Optional<T> absent() { return null; }
                    static final class Present<T> extends Optional<T> { final T t; Present(T t) { this.t = t; } }
                }
                class Fluent<E> {
                    final Iterable<E> delegate = new ArrayList<>();
                    Optional<@NonNull E> first() {
                        Iterator<E> iterator = delegate.iterator();
                        return iterator.hasNext() ? Optional.of(iterator.next()) : Optional.absent();
                    }
                }
                """, KotlinPrintOptions.DEFAULT, t -> { });
        contains(kotlin, "Optional.of(iterator.next()!!)");
    }

    /**
     * guava's Range.compareOrThrow(Comparable left, Comparable right) and RegularContiguousSet<C extends Comparable>:
     * compareTo on a raw Comparable, which Kotlin's Comparable<*> refuses: an unchecked Comparable<Any?>.
     */
    @Test
    @SuppressWarnings("rawtypes")
    public void compareToOnARawComparable() {
        String kotlin = kotlin("""
                package a;
                class Range<C extends Comparable> {
                    @SuppressWarnings("unchecked")
                    static int compareOrThrow(Comparable left, Comparable right) { return left.compareTo(right); }
                    @SuppressWarnings("unchecked")
                    boolean same(C a, C b) { return a.compareTo(b) == 0; }
                }
                """);
        contains(kotlin, "(a as Comparable<Any?>).compareTo(b)");
        compiles(kotlin);
    }

    /**
     * guava's LittleEndianByteArray: {@code (long) HANDLE.get(array, offset)}, a VarHandle's signature-polymorphic
     * get, whose result Kotlin types Any: the cast stays.
     */
    @Test
    public void castOfASignaturePolymorphicResult() {
        String kotlin = kotlin("""
                package a;
                import java.lang.invoke.*;
                import java.nio.ByteOrder;
                class Bytes {
                    static final VarHandle HANDLE = MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.LITTLE_ENDIAN);
                    static long get(byte[] array, int offset) { return (long) HANDLE.get(array, offset); }
                }
                """);
        contains(kotlin, "(HANDLE.get(array, offset) as Long)");
        compiles(kotlin);
    }

    /**
     * guava's Escaper: {@code private final Function<String, String> asFunction = this::escape;}. Kotlin converts a
     * reference to a Java interface only as an argument: the SAM constructor around it.
     */
    @Test
    public void referenceIntoAFunctionalField() {
        String kotlin = kotlin("""
                package a;
                import java.util.function.Function;
                abstract class Escaper {
                    private final Function<String, String> asFunction = this::escape;
                    abstract String escape(String s);
                    Function<String, String> asFunction() { return asFunction; }
                    static Function<String, Integer> length() { return String::length; }
                }
                """);
        contains(kotlin, "Function<String, String>(this ::escape)");
        compiles(kotlin);
    }

    /**
     * guava's Stats.of(IntStream): {@code values.collect(Acc::new, Acc::add, Acc::addAll)}, an ObjIntConsumer whose
     * int Java widens to add(double); Kotlin's reference does not widen.
     */
    @Test
    public void referenceWhoseParameterJavaWidens() {
        String kotlin = kotlin("""
                package a;
                import java.util.stream.IntStream;
                class Acc {
                    double sum;
                    void add(double v) { sum += v; }
                    void addAll(Acc other) { sum += other.sum; }
                    static double of(IntStream values) { return values.collect(Acc::new, Acc::add, Acc::addAll).sum; }
                }
                """);
        contains(kotlin, "{ p0, p1 -> p0.add(p1.toDouble()) }");
        compiles(kotlin);
    }

    /**
     * guava's ImmutableRangeMap again: the SAM constructor names com.google.common.base.Function, which Java never
     * wrote, in a file that imports java.util.function.Function. The simple name would mean the imported one.
     */
    @Test
    public void samConstructorOfATypeWhoseNameAnImportHolds() {
        String kotlin = kotlin("""
                package a;
                import java.util.*;
                import java.util.function.Function;
                import b.Search;
                class Range<C> { C lower; C lowerBound() { return lower; } }
                class Ranges {
                    static <C> int find(List<Range<C>> ranges, C c) { return Search.search(ranges, Range::lowerBound, c); }
                    static <C> Function<Range<C>, C> lower() { return Range::lowerBound; }
                }
                """, java.util.Map.of("b.Fn", """
                package b;
                public interface Fn<F, T> { T apply(F input); }
                """, "b.Function", """
                package b;
                public interface Function<F, T> extends Fn<F, T> { }
                """, "b.Search", """
                package b;
                import java.util.*;
                public class Search {
                    public static <E, K> int search(List<? extends E> list, Function<? super E, K> key, K target) { return 0; }
                    public static <E> int search(List<? extends E> list, E key, E target) { return 1; }
                }
                """));
        contains(kotlin, "b.Function<Range<C>, C>(");
        contains(kotlin, "import java.util.function.Function");
    }

    /**
     * guava's Graphs.inducedSubgraph: {@code MutableGraph<N> subgraph = c ? b.build() : b.build()}, a builder whose
     * {@code <N1 extends N> build()} only the target types. The local keeps its declared type.
     */
    @Test
    public void conditionalOfTargetTypedCalls() {
        String kotlin = kotlin("""
                package a;
                import java.util.*;
                class Builder<N> {
                    <N1 extends N> List<N1> build() { return new ArrayList<>(); }
                    static <N> List<N> sub(Builder<N> b, boolean big) {
                        List<N> result = big ? b.build() : b.build();
                        return result;
                    }
                }
                """);
        contains(kotlin, "val result: MutableList<N> = ");
        compiles(kotlin);
    }

    /**
     * guava's CollectCollectors and TableCollectors: Java types {@code Collector.of}'s accumulator from a generic
     * reference, a cast to a functional interface, or a cast to the raw Collector; Kotlin infers none of them.
     */
    @Test
    public void collectorOfGenericReferences() {
        String kotlin = kotlin("""
                package a;
                import java.util.*;
                import java.util.function.*;
                import java.util.stream.Collector;
                class Bag<E> {
                    static <E> Bag<E> create() { return new Bag<>(); }
                    static <E> Bag<E> create(int n) { return new Bag<>(); }
                    void add(E e) { }
                    Bag<E> merge(Bag<E> other) { return this; }
                    List<E> list() { return new ArrayList<>(); }
                    static <E> List<E> copyOf(Collection<? extends E> c) { return new ArrayList<>(c); }
                }
                class Acc<R, V> {
                    void put(R r, V v) { }
                    Acc<R, V> combine(Acc<R, V> other) { return this; }
                    Map<R, V> done() { return new HashMap<>(); }
                }
                class Collectors2 {
                    private static final Collector<Object, ?, List<Object>> TO_LIST =
                            Collector.of(ArrayList::new, (List<Object> l, Object o) -> l.add(o), (a, b) -> { a.addAll(b); return a; });
                    @SuppressWarnings("unchecked")
                    static <E> Collector<E, ?, List<E>> toList() { return (Collector) TO_LIST; }
                    static <T, E> Collector<T, ?, List<E>> toBag(Function<? super T, ? extends E> f) {
                        return Collector.of(Bag::create, (bag, t) -> bag.add(f.apply(t)), (b1, b2) -> b1.merge(b2), (Bag<E> b) -> b.list());
                    }
                    static <T, R, V> Collector<T, ?, Map<R, V>> toMap(Function<T, R> r, Function<T, V> v) {
                        return Collector.of(Acc<R, V>::new, (acc, t) -> acc.put(r.apply(t), v.apply(t)),
                                (a1, a2) -> a1.combine(a2), acc -> acc.done());
                    }
                    static <G> List<G> varargs(G... gs) {
                        return Optional.ofNullable(gs).map(List::of).orElse(List.of());
                    }
                    static <T, R, V> Collector<T, ?, Map<R, V>> toMap2(Function<T, R> r, Function<T, V> v) {
                        return Collector.of((Supplier<Acc<R, V>>) Acc::new, (acc, t) -> acc.put(r.apply(t), v.apply(t)),
                                Acc::combine, Acc::done);
                    }
                }
                """);
        contains(kotlin, "TO_LIST as Collector<E, *, MutableList<E>>");
        contains(kotlin, "Supplier<Bag<E>>(");
        contains(kotlin, "Supplier<Acc<R, V>>(");
        compiles(kotlin);
    }

    /**
     * guava's Joiner.join(Object[]) and Types.disallowPrimitiveType(Type[]): with the analysis's unmodified verdicts,
     * an array only read is still Array<out T> (the read-only collection rule left it invariant). Not where an
     * override may take it (Hashing.makeHash), nor where a pass-through hands it back (Lists' checkNotNull(rest)).
     */
    @Test
    public void covariantArraysWithVerdicts() {
        String kotlin = kotlin("""
                package a;
                import java.util.*;
                class J {
                    static <T> T checkNotNull(T t) { if (t == null) throw new NullPointerException(); return t; }
                    final String join(Object[] parts) {
                        List<?> partsList = Arrays.<Object>asList(parts);
                        return partsList.toString();
                    }
                    private static void disallow(java.lang.reflect.Type[] types) {
                        for (java.lang.reflect.Type t : types) if (t instanceof Class<?> c && c.isPrimitive()) throw new IllegalArgumentException();
                    }
                    int makeHash(Object[] hashes) { return hashes.length; }
                    static class Concat extends J {
                        @Override int makeHash(Object[] hashes) { return 1; }
                    }
                    static class Rest<E> {
                        final E[] rest;
                        Rest(E[] rest) { this.rest = checkNotNull(rest); }
                    }
                    static String all(java.lang.reflect.Type[] upper) { disallow(upper); return new J().join(upper); }
                    static int sum(Integer[] xs) { int n = 0; for (Integer x : xs) n += x; xs[0] = n; return n; }
                    private static Runnable captured(Integer[] values) {
                        return new Runnable() { @Override public void run() { sum(values); } };
                    }
                    @SafeVarargs
                    static <E> List<E> construct(E... es) { return Arrays.asList(es); }
                    static <E> List<E> copyOf(E[] elements) { return construct(elements.clone()); }
                    static <E> E[] copied(E[] elements) { return elements.clone(); }
                    static class Wildcard {
                        private final List<java.lang.reflect.Type> upperBounds;
                        Wildcard(java.lang.reflect.Type[] upperBounds) { this.upperBounds = List.of(upperBounds); }
                        int size() { return upperBounds.size(); }
                    }
                    static class Combined {
                        private final Object[] hashers;
                        Combined(Object[] hashers) { this.hashers = hashers; }
                        Object[] hashers() { return hashers; }
                    }
                }
                """, type -> type.methods().forEach(m -> m.parameters().forEach(
                p -> p.analysis().set(io.codelaser.maddi.cst.impl.analysis.PropertyImpl.UNMODIFIED_PARAMETER,
                        io.codelaser.maddi.cst.impl.analysis.ValueImpl.BoolImpl.TRUE))));
        contains(kotlin, "fun join(parts: Array<out Any>)");
        contains(kotlin, "fun disallow(types: Array<out java.lang.reflect.Type>)");
        contains(kotlin, "fun makeHash(hashes: Array<Any>)");
        contains(kotlin, "rest: Array<E>");
        // captured by an anonymous class that hands it on invariant
        contains(kotlin, "fun captured(values: Array<Int>)");
        // a constructor's parameter that a list field of its name copies is only read
        contains(kotlin, "(upperBounds: Array<out java.lang.reflect.Type>)");
        // a clone spread into a vararg is a copy again; a clone returned is the array
        contains(kotlin, "fun <E> copyOf(elements: Array<out E>)");
        contains(kotlin, "fun <E> copied(elements: Array<E>)");
        // a primary constructor's parameter is the property
        assertFalse(kotlin.contains("hashers: Array<out"), kotlin);
        compiles(kotlin);
    }

    /**
     * guava's toArray(T[] a) implementations: {@code Object[] unsoundlyCovariantArray = a} of a T[], and a T[] handed
     * to {@code copyIntoArray(Object[] dst, int offset)}, which writes it: Java's covariance, Kotlin's unchecked cast.
     */
    @Test
    public void typeVariableArraysIntoObjectArrays() {
        String kotlin = kotlin("""
                package a;
                class C {
                    Object[] items = new Object[] { "a" };
                    int copyIntoArray(Object[] dst, int offset) { dst[offset] = items[0]; return offset + 1; }
                    <T> T[] toArray(T[] a) {
                        if (a.length > 1) {
                            Object[] unsoundlyCovariantArray = a;
                            unsoundlyCovariantArray[1] = "b";
                        }
                        copyIntoArray(a, 0);
                        return a;
                    }
                }
                """);
        contains(kotlin, "= (a as Array<Any");
        contains(kotlin, "copyIntoArray((a as Array<Any");
        compiles(kotlin);
    }

    /**
     * guava's primitives and Converter.convertAll: Collection.toArray() is toTypedArray, IntStream.toArray() stays an
     * IntArray; a lambda into an Iterable is an object, as Kotlin's Iterable is no fun interface.
     */
    @Test
    public void toArrayShapesAndIterableLambdas() {
        String kotlin = kotlin("""
                package a;
                import java.util.*;
                import java.util.function.Function;
                import java.util.stream.IntStream;
                class P {
                    @java.lang.annotation.Target(java.lang.annotation.ElementType.TYPE_USE) @interface Nullable { }
                    static int[] toArray(Collection<? extends Number> collection) {
                        Object[] boxedArray = collection.toArray();
                        int[] array = new int[boxedArray.length];
                        for (int i = 0; i < boxedArray.length; i++) array[i] = ((Number) Objects.requireNonNull(boxedArray[i])).intValue();
                        return array;
                    }
                    static int[] copyOf(IntStream stream) {
                        int[] array = stream.toArray();
                        return array;
                    }
                    static <A, B> Iterable<B> convertAll(Iterable<? extends A> from, Function<A, B> f) {
                        return () -> new Iterator<B>() {
                            private final Iterator<? extends A> it = from.iterator();
                            @Override public boolean hasNext() { return it.hasNext(); }
                            @Override public B next() { return f.apply(it.next()); }
                        };
                    }
                }
                """);
        contains(kotlin, "collection.toTypedArray<");
        contains(kotlin, "stream.toArray()");
        contains(kotlin, "object : MutableIterable<B> { override fun iterator(): MutableIterator<B> = run ");
        compiles(kotlin);
    }

    /**
     * Java promotes byte, short and char operands to int: ~s, -c, and compound assignments narrow back (guava's
     * InetAddresses, UnicodeEscaper, UnsignedBytes, Hashing). Kotlin's Short has no inv(), Char no unaryMinus, Byte no xor.
     */
    @Test
    public void narrowOperandsPromote() {
        String kotlin = kotlin("""
                package a;
                class Ops {
                    static int port(short s) { return ~s & 0xffff; }
                    static int neg(char c) { return -c; }
                    static void flip(byte[] array) { for (int i = 0; i < array.length; i++) array[i] ^= Byte.MAX_VALUE; }
                    static void add(byte[] result, byte[] next) { for (int i = 0; i < next.length; i++) result[i] += next[i]; }
                    static short mul(short s) { s *= 3; return s; }
                }
                """);
        contains(kotlin, "s.toInt().inv()");
        contains(kotlin, "Int = - c.code");
        contains(kotlin, "array[i] = (array[i].toInt() xor java.lang.Byte.MAX_VALUE.toInt()).toByte()");
        contains(kotlin, "result[i] = (result[i].toInt() + next[i].toInt()).toByte()");
        contains(kotlin, "s = (s.toInt() * 3).toShort()");
        compiles(kotlin);
    }

    /** guava's Splitter: an anonymous class's method inside a lambda returns for itself, no return@lambda. */
    @Test
    public void anonymousMethodInsideLambdaReturnsItself() {
        String kotlin = kotlin("""
                package a;
                import java.util.function.Supplier;
                class Lab {
                    interface Strategy { int find(String s, int start); }
                    static Supplier<Strategy> on(String separator) {
                        return () -> new Strategy() {
                            @Override public int find(String s, int start) {
                                positions:
                                for (int p = start; p < s.length(); p++) {
                                    for (int i = 0; i < separator.length(); i++) {
                                        if (s.charAt(p + i) != separator.charAt(i)) continue positions;
                                    }
                                    return p;
                                }
                                return -1;
                            }
                        };
                    }
                }
                """);
        assertFalse(kotlin.contains("return@lambda"), kotlin);
        contains(kotlin, "return p");
        compiles(kotlin);
    }

    /**
     * guava's AbstractFuture: {@code @ReflectionSupport(ReflectionSupport.Level.FULL)} beside a static import of
     * java.util.logging.Level.SEVERE, which Kotlin imports as {@code Level}: the nested enum keeps its outer name.
     */
    @Test
    public void nestedTypeBesideImportedNamesake() {
        String kotlin = kotlin("""
                package a;
                import static java.util.logging.Level.SEVERE;
                import java.util.logging.Logger;
                @interface Support { enum Level { NONE, FULL } Level value(); }
                @Support(Support.Level.FULL)
                class X {
                    static void log(Logger logger) { logger.log(SEVERE, "x"); }
                }
                """);
        contains(kotlin, "@Support(Support.Level.FULL)");
        compiles(kotlin);
    }

    /** guava's Enums.StringConverter: Enum.valueOf is java.lang.Enum's; Kotlin's Enum has no valueOf. */
    @Test
    public void javaLangEnumStatic() {
        String kotlin = kotlin("""
                package a;
                class Conv<T extends Enum<T>> {
                    private final Class<T> enumClass;
                    Conv(Class<T> enumClass) { this.enumClass = enumClass; }
                    T forward(String value) { return Enum.valueOf(enumClass, value); }
                }
                """);
        contains(kotlin, "java.lang.Enum.valueOf(enumClass, value)");
        compiles(kotlin);
    }

    /**
     * guava's LocalCache and MapMakerInternalMap: this@LocalCache.get(key) and remove(key, value) with Object
     * arguments call the class's own overrides of Map's lookups, which Kotlin declares with K and V.
     */
    @Test
    public void ownLookupOverridesCastTheirArguments() {
        String kotlin = kotlin("""
                package a;
                import java.util.*;
                class Cache<K, V> extends AbstractMap<K, V> {
                    private final Map<K, V> map = new HashMap<>();
                    @Override public V get(Object key) { return map.get(key); }
                    @Override public boolean remove(Object key, Object value) { return map.remove(key, value); }
                    @Override public Set<Entry<K, V>> entrySet() { return map.entrySet(); }
                    static final class Synced<K, V> extends Cache<K, V> {
        @Override public V get(Object key) { return super.get(key); }
    }
    final class Entries {
                        boolean contains(Object o) {
                            if (!(o instanceof Entry)) return false;
                            Entry<?, ?> e = (Entry<?, ?>) o;
                            Object key = e.getKey();
                            V v = Cache.this.get(key);
                            return v != null && Cache.this.remove(key, e.getValue());
                        }
                    }
                }
                """);
        // the overrides' Kotlin parameters are non-null here (no NullabilityPass): the casts are to K and V
        contains(kotlin, "this@Cache.get((key as K))");
        contains(kotlin, "this@Cache.remove((key as K), (e.value as V))");
        // the override's own key is a K in Kotlin already (get(key: K) overriding Map.get): no cast
        contains(kotlin, "super.get(key)");
        contains(kotlin, "map.get(key)");
        compiles(kotlin);
    }

    /**
     * guava's Ordering.leastOf(): {@code TopKSelector<E> selector = TopKSelector.least(k, this)} takes E from the
     * local; the comparator, a {@code Comparator<? super T>}, does not decide it. Kotlin needs the local's type.
     */
    @Test
    public void typeArgumentFromTheLocalNotASuperBoundArgument() {
        String kotlin = kotlin("""
                package a;
                import java.util.*;
                abstract class Ord<T> implements Comparator<T> {
                    static final class Top<T> {
                        static <T> Top<T> least(int k, Comparator<? super T> comparator) { return new Top<>(); }
                        List<T> topK() { return new ArrayList<>(); }
                    }
                    <E extends T> List<E> leastOf(int k) {
                        Top<E> selector = Top.least(k, this);
                        return selector.topK();
                    }
                }
                """);
        contains(kotlin, "val selector: Top<E> =");
        compiles(kotlin);
    }

    /**
     * guava's NullsFirstOrdering.reverse(): {@code <@NonNull S>nullsLast()} with {@code S extends @Nullable T}, where
     * nullsLast wants a T: Kotlin's {@code S & Any}.
     */
    @Test
    public void nonNullTypeArgumentOfANullableBoundedVariable() {
        String kotlin = kotlin("""
                package a;
                import java.lang.annotation.*;
                abstract class Ord<T> {
                    @Target(ElementType.TYPE_USE) @interface Nullable { }
                    @Target(ElementType.TYPE_USE) @interface NonNull { }
                    abstract <S extends T> Ord<S> firstOf();
                    <S extends @Nullable T> Ord<?> wider() { return this.<@NonNull S>firstOf(); }
                }
                """);
        contains(kotlin, "firstOf<S & Any>()");
        compiles(kotlin);
    }

    /**
     * guava's NullsFirstOrdering.reverse() in full: {@code <@NonNull S>nullsLast()} is an {@code Ord<@Nullable S>},
     * returned as an {@code Ord<S>} whose S may be null: Kotlin's invariant generics need the unchecked cast.
     */
    @Test
    public void nonNullTypeArgumentResultCastToTheReturnType() {
        String kotlin = kotlin("""
                package a;
                import java.lang.annotation.*;
                abstract class Ord<T> {
                    @Target(ElementType.TYPE_USE) @interface Nullable { }
                    @Target(ElementType.TYPE_USE) @interface NonNull { }
                    abstract <S extends T> Ord<@Nullable S> nullsLast();
                    <S extends @Nullable T> Ord<S> reverse() { return this.<@NonNull S>nullsLast(); }
                }
                """);
        contains(kotlin, "(nullsLast<S & Any>() as Ord<S>)");
        compiles(kotlin);
    }

    /**
     * guava's Multimaps.MapMultimap.containsEntry: {@code map.entrySet().contains(Maps.immutableEntry(key, value))}
     * with Object key and value into a {@code Set<Entry<K, V>>}; and TreeRangeSet's {@code containsKey(Cut.belowAll())}.
     * Kotlin's lookup infers its T from the inputs alone: the argument is cast to the element type, unchecked.
     */
    @Test
    public void lookupOfAParameterizedElement() {
        String kotlin = kotlin("""
                package a;
                import java.util.*;
                class M<K, V> {
                    static final class Cut<C> { static <C> Cut<C> belowAll() { return new Cut<>(); } }
                    final Map<K, V> map = new HashMap<>();
                    final Map<Cut<String>, V> byCut = new HashMap<>();
                    boolean containsEntry(Object key, Object value) {
                        return map.entrySet().contains(new AbstractMap.SimpleEntry<>(key, value));
                    }
                    boolean below() { return byCut.containsKey(Cut.<Comparable<?>>belowAll()); }
                }
                """);
        contains(kotlin, " as MutableMap.MutableEntry<K, V>");
        contains(kotlin, " as Cut<String>?)");
        compiles(kotlin);
    }

    /**
     * guava's FluentIterable.firstMatch(): {@code Optional<@NonNull E>} returned from
     * {@code Iterables.<@NonNull E>tryFind((Iterable<@NonNull E>) getDelegate(), predicate)}: E & Any in the return
     * type and the cast as in the type argument.
     */
    @Test
    public void nonNullTypeVariableInReturnAndCast() {
        String kotlin = kotlin("""
                package a;
                import java.lang.annotation.*;
                import java.util.*;
                @Target(ElementType.TYPE_USE) @interface Nullable { }
                @Target(ElementType.TYPE_USE) @interface NonNull { }
                abstract class Fluent<E extends @Nullable Object> implements Iterable<E> {
                    static <T> Optional<T> tryFind(Iterable<T> iterable) { throw new UnsupportedOperationException(); }
                    Iterable<E> delegate() { return this; }
                    final Optional<@NonNull E> firstMatch() {
                        return Fluent.<@NonNull E>tryFind((Iterable<@NonNull E>) delegate());
                    }
                }
                """);
        contains(kotlin, "fun firstMatch(): Optional<E & Any>");
        contains(kotlin, "tryFind<E & Any>(");
        compiles(kotlin);
    }

    /**
     * guava's Parameter.getDeclaredAnnotations(): {@code annotations.toArray(new Annotation[0])} on its own
     * ImmutableCollection, whose {@code <T> T[] toArray(T[] other)} is translated: arrayOfNulls would make T nullable.
     */
    @Test
    public void emptyArrayIntoATranslatedToArray() {
        String kotlin = kotlin("""
                package a;
                import java.util.*;
                abstract class Coll<E> {
                    abstract <T> T[] toArray(T[] other);
                    static String[] names(Coll<String> c) { return c.toArray(new String[0]); }
                }
                """);
        contains(kotlin, "toArray(emptyArray<String>())");
        compiles(kotlin);
    }

    /**
     * guava's ClassToInstanceMap: {@code <T extends @NonNull B> T getInstance(Class<T> type)}, {@code putInstance(
     * Class<@NonNull T> type, T value)}, implemented without the annotation: the bound and the parameter type stay
     * as the override says them, which Kotlin wants exactly.
     */
    @Test
    public void nonNullTypeVariableNotInSignatures() {
        String kotlin = kotlin("""
                package a;
                import java.lang.annotation.*;
                @Target(ElementType.TYPE_USE) @interface Nullable { }
                @Target(ElementType.TYPE_USE) @interface NonNull { }
                interface ToInstance<B extends @Nullable Object> {
                    <T extends @NonNull B> T getInstance(Class<T> type);
                    <T extends B> T putInstance(Class<@NonNull T> type, T value);
                }
                final class Impl<B> implements ToInstance<B> {
                    public <T extends B> T getInstance(Class<T> type) { return null; }
                    public <T extends B> T putInstance(Class<T> type, T value) { return value; }
                }
                """);
        assertFalse(kotlin.contains("& Any"), kotlin);
    }

    /** guava's AbstractScheduledService.ReschedulableCallable: {@code Void call() { …; return null; }}, a Unit call(). */
    @Test
    public void voidReturningCallable() {
        String kotlin = kotlin("""
                package a;
                import java.util.concurrent.Callable;
                final class Task implements Callable<Void> {
                    int runs;
                    @Override public Void call() { runs++; return null; }
                }
                """);
        contains(kotlin, "override fun call() {");
        assertFalse(kotlin.contains("return null"), kotlin);
        compiles(kotlin);
    }

    /**
     * guava's Futures.allAsList: {@code ListenableFuture<List<@Nullable V>> nullable = new ListFuture<V>(…)}, then
     * {@code @SuppressWarnings("nullness") ListenableFuture<List<V>> nonNull = nullable}: the local keeps its declared
     * List<V?>, and the second assignment is Kotlin's unchecked cast.
     */
    @Test
    public void localDeclaredNullableArgumentAndItsUncheckedConversion() {
        String kotlin = kotlin("""
                package a;
                import java.lang.annotation.*;
                import java.util.*;
                import java.util.function.Supplier;
                @Target(ElementType.TYPE_USE) @interface Nullable { }
                final class F {
                    static final class ListSupplier<V> implements Supplier<List<@Nullable V>> {
                        public List<@Nullable V> get() { return new ArrayList<>(); }
                    }
                    static <V> Supplier<List<V>> allAsList() {
                        Supplier<List<@Nullable V>> nullable = new ListSupplier<V>();
                        @SuppressWarnings("nullness")
                        Supplier<List<V>> nonNull = (Supplier) nullable;
                        return nonNull;
                    }
                }
                """);
        contains(kotlin, "val nullable: Supplier<MutableList<V?>>");
        contains(kotlin, "nullable as Supplier<MutableList<V>>");
    }

    /**
     * guava's MoreExecutors.ScheduledListeningDecorator: a {@code Task<@Nullable Void>} where a {@code Future<Void>}
     * is expected: both are Kotlin's Unit argument.
     */
    @Test
    public void nullableVoidTypeArgumentIsUnit() {
        String kotlin = kotlin("""
                package a;
                import java.lang.annotation.*;
                import java.util.function.Supplier;
                @Target(ElementType.TYPE_USE) @interface Nullable { }
                final class M {
                    static final class Task<V> implements Supplier<V> { public V get() { return null; } }
                    static Supplier<Void> schedule() {
                        Task<@Nullable Void> task = new Task<>();
                        return task;
                    }
                }
                """);
        assertFalse(kotlin.contains("Unit?>"), kotlin);
    }

    /**
     * guava's Multisets.UnmodifiableMultiset.entrySet(): {@code return es == null ? entrySet = (Set)
     * Collections.unmodifiableSet(delegate) : es}: the raw cast in the assignment's value takes the field's arguments.
     */
    @Test
    public void rawCastInAnAssignmentUsedAsValue() {
        String kotlin = kotlin("""
                package a;
                import java.util.*;
                final class U<E> {
                    final Set<E> delegate = new HashSet<>();
                    transient Set<E> entrySet;
                    @SuppressWarnings("unchecked")
                    Set<E> entrySet() {
                        Set<E> es = entrySet;
                        return (es == null) ? entrySet = (Set) Collections.unmodifiableSet(delegate) : es;
                    }
                }
                """);
        contains(kotlin, "as MutableSet<E>");
        compiles(kotlin);
    }

    /**
     * guava's AbstractScheduledService: {@code new FutureAsCancellable(Futures.immediateCancelledFuture())} into a
     * {@code Future<?>}: the star fixes no V, the call states one.
     */
    @Test
    public void resultOnlyCallIntoAStarProjectedConstructorParameter() {
        String kotlin = kotlin("""
                package a;
                import java.util.concurrent.*;
                final class S {
                    static final class Holder { final Future<?> f; Holder(Future<?> f) { this.f = f; } }
                    static <V> Future<V> cancelled() { return new CompletableFuture<>(); }
                    static Holder make() { return new Holder(cancelled()); }
                }
                """);
        contains(kotlin, "Holder(cancelled<Any>())");
        compiles(kotlin);
    }

    /**
     * guava's Doubles.toArray(Collection), called as {@code toArray(new ArrayList<>())}: a translated toArray whose one
     * parameter is no array (5bd6109f7 asked for its element type and crashed the printer).
     */
    @Test
    public void translatedToArrayOfACollection() {
        String kotlin = kotlin("""
                package a;
                import java.util.*;
                final class D {
                    static double[] toArray(Collection<? extends Number> collection) { return new double[collection.size()]; }
                    static double[] empty() { return toArray(new ArrayList<Double>()); }
                }
                """);
        contains(kotlin, "toArray(ArrayList<Double>())");
        compiles(kotlin);
    }

    /** guava's ImmutableTable.SerializedForm.readResolve(): {@code Object readResolve() { return of(); }}. */
    @Test
    public void resultOnlyCallReturnedAsObject() {
        String kotlin = kotlin("""
                package a;
                import java.util.*;
                final class T {
                    static <R, C> Map<R, C> of() { return new HashMap<>(); }
                    Object readResolve() { return of(); }
                }
                """);
        contains(kotlin, "of<Any, Any>()");
        compiles(kotlin);
    }

    /** guava's Streams.findLast: {@code (Optional<T>) Optional.empty()}, the cast giving empty() no type. */
    @Test
    public void resultOnlyCallAsACastsOperand() {
        String kotlin = kotlin("""
                package a;
                import java.util.*;
                final class S {
                    static <T> List<T> none() { return new ArrayList<>(); }
                    @SuppressWarnings("unchecked")
                    static <T> Collection<T> last() { return (Collection<T>) none(); }
                }
                """);
        contains(kotlin, "none<Any>()");
        compiles(kotlin);
    }

    /** guava's Streams.findLast: {@code Optional.of(state.get())} of a T, which Kotlin's Optional.of wants T & Any. */
    @Test
    public void optionalOfATypeVariable() {
        String kotlin = kotlin("""
                package a;
                import java.util.*;
                final class S<T> {
                    T value;
                    T get() { return value; }
                    Optional<T> last() { return Optional.of(get()); }
                }
                """);
        contains(kotlin, "Optional.of(get()!!)");
        compiles(kotlin);
    }
}
