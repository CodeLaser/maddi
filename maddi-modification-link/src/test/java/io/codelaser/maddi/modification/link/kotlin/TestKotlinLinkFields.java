package io.codelaser.maddi.modification.link.kotlin;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/*
 Fields and properties: a primary constructor storing its parameters, the accessors K2 synthesizes, reads and
 writes of a property inside the class (the backing field) and through another instance, and a collection field
 read, modified and viewed.
 */
public class TestKotlinLinkFields extends CommonKotlinLinkTest {

    @Language("kotlin")
    private static final String KOTLIN = """
            package k
            class X(val list: MutableList<String>, var sb: StringBuilder) {
                fun get(i: Int): String = list[i]
                fun add(s: String) { list.add(s) }
                fun sub(n: Int): List<String> = list.subList(0, n)
                fun append(s: String): StringBuilder { sb.append(s); return sb }
                fun setVia(s: StringBuilder, x: X) { x.sb = s }
                fun getVia(x: X): StringBuilder = x.sb
            }
            """;

    @Language("java")
    private static final String JAVA = """
            package j;
            import java.util.List;
            class X {
                private final List<String> list;
                private StringBuilder sb;
                X(List<String> list, StringBuilder sb) { this.list = list; this.sb = sb; }
                List<String> getList() { return list; }
                StringBuilder getSb() { return sb; }
                void setSb(StringBuilder sb) { this.sb = sb; }
                String get(int i) { return list.get(i); }
                void add(String s) { list.add(s); }
                List<String> sub(int n) { return list.subList(0, n); }
                StringBuilder append(String s) { sb.append(s); return sb; }
                void setVia(StringBuilder s, X x) { x.sb = s; }
                StringBuilder getVia(X x) { return x.sb; }
            }
            """;

    private static Parsed p;

    @BeforeAll
    public static void beforeAll() {
        p = link(KOTLIN, JAVA);
    }

    @Test
    public void constructor() {
        p.assertSameAsJava("<init>");
        // K2 and javac insert the two links of a parameter in opposite order; the raw Kotlin string, unnormalized:
        assertEquals("[0:list.§m≡this*.list.§m,0:list→this*.list, 1:sb.§m≡this*.sb.§m,1:sb→this*.sb] --> -",
                p.kotlinLinks("<init>"));
    }

    @Test
    public void getters() {
        p.assertSameAsJava("getList");
        p.assertSameAsJava("getSb");
        assertEquals("[] --> getSb←this.sb", p.kotlinLinks("getSb"));
    }

    /*
     maddi#77 (fixed 2026-09-28): the synthesized setter's statement is statement "0", and it links as the Java twin
     does. The strings differ in the parameter's NAME only: kotlinc's synthesized setter calls it `value`.
     */
    @Test
    public void propertySetter() {
        assertEquals("[0:sb→this*.sb,0:sb.§m≡this*.sb.§m] --> -", p.javaLinks("setSb"));
        assertEquals("[0:value.§m≡this*.sb.§m,0:value→this*.sb] --> -", p.kotlinLinks("setSb"));
    }

    @Test
    public void collectionField() {
        p.assertSameAsJava("get");
        p.assertSameAsJava("add");
        p.assertSameAsJava("sub");
        assertEquals("[0:s∈this.list*.§$s] --> -", p.kotlinLinks("add"));
        assertEquals("[-] --> sub.§$s⊆this.list.§$s,sub.§m≡this.list.§m", p.kotlinLinks("sub"));
    }

    @Test
    public void modifiedAndReturned() {
        p.assertSameAsJava("append");
        assertEquals("[-] --> append←this.sb*", p.kotlinLinks("append"));
    }

    @Test
    public void otherInstance() {
        p.assertSameAsJava("setVia");
        p.assertSameAsJava("getVia");
    }
}
