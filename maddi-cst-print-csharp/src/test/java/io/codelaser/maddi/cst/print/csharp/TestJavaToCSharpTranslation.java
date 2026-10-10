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
        contains(cs, "public class Counter {");
        contains(cs, "private int count;");
        contains(cs, "private readonly string name;");
        contains(cs, "public const int LIMIT = 10;");
        contains(cs, "public Counter(string name) { this.name = name; }");
        contains(cs, "public virtual int GetCount() => count;");
        contains(cs, "} else if (count == LIMIT) {");
        contains(cs, "public string Name() => name;");
        contains(cs, "public override string ToString() => name + \":\" + count;");
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
        contains(cs, "public virtual double Area() => Unit() * scale;");
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
        contains(cs, "internal virtual Key<Number> Raw() => null;");
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
}
