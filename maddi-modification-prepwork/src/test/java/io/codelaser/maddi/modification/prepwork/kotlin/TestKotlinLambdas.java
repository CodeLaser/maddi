package io.codelaser.maddi.modification.prepwork.kotlin;

import io.codelaser.maddi.cst.api.expression.Lambda;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.statement.Statement;
import io.codelaser.maddi.modification.prepwork.variable.VariableData;
import io.codelaser.maddi.modification.prepwork.variable.impl.VariableDataImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/*
 Lambdas: a Kotlin lambda is a Function1 whose body prep analyses as its own method, with the enclosing method's
 variables carried in. What Kotlin can do and Java cannot is where the gaps are: assign an enclosing `var` (#72)
 and return from the enclosing function (#65). A local `fun` is lowered to a lambda held in a local variable.
 */
public class TestKotlinLambdas extends CommonKotlinTest {

    @Language("kotlin")
    private static final String KOTLIN = """
            package k
            class X {
                fun capturedRead(xs: List<String>, p: String): Int {
                    val n = xs.count { it == p }
                    return n
                }
                fun capturedVar(xs: List<String>, p: String): Int {
                    var n = 0
                    xs.forEach { if (it == p) n++ }
                    return n
                }
                fun nonLocal(xs: List<Int>): Int {
                    xs.forEach { if (it > 2) return it }
                    return 0
                }
                fun localFun(a: Int): Int {
                    fun twice(b: Int) = a + b + b
                    return twice(3)
                }
                fun scope(s: String?): Int {
                    val n = s?.let { it.length + 1 } ?: 0
                    return n
                }
            }
            """;

    @Language("java")
    private static final String JAVA = """
            package j;
            import java.util.List;
            import java.util.function.IntUnaryOperator;
            class X {
                int capturedRead(List<String> xs, String p) {
                    int n = (int) xs.stream().filter(it -> it.equals(p)).count();
                    return n;
                }
                int localFun(int a) {
                    IntUnaryOperator twice = b -> a + b + b;
                    return twice.applyAsInt(3);
                }
            }
            """;

    private static Parsed p;

    @BeforeAll
    public static void beforeAll() {
        p = prep(KOTLIN, JAVA);
    }

    private static Lambda onlyLambda(MethodInfo m) {
        List<Lambda> list = new ArrayList<>();
        m.methodBody().visit(e -> {
            if (e instanceof Lambda l) list.add(l);
            return true;
        });
        assertEquals(1, list.size(), "lambdas in " + m);
        return list.getFirst();
    }

    private static String lambdaSummary(MethodInfo m) {
        Statement s0 = onlyLambda(m).methodInfo().methodBody().statements().getFirst();
        return summary(VariableDataImpl.of(s0));
    }

    @Test
    public void capturedRead() {
        p.assertSameAsJava("capturedRead");
        // the lambda is analysed as a method of its own; the captured parameter is read there
        assertEquals("k.X.$0.invoke(String)", onlyLambda(p.kotlin("capturedRead")).methodInfo().fullyQualifiedName());
        assertEquals("""
                it: D:-, A:[] | R 0
                p: D:-, A:[] | R 0
                return invoke: D:-, A:[0] | R -""", lambdaSummary(p.kotlin("capturedRead")));
        // the lambda's own variables do not leak into the enclosing statement
        assertEquals("k.X.capturedRead(java.util.List,String):0:xs, k.X.capturedRead(java.util.List,String):1:p, n",
                VariableDataImpl.of(p.kotlin("capturedRead").methodBody().statements().getFirst())
                        .knownVariableNamesToString());
    }

    /*
     The lambda assigns the enclosing `var n`: kotlinc's IntRef holder (#72), declared and initialized at 0 and 1, and
     every read and write is one of `n.element`. The lambda's `n.element++` is an assignment at statement 2, the one
     creating the lambda (#94), so `return n.element` reads a value the lambda may have written.
     */
    @Test
    public void capturedVar() {
        MethodInfo m = p.kotlin("capturedVar");
        assertEquals("{IntRef n=new IntRef();n.element=0;CollectionsKt.forEach(xs,it->{if(it.equals(p)){n.element++;}});"
                     + "return n.element;}", m.methodBody().print(p.runtime().qualificationSimpleNames()).toString());
        VariableData last = VariableDataImpl.of(m.methodBody().statements().getLast());
        String element = last.variableInfoStream()
                .filter(vi -> vi.variable().fullyQualifiedName().equals("kotlin.jvm.internal.Ref.IntRef.element#n"))
                .map(vi -> vi.assignments() + " | R " + vi.reads()).findFirst().orElseThrow();
        assertEquals("D:0, A:[1, 2] | R 2, 3", element);
    }

    /*
     ⛔ maddi#65, as prep sees it. `return it` leaves `nonLocal` (ReturnStatement.exitLevels() == 1), but it is
     recorded as an assignment to the LAMBDA's return variable, and the enclosing method's return variable is
     assigned by `return 0` only. The front end marks the return; nothing downstream reads the mark yet.
     */
    @Test
    public void nonLocalReturn() {
        MethodInfo nonLocal = p.kotlin("nonLocal");
        assertEquals("""
                it: D:-, A:[] | R 0-E, 0.0.0
                return invoke: D:-, A:[0.0.0] | R -""", lambdaSummary(nonLocal));
        assertEquals("""
                return nonLocal: D:-, A:[1] | R -
                xs: D:-, A:[] | R 0""", summary(nonLocal));
    }

    /* a local `fun` is a Function1 in a local variable: the Java twin holds an IntUnaryOperator */
    @Test
    public void localFun() {
        p.assertSameAsJava("localFun");
        assertEquals("""
                a: D:-, A:[] | R 0
                b: D:-, A:[] | R 0
                return invoke: D:-, A:[0] | R -""", lambdaSummary(p.kotlin("localFun")));
    }

    /*
     The null-safe hoisting shape: `val n = s?.let { … } ?: 0` is `$nullSafe0 = …` and `int n = …`, siblings 0 and 1
     (#69: 0.0 and 0.1), so `return n` at 2 reads `n`.
     */
    @Test
    public void scope() {
        MethodInfo scope = p.kotlin("scope");
        assertEquals(3, scope.methodBody().statements().size());
        assertEquals("$nullSafe0, k.X.scope(String), k.X.scope(String):0:s, n",
                VariableDataImpl.of(scope.methodBody().statements().getLast()).knownVariableNamesToString());
        assertEquals("""
                $nullSafe0: D:0, A:[0] | R 1
                n: D:1, A:[1] | R 2
                return scope: D:-, A:[2] | R -
                s: D:-, A:[] | R 0""", summary(scope));
    }
}
