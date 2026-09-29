package io.codelaser.maddi.modification.analyzer.kotlin;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/*
 What kotlinc synthesizes and the idioms that compile to it: class delegation (a `$$delegate_0` field and one
 forwarder per interface method), default arguments (`$default`), a private setter, computed and lazy properties,
 top-level functions and extension properties (the file class `TopKt`), an operator, an anonymous object, a string
 template, and the scope functions applied to a field. Every Java twin is the shape kotlinc emits.
 */
public class TestKotlinAnalyzerSynthesized extends CommonKotlinAnalyzerTest {

    @Language("kotlin")
    private static final String KOTLIN = """
            package k
            interface Sink { fun put(s: StringBuilder) }
            class ListSink : Sink { private val all = ArrayList<StringBuilder>(); override fun put(s: StringBuilder) { all.add(s) } }
            class Forward(private val d: Sink) : Sink by d
            class Forward2(d: Sink) : Sink by d
            class Defaults { fun join(a: StringBuilder, b: String = "-"): StringBuilder = a.append(b) }
            class Acc { var name: String = ""
                private set
                fun rename(n: String) { name = n } }
            class Computed(private val xs: MutableList<String>) { val size: Int get() = xs.size; val first: String get() = xs[0] }
            class Lazy { val sb: StringBuilder by lazy { StringBuilder() } }
            """;

    @Language("kotlin")
    private static final String KOTLIN_TOP = """
            package k
            fun joinAll(xs: List<String>): String { val sb = StringBuilder(); for (x in xs) sb.append(x); return sb.toString() }
            fun addTo(xs: MutableList<String>, s: String) { xs.add(s) }
            val List<String>.second: String get() = this[1]
            class Vec(val x: Int) { operator fun plus(o: Vec): Vec = Vec(x + o.x) }
            class Idioms(private val sb: StringBuilder) {
                fun withIt(): Int = with(sb) { append("x"); length }
                fun runIt(): Int = sb.run { append("x"); length }
                fun alsoIt(): StringBuilder = sb.also { it.append("x") }
                fun useIt(r: java.io.BufferedReader): String? = r.use { it.readLine() }
                fun sum(a: Vec, b: Vec): Vec = a + b
                fun anon(): Sink = object : Sink { override fun put(s: StringBuilder) { sb.append(s) } }
                fun template(n: Int): String = "n=$n, sb=$sb"
            }
            """;

    @Language("java")
    private static final String JAVA = """
            package j;
            import java.util.ArrayList;
            import java.util.List;
            interface Sink { void put(StringBuilder s); }
            class ListSink implements Sink { private final ArrayList<StringBuilder> all = new ArrayList<>(); public void put(StringBuilder s) { all.add(s); } }
            final class Forward implements Sink { private final Sink d; private final Sink $$delegate_0; Forward(Sink d) { this.d = d; this.$$delegate_0 = d; } public void put(StringBuilder s) { $$delegate_0.put(s); } }
            final class Forward2 implements Sink { private final Sink $$delegate_0; Forward2(Sink d) { this.$$delegate_0 = d; } public void put(StringBuilder s) { $$delegate_0.put(s); } }
            final class Defaults { StringBuilder join(StringBuilder a, String b) { return a.append(b); } }
            class Acc { private String name = ""; String getName() { return name; } void rename(String n) { name = n; } }
            class Computed { private final List<String> xs; Computed(List<String> xs) { this.xs = xs; } int getSize() { return xs.size(); } String getFirst() { return xs.get(0); } }
            class Lazy { private final kotlin.Lazy<StringBuilder> sb$delegate = kotlin.LazyKt.lazy(StringBuilder::new); StringBuilder getSb() { return sb$delegate.getValue(); } }
            """;

