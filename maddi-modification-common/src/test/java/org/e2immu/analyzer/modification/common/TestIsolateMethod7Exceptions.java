package org.e2immu.analyzer.modification.common;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

// exception types: a custom exception thrown/caught must be stubbed as a Throwable subtype so the text compiles
public class TestIsolateMethod7Exceptions extends CommonIsolateMethodTest {

    @Language("java")
    public static final String E1 = """
            package a.b;
            public class X {
                void method() throws MyException {
                    throw new MyException();
                }
            }
            class MyException extends Exception { }
            """;

    @DisplayName("custom checked exception in throws clause and a throw")
    @Test
    public void e1() {
        TypeInfo X = parse("a.b.X", E1);
        String m = """
                void method() throws MyException {
                    throw new MyException();
                }""";
        String out = isolate(X, "method", 0, m);
        @Language("java")
        String expected = """
                public class X_method {
                    class MyException extends Exception {MyException() { } }
                    void method() throws MyException {
                    throw new MyException();
                }
                }
                """;
        assertEquals(expected, out);
        javaInspector.invalidateAllSources();
        assertNotNull(javaInspector.parse("X_method", out));
    }

    @Language("java")
    public static final String E2 = """
            package a.b;
            public class X {
                String method(Runnable r) {
                    try {
                        r.run();
                        return null;
                    } catch (MyException e) {
                        return e.detail;
                    }
                }
            }
            class MyException extends RuntimeException { String detail; }
            """;

    @DisplayName("custom exception caught in a catch clause (type only appears in the catch)")
    @Test
    public void e2() {
        TypeInfo X = parse("a.b.X", E2);
        String m = """
                String method(Runnable r) {
                    try {
                        r.run();
                        return null;
                    } catch (MyException e) {
                        return e.detail;
                    }
                }""";
        String out = isolate(X, "method", 1, m);
        @Language("java")
        String expected = """
                public class X_method {
                    class MyException extends RuntimeException { String detail; }
                    String method(Runnable r) {
                    try {
                        r.run();
                        return null;
                    } catch (MyException e) {
                        return e.detail;
                    }
                }
                }
                """;
        assertEquals(expected, out);
        javaInspector.invalidateAllSources();
        assertNotNull(javaInspector.parse("X_method", out));
    }
}
