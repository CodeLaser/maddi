package io.codelaser.maddi.modification.link.kotlin;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/*
 Class delegation: kotlinc stores the delegate in `$$delegate_0` and synthesizes one forwarder per interface method.
 The forwarders link as the Java twin; the constructor does not assign the field (#90).
 */
public class TestKotlinLinkDelegation extends CommonKotlinLinkTest {

    @Language("kotlin")
    private static final String KOTLIN = """
            package k
            interface Sink { fun put(s: StringBuilder); fun last(): StringBuilder }
            class X(d: Sink) : Sink by d
            """;

    @Language("java")
    private static final String JAVA = """
            package j;
            interface Sink { void put(StringBuilder s); StringBuilder last(); }
            final class X implements Sink {
                private final Sink $$delegate_0;
                X(Sink d) { this.$$delegate_0 = d; }
                public void put(StringBuilder s) { $$delegate_0.put(s); }
                public StringBuilder last() { return $$delegate_0.last(); }
            }
            """;

    private static Parsed p;

    @BeforeAll
    public static void beforeAll() {
        p = link(KOTLIN, JAVA);
    }

    @Test
    public void forwarders() {
        p.assertSameAsJava("put");
        p.assertSameAsJava("last");
    }

    /* #90 (fixed by #85, 2026-09-28): the constructor stores the delegate, `this.$$delegate_0 = d`, as kotlinc's does */
    @Test
    public void constructorStoresTheDelegate() {
        p.assertSameAsJava("<init>");
        assertEquals("[0:d.§m≡this*.$$delegate_0.§m,0:d→this*.$$delegate_0] --> -", p.kotlinLinks("<init>"));
    }
}
