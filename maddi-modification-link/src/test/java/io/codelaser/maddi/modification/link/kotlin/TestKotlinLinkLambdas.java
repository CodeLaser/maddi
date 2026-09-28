package io.codelaser.maddi.modification.link.kotlin;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
 The FunctionN path: a lambda passed to our own higher-order function, a lambda and a bound method reference as
 return values, and SAM conversion of a Kotlin lambda to a JDK functional interface (removeIf, computeIfAbsent,
 stream().filter). The Java twins pass kotlin.jvm.functions types where Kotlin has a function type, and a
 java.util.function lambda where Kotlin SAM-converts. And a Kotlin call into a Java-source class (#68).
 */
public class TestKotlinLinkLambdas extends CommonKotlinLinkTest {

    @Language("kotlin")
    private static final String KOTLIN = """
            package k
            class X(private val sb: StringBuilder) {
                fun each(xs: List<StringBuilder>, f: (StringBuilder) -> Unit) { for (x in xs) f(x) }
                fun addAllTo(xs: List<StringBuilder>, out: MutableList<StringBuilder>) { each(xs) { out.add(it) } }
                fun supplier(): () -> StringBuilder = { sb }
                fun appender(): (String) -> StringBuilder = sb::append
                fun dropEmpty(xs: MutableList<StringBuilder>) { xs.removeIf { it.isEmpty() } }
                fun bucket(m: MutableMap<String, MutableList<StringBuilder>>, k: String): MutableList<StringBuilder> =
                    m.computeIfAbsent(k) { ArrayList() }
                fun firstMatch(xs: List<StringBuilder>): StringBuilder? = xs.stream().filter { it.isEmpty() }.findFirst().orElse(null)
            }
            """;

    @Language("java")
    private static final String JAVA = """
            package j;
            import java.util.ArrayList;
            import java.util.List;
            import java.util.Map;
            import kotlin.Unit;
            import kotlin.jvm.functions.Function0;
            import kotlin.jvm.functions.Function1;
            final class X {
                private final StringBuilder sb;
                X(StringBuilder sb) { this.sb = sb; }
                void each(List<StringBuilder> xs, Function1<StringBuilder, Unit> f) { for (StringBuilder x : xs) f.invoke(x); }
                void addAllTo(List<StringBuilder> xs, List<StringBuilder> out) { each(xs, it -> { out.add(it); return Unit.INSTANCE; }); }
                Function0<StringBuilder> supplier() { return () -> sb; }
                Function1<String, StringBuilder> appender() { return sb::append; }
                void dropEmpty(List<StringBuilder> xs) { xs.removeIf(it -> it.isEmpty()); }
                List<StringBuilder> bucket(Map<String, List<StringBuilder>> m, String k) { return m.computeIfAbsent(k, it -> new ArrayList<>()); }
                StringBuilder firstMatch(List<StringBuilder> xs) { return xs.stream().filter(it -> it.isEmpty()).findFirst().orElse(null); }
            }
            """;

    private static Parsed p;

    @BeforeAll
    public static void beforeAll() {
        p = link(KOTLIN, JAVA);
    }

    @Test
    public void ownHigherOrderFunction() {
        p.assertSameAsJava("each");
        // the lambda's `out.add(it)` reaches neither `xs` nor `out`, on both sides
        p.assertSameAsJava("addAllTo");
        assertEquals("[-, -] --> -", p.kotlinLinks("addAllTo"));
    }

    @Test
    public void lambdaAsValue() {
        p.assertSameAsJava("supplier");
        assertEquals("[] --> supplier←Λ$_fi0", p.kotlinLinks("supplier"));
    }

    /*
     ⛔ maddi#92: `sb::append` is typed kotlin.reflect.KFunction<String,StringBuilder>, not a functional interface,
     so the link to it loses its Λ. When #92 is fixed, this becomes `p.assertSameAsJava("appender")`.
     */
    @Test
    public void boundMethodReference() {
        assertEquals("[] --> appender←Λ$_fi0", p.javaLinks("appender"));
        assertEquals("[] --> appender←$_fi0", p.kotlinLinks("appender"));
        assertEquals("Type kotlin.reflect.KFunction<String,StringBuilder>", p.kotlin("appender").methodBody()
                .statements().getFirst().expression().parameterizedType().toString());
    }

    @Test
    public void samConversionToTheJdk() {
        p.assertSameAsJava("dropEmpty");
        p.assertSameAsJava("bucket");
        p.assertSameAsJava("firstMatch");
        assertEquals("[0:xs.§$s∋$_ce6] --> firstMatch←$_ce6,firstMatch∈0:xs.§$s", p.kotlinLinks("firstMatch"));
    }

    @Language("kotlin")
    private static final String KOTLIN_TO_JAVA_SOURCE = """
            package k
            class X { fun fill(out: MutableList<StringBuilder>, s: StringBuilder) { j.Helper.put(out, s) }
                      fun each(xs: List<StringBuilder>, out: MutableList<StringBuilder>) { j.Helper.forAll(xs) { out.add(it) } } }
            """;

    @Language("java")
    private static final String JAVA_HELPER = """
            package j;
            import java.util.List;
            import java.util.function.Consumer;
            final class X { }
            final class Helper {
                static void put(List<StringBuilder> out, StringBuilder s) { out.add(s); }
                static void forAll(List<StringBuilder> xs, Consumer<StringBuilder> c) { for (StringBuilder x : xs) c.accept(x); }
            }
            """;

    /*
     ⛔ maddi#68: a Kotlin call to a member of a Java-source class is a k2-unresolved-call placeholder, arguments and
     lambda included, so there is nothing to link and the harness refuses the fixture. When #68 is fixed, this becomes a
     differential test against a Java caller of Helper.
     */
    @Test
    public void callIntoJavaSource() {
        AssertionError e = assertThrows(AssertionError.class, () -> link(KOTLIN_TO_JAVA_SOURCE, JAVA_HELPER));
        assertTrue(e.getMessage().contains("k2-unresolved-call:put"), e.getMessage());
        assertTrue(e.getMessage().contains("k2-unresolved-call:forAll"), e.getMessage());
    }
}
