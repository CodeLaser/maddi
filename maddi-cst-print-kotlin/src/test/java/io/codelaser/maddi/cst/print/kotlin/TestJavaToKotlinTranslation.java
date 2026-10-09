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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Java forms that have no one-to-one Kotlin counterpart, one test per translation rule. The corpus-scale check,
 * which also compiles the output, is maddi-run-openjdk's TestJavaToKotlinFernflower.
 */
public class TestJavaToKotlinTranslation extends CommonJavaToKotlin {

    /** Line by line, indentation ignored: the depth of a fragment is not what these tests are about. */
    private static void contains(String kotlin, String expected) {
        assertTrue(strip(kotlin).contains(strip(expected)), () -> "expected\n" + expected + "\nin\n" + kotlin);
    }

    private static String strip(String s) {
        return s.lines().map(String::strip).collect(java.util.stream.Collectors.joining("\n"));
    }

    @Language("java")
    private static final String SUPER_CALL = """
            package a;
            class B { public String toString() { return "b"; } }
            class C extends B {
                @Override public String toString() { return super.toString() + "c"; }
            }
            """;

    /** #103: {@code super.m()} lost its {@code super.} and became a call to the overriding method itself. */
    @Test
    public void superCall() {
        contains(kotlin(SUPER_CALL), "override fun toString(): String = super.toString() + \"c\"");
    }

    @Language("java")
    private static final String TRY_WITH_RESOURCES = """
            package a;
            import java.io.*;
            class C {
                int one(File f) throws IOException {
                    try (InputStream in = new FileInputStream(f); Reader r = new InputStreamReader(in)) {
                        return r.read();
                    }
                }
                void caught(File f) {
                    try (InputStream s = new FileInputStream(f)) {
                        s.read();
                    } catch (IllegalStateException | IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            """;

    /** #104: the resources were dropped, so nothing declared or closed them. Multi-catch: one clause per type. */
    @Test
    public void tryWithResources() {
        String kotlin = kotlin(TRY_WITH_RESOURCES);
        contains(kotlin, "FileInputStream(f).use { `in` -> InputStreamReader(`in`).use { r -> return r.read() } }");
        contains(kotlin, "try { FileInputStream(f).use { s -> s.read() } } catch (e: IllegalStateException) {");
        contains(kotlin, "catch (e: IOException) {");
    }

    @Language("java")
    private static final String CASTS = """
            package a;
            class C {
                int m(long l, double d, char ch, Object o) {
                    int i = (int) l;
                    char next = (char) (ch + 1);
                    int code = (int) ch;
                    byte b = (byte) d;
                    String s = (String) o;
                    return i + code + b + next + s.length();
                }
            }
            """;

    /** #105: {@code x as Int} on a Long throws where Java's {@code (int) x} truncates; primitive casts convert. */
    @Test
    public void primitiveCasts() {
        String kotlin = kotlin(CASTS);
        contains(kotlin, "val i = l.toInt()");
        contains(kotlin, "val next = (ch.code + 1).toChar()"); // Java's ch + 1 is an int
        contains(kotlin, "val code = ch.code");
        contains(kotlin, "val b = d.toInt().toByte()");
        contains(kotlin, "val s = o as String");
    }

    @Language("java")
    private static final String FOR_LOOPS = """
            package a;
            class C {
                int m(int[] a, int n) {
                    int s = 0;
                    for (int i = 0; i < a.length; i++) s += a[i];
                    for (int i = n; i >= 0; i -= 2) s += i;
                    for (int i = 0; i < n; i++) { n--; }
                    for (int i = 0, j = 10; i < j; i++, j--) { if (i == 3) continue; s += i * j; }
                    return s;
                }
            }
            """;

    /**
     * A range only when it is the same loop; a bound the body changes ({@code n--}) is not, because a range reads it
     * once. A {@code continue} must still run the updates.
     */
    @Test
    public void forLoops() {
        String kotlin = kotlin(FOR_LOOPS);
        contains(kotlin, "for (i in 0 until a.size) {");
        contains(kotlin, "for (i in n downTo 0 step 2) {");
        contains(kotlin, """
                        when { else -> {
                            var i = 0
                            while (i < n) {
                                n--
                                i++
                            }
                        } }
                """);
        contains(kotlin, """
                            var first1 = true
                            while (true) {
                                if (first1) first1 = false else {
                                    i++
                                    j--
                                }
                                if (!(i < j)) break
                                if (i == 3) { continue }
                """);
    }

