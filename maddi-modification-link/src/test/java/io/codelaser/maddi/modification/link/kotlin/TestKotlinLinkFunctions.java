package io.codelaser.maddi.modification.link.kotlin;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/*
 Function types and lambdas. A Kotlin function type is kotlin.jvm.functions.FunctionN: the Java twins that take a
 Function1 link exactly as Kotlin does, those that take a java.util.function type do not (#80). What Kotlin's
 lambdas can do and Java's cannot: return from the enclosing function (#65) and assign an enclosing `var` (#72, now a
 Ref holder; #94).
 */
public class TestKotlinLinkFunctions extends CommonKotlinLinkTest {

    @Language("kotlin")
    private static final String KOTLIN = """
            package k
            class X {
                fun apply(f: (StringBuilder) -> Unit, s: StringBuilder) { f(s) }
                fun applyR(f: (StringBuilder) -> StringBuilder, s: StringBuilder): StringBuilder = f(s)
                fun applyConsumer(f: (StringBuilder) -> Unit, s: StringBuilder) { f(s) }
                fun applyFunction(f: (StringBuilder) -> StringBuilder, s: StringBuilder): StringBuilder = f(s)
                fun find(xs: List<StringBuilder>): StringBuilder? { xs.forEach { if (it.isEmpty()) return it }; return null }
                fun captured(xs: List<StringBuilder>): StringBuilder? { var r: StringBuilder? = null; xs.forEach { r = it }; return r }
            }
            """;

    @Language("java")
    private static final String JAVA = """
            package j;
            import java.util.List;
            import java.util.function.Consumer;
            import java.util.function.Function;
            import kotlin.Unit;
            import kotlin.jvm.functions.Function1;
            class X {
                void apply(Function1<StringBuilder, Unit> f, StringBuilder s) { f.invoke(s); }
                StringBuilder applyR(Function1<StringBuilder, StringBuilder> f, StringBuilder s) { return f.invoke(s); }
                void applyConsumer(Consumer<StringBuilder> f, StringBuilder s) { f.accept(s); }
                StringBuilder applyFunction(Function<StringBuilder, StringBuilder> f, StringBuilder s) { return f.apply(s); }
                StringBuilder find(List<StringBuilder> xs) { for (StringBuilder x : xs) { if (x.isEmpty()) return x; } return null; }
                StringBuilder captured(List<StringBuilder> xs) { StringBuilder r = null; for (StringBuilder x : xs) { r = x; } return r; }
                StringBuilder capturedRef(List<StringBuilder> xs) { kotlin.jvm.internal.Ref.ObjectRef<StringBuilder> r = new kotlin.jvm.internal.Ref.ObjectRef<>(); r.element = null; xs.forEach(x -> { r.element = x; }); return r.element; }
                StringBuilder capturedRef1(List<StringBuilder> xs) { kotlin.jvm.internal.Ref.ObjectRef<StringBuilder> r = new kotlin.jvm.internal.Ref.ObjectRef<>(); r.element = xs.get(0); return r.element; }
                StringBuilder[] capturedArr(List<StringBuilder> xs) { StringBuilder[] r = new StringBuilder[1]; xs.forEach(x -> { r[0] = x; }); return r; }
            }
            """;

    private static Parsed p;

    @BeforeAll
    public static void beforeAll() {
        p = link(KOTLIN, JAVA);
    }

    /* Kotlin's `f(s)` is `f.invoke(s)` on a Function1, and links as the Java code calling the same interface */
    @Test
    public void functionTypeAsJavaFunction1() {
        p.assertSameAsJava("apply");
        p.assertSameAsJava("applyR");
        assertEquals("[0:f*.§$$.§$←1:s*, 1:s*→0:f*.§$$.§$] --> -", p.kotlinLinks("apply"));
    }

    /*
     ⛔ maddi#80: FunctionN is a custom functional interface to the link engine, java.util.function a standard
     one. With a Consumer the argument is not modified and the call is an applied-functional-interface link; with
     a Kotlin function type `s` is modified and flows into `f`. A decision, not a defect: if FunctionN becomes
     standard, the Kotlin side must equal these Java lines.
     */
    @Test
    public void functionTypeVersusJavaUtilFunction() {
        assertEquals("[0:f*↗$_afi0, -] --> -", p.javaLinks("applyConsumer"));
        assertEquals("[0:f*.§$$.§$←1:s*, 1:s*→0:f*.§$$.§$] --> -", p.kotlinLinks("applyConsumer"));
        assertEquals("[0:f*↗$_afi0, -] --> applyFunction←$_afi0,applyFunction↖Λ0:f*", p.javaLinks("applyFunction"));
        assertEquals("[0:f*.§$$.§$←1:s*, 1:s*→0:f*.§$$.§$] --> applyFunction→0:f*.§$$.§$",
                p.kotlinLinks("applyFunction"));
    }

    /*
     ⛔ maddi#65: `return it` inside forEach returns from `find`; its value never reaches find's return variable, so
     the result reads as linked to nothing. The Java loop links it to an element of xs. Unsound: an independence
     claim on `find` would be wrong.
     */
    @Test
    public void nonLocalReturn() {
        assertEquals("[0:xs.§$s∋$_ce2] --> find←$_ce2,find∈0:xs.§$s", p.javaLinks("find"));
        assertEquals("[-] --> -", p.kotlinLinks("find"));
    }

    /*
     ⛔ maddi#72: the lambda assigns the enclosing `var r`; the method's return variable only sees `r = null`, so the
     result is not linked to the elements of xs.
     */
    /*
     The lambda assigns the enclosing `var r`: a Ref holder, `r.element = it`, as kotlinc compiles it (#72). It links as
     the Java holder form does. ⛔ maddi#94: neither carries the lambda's write out -- the Java `for` loop links the
     result to the elements of xs -- and without a lambda the holder links (capturedRef1). When #94 is fixed, `captured`
     and `capturedRef` link as the loop does.
     */
    @Test
    public void varAssignedInLambda() {
        assertEquals("[-] --> captured∈0:xs.§$s", p.javaLinks("captured"));
        assertEquals("[-] --> -", p.javaLinks("capturedRef"));
        assertEquals("[-] --> -", p.javaLinks("capturedArr"));
        assertEquals("[-] --> capturedRef1∈0:xs.§$s", p.javaLinks("capturedRef1"));
        assertEquals(p.javaLinks("capturedRef"), p.kotlinLinks("captured"));
    }
}
