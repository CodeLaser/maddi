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
        contains(kotlin, "getOrDefault(d, java.util.function.Supplier(::D))");
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
    }
}