    @Language("java")
    private static final String OLD_SWITCH = """
            package a;
            enum Color { RED, GREEN }
            class C {
                String m(int k, Color c) {
                    String r = "";
                    switch (k) {
                        case 1: r = "one";
                        case 2: r += "two"; break;
                        case 3: if (r.isEmpty()) break; r = "three"; break;
                        default: r = "d";
                    }
                    switch (c) { case RED: return "r"; case GREEN: return "g"; }
                    return r;
                }
            }
            """;

    /**
     * Fall-through copies the next case's statements; a case's final {@code break} goes; a {@code break} in the
     * middle leaves a {@code run label@{ }}, since a Kotlin {@code break} in a {@code when} leaves the enclosing loop.
     */
    @Test
    public void oldStyleSwitch() {
        String kotlin = kotlin(OLD_SWITCH);
        contains(kotlin, """
                        1 -> {
                            r = "one"
                            r += "two"
                        }
                """);
        contains(kotlin, "2 -> { r += \"two\" }");
        contains(kotlin, "run switch1@ { when (k) {");
        contains(kotlin, "if (r.isEmpty()) { return@switch1 }");
        contains(kotlin, "Color.RED -> { return \"r\" }");
        assertFalse(kotlin.contains("break"), kotlin);
    }

    @Language("java")
    private static final String LAMBDAS = """
            package a;
            import java.util.List;
            class C {
                void m(List<String> list) {
                    Runnable r = () -> { if (list.isEmpty()) return; list.clear(); };
                    java.util.function.Function<String, Integer> f = s -> { int n = s.length(); return n * 2; };
                }
            }
            """;

    /** A bare {@code return} in a lambda returns from the enclosing function in Kotlin; the last one is a value. */
    @Test
    public void lambdaReturns() {
        String kotlin = kotlin(LAMBDAS);
        contains(kotlin, "lambda@ {");
        contains(kotlin, "return@lambda");
        contains(kotlin, """
                        val n = s.length
                        n * 2
                """);
    }

    @Language("java")
    private static final String CONSTRUCTORS = """
            package a;
            class B { B(int x) { } int h() { return 1; } }
            class C extends B {
                private int fun = 3;
                private String name;
                C() { this(1); }
                C(int in) { super(in); fun = in; }
            }
            """;

    /** Delegation goes into the header; hard keywords are escaped; Java's field defaults are written out. */
    @Test
    public void constructorsAndNames() {
        String kotlin = kotlin(CONSTRUCTORS);
        contains(kotlin, "open class C : B {");
        contains(kotlin, "private var `fun`: Int = 3");
        contains(kotlin, "private lateinit var name: String");
        contains(kotlin, "constructor() : this(1)");
        contains(kotlin, "constructor(`in`: Int) : super(`in`) { `fun` = `in` }");
        contains(kotlin, "open fun h(): Int = 1");
    }

    @Language("java")
    private static final String LONG_EXPRESSION = """
            package a;
            class C {
                static final int ACC_PUBLIC = 1, ACC_PROTECTED = 2, ACC_PRIVATE = 4, ACC_ABSTRACT = 8, ACC_STATIC = 16;
                static final int ACC_FINAL = 32, ACC_STRICT = 64;
                private static final int CLASS_ALLOWED = ACC_PUBLIC | ACC_PROTECTED | ACC_PRIVATE | ACC_ABSTRACT
                    | ACC_STATIC | ACC_FINAL | ACC_STRICT | ACC_PUBLIC | ACC_PROTECTED | ACC_PRIVATE | ACC_ABSTRACT;
                String describe(String simpleName, String type, String source, int accessFlags, String more) {
                    return simpleName + " " + accessFlags + " " + type + " " + source + " " + more + " " + simpleName
                        + " " + type + " " + source;
                }
            }
            """;

