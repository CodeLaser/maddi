package io.codelaser.maddi.modification.link.kotlin;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/*
 Values that flow through a method: identity, if/when/elvis as values, reassignment, casts and smart casts, arrays
 and varargs.
 */
public class TestKotlinLinkConditionals extends CommonKotlinLinkTest {

    @Language("kotlin")
    private static final String KOTLIN = """
            package k
            class X {
                fun ident(s: StringBuilder): StringBuilder = s
                fun cond(a: String, b: String, c: Boolean): String = if (c) a else b
                fun whenPick(a: String, b: String, i: Int): String = when (i) { 0 -> a; else -> b }
                fun elvis(a: String?, b: String): String = a ?: b
                fun reassign(a: String, b: String): String { var r = a; r = b; return r }
                fun cast(o: Any): String = o as String
                fun castMutable(o: Any): StringBuilder = o as StringBuilder
                fun smartIf(o: Any): StringBuilder? { if (o is StringBuilder) return o; return null }
                fun smartSb(o: Any): StringBuilder? = if (o is StringBuilder) o else null
                fun array(a: Array<String>, i: Int): String = a[i]
                fun setArray(a: Array<String>, i: Int, s: String) { a[i] = s }
                fun varargs(vararg xs: String): String = xs[0]
                fun ownVararg(a: StringBuilder): StringBuilder = pick(a)
                fun pick(vararg xs: StringBuilder): StringBuilder = xs[0]
            }
            """;

    @Language("java")
    private static final String JAVA = """
            package j;
            class X {
                StringBuilder ident(StringBuilder s) { return s; }
                String cond(String a, String b, boolean c) { return c ? a : b; }
                String whenPick(String a, String b, int i) { return switch (i) { case 0 -> a; default -> b; }; }
                String elvis(String a, String b) { return a == null ? b : a; }
                String reassign(String a, String b) { String r = a; r = b; return r; }
                String cast(Object o) { return (String) o; }
                StringBuilder castMutable(Object o) { return (StringBuilder) o; }
                StringBuilder smartIfCast(Object o) { if (o instanceof StringBuilder) return (StringBuilder) o; return null; }
                StringBuilder smartIf(Object o) { if (o instanceof StringBuilder s) return s; return null; }
                StringBuilder smartSb(Object o) { return o instanceof StringBuilder s ? s : null; }
                String array(String[] a, int i) { return a[i]; }
                void setArray(String[] a, int i, String s) { a[i] = s; }
                String varargs(String... xs) { return xs[0]; }
                StringBuilder ownVararg(StringBuilder a) { return pick(a); }
                StringBuilder pick(StringBuilder... xs) { return xs[0]; }
            }
            """;

    private static Parsed p;

    @BeforeAll
    public static void beforeAll() {
        p = link(KOTLIN, JAVA);
    }

    @Test
    public void identity() {
        p.assertSameAsJava("ident");
        assertEquals("[-] --> ident←0:s", p.kotlinLinks("ident"));
    }

    @Test
    public void conditionalValues() {
        p.assertSameAsJava("cond");
        p.assertSameAsJava("whenPick");
        p.assertSameAsJava("elvis");
        assertEquals("[-, -] --> elvis←0:a,elvis←1:b", p.kotlinLinks("elvis"));
    }

    @Test
    public void reassignment() {
        p.assertSameAsJava("reassign");
        assertEquals("[-, -] --> reassign←1:b", p.kotlinLinks("reassign"));
    }

    @Test
    public void cast() {
        p.assertSameAsJava("cast");
    }

    /*
     A smart-cast read is a cast (#67): `if (o is StringBuilder) return o` converts to `return (StringBuilder)o;` and
     links exactly as the Java cast form `smartIfCast`. Since maddi#95 a downcast returned whole keeps the
     modification-area link a pattern variable keeps (`§m≡0:o.§m`), so the Kotlin smart cast and the Java pattern
     summarise alike: modifying the result is modifying `o`.
     */
    @Test
    public void smartCastStatement() {
        assertEquals("[-] --> smartIf←$_ce0,smartIf←0:o,smartIf.§m≡0:o.§m", p.javaLinks("smartIf"));
        assertEquals("[-] --> smartIfCast←$_ce0,smartIfCast←0:o,smartIfCast.§m≡0:o.§m",
                p.links(p.java("smartIfCast")));
        p.assertSameAsJava("smartIf");
        p.assertSameAsJava("castMutable");
        assertEquals("[-] --> castMutable.§m≡0:o.§m,castMutable←0:o", p.kotlinLinks("castMutable"));
    }

    /*
     A pattern variable in a Java conditional expression links to the value it was bound from (#79, fixed; it linked
     to nothing). The Kotlin side reads `(StringBuilder)o`, a cast, which carries the same `§m≡` since maddi#95.
     */
    @Test
    public void smartCastExpression() {
        assertEquals("[-] --> smartSb←$_ce0,smartSb←0:o,smartSb.§m≡0:o.§m", p.javaLinks("smartSb"));
        p.assertSameAsJava("smartSb");
    }

    @Test
    public void arrays() {
        p.assertSameAsJava("array");
        p.assertSameAsJava("setArray");
        assertEquals("[0:a*∋2:s,0:a*[1:i]←2:s, -, 2:s∈0:a*,2:s→0:a*[1:i]] --> -", p.kotlinLinks("setArray"));
    }

    @Test
    public void varargs() {
        p.assertSameAsJava("varargs");
        p.assertSameAsJava("ownVararg");
        assertEquals("[-] --> ownVararg∈0:a,ownVararg←0:a[0]", p.kotlinLinks("ownVararg"));
    }
}
