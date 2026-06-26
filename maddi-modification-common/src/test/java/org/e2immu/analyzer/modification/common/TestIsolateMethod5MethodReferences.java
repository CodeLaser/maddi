package org.e2immu.analyzer.modification.common;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

// method references: a referenced method must be stubbed just like a regular call
public class TestIsolateMethod5MethodReferences extends CommonIsolateMethodTest {

    @Language("java")
    public static final String R1 = """
            package a.b;
            import java.util.List;
            public class X {
                void method(List<String> list) {
                    list.forEach(Helper::process);
                }
            }
            class Helper { static void process(String s) { } }
            """;

    @DisplayName("static method reference on an external stub type")
    @Test
    public void r1() {
        TypeInfo X = parse("a.b.X", R1);
        String m = """
                void method(List<String> list) {
                    list.forEach(Helper::process);
                }""";
        String out = isolate(X, "method", 1, m);
        @Language("java")
        String expected = """
                import java.util.List;
                public class X_method {
                    class Helper {static void process(String s) { } }
                    void method(List<String> list) {
                    list.forEach(Helper::process);
                }
                }
                """;
        assertEquals(expected, out);
        javaInspector.invalidateAllSources();
        assertNotNull(javaInspector.parse("X_method", out));
    }

    @Language("java")
    public static final String R2 = """
            package a.b;
            import java.util.List;
            public class X {
                void method(List<String> list) {
                    list.forEach(this::handle);
                }
                void handle(String s) { }
            }
            """;

    @DisplayName("instance method reference via this:: keeps the method on the frame")
    @Test
    public void r2() {
        TypeInfo X = parse("a.b.X", R2);
        String m = """
                void method(List<String> list) {
                    list.forEach(this::handle);
                }""";
        String out = isolate(X, "method", 1, m);
        @Language("java")
        String expected = """
                import java.util.List;
                public class X_method {
                    void handle(String s) { }
                    void method(List<String> list) {
                    list.forEach(this::handle);
                }
                }
                """;
        assertEquals(expected, out);
        javaInspector.invalidateAllSources();
        assertNotNull(javaInspector.parse("X_method", out));
    }

    @Language("java")
    public static final String R3 = """
            package a.b;
            import java.util.List;
            import java.util.function.Supplier;
            public class X {
                Supplier<Box> method() {
                    return Box::new;
                }
            }
            class Box { Box() { } }
            """;

    @DisplayName("constructor reference Box::new")
    @Test
    public void r3() {
        TypeInfo X = parse("a.b.X", R3);
        String m = """
                Supplier<Box> method() {
                    return Box::new;
                }""";
        String out = isolate(X, "method", 0, m);
        @Language("java")
        String expected = """
                import java.util.function.Supplier;
                public class X_method {
                    class Box {Box() { } }
                    Supplier<Box> method() {
                    return Box::new;
                }
                }
                """;
        assertEquals(expected, out);
        javaInspector.invalidateAllSources();
        assertNotNull(javaInspector.parse("X_method", out));
    }
}