    /** Kotlin ends a statement at a newline in front of an operator, so a long expression breaks after one. */
    @Test
    public void lineBreaksAfterOperators() {
        String kotlin = kotlin(LONG_EXPRESSION);
        assertTrue(kotlin.lines().map(String::trim).noneMatch(l -> l.startsWith("or ") || l.startsWith("+ ")),
                kotlin);
        assertTrue(kotlin.lines().anyMatch(l -> l.endsWith(" or") || l.endsWith(" +")), kotlin);
    }

    @Language("java")
    private static final String ARRAY_INITIALIZERS = """
            package a;
            class C {
                static final int[][][] TABLE = { {null, {1, 2}}, null };
                String[][] names = { {"a"}, {} };
            }
            """;

    /** An array initializer takes its type from what it initializes, not from its (possibly null) elements. */
    @Test
    public void arrayInitializers() {
        String kotlin = kotlin(ARRAY_INITIALIZERS);
        contains(kotlin, "@JvmField val TABLE: Array<Array<IntArray>> = arrayOf<Array<IntArray>>(");
        contains(kotlin, "arrayOf<IntArray>(null, intArrayOf(1, 2)),");
        contains(kotlin, "var names: Array<Array<String>> = arrayOf<Array<String>>(arrayOf<String>(\"a\"), arrayOf<String>())");
    }

    @Language("java")
    private static final String YIELD_IN_IF = """
            package a;
            class C {
                String m(int k, Object o) {
                    return switch (k) {
                        case 1 -> {
                            if (o instanceof String str) {
                                yield str;
                            } else if (o instanceof Integer n) {
                                String t = n.toString();
                                yield t;
                            }
                            throw new RuntimeException("type " + o);
                        }
                        case 2 -> {
                            String u = "u";
                            yield u;
                        }
                        default -> "d";
                    };
                }
            }
            """;

    /**
     * A {@code yield} inside an if: Kotlin's block arm takes its last expression as its value, so such an arm is a
     * {@code run { }} that returns from it; an arm that yields only at its end stays a block.
     */
    @Test
    public void yieldBeforeTheEnd() {
        String kotlin = kotlin(YIELD_IN_IF);
        contains(kotlin, "1 -> run {");
        contains(kotlin, "return@run o");
        contains(kotlin, "return@run t");
        contains(kotlin, "throw RuntimeException(");
        contains(kotlin, "val u = \"u\"");
        assertFalse(kotlin.contains("return@run u"), kotlin);
    }

    @Language("java")
    private static final String BOXED_OVERLOAD = """
            package a;
            class P {
                P(int type, Object value) { }
                P(int type, int index) { }
                static P of(int x) { return new P(1, Integer.valueOf(x)); }
                static P at(int x) { return new P(2, x); }
            }
            """;

    /**
     * Java's {@code Integer} argument takes the {@code (int, Object)} constructor; Kotlin types it {@code Int} and
     * would pick {@code (int, int)}: the argument is cast to the parameter's type.
     */
    @Test
    public void boxedArgumentKeepsJavasOverload() {
        String kotlin = kotlin(BOXED_OVERLOAD);
        contains(kotlin, "valueOf(x) as Any)");
        contains(kotlin, "P(2, x)");
    }

    @Language("java")
    private static final String TEST_FIXTURE_SHAPES = """
            package a;
            import java.util.List;
            enum Tool {
                JAVAC("javac") { public String run(String s) { return s + "c"; } },
                ECJ("ecj") { public String run(String s) { return s + "e"; } };
                private final String name;
                Tool(String name) { this.name = name; }
                public abstract String run(String s);
            }
            class Base {
                static List<String> collect(String s) { return List.of(s); }
            }
            class Sub extends Base { }
            class User {
                int count(String s) { return Sub.collect(s).size(); }
                String clean(String s) { return s.replace("\\r", "").stripTrailing(); }
            }
            """;

    /**
     * fernflower's test fixtures: an enum constant with members of its own, a static method called through a
     * subclass (a companion's members are not inherited), and Java's {@code String.stripTrailing()}.
     */
    @Test
    public void testFixtureShapes() {
        String kotlin = kotlin(TEST_FIXTURE_SHAPES);
        contains(kotlin, "JAVAC(\"javac\") {");
        contains(kotlin, "override fun run(s: String): String = s + \"c\"");
        contains(kotlin, "Base.collect(s).size");
        contains(kotlin, "as java.lang.String).stripTrailing()");
    }
}
