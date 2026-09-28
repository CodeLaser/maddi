package io.codelaser.maddi.modification.analyzer.kotlin;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/*
 Methods and functions: a function-typed parameter (FunctionN, the Java twin a Consumer), a function stored in a
 field, top-level extension functions (static methods of the XKt facade, receiver first), and the shapes that are
 wrong one layer down but agree here: a non-local return (#65), a `var` assigned in a lambda (#72), a property with a
 custom setter written from outside (#82). Their verdicts agree with Java on these fixtures, which is not evidence
 that the lower-layer defects are harmless: the fixtures' results are all @Independent / unmodified either way.
 */
public class TestKotlinAnalyzerFunctions extends CommonKotlinAnalyzerTest {

    @Language("kotlin")
    private static final String KOTLIN = """
            package k
            class Each { fun each(xs: List<StringBuilder>, f: (StringBuilder) -> Unit) { for (x in xs) f(x) } }
            class Fn(private val f: (String) -> Int) { fun apply(s: String): Int = f(s) }
            class Finder { fun find(xs: List<StringBuilder>): StringBuilder? { xs.forEach { if (it.isEmpty()) return it }; return null } }
            class Collector { fun last(xs: List<StringBuilder>): StringBuilder? { var r: StringBuilder? = null; xs.forEach { r = it }; return r } }
            class Logged { var count = 0; var sb: StringBuilder = StringBuilder()
                set(v) { count++; field = v } }
            class UsesLogged { fun put(l: Logged, s: StringBuilder) { l.sb = s } }
            fun MutableList<String>.addX() { add("x") }
            fun List<String>.firstOrX(): String = if (isEmpty()) "x" else get(0)
            """;

    @Language("java")
    private static final String JAVA = """
            package j;
            import java.util.List;
            import java.util.function.Consumer;
            import java.util.function.Function;
            final class Each { void each(List<StringBuilder> xs, Consumer<StringBuilder> f) { for (StringBuilder x : xs) f.accept(x); } }
            final class Fn { private final Function<String, Integer> f; Fn(Function<String, Integer> f) { this.f = f; } int apply(String s) { return f.apply(s); } }
            final class Finder { StringBuilder find(List<StringBuilder> xs) { for (StringBuilder x : xs) { if (x.isEmpty()) return x; } return null; } }
            final class Collector { StringBuilder last(List<StringBuilder> xs) { StringBuilder r = null; for (StringBuilder x : xs) { r = x; } return r; } }
            final class Logged { private int count; private StringBuilder sb = new StringBuilder(); int getCount() { return count; } void setCount(int c) { count = c; } StringBuilder getSb() { return sb; } void setSb(StringBuilder v) { count++; sb = v; } }
            final class UsesLogged { void put(Logged l, StringBuilder s) { l.setSb(s); } }
            final class XKt { static void addX(List<String> receiver) { receiver.add("x"); } static String firstOrX(List<String> receiver) { return receiver.isEmpty() ? "x" : receiver.get(0); } }
            """;

    private static Analyzed a;

    @BeforeAll
    public static void beforeAll() {
        a = analyze(KOTLIN, JAVA);
    }

    @Test
    public void functionTypedParameter() {
        a.assertSameAsJava("Each");
    }

    @Test
    public void functionInAField() {
        a.assertSameAsJava("Fn");
    }

    /* the facade has no constructor; the extension functions are compared by name */
    @Test
    public void extensionFunctions() {
        a.assertSameAsJava("XKt", "addX", "firstOrX");
    }

    @Test
    public void lowerLayerDefectsAgreeHere() {
        a.assertSameAsJava("Finder");
        a.assertSameAsJava("Collector");
        a.assertSameAsJava("Logged");
        a.assertSameAsJava("UsesLogged");
    }
}
