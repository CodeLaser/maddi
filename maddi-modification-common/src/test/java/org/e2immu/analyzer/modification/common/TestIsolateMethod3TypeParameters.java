package org.e2immu.analyzer.modification.common;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestIsolateMethod3TypeParameters extends CommonIsolateMethodTest {

    @Language("java")
    public static final String G1 = """
            package a.b;
            import java.util.List;
            public class X {
                <T> T method(T input, List<T> list) {
                    list.add(input);
                    return input;
                }
            }
            """;

    @DisplayName("isolated method is generic, JDK types only")
    @Test
    public void g1() {
        TypeInfo X = parse("a.b.X", G1);
        String m = """
                <T> T method(T input, List<T> list) {
                    list.add(input);
                    return input;
                }""";
        String out = isolate(X, "method", 2, m);
        @Language("java")
        String expected = """
                import java.util.List;
                public class X_method {
                    <T> T method(T input, List<T> list) {
                    list.add(input);
                    return input;
                }
                }
                """;
        assertEquals(expected, out);
        javaInspector.invalidateAllSources();
        assertNotNull(javaInspector.parse("X_method", out));
    }

    @Language("java")
    public static final String G2 = """
            package a.b;
            public class X {
                static class Box<U> { U value; }
                <T> T method(Box<T> box) {
                    return box.value;
                }
            }
            """;

    @DisplayName("isolated method generic + stub generic type")
    @Test
    public void g2() {
        TypeInfo X = parse("a.b.X", G2);
        String m = """
                <T> T method(Box<T> box) {
                    return box.value;
                }""";
        String out = isolate(X, "method", 1, m);
        @Language("java")
        String expected = """
                public class X_method {
                    class Box<U> { U value; }
                    <T> T method(Box<T> box) {
                    return box.value;
                }
                }
                """;
        assertEquals(expected, out);
        javaInspector.invalidateAllSources();
        assertNotNull(javaInspector.parse("X_method", out));
    }

    @Language("java")
    public static final String G3 = """
            package a.b;
            public class X {
                String method() {
                    return wrap("x");
                }
                <Y> Y wrap(Y y) { return y; }
            }
            """;

    @DisplayName("called local generic method")
    @Test
    public void g3() {
        TypeInfo X = parse("a.b.X", G3);
        String m = """
                String method() {
                    return wrap("x");
                }""";
        String out = isolate(X, "method", 0, m);
        @Language("java")
        String expected = """
                public class X_method {
                    <Y> Y wrap(Y y) { return null; }
                    String method() {
                    return wrap("x");
                }
                }
                """;
        assertEquals(expected, out);
        javaInspector.invalidateAllSources();
        assertNotNull(javaInspector.parse("X_method", out));
    }

    @Language("java")
    public static final String G4 = """
            package a.b;
            public class X {
                void method(Helper h) {
                    h.process("x");
                }
            }
            class Helper { <Z> void process(Z z) { } }
            """;

    @DisplayName("called generic method on an external stub type")
    @Test
    public void g4() {
        TypeInfo X = parse("a.b.X", G4);
        String m = """
                void method(Helper h) {
                    h.process("x");
                }""";
        String out = isolate(X, "method", 1, m);
        @Language("java")
        String expected = """
                public class X_method {
                    class Helper {<Z> void process(Z z) { } }
                    void method(Helper h) {
                    h.process("x");
                }
                }
                """;
        assertEquals(expected, out);
        javaInspector.invalidateAllSources();
        assertNotNull(javaInspector.parse("X_method", out));
    }
}
