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
 * Step 3 of the Java-to-Kotlin translation: where Java and Kotlin put things differently. Statics, constructors,
 * records, nested classes, pattern variables, the JDK types Kotlin maps to its own, functional interfaces, and the
 * implicit conversions Java does and Kotlin does not.
 */
public class TestJavaToKotlinStructure extends CommonJavaToKotlin {

    private static void contains(String kotlin, String expected) {
        assertTrue(strip(kotlin).contains(strip(expected)), () -> "expected\n" + expected + "\nin\n" + kotlin);
    }

    private static String strip(String s) {
        return s.lines().map(String::strip).collect(Collectors.joining("\n"));
    }

    @Language("java")
    private static final String STATICS = """
            package a;
            class Base {
                public static final int TYPE_INT = 4;
                protected static int helper(int x) { return x + 1; }
            }
            class C extends Base {
                static final String NAME = "c";
                private static int count;
                static { count = 1; }
                int m(String s) { return TYPE_INT + helper(count) + Integer.parseInt(s) + NAME.length(); }
            }
            """;

    /** Kotlin inherits no statics: they go to a companion object, and one inherited from a supertype is qualified. */
    @Test
    public void staticsInCompanion() {
        String kotlin = kotlin(STATICS);
        contains(kotlin, "const val TYPE_INT: Int = 4");
        contains(kotlin, "@JvmStatic protected fun helper(x: Int): Int = x + 1");
        contains(kotlin, """
                companion object {
                    const val NAME: String = "c"
                    private var count: Int = 0
                    init { count = 1 }
                }
                """);
        contains(kotlin, "Base.TYPE_INT + Base.helper(count) + java.lang.Integer.parseInt(s) + NAME.length");
    }

    @Language("java")
    private static final String CONSTRUCTORS = """
            package a;
            class Base { Base(int x) { } }
            class C extends Base {
                private final int size;
                private final String label;
                private final int doubled = size * 2;
                C(int size, String name) {
                    super(size + 1);
                    this.size = size;
                    label = name.trim();
                }
                class Inner { int get() { return size; } }
            }
            record Point(int x, int y) {
                Point { if (x < 0) throw new IllegalArgumentException(); }
                int sum() { return x() + y; }
            }
            """;

    /**
     * One constructor is the primary one, its body an {@code init} block, which may assign vals; the parameters are in
     * scope in property initializers, where the Java meant the field. A record is a data class.
     */
    @Test
    public void constructorsAndRecords() {
        String kotlin = kotlin(CONSTRUCTORS);
        contains(kotlin, "open class C(size: Int, name: String) : Base(size + 1) {");
        contains(kotlin, "private val doubled: Int = this.size * 2");
        contains(kotlin, """
                init {
                    this.size = size
                    label = name.trim()
                }
                """);
        contains(kotlin, "open inner class Inner {");
        contains(kotlin, "data class Point(val x: Int, val y: Int) {");
        contains(kotlin, "init { if (x < 0) {"); // the compact constructor's body
        contains(kotlin, "throw IllegalArgumentException() } }");
        contains(kotlin, "fun sum(): Int = x + y");
        assertFalse(kotlin.contains("fun x()"), kotlin);
    }

    @Language("java")
    private static final String PATTERNS = """
            package a;
            class C {
                Object f;
                int m(Object o) {
                    if (o instanceof String s && !s.isEmpty()) return s.length();
                    if (f instanceof String t) return t.length();
                    String s = "local";
                    return s.length();
                }
            }
            """;

    /**
     * {@code s} is {@code o}, smart-cast; a field is not smart-cast, so {@code t} is a cast. A later local of the same
     * name is itself again.
     */
    @Test
    public void patternVariables() {
        String kotlin = kotlin(PATTERNS);
        contains(kotlin, "if (o is String && !o.isEmpty()) { return o.length }");
        contains(kotlin, "if (f is String) { return (f as String).length }");
        contains(kotlin, """
                val s = "local"
                return s.length
                """);
    }

    @Language("java")
    private static final String MAPPED = """
            package a;
            import java.util.*;
            class C {
                int m(List<String> list, Map<String, Integer> map, String s) {
                    List<String> copy = new ArrayList<>(list);
                    copy.remove(0);
                    int n = 0;
                    for (Map.Entry<String, Integer> e : map.entrySet()) n += e.getKey().length() + e.getValue();
                    return n + copy.size() + s.charAt(0) + s.indexOf('x') + s.replaceAll("a+", "b").length();
                }
            }
            """;

