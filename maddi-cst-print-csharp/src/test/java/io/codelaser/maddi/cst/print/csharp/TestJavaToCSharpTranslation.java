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

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    /**
     * A getter of a field is a property that takes the field's place: {@code { get; }} for a field assigned only in
     * constructors, {@code private set} for one that its class writes too. The field's uses are the property's.
     */
    @Test
    public void classMembers() {
        String cs = translate("Counter", CLASS);
        contains(cs, "namespace Org.Example.Shapes;");
        contains(cs, "public sealed class Counter {");
        contains(cs, "public int Count { get; private set; }");
        contains(cs, "public string Name { get; }");
        contains(cs, "public const int LIMIT = 10;");
        contains(cs, "public Counter(string name) { this.Name = name; }");
        contains(cs, "if (Count < LIMIT) { Count++; } else if (Count == LIMIT) { Count = 0; } else { Count += 2; }");
        contains(cs, "public override string ToString() => Name + \":\" + Count;");
    }

    /** The policy can keep Java's getters as methods. */
    @Test
    public void classMembersAsMethods() {
        policy = CSharpProgram.Policy.DEFAULT.withAccessors(CSharpProgram.Accessors.METHODS);
        String cs = translate("CounterAsMethods", CLASS);
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
        contains(cs, "public virtual int Count { get; private set; }");
        contains(cs, "public string Name { get; }");
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

    /**
     * Methods that override one another, parameterless and of one type, are one property: the interface declares it
     * (with its default body), an abstract class leaves it abstract, the others compute it. The lambda adapter of
     * the functional interface implements it as a property.
     */
    @Test
    public void hierarchy() {
        String cs = translate("Hierarchy", HIERARCHY);
        contains(cs, "internal interface IShape {\ndouble Area { get; }");
        contains(cs, "string Describe => \"shape\";");
        contains(cs, "public sealed class Lambda(Func<double> f) : IShape { public double Area => f(); }");
        contains(cs, "internal abstract class Base : IShape {");
        contains(cs, "protected internal readonly double scale;");
        contains(cs, "public abstract double Unit { get; }");
        contains(cs, "public double Area => Unit * scale;");
        contains(cs, "internal sealed class Square : Base {");
        contains(cs, "internal Square(double side) : base(1.0) { this.side = side; }");
        contains(cs, "public override double Unit => side * side;");
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
        contains(cs, "internal Key<IConvertible> Raw() => null;");
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

    @Language("java")
    private static final String MEMBER_NAMES = """
            package org.example.members;
            final class Match<Embedded> {
                private final Embedded embedded;
                Match(Embedded embedded) { this.embedded = embedded; }
                Embedded embedded() { return embedded; }
            }
            final class Outcome {
                enum Result { SUCCESS, FAILURE }
                private final Result result;
                Outcome(Result result) { this.result = result; }
                Result result() { return result; }
            }
            """;

    /** C# has no member named as a type parameter or a nested type of its type. */
    @Test
    public void memberNames() {
        String cs = translate("Match", MEMBER_NAMES);
        contains(cs, "internal sealed class Match<TEmbedded> {");
        contains(cs, "internal TEmbedded Embedded { get; }");
        contains(cs, "internal Result ResultValue() => result;");
    }

    @Language("java")
    private static final String CAPTURE = """
            package org.example.capture;
            import java.util.List;
            interface Named { String name(); String title(); }
            class Context<Embedded> {
                List<Embedded> items() { return List.of(); }
            }
            interface Listener {
                default void onRequest(Context<?> context) { }
                default int size(Context<? extends Named> context) { return context.items().size(); }
                default int length(Context<? extends String> context) { return context.items().size(); }
            }
            final class Counting implements Listener {
                public void onRequest(Context<?> context) { System.out.println(context.items().size()); }
            }
            """;

    /** Java captures a wildcard in the body; C#'s method takes it as a type parameter of its own. */
    @Test
    public void wildcardCapture() {
        String cs = translate("Capture", CAPTURE);
        contains(cs, "void OnRequest<TEmbedded>(Context<TEmbedded> context) { }");
        contains(cs, "int Size<TEmbedded>(Context<TEmbedded> context) where TEmbedded : INamed");
        contains(cs, "int Length(Context<string> context)");
        contains(cs, "public void OnRequest<TEmbedded>(Context<TEmbedded> context)");
    }

    @Language("java")
    private static final String TYPE_TOKENS = """
            package org.example.tokens;
            import java.util.List;
            class Json {
                static <T> T fromJson(String json, Class<T> type) {
                    System.out.println(type.getName());
                    return null;
                }
                static <T> List<T> all(String json, Class<T> type) { return List.of(fromJson(json, type)); }
                static <T> String describe(Class<T> type) { return type.getName(); }
                static String use(Class<?> unknown) { return describe(unknown); }
                static Integer number(String json) { return fromJson(json, Integer.class); }
            }
            """;

    /** A type token, {@code Class<T> type}, is the method's type argument when every call says what it is. */
    @Test
    public void typeTokens() {
        String cs = translate("Json", TYPE_TOKENS);
        contains(cs, "internal static T FromJson<T>(string json) {");
        contains(cs, "Console.Out.WriteLine(typeof(T)");
        contains(cs, "internal static List<T> All<T>(string json) => new List<T> { FromJson<T>(json) };");
        contains(cs, "FromJson<int>(json)");
        contains(cs, "internal static string Describe<T>(Type type)");
    }

    @Language("java")
    private static final String COMPANION = """
            package org.example.companion;
            public final class Response<T> {
                private static final String EMPTY = "";
                private final T content;
                private Response(T content) { this.content = content; }
                public T content() { return content; }
                public String describe() { return EMPTY + content + count(); }
                private static int count() { return 1; }
                public static <T> Response<T> from(T content) { return new Response<>(content); }
            }
            interface Store<E> {
                int LIMIT = 10;
                static <E> Store<E> empty() { return null; }
                E get();
            }
            class User {
                Response<String> hello() { return Response.from("hello" + Store.LIMIT); }
            }
            """;

    /** C# reaches a generic class's statics through a constructed type: they are in a non-generic class beside it. */
    @Test
    public void companionClasses() {
        String cs = translate("Response", COMPANION);
        contains(cs, "public sealed class Response<T> {");
        contains(cs, "public string Describe() => Response.EMPTY + Content + Response.Count();");
        contains(cs, "internal Response(T content)");
        contains(cs, "public static class Response {");
        contains(cs, "internal const string EMPTY = \"\";");
        contains(cs, "internal static int Count() => 1;");
        contains(cs, "public static Response<T> From<T>(T content) => new Response<T>(content);");
        contains(cs, "internal static class IStore {");
        contains(cs, "public const int LIMIT = 10;");
        contains(cs, "Response.From(\"hello\" + IStore.LIMIT)");
    }

    @Language("java")
    private static final String VALUES = """
            package org.example.values;
            import java.math.BigDecimal;
            import java.net.URI;
            import java.time.Clock;
            import java.time.Duration;
            import java.time.LocalDate;
            import java.time.LocalDateTime;
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
        contains(cs, "internal static Guid Parse(string s) => Guid.Parse(s);");
        contains(cs, "internal static Uri Uri(string s) => new Uri(s, UriKind.RelativeOrAbsolute);");
        contains(cs, "internal static string Path(Uri uri) => uri.AbsolutePath;");
        contains(cs, ".CompareTo((decimal) 3L)");
        contains(cs, "o is IConvertible n ? Convert.ToDouble(n) : 0");
        contains(cs, "DateOnly.ParseExact(s, \"yyyy-MM-dd\", CultureInfo.InvariantCulture)");
        contains(cs, "internal static DateTime Now(TimeProvider clock) => clock.GetLocalNow().DateTime;");
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
        contains(cs, "internal static string Type(FileInfo path) => (new FileInfo(path.Name)).ToString();");
        contains(cs, "internal static string Type(string path) => Type(new FileInfo(path));");
    }

    /**
     * A client's mapping in front of the JDK's overrides single rules: here a UUID is a string, randomUUID()
     * Guid.NewGuid().ToString() and fromString(s) s; what it does not answer, URI, stays the JDK mapping's.
     */
    @Test
    public void libraryOverride() {
        LibraryMapping uuidAsString = new LibraryMapping() {
            @Override
            public String id() {
                return "client:uuid-as-string";
            }

            @Override
            public TypeMapping type(io.codelaser.maddi.cst.api.info.TypeInfo typeInfo) {
                return "java.util.UUID".equals(typeInfo.fullyQualifiedName())
                        ? new TypeMapping("string", null, false, false) : null;
            }

            @Override
            public Rule call(io.codelaser.maddi.cst.api.info.MethodInfo method,
                             io.codelaser.maddi.cst.api.expression.MethodCall call) {
                if (!"java.util.UUID".equals(method.typeInfo().fullyQualifiedName())) return null;
                return switch (method.name()) {
                    case "randomUUID" -> new Rule("Guid.NewGuid().ToString()", null, java.util.List.of("System"));
                    case "fromString" -> new Rule("$1", null, java.util.List.of());
                    default -> null;
                };
            }
        };
        policy = CSharpProgram.Policy.DEFAULT.withLibraries(CSharpLibraries.DEFAULT.withFirst(uuidAsString));
        String cs = translate("ValuesOverridden", VALUES);
        contains(cs, "internal static string Id() => Guid.NewGuid().ToString().ToString();");
        contains(cs, "internal static string Parse(string s) => s;");
        contains(cs, "internal static Uri Uri(string s) => new Uri(s, UriKind.RelativeOrAbsolute);");
    }

    private static final String UUID_AS_STRING = """
            # a client's overrides: a UUID is a string
            mapping client:uuid-as-string
            type java.util.UUID -> string
            member java.util.UUID.randomUUID/0 -> Guid.NewGuid().ToString() using System
            member java.util.UUID.fromString/1 -> $1
            member java.util.Random.nextInt/1 ?receiver=java.util.Random -> $0.Next(0, $1)   # a guard
            """;

    /** The same override as a mapping file, the way a client writes it without Java; with a guarded rule. */
    @Test
    public void libraryOverrideFile() {
        policy = CSharpProgram.Policy.DEFAULT.withLibraries(CSharpLibraries.DEFAULT
                .withFirst(CSharpMappingFile.parse(UUID_AS_STRING, "uuid.csmap")));
        String cs = translate("ValuesOverriddenByFile", VALUES);
        contains(cs, "internal static string Id() => Guid.NewGuid().ToString().ToString();");
        contains(cs, "internal static string Parse(string s) => s;");
        contains(cs, "internal static int Roll(Random random) => random.Next(0, 6);");
        IllegalArgumentException e = org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> CSharpMappingFile.parse("mapping x\nmember a.B.c/0 ?nope -> d", "bad.csmap"));
        org.junit.jupiter.api.Assertions.assertEquals("bad.csmap:2: no guard ?nope", e.getMessage());
    }

    /** A mapping's packages go into the translation's project file; the JDK's compatibility library is its source. */
    @Test
    public void libraryRequirements() {
        CSharpLibraries libraries = CSharpLibraries.choose("slf4j=none,jackson=none").withFirst(CSharpMappingFile.parse("""
                mapping slf4j:example
                nuget Microsoft.Extensions.Logging.Abstractions 10.0.0
                type org.slf4j.Logger -> ILogger using Microsoft.Extensions.Logging
                """, "example.csmap"));
        org.junit.jupiter.api.Assertions.assertEquals(java.util.List.of(
                new LibraryMapping.NuGetPackage("Microsoft.Extensions.Logging.Abstractions", "10.0.0")),
                libraries.requirements().packages());
        org.junit.jupiter.api.Assertions.assertEquals(java.util.List.of(CSharpCompat.FILE_NAME),
                libraries.requirements().sources().stream().map(LibraryMapping.Source::fileName).toList());
        String project = libraries.projectFile("net10.0");
        contains(project, "<PackageReference Include=\"Microsoft.Extensions.Logging.Abstractions\" Version=\"10.0.0\" />");
        contains(project, "<!-- generated by maddi's Java -> C# translation; libraries: slf4j:example,jdk -->");
    }

    @Language("java")
    private static final String LOGGING = """
            package org.example.logging;
            import org.slf4j.Logger;
            import org.slf4j.LoggerFactory;
            public class Worker {
                private static final Logger LOG = LoggerFactory.getLogger(Worker.class);
                void work(String output, int count, Exception e) {
                    LOG.debug("LLM output: {}", output);
                    LOG.warn("{} of {} failed {", count, output.length());
                    LOG.error("Ignored error", e);
                    LOG.warn("Failed {}", output, e);
                    if (LOG.isDebugEnabled()) LOG.info("done");
                }
            }
            """;

    /**
     * slf4j as Microsoft.Extensions.Logging, the default: a message's {} holes are named after their arguments, a
     * trailing exception is the first argument; as Serilog, the option.
     */
    @Test
    public void logging() {
        String cs = translate("Worker", LOGGING);
        contains(cs, "private static readonly ILogger LOG = JavaLogging.GetLogger(typeof(Worker));");
        contains(cs, "LOG.LogDebug(\"LLM output: {Output}\", output);");
        contains(cs, "LOG.LogWarning(\"{Count} of {Length} failed {{\", count, output.Length);");
        contains(cs, "LOG.LogError(e, \"Ignored error\");");
        contains(cs, "LOG.LogWarning(e, \"Failed {Output}\", output);");
        contains(cs, "if (LOG.IsEnabled(LogLevel.Debug)) { LOG.LogInformation(\"done\"); }");
    }

    @Test
    public void loggingSerilog() {
        policy = CSharpProgram.Policy.DEFAULT.withLibraries(CSharpLibraries.choose("slf4j=serilog"));
        String serilog = translate("WorkerSerilog", LOGGING);
        contains(serilog, "using Serilog;");
        contains(serilog, "private static readonly ILogger LOG = Log.ForContext(typeof(Worker));");
        contains(serilog, "LOG.Warning(e, \"Failed {Output}\", output);");
        contains(serilog, "if (LOG.IsEnabled(LogEventLevel.Debug)) { LOG.Information(\"done\"); }");
    }

    @Language("java")
    private static final String JSON = """
            package org.example.json;
            import com.fasterxml.jackson.annotation.JsonInclude;
            import com.fasterxml.jackson.annotation.JsonProperty;
            import com.fasterxml.jackson.annotation.JsonSubTypes;
            import com.fasterxml.jackson.annotation.JsonTypeInfo;
            import com.fasterxml.jackson.core.JsonProcessingException;
            import com.fasterxml.jackson.databind.JsonNode;
            import com.fasterxml.jackson.databind.ObjectMapper;
            public class Codec {
                @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
                @JsonSubTypes({@JsonSubTypes.Type(value = Text.class, name = "text"),
                               @JsonSubTypes.Type(value = Image.class, name = "image")})
                public static abstract class Content { }
                @JsonInclude(JsonInclude.Include.NON_NULL)
                public static class Text extends Content { @JsonProperty("body") public String text; }
                public static class Image extends Content { }
                private final ObjectMapper mapper = new ObjectMapper();
                Content read(String json) throws JsonProcessingException { return mapper.readValue(json, Content.class); }
                String write(Content c) throws JsonProcessingException { return mapper.writeValueAsString(c); }
                int year(JsonNode node) { return node.isObject() ? node.get("year").asInt() : 0; }
            }
            """;

    /** Jackson as System.Text.Json, the default; as Json.NET, the option. */
    @Test
    public void json() {
        String cs = translate("Codec", JSON);
        contains(cs, "[JsonPolymorphic(TypeDiscriminatorPropertyName = \"type\")]");
        contains(cs, "[JsonDerivedType(typeof(Text), \"text\")] [JsonDerivedType(typeof(Image), \"image\")]");
        contains(cs, "[JsonPropertyName(\"body\")]");
        contains(cs, "private readonly JsonSerializerOptions mapper = new JsonSerializerOptions();");
        contains(cs, "JsonSerializer.Deserialize<Content>(json, mapper)");
        contains(cs, "JsonSerializer.Serialize(c, mapper)");
        contains(cs, "(node is JsonObject) ? node[\"year\"].GetValue<int>() : 0");
        contains(cs, "public sealed class Text : Content {");
    }

    @Test
    public void jsonNewtonsoft() {
        policy = CSharpProgram.Policy.DEFAULT.withLibraries(CSharpLibraries.choose("jackson=newtonsoft"));
        String newtonsoft = translate("CodecNewtonsoft", JSON);
        contains(newtonsoft, "private readonly JsonSerializerSettings mapper = new JsonSerializerSettings();");
        contains(newtonsoft, "JsonConvert.DeserializeObject<Content>(json, mapper)");
        contains(newtonsoft, "[JsonProperty(\"body\")]");
        contains(newtonsoft, "[JsonObject(ItemNullValueHandling = NullValueHandling.Ignore)]");
        contains(newtonsoft, "(node.Type == JTokenType.Object) ? node[\"year\"].Value<int>() : 0");
    }

    @Language("java")
    private static final String PROPERTIES = """
            package org.example.properties;
            public class Person {
                private String name;
                private final int age;
                Person(int age) { this.age = age; }
                public String getName() { return name; }
                public void setName(String name) { this.name = name; }
                int getAge() { return age; }
                public boolean isAdult() { return age >= 18; }
            }
            class Greeter {
                String greet(Person p) {
                    p.setName("Ann");
                    return p.getName() + (p.isAdult() ? "" : " (" + p.getAge() + ")");
                }
            }
            """;

    /**
     * A setter of the getter's field is the property's set; the property is as visible as its getter; a method that
     * computes is no getter. Calls are the property's reads and assignments.
     */
    @Test
    public void properties() {
        String cs = translate("Person", PROPERTIES);
        contains(cs, "public string Name { get; set; }");
        contains(cs, "internal int Age { get; }");
        contains(cs, "internal Person(int age) { this.Age = age; }");
        contains(cs, "public bool IsAdult() => Age >= 18;");
        contains(cs, "p.Name = \"Ann\";");
        contains(cs, "return p.Name + (p.IsAdult() ? \"\" : \" (\" + p.Age + \")\");");
    }

    @Language("java")
    private static final String STORE = """
            package org.example.store;
            import java.util.Objects;
            public class Store {
                private final Object[] values = new Object[4];
                @SuppressWarnings("unchecked")
                <V> V get(int i) { return (V) values[i]; }
                String name() {
                    String s = get(0);
                    return Objects.requireNonNullElseGet(s, () -> {
                        String fallback = "x";
                        return fallback + values.length;
                    });
                }
            }
            """;

    /**
     * A method type parameter only the result mentions gets Java's inferred type argument (CS0411); a supplier with a
     * block body is called through its delegate where a template calls it.
     */
    @Test
    public void inferredAndSupplied() {
        String cs = translate("Store", STORE);
        contains(cs, "string s = Get<string>(0);");
        contains(cs, "s ?? ((Func<string>) (() => {");
        contains(cs, "}))();");
    }

    private static final String SELECTORS = """
            package org.example.selectors;
            import java.util.ArrayList;
            import java.util.List;
            public class Selectors {
                interface Selector { boolean isNeverEnding(); }
                abstract static class AbstractSelector implements Selector {
                    protected final String name = getClass().getSimpleName();
                    protected final List<String> seen = new ArrayList<>();
                }
                abstract static class AbstractDemandEnabledSelector extends AbstractSelector {
                    @Override
                    public abstract boolean equals(Object other);
                    @Override
                    public abstract int hashCode();
                }
                static final class FromSolution extends AbstractDemandEnabledSelector {
                    private final boolean random;
                    FromSolution(boolean random) { this.random = random; }
                    public boolean isNeverEnding() { return random; }
                    public boolean equals(Object other) { return other == this; }
                    public int hashCode() { return 1; }
                }
            }
            """;

    /**
     * An interface's method that an abstract class leaves to its subclasses is declared abstract once, by the first
     * abstract class; an initializer that uses this runs in the constructor, as Java's does.
     */
    @Test
    public void abstractAndInitializers() {
        String cs = translate("Selectors", SELECTORS);
        assertEquals(1, cs.split("public abstract bool NeverEnding", -1).length - 1, cs);
        contains(cs, "protected internal readonly string name;");
        contains(cs, "this.name = this.GetType().Name;");
        contains(cs, "protected AbstractSelector() {");
        contains(cs, "this.seen = new List<string>();");
    }

    private static final String INNER = """
            package org.example.inner;
            import java.util.Iterator;
            public class Range implements Iterable<Long> {
                private final long from;
                private final long to;
                public Range(long from, long to) { this.from = from; this.to = to; }
                public Iterator<Long> iterator() { return new Upcoming(); }
                long width() { return to - from; }
                Upcoming twice(Range other) { return other.new Upcoming(); }
                class Upcoming implements Iterator<Long> {
                    private long next = from;
                    public boolean hasNext() { return next < to; }
                    public Long next() { return next++; }
                    long left() { return width() - (next - Range.this.from); }
                    Upcoming copy() { return new Upcoming(); }
                }
                class Stepped extends Upcoming {
                    private final long step;
                    Stepped(long step) { this.step = step; }
                    long step() { return step; }
                }
                class Plain {
                    int one() { return 1; }
                }
            }
            """;

    /** A Java inner class's enclosing instance is C#'s field outer, which its constructors take first. */
    @Test
    public void innerClasses() {
        String cs = translate("Range", INNER);
        // created with this in the enclosing type's code, with the scope of other.new Upcoming()
        contains(cs, "public IJavaIterator<long> Iterator() => new Upcoming(this);");
        contains(cs, "internal Upcoming Twice(Range other) => new Upcoming(other);");
        // the field, the default constructor that takes it, and the initializer that uses it, moved after it
        contains(cs, "private readonly Range outer;");
        contains(cs, "this.outer = outer;");
        contains(cs, "this.next = outer.from;");
        assertFalse(cs.contains("private long next = outer.from;"), cs);
        // the enclosing instance's members, and Range.this
        contains(cs, "public bool HasNext() => next < outer.to;");
        contains(cs, "internal long Left() => outer.Width() - (next - outer.from);");
        // created with the enclosing instance from an inner class's code; passed on to the superclass
        contains(cs, "internal Upcoming Copy() => new Upcoming(outer);");
        contains(cs, "internal Stepped(Range outer, long step) : base(outer) {");
        // an inner class that does not use its enclosing instance does not take it
        contains(cs, "internal sealed class Plain { internal int One() => 1; }");
    }

    private static final String FACTS = """
            package org.example.facts;
            import java.util.ArrayList;
            import java.util.HashMap;
            import java.util.List;
            import java.util.Map;
            public class Catalog {
                private final List<String> names = new ArrayList<>();
                private String description;
                private List<String> stored;
                int score(List<String> items, Map<String, Integer> weights) {
                    int n = 0;
                    for (String s : items) {
                        if (items.contains(s) && weights.get(s) != null) n += weights.get(s);
                    }
                    return n + items.indexOf("x");
                }
                void addAll(List<String> more) { names.addAll(more); }
                void keep(List<String> list) { this.stored = list; }
                String find(String key) {
                    String d = description;
                    return key.isEmpty() ? null : d;
                }
                int size(Map<String, Integer> table) { return table.size(); }
                int sizeOf(Map<String, Integer> given) { return size(given); }
                int width(Map<String, Integer> sized) { return sized.size(); }
                int fresh() { return width(new HashMap<>()); }
                int length() { return find("k").length() + description.length(); }
                void collect(List<String> out) { out.add(find("k")); out.add(description); }
                boolean has(List<String> l) { return l.contains(description) || l.contains(null); }
                String[] pair() {
                    String[] a = { description, null };
                    a[1] = null;
                    return a;
                }
                String either(boolean b) { return b ? description : null; }
                Object cast() { return (CharSequence) find("k"); }
                int unbox(Map<String, Integer> m, List<Integer> l, Integer boxed, Integer checked) {
                    int a = m.get("k");
                    int b = l.get(0);
                    int c = boxed;
                    return checked == null ? a : a + b + c + checked;
                }
                void put(Map<Integer, String> byId, Integer id) { byId.put(id, description); }
                static <T> T ensure(T maybe) {
                    if (maybe == null) throw new IllegalArgumentException();
                    return maybe;
                }
                void over(String s) { }
                void over(Integer i) { }
                void callOver() { over((String) null); }
                int printed(Object o) { return o.toString().length(); }
                static class Holder<T> {
                    T held;
                    T held() { return held; }
                }
            }
            """;

    /** The analysis's verdicts as a client supplies them: these parameters unmodified, these declarations nullable. */
    private static final CSharpFacts CATALOG_FACTS = new CSharpFacts() {
        @Override
        public boolean unmodified(io.codelaser.maddi.cst.api.info.ParameterInfo parameter) {
            return java.util.Set.of("items", "weights", "more", "list", "table", "sized").contains(parameter.name());
        }

        @Override
        public io.codelaser.maddi.cst.api.type.NullableState nullable(io.codelaser.maddi.cst.api.info.ParameterInfo parameter) {
            return "maybe".equals(parameter.name()) ? io.codelaser.maddi.cst.api.type.NullableState.NULLABLE
                    : io.codelaser.maddi.cst.api.type.NullableState.UNSPECIFIED;
        }

        @Override
        public io.codelaser.maddi.cst.api.type.NullableState nullable(io.codelaser.maddi.cst.api.info.FieldInfo field) {
            return "description".equals(field.name()) || "held".equals(field.name())
                    ? io.codelaser.maddi.cst.api.type.NullableState.NULLABLE
                    : io.codelaser.maddi.cst.api.type.NullableState.UNSPECIFIED;
        }

        @Override
        public io.codelaser.maddi.cst.api.type.NullableState nullable(io.codelaser.maddi.cst.api.info.MethodInfo method) {
            // Map.get as an annotated JDK has it
            return "find".equals(method.name()) || "get".equals(method.name())
                                                   && "java.util.Map".equals(method.typeInfo().fullyQualifiedName())
                    ? io.codelaser.maddi.cst.api.type.NullableState.NULLABLE
                    : io.codelaser.maddi.cst.api.type.NullableState.UNSPECIFIED;
        }
    };

    /**
     * A collection parameter the method does not modify is read-only, when its uses allow (a stored one is not); the
     * calls on it are a read-only collection's. A declaration that may be null is T?.
     */
    @Test
    public void facts() {
        policy = CSharpProgram.Policy.DEFAULT.withFacts(CATALOG_FACTS);
        String cs = translate("Catalog", FACTS);
        contains(cs, "using System.Linq;");
        contains(cs, "private string? description;");
        contains(cs, "internal int Score(IReadOnlyList<string> items, IReadOnlyDictionary<string, int> weights) {");
        contains(cs, "if (items.Contains(s) && (weights.ContainsKey(s) ? weights[s] : (int?) null) != null)");
        contains(cs, "return n + JavaCollections.IndexOf(items, \"x\");");
        contains(cs, "internal void AddAll(IReadOnlyList<string> more)");
        contains(cs, "internal void Keep(List<string> list) { this.stored = list; }");
        contains(cs, "internal string? Find(string key)");
        // a caller's IDictionary is no IReadOnlyDictionary; a Dictionary is
        contains(cs, "internal int Size(IDictionary<string, int> table)");
        contains(cs, "internal int Width(IReadOnlyDictionary<string, int> sized)");
        contains(cs, "string d = description;");
    }

    /**
     * With the program's nullability analysed, C# checks it: a local's reference type is T?, as var's is, and a
     * dereference of a value that may be null has a {@code !}.
     */
    @Test
    public void nullabilityAnalysed() {
        policy = CSharpProgram.Policy.DEFAULT.withFacts(new CSharpFacts() {
            @Override
            public io.codelaser.maddi.cst.api.type.NullableState nullable(io.codelaser.maddi.cst.api.info.FieldInfo field) {
                return CATALOG_FACTS.nullable(field);
            }

            @Override
            public io.codelaser.maddi.cst.api.type.NullableState nullable(io.codelaser.maddi.cst.api.info.MethodInfo method) {
                return CATALOG_FACTS.nullable(method);
            }

            @Override
            public io.codelaser.maddi.cst.api.type.NullableState nullable(io.codelaser.maddi.cst.api.info.ParameterInfo parameter) {
                return CATALOG_FACTS.nullable(parameter);
            }

            @Override
            public boolean nullabilityAnalysed() {
                return true;
            }
        });
        String cs = translate("Catalog", FACTS);
        contains(cs, "private string? description;");
        contains(cs, "string? d = description;");
        contains(cs, "int n = 0;");
        // a dereference of a value that may be null: Java throws there, C# too, and ! says so
        contains(cs, "Find(\"k\")!.Length + description!.Length");
        // a value that may be null into a non-null parameter, a list's element here
        contains(cs, "@out.Add(Find(\"k\")!);");
        contains(cs, "@out.Add(description!);");
        // Java's contains(Object) takes null, C#'s Contains(T) not: it is asserted whatever the hints
        contains(cs, "l.Contains(description!) || l.Contains(null!)");
        // an array's elements are not nullable in C#: the null they hold, as in Java, is asserted
        contains(cs, "{ description!, null! }");
        contains(cs, "a[1] = null!;");
        // a method whose result the analysis does not find nullable
        contains(cs, "(b ? description : null)!");
        // a cast keeps the null of a value that may be null
        contains(cs, "(string?) Find(\"k\")");
        // unboxing throws on null, in C# as in Java: an Integer is C#'s int? whatever its declaration's verdict
        contains(cs, "int a = (int) m.GetValueOrNull(\"k\")!;");
        contains(cs, "int b = (int) l[0];");
        contains(cs, "int c = (int) boxed!;");
        // a boxed key is unboxed into Dictionary<int, string>'s K: its cast asserts it, once
        contains(cs, "byId[(int) id!] = description!;");
        // a nullable parameter of a type parameter's type: T is inferred without ?, the result is not null
        contains(cs, "internal static T Ensure<T>(T? maybe) {");
        // a cast of null, which picks an overload, keeps the null
        contains(cs, "Over((string?) null);");
        // C#'s object.ToString() is string?
        contains(cs, "o.ToString()!.Length");
        // a class's type parameter takes the ? of a nullable declaration
        contains(cs, "internal T? Held { get; }");
    }
}
