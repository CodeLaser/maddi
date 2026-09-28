package io.codelaser.maddi.modification.prepwork.kotlin;

import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.modification.prepwork.escape.ComputeAlwaysEscapes;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/*
 Statements that always escape, and the assignments of the return variable along the escaping paths. Kotlin's
 Nothing-typed calls (`error`, `TODO`) are inlined by K2 into the `throw` kotlinc emits; a `throw` in a `when`
 arm, and the control-flow elvis (`?: return`, `?: throw`) lower to Java shapes.
 */
public class TestKotlinEscapes extends CommonKotlinTest {

    @Language("kotlin")
    private static final String KOTLIN = """
            package k
            class X {
                fun error1(s: String): Int { if (s.isEmpty()) error("x"); return 1 }
                fun todo(): Int = TODO()
                fun whenThrows(i: Int): Int { return when (i) { 0 -> 1; else -> throw IllegalStateException() } }
                fun elvisReturn(s: String?): Int { val t = s ?: return 0; return t.length }
                fun elvisThrow(s: String?): Int { val t = s ?: throw IllegalStateException(); return t.length }
            }
            """;

    @Language("java")
    private static final String JAVA = """
            package j;
            class X {
                int error1(String s) { if (s.isEmpty()) throw new IllegalStateException("x"); return 1; }
                int todo() { throw new UnsupportedOperationException(); }
                int whenThrows(int i) { return switch (i) { case 0 -> 1; default -> throw new IllegalStateException(); }; }
                int elvisReturn(String s) { if (s == null) return 0; String t = s; return t.length(); }
                int elvisThrow(String s) { if (s == null) throw new IllegalStateException(); String t = s; return t.length(); }
            }
            """;

    private static Parsed p;

    @BeforeAll
    public static void beforeAll() {
        p = prep(KOTLIN, JAVA);
    }

    private static String escapes(MethodInfo m) {
        ComputeAlwaysEscapes.go(m);
        return m.methodBody().statements().stream().map(s -> s.source().index() + "=" + s.alwaysEscapes())
                .collect(Collectors.joining(", "));
    }

    @Test
    public void error() {
        p.assertSameAsJava("error1");
        assertEquals("0=false, 1=true", escapes(p.kotlin("error1")));
        assertEquals("""
                return error1: D:-, A:[0.0.0, 1] | R -
                s: D:-, A:[] | R 0-E""", summary(p.kotlin("error1")));
    }

    @Test
    public void todo() {
        p.assertSameAsJava("todo");
        assertEquals("0=true", escapes(p.kotlin("todo")));
    }

    /* maddi#75 (fixed 2026-09-28): the arms of a `when` used as a value are indexed under their statement, as in Java */
    @Test
    public void whenThrows() {
        assertEquals("0=true", escapes(p.kotlin("whenThrows")));
        p.assertSameAsJava("whenThrows");
        assertEquals("""
                i: D:-, A:[] | R 0
                return whenThrows: D:-, A:[0, 0.1.0] | R -""", summary(p.kotlin("whenThrows")));
    }

    /*
     ⛔ maddi#69, the control-flow elvis: `val t = s ?: return 0` is `if (s == null) return 0;` at 0.0 and
     `String t = s;` at 0.1. The Java twin has the same statements as siblings, 0 and 1. The escape is right; `t`
     is lost at statement 1, so `return t.length()` does not read it.
     */
    @Test
    public void elvisReturn() {
        assertEquals("0.0=false, 0.1=false, 1=true", escapes(p.kotlin("elvisReturn")));
        assertEquals("""
                return elvisReturn: D:-, A:[0.0.0.0, 1] | R -
                s: D:-, A:[] | R 0.0-E, 0.1""", summary(p.kotlin("elvisReturn")));
        assertEquals("""
                return elvisReturn: D:-, A:[0.0.0, 2] | R -
                s: D:-, A:[] | R 0-E, 1
                t: D:1, A:[1] | R 2""", summary(p.java("elvisReturn")));
    }

    @Test
    public void elvisThrow() {
        assertEquals("0.0=false, 0.1=false, 1=true", escapes(p.kotlin("elvisThrow")));
        assertEquals("""
                return elvisThrow: D:-, A:[0.0.0.0, 1] | R -
                s: D:-, A:[] | R 0.0-E, 0.1""", summary(p.kotlin("elvisThrow")));
    }
}
