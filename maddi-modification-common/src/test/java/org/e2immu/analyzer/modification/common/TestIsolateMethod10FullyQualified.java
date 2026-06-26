package org.e2immu.analyzer.modification.common;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

// detailed sources distinguish a package-qualified reference from a simple one: a type written fully qualified
// ('p.q.Helper') must be reproduced as a namespace stub so the verbatim text keeps resolving -- a simple-name
// frame stub (what the import-free fallback would produce) would not
public class TestIsolateMethod10FullyQualified extends CommonIsolateMethodTest {

    @Language("java")
    public static final String HELPER = """
            package p.q;
            public class Helper {
                public static String compute() { return null; }
            }
            """;

    @Language("java")
    public static final String X = """
            package a.b;
            public class X {
                String method() {
                    return p.q.Helper.compute();
                }
            }
            """;

    private TypeInfo parseAll() {
        return javaInspector.parse(Map.of(
                "p.q.Helper", HELPER,
                "a.b.X", X
        ), new JavaInspector.ParseOptions.Builder().setDetailedSources(true).build())
                .parseResult().findType("a.b.X");
    }

    @DisplayName("fully-qualified external reference becomes a namespace stub (detailed sources)")
    @Test
    public void fullyQualified() {
        TypeInfo x = parseAll();
        String m = """
                String method() {
                    return p.q.Helper.compute();
                }""";
        String out = isolate(x, "method", 0, m);
        @Language("java")
        String expected = """
                public class X_method {
                    class p { class q { class Helper {static String compute() { return null; } } } }
                    String method() {
                    return p.q.Helper.compute();
                }
                }
                """;
        assertEquals(expected, out);
        javaInspector.invalidateAllSources();
        assertNotNull(javaInspector.parse("X_method", out));
    }
}
