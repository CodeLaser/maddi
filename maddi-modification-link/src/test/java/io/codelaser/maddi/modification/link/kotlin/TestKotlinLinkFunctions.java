package io.codelaser.maddi.modification.link.kotlin;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/*
 Function types and lambdas. A Kotlin function type is kotlin.jvm.functions.FunctionN: the Java twins that take a
 Function1 link exactly as Kotlin does, those that take a java.util.function type do not (#80). What Kotlin's
 lambdas can do and Java's cannot: return from the enclosing function (#65) and assign an enclosing `var` (#72, now a
 Ref holder, whose write reaches the creator since #94).
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
                StringBuilder capturedLoopInit(List<StringBuilder> xs, StringBuilder a) { StringBuilder r = a; for (StringBuilder x : xs) { r = x; } return r; }
                StringBuilder capturedRefInit(List<StringBuilder> xs, StringBuilder a) { kotlin.jvm.internal.Ref.ObjectRef<StringBuilder> r = new kotlin.jvm.internal.Ref.ObjectRef<>(); r.element = a; xs.forEach(x -> { r.element = x; }); return r.element; }
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
     The lambda assigns the enclosing `var r`: a Ref holder, `r.element = it`, as kotlinc compiles it (#72). A lambda's
     write through a captured holder -- a Ref field, an array element -- now reaches its creator (#94): the lambda's
     summary keeps its parameter's link to the closure variable, and a Consumer applied to a collection's elements
     stores one of them there. The Java holder forms link as the `for` loop does; they also keep the holder's earlier
     value (`←1:a`), which the loop form drops, at the price of an over-approximate parameter link (`1:a∈0:xs.§$s`).
     */
    @Test
    public void varAssignedInLambda() {
        assertEquals("[-] --> captured∈0:xs.§$s", p.javaLinks("captured"));
        assertEquals("[-] --> capturedRef1∈0:xs.§$s", p.javaLinks("capturedRef1"));
        assertEquals("[0:xs.§$s∋$_ce1] --> capturedRef←$_ce1,capturedRef∈0:xs.§$s,capturedRef.§m~0:xs.§m",
                p.javaLinks("capturedRef"));
        assertEquals("[-] --> capturedArr[0]∈0:xs.§$s,capturedArr[0].§m~0:xs.§m", p.javaLinks("capturedArr"));
        assertEquals("[-, -] --> capturedLoopInit∈0:xs.§$s", p.javaLinks("capturedLoopInit"));
        assertEquals("[0:xs.§$s∋1:a,0:xs.§m~1:a.§m, 1:a∈0:xs.§$s,1:a.§m~0:xs.§m] --> capturedRefInit←1:a,"
                     + "capturedRefInit∈0:xs.§$s,capturedRefInit.§m←1:a.§m,capturedRefInit.§m~0:xs.§m",
                p.javaLinks("capturedRefInit"));
    }

    /*
     Kotlin's `forEach` is the stdlib's static extension CollectionsKt.forEach(Iterable, Function1<T, Unit>). Its
     derived contract roots the consumer at the receiver parameter as Java's Iterable.forEach roots it at 'this'
     (`0:$receiver.§ts⊇Λ1:action`), the call applies it with the first argument as the object, and a Unit-returning
     Function1 lifts as a Consumer (#94). `captured` links as the Java `capturedRef`, in another order.
     */
    @Test
    public void kotlinForEachIsContracted() {
        java.util.List<String> lambdaLinks = new java.util.ArrayList<>();
        p.kotlin("captured").methodBody().visit(e -> {
            if (e instanceof io.codelaser.maddi.cst.api.expression.Lambda l) lambdaLinks.add(p.links(l.methodInfo()));
            return true;
        });
        assertEquals("[[0:it.§m≡r*.element.§m,0:it→r*.element] --> -]", lambdaLinks.toString());
        assertEquals("[0:xs.§$s∋$_ce1] --> captured.§m~0:xs.§m,captured←$_ce1,captured∈0:xs.§$s",
                p.kotlinLinks("captured"));
    }
}
