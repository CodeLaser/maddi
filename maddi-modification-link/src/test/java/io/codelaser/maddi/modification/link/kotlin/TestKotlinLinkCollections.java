package io.codelaser.maddi.modification.link.kotlin;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/*
 Collections and maps: Kotlin's `List`/`MutableList`/`Map` are the java.util types, so indexing, `for`, `m[k] = v`
 and a copy constructor must link as their Java spelling does. The kotlin stdlib calls link through the contracts in
 libs/kotlin; their Java twins are the JDK calls with the same meaning.
 */
public class TestKotlinLinkCollections extends CommonKotlinLinkTest {

    @Language("kotlin")
    private static final String KOTLIN = """
            package k
            class X {
                fun first(xs: List<String>): String = xs[0]
                fun copyOf(xs: List<String>): List<String> = ArrayList(xs)
                fun mapGet(m: Map<String, StringBuilder>, k: String): StringBuilder? = m[k]
                fun mapPut(m: MutableMap<String, String>, k: String, v: String) { m[k] = v }
                fun iterate(xs: List<String>): String { for (x in xs) return x; return "" }
                fun listOfIt(a: StringBuilder): List<StringBuilder> = listOf(a)
                fun firstOf(xs: List<StringBuilder>): StringBuilder = xs.first()
                fun toListOf(xs: List<StringBuilder>): List<StringBuilder> = xs.toList()
                fun toSetOf(xs: List<StringBuilder>): Set<StringBuilder> = xs.toSet()
                fun filtered(xs: List<StringBuilder>): List<StringBuilder> = xs.filter { it.isEmpty() }
                fun mutableListOfIt(a: StringBuilder): MutableList<StringBuilder> = mutableListOf(a)
                fun mutableListOf2(a: StringBuilder, b: StringBuilder): MutableList<StringBuilder> = mutableListOf(a, b)
            }
            """;

    @Language("java")
    private static final String JAVA = """
            package j;
            import java.util.ArrayList;
            import java.util.List;
            import java.util.Map;
            import java.util.Set;
            class X {
                String first(List<String> xs) { return xs.get(0); }
                List<String> copyOf(List<String> xs) { return new ArrayList<>(xs); }
                StringBuilder mapGet(Map<String, StringBuilder> m, String k) { return m.get(k); }
                void mapPut(Map<String, String> m, String k, String v) { m.put(k, v); }
                String iterate(List<String> xs) { for (String x : xs) return x; return ""; }
                List<StringBuilder> listOfIt(StringBuilder a) { return List.of(a); }
                StringBuilder firstOf(List<StringBuilder> xs) { return xs.get(0); }
                List<StringBuilder> toListOf(List<StringBuilder> xs) { return List.copyOf(xs); }
                Set<StringBuilder> toSetOf(List<StringBuilder> xs) { return Set.copyOf(xs); }
                List<StringBuilder> filtered(List<StringBuilder> xs) { return xs.stream().filter(x -> x.isEmpty()).toList(); }
                List<StringBuilder> mutableListOfIt(StringBuilder a) { List<StringBuilder> l = new ArrayList<>(); l.add(a); return l; }
                List<StringBuilder> mutableListOf2(StringBuilder a, StringBuilder b) { return kotlin.collections.CollectionsKt.mutableListOf(a, b); }
            }
            """;

    private static Parsed p;

    @BeforeAll
    public static void beforeAll() {
        p = link(KOTLIN, JAVA);
    }

    @Test
    public void indexAndCopy() {
        p.assertSameAsJava("first");
        p.assertSameAsJava("copyOf");
        assertEquals("[-] --> copyOf.§$s⊆0:xs.§$s,copyOf.§m←0:xs.§m", p.kotlinLinks("copyOf"));
    }

    @Test
    public void maps() {
        p.assertSameAsJava("mapGet");
        p.assertSameAsJava("mapPut");
        assertEquals("[-, -] --> mapGet∈0:m.§$$s[-2]", p.kotlinLinks("mapGet"));
    }

    @Test
    public void forIn() {
        p.assertSameAsJava("iterate");
    }

    /* stdlib calls whose contract links as the JDK twin does */
    @Test
    public void stdlibAgrees() {
        p.assertSameAsJava("listOfIt");
        p.assertSameAsJava("firstOf");
        assertEquals("[-] --> listOfIt.§$s∋0:a", p.kotlinLinks("listOfIt"));
        assertEquals("[-] --> firstOf∈0:xs.§$s", p.kotlinLinks("firstOf"));
    }

    /*
     ⛔ maddi#78 (language-neutral: the same calls from Java link the same way). toList/toSet/filter return a
     collection sharing the receiver's elements, and link to nothing; the JDK twins link `⊆`.
     */
    @Test
    public void stdlibCopiesLinkNothing() {
        for (String name : new String[]{"toListOf", "toSetOf", "filtered"}) {
            assertEquals("[-] --> " + name + ".§$s⊆0:xs.§$s," + name + ".§m←0:xs.§m", p.javaLinks(name), name);
            assertEquals("[-] --> -", p.kotlinLinks(name), name);
        }
    }

    /*
     ⛔ maddi#78: a library vararg call links its LAST argument as if it were the vararg array. A Kotlin source
     vararg method is fine (TestKotlinLinkConditionals.varargs). The Java call of the same library method agrees
     with Kotlin on the wrong answer.
     */
    @Test
    public void stdlibVararg() {
        assertEquals("[-] --> mutableListOfIt.§$s∋0:a", p.javaLinks("mutableListOfIt"));
        assertEquals("[-] --> mutableListOfIt.§$s⊆0:a.§$s,mutableListOfIt.§m←0:a.§m", p.kotlinLinks("mutableListOfIt"));
        p.assertSameAsJava("mutableListOf2");
        assertEquals("[-, -] --> mutableListOf2.§$s⊆1:b.§$s,mutableListOf2.§m←1:b.§m", p.kotlinLinks("mutableListOf2"));
    }
}
