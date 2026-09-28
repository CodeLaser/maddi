package io.codelaser.maddi.modification.prepwork.kotlin;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/*
 Assignments and reads through the conditional constructs: if/else (statement and value), when (statement, value,
 subject-less), and the null-handling operators K2 lowers to a conditional (elvis, safe call, `!!`). Each Kotlin
 function has a Java twin with the same body structure; prep must give their locals and parameters the same
 definition, assignments and reads.
 */
public class TestKotlinAssignments extends CommonKotlinTest {

    @Language("kotlin")
    private static final String KOTLIN = """
            package k
            class X {
                fun ifElse(i: Int): Int {
                    var r: Int
                    if (i > 0) { r = 1 } else { r = 2 }
                    return r + i
                }
                fun ifNoElse(i: Int): Int {
                    var r = 0
                    if (i > 0) r = i
                    return r
                }
                fun ifValue(i: Int): Int {
                    val r = if (i > 0) 1 else 2
                    return r
                }
                fun compound(i: Int): Int {
                    var r = i
                    r += 2
                    r *= i
                    r++
                    return r
                }
                fun whenStatement(i: Int): Int {
                    var r = 0
                    when (i) {
                        0 -> r = 1
                        1, 2 -> r = 2
                        else -> r = 3
                    }
                    return r
                }
                fun whenValue(i: Int): Int {
                    val r = when (i) { 0 -> 1; else -> i + 1 }
                    return r
                }
                fun whenSubjectless(i: Int, j: Int): Int {
                    var r = 0
                    when {
                        i > j -> r = i
                        j > 0 -> r = j
                    }
                    return r
                }
                fun whenArmAssigns(i: Int): Int {
                    var x = 0
                    val r = when (i) { 0 -> { x = 5; 1 } else -> 2 }
                    return r + x
                }
                fun whenArmReads(i: Int): Int {
                    var x = 0
                    return when (i) { 0 -> { x = 5; x + 1 } else -> x }
                }
                fun elvis(s: String?): Int {
                    val t = s ?: "x"
                    return t.length
                }
                fun notNull(s: String?): Int {
                    val t = s!!
                    return t.length
                }
                fun template(a: String, b: Int): String {
                    val s = "$a:${b + 1}"
                    return s
                }
                fun array(a: IntArray, i: Int): Int {
                    a[i] = 3
                    return a[i]
                }
            }
            """;

    @Language("java")
    private static final String JAVA = """
            package j;
            class X {
                int ifElse(int i) {
                    int r;
                    if (i > 0) { r = 1; } else { r = 2; }
                    return r + i;
                }
                int ifNoElse(int i) {
                    int r = 0;
                    if (i > 0) r = i;
                    return r;
                }
                int ifValue(int i) {
                    int r = i > 0 ? 1 : 2;
                    return r;
                }
                int compound(int i) {
                    int r = i;
                    r += 2;
                    r *= i;
                    r++;
                    return r;
                }
                int whenStatement(int i) {
                    int r = 0;
                    switch (i) {
                        case 0 -> r = 1;
                        case 1, 2 -> r = 2;
                        default -> r = 3;
                    }
                    return r;
                }
                int whenValue(int i) {
                    int r = switch (i) { case 0 -> 1; default -> i + 1; };
                    return r;
                }
                int whenSubjectless(int i, int j) {
                    int r = 0;
                    if (i > j) { r = i; } else { if (j > 0) { r = j; } }
                    return r;
                }
                int whenArmAssigns(int i) {
                    int x = 0;
                    int r = switch (i) { case 0 -> { x = 5; yield 1; } default -> 2; };
                    return r + x;
                }
                int whenArmReads(int i) {
                    int x = 0;
                    return switch (i) { case 0 -> { x = 5; yield x + 1; } default -> x; };
                }
                int elvis(String s) {
                    String t = s == null ? "x" : s;
                    return t.length();
                }
                int notNull(String s) {
                    String t = java.util.Objects.requireNonNull(s);
                    return t.length();
                }
                String template(String a, int b) {
                    String s = a + ":" + (b + 1);
                    return s;
                }
                int array(int[] a, int i) {
                    a[i] = 3;
                    return a[i];
                }
            }
            """;

