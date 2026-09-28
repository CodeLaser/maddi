package io.codelaser.maddi.modification.analyzer.kotlin;

import io.codelaser.maddi.cst.api.analysis.Value;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.impl.analysis.PropertyImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/*
 Method-level verdicts: a fluent builder (with and without a scope function), an identity method, modification of a
 parameter and of a parameter's field, a Nothing-returning function, recursion, and stdlib calls on a parameter.
 */
public class TestKotlinAnalyzerMethods extends CommonKotlinAnalyzerTest {

    @Language("kotlin")
    private static final String KOTLIN = """
            package k
            class Builder {
                private val sb = StringBuilder()
                fun add(s: String): Builder { sb.append(s); return this }
                fun addAll(xs: List<String>): Builder = apply { xs.forEach { sb.append(it) } }
                fun addEach(xs: List<String>): Builder { xs.forEach { sb.append(it) }; return this }
                fun addLoop(xs: List<String>): Builder { for (x in xs) sb.append(x); return this }
                fun build(): String = sb.toString()
            }
            class Ops {
                fun <T> id(t: T): T = t
                fun addTo(xs: MutableList<String>, s: String) { xs.add(s) }
                fun clearBox(b: Box) { b.items.clear() }
                fun readBox(b: Box): Int = b.items.size
                fun fail(msg: String): Nothing = throw IllegalStateException(msg)
                fun fact(n: Int): Int = if (n <= 1) 1 else n * fact(n - 1)
                fun sortIt(xs: MutableList<String>) { xs.sort() }
                fun sumOf(xs: List<Int>): Int = xs.sum()
            }
            class Box(val items: MutableList<String>)
            """;

    @Language("java")
    private static final String JAVA = """
            package j;
            import java.util.List;
            final class Builder {
                private final StringBuilder sb = new StringBuilder();
                Builder add(String s) { sb.append(s); return this; }
                Builder addAll(List<String> xs) { for (String x : xs) sb.append(x); return this; }
                Builder addEach(List<String> xs) { xs.forEach(x -> sb.append(x)); return this; }
                Builder addLoop(List<String> xs) { for (String x : xs) sb.append(x); return this; }
                String build() { return sb.toString(); }
            }
            final class Ops {
                <T> T id(T t) { return t; }
                void addTo(List<String> xs, String s) { xs.add(s); }
                void clearBox(Box b) { b.getItems().clear(); }
                int readBox(Box b) { return b.getItems().size(); }
                Void fail(String msg) { throw new IllegalStateException(msg); }
                int fact(int n) { return n <= 1 ? 1 : n * fact(n - 1); }
                void sortIt(List<String> xs) { java.util.Collections.sort(xs); }
                int sumOf(List<Integer> xs) { int s = 0; for (int x : xs) s += x; return s; }
            }
            final class Box { private final List<String> items; Box(List<String> items) { this.items = items; } List<String> getItems() { return items; } }
            """;

    private static Analyzed a;

    @BeforeAll
    public static void beforeAll() {
        a = analyze(KOTLIN, JAVA);
    }

    private static String flags(MethodInfo m) {
        return m.name() + " fluent=" + m.analysis().getOrNull(PropertyImpl.FLUENT_METHOD, Value.class)
               + " identity=" + m.analysis().getOrNull(PropertyImpl.IDENTITY_METHOD, Value.class);
    }

    @Test
    public void fluent() {
        a.assertSameAsJava("Builder", "sb", "<init>", "add", "addEach", "addLoop", "build");
        for (String name : new String[]{"add", "addAll", "addEach", "addLoop", "build"}) {
            assertEquals(flags(method(a.javaType("Builder"), name)), flags(method(a.kotlinType("Builder"), name)), name);
        }
        assertEquals("add fluent=true identity=false", flags(method(a.kotlinType("Builder"), "add")));
    }

    /*
     ⛔ maddi#88: inside `apply { }`, `sb` is `$receiver.sb`, a modification of the lambda's parameter, which the
     engine does not carry back to the receiver. The method modifies `this` and reads as non-modifying: unsound.
     The same body without the scope function (addEach) is right.
     */
    @Test
    public void applyBuilder() {
        assertEquals("method addAll: nonModifying=false @Independent | 0: unmodified=true @Independent",
                a.java("Builder", "addAll").lines().skip(1).findFirst().orElseThrow());
        assertEquals("method addAll: nonModifying=true @Independent | 0: unmodified=true @Independent",
                a.kotlin("Builder", "addAll").lines().skip(1).findFirst().orElseThrow());
    }

    @Test
    public void identity() {
        assertEquals("id fluent=false identity=true", flags(method(a.kotlinType("Ops"), "id")));
        a.assertSameAsJava("Ops", "id");
    }

    @Test
    public void parameterModification() {
        a.assertSameAsJava("Ops", "addTo", "clearBox", "readBox", "sortIt");
        a.assertSameAsJava("Box");
    }

    @Test
    public void nothingAndRecursion() {
        a.assertSameAsJava("Ops", "fail", "fact");
    }

    /* ⛔ maddi#89: Iterable<Int>.sum() (sumOfInt) has no contract, so a read-only call modifies its argument */
    @Test
    public void stdlibSum() {
        assertEquals("method sumOf: nonModifying=true @Independent | 0: unmodified=true @Independent",
                a.java("Ops", "sumOf").lines().skip(1).findFirst().orElseThrow());
        assertEquals("method sumOf: nonModifying=true @Independent | 0: unmodified=false @Independent",
                a.kotlin("Ops", "sumOf").lines().skip(1).findFirst().orElseThrow());
    }
}
