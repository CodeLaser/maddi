package io.codelaser.maddi.modification.link.kotlin;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/*
 The FunctionN path: a lambda passed to our own higher-order function, a lambda and a bound method reference as
 return values, and SAM conversion of a Kotlin lambda to a JDK functional interface (removeIf, computeIfAbsent,
 stream().filter). The Java twins pass kotlin.jvm.functions types where Kotlin has a function type, and a
 java.util.function lambda where Kotlin SAM-converts. And Kotlin calls into a Java-source class.
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

    /* a bound callable reference is typed as the FunctionN it is on the JVM, and links as Java's method reference (#92) */
    @Test
    public void boundMethodReference() {
        p.assertSameAsJava("appender");
        assertEquals("[] --> appender←Λ$_fi0", p.kotlinLinks("appender"));
        assertEquals("Type kotlin.jvm.functions.Function1<String,StringBuilder>", p.kotlin("appender").methodBody()
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
            class X { fun fill(out: MutableList<StringBuilder>, s: StringBuilder) { j.X.Helper.put(out, s) }
                      fun each(xs: List<StringBuilder>, out: MutableList<StringBuilder>) { j.X.Helper.forAll(xs) { out.add(it) } }
                      fun viaInstance(out: MutableList<StringBuilder>, s: StringBuilder) { j.X().add(out, s) } }
            """;

    @Language("java")
    private static final String JAVA_HELPER = """
            package j;
            import java.util.List;
            import java.util.function.Consumer;
            public final class X {
                public void add(List<StringBuilder> out, StringBuilder s) { out.add(s); }
                void fill(List<StringBuilder> out, StringBuilder s) { Helper.put(out, s); }
                void each(List<StringBuilder> xs, List<StringBuilder> out) { Helper.forAll(xs, it -> out.add(it)); }
                void viaInstance(List<StringBuilder> out, StringBuilder s) { new X().add(out, s); }
                public static final class Helper {
                    public static void put(List<StringBuilder> out, StringBuilder s) { out.add(s); }
                    public static void forAll(List<StringBuilder> xs, Consumer<StringBuilder> c) { for (StringBuilder x : xs) c.accept(x); }
                }
            }
            """;

    /*
     A Kotlin call into a Java-source class (#68's shape, static and instance, with a lambda for a Consumer) is the
     Java front end's MethodInfo, and links as the same call written in Java.
     */
    @Test
    public void callIntoJavaSource() {
        Parsed q = link(KOTLIN_TO_JAVA_SOURCE, JAVA_HELPER);
        q.assertSameAsJava("fill");
        q.assertSameAsJava("each");
        q.assertSameAsJava("viaInstance");
    }
}
