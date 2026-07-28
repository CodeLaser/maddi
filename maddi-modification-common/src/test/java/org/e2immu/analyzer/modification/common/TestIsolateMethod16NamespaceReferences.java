package org.e2immu.analyzer.modification.common;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * A type written fully qualified in the pasted text becomes a namespace stub, nested a few levels deep
 * ({@code frame → p → q → Helper}). The pasted text keeps resolving, because it spells the whole chain. But a
 * <i>reconstructed</i> signature — a stub field, a stub method's return or parameter type — is printed with
 * simple names, and a simple name does not reach into a sibling namespace chain. Both spellings have to reach
 * the one declaration, so the reconstructed one has to be printed qualified.
 */
public class TestIsolateMethod16NamespaceReferences extends CommonIsolateMethodTest {

    @Language("java")
    public static final String VALUE = """
            package p.q;
            public class Value {
                public String text;
            }
            """;

    @Language("java")
    public static final String HOLDER = """
            package a.b;
            public class Holder {
                public p.q.Value value;
            }
            """;

    @Language("java")
    public static final String X = """
            package a.b;
            public class X {
                void method(Holder holder, Object o) {
                    holder.value = (p.q.Value) o;
                }
            }
            """;

    private TypeInfo parseAll() {
        return javaInspector.parse(Map.of(
                        "p.q.Value", VALUE,
                        "a.b.Holder", HOLDER,
                        "a.b.X", X
                ), new JavaInspector.ParseOptions.Builder().setDetailedSources(true).build())
                .parseResult().findType("a.b.X");
    }

    @DisplayName("a reconstructed field of a namespace-nested type must be printed qualified")
    @Test
    public void fieldOfNamespaceType() {
        TypeInfo x = parseAll();
        String m = """
                void method(Holder holder, Object o) {
                    holder.value = (p.q.Value) o;
                }""";
        String out = isolate(x, "method", 2, m);
        @Language("java")
        String expected = """
                public class X_method {
                    class Holder { X_method.p.q.Value value; }
                    class p { class q { class Value { } } }
                    void method(Holder holder, Object o) {
                    holder.value = (p.q.Value) o;
                }
                }
                """;
        // 'Value' alone does not resolve from inside Holder; the cast in the pasted text needs the chain
        assertEquals(expected, out);
        javaInspector.invalidateAllSources();
        assertNotNull(javaInspector.parse("X_method", out));
    }
}
