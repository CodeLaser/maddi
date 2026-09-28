package io.codelaser.maddi.modification.prepwork.kotlin;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/*
 Loops: for-in over a collection and over a range, while, do-while, break and continue, and labeled jumps out of a
 nested loop. The loop's own evaluation reads (`-E`, `;E`, `:E`) and the merges after the loop must be the Java
 twin's.
 */
public class TestKotlinLoops extends CommonKotlinTest {

    @Language("kotlin")
    private static final String KOTLIN = """
            package k
            class X {
                fun forIn(xs: List<String>): Int {
                    var n = 0
                    for (x in xs) { n += x.length }
                    return n
                }
                fun forArray(xs: IntArray): Int {
                    var n = 0
                    for (x in xs) { n += x }
                    return n
                }
                fun whileLoop(k: Int): Int {
                    var i = 0
                    var s = 0
                    while (i < k) { s += i; i++ }
                    return s
                }
                fun doWhile(k: Int): Int {
                    var i = 0
                    do { i++ } while (i < k)
                    return i
                }
                fun breakContinue(xs: List<Int>): Int {
                    var s = 0
                    for (x in xs) {
                        if (x < 0) continue
                        if (x > 100) break
                        s += x
                    }
                    return s
                }
                fun labeled(xss: List<List<Int>>): Int {
                    var s = 0
                    outer@ for (xs in xss) {
                        for (x in xs) {
                            if (x < 0) continue@outer
                            if (x > 100) break@outer
                            s += x
                        }
                    }
                    return s
                }
                fun whileTrue(k: Int): Int {
                    var i = 0
                    while (true) {
                        if (i >= k) break
                        i++
                    }
                    return i
                }
            }
            """;

    @Language("java")
    private static final String JAVA = """
            package j;
            import java.util.List;
            class X {
                int forIn(List<String> xs) {
                    int n = 0;
                    for (String x : xs) { n += x.length(); }
                    return n;
                }
                int forArray(int[] xs) {
                    int n = 0;
                    for (int x : xs) { n += x; }
                    return n;
                }
                int whileLoop(int k) {
                    int i = 0;
                    int s = 0;
                    while (i < k) { s += i; i++; }
                    return s;
                }
                int doWhile(int k) {
                    int i = 0;
                    do { i++; } while (i < k);
                    return i;
                }
                int breakContinue(List<Integer> xs) {
                    int s = 0;
                    for (int x : xs) {
                        if (x < 0) continue;
                        if (x > 100) break;
                        s += x;
                    }
                    return s;
                }
                int labeled(List<List<Integer>> xss) {
                    int s = 0;
                    outer: for (List<Integer> xs : xss) {
                        for (int x : xs) {
                            if (x < 0) continue outer;
                            if (x > 100) break outer;
                            s += x;
                        }
                    }
                    return s;
                }
                int whileTrue(int k) {
                    int i = 0;
                    while (true) {
                        if (i >= k) break;
                        i++;
                    }
                    return i;
                }
            }
            """;

    private static Parsed p;

    @BeforeAll
    public static void beforeAll() {
        p = prep(KOTLIN, JAVA);
    }

    @Test
    public void forIn() {
        p.assertSameAsJava("forIn");
        assertEquals("""
                n: D:0, A:[0, 1.0.0] | R 1.0.0, 2
                return forIn: D:-, A:[2] | R -
                xs: D:-, A:[] | R 1-E""", summary(p.kotlin("forIn")));
    }

    @Test
    public void forArray() {
        p.assertSameAsJava("forArray");
    }

    @Test
    public void whileLoop() {
        p.assertSameAsJava("whileLoop");
        assertEquals("""
                i: D:0, A:[0, 2.0.1] | R 2-E, 2.0.0, 2.0.1, 2;E
                k: D:-, A:[] | R 2-E, 2;E
                return whileLoop: D:-, A:[3] | R -
                s: D:1, A:[1, 2.0.0] | R 2.0.0, 3""", summary(p.kotlin("whileLoop")));
    }

    @Test
    public void doWhile() {
        p.assertSameAsJava("doWhile");
    }

    @Test
    public void breakContinue() {
        p.assertSameAsJava("breakContinue");
    }

    @Test
    public void labeled() {
        p.assertSameAsJava("labeled");
    }

    @Test
    public void whileTrue() {
        p.assertSameAsJava("whileTrue");
    }
}
