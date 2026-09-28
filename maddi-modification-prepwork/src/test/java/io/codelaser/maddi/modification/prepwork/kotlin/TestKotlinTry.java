package io.codelaser.maddi.modification.prepwork.kotlin;

import io.codelaser.maddi.cst.api.statement.Block;
import io.codelaser.maddi.modification.prepwork.variable.impl.VariableDataImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/*
 try/catch/finally, a try used as a value, and the other Kotlin expressions K2 lowers into a declaration followed
 by a statement that assigns it (`val v = try …`, `val v = if (…) { …; a } else …`).
 */
public class TestKotlinTry extends CommonKotlinTest {

    @Language("kotlin")
    private static final String KOTLIN = """
            package k
            class X {
                fun tryCatch(s: String): Int {
                    var v: Int
                    try { v = s.toInt() } catch (e: NumberFormatException) { v = -1 }
                    return v
                }
                fun tryFinally(s: String): Int {
                    var v: Int
                    var d: Int
                    try { v = s.toInt() } catch (e: NumberFormatException) { v = -1 } finally { d = 9 }
                    return v + d
                }
                fun severalCatches(s: String): Int {
                    var v = 0
                    try { v = s.toInt() } catch (e: NumberFormatException) { v = -1 } catch (e: RuntimeException) { v = -2 }
                    return v
                }
                fun rethrow(s: String): Int {
                    var v: Int
                    try { v = s.toInt() } catch (e: NumberFormatException) { throw IllegalStateException(e) }
                    return v
                }
                fun tryValue(s: String): Int {
                    val v = try { s.toInt() } catch (e: NumberFormatException) { -1 }
                    return v
                }
                fun ifBlockValue(s: String): Int {
                    val v = if (s.isEmpty()) { println(s); 1 } else 2
                    return v
                }
                fun tryValueInLoop(xs: List<String>): Int {
                    var sum = 0
                    for (x in xs) {
                        val v = try { x.toInt() } catch (e: NumberFormatException) { continue }
                        sum += v
                    }
                    return sum
                }
            }
            """;

    @Language("java")
    private static final String JAVA = """
            package j;
            import java.util.List;
            class X {
                int tryCatch(String s) {
                    int v;
                    try { v = Integer.parseInt(s); } catch (NumberFormatException e) { v = -1; }
                    return v;
                }
                int tryFinally(String s) {
                    int v;
                    int d;
                    try { v = Integer.parseInt(s); } catch (NumberFormatException e) { v = -1; } finally { d = 9; }
                    return v + d;
                }
                int severalCatches(String s) {
                    int v = 0;
                    try { v = Integer.parseInt(s); } catch (NumberFormatException e) { v = -1; } catch (RuntimeException e) { v = -2; }
                    return v;
                }
                int rethrow(String s) {
                    int v;
                    try { v = Integer.parseInt(s); } catch (NumberFormatException e) { throw new IllegalStateException(e); }
                    return v;
                }
                int tryValue(String s) {
                    int v;
                    try { v = Integer.parseInt(s); } catch (NumberFormatException e) { v = -1; }
                    return v;
                }
                int ifBlockValue(String s) {
                    int v;
                    if (s.isEmpty()) { System.out.println(s); v = 1; } else { v = 2; }
                    return v;
                }
                int tryValueInLoop(List<String> xs) {
                    int sum = 0;
                    for (String x : xs) {
                        int v;
                        try { v = Integer.parseInt(x); } catch (NumberFormatException e) { continue; }
                        sum += v;
                    }
                    return sum;
                }
            }
            """;

    private static Parsed p;

    @BeforeAll
    public static void beforeAll() {
        p = prep(KOTLIN, JAVA);
    }

    @Test
    public void tryCatch() {
        p.assertSameAsJava("tryCatch");
        assertEquals("""
                return tryCatch: D:-, A:[2] | R -
                s: D:-, A:[] | R 1.0.0
                v: D:0, A:[1.0.0, 1.1.0, 1=M] | R 2""", summary(p.kotlin("tryCatch")));
    }

    @Test
    public void tryFinally() {
        p.assertSameAsJava("tryFinally");
    }

    @Test
    public void severalCatches() {
        p.assertSameAsJava("severalCatches");
    }

    @Test
    public void rethrow() {
        p.assertSameAsJava("rethrow");
    }

    /*
     `val v = try { … } catch …` is TWO statements, `int v;` and a try assigning it, indexed as siblings, 0 and 1, as
     the Java twin's are (#69: they were 0.0 and 0.1, children of statement 0, and `v` was unknown at statement 1).
     */
    @Test
    public void tryValue() {
        p.assertSameAsJava("tryValue");
        assertEquals("""
                return tryValue: D:-, A:[2] | R -
                s: D:-, A:[] | R 1.0.0
                v: D:0, A:[1.0.0, 1.1.0, 1=M] | R 2""", summary(p.kotlin("tryValue")));
    }

    @Test
    public void ifBlockValue() {
        p.assertSameAsJava("ifBlockValue");
        assertEquals("""
                return ifBlockValue: D:-, A:[2] | R -
                s: D:-, A:[] | R 1-E, 1.0.0
                v: D:0, A:[1.0.1, 1.1.0, 1=M] | R 2""", summary(p.kotlin("ifBlockValue")));
    }

    /* inside a loop body too: `sum += v` knows `v` */
    @Test
    public void tryValueInLoop() {
        String expected = """
                sum: D:0, A:[0, 1.0.2] | R 1.0.2
                v: D:1.0.0, A:[1.0.1.0.0] | R 1.0.2
                x: D:1+E, A:[1+E] | R 1.0.1.0.0
                xs: D:-, A:[] | R 1-E""";
        Block javaLoop = p.java("tryValueInLoop").methodBody().statements().get(1).block();
        assertEquals(expected, summary(VariableDataImpl.of(javaLoop.statements().getLast())));
        Block kotlinLoop = p.kotlin("tryValueInLoop").methodBody().statements().get(1).block();
        assertEquals(expected, summary(VariableDataImpl.of(kotlinLoop.statements().getLast())));
    }
}
