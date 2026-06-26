package org.e2immu.analyzer.modification.common;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

// static field access: a field on an external type must be stubbed; a field on a JDK type must not (no mutation)
public class TestIsolateMethod6FieldAccess extends CommonIsolateMethodTest {

    @Language("java")
    public static final String F1 = """
            package a.b;
            public class X {
                String method() {
                    return Constants.NAME;
                }
            }
            class Constants { static String NAME = "x"; }
            """;

    @DisplayName("static field access on an external stub type")
    @Test
    public void f1() {
        TypeInfo X = parse("a.b.X", F1);
        String m = """
                String method() {
                    return Constants.NAME;
                }""";
        String out = isolate(X, "method", 0, m);
        @Language("java")
        String expected = """
                public class X_method {
                    class Constants { static String NAME; }
                    String method() {
                    return Constants.NAME;
                }
                }
                """;
        assertEquals(expected, out);
        javaInspector.invalidateAllSources();
        assertNotNull(javaInspector.parse("X_method", out));
    }

    @Language("java")
    public static final String F2 = """
            package a.b;
            public class X {
                int method() {
                    return Integer.MAX_VALUE;
                }
            }
            """;

    @DisplayName("static field access on a JDK type must not be stubbed")
    @Test
    public void f2() {
        TypeInfo X = parse("a.b.X", F2);
        String m = """
                int method() {
                    return Integer.MAX_VALUE;
                }""";
        String out = isolate(X, "method", 0, m);
        @Language("java")
        String expected = """
                public class X_method {
                    int method() {
                    return Integer.MAX_VALUE;
                }
                }
                """;
        assertEquals(expected, out);
        javaInspector.invalidateAllSources();
        assertNotNull(javaInspector.parse("X_method", out));
    }
}