    private static Parsed p;

    @BeforeAll
    public static void beforeAll() {
        p = prep(KOTLIN, JAVA);
    }

    @Test
    public void ifElse() {
        p.assertSameAsJava("ifElse");
        assertEquals("""
                i: D:-, A:[] | R 1-E, 2
                r: D:0, A:[1.0.0, 1.1.0, 1=M] | R 2
                return ifElse: D:-, A:[2] | R -""", summary(p.kotlin("ifElse")));
    }

    @Test
    public void ifNoElse() {
        p.assertSameAsJava("ifNoElse");
    }

    @Test
    public void ifValue() {
        p.assertSameAsJava("ifValue");
    }

    @Test
    public void compound() {
        p.assertSameAsJava("compound");
    }

    @Test
    public void whenStatement() {
        p.assertSameAsJava("whenStatement");
        assertEquals("""
                i: D:-, A:[] | R 1-E
                r: D:0, A:[0, 1.0.0, 1.1.0, 1.2.0, 1=M] | R 2
                return whenStatement: D:-, A:[2] | R -""", summary(p.kotlin("whenStatement")));
    }

    @Test
    public void whenValue() {
        p.assertSameAsJava("whenValue");
    }

    /*
     Java has no subject-less switch, and the twin's if-chain nests where K2's lowering does not: a subject-less
     `when` is a switch whose entries carry the conditions. Each entry is a sibling (1.0.0, 1.1.0), each condition is
     read at its own entry, and, as for an if-chain without a final else, there is no merge.
     */
    @Test
    public void whenSubjectless() {
        assertEquals("""
                i: D:-, A:[] | R 1.0.0
                j: D:-, A:[] | R 1.1.0
                r: D:0, A:[0, 1.0.0, 1.1.0] | R 2
                return whenSubjectless: D:-, A:[2] | R -""", summary(p.kotlin("whenSubjectless")));
        assertEquals("""
                i: D:-, A:[] | R 1-E, 1.0.0
                j: D:-, A:[] | R 1-E, 1.1.0-E, 1.1.0.0.0
                r: D:0, A:[0, 1.0.0, 1.1.0.0.0] | R 2
                return whenSubjectless: D:-, A:[2] | R -""", summary(p.java("whenSubjectless")));
    }

    /*
     ⛔ maddi#74 (Java and Kotlin alike): `x = 5` inside an arm of a switch expression is not an assignment of the
     enclosing statement, so `return r + x` sees `x = 0` only. Both sides agree, on the wrong answer.
     */
    @Test
    public void whenArmAssigns() {
        p.assertSameAsJava("whenArmAssigns");
        assertEquals("""
                i: D:-, A:[] | R 1
                r: D:1, A:[1] | R 2
                return whenArmAssigns: D:-, A:[2] | R -
                x: D:0, A:[0] | R 2""", summary(p.kotlin("whenArmAssigns")));
    }

    /*
     ⛔ maddi#74 and #75. The Java twin records no read of `x` in the arms (#74); the Kotlin side records one at
     statement 1, through arm statements indexed 0 and 1 rather than under statement 1 (#75).
     */
    @Test
    public void whenArmReads() {
        assertEquals("""
                i: D:-, A:[] | R 1
                return whenArmReads: D:-, A:[1] | R -
                x: D:0, A:[0] | R -""", summary(p.java("whenArmReads")));
        assertEquals("""
                i: D:-, A:[] | R 1
                return whenArmReads: D:-, A:[1] | R -
                x: D:0, A:[0] | R 1""", summary(p.kotlin("whenArmReads")));
    }

    @Test
    public void elvis() {
        p.assertSameAsJava("elvis");
    }

    @Test
    public void notNull() {
        p.assertSameAsJava("notNull");
    }

    @Test
    public void template() {
        p.assertSameAsJava("template");
    }

    @Test
    public void array() {
        p.assertSameAsJava("array");
        assertEquals("""
                a: D:-, A:[] | R 0, 1
                a[i]: D:-, A:[0] | R 1
                i: D:-, A:[] | R 0, 1
                return array: D:-, A:[1] | R -""", summary(p.kotlin("array")));
    }
}
