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

    @Language("java")
    public static final String F3 = """
            package a.b;
            interface Constants { String NAME = "x"; }
            public class X {
                String method() {
                    return Constants.NAME;
                }
            }
            """;

    @DisplayName("constant on an interface stub needs an initializer value")
    @Test
    public void f3() {
        TypeInfo X = parse("a.b.X", F3);
        String m = """
                String method() {
                    return Constants.NAME;
                }""";
        String out = isolate(X, "method", 0, m);
        @Language("java")
        String expected = """
                public class X_method {
                    interface Constants { String NAME = null; }
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
    public static final String F4 = """
            package a.b;
            public class X {
                interface C { int A = 0; int B = 1; int D = 2; }
                int method(int x) {
                    switch (x) {
                        case C.A: return 1;
                        case C.B: return 2;
                        case C.D: return 3;
                    }
                    return 0;
                }
            }
            """;

    @DisplayName("numeric interface constants used as switch labels get distinct values")
    @Test
    public void f4() {
        TypeInfo X = parse("a.b.X", F4);
        String m = """
                int method(int x) {
                    switch (x) {
                        case C.A: return 1;
                        case C.B: return 2;
                        case C.D: return 3;
                    }
                    return 0;
                }""";
        String out = isolate(X, "method", 1, m);
        @Language("java")
        String expected = """
                public class X_method {
                    interface C { int A = 0; int B = 1; int D = 2; }
                    int method(int x) {
                    switch (x) {
                        case C.A: return 1;
                        case C.B: return 2;
                        case C.D: return 3;
                    }
                    return 0;
                }
                }
                """;
        assertEquals(expected, out);
        javaInspector.invalidateAllSources();
        assertNotNull(javaInspector.parse("X_method", out));
    }

    @Language("java")
    public static final String F5 = """
            package a.b;
            public class X {
                static class C { static final int A = 7; static final int B = 9; }
                int method(int x) {
                    switch (x) {
                        case C.A: return 1;
                        case C.B: return 2;
                    }
                    return 0;
                }
            }
            """;

    @DisplayName("numeric class constants stay 'static final' with distinct values for switch labels")
    @Test
    public void f5() {
        TypeInfo X = parse("a.b.X", F5);
        String m = """
                int method(int x) {
                    switch (x) {
                        case C.A: return 1;
                        case C.B: return 2;
                    }
                    return 0;
                }""";
        String out = isolate(X, "method", 1, m);
        @Language("java")
        String expected = """
                public class X_method {
                    class C { static final int A = 0; static final int B = 1; }
                    int method(int x) {
                    switch (x) {
                        case C.A: return 1;
                        case C.B: return 2;
                    }
                    return 0;
                }
                }
                """;
        assertEquals(expected, out);
        javaInspector.invalidateAllSources();
        assertNotNull(javaInspector.parse("X_method", out));
    }
}
