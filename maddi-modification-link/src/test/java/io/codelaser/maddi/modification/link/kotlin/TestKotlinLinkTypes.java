package io.codelaser.maddi.modification.link.kotlin;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
 Kotlin's own type shapes, linked: a data class (constructor, componentN, destructuring, copy), a property of
 another class, an `object` (a singleton INSTANCE with instance fields, as kotlinc emits it), a loop collecting into
 a list, a try that reassigns, the scope functions, and a sequence.
 */
public class TestKotlinLinkTypes extends CommonKotlinLinkTest {

    @Language("kotlin")
    private static final String KOTLIN = """
            package k
            data class D(val a: StringBuilder, val b: List<StringBuilder>)
            class Box(var sb: StringBuilder)
            object Registry { val all = ArrayList<StringBuilder>(); fun register(s: StringBuilder) { all.add(s) } }
            class Logged { var count = 0; var sb: StringBuilder = StringBuilder()
                set(v) { count++; field = v } }
            class X {
                fun makeD(a: StringBuilder, b: List<StringBuilder>): D = D(a, b)
                fun component(d: D): StringBuilder = d.a
                fun destructure(d: D): StringBuilder { val (a, _) = d; return a }
                fun copyD(d: D, a: StringBuilder): D = d.copy(a = a)
                fun viaGetter(box: Box): StringBuilder = box.sb
                fun viaSetter(box: Box, s: StringBuilder) { box.sb = s }
                fun register(s: StringBuilder) { Registry.register(s) }
                fun collect(xs: List<StringBuilder>): List<StringBuilder> { val out = ArrayList<StringBuilder>(); for (x in xs) { if (x.isEmpty()) out.add(x) }; return out }
                fun tryPick(a: StringBuilder, b: StringBuilder): StringBuilder { var r = a; try { r.append("x") } catch (e: Exception) { r = b }; return r }
                fun alsoIt(s: StringBuilder): StringBuilder = s.also { it.append("x") }
                fun applyIt(s: StringBuilder): StringBuilder = s.apply { append("x") }
                fun letIt(s: StringBuilder): StringBuilder = s.let { it }
                fun seqFirst(xs: List<StringBuilder>): StringBuilder = xs.asSequence().first()
                fun customSet(l: Logged, s: StringBuilder) { l.sb = s }
            }
            """;

    @Language("java")
    private static final String JAVA = """
            package j;
            import java.util.ArrayList;
            import java.util.List;
            class X {
                record D(StringBuilder a, List<StringBuilder> b) {}
                static class Box { StringBuilder sb; Box(StringBuilder sb) { this.sb = sb; } StringBuilder getSb() { return sb; } void setSb(StringBuilder sb) { this.sb = sb; } }
                static final class Registry { static final Registry INSTANCE = new Registry(); final List<StringBuilder> all = new ArrayList<>(); void register(StringBuilder s) { all.add(s); } }
                D makeD(StringBuilder a, List<StringBuilder> b) { return new D(a, b); }
                StringBuilder component(D d) { return d.a(); }
                StringBuilder destructure(D d) { StringBuilder a = d.a(); return a; }
                D copyD(D d, StringBuilder a) { return new D(a, d.b()); }
                StringBuilder viaGetter(Box box) { return box.sb; }
                void viaSetter(Box box, StringBuilder s) { box.sb = s; }
                void register(StringBuilder s) { Registry.INSTANCE.register(s); }
                List<StringBuilder> collect(List<StringBuilder> xs) { List<StringBuilder> out = new ArrayList<>(); for (StringBuilder x : xs) { if (x.isEmpty()) out.add(x); } return out; }
                StringBuilder tryPick(StringBuilder a, StringBuilder b) { StringBuilder r = a; try { r.append("x"); } catch (Exception e) { r = b; } return r; }
                StringBuilder alsoIt(StringBuilder s) { s.append("x"); return s; }
                StringBuilder applyIt(StringBuilder s) { s.append("x"); return s; }
                StringBuilder letIt(StringBuilder s) { return s; }
                StringBuilder seqFirst(List<StringBuilder> xs) { return xs.stream().findFirst().orElseThrow(); }
            }
            """;

    private static Parsed p;

    @BeforeAll
    public static void beforeAll() {
        p = link(KOTLIN, JAVA);
    }

    @Test
    public void dataClass() {
        p.assertSameAsJava("makeD");
        p.assertSameAsJava("component");
        p.assertSameAsJava("destructure");
        assertEquals("[-] --> destructure←0:d.a", p.kotlinLinks("destructure"));
    }

