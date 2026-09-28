package io.codelaser.maddi.modification.analyzer.kotlin;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/*
 How a Kotlin object is constructed, and what that does to the verdicts: a constructor property, a property
 initializer that reads a constructor parameter, an `init` block, and a vararg property. kotlinc emits all of these
 into the primary constructor; the Java twins say so.
 */
public class TestKotlinAnalyzerConstruction extends CommonKotlinAnalyzerTest {

    @Language("kotlin")
    private static final String KOTLIN = """
            package k
            class KeepCtor(private val items: MutableList<StringBuilder>) { fun items(): MutableList<StringBuilder> = items }
            class Keep(xs: MutableList<StringBuilder>) { private val items = xs; fun items(): MutableList<StringBuilder> = items }
            class SnapInit(xs: List<StringBuilder>) { private val items: List<StringBuilder>; init { items = java.util.List.copyOf(xs) }; fun items(): List<StringBuilder> = items }
            class Varg(vararg val xs: String) { fun first(): String = xs[0] }
            """;

    @Language("java")
    private static final String JAVA = """
            package j;
            import java.util.List;
            final class KeepCtor { private final List<StringBuilder> items; KeepCtor(List<StringBuilder> items) { this.items = items; } List<StringBuilder> items() { return items; } }
            final class Keep { private final List<StringBuilder> items; Keep(List<StringBuilder> xs) { this.items = xs; } List<StringBuilder> items() { return items; } }
            final class SnapInit { private final List<StringBuilder> items; SnapInit(List<StringBuilder> xs) { { this.items = List.copyOf(xs); } } List<StringBuilder> items() { return items; } }
            final class SnapFlat { private final List<StringBuilder> items; SnapFlat(List<StringBuilder> xs) { this.items = List.copyOf(xs); } List<StringBuilder> items() { return items; } }
            final class Varg { private final String[] xs; Varg(String... xs) { this.xs = xs; } String[] getXs() { return xs; } String first() { return xs[0]; } }
            """;

    private static Analyzed a;

    @BeforeAll
    public static void beforeAll() {
        a = analyze(KOTLIN, JAVA);
    }

    @Test
    public void constructorProperty() {
        a.assertSameAsJava("KeepCtor");
        assertEquals("""
                type KeepCtor: @FinalFields @Dependent
                constructor: nonModifying=false @Independent | 0: unmodified=true @Dependent
                field items: final=true unmodified=true @Dependent
                method items: nonModifying=true @Dependent""", a.kotlin("KeepCtor"));
    }

    /*
     maddi#85 (fixed 2026-09-28): `private val items = xs` reads the constructor parameter, so it is code of the
     constructor, as kotlinc compiles it; the constructor stores its parameter, which reads @Dependent as in Java.
     */
    @Test
    public void propertyInitializer() {
        a.assertSameAsJava("Keep");
        assertEquals("constructor: nonModifying=false @Independent | 0: unmodified=true @Dependent",
                a.kotlin("Keep", "<init>").lines().skip(1).findFirst().orElseThrow());
    }

    /*
     maddi#84 (fixed 2026-09-28, Java too): a field assigned in a nested block of the constructor -- every Kotlin
     init block -- now gets its links merged into the block statement, and so its independence; the nested-block
     twin and the flat constructor give the same verdicts.
     */
    @Test
    public void initBlock() {
        a.assertSameAsJava("SnapInit");
        assertEquals("type SnapFlat: @Immutable(hc=true) @Independent(hc=true)",
                a.java("SnapFlat").lines().findFirst().orElseThrow());
        assertEquals("type SnapInit: @Immutable(hc=true) @Independent(hc=true)", a.kotlin("SnapInit").lines().findFirst().orElseThrow());
        assertEquals("field items: final=true unmodified=true @Independent(hc=true)", a.kotlin("SnapInit", "items").lines().skip(1)
                .filter(l -> l.startsWith("field")).findFirst().orElseThrow());
    }

    /* maddi#86 (fixed 2026-09-28): the constructor parameter of `vararg val xs: String` is a String[] varargs parameter */
    @Test
    public void varargProperty() {
        assertEquals("Type String[]", a.kotlinType("Varg").findConstructor(1).parameters().getFirst().parameterizedType()
                .toString());
        assertEquals("Type String[]", a.javaType("Varg").findConstructor(1).parameters().getFirst().parameterizedType()
                .toString());
        a.assertSameAsJava("Varg");
        assertEquals("type Varg: @FinalFields @Dependent", a.kotlin("Varg").lines().findFirst().orElseThrow());
    }
}
