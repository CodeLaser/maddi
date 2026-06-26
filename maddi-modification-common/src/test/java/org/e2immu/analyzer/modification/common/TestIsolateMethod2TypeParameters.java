package org.e2immu.analyzer.modification.common;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestIsolateMethod2TypeParameters extends CommonIsolateMethodTest {

    @Language("java")
    public static final String INPUT1 = """
            package a.b;
            public class X {
                static class Box<T> { T value; }
                String method(Box<String> box) {
                    return box.value;
                }
            }
            """;

    @DisplayName("nested generic type in the input")
    @Test
    public void test1() {
        TypeInfo X = parse("a.b.X", INPUT1);
        String methodString = """
                String method(Box<String> box) {
                    return box.value;
                }""";
        String out = isolate(X, "method", 1, methodString);
        @Language("java")
        String expected = """
                public class X_method {
                    class Box<T> { T value; }
                    String method(Box<String> box) {
                    return box.value;
                }
                }
                """;
        assertEquals(expected, out);
        javaInspector.invalidateAllSources();
        assertNotNull(javaInspector.parse("X_method", out));
    }

    @Language("java")
    public static final String INPUT2 = """
            package a.b;
            import java.util.List;
            public class X {
                String method(List<Holder<String>> list) {
                    return list.get(0).item;
                }
            }
            class Holder<T> { T item; }
            """;

    @DisplayName("external generic type used in a nested type-argument position")
    @Test
    public void test2() {
        TypeInfo X = parse("a.b.X", INPUT2);
        String methodString = """
                String method(List<Holder<String>> list) {
                    return list.get(0).item;
                }""";
        String out = isolate(X, "method", 1, methodString);
        @Language("java")
        String expected = """
                import java.util.List;
                public class X_method {
                    class Holder<T> { T item; }
                    String method(List<Holder<String>> list) {
                    return list.get(0).item;
                }
                }
                """;
        assertEquals(expected, out);
        javaInspector.invalidateAllSources();
        assertNotNull(javaInspector.parse("X_method", out));
    }

    @Language("java")
    public static final String INPUT3 = """
            package a.b;
            public class X {
                String method(Ext.Pair<String, Integer> p) {
                    return p.a;
                }
            }
            class Ext { static class Pair<A, B> { A a; B b; } }
            """;

    @DisplayName("external member (nested) generic type")
    @Test
    public void test3() {
        TypeInfo X = parse("a.b.X", INPUT3);
        String methodString = """
                String method(Ext.Pair<String, Integer> p) {
                    return p.a;
                }""";
        String out = isolate(X, "method", 1, methodString);
        @Language("java")
        String expected = """
                public class X_method {
                    class Ext { class Pair<A, B> { A a; } }
                    String method(Ext.Pair<String, Integer> p) {
                    return p.a;
                }
                }
                """;
        assertEquals(expected, out);
        javaInspector.invalidateAllSources();
        assertNotNull(javaInspector.parse("X_method", out));
    }

    @Language("java")
    public static final String INPUT4 = """
            package a.b;
            import org.junit.jupiter.api.function.ThrowingSupplier;
            public class X {
                String method(ThrowingSupplier<String> supplier) throws Throwable {
                    return supplier.get();
                }
            }
            """;

    @DisplayName("external library generic type (junit ThrowingSupplier<T>)")
    @Test
    public void test4() {
        TypeInfo X = parse("a.b.X", INPUT4);
        String methodString = """
                String method(ThrowingSupplier<String> supplier) throws Throwable {
                    return supplier.get();
                }""";
        String out = isolate(X, "method", 1, methodString);
        @Language("java")
        String expected = """
                public class X_method {
                    interface ThrowingSupplier<T> {default T get() { return null; } }
                    String method(ThrowingSupplier<String> supplier) throws Throwable {
                    return supplier.get();
                }
                }
                """;
        assertEquals(expected, out);
        javaInspector.invalidateAllSources();
        assertNotNull(javaInspector.parse("X_method", out));
    }
}
