package io.codelaser.maddi.modification.analyzer.kotlin;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/*
 Control and declaration shapes that kotlinc lowers: safe calls, elvis and `!!`, `when` with `is` arms (smart casts),
 destructuring a map entry in a for loop, `buildList`, a local function, interface and abstract properties, a nested
 class, and a companion factory in front of a private constructor.
 */
public class TestKotlinAnalyzerControl extends CommonKotlinAnalyzerTest {

    @Language("kotlin")
    private static final String KOTLIN = """
            package k
            class Nulls {
                fun safeAppend(sb: StringBuilder?) { sb?.append("x") }
                fun lengthOr(sb: StringBuilder?): Int = sb?.length ?: 0
                fun orNew(sb: StringBuilder?): StringBuilder = sb ?: StringBuilder()
                fun bang(sb: StringBuilder?) { sb!!.append("x") }
            }
            class Whens {
                fun kind(o: Any): Int = when (o) { is StringBuilder -> { o.append("x"); 1 } is String -> 2; else -> 3 }
                fun pick(o: Any): StringBuilder? = when (o) { is StringBuilder -> o; else -> null }
            }
            class Maps {
                fun total(m: Map<String, Int>): Int { var s = 0; for ((_, v) in m) s += v; return s }
                fun clearAll(m: Map<String, MutableList<String>>) { for ((_, v) in m) v.clear() }
                fun build(a: String, b: String): List<String> = buildList { add(a); add(b) }
            }
            class Locals {
                fun twice(sb: StringBuilder) { fun once() { sb.append("x") }; once(); once() }
            }
            interface Named { val name: String }
            abstract class Animal : Named { abstract val legs: Int; fun describe(): String = name + legs }
            class Dog : Animal() { override val name = "dog"; override val legs = 4 }
            class Outer { class Nested(val sb: StringBuilder) { fun add() { sb.append("x") } } }
            class Made private constructor(val items: List<String>) {
                companion object { fun of(vararg xs: String): Made = Made(xs.toList()) }
            }
            """;

    @Language("java")
    private static final String JAVA = """
            package j;
            import java.util.ArrayList;
            import java.util.List;
            import java.util.Map;
            final class Nulls {
                void safeAppend(StringBuilder sb) { if (sb != null) sb.append("x"); }
                int lengthOr(StringBuilder sb) { return sb != null ? sb.length() : 0; }
                StringBuilder orNew(StringBuilder sb) { return sb != null ? sb : new StringBuilder(); }
                void bang(StringBuilder sb) { java.util.Objects.requireNonNull(sb).append("x"); }
            }
            final class Whens {
                int kind(Object o) { if (o instanceof StringBuilder sb) { sb.append("x"); return 1; } else if (o instanceof String) return 2; else return 3; }
                StringBuilder pick(Object o) { if (o instanceof StringBuilder sb) return sb; else return null; }
            }
            final class Maps {
                int total(Map<String, Integer> m) { int s = 0; for (Map.Entry<String, Integer> e : m.entrySet()) s += e.getValue(); return s; }
                void clearAll(Map<String, List<String>> m) { for (Map.Entry<String, List<String>> e : m.entrySet()) e.getValue().clear(); }
                List<String> build(String a, String b) { List<String> l = new ArrayList<>(); l.add(a); l.add(b); return l; }
            }
            final class Locals {
                void twice(StringBuilder sb) { sb.append("x"); sb.append("x"); }
            }
            interface Named { String getName(); }
            abstract class Animal implements Named { abstract int getLegs(); String describe() { return getName() + getLegs(); } }
            final class Dog extends Animal { private final String name = "dog"; private final int legs = 4; public String getName() { return name; } int getLegs() { return legs; } }
            final class Outer { static final class Nested { private final StringBuilder sb; Nested(StringBuilder sb) { this.sb = sb; } StringBuilder getSb() { return sb; } void add() { sb.append("x"); } } }
            final class Made {
                private final List<String> items;
                private Made(List<String> items) { this.items = items; }
                List<String> getItems() { return items; }
                static final class Companion { Made of(String... xs) { return new Made(List.of(xs)); } }
            }
            """;

    private static Analyzed a;

    @BeforeAll
    public static void beforeAll() {
        a = analyze(KOTLIN, JAVA);
    }

    @Test
    public void nullability() {
        a.assertSameAsJava("Nulls");
    }

    @Test
    public void whenWithSmartCasts() {
        a.assertSameAsJava("Whens");
    }

    /* the loop over a map's entries and buildList agree; `clearAll` reads `m` unmodified on both sides */
    @Test
    public void mapsAndBuilders() {
        a.assertSameAsJava("Maps");
    }

    @Test
    public void localFunction() {
        a.assertSameAsJava("Locals");
    }

    @Test
    public void propertiesInInterfacesAndAbstractClasses() {
        a.assertSameAsJava("Named");
        a.assertSameAsJava("Animal");
        a.assertSameAsJava("Dog");
    }

    @Test
    public void nestedClass() {
        a.assertSameAsJava("Outer.Nested");
    }

    /*
     ⛔ maddi#87: the only caller of the private constructor passes `xs.toList()`, which is not a recognised copy, where
     the Java twin's `List.of(xs)` is: Java concludes @Immutable(hc=true) from the call site, Kotlin @FinalFields.
     The companion itself agrees. When #87 is fixed, this becomes `a.assertSameAsJava("Made", "<init>", "items",
     "getItems")`.
     */
    @Test
    public void companionFactory() {
        a.assertSameAsJava("Made.Companion");
        assertEquals("""
                type Made: @Immutable(hc=true) @Independent(hc=true)
                constructor: nonModifying=false @Independent | 0: unmodified=true @Independent(hc=true)
                field items: final=true unmodified=true @Independent(hc=true)
                method getItems: nonModifying=true @Independent(hc=true)""", a.java("Made", "<init>", "items", "getItems"));
        assertEquals("""
                type Made: @FinalFields @Dependent
                constructor: nonModifying=false @Independent | 0: unmodified=true @Dependent
                field items: final=true unmodified=true @Dependent
                method getItems: nonModifying=true @Dependent""", a.kotlin("Made", "<init>", "items", "getItems"));
    }
}
