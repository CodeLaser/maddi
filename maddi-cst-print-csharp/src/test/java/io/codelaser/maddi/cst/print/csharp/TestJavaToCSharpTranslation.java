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

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The Java forms C# says differently, one test per translation rule. Each sample is self-contained (no JDK members,
 * which the BCL mapping has yet to translate), and is written to {@code build/csharp-samples/} so that it can be
 * compiled with {@code dotnet build}.
 */
public class TestJavaToCSharpTranslation extends CommonJavaToCSharp {

    private String translate(String name, String java) {
        String csharp = csharp(java);
        try {
            Path dir = Path.of("build/csharp-samples");
            Files.createDirectories(dir);
            Files.writeString(dir.resolve(name + ".cs"), csharp);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return csharp;
    }

    @Language("java")
    private static final String CLASS = """
            package org.example.shapes;
            public class Counter {
                private int count;
                private final String name;
                public static final int LIMIT = 10;
                public Counter(String name) { this.name = name; }
                public int getCount() { return count; }
                public void increment() {
                    if (count < LIMIT) {
                        count++;
                    } else if (count == LIMIT) {
                        count = 0;
                    } else {
                        count += 2;
                    }
                }
                public final String name() { return name; }
                @Override public String toString() { return name + ":" + count; }
            }
            """;

    @Test
    public void classMembers() {
        String cs = translate("Counter", CLASS);
        contains(cs, "namespace Org.Example.Shapes;");
        contains(cs, "public sealed class Counter {");
        contains(cs, "private int count;");
        contains(cs, "private readonly string name;");
        contains(cs, "public const int LIMIT = 10;");
        contains(cs, "public Counter(string name) { this.name = name; }");
        contains(cs, "public int GetCount() => count;");
        contains(cs, "} else if (count == LIMIT) {");
        contains(cs, "public string Name() => name;");
        contains(cs, "public override string ToString() => name + \":\" + count;");
    }

    /** Code outside the program may extend a public class: the policy can keep it, and its methods, open. */
    @Test
    public void classMembersOpenPublicApi() {
        policy = new CSharpProgram.Policy(CSharpProgram.FunctionalInterfaces.DELEGATE_WHERE_POSSIBLE,
                CSharpProgram.Inheritance.OPEN_PUBLIC_API);
        String cs = translate("Counter", CLASS);
        contains(cs, "public class Counter {");
        contains(cs, "public virtual int GetCount() => count;");
    }

    @Language("java")
    private static final String HIERARCHY = """
            package org.example.shapes;
            interface Shape { double area(); default String describe() { return "shape"; } }
            abstract class Base implements Shape {
                protected final double scale;
                protected Base(double scale) { this.scale = scale; }
                public abstract double unit();
                public double area() { return unit() * scale; }
            }
            final class Square extends Base {
                private final double side;
                Square(double side) { super(1.0); this.side = side; }
                @Override public double unit() { return side * side; }
                @Override public String describe() { return "square"; }
            }
            """;

    @Test
    public void hierarchy() {
        String cs = translate("Hierarchy", HIERARCHY);
        contains(cs, "internal interface IShape {\ndouble Area();");
        contains(cs, "string Describe() => \"shape\";");
        contains(cs, "internal abstract class Base : IShape {");
        contains(cs, "protected internal readonly double scale;");
        contains(cs, "public abstract double Unit();");
        contains(cs, "public double Area() => Unit() * scale;");
        contains(cs, "internal sealed class Square : Base {");
        contains(cs, "internal Square(double side) : base(1.0) { this.side = side; }");
        contains(cs, "public override double Unit() => side * side;");
    }

    @Language("java")
    private static final String ENUMS = """
            package org.example.color;
            public class Palette {
                enum Primary { RED, GREEN, BLUE }
                enum Planet {
                    MERCURY(3.303e+23), EARTH(5.976e+24);
                    private final double mass;
                    Planet(double mass) { this.mass = mass; }
                    double mass() { return mass; }
                }
                static String name(Primary p) {
                    switch (p) {
                        case RED: return "red";
                        case GREEN:
                        case BLUE:
                            return "other";
                        default:
                            return "?";
                    }
                }
                static Primary first() { return Primary.RED; }
            }
            """;

    @Test
    public void enums() {
        String cs = translate("Palette", ENUMS);
        contains(cs, "internal enum Primary { Red, Green, Blue }");
        contains(cs, "internal sealed class Planet {");
        contains(cs, "public static readonly Planet Mercury = new Planet(3.303E23) { Name = \"MERCURY\", Ordinal = 0 };");
        contains(cs, "case Primary.Red:");
        contains(cs, "internal static Primary First() => Primary.Red;");
    }

    @Language("java")
    private static final String SWITCHES = """
            package org.example.flow;
            class Switches {
                static int fallThrough(int k) {
                    int r = 0;
                    switch (k) {
                        case 1:
                            r += 1;
                        case 2:
                            r += 2;
                            break;
                        case 3:
                            r = 3;
                        default:
                            r--;
                    }
                    return r;
                }
                static String arrows(int k) {
                    switch (k) {
                        case 1, 2 -> { return "small"; }
                        case 3 -> System.out.println("three");
                        default -> { }
                    }
                    return "large";
                }
                static int expression(String s) {
                    return switch (s) {
                        case "a", "b" -> 1;
                        case "c" -> {
                            int x = 2;
                            yield x * 2;
                        }
                        default -> throw new IllegalArgumentException(s);
                    };
                }
            }
            """;

    @Test
    public void switches() {
        String cs = translate("Switches", SWITCHES);
        contains(cs, "r += 1;\ngoto case 2;");
        contains(cs, "r = 3;\ngoto default;");
        contains(cs, "default:\nr--;\nbreak;");
        contains(cs, "case 1: case 2: return \"small\";");
        contains(cs, "\"a\" or \"b\" => 1,");
        contains(cs, "_ => throw new ArgumentException(s)");
    }

    @Language("java")
    private static final String LOOPS = """
            package org.example.flow;
            class Loops {
                static int search(int[][] grid, int target) {
                    int found = -1;
                    outer:
                    for (int i = 0; i < grid.length; i++) {
                        for (int j = 0; j < grid[i].length; j++) {
                            if (grid[i][j] < 0) continue outer;
                            if (grid[i][j] == target) { found = i; break outer; }
                        }
                    }
                    return found;
                }
                static long sum(int... values) {
                    long total = 0;
                    for (int v : values) total += v;
                    int k = 0;
                    do { k++; } while (k < 3);
                    while (k > 0) k--;
                    return total;
                }
            }
            """;

    @Test
    public void loops() {
        String cs = translate("Loops", LOOPS);
        contains(cs, "if (grid[i][j] < 0) { goto outer_continue; }");
        contains(cs, "goto outer_break;");
        contains(cs, "outer_continue: ;");
        contains(cs, "outer_break: ;");
        contains(cs, "for (int i = 0; i < grid.Length; i++) {");
        contains(cs, "internal static long Sum(params int[] values) {");
        contains(cs, "foreach (int v in values) { total += v; }");
    }

    @Language("java")
    private static final String EXPRESSIONS = """
            package org.example.expr;
            class Expressions {
                interface Op { int apply(int a, int b); }
                static int twice(Op op, int x) { return op.apply(x, x); }
                static int add(int a, int b) { return a + b; }
                static boolean same(String a, String b) { return a == b; }
                static String kind(Object o) {
                    if (o instanceof String s && !s.isEmpty()) return s;
                    if (!(o instanceof Integer)) return "other";
                    return o == null ? "null" : "int";
                }
                static int bits(int x) { return (x >>> 3) ^ ~x & 0xff; }
                static Class<?> type() { return Expressions.class; }
                static int[] numbers() { return new int[] { 1, 2, 3 }; }
                static long widen(int i) { return (long) i << 2; }
                static char letter() { return '\\''; }
            }
            """;

    @Test
    public void expressions() {
        String cs = translate("Expressions", EXPRESSIONS);
        contains(cs, "internal static bool Same(string a, string b) => object.ReferenceEquals(a, b);");
        contains(cs, "if (o is string s && !(s.Length == 0)) { return s; }");
        contains(cs, "if (o is not int) { return \"other\"; }");
        contains(cs, "x >>> 3");
        contains(cs, "typeof(Expressions)");
        contains(cs, "new int[] { 1, 2, 3 }");
        contains(cs, "'\\''");
    }

    @Language("java")
    private static final String TRY = """
            package org.example.io;
            class Resources {
                static class Res implements AutoCloseable {
                    public void close() { }
                    int read() { return 1; }
                }
                static int one() throws Exception {
                    try (Res a = new Res(); Res b = new Res()) {
                        return a.read() + b.read();
                    }
                }
                static int caught() {
                    try (Res r = new Res()) {
                        return r.read();
                    } catch (IllegalStateException | UnsupportedOperationException e) {
                        return -1;
                    } finally {
                        System.gc();
                    }
                }
            }
            """;

    @Test
    public void tryWithResources() {
        String cs = translate("Resources", TRY);
        contains(cs, "using (var a = new Res())");
        contains(cs, "using (var b = new Res())");
        contains(cs, "catch (Exception e) when (e is InvalidOperationException || e is NotSupportedException) {");
    }

    @Language("java")
    private static final String UTIL = """
            package org.example.util;
            public final class Strings {
                private Strings() { }
                public static final String EMPTY = "";
                public static <T extends Comparable<T>> T max(T a, T b) { return a.compareTo(b) >= 0 ? a : b; }
            }
            """;

    @Test
    public void staticClass() {
        String cs = translate("Strings", UTIL);
        contains(cs, "public static class Strings {");
        contains(cs, "public static T Max<T>(T a, T b) where T : IComparable<T> => a.CompareTo(b) >= 0 ? a : b;");
    }

    @Language("java")
    private static final String RECORD = """
            package org.example.geo;
            record Point(int x, int y) {
                Point scale(int f) { return new Point(x * f, y() * f); }
                static Point origin() { return new Point(0, 0); }
            }
            """;

    @Test
    public void record() {
        String cs = translate("Point", RECORD);
        contains(cs, "internal sealed record Point(int X, int Y) {");
        contains(cs, "internal Point Scale(int f) => new Point(X * f, Y * f);");
    }

    @Language("java")
    private static final String LAMBDAS = """
            package org.example.fn;
            import java.util.function.Function;
            import java.util.function.Supplier;
            class Lambdas {
                static int len(String s) { return 1; }
                static Function<String, Integer> lengths() { return Lambdas::len; }
                static Function<Integer, Integer> inc() { return x -> x + 1; }
                static Supplier<StringBuilder> fresh() { return StringBuilder::new; }
                static Function<Integer, Integer> block() {
                    return x -> {
                        int y = x * 2;
                        return y + 1;
                    };
                }
            }
            """;

    @Test
    public void lambdas() {
        String cs = translate("Lambdas", LAMBDAS);
        contains(cs, "internal static Func<string, int> Lengths() => Lambdas.Len;");
        contains(cs, "internal static Func<int, int> Inc() => x => x + 1;");
        contains(cs, "() => new StringBuilder()");
    }

    @Language("java")
    private static final String ACCESS = """
            package org.example.access;
            import java.util.List;
            class Outer {
                private int secret;
                private static class Helper {
                    private int used;
                    private int unused;
                    private Helper() { }
                    private int peek(Outer o) { return o.secret + unused; }
                }
                int read() { return new Helper().used; }
                static class Key<T extends Number> { }
                Key raw() { return null; }
                static class Base { protected void m() { } }
                static class Derived extends Base { @Override public void m() { } }
            }
            """;

    /**
     * Java's private reaches across the nested types of one top-level type; C#'s does not. What the outside reaches
     * is internal, the rest stays private. An override keeps the access of what it overrides: C# may not widen it.
     */
    @Test
    public void access() {
        String cs = translate("Access", ACCESS);
        contains(cs, "private int secret;");
        contains(cs, "internal int used;");
        contains(cs, "private int unused;");
        contains(cs, "internal Helper() { }");
        contains(cs, "private int Peek(Outer o) => o.secret + unused;");
        contains(cs, "internal Key<Number> Raw() => null;");
        contains(cs, "protected internal override void M() { }");
    }

    @Language("java")
    private static final String SCOPES = """
            package org.example.scope;
            class Scopes {
                static final int[][] TABLE = { null, { 1, 2 }, {} };
                static int later(int[] xs) {
                    for (int k = 0; k < xs.length; k++) {
                        int last = xs[k];
                        if (last > 0) return last;
                    }
                    int last = -1;
                    return last;
                }
                static String twice(Object o, Object p) {
                    if (o instanceof String s) return s;
                    if (p instanceof String s) return s + s;
                    return "";
                }
            }
            """;

    /**
     * C# scopes a local to its whole block, and a pattern variable of an if condition to the enclosing block: names
     * Java can reuse clash in C#, and are renamed. A jagged array's nested initializers are array creations.
     */
    @Test
    public void scopes() {
        String cs = translate("Scopes", SCOPES);
        contains(cs, "internal static readonly int[][] TABLE = { null, new int[] { 1, 2 }, new int[] { } };");
        contains(cs, "int last2 = xs[k];\nif (last2 > 0) { return last2; }");
        contains(cs, "int last = -1;\nreturn last;");
        contains(cs, "if (o is string s) { return s; }");
        contains(cs, "if (p is string s2) { return s2 + s2; }");
    }

    @Language("java")
    private static final String CONVERSIONS = """
            package org.example.conv;
            import java.util.HashMap;
            import java.util.Map;
            import java.util.Set;
            class Factory<E> {
                private int size() { return 3; }
                static class Item<E> {
                    final Set<E>[] buckets = new Set[2];
                    int n(Factory<E> f) { return f.size(); }
                }
                static <T> T none() { return null; }
                static String escape(char c) {
                    switch (c) {
                        case 0x8: return "\\b";
                        default: return "";
                    }
                }
                static int orZero(Integer i, Integer count) { return i == null ? 0 : i + count; }
                static int index(Map<String, Integer> map, String key, Integer boxed) {
                    Map<Integer, String> names = new HashMap<>();
                    String name = names.get(boxed);
                    int i = map.get(key);
                    if ((Boolean) (Object) Boolean.TRUE) return boxed;
                    return i + name.length();
                }
            }
            """;

    /**
     * A static nested type of a generic type is printed beside it (C# would make it generic in the outer type's
     * parameters); the outer type's private members it uses become internal. C# does not unbox: an Integer where an
     * int is expected, or as a key, gets a cast. Java's null of a type parameter is default; a raw array creation
     * takes the declared type's arguments.
     */
    @Test
    public void conversions() {
        String cs = translate("Factory", CONVERSIONS);
        contains(cs, "internal sealed class Item<E> {");
        contains(cs, "internal int Size() => 3;");
        contains(cs, "internal readonly ISet<E>[] buckets = new ISet<E>[2];");
        contains(cs, "internal static T None<T>() => default;");
        contains(cs, "string name = names.Get((int) boxed);");
        contains(cs, "int i = (int) map.GetValueOrNull(key);");
        contains(cs, "if ((bool) ((object) true)) { return (int) boxed; }");
        contains(cs, "case '\\b': return");
        contains(cs, "internal static int OrZero(int? i, int? count) => i == null ? 0 : (int) i + (int) count;");
    }

    @Language("java")
    private static final String LOCAL_CLASSES = """
            package org.example.local;
            import java.util.ArrayList;
            import java.util.List;
            class Locals {
                private int base = 1;
                int count(int n) {
                    class Entry {
                        final int value;
                        Entry(int value) { this.value = value; }
                    }
                    List<Entry> entries = new ArrayList<>();
                    for (int i = 0; i < n; i++) entries.add(new Entry(i));
                    return entries.size();
                }
                int captures(int n) {
                    class Adder {
                        int add(int x) { return x + n + base; }
                    }
                    return new Adder().add(1);
                }
            }
            """;

    /** A local class that captures nothing is lifted into its enclosing type; one that captures is not translated. */
    @Test
    public void localClasses() {
        String cs = translate("Locals", LOCAL_CLASSES);
        contains(cs, "private sealed class Entry {");
        contains(cs, "entries.Add(new Entry(i));");
        assertFalse(cs.contains("class Adder"), cs);
    }

    @Language("java")
    private static final String FUNCTIONAL = """
            package org.example.fun;
            import java.util.ArrayList;
            import java.util.List;
            class Graph {
                interface Visitor {
                    int visit(String node);
                }
                interface Listener {
                    void changed(String node);
                }
                static class Recorder implements Listener {
                    final List<String> seen = new ArrayList<>();
                    public void changed(String node) { seen.add(node); }
                }
                static int walk(Visitor v) { return v.visit("a"); }
                static int lengths() { return walk(s -> s.length()) + walk(Graph::one); }
                static int counted() {
                    return walk(new Visitor() {
                        @Override
                        public int visit(String node) { return 2; }
                    });
                }
                static Visitor bound(Visitor v) { return v::visit; }
                static int one(String s) { return 1; }
                static void tell(Listener l) { l.changed("b"); }
                static void quiet() { tell(n -> { }); }
            }
            """;

    /**
     * A translated functional interface that nothing in the program implements is a C# delegate; one that a class
     * implements stays an interface, with an adapter class for its lambdas.
     */
    @Test
    public void functionalInterface() {
        String cs = translate("Graph", FUNCTIONAL);
        contains(cs, "internal delegate int Visitor(string node);");
        contains(cs, "internal static int Walk(Visitor v) => v(\"a\");");
        contains(cs, "internal static int Lengths() => Walk(s => s.Length) + Walk(Graph.One);");
        contains(cs, "internal static int Counted() => Walk((string node) => 2);");
        contains(cs, "internal static Visitor Bound(Visitor v) => v.Invoke;");
        contains(cs, "public sealed class Lambda(Action<string> f) : IListener { public void Changed(string node) => f(node); }");
        contains(cs, "internal static void Tell(IListener l) { l.Changed(\"b\"); }");
        contains(cs, "internal static void Quiet() { Tell(new IListener.Lambda(n => { })); }");
    }

    @Language("java")
    private static final String ANONYMOUS = """
            package org.example.anon;
            import java.util.ArrayList;
            import java.util.List;
            class Engine {
                interface Graph {
                    List<String> nodes();
                    String first();
                }
                abstract static class Counter {
                    final int start;
                    Counter(int start) { this.start = start; }
                    abstract int next();
                }
                private final String prefix = "n";
                int size(Graph g) { return g.nodes().size(); }
                int run(int count) {
                    List<String> all = new ArrayList<>();
                    Graph g = new Graph() {
                        @Override
                        public List<String> nodes() { return all; }
                        @Override
                        public String first() { return prefix + count; }
                    };
                    Counter c = new Counter(count) {
                        private int i = start;
                        @Override
                        int next() { return i++; }
                    };
                    return size(g) + c.next();
                }
            }
            """;

    /**
     * An anonymous class that does not become a lambda is hoisted into a private nested class: what it captures is
     * passed to its constructor, the enclosing instance as {@code outer}, and its fields are initialised there.
     */
    @Test
    public void anonymousClasses() {
        String cs = translate("Engine", ANONYMOUS);
        contains(cs, "IGraph g = new GraphImpl(this, all, count);");
        contains(cs, "Counter c = new CounterImpl(count);");
        contains(cs, "private sealed class GraphImpl : IGraph {");
        contains(cs, "internal GraphImpl(Engine outer, List<string> all, int count) {");
        contains(cs, "public string First() => outer.prefix + count;");
        contains(cs, "private sealed class CounterImpl : Counter {");
        contains(cs, "internal CounterImpl(int p0) : base(p0) { this.i = start; }");
    }

    @Language("java")
    private static final String IDIOMS = """
            package org.example.idiom;
            import java.util.List;
            class Node implements Cloneable {
                int value;
                @Override
                public Node clone() {
                    try {
                        return (Node) super.clone();
                    } catch (CloneNotSupportedException e) {
                        throw new RuntimeException(e);
                    }
                }
                static int sum(List<Integer> values) {
                    int total = 0;
                    for (Integer v : values) {
                        if (v == null) v = 0;
                        total += v;
                    }
                    return total;
                }
            }
            """;

    /**
     * C#'s object has no clone to override: Java's clone is a method of its own, super.clone() is MemberwiseClone.
     * C#'s iteration variable is read-only: a loop that assigns it works on a copy.
     */
    @Test
    public void idioms() {
        String cs = translate("Node", IDIOMS);
        contains(cs, "internal sealed class Node {");
        contains(cs, "public Node Clone() {");
        contains(cs, "return (Node) base.MemberwiseClone();");
        contains(cs, "foreach (int? vItem in values) {\nint? v = vItem;");
    }

    @Language("java")
    private static final String EXPOSED = """
            package org.example.exposed;
            import java.util.ArrayList;
            import java.util.List;
            public class Result {
                private static class Pair {
                    final int a;
                    Pair(int a) { this.a = a; }
                }
                private static class Hidden {
                }
                private final List<Pair> pairs = new ArrayList<>();
                public List<Pair> pairs() { return pairs; }
                Hidden hidden() { return new Hidden(); }
            }
            """;

    /**
     * Java's public method may name a less accessible type, C#'s may not: the type becomes as visible as the member
     * that exposes it, no more than the member's own type.
     */
    @Test
    public void exposedTypes() {
        String cs = translate("Result", EXPOSED);
        contains(cs, "public sealed class Pair {");
        contains(cs, "internal sealed class Hidden {");
    }

    @Language("java")
    private static final String SUPPLIERS = """
            package org.example.supply;
            import java.util.ArrayList;
            import java.util.List;
            import java.util.Optional;
            class Defaults<R extends Defaults<R>> {
                static List<String> names(List<String> given) { return Optional.ofNullable(given).orElseGet(List::of); }
                static List<String> copy(List<String> given) { return Optional.ofNullable(given).orElseGet(ArrayList::new); }
                static String name(String given) { return Optional.ofNullable(given).orElseGet(() -> "none"); }
                static Defaults raw() { return null; }
            }
            """;

    /**
     * C# cannot call a lambda where it is written: a supplier argument that a template calls is written in place.
     * An F-bounded type used raw stops at its own erasure.
     */
    @Test
    public void suppliers() {
        String cs = translate("Defaults", SUPPLIERS);
        contains(cs, "internal static List<string> Names(List<string> given) => given ?? new List<string>();");
        contains(cs, "internal static List<string> Copy(List<string> given) => given ?? new List<string>();");
        contains(cs, "internal static string Name(string given) => given ?? \"none\";");
        contains(cs, "internal static Defaults<");
    }

    @Language("java")
    private static final String NAMESPACES = """
            package org.example.exception.query;
            class Query {
                static void check(String s) { if (s == null) throw new IllegalStateException("no " + s); }
            }
            """;

    /**
     * A namespace segment with the name of a type, of the program or of the BCL, is plural: the namespace
     * {@code Org.Example.Exception} would hide {@code System.Exception}.
     */
    @Test
    public void namespaces() {
        String cs = translate("Query", NAMESPACES);
        contains(cs, "namespace Org.Example.Exceptions.Queries;");
    }

    @Language("java")
    private static final String LANGUAGE = """
            package org.example.lang;
            import java.util.HashMap;
            import java.util.Map;
            class Metadata {
                interface Result {
                    boolean success();
                    default boolean failed() { return !success(); }
                }
                interface Listener<T> {
                    void onEvent(T event);
                }
                interface StartListener extends Listener<String> {
                }
                static class Ok implements Result {
                    public boolean success() { return true; }
                    boolean check() { return failed(); }
                }
                abstract static class Builder<T extends Builder<T>> {
                    String name;
                    T name(String name) { this.name = name; return (T) this; }
                }
                private final Map<String, Object> metadata = new HashMap<>();
                static Metadata metadata(String key, Object value) { return new Metadata(); }
                Object get(String key) { return metadata.get(key); }
            }
            """;

    /**
     * A C# class does not inherit its interfaces' default methods: a call goes through the interface. A cast to a type
     * parameter goes through object. A method named as its class is renamed. An adapter substitutes the arguments a
     * functional interface gives its generic super-interface.
     */
    @Test
    public void language() {
        String cs = translate("Metadata", LANGUAGE);
        contains(cs, "internal bool Check() => ((IResult) this).Failed();");
        contains(cs, "return (T) (object) this;");
        contains(cs, "internal static Metadata Of(string key, object value) => new Metadata();");
        contains(cs, "public sealed class Lambda(Action<string> f) : IStartListener { public void OnEvent(string @event) => f(@event); }");
    }

    @Language("java")
    private static final String ANNOTATIONS = """
            package org.example.attr;
            import java.lang.annotation.ElementType;
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;
            import java.lang.annotation.Target;
            import java.lang.reflect.Method;
            class Tools {
                @Retention(RetentionPolicy.RUNTIME)
                @Target({ElementType.METHOD})
                @interface Tool {
                    String name() default "";
                    String[] value() default "";
                    boolean required() default true;
                }
                @Tool(value = "Adds", name = "add")
                int add(int a, int b) { return a + b; }
                @Deprecated
                int old() { return 0; }
                static String nameOf(Tool tool) { return tool.name(); }
            }
            """;

    /**
     * An annotation type is an attribute class with properties and their defaults; a use is an attribute, its value
     * element positional; reading an element reads the property.
     */
    @Test
    public void annotations() {
        String cs = translate("Tools", ANNOTATIONS);
        contains(cs, "[AttributeUsage(AttributeTargets.Method)]");
        contains(cs, "internal sealed class ToolAttribute : Attribute {");
        contains(cs, "public ToolAttribute(params string[] value) { Value = value; }");
        contains(cs, "public string Name { get; set; } = \"\";");
        contains(cs, "public string[] Value { get; set; } = new string[] { \"\" };");
        contains(cs, "public bool Required { get; set; } = true;");
        contains(cs, "[Tool(\"Adds\", Name = \"add\")]");
        contains(cs, "[Obsolete]");
        contains(cs, "internal static string NameOf(ToolAttribute tool) => tool.Name;");
    }

    @Language("java")
    private static final String RECORDS = """
            package org.example.rec;
            interface Result<T> {
                T response();
            }
            record Success<T>(T response, String note) implements Result<T> {
                public Success {
                    if (response == null) throw new IllegalArgumentException("response");
                    note = note == null ? "" : note.trim();
                }
            }
            """;

    /**
     * A record with a compact constructor is a record with get-only properties and that constructor, which assigns
     * them after its body (the CST's compact constructor does). An accessor implementing an interface method implements it explicitly.
     */
    @Test
    public void records() {
        String cs = translate("Result", RECORDS);
        contains(cs, "internal sealed record Success<T> : IResult<T> {");
        contains(cs, "public T Response { get; }");
        contains(cs, "note = note == null ? \"\" : note.Trim();\nResponse = response;\nNote = note;");
        contains(cs, "T IResult<T>.Response() => Response;");
    }

    @Language("java")
    private static final String UNBOUND = """
            package org.example.unbound;
            import java.util.Collection;
            import java.util.List;
            class Checks {
                static <T extends Collection<?>> T ensureNotEmpty(T c) { if (c.isEmpty()) throw new IllegalArgumentException(); return c; }
                static int size(List<?> list) { return list.size(); }
                static List<String> names(List<String> names) { return ensureNotEmpty(names); }
            }
            """;

    /** A collection of {@code ?} is C#'s non-generic interface, which every generic collection implements. */
    @Test
    public void unboundWildcards() {
        String cs = translate("Checks", UNBOUND);
        contains(cs, "internal static T EnsureNotEmpty<T>(T c) where T : ICollection {");
        contains(cs, "internal static int Size(IList list) => list.Count;");
    }

    @Language("java")
    private static final String INTERFACES = """
            package org.example.interfaces;
            interface Listener<E> {
                void onEvent(E event);
                Class<E> eventClass();
            }
            interface StringListener extends Listener<String> {
                default Class<String> eventClass() { return String.class; }
            }
            interface Failure {
                Failure withCode(int code);
            }
            interface Model {
                int dimension();
                String embed(String text);
            }
            abstract class AbstractModel implements Model {
                public int dimension() { return embed("test").length(); }
            }
            final class UpperModel extends AbstractModel {
                public String embed(String text) { return text.toUpperCase(); }
            }
            class Holder {
                static final class SimpleFailure implements Failure {
                    public SimpleFailure withCode(int code) { return this; }
                }
            }
            """;

    /** C#'s interface members implement others only explicitly, and an abstract class declares what it leaves. */
    @Test
    public void interfaceImplementations() {
        String cs = translate("Listener", INTERFACES);
        contains(cs, "Type IListener<string>.EventClass() => typeof(string);");
        contains(cs, "public SimpleFailure WithCode(int code) => this;");
        contains(cs, "IFailure IFailure.WithCode(int code) => WithCode(code);");
        contains(cs, "public abstract string Embed(string text);");
        contains(cs, "public override string Embed(string text) => text.ToUpperInvariant();");
    }
}