    /*
     maddi#81 (fixed 2026-09-28): `d.copy(a = a)` binds to the data class's `copy$default`, whose body substitutes
     `this.b` for the omitted `b` before calling `copy`, as kotlinc's does.
     */
    @Test
    public void dataClassCopy() {
        assertEquals("{return d.copy$default(a,null,2);}",
                p.kotlin("copyD").methodBody().print(p.runtime().qualificationSimpleNames()).toString());
        assertEquals("[-, -] --> copyD.a←1:a,copyD.b←0:d.b,copyD.a.§m≡1:a.§m,copyD.b.§m≡0:d.b.§m",
                p.javaLinks("copyD"));
        // the copy's `b` now depends on `d.b`, as in Java. The rest is the bridge's conservatism: `$mask` is not
        // evaluated, so each property links to BOTH its argument and the original's field, and the omitted `b`
        // keeps its null placeholder ($_ce0) as a second source.
        assertEquals("[0:d*.b.§m≡$_ce0*.§m, 1:a*.§m≡0:d*.a.§m,1:a*→0:d*.a] --> "
                     + "copyD.a.§m≡0:d*.a.§m,copyD.a.§m≡1:a*.§m,copyD.b.§m≡$_ce0*.§m,copyD.b.§m≡0:d*.b.§m,"
                     + "copyD.a←0:d*.a,copyD.a←1:a*,copyD.b→$_ce0*,copyD.b←0:d*.b",
                p.kotlinLinks("copyD"));
    }

    /* a property of another instance, default accessors: K2 reads and writes the field, which means the same */
    @Test
    public void propertyOfAnotherInstance() {
        p.assertSameAsJava("viaGetter");
        p.assertSameAsJava("viaSetter");
    }

    /*
     maddi#82 (fixed 2026-09-28): with a custom setter a field write does not mean the same. kotlinc calls setSb
     (count++), and so does the CST now; the setter's side effect is part of customSet.
     */
    @Test
    public void customSetterFromOutside() {
        assertEquals("{l.setSb(s);}",
                p.kotlin("customSet").methodBody().print(p.runtime().qualificationSimpleNames()).toString());
    }

    /*
     ⚠ The owner is matched loosely: maddi#83 makes it print as the scope of a static call from an EARLIER parse in
     the same JVM (`List.INSTANCE.all`), depending on which test classes share the fork. TestKotlinLinkIsolation
     pins that; here only the shape counts.
     */
    @Test
    public void objectSingleton() {
        p.assertSameAsJava("register");
        String links = p.kotlinLinks("register");
        assertTrue(links.matches("\\[0:s\\*∈.+\\.INSTANCE\\.all\\*\\.§\\$s] --> -"), links);
    }

    @Test
    public void loopAndTry() {
        p.assertSameAsJava("collect");
        p.assertSameAsJava("tryPick");
        assertEquals("[-] --> collect.§$s~0:xs.§$s", p.kotlinLinks("collect"));
    }

    /*
     `also`/`apply` return their receiver, linked as Java's `return s`. The modification THROUGH the lambda
     (`it.append`) is not seen: the engine's design for any call that applies a lambda, shared with Java
     (`Optional.of(s).ifPresent(x -> x.append(..))`), see c0289fa84. The Java twin appends directly, hence `0:s*`.
     */
    @Test
    public void scopeFunctionsReturnTheReceiver() {
        assertEquals("[-] --> alsoIt←0:s*", p.javaLinks("alsoIt"));
        assertEquals("[-] --> alsoIt←0:s", p.kotlinLinks("alsoIt"));
        assertEquals("[-] --> applyIt←0:s", p.kotlinLinks("applyIt"));
    }

    /* ⛔ maddi#80: `s.let { it }` returns s, but the value of a FunctionN applied by `let` is not linked back */
    @Test
    public void letResult() {
        assertEquals("[-] --> letIt←0:s", p.javaLinks("letIt"));
        assertEquals("[-] --> -", p.kotlinLinks("letIt"));
    }

    /* ⛔ maddi#78: asSequence().first() returns an element of xs, and links to nothing */
    @Test
    public void sequenceFirst() {
        assertEquals("[-] --> seqFirst∈0:xs.§$s", p.javaLinks("seqFirst"));
        assertEquals("[-] --> -", p.kotlinLinks("seqFirst"));
    }
}
