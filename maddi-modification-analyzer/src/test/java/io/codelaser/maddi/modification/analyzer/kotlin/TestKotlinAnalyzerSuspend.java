package io.codelaser.maddi.modification.analyzer.kotlin;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/*
 Coroutines as far as kotlin-stdlib reaches (no kotlinx.coroutines on this class path): `suspend` functions, which
 kotlinc lowers to an extra Continuation parameter and an Object return, and the `sequence { }` builder, a suspend
 lambda handed to an uncontracted stdlib function. The Java twins are those lowerings, written by hand.
 */
public class TestKotlinAnalyzerSuspend extends CommonKotlinAnalyzerTest {

    @Language("kotlin")
    private static final String KOTLIN = """
            package k
            class S(private val sb: StringBuilder) {
                suspend fun add(s: String) { sb.append(s) }
                suspend fun read(): Int = sb.length
                suspend fun twice(s: String) { add(s); add(s) }
                suspend fun into(xs: MutableList<String>, s: String) { xs.add(s) }
            }
            """;

    @Language("java")
    private static final String JAVA = """
            package j;
            import java.util.List;
            import kotlin.Unit;
            import kotlin.coroutines.Continuation;
            final class S {
                private final StringBuilder sb;
                S(StringBuilder sb) { this.sb = sb; }
                Object add(String s, Continuation<? super Unit> $completion) { sb.append(s); return Unit.INSTANCE; }
                Object read(Continuation<? super Integer> $completion) { return sb.length(); }
                Object twice(String s, Continuation<? super Unit> $completion) { add(s, $completion); add(s, $completion); return Unit.INSTANCE; }
                Object into(List<String> xs, String s, Continuation<? super Unit> $completion) { xs.add(s); return Unit.INSTANCE; }
            }
            """;

    @Language("kotlin")
    private static final String KOTLIN_SEQ = """
            package k
            class Q(private val sb: StringBuilder) {
                fun seq(): Sequence<Int> = sequence { yield(1); yield(sb.length) }
                fun gen(xs: MutableList<String>): Sequence<String> = sequence { xs.add("x"); yieldAll(xs) }
                fun all(xs: List<StringBuilder>): Sequence<StringBuilder> = sequence { yieldAll(xs) }
            }
            """;

    @Language("java")
    private static final String JAVA_SEQ = """
            package j;
            import java.util.List;
            import kotlin.sequences.Sequence;
            import kotlin.sequences.SequencesKt;
            final class Q {
                private final StringBuilder sb;
                Q(StringBuilder sb) { this.sb = sb; }
                Sequence<Integer> seq() { return SequencesKt.sequence((scope, c) -> { scope.yield(1, c); return scope.yield(sb.length(), c); }); }
                Sequence<String> gen(List<String> xs) { return SequencesKt.sequence((scope, c) -> { xs.add("x"); return scope.yieldAll(xs, c); }); }
                Sequence<StringBuilder> all(List<StringBuilder> xs) { return SequencesKt.sequence((scope, c) -> scope.yieldAll(xs, c)); }
            }
            """;

    private static Analyzed s;
    private static Analyzed q;

    @BeforeAll
    public static void beforeAll() {
        s = analyze(KOTLIN, JAVA);
        q = analyze(List.of("Q.kt", KOTLIN_SEQ), List.of("Q.java", JAVA_SEQ));
    }

    /* the Continuation parameter is modelled, and a suspend call to a suspend function changes nothing */
    @Test
    public void suspendFunctions() {
        s.assertSameAsJava("S");
        assertEquals("k.S.into(java.util.List,String,kotlin.coroutines.Continuation)",
                method(s.kotlinType("S"), "into").fullyQualifiedName());
    }

    /* the builder's suspend lambda converts completely, receiver and continuation included */
    @Test
    public void sequenceBuilder() {
        q.assertSameAsJava("Q");
        assertEquals("{return SequencesKt.sequence(($receiver,$completion)->{xs.add(\"x\");"
                     + "$receiver.yieldAll(xs,$completion);});}",
                method(q.kotlinType("Q"), "gen").methodBody().print(q.runtime().qualificationSimpleNames()).toString());
    }

    /*
     ⚠ maddi#89, Java too: `sequence`/`yieldAll` have no contract. `all` returns a sequence over the StringBuilders
     of `xs`, yet reads @Independent on both sides (a copy would be @Independent(hc=true), a view @Dependent), and
     `xs` reads modified although nothing modifies it.
     */
    @Test
    public void uncontractedBuilder() {
        assertEquals("""
                type Q: @FinalFields @Dependent
                method all: nonModifying=true @Independent | 0: unmodified=false @Independent""", q.kotlin("Q", "all"));
    }
}