    @Language("java")
    private static final String JAVA_TOP = """
            package j;
            import java.util.List;
            final class TopKt {
                static String joinAll(List<String> xs) { StringBuilder sb = new StringBuilder(); for (String x : xs) sb.append(x); return sb.toString(); }
                static void addTo(List<String> xs, String s) { xs.add(s); }
                static String getSecond(List<String> $this$second) { return $this$second.get(1); }
            }
            final class Vec { private final int x; Vec(int x) { this.x = x; } int getX() { return x; } Vec plus(Vec o) { return new Vec(x + o.x); } }
            final class Idioms {
                private final StringBuilder sb;
                Idioms(StringBuilder sb) { this.sb = sb; }
                int withIt() { sb.append("x"); return sb.length(); }
                int runIt() { sb.append("x"); return sb.length(); }
                StringBuilder alsoIt() { sb.append("x"); return sb; }
                String useIt(java.io.BufferedReader r) throws java.io.IOException { try (r) { return r.readLine(); } }
                Vec sum(Vec a, Vec b) { return a.plus(b); }
                Sink anon() { return new Sink() { public void put(StringBuilder s) { sb.append(s); } }; }
                String template(int n) { return "n=" + n + ", sb=" + sb; }
            }
            """;

    private static Analyzed a;

    @BeforeAll
    public static void beforeAll() {
        a = analyze(List.of("X.kt", KOTLIN, "Top.kt", KOTLIN_TOP), List.of("X.java", JAVA, "TopKt.java", JAVA_TOP));
    }

    /*
     #90 (fixed by #85, 2026-09-28): the constructor stores the delegate, so the type, the constructor and the field
     agree with the Java twin. ⛔ maddi#93: the forwarder's parameter does not: Java's one-method Sink is a functional
     interface and `$$delegate_0.put(s)` takes the lambda path (`~Λ`, @Dependent); Kotlin's `interface Sink` is not
     (`fun interface` would be), and the ordinary abstract call reads `s` as @Independent of the delegate. When #93
     is settled, this becomes `a.assertSameAsJava("Forward2")` and `a.assertSameAsJava("Forward")`.
     */
    @Test
    public void delegation() {
        a.assertSameAsJava("ListSink");
        a.assertSameAsJava("Forward2", "<init>", "$$delegate_0");
        a.assertSameAsJava("Forward", "<init>", "$$delegate_0", "d");
        assertEquals("""
                type Forward2: @FinalFields @Dependent
                method put: nonModifying=false @Independent | 0: unmodified=false @Dependent""", a.java("Forward2", "put"));
        assertEquals("""
                type Forward2: @FinalFields @Dependent
                method put: nonModifying=false @Independent | 0: unmodified=false @Independent""", a.kotlin("Forward2", "put"));
        assertEquals(a.java("Forward2", "put").replace("Forward2", "Forward"), a.java("Forward", "put"));
        assertEquals(a.kotlin("Forward2", "put").replace("Forward2", "Forward"), a.kotlin("Forward", "put"));
    }

    /* the `$default` method carries the modification of the parameter it forwards; the type is unaffected */
    @Test
    public void defaultArguments() {
        a.assertSameAsJava("Defaults", "<init>", "join");
        assertEquals("""
                type Defaults: @Immutable @Independent
                method join$default: nonModifying=true @Independent | 0: unmodified=false @Independent, \
                1: unmodified=true @Independent, 2: unmodified=true @Independent""", a.kotlin("Defaults", "join$default"));
    }

    /* `private set` synthesizes a setter Java does not have; the verdicts of everything else are Java's */
    @Test
    public void privateSetter() {
        a.assertSameAsJava("Acc", "<init>", "name", "getName", "rename");
    }

    @Test
    public void computedAndLazyProperties() {
        a.assertSameAsJava("Computed");
        a.assertSameAsJava("Lazy");
    }

    /* a file class has no constructor in Kotlin; its static members agree */
    @Test
    public void topLevelFunctions() {
        a.assertSameAsJava("TopKt", "joinAll", "addTo", "getSecond");
    }

    @Test
    public void operatorAnonymousObjectTemplate() {
        a.assertSameAsJava("Vec");
        a.assertSameAsJava("Idioms", "<init>", "sb", "sum", "anon", "template");
    }

    /*
     `with`/`run` (the receiver), `also` (the `it`) and `use` (a try-with-resources on its receiver) are inlined
     (#88): a modification in the body is the method's, as in the Java twin.
     */
    @Test
    public void scopeFunctionsOnAField() {
        a.assertSameAsJava("Idioms", "withIt", "runIt", "alsoIt", "useIt");
    }
}