    /** The JDK types Kotlin maps: mutable collection interfaces, and the Kotlin member for the Java one. */
    @Test
    public void mappedTypes() {
        String kotlin = kotlin(MAPPED);
        assertFalse(kotlin.contains("import java.util.List"), kotlin);
        contains(kotlin, "open fun m(list: MutableList<String>, map: MutableMap<String, Int>, s: String): Int {");
        contains(kotlin, "val copy: MutableList<String> = ArrayList<String>(list)");
        contains(kotlin, "copy.removeAt(0)");
        contains(kotlin, "for (e in map.entries) { n += e.key.length + e.value }");
        contains(kotlin, "copy.size + s[0].code + s.indexOf('x') + s.replace(\"a+\".toRegex(), \"b\").length");
    }

    @Language("java")
    private static final String FUNCTIONAL = """
            package a;
            interface Visitor { int visit(String s); }
            class C {
                short code;
                int run(Visitor v) { return v.visit("x"); }
                int m(int p) {
                    p = p + 1;
                    Visitor v = s -> s.length() + p0();
                    long total = code;
                    if (code == p) total++;
                    return run(v) + (int) total;
                }
                int p0() { return 0; }
                @Override public boolean equals(Object o) { return o == this; }
            }
            enum E { A, B }
            class D {
                void sw(E e) { switch (e) { case A: System.out.println(); } }
            }
            """;

    /**
     * A functional interface is a {@code fun interface}, and a lambda names it; a reassigned parameter is a local
     * var; Java's implicit widening is explicit; a {@code when} over an enum is exhaustive; equals takes {@code Any?}.
     */
    @Test
    public void conversions() {
        String kotlin = kotlin(FUNCTIONAL);
        contains(kotlin, "fun interface Visitor {");
        contains(kotlin, "val v: Visitor = Visitor { s -> s.length + p0() }");
        contains(kotlin, """
                open fun m(p: Int): Int {
                    var p = p
                    p = p + 1
                """);
        contains(kotlin, "var total: Long = code.toLong()");
        contains(kotlin, "if (code.toInt() == p) { total++ }");
        contains(kotlin, "override fun equals(o: Any?): Boolean = o === this");
        contains(kotlin, "else -> { }");
    }

    @Language("java")
    private static final String LAMBDA_PARAMETER = """
            package a;
            import java.util.*;
            class C {
                void m(Map<String, List<String>> map) {
                    map.compute("k", (k, v) -> { if (v == null) v = new ArrayList<>(); v.add(k); return v; });
                }
            }
            """;

    /** A lambda parameter the body assigns is a `var` copy, as a method's: Kotlin's lambda parameters are vals. */
    @Test
    public void reassignedLambdaParameter() {
        contains(kotlin(LAMBDA_PARAMETER), """
                { k, v ->
                var v = v
                if (v == null) {""");
    }

    @Language("java")
    private static final String VALS = """
            package a;
            import java.util.function.Supplier;
            class C {
                private int fixed = 1;
                private int counted = 0;
                private int byAnonymous = 0;
                int visible = 2;
                int m(int p) {
                    int a = p + 1;
                    int b = 0;
                    b += a;
                    int c;
                    c = 3;
                    Supplier<Integer> s = () -> { int x = 1; x++; int y = 2; return x + y; };
                    Runnable r = new Runnable() { public void run() { byAnonymous = 1; } };
                    counted++;
                    return a + b + c + fixed + s.get();
                }
            }
            """;

    /**
     * A variable that is never assigned after its declaration is a {@code val}: a local in its own body (a lambda's
     * included), a private field with an initializer anywhere in its compilation unit (anonymous classes included).
     */
    @Test
    public void vals() {
        String kotlin = kotlin(VALS);
        contains(kotlin, "private val fixed: Int = 1");
        contains(kotlin, "private var counted: Int = 0");
        contains(kotlin, "private var byAnonymous: Int = 0");
        contains(kotlin, "var visible: Int = 2"); // not private: another class may assign it
        contains(kotlin, "val a = p + 1");
        contains(kotlin, "var b = 0");
        contains(kotlin, "var c: Int"); // declared without a value: which assignment comes first is flow
        contains(kotlin, "var x = 1");
        contains(kotlin, "val y = 2");
    }
}
